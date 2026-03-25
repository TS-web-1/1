import request from '../api.js'

// 推荐相关 API
export const recommendationApi = {
  // 获取个性化推荐
  getPersonalizedRecommendations: (limit = 10) => {
    return request.get('/recommendations/personalized', { params: { limit } })
  },

  // 获取热门推荐
  getPopularRecommendations: (limit = 10) => {
    return request.get('/recommendations/popular', { params: { limit } })
  },

  // 获取重磅推荐（按收藏数）
  getBookmarkRecommendations: (limit = 10) => {
    return request.get('/recommendations/bookmarks', { params: { limit } })
  },

  // 获取热门推荐（按阅读历史）
  getReadingHistoryRecommendations: (limit = 10) => {
    return request.get('/recommendations/reading-history', { params: { limit } })
  },

  // 获取完结精品推荐
  getCompletedRecommendations: (limit = 10) => {
    return request.get('/recommendations/completed', { params: { limit } })
  }
}

export default recommendationApi
