<template>
  <div class="community-page">
    <div class="community-header">
      <h1>社区</h1>
      <p>发现精彩话题，分享你的书单</p>
    </div>

    <div class="community-tabs">
      <button 
        :class="['tab-btn', { active: activeTab === 'topics' }]" 
        @click="activeTab = 'topics'"
      >
        话题讨论
      </button>
      <button 
        :class="['tab-btn', { active: activeTab === 'booklists' }]" 
        @click="activeTab = 'booklists'"
      >
        书单分享
      </button>
    </div>

    <div class="community-content">
      <div v-if="activeTab === 'topics'" class="topics-section">
        <div class="section-header">
          <h2>热门话题</h2>
          <button class="create-btn" @click="showCreateTopic = true">
            <span class="btn-icon">+</span> 发起话题
          </button>
        </div>

        <div class="topics-list">
          <div 
            v-for="topic in topics" 
            :key="topic.id" 
            class="topic-card"
            @click="viewTopic(topic)"
          >
            <div class="topic-header">
              <img :src="topic.authorAvatar || defaultAvatar" class="author-avatar" />
              <div class="topic-info">
                <span class="author-name">{{ topic.authorName }}</span>
                <span class="topic-time">{{ formatTime(topic.createdAt) }}</span>
              </div>
              <div class="topic-tags">
                <span v-for="tag in topic.tags" :key="tag" class="tag">{{ tag }}</span>
              </div>
            </div>
            <h3 class="topic-title">{{ topic.title }}</h3>
            <p class="topic-content">
              <template v-for="(part, index) in renderContentWithNovelLinks(topic.content)" :key="index">
                <template v-if="part.type === 'text'">{{ part.content }}</template>
                <span 
                  v-else-if="part.type === 'novel'" 
                  class="novel-link"
                  :class="{ 'has-link': part.id }"
                  @click.stop="handleNovelClick(part.id, part.name)"
                >
                  {{ part.fullMatch }}
                </span>
              </template>
            </p>
            <div class="topic-footer">
              <span class="topic-stat">
                <span class="stat-icon">👁</span>
                {{ topic.views }}
              </span>
              <span class="topic-stat">
                <span class="stat-icon">💬</span>
                {{ topic.replyCount }}
              </span>
              <span 
                class="topic-stat like-btn" 
                :class="{ liked: topic.isLiked }"
                @click="(e) => likeTopic(topic, e)"
              >
                <span class="stat-icon heart-icon">{{ topic.isLiked ? '❤️' : '🤍' }}</span>
                {{ topic.likes }}
              </span>
            </div>
          </div>
        </div>
        
        <!-- 话题分页 -->
        <div v-if="topicsTotalPages > 1" class="pagination">
          <button 
            class="page-btn" 
            :disabled="topicsPage === 0"
            @click="changeTopicsPage(topicsPage - 1)"
          >
            上一页
          </button>
          <span class="page-info">
            第 {{ topicsPage + 1 }} 页 / 共 {{ topicsTotalPages }} 页 ({{ topicsTotalElements }} 条)
          </span>
          <button 
            class="page-btn" 
            :disabled="topicsPage === topicsTotalPages - 1"
            @click="changeTopicsPage(topicsPage + 1)"
          >
            下一页
          </button>
        </div>
      </div>

      <div v-else class="booklists-section">
        <div class="section-header">
          <h2>精选书单</h2>
          <button class="create-btn" @click="showCreateBooklist = true">
            <span class="btn-icon">+</span> 创建书单
          </button>
        </div>

        <div class="booklists-grid">
          <div 
            v-for="booklist in booklists" 
            :key="booklist.id" 
            class="booklist-card"
            @click="viewBooklist(booklist)"
          >
            <div class="booklist-covers">
              <img 
                v-for="(cover, index) in booklist.covers.slice(0, 3)" 
                :key="index"
                :src="cover || defaultCover" 
                class="cover-img"
              />
              <div v-if="booklist.covers.length === 0" class="empty-covers">
                <span>暂无封面</span>
              </div>
            </div>
            <div class="booklist-info">
              <h3 class="booklist-title">{{ booklist.title }}</h3>
              <p class="booklist-desc">{{ booklist.description }}</p>
              <div class="booklist-meta">
                <span class="book-count">📚 {{ booklist.novelCount }} 本书</span>
                <span class="booklist-likes" :class="{ liked: booklist.isLiked }" @click="(e) => likeBooklist(booklist, e)">
                  <span class="heart-icon">{{ booklist.isLiked ? '❤️' : '🤍' }}</span> {{ booklist.likes }}
                </span>
              </div>
              <div class="booklist-creator">
                <img :src="booklist.creatorAvatar || defaultAvatar" class="creator-avatar" />
                <span class="creator-name">by {{ booklist.creatorName }}</span>
                <span class="create-time">{{ formatTime(booklist.createdAt) }}</span>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 书单分页 -->
        <div v-if="booklistsTotalPages > 1" class="pagination">
          <button 
            class="page-btn" 
            :disabled="booklistsPage === 0"
            @click="changeBooklistsPage(booklistsPage - 1)"
          >
            上一页
          </button>
          <span class="page-info">
            第 {{ booklistsPage + 1 }} 页 / 共 {{ booklistsTotalPages }} 页 ({{ booklistsTotalElements }} 条)
          </span>
          <button 
            class="page-btn" 
            :disabled="booklistsPage === booklistsTotalPages - 1"
            @click="changeBooklistsPage(booklistsPage + 1)"
          >
            下一页
          </button>
        </div>
      </div>
    </div>

    <!-- 创建话题弹窗 -->
    <div v-if="showCreateTopic" class="modal-overlay" @click.self="showCreateTopic = false">
      <div class="modal-content">
        <div class="modal-header">
          <h2>发起话题</h2>
          <button class="close-btn" @click="showCreateTopic = false">×</button>
        </div>
        <form @submit.prevent="createTopic">
          <div class="form-group">
            <label>话题标题 <span class="required">*</span></label>
            <input v-model="newTopic.title" type="text" placeholder="输入话题标题，让更多人看到" required maxlength="100" />
            <span class="char-count">{{ newTopic.title.length }}/100</span>
          </div>
          <div class="form-group">
            <label>话题内容 <span class="required">*</span></label>
            <textarea v-model="newTopic.content" placeholder="分享你的想法，和大家一起讨论..." required maxlength="2000"></textarea>
            <span class="char-count">{{ newTopic.content.length }}/2000</span>
          </div>
          <div class="form-group">
            <label>标签 <span class="hint">（选填，用空格分隔）</span></label>
            <input v-model="newTopic.tagsInput" type="text" placeholder="例如：玄幻 推荐 讨论" @input="newTopic.tags = $event.target.value.split(' ').filter(t => t)" />
            <div v-if="newTopic.tags.length > 0" class="tags-preview">
              <span v-for="tag in newTopic.tags" :key="tag" class="tag">{{ tag }}</span>
            </div>
          </div>
          <div class="form-actions">
            <button type="button" class="cancel-btn" @click="showCreateTopic = false">取消</button>
            <button type="submit" class="submit-btn">发布话题</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 创建书单弹窗 -->
    <div v-if="showCreateBooklist" class="modal-overlay" @click.self="showCreateBooklist = false">
      <div class="modal-content modal-large">
        <div class="modal-header">
          <h2>创建书单</h2>
          <button class="close-btn" @click="showCreateBooklist = false">×</button>
        </div>
        <form @submit.prevent="createBooklist">
          <div class="form-row">
            <div class="form-group form-group-half">
              <label>书单名称 <span class="required">*</span></label>
              <input v-model="newBooklist.title" type="text" placeholder="给你的书单起个名字" required maxlength="50" />
            </div>
            <div class="form-group form-group-half">
              <label>公开设置</label>
              <div class="radio-group">
                <label class="radio-label">
                  <input type="radio" v-model="newBooklist.isPublic" value="true" />
                  <span>公开</span>
                </label>
                <label class="radio-label">
                  <input type="radio" v-model="newBooklist.isPublic" value="false" />
                  <span>私密</span>
                </label>
              </div>
            </div>
          </div>
          <div class="form-group">
            <label>书单描述</label>
            <textarea v-model="newBooklist.description" placeholder="介绍一下你的书单，让更多人了解..." maxlength="500"></textarea>
            <span class="char-count">{{ newBooklist.description.length }}/500</span>
          </div>
          <div class="form-group">
            <label>选择小说 <span class="hint">（已选择 {{ newBooklist.selectedNovels.length }} 本）</span></label>
            <div class="novel-search">
              <input v-model="novelSearchKeyword" type="text" placeholder="搜索小说名称或作者" @input="handleNovelSearch" />
            </div>
            <div class="novels-selection">
              <div v-if="recommendedNovels.length > 0 && !novelSearchKeyword" class="recommended-novels">
                <h4>推荐小说</h4>
                <div class="recommended-novels-grid">
                  <div v-for="novel in recommendedNovels" :key="novel.id" 
                       class="novel-select-item recommended"
                       :class="{ selected: isNovelSelected(novel) }"
                       @click="toggleNovelSelection(novel)">
                    <img :src="novel.cover" class="novel-select-cover" />
                    <div class="novel-select-info">
                      <span class="novel-select-title">{{ novel.title }}</span>
                      <span class="novel-select-author">{{ novel.author }}</span>
                    </div>
                    <span v-if="isNovelSelected(novel)" class="selected-mark">✓</span>
                  </div>
                </div>
              </div>
              <div class="all-novels">
                <h4 v-if="!novelSearchKeyword">全部小说</h4>
                <h4 v-else>搜索结果</h4>
                <div class="all-novels-grid">
                  <div v-for="novel in filteredNovels" :key="novel.id" 
                       class="novel-select-item"
                       :class="{ selected: isNovelSelected(novel) }"
                       @click="toggleNovelSelection(novel)">
                    <img :src="novel.cover" class="novel-select-cover" />
                    <div class="novel-select-info">
                      <span class="novel-select-title">{{ novel.title }}</span>
                      <span class="novel-select-author">{{ novel.author }}</span>
                    </div>
                    <span v-if="isNovelSelected(novel)" class="selected-mark">✓</span>
                  </div>
                  <div v-if="filteredNovels.length === 0" class="no-results">
                    没有找到匹配的小说
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="form-actions">
            <button type="button" class="cancel-btn" @click="showCreateBooklist = false">取消</button>
            <button type="submit" class="submit-btn" :disabled="newBooklist.selectedNovels.length === 0">
              创建书单 ({{ newBooklist.selectedNovels.length }}本)
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 话题详情弹窗 -->
    <div v-if="showTopicDetail && currentTopic" class="modal-overlay" @click.self="showTopicDetail = false">
      <div class="modal-content modal-large">
        <div class="modal-header">
          <h2>话题详情</h2>
          <button class="close-btn" @click="showTopicDetail = false">×</button>
        </div>
        <div class="topic-detail">
          <div class="topic-detail-header">
            <img :src="currentTopic.authorAvatar" class="author-avatar large" />
            <div class="topic-detail-info">
              <span class="author-name">{{ currentTopic.authorName }}</span>
              <span class="topic-time">{{ formatTime(currentTopic.createdAt) }}</span>
            </div>
            <div class="topic-detail-tags">
              <span v-for="tag in currentTopic.tags" :key="tag" class="tag">{{ tag }}</span>
            </div>
          </div>
          <h3 class="topic-detail-title">{{ currentTopic.title }}</h3>
          <p class="topic-detail-content">
            <template v-for="(part, index) in renderContentWithNovelLinks(currentTopic.content)" :key="index">
              <template v-if="part.type === 'text'">{{ part.content }}</template>
              <span 
                v-else-if="part.type === 'novel'" 
                class="novel-link"
                :class="{ 'has-link': part.id }"
                @click.stop="handleNovelClick(part.id, part.name)"
              >
                {{ part.fullMatch }}
              </span>
            </template>
          </p>
          <div class="topic-detail-actions">
            <button class="action-btn" :class="{ liked: currentTopic.isLiked }" @click="likeTopic(currentTopic, $event)">
              <span class="heart-icon">{{ currentTopic.isLiked ? '❤️' : '🤍' }}</span> {{ currentTopic.isLiked ? '已赞' : '点赞' }} ({{ currentTopic.likes }})
            </button>
            <span class="view-count">👁 {{ currentTopic.views }} 次浏览</span>
          </div>
          
          <div class="replies-section">
            <h4>评论 ({{ currentTopic.replies.length }})</h4>
            <div class="reply-input-area">
              <textarea v-model="newReply" placeholder="发表你的看法..." maxlength="500"></textarea>
              <button class="submit-reply-btn" @click="submitReply">发表评论</button>
            </div>
            <div class="replies-list">
              <div v-for="reply in currentTopic.replies" :key="reply.id" class="reply-item">
                <img :src="reply.authorAvatar" class="author-avatar" />
                <div class="reply-content">
                  <div class="reply-header">
                    <span class="author-name">{{ reply.authorName }}</span>
                    <span class="reply-time">{{ formatTime(reply.createdAt) }}</span>
                  </div>
                  <p>{{ reply.content }}</p>
                  <div class="reply-actions">
                    <span class="reply-like">❤️ {{ reply.likes }}</span>
                  </div>
                </div>
              </div>
              <div v-if="currentTopic.replies.length === 0" class="no-replies">
                暂无评论，快来抢沙发吧！
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 书单详情弹窗 -->
    <div v-if="showBooklistDetail && currentBooklist" class="modal-overlay" @click.self="showBooklistDetail = false">
      <div class="modal-content modal-large">
        <div class="modal-header">
          <h2>书单详情</h2>
          <button class="close-btn" @click="showBooklistDetail = false">×</button>
        </div>
        <div class="booklist-detail">
          <div class="booklist-detail-header">
            <h3 class="booklist-detail-title">{{ currentBooklist.title }}</h3>
            <p class="booklist-detail-desc">{{ currentBooklist.description }}</p>
            <div class="booklist-detail-meta">
              <img :src="currentBooklist.creatorAvatar" class="creator-avatar" />
              <span class="creator-name">by {{ currentBooklist.creatorName }}</span>
              <span class="create-time">{{ formatTime(currentBooklist.createdAt) }}</span>
              <span class="book-count">📚 {{ currentBooklist.novelCount }} 本书</span>
              <button class="like-btn-large" :class="{ liked: currentBooklist.isLiked }" @click="likeBooklist(currentBooklist, $event)">
                <span class="heart-icon">{{ currentBooklist.isLiked ? '❤️' : '🤍' }}</span> {{ currentBooklist.likes }}
              </button>
            </div>
          </div>
          <div class="booklist-novels">
            <h4>书单内容</h4>
            <div class="booklist-novels-grid">
              <div v-for="novel in currentBooklist.novels" :key="novel.id" class="booklist-novel-item" @click="goToNovel(novel.id)">
                <img :src="novel.cover" class="novel-cover" />
                <div class="novel-info">
                  <span class="novel-title">{{ novel.title }}</span>
                  <span class="novel-author">{{ novel.author }}</span>
                </div>
              </div>
              <div v-if="currentBooklist.novels.length === 0" class="empty-novels">
                书单暂无小说
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * Community.vue - 社区页面组件
 * 
 * 该组件实现了社区功能，包括：
 * - 话题讨论列表和详情
 * - 书单分享列表和详情
 * - 创建话题和书单
 * - 点赞功能
 * - 分页显示
 */
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user.js'
import { discussionApi } from '../api'
import { novelApi } from '../api/modules/novel.js'
import { recommendationApi } from '../api/modules/recommendation.js'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('topics')
const topics = ref([])
const booklists = ref([])
const novels = ref([])
const showCreateTopic = ref(false)
const showCreateBooklist = ref(false)
const showTopicDetail = ref(false)
const showBooklistDetail = ref(false)
const currentTopic = ref(null)
const currentBooklist = ref(null)
const recommendedNovels = ref([])
const novelSearchKeyword = ref('')

