package org.qwen.aiqwen.config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.qwen.aiqwen.chatmemory.PersistentRedisChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeparateRedisAssistantConfig {
    @Autowired
    private PersistentRedisChatMemoryStore redisChatMemoryStore;
    @Bean
    ChatMemoryProvider redisChatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId).chatMemoryStore(redisChatMemoryStore).maxMessages(20).build();
    }
}
