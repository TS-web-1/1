<template>
  <div class="recommendation-page">
    <!-- 返回按钮 -->
    <div class="back-button" @click="goBack">
      <span class="arrow">←</span>
      <span>返回</span>
    </div>

    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">{{ pageTitle }}</h1>
      <p class="page-description">{{ pageDescription }}</p>
    </div>

    <!-- 小说列表 -->
    <div class="novel-grid">
      <NovelCard 
        v-for="novel in novels"
        :key="novel.id"
        :novel="novel"
      />
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-more">
      <div class="loading-dots">
        <span></span>
        <span></span>
        <span></span>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && novels.length === 0" class="empty-state">
      <p>暂无推荐小说</p>
    </div>

    <!-- 分页器 -->
    <div v-if="!loading && totalPages > 1" class="pagination-container">
      <div class="pagination">
        <button 
          class="page-btn page-btn-arrow"
          @click="goToFirstPage"
          :disabled="currentPage === 0"
          title="首页"
        >
          «
        </button>
        <button 
          class="page-btn page-btn-arrow"
          @click="goToPrevPage"
          :disabled="currentPage === 0"
          title="上一页"
        >
          ‹
        </button>
        
        <button 
          v-for="page in getPageNumbers" 
          :key="page"
          class="page-btn page-btn-number"
          :class="{ active: currentPage === page }"
          @click="goToPage(page)"
        >
          {{ page + 1 }}
        </button>
        
        <button 
          class="page-btn page-btn-arrow"
          @click="goToNextPage"
          :disabled="currentPage === totalPages - 1"
          title="下一页"
        >
          ›
        </button>
        <button 
          class="page-btn page-btn-arrow"
          @click="goToLastPage"
          :disabled="currentPage === totalPages - 1"
          title="末页"
        >
          »
        </button>
      </div>
      
      <div class="pagination-info">
        <span>共 {{ totalElements }} 本小说</span>
        <span>第 {{ currentPage + 1 }} / {{ totalPages }} 页</span>
      </div>
      
      <div class="pagination-jump">
        <span>跳转到</span>
        <input 
          type="number" 
          v-model.number="jumpPage"
          @keyup.enter="handleJumpPage"
          :min="1"
          :max="totalPages"
          class="jump-input"
        />
        <button @click="handleJumpPage" class="jump-btn">确定</button>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * RecommendationList.vue - 推荐列表页面组件
 * 
 * 该组件实现了小说推荐列表功能，包括：
 * - 个性化推荐：根据用户阅读喜好推荐
 * - 重磅推荐：收藏人数最多的热门小说
 * - 热门推荐：阅读历史记录最多的小说
 * - 完结精品：收藏数最多的完结小说
 * - 分页显示
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import NovelCard from '../components/NovelCard.vue'
import { novelApi } from '../api/modules/novel.js'
import { recommendationApi } from '../api/modules/recommendation.js'

const route = useRoute()
const router = useRouter()

const pageConfig = {
  personalized: {
    title: '个性化推荐',
    description: '根据您的阅读喜好和行为习惯，为您精心挑选的小说',
    type: 'personalized'
  },
  bookmarks: {
    title: '重磅推荐',
    description: '全站收藏人数最多的热门小说，本本精彩，不容错过',
    type: 'bookmarks'
  },
  'reading-history': {
    title: '热门推荐',
    description: '阅读历史记录最多的小说，人气爆棚',
    type: 'reading-history'
  },
  completed: {
    title: '完结精品',
    description: '观看人数与收藏数最多的完结小说，一次看过瘾',
    type: 'completed'
  }
}

const pageType = route.params.type || 'personalized'
const config = pageConfig[pageType] || pageConfig.personalized

const pageTitle = ref(config.title)
const pageDescription = ref(config.description)
const novels = ref([])
const loading = ref(false)
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const pageSize = ref(24)
const jumpPage = ref(1)

/**
 * 加载小说列表
 * @param {number} page - 页码
 */
