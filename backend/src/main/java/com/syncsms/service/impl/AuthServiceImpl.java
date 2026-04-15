package com.syncsms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syncsms.dto.CaptchaResponse;
import com.syncsms.dto.DeviceAuthRequest;
import com.syncsms.dto.DeviceHeartbeatRequest;
import com.syncsms.dto.LoginRequest;
import com.syncsms.dto.LoginResponse;
import com.syncsms.entity.Device;
import com.syncsms.entity.SysUser;
import com.syncsms.mapper.DeviceMapper;
import com.syncsms.mapper.SysUserMapper;
import com.syncsms.security.CaptchaGenerator;
import com.syncsms.security.JwtUtil;
import com.syncsms.security.LoginSecurityService;
import com.syncsms.service.AuthService;
import com.syncsms.service.AuthAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;

/**
 * @description 认证服务实现
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final DeviceMapper deviceMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginSecurityService loginSecurityService;
    private final AuthAuditService authAuditService;

    @Override
    public LoginResponse login(LoginRequest request, HttpServletRequest http) {
        String ip = getClientIp(http);
        String ua = http.getHeader("User-Agent");

        if (loginSecurityService.isLocked(request.getUsername(), ip)) {
            authAuditService.logLoginFail(request.getUsername(), ip, ua);
            throw new RuntimeException("登录失败次数过多，请稍后再试");
        }

        boolean needCaptcha = loginSecurityService.requireCaptcha(request.getUsername(), ip);
        if (needCaptcha) {
            boolean ok = loginSecurityService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode());
            if (!ok) {
                loginSecurityService.recordFail(request.getUsername(), ip);
                authAuditService.logLoginFail(request.getUsername(), ip, ua);
                throw new RuntimeException("请先通过验证码验证");
            }
        }

        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));

        if (user == null) {
            loginSecurityService.recordFail(request.getUsername(), ip);
            authAuditService.logLoginFail(request.getUsername(), ip, ua);
            throw new RuntimeException("用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            authAuditService.logLoginFail(request.getUsername(), ip, ua);
            throw new RuntimeException("账号已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginSecurityService.recordFail(request.getUsername(), ip);
            authAuditService.logLoginFail(request.getUsername(), ip, ua);
            throw new RuntimeException("用户名或密码错误");
        }

        loginSecurityService.recordSuccess(request.getUsername(), ip);
        authAuditService.logLoginSuccess(user.getId(), user.getUsername(), ip, ua);
        String token = jwtUtil.generateUserToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getUsername(), user.getNickname(), user.getRole());
    }

    @Override
    public CaptchaResponse getCaptcha() {
        String code = CaptchaGenerator.randomCode(5);
        String captchaId = loginSecurityService.createCaptcha(code, 5 * 60_000L);
        String base64 = CaptchaGenerator.renderPngBase64(code);
        return new CaptchaResponse(captchaId, base64);
    }

    @Override
    public void logout(Authentication auth, HttpServletRequest http) {
        if (auth == null || auth.getPrincipal() == null) return;
        // principal 里存的是 userId（见 JwtAuthenticationFilter）
        Long userId = (Long) auth.getPrincipal();
        String ip = getClientIp(http);
        String ua = http.getHeader("User-Agent");
        SysUser user = userMapper.selectById(userId);
        String username = user != null ? user.getUsername() : String.valueOf(userId);
        authAuditService.logLogout(userId, username, ip, ua);
    }

    @Override
    public String deviceAuth(DeviceAuthRequest request) {
        Device device = deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>().eq(Device::getDeviceKey, request.getDeviceKey()));

        if (device == null) {
            throw new RuntimeException("设备密钥无效");
        }
        if (device.getStatus() != 1) {
            throw new RuntimeException("设备已被禁用");
        }

        return jwtUtil.generateDeviceToken(device.getId(), device.getDeviceKey());
    }

    @Override
    public void deviceHeartbeat(Long deviceId, DeviceHeartbeatRequest request) {
        if (deviceId == null) return;
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) {
            throw new RuntimeException("设备不存在");
        }
        if (device.getStatus() != 1) {
            throw new RuntimeException("设备已被禁用");
        }

        Device patch = new Device();
        patch.setId(deviceId);
        patch.setLastSeenAt(LocalDateTime.now());
        patch.setBatteryPercent(request.getBatteryPercent());
        patch.setIsCharging(request.getCharging() == null ? null : (request.getCharging() ? 1 : 0));
        deviceMapper.updateById(patch);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
