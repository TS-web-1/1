﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿<script setup>
// 导入Vue相关的响应式API和路由
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'

// 初始化路由实例
const router = useRouter()

// 应用类型，默认为用户端
const appType = import.meta.env.VITE_APP_TYPE || 'user'
// 搜索关键词
const searchQuery = ref('')
// 用户菜单显示状态
const showUserMenu = ref(false)
// 用户菜单DOM引用
const userMenuWrapper = ref(null)
// 页面滚动状态
const isScrolled = ref(false)

/**
 * 获取用户信息在本地存储中的键名
 * @returns {string} 用户信息的键名
 */
const getUserInfoKey = () => {
  switch (appType) {
    case 'author':
      return 'authorUserInfo'
    case 'admin':
      return 'adminUserInfo'
    default:
      return 'userInfo'
  }
}

/**
 * 从本地存储获取管理员用户信息
 * @returns {Object} 管理员用户信息
 */
const getAdminUserInfoFromStorage = () => {
  try {
    const userInfo = JSON.parse(localStorage.getItem('adminUserInfo') || '{}')
    return userInfo
  } catch {
    return {}
  }
}

/**
 * 从本地存储获取用户名
 * @returns {string} 用户名
 */
const getUsernameFromStorage = () => {
  try {
    const userInfoKey = getUserInfoKey()
    const userInfo = JSON.parse(localStorage.getItem(userInfoKey) || '{}')
    return userInfo.username || '用户'
  } catch {
    return '用户'
  }
}

/**
 * 从本地存储获取用户头像
 * @returns {string} 头像URL
 */
const getAvatarFromStorage = () => {
  try {
    const userInfoKey = getUserInfoKey()
    const userInfo = JSON.parse(localStorage.getItem(userInfoKey) || '{}')
    return userInfo.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${getUsernameFromStorage()}`
  } catch {
    return `https://api.dicebear.com/7.x/avataaars/svg?seed=user`
  }
}

/**
 * 从本地存储获取用户角色
 * @returns {string} 用户角色
 */
const getUserRoleFromStorage = () => {
  try {
    const userInfoKey = getUserInfoKey()
    const userInfo = JSON.parse(localStorage.getItem(userInfoKey) || '{}')
    return userInfo.role || 'USER'
  } catch {
    return 'USER'
  }
}

/**
 * 获取Token在本地存储中的键名
 * @returns {string} Token的键名
 */
const getTokenKey = () => {
  switch (appType) {
    case 'author':
      return 'authorToken'
    case 'admin':
      return 'adminToken'
    default:
      return 'token'
  }
}

// 登录状态
const isLoggedIn = ref(localStorage.getItem(getTokenKey()) !== null)
// 用户名
const username = ref(isLoggedIn.value ? getUsernameFromStorage() : '用户')
// 用户角色
const userRole = ref(isLoggedIn.value ? getUserRoleFromStorage() : 'USER')

/**
 * 更新登录状态
 */
const updateLoginStatus = () => {
  isLoggedIn.value = localStorage.getItem(getTokenKey()) !== null
  if (isLoggedIn.value) {
    username.value = getUsernameFromStorage()
    userRole.value = getUserRoleFromStorage()
  } else {
    username.value = '用户'
    userRole.value = 'USER'
  }
}

// 登录状态检查定时器
let loginStatusInterval = null

/**
 * 处理页面滚动事件
 */
const handleScroll = () => {
  isScrolled.value = window.scrollY > 20
}

/**
 * 处理搜索操作
 */
const handleSearch = () => {
  if (searchQuery.value.trim()) {
    router.push({ path: '/search', query: { q: searchQuery.value } })
  }
}

/**
 * 处理退出登录
 */
