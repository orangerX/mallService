package com.mall.cart.service;

import com.mall.cart.dto.CartItemResponse;
import com.mall.cart.dto.CartResponse;
import com.mall.cart.mapper.CartItemMapper;
import com.mall.cart.model.CartItemDetailEntity;
import com.mall.cart.model.CartItemEntity;
import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.product.mapper.ProductSkuMapper;
import com.mall.product.model.ProductEntity;
import com.mall.product.model.ProductSkuEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    private static final int MAX_QUANTITY = 99;

    private final CartItemMapper cartItemMapper;
    private final ProductSkuMapper productSkuMapper;

    public CartService(CartItemMapper cartItemMapper, ProductSkuMapper productSkuMapper) {
        this.cartItemMapper = cartItemMapper;
        this.productSkuMapper = productSkuMapper;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public CartResponse getCart(Long userId, String baseUrl) {
        List<CartItemResponse> items = cartItemMapper.findDetailsByUserId(userId).stream()
                .map(item -> toResponse(item, baseUrl))
                .collect(Collectors.toList());
        int totalQuantity = items.stream().mapToInt(CartItemResponse::getQuantity).sum();
        BigDecimal totalAmount = items.stream()
                .filter(CartItemResponse::isAvailable)
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(items, items.size(), totalQuantity, totalAmount);
    }

    @Transactional(rollbackFor = Exception.class)
    public CartResponse addItem(Long userId, Long skuId, Integer quantity, String baseUrl) {
        ProductSkuEntity sku = requireAvailableSku(skuId);
        cartItemMapper.increaseQuantity(userId, skuId, quantity);
        CartItemEntity item = cartItemMapper.findByUserAndSku(userId, skuId);
        validateQuantity(item.getQuantity(), sku.getStock());
        return getCart(userId, baseUrl);
    }

    @Transactional(rollbackFor = Exception.class)
    public CartResponse updateItem(Long userId, Long skuId, Integer quantity, String baseUrl) {
        ProductSkuEntity sku = requireAvailableSku(skuId);
        if (cartItemMapper.findByUserAndSku(userId, skuId) == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.CART_ITEM_NOT_FOUND);
        }
        validateQuantity(quantity, sku.getStock());
        cartItemMapper.updateQuantity(userId, skuId, quantity);
        return getCart(userId, baseUrl);
    }

    @Transactional(rollbackFor = Exception.class)
    public CartResponse removeItem(Long userId, Long skuId, String baseUrl) {
        if (cartItemMapper.deleteItem(userId, skuId) == 0) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.CART_ITEM_NOT_FOUND);
        }
        return getCart(userId, baseUrl);
    }

    @Transactional(rollbackFor = Exception.class)
    public CartResponse clear(Long userId, String baseUrl) {
        cartItemMapper.deleteByUserId(userId);
        return getCart(userId, baseUrl);
    }

    private ProductSkuEntity requireAvailableSku(Long skuId) {
        ProductSkuEntity sku = productSkuMapper.findById(skuId);
        if (sku == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.SKU_NOT_FOUND);
        }
        if (sku.getProductStatus() == null || sku.getProductStatus() != ProductEntity.STATUS_ON_SHELF) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.PRODUCT_UNAVAILABLE);
        }
        if (sku.getStatus() == null || sku.getStatus() != ProductSkuEntity.STATUS_ON_SALE) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.SKU_UNAVAILABLE);
        }
        return sku;
    }

    private static void validateQuantity(int quantity, int stock) {
        if (quantity > MAX_QUANTITY) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.CART_QUANTITY_LIMIT);
        }
        if (quantity > stock) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    private static CartItemResponse toResponse(CartItemDetailEntity item, String baseUrl) {
        boolean available = item.getProductStatus() != null
                && item.getProductStatus() == ProductEntity.STATUS_ON_SHELF
                && item.getSkuStatus() != null
                && item.getSkuStatus() == ProductSkuEntity.STATUS_ON_SALE
                && item.getStock() != null
                && item.getStock() >= item.getQuantity();
        BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(
                item.getId(),
                item.getProductId(),
                item.getSkuId(),
                item.getSkuCode(),
                item.getSkuName(),
                item.getProductName(),
                item.getProductSubtitle(),
                baseUrl + item.getProductImagePath(),
                item.getPrice(),
                item.getOriginalPrice(),
                item.getUnit(),
                item.getQuantity(),
                item.getStock(),
                available,
                subtotal
        );
    }
}
