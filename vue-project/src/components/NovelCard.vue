<script setup>
/**
 * 小说卡片组件
 * 用于展示小说信息，包括封面、标题、作者、简介等
 *
 * 优化点：
 * 1. 使用 computed 缓存封面 URL 计算
 * 2. 优化事件处理
 * 3. 添加性能优化注释
 */

import { computed } from 'vue'
import { useRouter } from 'vue-router'

// ==================== Props ====================
/**
 * 组件属性定义
 */
const props = defineProps({
  /**
   * 小说对象
   */
  novel: {
    type: Object,
    required: true,
    // 使用 validator 进行 props 验证
    validator(value) {
      return value && typeof value.id !== 'undefined'
    }
  }
})

// ==================== Router ====================
const router = useRouter()

// ==================== Computed ====================
/**
 * 封面 URL - 使用 computed 缓存计算结果
 * 避免每次渲染都重新计算
 */
const coverUrl = computed(() => {
  const { novel } = props

  // 如果有 novelId，优先使用 API 获取封面
  if (novel.id) {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
    return `${baseUrl}/novels/${novel.id}/cover`
  }

  // 否则使用传入的 cover 或默认图片
  return novel.cover || 'https://picsum.photos/seed/default/300/400'
})

/**
 * 格式化数字 - 使用 computed 缓存
 */
const formatNumber = num => {
  if (!num) return '0'
  if (num >= 10000) {
    return `${(num / 10000).toFixed(1)}w`
  }
  if (num >= 1000) {
    return `${(num / 1000).toFixed(1)}k`
  }
  return num.toString()
}

// ==================== Methods ====================
/**
 * 处理卡片点击事件，跳转到小说详情页
 */
const handleClick = () => {
  router.push(`/novel/${props.novel.id}`)
}

/**
 * 处理键盘事件
 * @param {KeyboardEvent} e
 */
const handleKeyDown = e => {
  if (e.key === 'Enter' || e.key === ' ') {
    e.preventDefault()
    handleClick()
  }
}

/**
 * 处理图片加载失败事件
 * @param {Event} e 事件对象
 */
const handleImageError = e => {
  // 当图片加载失败时，使用默认图片
  e.target.src = 'https://picsum.photos/seed/default/300/400'
}
</script>

<template>
  <article
    class="novel-card"
    tabindex="0"
    role="button"
    :aria-label="`查看小说详情：${novel.title}`"
    @click="handleClick"
    @keydown="handleKeyDown"
  >
    <div class="card-cover">
      <!-- 使用 computed 的 coverUrl -->
      <img
        :src="coverUrl"
        :alt="novel.title"
        loading="lazy"
        decoding="async"
        @error="handleImageError"
      />
      <div class="cover-overlay"></div>
      <div class="card-badges">
        <span :class="['badge', novel.status === 'COMPLETED' ? 'completed' : 'ongoing']">
          {{ novel.status === 'COMPLETED' ? '完结' : '连载' }}
        </span>
      </div>
    </div>

    <div class="card-content">
      <h3 class="card-title">{{ novel.title }}</h3>
      <p class="card-author">{{ novel.author }}</p>
      <p class="card-description">{{ novel.description || '暂无简介' }}</p>
      <div class="card-footer">
        <span class="card-category">{{ novel.category }}</span>
        <div class="card-stats">
          <span class="stat" :title="`浏览：${novel.views || 0}`">
            <svg
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
            {{ formatNumber(novel.views) }}
          </span>
          <span class="stat" :title="`收藏：${novel.bookmarks || 0}`">
            <svg
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M19 21l-7-5-7 5V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
            </svg>
            {{ formatNumber(novel.bookmarks) }}
          </span>
          <span class="stat" :title="`点赞：${novel.totalLikes || 0}`">
            <svg
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
              />
            </svg>
            {{ formatNumber(novel.totalLikes) }}
          </span>
        </div>
      </div>
    </div>
  </article>
</template>

<style scoped>
.novel-card {
  display: flex;
  flex-direction: column;
  background: rgba(22, 27, 34, 0.8);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.06);
  position: relative;
  contain: layout style; /* 性能优化：限制布局影响范围 */
}

.novel-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(201, 169, 98, 0.1) 0%, transparent 50%);
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
  z-index: 1;
}

.novel-card:hover {
  transform: translateY(-6px);
  border-color: rgba(201, 169, 98, 0.3);
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(201, 169, 98, 0.1);
}

.novel-card:hover::before {
  opacity: 1;
}

.novel-card:focus-visible {
  outline: 2px solid #c9a962;
  outline-offset: 2px;
}

.card-cover {
  position: relative;
  aspect-ratio: 3 / 4;
  overflow: hidden;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
  will-change: transform; /* 性能优化：提示浏览器优化 transform */
}

.novel-card:hover .card-cover img {
  transform: scale(1.08);
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 0%, transparent 50%, rgba(13, 17, 23, 0.8) 100%);
  pointer-events: none;
}

.card-badges {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
}

.badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.badge.completed {
  background: linear-gradient(135deg, #238636 0%, #2ea043 100%);
  color: white;
}

.badge.ongoing {
  background: linear-gradient(135deg, #c9a962 0%, #d4b978 100%);
  color: #0d1117;
}

.card-content {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.card-title {
  font-family: 'Noto Serif SC', serif;
  font-size: 16px;
  font-weight: 600;
  color: #f0f6fc;
  margin: 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.3s ease;
}

.novel-card:hover .card-title {
  color: #c9a962;
}

.card-author {
  font-size: 13px;
  color: #8b949e;
  margin: 0;
  font-weight: 500;
}

.card-description {
  font-size: 12px;
  color: #6e7681;
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.card-category {
  font-size: 11px;
  color: #c9a962;
  font-weight: 500;
  padding: 4px 8px;
  background: rgba(201, 169, 98, 0.1);
  border-radius: 4px;
}

.card-stats {
  display: flex;
  gap: 12px;
}

.stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #6e7681;
}

.stat svg {
  opacity: 0.6;
}

@media (max-width: 768px) {
  .card-content {
    padding: 12px;
  }

  .card-title {
    font-size: 14px;
  }

  .card-author {
    font-size: 12px;
  }

  .card-description {
    font-size: 11px;
  }
}
</style>
