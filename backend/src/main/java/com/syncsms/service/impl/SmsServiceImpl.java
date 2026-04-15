package com.syncsms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syncsms.common.PageResult;
import com.syncsms.dto.SmsBatchRequest;
import com.syncsms.dto.SmsQueryRequest;
import com.syncsms.entity.Device;
import com.syncsms.entity.SmsMessage;
import com.syncsms.mapper.DeviceMapper;
import com.syncsms.mapper.SmsMessageMapper;
import com.syncsms.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @description 短信服务实现
 */
@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

    private final SmsMessageMapper smsMapper;
    private final DeviceMapper deviceMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public int batchSave(Long deviceId, SmsBatchRequest request) {
        List<SmsMessage> messages = new ArrayList<>();
        for (SmsBatchRequest.SmsItem item : request.getMessages()) {
            SmsMessage msg = new SmsMessage();
            msg.setDeviceId(deviceId);
            msg.setSender(item.getSender());
            msg.setContent(item.getContent());
            msg.setSmsTime(LocalDateTime.parse(item.getSmsTime(), FORMATTER));
            msg.setIsRead(0);
            msg.setSyncedAt(LocalDateTime.now());
            messages.add(msg);
        }

        int count = 0;
        for (SmsMessage msg : messages) {
            try {
                smsMapper.insert(msg);
                count++;
            } catch (DuplicateKeyException ignored) {
                // 全量同步场景下同一条短信可能重复上传，跳过即可
            }
        }

        Device device = deviceMapper.selectById(deviceId);
        if (device != null) {
            device.setLastSyncAt(LocalDateTime.now());
            deviceMapper.updateById(device);
        }

        return count;
    }

    @Override
    public PageResult<SmsMessage> queryPage(SmsQueryRequest query) {
        Page<SmsMessage> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<SmsMessage> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(query.getSender())) {
            wrapper.like(SmsMessage::getSender, query.getSender());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(SmsMessage::getContent, query.getKeyword());
        }
        if (query.getDeviceId() != null) {
            wrapper.eq(SmsMessage::getDeviceId, query.getDeviceId());
        }
        if (query.getIsRead() != null) {
            wrapper.eq(SmsMessage::getIsRead, query.getIsRead());
        }
        wrapper.orderByDesc(SmsMessage::getSmsTime);

        Page<SmsMessage> result = smsMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public SmsMessage getById(Long id) {
        SmsMessage msg = smsMapper.selectById(id);
        if (msg == null) {
            throw new RuntimeException("短信不存在");
        }
        return msg;
    }

    @Override
    public void clearAll() {
        smsMapper.delete(null);
    }

    @Override
    public void markRead(Long id, String username) {
        smsMapper.update(null,
                new LambdaUpdateWrapper<SmsMessage>()
                        .eq(SmsMessage::getId, id)
                        .set(SmsMessage::getIsRead, 1)
                        .set(SmsMessage::getReadBy, username)
                        .set(SmsMessage::getReadAt, LocalDateTime.now()));
    }
}
