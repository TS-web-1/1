<script setup>
/**
 * 首页视图组件
 * 展示小说推荐、轮播图和分类浏览功能
 * 
 * 优化点：
 * 1. 使用 shallowRef 优化大型数据响应式性能
 * 2. 使用 computed 缓存计算结果
 * 3. 优化 watch 使用，避免不必要的重新渲染
 * 4. 添加错误边界处理
 */

import { 
  ref, 
  shallowRef,  // 用于大型数据，减少响应式开销
  onMounted, 
  onUnmounted, 
  computed, 
  watch,
  nextTick
} from 'vue'
import { useRouter } from 'vue-router'
import NovelCard from '../components/NovelCard.vue'
import { novelApi } from '../api/modules/novel.js'
import { recommendationApi } from '../api/modules/recommendation.js'
import { ElMessage } from 'element-plus'

// ==================== Constants ====================
const DISPLAY_LIMIT = 9
const PAGE_SIZE = 24
const AUTO_PLAY_INTERVAL = 5000
const IMAGE_PROXY = '/api/proxy/image?url='

// ==================== Router & State ====================
const router = useRouter()

// 使用 shallowRef 优化大型数组的响应式性能
const novels = shallowRef([])
const personalizedNovels = shallowRef([])
const carouselItems = shallowRef([])

// 基础状态
const pageLoading = ref(true)
const contentLoaded = ref(false)
const loading = ref(false)
const activeTab = ref('recommend')
const currentSlide = ref(0)
const isLoggedIn = ref(localStorage.getItem('token') !== null)

// 分页状态
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const jumpPage = ref(1)

// 自动播放定时器（使用 ref 以便清理）
const autoPlayInterval = ref(null)

// ==================== Computed ====================
/**
 * 标签页配置 - 使用 computed 缓存
 */
const tabs = computed(() => {
  const baseTabs = [
    { id: 'recommend', label: '重磅推荐', icon: '🔥' },
    { id: 'latest', label: '热门推荐', icon: '📚' },
    { id: 'completed', label: '完结精品', icon: '✅' }
  ]
  
  if (isLoggedIn.value) {
    return [
      { id: 'personalized', label: '个性化推荐', icon: '✨' },
      ...baseTabs
    ]
  }
  return baseTabs
})

/**
 * 当前显示的小说列表 - 使用 computed 避免重复计算
 */
const displayedNovels = computed(() => {
  return activeTab.value === 'personalized' 
    ? personalizedNovels.value 
    : novels.value
})

/**
 * 获取显示的页码列表 - 使用 computed 缓存
 */
const pageNumbers = computed(() => {
  const pages = []
  const maxVisible = 5
  const halfVisible = Math.floor(maxVisible / 2)
  
  let start = Math.max(0, currentPage.value - halfVisible)
  let end = Math.min(totalPages.value, start + maxVisible)
  
  if (end - start < maxVisible) {
    start = Math.max(0, end - maxVisible)
  }
  
  for (let i = start; i < end; i++) {
    pages.push(i)
  }
  
  return pages
})

/**
 * 是否显示分页 - 使用 computed
 */
const showPagination = computed(() => {
  return !loading.value && totalPages.value > 1 && activeTab.value !== 'personalized'
})

// ==================== Methods ====================
/**
 * 代理图片URL
 */
const proxyImageUrl = (url) => {
  if (!url) return 'https://picsum.photos/seed/default/800/400'
  if (!url.startsWith('http')) return url
  if (url.includes('unsplash') || url.includes('placeholder') || url.includes('picsum')) {
    return `${IMAGE_PROXY}${encodeURIComponent(url)}`
  }
  return url
}

/**
 * 轮播图控制
 */
const nextSlide = () => {
  if (carouselItems.value.length === 0) return
  currentSlide.value = (currentSlide.value + 1) % carouselItems.value.length
}

const prevSlide = () => {
  if (carouselItems.value.length === 0) return
  currentSlide.value = (currentSlide.value - 1 + carouselItems.value.length) % carouselItems.value.length
}

const goToSlide = (index) => {
  currentSlide.value = index
}

/**
 * 自动播放控制
 */
const startAutoPlay = () => {
  stopAutoPlay()
  if (carouselItems.value.length > 1) {
    autoPlayInterval.value = setInterval(nextSlide, AUTO_PLAY_INTERVAL)
  }
}

const stopAutoPlay = () => {
  if (autoPlayInterval.value) {
    clearInterval(autoPlayInterval.value)
    autoPlayInterval.value = null
  }
}

