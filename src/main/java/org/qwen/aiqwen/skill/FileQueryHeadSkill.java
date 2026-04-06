package org.qwen.aiqwen.skill;

import lombok.extern.slf4j.Slf4j;
import org.qwen.aiqwen.common.Result;
import org.qwen.aiqwen.dto.ai.IntentResultAiDto;
import org.qwen.aiqwen.service.RagFileLoaderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 文件资料查询skill
 */
@Slf4j
@Component
public class FileQueryHeadSkill implements ParentSkill{
    @Autowired
    private RagFileLoaderService ragFileLoaderService;

    @Override
    public String supportIntent() {
        return "query_document_info";
    }

    @Override
    public Result<Object> execute(String memoryId,IntentResultAiDto intent) {
        String query = intent.getUserInput();
        if (query == null || query.trim().isEmpty()) {
            return Result.error("查询内容不能为空");
        }

        String answer = ragFileLoaderService.searchSimilar(memoryId, query, 2);
        return  Result.success(answer);
    }
}
