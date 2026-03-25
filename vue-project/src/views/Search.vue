<script setup>
/**
 * Search.vue - 小说搜索页面组件
 * 
 * 该组件实现了小说搜索功能，包括：
 * - 关键词搜索
 * - 高级筛选（分类、作者、字数、状态等）
 * - 搜索建议
 * - 热门关键词
 * - 分页显示搜索结果
 */
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NovelCard from '../components/NovelCard.vue'
import { searchApi } from '../api/modules/search.js'
import { categoryApi } from '../api/modules/category.js'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const searchQuery = ref(route.query.q || '')
const searchResults = ref([])
const loading = ref(false)
const showFilters = ref(false)

const filters = ref({
  category: '',
  author: '',
  minWordCount: null,
  maxWordCount: null,
  status: '',
  sortBy: 'createdAt'
})

const categories = ref([])

const pagination = ref({
  currentPage: 0,
  totalPages: 0,
  totalElements: 0,
  size: 20
})

const suggestions = ref([])
const showSuggestions = ref(false)

const hotKeywords = ref([])

/**
 * 加载分类列表
 */
const loadCategories = async () => {
  try {
    const response = await categoryApi.getAllCategories()
    if (response.code === 200) {
      categories.value = response.data || []
    }
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

// 加载热门关键词
const loadHotKeywords = async () => {
  try {
    const response = await searchApi.getHotKeywords()
    if (response.code === 200) {
      hotKeywords.value = response.data || []
    }
  } catch (error) {
    console.error('加载热门关键词失败:', error)
  }
}

// 获取搜索建议
const getSuggestions = async () => {
  if (!searchQuery.value || searchQuery.value.length < 2) {
    suggestions.value = []
    showSuggestions.value = false
    return
  }
  
  try {
    const response = await searchApi.getSearchSuggestions(searchQuery.value)
    if (response.code === 200) {
      suggestions.value = response.data || []
      showSuggestions.value = suggestions.value.length > 0
    }
  } catch (error) {
    console.error('获取搜索建议失败:', error)
  }
}

// 选择搜索建议
const selectSuggestion = (suggestion) => {
  searchQuery.value = suggestion
  showSuggestions.value = false
  handleSearch()
}

// 选择热门关键词
const selectHotKeyword = (keyword) => {
  searchQuery.value = keyword
  handleSearch()
}

// 高级搜索
const handleSearch = async (page = 0) => {
  if (!searchQuery.value.trim() && !filters.value.category && !filters.value.author) {
    ElMessage.warning('请输入搜索关键词或选择筛选条件')
    return
  }
  
  // 更新URL参数
  const query = { q: searchQuery.value }
  if (filters.value.category) query.category = filters.value.category
  router.push({ query })
  
  loading.value = true
  showSuggestions.value = false
  
  try {
    const params = {
      keyword: searchQuery.value || undefined,
      category: filters.value.category || undefined,
      author: filters.value.author || undefined,
      minWordCount: filters.value.minWordCount || undefined,
      maxWordCount: filters.value.maxWordCount || undefined,
      status: filters.value.status || undefined,
      sortBy: filters.value.sortBy,
      page,
      size: pagination.value.size
    }
    
    const response = await searchApi.advancedSearch(params)
    if (response.code === 200) {
      const data = response.data
      searchResults.value = data.content || []
      pagination.value = {
        currentPage: data.currentPage,
        totalPages: data.totalPages,
        totalElements: data.totalElements,
        size: data.size
      }
      
      if (searchResults.value.length === 0) {
        ElMessage.info('没有找到相关小说')
      }
    } else {
      ElMessage.error(response.message || '搜索失败')
    }
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 重置筛选条件
const resetFilters = () => {
  filters.value = {
    category: '',
    author: '',
    minWordCount: null,
    maxWordCount: null,
    status: '',
    sortBy: 'createdAt'
  }
  handleSearch()
}

// 切换筛选面板
const toggleFilters = () => {
  showFilters.value = !showFilters.value
}

// 监听URL参数变化
watch(() => route.query.q, (newQuery) => {
  if (newQuery) {
    searchQuery.value = newQuery
    handleSearch()
  }
})

// 监听搜索框输入
watch(searchQuery, () => {
  getSuggestions()
})

onMounted(() => {
  loadCategories()
  loadHotKeywords()
  if (searchQuery.value) {
    if (route.query.category) {
      filters.value.category = route.query.category
    }
    handleSearch()
  }
})
</script>

<template>
  <div class="search">
    <div class="search-header">
      <h1>搜索小说</h1>
      <div class="search-box-container">
        <div class="search-box">
          <input 
            type="text" 
            v-model="searchQuery" 
            placeholder="搜索小说名称、作者或关键词"
            @keyup.enter="handleSearch()"
            @focus="showSuggestions = suggestions.length > 0"
            @blur="setTimeout(() => showSuggestions = false, 200)"
          />
          <button @click="handleSearch()" :disabled="loading">
            {{ loading ? '搜索中...' : '搜索' }}
          </button>
        </div>
        
        <!-- 搜索建议 -->
        <div v-if="showSuggestions" class="suggestions-dropdown">
          <div 
            v-for="suggestion in suggestions" 
            :key="suggestion"
            class="suggestion-item"
            @click="selectSuggestion(suggestion)"
          >
            {{ suggestion }}
          </div>
        </div>
      </div>
      
      <!-- 热门搜索 -->
      <div v-if="hotKeywords.length > 0 && !searchQuery" class="hot-keywords">
        <span class="hot-label">热门搜索：</span>
        <span 
          v-for="keyword in hotKeywords.slice(0, 8)" 
          :key="keyword"
          class="hot-keyword"
          @click="selectHotKeyword(keyword)"
        >
          {{ keyword }}
        </span>
      </div>
    </div>
    
    <!-- 筛选按钮 -->
    <div class="filter-toggle">
      <button @click="toggleFilters" class="toggle-btn">
        <span>{{ showFilters ? '隐藏筛选' : '高级筛选' }}</span>
        <span class="toggle-icon">{{ showFilters ? '▲' : '▼' }}</span>
      </button>
    </div>
    
    <!-- 筛选面板 -->
    <div v-show="showFilters" class="filters-panel">
      <div class="filter-row">
        <div class="filter-item">
          <label>分类：</label>
          <select v-model="filters.category">
            <option value="">所有分类</option>
            <option v-for="cat in categories" :key="cat.id" :value="cat.name">
              {{ cat.name }}
            </option>
          </select>
        </div>
        
        <div class="filter-item">
          <label>作者：</label>
          <input type="text" v-model="filters.author" placeholder="作者名称" />
        </div>
        
        <div class="filter-item">
          <label>状态：</label>
          <select v-model="filters.status">
            <option value="">所有状态</option>
            <option value="连载中">连载中</option>
            <option value="已完结">已完结</option>
          </select>
        </div>
      </div>
      
      <div class="filter-row">
        <div class="filter-item">
          <label>字数范围：</label>
          <input 
            type="number" 
            v-model.number="filters.minWordCount" 
            placeholder="最小字数"
            class="word-count-input"
          />
          <span class="separator">至</span>
          <input 
            type="number" 
            v-model.number="filters.maxWordCount" 
            placeholder="最大字数"
            class="word-count-input"
          />
          <span class="word-unit">字</span>
        </div>
        
        <div class="filter-item">
          <label>排序方式：</label>
          <select v-model="filters.sortBy">
            <option value="createdAt">最新发布</option>
            <option value="updateTime">最近更新</option>
            <option value="viewCount">热度</option>
            <option value="bookmarkCount">收藏数</option>
          </select>
        </div>
      </div>
      
      <div class="filter-actions">
        <button @click="handleSearch()" class="apply-btn">应用筛选</button>
        <button @click="resetFilters" class="reset-btn">重置</button>
      </div>
    </div>
    
    <!-- 搜索结果 -->
    <div class="search-results">
      <div v-if="searchQuery || filters.category" class="results-header">
        <h2>搜索结果</h2>
        <span v-if="pagination.totalElements > 0" class="results-count">
          找到 {{ pagination.totalElements }} 本小说
        </span>
      </div>
      
      <div v-if="loading" class="loading-state">
        <p>搜索中...</p>
      </div>
      <div v-else-if="searchResults.length > 0">
        <div class="novel-list">
          <NovelCard v-for="novel in searchResults" :key="novel.id" :novel="novel" />
        </div>
        
        <!-- 分页 -->
        <div v-if="pagination.totalPages > 1" class="pagination">
          <button 
            @click="handleSearch(pagination.currentPage - 1)"
            :disabled="pagination.currentPage === 0"
            class="page-btn"
          >
            上一页
          </button>
          <span class="page-info">
            {{ pagination.currentPage + 1 }} / {{ pagination.totalPages }}
          </span>
          <button 
            @click="handleSearch(pagination.currentPage + 1)"
            :disabled="pagination.currentPage >= pagination.totalPages - 1"
            class="page-btn"
          >
            下一页
          </button>
        </div>
      </div>
      <div v-else-if="searchQuery || filters.category" class="empty-state">
        <p>没有找到相关小说</p>
        <p class="empty-tip">试试调整搜索关键词或筛选条件</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
@import '../assets/theme.css';

.search {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--spacing-xl);
  min-height: calc(100vh - 140px);
  width: 100%;
  flex: 1;
  position: relative;
}

.search::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  width: 4px;
  height: 100vh;
  background: linear-gradient(180deg, var(--color-primary) 0%, var(--color-accent) 100%);
  z-index: 100;
}

/* 搜索头部 - 文学殿堂主题 */
.search-header {
  text-align: center;
  margin-bottom: var(--spacing-2xl);
  padding: var(--spacing-2xl) var(--spacing-xl);
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--color-border);
  position: relative;
  overflow: hidden;
}

.search-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background: linear-gradient(90deg, var(--color-primary) 0%, var(--color-accent) 50%, var(--color-primary) 100%);
}

