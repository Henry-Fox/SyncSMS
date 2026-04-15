package com.syncsms.controller;

import com.syncsms.common.Result;
import com.syncsms.dto.CaptchaResponse;
import com.syncsms.dto.DeviceAuthRequest;
import com.syncsms.dto.DeviceHeartbeatRequest;
import com.syncsms.dto.LoginRequest;
import com.syncsms.dto.LoginResponse;
import com.syncsms.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @description 认证相关接口（用户登录、设备认证）
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * @description 用户登录
     * @param request 登录请求（用户名 + 密码）
     * @return 包含 JWT Token 的登录响应
     */
    @PostMapping("/user/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return Result.ok(authService.login(request, http));
    }

    /**
     * @description 获取验证码（当多次登录失败触发后使用）
     * @return captchaId + base64 png
     */
    @GetMapping("/user/captcha")
    public Result<CaptchaResponse> captcha() {
        return Result.ok(authService.getCaptcha());
    }

    /**
     * @description 用户注销（用于审计记录；JWT 无状态，服务端不保存 session）
     */
    @PostMapping("/user/logout")
    public Result<Void> logout(Authentication auth, HttpServletRequest http) {
        authService.logout(auth, http);
        return Result.ok();
    }

    /**
     * @description 设备认证
     * @param request 设备密钥
     * @return 包含设备 JWT Token
     */
    @PostMapping("/device/auth")
    public Result<String> deviceAuth(@Valid @RequestBody DeviceAuthRequest request) {
        return Result.ok(authService.deviceAuth(request));
    }

    /**
     * @description 设备心跳上报（在线状态/电量）
     * @param request 心跳数据
     * @param auth 设备认证信息（从 JWT 中提取 deviceId）
     * @return 成功标识
     */
    @PostMapping("/device/heartbeat")
    public Result<String> heartbeat(@Valid @RequestBody DeviceHeartbeatRequest request,
                                    Authentication auth) {
        // 当前实现：由 SmsService 在批量上传时更新 lastSyncAt；这里更新设备在线/电量字段
        // 具体更新逻辑放在 DeviceService 中实现（避免 controller 直接操作 mapper）
        authService.deviceHeartbeat((Long) auth.getPrincipal(), request);
        return Result.ok("alive");
    }
}
