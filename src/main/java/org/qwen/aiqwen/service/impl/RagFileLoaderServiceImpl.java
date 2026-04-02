package org.qwen.aiqwen.service.impl;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import org.qwen.aiqwen.service.RagFileLoaderService;
import org.qwen.aiqwen.util.LlmDocumentSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class RagFileLoaderServiceImpl implements RagFileLoaderService {

    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private OpenAiChatModel chatModel;
    @Autowired
    private PineconeEmbeddingStore pineconeEmbeddingStore;

    private static final int BATCH_SIZE = 10;



    /**
     * 获取文件类型
     */
    private String getFileType(String fileName) {
        if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
            return "WORD";
        } else if (fileName.endsWith(".pdf")) {
            return "PDF";
        } else if (fileName.endsWith(".txt")) {
            return "TXT";
        } else if (fileName.endsWith(".md")) {
            return "MARKDOWN";
        }
        return "OTHER";
    }


    /**
     * 加载文件接口
     * @param path
     */
    @Override
    public void loadRagFile(String path){


        Path filePath = Path.of(path);
        String fileName = filePath.getFileName().toString();

        // 2. 加载并解析文件（使用万能解析器，自动识别文件格式）
        Document document = FileSystemDocumentLoader.loadDocument(
                filePath,
                new ApacheTikaDocumentParser() // 解析器：负责提取文件中的纯文本
        );
        DocumentSplitter lineSplitter = new LlmDocumentSplitter();
        // 3. 分割文件
        List<TextSegment> segments = lineSplitter.split(document);
        // 3. 为每个片段添加元数据（文件名、路径等）
        for (TextSegment segment : segments) {
            segment.metadata().put("fileName", fileName);
            segment.metadata().put("filePath", filePath.toString());
            segment.metadata().put("fileType", getFileType(fileName));
        }
        // 4. 分批处理向量化（每批最多 10 个）
        int totalSegments = segments.size();
        int batches = (totalSegments + BATCH_SIZE - 1) / BATCH_SIZE;
        List<Embedding> list = new ArrayList<>();

        Response<List<Embedding>> responseList= null;
        for (int i = 0; i < batches; i++) {
            int start = i * BATCH_SIZE;
            int end = Math.min(start + BATCH_SIZE, totalSegments);
            List<TextSegment> batch = segments.subList(start, end);

            Response<List<Embedding>> response = embeddingModel.embedAll(batch);

            list.addAll(response.content());
        }

        List<String> ids = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            ids.add("seg_" + System.currentTimeMillis() + "_" + i);
        }
        pineconeEmbeddingStore.addAll( ids,list, segments);


    }


    /**
     * 查询相似的文本片段
     * @param query 查询文本
     * @param maxResults 最大返回结果数
     * @return 匹配的文本片段列表
     */
    public String searchSimilar(String query, int maxResults) {
        Response<Embedding> queryEmbedding = embeddingModel.embed(query);
        // 2. 构建元数据过滤器
        dev.langchain4j.store.embedding.filter.Filter metadataFilter = buildMetadataFilter("demo1.txt", null, null);

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding.content())
                .filter(metadataFilter)
                .maxResults(maxResults)
                .minScore(0.5)  // 只返回相似度 >= 0.7 的结果
                .build();



        List<EmbeddingMatch<TextSegment>> matches = pineconeEmbeddingStore.search(searchRequest).matches();
        // 3. 构建上下文
        StringBuilder context = new StringBuilder("相关文档信息：\n\n");
        for (EmbeddingMatch<TextSegment> match : matches) {
            context.append(match.embedded().text())
                    .append("\n[相似度：")
                    .append(String.format("%.2f", match.score() * 100))
                    .append("%]\n\n");
        }

        // 4. 构建提示词并调用 LLM
        String prompt = String.format(
                "%s\n\n请根据以上信息回答这个问题：%s",
                context.toString(),
                query
        );

        return chatModel.chat(prompt);
    }

    /**
     * 构建元数据过滤器
     * @param fileName 文件名
     * @param fileType 文件类型
     * @param filePath 文件路径
     * @return 组合过滤器
     */
    private dev.langchain4j.store.embedding.filter.Filter buildMetadataFilter(
            String fileName, String fileType, String filePath) {

        dev.langchain4j.store.embedding.filter.Filter filter = null;

        // 使用 MetadataFilterBuilder 构建过滤条件
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

        return filter;  // 如果所有参数都为 null，返回 null 表示不过滤
    }

}
