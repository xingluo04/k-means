-- 创建数据库
CREATE DATABASE IF NOT EXISTS student_quality_evaluation DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE student_quality_evaluation;

-- 删除旧表
DROP TABLE IF EXISTS cluster_result;
DROP TABLE IF EXISTS comprehensive_evaluation;
DROP TABLE IF EXISTS practice_evaluation;
DROP TABLE IF EXISTS art_evaluation;
DROP TABLE IF EXISTS physical_evaluation;
DROP TABLE IF EXISTS academic_evaluation;
DROP TABLE IF EXISTS moral_evaluation;
DROP TABLE IF EXISTS sys_log;
DROP TABLE IF EXISTS sys_notice;
DROP TABLE IF EXISTS student_info;
DROP TABLE IF EXISTS class_info;
DROP TABLE IF EXISTS sys_user;

-- 用户表
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) COMMENT '真实姓名',
    role VARCHAR(20) NOT NULL COMMENT '角色：admin-管理员，teacher-教师，student-学生，parent-家长',
    avatar VARCHAR(255) COMMENT '头像路径',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    gender TINYINT DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_role (role)
) COMMENT '系统用户表';

-- 班级表
CREATE TABLE class_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_name VARCHAR(100) NOT NULL COMMENT '班级名称',
    grade VARCHAR(50) COMMENT '年级',
    department VARCHAR(100) COMMENT '院系',
    teacher_id BIGINT COMMENT '班主任ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '班级信息表';

-- 学生信息表
CREATE TABLE student_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '关联用户ID',
    student_no VARCHAR(50) NOT NULL UNIQUE COMMENT '学号',
    class_id BIGINT COMMENT '班级ID',
    parent_id BIGINT COMMENT '家长用户ID',
    enrollment_year INT COMMENT '入学年份',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_class (class_id)
) COMMENT '学生信息表';

-- 德育测评表（权重25%）
CREATE TABLE moral_evaluation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生信息ID',
    academic_year VARCHAR(20) NOT NULL COMMENT '学年，如2023-2024',
    -- 德育实践
    moral_practice_base DECIMAL(5,2) DEFAULT 0 COMMENT '德育实践基础分（满分30）',
    -- 品德表现（满分50）
    political_thought DECIMAL(5,2) DEFAULT 0 COMMENT '政治思想（满分30）',
    integrity DECIMAL(5,2) DEFAULT 0 COMMENT '诚信原则（满分3）',
    learning_attitude DECIMAL(5,2) DEFAULT 0 COMMENT '学习态度（满分5）',
    discipline DECIMAL(5,2) DEFAULT 0 COMMENT '纪律作风（满分5）',
    collective DECIMAL(5,2) DEFAULT 0 COMMENT '集体观念（满分5）',
    civility DECIMAL(5,2) DEFAULT 0 COMMENT '文明礼貌（满分2）',
    moral_character_subtotal DECIMAL(5,2) DEFAULT 0 COMMENT '品德表现小计（满分50）',
    -- 奖励分（满分20）
    moral_honor DECIMAL(5,2) DEFAULT 0 COMMENT '德育荣誉分（满分10）',
    moral_social_work DECIMAL(5,2) DEFAULT 0 COMMENT '德育社会工作分（满分5）',
    moral_outstanding DECIMAL(5,2) DEFAULT 0 COMMENT '德育突出事例分（满分5）',
    moral_bonus_subtotal DECIMAL(5,2) DEFAULT 0 COMMENT '德育奖励分小计（满分20）',
    -- 扣分与汇总
    moral_deduction DECIMAL(5,2) DEFAULT 0 COMMENT '德育表现扣分',
    total_score DECIMAL(5,2) DEFAULT 0 COMMENT '德育测评总分',
    remark TEXT COMMENT '备注',
    evaluator_id BIGINT COMMENT '评价人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student (student_id),
    INDEX idx_academic_year (academic_year)
) COMMENT '德育测评表';

-- 智育测评表（权重50%）
CREATE TABLE academic_evaluation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生信息ID',
    academic_year VARCHAR(20) NOT NULL COMMENT '学年，如2023-2024',
    -- 学业成绩
    weighted_avg_score DECIMAL(5,2) DEFAULT 0 COMMENT '学分加权平均成绩',
    -- 奖励分（满分10）
    academic_bonus DECIMAL(5,2) DEFAULT 0 COMMENT '智育奖励分（满分10）',
    academic_other DECIMAL(5,2) DEFAULT 0 COMMENT '智育其他分',
    academic_bonus_subtotal DECIMAL(5,2) DEFAULT 0 COMMENT '智育奖励分小计',
    -- 扣分与汇总
    academic_deduction DECIMAL(5,2) DEFAULT 0 COMMENT '智育表现扣分',
    total_score DECIMAL(5,2) DEFAULT 0 COMMENT '智育测评总分',
    remark TEXT COMMENT '备注',
    evaluator_id BIGINT COMMENT '评价人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student (student_id),
    INDEX idx_academic_year (academic_year)
) COMMENT '智育测评表';

