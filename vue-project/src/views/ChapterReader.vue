<script setup>
/**
 * ChapterReader.vue - 章节阅读页面组件
 *
 * 该组件实现了小说章节阅读功能，包括：
 * - 章节内容展示
 * - 阅读进度自动保存
 * - 字体大小调整
 * - 夜间模式切换
 * - 上下章节导航
 * - 评论功能
 */
import { ref, onMounted, computed, watch, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { readingApi } from '../api'
import CommentBox from '../components/CommentBox.vue'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const novelId = computed(() => route.params.id)
const chapterId = computed(() => route.params.chapterId)

const chapter = ref({
  id: chapterId.value,
  title: '加载中...',
  content: ''
})

const fontsize = ref(16)
const darkMode = ref(false)
const prevChapter = ref(null)
const nextChapter = ref(null)
const loading = ref(true)

const contentRef = ref(null)
let readStartTime = 0
let progressSaveTimer = null

/**
 * 保存阅读进度
 * 定时保存用户的阅读位置和时长
 */
const saveProgress = () => {
  const token = localStorage.getItem('token')
  if (!token || !novelId.value || !chapterId.value) {
    console.log(
      '不保存阅读进度：token=',
      !!token,
      'novelId=',
      novelId.value,
      'chapterId=',
      chapterId.value
    )
    return
  }
  const position = contentRef.value ? Math.min(contentRef.value.scrollTop || 0, 999999) : 0
  const totalHeight = contentRef.value
    ? contentRef.value.scrollHeight - contentRef.value.clientHeight
    : 1
  const progressPercent = totalHeight > 0 ? Math.min(100, (position / totalHeight) * 100) : 0
  const readTime = Math.max(0, Math.floor((Date.now() - readStartTime) / 1000))

  console.log('=== 保存阅读进度 ===')
  console.log('novelId:', Number(novelId.value))
  console.log('chapterId:', Number(chapterId.value))
  console.log('position:', position)
  console.log('progressPercent:', progressPercent)
  console.log('readTime:', readTime)

  readingApi
    .saveReadingProgress(
      Number(novelId.value),
      Number(chapterId.value),
      position,
      Math.round(progressPercent * 100) / 100,
      readTime
    )
    .then(res => {
      console.log('=== 阅读进度保存成功 ===')
      console.log('响应:', res)
    })
    .catch(err => {
      console.error('=== 阅读进度保存失败 ===')
      console.error('错误:', err)
    })
}

/**
 * 加载阅读进度并恢复位置
 */
const loadProgressAndRestore = async () => {
  const token = localStorage.getItem('token')
  if (!token || !novelId.value) return
  try {
    const res = await readingApi.getReadingProgress(novelId.value)
    if (res.code === 200 && res.data && res.data.chapterId == chapterId.value && contentRef.value) {
      const pos = res.data.position || 0
      if (pos > 0) contentRef.value.scrollTop = pos
    }
  } catch (_) {}
}

/**
 * 增大字体
 */
const increaseFontSize = () => {
  if (fontsize.value < 24) {
    fontsize.value++
  }
}

/**
 * 减小字体
 */
const decreaseFontSize = () => {
  if (fontsize.value > 12) {
    fontsize.value--
  }
}

/**
 * 切换夜间模式
 */
const toggleDarkMode = () => {
  darkMode.value = !darkMode.value
}

/**
 * 加载章节内容
 */
const loadChapterContent = async () => {
  loading.value = true
  if (progressSaveTimer) {
    clearInterval(progressSaveTimer)
    progressSaveTimer = null
  }
  try {
    console.log('加载章节内容，chapterId:', chapterId.value)
    const response = await readingApi.getChapterContent(chapterId.value)
    console.log('章节内容响应:', response)
    if (response.code === 200) {
      chapter.value = response.data
      readStartTime = Date.now()
      await loadProgressAndRestore()
      saveProgress()
      progressSaveTimer = setInterval(saveProgress, 15000)
      nextTick(() => {
        window.scrollTo(0, 0)
        document.documentElement.scrollTop = 0
        document.body.scrollTop = 0
      })
    } else {
      console.error('章节内容响应错误:', response)
      ElMessage.error(`加载章节内容失败：${response.message || '未知错误'}`)
    }
  } catch (error) {
    console.error('加载章节内容失败:', error)
    ElMessage.error(`加载章节内容失败：${error.message}`)
  } finally {
    loading.value = false
  }
}

const chapterList = ref([])

/**
 * 加载章节导航信息
 * 获取上一章和下一章的信息
 * @param {number} nid - 小说ID
 * @param {number} cid - 章节ID
 */
const loadChapterNavigation = async (nid, cid) => {
  const novelIdToUse = nid || novelId.value
  const chapterIdToUse = cid || chapterId.value
  const currentId = Number(chapterIdToUse) || chapterIdToUse
  const currentNum = chapter.value?.chapterNumber ?? chapter.value?.chapter_number

  if (!novelIdToUse || !chapterIdToUse) return

  prevChapter.value = null
  nextChapter.value = null

  try {
    const res = await readingApi.getNovelChapters(novelIdToUse)
    if (res.code === 200 && Array.isArray(res.data)) {
      const list = res.data
      const seen = new Set()
      const deduped = list.filter(c => {
        const num = c.chapterNumber ?? c.chapter_number ?? 0
        const key = `${num}`
        if (seen.has(key)) return false
        seen.add(key)
        return true
      })
      chapterList.value = deduped
      let idx = deduped.findIndex(c => (c.id ?? c.chapterId) == currentId)
      if (idx < 0 && currentNum != null) {
        idx = deduped.findIndex(c => (c.chapterNumber ?? c.chapter_number) == currentNum)
      }
      if (idx >= 0) {
        prevChapter.value = idx > 0 ? deduped[idx - 1] : null
        nextChapter.value = idx < deduped.length - 1 ? deduped[idx + 1] : null
        return
      }
    }
    const response = await readingApi.getChapterNavigation(novelIdToUse, chapterIdToUse)
    if (response.code === 200 && response.data) {
      prevChapter.value = response.data.previous ?? null
      nextChapter.value = response.data.next ?? null
    }
  } catch (error) {
    console.error('加载章节导航失败:', error)
  }
}

/**
 * 跳转到上一章
 */
const handlePrevChapter = () => {
  const prev = prevChapter.value
  if (prev) {
    saveProgress()
    const id = prev.id ?? prev.chapterId
    if (id != null) {
      router.push(`/novel/${novelId.value}/chapter/${id}`)
    }
  } else {
    ElMessage.warning('已经是第一章了')
  }
}

/**
 * 跳转到下一章
 */
const handleNextChapter = () => {
  const next = nextChapter.value
  if (next) {
    saveProgress()
    const id = next.id ?? next.chapterId
    if (id != null) {
      router.push(`/novel/${novelId.value}/chapter/${id}`)
    }
  } else {
    ElMessage.warning('已经是最后一章了')
  }
}

/**
 * 返回小说详情页
 */
const goBack = () => {
  router.push(`/novel/${novelId.value}`)
}

onBeforeUnmount(() => {
  saveProgress()
  if (progressSaveTimer) clearInterval(progressSaveTimer)
})

watch(
  [() => route.params.id, () => route.params.chapterId],
  async ([newNovelId, newChapterId]) => {
    if (!newNovelId || !newChapterId) return
    await loadChapterContent()
    await nextTick()
    loadChapterNavigation(newNovelId, newChapterId)
  },
  { immediate: true }
)
</script>

<template>
  <div class="chapter-reader" :class="{ 'light-mode': !darkMode }">
    <div class="reader-header">
      <div class="header-left">
        <button class="back-btn" @click="goBack">
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="19" y1="12" x2="5" y2="12"></line>
            <polyline points="12 19 5 12 12 5"></polyline>
          </svg>
          返回详情
        </button>
        <h1 class="chapter-title">{{ chapter.title }}</h1>
      </div>
      <div class="reader-controls">
        <button class="control-btn" title="减小字号" @click="decreaseFontSize">
          <svg
            class="icon-sm"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="8" y1="11" x2="14" y2="11"></line>
          </svg>
        </button>
        <span class="font-size-display">{{ fontsize }}px</span>
        <button class="control-btn" title="增大字号" @click="increaseFontSize">
          <svg
            class="icon-sm"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="11" y1="8" x2="11" y2="14"></line>
            <line x1="8" y1="11" x2="14" y2="11"></line>
          </svg>
        </button>
        <div class="control-divider"></div>
        <button
          class="control-btn mode-toggle"
          :title="darkMode ? '切换日间模式' : '切换夜间模式'"
          @click="toggleDarkMode"
        >
          <svg
            v-if="darkMode"
            class="icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <circle cx="12" cy="12" r="5"></circle>
            <line x1="12" y1="1" x2="12" y2="3"></line>
            <line x1="12" y1="21" x2="12" y2="23"></line>
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line>
            <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line>
            <line x1="1" y1="12" x2="3" y2="12"></line>
            <line x1="21" y1="12" x2="23" y2="12"></line>
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line>
            <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>
          </svg>
          <svg
            v-else
            class="icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>
          </svg>
          {{ darkMode ? '日间' : '夜间' }}
        </button>
      </div>
    </div>

    <div ref="contentRef" class="chapter-content" :style="{ fontSize: fontsize + 'px' }">
      <p v-for="(paragraph, index) in (chapter.content || '').split('\n\n')" :key="index">
        {{ paragraph }}
      </p>
    </div>

    <div class="chapter-navigation">
      <button class="nav-btn prev-btn" :disabled="!prevChapter" @click="handlePrevChapter">
        <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="15 18 9 12 15 6"></polyline>
        </svg>
        上一章
      </button>
      <button class="nav-btn catalog-btn" @click="goBack">
        <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="8" y1="6" x2="21" y2="6"></line>
          <line x1="8" y1="12" x2="21" y2="12"></line>
          <line x1="8" y1="18" x2="21" y2="18"></line>
          <line x1="3" y1="6" x2="3.01" y2="6"></line>
          <line x1="3" y1="12" x2="3.01" y2="12"></line>
          <line x1="3" y1="18" x2="3.01" y2="18"></line>
        </svg>
        目录
      </button>
      <button class="nav-btn next-btn" :disabled="!nextChapter" @click="handleNextChapter">
        下一章
        <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="9 18 15 12 9 6"></polyline>
        </svg>
      </button>
    </div>

    <div class="comments-section">
      <CommentBox
        :chapter-id="parseInt(chapterId)"
        :novel-id="parseInt(novelId)"
        comment-type="CHAPTER"
      />
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.chapter-reader {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 24px;
  background: #0d1117;
  min-height: calc(100vh - 80px);
  transition: all 0.3s ease;
  width: 100%;
  position: relative;
}

.chapter-reader.light-mode {
  background: #faf8f5;
}

.chapter-reader::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  width: 4px;
  height: 100vh;
  background: linear-gradient(180deg, #c9a962 0%, #d4b978 50%, #c9a962 100%);
  z-index: 100;
}

.reader-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding: 20px 24px;
  background: rgba(22, 27, 34, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  position: sticky;
  top: 88px;
  z-index: 50;
}

.chapter-reader.light-mode .reader-header {
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(0, 0, 0, 0.08);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  min-width: 0;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.back-btn:hover {
  background: rgba(201, 169, 98, 0.1);
  border-color: rgba(201, 169, 98, 0.3);
  color: #c9a962;
}

.chapter-reader.light-mode .back-btn {
  background: rgba(0, 0, 0, 0.03);
  color: rgba(0, 0, 0, 0.7);
  border-color: rgba(0, 0, 0, 0.1);
}

.chapter-reader.light-mode .back-btn:hover {
  background: rgba(201, 169, 98, 0.1);
  border-color: rgba(201, 169, 98, 0.3);
  color: #a88b4a;
}

.icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.icon-sm {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.chapter-title {
  font-size: clamp(18px, 3vw, 26px);
  font-weight: 600;
  color: #ffffff;
  font-family: 'Noto Serif SC', 'Playfair Display', serif;
  letter-spacing: -0.3px;
  line-height: 1.3;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chapter-reader.light-mode .chapter-title {
  color: #1a1a1a;
}

.reader-controls {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-shrink: 0;
}

.control-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 14px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.3s ease;
}

.control-btn:hover {
  background: rgba(201, 169, 98, 0.1);
  border-color: rgba(201, 169, 98, 0.3);
  color: #c9a962;
}

.chapter-reader.light-mode .control-btn {
  background: rgba(0, 0, 0, 0.03);
  color: rgba(0, 0, 0, 0.6);
  border-color: rgba(0, 0, 0, 0.1);
}

.chapter-reader.light-mode .control-btn:hover {
  background: rgba(201, 169, 98, 0.1);
  border-color: rgba(201, 169, 98, 0.3);
  color: #a88b4a;
}

.font-size-display {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  min-width: 36px;
  text-align: center;
}

.chapter-reader.light-mode .font-size-display {
  color: rgba(0, 0, 0, 0.4);
}

.control-divider {
  width: 1px;
  height: 24px;
  background: rgba(255, 255, 255, 0.1);
  margin: 0 4px;
}

.chapter-reader.light-mode .control-divider {
  background: rgba(0, 0, 0, 0.1);
}

.mode-toggle {
  min-width: 70px;
}

.chapter-content {
  line-height: 2;
  margin-bottom: 32px;
  padding: 40px 48px;
  background: rgba(22, 27, 34, 0.6);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.04);
  font-family: 'Noto Serif SC', 'Playfair Display', serif;
  position: relative;
  overflow: hidden;
  max-width: 100%;
  max-height: 70vh;
  overflow-y: auto;
}

.chapter-reader.light-mode .chapter-content {
  background: rgba(255, 255, 255, 0.8);
  border-color: rgba(0, 0, 0, 0.05);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
}

.chapter-content::-webkit-scrollbar {
  width: 8px;
}

.chapter-content::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.02);
  border-radius: 4px;
}

