package com.novel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * LoginRequest登录请求类 - 用户登录请求参数封装
 * 
 * 该类用于接收用户登录请求的参数，包括：
 * - 用户名（必填）
 * - 密码（必填，至少6位）
 * - 应用类型（用于区分用户端、作者端、管理端）
 */
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少6位")
    private String password;
    
    private String appType;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getAppType() {
        return appType;
    }
    
    public void setAppType(String appType) {
        this.appType = appType;
    }
}
