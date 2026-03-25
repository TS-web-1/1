import request from '../api.js'

// 小说点赞相关 API
export const novelLikeApi = {
  // 点赞小说
  likeNovel: (novelId) => {
    return request.post(`/novels/${novelId}/like`)
  },
  
  // 取消点赞小说
  unlikeNovel: (novelId) => {
    return request.delete(`/novels/${novelId}/like`)
  },
  
  // 检查是否已点赞
  checkLike: (novelId) => {
    return request.get(`/novels/${novelId}/like/check`)
  }
}

export default novelLikeApi
