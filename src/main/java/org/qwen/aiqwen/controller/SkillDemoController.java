package org.qwen.aiqwen.controller;

import lombok.extern.slf4j.Slf4j;
import org.qwen.aiqwen.assistant.FileQueryAssistant;
import org.qwen.aiqwen.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/skill")
@Slf4j
public class SkillDemoController {

    @Autowired
    private FileQueryAssistant fileQueryAssistant;
    @PostMapping("/helloSkill")
    public Result chat(@RequestBody Map<String, Object> request) {
        String memoryId = request.get("memoryId").toString();
        String message = request.get("message").toString();

        // 这一步里，LLM 会自动完成：意图识别 -> 参数提取 -> 调用 Tool -> 总结回复
        String response = fileQueryAssistant.chat(memoryId, message);

        return Result.success(response);
    }
}
