// 作者相关API
import apiClient from '../api.js'

export const authorApi = {
  // ==================== 作品管理 ====================
  
  // 发布新作品
  publishNovel(data) {
    return apiClient.post('/author/novels', data)
  },

  // 更新作品
  updateNovel(novelId, data) {
    return apiClient.put(`/author/novels/${novelId}`, data)
  },

  // 删除作品
  deleteNovel(novelId) {
    return apiClient.delete(`/author/novels/${novelId}`)
  },

  // 获取作者的所有作品
  getAuthorNovels() {
    return apiClient.get('/author/novels')
  },

  // 获取作品统计数据
  getNovelStats(novelId) {
    return apiClient.get(`/author/novels/${novelId}/stats`)
  },

  // ==================== 章节管理 ====================

  // 添加章节
  addChapter(novelId, data) {
    return apiClient.post(`/author/novels/${novelId}/chapters`, data)
  },

  // 更新章节
  updateChapter(chapterId, data) {
    return apiClient.put(`/author/chapters/${chapterId}`, data)
  },

  // 删除章节
  deleteChapter(chapterId) {
    return apiClient.delete(`/author/chapters/${chapterId}`)
  },

  // 获取小说章节列表
  getNovelChapters(novelId) {
    return apiClient.get(`/author/novels/${novelId}/chapters`)
  },

  // ==================== 评论管理 ====================

  // 获取作品评论
  getNovelComments(novelId, page = 0, size = 20) {
    return apiClient.get(`/author/novels/${novelId}/comments`, {
      params: { page, size }
    })
  },

  // 删除评论
  deleteComment(commentId) {
    return apiClient.delete(`/author/comments/${commentId}`)
  },

  // 回复评论
  replyToComment(commentId, content) {
    return apiClient.post(`/author/comments/${commentId}/reply`, null, {
      params: { content }
    })
  },

  // ==================== 数据分析 ====================

  // 获取作品分析数据
  getNovelAnalytics(novelId) {
    return apiClient.get(`/author/analytics/${novelId}`)
  },

  // 获取作者总览数据
  getAuthorStats() {
    return apiClient.get('/author/stats')
  },
  
  // 获取阅读趋势数据
  getReadingTrend() {
    return apiClient.get('/author/reading-trend')
  },

  // 获取草稿箱
  getDrafts() {
    return apiClient.get('/author/drafts')
  },

  // 保存草稿
  saveDraft(data) {
    return apiClient.post('/author/drafts', data)
  },

  // 删除草稿
  deleteDraft(draftId) {
    return apiClient.delete(`/author/drafts/${draftId}`)
  }
}

export default authorApi
