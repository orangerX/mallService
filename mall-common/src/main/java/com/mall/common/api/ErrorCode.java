package com.mall.common.api;

public enum ErrorCode {
    VALIDATION_ERROR("VALIDATION_ERROR", "请求参数不正确"),
    USERNAME_EXISTS("USERNAME_EXISTS", "用户名已存在"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "用户名或密码错误"),
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", "账号已禁用"),
    USER_NOT_FOUND("USER_NOT_FOUND", "用户不存在"),
    USER_HAS_ORDERS("USER_HAS_ORDERS", "用户存在历史订单，不能删除"),
    ADDRESS_NOT_FOUND("ADDRESS_NOT_FOUND", "收货地址不存在"),
    CATEGORY_NOT_FOUND("CATEGORY_NOT_FOUND", "类目不存在"),
    CATEGORY_CODE_EXISTS("CATEGORY_CODE_EXISTS", "类目编码已存在"),
    CATEGORY_HAS_PRODUCTS("CATEGORY_HAS_PRODUCTS", "类目下存在商品，不能删除"),
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", "商品不存在"),
    PRODUCT_UNAVAILABLE("PRODUCT_UNAVAILABLE", "商品已下架"),
    PRODUCT_HAS_ORDERS("PRODUCT_HAS_ORDERS", "商品已有历史订单，不能删除"),
    PRODUCT_REQUIRES_SKU("PRODUCT_REQUIRES_SKU", "商品至少需要保留一个 SKU"),
    PRODUCT_REQUIRES_ACTIVE_SKU("PRODUCT_REQUIRES_ACTIVE_SKU", "商品至少需要保留一个在售 SKU"),
    SKU_NOT_FOUND("SKU_NOT_FOUND", "商品规格不存在"),
    SKU_UNAVAILABLE("SKU_UNAVAILABLE", "商品规格已下架"),
    SKU_CODE_EXISTS("SKU_CODE_EXISTS", "SKU 编码已存在"),
    SKU_NAME_EXISTS("SKU_NAME_EXISTS", "同一商品下的 SKU 名称已存在"),
    SKU_HAS_ORDERS("SKU_HAS_ORDERS", "SKU 已有历史订单，不能删除"),
    DEFAULT_SKU_CONFLICT("DEFAULT_SKU_CONFLICT", "每个商品只能有一个默认 SKU"),
    INSUFFICIENT_STOCK("INSUFFICIENT_STOCK", "商品库存不足"),
    CART_ITEM_NOT_FOUND("CART_ITEM_NOT_FOUND", "购物车中不存在该商品"),
    CART_QUANTITY_LIMIT("CART_QUANTITY_LIMIT", "单个商品最多加入 99 件"),
    CART_EMPTY("CART_EMPTY", "购物车为空"),
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "订单不存在"),
    COUPON_NOT_FOUND("COUPON_NOT_FOUND", "优惠券不存在"),
    COUPON_UNAVAILABLE("COUPON_UNAVAILABLE", "优惠券不可领取或已领完"),
    COUPON_RULE_INVALID("COUPON_RULE_INVALID", "优惠金额必须小于满减门槛"),
    COUPON_PERIOD_INVALID("COUPON_PERIOD_INVALID", "优惠券结束时间必须晚于开始时间"),
    COUPON_QUANTITY_INVALID("COUPON_QUANTITY_INVALID", "发行数量不能小于已领取数量"),
    COUPON_RULE_LOCKED("COUPON_RULE_LOCKED", "优惠券已被领取，不能修改满减规则或开始时间"),
    COUPON_HAS_RECEIPTS("COUPON_HAS_RECEIPTS", "优惠券已有领取记录，不能删除"),
    COUPON_ALREADY_RECEIVED("COUPON_ALREADY_RECEIVED", "该优惠券已领取"),
    COUPON_NOT_APPLICABLE("COUPON_NOT_APPLICABLE", "优惠券不可用于当前订单"),
    DATA_CONFLICT("DATA_CONFLICT", "数据冲突，请稍后重试"),
    TOKEN_REFRESH_REJECTED("TOKEN_REFRESH_REJECTED", "刷新令牌无效或已使用"),
    UNAUTHORIZED("UNAUTHORIZED", "未认证或令牌无效"),
    FORBIDDEN("FORBIDDEN", "无权访问该资源"),
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "仅支持 GET 和 POST 请求"),
    INTERNAL_ERROR("INTERNAL_ERROR", "服务器内部错误");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
