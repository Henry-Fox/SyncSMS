package com.syncsms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * @description 创建/更新用户请求
 */
@Data
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String nickname;

    @Pattern(regexp = "^(admin|viewer)$", message = "角色只能是 admin 或 viewer")
    private String role = "viewer";
}
