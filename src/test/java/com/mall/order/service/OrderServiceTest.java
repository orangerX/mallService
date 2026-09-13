package com.mall.order.service;

import com.mall.cart.mapper.CartItemMapper;
import com.mall.cart.model.CartItemDetailEntity;
import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.order.dto.OrderResponse;
import com.mall.order.dto.SubmitOrderRequest;
import com.mall.order.mapper.OrderMapper;
import com.mall.order.model.OrderEntity;
import com.mall.product.mapper.ProductSkuMapper;
import com.mall.product.model.ProductEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    @Test
    void submitCreatesSnapshotDecreasesStockAndClearsCart() {
        OrderMapper orderMapper = mock(OrderMapper.class);
        CartItemMapper cartMapper = mock(CartItemMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        when(cartMapper.findDetailsByUserId(7L)).thenReturn(Arrays.asList(
                detail(11L, "苹果", new BigDecimal("10.00"), 2, 20),
                detail(12L, "梨", new BigDecimal("6.50"), 3, 20)
        ));
        when(orderMapper.insertOrder(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            order.setId(99L);
            return 1;
        });
        when(skuMapper.decreaseStock(111L, 2)).thenReturn(1);
        when(skuMapper.decreaseStock(112L, 3)).thenReturn(1);
        OrderService service = new OrderService(orderMapper, cartMapper, skuMapper);

        OrderResponse response = service.submit(7L, request(), "https://mall.example.com");

        assertEquals(99L, response.getId());
        assertTrue(response.getOrderNo().startsWith("M"));
        assertEquals("待支付", response.getStatusText());
        assertEquals(new BigDecimal("39.50"), response.getTotalAmount());
        assertEquals(5, response.getTotalQuantity());
        assertEquals(2, response.getItems().size());
        assertEquals("https://mall.example.com/images/products/product.jpg",
                response.getItems().get(0).getImageUrl());
        verify(orderMapper).insertItems(anyList());
        verify(cartMapper).deleteByUserId(7L);
    }

    @Test
    void submitRejectsEmptyCart() {
        OrderMapper orderMapper = mock(OrderMapper.class);
        CartItemMapper cartMapper = mock(CartItemMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        when(cartMapper.findDetailsByUserId(7L)).thenReturn(Collections.emptyList());
        OrderService service = new OrderService(orderMapper, cartMapper, skuMapper);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.submit(7L, request(), "http://localhost"));

        assertEquals(ErrorCode.CART_EMPTY, exception.getErrorCode());
        verify(orderMapper, never()).insertOrder(any(OrderEntity.class));
    }

    @Test
    void submitRejectsConcurrentStockShortageWithoutClearingCart() {
        OrderMapper orderMapper = mock(OrderMapper.class);
        CartItemMapper cartMapper = mock(CartItemMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        when(cartMapper.findDetailsByUserId(7L)).thenReturn(Collections.singletonList(
                detail(11L, "苹果", new BigDecimal("10.00"), 2, 20)
        ));
        when(orderMapper.insertOrder(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity order = invocation.getArgument(0);
            order.setId(99L);
            return 1;
        });
        when(skuMapper.decreaseStock(111L, 2)).thenReturn(0);
        OrderService service = new OrderService(orderMapper, cartMapper, skuMapper);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.submit(7L, request(), "http://localhost"));

        assertEquals(ErrorCode.INSUFFICIENT_STOCK, exception.getErrorCode());
        verify(orderMapper, never()).insertItems(anyList());
        verify(cartMapper, never()).deleteByUserId(7L);
    }

    @Test
    void detailCannotReadAnotherUsersOrder() {
        OrderMapper orderMapper = mock(OrderMapper.class);
        CartItemMapper cartMapper = mock(CartItemMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        OrderService service = new OrderService(orderMapper, cartMapper, skuMapper);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.detail(7L, 99L, "http://localhost"));

        assertEquals(ErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
    }

    private static SubmitOrderRequest request() {
        SubmitOrderRequest request = new SubmitOrderRequest();
        request.setReceiverName("测试用户");
        request.setReceiverPhone("13800138000");
        request.setReceiverAddress("测试地址 1 号");
        request.setRemark("尽快配送");
        return request;
    }

    private static CartItemDetailEntity detail(Long productId, String name, BigDecimal price,
                                               int quantity, int stock) {
        CartItemDetailEntity item = new CartItemDetailEntity();
        item.setProductId(productId);
        item.setSkuId(productId + 100L);
        item.setSkuCode("SKU-" + productId);
        item.setSkuName("1kg标准装");
        item.setQuantity(quantity);
        item.setProductName(name);
        item.setProductImagePath("/images/products/product.jpg");
        item.setPrice(price);
        item.setOriginalPrice(price.multiply(new BigDecimal("1.20")));
        item.setUnit("件");
        item.setStock(stock);
        item.setProductStatus(ProductEntity.STATUS_ON_SHELF);
        item.setSkuStatus(com.mall.product.model.ProductSkuEntity.STATUS_ON_SALE);
        return item;
    }
}
