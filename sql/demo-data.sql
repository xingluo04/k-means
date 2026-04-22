USE student_quality_evaluation;

INSERT INTO sys_user (username, password, real_name, role, phone, email, gender, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'admin', '13800000001', 'admin@school.edu.cn', 1, 1),
('teacher1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '张明华', 'teacher', '13800000002', 'zhangmh@school.edu.cn', 1, 1),
('teacher2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '李秀英', 'teacher', '13800000003', 'lixiuying@school.edu.cn', 2, 1),
('student1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王小明', 'student', '13800000004', 'wangxm@school.edu.cn', 1, 1),
('student2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '刘思琪', 'student', '13800000005', 'liusq@school.edu.cn', 2, 1),
('student3', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '陈浩然', 'student', '13800000006', 'chenhr@school.edu.cn', 1, 1),
('student4', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '赵雨萱', 'student', '13800000007', 'zhaoyx@school.edu.cn', 2, 1),
('student5', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '孙博文', 'student', '13800000008', 'sunbw@school.edu.cn', 1, 1),
('student6', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '周雅琴', 'student', '13800000009', 'zhouyq@school.edu.cn', 2, 1),
('student7', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '吴天宇', 'student', '13800000010', 'wuty@school.edu.cn', 1, 1),
('student8', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '郑美玲', 'student', '13800000011', 'zhengml@school.edu.cn', 2, 1),
('parent1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王建国', 'parent', '13800000012', 'wangjg@163.com', 1, 1),
('parent2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '刘芳', 'parent', '13800000013', 'liufang@163.com', 2, 1);

INSERT INTO class_info (class_name, grade, department, teacher_id) VALUES
('计算机科学2023级1班', '2023级', '计算机科学与技术学院', 2),
('计算机科学2023级2班', '2023级', '计算机科学与技术学院', 3),
('软件工程2023级1班', '2023级', '软件工程学院', 2);

INSERT INTO student_info (user_id, student_no, class_id, parent_id, enrollment_year) VALUES
(4, '2023010001', 1, 12, 2023),
(5, '2023010002', 1, 13, 2023),
(6, '2023010003', 1, NULL, 2023),
(7, '2023010004', 2, NULL, 2023),
(8, '2023010005', 2, NULL, 2023),
(9, '2023010006', 2, NULL, 2023),
(10, '2023010007', 3, NULL, 2023),
(11, '2023010008', 3, NULL, 2023);

INSERT INTO sys_notice (title, content, type, publisher_id, status) VALUES
('2024-2025学年第二学期综合素质评价通知', '各位同学：\n\n2024-2025学年第二学期综合素质评价工作即将开始，请各位同学及时登录系统查看并完善个人信息。\n\n教务处', 2, 1, 1),
('系统升级维护通知', '系统将于2025年3月1日凌晨2:00-6:00进行升级维护，届时系统将暂停服务。\n\n技术支持部', 1, 1, 1);

INSERT INTO sys_log (user_id, username, operation, method, ip) VALUES
(1, 'admin', '用户登录', 'POST /api/auth/login', '192.168.1.100'),
(2, 'teacher1', '录入品德评价', 'POST /api/evaluation/moral', '192.168.1.101'),
(4, 'student1', '查看综合评价', 'GET /api/evaluation/comprehensive/my', '192.168.1.102');
