package com.mall.admin.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = {
        "com.mall.admin.mapper",
        "com.mall.address.mapper",
        "com.mall.banner.mapper",
        "com.mall.cart.mapper",
        "com.mall.coupon.mapper",
        "com.mall.order.mapper",
        "com.mall.product.mapper",
        "com.mall.user.mapper"
})
public class AdminMyBatisConfig {
}
