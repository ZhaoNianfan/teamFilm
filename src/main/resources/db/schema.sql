-- TeamFiles Database Schema
-- MySQL 8.4+
-- Character set: utf8mb4

CREATE DATABASE IF NOT EXISTS teamfiles
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE teamfiles;

-- ============================================================
-- 1. User table
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    username        VARCHAR(50)     NOT NULL COMMENT 'Username',
    password        VARCHAR(200)    NOT NULL COMMENT 'BCrypt encrypted password',
    nickname        VARCHAR(100)    DEFAULT NULL COMMENT 'Nickname',
    avatar          VARCHAR(500)    DEFAULT NULL COMMENT 'Avatar URL or path',
    role            VARCHAR(20)     NOT NULL DEFAULT 'FORMAL' COMMENT 'Role: ADMIN/FORMAL/GUEST',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '0=Disabled, 1=Enabled',
    first_login     TINYINT         NOT NULL DEFAULT 1 COMMENT '0=No, 1=Yes (must change password)',
    login_fail_count INT            DEFAULT 0 COMMENT 'Consecutive login failure count',
    locked_until    DATETIME        DEFAULT NULL COMMENT 'Account locked until this time',
    storage_quota   BIGINT          DEFAULT 5368709120 COMMENT 'Personal storage quota (bytes), default 5GB',
    remark          VARCHAR(500)    DEFAULT NULL COMMENT 'Remark',
    created_by      BIGINT          DEFAULT NULL COMMENT 'Creator user ID',
    last_login_time DATETIME        DEFAULT NULL COMMENT 'Last login time',
    last_login_ip   VARCHAR(50)     DEFAULT NULL COMMENT 'Last login IP',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT 'Logical delete: 0=Normal, 1=Deleted',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System user';

