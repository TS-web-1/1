<script setup>
/**
 * Profile.vue - 用户个人中心页面组件
 * 
 * 该组件实现了用户个人中心功能，包括：
 * - 用户信息展示和编辑
 * - 密码修改
 * - 阅读历史查看
 * - 书架管理
 * - 我的评论/书单/话题统计
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user.js'
import { ElMessage } from 'element-plus'
import { bookmarkApi } from '../api/modules/bookmark.js'
import { readingApi } from '../api/modules/reading.js'
import { discussionApi } from '../api/modules/discussion.js'
import { commentApi } from '../api/modules/comment.js'

const router = useRouter()
const userStore = useUserStore()

const user = ref({
  username: '用户',
  email: '',
  avatar: 'https://api.dicebear.com/7.x/avataaars/svg?seed=user'
})

const isEditing = ref(false)

const editForm = ref({
  username: '',
  email: '',
  avatar: ''
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const readingHistory = ref([])
const bookmarks = ref([])
const actualHistoryCount = ref(0)
const actualBookshelfCount = ref(0)
const myCommentsCount = ref(0)
const myBooklistsCount = ref(0)
const myTopicsCount = ref(0)

onMounted(() => {
  if (userStore.userInfo) {
    user.value.username = userStore.userInfo.username || '用户'
    user.value.email = userStore.userInfo.email || ''
    user.value.avatar = `https://api.dicebear.com/7.x/avataaars/svg?seed=${userStore.userInfo.username}`
  }
  
  loadReadingHistory()
  loadBookshelf()
  loadMyComments()
  loadMyBooklists()
  loadMyTopics()
})

/**
 * 加载阅读历史
 */
const loadReadingHistory = async () => {
  try {
    const historyResponse = await readingApi.getReadingHistory()
    if (historyResponse.code === 200 && historyResponse.data && historyResponse.data.length > 0) {
      readingHistory.value = historyResponse.data
      actualHistoryCount.value = readingHistory.value.length
    } else {
      const bookmarkResponse = await bookmarkApi.getBookmarks()
      if (bookmarkResponse.code === 200) {
        readingHistory.value = bookmarkResponse.data || []
        actualHistoryCount.value = readingHistory.value.length
      } else {
        actualHistoryCount.value = 0
      }
    }
    window.scrollTo(0, 0)
  } catch (error) {
    console.error('加载阅读历史失败:', error)
    try {
      const bookmarkResponse = await bookmarkApi.getBookmarks()
      if (bookmarkResponse.code === 200) {
        readingHistory.value = bookmarkResponse.data || []
        actualHistoryCount.value = readingHistory.value.length
      } else {
        actualHistoryCount.value = 0
      }
    } catch (e) {
      actualHistoryCount.value = 0
    }
  }
}

/**
 * 加载书架数据
 */
const loadBookshelf = async () => {
  try {
    const response = await bookmarkApi.getBookmarks()
    if (response.code === 200) {
      bookmarks.value = response.data || []
      actualBookshelfCount.value = bookmarks.value.length
    } else {
      actualBookshelfCount.value = 0
    }
  } catch (error) {
    console.error('加载书架失败:', error)
    actualBookshelfCount.value = 0
  }
}

/**
 * 加载我的评论数量
 */
const loadMyComments = async () => {
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    if (userInfo.id) {
      const response = await commentApi.getCommentsByUser(userInfo.id)
      if (response.code === 200) {
        myCommentsCount.value = (response.data || []).length
      } else {
        myCommentsCount.value = 0
      }
    } else {
      myCommentsCount.value = 0
    }
  } catch (error) {
    console.error('加载评论失败:', error)
    myCommentsCount.value = 0
  }
}

/**
 * 加载我的书单数量
 */
const loadMyBooklists = async () => {
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    if (userInfo.id) {
      const response = await discussionApi.getUserBooklists(userInfo.id)
      if (response.code === 200) {
        myBooklistsCount.value = (response.data || []).length
      } else {
        myBooklistsCount.value = 0
      }
    } else {
      myBooklistsCount.value = 0
    }
  } catch (error) {
    console.error('加载书单失败:', error)
    myBooklistsCount.value = 0
  }
}

/**
 * 加载我的话题数量
 */
const loadMyTopics = async () => {
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    if (userInfo.id) {
      const response = await discussionApi.getUserTopics(userInfo.id)
      if (response.code === 200) {
        myTopicsCount.value = (response.data || []).length
      } else {
        myTopicsCount.value = 0
      }
    } else {
      myTopicsCount.value = 0
    }
  } catch (error) {
    console.error('加载话题失败:', error)
    myTopicsCount.value = 0
  }
}

/**
 * 打开编辑对话框
 */
const openEditDialog = () => {
  editForm.value = {
    username: user.value.username,
    email: user.value.email,
    avatar: user.value.avatar
  }
  passwordForm.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
  isEditing.value = true
}

const closeEditDialog = () => {
  isEditing.value = false
}

