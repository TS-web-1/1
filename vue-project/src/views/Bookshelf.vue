<script setup>
/**
 * Bookshelf.vue - 用户书架页面组件
 *
 * 该组件实现了用户书架功能，包括：
 * - 展示用户收藏的小说列表
 * - 显示阅读进度
 * - 继续阅读功能
 * - 从书架移除小说
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import NovelCard from '../components/NovelCard.vue'
import { bookmarkApi } from '../api/modules/bookmark.js'
import { readingApi } from '../api/modules/reading.js'
import { ElMessage } from 'element-plus'

const router = useRouter()
const bookmarks = ref([])
const loading = ref(false)

onMounted(() => {
  loadBookmarks()
})

/**
 * 加载书架数据
 * 获取用户收藏的小说列表及阅读进度
 */
const loadBookmarks = async () => {
  loading.value = true
  try {
    const response = await bookmarkApi.getBookmarks()
    if (response.code === 200) {
      const bookmarksData = response.data || []

      for (const bookmark of bookmarksData) {
        try {
          const novelId = bookmark.novel?.id || bookmark.novelId

          let progressChapterId = null
          try {
            const progressResponse = await readingApi.getReadingProgress(novelId)
            if (
              progressResponse.code === 200 &&
              progressResponse.data &&
              progressResponse.data.chapterId
            ) {
              progressChapterId = progressResponse.data.chapterId

              if (progressChapterId !== bookmark.chapterId) {
                try {
                  await bookmarkApi.updateBookmark(bookmark.id, {
                    chapterId: progressChapterId,
                    title: bookmark.novel?.title || bookmark.title
                  })
                  bookmark.chapterId = progressChapterId
                } catch (updateError) {
                  console.log('书架加载 - 更新书签失败:', updateError)
                }
              } else {
                bookmark.chapterId = progressChapterId
              }
            }
          } catch (e) {
            console.log('书架加载 - 没有阅读进度:', e)
          }

          const chaptersResponse = await bookmarkApi.getNovelChapters(novelId)
          if (chaptersResponse.code === 200 && chaptersResponse.data) {
            const currentChapter = chaptersResponse.data.find(ch => ch.id === bookmark.chapterId)
            if (currentChapter) {
              bookmark.currentChapterTitle = currentChapter.title
            } else if (chaptersResponse.data.length > 0) {
              bookmark.chapterId = chaptersResponse.data[0].id
              bookmark.currentChapterTitle = chaptersResponse.data[0].title
            }
          }
        } catch (e) {
          console.log('获取章节列表失败:', e)
        }
      }

      bookmarks.value = bookmarksData
      window.scrollTo(0, 0)
    } else {
      ElMessage.error(response.message || '获取书架失败')
    }
  } catch (error) {
    console.error('获取书架失败:', error)
    ElMessage.error('获取书架失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const goToNovel = novelId => {
  router.push(`/novel/${novelId}`)
}

const continueReading = async bookmark => {
  try {
    let chapterId = bookmark.chapterId

    try {
      const novelId = bookmark.novel?.id || bookmark.novelId
      const progressResponse = await readingApi.getReadingProgress(novelId)
      if (
        progressResponse.code === 200 &&
        progressResponse.data &&
        progressResponse.data.chapterId
      ) {
        chapterId = progressResponse.data.chapterId
      }
    } catch (e) {
      console.log('获取阅读进度失败，使用书签中的 chapterId:', e)
    }

    const novelId = bookmark.novel?.id || bookmark.novelId
    const response = await bookmarkApi.getNovelChapters(novelId)

    if (response.code === 200 && response.data && response.data.length > 0) {
      const chapterExists = response.data.some(ch => ch.id === chapterId)

      if (!chapterExists || !chapterId) {
        chapterId = response.data[0].id
      }
    } else {
      chapterId = 1
    }

    router.push(`/novel/${novelId}/chapter/${chapterId}`)
  } catch (error) {
    console.error('获取章节失败:', error)
    ElMessage.error('获取章节失败')
  }
}

const removeBookmark = async bookmarkId => {
  try {
    const response = await bookmarkApi.deleteBookmark(bookmarkId)
    if (response.code === 200) {
      ElMessage.success('已从书架移除')
      loadBookmarks()
    } else {
      ElMessage.error(response.message || '移除失败')
    }
  } catch (error) {
    console.error('移除失败:', error)
    ElMessage.error('移除失败，请稍后重试')
  }
}
</script>

<template>
  <div class="bookshelf-page">
    <div class="bookshelf-header">
      <div class="header-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
          <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
        </svg>
      </div>
      <div class="header-content">
        <h1>我的书架</h1>
        <p>共 {{ bookmarks.length }} 本收藏</p>
      </div>
    </div>

    <div class="bookshelf-content">
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else-if="bookmarks.length > 0" class="novel-grid">
        <div v-for="bookmark in bookmarks" :key="bookmark.id" class="bookmark-card">
          <div class="card-cover" @click="goToNovel(bookmark.novel.id)">
            <img
              :src="
                bookmark.novel.coverImage
                  ? `/api/novels/${bookmark.novel.id}/cover`
                  : 'https://via.placeholder.com/200x280'
              "
              :alt="bookmark.novel.title"
            />
            <div class="cover-overlay">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                <circle cx="12" cy="12" r="3"></circle>
              </svg>
            </div>
          </div>

          <div class="card-content">
            <h3 class="novel-title" @click="goToNovel(bookmark.novel.id)">{{
              bookmark.novel.title
            }}</h3>
            <p class="novel-author">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
              {{ bookmark.novel.author }}
            </p>
            <p class="novel-category">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
              </svg>
              {{ bookmark.novel.category }}
            </p>
            <p class="novel-chapter">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="5 3 19 12 5 21 5 3"></polygon>
              </svg>
              {{ bookmark.currentChapterTitle || '第 ' + (bookmark.chapterId || 1) + ' 章' }}
            </p>
            <p class="novel-time">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <polyline points="12 6 12 12 16 14"></polyline>
              </svg>
              {{ new Date(bookmark.createdAt).toLocaleDateString() }}
            </p>
          </div>

          <div class="card-actions">
            <button class="btn-read" @click="continueReading(bookmark)">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="5 3 19 12 5 21 5 3"></polygon>
              </svg>
              继续阅读
            </button>
            <button class="btn-remove" @click="removeBookmark(bookmark.id)">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"></polyline>
                <path
                  d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"
                ></path>
              </svg>
            </button>
          </div>
        </div>
      </div>

      <div v-else class="empty-state">
        <div class="empty-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
            <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
          </svg>
        </div>
        <h3>书架空空如也</h3>
        <p>快去添加喜欢的小说吧！</p>
        <button class="btn-browse" @click="router.push('/')">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          去浏览小说
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@400;600;700&family=Noto+Serif+SC:wght@400;500;600;700&display=swap');

.bookshelf-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 24px;
  min-height: calc(100vh - 80px);
}

.bookshelf-header {
  display: flex;
  align-items: center;
  gap: 20px;
  background: rgba(22, 27, 34, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  padding: 32px 40px;
  margin-bottom: 32px;
  position: relative;
  overflow: hidden;
}

.bookshelf-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #c9a962 0%, #d4b978 50%, #c9a962 100%);
}

.header-icon {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(201, 169, 98, 0.15);
  border-radius: 16px;
  flex-shrink: 0;
}

.header-icon svg {
  width: 32px;
  height: 32px;
  color: #c9a962;
}

.header-content h1 {
  font-size: 32px;
  font-weight: 700;
  color: #ffffff;
  margin: 0 0 4px 0;
  font-family: 'Noto Serif SC', 'Playfair Display', serif;
}

.header-content p {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

.bookshelf-content {
  min-height: 400px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  background: rgba(22, 27, 34, 0.6);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.04);
}

.spinner {
  width: 48px;
  height: 48px;
  border: 3px solid rgba(255, 255, 255, 0.1);
  border-top-color: #c9a962;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-state p {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0;
}

.novel-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.bookmark-card {
  background: rgba(22, 27, 34, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.04);
  overflow: hidden;
  transition: all 0.3s ease;
}

.bookmark-card:hover {
  transform: translateY(-4px);
  border-color: rgba(201, 169, 98, 0.2);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.3);
}

