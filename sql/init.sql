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

-- 品德素质评价表
CREATE TABLE moral_evaluation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生信息ID',
    semester VARCHAR(20) NOT NULL COMMENT '学期',
    -- 德育实践
    moral_activity_count INT DEFAULT 0 COMMENT '德育实践活动参与次数',
    moral_activity_score DECIMAL(5,2) DEFAULT 0 COMMENT '德育实践得分',
    -- 品德表现
    political_thought_score DECIMAL(5,2) DEFAULT 0 COMMENT '政治思想得分',
    integrity_score DECIMAL(5,2) DEFAULT 0 COMMENT '诚信原则得分',
    learning_attitude_score DECIMAL(5,2) DEFAULT 0 COMMENT '学习态度得分',
    discipline_score DECIMAL(5,2) DEFAULT 0 COMMENT '纪律作风得分',
    collective_score DECIMAL(5,2) DEFAULT 0 COMMENT '集体观念得分',
    civility_score DECIMAL(5,2) DEFAULT 0 COMMENT '文明礼貌得分',
    -- 荣誉与奖励
    personal_honor_count INT DEFAULT 0 COMMENT '个人荣誉数',
    collective_honor_count INT DEFAULT 0 COMMENT '集体荣誉数',
    honor_score DECIMAL(5,2) DEFAULT 0 COMMENT '荣誉奖励得分',
    military_training_level VARCHAR(20) COMMENT '军训等级',
    -- 社会工作
    cadre_duty VARCHAR(100) COMMENT '学生干部职务',
    cadre_duration INT DEFAULT 0 COMMENT '任职时长(月)',
    social_work_score DECIMAL(5,2) DEFAULT 0 COMMENT '社会工作得分',
    -- 突出事例
    outstanding_event_count INT DEFAULT 0 COMMENT '突出事例数',
    blood_donation_count INT DEFAULT 0 COMMENT '无偿献血次数',
    outstanding_score DECIMAL(5,2) DEFAULT 0 COMMENT '突出事例得分',
    -- 扣分事项
    criticism_count INT DEFAULT 0 COMMENT '通报批评次数',
    discipline_punishment VARCHAR(50) COMMENT '纪律处分类型',
    absent_activity_count INT DEFAULT 0 COMMENT '无故缺席活动次数',
    deduction_score DECIMAL(5,2) DEFAULT 0 COMMENT '扣分合计',
    -- 汇总
    total_score DECIMAL(5,2) DEFAULT 0 COMMENT '品德素质总分',
    remark TEXT COMMENT '备注',
    evaluator_id BIGINT COMMENT '评价人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student (student_id),
    INDEX idx_semester (semester)
) COMMENT '品德素质评价表';

-- 学业素质评价表
CREATE TABLE academic_evaluation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生信息ID',
    semester VARCHAR(20) NOT NULL COMMENT '学期',
    -- 学业表现
    weighted_avg_score DECIMAL(5,2) DEFAULT 0 COMMENT '学分加权平均成绩',
    course_count INT DEFAULT 0 COMMENT '课程门数',
    retake_count INT DEFAULT 0 COMMENT '重修/补考门数',
    minor_count INT DEFAULT 0 COMMENT '辅修课程门数',
    academic_performance_score DECIMAL(5,2) DEFAULT 0 COMMENT '学业表现得分',
    -- 学科竞赛与科研
    competition_count INT DEFAULT 0 COMMENT '竞赛获奖数',
    competition_highest_level VARCHAR(20) COMMENT '竞赛最高级别',
    paper_count INT DEFAULT 0 COMMENT '论文发表数',
    paper_first_author INT DEFAULT 0 COMMENT '第一作者论文数',
    research_project_count INT DEFAULT 0 COMMENT '科研项目参与数',
    research_host_count INT DEFAULT 0 COMMENT '主持科研项目数',
    tech_promotion_count INT DEFAULT 0 COMMENT '科技推广活动次数',
    research_score DECIMAL(5,2) DEFAULT 0 COMMENT '学科竞赛与科研得分',
    -- 扣分事项
    exam_cheat_count INT DEFAULT 0 COMMENT '考试违纪作弊次数',
    class_violation_count INT DEFAULT 0 COMMENT '课堂违纪次数',
    academic_deduction DECIMAL(5,2) DEFAULT 0 COMMENT '学业扣分合计',
    -- 汇总
    total_score DECIMAL(5,2) DEFAULT 0 COMMENT '学业素质总分',
    remark TEXT COMMENT '备注',
    evaluator_id BIGINT COMMENT '评价人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student (student_id),
    INDEX idx_semester (semester)
) COMMENT '学业素质评价表';

