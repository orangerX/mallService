package com.mall.product.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class AdminProductSkuStatusRequest {
    @NotNull @Min(1)
    private Long skuId;
    @NotNull @Min(0) @Max(1)
    private Integer status;
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
