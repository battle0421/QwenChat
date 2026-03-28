package org.qwen.aiqwen.serivce;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.qwen.aiqwen.properties.QwenAPIkeyProperties;

import java.util.List;

public interface QwenMainService{

    public  ChatCompletion OpenAIQwenChat(List<String> messages);
}
