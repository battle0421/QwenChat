package org.qwen.aiqwen.controller;

import com.alibaba.dashscope.utils.JsonUtils;
import org.qwen.aiqwen.common.Result;
import org.qwen.aiqwen.dto.ChatRequestDto;
import org.qwen.aiqwen.prompt.PersonDto;
import org.qwen.aiqwen.properties.QwenAPIkeyProperties;
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
@RequestMapping("/api/rag")
public class RagQueryController {

    @Autowired
    private RagFileLoaderService ragFileLoaderService;


    @PostMapping("/query")
    public Result<String> query(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        if (query == null || query.trim().isEmpty()) {
            return Result.error("查询内容不能为空");
        }
        String memoryId = request.get("memoryId");
        int maxResult = Integer.parseInt(request.get("maxResult"));
        String answer = ragFileLoaderService.searchSimilar(memoryId, query, maxResult);


        return Result.success(answer);
    }


}