
package org.qwen.aiqwen.service;

import org.qwen.aiqwen.dto.ChatRequestDto;
import org.qwen.aiqwen.dto.ChatResponseDto;
import org.qwen.aiqwen.entity.ChatRecord;

import java.util.List;

public interface ChatRecordService {

    ChatResponseDto saveChatRecord(ChatRequestDto request, String response);

    List<ChatRecord> getAllRecords();

    ChatRecord getRecordById(Long id);

    void deleteRecord(Long id);

    void saveToRedis(String sessionId, String userId, ChatRecord record);

    List<ChatRecord> getSessionRecords(String sessionId, String userId);

    void deleteSessionRecords(String sessionId, String userId);
}