const topicsPage = ref(0)
const topicsSize = ref(10)
const topicsTotalPages = ref(0)
const topicsTotalElements = ref(0)

const booklistsPage = ref(0)
const booklistsSize = ref(10)
const booklistsTotalPages = ref(0)
const booklistsTotalElements = ref(0)

/**
 * 获取当前登录用户信息
 */
const currentUser = computed(() => {
  const userInfo = userStore.userInfo
  return {
    id: userInfo.id || 1,
    name: userInfo.username || userInfo.name || '游客用户',
    avatar: userInfo.avatar || ''
  }
})

/**
 * 过滤后的小说列表（根据搜索关键词）
 */
const filteredNovels = computed(() => {
  if (!novelSearchKeyword.value) {
    return novels.value
  }
  const keyword = novelSearchKeyword.value.toLowerCase()
  return novels.value.filter(novel => 
    novel.title.toLowerCase().includes(keyword) || 
    novel.author.toLowerCase().includes(keyword)
  )
})

/**
 * 处理小说搜索
 */
const handleNovelSearch = async () => {
  if (novelSearchKeyword.value.trim()) {
    try {
      const res = await novelApi.searchNovels(novelSearchKeyword.value)
      if (res.code === 200 && Array.isArray(res.data)) {
        novels.value = res.data.map(novel => ({
          id: novel.id,
          title: novel.title,
          author: novel.author,
          cover: novel.coverImage || defaultCover,
          category: novel.category
        }))
      }
    } catch (error) {
      console.error('搜索小说失败:', error)
    }
  } else {
    await loadNovels()
  }
}

const defaultAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed='
const defaultCover = 'https://via.placeholder.com/100x140/667eea/ffffff?text=No+Cover'

const newTopic = ref({
  title: '',
  content: '',
  tags: []
})

const newBooklist = ref({
  title: '',
  description: '',
  selectedNovels: [],
  isPublic: true
})

const newReply = ref('')

const novelNameToIdMap = ref({})
const novelMappingLoading = ref(false)
const NOVEL_MAPPING_CACHE_DURATION = 5 * 60 * 1000
const novelMappingCache = ref({
  data: null,
  timestamp: 0
})

/**
 * 格式化时间显示
 * @param {string} dateStr - 日期字符串
 * @returns {string} 格式化后的时间
 */
const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days > 7) return date.toLocaleDateString()
  if (days > 0) return `${days}天前`
  const hours = Math.floor(diff / (1000 * 60 * 60))
  if (hours > 0) return `${hours}小时前`
  const minutes = Math.floor(diff / (1000 * 60))
  return `${minutes}分钟前`
}

/**
 * 将API返回的话题数据映射为前端格式
 * @param {Object} d - API返回的话题数据
 * @returns {Object} 前端格式的话题对象
 */
const mapTopicFromApi = (d) => {
  const user = d.user || {}
  return {
    id: d.id,
    title: d.title,
    content: d.content || '',
    authorName: user.username || user.nickname || '用户',
    authorAvatar: user.avatar ? `${user.avatar}` : (defaultAvatar + (d.id || '')),
    views: d.views != null ? d.views : 0,
    replyCount: d.replies != null ? d.replies : 0,
    likes: d.likes != null ? d.likes : 0,
    isLiked: false,
    tags: d.tags || ['讨论'],
    createdAt: d.createdAt,
    replies: d.replyList || []
  }
}

