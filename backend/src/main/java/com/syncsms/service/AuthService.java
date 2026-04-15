package com.syncsms.service;

import com.syncsms.dto.DeviceAuthRequest;
import com.syncsms.dto.DeviceHeartbeatRequest;
import com.syncsms.dto.CaptchaResponse;
import com.syncsms.dto.LoginRequest;
import com.syncsms.dto.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

/**
 * @description 认证服务接口
 */
public interface AuthService {

    /**
     * @description 用户登录
     * @param request 登录请求
     * @return 登录响应（含 Token）
     */
    LoginResponse login(LoginRequest request, HttpServletRequest http);

    CaptchaResponse getCaptcha();

    void logout(Authentication auth, HttpServletRequest http);

    /**
     * @description 设备认证
     * @param request 设备认证请求
     * @return 设备 Token
     */
    String deviceAuth(DeviceAuthRequest request);

    /**
     * @description 设备心跳上报（在线/电量）
     * @param deviceId 设备ID（来自 JWT）
     * @param request 心跳数据
     */
    void deviceHeartbeat(Long deviceId, DeviceHeartbeatRequest request);
}
