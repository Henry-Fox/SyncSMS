package com.syncsms.service;

import com.syncsms.dto.DeviceCreateRequest;
import com.syncsms.dto.DeviceStatusResponse;
import com.syncsms.entity.Device;

import java.util.List;

/**
 * @description 设备管理服务接口
 */
public interface DeviceService {

    List<Device> listAll();

    /**
     * @description 供 Web 端展示设备状态（不包含 deviceKey）
     */
    List<DeviceStatusResponse> listStatus();

    Device create(DeviceCreateRequest request);

    Device toggleStatus(Long id);

    void delete(Long id);
}
