-- Campus Knowledge Assistant 数据库初始化脚本
-- 需要 PostgreSQL 16+ (含 pgvector 扩展) + MySQL 8.0+

-- ============================================
-- MySQL 业务数据库
-- ============================================
CREATE DATABASE IF NOT EXISTS campus_assistant DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_assistant;

-- 用户表
CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
    nickname VARCHAR(50) COMMENT '昵称',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    avatar VARCHAR(255) COMMENT '头像URL',
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT '角色: STUDENT/TEACHER/ADMIN',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0禁用 1启用',
    last_login_time DATETIME COMMENT '最后登录时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 会话表
CREATE TABLE conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(200) NOT NULL DEFAULT '新对话' COMMENT '会话标题',
    message_count INT NOT NULL DEFAULT 0 COMMENT '消息数量',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0已删除 1正常',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- 消息表
CREATE TABLE message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色: user/assistant',
    content TEXT NOT NULL COMMENT '消息内容(Markdown)',
    references_json JSON COMMENT '引用的知识片段',
    token_count INT COMMENT 'Token消耗',
    feedback VARCHAR(20) COMMENT '反馈: like/dislike',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 知识库分类表
CREATE TABLE knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '分类名称',
    description VARCHAR(500) COMMENT '分类描述',
    icon VARCHAR(50) COMMENT '图标',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库分类表';

