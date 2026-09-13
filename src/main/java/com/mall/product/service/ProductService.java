package com.mall.product.service;

import com.mall.product.dto.ProductResponse;
import com.mall.product.dto.ProductSkuResponse;
import com.mall.product.mapper.ProductMapper;
import com.mall.product.mapper.ProductSkuMapper;
import com.mall.product.model.ProductEntity;
import com.mall.product.model.ProductSkuEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;

    public ProductService(ProductMapper productMapper, ProductSkuMapper productSkuMapper) {
        this.productMapper = productMapper;
        this.productSkuMapper = productSkuMapper;
    }

    public List<ProductResponse> listByCategory(Long categoryId, String baseUrl) {
        List<ProductResponse> responses = new ArrayList<>();
        for (ProductEntity product : productMapper.findOnShelfByCategoryId(categoryId)) {
            List<ProductSkuEntity> skus = productSkuMapper.findOnSaleByProductId(product.getId());
            if (!skus.isEmpty()) {
                responses.add(toResponse(product, skus, baseUrl));
            }
        }
        return responses;
    }

    private static ProductResponse toResponse(ProductEntity product, List<ProductSkuEntity> skus, String baseUrl) {
        ProductSkuEntity defaultSku = skus.get(0);
        List<ProductSkuResponse> skuResponses = skus.stream()
                .map(ProductSkuResponse::from)
                .collect(Collectors.toList());
        return new ProductResponse(
                product.getId(),
                product.getCategoryId(),
                product.getName(),
                product.getSubtitle(),
                baseUrl + product.getImagePath(),
                defaultSku.getId(),
                defaultSku.getPrice(),
                defaultSku.getOriginalPrice(),
                defaultSku.getUnit(),
                defaultSku.getStock(),
                defaultSku.getSales(),
                skuResponses
        );
    }
}
