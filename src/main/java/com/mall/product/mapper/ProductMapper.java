package com.mall.product.mapper;

import com.mall.product.model.ProductEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    List<ProductEntity> findOnShelfByCategoryId(@Param("categoryId") Long categoryId);

    ProductEntity findById(@Param("id") Long id);
}
