package com.syncsms.dto;

import lombok.Data;

/**
 * @description 短信查询请求参数
 */
@Data
public class SmsQueryRequest {

    private Integer page = 1;

    private Integer size = 20;

    /** 按发送人搜索 */
    private String sender;

    /** 按内容搜索 */
    private String keyword;

    /** 设备ID筛选 */
    private Long deviceId;

    /** 是否已读筛选: 0/1 */
    private Integer isRead;
}
