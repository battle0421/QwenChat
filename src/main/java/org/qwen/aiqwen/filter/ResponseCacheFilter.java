package org.qwen.aiqwen.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Slf4j
@Component
public class ResponseCacheFilter implements Filter, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpServletRequest &&
                response instanceof HttpServletResponse httpServletResponse) {

            String path = httpServletRequest.getRequestURI();

            if (path.contains("/send/stream")) {
                log.debug("流式接口，跳过响应缓存包装：{}", path);
                chain.doFilter(request, response);
                return;
            }

            if (path.contains("/api/chat/") || path.contains("/api/rag/")) {
                log.debug("包装响应：{}", path);

                ContentCachingResponseWrapper wrappedResponse =
                        new ContentCachingResponseWrapper(httpServletResponse);

                try {
                    chain.doFilter(request, wrappedResponse);
                } finally {
                    byte[] content = wrappedResponse.getContentAsByteArray();
                    if (content != null && content.length > 0) {
                        String contentStr = new String(content, java.nio.charset.StandardCharsets.UTF_8);
                        httpServletRequest.setAttribute("cachedResponseBody", contentStr);
                        log.debug("响应内容已缓存，长度：{}", contentStr.length());
                    }

                    wrappedResponse.copyBodyToResponse();
                }
            } else {
                chain.doFilter(request, response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
