<script setup>
/**
 * Category.vue - 小说分类页面组件
 *
 * 该组件实现了小说分类浏览功能，包括：
 * - 分类导航展示
 * - 按分类获取小说列表
 * - 分页显示
 * - 页码跳转
 */
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NovelCard from '../components/NovelCard.vue'
import { novelApi, categoryApi } from '../api/modules/novel.js'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const novels = ref([])
const categories = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const jumpPage = ref(1)

const currentCategoryId = computed(() => route.params.id || 'xuanhuan')

const currentCategoryInfo = computed(() => {
  console.log('查找分类 - currentCategoryId:', currentCategoryId.value)
  console.log('查找分类 - 分类列表:', categories.value)
  const cat = categories.value.find(c => c.id === currentCategoryId.value)
  console.log('查找分类 - 找到的分类:', cat)
  return cat || { name: '全部', icon: '📚', novelCount: 0 }
})

const currentCategory = computed(() => currentCategoryInfo.value.name)
const currentIcon = computed(() => currentCategoryInfo.value.icon)
const currentCategoryCount = computed(() => currentCategoryInfo.value.novelCount)

watch(currentCategoryId, async () => {
  console.log('分类ID变化，重新获取小说数据:', currentCategoryId.value)
  await fetchNovels()
})

const filteredNovels = computed(() => {
  console.log('过滤小说 - currentCategoryId:', currentCategoryId.value)
  console.log('过滤小说 - currentCategory:', currentCategory.value)
  console.log('过滤小说 - 所有小说数量:', novels.value.length)
  console.log(
    '过滤小说 - 小说分类示例:',
    novels.value.slice(0, 5).map(n => n.category)
  )

  if (currentCategory.value === '全部') {
    return novels.value
  }
  const filtered = novels.value.filter(novel => {
    const match = novel.category === currentCategory.value
    console.log(
      `小说 ${novel.title} 分类: "${novel.category}" vs "${currentCategory.value}" = ${match}`
    )
    return match
  })
  console.log('过滤结果数量:', filtered.length)
  console.log('过滤结果:', filtered)
  return filtered
})

const paginatedNovels = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredNovels.value.slice(start, end)
})

const totalPages = computed(() => {
  return Math.ceil(filteredNovels.value.length / pageSize.value)
})

const displayedPages = computed(() => {
  const total = totalPages.value
  const current = currentPage.value
  const maxDisplay = 10

  if (total <= maxDisplay) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }

  let start = Math.max(1, current - Math.floor(maxDisplay / 2))
  let end = start + maxDisplay - 1

  if (end > total) {
    end = total
    start = Math.max(1, end - maxDisplay + 1)
  }

  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
})

/**
 * 处理页码跳转
 */
const handleJump = () => {
  const page = parseInt(jumpPage.value)
  if (page >= 1 && page <= totalPages.value) {
    goToPage(page)
    jumpPage.value = 1
  } else {
    ElMessage.warning(`请输入1-${totalPages.value}之间的页码`)
  }
}

