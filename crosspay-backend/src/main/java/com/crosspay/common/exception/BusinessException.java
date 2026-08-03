package com.crosspay.common.exception;

/**
 * 业务异常
 * 支付系统的错误需要明确区分：参数错误、业务规则不允许、第三方错误
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
