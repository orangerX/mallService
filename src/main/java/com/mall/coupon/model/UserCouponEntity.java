package com.mall.coupon.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UserCouponEntity {

    public static final int STATUS_UNUSED = 1;
    public static final int STATUS_USED = 2;
    public static final int STATUS_EXPIRED = 3;

    private Long id;
    private Long userId;
    private Long couponId;
    private Integer status;
    private LocalDateTime receivedAt;
    private LocalDateTime usedAt;
    private Long usedOrderId;
    private String couponName;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer couponStatus;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
    public Long getUsedOrderId() { return usedOrderId; }
    public void setUsedOrderId(Long usedOrderId) { this.usedOrderId = usedOrderId; }
    public String getCouponName() { return couponName; }
    public void setCouponName(String couponName) { this.couponName = couponName; }
    public BigDecimal getThresholdAmount() { return thresholdAmount; }
    public void setThresholdAmount(BigDecimal thresholdAmount) { this.thresholdAmount = thresholdAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public Integer getCouponStatus() { return couponStatus; }
    public void setCouponStatus(Integer couponStatus) { this.couponStatus = couponStatus; }
}
