
package org.qwen.aiqwen.service;

import org.qwen.aiqwen.dto.ChatRequestDto;
import org.qwen.aiqwen.entity.ChatRecord;
import org.qwen.aiqwen.vo.ChatResponseVo;

import java.util.List;

public interface ChatRecordService {

    ChatResponseVo saveChatRecord(ChatRequestDto request, String response);

    List<ChatRecord> getAllRecords();

    ChatRecord getRecordById(Long id);

    void deleteRecord(Long id);
}