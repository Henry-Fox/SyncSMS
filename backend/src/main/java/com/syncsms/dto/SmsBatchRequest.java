package com.syncsms.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @description 批量上传短信请求
 */
@Data
public class SmsBatchRequest {

    @NotEmpty(message = "短信列表不能为空")
    private List<SmsItem> messages;

    @Data
    public static class SmsItem {

        @NotNull(message = "发送人不能为空")
        private String sender;

        @NotNull(message = "短信内容不能为空")
        private String content;

        @NotNull(message = "短信时间不能为空")
        private String smsTime;
    }
}