-- 体育素质评价表
CREATE TABLE physical_evaluation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生信息ID',
    semester VARCHAR(20) NOT NULL COMMENT '学期',
    -- 体质健康
    fitness_test_score DECIMAL(5,2) DEFAULT 0 COMMENT '体质健康测试成绩',
    is_exempt TINYINT DEFAULT 0 COMMENT '是否免测：0-否，1-是',
    fitness_score DECIMAL(5,2) DEFAULT 0 COMMENT '体质健康得分',
    -- 体育课与日常锻炼
    pe_course_score DECIMAL(5,2) DEFAULT 0 COMMENT '体育课成绩',
    sports_team_member TINYINT DEFAULT 0 COMMENT '是否校/院运动队成员',
    sports_activity_count INT DEFAULT 0 COMMENT '体育活动参与次数',
    daily_exercise_count INT DEFAULT 0 COMMENT '日常锻炼打卡次数',
    exercise_score DECIMAL(5,2) DEFAULT 0 COMMENT '体育课与锻炼得分',
    -- 体育竞赛
    sports_competition_count INT DEFAULT 0 COMMENT '体育竞赛参赛次数',
    sports_highest_level VARCHAR(20) COMMENT '竞赛最高级别',
    sports_best_rank VARCHAR(20) COMMENT '最好名次',
    broke_record TINYINT DEFAULT 0 COMMENT '是否破纪录',
    sports_competition_score DECIMAL(5,2) DEFAULT 0 COMMENT '体育竞赛得分',
    -- 扣分事项
    absent_sports_count INT DEFAULT 0 COMMENT '无故缺席体育活动次数',
    physical_deduction DECIMAL(5,2) DEFAULT 0 COMMENT '体育扣分合计',
    -- 汇总
    total_score DECIMAL(5,2) DEFAULT 0 COMMENT '体育素质总分',
    remark TEXT COMMENT '备注',
    evaluator_id BIGINT COMMENT '评价人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student (student_id),
    INDEX idx_semester (semester)
) COMMENT '体育素质评价表';

-- 艺术素质评价表
CREATE TABLE art_evaluation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生信息ID',
    semester VARCHAR(20) NOT NULL COMMENT '学期',
    -- 美育课程与活动
    art_course_count INT DEFAULT 0 COMMENT '美育选修课程门数',
    art_course_avg_score DECIMAL(5,2) DEFAULT 0 COMMENT '美育课程平均成绩',
    art_group_member TINYINT DEFAULT 0 COMMENT '是否艺术团成员',
    art_activity_count INT DEFAULT 0 COMMENT '美育/文艺活动参与次数',
    art_activity_score DECIMAL(5,2) DEFAULT 0 COMMENT '美育课程与活动得分',
    -- 文艺竞赛
    art_competition_count INT DEFAULT 0 COMMENT '文艺竞赛参赛次数',
    art_competition_category VARCHAR(100) COMMENT '竞赛类别(美术/书法/音乐/舞蹈/征文/设计/视频等)',
    art_highest_level VARCHAR(20) COMMENT '竞赛最高级别',
    art_competition_score DECIMAL(5,2) DEFAULT 0 COMMENT '文艺竞赛得分',
    -- 扣分事项
    absent_art_count INT DEFAULT 0 COMMENT '无故缺席美育活动次数',
    art_deduction DECIMAL(5,2) DEFAULT 0 COMMENT '艺术扣分合计',
    -- 汇总
    total_score DECIMAL(5,2) DEFAULT 0 COMMENT '艺术素质总分',
    remark TEXT COMMENT '备注',
    evaluator_id BIGINT COMMENT '评价人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student (student_id),
    INDEX idx_semester (semester)
) COMMENT '艺术素质评价表';

