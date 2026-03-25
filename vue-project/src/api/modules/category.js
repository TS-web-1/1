import request from '../api.js'

// 获取所有分类
export const categoryApi = {
  getAllCategories: () => {
    return request.get('/categories')
  }
}
