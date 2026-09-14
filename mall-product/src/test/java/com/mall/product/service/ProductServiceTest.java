package com.mall.product.service;

import com.mall.product.dto.ProductResponse;
import com.mall.product.mapper.ProductMapper;
import com.mall.product.mapper.ProductSkuMapper;
import com.mall.product.model.ProductEntity;
import com.mall.product.model.ProductSkuEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    @Test
    void returnsProductsWithAbsoluteImageUrls() {
        ProductMapper mapper = mock(ProductMapper.class);
        ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
        ProductEntity product = new ProductEntity();
        product.setId(1L);
        product.setCategoryId(1L);
        product.setName("当季软籽石榴");
        product.setSubtitle("果粒饱满，籽软汁多");
        product.setImagePath("/images/products/soft-seed-pomegranate.jpg");
        when(mapper.findOnShelfByCategoryId(1L)).thenReturn(Collections.singletonList(product));
        when(skuMapper.findOnSaleByProductId(1L)).thenReturn(Arrays.asList(
                sku(2L, "1kg标准装", new BigDecimal("29.90"), true),
                sku(1L, "500g尝鲜装", new BigDecimal("16.45"), false),
                sku(3L, "2.5kg家庭装", new BigDecimal("65.78"), false)
        ));

        ProductService service = new ProductService(mapper, skuMapper);
        List<ProductResponse> response = service.listByCategory(1L, "https://mall.example.com");

        assertEquals(1, response.size());
        assertEquals("当季软籽石榴", response.get(0).getName());
        assertEquals("https://mall.example.com/images/products/soft-seed-pomegranate.jpg",
                response.get(0).getImageUrl());
        assertEquals(new BigDecimal("29.90"), response.get(0).getPrice());
        assertEquals(new BigDecimal("35.88"), response.get(0).getOriginalPrice());
        assertEquals(2L, response.get(0).getDefaultSkuId());
        assertEquals(3, response.get(0).getSkus().size());
    }

    private static ProductSkuEntity sku(Long id, String name, BigDecimal price, boolean defaultSku) {
        ProductSkuEntity sku = new ProductSkuEntity();
        sku.setId(id);
        sku.setSkuCode("SKU-" + id);
        sku.setSkuName(name);
        sku.setPrice(price);
        sku.setOriginalPrice(price.multiply(new BigDecimal("1.20")).setScale(2, RoundingMode.HALF_UP));
        sku.setUnit(name.replace("装", "/份"));
        sku.setStock(50);
        sku.setSales(10);
        sku.setIsDefault(defaultSku ? 1 : 0);
        return sku;
    }
}
