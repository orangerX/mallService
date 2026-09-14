package com.mall.admin.coupon;

import com.mall.common.api.ApiResponse;
import com.mall.common.api.PageResponse;
import com.mall.coupon.dto.AdminCouponCreateRequest;
import com.mall.coupon.dto.AdminCouponIdRequest;
import com.mall.coupon.dto.AdminCouponStatusRequest;
import com.mall.coupon.dto.AdminCouponUpdateRequest;
import com.mall.coupon.model.CouponEntity;
import com.mall.coupon.service.AdminCouponQueryService;
import com.mall.coupon.service.AdminCouponService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@Validated
@RestController
@RequestMapping("/admin/api/coupons")
public class AdminCouponController {
    private final AdminCouponQueryService queryService;
    private final AdminCouponService couponService;

    public AdminCouponController(AdminCouponQueryService queryService, AdminCouponService couponService) {
        this.queryService = queryService;
        this.couponService = couponService;
    }

    @GetMapping
    public ApiResponse<PageResponse<CouponEntity>> coupons(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(queryService.page(name, status, page, size));
    }

    @GetMapping("/detail")
    public ApiResponse<CouponEntity> detail(@RequestParam @Min(1) Long couponId) {
        return ApiResponse.success(queryService.detail(couponId));
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<CouponEntity>> create(
            @Valid @RequestBody AdminCouponCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(couponService.create(request)));
    }

    @PostMapping("/update")
    public ApiResponse<CouponEntity> update(@Valid @RequestBody AdminCouponUpdateRequest request) {
        return ApiResponse.success(couponService.update(request));
    }

    @PostMapping("/status")
    public ApiResponse<CouponEntity> changeStatus(@Valid @RequestBody AdminCouponStatusRequest request) {
        return ApiResponse.success(couponService.changeStatus(request.getCouponId(), request.getStatus()));
    }

    @PostMapping("/delete")
    public ApiResponse<Void> delete(@Valid @RequestBody AdminCouponIdRequest request) {
        couponService.delete(request.getCouponId());
        return ApiResponse.success();
    }
}
