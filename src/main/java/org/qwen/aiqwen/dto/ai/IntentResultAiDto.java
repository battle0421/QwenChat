package org.qwen.aiqwen.dto.ai;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class IntentResultAiDto {

    // ===================== 固定字段（所有意图都有） =====================
    @Description("意图分析 1：单据查找  add_sparesMaterialApplyHead:单据新增，单据删除，文件资料查找，其他")
    private String intent;          // 意图：lost_item / found_item / query_item / chat
    private String sessionId;       // 会话ID
    private String userInput;       // 用户原始输入

    // ===================== 扩展字段（所有场景都用它存！） =====================
    // 物品名称、地点、时间、颜色、金额、订单号、证件号...全部放这里！
    private Map<String, Object> slots = new HashMap<>();


    // ========== 工具方法：方便获取字段 ==========
    public String getString(String key) {
        return slots.get(key) == null ? null : slots.get(key).toString();
    }

    public Integer getInt(String key) {
        return slots.get(key) == null ? null : Integer.parseInt(slots.get(key).toString());
    }
}