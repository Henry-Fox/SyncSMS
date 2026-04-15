package com.syncsms.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * @description 更新用户请求（密码可选）
 */
@Data
public class UserUpdateRequest {

    private String password;

    private String nickname;

    @Pattern(regexp = "^(admin|viewer)$", message = "角色只能是 admin 或 viewer")
    private String role;

    private Integer status;
}
