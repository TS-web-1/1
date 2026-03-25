<script setup lang="ts">
/**
 * AuthorCenter_Optimized.vue - 作者中心页面组件（优化版）
 * 
 * 该组件实现了作者中心功能，包括：
 * - 作品管理：查看、编辑、删除作品
 * - 草稿管理：管理未发布的章节草稿
 * - 评论管理：查看作品收到的评论
 * - 数据统计：展示作品数据概览和趋势图表
 * - 作品编辑：创建和编辑作品信息
 */
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { authorApi } from '../api/modules/author.js'
import { useUserStore } from '../stores/user.js'
import * as echarts from 'echarts'

interface Novel {
  id: number
  title: string
  description: string
  coverImage: string
  category: string
  status: string
  authorId: number
  author: string
  views?: number
  bookmarks?: number
  totalChapters?: number
  totalWords?: number
  totalLikes?: number
  totalComments?: number
  reviewStatus?: string
  createdAt: string
  updatedAt: string
}

interface Draft {
  id: number
  novelId: number
  title: string
  content: string
  chapterNumber: number
  wordCount: number
  novelTitle: string
  updatedAt: string
}

interface Comment {
  id: number
  novelId: number
  novelTitle: string
  chapterTitle: string
  userName: string
  content: string
  createdAt: string
  isReply: boolean
}

interface Stats {
  totalNovels: number
  totalChapters: number
  totalWords: number
  totalViews: number
  totalLikes: number
  totalComments: number
  totalBookmarks: number
}

interface TrendData {
  date: string
  views: number
}

interface EditForm {
  title: string
  description: string
  coverImage: string
  category: string
  status: string
}

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref<'works' | 'drafts' | 'comments'>('works')
const novels = ref<Novel[]>([])
const drafts = ref<Draft[]>([])
const comments = ref<Comment[]>([])
const stats = ref<Stats | null>(null)
const trendData = ref<TrendData[]>([])
const isLoading = ref(false)
const chartInstance = ref<any>(null)

// 编辑功能相关
const isEditing = ref(false)
const currentNovel = ref<Novel | null>(null)
const editForm = ref<EditForm>({
  title: '',
  description: '',
  coverImage: '',
  category: '玄幻',
  status: 'ONGOING'
})

// 常量数据 - 使用 computed 或常量
const categories = Object.freeze(['玄幻', '仙侠', '都市', '历史', '科幻', '悬疑', '言情', '其他'])
const statuses = Object.freeze([
  { value: 'ONGOING', label: '连载中' },
  { value: 'COMPLETED', label: '已完结' }
])

// 计算属性 - 优化性能
const totalNovelCount = computed(() => novels.value.length)
const hasNovels = computed(() => novels.value.length > 0)
const hasDrafts = computed(() => drafts.value.length > 0)
const hasComments = computed(() => comments.value.length > 0)

// 数据加载函数 - 使用 async/await 和错误处理
const loadNovels = async () => {
  isLoading.value = true
  try {
    const response = await authorApi.getAuthorNovels()
    novels.value = Array.isArray(response) ? response : (response.data || [])
  } catch (error) {
    console.error('加载作品列表失败:', error)
    ElMessage.error('加载作品列表失败')
    novels.value = []
  } finally {
    isLoading.value = false
  }
}

const loadDrafts = async () => {
  try {
    const response = await authorApi.getDrafts()
    drafts.value = Array.isArray(response) ? response : (response.data || [])
  } catch (error) {
    console.error('加载草稿箱失败:', error)
    drafts.value = []
  }
}

const loadComments = async () => {
  try {
    const all: Comment[] = []
    for (const novel of novels.value) {
      try {
        const res = await authorApi.getNovelComments(novel.id, 0, 50)
        const raw = res?.content || res?.data || res
        if (Array.isArray(raw)) {
          const items = raw.map((c: any) => ({
            id: c.id,
            novelId: novel.id,
            novelTitle: novel.title,
            chapterTitle: c.chapterId ? `章节 ${c.chapterId}` : '全书',
            userName: c.userId ? `用户${c.userId}` : '读者',
            content: c.content,
            createdAt: c.createdAt,
            isReply: !!c.parentId
          }))
          all.push(...items)
        }
      } catch (_) {
        // 忽略单个小说的评论加载错误
      }
    }
    // 按时间倒序排序
    comments.value = all.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  } catch (error) {
    console.error('加载评论失败:', error)
    comments.value = []
  }
}

