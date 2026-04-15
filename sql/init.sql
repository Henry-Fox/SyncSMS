-- SyncSMS 数据库初始化脚本
-- 适用于 MySQL 8.0

CREATE DATABASE IF NOT EXISTS sync_sms DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE sync_sms;

-- 系统用户表
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '登录用户名',
    password    VARCHAR(255) NOT NULL COMMENT '密码（BCrypt 加密）',
    nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    role        VARCHAR(20)  NOT NULL DEFAULT 'viewer' COMMENT '角色：admin / viewer',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB COMMENT='系统用户';

-- 设备表
CREATE TABLE device (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    device_name   VARCHAR(100) NOT NULL COMMENT '设备名称',
    device_key    VARCHAR(64)  NOT NULL COMMENT '设备密钥（UUID）',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    last_sync_at  DATETIME     DEFAULT NULL COMMENT '最后同步时间',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_key (device_key)
) ENGINE=InnoDB COMMENT='设备';

-- 短信消息表
CREATE TABLE sms_message (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    device_id   BIGINT       NOT NULL COMMENT '设备ID',
    sender      VARCHAR(50)  NOT NULL COMMENT '发送人号码',
    content     TEXT         NOT NULL COMMENT '短信内容',
    sms_time    DATETIME     NOT NULL COMMENT '短信接收时间（手机端）',
    synced_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '同步到服务器时间',
    is_read     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读 1-已读',
    PRIMARY KEY (id),
    KEY idx_device_id (device_id),
    KEY idx_sms_time (sms_time),
    KEY idx_sender (sender),
    CONSTRAINT fk_sms_device FOREIGN KEY (device_id) REFERENCES device (id)
) ENGINE=InnoDB COMMENT='短信消息';

-- 初始管理员账号（密码: admin123，BCrypt 加密）
INSERT INTO sys_user (username, password, nickname, role)
VALUES ('admin', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36PQm4gyhR9VjKNSH4b.Kcu', '管理员', 'admin');
