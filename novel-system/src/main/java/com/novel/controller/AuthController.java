package com.novel.controller;

import com.novel.dto.ApiResponse;
import com.novel.dto.LoginRequest;
import com.novel.dto.LoginResponse;
import com.novel.dto.RegisterRequest;
import com.novel.model.User;
import com.novel.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 处理用户登录、注册和获取当前用户信息的请求
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    /**
     * 构造方法
     * @param userService 用户服务
     */
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户登录
     * @param request 登录请求
     * @return 登录响应
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ApiResponse.success(response);
    }

    /**
     * 用户注册
     * @param request 注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return ApiResponse.success("注册成功", null);
    }

    /**
     * 获取当前登录用户信息
     * @param user 当前认证用户
     * @return 用户信息
     */
    @GetMapping("/me")
    public ApiResponse<User> getCurrentUser(@AuthenticationPrincipal User user) {
        return ApiResponse.success(user);
    }
}