const loadStats = async () => {
  try {
    const response = await authorApi.getAuthorStats()
    console.log('统计数据 API 响应:', response)
    // response 是 ApiResponse 对象，需要取 response.data
    stats.value = response && response.data ? response.data : (response || {})
    console.log('处理后的统计数据:', stats.value)
  } catch (error) {
    console.error('加载统计数据失败:', error)
    // 提供默认值
    stats.value = {
      totalNovels: 0,
      totalChapters: 0,
      totalWords: 0,
      totalViews: 0,
      totalLikes: 0,
      totalComments: 0,
      totalBookmarks: 0
    }
  }
}

const loadReadingTrend = async () => {
  try {
    const response = await authorApi.getReadingTrend()
    console.log('阅读趋势 API 原始响应:', response)
    
    // 处理响应数据
    let data = []
    if (Array.isArray(response)) {
      data = response
    } else if (response && typeof response === 'object') {
      data = response.data || []
    }
    
    console.log('处理后的趋势数据:', data)
    trendData.value = data
    
    // 等待 DOM 更新后初始化图表
    await nextTick()
    console.log('DOM 更新完成，准备初始化图表')
    initChart()
  } catch (error) {
    console.error('加载阅读趋势失败:', error)
    trendData.value = []
  }
}

// 并行加载所有数据
const loadAllData = async () => {
  await Promise.all([
    loadNovels(),
    loadDrafts(),
    loadStats(),
    loadReadingTrend()
  ])
  // 评论加载依赖于 novels，所以单独处理
  await loadComments()
}

// 操作函数
const createNovel = () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再创建作品')
    router.push('/login')
    return
  }
  router.push('/author/novel/create')
}

const editNovel = (novel: Novel) => {
  currentNovel.value = novel
  editForm.value = {
    title: novel.title || '',
    description: novel.description || '',
    coverImage: novel.coverImage || '',
    category: novel.category || '玄幻',
    status: novel.status || 'ONGOING'
  }
  isEditing.value = true
}

const saveEdit = async () => {
  if (!editForm.value.title.trim()) {
    ElMessage.warning('请输入作品名称')
    return
  }
  
  if (!editForm.value.description.trim()) {
    ElMessage.warning('请输入作品简介')
    return
  }
  
  try {
    await authorApi.updateNovel(currentNovel.value!.id, editForm.value)
    ElMessage.success('作品更新成功')
    isEditing.value = false
    currentNovel.value = null
    await loadNovels()
  } catch (error) {
    console.error('更新作品失败:', error)
    ElMessage.error('更新失败，请稍后重试')
  }
}

const cancelEdit = () => {
  isEditing.value = false
  currentNovel.value = null
  editForm.value = {
    title: '',
    description: '',
    coverImage: '',
    category: '玄幻',
    status: 'ONGOING'
  }
}

const manageChapters = (novelId: number) => {
  router.push(`/author/novel/${novelId}/chapters`)
}

const deleteNovel = async (novelId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这部作品吗？此操作不可恢复！', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await authorApi.deleteNovel(novelId)
    ElMessage.success('作品已删除')
    await loadNovels()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除作品失败:', error)
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

const continueDraft = (draft: Draft) => {
  router.push({
    path: `/author/novel/${draft.novelId}/chapters`,
    query: {
      draftId: draft.id,
      draftTitle: draft.title,
      draftContent: draft.content
    }
  })
}

const deleteDraft = async (draftId: number) => {
  try {
    await authorApi.deleteDraft(draftId)
    ElMessage.success('草稿已删除')
    await loadDrafts()
  } catch (error) {
    console.error('删除草稿失败:', error)
    ElMessage.error('删除失败')
  }
}

const deleteComment = async (commentId: number) => {
  try {
    await authorApi.deleteComment(commentId)
    ElMessage.success('评论已删除')
    await loadComments()
  } catch (error) {
    console.error('删除评论失败:', error)
    ElMessage.error('删除失败')
  }
}

// 工具函数
const formatNumber = (num?: number | null): string => {
  if (!num) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万'
  }
  return num.toString()
}

const formatTime = (date: string): string => {
  const now = new Date()
  const diff = now.getTime() - new Date(date).getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days > 7) return new Date(date).toLocaleDateString()
  if (days > 0) return `${days}天前`
  
  const hours = Math.floor(diff / (1000 * 60 * 60))
  if (hours > 0) return `${hours}小时前`
  
  const minutes = Math.floor(diff / (1000 * 60))
  return `${minutes}分钟前`
}

