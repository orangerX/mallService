package com.mall.product.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class AdminProductSkuIdRequest {
    @NotNull @Min(1)
    private Long skuId;
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
}
