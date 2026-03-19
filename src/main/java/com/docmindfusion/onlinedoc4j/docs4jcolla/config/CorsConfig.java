package com.docmindfusion.onlinedoc4j.docs4jcolla.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 跨域配置
 * 允许来自前端和Collabora的跨域请求
 * CORS来源从环境变量读取
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    @Value("${app.cors.origins:http://localhost:5231,http://localhost:5670,http://localhost:9980}")
    private String corsOrigins;
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 将逗号分隔的字符串转换为数组
        String[] allowedOrigins = Arrays.stream(corsOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        
        registry.addMapping("/**")
                // 允许的来源（从环境变量读取）
                .allowedOriginPatterns(allowedOrigins)
                // 允许的HTTP方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许的请求头
                .allowedHeaders("*")
                // 允许携带凭证（cookies、授权头等）
                .allowCredentials(true)
                // 预检请求的缓存时间
                .maxAge(3600);
    }
}
