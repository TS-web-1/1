import request from '../api.js'

// 评论相关API
export const commentApi = {
  // 获取小说的所有评论
  getCommentsByNovel(novelId, page = 0, size = 10, sortBy = 'createdAt') {
    return request.get(`/comments/novel/${novelId}`, {
      params: { page, size, sortBy }
    })
  },
  
  // 获取热门评论
  getPopularComments(novelId, page = 0, size = 10) {
    return request.get(`/comments/novel/${novelId}/popular`, {
      params: { page, size }
    })
  },
  
  // 获取章节评论
  getChapterComments(chapterId, page = 0, size = 10) {
    return request.get(`/comments/chapter/${chapterId}`, {
      params: { page, size }
    })
  },
  
  // 获取评论的回复
  getCommentReplies(commentId) {
    return request.get(`/comments/${commentId}/replies`)
  },
  
  // 获取用户的所有评论
  getCommentsByUser(userId) {
    return request.get(`/comments/user/${userId}`)
  },
  
  // 获取评论详情
  getCommentById(commentId) {
    return request.get(`/comments/${commentId}`)
  },
  
  // 创建评论
  createComment(data) {
    const params = new URLSearchParams()
    if (data.novelId) params.append('novelId', data.novelId)
    if (data.chapterId) params.append('chapterId', data.chapterId)
    params.append('content', data.content)
    if (data.commentType) params.append('commentType', data.commentType)
    if (data.parentId) params.append('parentId', data.parentId)
    
    return request.post('/comments?' + params.toString(), {})
  },
  
  // 删除评论
  deleteComment(commentId) {
    return request.delete(`/comments/${commentId}`)
  },
  
  // 点赞评论
  likeComment(commentId) {
    return request.post(`/comments/${commentId}/like`)
  },
  
  // 取消点赞
  unlikeComment(commentId) {
    return request.delete(`/comments/${commentId}/like`)
  },
  
  // 检查是否点赞
  checkLike(commentId) {
    return request.get(`/comments/${commentId}/like/check`)
  },
  
  // 回复评论
  replyComment(parentId, content, novelId, chapterId) {
    const params = new URLSearchParams()
    params.append('parentId', parentId)
    params.append('content', content)
    if (novelId) params.append('novelId', novelId)
    if (chapterId) params.append('chapterId', chapterId)
    params.append('commentType', novelId ? 'NOVEL' : 'CHAPTER')
    
    return request.post('/comments?' + params.toString(), {})
  },
  
  // 获取评论统计
  getCommentStats(novelId) {
    return request.get(`/comments/stats/${novelId}`)
  }
}

// 默认导出
export default {
  comment: commentApi
}
