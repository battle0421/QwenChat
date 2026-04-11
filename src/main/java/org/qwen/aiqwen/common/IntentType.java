package org.qwen.aiqwen.common;

import lombok.Getter;

@Getter
public enum IntentType {

    ADD_SPARES_MATERIAL("add_sparesMaterialApplyHead", "物料申请单新增"),
    DELETE_SPARES_MATERIAL("delete_sparesMaterialApplyHead", "物料申请单删除"),
    QUERY_SPARES_MATERIAL("query_sparesMaterialApplyHead", "物料申请单查询"),
    QUERY_MATERIAL_INFO("query_document_info", "文件资料查找,工作履历查找"),
    OTHER("other", "其他非业务相关对话");

    private final String code;
    private final String description;

    IntentType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取所有意图定义的文本描述（优化格式，适合大模型理解）
     */
    public static String getAllDescriptions() {
        StringBuilder sb = new StringBuilder();
        sb.append("请从以下意图代码中选择一个最匹配的：\n\n");

        for (IntentType type : values()) {
            sb.append("【").append(type.getCode()).append("】").append(type.getDescription()).append("\n");
        }

        sb.append("\n注意：只返回意图代码本身（如 add_sparesMaterialApplyHead），不要返回中文描述。");

        return sb.toString();
    }

    /**
     * 根据代码获取枚举
     */
    public static IntentType fromCode(String code) {
        for (IntentType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return OTHER;
    }
}
