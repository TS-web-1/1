<template>
  <div class="topic-detail">
    <div v-if="loading" class="loading">加载中...</div>
    
    <div v-else-if="!topic" class="empty">
      <p>话题不存在</p>
      <button @click="goBack" class="back-btn">返回</button>
    </div>
    
    <div v-else class="topic-container">
      <div class="topic-header">
        <h1>{{ topic.title }}</h1>
        <div class="topic-meta">
          <span class="author">发布者：{{ topic.user?.username || '未知' }}</span>
          <span class="time">{{ formatTime(topic.createdAt) }}</span>
          <span class="views">浏览 {{ topic.viewCount || 0 }}</span>
        </div>
      </div>
      
      <div class="topic-content">
        <p>{{ topic.content }}</p>
      </div>
      
      <div class="topic-stats">
        <span class="stat-item">
          <el-icon><View /></el-icon>
          {{ topic.viewCount || 0 }}
        </span>
        <span class="stat-item">
          <el-icon><ChatDotRound /></el-icon>
          {{ topic.commentCount || 0 }}
        </span>
        <span class="stat-item">
          <el-icon><Star /></el-icon>
          {{ topic.likeCount || 0 }}
        </span>
      </div>
      
      <div class="back-link">
        <button @click="goBack" class="back-btn">返回话题列表</button>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * TopicDetail.vue - 话题详情页面组件
 * 
 * 该组件实现了话题详情展示功能，包括：
 * - 话题标题、内容、作者信息展示
 * - 浏览量、评论数、点赞数统计
 * - 返回话题列表功能
 */
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { discussionApi } from '../api/modules/discussion.js'
import { ElMessage } from 'element-plus'
import { View, ChatDotRound, Star } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const topic = ref(null)
const loading = ref(true)

/**
 * 组件挂载时加载话题详情
 */
onMounted(async () => {
  await loadTopic()
})

/**
 * 加载话题详情
 */
const loadTopic = async () => {
  loading.value = true
  try {
    const topicId = route.params.id
    const response = await discussionApi.getTopicById(topicId)
    if (response.code === 200) {
      topic.value = response.data
    } else {
      ElMessage.error('话题不存在')
    }
  } catch (error) {
    console.error('加载话题详情失败:', error)
    ElMessage.error('加载话题详情失败')
  } finally {
    loading.value = false
  }
}

/**
 * 返回上一页
 */
const goBack = () => {
  router.back()
}

const formatTime = (dateString) => {
  const date = new Date(dateString)
  const now = new Date()
  const diff = now - date
  
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  
  return date.toLocaleDateString()
}
</script>

<style scoped>
.topic-detail {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.loading, .empty {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.back-btn {
  margin-top: 20px;
  padding: 10px 30px;
  background-color: #409EFF;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.back-btn:hover {
  background-color: #66b1ff;
}

.topic-container {
  background: white;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.topic-header h1 {
  margin: 0 0 15px 0;
  font-size: 24px;
  color: #333;
}

.topic-meta {
  display: flex;
  gap: 20px;
  color: #999;
  font-size: 14px;
  margin-bottom: 20px;
}

.topic-content {
  margin: 30px 0;
  line-height: 1.8;
  color: #333;
  font-size: 16px;
  white-space: pre-wrap;
}

.topic-stats {
  display: flex;
  gap: 20px;
  padding: 15px 0;
  border-top: 1px solid #eee;
  color: #999;
  font-size: 14px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.back-link {
  margin-top: 30px;
  text-align: center;
}
</style>
