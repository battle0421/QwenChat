package org.qwen.aiqwen.controller;

import com.alibaba.dashscope.utils.JsonUtils;
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
    public String openAIQwenChatTest(@RequestBody String messages){
        String chatCompletion=  qwenMainService.OpenAIQwenChat(messages);

        ChatRequestDto request = new ChatRequestDto();
        request.setMessage(JsonUtils.toJson(messages));
        request.setModel(qwenAPIkeyProperties.getModel());
        chatRecordService.saveChatRecord(request,chatCompletion);
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
