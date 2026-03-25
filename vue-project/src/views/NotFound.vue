<script setup>
/**
 * NotFound.vue - 404页面组件
 * 
 * 该组件实现了404错误页面，包括：
 * - 错误提示展示
 * - 返回首页按钮
 */
import { useRouter } from 'vue-router'

const router = useRouter()

/**
 * 返回首页
 */
const goHome = () => {
  router.push('/')
}
</script>

<template>
  <div class="not-found">
    <div class="not-found-container">
      <h1>404</h1>
      <h2>页面未找到</h2>
      <p>抱歉，您访问的页面不存在。</p>
      <button class="btn-primary" @click="goHome">返回首页</button>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
  background-color: #f5f5f5;
}

.not-found-container {
  text-align: center;
  padding: 40px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.not-found-container h1 {
  font-size: 72px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 20px;
}

.not-found-container h2 {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 10px;
}

.not-found-container p {
  font-size: 16px;
  color: #666;
  margin-bottom: 30px;
}

.btn-primary {
  padding: 10px 24px;
  background-color: #409EFF;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.btn-primary:hover {
  background-color: #66B1FF;
}
</style>