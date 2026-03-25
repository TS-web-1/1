<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Header from './components/Header.vue'
import Footer from './components/Footer.vue'

const route = useRoute()

// 获取应用类型
const appType = import.meta.env.VITE_APP_TYPE || 'user'

// 管理后台和登录注册页面不显示 Header 和 Footer
const showLayout = computed(() => {
  if (appType === 'admin') return false
  // 登录和注册页面也不显示 Header 和 Footer
  if (route.path.startsWith('/login') || route.path.startsWith('/register')) return false
  return true
})

// 根据路由动态设置背景类
const getBackgroundClass = () => {
  const path = route.path

  if (appType === 'admin') return 'bg-admin'
  if (appType === 'author') return 'bg-author'

  if (path === '/') return 'bg-home'
  if (path.startsWith('/category')) return 'bg-category'
  if (path.startsWith('/novel/')) return 'bg-detail'
  if (path.startsWith('/reader/')) return 'bg-reader'
  if (path === '/profile') return 'bg-profile'
  if (path === '/bookshelf') return 'bg-bookshelf'
  if (path === '/community') return 'bg-community'
  if (path === '/search') return 'bg-search'
  if (path.startsWith('/login') || path.startsWith('/register')) return 'bg-auth'

  return 'bg-home'
}
</script>

<template>
  <div class="app-container" :class="getBackgroundClass()">
    <Header v-if="showLayout" />
    <main :class="['main-content', { 'no-layout': !showLayout }]">
      <RouterView />
    </main>
    <Footer v-if="showLayout" />
  </div>
</template>

<style>
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html,
body {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  overflow-x: hidden;
}

body {
  margin: 0;
  padding: 0;
  width: 100%;
  min-width: 100vw;
  overflow-x: hidden;
  font-family: Arial, sans-serif;
}

#app {
  width: 100%;
  min-width: 100vw;
}

.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 100%;
  margin: 0;
  padding: 0;
  transition: background 0.3s ease;
}

/* 背景类样式 */
.app-container.bg-home,
.app-container.bg-category,
.app-container.bg-detail,
.app-container.bg-reader,
.app-container.bg-profile,
.app-container.bg-bookshelf,
.app-container.bg-community,
.app-container.bg-search,
.app-container.bg-auth,
.app-container.bg-admin,
.app-container.bg-author {
  position: relative;
}

/* 确保内容在背景之上 */
.main-content {
  position: relative;
  z-index: 1;
}

.main-content {
  flex: 1;
  padding: 0;
  margin: 0;
  min-height: calc(100vh - 140px); /* 60px 头部 + 80px 底部 = 140px */
  width: 100%;
  max-width: 100%;
}

@media (min-width: 1024px) {
  .main-content {
    padding: 0;
    margin: 0;
    min-height: calc(100vh - 140px);
    width: 100%;
    max-width: 100%;
  }
}

/* 管理后台全屏样式 */
.main-content.no-layout {
  margin-top: 0;
  margin-bottom: 0;
  min-height: 100vh;
}
</style>
