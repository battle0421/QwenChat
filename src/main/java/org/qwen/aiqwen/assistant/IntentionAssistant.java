package org.qwen.aiqwen.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import org.qwen.aiqwen.dto.ai.IntentResultAiDto;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",chatMemoryProvider = "redisChatMemoryProvider")
public interface IntentionAssistant {

    @SystemMessage(fromResource="getIntention.txt")
    @UserMessage("当前用户memoryId{{memoryId}},用户消息:{{message}}")
    IntentResultAiDto intention(@MemoryId String memoryId,
                                @V("message") String message,
                                @V("intentDefinitions") String intentDefinitions);
}
