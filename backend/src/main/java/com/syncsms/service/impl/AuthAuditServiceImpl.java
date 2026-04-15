package com.syncsms.service.impl;

import com.syncsms.entity.AuthAuditLog;
import com.syncsms.mapper.AuthAuditLogMapper;
import com.syncsms.service.AuthAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @description 审计日志服务实现
 */
@Service
@RequiredArgsConstructor
public class AuthAuditServiceImpl implements AuthAuditService {

    private final AuthAuditLogMapper auditLogMapper;

    @Override
    public void logLoginSuccess(Long userId, String username, String ip, String userAgent) {
        insert(userId, username, "login_success", ip, userAgent);
    }

    @Override
    public void logLoginFail(String username, String ip, String userAgent) {
        insert(null, username, "login_fail", ip, userAgent);
    }

    @Override
    public void logLogout(Long userId, String username, String ip, String userAgent) {
        insert(userId, username, "logout", ip, userAgent);
    }

    private void insert(Long userId, String username, String action, String ip, String userAgent) {
        AuthAuditLog log = new AuthAuditLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setAction(action);
        log.setIp(ip);
        log.setUserAgent(userAgent != null && userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent);
        log.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(log);
    }
}

