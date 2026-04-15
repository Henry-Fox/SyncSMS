package com.syncsms.service;

import com.syncsms.common.PageResult;
import com.syncsms.dto.SmsBatchRequest;
import com.syncsms.dto.SmsQueryRequest;
import com.syncsms.entity.SmsMessage;

/**
 * @description 短信服务接口
 */
public interface SmsService {

    int batchSave(Long deviceId, SmsBatchRequest request);

    PageResult<SmsMessage> queryPage(SmsQueryRequest query);

    SmsMessage getById(Long id);

    void markRead(Long id, String username);

    void clearAll();
}
