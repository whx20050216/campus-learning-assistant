-- =============================================
-- 批次 C：补充 materials 表缺失字段 + 建立 FULLTEXT 索引
-- =============================================

USE learning_assistant;

-- =============================================
-- 1. 安全添加 materials 表缺失字段（兼容已初始化数据库）
-- =============================================
DELIMITER //

DROP PROCEDURE IF EXISTS AddColumnIfNotExists //

CREATE PROCEDURE AddColumnIfNotExists(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = p_table
        AND COLUMN_NAME = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table, ' ADD COLUMN ', p_column, ' ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

DELIMITER ;

CALL AddColumnIfNotExists('materials', 'audit_status', 'VARCHAR(50) DEFAULT "pending" COMMENT "审核状态：pending/approved/rejected"');
CALL AddColumnIfNotExists('materials', 'deleted_at', 'DATETIME DEFAULT NULL COMMENT "删除时间"');
CALL AddColumnIfNotExists('materials', 'deleted_by', 'VARCHAR(100) DEFAULT NULL COMMENT "删除操作人"');

DROP PROCEDURE IF EXISTS AddColumnIfNotExists;

-- =============================================
-- 2. 建立 FULLTEXT 索引（支持中文需 ngram 解析器）
-- =============================================

-- materials.title 全文索引（使用 ngram 解析器以支持中文分词）
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'materials'
    AND INDEX_NAME = 'ft_title'
);
SET @sql_title := IF(@idx_exists = 0,
    'ALTER TABLE materials ADD FULLTEXT INDEX ft_title(title) WITH PARSER ngram',
    'SELECT 1'
);
PREPARE stmt FROM @sql_title;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ocr_result.ocr_text 全文索引（使用 ngram 解析器以支持中文分词）
SET @idx_exists2 := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ocr_result'
    AND INDEX_NAME = 'ft_ocr_text'
);
SET @sql_ocr := IF(@idx_exists2 = 0,
    'ALTER TABLE ocr_result ADD FULLTEXT INDEX ft_ocr_text(ocr_text) WITH PARSER ngram',
    'SELECT 1'
);
PREPARE stmt2 FROM @sql_ocr;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
