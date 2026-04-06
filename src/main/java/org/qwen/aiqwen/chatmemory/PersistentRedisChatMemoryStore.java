package org.qwen.aiqwen.chatmemory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PersistentRedisChatMemoryStore implements ChatMemoryStore {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String MEMORY_KEY_PREFIX = "langchain4j:memory:";
    private static final int MAX_MESSAGES = 20;
    private static final long EXPIRE_HOURS = 24;

    /**
     * 获取指定会话的聊天消息
     *
     * @param memoryId 会话 ID（sessionId）
     * @return 聊天消息列表（最多 20 条）
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        //langchin4j:memory:sessionId  使用了记忆存储，如果Assistant 没有声明，有默认值
        if(memoryId!=null && !"default".equals(memoryId)){
            try {
                String key = buildKey(memoryId);

                // 从 Redis 获取 JSON 字符串
                Object value = redisTemplate.opsForValue().get(key);

                if (value == null) {
                    log.debug("会话 {} 没有历史消息", memoryId);
                    return new ArrayList<>();
                }

                // 反序列化为 Map 列表，然后转换为 ChatMessage
                if (value instanceof String) {
                    List<Map<String, String>> messageMaps = objectMapper.readValue((String) value, new TypeReference<List<Map<String, String>>>() {
                    });

                    List<ChatMessage> messages = messageMaps.stream().map(this::mapToChatMessage).collect(Collectors.toList());

                    log.debug("从 Redis 获取会话 {} 的 {} 条消息", memoryId, messages.size());
                    return messages;
                }

                return new ArrayList<>();
            } catch (Exception e) {
                log.error("从 Redis 获取聊天记忆失败，memoryId: {}", memoryId, e);
                return new ArrayList<>();
            }
        }else {
            log.warn("会话ID为空");
            return new ArrayList<>();
        }

    }

    /**
     * 更新指定会话的聊天消息
     *
     * @param memoryId 会话 ID（sessionId）
     * @param messages 完整的聊天消息列表
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        if(memoryId!=null && !"default".equals(memoryId)){
            try {
                String key = buildKey(memoryId);

                // 限制消息数量，只保留最近 20 条
                List<ChatMessage> limitedMessages = messages;
                if (messages.size() > MAX_MESSAGES) {
                    limitedMessages = messages.subList(messages.size() - MAX_MESSAGES, messages.size());
                    log.debug("会话 {} 消息数超过 {}，截取最近 {} 条", memoryId, MAX_MESSAGES, MAX_MESSAGES);
                }

                // 将 ChatMessage 转换为 Map，以便序列化
                List<Map<String, String>> messageMaps = limitedMessages.stream().map(this::chatMessageToMap).collect(Collectors.toList());
                // 序列化为 JSON
                String json = objectMapper.writeValueAsString(messageMaps);

                // 存储到 Redis，设置过期时间
                redisTemplate.opsForValue().set(key, json, EXPIRE_HOURS, TimeUnit.HOURS);

                log.debug("更新会话 {} 的记忆到 Redis，消息数: {}", memoryId, limitedMessages.size());
            } catch (Exception e) {
                log.error("保存聊天记忆到 Redis 失败，memoryId: {}", memoryId, e);
                throw new RuntimeException("保存聊天记忆失败", e);
            }
        }
    }

    /**
     * 删除指定会话的聊天消息
     *
     * @param memoryId 会话 ID（sessionId）
     */
    @Override
    public void deleteMessages(Object memoryId) {
        try {
            String key = buildKey(memoryId);
            Boolean deleted = redisTemplate.delete(key);

            if (Boolean.TRUE.equals(deleted)) {
                log.info("删除会话 {} 的记忆成功", memoryId);
            } else {
                log.warn("会话 {} 的记忆不存在，无需删除", memoryId);
            }
        } catch (Exception e) {
            log.error("删除聊天记忆失败，memoryId: {}", memoryId, e);
        }
    }

    /**
     * 构建 Redis Key
     *
     * @param memoryId 会话 ID
     * @return Redis Key
     */
    private String buildKey(Object memoryId) {
        return MEMORY_KEY_PREFIX + memoryId.toString();
    }

    /**
     * 获取会话的消息数量
     *
     * @param memoryId 会话 ID
     * @return 消息数量
     */
    public int getMessageCount(Object memoryId) {
        try {
            List<ChatMessage> messages = getMessages(memoryId);
            return messages.size();
        } catch (Exception e) {
            log.error("获取会话消息数量失败，memoryId: {}", memoryId, e);
            return 0;
        }
    }

    /**
     * 将 ChatMessage 转换为 Map（用于序列化）
     */
    private Map<String, String> chatMessageToMap(ChatMessage message) {
        Map<String, String> map = new HashMap<>();

        if (message instanceof UserMessage) {
            map.put("type", "USER");
            map.put("content", ((UserMessage) message).singleText());
        } else if (message instanceof AiMessage) {
            map.put("type", "AI");
            map.put("content", ((AiMessage) message).text());
        } else if (message instanceof dev.langchain4j.data.message.SystemMessage) {
            map.put("type", "SYSTEM");
            map.put("content", ((dev.langchain4j.data.message.SystemMessage) message).text());
        } else {
            map.put("type", "UNKNOWN");
            map.put("content", message.toString());
        }

        return map;
    }

    /**
     * 将 Map 转换为 ChatMessage（用于反序列化）
     */
    private ChatMessage mapToChatMessage(Map<String, String> map) {
        String type = map.get("type");
        String content = map.get("content");

        if ("USER".equals(type)) {
            return new UserMessage(content);
        } else if ("AI".equals(type)) {
            return new AiMessage(content);
        } else if ("SYSTEM".equals(type)) {
            return new dev.langchain4j.data.message.SystemMessage(content != null ? content : "");
        } else {
            log.warn("未知的消息类型: {}", type);
            return new UserMessage(content);
        }
    }
}
