<script setup>
/**
 * Register.vue - 用户注册页面组件
 *
 * 该组件实现了用户注册功能，包括：
 * - 用户身份选择（普通用户/作家）
 * - 用户名、邮箱、密码输入
 * - 密码确认验证
 * - 注册成功后跳转到登录页
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api/modules/user.js'

const router = useRouter()
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const email = ref('')
const userType = ref('user')
const loading = ref(false)

const userTypeOptions = [
  { value: 'user', label: '用户', icon: 'user', desc: '阅读小说、参与社区' },
  { value: 'author', label: '作家', icon: 'pen', desc: '发布作品、管理创作' }
]

/**
 * 处理用户注册
 * 验证密码一致性和长度、调用注册API
 */
const handleRegister = async () => {
  if (password.value !== confirmPassword.value) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  if (password.value.length < 6) {
    ElMessage.warning('密码长度至少6位')
    return
  }

  loading.value = true
  try {
    const response = await authApi.register({
      username: username.value,
      password: password.value,
      email: email.value || null,
      userType: userType.value
    })

    if (response.code === 200) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } else {
      ElMessage.error(response.message || '注册失败')
    }
  } catch (error) {
    console.error('注册失败:', error)
    const errorMessage = error.response?.data?.message || error.message || '注册失败'
    ElMessage.error(errorMessage)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-header">
        <h1>加入墨香书阁</h1>
        <p>开启您的阅读之旅</p>
      </div>

      <form class="register-form" @submit.prevent="handleRegister">
        <div class="user-type-selector">
          <label class="selector-label">选择注册身份</label>
          <div class="type-options">
            <div
              v-for="option in userTypeOptions"
              :key="option.value"
              class="type-option"
              :class="{ active: userType === option.value }"
              @click="userType = option.value"
            >
              <div class="type-icon">
                <svg
                  v-if="option.icon === 'user'"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                  <circle cx="12" cy="7" r="4"></circle>
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 19l7-7 3 3-7 7-3-3z"></path>
                  <path d="M18 13l-1.5-7.5L2 2l3.5 14.5L13 18l5-5z"></path>
                  <path d="M2 2l7.586 7.586"></path>
                  <circle cx="11" cy="11" r="2"></circle>
                </svg>
              </div>
              <div class="type-info">
                <span class="type-label">{{ option.label }}</span>
                <span class="type-desc">{{ option.desc }}</span>
              </div>
              <div v-if="userType === option.value" class="type-check">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                  <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
              </div>
            </div>
          </div>
        </div>

        <div class="form-group">
          <label for="username">用户名</label>
          <div class="input-wrapper">
            <svg
              class="input-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
            <input
              id="username"
              v-model="username"
              type="text"
              placeholder="请输入用户名"
              required
            />
          </div>
        </div>

        <div class="form-group">
          <label for="email">邮箱（可选）</label>
          <div class="input-wrapper">
            <svg
              class="input-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"
              ></path>
              <polyline points="22,6 12,13 2,6"></polyline>
            </svg>
            <input id="email" v-model="email" type="email" placeholder="请输入邮箱" />
          </div>
        </div>

        <div class="form-group">
          <label for="password">密码</label>
          <div class="input-wrapper">
            <svg
              class="input-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
            </svg>
            <input
              id="password"
              v-model="password"
              type="password"
              placeholder="请输入密码（至少6位）"
              required
            />
          </div>
        </div>

        <div class="form-group">
          <label for="confirmPassword">确认密码</label>
          <div class="input-wrapper">
            <svg
              class="input-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
            </svg>
            <input
              id="confirmPassword"
              v-model="confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              required
            />
          </div>
        </div>

        <button type="submit" class="btn-register" :disabled="loading">
          <span v-if="loading" class="loading-spinner"></span>
          {{ loading ? '注册中...' : '立即注册' }}
        </button>

        <div class="register-footer">
          <p>已有账号？<RouterLink to="/login">立即登录</RouterLink></p>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.register-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 40px 24px;
  background: linear-gradient(180deg, #0d1117 0%, #161b22 100%);
  position: relative;
  overflow: hidden;
}

.register-page::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    radial-gradient(ellipse at 20% 20%, rgba(201, 169, 98, 0.05) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 80%, rgba(201, 169, 98, 0.03) 0%, transparent 50%);
  pointer-events: none;
}

.register-container {
  background: rgba(22, 27, 34, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  padding: 48px;
  width: 100%;
  max-width: 480px;
  position: relative;
  z-index: 1;
  animation: slideUp 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.register-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #c9a962 0%, #d4b978 50%, #c9a962 100%);
  border-radius: 24px 24px 0 0;
}

@keyframes slideUp {
  from {
    transform: translateY(30px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.register-header {
  text-align: center;
  margin-bottom: 36px;
}

.register-header h1 {
  font-size: 32px;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 8px 0;
  font-family: 'Noto Serif SC', 'Playfair Display', serif;
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, #ffffff 0%, #c9a962 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.register-header p {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.user-type-selector {
  margin-bottom: 8px;
}

.selector-label {
  display: block;
  margin-bottom: 12px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  font-weight: 500;
}

.type-options {
  display: flex;
  gap: 12px;
}

.type-option {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.type-option:hover {
  background: rgba(201, 169, 98, 0.05);
  border-color: rgba(201, 169, 98, 0.2);
}

.type-option.active {
  background: rgba(201, 169, 98, 0.1);
  border-color: rgba(201, 169, 98, 0.4);
}

.type-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(201, 169, 98, 0.1);
  border-radius: 10px;
  flex-shrink: 0;
}

.type-icon svg {
  width: 22px;
  height: 22px;
  color: #c9a962;
}

.type-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.type-label {
  font-size: 15px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.type-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.type-check {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 20px;
  height: 20px;
  background: linear-gradient(135deg, #c9a962 0%, #d4b978 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.type-check svg {
  width: 12px;
  height: 12px;
  color: #0d1117;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  font-weight: 500;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 14px;
  width: 18px;
  height: 18px;
  color: rgba(255, 255, 255, 0.3);
  pointer-events: none;
}

.form-group input {
  width: 100%;
  padding: 14px 14px 14px 44px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  font-size: 15px;
  color: #ffffff;
  transition: all 0.3s ease;
  box-sizing: border-box;
}

.form-group input::placeholder {
  color: rgba(255, 255, 255, 0.3);
}

.form-group input:focus {
  outline: none;
  border-color: rgba(201, 169, 98, 0.5);
  background: rgba(201, 169, 98, 0.05);
  box-shadow: 0 0 0 3px rgba(201, 169, 98, 0.1);
}

.btn-register {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  padding: 16px;
  background: linear-gradient(135deg, #c9a962 0%, #d4b978 100%);
  color: #0d1117;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 8px 24px rgba(201, 169, 98, 0.3);
  margin-top: 8px;
}

.btn-register:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(201, 169, 98, 0.4);
}

.btn-register:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(13, 17, 23, 0.3);
  border-top-color: #0d1117;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.register-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.register-footer p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

.register-footer a {
  color: #c9a962;
  text-decoration: none;
  font-weight: 600;
  transition: color 0.3s ease;
}

.register-footer a:hover {
  color: #d4b978;
  text-decoration: underline;
}

@media (max-width: 480px) {
  .register-container {
    padding: 32px 24px;
  }

  .register-header h1 {
    font-size: 26px;
  }

  .type-options {
    flex-direction: column;
  }

  .type-option {
    padding: 14px;
  }
}
</style>
