import request from '../api.js'
import { readingApi } from './reading.js'

export const bookmarkApi = {
  getBookmarks() {
    return request.get('/bookmarks')
  },

  getBookmark(novelId) {
    return request.get(`/bookmarks/novel/${novelId}`)
  },

  addBookmark(data) {
    return request.post('/bookmarks', data)
  },

  createBookmark(data) {
    return request.post('/bookmarks', data)
  },

  deleteBookmark(bookmarkId) {
    return request.delete(`/bookmarks/${bookmarkId}`)
  },

  // 获取小说章节列表（复用 readingApi）
  getNovelChapters(novelId) {
    return readingApi.getNovelChapters(novelId)
  },

  // 更新书签
  updateBookmark(bookmarkId, data) {
    return request.put(`/bookmarks/${bookmarkId}`, data)
  }
}

export default {
  bookmark: bookmarkApi
}
