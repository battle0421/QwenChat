package org.qwen.aiqwen.config;

import com.openai.models.embeddings.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.qwen.aiqwen.properties.QwenAPIkeyProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AliyunBailianEmbeddingConfig {



    @Autowired
    private QwenAPIkeyProperties qwenAPIkeyProperties;
    // 注册阿里百练向量模型Bean，用于文本向量化
    @Bean
    public OpenAiEmbeddingModel aliyunBailianEmbeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(qwenAPIkeyProperties.getBaseUrl())
                .apiKey(qwenAPIkeyProperties.getApiKey())
                .modelName(qwenAPIkeyProperties.getEmbeddingModel())
                .build();
    }
}