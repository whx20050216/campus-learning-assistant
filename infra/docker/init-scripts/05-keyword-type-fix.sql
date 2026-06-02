-- =============================================
-- 批次 V 追加修复：为已存在的 keyword 表补充 type 字段
-- 适用场景：数据库已初始化运行，keyword 表缺少 type 字段
-- 执行方式：手动在 MySQL 中执行，或放入 init-scripts 后重启容器
-- =============================================

USE learning_assistant;

-- 安全添加 type 字段（幂等）
DELIMITER //

DROP PROCEDURE IF EXISTS AddKeywordTypeIfNotExists //

CREATE PROCEDURE AddKeywordTypeIfNotExists()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'keyword'
          AND COLUMN_NAME = 'type'
    ) THEN
        ALTER TABLE keyword
            ADD COLUMN type VARCHAR(50) DEFAULT 'keyword' COMMENT '关键词类型' AFTER weight;
    END IF;
END //

DELIMITER ;

CALL AddKeywordTypeIfNotExists();
DROP PROCEDURE IF EXISTS AddKeywordTypeIfNotExists;
