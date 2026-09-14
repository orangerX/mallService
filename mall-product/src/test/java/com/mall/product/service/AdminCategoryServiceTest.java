package com.mall.product.service;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.product.dto.AdminCategoryCreateRequest;
import com.mall.product.dto.AdminCategoryUpdateRequest;
import com.mall.product.mapper.CategoryMapper;
import com.mall.product.model.CategoryEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCategoryServiceTest {
    @Mock
    private CategoryMapper categoryMapper;
    private AdminCategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new AdminCategoryService(categoryMapper);
    }

    @Test
    void createsCategoryAndNormalizesText() {
        AdminCategoryCreateRequest request = createRequest("  鲜果礼盒  ", "gift-fruit");
        doAnswer(invocation -> {
            CategoryEntity category = invocation.getArgument(0);
            category.setId(8L);
            return 1;
        }).when(categoryMapper).insert(any(CategoryEntity.class));
        when(categoryMapper.findById(8L)).thenReturn(category(8L, "鲜果礼盒", "gift-fruit"));

        CategoryEntity result = categoryService.create(request);

        assertEquals(8L, result.getId());
        verify(categoryMapper).insert(org.mockito.ArgumentMatchers.argThat(category ->
                "鲜果礼盒".equals(category.getName()) && "gift-fruit".equals(category.getCode())));
    }

    @Test
    void rejectsDuplicateCode() {
        when(categoryMapper.findByCode("fresh-hot")).thenReturn(category(1L, "新鲜热卖", "fresh-hot"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> categoryService.create(createRequest("另一个类目", "fresh-hot")));

        assertEquals(ErrorCode.CATEGORY_CODE_EXISTS, exception.getErrorCode());
        verify(categoryMapper, never()).insert(any(CategoryEntity.class));
    }

    @Test
    void updatesExistingCategory() {
        AdminCategoryUpdateRequest request = new AdminCategoryUpdateRequest();
        request.setCategoryId(2L);
        request.setName("精品莓果");
        request.setCode("premium-berry");
        request.setSortOrder(6);
        request.setStatus(1);
        when(categoryMapper.findById(2L))
                .thenReturn(category(2L, "缤纷莓果", "berry-selection"),
                        category(2L, "精品莓果", "premium-berry"));

        CategoryEntity result = categoryService.update(request);

        assertEquals("premium-berry", result.getCode());
        verify(categoryMapper).update(any(CategoryEntity.class));
    }

    @Test
    void rejectsDeletingCategoryWithProducts() {
        when(categoryMapper.findById(1L)).thenReturn(category(1L, "新鲜热卖", "fresh-hot"));
        when(categoryMapper.countProducts(1L)).thenReturn(5L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> categoryService.delete(1L));

        assertEquals(ErrorCode.CATEGORY_HAS_PRODUCTS, exception.getErrorCode());
        verify(categoryMapper, never()).deleteById(1L);
    }

    @Test
    void deletesEmptyCategory() {
        when(categoryMapper.findById(8L)).thenReturn(category(8L, "临时类目", "temporary"));
        when(categoryMapper.countProducts(8L)).thenReturn(0L);

        categoryService.delete(8L);

        verify(categoryMapper).deleteById(8L);
    }

    private static AdminCategoryCreateRequest createRequest(String name, String code) {
        AdminCategoryCreateRequest request = new AdminCategoryCreateRequest();
        request.setName(name);
        request.setCode(code);
        request.setSortOrder(6);
        request.setStatus(1);
        return request;
    }

    private static CategoryEntity category(Long id, String name, String code) {
        CategoryEntity category = new CategoryEntity();
        category.setId(id);
        category.setName(name);
        category.setCode(code);
        category.setSortOrder(1);
        category.setStatus(1);
        return category;
    }
}
