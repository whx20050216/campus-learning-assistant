-- 创建应用用户（已由 docker-compose 环境变量 MYSQL_USER/MYSQL_PASSWORD 自动创建）
-- 仅保留授权语句
GRANT ALL PRIVILEGES ON learning_assistant.* TO 'cla_user'@'%';
FLUSH PRIVILEGES;

USE learning_assistant;

-- =============================================
-- 1. users 表（最新完整结构）
-- =============================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_no VARCHAR(50) UNIQUE COMMENT '学号',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    password_hash VARCHAR(255) NOT NULL COMMENT '加密密码',
    major VARCHAR(100) COMMENT '专业',
    grade VARCHAR(20) COMMENT '年级',
    role VARCHAR(20) DEFAULT 'STUDENT' COMMENT '角色：STUDENT/ADMIN',
    status INT DEFAULT 1 COMMENT '状态：1正常 0冻结',
    storage_quota BIGINT DEFAULT 5368709120 COMMENT '存储配额(字节)，默认5GB',
    used_storage BIGINT DEFAULT 0 COMMENT '已用存储(字节)',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_student_no (student_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- =============================================
-- 2. materials 表（最新完整结构）
-- =============================================
CREATE TABLE IF NOT EXISTS materials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '上传用户ID',
    title VARCHAR(255) NOT NULL COMMENT '资料标题',
    file_url VARCHAR(500) NOT NULL COMMENT 'MinIO文件URL',
    file_type VARCHAR(20) NOT NULL COMMENT '文件类型：PDF/JPG/PNG',
    file_size BIGINT COMMENT '文件大小(字节)',
    md5 VARCHAR(64) COMMENT 'MD5校验值',
    course_tag VARCHAR(100) COMMENT '课程标签',
    status VARCHAR(50) DEFAULT 'processing' COMMENT '处理状态',
    source VARCHAR(50) COMMENT '处理来源',
    audit_status VARCHAR(50) DEFAULT 'approved' COMMENT '审核状态',
    deleted_at DATETIME COMMENT '删除时间（逻辑删除）',
    deleted_by VARCHAR(100) COMMENT '删除操作人',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_course_tag (course_tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习资料表';

-- =============================================
-- 3. ocr_result 表
-- =============================================
CREATE TABLE IF NOT EXISTS ocr_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT NOT NULL UNIQUE COMMENT '关联资料ID',
    ocr_text TEXT COMMENT 'OCR识别文本',
    confidence FLOAT COMMENT '置信度',
    source VARCHAR(50) DEFAULT 'local' COMMENT '处理来源',
    engine VARCHAR(100) COMMENT '识别引擎',
    summary TEXT COMMENT '文本摘要',
    processing_time_ms INT COMMENT '处理耗时(毫秒)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OCR结果表';

-- =============================================
-- 4. keyword 表
-- =============================================
CREATE TABLE IF NOT EXISTS keyword (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT NOT NULL COMMENT '关联资料ID',
    keyword VARCHAR(100) NOT NULL COMMENT '关键词',
    weight FLOAT DEFAULT 0 COMMENT 'TF-IDF权重',
    type VARCHAR(50) DEFAULT 'keyword' COMMENT '关键词类型',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关键词表';

-- =============================================
-- 5. knowledge_point 表
-- =============================================
CREATE TABLE IF NOT EXISTS knowledge_point (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT NOT NULL COMMENT '关联资料ID',
    content TEXT NOT NULL COMMENT '知识点内容',
    type VARCHAR(50) COMMENT '知识点类型',
    position VARCHAR(50) COMMENT '位置信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点表';

-- =============================================
-- 6. study_plan 表
-- =============================================
CREATE TABLE IF NOT EXISTS study_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    name VARCHAR(255) NOT NULL COMMENT '计划名称',
    description TEXT COMMENT '计划描述',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    daily_hours FLOAT NOT NULL COMMENT '每日学习时长',
    total_pages INT NOT NULL COMMENT '总页数',
    progress FLOAT DEFAULT 0 COMMENT '进度0-100',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '计划状态',
    remind_date DATE COMMENT '提醒日期',
    reminder_sent TINYINT DEFAULT 0 COMMENT '提醒是否已发送',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习计划表';

-- =============================================
-- 7. study_task 表
-- =============================================
CREATE TABLE IF NOT EXISTS study_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL COMMENT '所属计划ID',
    material_id BIGINT COMMENT '关联资料ID',
    task_name VARCHAR(255) NOT NULL COMMENT '任务名称',
    task_date DATE NOT NULL COMMENT '任务日期',
    planned_hours FLOAT NOT NULL COMMENT '计划时长',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '任务状态',
    completed_at DATETIME COMMENT '完成时间',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习任务表';

-- =============================================
-- 8. study_record 表
-- =============================================
CREATE TABLE IF NOT EXISTS study_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '关联任务ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    study_date DATE NOT NULL COMMENT '学习日期',
    duration INT COMMENT '实际学习时长(分钟)',
    content TEXT COMMENT '学习内容备注',
    check_in_type VARCHAR(20) DEFAULT 'manual' COMMENT '打卡类型',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_user_date (user_id, study_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习打卡记录表';

-- =============================================
-- 9. 预置管理员账号
-- =============================================
INSERT IGNORE INTO users (student_no, username, email, password_hash, role, status, storage_quota)
VALUES ('admin', '管理员', 'admin@campus.edu', '$2b$10$lNbG2V2Dw14gwpR.5PYBfuC6bvSkG022oPqV/E3KRjwedzAn/BVRm', 'ADMIN', 1, 5368709120);


