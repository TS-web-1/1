import request from '../api.js'

// 小说相关API
export const novelApi = {
  // 获取所有小说
  getNovels(params = {}) {
    return request.get('/novels', { params })
  },

  // 获取小说详情
  getNovelById(novelId) {
    return request.get(`/novels/${novelId}`)
  },

  // 创建小说
  createNovel(data) {
    return request.post('/novels', data)
  },

  // 更新小说
  updateNovel(novelId, data) {
    return request.put(`/novels/${novelId}`, data)
  },

  // 删除小说
  deleteNovel(novelId) {
    return request.delete(`/novels/${novelId}`)
  },

  // 搜索小说
  searchNovels(keyword) {
    return request.get('/novels/search', { params: { keyword } })
  },

  // 按分类获取小说
  getNovelsByCategory(category) {
    console.log('调用getNovelsByCategory:', category)
    const encodedCategory = encodeURIComponent(category)
    console.log('编码后的分类名称:', encodedCategory)
    console.log('构建的URL:', `/novels/category/${encodedCategory}`)
    return request.get(`/novels/category/${encodedCategory}`)
  },

  // 按状态获取小说
  getNovelsByStatus(status) {
    return request.get(`/novels/status/${status}`)
  }
}

// 章节相关API
export const chapterApi = {
  // 获取章节列表
  getChapters(novelId) {
    return request.get(`/novels/${novelId}/chapters`)
  },

  // 获取章节详情
  getChapter(novelId, chapterId) {
    return request.get(`/novels/${novelId}/chapters/${chapterId}`)
  },

  // 创建章节
  createChapter(novelId, data) {
    return request.post(`/novels/${novelId}/chapters`, data)
  },

  // 更新章节
  updateChapter(novelId, chapterId, data) {
    return request.put(`/novels/${novelId}/chapters/${chapterId}`, data)
  },

  // 删除章节
  deleteChapter(novelId, chapterId) {
    return request.delete(`/novels/${novelId}/chapters/${chapterId}`)
  }
}

// 分类相关API
export const categoryApi = {
  // 获取所有分类
  getCategories() {
    return request.get('/categories')
  },

  // 获取分类详情
  getCategoryById(categoryId) {
    return request.get(`/categories/${categoryId}`)
  },

  // 按名称获取分类
  getCategoryByName(name) {
    return request.get(`/categories/name/${name}`)
  },

  // 创建分类
  createCategory(name, description) {
    return request.post('/categories', null, { params: { name, description } })
  },

  // 删除分类
  deleteCategory(categoryId) {
    return request.delete(`/categories/${categoryId}`)
  }
}

// 默认导出所有小说相关API
export default {
  novel: novelApi,
  chapter: chapterApi,
  category: categoryApi
}
