package com.mall.order.controller;

import com.mall.common.api.ApiResponse;
import com.mall.order.dto.OrderResponse;
import com.mall.order.dto.OrderSummaryResponse;
import com.mall.order.dto.SubmitOrderRequest;
import com.mall.order.service.OrderService;
import com.mall.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/submit")
    public ApiResponse<OrderResponse> submit(@AuthenticationPrincipal AuthenticatedUser user,
                                             @Valid @RequestBody SubmitOrderRequest request) {
        return ApiResponse.success(orderService.submit(user.getUserId(), request, baseUrl()));
    }

    @GetMapping
    public ApiResponse<List<OrderSummaryResponse>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(orderService.list(user.getUserId()));
    }

    @GetMapping("/detail")
    public ApiResponse<OrderResponse> detail(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @Positive(message = "订单 ID 必须大于 0") Long orderId) {
        return ApiResponse.success(orderService.detail(user.getUserId(), orderId, baseUrl()));
    }

    private static String baseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}
