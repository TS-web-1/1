// 搜索相关API
import apiClient from '../api.js'

export const searchApi = {
  // 关键词搜索
  search(keyword) {
    return apiClient.get('/search', {
      params: { keyword }
    })
  },

  // 高级搜索
  advancedSearch(params) {
    return apiClient.get('/search/advanced', { params })
  },

  // 按分类搜索
  searchByCategory(category) {
    return apiClient.get(`/search/category/${category}`)
  },

  // 按作者搜索
  searchByAuthor(author) {
    return apiClient.get('/search/author', {
      params: { author }
    })
  },

  // 获取搜索建议
  getSearchSuggestions(keyword, limit = 10) {
    return apiClient.get('/search/suggestions', {
      params: { keyword, limit }
    })
  },

  // 获取热门搜索关键词
  getHotKeywords() {
    return apiClient.get('/search/hot-keywords')
  }
}

export default searchApi
