/**
 * API模块统一导出文件
 * 
 * 该文件负责统一导出所有API模块，包括：
 * - 基础请求工具
 * - 用户认证API
 * - 小说相关API
 * - 评论、书签、阅读等API
 * - 管理后台API
 * - 作者中心API
 * - 社区讨论API
 */
import request, { apiClient } from './api.js'

import user, { authApi, userApi, userActionApi } from './modules/user.js'
import novel, { novelApi, chapterApi, categoryApi } from './modules/novel.js'
import comment, { commentApi } from './modules/comment.js'
import bookmark, { bookmarkApi } from './modules/bookmark.js'
import admin, { adminApi } from './modules/admin.js'
import reading, { readingApi } from './modules/reading.js'
import recommendation, { recommendationApi } from './modules/recommendation.js'
import search, { searchApi } from './modules/search.js'
import author, { authorApi } from './modules/author.js'
import discussion, { discussionApi } from './modules/discussion.js'

export {
  request,
  apiClient,
  
  authApi,
  userApi,
  userActionApi,
  
  novelApi,
  chapterApi,
  categoryApi,
  
  commentApi,
  
  bookmarkApi,
  
  adminApi,
  
  readingApi,
  
  recommendationApi,
  
  searchApi,
  
  authorApi,

  discussionApi
}

export default {
  request,
  user,
  novel,
  comment,
  bookmark,
  admin,
  reading,
  recommendation,
  search,
  author,
  discussion
}
