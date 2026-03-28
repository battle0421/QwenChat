package org.qwen.aiqwen.serivce.impl;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.qwen.aiqwen.properties.QwenAPIkeyProperties;
import org.qwen.aiqwen.serivce.QwenMainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class QwenMainServiceImpl implements QwenMainService {
    @Autowired
    public QwenAPIkeyProperties qwenAPIkeyProperties;
    @Override
    public ChatCompletion OpenAIQwenChat(String message) {

        ChatCompletion chatCompletion=null;
            OpenAIClient client = OpenAIOkHttpClient.builder()
                    .apiKey(qwenAPIkeyProperties.getApiKey())
                    .baseUrl(qwenAPIkeyProperties.getBaseUrl())
                    .build();

            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .addUserMessage(message)
                    .model(qwenAPIkeyProperties.getModel())
                    .build();

            try {
                 chatCompletion = client.chat().completions().create(params);

            } catch (Exception e) {
                System.err.println("Error occurred: " + e.getMessage());
                e.printStackTrace();

        }
        return chatCompletion;
    }
}
