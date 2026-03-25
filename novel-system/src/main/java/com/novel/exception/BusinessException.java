package com.novel.exception;

/**
 * 业务逻辑异常
 * 用于表示业务规则违反的异常情况
 */
public class BusinessException extends RuntimeException {
    
    private final int code;
    
    /**
     * 构造业务异常
     * @param message 异常信息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }
    
    /**
     * 构造业务异常
     * @param code 错误码
     * @param message 异常信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
    
    /**
     * 构造业务异常
     * @param message 异常信息
     * @param cause 原始异常
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 400;
    }
    
    /**
     * 获取错误码
     * @return 错误码
     */
    public int getCode() {
        return code;
    }
}
