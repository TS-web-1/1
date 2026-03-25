<script setup>
/**
 * AuthorNovelEdit.vue - 作者作品编辑页面组件
 * 
 * 该组件实现了作品的创建和编辑功能，包括：
 * - 作品标题、作者、简介输入
 * - 封面图片上传
 * - 分类和状态选择
 * - 新建/编辑模式切换
 */
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authorApi } from '../api/modules/author.js'

const route = useRoute()
const router = useRouter()
const novelId = route.params.id
const isEdit = !!novelId

const novel = ref({
  title: '',
  author: '',
  description: '',
  cover: '',
  category: '玄幻',
  status: '连载中'
})

const categories = ['玄幻', '仙侠', '都市', '历史', '科幻', '悬疑', '言情', '其他']
const statuses = ['连载中', '已完结']

/**
 * 加载作品信息（编辑模式）
 */
const loadNovel = async () => {
  if (isEdit) {
    try {
      const response = await authorApi.getNovelById(novelId)
      if (response.code === 200 && response.data) {
        const data = response.data
        novel.value = {
          title: data.title || '',
          author: data.author || '',
          description: data.description || '',
          cover: data.coverImage || data.cover || '',
          category: data.category || '玄幻',
          status: data.status === 'COMPLETED' ? '已完结' : '连载中'
        }
      } else {
        ElMessage.error('加载作品信息失败')
        router.push('/author')
      }
    } catch (error) {
      console.error('加载作品信息失败:', error)
      ElMessage.error('加载作品信息失败')
      router.push('/author')
    }
  }
}

/**
 * 保存作品
 */
const saveNovel = async () => {
  if (!novel.value.title.trim()) {
    ElMessage.warning('请输入作品名称')
    return
  }
  
  if (!novel.value.description.trim()) {
    ElMessage.warning('请输入作品简介')
    return
  }
  
  try {
    // 直接使用封面数据（支持 base64 或 URL）
    const coverUrl = novel.value.cover || ''
    
    const novelData = {
      title: novel.value.title,
      description: novel.value.description,
      coverImage: coverUrl,
      category: novel.value.category,
      status: novel.value.status === '连载中' ? 'ONGOING' : 'COMPLETED'
    }
    
    let response
    if (isEdit) {
      response = await authorApi.updateNovel(novelId, novelData)
    } else {
      response = await authorApi.publishNovel(novelData)
    }
    
    if (response.code === 200) {
      ElMessage.success(isEdit ? '作品更新成功' : '作品创建成功')
      router.push('/author')
    } else {
      ElMessage.error(response.message || '保存失败')
    }
  } catch (error) {
    console.error('保存作品失败:', error)
    ElMessage.error('保存失败，请稍后重试')
  }
}

// 取消
const cancel = () => {
  router.back()
}

// 处理封面上传
const handleCoverUpload = (event) => {
  const file = event.target.files[0]
  if (file) {
    const reader = new FileReader()
    reader.onload = (e) => {
      novel.value.cover = e.target.result
    }
    reader.readAsDataURL(file)
  }
}

onMounted(() => {
  loadNovel()
})
</script>

