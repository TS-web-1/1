<script setup>
/**
 * NovelDetail.vue - 小说详情页面组件
 * 
 * 该组件实现了小说详情展示功能，包括：
 * - 小说基本信息展示（封面、标题、作者、分类、简介等）
 * - 章节列表分页显示
 * - 加入/移出书架功能
 * - 点赞功能
 * - 开始阅读（自动跳转到上次阅读位置）
 * - 评论展示
 */
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { novelApi, chapterApi } from '../api/modules/novel.js'
import { bookmarkApi } from '../api/modules/bookmark.js'
import { readingApi } from '../api/modules/reading.js'
import { novelLikeApi } from '../api/modules/novelLike.js'
import CommentBox from '../components/CommentBox.vue'
import { ElMessage, ElPagination } from 'element-plus'

const route = useRoute()
const router = useRouter()
const novelId = computed(() => route.params.id)

const novel = ref(null)
const chapters = ref([])
const loading = ref(true)
const isInBookshelf = ref(false)
const hasLiked = ref(false)
const isLikedLoading = ref(false)

const currentPage = ref(1)
const pageSize = ref(50)
const total = ref(0)

/**
 * 获取小说详情
 */
const fetchNovelDetail = async () => {
  loading.value = true
  try {
    const response = await novelApi.getNovelById(novelId.value)
    console.log('获取小说详情响应:', response)
    if (response.code === 200 && response.data) {
      novel.value = response.data
      window.scrollTo(0, 0)
    } else {
      console.error('API 返回错误:', response)
      ElMessage.error('获取小说详情失败：' + (response.message || '未知错误'))
      novel.value = null
    }
  } catch (error) {
    console.error('获取小说详情失败:', error)
    ElMessage.error('获取小说详情失败: ' + error.message)
    novel.value = null
  } finally {
    loading.value = false
  }
}

/**
 * 获取章节列表
 * @param {number} page - 页码
 */
const fetchChapters = async (page = 1) => {
  try {
    const response = await chapterApi.getChapters(novelId.value)
    console.log('获取章节列表响应:', response)
    const list = response.data || []
    const seen = new Set()
    const uniqueChapters = list.filter(c => {
      const num = c.chapterNumber ?? c.chapter_number ?? 0
      const key = `${num}`
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
    
    total.value = uniqueChapters.length
    const start = (page - 1) * pageSize.value
    const end = start + pageSize.value
    chapters.value = uniqueChapters.slice(start, end)
  } catch (error) {
    console.error('获取章节列表失败:', error)
    chapters.value = [
      { id: 1, title: '第一章 开始', chapterNumber: 1 },
      { id: 2, title: '第二章 冒险', chapterNumber: 2 },
      { id: 3, title: '第三章 挑战', chapterNumber: 3 }
    ]
    total.value = chapters.value.length
  }
}

/**
 * 检查小说是否已在书架中
 */
const checkBookshelf = async () => {
  const token = localStorage.getItem('token')
  if (!token) return
  
  try {
    const response = await bookmarkApi.getBookmarks()
    isInBookshelf.value = response.data.some(b => b.novel?.id == novelId.value)
  } catch (error) {
    console.error('检查书架状态失败:', error)
  }
}

/**
 * 开始阅读
 * 跳转到上次阅读位置或第一章
 */
const handleStartReading = async () => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  if (chapters.value && chapters.value.length > 0) {
    let targetChapterId = chapters.value[0].id
    
    try {
      const progressResponse = await readingApi.getReadingProgress(parseInt(novelId.value))
      if (progressResponse.code === 200 && progressResponse.data && progressResponse.data.chapterId) {
        targetChapterId = progressResponse.data.chapterId
      }
    } catch (error) {
      console.log('获取阅读进度失败，将从第一章开始')
    }
    
    router.push(`/novel/${novelId.value}/chapter/${targetChapterId}`)
  } else {
    ElMessage.warning('暂无章节')
  }
}

/**
 * 加入/移出书架
 */
const handleAddToBookshelf = async () => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    if (isInBookshelf.value) {
      await handleRemoveFromBookshelf()
      return
    }
    
    let chapterId = 1
    try {
      const progressResponse = await readingApi.getReadingProgress(parseInt(novelId.value))
      if (progressResponse.code === 200 && progressResponse.data && progressResponse.data.chapterId) {
        chapterId = progressResponse.data.chapterId
      } else {
        const chaptersResponse = await readingApi.getNovelChapters(parseInt(novelId.value))
        if (chaptersResponse.code === 200 && chaptersResponse.data && chaptersResponse.data.length > 0) {
          chapterId = chaptersResponse.data[0].id
        }
      }
    } catch (e) {
      try {
        const chaptersResponse = await readingApi.getNovelChapters(parseInt(novelId.value))
        if (chaptersResponse.code === 200 && chaptersResponse.data && chaptersResponse.data.length > 0) {
          chapterId = chaptersResponse.data[0].id
        }
      } catch (e2) {
        chapterId = 1
      }
    }
    
    const response = await bookmarkApi.addBookmark({
      novelId: parseInt(novelId.value),
      chapterId: chapterId,
      title: novel.value.title
    })
    
    if (response.code === 200) {
      ElMessage.success('已加入书架')
      isInBookshelf.value = true
    } else {
      ElMessage.error(response.message || '加入书架失败')
    }
  } catch (error) {
    console.error('加入书架失败:', error)
    const errorMessage = error.response?.data?.message || '加入书架失败'
    ElMessage.error(errorMessage)
  }
}

