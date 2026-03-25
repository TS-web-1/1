import request from '../api.js'

// 管理员登录
export const adminLogin = (username, password) => {
  return request.post('/admin/login', { username, password })
}

// 获取统计数据
export const getStats = () => {
  return request.get('/admin/stats')
}

// 获取热门题材趋势
export const getTrendingGenres = () => {
  return request.get('/admin/stats/trending-genres')
}

// 获取待审核小说列表
export const getNovels = (params) => {
  return request.get('/admin/novels/pending', { 
    params: {
      page: params.page || 0,
      size: params.size || 20,
      ...params
    }
  })
}

// 获取所有小说列表（管理用）
export const getAllNovels = (params) => {
  return request.get('/admin/novels', { 
    params: {
      page: params.page || 0,
      size: params.size || 20,
      ...params
    }
  })
}

// 封禁小说
export const banNovel = (novelId) => {
  return request.post(`/admin/novels/${novelId}/ban`)
}

// 解封小说
export const unbanNovel = (novelId) => {
  return request.post(`/admin/novels/${novelId}/unban`)
}

// 删除小说
export const deleteNovel = (novelId) => {
  return request.delete(`/admin/novels/${novelId}`)
}

// 更新小说状态
export const updateNovelStatus = (novelId, status, reviewStatus) => {
  return request.put(`/admin/novels/${novelId}/status`, {
    status,
    reviewStatus
  })
}

// 审核小说
export const reviewNovel = (novelId, status, reviewComment) => {
  return request.post(`/admin/novels/${novelId}/review?status=${encodeURIComponent(status)}&reviewComment=${encodeURIComponent(reviewComment || '')}`)
}

// 获取待审核章节列表
export const getChapters = (params) => {
  return request.get('/admin/chapters/pending', { 
    params: {
      page: params.page || 0,
      size: params.size || 20,
      ...params
    }
  })
}

// 获取所有章节列表（管理用）
export const getAllChapters = (params) => {
  return request.get('/admin/chapters', { 
    params: {
      page: params.page || 0,
      size: params.size || 20,
      ...params
    }
  })
}

// 获取单个章节详情（包含内容）
export const getChapterDetail = (chapterId) => {
  return request.get(`/admin/chapters/${chapterId}`)
}

// 审核章节
export const reviewChapter = (chapterId, status, reviewComment) => {
  return request.post(`/admin/chapters/${chapterId}/review?status=${encodeURIComponent(status)}&reviewComment=${encodeURIComponent(reviewComment || '')}`)
}

// 获取分类列表
export const getCategories = () => {
  return request.get('/admin/categories')
}

export const getCategoriesPaginated = (page = 0, size = 10) => {
  return request.get('/admin/categories/paginated', {
    params: { page, size }
  })
}

// 获取分类统计
export const getCategoryStats = () => {
  return request.get('/admin/categories/stats')
}

// 添加分类
export const addCategory = (data) => {
  return request.post('/admin/categories', data)
}

// 更新分类
export const updateCategory = (categoryId, data) => {
  return request.put(`/admin/categories/${categoryId}`, data)
}

// 删除分类
export const deleteCategory = (categoryId) => {
  return request.delete(`/admin/categories/${categoryId}`)
}

// 获取用户列表
export const getUsers = (page = 0, size = 20, keyword = '') => {
  if (keyword) {
    return request.get('/admin/users/search', { params: { keyword } })
  }
  return request.get('/admin/users', { params: { page, size } })
}

// 搜索用户
export const searchUsers = (keyword) => {
  return request.get('/admin/users/search', { params: { keyword } })
}

// 封禁用户
export const banUser = (userId) => {
  return request.post(`/admin/users/${userId}/ban`)
}

// 解封用户
export const unbanUser = (userId) => {
  return request.post(`/admin/users/${userId}/unban`)
}

// 删除用户
export const deleteUser = (userId) => {
  return request.delete(`/admin/users/${userId}`)
}

// 创建用户
export const createUser = (data) => {
  return request.post('/admin/users', data)
}

// 获取评论列表
export const getComments = (page = 0, size = 20) => {
  return request.get('/admin/comments', { params: { page, size } })
}

// 删除评论
export const deleteComment = (commentId) => {
  return request.delete(`/admin/comments/${commentId}`)
}

// 获取话题列表
export const getTopics = (page = 0, size = 20) => {
  return request.get('/discussions/admin/all', { params: { page, size } })
}

// 删除话题
export const deleteTopic = (topicId) => {
  return request.delete(`/discussions/admin/${topicId}`)
}

// 获取书单列表
export const getBooklists = (page = 0, size = 20) => {
  return request.get('/booklists/admin/all', { params: { page, size } })
}

// 删除书单
export const deleteBooklist = (booklistId) => {
  return request.delete(`/booklists/admin/${booklistId}`)
}



// 默认导出
export const adminApi = {
  adminLogin,
  getStats,
  getTrendingGenres,
  getNovels,
  reviewNovel,
  getCategories,
  addCategory,
  updateCategory,
  deleteCategory,
  getUsers,
  searchUsers,
  banUser,
  unbanUser,
  deleteUser,
  createUser,
  getComments,
  deleteComment,
  getTopics,
  deleteTopic,
  getBooklists,
  deleteBooklist
}

export default adminApi
