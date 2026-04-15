package com.syncsms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @description 设备认证请求
 */
@Data
public class DeviceAuthRequest {

    @NotBlank(message = "设备密钥不能为空")
    private String deviceKey;
}
