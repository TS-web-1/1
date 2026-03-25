<script setup>
/**
 * History.vue - 阅读历史页面组件
 * 
 * 该组件实现了用户阅读历史功能，包括：
 * - 展示用户阅读过的小说列表
 * - 显示阅读进度和最后阅读时间
 * - 继续阅读功能
 * - 从历史记录中移除
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { readingApi } from '../api'
import { novelApi } from '../api/modules/novel.js'
import { bookmarkApi } from '../api/modules/bookmark.js'
import { readingApi as readingApiModule } from '../api/modules/reading.js'

const router = useRouter()
const readingHistory = ref([])
const loading = ref(false)

/**
 * 组件挂载时加载历史记录
 */
onMounted(() => {
  loadHistory()
})

/**
 * 格式化最后阅读时间
 * @param {string} dateStr - 日期字符串
 * @returns {string} 格式化后的日期
 */
const formatLastRead = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

/**
 * 加载阅读历史
 */
const loadHistory = async () => {
  loading.value = true
  try {
    // 先尝试获取阅读进度
    let list = []
    try {
      const res = await readingApi.getReadingHistory()
      if (res.code === 200 && Array.isArray(res.data) && res.data.length > 0) {
        list = res.data
      }
    } catch (e) {
      console.log('没有阅读进度数据')
    }
    
    // 如果没有阅读进度，使用书架数据
    if (list.length === 0) {
      try {
        const bookmarkRes = await bookmarkApi.getBookmarks()
        if (bookmarkRes.code === 200 && Array.isArray(bookmarkRes.data)) {
          list = bookmarkRes.data.map(b => ({
            id: b.id,
            novelId: b.novelId,
            chapterId: b.chapterId,
            lastReadAt: b.createdAt
          }))
        }
      } catch (e) {
        console.log('获取书架失败:', e)
      }
    }
    
    const novelCache = {}
    const chapterCache = {}
    
    const loadNovel = async (novelId) => {
      if (novelCache[novelId]) return novelCache[novelId]
      try {
        const r = await novelApi.getNovelById(novelId)
        if (r.code === 200 && r.data) {
          novelCache[novelId] = r.data
          return r.data
        }
      } catch (_) {}
      return null
    }
    
    const loadChapters = async (novelId) => {
      if (chapterCache[novelId]) return chapterCache[novelId]
      try {
        const r = await readingApiModule.getNovelChapters(novelId)
        if (r.code === 200 && r.data) {
          chapterCache[novelId] = r.data
          return r.data
        }
      } catch (_) {}
      return []
    }
    
    const items = await Promise.all(list.map(async (p) => {
      const novel = await loadNovel(p.novelId)
      const chapters = await loadChapters(p.novelId)
      
      let chapterTitle = `章节 ${p.chapterId}`
      if (chapters && chapters.length > 0) {
        const currentChapter = chapters.find(ch => ch.id === p.chapterId)
        if (currentChapter) {
          chapterTitle = currentChapter.title
        } else {
          chapterTitle = chapters[0].title
          p.chapterId = chapters[0].id
        }
      }
      
      return {
        id: p.id,
        novelId: p.novelId,
        chapterId: p.chapterId,
        title: novel?.title || `小说 ${p.novelId}`,
        author: novel?.author || '',
        cover: novel?.coverImage ? `/api/novels/${p.novelId}/cover` : 'https://via.placeholder.com/120x160',
        chapter: chapterTitle,
        lastRead: formatLastRead(p.lastReadAt)
      }
    }))
    readingHistory.value = items
    // 数据加载完成后滚动到顶部
    window.scrollTo(0, 0)
  } catch (error) {
    console.error('加载阅读历史失败:', error)
    readingHistory.value = []
    ElMessage.error('加载阅读历史失败')
  } finally {
    loading.value = false
  }
}

const goToNovel = (novelId) => {
  router.push(`/novel/${novelId}`)
}

