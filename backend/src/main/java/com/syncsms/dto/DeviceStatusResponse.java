package com.syncsms.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description 设备状态（给 Web 端展示用，不包含密钥）
 */
@Data
public class DeviceStatusResponse {

    private Long id;
    private String deviceName;
    private Integer batteryPercent;
    private Integer isCharging;
    private LocalDateTime lastSeenAt;
}

