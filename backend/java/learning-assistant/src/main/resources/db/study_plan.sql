-- 学习计划表
CREATE TABLE IF NOT EXISTS study_plans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(200) NOT NULL COMMENT '计划名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '计划描述',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    daily_hours FLOAT NOT NULL COMMENT '每日学习时长（小时）',
    total_pages INT NOT NULL COMMENT '总页数/工作量',
    progress FLOAT DEFAULT 0 COMMENT '完成进度 0-100',
    status ENUM('active','paused','completed','overdue') DEFAULT 'active' COMMENT '计划状态',
    remind_date DATE DEFAULT NULL COMMENT '提醒日期（结束日期前3天）',
    reminder_sent TINYINT DEFAULT 0 COMMENT '提醒是否已发送 0-否 1-是',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME DEFAULT NOW() COMMENT '创建时间',
    updated_at DATETIME DEFAULT NOW() ON UPDATE NOW() COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_end_date (end_date),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习计划表';

-- 学习任务表
CREATE TABLE IF NOT EXISTS study_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL COMMENT '计划ID',
    material_id BIGINT DEFAULT NULL COMMENT '关联资料ID',
    task_name VARCHAR(200) NOT NULL COMMENT '任务名称',
    task_date DATE NOT NULL COMMENT '任务日期',
    planned_hours FLOAT NOT NULL COMMENT '计划时长（小时）',
    status ENUM('pending','completed','skipped') DEFAULT 'pending' COMMENT '任务状态',
    completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME DEFAULT NOW() COMMENT '创建时间',
    updated_at DATETIME DEFAULT NOW() ON UPDATE NOW() COMMENT '更新时间',
    INDEX idx_plan_id (plan_id),
    INDEX idx_task_date (task_date),
    INDEX idx_status (status),
    FOREIGN KEY (plan_id) REFERENCES study_plans(id) ON DELETE CASCADE,
    FOREIGN KEY (material_id) REFERENCES materials(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习任务表';

-- 学习记录表
CREATE TABLE IF NOT EXISTS study_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL COMMENT '任务ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    study_date DATE NOT NULL COMMENT '学习日期',
    duration INT NOT NULL COMMENT '学习时长（分钟）',
    content TEXT DEFAULT NULL COMMENT '学习内容摘要',
    check_in_type ENUM('manual','auto') DEFAULT 'manual' COMMENT '打卡类型',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    created_at DATETIME DEFAULT NOW() COMMENT '创建时间',
    updated_at DATETIME DEFAULT NOW() ON UPDATE NOW() COMMENT '更新时间',
    INDEX idx_task_id (task_id),
    INDEX idx_user_id (user_id),
    INDEX idx_study_date (study_date),
    FOREIGN KEY (task_id) REFERENCES study_tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习记录表';
