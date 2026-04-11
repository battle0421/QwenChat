package org.qwen.aiqwen.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class MeetingDoc implements Serializable {
    private String fileName;
    private String content;

    public MeetingDoc() {}
    public MeetingDoc(String fileName, String content) {
        this.fileName = fileName;
        this.content = content;
    }
}