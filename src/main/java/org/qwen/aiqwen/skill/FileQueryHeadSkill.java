package org.qwen.aiqwen.skill;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.qwen.aiqwen.assistant.QueryRewriteAssistant;
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
    @Autowired
    private QueryRewriteAssistant queryRewriteAssistant;

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

        log.info("原始查询: {}", query);

        String cleanedQuery = cleanQuery(query);
        log.info("清洗后查询: {}", cleanedQuery);

        String rewrittenQuery = rewriteQuery(cleanedQuery);
        log.info("改写后查询: {}", rewrittenQuery);

        String answer = ragFileLoaderService.searchSimilar(memoryId, rewrittenQuery, 2);
        return Result.success(answer);
    }
    private String cleanQuery(String query) {
        if (query == null) {
            return "";
        }

        String cleaned = query.trim();

        cleaned = cleaned.replaceAll("\\s+", " ");

        cleaned = cleaned.replaceAll("[\\u200B-\\u200D\\uFEFF]", "");

        cleaned = cleaned.replaceAll("[\"'<>{}\\[\\]]", "");

        cleaned = cleaned.replaceAll("^\\s*(帮我|请|麻烦|能不能|可否|想要|需要)\\s*", "");

        cleaned = cleaned.replaceAll("\\s*(一下|哈|啊|呢|哦|呀|嘛|吧)\\s*$", "");

        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        return cleaned;
    }

    private String rewriteQuery(String cleanedQuery) {
        try {
            String rewritten = queryRewriteAssistant.rewriteQuery(cleanedQuery);
            if (rewritten != null && !rewritten.trim().isEmpty()) {
                return rewritten.trim();
            }
        } catch (Exception e) {
            log.warn("查询改写失败，使用清洗后的查询: {}", e.getMessage());
        }
        return cleanedQuery;
    }

}
