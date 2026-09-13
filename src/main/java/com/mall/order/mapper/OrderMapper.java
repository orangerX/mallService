package com.mall.order.mapper;

import com.mall.order.model.OrderEntity;
import com.mall.order.model.OrderItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {

    int insertOrder(OrderEntity order);

    int insertItems(@Param("items") List<OrderItemEntity> items);

    List<OrderEntity> findByUserId(@Param("userId") Long userId);

    OrderEntity findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<OrderItemEntity> findItemsByOrderId(@Param("orderId") Long orderId);
}
