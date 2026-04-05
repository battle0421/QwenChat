package org.qwen.aiqwen.common;

import lombok.Getter;

@Getter
public enum IntentType {

    ADD_SPARES_MATERIAL("add_sparesMaterialApplyHead", "物料申请单新增"),
    DELETE_SPARES_MATERIAL("delete_sparesMaterialApplyHead", "物料申请单删除"),
    QUERY_SPARES_MATERIAL("query_sparesMaterialApplyHead", "物料申请单查询"),
    QUERY_MATERIAL_INFO("query_material_info", "文件资料查找"),
    OTHER("other", "其他");

    private final String code;
    private final String description;

    IntentType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static String getAllDescriptions() {
        StringBuilder sb = new StringBuilder("意图类型：\n");
        for (IntentType type : values()) {
            sb.append(type.getCode()).append(": ").append(type.getDescription()).append("\n");
        }
        return sb.toString();
    }

    public static IntentType fromCode(String code) {
        for (IntentType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return OTHER;
    }
}