const getReviewStatusText = (status?: string): string => {
  const statusMap: Record<string, string> = {
    'PENDING': '审核中',
    'APPROVED': '已通过',
    'REJECTED': '已拒绝'
  }
  return statusMap[status || ''] || '未设置'
}

const getReviewStatusClass = (status?: string): string => {
  const classMap: Record<string, string> = {
    'PENDING': 'pending',
    'APPROVED': 'approved',
    'REJECTED': 'rejected'
  }
  return classMap[status || ''] || 'unset'
}

// 图表相关
const initChart = () => {
  const chartDom = document.getElementById('reading-trend-chart')
  if (!chartDom) {
    console.log('图表 DOM 未找到')
    return
  }
  
  // 检查是否有数据
  if (!trendData.value || trendData.value.length === 0) {
    console.log('没有趋势数据')
    return
  }
  
  if (chartInstance.value) {
    chartInstance.value.dispose()
  }
  
  chartInstance.value = echarts.init(chartDom)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(22, 27, 34, 0.9)',
      borderColor: 'rgba(201, 169, 98, 0.3)',
      textStyle: {
        color: '#f0f6fc'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trendData.value.map(item => item.date.substring(5)), // 只显示 MM-DD
      axisLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.1)'
        }
      },
      axisLabel: {
        color: 'rgba(255, 255, 255, 0.6)'
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.1)'
        }
      },
      axisLabel: {
        color: 'rgba(255, 255, 255, 0.6)',
        formatter: (value: number) => {
          if (value >= 10000) {
            return (value / 10000).toFixed(1) + '万'
          }
          return value.toString()
        }
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.05)'
        }
      }
    },
    series: [
      {
        name: '阅读量',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: {
          color: '#c9a962'
        },
        lineStyle: {
          width: 3,
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 1,
            y2: 0,
            colorStops: [
              { offset: 0, color: '#c9a962' },
              { offset: 1, color: '#d4b978' }
            ]
          }
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(201, 169, 98, 0.3)' },
              { offset: 1, color: 'rgba(201, 169, 98, 0.05)' }
            ]
          }
        },
        data: trendData.value.map(item => item.views)
      }
    ]
  }
  
  chartInstance.value.setOption(option)
  
  // 响应窗口大小变化
  window.addEventListener('resize', () => {
    chartInstance.value?.resize()
  })
}

// 生命周期钩子
onMounted(() => {
  loadAllData()
})

// 组件名称（用于 KeepAlive 等）
defineOptions({
  name: 'AuthorCenter'
})
</script>

