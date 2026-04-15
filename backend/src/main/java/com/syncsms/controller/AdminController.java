package com.syncsms.controller;

import com.syncsms.common.Result;
import com.syncsms.dto.DeviceCreateRequest;
import com.syncsms.dto.UserCreateRequest;
import com.syncsms.dto.UserUpdateRequest;
import com.syncsms.entity.Device;
import com.syncsms.entity.SysUser;
import com.syncsms.service.DeviceService;
import com.syncsms.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description 后台管理接口（仅管理员可访问）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final DeviceService deviceService;

    // ==================== 用户管理 ====================

    /**
     * @description 获取所有用户列表
     * @return 用户列表（密码字段脱敏）
     */
    @GetMapping("/users")
    public Result<List<SysUser>> listUsers() {
        return Result.ok(userService.listAll());
    }

    /**
     * @description 创建新用户
     * @param request 用户信息
     * @return 创建后的用户
     */
    @PostMapping("/users")
    public Result<SysUser> createUser(@Valid @RequestBody UserCreateRequest request) {
        return Result.ok(userService.create(request));
    }

    /**
     * @description 更新用户信息
     * @param id      用户ID
     * @param request 更新内容
     * @return 更新后的用户
     */
    @PutMapping("/users/{id}")
    public Result<SysUser> updateUser(@PathVariable Long id,
                                      @Valid @RequestBody UserUpdateRequest request) {
        return Result.ok(userService.update(id, request));
    }

    /**
     * @description 删除用户
     * @param id 用户ID
     * @return 操作结果
     */
    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }

    // ==================== 设备管理 ====================

    /**
     * @description 获取所有设备列表
     * @return 设备列表
     */
    @GetMapping("/devices")
    public Result<List<Device>> listDevices() {
        return Result.ok(deviceService.listAll());
    }

    /**
     * @description 添加新设备（自动生成设备密钥）
     * @param request 设备名称
     * @return 创建后的设备（含密钥）
     */
    @PostMapping("/devices")
    public Result<Device> createDevice(@Valid @RequestBody DeviceCreateRequest request) {
        return Result.ok(deviceService.create(request));
    }

    /**
     * @description 切换设备启用/禁用状态
     * @param id 设备ID
     * @return 更新后的设备
     */
    @PutMapping("/devices/{id}/toggle")
    public Result<Device> toggleDevice(@PathVariable Long id) {
        return Result.ok(deviceService.toggleStatus(id));
    }

    /**
     * @description 删除设备
     * @param id 设备ID
     * @return 操作结果
     */
    @DeleteMapping("/devices/{id}")
    public Result<Void> deleteDevice(@PathVariable Long id) {
        deviceService.delete(id);
        return Result.ok();
    }
}
