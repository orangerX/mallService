package com.mall.product.dto;

import com.mall.product.model.ProductSkuEntity;
import java.util.List;

public class AdminProductDetailResponse {
    private final AdminProductResponse product;
    private final List<ProductSkuEntity> skus;

    public AdminProductDetailResponse(AdminProductResponse product, List<ProductSkuEntity> skus) {
        this.product = product;
        this.skus = skus;
    }

    public AdminProductResponse getProduct() { return product; }
    public List<ProductSkuEntity> getSkus() { return skus; }
}
