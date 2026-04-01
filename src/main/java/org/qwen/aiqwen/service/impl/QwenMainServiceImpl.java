package org.qwen.aiqwen.service.impl;


import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import dev.langchain4j.model.openai.OpenAiChatModel;

import org.qwen.aiqwen.properties.QwenAPIkeyProperties;
import org.qwen.aiqwen.service.QwenMainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QwenMainServiceImpl implements QwenMainService {

    @Autowired
    private OpenAiChatModel chatModel;

    @Autowired
    public QwenAPIkeyProperties qwenAPIkeyProperties;


    @Override
    public String OpenAIQwenChat(String messages) {
//        ChatCompletion chatCompletion=null;
//        GenerationParam   client = GenerationParam.builder()
//                    .apiKey(qwenAPIkeyProperties.getApiKey())
//                    .baseUrl(qwenAPIkeyProperties.getBaseUrl())
//                    .build();
//
//            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
//                    .addSystemMessage(messages.get(0))
//                    .addAssistantMessage(messages.get(1))
//                    .addUserMessage(messages.get(2))
//                    .model(qwenAPIkeyProperties.getModel())
//                    .build();
//
//            try {
//                 chatCompletion = client.chat().completions().create(params);
//
//            } catch (Exception e) {
//                System.err.println("Error occurred: " + e.getMessage());
//                e.printStackTrace();
//
//        }
//        return chatCompletion;
        GenerationResult result = null;
        try {
            Generation gen = new Generation();

            Message message = Message.builder()
                    .role(Role.USER.getValue())
                    .content(messages)
                    .build();

            GenerationParam param = GenerationParam.builder()
                    .apiKey(qwenAPIkeyProperties.getApiKey()) // 你的key
                    .model(qwenAPIkeyProperties.getModel())
                    .messages(List.of(message))
                    .build();

            result = gen.call(param);
        } catch (Exception e) {
//            log.atError().log("Error occurred: " + e.getMessage());
        }
        return result == null ? "" : result.getOutput().getChoices().get(0).getMessage().getContent();
    }


    @Override
    public String OpenAILangchain4jChat(List<String> messages) {
        String userMessage = messages.get(messages.size() - 1);
        return chatModel.chat(userMessage);
    }
}
