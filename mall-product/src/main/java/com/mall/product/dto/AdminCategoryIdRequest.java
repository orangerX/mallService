package com.mall.product.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class AdminCategoryIdRequest {
    @NotNull
    @Min(1)
    private Long categoryId;

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
}