/**
 * 加载话题列表
 * @param {number} page - 页码
 */
const loadTopics = async (page = 0) => {
  try {
    const res = await discussionApi.getTopicsPaginated(page, topicsSize.value)
    if (res.code === 200 && res.data) {
      topics.value = res.data.content.map(mapTopicFromApi)
      topicsTotalPages.value = res.data.totalPages
      topicsTotalElements.value = res.data.totalElements
      topicsPage.value = res.data.currentPage
    } else {
      topics.value = []
    }
  } catch (error) {
    console.error('加载话题失败:', error)
    topics.value = []
    ElMessage.error('加载话题失败')
  }
}

/**
 * 构建小说名称映射
 * @param {Array} novels - 小说列表
 * @returns {Object} 小说名称到ID的映射对象
 */
const buildNovelMapping = (novels) => {
  const mapping = {}
  
  novels.forEach(novel => {
    if (!novel.id) return
    
    if (novel.title) {
      mapping[novel.title] = novel.id
    }
    
    if (novel.aliases && Array.isArray(novel.aliases)) {
      novel.aliases.forEach(alias => {
        mapping[alias] = novel.id
      })
    }
  })
  
  return mapping
}

/**
 * 动态加载小说名称映射（带缓存）
 * @param {boolean} forceRefresh - 是否强制刷新缓存
 */
const loadNovelMapping = async (forceRefresh = false) => {
  const now = Date.now()
  const cacheAge = now - novelMappingCache.value.timestamp
  
  if (!forceRefresh && 
      novelMappingCache.value.data && 
      cacheAge < NOVEL_MAPPING_CACHE_DURATION) {
    novelNameToIdMap.value = novelMappingCache.value.data
    return
  }
  
  if (novelMappingLoading.value) return
  
  try {
    novelMappingLoading.value = true
    const res = await novelApi.getNovels()
    
    if (res.code === 200 && Array.isArray(res.data)) {
      const mapping = buildNovelMapping(res.data)
      
      novelMappingCache.value = {
        data: mapping,
        timestamp: now
      }
      
      novelNameToIdMap.value = mapping
    }
  } catch (error) {
    console.error('加载小说映射失败:', error)
  } finally {
    novelMappingLoading.value = false
  }
}

/**
 * 强制刷新小说映射
 */
const refreshNovelMapping = () => {
  return loadNovelMapping(true)
}

/**
 * 将内容中的小说名称转换为可点击的链接
 * @param {string} content - 原始内容
 * @returns {Array} 包含文本和链接的数组
 */
const renderContentWithNovelLinks = (content) => {
  if (!content) return ''
  
  // 匹配《小说名》格式的文本
  const regex = /《([^》]+)》/g
  const parts = []
  let lastIndex = 0
  let match
  
  while ((match = regex.exec(content)) !== null) {
    // 添加匹配前的普通文本
    if (match.index > lastIndex) {
      parts.push({ type: 'text', content: content.slice(lastIndex, match.index) })
    }
    
    // 添加小说名称链接
    const novelName = match[1]
    const novelId = novelNameToIdMap.value[novelName]
    parts.push({ 
      type: 'novel', 
      name: novelName, 
      id: novelId,
      fullMatch: match[0]
    })
    
    lastIndex = match.index + match[0].length
  }
  
  // 添加剩余文本
  if (lastIndex < content.length) {
    parts.push({ type: 'text', content: content.slice(lastIndex) })
  }
  
  return parts
}

/**
 * 点击小说名称跳转
 * @param {number} novelId - 小说ID
 * @param {string} novelName - 小说名称
 */
const handleNovelClick = (novelId, novelName) => {
  if (novelId) {
    router.push(`/novel/${novelId}`)
  } else {
    ElMessage.info(`《${novelName}》暂未收录，敬请期待！`)
  }
}

/**
 * 加载书单列表
 * @param {number} page - 页码
 */
