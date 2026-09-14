package com.mall.product.mapper;

import com.mall.product.model.ProductSkuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductSkuMapper {

    List<ProductSkuEntity> findOnSaleByProductId(@Param("productId") Long productId);

    List<ProductSkuEntity> findAllByProductId(@Param("productId") Long productId);

    ProductSkuEntity findById(@Param("id") Long id);

    ProductSkuEntity findByIdForUpdate(@Param("id") Long id);

    ProductSkuEntity findByCode(@Param("skuCode") String skuCode);

    ProductSkuEntity findByProductAndName(@Param("productId") Long productId,
                                          @Param("skuName") String skuName);

    ProductSkuEntity findFirstOther(@Param("productId") Long productId, @Param("id") Long id);

    ProductSkuEntity findFirstOtherOnSale(@Param("productId") Long productId, @Param("id") Long id);

    int insert(ProductSkuEntity sku);

    int update(ProductSkuEntity sku);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    int clearDefaultByProductId(@Param("productId") Long productId);

    int setDefault(@Param("id") Long id);

    long countByProductId(@Param("productId") Long productId);

    long countOrderItems(@Param("id") Long id);

    int deleteById(@Param("id") Long id);

    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
