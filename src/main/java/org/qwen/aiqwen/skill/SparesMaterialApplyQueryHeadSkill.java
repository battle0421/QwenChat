package org.qwen.aiqwen.skill;

import lombok.extern.slf4j.Slf4j;
import org.qwen.aiqwen.common.Result;
import org.qwen.aiqwen.dto.ai.IntentResultAiDto;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class SparesMaterialApplyQueryHeadSkill implements ParentSkill{
    @Override
    public String supportIntent() {
        return "query_sparesMaterialApplyHead";
    }

    @Override
    public Result<Object> execute( String memoryId,IntentResultAiDto intent) {
        log.info("查询成功：{}", intent);
        return Result.success("查询成功");
    }
}
