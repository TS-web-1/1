<script setup>
// 导入Vue相关的响应式API
import { ref, onMounted, computed, watch } from 'vue'
// 导入评论API
import { commentApi } from '../api/modules/comment.js'
// 导入用户状态管理
import { useUserStore } from '../stores/user.js'
// 导入Element Plus消息组件
import { ElMessage } from 'element-plus'

/**
 * 评论框组件
 * 用于显示和提交评论，支持回复功能
 */

// 组件属性定义
const props = defineProps({
  /**
   * 小说ID
   */
  novelId: {
    type: [Number, String],
    default: null
  },
  /**
   * 章节ID
   */
  chapterId: {
    type: [Number, String],
    default: null
  },
  /**
   * 评论类型，默认为NOVEL
   */
  commentType: {
    type: String,
    default: 'NOVEL'
  }
})

// 用户状态
const userStore = useUserStore()
// 评论列表
const comments = ref([])
// 新评论内容
const newComment = ref('')
// 加载状态
const loading = ref(false)
// 提交状态
const submitting = ref(false)
// 回复对象
const replyTo = ref(null)
// 回复内容
const replyContent = ref('')

// 当前用户信息
const currentUser = computed(() => userStore.userInfo)

// 有效小说ID
const effectiveNovelId = computed(() => {
  if (props.novelId) return props.novelId
  return null
})

// 有效章节ID
const effectiveChapterId = computed(() => {
  if (props.chapterId) return props.chapterId
  return null
})

// 有效评论类型
const effectiveCommentType = computed(() => {
  if (effectiveChapterId.value) return 'CHAPTER'
  return props.commentType || 'NOVEL'
})

/**
 * 加载评论列表
 */
const loadComments = async () => {
  loading.value = true
  try {
    let response
    if (effectiveCommentType.value === 'CHAPTER' && effectiveChapterId.value) {
      response = await commentApi.getCommentsByChapter(effectiveChapterId.value)
    } else if (effectiveNovelId.value) {
      response = await commentApi.getCommentsByNovel(effectiveNovelId.value)
    } else {
      comments.value = []
      return
    }

    if (response.code === 200) {
      const data = response.data
      comments.value = data.content || data || []
    } else {
      comments.value = []
    }
  } catch (error) {
    console.error('加载评论失败:', error)
    comments.value = []
  } finally {
    loading.value = false
  }
}

// 监听小说ID和章节ID变化，重新加载评论
watch(
  [effectiveNovelId, effectiveChapterId],
  () => {
    loadComments()
  },
  { immediate: true }
)

// 组件挂载时加载评论
onMounted(() => {
  loadComments()
})

/**
 * 提交评论
 */
