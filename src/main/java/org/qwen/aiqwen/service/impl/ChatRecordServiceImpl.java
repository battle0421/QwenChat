
package org.qwen.aiqwen.service.impl;

import org.qwen.aiqwen.common.Constants;
import org.qwen.aiqwen.dto.ChatRequestDto;
import org.qwen.aiqwen.entity.ChatRecord;
import org.qwen.aiqwen.exception.BusinessException;
import org.qwen.aiqwen.repository.ChatRecordRepository;
import org.qwen.aiqwen.service.ChatRecordService;
import org.qwen.aiqwen.vo.ChatResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ChatRecordServiceImpl implements ChatRecordService {

    @Autowired
    private ChatRecordRepository chatRecordRepository;

    @Override
    public ChatResponseVo saveChatRecord(ChatRequestDto request, String response) {
        try {
            ChatRecord record = new ChatRecord();
            record.setMessage(request.getMessage());
            record.setModel(request.getModel() != null ? request.getModel() : Constants.DEFAULT_MODEL);
            record.setResponse(response != null ? response : "");
            record.setStatus("success");
//            record.setCreateTime(LocalDateTime.now());

            ChatRecord saved = chatRecordRepository.save(record);
            return ChatResponseVo.success(saved);
        } catch (Exception e) {
            throw new BusinessException("保存聊天记录失败：" + e.getMessage());
        }
    }

    @Override
    public List<ChatRecord> getAllRecords() {
        return chatRecordRepository.findAll();
    }

    @Override
    public ChatRecord getRecordById(Long id) {
        Optional<ChatRecord> optional = chatRecordRepository.findById(id);
        return optional.orElseThrow(() -> new BusinessException("记录不存在"));
    }

    @Override
    public void deleteRecord(Long id) {
        if (!chatRecordRepository.existsById(id)) {
            throw new BusinessException("记录不存在");
        }
        chatRecordRepository.deleteById(id);
    }
}