/**
 * 从书架移除
 */
const handleRemoveFromBookshelf = async () => {
  try {
    const response = await bookmarkApi.getBookmarks()
    const bookmark = response.data.find(b => b.novel?.id == novelId.value)
    if (bookmark) {
      const deleteResponse = await bookmarkApi.deleteBookmark(bookmark.id)
      if (deleteResponse.code === 200) {
        ElMessage.success('已从书架移除')
        isInBookshelf.value = false
      } else {
        ElMessage.error(deleteResponse.message || '移除失败')
      }
    }
  } catch (error) {
    console.error('移除书架失败:', error)
    ElMessage.error('移除失败，请稍后重试')
  }
}

/**
 * 点击章节跳转到阅读页
 * @param {number} chapterId - 章节ID
 */
const handleChapterClick = (chapterId) => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  router.push(`/novel/${novelId.value}/chapter/${chapterId}`)
}

/**
 * 章节分页切换
 * @param {number} page - 页码
 */
const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchChapters(page)
}

/**
 * 点赞/取消点赞
 */
const handleLike = async () => {
  const token = localStorage.getItem('token')
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  if (isLikedLoading.value) return
  isLikedLoading.value = true
  
  try {
    if (hasLiked.value) {
      await novelLikeApi.unlikeNovel(parseInt(novelId.value))
      hasLiked.value = false
      if (novel.value && novel.value.totalLikes > 0) {
        novel.value.totalLikes--
      }
      ElMessage.success('已取消点赞')
    } else {
      await novelLikeApi.likeNovel(parseInt(novelId.value))
      hasLiked.value = true
      if (novel.value) {
        novel.value.totalLikes = (novel.value.totalLikes || 0) + 1
      }
      ElMessage.success('点赞成功')
    }
  } catch (error) {
    console.error('点赞操作失败:', error)
    ElMessage.error(error.message || '操作失败，请稍后重试')
  } finally {
    isLikedLoading.value = false
  }
}

/**
 * 检查点赞状态
 */
const checkLikeStatus = async () => {
  const token = localStorage.getItem('token')
  if (!token) return
  
  try {
    const response = await novelLikeApi.checkLike(parseInt(novelId.value))
    if (response.code === 200) {
      hasLiked.value = response.data || false
    }
  } catch (error) {
    console.log('检查点赞状态失败:', error)
  }
}

/**
 * 组件挂载时初始化
 * 加载小说详情、章节列表、书架状态和点赞状态
 */
onMounted(() => {
  window.scrollTo(0, 0)
  fetchNovelDetail()
  fetchChapters()
  checkBookshelf()
  checkLikeStatus()
})
</script>

