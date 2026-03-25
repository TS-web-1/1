<script setup>
/**
 * AuthorChapterManage.vue - 作者章节管理页面组件
 *
 * 该组件实现了作者对作品章节的管理功能，包括：
 * - 章节列表展示
 * - 创建新章节
 * - 编辑已有章节
 * - 删除章节
 * - 发布/取消发布章节
 */
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { authorApi } from '@/api/modules/author.js'

const route = useRoute()
const router = useRouter()
const novelId = route.params.novelId

const novel = ref({
  title: '',
  totalChapters: 0
})

const chapters = ref([])
const showEditModal = ref(false)
const editingChapter = ref(null)
const editForm = ref({
  title: '',
  content: '',
  isPublish: true
})

/**
 * 加载小说基本信息
 */
const loadNovelInfo = async () => {
  try {
    const response = await authorApi.getAuthorNovels()
    if (response.code === 200) {
      const novels = response.data || []
      const currentNovel = novels.find(n => n.id == novelId)
      if (currentNovel) {
        novel.value.title = currentNovel.title
      }
    }
  } catch (error) {
    console.error('加载小说信息失败:', error)
  }
}

/**
 * 加载章节列表
 */
const loadChapters = async () => {
  try {
    const response = await authorApi.getNovelChapters(novelId)
    if (response.code === 200) {
      chapters.value = response.data || []
      novel.value.totalChapters = chapters.value.length
    } else {
      ElMessage.error(response.message || '加载章节失败')
    }
  } catch (error) {
    console.error('加载章节失败:', error)
    ElMessage.error('加载章节失败，请稍后重试')
  }
}

/**
 * 获取章节标题
 * @param {number} num - 章节序号
 * @returns {string} 章节标题
 */
const getChapterTitle = num => {
  const titles = [
    '初入江湖',
    '意外收获',
    '突破瓶颈',
    '强敌来袭',
    '生死一战',
    '奇遇连连',
    '师傅教诲',
    '下山历练',
    '结识好友',
    '共闯难关'
  ]
  return titles[num % titles.length] || '新的篇章'
}

/**
 * 创建新章节
 */
const createChapter = () => {
  editingChapter.value = null
  editForm.value = {
    title: '',
    content: '',
    isPublish: true
  }
  showEditModal.value = true
}

/**
 * 编辑章节
 * @param {Object} chapter - 章节对象
 */
const editChapter = chapter => {
  editingChapter.value = chapter
  editForm.value = {
    title: chapter.title.replace(`第${chapter.chapterNumber}章 `, ''),
    content: '这是章节内容...',
    isPublish: true
  }
  showEditModal.value = true
}

// 保存章节
const saveChapter = async () => {
  if (!editForm.value.title.trim()) {
    ElMessage.warning('请输入章节标题')
    return
  }

  try {
    const chapterData = {
      title: editForm.value.title,
      content: editForm.value.content,
      wordCount: editForm.value.content.length
    }

    // 如果是被退回修改的章节（PENDING且有审核意见），重新提交审核
    const isResubmit =
      editingChapter.value &&
      editingChapter.value.reviewStatus === 'PENDING' &&
      editingChapter.value.reviewComment

    if (editingChapter.value) {
      // 更新现有章节
      const response = await authorApi.updateChapter(editingChapter.value.id, chapterData)
      if (response.code === 200) {
        if (isResubmit) {
          ElMessage.success('章节已修改并重新提交审核')
        } else {
          ElMessage.success('章节更新成功')
        }
        await loadChapters() // 重新加载章节列表
      } else {
        ElMessage.error(response.message || '更新失败')
      }
    } else {
      // 创建新章节
      const response = await authorApi.addChapter(novelId, chapterData)
      if (response.code === 200) {
        ElMessage.success('章节创建成功')
        await loadChapters() // 重新加载章节列表
      } else {
        ElMessage.error(response.message || '创建失败')
      }
    }

    showEditModal.value = false
  } catch (error) {
    console.error('保存章节失败:', error)
    ElMessage.error('保存失败，请稍后重试')
  }
}

