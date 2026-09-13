package com.mall.product.mapper;

import com.mall.product.model.ProductSkuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductSkuMapper {

    List<ProductSkuEntity> findOnSaleByProductId(@Param("productId") Long productId);

    ProductSkuEntity findById(@Param("id") Long id);

    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