const handleSubmit = async () => {
  if (!currentUser.value) {
    ElMessage.warning('请先登录')
    return
  }

  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  submitting.value = true
  try {
    const commentData = {
      content: newComment.value.trim(),
      commentType: effectiveCommentType.value
    }

    if (effectiveCommentType.value === 'CHAPTER' && effectiveChapterId.value) {
      commentData.chapterId = effectiveChapterId.value
    } else if (effectiveNovelId.value) {
      commentData.novelId = effectiveNovelId.value
    }

    const response = await commentApi.createComment(commentData)

    if (response.code === 200) {
      ElMessage.success('评论成功')
      newComment.value = ''
      loadComments()
    } else {
      ElMessage.error(response.message || '评论失败')
    }
  } catch (error) {
    console.error('评论失败:', error)
    ElMessage.error('评论失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

/**
 * 回复评论
 * @param {Object} parentComment 父评论对象
 */
const handleReply = async parentComment => {
  if (!currentUser.value) {
    ElMessage.warning('请先登录')
    return
  }

  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  submitting.value = true
  try {
    const commentData = {
      content: replyContent.value.trim(),
      commentType: effectiveCommentType.value,
      parentId: parentComment.id
    }

    if (effectiveCommentType.value === 'CHAPTER' && effectiveChapterId.value) {
      commentData.chapterId = effectiveChapterId.value
    } else if (effectiveNovelId.value) {
      commentData.novelId = effectiveNovelId.value
    }

    const response = await commentApi.createComment(commentData)

    if (response.code === 200) {
      ElMessage.success('回复成功')
      replyContent.value = ''
      replyTo.value = null
      loadComments()
    } else {
      ElMessage.error(response.message || '回复失败')
    }
  } catch (error) {
    console.error('回复失败:', error)
    ElMessage.error('回复失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

/**
 * 设置回复对象
 * @param {Object} comment 评论对象
 */
const setReplyTo = comment => {
  replyTo.value = comment
  replyContent.value = ''
}

/**
 * 取消回复
 */
const cancelReply = () => {
  replyTo.value = null
  replyContent.value = ''
}

/**
 * 格式化日期
 * @param {string} dateStr 日期字符串
 * @return {string} 格式化后的日期
 */
const formatDate = dateStr => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<template>
  <div class="comment-box">
    <div class="comment-header">
      <div class="header-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
        </svg>
      </div>
      <h3>评论区</h3>
      <span class="comment-count">{{ comments.length }} 条评论</span>
    </div>

    <div class="comment-input-area">
      <div class="input-wrapper">
        <img
          v-if="currentUser"
          :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${currentUser.username}`"
          class="user-avatar"
          alt="avatar"
        />
        <div v-else class="avatar-placeholder">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
            <circle cx="12" cy="7" r="4"></circle>
          </svg>
        </div>
        <textarea
          v-model="newComment"
          placeholder="写下你的想法..."
          rows="3"
          :disabled="!currentUser"
        ></textarea>
      </div>
      <div class="input-actions">
        <span v-if="!currentUser" class="login-hint">
          <RouterLink to="/login">登录</RouterLink> 后参与评论
        </span>
        <button
          class="btn-submit"
          :disabled="!currentUser || !newComment.trim() || submitting"
          @click="handleSubmit"
        >
          <svg v-if="submitting" class="spinner-icon" viewBox="0 0 24 24">
            <circle
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              stroke-width="2"
              fill="none"
              stroke-dasharray="31.4"
              stroke-dashoffset="10"
            ></circle>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="22" y1="2" x2="11" y2="13"></line>
            <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
          </svg>
          {{ submitting ? '发送中...' : '发表评论' }}
        </button>
      </div>
    </div>

    <div v-if="!loading && comments.length > 0" class="comments-list">
      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <div class="comment-avatar">
          <img
            :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${comment.user?.username || 'anonymous'}`"
            alt="avatar"
          />
        </div>
        <div class="comment-content">
          <div class="comment-meta">
            <span class="comment-author">{{ comment.user?.username || '匿名用户' }}</span>
            <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
          </div>
          <p class="comment-text">{{ comment.content }}</p>
          <div class="comment-actions">
            <button class="action-btn" @click="setReplyTo(comment)">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path
                  d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"
                ></path>
              </svg>
              回复
            </button>
          </div>

          <div v-if="replyTo?.id === comment.id" class="reply-input-area">
            <textarea
              v-model="replyContent"
              :placeholder="`回复 ${comment.user?.username || '匿名用户'}...`"
              rows="2"
            ></textarea>
            <div class="reply-actions">
              <button class="btn-cancel" @click="cancelReply">取消</button>
              <button
                class="btn-reply"
                :disabled="!replyContent.trim() || submitting"
                @click="handleReply(comment)"
              >
                {{ submitting ? '发送中...' : '发送' }}
              </button>
            </div>
          </div>

          <div v-if="comment.replies && comment.replies.length > 0" class="replies-list">
            <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
              <div class="reply-avatar">
                <img
                  :src="`https://api.dicebear.com/7.x/avataaars/svg?seed=${reply.user?.username || 'anonymous'}`"
                  alt="avatar"
                />
              </div>
              <div class="reply-content">
                <div class="reply-meta">
                  <span class="reply-author">{{ reply.user?.username || '匿名用户' }}</span>
                  <span class="reply-time">{{ formatDate(reply.createdAt) }}</span>
                </div>
                <p class="reply-text">{{ reply.content }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>加载评论中...</p>
    </div>

    <div v-else class="empty-state">
      <div class="empty-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
        </svg>
      </div>
      <p>暂无评论，快来抢沙发吧！</p>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.comment-box {
  width: 100%;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.header-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(201, 169, 98, 0.15);
  border-radius: 10px;
}

.header-icon svg {
  width: 20px;
  height: 20px;
  color: #c9a962;
}

.comment-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
  font-family: 'Noto Serif SC', serif;
}

.comment-count {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  margin-left: auto;
}

.comment-input-area {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 24px;
  border: 1px solid rgba(255, 255, 255, 0.04);
}

.input-wrapper {
  display: flex;
  gap: 14px;
  margin-bottom: 16px;
}

.user-avatar,
.avatar-placeholder {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  flex-shrink: 0;
  background: rgba(201, 169, 98, 0.1);
  border: 2px solid rgba(201, 169, 98, 0.2);
}

.user-avatar {
  object-fit: cover;
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-placeholder svg {
  width: 22px;
  height: 22px;
  color: rgba(255, 255, 255, 0.4);
}

.input-wrapper textarea {
  flex: 1;
  padding: 14px 16px;
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  font-size: 14px;
  color: #ffffff;
  resize: none;
  transition: all 0.3s ease;
  font-family: inherit;
  line-height: 1.6;
}

.input-wrapper textarea::placeholder {
  color: rgba(255, 255, 255, 0.3);
}

.input-wrapper textarea:focus {
  outline: none;
  border-color: rgba(201, 169, 98, 0.4);
  background: rgba(201, 169, 98, 0.05);
}

.input-wrapper textarea:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.login-hint {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}

.login-hint a {
  color: #c9a962;
  text-decoration: none;
  font-weight: 500;
}

.login-hint a:hover {
  text-decoration: underline;
}

.btn-submit {
  display: flex;
  align-items: center;
  gap: 8px;
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

.btn-submit svg {
  width: 16px;
  height: 16px;
}

.btn-submit:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(201, 169, 98, 0.3);
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.spinner-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comment-item {
  display: flex;
  gap: 14px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.04);
  transition: all 0.3s ease;
}

.comment-item:hover {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.08);
}

.comment-avatar {
  flex-shrink: 0;
}

.comment-avatar img {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(201, 169, 98, 0.2);
}

.comment-content {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.comment-author {
  font-size: 15px;
  font-weight: 600;
  color: #ffffff;
}

.comment-time {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

.comment-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.7;
  margin: 0 0 12px 0;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: transparent;
  color: rgba(255, 255, 255, 0.5);
  border: 1px solid transparent;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-btn svg {
  width: 14px;
  height: 14px;
}

.action-btn:hover {
  color: #c9a962;
  background: rgba(201, 169, 98, 0.1);
  border-color: rgba(201, 169, 98, 0.2);
}

.reply-input-area {
  margin-top: 16px;
  padding: 16px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.reply-input-area textarea {
  width: 100%;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  font-size: 14px;
  color: #ffffff;
  resize: none;
  margin-bottom: 12px;
  font-family: inherit;
}

.reply-input-area textarea::placeholder {
  color: rgba(255, 255, 255, 0.3);
}

.reply-input-area textarea:focus {
  outline: none;
  border-color: rgba(201, 169, 98, 0.4);
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn-cancel {
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-cancel:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
}

.btn-reply {
  padding: 8px 16px;
  background: linear-gradient(135deg, #c9a962 0%, #d4b978 100%);
  color: #0d1117;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-reply:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(201, 169, 98, 0.3);
}

.btn-reply:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.replies-list {
  margin-top: 16px;
  padding-left: 16px;
  border-left: 2px solid rgba(201, 169, 98, 0.2);
}

.reply-item {
  display: flex;
  gap: 12px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 10px;
  margin-bottom: 10px;
}

.reply-item:last-child {
  margin-bottom: 0;
}

.reply-avatar img {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(201, 169, 98, 0.15);
}

.reply-content {
  flex: 1;
}

.reply-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.reply-author {
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.reply-time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
}

.reply-text {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  line-height: 1.6;
  margin: 0;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.1);
  border-top-color: #c9a962;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

.loading-state p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 14px;
  border: 1px dashed rgba(255, 255, 255, 0.1);
}

.empty-icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(201, 169, 98, 0.1);
  border-radius: 14px;
  margin-bottom: 16px;
}

.empty-icon svg {
  width: 28px;
  height: 28px;
  color: rgba(201, 169, 98, 0.5);
}

.empty-state p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

@media (max-width: 768px) {
  .comment-input-area {
    padding: 16px;
  }

  .input-wrapper {
    flex-direction: column;
    gap: 12px;
  }

  .user-avatar,
  .avatar-placeholder {
    width: 36px;
    height: 36px;
  }

  .input-actions {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .login-hint {
    text-align: center;
  }

  .btn-submit {
    justify-content: center;
  }

  .comment-item {
    padding: 16px;
  }

  .comment-avatar img {
    width: 36px;
    height: 36px;
  }

  .replies-list {
    padding-left: 12px;
  }
}
</style>
