package org.qwen.aiqwen.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.qwen.aiqwen.dto.ChatRequestDto;
import org.qwen.aiqwen.entity.ChatRecord;
import org.qwen.aiqwen.repository.ChatRecordRepository;
import org.qwen.aiqwen.service.ChatRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class ChatRecordInterceptor implements HandlerInterceptor {
    @Autowired
    private ChatRecordRepository chatRecordRepository;
    @Autowired
    private ChatRecordService chatRecordService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 只处理方法类型的请求
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 检查是否需要记录聊天（可以通过注解或路径判断）
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();

        // 这里可以根据注解或方法名判断是否需要记录
        // 暂时对所有 POST 请求进行记录

        // 生成或获取会话信息
        String sessionId = request.getHeader("X-Session-ID");
        String userId = request.getHeader("X-User-ID");
        String role = request.getHeader("X-Role");

        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
            log.info("生成新会话 ID: {}", sessionId);
        }

        if (userId == null || userId.isEmpty()) {
            userId = "anonymous";
        }

        if (role == null || role.isEmpty()) {
            role = "user";
        }

        // 将会话信息设置到 request attribute 中
        request.setAttribute("sessionId", sessionId);
        request.setAttribute("userId", userId);
        request.setAttribute("role", role);
        request.setAttribute("requestBody", readRequestBody(request));

        log.debug("会话信息 - SessionId: {}, UserId: {}, Role: {}", sessionId, userId, role);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!(handler instanceof HandlerMethod)) {
            return;
        }

        // 只在请求成功后保存记录
        if (ex != null || response.getStatus() >= 400) {
            log.warn("请求失败或异常，不保存聊天记录");
            return;
        }

        try {
            // 从 request 中获取请求体内容
            String requestBody = (String) request.getAttribute("requestBody");
            String sessionId = (String) request.getAttribute("sessionId");
            String userId = (String) request.getAttribute("userId");
            String role = (String) request.getAttribute("role");

            String responseBody = (String) request.getAttribute("cachedResponseBody");
            if (requestBody != null && !requestBody.isEmpty()) {
                saveChatRecord(requestBody, response, sessionId, userId, role);
            }
        } catch (Exception e) {
            log.error("保存聊天记录失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 读取请求体
     */
    private String readRequestBody(HttpServletRequest request) {
        try {
            // 注意：需要在过滤器中提前缓存请求体
            Object cachedBody = request.getAttribute("cachedRequestBody");
            if (cachedBody != null) {
                return cachedBody.toString();
            }
        } catch (Exception e) {
            log.error("读取请求体失败：{}", e.getMessage());
        }
        return null;
    }

    /**
     * 保存聊天记录
     */
    private void saveChatRecord(String requestBody, HttpServletResponse response, String sessionId, String userId, String role) {

        try {
            // 尝试解析请求体为 Map
            Map<String, Object> requestMap = objectMapper.readValue(requestBody, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
            });

            // 构建 ChatRequestDto
            ChatRequestDto requestDto = new ChatRequestDto();

            // 从 Map 中提取字段
            if (requestMap.containsKey("message")) {
                requestDto.setMessage(requestMap.get("message").toString());
            } else if (requestMap.containsKey("query")) {
                // RAG 查询的情况
                requestDto.setMessage(requestMap.get("query").toString());
            }
            if (requestMap.containsKey("question")) {
                // RAG 查询的情况
                requestDto.setMessage(requestMap.get("question").toString());
            }

            if (requestMap.containsKey("model")) {
                requestDto.setModel(requestMap.get("model").toString());
            } else {
                requestDto.setModel("RAG");
            }

            if (requestMap.containsKey("systemPrompt")) {
                requestDto.setSystemPrompt(requestMap.get("systemPrompt").toString());
            }

            // 设置会话信息
            requestDto.setSessionId(sessionId);
            requestDto.setUserId(userId);
            requestDto.setRole(role);
            // 解析响应内容
            String responseContent =extractResponseContent(response);

            // 调用 Service 保存记录（会自动保存到数据库和 Redis）
            chatRecordService.saveChatRecord(requestDto, responseContent);

        } catch (Exception e) {
            log.error("解析请求体失败：{}", e.getMessage());
        }
    }

    /**
     * 169→     * 从 HttpServletResponse 中提取响应内容为 String
     * 170→     * @param response HTTP 响应对象
     * 171→     * @return 响应内容字符串
     * 172→
     */
    private String extractResponseContent(HttpServletResponse response) {
        try {
            // 如果响应被 ContentCachingResponseWrapper 包装
            if (response instanceof org.springframework.web.util.ContentCachingResponseWrapper wrappedResponse) {
                byte[] content = wrappedResponse.getContentAsByteArray();
                String contentStr = new String(content, StandardCharsets.UTF_8);
                log.debug("成功从包装的响应中获取内容，长度：{}", contentStr.length());
                return new String(content, java.nio.charset.StandardCharsets.UTF_8);
            }

            // 如果是普通的 HttpServletResponse，尝试从缓冲区读取
            // 注意：这种方式可能无法获取到内容，因为响应可能已经被提交
            log.warn("响应未被包装，可能无法获取响应内容");
            return "";
        } catch (Exception e) {
            log.error("提取响应内容失败：{}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * 158→     * 解析响应内容
     * 159→     * @param responseBody 响应 JSON 字符串
     * 160→     * @return 解析后的响应内容
     * 161→
     */
    private String parseResponseContent(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return "";
        }

        try {
            // 尝试解析 JSON 响应
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {
            });

            // 根据不同的响应格式提取内容
            if (responseMap.containsKey("data")) {
                Object data = responseMap.get("data");
                if (data instanceof Map) {
                    Map<?, ?> dataMap = (Map<?, ?>) data;
                    if (dataMap.containsKey("content")) {
                        return dataMap.get("content").toString();
                    }
                    if (dataMap.containsKey("text")) {
                        return dataMap.get("text").toString();
                    }
                }
                return data != null ? data.toString() : "";
            }

            if (responseMap.containsKey("content")) {
                return responseMap.get("content").toString();
            }

            if (responseMap.containsKey("text")) {
                return responseMap.get("text").toString();
            }

            if (responseMap.containsKey("message")) {
                return responseMap.get("message").toString();
            }

            if (responseMap.containsKey("answer")) {
                return responseMap.get("answer").toString();
            }

            // 如果是字符串直接返回
            return responseBody;
        } catch (Exception e) {
            log.warn("解析响应内容失败，使用原始响应：{}", e.getMessage());
            return responseBody;
        }
    }
}