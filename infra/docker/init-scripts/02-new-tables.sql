-- =============================================
-- 批次 0：ORM 迁移与数据库重建
-- 作用：
--   1. 修改 users / materials 表结构
--   2. 新建 ocr_result, keyword, knowledge_point, study_plan, study_task, study_record
-- =============================================

USE learning_assistant;

-- =============================================
-- 1. users 表结构变更（幂等，IF NOT EXISTS）
-- =============================================
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS student_no VARCHAR(50) UNIQUE COMMENT '学号，登录凭据' AFTER id,
    ADD COLUMN IF NOT EXISTS major VARCHAR(100) COMMENT '专业' AFTER email,
    ADD COLUMN IF NOT EXISTS grade VARCHAR(20) COMMENT '年级' AFTER major,
    ADD COLUMN IF NOT EXISTS role VARCHAR(20) DEFAULT 'STUDENT' COMMENT '角色：STUDENT/ADMIN' AFTER grade,
    ADD COLUMN IF NOT EXISTS status INT DEFAULT 1 COMMENT '账号状态：1-正常 0-禁用' AFTER role,
    ADD COLUMN IF NOT EXISTS storage_quota BIGINT DEFAULT 5368709120 COMMENT '存储配额字节，默认5GB' AFTER status,
    ADD COLUMN IF NOT EXISTS used_storage BIGINT DEFAULT 0 COMMENT '已用存储字节' AFTER storage_quota,
    ADD INDEX IF NOT EXISTS idx_student_no (student_no);

-- =============================================
-- 2. materials 表结构变更（幂等）
-- =============================================

-- 新增字段
ALTER TABLE materials
    ADD COLUMN IF NOT EXISTS pages INT COMMENT '资料页数（学习计划用）' AFTER file_size,
    ADD COLUMN IF NOT EXISTS md5 VARCHAR(64) COMMENT '文件MD5校验值' AFTER pages,
    ADD COLUMN IF NOT EXISTS course_tag VARCHAR(100) COMMENT '课程分类标签' AFTER md5;

-- 重命名字段（条件判断，避免重复执行报错）
SET @rename_needed = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'materials' AND COLUMN_NAME = 'filename');
SET @rename_sql = IF(@rename_needed > 0, 'ALTER TABLE materials RENAME COLUMN filename TO title, RENAME COLUMN minio_path TO file_url', 'SELECT 1');
PREPARE rename_stmt FROM @rename_sql;
EXECUTE rename_stmt;
DEALLOCATE PREPARE rename_stmt;

-- 移除 OCR/NLP 相关字段（迁移到 ocr_result / keyword 表）
ALTER TABLE materials
    DROP COLUMN IF EXISTS ocr_text,
    DROP COLUMN IF EXISTS ocr_confidence,
    DROP COLUMN IF EXISTS keywords,
    DROP COLUMN IF EXISTS summary;

-- 为 course_tag 加索引
ALTER TABLE materials ADD INDEX IF NOT EXISTS idx_course_tag (course_tag);

-- =============================================
-- 3. ocr_result 表（OCR 与 NLP 结果）
-- =============================================
CREATE TABLE IF NOT EXISTS ocr_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT NOT NULL UNIQUE COMMENT '关联资料ID',
    ocr_text TEXT COMMENT 'OCR识别出的完整文本',
    confidence FLOAT COMMENT 'OCR置信度 0.0-1.0',
    source VARCHAR(50) DEFAULT 'local' COMMENT '处理来源：local/ai_enhanced/local_fallback',
    engine VARCHAR(100) COMMENT '实际使用的识别引擎',
    summary TEXT COMMENT 'TextRank生成的文本摘要',
    processing_time_ms INT COMMENT '处理耗时（毫秒）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (material_id) REFERENCES materials(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OCR识别与NLP分析结果表';

-- =============================================
-- 4. keyword 表（关键词独立表，替代 materials.keywords 字段）
-- =============================================
CREATE TABLE IF NOT EXISTS keyword (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT NOT NULL COMMENT '关联资料ID',
    keyword VARCHAR(100) NOT NULL COMMENT '关键词',
    weight FLOAT DEFAULT 0 COMMENT 'TF-IDF权重',
    type VARCHAR(50) DEFAULT 'keyword' COMMENT '关键词类型',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (material_id) REFERENCES materials(id) ON DELETE CASCADE,
    INDEX idx_keyword (keyword)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资料关键词表';

-- =============================================
-- 5. knowledge_point 表（知识点）
-- =============================================
CREATE TABLE IF NOT EXISTS knowledge_point (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT NOT NULL COMMENT '关联资料ID',
    content TEXT NOT NULL COMMENT '知识点内容',
    type VARCHAR(50) COMMENT '知识点类型',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (material_id) REFERENCES materials(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点提取表';

-- =============================================
-- 6. study_plan 表（学习计划）
-- =============================================
CREATE TABLE IF NOT EXISTS study_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    name VARCHAR(255) NOT NULL COMMENT '计划名称',
    description TEXT COMMENT '计划描述',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    daily_hours FLOAT NOT NULL COMMENT '每日学习时长（小时）',
    total_pages INT NOT NULL COMMENT '总页数',
    progress FLOAT DEFAULT 0 COMMENT '进度 0-100',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '计划状态：ACTIVE/COMPLETED/CANCELLED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习计划表';

-- =============================================
-- 7. study_task 表（学习任务）
-- =============================================
CREATE TABLE IF NOT EXISTS study_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL COMMENT '所属计划ID',
    material_id BIGINT COMMENT '关联资料ID',
    task_name VARCHAR(255) NOT NULL COMMENT '任务名称',
    task_date DATE NOT NULL COMMENT '任务日期',
    planned_hours FLOAT NOT NULL COMMENT '计划时长（小时）',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '任务状态：PENDING/COMPLETED/SKIPPED',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (plan_id) REFERENCES study_plan(id) ON DELETE CASCADE,
    FOREIGN KEY (material_id) REFERENCES materials(id) ON DELETE SET NULL,
    INDEX idx_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习任务表';

-- =============================================
-- 8. study_record 表（学习记录/打卡）
-- =============================================
CREATE TABLE IF NOT EXISTS study_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '关联任务ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    study_date DATE NOT NULL COMMENT '学习日期',
    duration INT COMMENT '实际学习时长（分钟）',
    content TEXT COMMENT '学习内容备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (task_id) REFERENCES study_task(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_task_id (task_id),
    INDEX idx_user_date (user_id, study_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习打卡记录表';
