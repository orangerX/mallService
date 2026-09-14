package com.mall.order.service;

import com.mall.common.api.ErrorCode;
import com.mall.common.api.PageResponse;
import com.mall.common.exception.BusinessException;
import com.mall.order.dto.AdminOrderDetailResponse;
import com.mall.order.dto.AdminOrderResponse;
import com.mall.order.mapper.OrderMapper;
import com.mall.order.model.OrderItemEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderQueryServiceTest {
    @Mock OrderMapper orderMapper;
    private AdminOrderQueryService service;

    @BeforeEach
    void setUp() {
        service = new AdminOrderQueryService(orderMapper);
    }

    @Test
    void pagesOrdersAndNormalizesOrderNumber() {
        when(orderMapper.findAdminPage("ORDER-1", 2L, 1, 20, 10))
                .thenReturn(Collections.emptyList());
        when(orderMapper.countAdminPage("ORDER-1", 2L, 1)).thenReturn(31L);

        PageResponse<AdminOrderResponse> result = service.page("  ORDER-1  ", 2L, 1, 3, 10);

        assertEquals(3, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(31L, result.getTotal());
    }

    @Test
    void returnsOrderItemsWithAbsoluteImageUrl() {
        AdminOrderResponse order = new AdminOrderResponse();
        order.setId(8L);
        OrderItemEntity item = new OrderItemEntity();
        item.setProductImagePath("/images/products/apple.jpg");
        when(orderMapper.findAdminById(8L)).thenReturn(order);
        when(orderMapper.findItemsByOrderId(8L)).thenReturn(Collections.singletonList(item));

        AdminOrderDetailResponse result = service.detail(8L, "http://127.0.0.1:8081");

        assertEquals("http://127.0.0.1:8081/images/products/apple.jpg",
                result.getItems().get(0).getProductImagePath());
    }

    @Test
    void rejectsMissingOrder() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.detail(99L, "http://127.0.0.1:8081"));

        assertEquals(ErrorCode.ORDER_NOT_FOUND, exception.getErrorCode());
        verify(orderMapper).findAdminById(99L);
    }
}
