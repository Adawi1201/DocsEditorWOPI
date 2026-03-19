package com.docmindfusion.onlinedoc4j.docs4jcolla.entity;

import java.time.LocalDateTime;

/**
 * WOPI文档实体类
 * 对应数据库表 wopi_document
 * 使用MyBatis进行数据库操作，不需要JPA注解
 */
public class WopiDocument {
    
    private String id;
    private String filename;
    private String documentType;
    private String filePath;
    private Long fileSize;
    private Integer version;
    private String owner;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isLocked;
    private String lockToken;
    
    // Constructors
    public WopiDocument() {
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public String getDocumentType() {
        return documentType;
    }
    
    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    public String getOwner() {
        return owner;
    }
    
    public void setOwner(String owner) {
        this.owner = owner;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Boolean getIsLocked() {
        return isLocked;
    }
    
    public void setIsLocked(Boolean isLocked) {
        this.isLocked = isLocked;
    }
    
    public String getLockToken() {
        return lockToken;
    }
    
    public void setLockToken(String lockToken) {
        this.lockToken = lockToken;
    }
}