package org.qwen.aiqwen.service;

import com.openai.models.chat.completions.ChatCompletion;

import java.util.List;

public interface QwenMainService{

    public  ChatCompletion OpenAIQwenChat(List<String> messages);
}
