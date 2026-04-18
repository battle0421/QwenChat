package org.qwen.aiqwen.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.qwen.aiqwen.assistant.SeparateRedisAssistant;
import org.qwen.aiqwen.common.ChatState;
import org.qwen.aiqwen.dto.MeetingDoc;
import org.qwen.aiqwen.dto.UserSessionState;
import org.qwen.aiqwen.exception.BusinessException;
import org.qwen.aiqwen.prompt.PersonDto;
import org.qwen.aiqwen.service.RagFileLoaderService;
import org.qwen.aiqwen.util.LlmDocumentSplitter;
import org.qwen.aiqwen.util.SmartWordDocumentSplitter;
import org.qwen.aiqwen.util.WordDocumentProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RagFileLoaderServiceImpl implements RagFileLoaderService {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private PineconeEmbeddingStore pineconeEmbeddingStore;

    @Autowired
    private SparseVectorService sparseVectorService;

    private static final int BATCH_SIZE = 10;

    @Autowired
    SeparateRedisAssistant separateRedisAssistant;

    @Autowired
    private WordDocumentProcessor wordDocumentProcessor;


    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String KEY_PREFIX = "chat:state:";
    private static final long EXPIRE_MIN = 5;

    // 混合检索权重配置
    private static final double DENSE_WEIGHT = 0.5;
    private static final double SPARSE_WEIGHT = 0.5;

    @Override
    public void loadRagFile(String path) {
        Path rootPath = Path.of(path);

        if (Files.isRegularFile(rootPath)) {
            processSingleFile(rootPath);
        } else if (Files.isDirectory(rootPath)) {
            try {
                Files.walk(rootPath)
                        .filter(Files::isRegularFile)
                        .filter(this::isSupportedFileType)
                        .forEach(this::processSingleFile);
            } catch (IOException e) {
                throw new BusinessException("读取目录失败：" + e.getMessage());
            }
        } else {
            throw new BusinessException("路径不存在：" + path);
        }
    }

    public void processSingleFile(Path filePath) {
        String fileName = filePath.getFileName().toString();
        String fileType = getFileType(fileName);

        Document document = FileSystemDocumentLoader.loadDocument(
                filePath,
                new ApacheTikaDocumentParser()
        );
        document.metadata().put("filePath", filePath.toString());

        DocumentSplitter splitter;
        if ("WORD".equals(fileType)) {
            splitter = new SmartWordDocumentSplitter();
            log.info("使用智能 Word 分割器处理文件: {}", fileName);
        } else {
            splitter = new LlmDocumentSplitter();
        }

        List<TextSegment> segments = splitter.split(document);

        for (TextSegment segment : segments) {
            segment.metadata().put("fileName", fileName);
            segment.metadata().put("filePath", filePath.toString());
            segment.metadata().put("fileType", fileType);
        }

        int totalSegments = segments.size();
        int batches = (totalSegments + BATCH_SIZE - 1) / BATCH_SIZE;
        List<Embedding> denseEmbeddings = new ArrayList<>();
        List<Map<String, Float>> sparseVectors = new ArrayList<>();
        List<String> texts = new ArrayList<>();

        for (int i = 0; i < batches; i++) {
            int start = i * BATCH_SIZE;
            int end = Math.min(start + BATCH_SIZE, totalSegments);
            List<TextSegment> batch = segments.subList(start, end);

            Response<List<Embedding>> response = embeddingModel.embedAll(batch);
            denseEmbeddings.addAll(response.content());

            for (TextSegment segment : batch) {
                texts.add(segment.text());
                sparseVectors.add(sparseVectorService.generateSparseVector(segment.text()));
            }
        }

        sparseVectorService.updateIdf(texts);

        List<String> ids = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            ids.add("seg_" + System.currentTimeMillis() + "_" + i);
        }

        pineconeEmbeddingStore.addAll(ids, denseEmbeddings, segments);

        log.info("文件处理完成: {}, 片段数: {}", fileName, segments.size());
    }


    private String getFileType(String path) {
        if (path.endsWith(".doc") || path.endsWith(".docx")) {
            return "WORD";
        } else if (path.endsWith(".pdf")) {
            return "PDF";
        } else if (path.endsWith(".txt")) {
            return "TXT";
        } else if (path.endsWith(".md")) {
            return "MARKDOWN";
        }
        return "OTHER";
    }

    private boolean isSupportedFileType(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".pdf") ||
                fileName.endsWith(".doc") ||
                fileName.endsWith(".docx") ||
                fileName.endsWith(".txt") ||
                fileName.endsWith(".md") ||
                fileName.endsWith(".html");
    }

    /**
     * 混合检索：结合密集向量和稀疏向量（BM25）
     */
    public String searchSimilar(String memoryId, String query, int maxResults) {
        try {
            log.info("开始混合检索，查询: {}", query);

            Response<Embedding> queryEmbedding = embeddingModel.embed(query);

            String[] queryTokens = tokenize(query);

            dev.langchain4j.store.embedding.filter.Filter metadataFilter = buildMetadataFilter(null, null, null);

            EmbeddingSearchRequest denseSearchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding.content())
                    .filter(metadataFilter)
                    .maxResults(maxResults * 2)
                    .minScore(0.5)
                    .build();

            List<EmbeddingMatch<TextSegment>> denseMatches = pineconeEmbeddingStore.search(denseSearchRequest).matches();
            log.info("密集向量检索结果数: {}", denseMatches.size());

            List<Map<String, Object>> sparseMatches = bm25HybridSearch(queryTokens, denseMatches, maxResults * 2);
            log.info("稀疏向量(BM25)检索结果数: {}", sparseMatches.size());

            List<HybridResult> hybridResults = mergeResults(denseMatches, sparseMatches, maxResults);
            log.info("混合检索合并后结果数: {}", hybridResults.size());

            if (hybridResults.isEmpty()) {
                return "未找到相关文档，请尝试使用更具体的关键词或更换查询内容。";
            }

            List<MeetingDoc> docList = new ArrayList<>();
            String docname = "";

            for (HybridResult result : hybridResults) {
                TextSegment segment = result.getSegment();
                String fileName = segment.metadata().getString("fileName");
                MeetingDoc doc = new MeetingDoc(fileName, segment.text());
                docList.add(doc);
                if (!docname.contains(fileName)) {
                    docname = docname + " | " + fileName;
                }
            }

//            if (docList.size() > 1) {
//                UserSessionState userSessionState = new UserSessionState();
//                userSessionState.setCurrentState(ChatState.WAIT_USER_CHOOSE_DOC);
//                userSessionState.setDocList(docList);
//                saveState(memoryId, userSessionState);
//
//                return "找到 " + docList.size() + " 份相关文档，请选择第几份：" + docname;
//            }

            StringBuilder context = new StringBuilder("根据以下资料信息回答用户问题:\n\n");
            for (int i = 0; i < hybridResults.size(); i++) {
                HybridResult result = hybridResults.get(i);
                context.append("[资料").append(i + 1).append("] (相关度: ").append(String.format("%.2f", result.getFinalScore())).append(")\n");
                context.append(result.getSegment().text()).append("\n\n");
            }
            context.append("\n请根据以上资料直接回答用户的问题: ").append(query);

            log.info("最终提示词长度: {}", context.length());
            return separateRedisAssistant.chat(memoryId, context.toString());
        } catch (Exception e) {
            log.error("混合检索异常", e);
            return "系统异常，请稍后重试。详细错误: " + e.getMessage();
        }
    }

    /**
     * BM25 稀疏向量检索
     */
    private List<Map<String, Object>> bm25HybridSearch(String[] queryTokens,
                                                       List<EmbeddingMatch<TextSegment>> denseMatches,
                                                       int maxResults) {
        List<Map<String, Object>> results = new ArrayList<>();

        if (queryTokens == null || queryTokens.length == 0) {
            return results;
        }

        try {
            Map<String, Integer> docFreq = new HashMap<>();
            int totalDocs = denseMatches.size();

            for (EmbeddingMatch<TextSegment> match : denseMatches) {
                TextSegment segment = match.embedded();
                String[] docTokens = tokenize(segment.text());
                Set<String> uniqueTokens = new HashSet<>(Arrays.asList(docTokens));

                for (String token : uniqueTokens) {
                    docFreq.put(token, docFreq.getOrDefault(token, 0) + 1);
                }
            }

            double k1Param = 1.5;
            double bParam = 0.75;

            for (EmbeddingMatch<TextSegment> match : denseMatches) {
                TextSegment segment = match.embedded();
                String[] docTokens = tokenize(segment.text());
                int docLength = docTokens.length;
                double avgDocLength = totalDocs > 0 ?
                        denseMatches.stream().mapToLong(m -> m.embedded().text().split("\\s+").length).average().orElse(100) : 100;

                double bm25Score = 0.0;
                for (String queryToken : queryTokens) {
                    int termFreq = (int) Arrays.stream(docTokens).filter(t -> t.equals(queryToken)).count();
                    int df = docFreq.getOrDefault(queryToken, 0);

                    double idf = Math.log((totalDocs - df + 0.5) / (df + 0.5) + 1.0);
                    double tfWeight = termFreq * (k1Param + 1) / (termFreq + k1Param * (1 - bParam + bParam * docLength / avgDocLength));

                    bm25Score += idf * tfWeight;
                }

                if (bm25Score > 0.5) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("segment", segment);
                    result.put("score", (float) bm25Score);
                    result.put("id", match.embeddingId());
                    results.add(result);
                }
            }

            results.sort((resultA, resultB) -> Float.compare(
                    ((Number) resultB.get("score")).floatValue(),
                    ((Number) resultA.get("score")).floatValue()
            ));

            if (results.size() > maxResults) {
                results = results.subList(0, maxResults);
            }

            log.info("BM25 检索完成，返回 {} 个结果", results.size());
        } catch (Exception e) {
            log.warn("BM25 检索失败: {}", e.getMessage());
        }

        return results;
    }

    /**
     * 分词工具
     */
    private String[] tokenize(String text) {
        return text.toLowerCase()
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                .split("\\s+");
    }



    /**
     * 合并密集和稀疏检索结果
     */
    private List<HybridResult> mergeResults(
            List<EmbeddingMatch<TextSegment>> denseMatches,
            List<Map<String, Object>> sparseMatches,
            int maxResults) {

        Map<String, HybridResult> mergedMap = new HashMap<>();

        for (EmbeddingMatch<TextSegment> match : denseMatches) {
            String id = match.embeddingId();
            HybridResult result = new HybridResult();
            result.setSegment(match.embedded());
            result.setDenseScore(match.score());
            result.setSparseScore(0.0f);
            result.setId(id);
            mergedMap.put(id, result);
        }

        for (Map<String, Object> sparseResult : sparseMatches) {
            String id = (String) sparseResult.get("id");
            TextSegment segment = (TextSegment) sparseResult.get("segment");
            float score = (float) sparseResult.get("score");

            if (mergedMap.containsKey(id)) {
                mergedMap.get(id).setSparseScore(score);
            } else {
                HybridResult result = new HybridResult();
                result.setSegment(segment);
                result.setDenseScore(0.0);
                result.setSparseScore(score);
                result.setId(id);
                mergedMap.put(id, result);
            }
        }

        List<HybridResult> results = new ArrayList<>(mergedMap.values());

        for (HybridResult result : results) {
            double normalizedDense = result.getDenseScore() / (result.getDenseScore() + 1e-8);
            double normalizedSparse = result.getSparseScore() / (result.getSparseScore() + 1e-8);
            double finalScore = DENSE_WEIGHT * normalizedDense + SPARSE_WEIGHT * normalizedSparse;
            result.setFinalScore(finalScore);
        }

        results.sort((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()));

        if (results.size() > maxResults) {
            results = results.subList(0, maxResults);
        }

        return results;
    }

    private void saveState(String sessionId, UserSessionState state) throws Exception {
        String json = objectMapper.writeValueAsString(state);
        redisTemplate.opsForValue().set(KEY_PREFIX + sessionId, json, EXPIRE_MIN, TimeUnit.MINUTES);
    }

    private dev.langchain4j.store.embedding.filter.Filter buildMetadataFilter(
            String fileName, String fileType, String filePath) {

        dev.langchain4j.store.embedding.filter.Filter filter = null;

        if (fileName != null && !fileName.isEmpty()) {
            filter = dev.langchain4j.store.embedding.filter.MetadataFilterBuilder
                    .metadataKey("fileName")
                    .isEqualTo(fileName);
        }

        if (fileType != null && !fileType.isEmpty()) {
            dev.langchain4j.store.embedding.filter.Filter typeFilter = dev.langchain4j.store.embedding.filter.MetadataFilterBuilder
                    .metadataKey("fileType")
                    .isEqualTo(fileType);

            filter = filter == null ? typeFilter : filter.and(typeFilter);
        }

        if (filePath != null && !filePath.isEmpty()) {
            dev.langchain4j.store.embedding.filter.Filter pathFilter = dev.langchain4j.store.embedding.filter.MetadataFilterBuilder
                    .metadataKey("filePath")
                    .isEqualTo(filePath);

            filter = filter == null ? pathFilter : filter.and(pathFilter);
        }

        return filter;
    }

    public PersonDto extractPerson(String memoryId, String message) {
        PersonDto personDto = separateRedisAssistant.extractPerson(memoryId, message);
        return personDto;
    }

    public Boolean isGoodFlag(String message) {
        Boolean flag = separateRedisAssistant.isGoodOrBad(message);
        return flag;
    }

    @Override
    public void loadRagWordFile(String path) {
        Path rootPath = Path.of(path);

        if (Files.isRegularFile(rootPath)) {
            wordDocumentProcessor.processAndStoreWordDocument(rootPath.toString());
        } else if (Files.isDirectory(rootPath)) {
            try {
                Files.walk(rootPath)
                        .filter(Files::isRegularFile)
                        .filter(this::isSupportedFileType)
                        .forEach(file -> wordDocumentProcessor.processAndStoreWordDocument(file.toString()));
            } catch (IOException e) {
                throw new BusinessException("读取目录失败：" + e.getMessage());
            }
        } else {
            throw new BusinessException("路径不存在：" + path);
        }
    }

    /**
     * 混合检索结果
     */
    private static class HybridResult {
        private String id;
        private TextSegment segment;
        private double denseScore;
        private float sparseScore;
        private double finalScore;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public TextSegment getSegment() { return segment; }
        public void setSegment(TextSegment segment) { this.segment = segment; }

        public double getDenseScore() { return denseScore; }
        public void setDenseScore(double denseScore) { this.denseScore = denseScore; }

        public float getSparseScore() { return sparseScore; }
        public void setSparseScore(float sparseScore) { this.sparseScore = sparseScore; }

        public double getFinalScore() { return finalScore; }
        public void setFinalScore(double finalScore) { this.finalScore = finalScore; }
    }
}
