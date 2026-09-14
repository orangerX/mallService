package com.mall.product.mapper;

import com.mall.product.model.ProductEntity;
import com.mall.product.dto.AdminProductResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    List<ProductEntity> findOnShelfByCategoryId(@Param("categoryId") Long categoryId);

    ProductEntity findById(@Param("id") Long id);

    ProductEntity findByIdForUpdate(@Param("id") Long id);

    long countAdminPage(@Param("keyword") String keyword, @Param("categoryId") Long categoryId,
                        @Param("status") Integer status);

    List<AdminProductResponse> findAdminPage(@Param("keyword") String keyword,
                                             @Param("categoryId") Long categoryId,
                                             @Param("status") Integer status,
                                             @Param("offset") int offset, @Param("size") int size);

    AdminProductResponse findAdminById(@Param("id") Long id);

    int insert(ProductEntity product);

    int update(ProductEntity product);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    long countOrderItems(@Param("id") Long id);

    int deleteById(@Param("id") Long id);
}
