
package org.qwen.aiqwen.dto;

import java.io.Serializable;

public class ChatRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String message;

    private String model;

    private String systemPrompt;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
}