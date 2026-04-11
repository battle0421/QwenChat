package org.qwen.aiqwen.dto;

import lombok.Data;
import org.qwen.aiqwen.common.ChatState;

import java.io.Serializable;
import java.util.List;

@Data
public class UserSessionState implements Serializable {
    private ChatState currentState;
    private List<MeetingDoc> docList;
}
