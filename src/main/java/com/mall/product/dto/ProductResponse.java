package com.mall.product.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProductResponse {

    private final Long id;
    private final Long categoryId;
    private final String name;
    private final String subtitle;
    private final String imageUrl;
    private final Long defaultSkuId;
    private final BigDecimal price;
    private final BigDecimal originalPrice;
    private final String unit;
    private final Integer stock;
    private final Integer sales;
    private final List<ProductSkuResponse> skus;

    public ProductResponse(Long id, Long categoryId, String name, String subtitle, String imageUrl,
                           Long defaultSkuId, BigDecimal price, BigDecimal originalPrice, String unit,
                           Integer stock, Integer sales, List<ProductSkuResponse> skus) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.defaultSkuId = defaultSkuId;
        this.price = price;
        this.originalPrice = originalPrice;
        this.unit = unit;
        this.stock = stock;
        this.sales = sales;
        this.skus = skus;
    }

    public Long getId() {
        return id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Long getDefaultSkuId() {
        return defaultSkuId;
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

    public Integer getStock() {
        return stock;
    }

    public Integer getSales() {
        return sales;
    }

    public List<ProductSkuResponse> getSkus() {
        return skus;
    }
}
