package com.syncsms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * @description 设备心跳上报（电量/充电状态）
 */
@Data
public class DeviceHeartbeatRequest {

    /** 0-100 */
    @Min(0)
    @Max(100)
    private Integer batteryPercent;

    /** 是否在充电 */
    private Boolean charging;
}

