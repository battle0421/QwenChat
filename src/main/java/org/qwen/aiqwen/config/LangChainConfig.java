package org.qwen.aiqwen.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.qwen.aiqwen.properties.QwenAPIkeyProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChainConfig {

    @Autowired
    private QwenAPIkeyProperties qwenAPIkeyProperties;

    @Bean
    public OpenAiChatModel openAiChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(qwenAPIkeyProperties.getBaseUrl())
                .apiKey(qwenAPIkeyProperties.getApiKey())
                .modelName(qwenAPIkeyProperties.getModel())
                .maxRetries(3)
                .temperature(0.7)
                .timeout(java.time.Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }



}
