package com.mall.product.dto;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class AdminProductSkuUpdateRequest {
    @NotNull @Min(1)
    private Long skuId;
    @Valid @NotNull
    private AdminProductSkuInput sku;
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public AdminProductSkuInput getSku() { return sku; }
    public void setSku(AdminProductSkuInput sku) { this.sku = sku; }
}
