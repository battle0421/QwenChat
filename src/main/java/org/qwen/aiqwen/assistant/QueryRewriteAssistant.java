package org.qwen.aiqwen.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel")
public interface QueryRewriteAssistant {

    @SystemMessage("你是一个专业的查询改写助手。你的任务是将用户的原始查询改写成更适合向量检索的形式。\n" +
            "规则：\n" +
            "1. 提取核心关键词和实体\n" +
            "2. 去除无关的语气词、标点符号\n" +
            "3. 保持语义不变，但使表达更加简洁明确\n" +
            "4. 如果是技术查询，保留关键技术术语\n" +
            "5. 如果是人名查询，保留人名\n" +
            "6. 只返回改写后的查询文本，不要任何解释")
    String rewriteQuery(@UserMessage String query);
}
