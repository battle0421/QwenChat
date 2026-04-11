package org.qwen.aiqwen.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;


@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",chatMemoryProvider = "redisChatMemoryProvider",
        tools= {"fileQueryHeadSkill"}
)
public interface FileQueryAssistant {
    @SystemMessage("你是一个专业的企业助手，负责文件查询和单据维护。")
    String chat(@MemoryId String memoryId, @UserMessage String message);
}
