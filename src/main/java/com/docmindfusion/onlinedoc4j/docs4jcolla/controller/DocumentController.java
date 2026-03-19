package com.docmindfusion.onlinedoc4j.docs4jcolla.controller;

import com.docmindfusion.onlinedoc4j.docs4jcolla.entity.WopiDocument;
import com.docmindfusion.onlinedoc4j.docs4jcolla.mapper.WopiDocumentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 文档业务控制器
 * 提供REST API供前端调用
 * 
 * 接口：
 * - GET /api/docs - 获取文档列表
 * - POST /api/docs/upload - 上传新文档
 * - GET /api/docs/{id}/edit - 生成编辑URL
 * - DELETE /api/docs/{id} - 删除文档
 */
@RestController
@RequestMapping("/api/docs")
@CrossOrigin(origins = {"${app.cors.origins}"})
public class DocumentController {
    
    @Autowired
    private WopiDocumentMapper documentMapper;
    
    // 文件上传目录（从环境变量读取）
    @Value("${app.file.upload-dir:./docs}")
    private String uploadDir;
    
    // Collabora服务器地址（从环境变量读取）
    @Value("${app.collabora.host:http://localhost:9980}")
    private String collaboraHost;
    
    // Collabora编辑器的HTML路径（从环境变量读取）
    @Value("${app.collabora.html-path:/browser/4fd2181/cool.html}")
    private String collaboraHtmlPath;
    
    // 后端WOPI服务器地址（从环境变量读取）
    @Value("${app.collabora.wopi-host:http://host.docker.internal:8808}")
    private String wopiHost;
    
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
            return docId; // fallback
        }
    }
    
    /**
     * 获取文档列表
     */
    @GetMapping
    public ResponseEntity<List<WopiDocument>> listDocuments() {
        List<WopiDocument> documents = documentMapper.findAll();
        return ResponseEntity.ok(documents);
    }
    
    /**
     * 上传新文档
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "请选择要上传的文件");
            return ResponseEntity.badRequest().body(error);
        }
        
        try {
            // 确保上传目录存在
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // 生成唯一ID
            String id = UUID.randomUUID().toString();
            String originalFilename = file.getOriginalFilename();
            String uniqueFilename = id + "_" + originalFilename;
            Path filePath = uploadPath.resolve(uniqueFilename);
            
            // 保存文件到磁盘
            byte[] content = file.getBytes();
            Files.write(filePath, content);
            
            // 确定文档类型
            String documentType = getDocumentType(originalFilename);
            
            // 创建文档记录
            WopiDocument doc = new WopiDocument();
            doc.setId(id);
            doc.setFilename(originalFilename);
            doc.setDocumentType(documentType);
            doc.setFilePath(uniqueFilename);
            doc.setFileSize((long) content.length);
            doc.setVersion(1);
            doc.setOwner(defaultUser);
            doc.setCreatedAt(LocalDateTime.now());
            doc.setUpdatedAt(LocalDateTime.now());
            doc.setIsLocked(false);
            doc.setLockToken(null);
            
            documentMapper.insert(doc);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "文档上传成功");
            result.put("document", doc);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
            
        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "文档保存失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 生成文档编辑URL
     * 返回包含WOPI访问令牌的完整URL，前端用此URL嵌入iframe
     */
    @GetMapping("/{id}/edit")
    public ResponseEntity<Map<String, Object>> getEditUrl(@PathVariable String id) {
        WopiDocument doc = documentMapper.findById(id);
        
        if (doc == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "文档不存在");
            return ResponseEntity.notFound().build();
        }
        
        // 生成访问令牌
        String accessToken = generateAccessToken(id);
        
        // 构建WOPI URL（现代版本格式）
        // 格式: http://localhost:9980/browser/4fd2181/cool.html?WOPISrc=<encoded_wopi_url>&lang=zh-CN
        String wopiSrc = String.format("%s/wopi/files/%s?access_token=%s",
                wopiHost,
                id,
                java.net.URLEncoder.encode(accessToken, java.nio.charset.StandardCharsets.UTF_8));
        
        String wopiUrl = String.format("%s%s?WOPISrc=%s&lang=zh-CN&permission=edit",
                collaboraHost,
                collaboraHtmlPath,
                java.net.URLEncoder.encode(wopiSrc, java.nio.charset.StandardCharsets.UTF_8));
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("editUrl", wopiUrl);
        result.put("documentId", id);
        result.put("fileName", doc.getFilename());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 删除文档
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable String id) {
        WopiDocument doc = documentMapper.findById(id);
        
        if (doc == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "文档不存在");
            return ResponseEntity.notFound().build();
        }
        
        try {
            // 删除物理文件
            Path filePath = Paths.get(uploadDir).resolve(doc.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // 文件删除失败，但继续删除数据库记录
        }
        
        // 删除数据库记录
        documentMapper.deleteById(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "文档删除成功");
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取单个文档信息
     */
    @GetMapping("/{id}")
    public ResponseEntity<WopiDocument> getDocument(@PathVariable String id) {
        WopiDocument doc = documentMapper.findById(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(doc);
    }
    
    /**
     * 根据文件扩展名判断文档类型
     */
    private String getDocumentType(String filename) {
        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".docx") || lowerName.endsWith(".doc") || lowerName.endsWith(".odt")) {
            return "word";
        } else if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls") || lowerName.endsWith(".ods")) {
            return "excel";
        } else if (lowerName.endsWith(".pptx") || lowerName.endsWith(".ppt") || lowerName.endsWith(".odp")) {
            return "presentation";
        }
        return "word";
    }
}
