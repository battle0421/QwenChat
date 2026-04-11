package org.qwen.aiqwen.controller;

import lombok.extern.slf4j.Slf4j;
import org.qwen.aiqwen.service.RagFileLoaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/AI-file")
public class RagDocController {
    @Autowired
    private RagFileLoaderService ragFileLoaderService;

    @RequestMapping("/loadRagFile")
    public String loadRagFile(@RequestBody   String path){
        log.info("加载文件：{}", path);
        ragFileLoaderService.loadRagWordFile(path);
        return "加载成功";
    }


}
