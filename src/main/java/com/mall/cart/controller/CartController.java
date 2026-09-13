package com.mall.cart.controller;

import com.mall.cart.dto.CartItemQuantityRequest;
import com.mall.cart.dto.CartItemRequest;
import com.mall.cart.dto.CartResponse;
import com.mall.cart.service.CartService;
import com.mall.common.api.ApiResponse;
import com.mall.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<CartResponse> getCart(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(cartService.getCart(user.getUserId(), baseUrl()));
    }

    @PostMapping("/items/add")
    public ApiResponse<CartResponse> addItem(@AuthenticationPrincipal AuthenticatedUser user,
                                             @Valid @RequestBody CartItemQuantityRequest request) {
        return ApiResponse.success(cartService.addItem(
                user.getUserId(), request.getSkuId(), request.getQuantity(), baseUrl()
        ));
    }

    @PostMapping("/items/update")
    public ApiResponse<CartResponse> updateItem(@AuthenticationPrincipal AuthenticatedUser user,
                                                @Valid @RequestBody CartItemQuantityRequest request) {
        return ApiResponse.success(cartService.updateItem(
                user.getUserId(), request.getSkuId(), request.getQuantity(), baseUrl()
        ));
    }

    @PostMapping("/items/remove")
    public ApiResponse<CartResponse> removeItem(@AuthenticationPrincipal AuthenticatedUser user,
                                                @Valid @RequestBody CartItemRequest request) {
        return ApiResponse.success(cartService.removeItem(user.getUserId(), request.getSkuId(), baseUrl()));
    }

    @PostMapping("/clear")
    public ApiResponse<CartResponse> clear(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(cartService.clear(user.getUserId(), baseUrl()));
    }

    private static String baseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}
