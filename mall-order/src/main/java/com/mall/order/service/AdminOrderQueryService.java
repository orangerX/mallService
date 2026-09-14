package com.mall.order.service;

import com.mall.common.api.ErrorCode;
import com.mall.common.api.PageResponse;
import com.mall.common.exception.BusinessException;
import com.mall.order.dto.AdminOrderDetailResponse;
import com.mall.order.dto.AdminOrderResponse;
import com.mall.order.mapper.OrderMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminOrderQueryService {
    private final OrderMapper orderMapper;

    public AdminOrderQueryService(OrderMapper orderMapper) { this.orderMapper = orderMapper; }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public PageResponse<AdminOrderResponse> page(String orderNo, Long userId, Integer status,
                                                 int page, int size) {
        String normalized = orderNo == null || orderNo.trim().isEmpty() ? null : orderNo.trim();
        return new PageResponse<>(orderMapper.findAdminPage(normalized, userId, status,
                (page - 1) * size, size), page, size,
                orderMapper.countAdminPage(normalized, userId, status));
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public AdminOrderDetailResponse detail(Long orderId, String baseUrl) {
        AdminOrderResponse order = orderMapper.findAdminById(orderId);
        if (order == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.ORDER_NOT_FOUND);
        }
        List<com.mall.order.model.OrderItemEntity> items = orderMapper.findItemsByOrderId(orderId);
        items.forEach(item -> item.setProductImagePath(absolute(baseUrl, item.getProductImagePath())));
        return new AdminOrderDetailResponse(order, items);
    }

    private static String absolute(String baseUrl, String path) {
        if (path == null || path.startsWith("http://") || path.startsWith("https://")) return path;
        return baseUrl + (path.startsWith("/") ? path : "/" + path);
    }
}
