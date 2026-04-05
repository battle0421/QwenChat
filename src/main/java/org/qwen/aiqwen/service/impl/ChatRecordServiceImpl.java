package org.qwen.aiqwen.service.impl;

import com.alibaba.dashscope.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.grammars.hql.HqlParser;
import org.qwen.aiqwen.common.Constants;
import org.qwen.aiqwen.dto.ChatRequestDto;
import org.qwen.aiqwen.entity.ChatRecord;
import org.qwen.aiqwen.exception.BusinessException;
import org.qwen.aiqwen.properties.QwenAPIkeyProperties;
import org.qwen.aiqwen.repository.ChatRecordRepository;
import org.qwen.aiqwen.service.ChatRecordService;
import org.qwen.aiqwen.vo.ChatResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class ChatRecordServiceImpl implements ChatRecordService {
    @Autowired
    public QwenAPIkeyProperties qwenAPIkeyProperties;
    @Autowired
    private ChatRecordRepository chatRecordRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
 // 日期时间格式化器
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public ChatResponseVo saveChatRecord(ChatRequestDto request, String response) {
        try {
            ChatRecord record = new ChatRecord();
            record.setMessage(request.getMessage());
            record.setModel(qwenAPIkeyProperties.getModel());
            record.setResponse(response != null ? response : "");
            record.setSessionId(request.getSessionId());
            record.setUserId(request.getUserId());
            record.setRole(request.getRole());
            record.setStatus("success");
            ChatRecord saved = chatRecordRepository.save(record);
            saveToRedis(request.getSessionId(), request.getUserId(), saved);
            return ChatResponseVo.success(saved);
        } catch (Exception e) {
            throw new BusinessException("保存聊天记录失败：" + e.getMessage());
        }


    }

    @Override
    public void saveToRedis(String sessionId, String userId, ChatRecord record) {
        try {
            String key = "chat:session:" + userId + ":" + sessionId;

            // 获取现有的会话记录
            List<ChatRecord> records = getSessionRecords(sessionId, userId);
            records.add(record);

            // 使用配置好的 ObjectMapper 序列化（支持 LocalDateTime）
            String json = objectMapper.writeValueAsString(records);
            redisTemplate.opsForValue().set(key, json, 7, TimeUnit.DAYS);

        } catch (Exception e) {
            throw new BusinessException("保存到 Redis 失败：" + e.getMessage());
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



    @Override
    public List<ChatRecord> getSessionRecords(String sessionId, String userId) {
        try {
            String key = "chat:session:" + userId + ":" + sessionId;
            Object value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                return new ArrayList<>();
            }

            // 反序列化 JSON
            if (value instanceof String) {
                return objectMapper.readValue((String) value, new TypeReference<List<ChatRecord>>() {
                });
            }

            return new ArrayList<>();
        } catch (Exception e) {
            throw new BusinessException("获取会话记录失败：" + e.getMessage());
        }
    }

    @Override
    public void deleteSessionRecords(String sessionId, String userId) {
        try {
            String key = "chat:session:" + userId + ":" + sessionId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            throw new BusinessException("删除会话记录失败：" + e.getMessage());
        }
    }



}