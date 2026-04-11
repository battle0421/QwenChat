package org.qwen.aiqwen.skill;

import dev.langchain4j.agent.tool.Tool;
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


    @Tool(value = "查询公司内部文档、个人简历或技术资料。" +
            "当用户提到‘查询’、‘查找’、‘搜索’、‘简历’、‘文档’、‘资料’等关键词，" +
            "或者询问关于某个人、某个技术点、某份文件的具体信息时，必须调用此工具。")
    public Result<Object> execute(String memoryId,IntentResultAiDto intent) {
        String query = intent.getUserInput();
        if (query == null || query.trim().isEmpty()) {
            return Result.error("查询内容不能为空");
        }

        String answer = ragFileLoaderService.searchSimilar(memoryId, query, 2);
        return  Result.success(answer);
    }
}
