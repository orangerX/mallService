package com.mall.order.dto;

import com.mall.order.model.OrderEntity;

public class AdminOrderResponse extends OrderEntity {
    private String username;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
