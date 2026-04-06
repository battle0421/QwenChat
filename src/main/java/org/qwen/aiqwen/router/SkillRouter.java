package org.qwen.aiqwen.router;

import lombok.extern.slf4j.Slf4j;
import org.qwen.aiqwen.assistant.IntentionAssistant;
import org.qwen.aiqwen.assistant.SeparateRedisAssistant;
import org.qwen.aiqwen.common.IntentType;
import org.qwen.aiqwen.common.Result;
import org.qwen.aiqwen.dto.ai.IntentResultAiDto;
import org.qwen.aiqwen.skill.ParentSkill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SkillRouter {

    private final Map<String, ParentSkill> skillMap;

    @Autowired
    private IntentionAssistant llm;

    @Autowired
    private   SeparateRedisAssistant separateRedisAssistant;

    @Autowired
    public SkillRouter(List<ParentSkill> skills) {
        skillMap = skills.stream()
                .collect(Collectors.toMap(ParentSkill::supportIntent, s -> s));
    }

    public Result<Object> route(String memoryId, String userInput) {
        String intentDefinitions = IntentType.getAllDescriptions();
        log.info("意图定义：{}", intentDefinitions);
        // 大模型返回统一结构 IntentResult
        IntentResultAiDto intent = llm.intention(intentDefinitions,userInput);


        ParentSkill skill = skillMap.get(intent.getIntentDefinitions());
        if (skill != null) {
            return skill.execute( intent);
        } else {
            return Result.success(separateRedisAssistant.chat(memoryId, userInput));
        }
    }
}