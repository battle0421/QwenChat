package org.qwen.aiqwen.skill;

import org.qwen.aiqwen.common.Result;
import org.qwen.aiqwen.dto.ai.IntentResultAiDto;

public interface  ParentSkill {
    // 当前技能支持的意图
    String supportIntent();

    // 执行技能逻辑
    Result<Object> execute( IntentResultAiDto intent);
}