const goToChapter = async (novelId, chapterId) => {
  try {
    console.log('继续阅读 - novelId:', novelId, 'chapterId:', chapterId)
    
    // 优先从阅读进度获取最新的章节 ID
    try {
      const progressResponse = await readingApi.getReadingProgress(novelId)
      console.log('阅读进度响应:', progressResponse)
      if (progressResponse.code === 200 && progressResponse.data && progressResponse.data.chapterId) {
        chapterId = progressResponse.data.chapterId
        console.log('使用阅读进度中的 chapterId:', chapterId)
      }
    } catch (e) {
      console.log('获取阅读进度失败:', e)
    }
    
    // 获取章节列表验证 chapterId
    const chaptersResponse = await readingApiModule.getNovelChapters(novelId)
    console.log('章节列表响应:', chaptersResponse)
    
    if (chaptersResponse.code === 200 && chaptersResponse.data && chaptersResponse.data.length > 0) {
      const chapterExists = chaptersResponse.data.some(ch => ch.id === chapterId)
      console.log('chapterId 是否存在:', chapterExists)
      
      if (!chapterExists || !chapterId) {
        chapterId = chaptersResponse.data[0].id
        console.log('使用第一章 ID:', chapterId)
      }
    }
    
    console.log('最终跳转 chapterId:', chapterId)
    router.push(`/novel/${novelId}/chapter/${chapterId}`)
  } catch (error) {
    console.error('跳转失败:', error)
    // 出错时直接跳转
    router.push(`/novel/${novelId}/chapter/${chapterId}`)
  }
}

const clearHistory = async () => {
  readingHistory.value = []
  ElMessage.success('阅读历史已清空')
}

const removeHistoryItem = (id) => {
  readingHistory.value = readingHistory.value.filter(item => item.id !== id)
  ElMessage.success('已移除')
}
</script>

<template>
  <div class="history">
    <div class="history-header">
      <h1>阅读历史</h1>
      <div class="header-actions">
        <p>共 {{ readingHistory.length }} 本</p>
        <button 
          v-if="readingHistory.length > 0" 
          class="btn-clear" 
          @click="clearHistory"
        >
          清空历史
        </button>
      </div>
    </div>
    
    <div class="history-content">
      <div v-if="loading" class="loading-state">
        <p>加载中...</p>
      </div>
      <div v-else-if="readingHistory.length > 0" class="history-list">
        <div 
          v-for="item in readingHistory" 
          :key="item.id" 
          class="history-item"
        >
          <div class="novel-info" @click="goToNovel(item.novelId)">
            <img 
              :src="item.cover" 
              :alt="item.title"
              class="novel-cover"
            />
            <div class="novel-details">
              <h3>{{ item.title }}</h3>
              <p class="author">作者：{{ item.author }}</p>
              <p class="chapter">阅读到：{{ item.chapter }}</p>
              <p class="time">最后阅读：{{ item.lastRead }}</p>
            </div>
          </div>
          <div class="history-actions">
            <button class="btn-continue" @click="goToChapter(item.novelId, item.chapterId)">
              继续阅读
            </button>
            <button class="btn-remove" @click="removeHistoryItem(item.id)">
              移除
            </button>
          </div>
        </div>
      </div>
      <div v-else class="empty-state">
        <p>暂无阅读历史，快去阅读喜欢的小说吧！</p>
        <button class="btn-browse" @click="router.push('/')">去浏览小说</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.history {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--spacing-xl);
  min-height: calc(100vh - 140px);
  width: 100%;
  flex: 1;
  position: relative;
}

.history::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  width: 4px;
  height: 100vh;
  background: linear-gradient(180deg, var(--color-primary) 0%, var(--color-accent) 100%);
  z-index: 100;
}

.history-header {
  text-align: center;
  margin-bottom: var(--spacing-2xl);
  padding: var(--spacing-2xl);
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--color-border);
  position: relative;
  overflow: hidden;
}

