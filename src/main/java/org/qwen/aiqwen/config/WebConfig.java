
package org.qwen.aiqwen.config;

import org.qwen.aiqwen.filter.ChatRecordInterceptor;
import org.qwen.aiqwen.filter.RequestBodyCacheFilter;
import org.qwen.aiqwen.filter.ResponseCacheFilter;
import org.qwen.aiqwen.interceptor.RequestLoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RequestLoggingInterceptor requestLoggingInterceptor;

    @Autowired
    private ResponseCacheFilter responseCacheFilter;

    @Autowired
    private ChatRecordInterceptor chatRecordInterceptor;

    @Autowired
    private RequestBodyCacheFilter requestBodyCacheFilter;

    /**
     * 31→     * 注册请求体缓存过滤器
     * 32→
     */
    @Bean
    public FilterRegistrationBean<RequestBodyCacheFilter> requestBodyCacheFilterRegistration() {
        FilterRegistrationBean<RequestBodyCacheFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(requestBodyCacheFilter);

        registration.addUrlPatterns("/api/chat/*", "/api/rag/*");
        registration.setName("requestBodyCacheFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 日志拦截器
        registry.addInterceptor(requestLoggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/error");


        // 聊天记录拦截器 - 保存聊天记录到 Redis 和数据库
        registry.addInterceptor(chatRecordInterceptor)
                .addPathPatterns("/api/chat/**", "/api/rag/**")
                .order(2);
    }

    /**
     * 47→     * 注册响应缓存过滤器
     * 48→
     */
    @Bean
    public FilterRegistrationBean<ResponseCacheFilter> responseCacheFilterRegistration() {
        FilterRegistrationBean<ResponseCacheFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(responseCacheFilter);
        registration.addUrlPatterns("/api/chat/*", "/api/rag/*");
        registration.setName("responseCacheFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }
}