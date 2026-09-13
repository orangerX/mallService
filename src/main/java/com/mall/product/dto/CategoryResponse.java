package com.mall.product.dto;

import com.mall.product.model.CategoryEntity;

public class CategoryResponse {

    private final Long id;
    private final String name;
    private final String code;
    private final Integer sortOrder;

    public CategoryResponse(Long id, String name, String code, Integer sortOrder) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.sortOrder = sortOrder;
    }

    public static CategoryResponse from(CategoryEntity category) {
        return new CategoryResponse(
                category.getId(), category.getName(), category.getCode(), category.getSortOrder()
        );
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }
}
