package com.mall.common.api;

import java.util.List;

public class PageResponse<T> {

    private final List<T> records;
    private final int page;
    private final int size;
    private final long total;

    public PageResponse(List<T> records, int page, int size, long total) {
        this.records = records;
        this.page = page;
        this.size = size;
        this.total = total;
    }

    public List<T> getRecords() { return records; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotal() { return total; }
    public int getPages() { return size == 0 ? 0 : (int) ((total + size - 1) / size); }
}
