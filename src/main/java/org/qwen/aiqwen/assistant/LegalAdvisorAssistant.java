package org.qwen.aiqwen.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import org.qwen.aiqwen.prompt.LegalAdvisorPrompt;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",chatMemoryProvider = "redisChatMemoryProvider")
public interface LegalAdvisorAssistant {

    @SystemMessage("你是一位专业的法律顾问，只回答法律相关的问题，输出限制:其他领域的问题可以委婉拒绝")
    String lawAdvisor(@MemoryId String memoryId,  @UserMessage LegalAdvisorPrompt legalAdvisorPrompt);
}