-- 实践素质评价表
CREATE TABLE practice_evaluation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生信息ID',
    semester VARCHAR(20) NOT NULL COMMENT '学期',
    -- 日常劳动
    public_labor_count INT DEFAULT 0 COMMENT '公益/义务劳动参与次数',
    dorm_hygiene_pass INT DEFAULT 0 COMMENT '宿舍卫生合格次数',
    dorm_hygiene_fail INT DEFAULT 0 COMMENT '宿舍卫生不合格次数',
    civilized_dorm TINYINT DEFAULT 0 COMMENT '是否文明寝室',
    daily_labor_score DECIMAL(5,2) DEFAULT 0 COMMENT '日常劳动得分',
    -- 劳动教育课程
    labor_course_count INT DEFAULT 0 COMMENT '劳动教育课程门数',
    labor_course_score DECIMAL(5,2) DEFAULT 0 COMMENT '劳动教育课程得分',
    -- 社会实践与志愿服务
    volunteer_hours DECIMAL(6,1) DEFAULT 0 COMMENT '志愿服务时长(小时)',
    social_practice_count INT DEFAULT 0 COMMENT '社会实践活动次数',
    study_style_activity_count INT DEFAULT 0 COMMENT '学风建设活动参与次数',
    volunteer_score DECIMAL(5,2) DEFAULT 0 COMMENT '社会实践与志愿服务得分',
    -- 实践竞赛
    practice_competition_count INT DEFAULT 0 COMMENT '实践类竞赛获奖数',
    practice_highest_level VARCHAR(20) COMMENT '竞赛最高级别',
    practice_competition_score DECIMAL(5,2) DEFAULT 0 COMMENT '实践竞赛得分',
    -- 扣分事项
    waste_count INT DEFAULT 0 COMMENT '铺张浪费行为次数',
    dirty_dorm_count INT DEFAULT 0 COMMENT '脏乱差寝室通报次数',
    practice_deduction DECIMAL(5,2) DEFAULT 0 COMMENT '实践扣分合计',
    -- 汇总
    total_score DECIMAL(5,2) DEFAULT 0 COMMENT '实践素质总分',
    remark TEXT COMMENT '备注',
    evaluator_id BIGINT COMMENT '评价人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_student (student_id),
    INDEX idx_semester (semester)
) COMMENT '实践素质评价表';

-- 综合素质评价汇总表
CREATE TABLE comprehensive_evaluation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL COMMENT '学生信息ID',
    semester VARCHAR(20) NOT NULL COMMENT '学期',
    moral_score DECIMAL(5,2) DEFAULT 0 COMMENT '品德素质得分',
    academic_score DECIMAL(5,2) DEFAULT 0 COMMENT '学业素质得分',
    physical_score DECIMAL(5,2) DEFAULT 0 COMMENT '体育素质得分',
    art_score DECIMAL(5,2) DEFAULT 0 COMMENT '艺术素质得分',
    practice_score DECIMAL(5,2) DEFAULT 0 COMMENT '实践素质得分',
    total_score DECIMAL(5,2) DEFAULT 0 COMMENT '综合总分',
    cluster_label INT COMMENT '聚类标签',
    cluster_name VARCHAR(50) COMMENT '聚类类别名称',
    suggestion TEXT COMMENT '个性化建议',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_student_semester (student_id, semester),
    INDEX idx_cluster (cluster_label)
) COMMENT '综合素质评价汇总表';

-- 聚类分析结果表
CREATE TABLE cluster_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    semester VARCHAR(20) NOT NULL COMMENT '学期',
    cluster_count INT NOT NULL COMMENT '聚类数量',
    silhouette_score DECIMAL(5,4) COMMENT '轮廓系数',
    cluster_label INT NOT NULL COMMENT '聚类标签',
    cluster_name VARCHAR(50) COMMENT '聚类名称',
    student_count INT DEFAULT 0 COMMENT '该类学生数量',
    avg_moral DECIMAL(5,2) DEFAULT 0,
    avg_academic DECIMAL(5,2) DEFAULT 0,
    avg_physical DECIMAL(5,2) DEFAULT 0,
    avg_art DECIMAL(5,2) DEFAULT 0,
    avg_practice DECIMAL(5,2) DEFAULT 0,
    description TEXT COMMENT '聚类描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_semester (semester)
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
