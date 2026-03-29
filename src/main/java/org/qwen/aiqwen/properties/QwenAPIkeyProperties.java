package org.qwen.aiqwen.properties;

import lombok.Data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "")
public class QwenAPIkeyProperties {
    @Value("${qwenApi.key}")
    private String apiKey;

    @Value("${qwenApi.baseUrl}")
    private String baseUrl;

    @Value("${qwenApi.model}")
    private String model;

    @Value("${langchain4j.community.dashscope.embedding-model}")
    private String embeddingModel;
}
