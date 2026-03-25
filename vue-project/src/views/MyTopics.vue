<template>
  <div class="my-topics">
    <h1>我的话题</h1>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="topics.length === 0" class="empty">
      <p>暂无话题</p>
      <button class="create-btn" @click="goToCommunity">发起话题</button>
    </div>

    <div v-else class="topics-list">
      <div v-for="topic in topics" :key="topic.id" class="topic-item">
        <div class="topic-header">
          <h3>{{ topic.title }}</h3>
          <span class="topic-time">{{ formatTime(topic.createdAt) }}</span>
        </div>
        <div class="topic-content">{{ topic.content }}</div>
        <div class="topic-meta">
          <span>👁️ {{ topic.viewCount || 0 }}</span>
          <span>💬 {{ topic.replyCount || 0 }}</span>
          <span>❤️ {{ topic.likeCount || 0 }}</span>
        </div>
        <div class="topic-actions">
          <button class="view-btn" @click="viewTopic(topic.id)">查看</button>
          <button v-if="!topic.deleted" class="delete-btn" @click="deleteTopic(topic.id)">
            删除
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * MyTopics.vue - 我的话题页面组件
 *
 * 该组件实现了用户话题管理功能，包括：
 * - 展示用户创建的话题列表
 * - 查看话题详情
 * - 删除话题
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { discussionApi } from '../api/modules/discussion.js'
import { ElMessage } from 'element-plus'

const router = useRouter()
const topics = ref([])
const loading = ref(false)

/**
 * 组件挂载时加载我的话题
 */
onMounted(() => {
  loadMyTopics()
})

/**
 * 加载我的话题列表
 */
const loadMyTopics = async () => {
  loading.value = true
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    if (userInfo.id) {
      const response = await discussionApi.getUserTopics(userInfo.id)
      if (response.code === 200) {
        topics.value = response.data || []
      }
    }
  } catch (error) {
    console.error('加载话题失败:', error)
    ElMessage.error('加载话题失败')
  } finally {
    loading.value = false
  }
}

/**
 * 查看话题详情
 * @param {number} topicId - 话题ID
 */
const viewTopic = topicId => {
  router.push(`/topic/${topicId}`)
}

/**
 * 删除话题
 * @param {number} topicId - 话题ID
 */
const deleteTopic = async topicId => {
  if (!confirm('确定要删除这个话题吗？')) return

  try {
    const response = await discussionApi.deleteTopic(topicId)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      loadMyTopics()
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

const formatTime = dateString => {
  const date = new Date(dateString)
  const now = new Date()
  const diff = now - date

  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) {
    return '刚刚'
  } else if (diff < hour) {
    return `${Math.floor(diff / minute)}分钟前`
  } else if (diff < day) {
    return `${Math.floor(diff / hour)}小时前`
  } else if (diff < 7 * day) {
    return `${Math.floor(diff / day)}天前`
  } else {
    return date.toLocaleDateString()
  }
}
</script>

<style scoped>
.my-topics {
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

.loading,
.empty {
  text-align: center;
  padding: 60px 20px;
  color: #999;
}

.create-btn {
  margin-top: 16px;
  padding: 10px 24px;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.create-btn:hover {
  background: #66b1ff;
}

.topics-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.topic-item {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.topic-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.topic-header h3 {
  font-size: 18px;
  color: #333;
  margin: 0;
}

.topic-time {
  font-size: 12px;
  color: #999;
}

.topic-content {
  color: #666;
  line-height: 1.6;
  margin-bottom: 12px;
  max-height: 100px;
  overflow: hidden;
}

.topic-meta {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: #999;
  margin-bottom: 12px;
}

.topic-actions {
  display: flex;
  gap: 8px;
}

.view-btn,
.delete-btn {
  padding: 8px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.view-btn {
  background: #409eff;
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