.search-header h1 {
  font-size: 36px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 var(--spacing-xl) 0;
  font-family: var(--font-serif);
  letter-spacing: -0.5px;
  position: relative;
}

.search-header h1::after {
  content: '';
  position: absolute;
  bottom: -12px;
  left: 50%;
  transform: translateX(-50%);
  width: 80px;
  height: 3px;
  background: linear-gradient(90deg, transparent 0%, var(--color-accent) 50%, transparent 100%);
}

.search-box-container {
  position: relative;
  max-width: 700px;
  margin: 0 auto;
}

.search-box {
  display: flex;
  box-shadow: var(--shadow-lg);
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--color-border);
}

.search-box input {
  flex: 1;
  padding: var(--spacing-md) var(--spacing-lg);
  border: none;
  font-size: 16px;
  outline: none;
  color: var(--color-text-primary);
  font-family: var(--font-sans);
  background: transparent;
}

.search-box input::placeholder {
  color: var(--color-text-tertiary);
}

.search-box button {
  padding: 0 var(--spacing-xl);
  background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-light) 100%);
  color: white;
  border: none;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  transition: all var(--transition-base);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.search-box button:hover:not(:disabled) {
  background: linear-gradient(135deg, var(--color-accent-light) 0%, var(--color-accent) 100%);
  transform: translateX(2px);
  box-shadow: 0 4px 12px rgba(214, 158, 46, 0.4);
}

