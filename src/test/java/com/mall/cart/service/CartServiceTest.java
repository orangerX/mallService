package com.mall.cart.service;

import com.mall.cart.dto.CartResponse;
import com.mall.cart.mapper.CartItemMapper;
import com.mall.cart.model.CartItemDetailEntity;
import com.mall.cart.model.CartItemEntity;
import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.product.mapper.ProductSkuMapper;
import com.mall.product.model.ProductEntity;
import com.mall.product.model.ProductSkuEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartServiceTest {

    @Test
    void buildsCartSummaryAndExcludesUnavailableItemsFromTotalAmount() {
        CartItemMapper cartMapper = mock(CartItemMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        when(cartMapper.findDetailsByUserId(7L)).thenReturn(Arrays.asList(
                detail(1L, 11L, 2, new BigDecimal("10.00"), 10, ProductEntity.STATUS_ON_SHELF),
                detail(2L, 12L, 3, new BigDecimal("5.00"), 10, ProductEntity.STATUS_OFF_SHELF)
        ));
        CartService service = new CartService(cartMapper, skuMapper);

        CartResponse response = service.getCart(7L, "https://mall.example.com");

        assertEquals(2, response.getItemCount());
        assertEquals(5, response.getTotalQuantity());
        assertEquals(new BigDecimal("20.00"), response.getTotalAmount());
        assertEquals("https://mall.example.com/images/products/product.jpg",
                response.getItems().get(0).getImageUrl());
    }

    @Test
    void addItemRejectsAccumulatedQuantityAboveStock() {
        CartItemMapper cartMapper = mock(CartItemMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        when(skuMapper.findById(11L)).thenReturn(sku(11L, 2));
        CartItemEntity item = new CartItemEntity();
        item.setQuantity(3);
        when(cartMapper.findByUserAndSku(7L, 11L)).thenReturn(item);
        CartService service = new CartService(cartMapper, skuMapper);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.addItem(7L, 11L, 2, "http://localhost"));

        assertEquals(ErrorCode.INSUFFICIENT_STOCK, exception.getErrorCode());
        verify(cartMapper).increaseQuantity(7L, 11L, 2);
    }

    @Test
    void updateItemRejectsMissingCartItem() {
        CartItemMapper cartMapper = mock(CartItemMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        when(skuMapper.findById(11L)).thenReturn(sku(11L, 10));
        CartService service = new CartService(cartMapper, skuMapper);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateItem(7L, 11L, 2, "http://localhost"));

        assertEquals(ErrorCode.CART_ITEM_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void clearIsIdempotentAndReturnsEmptyCart() {
        CartItemMapper cartMapper = mock(CartItemMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        when(cartMapper.findDetailsByUserId(7L)).thenReturn(Collections.emptyList());
        CartService service = new CartService(cartMapper, skuMapper);

        CartResponse response = service.clear(7L, "http://localhost");

        assertEquals(0, response.getItemCount());
        assertEquals(BigDecimal.ZERO, response.getTotalAmount());
        verify(cartMapper).deleteByUserId(7L);
    }

    private static ProductSkuEntity sku(Long id, int stock) {
        ProductSkuEntity sku = new ProductSkuEntity();
        sku.setId(id);
        sku.setStatus(ProductSkuEntity.STATUS_ON_SALE);
        sku.setProductStatus(ProductEntity.STATUS_ON_SHELF);
        sku.setStock(stock);
        return sku;
    }

    private static CartItemDetailEntity detail(Long id, Long productId, int quantity,
                                               BigDecimal price, int stock, int status) {
        CartItemDetailEntity item = new CartItemDetailEntity();
        item.setId(id);
        item.setProductId(productId);
        item.setSkuId(productId + 100L);
        item.setSkuCode("SKU-" + productId);
        item.setSkuName("1kg标准装");
        item.setQuantity(quantity);
        item.setProductName("商品" + productId);
        item.setProductSubtitle("新鲜水果");
        item.setProductImagePath("/images/products/product.jpg");
        item.setPrice(price);
        item.setOriginalPrice(price.multiply(new BigDecimal("1.20")));
        item.setUnit("件");
        item.setStock(stock);
        item.setProductStatus(status);
        item.setSkuStatus(ProductSkuEntity.STATUS_ON_SALE);
        return item;
    }
}
