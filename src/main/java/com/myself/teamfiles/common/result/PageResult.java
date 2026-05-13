package com.myself.teamfiles.common.result;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private long page;
    private long pageSize;
    private List<T> records;

    public static <T> PageResult<T> of(long total, long page, long pageSize, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.total = total;
        result.page = page;
        result.pageSize = pageSize;
        result.records = records;
        return result;
    }
}
