package com.mall.coupon.controller;

import com.mall.common.api.ApiResponse;
import com.mall.coupon.dto.CouponIdRequest;
import com.mall.coupon.dto.CouponResponse;
import com.mall.coupon.dto.UserCouponResponse;
import com.mall.coupon.service.CouponService;
import com.mall.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/available")
    public ApiResponse<List<CouponResponse>> available() {
        return ApiResponse.success(couponService.listAvailable());
    }

    @PostMapping("/receive")
    public ApiResponse<UserCouponResponse> receive(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CouponIdRequest request) {
        return ApiResponse.success(couponService.receive(user.getUserId(), request.getCouponId()));
    }

    @GetMapping("/mine")
    public ApiResponse<List<UserCouponResponse>> mine(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false)
            @Min(value = 1, message = "优惠券状态不能小于 1")
            @Max(value = 3, message = "优惠券状态不能大于 3") Integer status) {
        return ApiResponse.success(couponService.listMine(user.getUserId(), status));
    }
}