.chapter-content::-webkit-scrollbar-thumb {
  background: rgba(201, 169, 98, 0.2);
  border-radius: 4px;
}

.chapter-content::-webkit-scrollbar-thumb:hover {
  background: rgba(201, 169, 98, 0.3);
}

.chapter-reader.light-mode .chapter-content::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.02);
}

.chapter-reader.light-mode .chapter-content::-webkit-scrollbar-thumb {
  background: rgba(201, 169, 98, 0.3);
}

.chapter-content p {
  margin-bottom: 20px;
  text-indent: 2em;
  color: rgba(255, 255, 255, 0.85);
  font-size: inherit;
  letter-spacing: 0.5px;
  position: relative;
  line-height: 2;
}

.chapter-reader.light-mode .chapter-content p {
  color: rgba(0, 0, 0, 0.8);
}

.chapter-content p:first-child::first-letter {
  font-size: 2em;
  font-weight: 600;
  color: #c9a962;
}

.chapter-navigation {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 32px;
  margin-bottom: 24px;
  padding: 20px;
  background: rgba(22, 27, 34, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.04);
  gap: 12px;
}

.chapter-reader.light-mode .chapter-navigation {
  background: rgba(255, 255, 255, 0.8);
  border-color: rgba(0, 0, 0, 0.05);
}

