package com.mall.order.dto;

import java.math.BigDecimal;

public class OrderItemResponse {

    private final Long productId;
    private final Long skuId;
    private final String skuCode;
    private final String skuName;
    private final String productName;
    private final String imageUrl;
    private final BigDecimal price;
    private final BigDecimal originalPrice;
    private final String unit;
    private final Integer quantity;
    private final BigDecimal subtotal;

    public OrderItemResponse(Long productId, Long skuId, String skuCode, String skuName,
                             String productName, String imageUrl, BigDecimal price,
                             BigDecimal originalPrice, String unit, Integer quantity, BigDecimal subtotal) {
        this.productId = productId;
        this.skuId = skuId;
        this.skuCode = skuCode;
        this.skuName = skuName;
        this.productName = productName;
        this.imageUrl = imageUrl;
        this.price = price;
        this.originalPrice = originalPrice;
        this.unit = unit;
        this.quantity = quantity;
        this.subtotal = subtotal;
    }

    public Long getProductId() { return productId; }
    public Long getSkuId() { return skuId; }
    public String getSkuCode() { return skuCode; }
    public String getSkuName() { return skuName; }
    public String getProductName() { return productName; }
    public String getImageUrl() { return imageUrl; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public String getUnit() { return unit; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getSubtotal() { return subtotal; }
}
