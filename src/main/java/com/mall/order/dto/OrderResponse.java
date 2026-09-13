package com.mall.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {

    private final Long id;
    private final String orderNo;
    private final Integer status;
    private final String statusText;
    private final BigDecimal totalAmount;
    private final Integer totalQuantity;
    private final String receiverName;
    private final String receiverPhone;
    private final String receiverAddress;
    private final String remark;
    private final LocalDateTime createdAt;
    private final List<OrderItemResponse> items;

    public OrderResponse(Long id, String orderNo, Integer status, String statusText,
                         BigDecimal totalAmount, Integer totalQuantity, String receiverName,
                         String receiverPhone, String receiverAddress, String remark,
                         LocalDateTime createdAt, List<OrderItemResponse> items) {
        this.id = id;
        this.orderNo = orderNo;
        this.status = status;
        this.statusText = statusText;
        this.totalAmount = totalAmount;
        this.totalQuantity = totalQuantity;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
        this.remark = remark;
        this.createdAt = createdAt;
        this.items = items;
    }

    public Long getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public Integer getStatus() { return status; }
    public String getStatusText() { return statusText; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public String getReceiverName() { return receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public String getReceiverAddress() { return receiverAddress; }
    public String getRemark() { return remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<OrderItemResponse> getItems() { return items; }
}
