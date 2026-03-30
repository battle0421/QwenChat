package org.qwen.aiqwen.controller;

import org.qwen.aiqwen.common.Result;
import org.qwen.aiqwen.service.RagFileLoaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        int maxResult = Integer.parseInt(request.get("maxResult"));
        String answer = ragFileLoaderService.searchSimilar(query, maxResult);
        return Result.success(answer);
    }
}