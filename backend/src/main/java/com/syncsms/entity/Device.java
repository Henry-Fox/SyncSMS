package com.syncsms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description 设备实体
 */
@Data
@TableName("device")
public class Device {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String deviceName;

    private String deviceKey;

    /** 0-禁用 1-启用 */
    private Integer status;

    private LocalDateTime lastSyncAt;

    /** 0-100，NULL 表示未知 */
    private Integer batteryPercent;

    /** 0-否 1-是，NULL 表示未知 */
    private Integer isCharging;

    private LocalDateTime lastSeenAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
