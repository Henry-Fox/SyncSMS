package com.syncsms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @description 登录/注销审计日志
 */
@Data
@TableName("auth_audit_log")
public class AuthAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    /**
     * login_success / login_fail / logout
     */
    private String action;

    private String ip;

    private String userAgent;

    private LocalDateTime createdAt;
}

