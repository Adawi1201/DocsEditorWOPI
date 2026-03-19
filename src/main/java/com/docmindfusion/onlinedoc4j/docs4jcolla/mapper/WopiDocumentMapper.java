package com.docmindfusion.onlinedoc4j.docs4jcolla.mapper;

import com.docmindfusion.onlinedoc4j.docs4jcolla.entity.WopiDocument;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * WOPI文档MyBatis Mapper接口
 */
@Mapper
public interface WopiDocumentMapper {
    
    /**
     * 查询所有文档
     */
    @Select("SELECT * FROM wopi_document ORDER BY created_at DESC")
    List<WopiDocument> findAll();
    
    /**
     * 根据ID查询文档
     */
    @Select("SELECT * FROM wopi_document WHERE id = #{id}")
    WopiDocument findById(String id);
    
    /**
     * 根据文件名查询文档
     */
    @Select("SELECT * FROM wopi_document WHERE filename = #{filename}")
    WopiDocument findByFilename(String filename);
    
    /**
     * 根据所有者查询文档
     */
    @Select("SELECT * FROM wopi_document WHERE owner = #{owner} ORDER BY created_at DESC")
    List<WopiDocument> findByOwner(String owner);
    
    /**
     * 插入文档
     */
    @Insert("INSERT INTO wopi_document (id, filename, document_type, file_path, file_size, version, owner, created_at, updated_at, is_locked, lock_token) " +
            "VALUES (#{id}, #{filename}, #{documentType}, #{filePath}, #{fileSize}, #{version}, #{owner}, #{createdAt}, #{updatedAt}, #{isLocked}, #{lockToken})")
    int insert(WopiDocument document);
    
    /**
     * 更新文档
     */
    @Update("UPDATE wopi_document SET filename = #{filename}, document_type = #{documentType}, file_path = #{filePath}, " +
            "file_size = #{fileSize}, version = #{version}, owner = #{owner}, updated_at = #{updatedAt}, " +
            "is_locked = #{isLocked}, lock_token = #{lockToken} WHERE id = #{id}")
    int update(WopiDocument document);
    
    /**
     * 删除文档
     */
    @Delete("DELETE FROM wopi_document WHERE id = #{id}")
    int deleteById(String id);
    
    /**
     * 检查文档是否存在
     */
    @Select("SELECT COUNT(*) FROM wopi_document WHERE filename = #{filename}")
    int countByFilename(String filename);
    
    /**
     * 更新文件大小
     */
    @Update("UPDATE wopi_document SET file_size = #{fileSize}, updated_at = NOW() WHERE id = #{id}")
    int updateFileSize(@Param("id") String id, @Param("fileSize") Long fileSize);
    
    /**
     * 更新版本
     */
    @Update("UPDATE wopi_document SET version = version + 1, updated_at = NOW() WHERE id = #{id}")
    int incrementVersion(String id);
    
    /**
     * 锁定文档
     */
    @Update("UPDATE wopi_document SET is_locked = 1, lock_token = #{lockToken}, updated_at = NOW() WHERE id = #{id}")
    int lockDocument(@Param("id") String id, @Param("lockToken") String lockToken);
    
    /**
     * 解锁文档
     */
    @Update("UPDATE wopi_document SET is_locked = 0, lock_token = NULL, updated_at = NOW() WHERE id = #{id}")
    int unlockDocument(String id);
}