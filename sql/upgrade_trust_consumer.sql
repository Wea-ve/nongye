-- =============================================
-- 信任者消费助手升级 DDL，可重复执行
-- 数据库：nongyeplus
-- 说明：脚本幂等（先查 information_schema 再决定是否 CREATE/ALTER）
-- =============================================

USE nongyeplus;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 新增 6 张表
-- ----------------------------

-- 1. consumer_account (信任者账户)
SET @has_table := (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'consumer_account');
SET @ddl := IF(@has_table = 0,
    'CREATE TABLE consumer_account (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        nickname VARCHAR(50) NULL,
        phone VARCHAR(20) NOT NULL,
        open_id VARCHAR(64) NULL,
        trust_level VARCHAR(20) DEFAULT ''FOLLOWER'' NULL,
        invite_code VARCHAR(32) NULL,
        inviter_type VARCHAR(20) NULL,
        inviter_id BIGINT NULL,
        profile_json VARCHAR(1000) NULL,
        status TINYINT DEFAULT 1 NULL,
        last_active_time DATETIME NULL,
        create_time DATETIME DEFAULT CURRENT_TIMESTAMP NULL,
        UNIQUE KEY uk_consumer_phone (phone),
        INDEX idx_consumer_inviter (inviter_type, inviter_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''信任者账户表''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. consumer_follow (关注关系)
SET @has_table := (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'consumer_follow');
SET @ddl := IF(@has_table = 0,
    'CREATE TABLE consumer_follow (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        consumer_id BIGINT NOT NULL,
        target_type VARCHAR(20) NOT NULL,
        target_id BIGINT NOT NULL,
        notify_enabled TINYINT DEFAULT 1 NULL,
        create_time DATETIME DEFAULT CURRENT_TIMESTAMP NULL,
        UNIQUE KEY uk_follow (consumer_id, target_type, target_id),
        INDEX idx_follow_target (target_type, target_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''关注关系表''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. consumer_subscription (常购订阅)
SET @has_table := (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'consumer_subscription');
SET @ddl := IF(@has_table = 0,
    'CREATE TABLE consumer_subscription (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        consumer_id BIGINT NOT NULL,
        batch_id BIGINT NULL,
        crop_name VARCHAR(50) NOT NULL,
        qty DECIMAL(10,2) NULL,
        unit VARCHAR(10) DEFAULT ''斤'' NULL,
        cycle_days INT DEFAULT 7 NULL,
        next_date DATE NULL,
        status VARCHAR(20) DEFAULT ''ACTIVE'' NULL,
        create_time DATETIME DEFAULT CURRENT_TIMESTAMP NULL,
        INDEX idx_sub_consumer (consumer_id, status)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''常购订阅表''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. consumer_event (行为事件流)
SET @has_table := (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'consumer_event');
SET @ddl := IF(@has_table = 0,
    'CREATE TABLE consumer_event (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        consumer_id BIGINT NULL,
        event_type VARCHAR(30) NULL,
        batch_id BIGINT NULL,
        payload VARCHAR(1000) NULL,
        create_time DATETIME DEFAULT CURRENT_TIMESTAMP NULL,
        INDEX idx_event_consumer (consumer_id, create_time),
        INDEX idx_event_type (event_type, create_time)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''消费者行为事件表''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. trust_promise (信任承诺)
SET @has_table := (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'trust_promise');
SET @ddl := IF(@has_table = 0,
    'CREATE TABLE trust_promise (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        batch_id BIGINT NOT NULL,
        farmer_id BIGINT NULL,
        promise_type VARCHAR(30) NULL,
        promise_value VARCHAR(100) NULL,
        evidence_path VARCHAR(200) NULL,
        status VARCHAR(20) DEFAULT ''ACTIVE'' NULL,
        create_time DATETIME DEFAULT CURRENT_TIMESTAMP NULL,
        INDEX idx_promise_batch (batch_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''批次信任承诺表''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6. fulfillment_record (履约记录)
SET @has_table := (SELECT COUNT(*) FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fulfillment_record');
SET @ddl := IF(@has_table = 0,
    'CREATE TABLE fulfillment_record (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        batch_id BIGINT NULL,
        dealer_id BIGINT NULL,
        consumer_id BIGINT NULL,
        ship_time DATETIME NULL,
        arrive_time DATETIME NULL,
        quality_flag VARCHAR(20) DEFAULT ''OK'' NULL,
        aftersale_hours DECIMAL(6,2) NULL,
        create_time DATETIME DEFAULT CURRENT_TIMESTAMP NULL,
        INDEX idx_fulfill_batch (batch_id),
        INDEX idx_fulfill_consumer (consumer_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''履约记录表''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 4 个 ALTER（只在列不存在时执行）
-- ----------------------------

-- A. complaint ADD consumer_id BIGINT, sla_hours INT DEFAULT 24, first_reply_time DATETIME, overdue TINYINT DEFAULT 0
SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'complaint' AND COLUMN_NAME = 'consumer_id');
SET @ddl := IF(@has_col = 0,
    'ALTER TABLE complaint ADD COLUMN consumer_id BIGINT NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'complaint' AND COLUMN_NAME = 'sla_hours');
SET @ddl := IF(@has_col = 0,
    'ALTER TABLE complaint ADD COLUMN sla_hours INT DEFAULT 24 NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'complaint' AND COLUMN_NAME = 'first_reply_time');
SET @ddl := IF(@has_col = 0,
    'ALTER TABLE complaint ADD COLUMN first_reply_time DATETIME NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'complaint' AND COLUMN_NAME = 'overdue');
SET @ddl := IF(@has_col = 0,
    'ALTER TABLE complaint ADD COLUMN overdue TINYINT DEFAULT 0 NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- B. scan_log ADD consumer_id BIGINT, scene VARCHAR(20) DEFAULT 'PACKAGE'
SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scan_log' AND COLUMN_NAME = 'consumer_id');
SET @ddl := IF(@has_col = 0,
    'ALTER TABLE scan_log ADD COLUMN consumer_id BIGINT NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scan_log' AND COLUMN_NAME = 'scene');
SET @ddl := IF(@has_col = 0,
    'ALTER TABLE scan_log ADD COLUMN scene VARCHAR(20) DEFAULT ''PACKAGE'' NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- C. crop_batch ADD on_sale TINYINT DEFAULT 0, price DECIMAL(10,2), stock DECIMAL(10,2), subscribe_open TINYINT DEFAULT 0, promise_summary VARCHAR(200)
SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crop_batch' AND COLUMN_NAME = 'on_sale');
SET @ddl := IF(@has_col = 0,
    'ALTER TABLE crop_batch ADD COLUMN on_sale TINYINT DEFAULT 0 NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crop_batch' AND COLUMN_NAME = 'price');
SET @ddl := IF(@has_col = 0,
    'ALTER TABLE crop_batch ADD COLUMN price DECIMAL(10,2) NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crop_batch' AND COLUMN_NAME = 'stock');
SET @ddl := IF(@has_col = 0,
    'ALTER TABLE crop_batch ADD COLUMN stock DECIMAL(10,2) NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crop_batch' AND COLUMN_NAME = 'subscribe_open');
SET @ddl := IF(@has_col = 0,
    'ALTER TABLE crop_batch ADD COLUMN subscribe_open TINYINT DEFAULT 0 NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crop_batch' AND COLUMN_NAME = 'promise_summary');
SET @ddl := IF(@has_col = 0,
    'ALTER TABLE crop_batch ADD COLUMN promise_summary VARCHAR(200) NULL',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
