package org.qwen.aiqwen.service.impl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


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
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import org.qwen.aiqwen.service.RagFileLoaderService;
import org.qwen.aiqwen.util.LlmDocumentSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
     * 加载文件接口
     * @param path
     */
    @Override
    public void loadRagFile(String path){


        Path filePath = Path.of(path);

        // 2. 加载并解析文件（使用万能解析器，自动识别文件格式）
        Document document = FileSystemDocumentLoader.loadDocument(
                filePath,
                new ApacheTikaDocumentParser() // 解析器：负责提取文件中的纯文本
        );
        DocumentSplitter lineSplitter = new LlmDocumentSplitter();
        // 3. 分割文件
        List<TextSegment> segments = lineSplitter.split(document);

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
//            embeddingStore.addAll( response.content(), batch);
            list.addAll(response.content());
        }
// 先生成 IDs 列表
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
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding.content())
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

        return chatModel.generate(prompt);
    }
}
