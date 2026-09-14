package com.mall.product.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class AdminProductIdRequest {
    @NotNull @Min(1)
    private Long productId;
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
}
