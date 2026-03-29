package org.qwen.aiqwen.service.impl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


import com.alibaba.dashscope.utils.JsonUtils;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.apache.poi.ss.formula.functions.T;
import org.qwen.aiqwen.service.RagFileLoaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RagFileLoaderServiceImpl implements RagFileLoaderService {

    @Autowired
    private OpenAiEmbeddingModel embeddingModel;

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
//        for (TextSegment segment : segments) {
//            embeddingModel.embedAll(segments);
//        }
        // 4. 分批处理向量化（每批最多 10 个）
        int totalSegments = segments.size();
        int batches = (totalSegments + BATCH_SIZE - 1) / BATCH_SIZE;
//        List<Embedding> list = new ArrayList<>();

        Response<List<Embedding>> responseList= null;
        for (int i = 0; i < batches; i++) {
            int start = i * BATCH_SIZE;
            int end = Math.min(start + BATCH_SIZE, totalSegments);
            List<TextSegment> batch = segments.subList(start, end);

            Response<List<Embedding>> response = embeddingModel.embedAll(batch);
            responseList.content().addAll(response.content());
        }



    }
}
