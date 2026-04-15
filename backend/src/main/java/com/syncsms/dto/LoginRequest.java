package com.syncsms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @description 用户登录请求
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 验证码ID（当触发验证码策略时必填）
     */
    private String captchaId;

    /**
     * 验证码（当触发验证码策略时必填）
     */
    private String captchaCode;
}