<template>
  <div class="author-center">
    <!-- 顶部统计卡片 -->
    <div class="stats-overview" v-if="stats">
      <div class="stat-card">
        <div class="stat-value">{{ formatNumber(stats.totalViews) }}</div>
        <div class="stat-label">总阅读量</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ formatNumber(stats.totalBookmarks) }}</div>
        <div class="stat-label">总收藏数</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ formatNumber(stats.totalNovels) }}</div>
        <div class="stat-label">作品数量</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ formatNumber(stats.totalLikes) }}</div>
        <div class="stat-label">总点赞数</div>
      </div>
    </div>
    
    <!-- 阅读趋势图 -->
    <div class="trend-section" v-if="trendData.length > 0">
      <div class="trend-header">
        <h3 class="trend-title">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
            <polyline points="17 6 23 6 23 12"></polyline>
          </svg>
          阅读趋势
        </h3>
        <span class="trend-subtitle">最近 7 天数据</span>
      </div>
      <div id="reading-trend-chart" class="trend-chart"></div>
    </div>

    <!-- 标签页切换 -->
    <div class="tabs-container">
      <div class="tabs">
        <button
          :class="['tab', { active: activeTab === 'works' }]"
          @click="activeTab = 'works'"
        >
          作品管理
        </button>
        <button
          :class="['tab', { active: activeTab === 'drafts' }]"
          @click="activeTab = 'drafts'"
        >
          草稿箱
          <span v-if="drafts.length > 0" class="badge">{{ drafts.length }}</span>
        </button>
        <button
          :class="['tab', { active: activeTab === 'comments' }]"
          @click="activeTab = 'comments'"
        >
          评论管理
          <span v-if="comments.length > 0" class="badge">{{ comments.length }}</span>
        </button>
      </div>
      <button class="create-btn" @click="createNovel">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 5v14M5 12h14"/>
        </svg>
        创建新作品
      </button>
    </div>

    <!-- 作品管理 -->
    <div v-show="activeTab === 'works'" class="tab-content">
      <div v-if="isLoading" class="loading-state">加载中...</div>
      <div v-else-if="!hasNovels" class="empty-state">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/>
        </svg>
        <p>还没有作品，开始创作吧！</p>
        <button class="primary-btn" @click="createNovel">创建第一部作品</button>
      </div>
      <div v-else class="novel-list">
        <div v-for="novel in novels" :key="novel.id" class="novel-card">
          <img :src="novel.coverImage ? `/api/novels/${novel.id}/cover` : 'https://via.placeholder.com/200x280'" :alt="novel.title" class="novel-cover" />
          <div class="novel-info">
            <h3 class="novel-title">{{ novel.title }}</h3>
            <div class="novel-meta">
              <span class="category-tag">{{ novel.category }}</span>
              <span :class="['status-tag', novel.status]">
                {{ novel.status === 'ONGOING' ? '连载中' : '已完结' }}
              </span>
              <span :class="['review-status', getReviewStatusClass(novel.reviewStatus)]">
                {{ getReviewStatusText(novel.reviewStatus) }}
              </span>
            </div>
            <p class="novel-desc">{{ novel.description }}</p>
            <div class="novel-stats">
              <span>📖 {{ formatNumber(novel.views || 0) }}</span>
              <span>⭐ {{ formatNumber(novel.bookmarks || 0) }}</span>
              <span>📝 {{ novel.totalChapters || 0 }} 章</span>
              <span>✍️ {{ formatNumber(novel.totalWords || 0) }} 字</span>
            </div>
            <div class="novel-actions">
              <button class="action-btn" @click="manageChapters(novel.id)">管理章节</button>
              <button class="action-btn" @click="editNovel(novel)">编辑信息</button>
              <button class="action-btn danger" @click="deleteNovel(novel.id)">删除</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 草稿箱 -->
    <div v-show="activeTab === 'drafts'" class="tab-content">
      <div v-if="!hasDrafts" class="empty-state">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
        </svg>
        <p>草稿箱空空如也</p>
      </div>
      <div v-else class="draft-list">
        <div v-for="draft in drafts" :key="draft.id" class="draft-card">
          <div class="draft-info">
            <h4 class="draft-title">{{ draft.title }}</h4>
            <p class="draft-novel">《{{ draft.novelTitle }}》</p>
            <div class="draft-meta">
              <span>{{ draft.wordCount }} 字</span>
              <span>更新于 {{ formatTime(draft.updatedAt) }}</span>
            </div>
          </div>
          <div class="draft-actions">
            <button class="action-btn primary" @click="continueDraft(draft)">继续编辑</button>
            <button class="action-btn danger" @click="deleteDraft(draft.id)">删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 评论管理 -->
    <div v-show="activeTab === 'comments'" class="tab-content">
      <div v-if="!hasComments" class="empty-state">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
        </svg>
        <p>还没有评论</p>
      </div>
      <div v-else class="comment-list">
        <div v-for="comment in comments" :key="comment.id" class="comment-card">
          <div class="comment-header">
            <span class="comment-user">{{ comment.userName }}</span>
            <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
          </div>
          <div class="comment-context">
            在《{{ comment.novelTitle }}》的{{ comment.chapterTitle }}中评论：
          </div>
          <p class="comment-content">{{ comment.content }}</p>
          <div class="comment-actions">
            <button class="action-btn" @click="deleteComment(comment.id)">删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <div v-if="isEditing" class="edit-modal-overlay" @click.self="cancelEdit">
      <div class="edit-modal">
        <h3>编辑作品信息</h3>
        <div class="edit-form">
          <div class="form-group">
            <label>作品名称</label>
            <input v-model="editForm.title" type="text" placeholder="请输入作品名称" />
          </div>
          <div class="form-group">
            <label>作品简介</label>
            <textarea v-model="editForm.description" rows="4" placeholder="请输入作品简介" />
          </div>
          <div class="form-group">
            <label>封面图片 URL</label>
            <input v-model="editForm.coverImage" type="text" placeholder="请输入封面图片地址" />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>分类</label>
              <select v-model="editForm.category">
                <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
              </select>
            </div>
            <div class="form-group">
              <label>状态</label>
              <select v-model="editForm.status">
                <option v-for="status in statuses" :key="status.value" :value="status.value">
                  {{ status.label }}
                </option>
              </select>
            </div>
          </div>
        </div>
        <div class="modal-actions">
          <button class="action-btn" @click="cancelEdit">取消</button>
          <button class="action-btn primary" @click="saveEdit">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.author-center {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

/* 统计卡片 */
.stats-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 32px;
}