.nav-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 14px 24px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  flex: 1;
  max-width: 180px;
}

.nav-btn:hover:not(:disabled) {
  background: rgba(201, 169, 98, 0.1);
  border-color: rgba(201, 169, 98, 0.3);
  color: #c9a962;
  transform: translateY(-2px);
}

.nav-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.chapter-reader.light-mode .nav-btn {
  background: rgba(0, 0, 0, 0.03);
  color: rgba(0, 0, 0, 0.7);
  border-color: rgba(0, 0, 0, 0.1);
}

.chapter-reader.light-mode .nav-btn:hover:not(:disabled) {
  background: rgba(201, 169, 98, 0.1);
  border-color: rgba(201, 169, 98, 0.3);
  color: #a88b4a;
}

.catalog-btn {
  background: linear-gradient(135deg, #c9a962 0%, #d4b978 100%);
  color: #0d1117;
  border: none;
  max-width: 120px;
}

.catalog-btn:hover {
  background: linear-gradient(135deg, #d4b978 0%, #c9a962 100%);
  color: #0d1117;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(201, 169, 98, 0.3);
}

.comments-section {
  margin-top: 32px;
  padding: 32px;
  background: rgba(22, 27, 34, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  position: relative;
  overflow: hidden;
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

.chapter-reader.light-mode .comments-section {
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(0, 0, 0, 0.08);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

@media (max-width: 1024px) {
  .chapter-reader {
    padding: 24px 16px;
  }

  .chapter-content {
    padding: 32px 24px;
  }
}

@media (max-width: 768px) {
  .chapter-reader {
    padding: 16px 12px;
  }

  .reader-header {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
    padding: 16px;
    position: relative;
    top: 0;
  }

  .header-left {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .chapter-title {
    font-size: 18px;
    white-space: normal;
  }

  .reader-controls {
    justify-content: flex-end;
    flex-wrap: wrap;
  }

  .control-btn {
    padding: 6px 10px;
    font-size: 12px;
  }

  .mode-toggle {
    min-width: auto;
  }

  .chapter-content {
    padding: 24px 16px;
    max-height: 60vh;
  }

  .chapter-navigation {
    flex-direction: column;
    gap: 10px;
  }

  .nav-btn {
    width: 100%;
    max-width: none;
  }
}

@media print {
  .chapter-reader {
    background: white;
    color: black;
  }

  .reader-header,
  .reader-controls,
  .chapter-navigation,
  .comments-section {
    display: none;
  }

  .chapter-content {
    box-shadow: none;
    border: none;
    padding: 0;
    max-height: none;
    background: white;
  }

  .chapter-content p {
    color: black;
  }
}
</style>
