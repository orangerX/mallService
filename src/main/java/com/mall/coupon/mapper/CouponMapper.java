package com.mall.coupon.mapper;

import com.mall.coupon.model.CouponEntity;
import com.mall.coupon.model.UserCouponEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CouponMapper {

    List<CouponEntity> findAvailable(@Param("now") LocalDateTime now);
    CouponEntity findById(@Param("id") Long id);
    UserCouponEntity findByUserAndCoupon(@Param("userId") Long userId,
                                         @Param("couponId") Long couponId);
    int increaseReceivedQuantity(@Param("couponId") Long couponId,
                                 @Param("now") LocalDateTime now);
    int insertUserCoupon(UserCouponEntity userCoupon);
    int expireByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
    List<UserCouponEntity> findByUserId(@Param("userId") Long userId,
                                        @Param("status") Integer status);
    UserCouponEntity findUserCouponForUpdate(@Param("id") Long id,
                                              @Param("userId") Long userId);
    int markUsed(@Param("id") Long id, @Param("userId") Long userId,
                 @Param("orderId") Long orderId, @Param("usedAt") LocalDateTime usedAt);
}
