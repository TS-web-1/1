package com.novel.service;

import com.novel.dto.LoginRequest;
import com.novel.dto.LoginResponse;
import com.novel.dto.RegisterRequest;
import com.novel.model.User;
import com.novel.repository.UserRepository;
import com.novel.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        // 检查密码 - 支持明文密码（开发环境）和加密密码
        String inputPassword = request.getPassword();
        String storedPassword = user.getPassword();
        boolean passwordMatches;
        
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            // 加密密码，使用passwordEncoder验证
            passwordMatches = passwordEncoder.matches(inputPassword, storedPassword);
        } else {
            // 明文密码，直接比较（仅用于开发环境）
            passwordMatches = inputPassword.equals(storedPassword);
        }
        
        if (!passwordMatches) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (user.getIsBanned()) {
            throw new RuntimeException("该账号已被封禁");
        }

        // 根据应用类型验证角色权限
        String appType = request.getAppType();
        String userRole = user.getRole().toUpperCase();
        
        if ("admin".equals(appType)) {
            // 管理后端只有管理员可以登录
            if (!"ADMIN".equals(userRole)) {
                throw new RuntimeException("该账号没有管理员权限");
            }
        } else if ("author".equals(appType)) {
            // 作者端只有作者可以登录
            if (!"AUTHOR".equals(userRole)) {
                throw new RuntimeException("该账号不是作者账号");
            }
        } else {
            // 用户端只有普通用户可以登录
            if (!"USER".equals(userRole)) {
                throw new RuntimeException("请使用" + ("AUTHOR".equals(userRole) ? "作家" : "管理员") + "入口登录");
            }
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getId());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setIsBanned(false);
        
        // 根据用户类型设置角色
        String userType = request.getUserType();
        if ("author".equals(userType)) {
            user.setRole("AUTHOR");
        } else if ("admin".equals(userType)) {
            user.setRole("ADMIN");
        } else {
            user.setRole("USER");
        }

        userRepository.save(user);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在：" + id));
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在：" + username));
    }
}
