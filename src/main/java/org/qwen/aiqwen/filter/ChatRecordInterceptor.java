
package org.qwen.aiqwen.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.qwen.aiqwen.dto.ChatRequestDto;
import org.qwen.aiqwen.entity.ChatRecord;
import org.qwen.aiqwen.service.ChatRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.util.UUID;

@Slf4j
@Component
public class ChatRecordInterceptor implements HandlerInterceptor {

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
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
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

            if (requestBody != null && !requestBody.isEmpty()) {
                saveChatRecord(requestBody, sessionId, userId, role);
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
    private void saveChatRecord(String requestBody, String sessionId, String userId, String role) {
        try {
            // 尝试解析请求体为 ChatRequestDto
            ChatRequestDto requestDto = objectMapper.readValue(requestBody, ChatRequestDto.class);

            // 如果解析成功，使用解析后的数据
            if (requestDto != null) {
                requestDto.setSessionId(sessionId);
                requestDto.setUserId(userId);
                requestDto.setRole(role);

                // 注意：这里不保存 response，因为响应已经发送了
                // 可以考虑在 Service 层或通过其他方式获取响应
                log.info("准备保存聊天记录 - SessionId: {}, Message: {}",
                        sessionId, requestDto.getMessage());
            }
        } catch (Exception e) {
            log.error("解析请求体失败：{}", e.getMessage());
        }
    }
}