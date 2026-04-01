package org.qwen.aiqwen.service;



import java.util.List;

public interface QwenMainService{

    /**
     * 百炼平台sdk调用
     * @param messages
     * @return
     */
    public  String OpenAIQwenChat(String messages);

    /**
     * langchain4j-open-ai-spring-boot-starter
     * langchain4j  sdk调用
     */
    public  String OpenAILangchain4jChat(List<String> messages);

}
