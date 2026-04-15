package com.syncsms.service.impl;

import com.syncsms.dto.DeviceCreateRequest;
import com.syncsms.dto.DeviceStatusResponse;
import com.syncsms.entity.Device;
import com.syncsms.mapper.DeviceMapper;
import com.syncsms.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @description 设备管理服务实现
 */
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;

    @Override
    public List<Device> listAll() {
        return deviceMapper.selectList(null);
    }

    @Override
    public List<DeviceStatusResponse> listStatus() {
        List<Device> list = deviceMapper.selectList(null);
        return list.stream().map(d -> {
            DeviceStatusResponse r = new DeviceStatusResponse();
            r.setId(d.getId());
            r.setDeviceName(d.getDeviceName());
            r.setBatteryPercent(d.getBatteryPercent());
            r.setIsCharging(d.getIsCharging());
            r.setLastSeenAt(d.getLastSeenAt());
            return r;
        }).collect(Collectors.toList());
    }

    @Override
    public Device create(DeviceCreateRequest request) {
        Device device = new Device();
        device.setDeviceName(request.getDeviceName());
        device.setDeviceKey(UUID.randomUUID().toString().replace("-", ""));
        device.setStatus(1);
        deviceMapper.insert(device);
        return device;
    }

    @Override
    public Device toggleStatus(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new RuntimeException("设备不存在");
        }
        device.setStatus(device.getStatus() == 1 ? 0 : 1);
        deviceMapper.updateById(device);
        return device;
    }

    @Override
    public void delete(Long id) {
        if (deviceMapper.selectById(id) == null) {
            throw new RuntimeException("设备不存在");
        }
        deviceMapper.deleteById(id);
    }
}
