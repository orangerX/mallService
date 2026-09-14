package com.mall.product.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class AdminProductStatusRequest {
    @NotNull @Min(1)
    private Long productId;
    @NotNull @Min(0) @Max(1)
    private Integer status;
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
