package com.mall.coupon.service;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.coupon.dto.AdminCouponCreateRequest;
import com.mall.coupon.dto.AdminCouponUpdateRequest;
import com.mall.coupon.mapper.CouponMapper;
import com.mall.coupon.model.CouponEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class AdminCouponService {
    private final CouponMapper couponMapper;

    public AdminCouponService(CouponMapper couponMapper) {
        this.couponMapper = couponMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public CouponEntity create(AdminCouponCreateRequest request) {
        validateRule(request.getThresholdAmount(), request.getDiscountAmount(),
                request.getStartAt(), request.getEndAt());
        CouponEntity coupon = coupon(request.getName(), request.getThresholdAmount(),
                request.getDiscountAmount(), request.getTotalQuantity(), request.getStartAt(),
                request.getEndAt(), request.getStatus(), request.getSortOrder());
        coupon.setReceivedQuantity(0);
        couponMapper.insertCoupon(coupon);
        return required(coupon.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public CouponEntity update(AdminCouponUpdateRequest request) {
        CouponEntity existing = requiredForUpdate(request.getCouponId());
        validateRule(request.getThresholdAmount(), request.getDiscountAmount(),
                request.getStartAt(), request.getEndAt());
        if (request.getTotalQuantity() < existing.getReceivedQuantity()) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.COUPON_QUANTITY_INVALID);
        }
        if (existing.getReceivedQuantity() > 0 && ruleChanged(existing, request)) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.COUPON_RULE_LOCKED);
        }
        CouponEntity coupon = coupon(request.getName(), request.getThresholdAmount(),
                request.getDiscountAmount(), request.getTotalQuantity(), request.getStartAt(),
                request.getEndAt(), request.getStatus(), request.getSortOrder());
        coupon.setId(request.getCouponId());
        couponMapper.updateCoupon(coupon);
        return required(coupon.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public CouponEntity changeStatus(Long couponId, Integer status) {
        requiredForUpdate(couponId);
        couponMapper.updateCouponStatus(couponId, status);
        return required(couponId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long couponId) {
        requiredForUpdate(couponId);
        if (couponMapper.countUserCoupons(couponId) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.COUPON_HAS_RECEIPTS);
        }
        couponMapper.deleteCoupon(couponId);
    }

    private static void validateRule(BigDecimal thresholdAmount, BigDecimal discountAmount,
                                     java.time.LocalDateTime startAt, java.time.LocalDateTime endAt) {
        if (discountAmount.compareTo(thresholdAmount) >= 0) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.COUPON_RULE_INVALID);
        }
        if (!endAt.isAfter(startAt)) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.COUPON_PERIOD_INVALID);
        }
    }

    private static boolean ruleChanged(CouponEntity existing, AdminCouponUpdateRequest request) {
        return existing.getThresholdAmount().compareTo(request.getThresholdAmount()) != 0
                || existing.getDiscountAmount().compareTo(request.getDiscountAmount()) != 0
                || !existing.getStartAt().equals(request.getStartAt());
    }

    private CouponEntity required(Long couponId) {
        CouponEntity coupon = couponMapper.findById(couponId);
        if (coupon == null) throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.COUPON_NOT_FOUND);
        return coupon;
    }

    private CouponEntity requiredForUpdate(Long couponId) {
        CouponEntity coupon = couponMapper.findByIdForUpdate(couponId);
        if (coupon == null) throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.COUPON_NOT_FOUND);
        return coupon;
    }

    private static CouponEntity coupon(String name, BigDecimal thresholdAmount,
                                       BigDecimal discountAmount, Integer totalQuantity,
                                       java.time.LocalDateTime startAt, java.time.LocalDateTime endAt,
                                       Integer status, Integer sortOrder) {
        CouponEntity coupon = new CouponEntity();
        coupon.setName(name.trim());
        coupon.setThresholdAmount(thresholdAmount.setScale(2, RoundingMode.HALF_UP));
        coupon.setDiscountAmount(discountAmount.setScale(2, RoundingMode.HALF_UP));
        coupon.setTotalQuantity(totalQuantity);
        coupon.setStartAt(startAt);
        coupon.setEndAt(endAt);
        coupon.setStatus(status);
        coupon.setSortOrder(sortOrder);
        return coupon;
    }
}
