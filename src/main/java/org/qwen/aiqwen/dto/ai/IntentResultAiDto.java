package org.qwen.aiqwen.dto.ai;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class IntentResultAiDto {

    // ===================== 固定字段（所有意图都有） =====================

    private String intentDefinitions;

    private String sessionId;

    private String userInput;


    private Map<String, Object> slots = new HashMap<>();



    public String getString(String key) {
        return slots.get(key) == null ? null : slots.get(key).toString();
    }

    public Integer getInt(String key) {
        return slots.get(key) == null ? null : Integer.parseInt(slots.get(key).toString());
    }
}