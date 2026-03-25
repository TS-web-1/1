<script setup>
// 导入Vue相关的响应式API
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
// 导入小说卡片组件
import NovelCard from '../components/NovelCard.vue'
// 导入API模块
import { novelApi } from '../api/modules/novel.js'
import { recommendationApi } from '../api/modules/recommendation.js'
// 导入Element Plus消息组件
import { ElMessage } from 'element-plus'

/**
 * 首页视图组件
 * 展示小说推荐、轮播图和分类浏览功能
 */

// 页面加载状态
const pageLoading = ref(true)
// 内容加载完成状态
const contentLoaded = ref(false)

// 路由实例
const router = useRouter()
// 小说列表
const novels = ref([])
// 个性化推荐小说列表
const personalizedNovels = ref([])
// 加载状态
const loading = ref(false)
// 当前激活的标签
const activeTab = ref('recommend')

// 分页相关数据
const currentPage = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const pageSize = ref(24)
const jumpPage = ref(1)

/**
 * 处理页码跳转
 */
const handleJumpPage = () => {
  if (jumpPage.value && jumpPage.value >= 1 && jumpPage.value <= totalPages.value) {
    goToPage(jumpPage.value - 1)
    jumpPage.value = 1
  } else {
    ElMessage.warning(`请输入有效的页码（1-${totalPages.value}）`)
  }
}

// 登录状态
const isLoggedIn = ref(localStorage.getItem('token') !== null)

// 轮播图数据
const carouselItems = ref([])
// 图片代理URL
const IMAGE_PROXY = '/api/proxy/image?url='

/**
 * 代理图片URL
 * @param {string} url 原始图片URL
 * @return {string} 代理后的图片URL
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
 * 加载轮播图小说
 */