const loadNovels = async (page = 0) => {
  loading.value = true
  try {
    let response
    
    if (pageType === 'personalized') {
      // 个性化推荐
      response = await recommendationApi.getPersonalizedRecommendations(100)
      if (response.code === 200 && response.data) {
        const novelList = Array.isArray(response.data) ? response.data : (response.data.content || [])
        const allNovels = novelList.map(novel => ({
          id: novel.id,
          title: novel.title,
          author: novel.author,
          description: novel.description,
          cover: novel.coverImage || 'https://via.placeholder.com/150x200',
          category: novel.category,
          status: novel.status
        }))
        
        // 计算分页
        totalElements.value = allNovels.length
        totalPages.value = Math.ceil(allNovels.length / pageSize.value)
        const start = page * pageSize.value
        const end = start + pageSize.value
        novels.value = allNovels.slice(start, end)
      }
    } else if (pageType === 'bookmarks') {
      // 重磅推荐（收藏人数最多）
      response = await recommendationApi.getBookmarkRecommendations(100)
      if (response.code === 200 && response.data) {
        const novelList = Array.isArray(response.data) ? response.data : (response.data.content || [])
        const allNovels = novelList.map(novel => ({
          id: novel.id,
          title: novel.title,
          author: novel.author,
          description: novel.description,
          cover: novel.coverImage || 'https://via.placeholder.com/150x200',
          category: novel.category,
          status: novel.status,
          views: novel.views || 0,
          bookmarks: novel.bookmarks || 0
        }))
        
        // 计算分页
        totalElements.value = allNovels.length
        totalPages.value = Math.ceil(allNovels.length / pageSize.value)
        const start = page * pageSize.value
        const end = start + pageSize.value
        novels.value = allNovels.slice(start, end)
      }
    } else if (pageType === 'reading-history') {
      // 热门推荐（阅读历史最多）
      response = await recommendationApi.getReadingHistoryRecommendations(100)
      if (response.code === 200 && response.data) {
        const novelList = Array.isArray(response.data) ? response.data : (response.data.content || [])
        const allNovels = novelList.map(novel => ({
          id: novel.id,
          title: novel.title,
          author: novel.author,
          description: novel.description,
          cover: novel.coverImage || 'https://via.placeholder.com/150x200',
          category: novel.category,
          status: novel.status,
          views: novel.views || 0,
          bookmarks: novel.bookmarks || 0
        }))
        
        // 计算分页
        totalElements.value = allNovels.length
        totalPages.value = Math.ceil(allNovels.length / pageSize.value)
        const start = page * pageSize.value
        const end = start + pageSize.value
        novels.value = allNovels.slice(start, end)
      }
    } else if (pageType === 'completed') {
      // 完结精品（观看人数与收藏数最多的完结小说）
      response = await recommendationApi.getCompletedRecommendations(100)
      if (response.code === 200 && response.data) {
        const novelList = Array.isArray(response.data) ? response.data : (response.data.content || [])
        const allNovels = novelList.map(novel => ({
          id: novel.id,
          title: novel.title,
          author: novel.author,
          description: novel.description,
          cover: novel.coverImage || 'https://via.placeholder.com/150x200',
          category: novel.category,
          status: novel.status,
          views: novel.views || 0,
          bookmarks: novel.bookmarks || 0
        }))
        
        // 计算分页
        totalElements.value = allNovels.length
        totalPages.value = Math.ceil(allNovels.length / pageSize.value)
        const start = page * pageSize.value
        const end = start + pageSize.value
        novels.value = allNovels.slice(start, end)
      }
    }
  } catch (error) {
    console.error('加载小说列表失败:', error)
    ElMessage.error('加载小说列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 分页控制函数
const goToPage = (page) => {
  if (page < 0 || page >= totalPages.value) return
  currentPage.value = page
  loadNovels(page)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const goToFirstPage = () => {
  goToPage(0)
}

const goToLastPage = () => {
  goToPage(totalPages.value - 1)
}

const goToPrevPage = () => {
  goToPage(currentPage.value - 1)
}

const goToNextPage = () => {
  goToPage(currentPage.value + 1)
}

const handleJumpPage = () => {
  if (jumpPage.value && jumpPage.value >= 1 && jumpPage.value <= totalPages.value) {
    goToPage(jumpPage.value - 1)
    jumpPage.value = 1
  } else {
    ElMessage.warning(`请输入有效的页码（1-${totalPages.value}）`)
  }
}

// 生成页码列表（最多显示 5 个页码）
const getPageNumbers = computed(() => {
  const pages = []
  const maxVisible = 5
  let start = Math.max(0, currentPage.value - Math.floor(maxVisible / 2))
  let end = Math.min(totalPages.value, start + maxVisible)
  
  if (end - start < maxVisible) {
    start = Math.max(0, end - maxVisible)
  }
  
  for (let i = start; i < end; i++) {
    pages.push(i)
  }
  
  return pages
})

const goBack = () => {
  router.push('/')
}

onMounted(() => {
  loadNovels()
})
</script>

<style scoped>
.recommendation-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #0f1419 0%, #1a2634 50%, #0d1117 100%);
  padding: 40px 20px;
}

.back-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #8b949e;
  cursor: pointer;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
  margin-bottom: 30px;
}

.back-button:hover {
  transform: translateX(-4px);
  background: rgba(201, 169, 98, 0.1);
  border-color: rgba(201, 169, 98, 0.3);
  color: #c9a962;
}

.back-button .arrow {
  font-size: 20px;
  font-weight: bold;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-title {
  font-size: 36px;
  font-weight: 700;
  color: #f0f6fc;
  margin: 0 0 10px 0;
}

.page-description {
  font-size: 16px;
  color: #8b949e;
  margin: 0;
}

.novel-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 24px;
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px 0;
}

.loading-more {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #8b949e;
  font-size: 16px;
}

/* 分页器 */
.pagination-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 40px 0;
  margin-top: 20px;
}

.pagination {
  display: flex;
  gap: 8px;
  align-items: center;
}

.page-btn {
  min-width: 40px;
  height: 40px;
  padding: 0 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  color: #8b949e;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.page-btn:hover:not(:disabled) {
  background: rgba(201, 169, 98, 0.15);
  border-color: rgba(201, 169, 98, 0.4);
  color: #c9a962;
  transform: translateY(-2px);
}

.page-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.page-btn-number.active {
  background: linear-gradient(135deg, #c9a962 0%, #d4b978 100%);
  color: #0d1117;
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(201, 169, 98, 0.3);
}

.pagination-info {
  display: flex;
  gap: 20px;
  color: #8b949e;
  font-size: 14px;
}

.pagination-jump {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #8b949e;
  font-size: 14px;
}

.jump-input {
  width: 60px;
  height: 36px;
  padding: 0 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  text-align: center;
  font-size: 14px;
  outline: none;
  background: rgba(255, 255, 255, 0.05);
  color: #f0f6fc;
  transition: all 0.3s ease;
}

.jump-input:focus {
  border-color: #c9a962;
  box-shadow: 0 0 0 3px rgba(201, 169, 98, 0.1);
}

.jump-btn {
  height: 36px;
  padding: 0 16px;
  background: linear-gradient(135deg, #c9a962 0%, #d4b978 100%);
  color: #0d1117;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.jump-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(201, 169, 98, 0.3);
}

.loading-dots {
  display: flex;
  gap: 8px;
}

.loading-dots span {
  width: 12px;
  height: 12px;
  background: #c9a962;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}
</style>
