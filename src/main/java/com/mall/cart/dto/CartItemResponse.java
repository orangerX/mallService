package com.mall.cart.dto;

import java.math.BigDecimal;

public class CartItemResponse {

    private final Long id;
    private final Long productId;
    private final Long skuId;
    private final String skuCode;
    private final String skuName;
    private final String name;
    private final String subtitle;
    private final String imageUrl;
    private final BigDecimal price;
    private final BigDecimal originalPrice;
    private final String unit;
    private final Integer quantity;
    private final Integer stock;
    private final boolean available;
    private final BigDecimal subtotal;

    public CartItemResponse(Long id, Long productId, Long skuId, String skuCode, String skuName,
                            String name, String subtitle, String imageUrl,
                            BigDecimal price, BigDecimal originalPrice, String unit, Integer quantity,
                            Integer stock, boolean available, BigDecimal subtotal) {
        this.id = id;
        this.productId = productId;
        this.skuId = skuId;
        this.skuCode = skuCode;
        this.skuName = skuName;
        this.name = name;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.price = price;
        this.originalPrice = originalPrice;
        this.unit = unit;
        this.quantity = quantity;
        this.stock = stock;
        this.available = available;
        this.subtotal = subtotal;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getSkuId() { return skuId; }
    public String getSkuCode() { return skuCode; }
    public String getSkuName() { return skuName; }

    public String getName() {
        return name;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public String getUnit() {
        return unit;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Integer getStock() {
        return stock;
    }

    public boolean isAvailable() {
        return available;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}
