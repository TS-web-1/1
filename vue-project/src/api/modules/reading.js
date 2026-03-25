// 阅读相关API
import apiClient from '../api.js'

export const readingApi = {
  // 获取章节内容
  getChapterContent(chapterId) {
    return apiClient.get(`/reading/chapter/${chapterId}`)
  },

  // 获取小说所有章节
  getNovelChapters(novelId) {
    return apiClient.get(`/reading/chapters/${novelId}`)
  },

  // 获取章节导航（上一章/下一章）
  getChapterNavigation(novelId, currentChapterId) {
    return apiClient.get(
      `/reading/chapter/navigation/${novelId}?currentChapterId=${currentChapterId}`
    )
  },

  // 保存阅读进度
  saveReadingProgress(novelId, chapterId, position, progressPercent, readTime) {
    return apiClient.post(
      `/reading/progress/${novelId}?chapterId=${chapterId}&position=${position}&progressPercent=${progressPercent}&readTime=${readTime}`
    )
  },

  // 获取阅读进度
  getReadingProgress(novelId) {
    return apiClient.get(`/reading/progress/${novelId}`)
  },

  // 获取阅读历史
  getReadingHistory() {
    return apiClient.get('/reading/history')
  },

  // 添加书签
  addBookmark(novelId, chapterId, title, position, contentPreview) {
    return apiClient.post('/reading/bookmark', null, {
      params: { novelId, chapterId, title, position, contentPreview }
    })
  },

  // 获取用户所有书签
  getBookmarks() {
    return apiClient.get('/reading/bookmarks')
  },

  // 获取特定小说的书签
  getNovelBookmarks(novelId) {
    return apiClient.get(`/reading/bookmarks/${novelId}`)
  },

  // 删除书签
  deleteBookmark(bookmarkId) {
    return apiClient.delete(`/reading/bookmark/${bookmarkId}`)
  },

  // 检查是否存在书签
  checkBookmark(novelId, chapterId) {
    return apiClient.get('/reading/bookmark/check', {
      params: { novelId, chapterId }
    })
  }
}

export default readingApi
