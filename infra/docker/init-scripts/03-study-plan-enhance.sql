-- =============================================
-- 批次 5：学习计划表字段补充
-- 补充 remind_date, reminder_sent, version, check_in_type 等字段
-- =============================================

USE learning_assistant;

-- study_plan 补充字段
ALTER TABLE study_plan
    ADD COLUMN IF NOT EXISTS remind_date DATE DEFAULT NULL COMMENT '提醒日期（结束日期前3天）' AFTER status,
    ADD COLUMN IF NOT EXISTS reminder_sent TINYINT DEFAULT 0 COMMENT '提醒是否已发送 0-否 1-是' AFTER remind_date,
    ADD COLUMN IF NOT EXISTS version INT DEFAULT 0 COMMENT '乐观锁版本号' AFTER reminder_sent;

-- study_task 补充字段
ALTER TABLE study_task
    ADD COLUMN IF NOT EXISTS version INT DEFAULT 0 COMMENT '乐观锁版本号' AFTER completed_at;

-- study_record 补充字段
ALTER TABLE study_record
    ADD COLUMN IF NOT EXISTS check_in_type ENUM('manual','auto') DEFAULT 'manual' COMMENT '打卡类型' AFTER content,
    ADD COLUMN IF NOT EXISTS version INT DEFAULT 0 COMMENT '乐观锁版本号' AFTER check_in_type,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER created_at;
