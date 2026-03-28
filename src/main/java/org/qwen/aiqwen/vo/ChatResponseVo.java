
package org.qwen.aiqwen.vo;

import java.io.Serializable;

public class ChatResponseVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;

    private String message;

    private Object data;

    private Long timestamp;

    public ChatResponseVo() {
        this.timestamp = System.currentTimeMillis();
    }

    public static ChatResponseVo success(Object data) {
        ChatResponseVo vo = new ChatResponseVo();
        vo.setCode("200");
        vo.setMessage("success");
        vo.setData(data);
        return vo;
    }

    public static ChatResponseVo error(String message) {
        ChatResponseVo vo = new ChatResponseVo();
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