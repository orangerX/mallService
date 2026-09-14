package com.mall.coupon.service;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.coupon.dto.AdminCouponCreateRequest;
import com.mall.coupon.dto.AdminCouponUpdateRequest;
import com.mall.coupon.mapper.CouponMapper;
import com.mall.coupon.model.CouponEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCouponServiceTest {
    @Mock CouponMapper couponMapper;
    private AdminCouponService service;

    @BeforeEach
    void setUp() {
        service = new AdminCouponService(couponMapper);
    }

    @Test
    void createsFullReductionCoupon() {
        AdminCouponCreateRequest request = createRequest();
        doAnswer(invocation -> {
            CouponEntity coupon = invocation.getArgument(0);
            coupon.setId(8L);
            return 1;
        }).when(couponMapper).insertCoupon(any(CouponEntity.class));
        when(couponMapper.findById(8L)).thenReturn(coupon(8L, 0));

        CouponEntity result = service.create(request);

        assertEquals(8L, result.getId());
        verify(couponMapper).insertCoupon(org.mockito.ArgumentMatchers.argThat(coupon ->
                "周末鲜果券".equals(coupon.getName())
                        && new BigDecimal("10.00").equals(coupon.getDiscountAmount())
                        && coupon.getReceivedQuantity() == 0));
    }

    @Test
    void rejectsDiscountNotLowerThanThreshold() {
        AdminCouponCreateRequest request = createRequest();
        request.setDiscountAmount(new BigDecimal("100.00"));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(request));

        assertEquals(ErrorCode.COUPON_RULE_INVALID, exception.getErrorCode());
        verify(couponMapper, never()).insertCoupon(any(CouponEntity.class));
    }

    @Test
    void rejectsInvalidPeriod() {
        AdminCouponCreateRequest request = createRequest();
        request.setEndAt(request.getStartAt());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.create(request));

        assertEquals(ErrorCode.COUPON_PERIOD_INVALID, exception.getErrorCode());
    }

    @Test
    void rejectsQuantityBelowAlreadyReceived() {
        CouponEntity existing = coupon(2L, 50);
        when(couponMapper.findByIdForUpdate(2L)).thenReturn(existing);
        AdminCouponUpdateRequest request = updateRequest(existing);
        request.setTotalQuantity(49);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.update(request));

        assertEquals(ErrorCode.COUPON_QUANTITY_INVALID, exception.getErrorCode());
    }

    @Test
    void locksRuleAfterCouponWasReceived() {
        CouponEntity existing = coupon(2L, 1);
        when(couponMapper.findByIdForUpdate(2L)).thenReturn(existing);
        AdminCouponUpdateRequest request = updateRequest(existing);
        request.setDiscountAmount(new BigDecimal("11.00"));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.update(request));

        assertEquals(ErrorCode.COUPON_RULE_LOCKED, exception.getErrorCode());
        verify(couponMapper, never()).updateCoupon(any(CouponEntity.class));
    }

    @Test
    void rejectsDeletingCouponWithReceipts() {
        when(couponMapper.findByIdForUpdate(2L)).thenReturn(coupon(2L, 1));
        when(couponMapper.countUserCoupons(2L)).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.delete(2L));

        assertEquals(ErrorCode.COUPON_HAS_RECEIPTS, exception.getErrorCode());
        verify(couponMapper, never()).deleteCoupon(2L);
    }

    @Test
    void deletesCouponWithoutReceipts() {
        when(couponMapper.findByIdForUpdate(8L)).thenReturn(coupon(8L, 0));

        service.delete(8L);

        verify(couponMapper).deleteCoupon(8L);
    }

    private static AdminCouponCreateRequest createRequest() {
        AdminCouponCreateRequest request = new AdminCouponCreateRequest();
        request.setName(" 周末鲜果券 ");
        request.setThresholdAmount(new BigDecimal("100.00"));
        request.setDiscountAmount(new BigDecimal("10.00"));
        request.setTotalQuantity(500);
        request.setStartAt(LocalDateTime.of(2026, 9, 1, 0, 0));
        request.setEndAt(LocalDateTime.of(2026, 10, 1, 0, 0));
        request.setStatus(1);
        request.setSortOrder(1);
        return request;
    }

    private static AdminCouponUpdateRequest updateRequest(CouponEntity coupon) {
        AdminCouponUpdateRequest request = new AdminCouponUpdateRequest();
        request.setCouponId(coupon.getId());
        request.setName(coupon.getName());
        request.setThresholdAmount(coupon.getThresholdAmount());
        request.setDiscountAmount(coupon.getDiscountAmount());
        request.setTotalQuantity(coupon.getTotalQuantity());
        request.setStartAt(coupon.getStartAt());
        request.setEndAt(coupon.getEndAt());
        request.setStatus(coupon.getStatus());
        request.setSortOrder(coupon.getSortOrder());
        return request;
    }

    private static CouponEntity coupon(Long id, int received) {
        CouponEntity coupon = new CouponEntity();
        coupon.setId(id);
        coupon.setName("周末鲜果券");
        coupon.setThresholdAmount(new BigDecimal("100.00"));
        coupon.setDiscountAmount(new BigDecimal("10.00"));
        coupon.setTotalQuantity(500);
        coupon.setReceivedQuantity(received);
        coupon.setStartAt(LocalDateTime.of(2026, 9, 1, 0, 0));
        coupon.setEndAt(LocalDateTime.of(2026, 10, 1, 0, 0));
        coupon.setStatus(1);
        coupon.setSortOrder(1);
        return coupon;
    }
}