// 删除章节
const deleteChapter = async chapter => {
  try {
    await ElMessageBox.confirm(`确定要删除"${chapter.title}"吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const response = await authorApi.deleteChapter(chapter.id)
    if (response.code === 200) {
      ElMessage.success('章节已删除')
      await loadChapters() // 重新加载章节列表
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除章节失败:', error)
      ElMessage.error('删除失败，请稍后重试')
    }
  }
}

// 调整章节顺序
const moveChapter = (chapter, direction) => {
  const index = chapters.value.findIndex(c => c.id === chapter.id)
  if (direction === 'up' && index > 0) {
    ;[chapters.value[index], chapters.value[index - 1]] = [
      chapters.value[index - 1],
      chapters.value[index]
    ]
  } else if (direction === 'down' && index < chapters.value.length - 1) {
    ;[chapters.value[index], chapters.value[index + 1]] = [
      chapters.value[index + 1],
      chapters.value[index]
    ]
  }
  // 重新编号
  chapters.value.forEach((c, i) => {
    c.chapterNumber = i + 1
  })
}

// 保存到草稿箱
const saveToDraft = async () => {
  if (!editForm.value.title.trim() && !editForm.value.content.trim()) {
    ElMessage.warning('请输入标题或内容后再保存')
    return
  }

  try {
    const draftData = {
      novelId,
      title: editForm.value.title || `第${chapters.value.length + 1}章`,
      content: editForm.value.content,
      wordCount: editForm.value.content.length
    }

    const response = await authorApi.saveDraft(draftData)
    if (response.code === 200) {
      ElMessage.success('已保存到草稿箱')
    } else {
      ElMessage.error(response.message || '保存草稿失败')
    }
  } catch (error) {
    console.error('保存草稿失败:', error)
    ElMessage.error('保存草稿失败，请稍后重试')
  }
}

// 自动保存
let autoSaveTimer = null
const startAutoSave = () => {
  autoSaveTimer = setInterval(() => {
    if (editForm.value.content || editForm.value.title) {
      console.log('自动保存:', editForm.value)
    }
  }, 30000) // 30秒自动保存
}

// 字数统计
const wordCount = computed(() => {
  return editForm.value.content.length
})

// 格式化时间
const formatTime = date => {
  return new Date(date).toLocaleDateString()
}

onMounted(() => {
  loadNovelInfo()
  loadChapters()
  startAutoSave()

  // 检查是否有从草稿跳转过来的参数
  const draftId = route.query.draftId
  const draftTitle = route.query.draftTitle
  const draftContent = route.query.draftContent

  if (draftId) {
    // 从草稿跳转过来，自动打开编辑框并填充内容
    editingChapter.value = null
    editForm.value = {
      title: draftTitle || '',
      content: draftContent || '',
      isPublish: true,
      draftId // 保存草稿ID，发布后可以删除草稿
    }
    showEditModal.value = true
    ElMessage.info('已加载草稿内容')
  }
})
</script>

<template>
  <div class="chapter-manage">
    <div class="manage-header">
      <div class="header-left">
        <button class="back-btn" @click="$router.back()">← 返回</button>
        <h1>《{{ novel.title }}》- 章节管理</h1>
      </div>
      <button class="create-btn" @click="createChapter">
        <span class="btn-icon">+</span> 新建章节
      </button>
    </div>

    <div class="stats-bar">
      <span>共 {{ novel.totalChapters }} 章</span>
      <span>总字数 {{ chapters.reduce((sum, c) => sum + c.wordCount, 0).toLocaleString() }}</span>
    </div>

    <div class="chapters-list">
      <div v-for="chapter in chapters" :key="chapter.id" class="chapter-item">
        <div class="chapter-number">{{ chapter.chapterNumber }}</div>
        <div class="chapter-info">
          <h4 class="chapter-title">{{ chapter.title }}</h4>
          <div class="chapter-meta">
            <span>{{ chapter.wordCount }}字</span>
            <span>👁 {{ chapter.views }}</span>
            <span>{{ formatTime(chapter.createdAt) }}</span>
            <span v-if="chapter.isVip" class="vip-badge">VIP</span>
            <span :class="['review-status', chapter.reviewStatus?.toLowerCase()]">
              {{
                chapter.reviewStatus === 'PENDING'
                  ? chapter.reviewComment
                    ? '需修改'
                    : '审核中'
                  : chapter.reviewStatus === 'APPROVED'
                    ? '已通过'
                    : '未通过'
              }}
            </span>
          </div>
          <div v-if="chapter.reviewComment" class="review-comment">
            <span class="comment-label">{{
              chapter.reviewStatus === 'PENDING' ? '修改意见：' : '未通过原因：'
            }}</span>
            <span class="comment-content">{{ chapter.reviewComment }}</span>
          </div>
        </div>
        <div class="chapter-actions">
          <button
            class="action-btn"
            :disabled="chapter.chapterNumber === 1"
            @click="moveChapter(chapter, 'up')"
            >↑</button
          >
          <button
            class="action-btn"
            :disabled="chapter.chapterNumber === chapters.length"
            @click="moveChapter(chapter, 'down')"
            >↓</button
          >
          <button class="action-btn primary" @click="editChapter(chapter)">编辑</button>
          <button class="action-btn danger" @click="deleteChapter(chapter)">删除</button>
        </div>
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <div v-if="showEditModal" class="modal-overlay" @click.self="showEditModal = false">
      <div class="modal-content modal-large">
        <div class="modal-header">
          <h2>{{ editingChapter ? '编辑章节' : '新建章节' }}</h2>
          <button class="close-btn" @click="showEditModal = false">×</button>
        </div>
        <div class="edit-form">
          <div class="form-group">
            <label>章节标题</label>
            <input
              v-model="editForm.title"
              type="text"
              :placeholder="`第${editingChapter ? editingChapter.chapterNumber : chapters.length + 1}章`"
            />
          </div>
          <div class="form-group">
            <label>章节内容</label>
            <textarea
              v-model="editForm.content"
              placeholder="在此输入章节内容..."
              rows="20"
            ></textarea>
            <div class="editor-toolbar">
              <span class="word-count">字数：{{ wordCount }}</span>
              <button class="toolbar-btn" @click="saveToDraft">保存草稿</button>
            </div>
          </div>
          <div class="form-actions">
            <button class="btn-cancel" @click="showEditModal = false">取消</button>
            <button class="btn-save" @click="saveChapter">
              {{ editingChapter ? '保存修改' : '发布章节' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chapter-manage {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--spacing-xl);
}

.manage-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-xl);
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.back-btn {
  padding: 8px 16px;
  border: 1px solid var(--color-border);
  background: rgba(255, 255, 255, 0.05);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.3s;
  color: var(--color-text-primary);
}

.back-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.manage-header h1 {
  font-size: 22px;
  color: var(--color-text-primary);
  font-weight: 600;
}

.create-btn {
  padding: 12px 24px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: #0d1117;
  border: none;
  border-radius: var(--radius-lg);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(201, 169, 98, 0.3);
}

.stats-bar {
  display: flex;
  gap: 30px;
  padding: var(--spacing-md) var(--spacing-lg);
  background: rgba(255, 255, 255, 0.03);
  border-radius: var(--radius-lg);
  margin-bottom: var(--spacing-xl);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}

.chapters-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.chapter-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-lg) var(--spacing-xl);
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  border: 1px solid var(--color-border);
  transition: all 0.3s;
}

.chapter-item:hover {
  transform: translateX(5px);
  border-color: var(--color-primary);
}

.chapter-number {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: #0d1117;
  border-radius: 50%;
  font-weight: bold;
}

.chapter-info {
  flex: 1;
}

.chapter-title {
  font-size: 16px;
  color: var(--color-text-primary);
  margin-bottom: 5px;
}

.chapter-meta {
  display: flex;
  gap: 15px;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.vip-badge {
  background: linear-gradient(135deg, #ffd700 0%, #ffb700 100%);
  color: #8b6914;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}

.chapter-actions {
  display: flex;
  gap: var(--spacing-sm);
}

.action-btn {
  padding: 6px 12px;
  border: 1px solid var(--color-border);
  background: rgba(255, 255, 255, 0.05);
  border-radius: var(--radius-sm);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
  color: var(--color-text-primary);
}

.action-btn:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-btn.primary {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: #0d1117;
  border-color: var(--color-primary);
  font-weight: 600;
}

.action-btn.primary:hover {
  transform: translateY(-1px);
}

.action-btn.danger {
  color: #f87171;
  border-color: rgba(248, 113, 113, 0.3);
}

.action-btn.danger:hover {
  background: rgba(248, 113, 113, 0.1);
  border-color: #f87171;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: var(--color-bg-secondary);
  border-radius: var(--radius-xl);
  width: 90%;
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
  border: 1px solid var(--color-border);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--spacing-xl);
  border-bottom: 1px solid var(--color-border);
}

.modal-header h2 {
  margin: 0;
  font-size: 20px;
  color: var(--color-text-primary);
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: color 0.3s;
}

.close-btn:hover {
  color: var(--color-primary);
}

.edit-form {
  padding: var(--spacing-xl);
}

.form-group {
  margin-bottom: var(--spacing-lg);
}

.form-group label {
  display: block;
  margin-bottom: var(--spacing-sm);
  font-size: 14px;
  color: var(--color-text-primary);
  font-weight: 500;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  font-family: inherit;
  background: rgba(255, 255, 255, 0.03);
  color: var(--color-text-primary);
  transition: all 0.3s;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(201, 169, 98, 0.1);
}

.form-group input::placeholder,
.form-group textarea::placeholder {
  color: var(--color-text-tertiary);
}

.form-group textarea {
  resize: vertical;
  min-height: 300px;
  line-height: 1.8;
}

.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  background: rgba(255, 255, 255, 0.03);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}

.word-count {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.toolbar-btn {
  padding: 6px 12px;
  border: 1px solid var(--color-border);
  background: rgba(255, 255, 255, 0.05);
  border-radius: var(--radius-sm);
  font-size: 13px;
  cursor: pointer;
  color: var(--color-text-primary);
  transition: all 0.3s;
}

.toolbar-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-md);
  padding-top: var(--spacing-xl);
  border-top: 1px solid var(--color-border);
}

.btn-cancel {
  padding: 10px 24px;
  border: 1px solid var(--color-border);
  background: rgba(255, 255, 255, 0.05);
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--color-text-primary);
  transition: all 0.3s;
}

.btn-cancel:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.btn-save {
  padding: 10px 24px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: #0d1117;
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s;
}

.btn-save:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(201, 169, 98, 0.3);
}

@media (max-width: 768px) {
  .chapter-item {
    flex-wrap: wrap;
  }

  .chapter-actions {
    width: 100%;
    justify-content: flex-end;
    margin-top: var(--spacing-sm);
  }
}

.review-status {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
}

.review-status.pending {
  background: rgba(245, 158, 11, 0.15);
  color: #fbbf24;
  border: 1px solid rgba(245, 158, 11, 0.3);
}

.review-status.approved {
  background: rgba(34, 197, 94, 0.15);
  color: #4ade80;
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.review-status.rejected {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.review-comment {
  margin-top: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  background: rgba(239, 68, 68, 0.1);
  border-left: 3px solid #f87171;
  border-radius: var(--radius-sm);
  font-size: 13px;
}

.comment-label {
  font-weight: 500;
  color: #f87171;
  margin-right: 5px;
}

.comment-content {
  color: var(--color-text-secondary);
  line-height: 1.4;
}
</style>
