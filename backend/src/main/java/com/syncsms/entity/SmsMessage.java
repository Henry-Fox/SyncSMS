package com.syncsms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description 短信消息实体
 */
@Data
@TableName("sms_message")
public class SmsMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long deviceId;

    private String sender;

    private String content;

    /** 短信在手机上的接收时间 */
    private LocalDateTime smsTime;

    /** 同步到服务器的时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime syncedAt;

    /** 0-未读 1-已读 */
    private Integer isRead;

    /** 标记已读的用户名 */
    private String readBy;

    /** 标记已读的时间 */
    private LocalDateTime readAt;
}
