package com.syncsms.common;

import lombok.Data;

import java.util.List;

/**
 * @description 分页查询结果封装
 * @param <T> 记录类型
 */
@Data
public class PageResult<T> {

    private long total;
    private long page;
    private long size;
    private List<T> records;

    public PageResult(long total, long page, long size, List<T> records) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.records = records;
    }
}