const loadBooklists = async (page = 0) => {
  try {
    console.log('=== 开始加载书单 ===')
    const res = await discussionApi.getPublicBooklistsPaginated(page, booklistsSize.value)
    console.log('公开书单API响应:', res)
    
    let booklistData = []
    
    if (res.code === 200 && res.data && res.data.content && res.data.content.length > 0) {
      console.log('公开书单数据:', res.data.content)
      booklistData = res.data.content.map(booklist => ({
        id: booklist.id,
        title: booklist.title,
        description: booklist.description,
        creatorName: booklist.creatorName || booklist.userId || '用户',
        creatorAvatar: defaultAvatar + (booklist.id || ''),
        novelCount: booklist.bookCount || 0,
        likes: booklist.likeCount || 0,
        isLiked: false,
        covers: [], 
        novels: [],
        createdAt: booklist.createdAt
      }))
      booklistsTotalPages.value = res.data.totalPages
      booklistsTotalElements.value = res.data.totalElements
      booklistsPage.value = res.data.currentPage
    } else {
      console.log('公开书单为空，尝试加载当前用户书单')
      if (isLoggedIn.value && currentUser.value && currentUser.value.id) {
        const userRes = await discussionApi.getUserBooklists(currentUser.value.id)
        console.log('用户书单API响应:', userRes)
        if (userRes.code === 200 && userRes.data) {
          booklistData = userRes.data.map(booklist => ({
             id: booklist.id,
             title: booklist.title,
             description: booklist.description || '',
             creatorName: currentUser.value.username || '我',
             creatorAvatar: currentUser.value.avatar || defaultAvatar,
             novelCount: booklist.novels?.length || 0,
             likes: booklist.likes || 0,
             isLiked: false,
             covers: [],
             novels: booklist.novels || [],
             createdAt: booklist.createdAt
           }))
        }
      }
    }
    
    console.log('解析后的书单列表:', booklistData)
    booklists.value = booklistData
  } catch (error) {
    console.error('加载书单失败:', error)
    booklists.value = []
  }
}

/**
 * 加载小说列表
 */
const loadNovels = async () => {
  try {
    const res = await novelApi.getNovelsByStatus('APPROVED')
    if (res.code === 200 && Array.isArray(res.data)) {
      novels.value = res.data.map(novel => ({
        id: novel.id,
        title: novel.title,
        author: novel.author,
        cover: novel.coverImage || defaultCover,
        category: novel.category
      }))
    } else {
      novels.value = []
    }
  } catch (error) {
    console.error('加载小说失败:', error)
    novels.value = []
  }
}

/**
 * 加载推荐小说
 */
const loadRecommendedNovels = async () => {
  try {
    const res = await recommendationApi.getPersonalizedRecommendations(6)
    if (res.code === 200 && Array.isArray(res.data)) {
      recommendedNovels.value = res.data.map(novel => ({
        id: novel.id,
        title: novel.title,
        author: novel.author,
        cover: novel.coverImage || defaultCover,
        category: novel.category
      }))
    } else {
      recommendedNovels.value = []
    }
  } catch (error) {
    console.error('加载推荐小说失败:', error)
    recommendedNovels.value = []
  }
}

/**
 * 查看话题详情
 * @param {Object} topic - 话题对象
 */
const viewTopic = (topic) => {
  currentTopic.value = topic
  showTopicDetail.value = true
  topic.views++
}

/**
 * 查看书单详情
 * @param {Object} booklist - 书单对象
 */
const viewBooklist = async (booklist) => {
  try {
    // 调用后端 API 获取书单详情（包含小说列表）
    const res = await discussionApi.getBooklistById(booklist.id)
    console.log('书单详情 API 响应:', res)
    
    if (res && res.data) {
      // 更新当前书单为完整的详情数据
      currentBooklist.value = {
        id: res.data.id,
        userId: res.data.userId,
        title: res.data.title,
        description: res.data.description,
        listType: res.data.listType,
        bookCount: res.data.bookCount,
        viewCount: res.data.viewCount,
        collectCount: res.data.collectCount,
        likeCount: res.data.likeCount,
        createdAt: res.data.createdAt,
        user: res.data.user,
        novels: res.data.novels || []
      }
      showBooklistDetail.value = true
    } else {
      ElMessage.error('书单不存在')
    }
  } catch (error) {
    console.error('获取书单详情失败:', error)
    ElMessage.error('获取书单详情失败')
  }
}

/**
 * 点赞话题
 * @param {Object} topic - 话题对象
 * @param {Event} event - 点击事件
 */
const likeTopic = (topic, event) => {
  event.stopPropagation()
  if (topic.isLiked) {
    topic.likes--
    topic.isLiked = false
  } else {
    topic.likes++
    topic.isLiked = true
    ElMessage.success('点赞成功！')
  }
}

/**
 * 点赞书单
 * @param {Object} booklist - 书单对象
 * @param {Event} event - 点击事件
 */
const likeBooklist = (booklist, event) => {
  event.stopPropagation()
  if (booklist.isLiked) {
    booklist.likes--
    booklist.isLiked = false
  } else {
    booklist.likes++
    booklist.isLiked = true
    ElMessage.success('收藏书单成功！')
  }
}

/**
 * 创建话题
 */
const createTopic = async () => {
  if (!newTopic.value.title.trim() || !newTopic.value.content.trim()) {
    ElMessage.warning('请填写完整的话题信息')
    return
  }
  try {
    const res = await discussionApi.createTopic({
      title: newTopic.value.title.trim(),
      content: newTopic.value.content.trim()
    })
    if (res.code === 200 && res.data) {
      topics.value.unshift(mapTopicFromApi(res.data))
      showCreateTopic.value = false
      newTopic.value = { title: '', content: '', tags: [] }
      ElMessage.success('话题发布成功！')
    } else {
      ElMessage.error(res.message || '发布失败')
    }
  } catch (error) {
    console.error('发布话题失败:', error)
    ElMessage.error(error.response?.data?.message || '发布话题失败，请先登录')
  }
}

/**
 * 创建书单
 */