-- ============================================================
-- 2. Folder table (self-referencing hierarchy)
-- ============================================================
CREATE TABLE IF NOT EXISTS folder (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    folder_name     VARCHAR(200)    NOT NULL COMMENT 'Folder name',
    parent_id       BIGINT          DEFAULT 0 COMMENT 'Parent folder ID, 0=root',
    storage_space   VARCHAR(20)     NOT NULL COMMENT 'PERSONAL/TEAM',
    owner_user_id   BIGINT          NOT NULL COMMENT 'Owner user ID',
    sort_order      INT             DEFAULT 0 COMMENT 'Sort order',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT 'Logical delete',
    INDEX idx_parent_id (parent_id),
    INDEX idx_owner_user (owner_user_id),
    INDEX idx_storage_space (storage_space)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Folder';

-- ============================================================
-- 3. File info table
-- ============================================================
CREATE TABLE IF NOT EXISTS file_info (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    file_name       VARCHAR(500)    NOT NULL COMMENT 'Stored file name (UUID)',
    original_name   VARCHAR(500)    NOT NULL COMMENT 'Original file name',
    file_path       VARCHAR(1000)   NOT NULL COMMENT 'Relative storage path',
    file_size       BIGINT          NOT NULL COMMENT 'File size (bytes)',
    file_type       VARCHAR(50)     NOT NULL COMMENT 'IMAGE/DOCUMENT/OTHER',
    mime_type       VARCHAR(100)    DEFAULT NULL COMMENT 'MIME type',
    file_extension  VARCHAR(20)     DEFAULT NULL COMMENT 'File extension',
    md5             VARCHAR(64)     DEFAULT NULL COMMENT 'MD5 checksum',
    storage_space   VARCHAR(20)     NOT NULL COMMENT 'PERSONAL/TEAM',
    folder_id       BIGINT          DEFAULT NULL COMMENT 'Folder ID',
    upload_user_id  BIGINT          NOT NULL COMMENT 'Uploader user ID',
    download_count  INT             DEFAULT 0 COMMENT 'Download count',
    preview_count   INT             DEFAULT 0 COMMENT 'Preview count',
    remark          VARCHAR(1000)   DEFAULT NULL COMMENT 'Remark',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT 'Logical delete',
    INDEX idx_upload_user (upload_user_id),
    INDEX idx_storage_space (storage_space),
    INDEX idx_folder_id (folder_id),
    INDEX idx_md5 (md5),
    INDEX idx_file_type (file_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='File information';

-- ============================================================
-- 4. File chunk upload table
-- ============================================================
CREATE TABLE IF NOT EXISTS file_chunk (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    upload_id       VARCHAR(64)     NOT NULL COMMENT 'Upload task unique ID',
    file_name       VARCHAR(500)    NOT NULL COMMENT 'Target file name',
    file_md5        VARCHAR(64)     DEFAULT NULL COMMENT 'Full file MD5',
    chunk_index     INT             NOT NULL COMMENT 'Chunk index (0-based)',
    chunk_count     INT             NOT NULL COMMENT 'Total chunk count',
    chunk_size      BIGINT          NOT NULL COMMENT 'Chunk size (bytes)',
    chunk_path      VARCHAR(1000)   DEFAULT NULL COMMENT 'Chunk temp storage path',
    status          TINYINT         DEFAULT 0 COMMENT '0=Not uploaded, 1=Uploaded',
    upload_user_id  BIGINT          NOT NULL COMMENT 'Uploader user ID',
    storage_space   VARCHAR(20)     DEFAULT NULL COMMENT 'Target storage space',
    folder_id       BIGINT          DEFAULT NULL COMMENT 'Target folder ID',
    expired_at      DATETIME        DEFAULT NULL COMMENT 'Expiration time (24h incomplete cleanup)',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    UNIQUE KEY uk_upload_chunk (upload_id, chunk_index),
    INDEX idx_upload_id (upload_id),
    INDEX idx_expired_at (expired_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='File chunk upload records';

-- ============================================================
-- 5. Tag table
-- ============================================================
CREATE TABLE IF NOT EXISTS tag (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    tag_name        VARCHAR(100)    NOT NULL COMMENT 'Tag name',
    color           VARCHAR(20)     DEFAULT '#409EFF' COMMENT 'Tag color',
    creator_user_id BIGINT          DEFAULT NULL COMMENT 'Creator user ID (weak binding)',
    sort_order      INT             DEFAULT 0 COMMENT 'Sort order',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT 'Soft delete (weak delete)',
    INDEX idx_tag_name (tag_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tag';

-- ============================================================
-- 6. User tag visibility table
-- ============================================================
CREATE TABLE IF NOT EXISTS user_tag_visible (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    user_id         BIGINT          NOT NULL COMMENT 'User ID',
    tag_id          BIGINT          NOT NULL COMMENT 'Tag ID',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    UNIQUE KEY uk_user_tag (user_id, tag_id),
    INDEX idx_user_id (user_id),
    INDEX idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User tag visibility';

-- ============================================================
-- 7. File-Tag association table
-- ============================================================
CREATE TABLE IF NOT EXISTS file_tag (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    file_id         BIGINT          NOT NULL COMMENT 'File ID',
    tag_id          BIGINT          NOT NULL COMMENT 'Tag ID',
    created_by      BIGINT          DEFAULT NULL COMMENT 'Created by user ID',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    UNIQUE KEY uk_file_tag (file_id, tag_id),
    INDEX idx_file_id (file_id),
    INDEX idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='File-Tag association';

-- ============================================================
-- 8. Tag group table (user-private)
-- ============================================================
CREATE TABLE IF NOT EXISTS tag_group (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    group_name      VARCHAR(100)    NOT NULL COMMENT 'Group name',
    owner_user_id   BIGINT          NOT NULL COMMENT 'Owner user ID',
    color           VARCHAR(20)     DEFAULT '#409EFF' COMMENT 'Group color',
    sort_order      INT             DEFAULT 0 COMMENT 'Sort order',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT 'Logical delete',
    INDEX idx_owner_user (owner_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tag group';

-- ============================================================
-- 9. Tag group item table
-- ============================================================
CREATE TABLE IF NOT EXISTS tag_group_item (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    group_id        BIGINT          NOT NULL COMMENT 'Tag group ID',
    tag_id          BIGINT          NOT NULL COMMENT 'Tag ID',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    UNIQUE KEY uk_group_tag (group_id, tag_id),
    INDEX idx_group_id (group_id),
    INDEX idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Tag group item';

-- ============================================================
-- 10. Recycle bin table
-- ============================================================
CREATE TABLE IF NOT EXISTS recycle_bin (
    id                  BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    original_type       VARCHAR(20)     NOT NULL COMMENT 'FILE/FOLDER',
    original_id         BIGINT          NOT NULL COMMENT 'Original file/folder ID',
    file_name           VARCHAR(500)    DEFAULT NULL COMMENT 'File/folder name',
    file_path           VARCHAR(1000)   DEFAULT NULL COMMENT 'Original file path',
    file_size           BIGINT          DEFAULT NULL COMMENT 'File size',
    storage_space       VARCHAR(20)     NOT NULL COMMENT 'PERSONAL/TEAM',
    original_parent_id  BIGINT          DEFAULT NULL COMMENT 'Original parent folder ID (for restore)',
    deleted_by          BIGINT          NOT NULL COMMENT 'Deleted by user ID',
    deleted_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Deleted time',
    expire_at           DATETIME        NOT NULL COMMENT 'Expiration time (deleted_at + 30 days)',
    INDEX idx_deleted_by (deleted_by),
    INDEX idx_expire_at (expire_at),
    INDEX idx_storage_space (storage_space)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Recycle bin';

-- ============================================================
-- 11. Search history table
-- ============================================================
CREATE TABLE IF NOT EXISTS search_history (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    user_id         BIGINT          NOT NULL COMMENT 'User ID',
    keyword         VARCHAR(500)    NOT NULL COMMENT 'Search keyword',
    search_type     VARCHAR(50)     DEFAULT NULL COMMENT 'FILE/TAG/CONTENT',
    result_count    INT             DEFAULT NULL COMMENT 'Result count',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    INDEX idx_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Search history';

-- ============================================================
-- 12. Search template table
-- ============================================================
CREATE TABLE IF NOT EXISTS search_template (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    user_id         BIGINT          NOT NULL COMMENT 'User ID',
    template_name   VARCHAR(200)    NOT NULL COMMENT 'Template name',
    search_condition JSON           NOT NULL COMMENT 'Search condition JSON',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Search template';

-- ============================================================
-- 13. AI API config table
-- ============================================================
CREATE TABLE IF NOT EXISTS ai_api_config (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    user_id         BIGINT          DEFAULT NULL COMMENT 'User ID (NULL for system-wide config)',
    is_system       TINYINT         DEFAULT 0 COMMENT '0=User config, 1=Admin global config',
    api_type        VARCHAR(50)     NOT NULL COMMENT 'API type: OPENAI/CLAUDE/QWEN/GLM/LOCAL',
    api_key         VARCHAR(500)    NOT NULL COMMENT 'AES encrypted API key',
    api_base_url    VARCHAR(500)    DEFAULT NULL COMMENT 'API base URL',
    model_name      VARCHAR(100)    NOT NULL COMMENT 'Model name',
    is_active       TINYINT         DEFAULT 1 COMMENT '0=Disabled, 1=Enabled',
    tested          TINYINT         DEFAULT 0 COMMENT '0=Not tested, 1=Test passed',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated time',
    INDEX idx_user_id (user_id),
    INDEX idx_is_system (is_system)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI API configuration';

-- ============================================================
-- 14. Operation log table
-- ============================================================
CREATE TABLE IF NOT EXISTS operation_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'Primary key',
    user_id         BIGINT          DEFAULT NULL COMMENT 'Operator user ID',
    username        VARCHAR(50)     DEFAULT NULL COMMENT 'Operator username',
    module          VARCHAR(50)     NOT NULL COMMENT 'Module: FILE/USER/TAG/SYSTEM',
    operation       VARCHAR(100)    NOT NULL COMMENT 'Operation description',
    method          VARCHAR(200)    DEFAULT NULL COMMENT 'Request method',
    request_params  TEXT            DEFAULT NULL COMMENT 'Request parameters',
    ip              VARCHAR(50)     DEFAULT NULL COMMENT 'Client IP',
    execution_time  BIGINT          DEFAULT NULL COMMENT 'Execution time (ms)',
    result          TINYINT         DEFAULT 1 COMMENT '0=Failed, 1=Succeeded',
    error_msg       VARCHAR(2000)   DEFAULT NULL COMMENT 'Error message',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Created time',
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Operation log';

-- ============================================================
-- Initial data: Admin user (password: admin123, BCrypt encoded)
-- ============================================================
INSERT INTO sys_user (username, password, nickname, role, status, first_login, remark, created_by, created_at, updated_at)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', 'System Admin', 'ADMIN', 1, 1, 'Built-in administrator account', NULL, NOW(), NOW());
