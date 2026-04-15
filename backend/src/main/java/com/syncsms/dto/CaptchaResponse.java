package com.syncsms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @description 图形验证码返回（base64 png）
 */
@Data
@AllArgsConstructor
public class CaptchaResponse {
    private String captchaId;
    private String imageBase64;
}

