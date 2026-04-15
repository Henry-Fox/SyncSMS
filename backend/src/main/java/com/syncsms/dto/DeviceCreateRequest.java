package com.syncsms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @description 创建设备请求
 */
@Data
public class DeviceCreateRequest {

    @NotBlank(message = "设备名称不能为空")
    private String deviceName;
}
