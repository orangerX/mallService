package com.mall.product.service;

import com.mall.common.api.ErrorCode;
import com.mall.common.api.PageResponse;
import com.mall.common.exception.BusinessException;
import com.mall.product.dto.AdminProductDetailResponse;
import com.mall.product.dto.AdminProductResponse;
import com.mall.product.mapper.CategoryMapper;
import com.mall.product.mapper.ProductMapper;
import com.mall.product.mapper.ProductSkuMapper;
import com.mall.product.model.CategoryEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminProductQueryService {
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;

    public AdminProductQueryService(CategoryMapper categoryMapper, ProductMapper productMapper,
                                    ProductSkuMapper skuMapper) {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<CategoryEntity> categories() { return categoryMapper.findAllCategories(); }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public PageResponse<AdminProductResponse> page(String keyword, Long categoryId, Integer status,
                                                    int page, int size, String baseUrl) {
        String normalized = normalize(keyword);
        List<AdminProductResponse> records = productMapper.findAdminPage(
                normalized, categoryId, status, (page - 1) * size, size);
        records.forEach(item -> item.setImageUrl(absolute(baseUrl, item.getImageUrl())));
        return new PageResponse<>(records, page, size,
                productMapper.countAdminPage(normalized, categoryId, status));
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public AdminProductDetailResponse detail(Long productId, String baseUrl) {
        AdminProductResponse product = productMapper.findAdminById(productId);
        if (product == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.PRODUCT_NOT_FOUND);
        }
        product.setImageUrl(absolute(baseUrl, product.getImageUrl()));
        return new AdminProductDetailResponse(product, skuMapper.findAllByProductId(productId));
    }

    private static String normalize(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static String absolute(String baseUrl, String path) {
        if (path == null || path.startsWith("http://") || path.startsWith("https://")) return path;
        return baseUrl + (path.startsWith("/") ? path : "/" + path);
    }
}
