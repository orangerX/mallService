package com.mall.product.mapper;

import com.mall.product.model.CategoryEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CategoryMapper {

    List<CategoryEntity> findEnabledCategories();
}
