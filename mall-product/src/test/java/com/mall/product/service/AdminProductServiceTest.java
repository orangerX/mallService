package com.mall.product.service;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.product.dto.AdminProductCreateRequest;
import com.mall.product.dto.AdminProductSkuCreateRequest;
import com.mall.product.dto.AdminProductSkuInput;
import com.mall.product.mapper.CategoryMapper;
import com.mall.product.mapper.ProductMapper;
import com.mall.product.mapper.ProductSkuMapper;
import com.mall.product.model.CategoryEntity;
import com.mall.product.model.ProductEntity;
import com.mall.product.model.ProductSkuEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {
    @Mock CategoryMapper categoryMapper;
    @Mock ProductMapper productMapper;
    @Mock ProductSkuMapper skuMapper;
    private AdminProductService service;

    @BeforeEach
    void setUp() {
        service = new AdminProductService(categoryMapper, productMapper, skuMapper);
    }

    @Test
    void createsProductAndCalculatesOriginalPrice() {
        when(categoryMapper.findById(1L)).thenReturn(new CategoryEntity());
        doAnswer(invocation -> {
            ProductEntity product = invocation.getArgument(0);
            product.setId(20L);
            return 1;
        }).when(productMapper).insert(any(ProductEntity.class));
        doAnswer(invocation -> {
            ProductSkuEntity sku = invocation.getArgument(0);
            sku.setId(30L);
            return 1;
        }).when(skuMapper).insert(any(ProductSkuEntity.class));

        Long id = service.create(createRequest(Collections.singletonList(skuInput("APPLE-1KG", true))));

        assertEquals(20L, id);
        verify(skuMapper).insert(org.mockito.ArgumentMatchers.argThat(sku ->
                new BigDecimal("23.88").equals(sku.getOriginalPrice()) && sku.getIsDefault() == 1
                        && sku.getStock() == 100));
    }

    @Test
    void rejectsMoreThanOneDefaultSku() {
        when(categoryMapper.findById(1L)).thenReturn(new CategoryEntity());
        AdminProductCreateRequest request = createRequest(Arrays.asList(
                skuInput("APPLE-S", true), skuInput("APPLE-L", true)));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(request));

        assertEquals(ErrorCode.DEFAULT_SKU_CONFLICT, exception.getErrorCode());
        verify(productMapper, never()).insert(any(ProductEntity.class));
    }

    @Test
    void rejectsDuplicateSkuCode() {
        ProductEntity product = product(1L);
        when(productMapper.findByIdForUpdate(1L)).thenReturn(product);
        ProductSkuEntity duplicate = sku(9L, 2L, 0);
        when(skuMapper.findByCode("APPLE-1KG")).thenReturn(duplicate);
        AdminProductSkuCreateRequest request = new AdminProductSkuCreateRequest();
        request.setProductId(1L);
        request.setSku(skuInput("APPLE-1KG", false));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createSku(request));

        assertEquals(ErrorCode.SKU_CODE_EXISTS, exception.getErrorCode());
        verify(skuMapper, never()).insert(any(ProductSkuEntity.class));
    }

    @Test
    void rejectsDeletingProductReferencedByOrder() {
        when(productMapper.findByIdForUpdate(1L)).thenReturn(product(1L));
        when(productMapper.countOrderItems(1L)).thenReturn(2L);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.delete(1L));

        assertEquals(ErrorCode.PRODUCT_HAS_ORDERS, exception.getErrorCode());
        verify(productMapper, never()).deleteById(1L);
    }

    @Test
    void rejectsDeletingOnlySku() {
        ProductSkuEntity onlySku = sku(8L, 1L, 1);
        when(skuMapper.findById(8L)).thenReturn(onlySku);
        when(productMapper.findByIdForUpdate(1L)).thenReturn(product(1L));
        when(skuMapper.findByIdForUpdate(8L)).thenReturn(onlySku);
        when(skuMapper.countByProductId(1L)).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.deleteSku(8L));

        assertEquals(ErrorCode.PRODUCT_REQUIRES_SKU, exception.getErrorCode());
        verify(skuMapper, never()).deleteById(8L);
    }

    @Test
    void deletingDefaultSkuPromotesReplacement() {
        ProductSkuEntity current = sku(8L, 1L, 1);
        ProductSkuEntity replacement = sku(9L, 1L, 0);
        when(skuMapper.findById(8L)).thenReturn(current);
        when(productMapper.findByIdForUpdate(1L)).thenReturn(product(1L));
        when(skuMapper.findByIdForUpdate(8L)).thenReturn(current);
        when(skuMapper.countByProductId(1L)).thenReturn(2L);
        when(skuMapper.findFirstOtherOnSale(1L, 8L)).thenReturn(replacement);

        service.deleteSku(8L);

        verify(skuMapper).deleteById(8L);
        verify(skuMapper).setDefault(9L);
    }

    private static AdminProductCreateRequest createRequest(java.util.List<AdminProductSkuInput> skus) {
        AdminProductCreateRequest request = new AdminProductCreateRequest();
        request.setCategoryId(1L);
        request.setName(" 测试苹果 ");
        request.setSubtitle(" 清甜爽脆 ");
        request.setImagePath("/images/products/apple.jpg");
        request.setSortOrder(1);
        request.setStatus(1);
        request.setSkus(skus);
        return request;
    }

    private static AdminProductSkuInput skuInput(String code, boolean defaultSku) {
        AdminProductSkuInput input = new AdminProductSkuInput();
        input.setSkuCode(code);
        input.setSkuName(code + "规格");
        input.setPrice(new BigDecimal("19.90"));
        input.setUnit("1kg/份");
        input.setStock(100);
        input.setDefaultSku(defaultSku);
        input.setSortOrder(1);
        input.setStatus(1);
        return input;
    }

    private static ProductEntity product(Long id) {
        ProductEntity product = new ProductEntity();
        product.setId(id);
        return product;
    }

    private static ProductSkuEntity sku(Long id, Long productId, int isDefault) {
        ProductSkuEntity sku = new ProductSkuEntity();
        sku.setId(id);
        sku.setProductId(productId);
        sku.setIsDefault(isDefault);
        sku.setStatus(1);
        sku.setSales(0);
        return sku;
    }
}
