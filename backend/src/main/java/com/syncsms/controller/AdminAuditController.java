package com.syncsms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syncsms.common.PageResult;
import com.syncsms.common.Result;
import com.syncsms.entity.AuthAuditLog;
import com.syncsms.mapper.AuthAuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description 管理员查看登录/注销审计日志
 */
@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AuthAuditLogMapper auditLogMapper;

    @GetMapping("/auth-log")
    public Result<PageResult<AuthAuditLog>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action
    ) {
        Page<AuthAuditLog> p = new Page<>(page, size);
        LambdaQueryWrapper<AuthAuditLog> w = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) {
            w.like(AuthAuditLog::getUsername, username);
        }
        if (action != null && !action.isBlank()) {
            w.eq(AuthAuditLog::getAction, action);
        }
        w.orderByDesc(AuthAuditLog::getCreatedAt);
        Page<AuthAuditLog> r = auditLogMapper.selectPage(p, w);
        return Result.ok(new PageResult<>(r.getTotal(), r.getCurrent(), r.getSize(), r.getRecords()));
    }
}