<template>
  <div class="novel-detail-page" v-if="!loading && novel">
    <div class="novel-info-card">
      <div class="novel-cover-wrapper">
        <img 
          class="novel-cover" 
          :src="novel.cover || novel.coverImage ? `/api/novels/${novel.id}/cover` : 'https://via.placeholder.com/280x420'" 
          :alt="novel.title" 
        />
        <div class="cover-overlay">
          <span class="status-badge" :class="{ completed: novel.status === 'COMPLETED' || novel.status === '已完结' }">
            {{ novel.status === 'COMPLETED' || novel.status === '已完结' ? '已完结' : '连载中' }}
          </span>
        </div>
      </div>
      
      <div class="novel-meta">
        <div class="novel-header">
          <h1 class="novel-title">{{ novel.title }}</h1>
          <p class="novel-author">
            <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
              <circle cx="12" cy="7" r="4"></circle>
            </svg>
            {{ novel.author }}
          </p>
        </div>
        
        <div class="novel-stats">
          <span class="stat-badge">
            <svg class="icon-sm" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
              <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
            </svg>
            {{ novel.category || '未分类' }}
          </span>
          <span class="stat-badge">
            <svg class="icon-sm" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
              <circle cx="12" cy="12" r="3"></circle>
            </svg>
            {{ novel.views || novel.viewCount || 0 }} 阅读
          </span>
          <span class="stat-badge">
            <svg class="icon-sm" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path>
            </svg>
            {{ novel.bookmarks || novel.favoriteCount || 0 }} 收藏
          </span>
          <span 
            class="stat-badge like-badge" 
            :class="{ 'liked': hasLiked }"
            @click="handleLike"
            style="cursor: pointer;"
          >
            <svg class="icon-sm" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
            </svg>
            {{ novel.totalLikes || 0 }} 点赞
          </span>
        </div>
        
        <div class="novel-description-wrapper">
          <h3 class="novel-description-label">
            <svg class="icon-sm" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
              <polyline points="14 2 14 8 20 8"></polyline>
              <line x1="16" y1="13" x2="8" y2="13"></line>
              <line x1="16" y1="17" x2="8" y2="17"></line>
              <polyline points="10 9 9 9 8 9"></polyline>
            </svg>
            内容简介
          </h3>
          <p class="novel-description">{{ novel.description || '暂无简介' }}</p>
        </div>
        
        <div class="novel-actions">
          <button class="btn-start-reading" @click="handleStartReading">
            <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="5 3 19 12 5 21 5 3"></polygon>
            </svg>
            开始阅读
          </button>
          <button 
            class="btn-add-bookshelf" 
            :class="{ 'btn-remove': isInBookshelf }"
            @click="handleAddToBookshelf"
          >
            <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path v-if="!isInBookshelf" d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"></path>
              <path v-else d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
            </svg>
            {{ isInBookshelf ? '移除书架' : '加入书架' }}
          </button>
        </div>
      </div>
    </div>
    
    <div class="chapters-section">
      <h2 class="chapters-header">
        <svg class="icon-lg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
          <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
        </svg>
        章节列表
        <span class="chapter-count">共 {{ total }} 章</span>
      </h2>
      
      <div class="chapter-grid" v-if="chapters.length > 0">
        <div 
          v-for="chapter in chapters" 
          :key="chapter.id" 
          class="chapter-item"
          @click="handleChapterClick(chapter.id)"
        >
          <span class="chapter-number">{{ chapter.chapterNumber || chapter.chapter_number }}</span>
          <span class="chapter-title">{{ chapter.title }}</span>
        </div>
      </div>
      <p v-else class="no-chapters">暂无章节</p>
      
      <div class="pagination-wrapper" v-if="total > pageSize">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
    
    <div class="comments-section">
      <CommentBox :novel-id="novelId" />
    </div>
  </div>
  
  <div v-else-if="loading" class="loading-container">
    <div class="loading-spinner">
      <div class="spinner"></div>
      <p class="loading-text">正在加载小说详情...</p>
    </div>
  </div>
  
  <div v-else-if="!loading && !novel" class="error-page">
    <div class="error-content">
      <svg class="error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"></circle>
        <line x1="12" y1="8" x2="12" y2="12"></line>
        <line x1="12" y1="16" x2="12.01" y2="16"></line>
      </svg>
      <h2>小说不存在</h2>
      <p>您访问的小说不存在或已被删除</p>
      <button class="btn-back" @click="router.push('/')">
        <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="19" y1="12" x2="5" y2="12"></line>
          <polyline points="12 19 5 12 12 5"></polyline>
        </svg>
        返回首页
      </button>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.novel-detail-page {
  max-width: 1280px;
  margin: 0 auto;
  padding: 40px 24px;
  min-height: calc(100vh - 80px);
}

.novel-info-card {
  display: flex;
  gap: 48px;
  background: rgba(22, 27, 34, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  padding: 40px;
  margin-bottom: 32px;
  position: relative;
  overflow: hidden;
  animation: slideUp 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.novel-info-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #c9a962 0%, #d4b978 50%, #c9a962 100%);
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

.novel-cover-wrapper {
  flex-shrink: 0;
  position: relative;
}

.novel-cover {
  width: 280px;
  height: 420px;
  object-fit: cover;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
  border: 2px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.novel-cover:hover {
  transform: scale(1.02) translateY(-4px);
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.5);
}

.cover-overlay {
  position: absolute;
  top: 16px;
  right: 16px;
}

.status-badge {
  padding: 6px 14px;
  background: linear-gradient(135deg, rgba(201, 169, 98, 0.2) 0%, rgba(201, 169, 98, 0.1) 100%);
  border: 1px solid rgba(201, 169, 98, 0.3);
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  color: #c9a962;
  backdrop-filter: blur(10px);
}

.status-badge.completed {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.2) 0%, rgba(34, 197, 94, 0.1) 100%);
  border-color: rgba(34, 197, 94, 0.3);
  color: #22c55e;
}

