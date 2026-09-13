package com.mall.coupon.dto;

import com.mall.coupon.model.UserCouponEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UserCouponResponse {

    private final Long userCouponId;
    private final Long couponId;
    private final String name;
    private final BigDecimal thresholdAmount;
    private final BigDecimal discountAmount;
    private final Integer status;
    private final String statusText;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final LocalDateTime receivedAt;
    private final LocalDateTime usedAt;
    private final Long usedOrderId;

    public UserCouponResponse(Long userCouponId, Long couponId, String name,
                              BigDecimal thresholdAmount, BigDecimal discountAmount,
                              Integer status, String statusText, LocalDateTime startAt,
                              LocalDateTime endAt, LocalDateTime receivedAt,
                              LocalDateTime usedAt, Long usedOrderId) {
        this.userCouponId = userCouponId;
        this.couponId = couponId;
        this.name = name;
        this.thresholdAmount = thresholdAmount;
        this.discountAmount = discountAmount;
        this.status = status;
        this.statusText = statusText;
        this.startAt = startAt;
        this.endAt = endAt;
        this.receivedAt = receivedAt;
        this.usedAt = usedAt;
        this.usedOrderId = usedOrderId;
    }

    public static UserCouponResponse from(UserCouponEntity coupon) {
        return new UserCouponResponse(
                coupon.getId(), coupon.getCouponId(), coupon.getCouponName(),
                coupon.getThresholdAmount(), coupon.getDiscountAmount(), coupon.getStatus(),
                statusText(coupon.getStatus()), coupon.getStartAt(), coupon.getEndAt(),
                coupon.getReceivedAt(), coupon.getUsedAt(), coupon.getUsedOrderId()
        );
    }

    private static String statusText(Integer status) {
        if (status != null && status == UserCouponEntity.STATUS_UNUSED) { return "未使用"; }
        if (status != null && status == UserCouponEntity.STATUS_USED) { return "已使用"; }
        if (status != null && status == UserCouponEntity.STATUS_EXPIRED) { return "已过期"; }
        return "未知状态";
    }

    public Long getUserCouponId() { return userCouponId; }
    public Long getCouponId() { return couponId; }
    public String getName() { return name; }
    public BigDecimal getThresholdAmount() { return thresholdAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public Integer getStatus() { return status; }
    public String getStatusText() { return statusText; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public Long getUsedOrderId() { return usedOrderId; }
}
