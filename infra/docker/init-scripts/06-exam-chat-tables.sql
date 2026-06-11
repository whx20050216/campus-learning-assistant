-- ============================================
-- 06-exam-chat-tables.sql
-- 试卷、题目、答题记录、AI对话会话/消息表
-- 执行顺序：06
-- ============================================

-- 试卷表
CREATE TABLE IF NOT EXISTS exam_papers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    study_plan_id BIGINT COMMENT '关联学习计划ID（一键组卷时填充）',
    course_tag VARCHAR(50) NOT NULL COMMENT '课程标签（组卷时必填，用于数据分析分类）',
    title VARCHAR(255) NOT NULL COMMENT '试卷标题',
    material_ids VARCHAR(500) NOT NULL COMMENT '关联资料ID，逗号分隔',
    question_count INT NOT NULL DEFAULT 10 COMMENT '题目数量',
    difficulty VARCHAR(20) DEFAULT 'medium' COMMENT '难度：easy/medium/hard',
    status VARCHAR(20) DEFAULT 'draft' COMMENT '状态：draft/published',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_study_plan_id (study_plan_id),
    INDEX idx_course_tag (course_tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷表';

-- 题目表
CREATE TABLE IF NOT EXISTS exam_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    paper_id BIGINT NOT NULL COMMENT '试卷ID',
    material_id BIGINT COMMENT '来源资料ID',
    type VARCHAR(20) NOT NULL COMMENT '题型：single/multiple/judge/essay',
    content TEXT NOT NULL COMMENT '题目内容',
    options JSON COMMENT '选项JSON（单选/多选/判断）',
    answer TEXT COMMENT '标准答案',
    analysis TEXT COMMENT '解析',
    difficulty VARCHAR(20) DEFAULT 'medium' COMMENT '难度',
    knowledge_point VARCHAR(255) COMMENT '知识点',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_paper_id (paper_id),
    FOREIGN KEY (paper_id) REFERENCES exam_papers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- 答题记录表
CREATE TABLE IF NOT EXISTS exam_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    paper_id BIGINT NOT NULL COMMENT '试卷ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    answers JSON COMMENT '用户答案JSON [{questionId, userAnswer, isCorrect}]',
    score INT DEFAULT 0 COMMENT '得分（百分制）',
    correct_count INT DEFAULT 0 COMMENT '正确题数',
    total_count INT NOT NULL DEFAULT 0 COMMENT '总题数',
    spent_time_seconds INT DEFAULT 0 COMMENT '答题耗时（秒）',
    status VARCHAR(20) DEFAULT 'submitted' COMMENT '状态：submitted/corrected',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_paper_id (paper_id),
    INDEX idx_user_id (user_id),
    FOREIGN KEY (paper_id) REFERENCES exam_papers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题记录表';

-- AI对话会话表
CREATE TABLE IF NOT EXISTS chat_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(100) DEFAULT '新对话' COMMENT '会话标题',
    material_id BIGINT COMMENT '关联资料ID（RAG上下文，null为通用问答）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话会话表';

-- AI对话消息表
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色：user/assistant',
    content TEXT NOT NULL COMMENT '消息内容',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id),
    FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话消息表';
