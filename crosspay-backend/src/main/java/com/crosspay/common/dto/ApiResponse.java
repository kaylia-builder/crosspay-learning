package com.crosspay.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一 API 响应格式
 * 所有接口返回都用这个包装，前端统一处理
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ---- 工厂方法 ----

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(200, "success", null);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    // ---- Getters ----

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}