const createBooklist = async () => {
  if (!newBooklist.value.title.trim()) {
    ElMessage.warning('请填写书单名称')
    return
  }
  
  try {
    const isPublicValue = true
    console.log('创建书单 - isPublic 强制设置为:', isPublicValue)
    
    const res = await discussionApi.createBooklist({
      userId: currentUser.value.id,
      title: newBooklist.value.title.trim(),
      description: newBooklist.value.description.trim(),
      isPublic: isPublicValue,
      novelIds: newBooklist.value.selectedNovels.map(novel => novel.id)
    })
    
    if (res.code === 200 && res.data) {
      // 构建前端显示的数据结构
      const booklist = {
        id: res.data.id,
        title: res.data.title,
        description: res.data.description,
        creatorName: currentUser.value.name,
        creatorAvatar: currentUser.value.avatar || defaultAvatar + 'me',
        novelCount: newBooklist.value.selectedNovels.length,
        likes: 0,
        isLiked: false,
        covers: newBooklist.value.selectedNovels.slice(0, 3).map(n => n.cover),
        novels: newBooklist.value.selectedNovels,
        createdAt: new Date()
      }
      
      booklists.value.unshift(booklist)
      showCreateBooklist.value = false
      newBooklist.value = { title: '', description: '', selectedNovels: [], isPublic: true }
      ElMessage.success('书单创建成功！')
      
      // 刷新小说映射以获取最新数据
      refreshNovelMapping()
    } else {
      ElMessage.error(res.message || '创建书单失败')
    }
  } catch (error) {
    console.error('创建书单失败:', error)
    ElMessage.error(error.response?.data?.message || '创建书单失败，请先登录')
  }
}

/**
 * 切换小说选择状态
 * @param {Object} novel - 小说对象
 */
const toggleNovelSelection = (novel) => {
  const index = newBooklist.value.selectedNovels.findIndex(n => n.id === novel.id)
  if (index > -1) {
    newBooklist.value.selectedNovels.splice(index, 1)
  } else {
    newBooklist.value.selectedNovels.push(novel)
  }
}

/**
 * 检查小说是否已选中
 * @param {Object} novel - 小说对象
 * @returns {boolean} 是否已选中
 */
const isNovelSelected = (novel) => {
  return newBooklist.value.selectedNovels.some(n => n.id === novel.id)
}

/**
 * 提交回复
 */
const submitReply = () => {
  if (!newReply.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  
  const reply = {
    id: Date.now(),
    authorName: currentUser.value.name,
    authorAvatar: currentUser.value.avatar || defaultAvatar + 'me',
    content: newReply.value,
    likes: 0,
    createdAt: new Date()
  }
  
  currentTopic.value.replies.push(reply)
  currentTopic.value.replyCount++
  newReply.value = ''
  ElMessage.success('回复成功！')
}

/**
 * 跳转到小说详情页
 * @param {number} novelId - 小说ID
 */
const goToNovel = (novelId) => {
  router.push(`/novel/${novelId}`)
}

/**
 * 话题分页控制
 * @param {number} page - 页码
 */
const changeTopicsPage = (page) => {
  if (page >= 0 && page < topicsTotalPages.value) {
    loadTopics(page)
  }
}

/**
 * 书单分页控制
 * @param {number} page - 页码
 */
const changeBooklistsPage = (page) => {
  if (page >= 0 && page < booklistsTotalPages.value) {
    loadBooklists(page)
  }
}

/**
 * 组件挂载时初始化
 */
onMounted(() => {
  loadTopics()
  loadBooklists()
  loadNovels()
  loadNovelMapping()
})

/**
 * 监听创建书单弹窗状态
 * 弹窗打开时加载推荐小说
 */
watch(showCreateBooklist, async (newValue) => {
  if (newValue) {
    await loadRecommendedNovels()
  }
})
</script>

<style scoped>
.community-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--spacing-xl);
  min-height: calc(100vh - 140px);
  width: 100%;
  flex: 1;
  position: relative;
  background: rgba(22, 27, 34, 0.8);
  backdrop-filter: blur(20px);
}

.community-page::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  width: 4px;
  height: 100vh;
  background: linear-gradient(180deg, var(--color-primary) 0%, var(--color-accent) 100%);
  z-index: 100;
}

.community-content {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

.community-header {
  text-align: center;
  margin-bottom: var(--spacing-2xl);
  background: linear-gradient(135deg, rgba(22, 27, 34, 0.9) 0%, rgba(13, 17, 23, 0.95) 100%);
  border-radius: var(--radius-xl);
  padding: var(--spacing-2xl);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--color-border);
  position: relative;
  overflow: hidden;
}

.community-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--color-primary) 0%, var(--color-accent) 100%);
}

.community-header h1 {
  font-size: 36px;
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-sm);
  font-family: var(--font-serif);
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-accent) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.community-header p {
  color: var(--color-text-secondary);
  font-size: 16px;
  font-weight: 500;
}

.community-tabs {
  display: flex;
  justify-content: center;
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-2xl);
}

.tab-btn {
  padding: var(--spacing-md) var(--spacing-2xl);
  border: 2px solid var(--color-border);
  background: linear-gradient(135deg, rgba(22, 27, 34, 0.9) 0%, rgba(13, 17, 23, 0.95) 100%);
  border-radius: var(--radius-xl);
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  transition: all var(--transition-base);
  color: var(--color-text-primary);
  box-shadow: var(--shadow-sm);
}

.tab-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--color-primary);
}

.tab-btn.active {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: #0d1117;
  border-color: var(--color-primary);
  box-shadow: 0 4px 12px rgba(201, 169, 98, 0.3);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h2 {
  font-size: 24px;
  color: var(--color-text-primary);
}

.create-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: #0d1117;
  border: none;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all 0.3s;
  font-weight: 600;
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(201, 169, 98, 0.3);
}

.topics-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.topic-card {
  background: rgba(22, 27, 34, 0.9);
  border-radius: var(--radius-lg);
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid var(--color-border);
}

.topic-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
  border-color: var(--color-primary);
}

.topic-header {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
  flex-wrap: wrap;
  gap: 10px;
}

