-- 考勤管理系统数据库初始化脚本
-- 创建数据库
DROP DATABASE IF EXISTS attendance_system;
CREATE DATABASE IF NOT EXISTS attendance_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE attendance_system;

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS abnormal_remind_log;
DROP TABLE IF EXISTS user_leave_balance;
DROP TABLE IF EXISTS leave_quota_rule;
DROP TABLE IF EXISTS overtime_record;
DROP TABLE IF EXISTS notice_config;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS appeal;
DROP TABLE IF EXISTS fieldwork;
DROP TABLE IF EXISTS approval_record;
DROP TABLE IF EXISTS approval_flow;
DROP TABLE IF EXISTS leave_record;
DROP TABLE IF EXISTS attendance;
DROP TABLE IF EXISTS attendance_rule;
DROP TABLE IF EXISTS attendance_group;
DROP TABLE IF EXISTS user;
DROP TABLE IF EXISTS department;
SET FOREIGN_KEY_CHECKS = 1;

-- 考勤组表（可绑定多个部门，共享同一班次规则）
CREATE TABLE IF NOT EXISTS attendance_group (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '考勤组ID',
    name VARCHAR(100) NOT NULL COMMENT '考勤组名称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤组表';

-- 部门表
CREATE TABLE IF NOT EXISTS department (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
    name VARCHAR(100) NOT NULL COMMENT '部门名称',
    manager_id BIGINT COMMENT '负责人ID',
    attendance_group_id BIGINT NULL COMMENT '关联考勤组ID(可为空，空则默认使用部门ID作为考勤组)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    employee_no VARCHAR(50) NOT NULL COMMENT '工号',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号',
    password VARCHAR(255) NOT NULL COMMENT '密码(MD5+salt)',
    salt VARCHAR(32) NOT NULL COMMENT '密码盐值',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    department_id BIGINT COMMENT '部门ID',
    attendance_group_id BIGINT NULL COMMENT '考勤组ID(可为空，空则按部门自动匹配)',
    hire_date DATE NULL COMMENT '入职日期',
    resign_date DATE NULL COMMENT '离职日期',
    position VARCHAR(50) COMMENT '岗位',
    phone VARCHAR(20) COMMENT '联系方式',
    role_type TINYINT DEFAULT 0 COMMENT '角色类型：0-普通员工，1-管理员，2-部门主管',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    register_status TINYINT DEFAULT 1 COMMENT '注册状态：0-待审核，1-通过，2-驳回',
    login_fail_count INT DEFAULT 0 COMMENT '连续登录失败次数',
    lock_until DATETIME NULL COMMENT '锁定截止时间',
    openid VARCHAR(64) NULL COMMENT '微信openid',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (department_id) REFERENCES department(id) ON DELETE SET NULL,
    FOREIGN KEY (attendance_group_id) REFERENCES attendance_group(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 考勤规则表
CREATE TABLE IF NOT EXISTS attendance_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '规则ID',
    shift_name VARCHAR(100) DEFAULT '默认班次' COMMENT '班次名称',
    department_id BIGINT NULL COMMENT '关联考勤组 ID（可绑定多个部门，实现多部门共享同一班次规则），支持管理员为不同考勤组配置不同班次，员工通过attendance_group_id关联对应班次规则',
    check_in_time VARCHAR(10) DEFAULT '09:00' COMMENT '上班时间',
    check_out_time VARCHAR(10) DEFAULT '18:00' COMMENT '下班时间',
    early_check_in_minutes INT DEFAULT 120 COMMENT '早签到允许时间(分钟，提前可打卡)',
    late_threshold INT DEFAULT 30 COMMENT '迟到阈值(分钟)',
    early_threshold INT DEFAULT 30 COMMENT '早退阈值(分钟)',
    late_check_out_minutes INT DEFAULT 240 COMMENT '晚签退允许时间(分钟，延后可打卡)',
    address VARCHAR(255) COMMENT '公司地址',
    longitude DECIMAL(10, 6) COMMENT '经度',
    latitude DECIMAL(10, 6) COMMENT '纬度',
    check_in_range INT DEFAULT 500 COMMENT '打卡范围(米)',
    photo_verify TINYINT DEFAULT 0 COMMENT '是否开启照片验证：0-否，1-是',
    fieldwork_required TINYINT DEFAULT 0 COMMENT '外勤报备是否必填：0-否，1-是',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用：0-否，1-是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤规则表';

-- 考勤记录表
CREATE TABLE IF NOT EXISTS attendance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '员工ID',
    attendance_date DATE NOT NULL COMMENT '考勤日期',
    check_in_time VARCHAR(10) COMMENT '签到时间',
    check_out_time VARCHAR(10) COMMENT '签退时间',
    check_in_status TINYINT COMMENT '签到状态：0-正常，1-迟到',
    check_out_status TINYINT COMMENT '签退状态：0-正常，2-早退',
    check_in_location VARCHAR(255) COMMENT '签到定位',
    check_out_location VARCHAR(255) COMMENT '签退定位',
    check_in_photo VARCHAR(500) COMMENT '签到照片路径',
    check_out_photo VARCHAR(500) COMMENT '签退照片路径',
    status TINYINT DEFAULT 0 COMMENT '考勤状态：0-正常，1-异常，2-请假，3-旷工',
    is_abnormal TINYINT DEFAULT 0 COMMENT '是否异常：0-否，1-是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_date (user_id, attendance_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤记录表';

-- 请假记录表
CREATE TABLE IF NOT EXISTS leave_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '员工ID',
    leave_type TINYINT NOT NULL COMMENT '请假类型：1-事假，2-病假，3-年假，4-婚假，5-产假，6-丧假',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    days DECIMAL(5, 1) NOT NULL COMMENT '请假天数',
    reason VARCHAR(500) NOT NULL COMMENT '请假事由',
    images VARCHAR(1000) COMMENT '证明材料(JSON数组)',
    status TINYINT DEFAULT 0 COMMENT '审批状态：0-审批中，1-已通过，2-已拒绝，3-已撤销',
    current_approver_id BIGINT COMMENT '当前审批人ID',
    current_stage INT DEFAULT 1 COMMENT '当前审批阶段：1-部门主管，2-管理员',
    total_stage INT DEFAULT 1 COMMENT '总审批阶段数：1-单级，2-双级',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假记录表';

-- 加班记录表
CREATE TABLE IF NOT EXISTS overtime_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '员工ID',
    overtime_type TINYINT NOT NULL COMMENT '加班类型：1-工作日，2-周末，3-节假日',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    hours DECIMAL(6, 2) DEFAULT 0 COMMENT '加班工时(小时)',
    reason VARCHAR(500) NOT NULL COMMENT '加班事由',
    images VARCHAR(1000) COMMENT '相关凭证(JSON数组)',
    status TINYINT DEFAULT 0 COMMENT '审批状态：0-待主管初审，1-主管已通过待管理员终审，2-已拒绝，3-已通过，4-已撤销',
    current_approver_id BIGINT COMMENT '当前审批人ID',
    current_stage INT DEFAULT 1 COMMENT '当前审批阶段：1-部门主管，2-管理员',
    total_stage INT DEFAULT 2 COMMENT '总审批阶段数：固定2级',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加班记录表';

-- 假期额度规则表（年度）
CREATE TABLE IF NOT EXISTS leave_quota_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '规则ID',
    leave_type TINYINT NOT NULL COMMENT '请假类型：1-事假，2-病假，3-年假，4-婚假，5-产假，6-丧假',
    year INT NOT NULL COMMENT '年度',
    total_days DECIMAL(6, 1) NOT NULL COMMENT '年度额度(天)',
    position VARCHAR(50) NULL COMMENT '岗位(可选)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='假期额度规则表';

-- 用户假期余额表
CREATE TABLE IF NOT EXISTS user_leave_balance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '余额ID',
    user_id BIGINT NOT NULL COMMENT '员工ID',
    leave_type TINYINT NOT NULL COMMENT '请假类型',
    year INT NOT NULL COMMENT '年度',
    total_days DECIMAL(6, 1) NOT NULL COMMENT '总额度(天)',
    used_days DECIMAL(6, 1) NOT NULL DEFAULT 0 COMMENT '已用(天)',
    remaining_days DECIMAL(6, 1) NOT NULL COMMENT '剩余(天)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_leave_year (user_id, leave_type, year),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户假期余额表';

-- 考勤异常提醒发送记录（防重复）
CREATE TABLE IF NOT EXISTS abnormal_remind_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '员工ID',
    attendance_date DATE NOT NULL COMMENT '考勤日期',
    remind_type VARCHAR(30) NOT NULL COMMENT '提醒类型：miss_check_in/miss_check_out',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_date_type (user_id, attendance_date, remind_type),
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤异常提醒记录表';

-- 审批流程配置表
CREATE TABLE IF NOT EXISTS approval_flow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '流程ID',
    record_type VARCHAR(20) DEFAULT 'leave' COMMENT '流程类型：leave-请假',
    leave_type TINYINT NULL COMMENT '请假类型(可选)',
    min_days DECIMAL(5, 1) NOT NULL COMMENT '最小天数',
    max_days DECIMAL(5, 1) NOT NULL COMMENT '最大天数',
    need_admin TINYINT DEFAULT 0 COMMENT '是否需要管理员终审：0-否，1-是',
    approver_id BIGINT NULL COMMENT '管理员终审审批人ID(可选)',
    level INT DEFAULT 1 COMMENT '审批层级',
    department_id BIGINT COMMENT '部门ID(可选)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (approver_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流程配置表';

-- 审批记录表
CREATE TABLE IF NOT EXISTS approval_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    record_type VARCHAR(20) NOT NULL COMMENT '记录类型：leave-请假，fieldwork-外勤，appeal-申诉',
    record_id BIGINT NOT NULL COMMENT '关联记录ID',
    approver_id BIGINT NOT NULL COMMENT '审批人ID',
    status TINYINT DEFAULT 0 COMMENT '审批状态：0-待审批，1-已通过，2-已拒绝',
    opinion VARCHAR(500) COMMENT '审批意见',
    approval_time DATETIME COMMENT '审批时间',
    level INT DEFAULT 1 COMMENT '审批层级',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (approver_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批记录表';

-- 外勤记录表
CREATE TABLE IF NOT EXISTS fieldwork (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '员工ID',
    fieldwork_date DATE NOT NULL COMMENT '外勤日期',
    location VARCHAR(255) NOT NULL COMMENT '外勤地点',
    reason VARCHAR(500) NOT NULL COMMENT '外勤事由',
    images VARCHAR(1000) COMMENT '相关凭证(JSON数组)',
    status TINYINT DEFAULT 0 COMMENT '审批状态：0-审批中，1-已通过，2-已拒绝',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外勤记录表';

-- 异常申诉表
CREATE TABLE IF NOT EXISTS appeal (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '员工ID',
    attendance_date DATE NOT NULL COMMENT '考勤日期',
    appeal_type TINYINT NOT NULL COMMENT '申诉类型：1-漏签到，2-漏签退，3-定位失败，4-设备故障，5-其他',
    reason VARCHAR(500) NOT NULL COMMENT '申诉原因',
    images VARCHAR(1000) COMMENT '证明材料(JSON数组)',
    status TINYINT DEFAULT 0 COMMENT '审核状态：0-审核中，1-已通过，2-已拒绝',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常申诉表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    operator_id BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(50) COMMENT '操作人姓名',
    action VARCHAR(100) NOT NULL COMMENT '操作类型',
    detail VARCHAR(500) COMMENT '操作详情',
    ip VARCHAR(50) COMMENT 'IP地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 消息通知配置表
CREATE TABLE IF NOT EXISTS notice_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    attendance_alert TINYINT DEFAULT 1 COMMENT '考勤异常提醒：0-关闭，1-开启',
    approval_result TINYINT DEFAULT 1 COMMENT '审批结果推送：0-关闭，1-开启',
    pending_approval TINYINT DEFAULT 1 COMMENT '待审批提醒：0-关闭，1-开启',
    check_in_reminder TINYINT DEFAULT 0 COMMENT '打卡提醒：0-关闭，1-开启',
    morning_reminder_time VARCHAR(10) DEFAULT '08:50' COMMENT '上班提醒时间',
    evening_reminder_time VARCHAR(10) DEFAULT '18:00' COMMENT '下班提醒时间',
    miss_check_in_after_minutes INT DEFAULT 30 COMMENT '上班后未打卡提醒(分钟)',
    miss_check_out_after_minutes INT DEFAULT 30 COMMENT '下班后未签退提醒(分钟)',
    late_remind TINYINT DEFAULT 1 COMMENT '迟到提醒：0-关闭，1-开启',
    early_remind TINYINT DEFAULT 1 COMMENT '早退提醒：0-关闭，1-开启',
    miss_check_in_remind TINYINT DEFAULT 1 COMMENT '未打卡提醒：0-关闭，1-开启',
    miss_check_out_remind TINYINT DEFAULT 1 COMMENT '未签退提醒：0-关闭，1-开启',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知配置表';

-- 初始化数据
-- 插入默认管理员账号（密码：123456，使用MD5+salt加密）
INSERT INTO department (name) VALUES ('总经办');
INSERT INTO attendance_group (name) VALUES ('默认考勤组');
UPDATE department SET attendance_group_id = 1 WHERE id = 1;
INSERT INTO user (employee_no, username, password, salt, name, department_id, role_type, status, register_status) 
VALUES ('ADMIN001', 'admin', 'd47136406e7803daa65ce764b19e6890', 'admin_salt', '系统管理员', 1, 1, 1, 1);
UPDATE user SET attendance_group_id = 1 WHERE id = 1;

-- 插入默认考勤规则
INSERT INTO attendance_rule (check_in_time, check_out_time, late_threshold, early_threshold, check_in_range) 
VALUES ('09:00', '18:00', 30, 30, 500);

-- 插入默认通知配置
INSERT INTO notice_config (attendance_alert, approval_result, pending_approval, check_in_reminder) 
VALUES (1, 1, 1, 0);
