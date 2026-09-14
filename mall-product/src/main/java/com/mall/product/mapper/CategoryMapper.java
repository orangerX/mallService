package com.mall.product.mapper;

import com.mall.product.model.CategoryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CategoryMapper {

    List<CategoryEntity> findEnabledCategories();

    List<CategoryEntity> findAllCategories();

    CategoryEntity findById(@Param("id") Long id);

    CategoryEntity findByCode(@Param("code") String code);

    int insert(CategoryEntity category);

    int update(CategoryEntity category);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    long countProducts(@Param("id") Long id);

    int deleteById(@Param("id") Long id);
}
