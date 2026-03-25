/**
 * 用户状态管理Store - Pinia Store
 * 
 * 该文件使用Pinia定义用户状态管理，包括：
 * - 用户Token管理
 * - 用户信息管理
 * - 登录状态判断
 * - 用户角色判断
 * - 支持多应用类型（用户端、作者端、管理端）
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const TOKEN_KEY = 'token'
const ADMIN_TOKEN_KEY = 'adminToken'
const AUTHOR_TOKEN_KEY = 'authorToken'
const USER_INFO_KEY = 'userInfo'
const ADMIN_USER_INFO_KEY = 'adminUserInfo'
const AUTHOR_USER_INFO_KEY = 'authorUserInfo'

/**
 * 获取当前应用类型
 * @returns {string} 应用类型（user/author/admin）
 */
function getAppType() {
  return import.meta.env.VITE_APP_TYPE || 'user'
}

/**
 * 根据应用类型获取Token存储键名
 * @param {string} appType - 应用类型
 * @returns {string} Token键名
 */
function getTokenKeyByAppType(appType) {
  switch (appType) {
    case 'author':
      return AUTHOR_TOKEN_KEY
    case 'admin':
      return ADMIN_TOKEN_KEY
    default:
      return TOKEN_KEY
  }
}

/**
 * 根据应用类型获取用户信息存储键名
 * @param {string} appType - 应用类型
 * @returns {string} 用户信息键名
 */
function getUserInfoKeyByAppType(appType) {
  switch (appType) {
    case 'author':
      return AUTHOR_USER_INFO_KEY
    case 'admin':
      return ADMIN_USER_INFO_KEY
    default:
      return USER_INFO_KEY
  }
}

/**
 * 从localStorage获取存储的Token
 * @returns {string} Token字符串
 */
function getStoredToken() {
  const appType = getAppType()
  const tokenKey = getTokenKeyByAppType(appType)
  return localStorage.getItem(tokenKey) || ''
}

/**
 * 从localStorage获取存储的用户信息
 * @returns {Object} 用户信息对象
 */
function getStoredUserInfo() {
  try {
    const appType = getAppType()
    const userInfoKey = getUserInfoKeyByAppType(appType)
    const stored = localStorage.getItem(userInfoKey)
    return stored ? JSON.parse(stored) : {}
  } catch {
    return {}
  }
}

/**
 * 用户Store定义
 */
export const useUserStore = defineStore('user', () => {
  const token = ref(getStoredToken())
  const userInfo = ref(getStoredUserInfo())

  const isLoggedIn = computed(() => !!token.value)
  const userRole = computed(() => userInfo.value?.role || 'USER')

  /**
   * 设置Token
   * @param {string} newToken - 新Token
   */
  function setToken(newToken) {
    token.value = newToken
    const appType = getAppType()
    const tokenKey = getTokenKeyByAppType(appType)
    localStorage.setItem(tokenKey, newToken)
  }

  /**
   * 设置用户信息
   * @param {Object} info - 用户信息对象
   */
  function setUserInfo(info) {
    userInfo.value = info
    const appType = getAppType()
    const userInfoKey = getUserInfoKeyByAppType(appType)
    localStorage.setItem(userInfoKey, JSON.stringify(info))
  }

  /**
   * 退出登录
   * 清除Token和用户信息
   */
  function logout() {
    token.value = ''
    userInfo.value = {}
    const appType = getAppType()
    const tokenKey = getTokenKeyByAppType(appType)
    const userInfoKey = getUserInfoKeyByAppType(appType)
    localStorage.removeItem(tokenKey)
    localStorage.removeItem(userInfoKey)
  }

  /**
   * 检查用户是否具有指定角色
   * @param {string} role - 角色名称
   * @returns {boolean} 是否具有该角色
   */
  function hasRole(role) {
    const currentRole = (userInfo.value?.role || 'USER').toUpperCase()
    return currentRole === role.toUpperCase()
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    userRole,
    setToken,
    setUserInfo,
    logout,
    hasRole
  }
})
