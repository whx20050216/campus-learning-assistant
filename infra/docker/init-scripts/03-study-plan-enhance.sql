-- =============================================
-- 批次 5：学习计划表字段补充
-- 补充 remind_date, reminder_sent, version, check_in_type 等字段
-- =============================================

USE learning_assistant;

DELIMITER //

DROP PROCEDURE IF EXISTS SafeAlterStudyPlanEnhance //

CREATE PROCEDURE SafeAlterStudyPlanEnhance()
BEGIN
    -- study_plan 补充字段
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'study_plan' AND COLUMN_NAME = 'remind_date') THEN
        ALTER TABLE study_plan ADD COLUMN remind_date DATE DEFAULT NULL COMMENT '提醒日期（结束日期前3天）' AFTER status;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'study_plan' AND COLUMN_NAME = 'reminder_sent') THEN
        ALTER TABLE study_plan ADD COLUMN reminder_sent TINYINT DEFAULT 0 COMMENT '提醒是否已发送 0-否 1-是' AFTER remind_date;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'study_plan' AND COLUMN_NAME = 'version') THEN
        ALTER TABLE study_plan ADD COLUMN version INT DEFAULT 0 COMMENT '乐观锁版本号' AFTER reminder_sent;
    END IF;

    -- study_task 补充字段
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'study_task' AND COLUMN_NAME = 'version') THEN
        ALTER TABLE study_task ADD COLUMN version INT DEFAULT 0 COMMENT '乐观锁版本号' AFTER completed_at;
    END IF;

    -- study_record 补充字段
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'study_record' AND COLUMN_NAME = 'check_in_type') THEN
        ALTER TABLE study_record ADD COLUMN check_in_type ENUM('manual','auto') DEFAULT 'manual' COMMENT '打卡类型' AFTER content;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'study_record' AND COLUMN_NAME = 'version') THEN
        ALTER TABLE study_record ADD COLUMN version INT DEFAULT 0 COMMENT '乐观锁版本号' AFTER check_in_type;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'study_record' AND COLUMN_NAME = 'updated_at') THEN
        ALTER TABLE study_record ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER created_at;
    END IF;
END //

DELIMITER ;

CALL SafeAlterStudyPlanEnhance();
DROP PROCEDURE IF EXISTS SafeAlterStudyPlanEnhance;
