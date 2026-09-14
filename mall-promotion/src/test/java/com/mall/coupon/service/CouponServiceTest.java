package com.mall.coupon.service;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.coupon.dto.CouponResponse;
import com.mall.coupon.dto.UserCouponResponse;
import com.mall.coupon.mapper.CouponMapper;
import com.mall.coupon.model.CouponEntity;
import com.mall.coupon.model.UserCouponEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CouponServiceTest {

    @Test
    void listsAvailableFullReductionCoupons() {
        CouponMapper mapper = mock(CouponMapper.class);
        when(mapper.findAvailable(any(LocalDateTime.class))).thenReturn(Collections.singletonList(coupon()));
        CouponService service = new CouponService(mapper);

        List<CouponResponse> result = service.listAvailable();

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("99.00"), result.get(0).getThresholdAmount());
        assertEquals(new BigDecimal("15.00"), result.get(0).getDiscountAmount());
        assertEquals(900, result.get(0).getRemainingQuantity());
    }

    @Test
    void receivesCouponAtomically() {
        CouponMapper mapper = mock(CouponMapper.class);
        when(mapper.findById(2L)).thenReturn(coupon());
        when(mapper.increaseReceivedQuantity(any(Long.class), any(LocalDateTime.class))).thenReturn(1);
        when(mapper.insertUserCoupon(any(UserCouponEntity.class))).thenAnswer(invocation -> {
            UserCouponEntity userCoupon = invocation.getArgument(0);
            userCoupon.setId(8L);
            return 1;
        });
        CouponService service = new CouponService(mapper);

        UserCouponResponse response = service.receive(7L, 2L);

        assertEquals(8L, response.getUserCouponId());
        assertEquals("未使用", response.getStatusText());
        assertEquals(new BigDecimal("15.00"), response.getDiscountAmount());
        verify(mapper).insertUserCoupon(any(UserCouponEntity.class));
    }

    @Test
    void rejectsDuplicateReceipt() {
        CouponMapper mapper = mock(CouponMapper.class);
        when(mapper.findById(2L)).thenReturn(coupon());
        when(mapper.findByUserAndCoupon(7L, 2L)).thenReturn(new UserCouponEntity());
        CouponService service = new CouponService(mapper);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.receive(7L, 2L));

        assertEquals(ErrorCode.COUPON_ALREADY_RECEIVED, exception.getErrorCode());
        verify(mapper, never()).insertUserCoupon(any(UserCouponEntity.class));
    }

    @Test
    void rejectsSoldOutCoupon() {
        CouponMapper mapper = mock(CouponMapper.class);
        CouponEntity coupon = coupon();
        coupon.setReceivedQuantity(coupon.getTotalQuantity());
        when(mapper.findById(2L)).thenReturn(coupon);
        CouponService service = new CouponService(mapper);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.receive(7L, 2L));

        assertEquals(ErrorCode.COUPON_UNAVAILABLE, exception.getErrorCode());
    }

    private static CouponEntity coupon() {
        CouponEntity coupon = new CouponEntity();
        coupon.setId(2L);
        coupon.setName("水果畅享券");
        coupon.setThresholdAmount(new BigDecimal("99.00"));
        coupon.setDiscountAmount(new BigDecimal("15.00"));
        coupon.setTotalQuantity(1000);
        coupon.setReceivedQuantity(100);
        coupon.setStartAt(LocalDateTime.now().minusDays(1));
        coupon.setEndAt(LocalDateTime.now().plusDays(1));
        coupon.setStatus(CouponEntity.STATUS_ENABLED);
        coupon.setSortOrder(2);
        return coupon;
    }
}