.card-cover {
  position: relative;
  height: 200px;
  cursor: pointer;
  overflow: hidden;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.bookmark-card:hover .card-cover img {
  transform: scale(1.05);
}

.cover-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.cover-overlay svg {
  width: 48px;
  height: 48px;
  color: #ffffff;
}

.card-cover:hover .cover-overlay {
  opacity: 1;
}

.card-content {
  padding: 20px;
}

.novel-title {
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  margin: 0 0 12px 0;
  cursor: pointer;
  transition: color 0.3s ease;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-family: 'Noto Serif SC', 'Playfair Display', serif;
}

.novel-title:hover {
  color: #c9a962;
}

.novel-author,
.novel-category,
.novel-chapter,
.novel-time {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  margin: 0 0 8px 0;
}

.novel-author svg,
.novel-category svg,
.novel-chapter svg,
.novel-time svg {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  color: rgba(255, 255, 255, 0.4);
}

.novel-chapter {
  color: #c9a962;
  font-weight: 500;
}

.novel-chapter svg {
  color: #c9a962;
}

.novel-time {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  margin-bottom: 0;
}

.card-actions {
  display: flex;
  gap: 8px;
  padding: 0 20px 20px;
}

.btn-read {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #c9a962 0%, #d4b978 100%);
  color: #0d1117;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-read svg {
  width: 16px;
  height: 16px;
}

.btn-read:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(201, 169, 98, 0.3);
}

