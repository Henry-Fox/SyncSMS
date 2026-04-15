package com.syncsms.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description 登录安全：失败计数、临时锁定、验证码（内存实现；单机可用）
 */
@Component
public class LoginSecurityService {

    @Value("${syncsms.security.captchaAfterFailures:3}")
    private int captchaAfterFailures;

    @Value("${syncsms.security.lockMinutes:10}")
    private int lockMinutes;

    @Value("${syncsms.security.failureWindowMinutes:15}")
    private int failureWindowMinutes;

    private static class FailState {
        int count;
        long windowStartEpochMs;
        long lockedUntilEpochMs;
    }

    private static class CaptchaState {
        String code;
        long expiresAtEpochMs;
    }

    private final Map<String, FailState> failByKey = new ConcurrentHashMap<>();
    private final Map<String, CaptchaState> captchaById = new ConcurrentHashMap<>();

    public boolean isLocked(String username, String ip) {
        FailState s = failByKey.get(failKey(username, ip));
        return s != null && s.lockedUntilEpochMs > now();
    }

    public boolean requireCaptcha(String username, String ip) {
        FailState s = currentState(username, ip);
        return s.count >= captchaAfterFailures;
    }

    public void recordFail(String username, String ip) {
        FailState s = currentState(username, ip);
        s.count++;
        if (s.count >= captchaAfterFailures + 3) {
            s.lockedUntilEpochMs = now() + lockMinutes * 60_000L;
        }
    }

    public void recordSuccess(String username, String ip) {
        failByKey.remove(failKey(username, ip));
    }

    public String createCaptcha(String code, long ttlMs) {
        String id = UUID.randomUUID().toString().replace("-", "");
        CaptchaState cs = new CaptchaState();
        cs.code = code;
        cs.expiresAtEpochMs = now() + ttlMs;
        captchaById.put(id, cs);
        return id;
    }

    public boolean verifyCaptcha(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null) return false;
        CaptchaState cs = captchaById.remove(captchaId);
        if (cs == null) return false;
        if (cs.expiresAtEpochMs < now()) return false;
        return cs.code.equalsIgnoreCase(captchaCode.trim());
    }

    private FailState currentState(String username, String ip) {
        String key = failKey(username, ip);
        FailState existing = failByKey.get(key);
        long now = now();
        if (existing == null) {
            FailState s = new FailState();
            s.count = 0;
            s.windowStartEpochMs = now;
            failByKey.put(key, s);
            return s;
        }
        if (now - existing.windowStartEpochMs > failureWindowMinutes * 60_000L) {
            existing.count = 0;
            existing.windowStartEpochMs = now;
            existing.lockedUntilEpochMs = 0;
        }
        return existing;
    }

    private String failKey(String username, String ip) {
        return (username == null ? "" : username.toLowerCase()) + "|" + (ip == null ? "" : ip);
    }

    private long now() {
        return Instant.now().toEpochMilli();
    }
}

