package org.qwen.aiqwen.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import org.qwen.aiqwen.dto.ai.IntentResultAiDto;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel")
public interface IntentionAssistant {

    @SystemMessage(fromResource="getIntention.txt")
    IntentResultAiDto intention(@V("intentDefinitions") String intentDefinitions,@UserMessage String message);
}
