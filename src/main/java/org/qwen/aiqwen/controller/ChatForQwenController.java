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
import org.qwen.aiqwen.service.ChatRecordService;
import org.qwen.aiqwen.service.QwenMainService;
import org.qwen.aiqwen.service.RagFileLoaderService;
import org.qwen.aiqwen.util.NumberUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


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
    private ChatRecordService chatRecordService;
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





            if (memoryId == null || memoryId.trim().isEmpty()) {
                return Result.error("会话 ID 不能为空");
            }

            if (userInput == null || userInput.trim().isEmpty()) {
                return Result.error("消息内容不能为空");
            }

            log.info("收到消息 - SessionId: {}, Message: {}", memoryId, userInput);

            return skillRouter.route(memoryId, userInput);


    }
    @PostMapping(value = "/send/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStream(@RequestBody Map<String, String> request) {
        String memoryId = request.get("memoryId");
        String userInput = request.get("message");

        SseEmitter emitter = new SseEmitter(60000L);
        final boolean[] isCompleted = {false};

        if (memoryId == null || memoryId.trim().isEmpty()) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("会话 ID 不能为空"));
                emitter.complete();
                isCompleted[0] = true;
            } catch (IOException e) {
                log.error("发送错误消息失败", e);
            }
            return emitter;
        }

        if (userInput == null || userInput.trim().isEmpty()) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("消息内容不能为空"));
                emitter.complete();
                isCompleted[0] = true;
            } catch (IOException e) {
                log.error("发送错误消息失败", e);
            }
            return emitter;
        }

        log.info("收到流式消息 - SessionId: {}, Message: {}", memoryId, userInput);

        CompletableFuture.runAsync(() -> {
            try {
                log.info("开始处理流式请求 - SessionId: {}", memoryId);
                Result<Object> result = skillRouter.route(memoryId, userInput);

                if ("200".equals(result.getCode())) {
                    String content = result.getData() != null ? result.getData().toString() : "";
                    log.info("获取到回复内容，长度: {}", content.length());

                    char[] chars = content.toCharArray();
                    int sentCount = 0;
                    for (int i = 0; i < chars.length && !isCompleted[0]; i++) {
                        try {
                            String charStr = String.valueOf(chars[i]);
                            SseEmitter.SseEventBuilder event = SseEmitter.event()
                                    .name("message")
                                    .data(charStr)
                                    .reconnectTime(3000);
                            emitter.send(event);
                            sentCount++;

                            if (sentCount <= 5 || sentCount % 10 == 0) {
                                log.debug("已发送 {} 个字符: {}", sentCount, charStr);
                            }

                            try {
                                Thread.sleep(30);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                log.warn("流式发送被中断");
                                break;
                            }
                        } catch (IOException e) {
                            log.error("发送消息片段失败，已发送: {} 个字符", sentCount, e);
                            isCompleted[0] = true;
                            break;
                        }
                    }

                    log.info("流式发送完成，共发送: {} 个字符", sentCount);

                    if (!isCompleted[0]) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data("[DONE]")
                                    .reconnectTime(0));
                            log.info("发送完成信号");
                        } catch (IOException e) {
                            log.error("发送完成信号失败", e);
                        } finally {
                            emitter.complete();
                            isCompleted[0] = true;

                            try {
                                org.qwen.aiqwen.dto.ChatRequestDto chatRequest = new org.qwen.aiqwen.dto.ChatRequestDto();
                                chatRequest.setMessage(userInput);
                                chatRequest.setSessionId(memoryId);
                                chatRequest.setUserId("anonymous");
                                chatRequest.setRole("user");
                                chatRequest.setModel("qwen-stream");

                                chatRecordService.saveChatRecord(chatRequest, content);
                                log.info("流式聊天记录已保存");
                            } catch (Exception e) {
                                log.error("保存流式聊天记录失败", e);
                            }
                        }
                    }
                } else {
                    log.warn("技能路由返回错误: {}", result.getMessage());
                    if (!isCompleted[0]) {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data(result.getMessage()));
                        } catch (IOException e) {
                            log.error("发送错误消息失败", e);
                        } finally {
                            emitter.complete();
                            isCompleted[0] = true;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("流式处理消息失败", e);
                if (!isCompleted[0]) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data("处理消息失败：" + e.getMessage()));
                    } catch (IOException ex) {
                        log.error("发送错误消息失败", ex);
                    } finally {
                        emitter.complete();
                        isCompleted[0] = true;
                    }
                }
            }
        });

        emitter.onCompletion(() -> {
            log.info("SSE 连接完成 - SessionId: {}", memoryId);
            isCompleted[0] = true;
        });

        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时 - SessionId: {}", memoryId);
            isCompleted[0] = true;
            emitter.complete();
        });

        emitter.onError((ex) -> {
            log.error("SSE 连接错误 - SessionId: {}", memoryId, ex);
            isCompleted[0] = true;
        });

        return emitter;
    }



}
