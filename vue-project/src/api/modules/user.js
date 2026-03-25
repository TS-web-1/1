import request from '../api.js'

// 用户认证相关API
export const authApi = {
  // 登录
  login(data) {
    return request.post('/auth/login', data)
  },

  // 注册
  register(data) {
    return request.post('/auth/register', data)
  },

  // 获取当前用户信息
  getCurrentUser() {
    return request.get('/auth/me')
  }
}

// 用户管理相关API
export const userApi = {
  // 获取用户信息
  getUserInfo(userId) {
    return request.get(`/users/${userId}`)
  },

  // 更新用户信息
  updateUserInfo(userId, data) {
    return request.put(`/users/${userId}`, data)
  },

  // 修改密码
  changePassword(data) {
    return request.post('/users/change-password', data)
  }
}

// 用户行为相关API
export const userActionApi = {
  // 记录用户行为
  recordAction(data) {
    return request.post('/actions', data)
  },

  // 获取用户行为历史
  getUserActions(userId) {
    return request.get(`/actions/user/${userId}`)
  },

  // 获取小说行为记录
  getNovelActions(novelId) {
    return request.get(`/actions/novel/${novelId}`)
  }
}

// 默认导出所有用户相关API
export default {
  auth: authApi,
  user: userApi,
  action: userActionApi
}
