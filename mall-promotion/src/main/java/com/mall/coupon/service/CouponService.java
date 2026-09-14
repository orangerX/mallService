package com.mall.coupon.service;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.coupon.dto.CouponResponse;
import com.mall.coupon.dto.UserCouponResponse;
import com.mall.coupon.mapper.CouponMapper;
import com.mall.coupon.model.CouponEntity;
import com.mall.coupon.model.UserCouponEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CouponService {

    private final CouponMapper couponMapper;

    public CouponService(CouponMapper couponMapper) {
        this.couponMapper = couponMapper;
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public List<CouponResponse> listAvailable() {
        return couponMapper.findAvailable(LocalDateTime.now()).stream()
                .map(CouponResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public UserCouponResponse receive(Long userId, Long couponId) {
        CouponEntity coupon = couponMapper.findById(couponId);
        if (coupon == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.COUPON_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        validateAvailable(coupon, now);
        if (couponMapper.findByUserAndCoupon(userId, couponId) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.COUPON_ALREADY_RECEIVED);
        }
        if (couponMapper.increaseReceivedQuantity(couponId, now) == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.COUPON_UNAVAILABLE);
        }

        UserCouponEntity userCoupon = new UserCouponEntity();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(UserCouponEntity.STATUS_UNUSED);
        userCoupon.setReceivedAt(now);
        copyCouponFields(userCoupon, coupon);
        couponMapper.insertUserCoupon(userCoupon);
        return UserCouponResponse.from(userCoupon);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<UserCouponResponse> listMine(Long userId, Integer status) {
        LocalDateTime now = LocalDateTime.now();
        couponMapper.expireByUserId(userId, now);
        return couponMapper.findByUserId(userId, status).stream()
                .map(UserCouponResponse::from)
                .collect(Collectors.toList());
    }

    private static void validateAvailable(CouponEntity coupon, LocalDateTime now) {
        boolean unavailable = coupon.getStatus() == null
                || coupon.getStatus() != CouponEntity.STATUS_ENABLED
                || coupon.getStartAt() == null || now.isBefore(coupon.getStartAt())
                || coupon.getEndAt() == null || now.isAfter(coupon.getEndAt())
                || coupon.getReceivedQuantity() == null || coupon.getTotalQuantity() == null
                || coupon.getReceivedQuantity() >= coupon.getTotalQuantity();
        if (unavailable) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.COUPON_UNAVAILABLE);
        }
    }

    private static void copyCouponFields(UserCouponEntity target, CouponEntity coupon) {
        target.setCouponName(coupon.getName());
        target.setThresholdAmount(coupon.getThresholdAmount());
        target.setDiscountAmount(coupon.getDiscountAmount());
        target.setStartAt(coupon.getStartAt());
        target.setEndAt(coupon.getEndAt());
        target.setCouponStatus(coupon.getStatus());
    }
}