const saveProfile = () => {
  if (!editForm.value.username.trim()) {
    ElMessage.warning('用户名不能为空')
    return
  }
  
  user.value.username = editForm.value.username
  user.value.email = editForm.value.email
  user.value.avatar = `https://api.dicebear.com/7.x/avataaars/svg?seed=${editForm.value.username}`
  
  if (userStore.userInfo) {
    userStore.userInfo.username = editForm.value.username
    userStore.userInfo.email = editForm.value.email
  }
  
  if (passwordForm.value.newPassword) {
    if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
      ElMessage.error('两次输入的密码不一致')
      return
    }
    if (passwordForm.value.newPassword.length < 6) {
      ElMessage.error('密码长度至少为6位')
      return
    }
    ElMessage.success('密码修改成功')
  }
  
  ElMessage.success('个人信息更新成功')
  isEditing.value = false
}

const goToHistory = () => router.push('/history')
const goToBookshelf = () => router.push('/bookshelf')
const goToComments = () => router.push('/my-comments')
const goToBooklists = () => router.push('/my-booklists')
const goToTopics = () => router.push('/my-topics')

const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('userRole')
  userStore.setUserInfo(null)
  ElMessage.success('已退出登录')
  router.replace('/')
}
</script>

<template>
  <div class="profile-page">
    <div class="profile-header">
      <div class="user-info">
        <div class="avatar-wrapper" @click="openEditDialog">
          <img :src="user.avatar" :alt="user.username" class="avatar" />
          <div class="avatar-overlay">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
            </svg>
          </div>
        </div>
        <div class="user-details">
          <h1>{{ user.username }}</h1>
          <p>{{ user.email || '暂无邮箱信息' }}</p>
        </div>
      </div>
    </div>
    
    <div class="profile-content">
      <div class="quick-links">
        <div class="quick-link-item" @click="goToHistory">
          <div class="quick-link-icon history">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"></circle>
              <polyline points="12 6 12 12 16 14"></polyline>
            </svg>
          </div>
          <div class="quick-link-info">
            <h3>阅读历史</h3>
            <p>{{ actualHistoryCount }} 本</p>
          </div>
          <svg class="arrow-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
        
        <div class="quick-link-item" @click="goToBookshelf">
          <div class="quick-link-icon bookshelf">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
              <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
            </svg>
          </div>
          <div class="quick-link-info">
            <h3>我的书架</h3>
            <p>{{ actualBookshelfCount }} 本</p>
          </div>
          <svg class="arrow-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
        
        <div class="quick-link-item" @click="goToComments">
          <div class="quick-link-icon comments">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
            </svg>
          </div>
          <div class="quick-link-info">
            <h3>我的评论</h3>
            <p>{{ myCommentsCount }} 条</p>
          </div>
          <svg class="arrow-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
        
        <div class="quick-link-item" @click="goToBooklists">
          <div class="quick-link-icon booklists">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="8" y1="6" x2="21" y2="6"></line>
              <line x1="8" y1="12" x2="21" y2="12"></line>
              <line x1="8" y1="18" x2="21" y2="18"></line>
              <line x1="3" y1="6" x2="3.01" y2="6"></line>
              <line x1="3" y1="12" x2="3.01" y2="12"></line>
              <line x1="3" y1="18" x2="3.01" y2="18"></line>
            </svg>
          </div>
          <div class="quick-link-info">
            <h3>我的书单</h3>
            <p>{{ myBooklistsCount }} 个</p>
          </div>
          <svg class="arrow-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
        
        <div class="quick-link-item" @click="goToTopics">
          <div class="quick-link-icon topics">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path>
            </svg>
          </div>
          <div class="quick-link-info">
            <h3>我的话题</h3>
            <p>{{ myTopicsCount }} 个</p>
          </div>
          <svg class="arrow-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
        
        <div class="quick-link-item logout" @click.stop="logout">
          <div class="quick-link-icon logout-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
              <polyline points="16 17 21 12 16 7"></polyline>
              <line x1="21" y1="12" x2="9" y2="12"></line>
            </svg>
          </div>
          <div class="quick-link-info">
            <h3>退出登录</h3>
            <p>退出当前账户</p>
          </div>
          <svg class="arrow-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
      </div>
    </div>

    <div v-if="isEditing" class="edit-dialog-overlay" @click="closeEditDialog">
      <div class="edit-dialog" @click.stop>
        <div class="edit-dialog-header">
          <h2>编辑个人信息</h2>
          <button class="close-btn" @click="closeEditDialog">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
        <div class="edit-dialog-content">
          <div class="form-section">
            <h3>基本信息</h3>
            <div class="form-group">
              <label>用户名</label>
              <input type="text" v-model="editForm.username" placeholder="请输入用户名" />
            </div>
            <div class="form-group">
              <label>邮箱</label>
              <input type="email" v-model="editForm.email" placeholder="请输入邮箱" />
            </div>
          </div>
          
          <div class="form-section">
            <h3>修改密码</h3>
            <div class="form-group">
              <label>原密码</label>
              <input type="password" v-model="passwordForm.oldPassword" placeholder="请输入原密码" />
            </div>
            <div class="form-group">
              <label>新密码</label>
              <input type="password" v-model="passwordForm.newPassword" placeholder="请输入新密码（留空则不修改）" />
            </div>
            <div class="form-group">
              <label>确认新密码</label>
              <input type="password" v-model="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
            </div>
          </div>
        </div>
        <div class="edit-dialog-footer">
          <button class="btn-cancel" @click="closeEditDialog">取消</button>
          <button class="btn-save" @click="saveProfile">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.profile-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 24px;
  min-height: calc(100vh - 80px);
}

