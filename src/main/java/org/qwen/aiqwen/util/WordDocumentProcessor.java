package org.qwen.aiqwen.util;

import com.alibaba.dashscope.tokenizers.QwenTokenizer;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class WordDocumentProcessor {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private PineconeEmbeddingStore pineconeEmbeddingStore;

    /**
     * 处理Word文档并存储向量
     * @param filePath 文件路径
     */
    public void processAndStoreWordDocument(String filePath) {
        Path path = Path.of(filePath);

        try {
            // 1. 加载并解析文档
            Document document = loadWordDocument(path);
            String fileName = path.getFileName().toString();
            // 2. 优化文档分块
            DocumentSplitter optimizedSplitter = new OptimizedWordSplitter();


            List<TextSegment> segments = optimizedSplitter.split(document);

            // 3. 提取型号和故障信息
            extractModelAndFaultInfo(segments,fileName);

            // 4. 向量化并存储
            storeDocumentVectors(segments, path);

            log.info("成功处理文档: {}", filePath);

        } catch (IOException e) {
            log.error("处理文档失败: {}", filePath, e);
            throw new RuntimeException("处理文档失败: " + e.getMessage());
        }
    }

    /**
     * 加载Word文档
     */
    private Document loadWordDocument(Path filePath) throws IOException {
        // 2. 加载并解析文件(使用万能解析器,自动识别文件格式)
        ApachePoiDocumentParser parser = new  ApachePoiDocumentParser();

        return FileSystemDocumentLoader.loadDocument(
                filePath,
                parser
        );
    }



    /**
     * 提取型号和故障信息
     */
    private void extractModelAndFaultInfo(List<TextSegment> segments,String fileName) {
        Pattern modelPattern = Pattern.compile("机型.*?:\\s*(.+)");
        Pattern faultPattern = Pattern.compile("故障.*?:\\s*(.+)");

        for (TextSegment segment : segments) {
            String text = segment.text();

            // 提取型号
            Matcher modelMatcher = modelPattern.matcher(text);
            StringBuilder models = new StringBuilder();
            while (modelMatcher.find()) {
                if (models.length() > 0) models.append(",");
                models.append(modelMatcher.group());
            }
            if (models.length() > 0) {
                segment.metadata().put("models", models.toString());
            }

            // 提取故障描述
            Matcher faultMatcher = faultPattern.matcher(text);
            StringBuilder faults = new StringBuilder();
            while (faultMatcher.find()) {
                if (faults.length() > 0) faults.append(",");
                faults.append(faultMatcher.group(1).trim());
            }
            if (faults.length() > 0) {
                segment.metadata().put("faults", faults.toString());
            }
            segment.metadata().put("file_name", fileName);
        }

    }

    /**
     * 向量化并存储文档
     */
    private void storeDocumentVectors(List<TextSegment> segments, Path filePath) {
        int batchSize = 10;
        int totalSegments = segments.size();
        int batches = (totalSegments + batchSize - 1) / batchSize;
        // 生成一个安全的文件标识符（ASCII）
        String safeFileId = generateSafeId(filePath.getFileName().toString());
        for (int i = 0; i < batches; i++) {
            int start = i * batchSize;
            int end = Math.min(start + batchSize, totalSegments);
            List<TextSegment> batch = segments.subList(start, end);

            // 向量化
            Response<List<Embedding>> response = embeddingModel.embedAll(batch);
            List<Embedding> embeddings = response.content();

            // 生成ID
            List<String> ids = new ArrayList<>();
            for (int j = 0; j < batch.size(); j++) {
                ids.add("doc_" + safeFileId + "_seg_" + i + "_" + j);
            }

            // 存储到向量数据库
            pineconeEmbeddingStore.addAll(ids, embeddings, batch);
        }
    }
    /**
     * 将文件名转换为安全的 ASCII ID (使用 MD5 哈希)
     */
    private String generateSafeId(String fileName) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(fileName.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString(); // 返回 32 位十六进制字符串
        } catch (NoSuchAlgorithmException e) {
            // 如果 MD5 不可用，回退到简单的正则替换（移除所有非 ASCII 字符）
            return fileName.replaceAll("[^a-zA-Z0-9_-]", "_");
        }
    }
}
