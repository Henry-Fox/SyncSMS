package com.syncsms.controller;

import com.syncsms.common.Result;
import com.syncsms.dto.DeviceStatusResponse;
import com.syncsms.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @description 设备状态接口（给 Web 端展示用，不包含密钥）
 */
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * @description 获取设备状态列表（任何已登录用户可访问）
     */
    @GetMapping("/status")
    public Result<List<DeviceStatusResponse>> status() {
        return Result.ok(deviceService.listStatus());
    }
}