.author-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 10px;
  border: 2px solid var(--color-primary);
}

.author-avatar.large {
  width: 50px;
  height: 50px;
}

.topic-info {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.author-name {
  font-weight: bold;
  color: var(--color-text-primary);
}

.topic-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.topic-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tag {
  padding: 4px 10px;
  background: rgba(201, 169, 98, 0.15);
  color: var(--color-primary);
  border-radius: 12px;
  font-size: 12px;
  border: 1px solid rgba(201, 169, 98, 0.3);
}

.topic-title {
  font-size: 18px;
  color: var(--color-text-primary);
  margin-bottom: 10px;
}

.topic-content {
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin-bottom: 15px;
}

.topic-footer {
  display: flex;
  gap: 20px;
}

.topic-stat {
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--color-text-tertiary);
  font-size: 14px;
}

.topic-stat.like-btn {
  cursor: pointer;
  transition: all 0.3s;
  color: var(--color-text-tertiary);
}

.topic-stat.like-btn:hover {
  color: #ff6b6b;
}

.topic-stat.like-btn.liked {
  color: #ff6b6b;
}

.heart-icon {
  font-size: 14px;
  transition: all 0.3s;
}

/* 小说名称链接样式 */
.novel-link {
  color: var(--color-primary);
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.novel-link:hover {
  color: var(--color-primary-light);
  text-decoration: underline;
}

.novel-link.has-link {
  color: var(--color-primary);
}

.novel-link:not(.has-link) {
  color: var(--color-text-tertiary);
  cursor: default;
}

.novel-link:not(.has-link):hover {
  text-decoration: none;
}

.booklists-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.booklist-card {
  background: rgba(22, 27, 34, 0.9);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid var(--color-border);
}

.booklist-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
  border-color: var(--color-primary);
}

.booklist-covers {
  display: flex;
  height: 140px;
  background: linear-gradient(135deg, rgba(201, 169, 98, 0.3) 0%, rgba(214, 158, 46, 0.3) 100%);
  position: relative;
}

.cover-img {
  width: 33.33%;
  height: 100%;
  object-fit: cover;
  border-right: 1px solid rgba(255, 255, 255, 0.1);
}

.empty-covers {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.booklist-info {
  padding: 15px;
}

.booklist-title {
  font-size: 18px;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

.booklist-desc {
  color: var(--color-text-secondary);
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 10px;
}

.booklist-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--color-text-tertiary);
  font-size: 12px;
  margin-bottom: 10px;
}

.booklist-likes {
  cursor: pointer;
  transition: all 0.3s;
  padding: 4px 8px;
  border-radius: 12px;
}

.booklist-likes:hover {
  background: rgba(255, 107, 107, 0.1);
  color: #ff6b6b;
}

.booklist-likes.liked {
  color: #ff6b6b;
  background: rgba(255, 107, 107, 0.1);
}

.booklist-creator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.creator-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1px solid var(--color-primary);
}

.creator-name {
  color: var(--color-text-secondary);
}

.create-time {
  color: var(--color-text-tertiary);
  margin-left: auto;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(5px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: rgba(22, 27, 34, 0.98);
  border-radius: var(--radius-xl);
  width: 500px;
  max-width: 90%;
  max-height: 90vh;
  overflow-y: auto;
  border: 1px solid var(--color-border);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
}

.modal-content.modal-large {
  width: 700px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 30px;
  border-bottom: 1px solid var(--color-border);
}

.modal-header h2 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 20px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: var(--color-text-tertiary);
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: var(--color-text-primary);
}

.modal-content form {
  padding: 20px 30px 30px;
}

.form-group {
  margin-bottom: 20px;
}

.form-row {
  display: flex;
  gap: 20px;
}

.form-group-half {
  flex: 1;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: var(--color-text-primary);
  font-weight: 500;
}

.form-group .required {
  color: #ff6b6b;
}

.form-group .hint {
  color: var(--color-text-tertiary);
  font-weight: normal;
  font-size: 12px;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  background: rgba(255, 255, 255, 0.03);
  color: var(--color-text-primary);
  transition: all 0.3s;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(201, 169, 98, 0.1);
  background: rgba(201, 169, 98, 0.05);
}

.form-group input::placeholder,
.form-group textarea::placeholder {
  color: var(--color-text-tertiary);
}

.form-group textarea {
  min-height: 120px;
  resize: vertical;
}

.char-count {
  display: block;
  text-align: right;
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-top: 5px;
}

.tags-preview {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.radio-group {
  display: flex;
  gap: 20px;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-weight: normal;
  color: var(--color-text-secondary);
}

.radio-label input {
  width: auto;
  accent-color: var(--color-primary);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.cancel-btn {
  padding: 10px 20px;
  border: 1px solid var(--color-border);
  background: transparent;
  color: var(--color-text-secondary);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.3s;
}

.cancel-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.submit-btn {
  padding: 10px 20px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: #0d1117;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(201, 169, 98, 0.3);
}

.submit-btn:disabled {
  background: var(--color-text-tertiary);
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 小说选择区域 */
.novel-search {
  margin-bottom: 15px;
}

.novel-search input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  background: rgba(255, 255, 255, 0.03);
  color: var(--color-text-primary);
  transition: all 0.3s;
}

.novel-search input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(201, 169, 98, 0.1);
}

.novels-selection {
  max-height: 350px;
  overflow-y: auto;
  padding: 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.02);
}

.no-results {
  text-align: center;
  padding: 20px;
  color: var(--color-text-tertiary);
  font-size: 14px;
}

.recommended-novels {
  margin-bottom: 20px;
}

.recommended-novels h4,
.all-novels h4 {
  margin-top: 0;
  margin-bottom: 10px;
  color: var(--color-text-primary);
  font-size: 14px;
  font-weight: 600;
}

.recommended-novels-grid,
.all-novels-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 10px;
  margin-bottom: 15px;
}

