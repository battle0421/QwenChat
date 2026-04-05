package org.qwen.aiqwen.controller;

import com.alibaba.dashscope.utils.JsonUtils;
import org.qwen.aiqwen.assistant.LegalAdvisorAssistant;
import org.qwen.aiqwen.common.Result;
import org.qwen.aiqwen.dto.ChatRequestDto;
import org.qwen.aiqwen.prompt.LegalAdvisorPrompt;
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
@RequestMapping("/api/chat")
public class ChatForQwenController {
    @Autowired
    public QwenMainService qwenMainService;
    @Autowired
    public QwenAPIkeyProperties qwenAPIkeyProperties;
    @Autowired
    private RagFileLoaderService ragFileLoaderService;
    @Autowired
    private
    LegalAdvisorAssistant assistant;

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

}
