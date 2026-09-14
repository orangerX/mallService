package com.mall.coupon.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class AdminCouponStatusRequest {
    @NotNull @Min(1)
    private Long couponId;
    @NotNull @Min(0) @Max(1)
    private Integer status;
    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