const loadCarouselNovels = async () => {
  try {
    const response = await recommendationApi.getPopularRecommendations(3)
    console.log('轮播图 API 响应:', response)
    if (response.code === 200 && response.data) {
      carouselItems.value = response.data.map((novel, index) => {
        console.log('处理小说:', novel)
        return ({
          id: novel.id,
          title: novel.title,
          subtitle: `${novel.author} · ${novel.category}小说`,
          description: novel.description || `这是一本精彩的${novel.category}小说。`,
          image: `/api/novels/${novel.id}/cover`,
          novelId: novel.id
        })
      })
      console.log('轮播图数据:', carouselItems.value)
    }
  } catch (error) {
    console.error('加载轮播图失败:', error)
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

// 当前轮播图索引
const currentSlide = ref(0)
// 自动播放定时器
let autoPlayInterval = null

/**
 * 下一张轮播图
 */
const nextSlide = () => {
  currentSlide.value = (currentSlide.value + 1) % carouselItems.value.length
}

/**
 * 上一张轮播图
 */
const prevSlide = () => {
  currentSlide.value = (currentSlide.value - 1 + carouselItems.value.length) % carouselItems.value.length
}

/**
 * 跳转到指定轮播图
 * @param {number} index 轮播图索引
 */
const goToSlide = (index) => {
  currentSlide.value = index
}

/**
 * 跳转到小说详情页
 * @param {number} novelId 小说ID
 */
const goToNovel = (novelId) => {
  router.push(`/novel/${novelId}`)
}

/**
 * 开始自动播放轮播图
 */
const startAutoPlay = () => {
  stopAutoPlay()
  if (carouselItems.value.length > 1) {
    autoPlayInterval = setInterval(() => {
      nextSlide()
    }, 5000)
  }
}

/**
 * 停止自动播放轮播图
 */
const stopAutoPlay = () => {
  if (autoPlayInterval) {
    clearInterval(autoPlayInterval)
    autoPlayInterval = null
  }
}

/**
 * 标签页配置
 */
const tabs = computed(() => {
  if (isLoggedIn.value) {
    return [
      { id: 'personalized', label: '个性化推荐', icon: '✨' },
      { id: 'recommend', label: '重磅推荐', icon: '🔥' },
      { id: 'latest', label: '热门推荐', icon: '📚' },
      { id: 'completed', label: '完结精品', icon: '✅' }
    ]
  }
  return [
    { id: 'recommend', label: '重磅推荐', icon: '🔥' },
    { id: 'latest', label: '热门推荐', icon: '📚' },
    { id: 'completed', label: '完结精品', icon: '✅' }
  ]
})

// 显示限制
const DISPLAY_LIMIT = 9

/**
 * 查看全部小说
 */
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
 * 获取小说列表
 * @param {number} page 页码
 */
const fetchNovels = async (page = 0) => {
  loading.value = true
  try {
    const params = {
      page: page,
      size: pageSize.value,
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
        createdAt: novel.createdAt || novel.created_at || new Date().toISOString()
      }))
      generateRankingList()
    }
  } catch (error) {
    console.error('获取小说列表失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 获取个性化推荐
 */
const fetchPersonalizedRecommendations = async () => {
  try {
    const response = await recommendationApi.getPersonalizedRecommendations(9)
    if (response.code === 200 && response.data) {
      let novelList = Array.isArray(response.data) ? response.data : 
                     (response.data.content || [])
      
      personalizedNovels.value = novelList.slice(0, DISPLAY_LIMIT).map(novel => ({
        id: novel.id,
        title: novel.title,
        author: novel.author,
        description: novel.description,
        cover: novel.coverImage || 'https://via.placeholder.com/150x200',
        category: novel.category,
        status: novel.status
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
    const response = await recommendationApi.getBookmarkRecommendations(9)
    if (response.code === 200 && response.data) {
      let novelList = Array.isArray(response.data) ? response.data : 
                     (response.data.content || [])
      
      novels.value = novelList.slice(0, DISPLAY_LIMIT).map(novel => ({
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
    const response = await recommendationApi.getReadingHistoryRecommendations(9)
    if (response.code === 200 && response.data) {
      let novelList = Array.isArray(response.data) ? response.data : 
                     (response.data.content || [])
      
      novels.value = novelList.slice(0, DISPLAY_LIMIT).map(novel => ({
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
    const response = await recommendationApi.getCompletedRecommendations(9)
    if (response.code === 200 && response.data) {
      let novelList = Array.isArray(response.data) ? response.data : 
                     (response.data.content || [])
      
      novels.value = novelList.slice(0, DISPLAY_LIMIT).map(novel => ({
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
    }
  } catch (error) {
    console.error('获取完结精品失败:', error)
    novels.value = []
  }
}

// 组件挂载时的初始化
onMounted(() => {
  setTimeout(() => {
    pageLoading.value = false
    setTimeout(() => {
      contentLoaded.value = true
    }, 300)
  }, 800)
  
  if (!isLoggedIn.value) {
    activeTab.value = 'recommend'
  }
  
  loadCarouselNovels()
  loadTabData(activeTab.value)
  startAutoPlay()
})

/**
 * 加载标签页数据
 * @param {string} tab 标签ID
 */
const loadTabData = (tab) => {
  currentPage.value = 0
  if (tab === 'personalized') {
    fetchPersonalizedRecommendations()
  } else if (tab === 'recommend') {
    fetchPopularRecommendations()
  } else if (tab === 'latest') {
    fetchLatestUpdates()
  } else if (tab === 'completed') {
    fetchCompletedNovels()
  }
}

// 监听标签变化，加载对应数据
watch(activeTab, (newTab) => {
  loadTabData(newTab)
})

/**
 * 跳转到指定页码
 * @param {number} page 页码
 */
const goToPage = (page) => {
  if (page < 0 || page >= totalPages.value) return
  currentPage.value = page
  fetchNovels(page)
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

/**
 * 跳转到首页
 */
const goToFirstPage = () => {
  goToPage(0)
}

/**
 * 跳转到末页
 */
const goToLastPage = () => {
  goToPage(totalPages.value - 1)
}

/**
 * 跳转到上一页
 */
const goToPrevPage = () => {
  goToPage(currentPage.value - 1)
}

/**
 * 跳转到下一页
 */
const goToNextPage = () => {
  goToPage(currentPage.value + 1)
}

/**
 * 获取显示的页码列表
 */
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

// 组件卸载时清理
onUnmounted(() => {
  stopAutoPlay()
})

/**
 * 生成排行榜列表
 */
const generateRankingList = () => {
  // 这里可以添加生成排行榜的逻辑
}
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
                <img :src="item.image" :alt="item.title" @error="$event.target.src='https://picsum.photos/seed/carousel-default/800/400'" />
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
            v-for="novel in (activeTab === 'personalized' ? personalizedNovels : novels)"
            :key="novel.id"
            :novel="novel"
          />
        </TransitionGroup>
        
        <div v-if="(activeTab === 'personalized' ? personalizedNovels : novels).length === 0 && !loading" class="empty-state">
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

      <nav v-if="!loading && totalPages > 1 && activeTab !== 'personalized'" class="pagination" aria-label="分页导航">
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
              v-for="page in getPageNumbers" 
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

.carousel-fade-enter-active,
.carousel-fade-leave-active {
  transition: opacity 0.8s ease;
}

.carousel-fade-enter-from,
.carousel-fade-leave-to {
  opacity: 0;
}

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