.novel-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.novel-header {
  margin-bottom: 4px;
}

.novel-title {
  font-size: clamp(32px, 5vw, 48px);
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 12px 0;
  font-family: 'Noto Serif SC', 'Playfair Display', serif;
  letter-spacing: -0.5px;
  line-height: 1.2;
  background: linear-gradient(135deg, #ffffff 0%, #c9a962 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.novel-author {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.icon-sm {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.icon-lg {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}

.novel-stats {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.stat-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  font-weight: 500;
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: all 0.3s ease;
}

.stat-badge:hover {
  background: rgba(201, 169, 98, 0.1);
  border-color: rgba(201, 169, 98, 0.3);
  color: #c9a962;
}

.like-badge.liked {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.2) 0%, rgba(239, 68, 68, 0.1) 100%);
  border-color: rgba(239, 68, 68, 0.4);
  color: #ef4444;
}

.like-badge.liked svg {
  fill: #ef4444;
  stroke: #ef4444;
}

.like-badge:active {
  transform: scale(0.95);
}

.novel-description-wrapper {
  flex: 1;
}

.novel-description-label {
  font-size: 14px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.5);
  margin: 0 0 12px 0;
  display: flex;
  align-items: center;
  gap: 8px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.novel-description {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.75);
  line-height: 1.8;
  margin: 0;
  padding: 20px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 12px;
  border-left: 3px solid #c9a962;
  max-height: 200px;
  overflow-y: auto;
}

.novel-description::-webkit-scrollbar {
  width: 6px;
}

.novel-description::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
}

.novel-description::-webkit-scrollbar-thumb {
  background: rgba(201, 169, 98, 0.3);
  border-radius: 3px;
}

.novel-actions {
  display: flex;
  gap: 16px;
  margin-top: auto;
  padding-top: 20px;
}

.btn-start-reading {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 16px 32px;
  background: linear-gradient(135deg, #c9a962 0%, #d4b978 100%);
  color: #0d1117;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 8px 24px rgba(201, 169, 98, 0.3);
}

.btn-start-reading:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 32px rgba(201, 169, 98, 0.4);
}

.btn-start-reading .icon {
  transition: transform 0.3s ease;
}

.btn-start-reading:hover .icon {
  transform: translateX(4px);
}

.btn-add-bookshelf {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 16px 32px;
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.btn-add-bookshelf:hover {
  background: rgba(201, 169, 98, 0.1);
  border-color: rgba(201, 169, 98, 0.4);
  color: #c9a962;
}

.btn-add-bookshelf.btn-remove {
  color: #ef4444;
  border-color: rgba(239, 68, 68, 0.3);
}

.btn-add-bookshelf.btn-remove:hover {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.5);
}

