package com.novel.controller;

import com.novel.dto.ApiResponse;
import com.novel.model.User;
import com.novel.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    public ApiResponse<User> getUserInfo(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        if (user != null) {
            return ApiResponse.success(user);
        }
        return ApiResponse.error(404, "用户不存在");
    }
}
