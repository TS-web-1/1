package com.novel.exception;

/**
 * 资源未找到异常
 * 用于表示请求的资源不存在的异常情况
 */
public class ResourceNotFoundException extends RuntimeException {
    
    /**
     * 构造资源未找到异常
     * @param message 异常信息
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    /**
     * 构造资源未找到异常
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     */
    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(String.format("%s 不存在: %s", resourceType, resourceId));
    }
    
    /**
     * 构造资源未找到异常
     * @param message 异常信息
     * @param cause 原始异常
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