const fetchNovels = async () => {
  loading.value = true
  console.log('开始获取小说数据...')
  try {
    // 直接调用按分类获取小说的API
    console.log('获取分类小说 - currentCategoryId:', currentCategoryId.value)
    console.log('获取分类小说 - currentCategory:', currentCategory.value)
    console.log('获取分类小说 - currentCategoryInfo:', currentCategoryInfo.value)

    // 构建分类名称映射
    const idToCategoryMap = {
      xuanhuan: '玄幻',
      lishi: '历史',
      wuxia: '武侠',
      xianxia: '仙侠',
      kehuan: '科幻',
      dushi: '都市',
      youxi: '游戏',
      qita: '其他'
    }

    // 获取实际的分类名称
    const categoryName = idToCategoryMap[currentCategoryId.value] || currentCategory.value
    console.log('实际请求的分类名称:', categoryName)

    console.log('调用API获取小说数据...')
    const response = await novelApi.getNovelsByCategory(categoryName)
    console.log('获取小说列表响应:', response)

    // 检查响应格式
    if (Array.isArray(response)) {
      // 响应是直接的小说数组
      console.log('获取到的小说数量:', response.length)
      console.log('小说数据示例:', response.slice(0, 3))
      console.log(
        '小说分类示例:',
        response.slice(0, 3).map(n => n.category)
      )
      novels.value = response.map(novel => ({
        id: novel.id,
        title: novel.title,
        author: novel.author,
        description: novel.description,
        cover: novel.coverImage
          ? `/api/novels/${novel.id}/cover`
          : 'https://via.placeholder.com/150x200',
        category: novel.category,
        status: novel.status
      }))
      console.log('转换后的小说数据:', novels.value.slice(0, 3))
      console.log('转换后的小说数量:', novels.value.length)
    } else if (response.code === 200) {
      // 响应是包含code和data属性的对象
      console.log('获取到的小说数量:', response.data.length)
      console.log('小说数据示例:', response.data.slice(0, 3))
      console.log(
        '小说分类示例:',
        response.data.slice(0, 3).map(n => n.category)
      )
      novels.value = response.data.map(novel => ({
        id: novel.id,
        title: novel.title,
        author: novel.author,
        description: novel.description,
        cover: novel.coverImage
          ? `/api/novels/${novel.id}/cover`
          : 'https://via.placeholder.com/150x200',
        category: novel.category,
        status: novel.status
      }))
      console.log('转换后的小说数据:', novels.value.slice(0, 3))
      console.log('转换后的小说数量:', novels.value.length)
    } else {
      console.error('API返回错误:', response.message)
      ElMessage.error(response.message || '获取小说列表失败')
    }
  } catch (error) {
    console.error('获取小说列表失败:', error)
    console.error('错误详情:', error.message)
    novels.value = [
      {
        id: 1,
        title: '斗破苍穹',
        author: '天蚕土豆',
        description: '这里是天才云集的世界...',
        cover: 'https://via.placeholder.com/150x200',
        category: '玄幻',
        status: '完结'
      },
      {
        id: 2,
        title: '庆余年',
        author: '猫腻',
        description: '一个现代青年穿越到古代...',
        cover: 'https://via.placeholder.com/150x200',
        category: '历史',
        status: '完结'
      },
      {
        id: 3,
        title: '赘婿',
        author: '愤怒的香蕉',
        description: '一个现代金融界巨头...',
        cover: 'https://via.placeholder.com/150x200',
        category: '历史',
        status: '连载中'
      },
      {
        id: 4,
        title: '凡人修仙传',
        author: '忘语',
        description: '一个普通山村小子...',
        cover: 'https://via.placeholder.com/150x200',
        category: '仙侠',
        status: '完结'
      },
      {
        id: 5,
        title: '雪中悍刀行',
        author: '烽火戏诸侯',
        description: '江湖与庙堂的纷争...',
        cover: 'https://via.placeholder.com/150x200',
        category: '武侠',
        status: '完结'
      },
      {
        id: 6,
        title: '诡秘之主',
        author: '爱潜水的乌贼',
        description: '蒸汽与机械的浪潮...',
        cover: 'https://via.placeholder.com/150x200',
        category: '玄幻',
        status: '完结'
      }
    ]
  } finally {
    loading.value = false
    console.log('获取小说数据完成')
  }
}

const fetchCategories = async () => {
  try {
    const response = await categoryApi.getCategories()
    console.log('获取分类列表响应:', response)

    // 检查响应格式
    let categoryData
    if (Array.isArray(response)) {
      // 响应是直接的分类数组
      categoryData = response
    } else if (response.code === 200) {
      // 响应是包含code和data属性的对象
      categoryData = response.data
    } else {
      console.error('API返回错误:', response.message)
      ElMessage.error(response.message || '获取分类列表失败')
      return
    }

    console.log('获取到的分类数据:', categoryData)
    const categoryMap = {
      玄幻: 'xuanhuan',
      历史: 'lishi',
      武侠: 'wuxia',
      仙侠: 'xianxia',
      科幻: 'kehuan',
      都市: 'dushi',
      游戏: 'youxi',
      其他: 'qita'
    }
    console.log('分类映射表:', categoryMap)
    categories.value = categoryData.map(cat => ({
      id: categoryMap[cat.name] || cat.name.toLowerCase(),
      name: cat.name,
      icon: cat.icon,
      novelCount: cat.novelCount
    }))
    console.log('转换后的分类数据:', categories.value)
    // 打印每个分类的ID和名称
    categories.value.forEach(cat => {
      console.log(`分类: ID=${cat.id}, 名称=${cat.name}, 计数=${cat.novelCount}`)
    })
  } catch (error) {
    console.error('获取分类列表失败:', error)
  }
}

const selectCategory = categoryId => {
  router.push(`/category/${categoryId}`)
  currentPage.value = 1
}

const goToPage = page => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

const goBack = () => {
  router.push('/')
}

onMounted(async () => {
  console.log('开始获取数据...')
  await fetchCategories()
  console.log('分类数据获取完成')
  await fetchNovels()
  console.log('小说数据获取完成')
})

// 监听分类ID变化，重新获取小说数据
watch(currentCategoryId, async () => {
  console.log('分类ID变化，重新获取小说数据:', currentCategoryId.value)
  await fetchNovels()
})
</script>

