package com.mall.coupon.service;

import com.mall.common.api.PageResponse;
import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.coupon.mapper.CouponMapper;
import com.mall.coupon.model.CouponEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

@Service
public class AdminCouponQueryService {
    private final CouponMapper couponMapper;

    public AdminCouponQueryService(CouponMapper couponMapper) { this.couponMapper = couponMapper; }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public PageResponse<CouponEntity> page(String name, Integer status, int page, int size) {
        String normalized = name == null || name.trim().isEmpty() ? null : name.trim();
        return new PageResponse<>(couponMapper.findAdminPage(normalized, status, (page - 1) * size, size),
                page, size, couponMapper.countAdminPage(normalized, status));
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public CouponEntity detail(Long couponId) {
        CouponEntity coupon = couponMapper.findById(couponId);
        if (coupon == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.COUPON_NOT_FOUND);
        }
        return coupon;
    }
}
