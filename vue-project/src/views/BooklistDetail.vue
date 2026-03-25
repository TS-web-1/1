<template>
  <div class="booklist-detail">
    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="!booklist" class="empty">
      <p>书单不存在</p>
      <button class="back-btn" @click="goBack">返回</button>
    </div>

    <div v-else class="booklist-container">
      <div class="booklist-header">
        <h1>{{ booklist.title }}</h1>
        <div class="booklist-meta">
          <span class="author">创建者：{{ booklist.user?.username || '未知' }}</span>
          <span class="time">{{ formatTime(booklist.createdAt) }}</span>
          <span class="count">{{ booklist.novels?.length || 0 }} 本小说</span>
        </div>
        <p v-if="booklist.description" class="description">{{ booklist.description }}</p>
      </div>

      <div class="novels-grid">
        <div
          v-for="novel in booklist.novels"
          :key="novel.id"
          class="novel-card"
          @click="goToNovel(novel.id)"
        >
          <div class="novel-cover">
            <img
              :src="
                novel.coverImage
                  ? `/api/novels/${novel.id}/cover`
                  : 'https://via.placeholder.com/200x260'
              "
              :alt="novel.title"
            />
          </div>
          <div class="novel-info">
            <h3>{{ novel.title }}</h3>
            <p class="author">{{ novel.author }}</p>
            <p v-if="novel.description" class="description"
              >{{ novel.description.substring(0, 80) }}...</p
            >
          </div>
        </div>
      </div>

      <div v-if="!booklist.novels || booklist.novels.length === 0" class="no-novels">
        <p>这个书单还没有添加小说</p>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * BooklistDetail.vue - 书单详情页面组件
 *
 * 该组件实现了书单详情展示功能，包括：
 * - 书单标题、描述、创建者信息展示
 * - 书单中的小说列表展示
 * - 点击小说跳转到详情页
 */
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { discussionApi } from '../api/modules/discussion.js'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const booklist = ref(null)
const loading = ref(true)

/**
 * 组件挂载时加载书单详情
 */
onMounted(async () => {
  await loadBooklist()
})

/**
 * 加载书单详情
 */
const loadBooklist = async () => {
  loading.value = true
  try {
    const booklistId = route.params.id
    const response = await discussionApi.getBooklistById(booklistId)
    if (response.code === 200) {
      booklist.value = response.data
    } else {
      ElMessage.error('书单不存在')
    }
  } catch (error) {
    console.error('加载书单失败:', error)
    ElMessage.error('加载书单失败')
  } finally {
    loading.value = false
  }
}

/**
 * 跳转到小说详情页
 * @param {number} novelId - 小说ID
 */
const goToNovel = novelId => {
  router.push(`/novel/${novelId}`)
}

const goBack = () => {
  router.back()
}

const formatTime = dateString => {
  const date = new Date(dateString)
  return date.toLocaleDateString()
}
</script>

<style scoped>
.booklist-detail {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  min-height: calc(100vh - 140px);
}

.loading,
.empty {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.back-btn {
  margin-top: 16px;
  padding: 10px 24px;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.back-btn:hover {
  background: #66b1ff;
}

.booklist-container {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.booklist-header {
  margin-bottom: 30px;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 20px;
}

.booklist-header h1 {
  font-size: 28px;
  color: #333;
  margin-bottom: 12px;
}

.booklist-meta {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
}

.description {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.novels-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.novel-card {
  background: #f9f9f9;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}

.novel-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.novel-cover {
  width: 100%;
  height: 260px;
  overflow: hidden;
  background: #f0f0f0;
}

.novel-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.novel-info {
  padding: 16px;
}

.novel-info h3 {
  font-size: 16px;
  color: #333;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.novel-info .author {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.novel-info .description {
  font-size: 12px;
  color: #999;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.no-novels {
  text-align: center;
  padding: 40px 20px;
  color: #999;
}
</style>
