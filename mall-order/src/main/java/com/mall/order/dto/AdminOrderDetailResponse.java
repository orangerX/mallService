package com.mall.order.dto;

import com.mall.order.model.OrderItemEntity;
import java.util.List;

public class AdminOrderDetailResponse {
    private final AdminOrderResponse order;
    private final List<OrderItemEntity> items;

    public AdminOrderDetailResponse(AdminOrderResponse order, List<OrderItemEntity> items) {
        this.order = order;
        this.items = items;
    }

    public AdminOrderResponse getOrder() { return order; }
    public List<OrderItemEntity> getItems() { return items; }
}
