package com.docmindfusion.onlinedoc4j.docs4jcolla.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * WOPI Token验证过滤器
 * 
 * Collabora通过WOPI协议调用后端时，会在URL参数中传递access_token。
 * 为了避免与Spring Security的Authorization头冲突，我们需要：
 * 1. 从URL参数中提取access_token进行验证
 * 2. 忽略HTTP头中的Authorization（因为Collabora可能也会发送）
 * 
 * 此过滤器在Spring Security之前执行，确保WOPI请求能够正确处理。
 */
@Component
@Order(1)
public class WopiTokenFilter implements Filter {
    
    // WOPI访问令牌密钥（从环境变量读取）
    @Value("${app.wopi.access-token-secret}")
    private String accessTokenSecret;
    
    // 默认用户名（从环境变量读取）
    @Value("${app.wopi.default-user:admin}")
    private String defaultUser;
    
    /**
     * 生成访问令牌（使用SHA256）
     * 格式: SHA256("{docId}:{user}:{secret}")
     */
    private String generateAccessToken(String docId) {
        try {
            String tokenStr = docId + ":" + defaultUser + ":" + accessTokenSecret;
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(tokenStr.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return docId;
        }
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestURI = httpRequest.getRequestURI();
        String accessToken = httpRequest.getParameter("access_token");
        
        // 调试日志（生产环境应关闭）
        System.out.println("=== WopiTokenFilter Debug ===");
        System.out.println("Request URI: " + requestURI);
        System.out.println("Access Token: " + accessToken);
        
        // 只对WOPI接口进行token验证
        if (requestURI.startsWith("/wopi/")) {
            // 如果没有token，返回401
            if (accessToken == null || accessToken.isEmpty()) {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"error\": \"Missing access_token\"}");
                return;
            }
            
            // 从URI中提取docId（格式: /wopi/files/{docId}）
            String docId = extractDocId(requestURI);
            System.out.println("Extracted docId: " + docId);
            
            if (docId != null) {
                String expectedToken = generateAccessToken(docId);
                System.out.println("Expected token: " + expectedToken);
                System.out.println("Tokens match: " + expectedToken.equals(accessToken));
                
                if (!expectedToken.equals(accessToken)) {
                    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    httpResponse.setContentType("application/json");
                    httpResponse.getWriter().write("{\"error\": \"Invalid access_token\"}");
                    return;
                }
            } else {
                System.out.println("docId extraction failed!");
            }
        }
        
        // 继续执行过滤器链
        chain.doFilter(request, response);
    }
    
    /**
     * 从请求URI中提取docId
     */
    private String extractDocId(String requestURI) {
        // 格式: /wopi/files/{docId} 或 /wopi/files/{docId}/contents
        String pattern = "/wopi/files/";
        if (requestURI.contains(pattern)) {
            String path = requestURI.substring(requestURI.indexOf(pattern) + pattern.length());
            int slashIndex = path.indexOf('/');
            if (slashIndex > 0) {
                return path.substring(0, slashIndex);
            }
            return path;
        }
        return null;
    }
}
