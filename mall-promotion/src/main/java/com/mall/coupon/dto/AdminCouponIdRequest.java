package com.mall.coupon.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class AdminCouponIdRequest {
    @NotNull @Min(1)
    private Long couponId;
    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }
}