-- 体育测评表（权重8%）
CREATE TABLE physical_evaluation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生信息ID',
    academic_year VARCHAR(20) NOT NULL COMMENT '学年，如2023-2024',
    -- 体育表现（满分80）
    physical_performance DECIMAL(5,2) DEFAULT 0 COMMENT '体育表现分（满分80）',
    -- 奖励分（满分20）
    physical_bonus DECIMAL(5,2) DEFAULT 0 COMMENT '体育奖励分（满分20）',
    -- 扣分与汇总
    physical_deduction DECIMAL(5,2) DEFAULT 0 COMMENT '体育表现扣分',
    total_score DECIMAL(5,2) DEFAULT 0 COMMENT '体育测评总分',
    remark TEXT COMMENT '备注',
    evaluator_id BIGINT COMMENT '评价人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student (student_id),
    INDEX idx_academic_year (academic_year)
) COMMENT '体育测评表';

-- 美育测评表（权重8%）
CREATE TABLE art_evaluation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生信息ID',
    academic_year VARCHAR(20) NOT NULL COMMENT '学年，如2023-2024',
    -- 美育表现（满分70）
    art_performance DECIMAL(5,2) DEFAULT 0 COMMENT '美育表现分（满分70）',
    -- 奖励分（满分30）
    art_bonus DECIMAL(5,2) DEFAULT 0 COMMENT '美育奖励分（满分30）',
    -- 扣分与汇总
    art_deduction DECIMAL(5,2) DEFAULT 0 COMMENT '美育表现扣分',
    total_score DECIMAL(5,2) DEFAULT 0 COMMENT '美育测评总分',
    remark TEXT COMMENT '备注',
    evaluator_id BIGINT COMMENT '评价人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student (student_id),
    INDEX idx_academic_year (academic_year)
) COMMENT '美育测评表';

-- 劳动教育测评表（权重9%）
CREATE TABLE practice_evaluation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生信息ID',
    academic_year VARCHAR(20) NOT NULL COMMENT '学年，如2023-2024',
    -- 劳动教育表现（满分70）
    labor_performance DECIMAL(5,2) DEFAULT 0 COMMENT '劳动教育表现分（满分70）',
    -- 奖励分（满分30）
    labor_bonus DECIMAL(5,2) DEFAULT 0 COMMENT '劳动教育奖励分（满分30）',
    -- 扣分与汇总
    labor_deduction DECIMAL(5,2) DEFAULT 0 COMMENT '劳动教育表现扣分',
    total_score DECIMAL(5,2) DEFAULT 0 COMMENT '劳动教育测评总分',
    remark TEXT COMMENT '备注',
    evaluator_id BIGINT COMMENT '评价人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student (student_id),
    INDEX idx_academic_year (academic_year)
) COMMENT '劳动教育测评表';

-- 综合素质评价汇总表
CREATE TABLE comprehensive_evaluation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生信息ID',
    academic_year VARCHAR(20) NOT NULL COMMENT '学年，如2023-2024',
    moral_score DECIMAL(5,2) DEFAULT 0 COMMENT '德育测评得分',
    academic_score DECIMAL(5,2) DEFAULT 0 COMMENT '智育测评得分',
    physical_score DECIMAL(5,2) DEFAULT 0 COMMENT '体育测评得分',
    art_score DECIMAL(5,2) DEFAULT 0 COMMENT '美育测评得分',
    practice_score DECIMAL(5,2) DEFAULT 0 COMMENT '劳动教育测评得分',
    total_score DECIMAL(5,2) DEFAULT 0 COMMENT '综合总分（加权）',
    cluster_label INT COMMENT '聚类标签',
    cluster_name VARCHAR(50) COMMENT '聚类类别名称',
    suggestion TEXT COMMENT '个性化建议',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_student_year (student_id, academic_year),
    INDEX idx_cluster (cluster_label)
) COMMENT '综合素质评价汇总表';

-- 聚类分析结果表
CREATE TABLE cluster_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    academic_year VARCHAR(20) NOT NULL COMMENT '学年',
    cluster_count INT NOT NULL COMMENT '聚类数量',
    silhouette_score DECIMAL(5,4) COMMENT '轮廓系数',
    cluster_label INT NOT NULL COMMENT '聚类标签',
    cluster_name VARCHAR(50) COMMENT '聚类名称',
    student_count INT DEFAULT 0 COMMENT '该类学生数量',
    avg_moral DECIMAL(5,2) DEFAULT 0 COMMENT '德育平均分',
    avg_academic DECIMAL(5,2) DEFAULT 0 COMMENT '智育平均分',
    avg_physical DECIMAL(5,2) DEFAULT 0 COMMENT '体育平均分',
    avg_art DECIMAL(5,2) DEFAULT 0 COMMENT '美育平均分',
    avg_practice DECIMAL(5,2) DEFAULT 0 COMMENT '劳动教育平均分',
    description TEXT COMMENT '聚类描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_academic_year (academic_year)
) COMMENT '聚类分析结果表';

-- 系统通知表
CREATE TABLE sys_notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    type TINYINT DEFAULT 1 COMMENT '1-系统通知，2-评价通知',
    publisher_id BIGINT,
    status TINYINT DEFAULT 1 COMMENT '0-草稿，1-已发布',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '系统通知表';

-- 操作日志表
CREATE TABLE sys_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    username VARCHAR(50),
    operation VARCHAR(200),
    method VARCHAR(200),
    params TEXT,
    ip VARCHAR(50),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '操作日志表';
