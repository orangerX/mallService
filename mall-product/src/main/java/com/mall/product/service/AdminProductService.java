package com.mall.product.service;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.product.dto.AdminProductCreateRequest;
import com.mall.product.dto.AdminProductSkuCreateRequest;
import com.mall.product.dto.AdminProductSkuInput;
import com.mall.product.dto.AdminProductSkuUpdateRequest;
import com.mall.product.dto.AdminProductUpdateRequest;
import com.mall.product.mapper.CategoryMapper;
import com.mall.product.mapper.ProductMapper;
import com.mall.product.mapper.ProductSkuMapper;
import com.mall.product.model.ProductEntity;
import com.mall.product.model.ProductSkuEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AdminProductService {
    private static final BigDecimal ORIGINAL_PRICE_RATE = new BigDecimal("1.20");

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper skuMapper;

    public AdminProductService(CategoryMapper categoryMapper, ProductMapper productMapper,
                               ProductSkuMapper skuMapper) {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(AdminProductCreateRequest request) {
        requireCategory(request.getCategoryId());
        int defaultIndex = defaultSkuIndex(request.getSkus());
        ProductEntity product = product(request.getCategoryId(), request.getName(), request.getSubtitle(),
                request.getImagePath(), request.getSortOrder(), request.getStatus());
        try {
            productMapper.insert(product);
            for (int index = 0; index < request.getSkus().size(); index++) {
                AdminProductSkuInput input = request.getSkus().get(index);
                ensureSkuAvailable(product.getId(), input, null);
                skuMapper.insert(sku(product.getId(), input, index == defaultIndex, 0));
            }
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.DATA_CONFLICT);
        }
        return product.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long update(AdminProductUpdateRequest request) {
        requiredProductForUpdate(request.getProductId());
        requireCategory(request.getCategoryId());
        ProductEntity product = product(request.getCategoryId(), request.getName(), request.getSubtitle(),
                request.getImagePath(), request.getSortOrder(), request.getStatus());
        product.setId(request.getProductId());
        productMapper.update(product);
        return product.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long changeStatus(Long productId, Integer status) {
        requiredProductForUpdate(productId);
        productMapper.updateStatus(productId, status);
        return productId;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long productId) {
        requiredProductForUpdate(productId);
        if (productMapper.countOrderItems(productId) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.PRODUCT_HAS_ORDERS);
        }
        productMapper.deleteById(productId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProductSkuEntity createSku(AdminProductSkuCreateRequest request) {
        requiredProductForUpdate(request.getProductId());
        AdminProductSkuInput input = request.getSku();
        ensureSkuAvailable(request.getProductId(), input, null);
        boolean firstSku = skuMapper.countByProductId(request.getProductId()) == 0;
        boolean defaultSku = firstSku || Boolean.TRUE.equals(input.getDefaultSku());
        if (defaultSku && input.getStatus() != ProductSkuEntity.STATUS_ON_SALE) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.SKU_UNAVAILABLE,
                    "下架 SKU 不能设为默认规格");
        }
        if (defaultSku) skuMapper.clearDefaultByProductId(request.getProductId());
        ProductSkuEntity sku = sku(request.getProductId(), input, defaultSku, 0);
        try {
            skuMapper.insert(sku);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.DATA_CONFLICT);
        }
        return requiredSku(sku.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public ProductSkuEntity updateSku(AdminProductSkuUpdateRequest request) {
        ProductSkuEntity existing = requiredSkuForUpdate(request.getSkuId());
        AdminProductSkuInput input = request.getSku();
        ensureSkuAvailable(existing.getProductId(), input, existing.getId());
        boolean makeDefault = Boolean.TRUE.equals(input.getDefaultSku());
        if (makeDefault && input.getStatus() != ProductSkuEntity.STATUS_ON_SALE) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.SKU_UNAVAILABLE,
                    "下架 SKU 不能设为默认规格");
        }
        if (existing.getIsDefault() == 1 && !makeDefault) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.DEFAULT_SKU_CONFLICT,
                    "请先将其他 SKU 设为默认规格");
        }
        if (makeDefault) skuMapper.clearDefaultByProductId(existing.getProductId());
        ProductSkuEntity sku = sku(existing.getProductId(), input, makeDefault, existing.getSales());
        sku.setId(existing.getId());
        try {
            skuMapper.update(sku);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.DATA_CONFLICT);
        }
        return requiredSku(sku.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public ProductSkuEntity changeSkuStatus(Long skuId, Integer status) {
        ProductSkuEntity sku = requiredSkuForUpdate(skuId);
        if (status == ProductSkuEntity.STATUS_OFF_SALE && sku.getIsDefault() == 1) {
            ProductSkuEntity replacement = skuMapper.findFirstOtherOnSale(sku.getProductId(), skuId);
            if (replacement == null) {
                throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.PRODUCT_REQUIRES_ACTIVE_SKU);
            }
            skuMapper.clearDefaultByProductId(sku.getProductId());
            skuMapper.setDefault(replacement.getId());
        }
        skuMapper.updateStatus(skuId, status);
        return requiredSku(skuId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ProductSkuEntity setDefaultSku(Long skuId) {
        ProductSkuEntity sku = requiredSkuForUpdate(skuId);
        if (sku.getStatus() != ProductSkuEntity.STATUS_ON_SALE) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.SKU_UNAVAILABLE,
                    "下架 SKU 不能设为默认规格");
        }
        skuMapper.clearDefaultByProductId(sku.getProductId());
        skuMapper.setDefault(skuId);
        return requiredSku(skuId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSku(Long skuId) {
        ProductSkuEntity sku = requiredSkuForUpdate(skuId);
        if (skuMapper.countOrderItems(skuId) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.SKU_HAS_ORDERS);
        }
        if (skuMapper.countByProductId(sku.getProductId()) <= 1) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.PRODUCT_REQUIRES_SKU);
        }
        ProductSkuEntity replacement = sku.getIsDefault() == 1
                ? skuMapper.findFirstOtherOnSale(sku.getProductId(), skuId) : null;
        if (sku.getIsDefault() == 1 && replacement == null) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.PRODUCT_REQUIRES_ACTIVE_SKU);
        }
        skuMapper.deleteById(skuId);
        if (replacement != null) skuMapper.setDefault(replacement.getId());
    }

    private int defaultSkuIndex(List<AdminProductSkuInput> skus) {
        int result = -1;
        for (int index = 0; index < skus.size(); index++) {
            if (Boolean.TRUE.equals(skus.get(index).getDefaultSku())) {
                if (result >= 0) {
                    throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.DEFAULT_SKU_CONFLICT);
                }
                if (skus.get(index).getStatus() != ProductSkuEntity.STATUS_ON_SALE) {
                    throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.SKU_UNAVAILABLE,
                            "下架 SKU 不能设为默认规格");
                }
                result = index;
            }
        }
        if (result >= 0) return result;
        for (int index = 0; index < skus.size(); index++) {
            if (skus.get(index).getStatus() == ProductSkuEntity.STATUS_ON_SALE) return index;
        }
        throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.PRODUCT_REQUIRES_ACTIVE_SKU);
    }

    private void ensureSkuAvailable(Long productId, AdminProductSkuInput input, Long currentId) {
        String code = input.getSkuCode().trim();
        ProductSkuEntity codeMatch = skuMapper.findByCode(code);
        if (codeMatch != null && !codeMatch.getId().equals(currentId)) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.SKU_CODE_EXISTS);
        }
        String name = input.getSkuName().trim();
        ProductSkuEntity nameMatch = skuMapper.findByProductAndName(productId, name);
        if (nameMatch != null && !nameMatch.getId().equals(currentId)) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.SKU_NAME_EXISTS);
        }
    }

    private void requireCategory(Long categoryId) {
        if (categoryMapper.findById(categoryId) == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.CATEGORY_NOT_FOUND);
        }
    }

    private ProductEntity requiredProduct(Long productId) {
        ProductEntity product = productMapper.findById(productId);
        if (product == null) throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.PRODUCT_NOT_FOUND);
        return product;
    }

    private ProductEntity requiredProductForUpdate(Long productId) {
        ProductEntity product = productMapper.findByIdForUpdate(productId);
        if (product == null) throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.PRODUCT_NOT_FOUND);
        return product;
    }

    private ProductSkuEntity requiredSku(Long skuId) {
        ProductSkuEntity sku = skuMapper.findById(skuId);
        if (sku == null) throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.SKU_NOT_FOUND);
        return sku;
    }

    private ProductSkuEntity requiredSkuForUpdate(Long skuId) {
        ProductSkuEntity initial = requiredSku(skuId);
        requiredProductForUpdate(initial.getProductId());
        ProductSkuEntity sku = skuMapper.findByIdForUpdate(skuId);
        if (sku == null) throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.SKU_NOT_FOUND);
        return sku;
    }

    private static ProductEntity product(Long categoryId, String name, String subtitle, String imagePath,
                                         Integer sortOrder, Integer status) {
        ProductEntity product = new ProductEntity();
        product.setCategoryId(categoryId);
        product.setName(name.trim());
        product.setSubtitle(subtitle.trim());
        product.setImagePath(imagePath.trim());
        product.setSortOrder(sortOrder);
        product.setStatus(status);
        return product;
    }

    private static ProductSkuEntity sku(Long productId, AdminProductSkuInput input,
                                        boolean defaultSku, Integer sales) {
        ProductSkuEntity sku = new ProductSkuEntity();
        sku.setProductId(productId);
        sku.setSkuCode(input.getSkuCode().trim());
        sku.setSkuName(input.getSkuName().trim());
        sku.setPrice(input.getPrice().setScale(2, RoundingMode.HALF_UP));
        sku.setOriginalPrice(input.getPrice().multiply(ORIGINAL_PRICE_RATE).setScale(2, RoundingMode.HALF_UP));
        sku.setUnit(input.getUnit().trim());
        sku.setStock(input.getStock());
        sku.setSales(sales);
        sku.setIsDefault(defaultSku ? 1 : 0);
        sku.setSortOrder(input.getSortOrder());
        sku.setStatus(input.getStatus());
        return sku;
    }
}
