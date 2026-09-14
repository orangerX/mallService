package com.mall.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public class CartResponse {

    private final List<CartItemResponse> items;
    private final int itemCount;
    private final int totalQuantity;
    private final BigDecimal totalAmount;

    public CartResponse(List<CartItemResponse> items, int itemCount, int totalQuantity,
                        BigDecimal totalAmount) {
        this.items = items;
        this.itemCount = itemCount;
        this.totalQuantity = totalQuantity;
        this.totalAmount = totalAmount;
    }

    public List<CartItemResponse> getItems() {
        return items;
    }

    public int getItemCount() {
        return itemCount;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
