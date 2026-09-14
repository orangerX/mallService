package com.mall.product.dto;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class AdminProductSkuCreateRequest {
    @NotNull @Min(1)
    private Long productId;
    @Valid @NotNull
    private AdminProductSkuInput sku;
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public AdminProductSkuInput getSku() { return sku; }
    public void setSku(AdminProductSkuInput sku) { this.sku = sku; }
}
