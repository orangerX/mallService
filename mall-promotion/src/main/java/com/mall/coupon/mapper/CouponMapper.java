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
    CouponEntity findByIdForUpdate(@Param("id") Long id);
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

    long countAdminPage(@Param("name") String name, @Param("status") Integer status);

    List<CouponEntity> findAdminPage(@Param("name") String name, @Param("status") Integer status,
                                     @Param("offset") int offset, @Param("size") int size);

    int insertCoupon(CouponEntity coupon);
    int updateCoupon(CouponEntity coupon);
    int updateCouponStatus(@Param("id") Long id, @Param("status") Integer status);
    long countUserCoupons(@Param("id") Long id);
    int deleteCoupon(@Param("id") Long id);
}