.search-box button:disabled {
  background: var(--color-text-tertiary);
  cursor: not-allowed;
  opacity: 0.6;
}

/* 搜索建议 */
.suggestions-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-top: none;
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
  box-shadow: var(--shadow-xl);
  max-height: 300px;
  overflow-y: auto;
  z-index: 100;
  margin-top: 4px;
}

.suggestion-item {
  padding: var(--spacing-md) var(--spacing-lg);
  cursor: pointer;
  transition: all var(--transition-fast);
  color: var(--color-text-primary);
  font-size: 15px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.5);
}

.suggestion-item:last-child {
  border-bottom: none;
}

.suggestion-item:hover {
  background: linear-gradient(90deg, rgba(214, 158, 46, 0.1) 0%, transparent 100%);
  color: var(--color-primary);
  padding-left: calc(var(--spacing-lg) + 4px);
}

/* 热门搜索 */
.hot-keywords {
  margin-top: var(--spacing-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: var(--spacing-sm);
}

.hot-label {
  color: var(--color-text-tertiary);
  font-size: 14px;
  font-weight: 600;
}

.hot-keyword {
  padding: var(--spacing-xs) var(--spacing-md);
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-lg);
  color: var(--color-text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all var(--transition-fast);
  border: 1px solid transparent;
}

.hot-keyword:hover {
  background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-light) 100%);
  color: white;
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

/* 筛选切换按钮 */
.filter-toggle {
  text-align: center;
  margin-bottom: var(--spacing-lg);
}

