
package org.qwen.aiqwen.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class RequestBodyCacheFilter implements Filter, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws java.io.IOException, ServletException {

        if (request instanceof HttpServletRequest httpServletRequest) {
            // 缓存请求体
            byte[] body = readRequestBody(httpServletRequest);
            httpServletRequest.setAttribute("cachedRequestBody", new String(body, StandardCharsets.UTF_8));

            // 包装请求，使其可以重复读取
            CachedBodyHttpServletRequest wrappedRequest =
                    new CachedBodyHttpServletRequest(httpServletRequest, body);

            chain.doFilter(wrappedRequest, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    private byte[] readRequestBody(HttpServletRequest request) throws java.io.IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}