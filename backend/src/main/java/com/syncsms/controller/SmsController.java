package com.syncsms.controller;

import com.syncsms.common.PageResult;
import com.syncsms.common.Result;
import com.syncsms.dto.SmsBatchRequest;
import com.syncsms.dto.SmsQueryRequest;
import com.syncsms.entity.SmsMessage;
import com.syncsms.service.SmsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * @description 短信相关接口
 */
@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;

    /**
     * @description APP 端批量上传短信
     * @param request 短信列表
     * @param auth    设备认证信息（从 JWT 中提取 deviceId）
     * @return 上传成功条数
     */
    @PostMapping("/batch")
    public Result<Integer> batchUpload(@Valid @RequestBody SmsBatchRequest request,
                                       Authentication auth) {
        Long deviceId = (Long) auth.getPrincipal();
        return Result.ok(smsService.batchSave(deviceId, request));
    }

    /**
     * @description Web 端分页查询短信列表
     * @param query 查询参数
     * @return 分页结果
     */
    @GetMapping("/list")
    public Result<PageResult<SmsMessage>> list(SmsQueryRequest query) {
        return Result.ok(smsService.queryPage(query));
    }

    /**
     * @description 获取短信详情
     * @param id 短信ID
     * @return 短信详情
     */
    @GetMapping("/{id}")
    public Result<SmsMessage> detail(@PathVariable Long id) {
        return Result.ok(smsService.getById(id));
    }

    /**
     * @description 标记短信为已读
     * @param id   短信ID
     * @param auth 当前登录用户
     * @return 操作结果
     */
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id, Authentication auth) {
        smsService.markRead(id, auth.getName());
        return Result.ok();
    }

    /**
     * @description 清空所有短信（管理员）
     * @return 操作结果
     */
    @DeleteMapping("/clear")
    public Result<Void> clearAll() {
        smsService.clearAll();
        return Result.ok();
    }
}
