package org.qwen.aiqwen.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel")
public interface TokenizationAssistant {

    @SystemMessage("你是一个专业的中文分词专家。请对以下文本进行分词，要求：\n" +
            "\n" +
            "1. **保护专业术语完整性**：序列号、物料号、型号等不能被拆分\n" +
            "   - 序列号示例：DH-STMJ03-DFJ-0P1, SN123456\n" +
            "   - 物料号示例：10.2.3.4343.2302\n" +
            "   - IP地址：192.168.1.1\n" +
            "   - 版本号：v1.2.3\n" +
            "\n" +
            "2. **中文按语义分词**：如\"维修手册\"分为[\"维修\", \"手册\"]\n" +
            "\n" +
            "3. **英文单词保持完整**：如\"manual\"不分拆\n" +
            "\n" +
            "4. **返回格式**：纯 JSON 数组，每个元素是一个分词结果，不要有其他说明文字\n" +
            "\n" +
            "待分词文本：\"%s\"\n" +
            "\n" +
            "请直接返回 JSON 数组：")
    String tokenizationhandler(@UserMessage String query);
}