.btn-remove {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-remove svg {
  width: 18px;
  height: 18px;
}

.btn-remove:hover {
  background: rgba(239, 68, 68, 0.2);
  border-color: rgba(239, 68, 68, 0.4);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  background: rgba(22, 27, 34, 0.6);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.04);
  padding: 48px;
  text-align: center;
}

.empty-icon {
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(201, 169, 98, 0.1);
  border-radius: 20px;
  margin-bottom: 24px;
}

.empty-icon svg {
  width: 40px;
  height: 40px;
  color: rgba(201, 169, 98, 0.5);
}

.empty-state h3 {
  font-size: 24px;
  font-weight: 600;
  color: #ffffff;
  margin: 0 0 8px 0;
  font-family: 'Noto Serif SC', 'Playfair Display', serif;
}

.empty-state p {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.5);
  margin: 0 0 24px 0;
}

.btn-browse {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 28px;
  background: linear-gradient(135deg, #c9a962 0%, #d4b978 100%);
  color: #0d1117;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 8px 24px rgba(201, 169, 98, 0.3);
}

.btn-browse svg {
  width: 18px;
  height: 18px;
}

.btn-browse:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(201, 169, 98, 0.4);
}

@media (max-width: 1024px) {
  .bookshelf-page {
    padding: 24px 16px;
  }

  .bookshelf-header {
    padding: 24px;
  }

  .header-content h1 {
    font-size: 26px;
  }

  .novel-grid {
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
    gap: 16px;
  }
}

@media (max-width: 768px) {
  .bookshelf-header {
    flex-direction: column;
    text-align: center;
    padding: 24px;
  }

  .header-icon {
    margin-bottom: 8px;
  }

  .novel-grid {
    grid-template-columns: 1fr;
  }

  .card-cover {
    height: 180px;
  }
}
</style>
