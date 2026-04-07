
package org.qwen.aiqwen.dto;

import java.io.Serializable;

public class ChatResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;

    private String message;

    private Object data;

    private Long timestamp;

    public ChatResponseDto() {
        this.timestamp = System.currentTimeMillis();
    }

    public static ChatResponseDto success(Object data) {
        ChatResponseDto vo = new ChatResponseDto();
        vo.setCode("200");
        vo.setMessage("success");
        vo.setData(data);
        return vo;
    }

    public static ChatResponseDto error(String message) {
        ChatResponseDto vo = new ChatResponseDto();
        vo.setCode("500");
        vo.setMessage(message);
        return vo;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}