package com.mall.coupon.dto;

import com.mall.coupon.model.CouponEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CouponResponse {

    private final Long id;
    private final String name;
    private final BigDecimal thresholdAmount;
    private final BigDecimal discountAmount;
    private final Integer remainingQuantity;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;

    public CouponResponse(Long id, String name, BigDecimal thresholdAmount, BigDecimal discountAmount,
                          Integer remainingQuantity, LocalDateTime startAt, LocalDateTime endAt) {
        this.id = id;
        this.name = name;
        this.thresholdAmount = thresholdAmount;
        this.discountAmount = discountAmount;
        this.remainingQuantity = remainingQuantity;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public static CouponResponse from(CouponEntity coupon) {
        int remaining = Math.max(coupon.getTotalQuantity() - coupon.getReceivedQuantity(), 0);
        return new CouponResponse(coupon.getId(), coupon.getName(), coupon.getThresholdAmount(),
                coupon.getDiscountAmount(), remaining, coupon.getStartAt(), coupon.getEndAt());
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getThresholdAmount() { return thresholdAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public Integer getRemainingQuantity() { return remainingQuantity; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
}
