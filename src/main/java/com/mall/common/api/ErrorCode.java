package com.mall.common.api;

public enum ErrorCode {
    VALIDATION_ERROR("VALIDATION_ERROR", "请求参数不正确"),
    USERNAME_EXISTS("USERNAME_EXISTS", "用户名已存在"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "用户名或密码错误"),
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", "账号已禁用"),
    USER_NOT_FOUND("USER_NOT_FOUND", "用户不存在"),
    ADDRESS_NOT_FOUND("ADDRESS_NOT_FOUND", "收货地址不存在"),
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", "商品不存在"),
    PRODUCT_UNAVAILABLE("PRODUCT_UNAVAILABLE", "商品已下架"),
    SKU_NOT_FOUND("SKU_NOT_FOUND", "商品规格不存在"),
    SKU_UNAVAILABLE("SKU_UNAVAILABLE", "商品规格已下架"),
    INSUFFICIENT_STOCK("INSUFFICIENT_STOCK", "商品库存不足"),
    CART_ITEM_NOT_FOUND("CART_ITEM_NOT_FOUND", "购物车中不存在该商品"),
    CART_QUANTITY_LIMIT("CART_QUANTITY_LIMIT", "单个商品最多加入 99 件"),
    CART_EMPTY("CART_EMPTY", "购物车为空"),
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "订单不存在"),
    COUPON_NOT_FOUND("COUPON_NOT_FOUND", "优惠券不存在"),
    COUPON_UNAVAILABLE("COUPON_UNAVAILABLE", "优惠券不可领取或已领完"),
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
