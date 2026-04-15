package com.syncsms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @description 登录成功返回
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String username;
    private String nickname;
    private String role;
}
