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
import org.qwen.aiqwen.util.SmartTokenizer;
import org.qwen.aiqwen.util.SmartWordDocumentSplitter;
import org.qwen.aiqwen.util.WordDocumentProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RagFileLoaderServiceImpl implements RagFileLoaderService {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private PineconeEmbeddingStore pineconeEmbeddingStore;



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

    private static final double DENSE_WEIGHT = 0.5;
    private static final double SPARSE_WEIGHT = 0.5;


    private final Object buildLock = new Object();

    private static final String BM25_INDEX_KEY = "rag:bm25:index";
    private static final String BM25_DOC_FREQ_KEY = "rag:bm25:docfreq";
    private static final String BM25_DOC_STORE_KEY = "rag:bm25:docstore";
    private static final String BM25_TOTAL_COUNT_KEY = "rag:bm25:totalcount";

    @Autowired
    private SmartTokenizer smartTokenizer;
// ... existing code ...

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


        for (int i = 0; i < batches; i++) {
            int start = i * BATCH_SIZE;
            int end = Math.min(start + BATCH_SIZE, totalSegments);
            List<TextSegment> batch = segments.subList(start, end);

            Response<List<Embedding>> response = embeddingModel.embedAll(batch);
            denseEmbeddings.addAll(response.content());

        }


        List<String> ids = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            String docId = "seg_" + System.currentTimeMillis() + "_" + i;
            ids.add(docId);
            // ✅ 关键修改：入库时同步构建 BM25 索引
            buildAndStoreBM25Index(docId, segments.get(i));
        }

        pineconeEmbeddingStore.addAll(ids, denseEmbeddings, segments);



        log.info("文件处理完成: {}, 片段数: {}", fileName, segments.size());
    }


    /**
     * 构建并存储 BM25 索引到 Redis
     */
    private void buildAndStoreBM25Index(String docId, TextSegment segment) {
        try {
            String[] tokens = tokenize(segment.text());
            Map<String, Integer> termFreqMap = new HashMap<>();

            for (String token : tokens) {
                termFreqMap.put(token, termFreqMap.getOrDefault(token, 0) + 1);
            }

            String termFreqJson = objectMapper.writeValueAsString(termFreqMap);
            redisTemplate.opsForHash().put(BM25_INDEX_KEY, docId, termFreqJson);

            Map<String, String> segmentData = new HashMap<>();
            segmentData.put("text", segment.text());
            segmentData.put("fileName", segment.metadata().getString("fileName"));
            segmentData.put("filePath", segment.metadata().getString("filePath"));
            segmentData.put("fileType", segment.metadata().getString("fileType"));

            String segmentJson = objectMapper.writeValueAsString(segmentData);
            redisTemplate.opsForHash().put(BM25_DOC_STORE_KEY, docId, segmentJson);

            Set<String> uniqueTerms = termFreqMap.keySet();
            for (String term : uniqueTerms) {
                redisTemplate.opsForHash().increment(BM25_DOC_FREQ_KEY, term, 1);
            }

            redisTemplate.opsForValue().increment(BM25_TOTAL_COUNT_KEY);

            log.debug("BM25 索引构建成功, docId: {}, 词数: {}", docId, termFreqMap.size());
        } catch (Exception e) {
            log.error("构建 BM25 索引失败, docId: {}", docId, e);
        }
    }
