-- MySQL 数据库初始化脚本 - 分步执行版本
-- 执行方式：逐条复制执行或使用 mysql 命令行

-- 第1步：创建数据库
CREATE DATABASE IF NOT EXISTS docs_online CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 第2步：选择数据库
USE docs_online;

-- 第3步：创建文档表
CREATE TABLE IF NOT EXISTS wopi_document (
    id CHAR(36) NOT NULL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    document_type VARCHAR(20) NOT NULL DEFAULT 'word',
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 1,
    owner VARCHAR(100) NOT NULL DEFAULT 'admin',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_locked TINYINT(1) NOT NULL DEFAULT 0,
    lock_token VARCHAR(100) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 第4步：创建用户表（可选）
CREATE TABLE IF NOT EXISTS wopi_user (
    id CHAR(36) NOT NULL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login DATETIME NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 第5步：插入测试数据（可选）
INSERT INTO wopi_document (id, filename, document_type, file_path, file_size, version, owner)
VALUES 
('550e8400-e29b-41d4-a716-446655440000', 'test.docx', 'word', 'documents/test.docx', 0, 1, 'admin'),
('550e8400-e29b-41d4-a716-446655440001', 'data.xlsx', 'excel', 'documents/data.xlsx', 0, 1, 'admin'),
('550e8400-e29b-41d4-a716-446655440002', 'records.csv', 'excel', 'documents/records.csv', 0, 1, 'admin')
ON DUPLICATE KEY UPDATE filename=filename;

-- 验证结果
SELECT 'Database and tables created successfully!' AS status;
SHOW TABLES;