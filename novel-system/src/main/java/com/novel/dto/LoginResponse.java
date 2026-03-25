package com.novel.dto;

/**
 * LoginResponse登录响应类 - 登录成功后的响应数据封装
 * 
 * 该类用于封装登录成功后返回给前端的数据，包括：
 * - JWT令牌（token）
 * - 用户信息（UserInfo）
 */
public class LoginResponse {
    private String token;
    private UserInfo userInfo;

    public LoginResponse(String token, UserInfo userInfo) {
        this.token = token;
        this.userInfo = userInfo;
    }

    public LoginResponse(String token, Long userId, String username, String email, String role) {
        this.token = token;
        this.userInfo = new UserInfo(userId, username, email, role);
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    /**
     * UserInfo内部类 - 用户基本信息
     * 用于在登录响应中返回用户的基本信息
     */
    public static class UserInfo {
        private Long id;
        private String username;
        private String email;
        private String role;
        private String avatar;

        public UserInfo(Long id, String username, String email, String role) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.role = role;
            this.avatar = "";
        }

        public UserInfo(Long id, String username, String email, String role, String avatar) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.role = role;
            this.avatar = avatar;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }
    }
}
