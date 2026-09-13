package com.mall.cart.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public class CartItemQuantityRequest {

    @NotNull(message = "SKU ID 不能为空")
    @Positive(message = "SKU ID 必须大于 0")
    private Long skuId;

    @NotNull(message = "商品数量不能为空")
    @Positive(message = "商品数量必须大于 0")
    @Max(value = 99, message = "单个商品最多加入 99 件")
    private Integer quantity;

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
