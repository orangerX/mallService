package com.mall.product.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class AdminCategoryStatusRequest {
    @NotNull
    @Min(1)
    private Long categoryId;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer status;

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
