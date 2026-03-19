package com.docmindfusion.onlinedoc4j.docs4jcolla.controller;

import com.docmindfusion.onlinedoc4j.docs4jcolla.entity.WopiDocument;
import com.docmindfusion.onlinedoc4j.docs4jcolla.mapper.WopiDocumentMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * WOPI协议控制器
 * 实现WOPI主机的核心接口，供Collabora Online调用
 *
 * WOPI协议端点：
 * - GET /wopi/files/{fileId} - 获取文件元数据
 * - GET /wopi/files/{fileId}/contents - 获取文件内容
 * - POST /wopi/files/{fileId}/contents - 保存文件内容
 */
@RestController
@RequestMapping("/wopi")
@CrossOrigin(origins = {"${app.cors.origins}"})
public class WopiController {
    
    @Autowired
    private WopiDocumentMapper documentMapper;
    
    @Value("${app.file.upload-dir:./docs}")
    private String uploadDir;
    
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
    
    /**
     * 验证访问令牌
     */
    private boolean verifyAccessToken(String token, String docId) {
        String expected = generateAccessToken(docId);
        return expected.equals(token);
    }
    
    /**
     * 处理WOPI接口的OPTIONS预检请求
     */
    @RequestMapping(value = "/files/**", method = RequestMethod.OPTIONS)
    public ResponseEntity<Void> handleOptions() {
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/files/{fileId}")
    public ResponseEntity<Map<String, Object>> getFileInfo(
            @PathVariable String fileId,
            HttpServletRequest request) {
        
        // 验证access_token（使用SHA256验证）
        String accessToken = request.getParameter("access_token");
        if (accessToken == null || accessToken.isEmpty() || !verifyAccessToken(accessToken, fileId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        WopiDocument doc = documentMapper.findById(fileId);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        
        // 构建WOPI文件信息响应
        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("BaseFileName", doc.getFilename());
        fileInfo.put("Size", doc.getFileSize() != null ? doc.getFileSize() : 0);
        fileInfo.put("OwnerId", doc.getOwner() != null ? doc.getOwner() : defaultUser);
        fileInfo.put("UserId", defaultUser);
        fileInfo.put("UserCanWrite", true);
        fileInfo.put("UserCanRead", true);
        fileInfo.put("Version", doc.getVersion() != null ? doc.getVersion().toString() : "1");
        fileInfo.put("SupportsExtendedLockLength", true);
        fileInfo.put("SupportsCobalt", true);
        fileInfo.put("SupportsDeleteFile", true);
        fileInfo.put("SupportsRename", true);
        fileInfo.put("SupportsUpdate", true);
        // WOPI客户端显示用
        fileInfo.put("BreadcrumbDocName", doc.getFilename());
        fileInfo.put("HidePrint", false);
        fileInfo.put("HideSave", false);
        fileInfo.put("DisableExport", false);
        // WOPI协议要求ISO 8601格式的时间
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        LocalDateTime lastModified = doc.getUpdatedAt() != null ? doc.getUpdatedAt() : doc.getCreatedAt();
        fileInfo.put("LastModifiedTime", lastModified.format(formatter) + "Z");
        
        // 添加WOPI所需的响应头
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-WOPI-ItemVersion", doc.getVersion() != null ? doc.getVersion().toString() : "1");
        
        return ResponseEntity.ok().headers(headers).body(fileInfo);
    }
    
    /**
     * WOPI端点：获取文件内容
     * Collabora通过此接口读取文件内容进行编辑
     * 
     * @param fileId 文件ID
     * @param request HTTP请求（包含access_token参数）
     * @return 文件二进制内容
     */
    @GetMapping("/files/{fileId}/contents")
    public ResponseEntity<Resource> getFileContents(
            @PathVariable String fileId,
            HttpServletRequest request) {
        
        // 验证access_token
        String accessToken = request.getParameter("access_token");
        if (accessToken == null || accessToken.isEmpty() || !verifyAccessToken(accessToken, fileId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            WopiDocument doc = documentMapper.findById(fileId);
            if (doc == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 从磁盘读取文件
            Path filePath = Paths.get(uploadDir).resolve(doc.getFilePath());
            byte[] content = Files.readAllBytes(filePath);
            
            ByteArrayResource resource = new ByteArrayResource(content);
            
            // 根据文档类型确定Content-Type
            String contentType = getContentType(doc.getFilename());
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + doc.getFilename() + "\"");
            headers.add("X-WOPI-ItemVersion", doc.getVersion() != null ? doc.getVersion().toString() : "1");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(content.length)
                    .body(resource);
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * WOPI端点：保存文件内容
     * Collabora编辑完成后通过此接口保存文件
     * 
     * @param fileId 文件ID
     * @param request HTTP请求（包含access_token参数）
     * @return 保存结果
     */
    @PostMapping("/files/{fileId}/contents")
    public ResponseEntity<Map<String, Object>> saveFileContents(
            @PathVariable String fileId,
            HttpServletRequest request) {
        
        // 验证access_token
        String accessToken = request.getParameter("access_token");
        if (accessToken == null || accessToken.isEmpty() || !verifyAccessToken(accessToken, fileId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            WopiDocument doc = documentMapper.findById(fileId);
            if (doc == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 读取请求体中的文件内容
            byte[] content = request.getInputStream().readAllBytes();
            
            // 保存到磁盘
            Path filePath = Paths.get(uploadDir).resolve(doc.getFilePath());
            Files.write(filePath, content);
            
            // 更新数据库记录
            doc.setFileSize((long) content.length);
            doc.setUpdatedAt(LocalDateTime.now());
            documentMapper.incrementVersion(fileId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "文件保存成功");
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-WOPI-ItemVersion", doc.getVersion() != null ? doc.getVersion().toString() : "1");
            
            return ResponseEntity.ok().headers(headers).body(result);
            
        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "文件保存失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 根据文件扩展名获取Content-Type
     */
    private String getContentType(String filename) {
        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lowerName.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if (lowerName.endsWith(".pptx")) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        } else if (lowerName.endsWith(".odt")) {
            return "application/vnd.oasis.opendocument.text";
        } else if (lowerName.endsWith(".ods")) {
            return "application/vnd.oasis.opendocument.spreadsheet";
        } else if (lowerName.endsWith(".odp")) {
            return "application/vnd.oasis.opendocument.presentation";
        } else if (lowerName.endsWith(".txt")) {
            return "text/plain";
        }
        return "application/octet-stream";
    }
}