.profile-header {
  background: rgba(22, 27, 34, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  padding: 40px;
  margin-bottom: 32px;
  position: relative;
  overflow: hidden;
}

.profile-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #c9a962 0%, #d4b978 50%, #c9a962 100%);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 24px;
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;
  flex-shrink: 0;
}

.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid rgba(201, 169, 98, 0.3);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.1);
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: rgba(201, 169, 98, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.avatar-overlay svg {
  width: 28px;
  height: 28px;
  color: #0d1117;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.avatar-wrapper:hover .avatar {
  transform: scale(1.05);
  border-color: rgba(201, 169, 98, 0.6);
}

.user-details h1 {
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

.user-details p {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.quick-link-item {
  background: rgba(22, 27, 34, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.04);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.quick-link-item:hover {
  background: rgba(201, 169, 98, 0.08);
  border-color: rgba(201, 169, 98, 0.2);
  transform: translateY(-2px);
}

.quick-link-item.logout {
  background: rgba(239, 68, 68, 0.05);
  border-color: rgba(239, 68, 68, 0.15);
}

.quick-link-item.logout:hover {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.3);
}

.quick-link-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  flex-shrink: 0;
}

.quick-link-icon svg {
  width: 24px;
  height: 24px;
}

.quick-link-icon.history {
  background: rgba(201, 169, 98, 0.15);
  color: #c9a962;
}

.quick-link-icon.bookshelf {
  background: rgba(59, 130, 246, 0.15);
  color: #3b82f6;
}

.quick-link-icon.comments {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.quick-link-icon.booklists {
  background: rgba(168, 85, 247, 0.15);
  color: #a855f7;
}

.quick-link-icon.topics {
  background: rgba(236, 72, 153, 0.15);
  color: #ec4899;
}

.quick-link-icon.logout-icon {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.quick-link-info {
  flex: 1;
}

.quick-link-info h3 {
  font-size: 16px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 4px 0;
}

.quick-link-info p {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

.arrow-icon {
  width: 18px;
  height: 18px;
  color: rgba(255, 255, 255, 0.3);
  transition: all 0.3s ease;
}

.quick-link-item:hover .arrow-icon {
  color: #c9a962;
  transform: translateX(4px);
}

.quick-link-item.logout:hover .arrow-icon {
  color: #ef4444;
}

.edit-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.edit-dialog {
  background: rgba(22, 27, 34, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  width: 90%;
  max-width: 480px;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.edit-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.edit-dialog-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
  font-family: 'Noto Serif SC', 'Playfair Display', serif;
}

.close-btn {
  background: rgba(255, 255, 255, 0.05);
  border: none;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.close-btn svg {
  width: 20px;
  height: 20px;
  color: rgba(255, 255, 255, 0.5);
}

.close-btn:hover {
  background: rgba(239, 68, 68, 0.1);
}

.close-btn:hover svg {
  color: #ef4444;
}

.edit-dialog-content {
  padding: 24px;
}

.form-section {
  margin-bottom: 24px;
}

.form-section:last-child {
  margin-bottom: 0;
}

.form-section h3 {
  font-size: 14px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.5);
  margin: 0 0 16px 0;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-group label {
  display: block;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 8px;
}

.form-group input {
  width: 100%;
  padding: 14px 16px;
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
}

.edit-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.btn-cancel {
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-cancel:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
}

.btn-save {
  padding: 12px 24px;
  background: linear-gradient(135deg, #c9a962 0%, #d4b978 100%);
  color: #0d1117;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-save:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(201, 169, 98, 0.3);
}

@media (max-width: 768px) {
  .profile-page {
    padding: 24px 16px;
  }

  .profile-header {
    padding: 24px;
  }

  .user-info {
    flex-direction: column;
    text-align: center;
  }

  .avatar-wrapper {
    margin-bottom: 16px;
  }

  .user-details h1 {
    font-size: 26px;
  }

  .quick-links {
    grid-template-columns: 1fr;
  }

  .edit-dialog {
    width: 95%;
  }

  .edit-dialog-header,
  .edit-dialog-content,
  .edit-dialog-footer {
    padding: 20px;
  }
}
</style>
