package com.mall.cart.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public class CartItemRequest {

    @NotNull(message = "SKU ID 不能为空")
    @Positive(message = "SKU ID 必须大于 0")
    private Long skuId;

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }
}