<template>
  <div class="novel-edit">
    <div class="edit-container">
      <h1>{{ isEdit ? '编辑作品' : '创建新作品' }}</h1>
      
      <form @submit.prevent="saveNovel" class="edit-form">
        <div class="form-main">
          <div class="form-group">
            <label>作品名称 <span class="required">*</span></label>
            <input 
              v-model="novel.title" 
              type="text" 
              placeholder="请输入作品名称"
              maxlength="50"
            />
            <span class="char-count">{{ novel.title.length }}/50</span>
          </div>
          
          <div class="form-row">
            <div class="form-group">
              <label>分类</label>
              <select v-model="novel.category">
                <option v-for="cat in categories" :key="cat" :value="cat">
                  {{ cat }}
                </option>
              </select>
            </div>
            
            <div class="form-group">
              <label>状态</label>
              <select v-model="novel.status">
                <option v-for="status in statuses" :key="status" :value="status">
                  {{ status }}
                </option>
              </select>
            </div>
          </div>
          
          <div class="form-group">
            <label>作品简介 <span class="required">*</span></label>
            <textarea 
              v-model="novel.description" 
              placeholder="请输入作品简介，让读者了解你的作品..."
              rows="6"
              maxlength="500"
            ></textarea>
            <span class="char-count">{{ novel.description.length }}/500</span>
          </div>
        </div>
        
        <div class="form-side">
          <div class="cover-upload">
            <label>作品封面</label>
            <div class="cover-preview" @click="$refs.coverInput.click()">
              <img v-if="novel.cover" :src="novel.cover" @error="$event.target.style.display='none'; $event.target.nextElementSibling.style.display='flex'" />
              <div class="cover-placeholder" :style="{ display: novel.cover ? 'none' : 'flex' }">
                <span>+</span>
                <p>点击上传封面</p>
              </div>
            </div>
            <input 
              ref="coverInput"
              type="file" 
              accept="image/*" 
              style="display: none"
              @change="handleCoverUpload"
            />
            <p class="cover-tip">建议尺寸：300x400像素</p>
          </div>
        </div>
      </form>
      
      <div class="form-actions">
        <button type="button" class="btn-cancel" @click="cancel">取消</button>
        <button type="submit" class="btn-save" @click="saveNovel">
          {{ isEdit ? '保存修改' : '创建作品' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.novel-edit {
  max-width: 1000px;
  margin: 0 auto;
  padding: 30px 20px;
}

.edit-container {
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  border-radius: var(--radius-xl);
  padding: var(--spacing-2xl);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--color-border);
}

.edit-container h1 {
  font-size: 28px;
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-2xl);
  font-weight: 600;
}

.edit-form {
  display: grid;
  grid-template-columns: 1fr 250px;
  gap: var(--spacing-2xl);
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

.required {
  color: #f87171;
}

.form-group input,
.form-group select,
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
.form-group select:focus,
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

.form-group select option {
  background: var(--color-bg-secondary);
  color: var(--color-text-primary);
}

.form-group textarea {
  resize: vertical;
  min-height: 120px;
}

.char-count {
  display: block;
  text-align: right;
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-top: var(--spacing-xs);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-lg);
}

.cover-upload label {
  display: block;
  margin-bottom: var(--spacing-sm);
  font-size: 14px;
  color: var(--color-text-primary);
  font-weight: 500;
}

.cover-preview {
  width: 100%;
  aspect-ratio: 3/4;
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s;
  background: rgba(255, 255, 255, 0.02);
}

.cover-preview:hover {
  border-color: var(--color-primary);
  background: rgba(201, 169, 98, 0.05);
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  text-align: center;
  color: var(--color-text-tertiary);
}

.cover-placeholder span {
  font-size: 48px;
  line-height: 1;
  color: var(--color-primary);
}

.cover-placeholder p {
  margin-top: var(--spacing-sm);
  font-size: 14px;
}

.cover-tip {
  font-size: 12px;
  color: var(--color-text-tertiary);
  text-align: center;
  margin-top: var(--spacing-sm);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-md);
  margin-top: var(--spacing-2xl);
  padding-top: var(--spacing-xl);
  border-top: 1px solid var(--color-border);
}

.btn-cancel {
  padding: 12px 30px;
  border: 1px solid var(--color-border);
  background: rgba(255, 255, 255, 0.05);
  border-radius: var(--radius-md);
  font-size: 15px;
  cursor: pointer;
  transition: all 0.3s;
  color: var(--color-text-primary);
}

.btn-cancel:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.btn-save {
  padding: 12px 30px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: #0d1117;
  border: none;
  border-radius: var(--radius-md);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-save:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(201, 169, 98, 0.3);
}

@media (max-width: 768px) {
  .edit-form {
    grid-template-columns: 1fr;
  }
  
  .form-row {
    grid-template-columns: 1fr;
  }
  
  .cover-upload {
    max-width: 250px;
    margin: 0 auto;
  }
}
</style>
