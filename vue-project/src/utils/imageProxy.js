/**
 * 图片代理工具函数
 * 解决外部图片 CORS 问题 (Unsplash, Placeholder 等)
 */

// 图片代理 URL 前缀
const IMAGE_PROXY = '/api/proxy/image?url='

/**
 * 将外部图片 URL 转换为代理 URL
 * @param {string} url - 原始图片 URL
 * @returns {string} - 代理后的 URL
 */
export const proxyImageUrl = url => {
  if (!url) return ''

  // 如果是本地图片，直接返回
  if (!url.startsWith('http')) {
    return url
  }

  // 如果是 Unsplash 或 Placeholder 图片，使用代理
  if (url.includes('unsplash') || url.includes('placeholder')) {
    return `${IMAGE_PROXY}${encodeURIComponent(url)}`
  }

  // 其他外部图片也使用代理
  return `${IMAGE_PROXY}${encodeURIComponent(url)}`
}

/**
 * 批量转换图片 URL
 * @param {Array} items - 包含图片 URL 的对象数组
 * @param {string} imageKey - 图片字段名 (默认 'image')
 * @returns {Array} - 转换后的数组
 */
export const proxyImageUrls = (items, imageKey = 'image') => {
  return items.map(item => ({
    ...item,
    [imageKey]: proxyImageUrl(item[imageKey])
  }))
}
