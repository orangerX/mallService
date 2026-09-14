package com.mall.product.service;

import com.mall.product.dto.CategoryResponse;
import com.mall.product.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<CategoryResponse> listHomeCategories() {
        return categoryMapper.findEnabledCategories().stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }
}
