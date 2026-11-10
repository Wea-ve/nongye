-- =============================================
-- AI 可核验升级：知识来源/版本 + 投诉查询码
-- 数据库：nongyeplus
-- 说明：脚本可重复执行（先查 information_schema 再决定是否 ALTER）
-- =============================================

USE nongyeplus;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. knowledge_doc：补充依据来源与版本
-- ----------------------------
SET @has_source := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_doc' AND COLUMN_NAME = 'source');
SET @ddl := IF(@has_source = 0,
    'ALTER TABLE knowledge_doc ADD COLUMN source VARCHAR(200) NULL COMMENT ''依据来源（标准号/发布机构/平台整理）'' AFTER content',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_version := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_doc' AND COLUMN_NAME = 'doc_version');
SET @ddl := IF(@has_version = 0,
    'ALTER TABLE knowledge_doc ADD COLUMN doc_version VARCHAR(50) NULL COMMENT ''知识版本（标准版本或整理月份）'' AFTER source',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 按分类回填来源（已有值不覆盖）
UPDATE knowledge_doc SET source = CASE category
        WHEN '法规标准' THEN 'GB 2763 等国家标准 / 农业农村部公告'
        WHEN '种植标准' THEN '农业行业标准 / 农技推广规范'
        WHEN '储存保鲜' THEN '农产品贮藏保鲜技术规范'
        WHEN '检测知识' THEN 'GB 2763 / 检测机构公开资料'
        WHEN '流通标准' THEN '农产品冷链物流与流通规范'
        WHEN '消费科普' THEN '平台根据公开资料整理'
        ELSE '平台知识库'
    END
WHERE source IS NULL OR source = '';

UPDATE knowledge_doc SET doc_version = '2026-10'
WHERE doc_version IS NULL OR doc_version = '';

-- 标准类条目使用精确版本号（覆盖上面的通用回填）
UPDATE knowledge_doc SET source = 'GB 2763-2021 / 农药标签', doc_version = 'GB 2763-2021'
WHERE title = '吡虫啉在番茄上的残留限量与安全间隔期';

-- ----------------------------
-- 2. complaint：消费者查询/确认凭证
-- ----------------------------
SET @has_query_code := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'complaint' AND COLUMN_NAME = 'query_code');
SET @ddl := IF(@has_query_code = 0,
    'ALTER TABLE complaint ADD COLUMN query_code VARCHAR(16) NULL COMMENT ''消费者查询/确认凭证'' AFTER phone',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 历史工单回填随机查询码
UPDATE complaint
SET query_code = UPPER(SUBSTRING(MD5(CONCAT(id, RAND(), NOW())), 1, 8))
WHERE query_code IS NULL OR query_code = '';

-- 唯一索引（不存在才创建）
SET @has_index := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'complaint' AND INDEX_NAME = 'uk_complaint_query_code');
SET @ddl := IF(@has_index = 0,
    'ALTER TABLE complaint ADD UNIQUE INDEX uk_complaint_query_code (query_code)',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
