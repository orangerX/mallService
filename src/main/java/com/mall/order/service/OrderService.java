package com.mall.order.service;

import com.mall.cart.mapper.CartItemMapper;
import com.mall.cart.model.CartItemDetailEntity;
import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.order.dto.OrderItemResponse;
import com.mall.order.dto.OrderResponse;
import com.mall.order.dto.OrderSummaryResponse;
import com.mall.order.dto.SubmitOrderRequest;
import com.mall.order.mapper.OrderMapper;
import com.mall.order.model.OrderEntity;
import com.mall.order.model.OrderItemEntity;
import com.mall.product.mapper.ProductSkuMapper;
import com.mall.product.model.ProductEntity;
import com.mall.product.model.ProductSkuEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final DateTimeFormatter ORDER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final OrderMapper orderMapper;
    private final CartItemMapper cartItemMapper;
    private final ProductSkuMapper productSkuMapper;

    public OrderService(OrderMapper orderMapper, CartItemMapper cartItemMapper,
                        ProductSkuMapper productSkuMapper) {
        this.orderMapper = orderMapper;
        this.cartItemMapper = cartItemMapper;
        this.productSkuMapper = productSkuMapper;
    }

    @Transactional
    public OrderResponse submit(Long userId, SubmitOrderRequest request, String baseUrl) {
        List<CartItemDetailEntity> cartItems = cartItemMapper.findDetailsByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.CART_EMPTY);
        }
        cartItems.forEach(OrderService::validateCartItem);

        LocalDateTime now = LocalDateTime.now();
        OrderEntity order = createOrder(userId, request, cartItems, now);
        orderMapper.insertOrder(order);

        List<OrderItemEntity> orderItems = cartItems.stream()
                .map(item -> createOrderItem(order.getId(), item, now))
                .collect(Collectors.toList());
        for (OrderItemEntity item : orderItems) {
            if (productSkuMapper.decreaseStock(item.getSkuId(), item.getQuantity()) == 0) {
                throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.INSUFFICIENT_STOCK);
            }
        }
        orderMapper.insertItems(orderItems);
        cartItemMapper.deleteByUserId(userId);
        return toResponse(order, orderItems, baseUrl);
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> list(Long userId) {
        return orderMapper.findByUserId(userId).stream()
                .map(OrderService::toSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse detail(Long userId, Long orderId, String baseUrl) {
        OrderEntity order = orderMapper.findByIdAndUserId(orderId, userId);
        if (order == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.ORDER_NOT_FOUND);
        }
        return toResponse(order, orderMapper.findItemsByOrderId(orderId), baseUrl);
    }

    private static OrderEntity createOrder(Long userId, SubmitOrderRequest request,
                                           List<CartItemDetailEntity> cartItems, LocalDateTime now) {
        OrderEntity order = new OrderEntity();
        order.setOrderNo(generateOrderNo(now));
        order.setUserId(userId);
        order.setStatus(OrderEntity.STATUS_PENDING_PAYMENT);
        order.setTotalAmount(cartItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        order.setTotalQuantity(cartItems.stream().mapToInt(CartItemDetailEntity::getQuantity).sum());
        order.setReceiverName(request.getReceiverName().trim());
        order.setReceiverPhone(request.getReceiverPhone().trim());
        order.setReceiverAddress(request.getReceiverAddress().trim());
        String remark = request.getRemark();
        order.setRemark(remark == null || remark.trim().isEmpty() ? null : remark.trim());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        return order;
    }

    private static OrderItemEntity createOrderItem(Long orderId, CartItemDetailEntity item,
                                                   LocalDateTime now) {
        OrderItemEntity orderItem = new OrderItemEntity();
        orderItem.setOrderId(orderId);
        orderItem.setProductId(item.getProductId());
        orderItem.setSkuId(item.getSkuId());
        orderItem.setSkuCode(item.getSkuCode());
        orderItem.setSkuName(item.getSkuName());
        orderItem.setProductName(item.getProductName());
        orderItem.setProductImagePath(item.getProductImagePath());
        orderItem.setPrice(item.getPrice());
        orderItem.setOriginalPrice(item.getOriginalPrice());
        orderItem.setUnit(item.getUnit());
        orderItem.setQuantity(item.getQuantity());
        orderItem.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        orderItem.setCreatedAt(now);
        return orderItem;
    }

    private static void validateCartItem(CartItemDetailEntity item) {
        if (item.getProductStatus() == null || item.getProductStatus() != ProductEntity.STATUS_ON_SHELF) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.PRODUCT_UNAVAILABLE);
        }
        if (item.getSkuStatus() == null || item.getSkuStatus() != ProductSkuEntity.STATUS_ON_SALE) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.SKU_UNAVAILABLE);
        }
        if (item.getStock() == null || item.getStock() < item.getQuantity()) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    private static String generateOrderNo(LocalDateTime now) {
        String randomPart = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase(Locale.ROOT);
        return "M" + now.format(ORDER_TIME_FORMAT) + randomPart;
    }

    private static OrderSummaryResponse toSummary(OrderEntity order) {
        return new OrderSummaryResponse(
                order.getId(), order.getOrderNo(), order.getStatus(), statusText(order.getStatus()),
                order.getTotalAmount(), order.getTotalQuantity(), order.getCreatedAt()
        );
    }

    private static OrderResponse toResponse(OrderEntity order, List<OrderItemEntity> items, String baseUrl) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> new OrderItemResponse(
                        item.getProductId(), item.getSkuId(), item.getSkuCode(), item.getSkuName(),
                        item.getProductName(), baseUrl + item.getProductImagePath(),
                        item.getPrice(), item.getOriginalPrice(), item.getUnit(), item.getQuantity(),
                        item.getSubtotal()
                ))
                .collect(Collectors.toList());
        return new OrderResponse(
                order.getId(), order.getOrderNo(), order.getStatus(), statusText(order.getStatus()),
                order.getTotalAmount(), order.getTotalQuantity(), order.getReceiverName(),
                order.getReceiverPhone(), order.getReceiverAddress(), order.getRemark(),
                order.getCreatedAt(), itemResponses
        );
    }

    private static String statusText(Integer status) {
        return status != null && status == OrderEntity.STATUS_PENDING_PAYMENT ? "待支付" : "未知状态";
    }
}