.chapters-section {
  background: rgba(22, 27, 34, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  padding: 40px;
  margin-bottom: 32px;
  animation: slideUp 0.6s cubic-bezier(0.4, 0, 0.2, 1) 0.2s both;
}

.chapters-header {
  font-size: 28px;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 32px 0;
  font-family: 'Noto Serif SC', 'Playfair Display', serif;
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
}

.chapters-header::after {
  content: '';
  position: absolute;
  bottom: -12px;
  left: 0;
  width: 60px;
  height: 3px;
  background: linear-gradient(90deg, #c9a962 0%, transparent 100%);
  border-radius: 2px;
}

.chapter-count {
  font-size: 14px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.5);
  margin-left: auto;
  font-family: system-ui, -apple-system, sans-serif;
}

.chapter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.chapter-item {
  display: flex;
  align-items: center;
  padding: 14px 18px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
  position: relative;
  overflow: hidden;
}

.chapter-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 3px;
  height: 100%;
  background: linear-gradient(180deg, #c9a962 0%, #d4b978 100%);
  transform: scaleY(0);
  transition: transform 0.3s ease;
}

.chapter-item:hover {
  background: rgba(201, 169, 98, 0.08);
  transform: translateX(6px);
  border-color: rgba(201, 169, 98, 0.2);
  color: #ffffff;
}

.chapter-item:hover::before {
  transform: scaleY(1);
}

.chapter-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  background: rgba(201, 169, 98, 0.1);
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #c9a962;
  margin-right: 12px;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.chapter-item:hover .chapter-number {
  background: linear-gradient(135deg, #c9a962 0%, #d4b978 100%);
  color: #0d1117;
  transform: scale(1.05);
}

.chapter-title {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.no-chapters {
  color: rgba(255, 255, 255, 0.4);
  text-align: center;
  padding: 48px;
  font-size: 16px;
  font-style: italic;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.pagination-wrapper :deep(.el-pagination) {
  --el-pagination-bg-color: rgba(255, 255, 255, 0.05);
  --el-pagination-text-color: rgba(255, 255, 255, 0.7);
  --el-pagination-button-disabled-bg-color: rgba(255, 255, 255, 0.03);
  --el-pagination-button-disabled-color: rgba(255, 255, 255, 0.3);
  --el-pagination-hover-color: #c9a962;
  --el-pagination-button-bg-color: rgba(255, 255, 255, 0.05);
  --el-pagination-button-color: rgba(255, 255, 255, 0.7);
}

.pagination-wrapper :deep(.el-pagination .btn-prev),
.pagination-wrapper :deep(.el-pagination .btn-next),
.pagination-wrapper :deep(.el-pagination .number) {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.3s ease;
}

.pagination-wrapper :deep(.el-pagination .btn-prev:hover),
.pagination-wrapper :deep(.el-pagination .btn-next:hover),
.pagination-wrapper :deep(.el-pagination .number:hover) {
  background: rgba(201, 169, 98, 0.15);
  border-color: rgba(201, 169, 98, 0.3);
  color: #c9a962;
}

.pagination-wrapper :deep(.el-pagination .is-active) {
  background: linear-gradient(135deg, #c9a962 0%, #d4b978 100%);
  border-color: #c9a962;
  color: #0d1117;
}

.comments-section {
  background: rgba(22, 27, 34, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  padding: 40px;
  position: relative;
  overflow: hidden;
  animation: slideUp 0.6s cubic-bezier(0.4, 0, 0.2, 1) 0.4s both;
}

.comments-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #c9a962 0%, #d4b978 50%, #c9a962 100%);
}

.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 80px);
}

.loading-spinner {
  text-align: center;
}

.spinner {
  width: 60px;
  height: 60px;
  border: 3px solid rgba(255, 255, 255, 0.1);
  border-top: 3px solid #c9a962;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 24px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-text {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.6);
  font-weight: 500;
}

.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 80px);
}

.error-content {
  text-align: center;
  padding: 48px 64px;
  background: rgba(22, 27, 34, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  animation: slideUp 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.error-icon {
  width: 64px;
  height: 64px;
  color: #ef4444;
  margin-bottom: 24px;
}

.error-content h2 {
  font-size: 28px;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 12px 0;
  font-family: 'Noto Serif SC', 'Playfair Display', serif;
}

.error-content p {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 32px 0;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 28px;
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-back:hover {
  background: rgba(201, 169, 98, 0.1);
  border-color: rgba(201, 169, 98, 0.3);
  color: #c9a962;
}

@media (max-width: 1024px) {
  .novel-detail-page {
    padding: 24px 16px;
  }
  
  .novel-info-card {
    padding: 32px;
  }
  
  .novel-cover {
    width: 220px;
    height: 330px;
  }
}

@media (max-width: 768px) {
  .novel-info-card {
    flex-direction: column;
    padding: 24px;
    gap: 32px;
  }
  
  .novel-cover-wrapper {
    display: flex;
    justify-content: center;
  }
  
  .novel-cover {
    width: 180px;
    height: 270px;
  }
  
  .novel-title {
    font-size: 28px;
    text-align: center;
  }
  
  .novel-author {
    justify-content: center;
  }
  
  .novel-stats {
    justify-content: center;
  }
  
  .novel-actions {
    flex-direction: column;
  }
  
  .btn-start-reading,
  .btn-add-bookshelf {
    width: 100%;
    justify-content: center;
  }
  
  .chapter-grid {
    grid-template-columns: 1fr;
  }
  
  .chapters-section,
  .comments-section {
    padding: 24px;
  }
  
  .chapters-header {
    font-size: 22px;
    flex-wrap: wrap;
  }
  
  .chapter-count {
    width: 100%;
    margin-left: 0;
    margin-top: 8px;
  }
}
</style>
