package com.mall.product.dto;

import com.mall.product.model.ProductSkuEntity;

import java.math.BigDecimal;

public class ProductSkuResponse {

    private final Long id;
    private final String skuCode;
    private final String skuName;
    private final BigDecimal price;
    private final BigDecimal originalPrice;
    private final String unit;
    private final Integer stock;
    private final Integer sales;
    private final boolean defaultSku;

    public ProductSkuResponse(Long id, String skuCode, String skuName, BigDecimal price,
                              BigDecimal originalPrice, String unit, Integer stock,
                              Integer sales, boolean defaultSku) {
        this.id = id;
        this.skuCode = skuCode;
        this.skuName = skuName;
        this.price = price;
        this.originalPrice = originalPrice;
        this.unit = unit;
        this.stock = stock;
        this.sales = sales;
        this.defaultSku = defaultSku;
    }

    public static ProductSkuResponse from(ProductSkuEntity sku) {
        return new ProductSkuResponse(
                sku.getId(), sku.getSkuCode(), sku.getSkuName(), sku.getPrice(),
                sku.getOriginalPrice(), sku.getUnit(), sku.getStock(), sku.getSales(),
                sku.getIsDefault() != null && sku.getIsDefault() == 1
        );
    }

    public Long getId() { return id; }
    public String getSkuCode() { return skuCode; }
    public String getSkuName() { return skuName; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public String getUnit() { return unit; }
    public Integer getStock() { return stock; }
    public Integer getSales() { return sales; }
    public boolean isDefaultSku() { return defaultSku; }
}