-- 文档表
CREATE TABLE document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    knowledge_base_id BIGINT NOT NULL COMMENT '知识库ID',
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_type VARCHAR(20) NOT NULL COMMENT '文件类型: pdf/docx/md/txt/url',
    file_size BIGINT COMMENT '文件大小(字节)',
    file_url VARCHAR(500) COMMENT 'MinIO文件URL',
    chunk_count INT NOT NULL DEFAULT 0 COMMENT '切片数量',
    parse_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '解析状态: PENDING/PARSING/COMPLETED/FAILED',
    parse_error VARCHAR(500) COMMENT '解析错误信息',
    is_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否参与检索',
    created_by BIGINT COMMENT '上传者ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_knowledge_base_id (knowledge_base_id),
    INDEX idx_parse_status (parse_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档表';

-- 反馈表
CREATE TABLE feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    message_id BIGINT NOT NULL COMMENT '消息ID',
    feedback_type VARCHAR(20) NOT NULL COMMENT '反馈类型: like/dislike',
    reason VARCHAR(500) COMMENT '反馈原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_message_id (message_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈表';

-- 操作日志表
CREATE TABLE user_action_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT COMMENT '用户ID',
    action VARCHAR(50) NOT NULL COMMENT '操作类型: LOGIN/CHAT/UPLOAD/SEARCH',
    target_type VARCHAR(50) COMMENT '目标类型',
    target_id BIGINT COMMENT '目标ID',
    ip VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    detail JSON COMMENT '操作详情',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 初始管理员账号: admin / admin123
INSERT INTO user (username, password, nickname, role, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '系统管理员', 'ADMIN', 1);

-- 初始知识库分类
INSERT INTO knowledge_base (name, description, icon, sort_order) VALUES
('规章制度', '学校各类规章制度、管理办法', 'rule', 1),
('办事流程', '校园办事指南、流程说明', 'guide', 2),
('课程信息', '课程大纲、选课说明、学分要求', 'course', 3),
('校园生活', '食宿、交通、社团等校园生活信息', 'life', 4);


-- ============================================
-- 私信系统
-- ============================================
CREATE TABLE im_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user1_id BIGINT NOT NULL COMMENT '用户1ID（较小值）',
    user2_id BIGINT NOT NULL COMMENT '用户2ID（较大值）',
    last_message VARCHAR(500) COMMENT '最后一条消息',
    last_message_at DATETIME COMMENT '最后消息时间',
    user1_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '用户1是否删除',
    user2_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '用户2是否删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_user_pair (user1_id, user2_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私信会话表';

CREATE TABLE im_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    content TEXT NOT NULL COMMENT '消息内容',
    type VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT/IMAGE',
    is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
    read_at DATETIME COMMENT '阅读时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私信消息表';

-- ============================================
-- 失物招领
-- ============================================
CREATE TABLE lost_found_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(10) NOT NULL COMMENT 'LOST寻物 FOUND拾到',
    title VARCHAR(100) NOT NULL COMMENT '物品名称',
    description TEXT COMMENT '详细描述',
    category VARCHAR(20) COMMENT '证件/电子/衣物/书本/钥匙/其他',
    location VARCHAR(200) COMMENT '捡到或丢失地点',
    contact_way VARCHAR(50) COMMENT '联系方式',
    image_urls VARCHAR(1000) COMMENT '图片URL逗号分隔',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/RESOLVED',
    publisher_id BIGINT NOT NULL COMMENT '发布者ID',
    published_at DATETIME COMMENT '发布时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_type_status (type, status),
    INDEX idx_category (category),
    INDEX idx_publisher (publisher_id),
    INDEX idx_published_at (published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='失物招领表';

-- ============================================
-- 公告系统
-- ============================================
USE campus_assistant;

CREATE TABLE announcement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '标题',
    summary VARCHAR(500) COMMENT '摘要',
    content LONGTEXT COMMENT '正文（富文本）',
    cover_image VARCHAR(500) NOT NULL COMMENT '封面图URL',
    category VARCHAR(20) NOT NULL COMMENT '分类: NOTICE/ACTIVITY/EXAM/COURSE',
    priority TINYINT NOT NULL DEFAULT 0 COMMENT '0普通 1重要 2紧急',
    is_carousel TINYINT NOT NULL DEFAULT 0 COMMENT '是否轮播图',
    carousel_sort INT NOT NULL DEFAULT 0 COMMENT '轮播排序',
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' COMMENT 'DRAFT/PUBLISHED',
    publisher_id BIGINT NOT NULL COMMENT '发布者ID',
    published_at DATETIME COMMENT '发布时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_category (category),
    INDEX idx_carousel (is_carousel, carousel_sort),
    INDEX idx_published_at (published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

CREATE TABLE announcement_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    announcement_id BIGINT NOT NULL COMMENT '公告ID',
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT,
    file_url VARCHAR(500) NOT NULL COMMENT 'MinIO存储路径',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_announcement_id (announcement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告附件表';


-- ============================================
-- 课程学习
-- ============================================
CREATE TABLE course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '课程标题',
    description TEXT COMMENT '课程简介',
    cover_image VARCHAR(500) COMMENT '封面图URL',
    instructor VARCHAR(100) COMMENT '讲师',
    category VARCHAR(50) COMMENT '分类',
    view_count INT NOT NULL DEFAULT 0 COMMENT '播放量',
    like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    favorite_count INT NOT NULL DEFAULT 0 COMMENT '收藏数',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0隐藏 1公开',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

CREATE TABLE course_chapter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL COMMENT '课程ID',
    title VARCHAR(200) NOT NULL COMMENT '章节标题',
    description VARCHAR(500) COMMENT '章节简介',
    video_url VARCHAR(500) COMMENT '视频URL',
    duration INT COMMENT '视频时长(秒)',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_course_id (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程章节表';

CREATE TABLE course_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL COMMENT '课程ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    parent_id BIGINT COMMENT '父评论ID(回复)',
    reply_to_user_id BIGINT COMMENT '回复目标用户ID',
    content TEXT NOT NULL COMMENT '评论内容',
    like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_course_id (course_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程评论表';

CREATE TABLE course_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL COMMENT '课程ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_course_user (course_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程点赞记录';

CREATE TABLE course_favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL COMMENT '课程ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_course_user (course_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程收藏记录';

CREATE TABLE course_comment_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL COMMENT '评论ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_comment_user (comment_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程评论点赞记录';

-- ============================================
-- 问答广场
-- ============================================
CREATE TABLE square_post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '发布者ID',
    conversation_id BIGINT COMMENT '来源会话ID',
    message_id BIGINT COMMENT '来源消息ID',
    title VARCHAR(200) NOT NULL COMMENT '问题标题',
    question TEXT NOT NULL COMMENT '原始问题',
    answer TEXT NOT NULL COMMENT 'AI回答(Markdown)',
    references_json JSON COMMENT '引用的知识来源',
    category VARCHAR(50) COMMENT '分类',
    view_count INT NOT NULL DEFAULT 0 COMMENT '浏览量',
    like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0隐藏 1公开',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    FULLTEXT INDEX ft_title_question (title, question) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答广场帖子表';

CREATE TABLE square_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_post_user (post_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问答广场点赞记录';

-- ============================================
-- 二手交易市场
-- ============================================
CREATE TABLE market_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '发布者ID',
    title VARCHAR(200) NOT NULL COMMENT '商品标题',
    description TEXT COMMENT '商品描述',
    price DECIMAL(10,2) NOT NULL COMMENT '售价',
    original_price DECIMAL(10,2) COMMENT '原价',
    category VARCHAR(50) COMMENT '分类',
    `condition` VARCHAR(20) COMMENT '成色: NEW/LIKE_NEW/USED/OLD',
    images VARCHAR(2000) COMMENT '图片URL JSON数组',
    status VARCHAR(20) NOT NULL DEFAULT 'ON_SALE' COMMENT 'ON_SALE/SOLD/REMOVED',
    view_count INT NOT NULL DEFAULT 0 COMMENT '浏览量',
    like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='二手交易商品表';

CREATE TABLE market_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL COMMENT '商品ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    parent_id BIGINT COMMENT '父评论ID(回复)',
    reply_to_user_id BIGINT COMMENT '回复目标用户ID',
    content TEXT NOT NULL COMMENT '评论内容',
    like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_item_id (item_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='二手交易评论表';

CREATE TABLE market_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL COMMENT '商品ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_item_user (item_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='二手交易点赞记录';

CREATE TABLE market_offer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL COMMENT '商品ID',
    buyer_id BIGINT NOT NULL COMMENT '买家ID',
    seller_id BIGINT NOT NULL COMMENT '卖家ID',
    price DECIMAL(10,2) NOT NULL COMMENT '出价',
    message VARCHAR(500) COMMENT '留言',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/REJECTED/CANCELLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_item_id (item_id),
    INDEX idx_buyer_id (buyer_id),
    INDEX idx_seller_id (seller_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='二手交易出价表';

-- ============================================
-- PostgreSQL 向量数据库 (需先安装 pgvector 扩展)
-- ============================================
-- CREATE EXTENSION IF NOT EXISTS vector;
--
-- CREATE TABLE document_chunk (
--     id BIGSERIAL PRIMARY KEY,
--     document_id BIGINT NOT NULL,
--     chunk_index INT NOT NULL,
--     content TEXT NOT NULL,
--     embedding vector(384),
--     metadata JSONB,
--     created_at TIMESTAMP NOT NULL DEFAULT NOW()
-- );
--
-- CREATE INDEX ON document_chunk USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);


-- ============================================
-- 课程视频表 (CourseVideo 实体使用)
-- ============================================
CREATE TABLE course_video (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chapter_id BIGINT NOT NULL COMMENT '章节ID',
    title VARCHAR(200) COMMENT '视频标题',
    video_url VARCHAR(500) COMMENT '视频URL',
    duration INT COMMENT '视频时长(秒)',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_chapter_deleted_sort (chapter_id, deleted, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程视频表';

-- ============================================
-- 通知表 (Notification 实体使用)
-- ============================================
CREATE TABLE notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    type VARCHAR(50) COMMENT '通知类型: LIKE/COMMENT/REPLY/OFFER/SYSTEM',
    title VARCHAR(200) COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    target_type VARCHAR(50) COMMENT '关联目标类型: MARKET/COURSE/SQUARE',
    target_id BIGINT COMMENT '关联目标ID',
    is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_user_read_created (user_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- ============================================
-- 广场评论表
-- ============================================
CREATE TABLE square_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    parent_id BIGINT COMMENT '父评论ID(回复)',
    reply_to_user_id BIGINT COMMENT '回复目标用户ID',
    content TEXT NOT NULL COMMENT '评论内容',
    like_count INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_post_id (post_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广场评论表';

-- ============================================
-- 广场收藏和评论点赞表
-- ============================================
CREATE TABLE square_favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_post_user (post_id, user_id),
    INDEX idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广场收藏记录';

CREATE TABLE square_comment_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL COMMENT '评论ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_comment_user (comment_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广场评论点赞记录';
