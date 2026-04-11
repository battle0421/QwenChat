package org.qwen.aiqwen.router;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.qwen.aiqwen.assistant.IntentionAssistant;
import org.qwen.aiqwen.assistant.SeparateRedisAssistant;
import org.qwen.aiqwen.common.ChatState;
import org.qwen.aiqwen.common.IntentType;
import org.qwen.aiqwen.common.Result;
import org.qwen.aiqwen.dto.MeetingDoc;
import org.qwen.aiqwen.dto.UserSessionState;
import org.qwen.aiqwen.dto.ai.IntentResultAiDto;
import org.qwen.aiqwen.skill.ParentSkill;
import org.qwen.aiqwen.util.NumberUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
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
    private SeparateRedisAssistant separateRedisAssistant;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String KEY_PREFIX = "chat:state:";


    @Autowired
    public SkillRouter(List<ParentSkill> skills) {
        skillMap = skills.stream().collect(Collectors.toMap(ParentSkill::supportIntent, s -> s));
    }

    public Result<Object> route(String memoryId, String userInput) {
        try {
            // 1. 从 Redis 拿状态
            UserSessionState state = getState(memoryId);
            // 判断是否为纯数字
            if (userInput.matches("\\d+")) {

                if (state != null && state.getCurrentState() == ChatState.WAIT_USER_CHOOSE_DOC) {
                    return Result.success(chooseDoc(memoryId, userInput, state));
                }
            }

            String intentDefinitions = IntentType.getAllDescriptions();
            // 大模型返回统一结构 IntentResult
            IntentResultAiDto intent = llm.intention(intentDefinitions, userInput);

            log.info("意图识别结果======>：{}", intent.getIntentDefinitions());
            ParentSkill skill = skillMap.get(intent.getIntentDefinitions());
            if (skill != null) {
                // 调用技能
                return skill.execute(memoryId, intent);
            } else {
                // 没有匹配的技能，则调用大模型,存聊天
                return Result.success(separateRedisAssistant.chat(memoryId, userInput));
            }
        } catch (Exception e) {
            log.error("处理消息失败", e);
            return Result.error("处理消息失败：" + e.getMessage());
        }
    }

    private UserSessionState getState(String sessionId) throws Exception {
        Object json = redisTemplate.opsForValue().get(KEY_PREFIX + sessionId);
        if (json == null) return null;
        return objectMapper.readValue(json.toString(), UserSessionState.class);
    }

    private void clearState(String sessionId) {
        redisTemplate.delete(KEY_PREFIX + sessionId);
    }


    // ====================== 状态机核心：用户选第几个 ======================
    private String chooseDoc(String sessionId, String userInput, UserSessionState state) {
        Integer idx = NumberUtil.extract(userInput);
        List<MeetingDoc> docs = state.getDocList();

        if (idx == null || idx < 1 || idx > docs.size()) {
            return "请输入有效序号：1~" + docs.size();
        }

        // 选完清状态
        clearState(sessionId);

        MeetingDoc doc = docs.get(idx - 1);
        return "已为你打开第" + idx + "条：\n" + doc.getFileName() + "\n\n" + doc.getContent();
    }
}