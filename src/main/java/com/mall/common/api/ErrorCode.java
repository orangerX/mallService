package com.mall.common.api;

public enum ErrorCode {
    VALIDATION_ERROR("VALIDATION_ERROR", "请求参数不正确"),
    USERNAME_EXISTS("USERNAME_EXISTS", "用户名已存在"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "用户名或密码错误"),
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", "账号已禁用"),
    USER_NOT_FOUND("USER_NOT_FOUND", "用户不存在"),
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