<template>
  <div class="category-page">
    <!-- 分类导航 -->
    <div class="category-nav-section">
      <h2 class="nav-title">📚 小说分类</h2>
      <div class="category-nav">
        <div
          v-for="category in categories"
          :key="category.id"
          class="category-item"
          :class="{ active: category.id === currentCategoryId }"
          @click="selectCategory(category.id)"
        >
          <span class="category-icon">{{ category.icon }}</span>
          <span class="category-name">{{ category.name }}</span>
        </div>
      </div>
    </div>

    <!-- 当前分类内容 -->
    <div class="category-content">
      <div class="category-header">
        <div class="category-title">
          <span class="title-icon">{{ currentIcon }}</span>
          <h1>{{ currentCategory }}小说</h1>
        </div>
        <p class="category-desc">共 {{ currentCategoryCount }} 本{{ currentCategory }}类小说</p>
      </div>

      <!-- 小说列表 -->
      <div v-if="loading" class="loading">
        <p>加载中...</p>
      </div>
      <div v-else-if="paginatedNovels.length > 0" class="novel-list">
        <NovelCard v-for="novel in paginatedNovels" :key="novel.id" :novel="novel" />
      </div>
      <div v-else class="empty-state">
        <p>暂无{{ currentCategory }}类小说</p>
        <button class="btn-browse" @click="goBack">返回首页</button>
      </div>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="pagination">
        <button class="page-btn" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">
          上一页
        </button>
        <div class="page-numbers">
          <button
            v-for="page in displayedPages"
            :key="page"
            class="page-number"
            :class="{ active: page === currentPage }"
            @click="goToPage(page)"
          >
            {{ page }}
          </button>
        </div>
        <button
          class="page-btn"
          :disabled="currentPage === totalPages"
          @click="goToPage(currentPage + 1)"
        >
          下一页
        </button>

        <!-- 跳转到指定页 -->
        <div class="page-jump">
          <span class="jump-text">共 {{ totalPages }} 页，跳至</span>
          <input
            v-model.number="jumpPage"
            type="number"
            class="jump-input"
            :min="1"
            :max="totalPages"
            @keyup.enter="handleJump"
          />
          <span class="jump-text">页</span>
          <button class="jump-btn" @click="handleJump">GO</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.category-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--spacing-xl);
  min-height: calc(100vh - 140px);
  width: 100%;
  flex: 1;
  position: relative;
}

.category-page::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  width: 4px;
  height: 100vh;
  background: linear-gradient(180deg, var(--color-primary) 0%, var(--color-accent) 100%);
  z-index: 100;
}

/* 分类导航区域 */
.category-nav-section {
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  border-radius: var(--radius-xl);
  padding: var(--spacing-2xl);
  margin-bottom: var(--spacing-2xl);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--color-border);
  position: relative;
  overflow: hidden;
}

.category-nav-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--color-primary) 0%, var(--color-accent) 100%);
}

.nav-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-xl);
  text-align: center;
  font-family: var(--font-serif);
  letter-spacing: -0.5px;
  position: relative;
}

.nav-title::after {
  content: '';
  position: absolute;
  bottom: -12px;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 3px;
  background: linear-gradient(90deg, var(--color-primary) 0%, var(--color-accent) 100%);
  border-radius: 2px;
}

.category-nav {
  display: flex;
  justify-content: center;
  gap: var(--spacing-lg);
  flex-wrap: wrap;
  margin-top: var(--spacing-lg);
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--spacing-xl) var(--spacing-2xl);
  cursor: pointer;
  transition: all var(--transition-base);
  border-radius: var(--radius-lg);
  border: 2px solid transparent;
  background: var(--color-bg-secondary);
  box-shadow: var(--shadow-sm);
  position: relative;
  overflow: hidden;
  min-width: 120px;
}

.category-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(26, 54, 93, 0.05) 0%, rgba(214, 158, 46, 0.05) 100%);
  opacity: 0;
  transition: opacity var(--transition-base);
}

.category-item:hover::before {
  opacity: 1;
}

.category-item:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-xl);
  border-color: var(--color-primary);
}

.category-item.active {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  border-color: var(--color-primary);
  box-shadow: 0 8px 24px rgba(26, 54, 93, 0.3);
  transform: translateY(-4px);
}

.category-item.active .category-name {
  color: white;
}

.category-item.active .category-icon {
  transform: scale(1.1);
}

.category-icon {
  font-size: 42px;
  margin-bottom: var(--spacing-sm);
  transition: transform var(--transition-base);
  position: relative;
  z-index: 1;
}

.category-name {
  font-size: 16px;
  color: var(--color-text-primary);
  font-weight: 600;
  transition: all var(--transition-base);
  position: relative;
  z-index: 1;
}

