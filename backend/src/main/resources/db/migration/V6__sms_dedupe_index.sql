-- V6: 清空历史数据并增加去重唯一索引
TRUNCATE TABLE sms_message;
ALTER TABLE sms_message
    ADD UNIQUE KEY uk_device_sender_time (device_id, sender, sms_time);