const logout = () => {
  const tokenKey = getTokenKey()
  const userInfoKey = getUserInfoKey()
  localStorage.removeItem(tokenKey)
  localStorage.removeItem(userInfoKey)
  localStorage.removeItem('userRole')
  showUserMenu.value = false
  
  if (appType === 'author') {
    // 作者端退出登录，清除所有 token 和用户信息后跳转到用户端首页
    localStorage.removeItem('token')
    localStorage.removeItem('adminToken')
    localStorage.removeItem('authorToken')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('userRole')
    // 使用 replaceState 清除浏览器历史记录，避免后退回到作者端
    window.location.replace('http://localhost:8081/')
  } else if (appType === 'admin') {
    window.location.href = '/login'
  } else {
    window.location.href = '/'
  }
}

/**
 * 处理用户点击事件
 */
const handleUserClick = () => {
  showUserMenu.value = !showUserMenu.value
}

/**
 * 处理点击菜单外部的事件
 * @param {Event} event 点击事件
 */
const handleClickOutside = (event) => {
  if (userMenuWrapper.value && !userMenuWrapper.value.contains(event.target)) {
    showUserMenu.value = false
  }
}

/**
 * 检查登录状态并导航
 * @param {string} path 导航路径
 */
const checkLoginAndNavigate = (path) => {
  if (!isLoggedIn.value) {
    localStorage.setItem('redirectAfterLogin', path)
    router.push('/login')
  } else {
    router.push(path)
  }
}

/**
 * 跳转到作家中心
 */
const goToAuthorCenter = () => {
  if (!isLoggedIn.value) {
    window.open('http://localhost:8082/login', '_blank')
  } else if (userRole.value === 'AUTHOR') {
    window.open('http://localhost:8082', '_blank')
  } else {
    alert('您当前不是作家账号，请在作者端使用作家账号登录')
    window.open('http://localhost:8082/login', '_blank')
  }
}

/**
 * 跳转到管理后台
 */
const goToAdmin = () => {
  if (!isLoggedIn.value) {
    window.open('http://localhost:8083/login', '_blank')
  } else if (userRole.value === 'ADMIN') {
    window.open('http://localhost:8083', '_blank')
  } else {
    alert('您当前不是管理员账号，请在管理后端使用管理员账号登录')
    window.open('http://localhost:8083/login', '_blank')
  }
}

/**
 * 组件挂载时的生命周期钩子
 */
onMounted(() => {
  // 添加点击外部关闭菜单的事件监听
  document.addEventListener('click', handleClickOutside)
  // 添加滚动事件监听
  window.addEventListener('scroll', handleScroll)
  // 定时检查登录状态
  loginStatusInterval = setInterval(updateLoginStatus, 500)
})

/**
 * 组件卸载时的生命周期钩子
 */
onUnmounted(() => {
  // 移除事件监听
  document.removeEventListener('click', handleClickOutside)
  window.removeEventListener('scroll', handleScroll)
  // 清除定时器
  if (loginStatusInterval) {
    clearInterval(loginStatusInterval)
  }
})
</script>

