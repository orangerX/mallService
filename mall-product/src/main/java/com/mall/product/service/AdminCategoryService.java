package com.mall.product.service;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.product.dto.AdminCategoryCreateRequest;
import com.mall.product.dto.AdminCategoryUpdateRequest;
import com.mall.product.mapper.CategoryMapper;
import com.mall.product.model.CategoryEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCategoryService {
    private final CategoryMapper categoryMapper;

    public AdminCategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public CategoryEntity create(AdminCategoryCreateRequest request) {
        String code = request.getCode().trim();
        ensureCodeAvailable(code, null);
        CategoryEntity category = new CategoryEntity();
        category.setName(request.getName().trim());
        category.setCode(code);
        category.setSortOrder(request.getSortOrder());
        category.setStatus(request.getStatus());
        try {
            categoryMapper.insert(category);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.CATEGORY_CODE_EXISTS);
        }
        return required(category.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public CategoryEntity update(AdminCategoryUpdateRequest request) {
        required(request.getCategoryId());
        String code = request.getCode().trim();
        ensureCodeAvailable(code, request.getCategoryId());
        CategoryEntity category = new CategoryEntity();
        category.setId(request.getCategoryId());
        category.setName(request.getName().trim());
        category.setCode(code);
        category.setSortOrder(request.getSortOrder());
        category.setStatus(request.getStatus());
        try {
            categoryMapper.update(category);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.CATEGORY_CODE_EXISTS);
        }
        return required(category.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public CategoryEntity changeStatus(Long categoryId, Integer status) {
        required(categoryId);
        categoryMapper.updateStatus(categoryId, status);
        return required(categoryId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long categoryId) {
        required(categoryId);
        if (categoryMapper.countProducts(categoryId) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.CATEGORY_HAS_PRODUCTS);
        }
        categoryMapper.deleteById(categoryId);
    }

    private void ensureCodeAvailable(String code, Long currentId) {
        CategoryEntity category = categoryMapper.findByCode(code);
        if (category != null && !category.getId().equals(currentId)) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.CATEGORY_CODE_EXISTS);
        }
    }

    private CategoryEntity required(Long categoryId) {
        CategoryEntity category = categoryMapper.findById(categoryId);
        if (category == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.CATEGORY_NOT_FOUND);
        }
        return category;
    }
}
