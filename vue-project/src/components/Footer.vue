<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

// 进入作家中心
const goToAuthorCenter = () => {
  const token = localStorage.getItem('authorToken') || localStorage.getItem('token')
  const userInfoStr = localStorage.getItem('userInfo')
  const userInfo = userInfoStr ? JSON.parse(userInfoStr) : null
  const userRole = userInfo?.role || 'USER'
  
  console.log('goToAuthorCenter - token:', token ? '存在' : '不存在')
  console.log('goToAuthorCenter - userInfo:', userInfo)
  console.log('goToAuthorCenter - userRole:', userRole)
  
  if (!token) {
    console.log('未登录，跳转到作者端登录页')
    // 未登录，跳转到作者端登录页
    window.open('http://localhost:8082/login', '_blank')
  } else if (userRole === 'AUTHOR') {
    console.log('已登录且是作家，打开作者端')
    // 已登录且是作家，直接打开作者端
    window.open('http://localhost:8082', '_blank')
  } else {
    console.log('已登录但不是作家，提示并跳转')
    // 已登录但不是作家，提示并跳转到作者端登录页
    const message = '您当前不是作家账号，请在作者端使用作家账号登录'
    alert(message)
    window.open('http://localhost:8082/login', '_blank')
  }
}

// 进入管理后台
const goToAdmin = () => {
  const token = localStorage.getItem('adminToken') || localStorage.getItem('token')
  const userInfoStr = localStorage.getItem('userInfo')
  const userInfo = userInfoStr ? JSON.parse(userInfoStr) : null
  const userRole = userInfo?.role || 'USER'
  
  console.log('goToAdmin - token:', token ? '存在' : '不存在')
  console.log('goToAdmin - userInfo:', userInfo)
  console.log('goToAdmin - userRole:', userRole)
  
  if (!token) {
    console.log('未登录，跳转到管理后端登录页')
    // 未登录，跳转到管理后端登录页
    window.open('http://localhost:8083/login', '_blank')
  } else if (userRole === 'ADMIN') {
    console.log('已登录且是管理员，打开管理后端')
    // 已登录且是管理员，直接打开管理后端
    window.open('http://localhost:8083', '_blank')
  } else {
    console.log('已登录但不是管理员，提示并跳转')
    // 已登录但不是管理员，提示并跳转到管理后端登录页
    const message = '您当前不是管理员账号，请在管理后端使用管理员账号登录'
    alert(message)
    window.open('http://localhost:8083/login', '_blank')
  }
}
</script>

<template>
  <footer class="footer">
    <div class="footer-container">
      <div class="footer-about">
        <h3>关于我们</h3>
        <p>墨香书阁是一个提供优质小说内容的平台，致力于为用户提供良好的阅读体验。</p>
      </div>
      <div class="footer-links">
        <h3>快速链接</h3>
        <ul>
          <li><RouterLink to="/">首页</RouterLink></li>
          <li><RouterLink to="/search">搜索</RouterLink></li>
          <li><RouterLink to="/category/xuanhuan">分类</RouterLink></li>
          <li><RouterLink to="/community">社区</RouterLink></li>
          <li><a href="javascript:void(0)" @click="goToAuthorCenter">作家中心</a></li>
          <li><a href="javascript:void(0)" @click="goToAdmin">管理后台</a></li>
          <li><RouterLink to="/profile">个人中心</RouterLink></li>
        </ul>
      </div>
      <div class="footer-contact">
        <h3>联系我们</h3>
        <p>邮箱：contact@novel.com</p>
        <p>电话：123-456-7890</p>
      </div>
    </div>
    <div class="footer-bottom">
      <p>&copy; 2026 墨香书阁。保留所有权利.</p>
    </div>
  </footer>
</template>

<style scoped>
@import '../assets/theme.css';

.footer {
  background: linear-gradient(135deg, #1a1a2e 0%, #2d2d44 100%);
  padding: 60px 0 30px;
  margin-top: 60px;
  color: #ffffff;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.3);
}

.footer-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 var(--spacing-xl);
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--spacing-2xl);
}

.footer-about h3,
.footer-links h3,
.footer-contact h3 {
  margin-bottom: var(--spacing-lg);
  color: #ffffff;
  font-size: 18px;
  font-weight: 700;
  font-family: var(--font-serif);
  letter-spacing: 0.5px;
  position: relative;
  padding-bottom: var(--spacing-sm);
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.footer-about h3::after,
.footer-links h3::after,
.footer-contact h3::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 40px;
  height: 3px;
  background: var(--color-accent);
}

.footer-about p {
  color: #f0f0f0;
  line-height: 1.7;
  font-size: 15px;
  opacity: 1;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.footer-links ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.footer-links li {
  margin-bottom: var(--spacing-sm);
}

.footer-links a {
  color: #ffffff;
  text-decoration: none;
  font-size: 15px;
  transition: all var(--transition-fast);
  display: inline-block;
  position: relative;
  opacity: 1;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.footer-links a::before {
  content: '›';
  position: absolute;
  left: -15px;
  opacity: 0;
  color: var(--color-accent);
  transition: all var(--transition-fast);
}

.footer-links a:hover {
  color: var(--color-accent);
  padding-left: 10px;
}

.footer-links a:hover::before {
  opacity: 1;
}

.footer-contact p {
  color: #f0f0f0;
  margin-bottom: var(--spacing-sm);
  font-size: 15px;
  display: flex;
  align-items: center;
  gap: 8px;
  opacity: 1;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.footer-contact p::before {
  content: '●';
  font-size: 6px;
  color: var(--color-accent);
}

.footer-bottom {
  text-align: center;
  padding-top: var(--spacing-xl);
  margin-top: var(--spacing-2xl);
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  color: #ffffff;
  font-size: 14px;
  opacity: 1;
  font-weight: 500;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

@media (max-width: 768px) {
  .footer {
    padding: 40px 0 20px;
    margin-top: 40px;
  }
  
  .footer-container {
    padding: 0 var(--spacing-md);
    gap: var(--spacing-xl);
  }
  
  .footer-about h3,
  .footer-links h3,
  .footer-contact h3 {
    font-size: 16px;
  }
}
</style>