.toggle-btn {
  padding: var(--spacing-sm) var(--spacing-xl);
  background: linear-gradient(135deg, var(--color-bg-tertiary) 0%, var(--color-bg-secondary) 100%);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  transition: all var(--transition-fast);
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.toggle-btn:hover {
  border-color: var(--color-accent);
  color: var(--color-primary);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.toggle-icon {
  font-size: 12px;
  transition: transform var(--transition-fast);
}

.toggle-btn.active .toggle-icon {
  transform: rotate(180deg);
}

/* 筛选面板 */
.filters-panel {
  background: var(--color-bg-secondary);
  padding: var(--spacing-2xl);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  margin-bottom: var(--spacing-2xl);
  border: 1px solid var(--color-border);
}

.filter-row {
  display: flex;
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-lg);
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  flex: 1;
  min-width: 280px;
}

.filter-item label {
  font-size: 14px;
  color: var(--color-text-secondary);
  white-space: nowrap;
  font-weight: 600;
}

.filter-item select,
.filter-item input[type="text"] {
  flex: 1;
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  background: var(--color-bg-secondary);
  color: var(--color-text-primary);
  transition: all var(--transition-fast);
}

.filter-item select:focus,
.filter-item input[type="text"]:focus {
  border-color: var(--color-accent);
  outline: none;
  box-shadow: 0 0 0 3px rgba(214, 158, 46, 0.1);
}

.word-count-input {
  width: 120px !important;
  flex: none !important;
  padding: var(--spacing-sm) var(--spacing-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
}

.separator {
  color: var(--color-text-tertiary);
  font-size: 14px;
}

.word-unit {
  color: var(--color-text-tertiary);
  font-size: 14px;
}

.filter-actions {
  display: flex;
  justify-content: center;
  gap: var(--spacing-lg);
  margin-top: var(--spacing-xl);
  padding-top: var(--spacing-xl);
  border-top: 1px solid var(--color-border);
}

.apply-btn,
.reset-btn {
  padding: var(--spacing-sm) var(--spacing-xl);
  border: none;
  border-radius: var(--radius-lg);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-base);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.apply-btn {
  background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-light) 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(214, 158, 46, 0.3);
}

.apply-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(214, 158, 46, 0.4);
}

.reset-btn {
  background: var(--color-bg-tertiary);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}

.reset-btn:hover {
  background: var(--color-text-tertiary);
  color: white;
  transform: translateY(-2px);
}

/* 搜索结果 */
.search-results {
  margin-top: var(--spacing-2xl);
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-xl);
  padding-bottom: var(--spacing-md);
  border-bottom: 2px solid var(--color-border);
}

.results-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
  font-family: var(--font-serif);
}

.results-count {
  color: var(--color-text-tertiary);
  font-size: 14px;
  font-weight: 500;
}

.novel-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: var(--spacing-lg);
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--spacing-md);
  margin-top: var(--spacing-2xl);
  padding: var(--spacing-xl) 0;
}

.page-btn {
  padding: var(--spacing-sm) var(--spacing-md);
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  transition: all var(--transition-fast);
}

.page-btn:hover:not(:disabled) {
  border-color: var(--color-accent);
  color: var(--color-accent);
  background: linear-gradient(135deg, rgba(214, 158, 46, 0.05) 0%, transparent 100%);
  transform: translateY(-2px);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 14px;
  color: var(--color-text-secondary);
  font-weight: 600;
  padding: 0 var(--spacing-md);
}

.empty-state, .loading-state {
  text-align: center;
  padding: var(--spacing-2xl);
  color: var(--color-text-tertiary);
  min-height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-secondary);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  border: 1px solid var(--color-border);
}

.empty-state p:first-child {
  font-size: 18px;
  margin-bottom: var(--spacing-sm);
  color: var(--color-text-secondary);
  font-family: var(--font-serif);
}

.empty-tip {
  font-size: 14px;
  color: var(--color-text-tertiary);
}

.loading-state p {
  font-size: 16px;
  color: var(--color-text-secondary);
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .search {
    padding: var(--spacing-md);
  }
  
  .search-header h1 {
    font-size: 28px;
  }
  
  .novel-list {
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  }
}

@media (max-width: 768px) {
  .search {
    padding: var(--spacing-sm);
  }
  
  .search-header {
    padding: var(--spacing-xl) var(--spacing-md);
  }
  
  .search-header h1 {
    font-size: 24px;
  }
  
  .novel-list {
    grid-template-columns: 1fr;
  }
  
  .filter-row {
    flex-direction: column;
  }
  
  .filter-item {
    min-width: 100%;
  }
  
  .search-box-container {
    max-width: 100%;
  }
  
  .search-box {
    flex-direction: column;
  }
  
  .search-box button {
    padding: var(--spacing-md);
    width: 100%;
  }
}
</style>
