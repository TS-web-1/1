package com.novel.dto;

/**
 * ApiResponse通用响应类 - 统一API响应格式
 * 
 * 该类用于封装所有API接口的响应数据，包括：
 * - 响应状态码（code）
 * - 响应消息（message）
 * - 响应数据（data）
 * 
 * 提供静态工厂方法快速创建成功或失败的响应
 */
public class ApiResponse<T> {
    private Integer code;
    private String message;
    private T data;

    public ApiResponse() {
        this.code = 200;
        this.message = "success";
    }

    public ApiResponse(T data) {
        this();
        this.data = data;
    }

    /**
     * 创建成功响应
     * @param data 响应数据
     * @return ApiResponse对象
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data);
    }

    /**
     * 创建带消息的成功响应
     * @param message 响应消息
     * @param data 响应数据
     * @return ApiResponse对象
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>(data);
        response.setMessage(message);
        return response;
    }

    /**
     * 创建错误响应
     * @param code 错误码
     * @param message 错误消息
     * @return ApiResponse对象
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }

    /**
     * 创建默认错误响应（错误码500）
     * @param message 错误消息
     * @return ApiResponse对象
     */
    public static <T> ApiResponse<T> error(String message) {
        return error(500, message);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
