package com.mall.admin.order;

import com.mall.common.api.ApiResponse;
import com.mall.common.api.PageResponse;
import com.mall.order.dto.AdminOrderDetailResponse;
import com.mall.order.dto.AdminOrderResponse;
import com.mall.order.service.AdminOrderQueryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/admin/api/orders")
public class AdminOrderController {
    private final AdminOrderQueryService orderQueryService;

    public AdminOrderController(AdminOrderQueryService orderQueryService) {
        this.orderQueryService = orderQueryService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminOrderResponse>> orders(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) @Min(1) Long userId,
            @RequestParam(required = false) @Min(0) Integer status,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(orderQueryService.page(orderNo, userId, status, page, size));
    }

    @GetMapping("/detail")
    public ApiResponse<AdminOrderDetailResponse> detail(@RequestParam @Min(1) Long orderId) {
        return ApiResponse.success(orderQueryService.detail(orderId, baseUrl()));
    }

    private static String baseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}
