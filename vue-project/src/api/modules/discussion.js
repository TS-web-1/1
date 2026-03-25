// 社区讨论相关 API
import apiClient from '../api.js'

export const discussionApi = {
  // 获取全部话题
  getTopics() {
    return apiClient.get('/discussions')
  },

  // 获取分页话题
  getTopicsPaginated(page = 0, size = 10) {
    return apiClient.get('/discussions/paginated', {
      params: { page, size }
    })
  },

  // 获取话题详情
  getTopicById(id) {
    return apiClient.get(`/discussions/${id}`)
  },

  // 发起话题
  createTopic(data) {
    return apiClient.post('/discussions', data)
  },

  // 删除话题
  deleteTopic(id) {
    return apiClient.delete(`/discussions/${id}`)
  },

  // 创建书单
  createBooklist(data) {
    return apiClient.post('/booklists', data)
  },

  // 获取用户书单
  getUserBooklists(userId) {
    return apiClient.get(`/booklists/user/${userId}`)
  },

  // 获取公开书单
  getPublicBooklists() {
    return apiClient.get('/booklists/public')
  },

  // 获取分页公开书单
  getPublicBooklistsPaginated(page = 0, size = 10) {
    return apiClient.get('/booklists/public/paginated', {
      params: { page, size }
    })
  },

  // 删除书单
  deleteBooklist(id) {
    return apiClient.delete(`/booklists/${id}`)
  },

  // 获取用户话题
  getUserTopics(userId) {
    return apiClient.get(`/discussions/user/${userId}`)
  },

  // 获取书单详情
  getBooklistById(id) {
    return apiClient.get(`/booklists/${id}`)
  }
}

export default discussionApi