/* 分类内容区域 */
.category-content {
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  border-radius: var(--radius-xl);
  padding: var(--spacing-2xl);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--color-border);
  position: relative;
  overflow: hidden;
}

.category-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--color-accent) 0%, var(--color-accent-light) 100%);
}

.category-header {
  margin-bottom: var(--spacing-2xl);
  padding-bottom: var(--spacing-xl);
  border-bottom: 2px solid var(--color-border);
  text-align: center;
}

.category-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-sm);
}

.title-icon {
  font-size: 42px;
  filter: drop-shadow(0 2px 8px rgba(214, 158, 46, 0.3));
}

.category-title h1 {
  font-size: 36px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
  font-family: var(--font-serif);
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-accent) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.category-desc {
  color: var(--color-text-secondary);
  font-size: 16px;
  margin: 0;
  font-weight: 500;
}

.loading {
  text-align: center;
  padding: var(--spacing-2xl);
  font-size: 18px;
  color: var(--color-text-tertiary);
  font-weight: 500;
}

.novel-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-2xl);
}

.empty-state {
  text-align: center;
  padding: var(--spacing-2xl) var(--spacing-xl);
}

.empty-state p {
  font-size: 18px;
  color: var(--color-text-tertiary);
  margin-bottom: var(--spacing-xl);
  font-weight: 500;
}

.btn-browse {
  padding: var(--spacing-md) var(--spacing-xl);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: white;
  border: none;
  border-radius: var(--radius-lg);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-base);
  box-shadow: 0 4px 12px rgba(26, 54, 93, 0.3);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.btn-browse:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(26, 54, 93, 0.4);
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--spacing-md);
  margin-top: var(--spacing-2xl);
  padding-top: var(--spacing-xl);
  border-top: 2px solid var(--color-border);
}

.page-btn {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 15px;
  font-weight: 500;
  transition: all var(--transition-base);
  box-shadow: var(--shadow-sm);
}

.page-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: white;
  border-color: var(--color-primary);
  box-shadow: 0 4px 12px rgba(26, 54, 93, 0.3);
  transform: translateY(-2px);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  gap: var(--spacing-sm);
}

.page-number {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 15px;
  font-weight: 500;
  transition: all var(--transition-base);
  box-shadow: var(--shadow-sm);
}

.page-number:hover {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: white;
  border-color: var(--color-primary);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(26, 54, 93, 0.3);
}

.page-number.active {
  background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-light) 100%);
  color: white;
  border-color: var(--color-accent);
  box-shadow: 0 4px 12px rgba(214, 158, 46, 0.4);
  font-weight: 600;
}

/* 页码跳转 */
.page-jump {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  margin-left: var(--spacing-md);
  padding-left: var(--spacing-md);
  border-left: 2px solid var(--color-border);
}

.jump-text {
  font-size: 14px;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.jump-input {
  width: 60px;
  height: 40px;
  text-align: center;
  border: 2px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 500;
  outline: none;
  transition: all var(--transition-base);
  background: var(--color-bg-secondary);
  color: var(--color-text-primary);
}

.jump-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(26, 54, 93, 0.1);
}

.jump-btn {
  padding: var(--spacing-sm) var(--spacing-md);
  background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-light) 100%);
  color: white;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all var(--transition-base);
  box-shadow: 0 4px 12px rgba(214, 158, 46, 0.3);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.jump-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(214, 158, 46, 0.4);
}

@media (max-width: 768px) {
  .category-page {
    padding: var(--spacing-md);
  }

  .category-nav-section {
    padding: var(--spacing-lg);
  }

  .nav-title {
    font-size: 22px;
  }

  .category-nav {
    gap: var(--spacing-sm);
  }

  .category-item {
    padding: var(--spacing-md) var(--spacing-lg);
    min-width: 100px;
  }

  .category-icon {
    font-size: 32px;
  }

  .category-name {
    font-size: 14px;
  }

  .category-content {
    padding: var(--spacing-lg);
  }

  .category-title h1 {
    font-size: 24px;
  }

  .title-icon {
    font-size: 32px;
  }

  .category-desc {
    font-size: 14px;
  }

  .novel-list {
    grid-template-columns: 1fr;
  }

  .pagination {
    flex-wrap: wrap;
    gap: var(--spacing-sm);
  }

  .page-numbers {
    order: -1;
    width: 100%;
    justify-content: center;
  }

  .page-jump {
    order: 2;
    width: 100%;
    justify-content: center;
    margin-left: 0;
    padding-left: 0;
    border-left: none;
    border-top: 1px solid var(--color-border);
    padding-top: var(--spacing-sm);
    margin-top: var(--spacing-sm);
  }
}
</style>
