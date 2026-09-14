package com.mall.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderSummaryResponse {

    private final Long id;
    private final String orderNo;
    private final Integer status;
    private final String statusText;
    private final BigDecimal totalAmount;
    private final BigDecimal couponDiscountAmount;
    private final BigDecimal paymentAmount;
    private final Integer totalQuantity;
    private final LocalDateTime createdAt;

    public OrderSummaryResponse(Long id, String orderNo, Integer status, String statusText,
                                BigDecimal totalAmount, BigDecimal couponDiscountAmount,
                                BigDecimal paymentAmount, Integer totalQuantity, LocalDateTime createdAt) {
        this.id = id;
        this.orderNo = orderNo;
        this.status = status;
        this.statusText = statusText;
        this.totalAmount = totalAmount;
        this.couponDiscountAmount = couponDiscountAmount;
        this.paymentAmount = paymentAmount;
        this.totalQuantity = totalQuantity;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public Integer getStatus() { return status; }
    public String getStatusText() { return statusText; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getCouponDiscountAmount() { return couponDiscountAmount; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
