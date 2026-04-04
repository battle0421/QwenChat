package org.qwen.aiqwen.controller;

import com.alibaba.dashscope.utils.JsonUtils;
import org.qwen.aiqwen.common.Result;
import org.qwen.aiqwen.dto.ChatRequestDto;
import org.qwen.aiqwen.properties.QwenAPIkeyProperties;
import org.qwen.aiqwen.service.ChatRecordService;
import org.qwen.aiqwen.service.QwenMainService;
import org.qwen.aiqwen.service.RagFileLoaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class RagQueryController {

    @Autowired
    public QwenMainService qwenMainService;
    @Autowired
    public QwenAPIkeyProperties qwenAPIkeyProperties;
    @Autowired
    private RagFileLoaderService ragFileLoaderService;

    @PostMapping("/query")
    public Result<String> query(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        if (query == null || query.trim().isEmpty()) {
            return Result.error("查询内容不能为空");
        }
        int maxResult = Integer.parseInt(request.get("maxResult"));
        String answer = ragFileLoaderService.searchSimilar(query, maxResult);
        return Result.success(answer);
    }

    @PostMapping("/helloQwen")
    public String openAIQwenChatTest(@RequestBody String messages){
        String chatCompletion=  qwenMainService.OpenAIQwenChat(messages);

        ChatRequestDto request = new ChatRequestDto();
        request.setMessage(JsonUtils.toJson(messages));
        request.setModel(qwenAPIkeyProperties.getModel());
        return chatCompletion;
    }

    @PostMapping("/helloQwenForLangchain4j")
    public String openAIQwenChatLangchain4j(@RequestBody List<String> messages) {
        return qwenMainService.OpenAILangchain4jChat(messages);
    }
}