package com.mall.coupon.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public class CouponIdRequest {

    @NotNull(message = "优惠券 ID 不能为空")
    @Positive(message = "优惠券 ID 必须大于 0")
    private Long couponId;

    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }
}
