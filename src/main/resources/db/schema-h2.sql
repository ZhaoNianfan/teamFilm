-- TeamFiles H2 Database Schema (for deployment)
-- H2 MySQL compatibility mode

-- User
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL,
    password        VARCHAR(200)    NOT NULL,
    nickname        VARCHAR(100)    DEFAULT NULL,
    avatar          VARCHAR(500)    DEFAULT NULL,
    role            VARCHAR(20)     NOT NULL DEFAULT 'FORMAL',
    status          TINYINT         NOT NULL DEFAULT 1,
    first_login     TINYINT         NOT NULL DEFAULT 1,
    login_fail_count INT            DEFAULT 0,
    locked_until    TIMESTAMP       DEFAULT NULL,
    storage_quota   BIGINT          DEFAULT 5368709120,
    remark          VARCHAR(500)    DEFAULT NULL,
    created_by      BIGINT          DEFAULT NULL,
    last_login_time TIMESTAMP       DEFAULT NULL,
    last_login_ip   VARCHAR(50)     DEFAULT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0,
    UNIQUE (username)
);

-- Folder
CREATE TABLE IF NOT EXISTS folder (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    folder_name     VARCHAR(200)    NOT NULL,
    parent_id       BIGINT          DEFAULT 0,
    storage_space   VARCHAR(20)     NOT NULL,
    owner_user_id   BIGINT          NOT NULL,
    sort_order      INT             DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0
);

-- File info
CREATE TABLE IF NOT EXISTS file_info (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    file_name       VARCHAR(500)    NOT NULL,
    original_name   VARCHAR(500)    NOT NULL,
    file_path       VARCHAR(1000)   NOT NULL,
    file_size       BIGINT          NOT NULL,
    file_type       VARCHAR(50)     NOT NULL,
    mime_type       VARCHAR(100)    DEFAULT NULL,
    file_extension  VARCHAR(20)     DEFAULT NULL,
    md5             VARCHAR(64)     DEFAULT NULL,
    storage_space   VARCHAR(20)     NOT NULL,
    folder_id       BIGINT          DEFAULT NULL,
    upload_user_id  BIGINT          NOT NULL,
    download_count  INT             DEFAULT 0,
    preview_count   INT             DEFAULT 0,
    remark          VARCHAR(1000)   DEFAULT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0
);

-- File chunk
CREATE TABLE IF NOT EXISTS file_chunk (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    upload_id       VARCHAR(64)     NOT NULL,
    file_name       VARCHAR(500)    NOT NULL,
    file_md5        VARCHAR(64)     DEFAULT NULL,
    chunk_index     INT             NOT NULL,
    chunk_count     INT             NOT NULL,
    chunk_size      BIGINT          NOT NULL,
    chunk_path      VARCHAR(1000)   DEFAULT NULL,
    status          TINYINT         DEFAULT 0,
    upload_user_id  BIGINT          NOT NULL,
    storage_space   VARCHAR(20)     DEFAULT NULL,
    folder_id       BIGINT          DEFAULT NULL,
    expired_at      TIMESTAMP       DEFAULT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (upload_id, chunk_index)
);

-- Tag
CREATE TABLE IF NOT EXISTS tag (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tag_name        VARCHAR(100)    NOT NULL,
    color           VARCHAR(20)     DEFAULT '#409EFF',
    creator_user_id BIGINT          DEFAULT NULL,
    sort_order      INT             DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0
);

-- User tag visible
CREATE TABLE IF NOT EXISTS user_tag_visible (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    tag_id          BIGINT          NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, tag_id)
);

-- File tag
CREATE TABLE IF NOT EXISTS file_tag (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    file_id         BIGINT          NOT NULL,
    tag_id          BIGINT          NOT NULL,
    created_by      BIGINT          DEFAULT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (file_id, tag_id)
);

-- Tag group
CREATE TABLE IF NOT EXISTS tag_group (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    group_name      VARCHAR(100)    NOT NULL,
    owner_user_id   BIGINT          NOT NULL,
    color           VARCHAR(20)     DEFAULT '#409EFF',
    sort_order      INT             DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         TINYINT         NOT NULL DEFAULT 0
);

-- Tag group item
CREATE TABLE IF NOT EXISTS tag_group_item (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    group_id        BIGINT          NOT NULL,
    tag_id          BIGINT          NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (group_id, tag_id)
);

-- Recycle bin
CREATE TABLE IF NOT EXISTS recycle_bin (
    id                  BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    original_type       VARCHAR(20)     NOT NULL,
    original_id         BIGINT          NOT NULL,
    file_name           VARCHAR(500)    DEFAULT NULL,
    file_path           VARCHAR(1000)   DEFAULT NULL,
    file_size           BIGINT          DEFAULT NULL,
    storage_space       VARCHAR(20)     NOT NULL,
    original_parent_id  BIGINT          DEFAULT NULL,
    deleted_by          BIGINT          NOT NULL,
    deleted_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expire_at           TIMESTAMP       NOT NULL
);

-- Search history
CREATE TABLE IF NOT EXISTS search_history (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    keyword         VARCHAR(500)    NOT NULL,
    search_type     VARCHAR(50)     DEFAULT NULL,
    result_count    INT             DEFAULT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Search template
CREATE TABLE IF NOT EXISTS search_template (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    template_name   VARCHAR(200)    NOT NULL,
    search_condition CLOB           NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- AI config
CREATE TABLE IF NOT EXISTS ai_api_config (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          DEFAULT NULL,
    is_system       TINYINT         DEFAULT 0,
    api_type        VARCHAR(50)     NOT NULL,
    api_key         VARCHAR(500)    NOT NULL,
    api_base_url    VARCHAR(500)    DEFAULT NULL,
    model_name      VARCHAR(100)    NOT NULL,
    is_active       TINYINT         DEFAULT 1,
    tested          TINYINT         DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Operation log
CREATE TABLE IF NOT EXISTS operation_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          DEFAULT NULL,
    username        VARCHAR(50)     DEFAULT NULL,
    module          VARCHAR(50)     NOT NULL,
    operation       VARCHAR(100)    NOT NULL,
    method          VARCHAR(200)    DEFAULT NULL,
    request_params  CLOB            DEFAULT NULL,
    ip              VARCHAR(50)     DEFAULT NULL,
    execution_time  BIGINT          DEFAULT NULL,
    result          TINYINT         DEFAULT 1,
    error_msg       VARCHAR(2000)   DEFAULT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);