.history-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--color-primary) 0%, var(--color-accent) 100%);
}

.history-header h1 {
  font-size: 36px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-md);
  font-family: var(--font-serif);
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-accent) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-actions {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--spacing-lg);
}

.header-actions p {
  font-size: 16px;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.btn-clear {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: var(--color-bg-tertiary);
  color: var(--color-text-primary);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all var(--transition-base);
}

.btn-clear:hover {
  background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
  color: white;
  border-color: #ff4d4f;
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
  transform: translateY(-2px);
}

.history-content {
  min-height: 400px;
}

.loading-state,
.empty-state {
  text-align: center;
  padding: var(--spacing-2xl);
  color: var(--color-text-tertiary);
  font-size: 18px;
  font-weight: 500;
}

.empty-state p {
  font-size: 18px;
  margin-bottom: var(--spacing-xl);
  color: var(--color-text-secondary);
}

.btn-browse {
  padding: var(--spacing-md) var(--spacing-xl);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: white;
  border: none;
  border-radius: var(--radius-lg);
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  transition: all var(--transition-base);
  box-shadow: 0 4px 12px rgba(26, 54, 93, 0.3);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.btn-browse:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(26, 54, 93, 0.4);
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-lg);
}

.history-item {
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  padding: var(--spacing-xl);
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all var(--transition-base);
  border: 1px solid var(--color-border);
  position: relative;
  overflow: hidden;
}

.history-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--color-primary) 0%, var(--color-accent) 100%);
  transform: scaleX(0);
  transition: transform var(--transition-base);
}

.history-item:hover::before {
  transform: scaleX(1);
}

.history-item:hover {
  box-shadow: var(--shadow-xl);
  transform: translateY(-4px);
  border-color: var(--color-primary);
}

.novel-info {
  display: flex;
  flex: 1;
  cursor: pointer;
}

.novel-cover {
  width: 120px;
  height: 160px;
  object-fit: cover;
  border-radius: var(--radius-lg);
  margin-right: var(--spacing-xl);
  box-shadow: var(--shadow-md);
  border: 2px solid white;
  transition: transform var(--transition-base);
}

.novel-info:hover .novel-cover {
  transform: scale(1.02);
}

.novel-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.novel-details h3 {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-sm);
  font-family: var(--font-serif);
  letter-spacing: -0.5px;
}

.novel-details p {
  font-size: 15px;
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-xs);
  font-weight: 500;
}

.novel-details .chapter {
  color: var(--color-primary);
  font-weight: 600;
}

.novel-details .time {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.history-actions {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);
}

.btn-continue,
.btn-remove {
  padding: var(--spacing-md) var(--spacing-xl);
  border: none;
  border-radius: var(--radius-lg);
  cursor: pointer;
  font-size: 15px;
  font-weight: 600;
  transition: all var(--transition-base);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.btn-continue {
  background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-light) 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(214, 158, 46, 0.3);
}

.btn-continue:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(214, 158, 46, 0.4);
}

.btn-remove {
  background: var(--color-bg-tertiary);
  color: var(--color-text-primary);
  border: 2px solid var(--color-border);
}

.btn-remove:hover {
  background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
  color: white;
  border-color: #ff4d4f;
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
  transform: translateY(-2px);
}

@media (max-width: 768px) {
  .history {
    padding: var(--spacing-md);
  }

  .history-header {
    padding: var(--spacing-xl);
  }

  .history-header h1 {
    font-size: 24px;
  }

  .history-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .novel-cover {
    width: 100px;
    height: 140px;
    margin-right: var(--spacing-md);
  }

  .novel-details h3 {
    font-size: 18px;
  }

  .history-actions {
    flex-direction: row;
    width: 100%;
    margin-top: var(--spacing-md);
  }

  .btn-continue,
  .btn-remove {
    flex: 1;
    padding: var(--spacing-sm) var(--spacing-md);
    font-size: 14px;
  }
}
</style>
