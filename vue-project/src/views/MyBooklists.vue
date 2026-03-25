<template>
  <div class="my-booklists">
    <h1>我的书单</h1>
    
    <div v-if="loading" class="loading">加载中...</div>
    
    <div v-else-if="booklists.length === 0" class="empty">
      <p>暂无书单</p>
      <button @click="goToCommunity" class="create-btn">创建书单</button>
    </div>
    
    <div v-else class="booklists-grid">
      <div 
        v-for="booklist in booklists" 
        :key="booklist.id" 
        class="booklist-card"
      >
        <div class="booklist-cover">
          <img 
            v-if="booklist.novels && booklist.novels.length > 0" 
            :src="booklist.novels[0].coverImage ? `/api/novels/${booklist.novels[0].id}/cover` : 'https://via.placeholder.com/300x200'" 
            :alt="booklist.title"
          />
          <img v-else src="https://via.placeholder.com/300x200" :alt="booklist.title" />
        </div>
        <div class="booklist-info">
          <h3>{{ booklist.title }}</h3>
          <p class="description">{{ booklist.description || '暂无描述' }}</p>
          <div class="booklist-meta">
            <span>{{ booklist.bookCount || 0 }} 本小说</span>
            <span>{{ formatTime(booklist.createdAt) }}</span>
          </div>
        </div>
        <div class="booklist-actions">
          <button @click="viewBooklist(booklist.id)" class="view-btn">查看</button>
          <button @click="deleteBooklist(booklist.id)" class="delete-btn">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * MyBooklists.vue - 我的书单页面组件
 * 
 * 该组件实现了用户书单管理功能，包括：
 * - 展示用户创建的书单列表
 * - 查看书单详情
 * - 删除书单
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { discussionApi } from '../api/modules/discussion.js'
import { ElMessage } from 'element-plus'

const router = useRouter()
const booklists = ref([])
const loading = ref(false)

/**
 * 组件挂载时加载我的书单
 */
onMounted(() => {
  loadMyBooklists()
})

/**
 * 加载我的书单列表
 */
const loadMyBooklists = async () => {
  loading.value = true
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    if (userInfo.id) {
      const response = await discussionApi.getUserBooklists(userInfo.id)
      if (response.code === 200) {
        booklists.value = response.data || []
      }
    }
  } catch (error) {
    console.error('加载书单失败:', error)
    ElMessage.error('加载书单失败')
  } finally {
    loading.value = false
  }
}

/**
 * 查看书单详情
 * @param {number} booklistId - 书单ID
 */
const viewBooklist = (booklistId) => {
  router.push(`/booklist/${booklistId}`)
}

/**
 * 删除书单
 * @param {number} booklistId - 书单ID
 */
const deleteBooklist = async (booklistId) => {
  if (!confirm('确定要删除这个书单吗？')) return
  
  try {
    const response = await discussionApi.deleteBooklist(booklistId)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      loadMyBooklists()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败')
  }
}

const goToCommunity = () => {
  router.push('/community')
}

const formatTime = (dateString) => {
  const date = new Date(dateString)
  return date.toLocaleDateString()
}
</script>

<style scoped>
.my-booklists {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 140px);
}

h1 {
  font-size: 24px;
  margin-bottom: 20px;
  color: #333;
}

.loading, .empty {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.create-btn {
  margin-top: 16px;
  padding: 10px 24px;
  background: #409EFF;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.create-btn:hover {
  background: #66b1ff;
}

.booklists-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.booklist-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s;
}

.booklist-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.booklist-cover {
  width: 100%;
  height: 200px;
  overflow: hidden;
  background: #f5f5f5;
}

.booklist-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.booklist-info {
  padding: 16px;
}

.booklist-info h3 {
  font-size: 16px;
  margin-bottom: 8px;
  color: #333;
}

.description {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
  line-height: 1.5;
}

.booklist-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
}

.booklist-actions {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
}

.view-btn, .delete-btn {
  flex: 1;
  padding: 8px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.view-btn {
  background: #409EFF;
  color: #fff;
}

.view-btn:hover {
  background: #66b1ff;
}

.delete-btn {
  background: #f5f5f5;
  color: #666;
}

.delete-btn:hover {
  background: #ffebee;
  color: #f44336;
}
</style>
