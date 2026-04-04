package org.qwen.aiqwen.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
chatModel = "openAiChatModel",chatMemoryProvider = "redisChatMemoryProvider")
public interface SeparateRedisAssistant {
    String chat(@MemoryId String memoryId,@UserMessage String message);
}
