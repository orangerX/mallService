package com.mall.product.controller;

import com.mall.common.api.ApiResponse;
import com.mall.product.dto.CategoryResponse;
import com.mall.product.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/home")
    public ApiResponse<List<CategoryResponse>> home() {
        return ApiResponse.success(categoryService.listHomeCategories());
    }
}
