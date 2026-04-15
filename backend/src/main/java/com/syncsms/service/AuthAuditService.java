package com.syncsms.service;

/**
 * @description 审计日志服务
 */
public interface AuthAuditService {

    void logLoginSuccess(Long userId, String username, String ip, String userAgent);

    void logLoginFail(String username, String ip, String userAgent);

    void logLogout(Long userId, String username, String ip, String userAgent);
}

