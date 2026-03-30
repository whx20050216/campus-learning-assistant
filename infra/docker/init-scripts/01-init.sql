-- 创建应用用户（不用root连接）
CREATE USER IF NOT EXISTS 'cla_user'@'%' IDENTIFIED BY 'ClaPassword2024';
GRANT ALL PRIVILEGES ON learning_assistant.* TO 'cla_user'@'%';
FLUSH PRIVILEGES;

USE learning_assistant;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    password_hash VARCHAR(255) NOT NULL COMMENT '加密密码',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 学习资料表
CREATE TABLE IF NOT EXISTS materials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '上传用户ID',
    filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_type ENUM('PPT', 'PDF', 'IMAGE') NOT NULL COMMENT '文件类型',
    minio_path VARCHAR(500) NOT NULL COMMENT 'MinIO存储路径',
    file_size BIGINT COMMENT '文件大小(字节)',
    ocr_text TEXT COMMENT 'OCR识别文本',
    ocr_confidence FLOAT COMMENT 'OCR置信度(0-1)',
    keywords JSON COMMENT '提取的关键词(JSON数组)',
    summary TEXT COMMENT '文本摘要',
    source ENUM('local', 'ai_enhanced', 'local_fallback') DEFAULT 'local' COMMENT '处理来源',
    status ENUM('uploaded', 'processing', 'completed', 'failed') DEFAULT 'uploaded' COMMENT '处理状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    FULLTEXT INDEX ft_ocr_text (ocr_text) COMMENT '全文搜索索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习资料表';

-- API调用日志表（用于额度管控）
CREATE TABLE IF NOT EXISTS api_calls (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    api_type VARCHAR(50) NOT NULL COMMENT 'API类型(ocr/chat)',
    call_date DATE NOT NULL COMMENT '调用日期',
    call_count INT DEFAULT 1 COMMENT '当日调用次数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_api_date (user_id, api_type, call_date),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API调用日志表';