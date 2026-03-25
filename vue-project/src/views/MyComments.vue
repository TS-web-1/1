<template>
  <div class="my-comments">
    <h1>我的评论</h1>
    
    <div v-if="loading" class="loading">加载中...</div>
    
    <div v-else-if="comments.length === 0" class="empty">
      <p>暂无评论</p>
    </div>
    
    <div v-else class="comments-list">
      <div 
        v-for="comment in comments" 
        :key="comment.id" 
        class="comment-item"
      >
        <div class="comment-header">
          <span class="novel-title">{{ comment.novelTitle || '小说' }}</span>
          <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
        </div>
        <div class="comment-content">{{ comment.content }}</div>
        <div class="comment-actions">
          <button 
            v-if="!comment.deleted"
            @click="deleteComment(comment.id)" 
            class="delete-btn"
          >
            删除
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * MyComments.vue - 我的评论页面组件
 * 
 * 该组件实现了用户评论管理功能，包括：
 * - 展示用户发表的评论列表
 * - 显示评论所属的小说和时间
 * - 删除评论
 */
import { ref, onMounted } from 'vue'
import { commentApi } from '../api/modules/comment.js'
import { ElMessage } from 'element-plus'

const comments = ref([])
const loading = ref(false)

/**
 * 组件挂载时加载我的评论
 */
onMounted(() => {
  loadMyComments()
})

/**
 * 加载我的评论列表
 */
const loadMyComments = async () => {
  loading.value = true
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    if (userInfo.id) {
      const response = await commentApi.getCommentsByUser(userInfo.id)
      if (response.code === 200) {
        comments.value = response.data || []
      }
    }
  } catch (error) {
    console.error('加载评论失败:', error)
    ElMessage.error('加载评论失败')
  } finally {
    loading.value = false
  }
}

/**
 * 删除评论
 * @param {number} commentId - 评论ID
 */
const deleteComment = async (commentId) => {
  if (!confirm('确定要删除这条评论吗？')) return
  
  try {
    const response = await commentApi.deleteComment(commentId)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      loadMyComments()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败')
  }
}

const formatTime = (dateString) => {
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
.my-comments {
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

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-item {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.novel-title {
  font-weight: bold;
  color: #409EFF;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-content {
  color: #333;
  line-height: 1.6;
  margin-bottom: 12px;
}

.delete-btn {
  padding: 6px 16px;
  background: #f5f5f5;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  color: #666;
}

.delete-btn:hover {
  background: #ffebee;
  color: #f44336;
}
</style>