.novel-select-item.recommended {
  border: 2px solid var(--color-primary);
  background-color: rgba(201, 169, 98, 0.1);
}

.novel-select-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border: 2px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.3s;
  background: rgba(255, 255, 255, 0.03);
}

.novel-select-item:hover {
  background: rgba(201, 169, 98, 0.1);
  border-color: var(--color-primary);
}

.novel-select-item.selected {
  border-color: var(--color-primary);
  background: rgba(201, 169, 98, 0.15);
}

.novel-select-cover {
  width: 40px;
  height: 56px;
  object-fit: cover;
  border-radius: 3px;
}

.novel-select-info {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.novel-select-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.novel-select-author {
  font-size: 11px;
  color: var(--color-text-tertiary);
}

.selected-mark {
  width: 20px;
  height: 20px;
  background: var(--color-primary);
  color: #0d1117;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
}

/* 话题详情 */
.topic-detail {
  padding: 20px 30px;
}

.topic-detail-header {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.topic-detail-info {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.topic-detail-tags {
  display: flex;
  gap: 8px;
}

.topic-detail-title {
  font-size: 22px;
  color: var(--color-text-primary);
  margin-bottom: 15px;
  line-height: 1.4;
}

.topic-detail-content {
  color: var(--color-text-secondary);
  line-height: 1.8;
  margin-bottom: 20px;
  white-space: pre-line;
}

.topic-detail-actions {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 15px 0;
  border-top: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 20px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 16px;
  border: 1px solid var(--color-border);
  background: transparent;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;
  color: var(--color-text-secondary);
}

.action-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.action-btn.liked {
  border-color: #ff6b6b;
  color: #ff6b6b;
  background: rgba(255, 107, 107, 0.1);
}

.view-count {
  color: var(--color-text-tertiary);
  font-size: 14px;
}

/* 评论区 */
.replies-section {
  margin-top: 20px;
}

.replies-section h4 {
  margin-bottom: 15px;
  color: var(--color-text-primary);
}

.reply-input-area {
  margin-bottom: 20px;
}

.reply-input-area textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  resize: vertical;
  min-height: 80px;
  margin-bottom: 10px;
  background: rgba(255, 255, 255, 0.03);
  color: var(--color-text-primary);
}

.reply-input-area textarea:focus {
  outline: none;
  border-color: var(--color-primary);
}

.submit-reply-btn {
  padding: 8px 20px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: #0d1117;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  float: right;
  font-weight: 600;
}

.submit-reply-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(201, 169, 98, 0.3);
}

.replies-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.reply-item {
  display: flex;
  gap: 12px;
  padding: 15px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}

.reply-content {
  flex: 1;
}

.reply-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.reply-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.reply-content p {
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin: 0;
}

.reply-actions {
  margin-top: 8px;
}

.reply-like {
  font-size: 12px;
  color: var(--color-text-tertiary);
  cursor: pointer;
}

.no-replies {
  text-align: center;
  padding: 40px;
  color: var(--color-text-tertiary);
}

/* 书单详情 */
.booklist-detail {
  padding: 20px 30px;
}

.booklist-detail-header {
  margin-bottom: 25px;
}

.booklist-detail-title {
  font-size: 24px;
  color: var(--color-text-primary);
  margin-bottom: 10px;
}

.booklist-detail-desc {
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin-bottom: 15px;
}

.booklist-detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.like-btn-large {
  padding: 8px 16px;
  border: 1px solid var(--color-border);
  background: transparent;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;
  margin-left: auto;
  color: var(--color-text-secondary);
}

.like-btn-large:hover {
  border-color: #ff6b6b;
  color: #ff6b6b;
}

.like-btn-large.liked {
  border-color: #ff6b6b;
  color: #ff6b6b;
  background: rgba(255, 107, 107, 0.1);
}

.booklist-novels h4 {
  margin-bottom: 15px;
  color: var(--color-text-primary);
}

.booklist-novels-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 15px;
}

.booklist-novel-item {
  cursor: pointer;
  transition: transform 0.3s;
}

.booklist-novel-item:hover {
  transform: translateY(-3px);
}

.booklist-novel-item .novel-cover {
  width: 100%;
  aspect-ratio: 10/14;
  object-fit: cover;
  border-radius: var(--radius-md);
  margin-bottom: 8px;
  border: 1px solid var(--color-border);
}

.booklist-novel-item .novel-info {
  text-align: center;
}

.booklist-novel-item .novel-title {
  display: block;
  font-size: 13px;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.booklist-novel-item .novel-author {
  display: block;
  font-size: 11px;
  color: var(--color-text-tertiary);
}

.empty-novels {
  grid-column: 1 / -1;
  text-align: center;
  padding: 40px;
  color: var(--color-text-tertiary);
}

.btn-icon {
  margin-right: 4px;
}

/* 分页样式 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  margin-top: 30px;
  padding: 20px;
}

.page-btn {
  padding: 10px 20px;
  border: 1px solid var(--color-border);
  background: transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s;
  color: var(--color-text-secondary);
}

.page-btn:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.page-btn:disabled {
  color: var(--color-text-tertiary);
  cursor: not-allowed;
  border-color: var(--color-border);
  opacity: 0.5;
}

.page-info {
  font-size: 14px;
  color: var(--color-text-secondary);
}

@media (max-width: 768px) {
  .community-header h1 {
    font-size: 24px;
  }

  .community-tabs {
    flex-direction: column;
    align-items: center;
  }

  .tab-btn {
    width: 200px;
  }

  .booklists-grid {
    grid-template-columns: 1fr;
  }

  .form-row {
    flex-direction: column;
    gap: 0;
  }

  .novels-selection {
    grid-template-columns: 1fr;
  }

  .modal-content.modal-large {
    width: 95%;
  }

  .booklist-novels-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .topic-detail-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .booklist-detail-meta {
    flex-direction: column;
    align-items: flex-start;
  }

  .like-btn-large {
    margin-left: 0;
    margin-top: 10px;
  }
}
</style>