.stat-card {
  background: linear-gradient(135deg, rgba(201, 169, 98, 0.1) 0%, rgba(201, 169, 98, 0.05) 100%);
  border: 1px solid rgba(201, 169, 98, 0.2);
  border-radius: 12px;
  padding: 24px;
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #c9a962;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

/* 阅读趋势图 */
.trend-section {
  background: linear-gradient(135deg, rgba(22, 27, 34, 0.9) 0%, rgba(13, 17, 23, 0.95) 100%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 32px;
}

.trend-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.trend-title {
  font-size: 18px;
  font-weight: 600;
  color: #f0f6fc;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.trend-subtitle {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  padding: 4px 12px;
  background: rgba(201, 169, 98, 0.1);
  border-radius: 12px;
  color: #c9a962;
}

.trend-chart {
  width: 100%;
  height: 300px;
}

/* 标签页 */
.tabs-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.tabs {
  display: flex;
  gap: 8px;
}

.tab {
  padding: 10px 20px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.tab:hover {
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.05);
}

.tab.active {
  color: #c9a962;
  background: rgba(201, 169, 98, 0.1);
  border-color: rgba(201, 169, 98, 0.3);
}

.badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background: #c9a962;
  color: #0d1117;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 10px;
  font-weight: 600;
}

.create-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #c9a962 0%, #d4b978 100%);
  color: #0d1117;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(201, 169, 98, 0.4);
}

/* 空状态和加载状态 */
.empty-state, .loading-state {
  text-align: center;
  padding: 64px 24px;
  color: rgba(255, 255, 255, 0.5);
}

.empty-state svg {
  margin-bottom: 16px;
  opacity: 0.5;
}

.primary-btn {
  margin-top: 16px;
  padding: 12px 32px;
  background: linear-gradient(135deg, #c9a962 0%, #d4b978 100%);
  color: #0d1117;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.primary-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(201, 169, 98, 0.4);
}

/* 作品列表 */
.novel-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.novel-card {
  display: flex;
  gap: 20px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 20px;
  transition: all 0.3s ease;
}

.novel-card:hover {
  border-color: rgba(201, 169, 98, 0.3);
  background: rgba(201, 169, 98, 0.05);
}

.novel-cover {
  width: 120px;
  height: 160px;
  object-fit: cover;
  border-radius: 8px;
}

.novel-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.novel-title {
  font-size: 20px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
}

.novel-meta {
  display: flex;
  gap: 8px;
}

.category-tag, .status-tag, .review-status {
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
}

.category-tag {
  background: rgba(201, 169, 98, 0.2);
  color: #c9a962;
}

.status-tag.ongoing {
  background: rgba(76, 175, 80, 0.2);
  color: #4caf50;
}

.status-tag.completed {
  background: rgba(33, 150, 243, 0.2);
  color: #2196f3;
}

.review-status.pending {
  background: rgba(255, 193, 7, 0.2);
  color: #ffc107;
}

.review-status.approved {
  background: rgba(76, 175, 80, 0.2);
  color: #4caf50;
}

.review-status.rejected {
  background: rgba(244, 67, 54, 0.2);
  color: #f44336;
}

.novel-desc {
  color: rgba(255, 255, 255, 0.6);
  margin: 0;
  line-height: 1.6;
}

.novel-stats {
  display: flex;
  gap: 16px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
}

.novel-actions {
  display: flex;
  gap: 8px;
  margin-top: auto;
}

.action-btn {
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-btn:hover {
  border-color: rgba(201, 169, 98, 0.5);
  color: #c9a962;
}

.action-btn.primary {
  background: linear-gradient(135deg, #c9a962 0%, #d4b978 100%);
  color: #0d1117;
  border: none;
}

.action-btn.danger:hover {
  border-color: rgba(244, 67, 54, 0.5);
  color: #f44336;
}

/* 草稿和评论列表 */
.draft-list, .comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.draft-card, .comment-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 20px;
}

.draft-actions, .comment-actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}

/* 编辑弹窗 */
.edit-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.edit-modal {
  background: #161b22;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 32px;
  width: 100%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
}

.edit-modal h3 {
  margin: 0 0 24px;
  font-size: 20px;
  color: #ffffff;
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  font-weight: 500;
}

.form-group input,
.form-group textarea,
.form-group select {
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: #ffffff;
  font-size: 14px;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  outline: none;
  border-color: rgba(201, 169, 98, 0.5);
  background: rgba(201, 169, 98, 0.05);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .novel-card {
    flex-direction: column;
  }
  
  .novel-cover {
    width: 100%;
    height: 240px;
  }
  
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
