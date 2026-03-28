package org.qwen.aiqwen.controller;

import com.openai.models.chat.completions.ChatCompletion;
import lombok.extern.slf4j.Slf4j;
import org.qwen.aiqwen.service.QwenMainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/AI-frist")
public class QwenChatController {
    @Autowired
    public QwenMainService qwenMainService;


    @PostMapping("/helloQwen")
    public ChatCompletion openAIQwenChatTest(@RequestBody List<String> messages){
      return   qwenMainService.OpenAIQwenChat(messages);
    }
}
