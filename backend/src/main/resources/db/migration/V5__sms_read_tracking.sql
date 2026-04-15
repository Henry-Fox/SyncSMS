-- V5: 为 sms_message 增加已读操作追踪字段
ALTER TABLE sms_message
    ADD COLUMN read_by  VARCHAR(50)  DEFAULT NULL COMMENT '标记已读的用户名' AFTER is_read,
    ADD COLUMN read_at  DATETIME     DEFAULT NULL COMMENT '标记已读的时间'   AFTER read_by;
