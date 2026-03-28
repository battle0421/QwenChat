package org.qwen.aiqwen.service;

import com.openai.models.chat.completions.ChatCompletion;

import java.util.List;

public interface QwenMainService{

    /**
     * 百炼平台sdk调用
     * @param messages
     * @return
     */
    public  ChatCompletion OpenAIQwenChat(List<String> messages);

    /**
     * langchain4j-open-ai-spring-boot-starter
     * langchain4j  sdk调用
     */
    public  String OpenAILangchain4jChat(List<String> messages);

}
