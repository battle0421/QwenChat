package org.qwen.aiqwen.controller;

import com.alibaba.dashscope.utils.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.qwen.aiqwen.assistant.LegalAdvisorAssistant;
import org.qwen.aiqwen.common.ChatState;
import org.qwen.aiqwen.common.Result;
import org.qwen.aiqwen.dto.ChatRequestDto;
import org.qwen.aiqwen.dto.MeetingDoc;
import org.qwen.aiqwen.dto.UserSessionState;
import org.qwen.aiqwen.prompt.LegalAdvisorPrompt;
import org.qwen.aiqwen.prompt.PersonDto;
import org.qwen.aiqwen.properties.QwenAPIkeyProperties;
import org.qwen.aiqwen.router.SkillRouter;
import org.qwen.aiqwen.service.QwenMainService;
import org.qwen.aiqwen.service.RagFileLoaderService;
import org.qwen.aiqwen.util.NumberUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/chat")
@Slf4j
public class ChatForQwenController {
    @Autowired
    public QwenMainService qwenMainService;
    @Autowired
    public QwenAPIkeyProperties qwenAPIkeyProperties;
    @Autowired
    private RagFileLoaderService ragFileLoaderService;
    @Autowired
    private LegalAdvisorAssistant assistant;

    @Autowired
    private SkillRouter skillRouter;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String KEY_PREFIX = "chat:state:";
    private static final long EXPIRE_MIN = 5; // 5分钟过期
    @PostMapping("/helloQwen")
    public String openAIQwenChatTest(@RequestBody String messages) {
        String chatCompletion = qwenMainService.OpenAIQwenChat(messages);

        ChatRequestDto request = new ChatRequestDto();
        request.setMessage(JsonUtils.toJson(messages));
        request.setModel(qwenAPIkeyProperties.getModel());
        return chatCompletion;
    }

    @PostMapping("/helloQwenForLangchain4j")
    public String openAIQwenChatLangchain4j(@RequestBody List<String> messages) {
        return qwenMainService.OpenAILangchain4jChat(messages);
    }

    /**
     * 测试接口
     *
     * @param request
     * @return
     */
    @PostMapping("/getperson")
    public Result<PersonDto> chaForQwen(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        if (query == null || query.trim().isEmpty()) {
            return Result.error("查询内容不能为空");
        }
        String memoryId = request.get("memoryId");
        PersonDto answer = ragFileLoaderService.extractPerson(memoryId, query);


        return Result.success(answer);
    }


    /**
     * 测试接口
     *
     * @param request
     * @return
     */
    @PostMapping("/isGoodFlag")
    public Result<Boolean> isGoodFlag(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        if (query == null || query.trim().isEmpty()) {
            return Result.error("查询内容不能为空");
        }
        Boolean answer = ragFileLoaderService.isGoodFlag(query);


        return Result.success(answer);
    }

    /**
     * 法律顾问
     *
     * @param request
     * @return
     */
    @PostMapping("/legalAdvisorQuesion")
    public String legalAdvisorQuesion(@RequestBody Map<String, String> request) {
        String query = request.get("question");
        if (query == null || query.trim().isEmpty()) {
            return "查询内容不能为空";
        }
        String law = request.get("law");

        LegalAdvisorPrompt prompt = new LegalAdvisorPrompt();
        prompt.setQuestion(query);
        prompt.setLaw(law);

        String memoryId = request.get("memoryId");

        String answer = assistant.lawAdvisor(memoryId, prompt);


        return answer;
    }


    /**
     * 智能聊天接口（带意图识别和技能路由）
     */
    @PostMapping("/send")
    public Result<Object> sendMessage(@RequestBody Map<String, String> request) {
        String memoryId = request.get("memoryId");
        String userInput = request.get("message");
        //String query = intent.getUserInput();
        try {
            // 1. 从 Redis 拿状态
            UserSessionState state = getState(memoryId);
            // 判断是否为纯数字
            if (userInput.matches("\\d+")) {

                if (state != null && state.getCurrentState() == ChatState.WAIT_USER_CHOOSE_DOC) {
                    return Result.success(chooseDoc(memoryId, userInput, state));
                }
            }





            if (memoryId == null || memoryId.trim().isEmpty()) {
                return Result.error("会话 ID 不能为空");
            }

            if (userInput == null || userInput.trim().isEmpty()) {
                return Result.error("消息内容不能为空");
            }

            log.info("收到消息 - SessionId: {}, Message: {}", memoryId, userInput);

            return skillRouter.route(memoryId, userInput);

        } catch (Exception e) {
            log.error("处理消息失败", e);
            return Result.error("处理消息失败：" + e.getMessage());
        }
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


    private UserSessionState getState(String sessionId) throws Exception {
        Object json = redisTemplate.opsForValue().get(KEY_PREFIX + sessionId);
        if (json == null) return null;
        return objectMapper.readValue(json.toString(), UserSessionState.class);
    }

    private void clearState(String sessionId) {
        redisTemplate.delete(KEY_PREFIX + sessionId);
    }

}
