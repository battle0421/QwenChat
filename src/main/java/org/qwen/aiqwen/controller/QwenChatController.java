package org.qwen.aiqwen.controller;

import com.alibaba.dashscope.utils.JsonUtils;
import com.openai.models.chat.completions.ChatCompletion;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.qwen.aiqwen.dto.ChatRequestDto;
import org.qwen.aiqwen.entity.ChatRecord;
import org.qwen.aiqwen.properties.QwenAPIkeyProperties;
import org.qwen.aiqwen.service.ChatRecordService;
import org.qwen.aiqwen.service.QwenMainService;
import org.qwen.aiqwen.vo.ChatResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/AI-frist")
public class QwenChatController {
    @Autowired
    private ChatRecordService chatRecordService;
    @Autowired
    public QwenMainService qwenMainService;
    @Autowired
    public QwenAPIkeyProperties qwenAPIkeyProperties;

    @PostMapping("/helloQwen")
    public ChatCompletion openAIQwenChatTest(@RequestBody List<String> messages){
        ChatCompletion chatCompletion=  qwenMainService.OpenAIQwenChat(messages);
        String responseContent = "";
        if (chatCompletion != null && chatCompletion.choices() != null && !chatCompletion.choices().isEmpty()) {
            responseContent = String.valueOf(chatCompletion.choices().get(0).message().content());
        }
        ChatRequestDto request = new ChatRequestDto();
        request.setMessage(JsonUtils.toJson(messages));
        request.setModel(qwenAPIkeyProperties.getModel());
//        request.setSystemPrompt(messages.get(0));
        chatRecordService.saveChatRecord(request,responseContent);
        return chatCompletion;
    }

    @PostMapping("/helloQwenForLangchain4j")
    public String openAIQwenChatLangchain4j(@RequestBody List<String> messages) {
        return qwenMainService.OpenAILangchain4jChat(messages);
    }





    @GetMapping("/list")
    public ChatResponseVo listRecords() {
        log.info("获取所有聊天记录");
        List<ChatRecord> records = chatRecordService.getAllRecords();
        return ChatResponseVo.success(records);
    }

    @GetMapping("/{id}")
    public ChatResponseVo getRecord(@PathVariable Long id) {
        log.info("获取聊天记录：{}", id);
        ChatRecord record = chatRecordService.getRecordById(id);
        return ChatResponseVo.success(record);
    }

    @DeleteMapping("/{id}")
    public ChatResponseVo deleteRecord(@PathVariable Long id) {
        log.info("删除聊天记录：{}", id);
        chatRecordService.deleteRecord(id);
        return ChatResponseVo.success(null);
    }
}
