package org.qwen.aiqwen.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
chatModel = "openAiChatModel",chatMemoryProvider = "redisChatMemoryProvider")
public interface SeparateRedisAssistant {
    @SystemMessage("""
        你是一个亲切、自然、口语化的智能助手。
        回答简短、直接、像聊天，不解释规则、不提隐私、不客套。
        不要说“根据对话”“在当前会话中”这种生硬的话。
        直接回答，像朋友一样自然交流。
    """)
    String chat(@MemoryId String memoryId,@UserMessage String message);
}
