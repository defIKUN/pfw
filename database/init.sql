-- 创建数据库
CREATE DATABASE IF NOT EXISTS river_detection DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE river_detection;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密）',
    `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色（admin/user）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 图片识别任务表
CREATE TABLE IF NOT EXISTS `image_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `image_path` VARCHAR(500) NOT NULL COMMENT '原图存储路径',
    `result_path` VARCHAR(500) DEFAULT NULL COMMENT '识别结果图存储路径',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0:未开始，1:进行中，2:已完成，3:失败）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片识别任务表';

-- 视频识别任务表
CREATE TABLE IF NOT EXISTS `video_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `video_path` VARCHAR(500) NOT NULL COMMENT '视频存储路径',
    `result_path` VARCHAR(500) DEFAULT NULL COMMENT '识别结果视频存储路径',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0:未开始，1:进行中，2:已完成，3:失败）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频识别任务表';

-- 识别结果表
CREATE TABLE IF NOT EXISTS `detection_result` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id` BIGINT NOT NULL COMMENT '任务ID',
    `task_type` TINYINT NOT NULL COMMENT '任务类型（0:图片，1:视频）',
    `object_class` VARCHAR(50) NOT NULL COMMENT '漂浮物类别',
    `confidence` DECIMAL(5,4) NOT NULL COMMENT '置信度',
    `location` JSON NOT NULL COMMENT '位置信息（边界框坐标）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_task_type` (`task_type`),
    KEY `idx_object_class` (`object_class`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='识别结果表';

-- 多地区监控源表
CREATE TABLE IF NOT EXISTS `monitor_source` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` VARCHAR(100) NOT NULL COMMENT '监控点名称',
    `region` VARCHAR(100) NOT NULL COMMENT '所属地区/河段',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '说明',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态（1:在线，0:离线）',
    `video_task_id` BIGINT DEFAULT NULL COMMENT '关联的视频任务ID',
    `video_path` VARCHAR(500) DEFAULT NULL COMMENT '原始视频路径',
    `result_path` VARCHAR(500) DEFAULT NULL COMMENT '识别后的视频路径',
    `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图地址',
    `latitude` DOUBLE DEFAULT NULL COMMENT '纬度',
    `longitude` DOUBLE DEFAULT NULL COMMENT '经度',
    `summary` JSON DEFAULT NULL COMMENT '识别结果摘要',
    `last_detect_time` DATETIME DEFAULT NULL COMMENT '最近检测时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_region` (`region`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多地区监控源表';

-- 插入默认管理员用户（密码：admin123）
INSERT INTO `user` (`username`, `password`, `role`) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwK8p6/Pa', 'admin');