// ... existing code ...


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
     * 混合检索：多路召回（密集向量 + BM25全文检索）
     */
    public String searchSimilar(String memoryId, String query, int maxResults) {
        try {
            log.info("开始多路召回检索，查询: {}", query);



            Response<Embedding> queryEmbedding = embeddingModel.embed(query);
            String[] queryTokens = tokenize(query);

            dev.langchain4j.store.embedding.filter.Filter metadataFilter = buildMetadataFilter(null, null, null);

            EmbeddingSearchRequest denseSearchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding.content())
                    .filter(metadataFilter)
                    .maxResults(maxResults * 2)
                    .minScore(0.3)
                    .build();

            List<EmbeddingMatch<TextSegment>> denseMatches = pineconeEmbeddingStore.search(denseSearchRequest).matches();
            log.info("第一路-密集向量检索结果数: {}", denseMatches.size());

            List<Map<String, Object>> sparseMatches = bm25FullTextSearch(queryTokens, maxResults * 2);
            log.info("第二路-BM25全文检索结果数: {}", sparseMatches.size());

            List<HybridResult> hybridResults = mergeResults(denseMatches, sparseMatches, maxResults);
            log.info("多路召回融合后结果数: {}", hybridResults.size());

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
            log.error("多路召回检索异常", e);
            return "系统异常，请稍后重试。详细错误: " + e.getMessage();
        }
    }
    /**
     * BM25 全文检索 - 基于 Redis 中存储的倒排索引进行检索（不依赖向量）
     */
    private List<Map<String, Object>> bm25FullTextSearch(String[] queryTokens, int maxResults) {
        List<Map<String, Object>> results = new ArrayList<>();

        if (queryTokens == null || queryTokens.length == 0) {
            return results;
        }

        try {
            Object totalCountObj = redisTemplate.opsForValue().get(BM25_TOTAL_COUNT_KEY);
            if (totalCountObj == null) {
                log.warn("BM25 索引为空，请先加载文档");
                return results;
            }

            int totalDocs = ((Number) totalCountObj).intValue();
            if (totalDocs == 0) {
                log.warn("BM25 索引中文档数量为0");
                return results;
            }

            double k1Param = 1.5;
            double bParam = 0.75;
            double avgDocLength = calculateAvgDocLengthFromRedis();

            Map<String, Double> bm25Scores = new HashMap<>();

            for (String queryToken : queryTokens) {
                Object dfObj = redisTemplate.opsForHash().get(BM25_DOC_FREQ_KEY, queryToken);
                int df = dfObj != null ? ((Number) dfObj).intValue() : 0;

                if (df == 0) {
                    log.debug("查询词 '{}' 在索引中不存在", queryToken);
                    continue;
                }

                double idf = Math.log((totalDocs - df + 0.5) / (df + 0.5) + 1.0);

                Map<Object, Object> allDocs = redisTemplate.opsForHash().entries(BM25_INDEX_KEY);

                for (Map.Entry<Object, Object> entry : allDocs.entrySet()) {
                    String docId = (String) entry.getKey();
                    String termFreqJson = (String) entry.getValue();

                    try {
                        Map<String, Integer> termFreqMap = objectMapper.readValue(
                                termFreqJson,
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Integer>>() {}
                        );

                        Integer termFreq = termFreqMap.get(queryToken);
                        if (termFreq == null || termFreq == 0) {
                            continue;
                        }

                        int docLength = termFreqMap.values().stream().mapToInt(Integer::intValue).sum();

                        double tfWeight = termFreq * (k1Param + 1) /
                                (termFreq + k1Param * (1 - bParam + bParam * docLength / avgDocLength));

                        double score = idf * tfWeight;
                        bm25Scores.merge(docId, score, Double::sum);

                    } catch (Exception e) {
                        log.warn("解析文档词频失败, docId: {}", docId, e);
                    }
                }
            }

            for (Map.Entry<String, Double> entry : bm25Scores.entrySet()) {
                String docId = entry.getKey();
                double score = entry.getValue();

                if (score > 0) {
                    Object segmentJson = redisTemplate.opsForHash().get(BM25_DOC_STORE_KEY, docId);
                    if (segmentJson != null) {
                        try {
                            Map<String, String> segmentData = objectMapper.readValue(
                                    (String) segmentJson,
                                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {}
                            );

                            dev.langchain4j.data.document.Metadata metadata =
                                    dev.langchain4j.data.document.Metadata.from(Map.of(
                                            "fileName", segmentData.get("fileName"),
                                            "filePath", segmentData.get("filePath"),
                                            "fileType", segmentData.get("fileType")
                                    ));

                            TextSegment segment = TextSegment.from(
                                    segmentData.get("text"),
                                    metadata
                            );

                            Map<String, Object> result = new HashMap<>();
                            result.put("segment", segment);
                            result.put("score", (float) score);
                            result.put("id", docId);
                            results.add(result);
                        } catch (Exception e) {
                            log.warn("还原文档片段失败, docId: {}", docId, e);
                        }
                    }
                }
            }

            results.sort((resultA, resultB) -> Float.compare(
                    ((Number) resultB.get("score")).floatValue(),
                    ((Number) resultA.get("score")).floatValue()
            ));

            if (results.size() > maxResults) {
                results = results.subList(0, maxResults);
            }

            log.info("BM25 全文检索完成，查询词数: {}, 总文档数: {}, 匹配结果数: {}",
                    queryTokens.length, totalDocs, results.size());
        } catch (Exception e) {
            log.error("BM25 全文检索失败", e);
        }

        return results;
    }

    /**
     * 从 Redis 计算平均文档长度
     */
    private double calculateAvgDocLengthFromRedis() {
        try {
            Map<Object, Object> allDocs = redisTemplate.opsForHash().entries(BM25_INDEX_KEY);
            if (allDocs.isEmpty()) {
                return 100;
            }

            double totalLength = 0;
            int count = 0;

            for (Object termFreqJson : allDocs.values()) {
                try {
                    Map<String, Integer> termFreqMap = objectMapper.readValue(
                            (String) termFreqJson,
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Integer>>() {}
                    );
                    totalLength += termFreqMap.values().stream().mapToInt(Integer::intValue).sum();
                    count++;
                } catch (Exception e) {
                    log.warn("计算文档长度失败", e);
                }
            }

            return count > 0 ? totalLength / count : 100;
        } catch (Exception e) {
            log.error("计算平均文档长度失败", e);
            return 100;
        }
    }

// ... existing code ...





    /**
     * 智能分词工具 - 保护专业术语，支持中英文混合
     */
    private String[] tokenize(String text) {
        return smartTokenizer.tokenize(text);
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
