package com.mall.admin.security;

public class AdminTokenValidationException extends RuntimeException {
    public AdminTokenValidationException(String message) { super(message); }
    public AdminTokenValidationException(String message, Throwable cause) { super(message, cause); }
}
