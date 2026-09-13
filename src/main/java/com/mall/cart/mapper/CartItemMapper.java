package com.mall.cart.mapper;

import com.mall.cart.model.CartItemDetailEntity;
import com.mall.cart.model.CartItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartItemMapper {

    List<CartItemDetailEntity> findDetailsByUserId(@Param("userId") Long userId);

    CartItemEntity findByUserAndSku(@Param("userId") Long userId,
                                    @Param("skuId") Long skuId);

    int increaseQuantity(@Param("userId") Long userId, @Param("skuId") Long skuId,
                         @Param("quantity") Integer quantity);

    int updateQuantity(@Param("userId") Long userId, @Param("skuId") Long skuId,
                       @Param("quantity") Integer quantity);

    int deleteItem(@Param("userId") Long userId, @Param("skuId") Long skuId);

    int deleteByUserId(@Param("userId") Long userId);
}