<template>
  <header :class="['header', { scrolled: isScrolled }]">
    <div class="header-inner">
      <RouterLink to="/" class="logo">
        <svg class="logo-icon" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/>
        </svg>
        <span class="logo-text">
          {{ appType === 'author' ? '作家中心' : appType === 'admin' ? '管理后台' : '墨香书阁' }}
        </span>
      </RouterLink>
      
      <nav v-if="appType === 'user'" class="nav-links">
        <RouterLink to="/" class="nav-link">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
            <polyline points="9 22 9 12 15 12 15 22"/>
          </svg>
          <span>首页</span>
        </RouterLink>
        <RouterLink to="/category/xuanhuan" class="nav-link">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="7" height="7"/>
            <rect x="14" y="3" width="7" height="7"/>
            <rect x="14" y="14" width="7" height="7"/>
            <rect x="3" y="14" width="7" height="7"/>
          </svg>
          <span>分类</span>
        </RouterLink>
        <RouterLink to="/community" class="nav-link">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
          <span>社区</span>
        </RouterLink>
      </nav>

      <div class="header-actions">
        <div v-if="appType === 'user'" class="search-box">
          <svg class="search-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <path d="M21 21l-4.35-4.35"/>
          </svg>
          <input 
            type="text" 
            v-model="searchQuery" 
            placeholder="搜索小说、作者..."
            @keyup.enter="handleSearch"
            aria-label="搜索小说"
          />
        </div>

        <div class="user-menu-wrapper" ref="userMenuWrapper">
          <template v-if="!isLoggedIn">
            <button class="user-btn login-btn" @click="router.push('/login')">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
              <span>登录</span>
            </button>
          </template>
          <template v-else>
            <button 
              class="user-btn" 
              @click="appType === 'author' ? handleUserClick() : checkLoginAndNavigate('/profile')"
            >
              <img :src="getAvatarFromStorage()" alt="头像" class="user-avatar" />
              <span class="user-name">{{ username }}</span>
              <svg v-if="appType === 'author'" class="dropdown-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M6 9l6 6 6-6"/>
              </svg>
            </button>
            
            <Transition name="menu-fade">
              <div v-if="showUserMenu && appType === 'author'" class="user-dropdown">
                <button class="dropdown-item" @click="router.push('/author'); showUserMenu = false">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
                  </svg>
                  作品管理
                </button>
                <div class="dropdown-divider"></div>
                <button class="dropdown-item logout" @click="logout">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                    <polyline points="16 17 21 12 16 7"/>
                    <line x1="21" y1="12" x2="9" y2="12"/>
                  </svg>
                  退出登录
                </button>
              </div>
            </Transition>
          </template>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(13, 17, 23, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  transition: all 0.3s ease;
}

.header.scrolled {
  background: rgba(13, 17, 23, 0.95);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
}

.header-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 40px;
  height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  color: #f0f6fc;
}

.logo-icon {
  color: #c9a962;
}

.logo-text {
  font-family: 'Playfair Display', 'Noto Serif SC', serif;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 1px;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  color: #8b949e;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.nav-link:hover {
  color: #c9d1d9;
  background: rgba(255, 255, 255, 0.05);
}

.nav-link.router-link-active {
  color: #c9a962;
  background: rgba(201, 169, 98, 0.1);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 14px;
  color: #6e7681;
  pointer-events: none;
}

.search-box input {
  width: 280px;
  height: 42px;
  padding: 0 16px 0 44px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  color: #c9d1d9;
  font-size: 14px;
  transition: all 0.3s ease;
}

.search-box input::placeholder {
  color: #6e7681;
}

.search-box input:focus {
  outline: none;
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(201, 169, 98, 0.3);
  box-shadow: 0 0 0 3px rgba(201, 169, 98, 0.1);
}

.user-menu-wrapper {
  position: relative;
}

.user-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  color: #c9d1d9;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.user-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(201, 169, 98, 0.3);
}

.user-btn.login-btn {
  background: linear-gradient(135deg, #c9a962 0%, #d4b978 100%);
  border: none;
  color: #0d1117;
  font-weight: 600;
}

.user-btn.login-btn:hover {
  box-shadow: 0 4px 15px rgba(201, 169, 98, 0.3);
  transform: translateY(-1px);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid rgba(201, 169, 98, 0.3);
}

.user-name {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-icon {
  color: #6e7681;
  transition: transform 0.3s ease;
}

.user-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 180px;
  background: rgba(22, 27, 34, 0.98);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 8px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.4);
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px 14px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: #c9d1d9;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dropdown-item:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #f0f6fc;
}

.dropdown-item.logout {
  color: #f85149;
}

.dropdown-item.logout:hover {
  background: rgba(248, 81, 73, 0.1);
}

.dropdown-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.06);
  margin: 8px 0;
}

.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: all 0.2s ease;
}

.menu-fade-enter-from,
.menu-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@media (max-width: 1024px) {
  .header-inner {
    padding: 0 24px;
  }
  
  .search-box input {
    width: 220px;
  }
}

@media (max-width: 768px) {
  .header-inner {
    padding: 0 16px;
    height: 64px;
  }
  
  .logo-text {
    font-size: 18px;
  }
  
  .nav-links {
    display: none;
  }
  
  .search-box {
    display: none;
  }
  
  .user-name {
    display: none;
  }
  
  .user-btn {
    padding: 8px;
  }
}
</style>