/**
 * 导航方法
 */
const goToNovel = (novelId) => {
  router.push(`/novel/${novelId}`)
}

const viewAll = () => {
  const typeMap = {
    personalized: 'personalized',
    recommend: 'bookmarks',
    latest: 'reading-history',
    completed: 'completed'
  }
  router.push(`/recommendations/${typeMap[activeTab.value]}`)
}

/**
 * 分页控制
 */
const goToPage = (page) => {
  if (page < 0 || page >= totalPages.value) return
  currentPage.value = page
  fetchNovels(page)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const goToFirstPage = () => goToPage(0)
const goToLastPage = () => goToPage(totalPages.value - 1)
const goToPrevPage = () => goToPage(currentPage.value - 1)
const goToNextPage = () => goToPage(currentPage.value + 1)

const handleJumpPage = () => {
  if (jumpPage.value >= 1 && jumpPage.value <= totalPages.value) {
    goToPage(jumpPage.value - 1)
    jumpPage.value = 1
  } else {
    ElMessage.warning(`请输入有效的页码（1-${totalPages.value}）`)
  }
}

// ==================== Data Fetching ====================
/**
 * 加载轮播图小说
 */
const loadCarouselNovels = async () => {
  try {
    const response = await recommendationApi.getPopularRecommendations(3)
    if (response.code === 200 && response.data) {
      carouselItems.value = response.data.map((novel) => ({
        id: novel.id,
        title: novel.title,
        subtitle: `${novel.author} · ${novel.category}小说`,
        description: novel.description || `这是一本精彩的${novel.category}小说。`,
        image: `/api/novels/${novel.id}/cover`,
        novelId: novel.id
      }))
    }
  } catch (error) {
    console.error('加载轮播图失败:', error)
    // 使用默认数据
    carouselItems.value = [
      {
        id: 1,
        title: "热门精选",
        subtitle: "平台推荐 · 精彩小说",
        description: "这是为您精心挑选的热门小说。",
        image: "https://picsum.photos/seed/carousel1/800/400",
        novelId: 1
      }
    ]
  }
}

/**
 * 获取小说列表 - 带错误处理
 */
const fetchNovels = async (page = 0) => {
  loading.value = true
  try {
    const params = {
      page,
      size: PAGE_SIZE,
      category: activeTab.value === 'completed' ? '' : 'all',
      status: activeTab.value === 'completed' ? 'completed' : ''
    }
    
    const response = await novelApi.getNovels(params)
    if (response.code === 200 && response.data) {
      let novelList = []
      if (Array.isArray(response.data)) {
        novelList = response.data
        totalPages.value = 1
        totalElements.value = response.data.length
      } else if (response.data.content && Array.isArray(response.data.content)) {
        novelList = response.data.content
        totalPages.value = response.data.totalPages || 1
        totalElements.value = response.data.totalElements || response.data.content.length
        currentPage.value = response.data.currentPage || response.data.number || page
      }
      
      // 使用 shallowRef 减少响应式开销
      novels.value = novelList.map(novel => ({
        id: novel.id,
        title: novel.title,
        author: novel.author,
        description: novel.description,
        cover: novel.coverImage || 'https://via.placeholder.com/150x200',
        category: novel.category,
        status: novel.status,
        views: novel.views || 0,
        bookmarks: novel.bookmarks || 0,
        totalLikes: novel.totalLikes || 0,
        createdAt: novel.createdAt || new Date().toISOString()
      }))
    }
  } catch (error) {
    console.error('获取小说列表失败:', error)
    ElMessage.error('获取小说列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

/**
 * 获取个性化推荐
 */
const fetchPersonalizedRecommendations = async () => {
  try {
    const response = await recommendationApi.getPersonalizedRecommendations(DISPLAY_LIMIT)
    if (response.code === 200 && response.data) {
      const novelList = Array.isArray(response.data) 
        ? response.data 
        : (response.data.content || [])
      
      personalizedNovels.value = novelList.slice(0, DISPLAY_LIMIT).map(novel => ({
        id: novel.id,
        title: novel.title,
        author: novel.author,
        description: novel.description,
        cover: novel.coverImage || 'https://via.placeholder.com/150x200',
        category: novel.category,
        status: novel.status,
        views: novel.views || 0,
        bookmarks: novel.bookmarks || 0,
        totalLikes: novel.totalLikes || 0
      }))
    }
  } catch (error) {
    console.error('获取个性化推荐失败:', error)
    personalizedNovels.value = []
  }
}

/**
 * 获取重磅推荐
 */
const fetchPopularRecommendations = async () => {
  try {
    const response = await recommendationApi.getBookmarkRecommendations(DISPLAY_LIMIT)
    if (response.code === 200 && response.data) {
      const novelList = Array.isArray(response.data) 
        ? response.data 
        : (response.data.content || [])
      
      novels.value = novelList.slice(0, DISPLAY_LIMIT).map(novel => ({
        id: novel.id,
        title: novel.title,
        author: novel.author,
        description: novel.description,
        cover: novel.coverImage || 'https://via.placeholder.com/150x200',
        category: novel.category,
        status: novel.status,
        views: novel.views || 0,
        bookmarks: novel.bookmarks || 0,
        totalLikes: novel.totalLikes || 0
      }))
    }
  } catch (error) {
    console.error('获取重磅推荐失败:', error)
    novels.value = []
  }
}

/**
 * 获取热门推荐
 */
const fetchLatestUpdates = async () => {
  try {
    const response = await recommendationApi.getReadingHistoryRecommendations(DISPLAY_LIMIT)
    if (response.code === 200 && response.data) {
      const novelList = Array.isArray(response.data) 
        ? response.data 
        : (response.data.content || [])
      
      novels.value = novelList.slice(0, DISPLAY_LIMIT).map(novel => ({
        id: novel.id,
        title: novel.title,
        author: novel.author,
        description: novel.description,
        cover: novel.coverImage || 'https://via.placeholder.com/150x200',
        category: novel.category,
        status: novel.status,
        views: novel.views || 0,
        bookmarks: novel.bookmarks || 0,
        totalLikes: novel.totalLikes || 0
      }))
    }
  } catch (error) {
    console.error('获取热门推荐失败:', error)
    novels.value = []
  }
}

/**
 * 获取完结精品
 */
const fetchCompletedNovels = async () => {
  try {
    const response = await recommendationApi.getCompletedRecommendations(DISPLAY_LIMIT)
    if (response.code === 200 && response.data) {
      const novelList = Array.isArray(response.data) 
        ? response.data 
        : (response.data.content || [])
      
      novels.value = novelList.slice(0, DISPLAY_LIMIT).map(novel => ({
        id: novel.id,
        title: novel.title,
        author: novel.author,
        description: novel.description,
        cover: novel.coverImage || 'https://via.placeholder.com/150x200',
        category: novel.category,
        status: novel.status,
        views: novel.views || 0,
        bookmarks: novel.bookmarks || 0,
        totalLikes: novel.totalLikes || 0
      }))
    }
  } catch (error) {
    console.error('获取完结精品失败:', error)
    novels.value = []
  }
}

/**
 * 加载标签页数据
 */
const loadTabData = (tab) => {
  currentPage.value = 0
  const loaders = {
    personalized: fetchPersonalizedRecommendations,
    recommend: fetchPopularRecommendations,
    latest: fetchLatestUpdates,
    completed: fetchCompletedNovels
  }
  
  const loader = loaders[tab]
  if (loader) {
    loader()
  }
}

// ==================== Lifecycle ====================
/**
 * 监听标签变化 - 使用 watch 优化
 */
watch(activeTab, (newTab) => {
  loadTabData(newTab)
})

/**
 * 组件挂载
 */
onMounted(() => {
  // 页面加载动画
  setTimeout(() => {
    pageLoading.value = false
    nextTick(() => {
      setTimeout(() => {
        contentLoaded.value = true
      }, 300)
    })
  }, 800)
  
  // 初始化
  if (!isLoggedIn.value) {
    activeTab.value = 'recommend'
  }
  
  loadCarouselNovels()
  loadTabData(activeTab.value)
  startAutoPlay()
})

/**
 * 组件卸载 - 清理资源
 */
onUnmounted(() => {
  stopAutoPlay()
})
</script>

<template>
  <Transition name="page-fade">
    <div v-if="pageLoading" class="page-loader">
      <div class="loader-content">
        <div class="loader-icon">
          <svg viewBox="0 0 100 100" class="book-svg">
            <path class="book-path" d="M20,20 L50,10 L80,20 L80,80 L50,90 L20,80 Z" fill="none" stroke="currentColor" stroke-width="2"/>
            <path class="book-page" d="M50,10 L50,90" fill="none" stroke="currentColor" stroke-width="1"/>
          </svg>
        </div>
        <p class="loader-text">正在加载精彩内容...</p>
      </div>
    </div>
  </Transition>

  <div v-show="!pageLoading" class="home" :class="{ 'content-loaded': contentLoaded }">
    <!-- 轮播图区域 -->
    <section class="hero-section">
      <div class="carousel-container" @mouseenter="stopAutoPlay" @mouseleave="startAutoPlay">
        <div class="carousel-track">
          <TransitionGroup name="carousel-fade">
            <div 
              v-for="(item, index) in carouselItems" 
              :key="item.id"
              v-show="index === currentSlide"
              class="carousel-slide"
              @click="goToNovel(item.novelId)"
            >
              <div class="slide-image">
                <img :src="item.image" :alt="item.title" loading="eager" @error="$event.target.src='https://picsum.photos/seed/carousel-default/800/400'" />
                <div class="image-overlay"></div>
              </div>
              <div class="slide-content">
                <span class="slide-category">{{ item.subtitle.split(' · ')[1] }}</span>
                <h2 class="slide-title">{{ item.title }}</h2>
                <p class="slide-author">{{ item.subtitle.split(' · ')[0] }}</p>
                <p class="slide-description">{{ item.description }}</p>
                <button class="slide-cta">
                  <span>开始阅读</span>
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M5 12h14M12 5l7 7-7 7"/>
                  </svg>
                </button>
              </div>
            </div>
          </TransitionGroup>
        </div>
        
        <button class="carousel-nav prev" @click.stop="prevSlide" aria-label="上一张">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M15 18l-6-6 6-6"/>
          </svg>
        </button>
        <button class="carousel-nav next" @click.stop="nextSlide" aria-label="下一张">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 18l6-6-6-6"/>
          </svg>
        </button>
        
        <div class="carousel-indicators">
          <button 
            v-for="(_, index) in carouselItems" 
            :key="index"
            :class="['indicator', { active: index === currentSlide }]"
            @click.stop="goToSlide(index)"
            :aria-label="`跳转到第 ${index + 1} 张`"
          ></button>
        </div>
      </div>
    </section>

    <!-- 内容区域 -->
    <section class="content-section">
      <div class="section-header">
        <nav class="tab-nav" role="tablist">
          <button 
            v-for="tab in tabs" 
            :key="tab.id"
            :class="['tab-btn', { active: activeTab === tab.id }]"
            @click="activeTab = tab.id"
            role="tab"
            :aria-selected="activeTab === tab.id"
          >
            <span class="tab-icon">{{ tab.icon }}</span>
            <span class="tab-label">{{ tab.label }}</span>
          </button>
        </nav>
        
        <button class="view-all-btn" @click="viewAll">
          <span>查看全部</span>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M5 12h14M12 5l7 7-7 7"/>
          </svg>
        </button>
      </div>

      <div class="novels-container">
        <TransitionGroup name="novel-list" tag="div" class="novel-grid">
          <NovelCard 
            v-for="novel in displayedNovels"
            :key="novel.id"
            :novel="novel"
          />
        </TransitionGroup>
        
        <div v-if="displayedNovels.length === 0 && !loading" class="empty-state">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/>
          </svg>
          <p>暂无推荐小说</p>
        </div>

        <div v-if="loading" class="loading-state">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>
      </div>

      <!-- 分页 -->
      <nav v-if="showPagination" class="pagination" aria-label="分页导航">
        <div class="pagination-controls">
          <button class="page-btn" @click="goToFirstPage" :disabled="currentPage === 0" aria-label="首页">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 17l-5-5 5-5M18 17l-5-5 5-5"/>
            </svg>
          </button>
          <button class="page-btn" @click="goToPrevPage" :disabled="currentPage === 0" aria-label="上一页">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M15 18l-6-6 6-6"/>
            </svg>
          </button>
          
          <div class="page-numbers">
            <button 
              v-for="page in pageNumbers" 
              :key="page"
              :class="['page-num', { active: currentPage === page }]"
              @click="goToPage(page)"
              :aria-label="`第 ${page + 1} 页`"
              :aria-current="currentPage === page ? 'page' : null"
            >
              {{ page + 1 }}
            </button>
          </div>
          
          <button class="page-btn" @click="goToNextPage" :disabled="currentPage === totalPages - 1" aria-label="下一页">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 18l6-6-6-6"/>
            </svg>
          </button>
          <button class="page-btn" @click="goToLastPage" :disabled="currentPage === totalPages - 1" aria-label="末页">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M13 17l5-5-5-5M6 17l5-5-5-5"/>
            </svg>
          </button>
        </div>
        
        <div class="pagination-info">
          <span>共 {{ totalElements }} 本</span>
          <span>第 {{ currentPage + 1 }} / {{ totalPages }} 页</span>
        </div>
        
        <div class="pagination-jump">
          <label for="page-jump">跳转至</label>
          <input 
            id="page-jump"
            type="number" 
            v-model.number="jumpPage"
            @keyup.enter="handleJumpPage"
            :min="1"
            :max="totalPages"
            class="jump-input"
          />
          <button class="jump-btn" @click="handleJumpPage">确定</button>
        </div>
      </nav>
    </section>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

/* 页面加载动画 */
.page-loader {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f1419 0%, #1a2634 50%, #0d1117 100%);
  z-index: 9999;
}

.loader-content {
  text-align: center;
}

.loader-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  color: #c9a962;
  animation: float 3s ease-in-out infinite;
}

.book-svg {
  width: 100%;
  height: 100%;
}

.book-path {
  stroke-dasharray: 300;
  stroke-dashoffset: 300;
  animation: draw 2s ease forwards;
}

.book-page {
  stroke-dasharray: 100;
  stroke-dashoffset: 100;
  animation: draw 1.5s ease 0.5s forwards;
}

@keyframes draw {
  to { stroke-dashoffset: 0; }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.loader-text {
  color: #8b949e;
  font-size: 14px;
  font-family: 'Noto Serif SC', serif;
  letter-spacing: 2px;
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.5s ease;
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
}

/* 主页样式 */
.home {
  min-height: 100vh;
  background: linear-gradient(180deg, #0d1117 0%, #161b22 100%);
  opacity: 0;
  transform: translateY(20px);
  transition: opacity 0.6s ease, transform 0.6s ease;
}

.home.content-loaded {
  opacity: 1;
  transform: translateY(0);
}

/* 轮播图样式 */
.hero-section {
  position: relative;
  padding: 0;
}

.carousel-container {
  position: relative;
  width: 100%;
  height: 70vh;
  min-height: 500px;
  max-height: 700px;
  overflow: hidden;
}

.carousel-track {
  position: relative;
  width: 100%;
  height: 100%;
}

.carousel-slide {
  position: absolute;
  inset: 0;
  cursor: pointer;
}

.slide-image {
  position: absolute;
  inset: 0;
}

.slide-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.8s ease;
}

.carousel-slide:hover .slide-image img {
  transform: scale(1.05);
}

.image-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    135deg,
    rgba(13, 17, 23, 0.7) 0%,
    rgba(13, 17, 23, 0.4) 40%,
    rgba(13, 17, 23, 0.8) 100%
  );
}

.slide-content {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 60px 80px;
  z-index: 10;
}

.slide-category {
  display: inline-block;
  padding: 6px 16px;
  background: rgba(201, 169, 98, 0.15);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(201, 169, 98, 0.3);
  border-radius: 20px;
  color: #c9a962;
  font-size: 12px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 16px;
}

.slide-title {
  font-family: 'Playfair Display', 'Noto Serif SC', serif;
  font-size: clamp(32px, 5vw, 56px);
  font-weight: 700;
  color: #f0f6fc;
  margin: 0 0 12px;
  line-height: 1.2;
  text-shadow: 0 4px 20px rgba(0, 0, 0, 0.5);
}

.slide-author {
  font-size: 16px;
  color: #8b949e;
  margin: 0 0 16px;
  font-weight: 500;
}

.slide-description {
  font-size: 15px;
  color: #c9d1d9;
  margin: 0 0 28px;
  line-height: 1.7;
  max-width: 500px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.slide-cta {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 14px 32px;
  background: linear-gradient(135deg, #c9a962 0%, #d4b978 100%);
  border: none;
  border-radius: 8px;
  color: #0d1117;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 20px rgba(201, 169, 98, 0.3);
}

.slide-cta:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(201, 169, 98, 0.4);
}

.slide-cta svg {
  transition: transform 0.3s ease;
}

.slide-cta:hover svg {
  transform: translateX(4px);
}

.carousel-nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 52px;
  height: 52px;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  color: #f0f6fc;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  z-index: 20;
}

.carousel-nav:hover {
  background: rgba(201, 169, 98, 0.2);
  border-color: rgba(201, 169, 98, 0.4);
  transform: translateY(-50%) scale(1.1);
}

.carousel-nav.prev { left: 30px; }
.carousel-nav.next { right: 30px; }

.carousel-indicators {
  position: absolute;
  bottom: 30px;
  right: 80px;
  display: flex;
  gap: 10px;
  z-index: 20;
}

.indicator {
  width: 40px;
  height: 4px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 2px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.indicator.active {
  background: #c9a962;
  width: 60px;
}

/* 内容区域 */
.content-section {
  padding: 60px 80px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 40px;
  gap: 24px;
}

.tab-nav {
  display: flex;
  gap: 8px;
  background: rgba(255, 255, 255, 0.03);
  padding: 6px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: #8b949e;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tab-btn:hover {
  color: #c9d1d9;
  background: rgba(255, 255, 255, 0.05);
}

.tab-btn.active {
  background: linear-gradient(135deg, #c9a962 0%, #d4b978 100%);
  color: #0d1117;
  box-shadow: 0 4px 15px rgba(201, 169, 98, 0.3);
}

.tab-icon {
  font-size: 16px;
}

.view-all-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: transparent;
  border: 1px solid rgba(201, 169, 98, 0.3);
  border-radius: 8px;
  color: #c9a962;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.view-all-btn:hover {
  background: rgba(201, 169, 98, 0.1);
  border-color: #c9a962;
}

.view-all-btn svg {
  transition: transform 0.3s ease;
}

.view-all-btn:hover svg {
  transform: translateX(4px);
}

.novels-container {
  min-height: 400px;
}

.novel-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 24px;
}

.novel-list-enter-active,
.novel-list-leave-active {
  transition: all 0.4s ease;
}

.novel-list-enter-from,
.novel-list-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

.empty-state,
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: #8b949e;
}

.empty-state svg {
  margin-bottom: 16px;
  opacity: 0.5;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(201, 169, 98, 0.2);
  border-top-color: #c9a962;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 分页 */
.pagination {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  padding: 40px 0;
  margin-top: 40px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: #8b949e;
  cursor: pointer;
  transition: all 0.3s ease;
}

.page-btn:hover:not(:disabled) {
  background: rgba(201, 169, 98, 0.1);
  border-color: rgba(201, 169, 98, 0.3);
  color: #c9a962;
}

.page-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  gap: 6px;
}

.page-num {
  min-width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 8px;
  color: #8b949e;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.page-num:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #c9d1d9;
}

.page-num.active {
  background: linear-gradient(135deg, #c9a962 0%, #d4b978 100%);
  color: #0d1117;
  border-color: transparent;
}

.pagination-info {
  display: flex;
  gap: 24px;
  font-size: 13px;
  color: #8b949e;
}

.pagination-info span {
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.pagination-jump {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.pagination-jump label {
  font-size: 13px;
  color: #8b949e;
}

.jump-input {
  width: 60px;
  height: 36px;
  padding: 0 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: #c9d1d9;
  font-size: 14px;
  text-align: center;
  transition: all 0.3s ease;
}

.jump-input:focus {
  outline: none;
  border-color: #c9a962;
  box-shadow: 0 0 0 3px rgba(201, 169, 98, 0.1);
}

.jump-btn {
  height: 36px;
  padding: 0 16px;
  background: linear-gradient(135deg, #c9a962 0%, #d4b978 100%);
  border: none;
  border-radius: 6px;
  color: #0d1117;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.jump-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(201, 169, 98, 0.3);
}

/* 轮播图过渡 */
.carousel-fade-enter-active,
.carousel-fade-leave-active {
  transition: opacity 0.8s ease;
}

.carousel-fade-enter-from,
.carousel-fade-leave-to {
  opacity: 0;
}

/* 响应式 */
@media (max-width: 1024px) {
  .content-section {
    padding: 40px 40px;
  }
  
  .slide-content {
    padding: 40px;
  }
}

@media (max-width: 768px) {
  .carousel-container {
    height: 60vh;
    min-height: 400px;
  }
  
  .slide-content {
    padding: 24px;
  }
  
  .slide-title {
    font-size: 28px;
  }
  
  .content-section {
    padding: 24px 16px;
  }
  
  .section-header {
    flex-direction: column;
    align-items: stretch;
  }
  
  .tab-nav {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  
  .tab-btn {
    flex-shrink: 0;
    padding: 10px 16px;
  }
  
  .view-all-btn {
    justify-content: center;
  }
  
  .novel-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 16px;
  }
  
  .carousel-nav {
    width: 40px;
    height: 40px;
  }
  
  .carousel-nav.prev { left: 16px; }
  .carousel-nav.next { right: 16px; }
  
  .carousel-indicators {
    right: 24px;
  }
}
</style>
