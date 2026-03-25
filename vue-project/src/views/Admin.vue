<template>
  <div class="admin-page">
    <div class="admin-sidebar">
      <div class="admin-logo">
        <h2>管理后台</h2>
        <p class="admin-username">{{ adminUsername }}</p>
      </div>
      <nav class="sidebar-nav">
        <a 
          v-for="item in navItems" 
          :key="item.key"
          :class="['nav-item', { active: activeSection === item.key }]"
          @click="activeSection = item.key"
        >
          {{ item.label }}
        </a>
        <a class="nav-item logout" @click="logout">
          退出登录
        </a>
      </nav>
    </div>

    <div class="admin-content">
      <div v-if="activeSection === 'dashboard'" class="dashboard-section">
        <h2>数据概览</h2>
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon users">用户</div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalUsers }}</div>
              <div class="stat-label">注册用户</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon novels">小说</div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalNovels }}</div>
              <div class="stat-label">小说总数</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon views">阅读</div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalViews }}</div>
              <div class="stat-label">总阅读量</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon comments">评论</div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalComments }}</div>
              <div class="stat-label">评论总数</div>
            </div>
          </div>
        </div>

        <div class="chart-section">
          <h3>热门题材趋势</h3>
          <div class="trend-list">
            <div v-for="trend in trends" :key="trend.genre" class="trend-item">
              <span class="trend-name">{{ trend.genre }}</span>
              <div class="trend-bar">
                <div class="trend-fill" :style="{ width: calculatePercent(trend.count) + '%' }"></div>
              </div>
              <span class="trend-count">{{ trend.count }} 本</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="activeSection === 'chapters'" class="chapters-section">
        <div class="section-header">
          <h2>章节审核</h2>
          <button class="btn-refresh" @click="loadChapters()">刷新</button>
        </div>
        
        <div class="chapters-table">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>章节标题</th>
                <th>小说ID</th>
                <th>章节号</th>
                <th>字数</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="chapter in chapters" :key="chapter.id">
                <td>{{ chapter.id }}</td>
                <td>{{ chapter.title }}</td>
                <td>{{ chapter.novelId }}</td>
                <td>{{ chapter.chapterNumber }}</td>
                <td>{{ chapter.wordCount || 0 }}</td>
                <td>{{ formatTime(chapter.createdAt) }}</td>
                <td>
                  <div class="action-btns">
                    <button class="btn-view" @click="viewChapter(chapter)">查看</button>
                    <button class="btn-approve" @click="approveChapter(chapter.id)">通过</button>
                    <button class="btn-reject" @click="rejectChapter(chapter.id)">拒绝</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="!chapters || chapters.length === 0" class="empty-data">暂无待审核章节</div>
        </div>

        <div class="pagination">
          <button 
            class="page-btn" 
            :disabled="pendingChaptersPage === 0"
            @click="loadChapters(pendingChaptersPage - 1)"
          >
            上一页
          </button>
          <span class="page-info">
            第
            <input 
              type="number" 
              v-model.number="jumpToPage" 
              @keyup.enter="jumpToPageFunc('pending')"
              :min="1" 
              :max="pendingChaptersTotalPages"
              class="page-input"
            /> 
            / {{ pendingChaptersTotalPages }} 页
            <button @click="jumpToPageFunc('pending')" class="jump-btn">跳转</button>
          </span>
          <button 
            class="page-btn" 
            :disabled="pendingChaptersPage === pendingChaptersTotalPages - 1"
            @click="loadChapters(pendingChaptersPage + 1)"
          >
            下一页
          </button>
        </div>
      </div>

      <div v-if="activeSection === 'novelManage'" class="novel-manage-section">
        <div class="section-header">
          <h2>小说管理</h2>
          <div class="filter-bar">
            <input v-model="novelManageSearch" placeholder="搜索小说..." @keyup.enter="loadAllNovels(0)" />
            <select v-model="novelManageFilter" @change="loadAllNovels(0)">
              <option value="all">全部状态</option>
              <option value="APPROVED">已通过</option>
              <option value="PENDING">待审核</option>
              <option value="REJECTED">已拒绝</option>
            </select>
            <button @click="loadAllNovels(0)">搜索</button>
          </div>
        </div>

        <div class="novels-table">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>标题</th>
                <th>作者</th>
                <th>分类</th>
                <th>字数</th>
                <th>审核状态</th>
                <th>连载状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="novel in allNovels" :key="novel.id">
                <td>{{ novel.id }}</td>
                <td>{{ novel.title }}</td>
                <td>{{ novel.author }}</td>
                <td>{{ novel.category }}</td>
                <td>{{ novel.wordCount || 0 }}</td>
                <td>
                  <span :class="['status-badge', novel.reviewStatus?.toLowerCase()]">
                    {{ novel.reviewStatus === 'APPROVED' ? '已通过' : novel.reviewStatus === 'PENDING' ? '待审核' : '已拒绝' }}
                  </span>
                </td>
                <td>
                  <span :class="['status-badge', novel.status === 'ONGOING' ? 'ongoing' : novel.status === 'COMPLETED' ? 'completed' : 'normal']">
                    {{ novel.status === 'ONGOING' ? '连载中' : novel.status === 'COMPLETED' ? '已完结' : novel.status === 'BANNED' ? '已封禁' : '未知' }}
                  </span>
                </td>
                <td>
                  <div class="action-btns">
                    <button class="btn-view" @click="viewNovel(novel.id)">查看</button>
                    <button class="btn-status" @click="showNovelStatusModal(novel)">修改状态</button>
                    <button v-if="novel.status !== 'BANNED'" class="btn-ban" @click="banNovel(novel.id)">封禁</button>
                    <button v-if="novel.status === 'BANNED'" class="btn-unban" @click="unbanNovel(novel.id)">解禁</button>
                    <button class="btn-delete" @click="deleteNovel(novel.id)">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="allNovels.length === 0" class="empty-data">暂无小说数据</div>
        </div>

        <div class="pagination">
          <button 
            class="page-btn" 
            :disabled="novelsPage === 0"
            @click="loadAllNovels(novelsPage - 1)"
          >
            上一页
          </button>
          <span class="page-info">
            第
            <input 
              type="number" 
              v-model.number="jumpToPage" 
              @keyup.enter="jumpToPageFunc('novel')"
              :min="1" 
              :max="novelsTotalPages"
              class="page-input"
            /> 
            / {{ novelsTotalPages }} 页
            <button @click="jumpToPageFunc('novel')" class="jump-btn">跳转</button>
          </span>
          <button 
            class="page-btn" 
            :disabled="novelsPage === novelsTotalPages - 1"
            @click="loadAllNovels(novelsPage + 1)"
          >
            下一页
          </button>
        </div>
      </div>

      <div v-if="activeSection === 'categories'" class="categories-section">
        <div class="section-header">
          <h2>分类管理</h2>
          <button class="create-btn" @click="showAddCategory = true">添加分类</button>
        </div>

        <div class="categories-list">
          <div v-for="cat in categories" :key="cat.id" class="category-item">
            <div class="category-info">
              <span class="category-icon">{{ cat.icon || '📚' }}</span>
              <div class="category-details">
                <h4>{{ cat.name }}</h4>
                <p>{{ cat.description || '暂无描述' }}</p>
              </div>
              <span class="novel-count">{{ categoryStats[cat.name] || 0 }} 本</span>
            </div>
            <div class="category-actions">
              <button class="action-btn" @click="editCategory(cat)">编辑</button>
              <button class="action-btn danger" @click="deleteCategory(cat.id)">删除</button>
            </div>
          </div>
          <div v-if="categories.length === 0" class="empty-data">暂无分类数据</div>
        </div>

        <div v-if="categoryTotalPages > 1" class="pagination">
          <button 
            :disabled="categoryPage === 0" 
            @click="loadCategories(categoryPage - 1)"
          >
            上一页
          </button>
          <span class="page-info">
            第 {{ categoryPage + 1 }} / {{ categoryTotalPages }} 页
          </span>
          <button 
            :disabled="categoryPage >= categoryTotalPages - 1" 
            @click="loadCategories(categoryPage + 1)"
          >
            下一页
          </button>
        </div>
      </div>

      <div v-if="activeSection === 'users'" class="users-section">
        <div class="section-header">
          <h2>用户管理</h2>
          <div class="header-actions">
            <div class="search-bar">
              <input v-model="userSearch" placeholder="搜索用户..." @keyup.enter="searchUsers" />
              <button @click="searchUsers">搜索</button>
            </div>
            <button class="create-btn" @click="showAddUser = true">+ 添加用户</button>
          </div>
        </div>

        <div class="users-table">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>用户名</th>
                <th>邮箱</th>
                <th>角色</th>
                <th>注册时间</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in users" :key="user.id">
                <td>{{ user.id }}</td>
                <td>{{ user.username }}</td>
                <td>{{ user.email || '-' }}</td>
                <td>
                  <span :class="['role-tag', user.role]">{{ user.role || 'USER' }}</span>
                </td>
                <td>{{ formatTime(user.createdAt) }}</td>
                <td>
                  <span :class="['status-tag', user.isBanned ? 'BANNED' : 'ACTIVE']">
                    {{ user.isBanned ? '已封禁' : '正常' }}
                  </span>
                </td>
                <td>
                  <div class="action-btns">
                    <button 
                      v-if="!user.isBanned" 
                      class="btn-ban" 
                      @click="banUser(user.id)"
                    >
                      封禁
                    </button>
                    <button 
                      v-else 
                      class="btn-unban" 
                      @click="unbanUser(user.id)"
                    >
                      解禁
                    </button>
                    <button 
                      v-if="user.role !== 'ADMIN'" 
                      class="btn-delete" 
                      @click="deleteUser(user.id)"
                    >
                      删除
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="users.length === 0" class="empty-data">暂无用户数据</div>
        </div>

        <div class="pagination">
          <button 
            class="page-btn" 
            :disabled="usersPage === 0"
            @click="loadUsers(usersPage - 1)"
          >
            上一页
          </button>
          <span class="page-info">
            第
            <input 
              type="number" 
              v-model.number="jumpToPage" 
              @keyup.enter="jumpToPageFunc('users')"
              :min="1" 
              :max="usersTotalPages"
              class="page-input"
            /> 
            / {{ usersTotalPages }} 页
            <button @click="jumpToPageFunc('users')" class="jump-btn">跳转</button>
          </span>
          <button 
            class="page-btn" 
            :disabled="usersPage === usersTotalPages - 1"
            @click="loadUsers(usersPage + 1)"
          >
            下一页
          </button>
        </div>
      </div>

      <div v-if="showAddUser" class="modal-overlay" @click.self="showAddUser = false">
        <div class="modal-content">
          <h3>添加用户</h3>
          <div class="form-group">
            <label>用户名</label>
            <input v-model="newUser.username" placeholder="请输入用户名" />
          </div>
          <div class="form-group">
            <label>密码</label>
            <input v-model="newUser.password" type="password" placeholder="请输入密码" />
          </div>
          <div class="form-group">
            <label>邮箱</label>
            <input v-model="newUser.email" placeholder="请输入邮箱" />
          </div>
          <div class="form-group">
            <label>角色</label>
            <select v-model="newUser.role">
              <option value="USER">普通用户</option>
              <option value="AUTHOR">作者</option>
              <option value="ADMIN">管理员</option>
            </select>
          </div>
          <div class="modal-actions">
            <button class="btn-cancel" @click="showAddUser = false">取消</button>
            <button class="btn-confirm" @click="addUser">确认</button>
          </div>
        </div>
      </div>

      <div v-if="activeSection === 'comments'" class="comments-section">
        <div class="section-header">
          <h2>评论管理</h2>
          <div class="pagination-info">
            共 {{ commentsTotal }} 条评论
          </div>
        </div>

        <div class="comments-list">
          <div v-for="comment in comments" :key="comment.id" class="comment-item">
            <div class="comment-header">
              <span class="commenter">{{ comment.userName || '匿名用户' }}</span>
              <span class="novel-title">{{ comment.novelTitle || '未知小说' }}</span>
              <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
            </div>
            <p class="comment-content">{{ comment.content }}</p>
            <div class="comment-actions">
              <button class="action-btn danger" @click="deleteComment(comment.id)">删除</button>
            </div>
          </div>
          <div v-if="comments.length === 0" class="empty-data">暂无评论数据</div>
        </div>

        <div class="pagination">
          <button 
            class="page-btn" 
            :disabled="commentsPage === 0"
            @click="loadComments(commentsPage - 1)"
          >
            上一页
          </button>
          <span class="page-info">
            第
            <input 
              type="number" 
              v-model.number="jumpToPage" 
              @keyup.enter="jumpToPageFunc('comments')"
              :min="1" 
              :max="commentsTotalPages"
              class="page-input"
            /> 
            / {{ commentsTotalPages }} 页
            <button @click="jumpToPageFunc('comments')" class="jump-btn">跳转</button>
          </span>
          <button 
            class="page-btn" 
            :disabled="commentsPage === commentsTotalPages - 1"
            @click="loadComments(commentsPage + 1)"
          >
            下一页
          </button>
        </div>
      </div>

      <div v-if="activeSection === 'topics'" class="topics-section">
        <div class="section-header">
          <h2>话题管理</h2>
          <div class="pagination-info">
            共 {{ topicsTotal }} 个话题
          </div>
        </div>

        <div class="topics-table">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>标题</th>
                <th>内容</th>
                <th>作者</th>
                <th>回复数</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="topic in topics" :key="topic.id">
                <td>{{ topic.id }}</td>
                <td>{{ topic.title }}</td>
                <td class="content-cell">{{ topic.content }}</td>
                <td>{{ topic.user?.username || '匿名' }}</td>
                <td>{{ topic.replies || 0 }}</td>
                <td>{{ formatTime(topic.createdAt) }}</td>
                <td>
                  <div class="action-btns">
                    <button class="btn-delete" @click="deleteTopic(topic.id)">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="topics.length === 0" class="empty-data">暂无话题数据</div>
        </div>

        <div class="pagination">
          <button 
            class="page-btn" 
            :disabled="topicsPage === 0"
            @click="loadTopics(topicsPage - 1)"
          >
            上一页
          </button>
          <span class="page-info">
            第
            <input 
              type="number" 
              v-model.number="jumpToPage" 
              @keyup.enter="jumpToPageFunc('topics')"
              :min="1" 
              :max="topicsTotalPages"
              class="page-input"
            /> 
            / {{ topicsTotalPages }} 页
            <button @click="jumpToPageFunc('topics')" class="jump-btn">跳转</button>
          </span>
          <button 
            class="page-btn" 
            :disabled="topicsPage === topicsTotalPages - 1"
            @click="loadTopics(topicsPage + 1)"
          >
            下一页
          </button>
        </div>
      </div>

      <div v-if="activeSection === 'booklists'" class="booklists-section">
        <div class="section-header">
          <h2>书单管理</h2>
          <div class="pagination-info">
            共 {{ booklistsTotal }} 个书单
          </div>
        </div>

        <div class="booklists-table">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>标题</th>
                <th>描述</th>
                <th>用户ID</th>
                <th>小说数</th>
                <th>类型</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="booklist in booklists" :key="booklist.id">
                <td>{{ booklist.id }}</td>
                <td>{{ booklist.title }}</td>
                <td class="content-cell">{{ booklist.description || '-' }}</td>
                <td>{{ booklist.userId }}</td>
                <td>{{ booklist.bookCount || 0 }}</td>
                <td>
                  <span :class="['status-tag', booklist.listType]">
                    {{ booklist.listType === 'PUBLIC' ? '公开' : '私密' }}
                  </span>
                </td>
                <td>{{ formatTime(booklist.createdAt) }}</td>
                <td>
                  <div class="action-btns">
                    <button class="btn-delete" @click="deleteBooklist(booklist.id)">删除</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="booklists.length === 0" class="empty-data">暂无书单数据</div>
        </div>

        <div class="pagination">
          <button 
            class="page-btn" 
            :disabled="booklistsPage === 0"
            @click="loadBooklists(booklistsPage - 1)"
          >
            上一页
          </button>
          <span class="page-info">
            第
            <input 
              type="number" 
              v-model.number="jumpToPage" 
              @keyup.enter="jumpToPageFunc('booklists')"
              :min="1" 
              :max="booklistsTotalPages"
              class="page-input"
            /> 
            / {{ booklistsTotalPages }} 页
            <button @click="jumpToPageFunc('booklists')" class="jump-btn">跳转</button>
          </span>
          <button 
            class="page-btn" 
            :disabled="booklistsPage === booklistsTotalPages - 1"
            @click="loadBooklists(booklistsPage + 1)"
          >
            下一页
          </button>
        </div>
      </div>

    </div>

    <div v-if="showAddCategory" class="modal-overlay" @click.self="closeCategoryModal">
      <div class="modal-content">
        <h2>{{ editingCategory ? '编辑分类' : '添加分类' }}</h2>
        <form @submit.prevent="saveCategory">
          <div class="form-group">
            <label>分类名称</label>
            <input v-model="categoryForm.name" type="text" required />
          </div>
          <div class="form-group">
            <label>分类描述</label>
            <textarea v-model="categoryForm.description" rows="3"></textarea>
          </div>
          <div class="form-group">
            <label>图标</label>
            <input v-model="categoryForm.icon" type="text" placeholder="emoji: 📚" />
          </div>
          <div class="form-actions">
            <button type="button" class="cancel-btn" @click="closeCategoryModal">取消</button>
            <button type="submit" class="submit-btn">{{ editingCategory ? '保存' : '添加' }}</button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showChapterModal" class="modal-overlay" @click.self="showChapterModal = false">
      <div class="modal-content chapter-modal">
        <div class="modal-header">
          <h2>{{ selectedChapter?.title }}</h2>
          <button class="close-btn" @click="showChapterModal = false">×</button>
        </div>
        <div class="chapter-info">
          <p><strong>章节号：</strong>{{ selectedChapter?.chapterNumber }}</p>
          <p><strong>创建时间：</strong>{{ formatTime(selectedChapter?.createdAt) }}</p>
        </div>
        <div class="chapter-content">
          <h3>章节内容</h3>
          <div class="content-text">{{ selectedChapter?.content }}</div>
        </div>
      </div>
    </div>

    <div v-if="showNovelStatusModalFlag" class="modal-overlay" @click.self="showNovelStatusModalFlag = false">
      <div class="modal-content">
        <div class="modal-header">
          <h2>修改小说状态</h2>
          <button class="close-btn" @click="showNovelStatusModalFlag = false">×</button>
        </div>
        <div class="novel-status-form">
          <div class="form-group">
            <label>连载状态</label>
            <select v-model="novelStatusForm.status">
              <option value="ONGOING">连载中</option>
              <option value="COMPLETED">已完结</option>
            </select>
          </div>
          <div class="form-group">
            <label>审核状态</label>
            <select v-model="novelStatusForm.reviewStatus">
              <option value="APPROVED">已通过</option>
              <option value="PENDING">待审核</option>
            </select>
          </div>
          <div class="form-actions">
            <button class="btn-cancel" @click="showNovelStatusModalFlag = false">取消</button>
            <button class="btn-save" @click="saveNovelStatus">保存</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * Admin.vue - 管理后台主页面组件
 * 
 * 该组件实现了小说系统的完整管理后台功能，包括：
 * - 数据概览仪表板：显示用户数、小说数、阅读量、评论数等统计数据
 * - 章节审核：审核作者提交的章节内容
 * - 小说管理：管理所有小说的状态、审核、封禁等
 * - 分类管理：添加、编辑、删除小说分类
 * - 用户管理：查看、封禁、删除用户
 * - 评论管理：查看和删除评论
 * - 话题管理：管理社区话题
 * - 书单管理：管理用户创建的书单
 */
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getChapters,
  getAllChapters,
  getChapterDetail,
  getAllNovels,
  getCategories,
  getCategoriesPaginated,
  getCategoryStats,
  getUsers,
  getComments,
  getStats,
  getTrendingGenres,
  reviewChapter,
  updateNovelStatus,
  addCategory as apiAddCategory,
  updateCategory as apiUpdateCategory,
  deleteCategory as apiDeleteCategory,
  banUser as apiBanUser,
  unbanUser as apiUnbanUser,
  deleteUser as apiDeleteUser,
  createUser as apiCreateUser,
  deleteComment as apiDeleteComment,
  banNovel as banNovelApi,
  unbanNovel as unbanNovelApi,
  deleteNovel as deleteNovelApi,
  getTopics,
  deleteTopic as apiDeleteTopic,
  getBooklists,
  deleteBooklist as apiDeleteBooklist
} from '../api/modules/admin.js'
import { useUserStore } from '../stores/user.js'

const router = useRouter()
const userStore = useUserStore()

const activeSection = ref('dashboard')
const novelFilter = ref('all')
const userSearch = ref('')
const showAddCategory = ref(false)
const editingCategory = ref(null)
const showAddUser = ref(false)
const adminUsername = ref(JSON.parse(localStorage.getItem('adminUserInfo') || '{}').username || '管理员')

const newUser = ref({
  username: '',
  password: '',
  email: '',
  role: 'USER'
})

const navItems = [
  { key: 'dashboard', label: '数据概览' },
  { key: 'chapters', label: '章节审核' },
  { key: 'novelManage', label: '小说管理' },
  { key: 'categories', label: '分类管理' },
  { key: 'users', label: '用户管理' },
  { key: 'comments', label: '评论管理' },
  { key: 'topics', label: '话题管理' },
  { key: 'booklists', label: '书单管理' }
]

const stats = ref({
  totalUsers: 0,
  totalNovels: 0,
  totalViews: 0,
  totalComments: 0
})

const trends = ref([])
const chapters = ref([])
const allNovels = ref([])
const categories = ref([])
const categoryStats = ref({})
const categoryPage = ref(0)
const categoryTotalPages = ref(0)
const categoryTotal = ref(0)
const users = ref([])
const usersPage = ref(0)
const usersTotalPages = ref(0)
const usersTotal = ref(0)
const comments = ref([])
const commentsPage = ref(0)
const commentsTotalPages = ref(0)
const commentsTotal = ref(0)
const topics = ref([])
const topicsPage = ref(0)
const topicsTotalPages = ref(0)
const topicsTotal = ref(0)
const booklists = ref([])
const booklistsPage = ref(0)
const booklistsTotalPages = ref(0)
const booklistsTotal = ref(0)
const novelsPage = ref(0)
const novelsTotalPages = ref(0)
const novelsTotal = ref(0)
const pendingChaptersPage = ref(0)
const pendingChaptersTotalPages = ref(0)
const pendingChaptersTotal = ref(0)
const novelManageSearch = ref('')
const novelManageFilter = ref('all')
const jumpToPage = ref(1)
const showChapterModal = ref(false)
const selectedChapter = ref(null)
const showNovelStatusModalFlag = ref(false)
const selectedNovelForStatus = ref(null)
const novelStatusForm = ref({
  status: 'ONGOING',
  reviewStatus: 'APPROVED'
})

const categoryForm = ref({
  name: '',
  description: '',
  icon: ''
})

/**
 * 检查管理员权限
 * 验证用户是否已登录且具有管理员角色
 * @returns {boolean} 是否具有管理员权限
 */
const checkAdminAuth = () => {
  const token = localStorage.getItem('adminToken')
  const userInfo = JSON.parse(localStorage.getItem('adminUserInfo') || '{}')
  const userRole = (userInfo.role || '').toUpperCase()
  
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return false
  }
  
  if (userRole !== 'ADMIN') {
    ElMessage.warning('当前账号不是管理员，请使用管理员账号登录')
    router.push('/login')
    return false
  }
  
  return true
}

/**
 * 格式化时间显示
 * @param {string} dateStr - 日期字符串
 * @returns {string} 格式化后的日期
 */
const formatTime = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString()
}

/**
 * 计算趋势百分比
 * 用于显示热门题材的进度条
 * @param {number} count - 当前题材的小说数量
 * @returns {number} 百分比值
 */
const calculatePercent = (count) => {
  const max = Math.max(...trends.value.map(t => t.count), 1)
  return Math.round((count / max) * 100)
}

/**
 * 加载统计数据
 * 获取用户数、小说数、阅读量、评论数等统计信息
 */
const loadStats = async () => {
  try {
    const res = await getStats()
    if (res.code === 200) {
      stats.value = {
        totalUsers: res.data.totalUsers || 0,
        totalNovels: res.data.totalNovels || 0,
        totalViews: res.data.totalViews || 0,
        totalComments: res.data.totalComments || 0
      }
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

/**
 * 加载热门题材趋势数据
 * 获取各分类的小说数量统计
 */
const loadTrends = async () => {
  try {
    const res = await getTrendingGenres()
    if (res.code === 200) {
      trends.value = res.data || []
    }
  } catch (error) {
    console.error('加载趋势数据失败:', error)
  }
}

/**
 * 加载待审核章节列表
 * @param {number} page - 页码，从0开始
 */
const loadChapters = async (page = 0) => {
  try {
    const res = await getChapters({ page, size: 20 })
    if (res.code === 200 && res.data) {
      chapters.value = res.data.content
      pendingChaptersPage.value = res.data.currentPage
      pendingChaptersTotalPages.value = res.data.totalPages
      pendingChaptersTotal.value = res.data.totalElements
    }
  } catch (error) {
    console.error('加载章节失败:', error)
    ElMessage.error('加载章节失败')
  }
}

/**
 * 加载所有小说列表
 * 支持搜索和状态筛选
 * @param {number} page - 页码
 */
const loadAllNovels = async (page = 0) => {
  try {
    const res = await getAllNovels({
      page,
      size: 20,
      keyword: novelManageSearch.value,
      status: novelManageFilter.value === 'all' ? '' : novelManageFilter.value
    })
    if (res.code === 200 && res.data) {
      allNovels.value = res.data.content
      novelsPage.value = res.data.currentPage
      novelsTotalPages.value = res.data.totalPages
      novelsTotal.value = res.data.totalElements
    }
  } catch (error) {
    console.error('加载小说失败:', error)
    ElMessage.error('加载小说失败')
  }
}

/**
 * 封禁小说
 * @param {number} novelId - 小说ID
 */
const banNovel = async (novelId) => {
  try {
    await ElMessageBox.confirm('确定要封禁这本小说吗？', '封禁确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await banNovelApi(novelId)
    if (res.code === 200) {
      ElMessage.success('封禁成功')
      loadAllNovels()
    } else {
      ElMessage.error(res.message || '封禁失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('封禁小说失败:', error)
      ElMessage.error('封禁失败')
    }
  }
}

/**
 * 解禁小说
 * @param {number} novelId - 小说ID
 */
const unbanNovel = async (novelId) => {
  try {
    const res = await unbanNovelApi(novelId)
    if (res.code === 200) {
      ElMessage.success('解禁成功')
      loadAllNovels()
    } else {
      ElMessage.error(res.message || '解禁失败')
    }
  } catch (error) {
    console.error('解禁小说失败:', error)
    ElMessage.error('解禁失败')
  }
}

/**
 * 删除小说
 * @param {number} novelId - 小说ID
 */
const deleteNovel = async (novelId) => {
  try {
    await ElMessageBox.confirm('确定要删除这本小说吗？此操作不可恢复！', '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'danger'
    })
    const res = await deleteNovelApi(novelId)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadAllNovels(novelsPage.value)
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除小说失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

/**
 * 显示小说状态修改弹窗
 * @param {Object} novel - 小说对象
 */
const showNovelStatusModal = (novel) => {
  selectedNovelForStatus.value = novel
  novelStatusForm.value = {
    status: novel.status || 'ONGOING',
    reviewStatus: novel.reviewStatus || 'PENDING'
  }
  showNovelStatusModalFlag.value = true
}

/**
 * 保存小说状态修改
 */
const saveNovelStatus = async () => {
  try {
    const res = await updateNovelStatus(
      selectedNovelForStatus.value.id,
      novelStatusForm.value.status,
      novelStatusForm.value.reviewStatus
    )
    if (res.code === 200) {
      ElMessage.success('状态更新成功')
      showNovelStatusModalFlag.value = false
      loadAllNovels(novelsPage.value)
    } else {
      ElMessage.error(res.message || '更新失败')
    }
  } catch (error) {
    console.error('更新小说状态失败:', error)
    ElMessage.error('更新失败')
  }
}

/**
 * 跳转到指定页面
 * @param {string} type - 分页类型（pending/novel/category/users/comments/topics/booklists）
 */
const jumpToPageFunc = async (type) => {
  const page = jumpToPage.value - 1
  if (isNaN(page) || page < 0) {
    ElMessage.warning('请输入有效的页码')
    return
  }
  
  try {
    if (type === 'pending') {
      if (page >= pendingChaptersTotalPages.value) {
        ElMessage.warning(`页码超出范围，最大页数为 ${pendingChaptersTotalPages.value}`)
        return
      }
      await loadChapters(page)
    } else if (type === 'novel') {
      if (page >= novelsTotalPages.value) {
        ElMessage.warning(`页码超出范围，最大页数为 ${novelsTotalPages.value}`)
        return
      }
      await loadAllNovels(page)
    } else if (type === 'category') {
      if (page >= categoryTotalPages.value) {
        ElMessage.warning(`页码超出范围，最大页数为 ${categoryTotalPages.value}`)
        return
      }
      await loadCategories(page)
    } else if (type === 'users') {
      if (page >= usersTotalPages.value) {
        ElMessage.warning(`页码超出范围，最大页数为 ${usersTotalPages.value}`)
        return
      }
      await loadUsers(page)
    } else if (type === 'comments') {
      if (page >= commentsTotalPages.value) {
        ElMessage.warning(`页码超出范围，最大页数为 ${commentsTotalPages.value}`)
        return
      }
      await loadComments(page)
    } else if (type === 'topics') {
      if (page >= topicsTotalPages.value) {
        ElMessage.warning(`页码超出范围，最大页数为 ${topicsTotalPages.value}`)
        return
      }
      await loadTopics(page)
    } else if (type === 'booklists') {
      if (page >= booklistsTotalPages.value) {
        ElMessage.warning(`页码超出范围，最大页数为 ${booklistsTotalPages.value}`)
        return
      }
      await loadBooklists(page)
    }
  } catch (error) {
    console.error('跳转页面失败:', error)
  }
}

/**
 * 加载分类列表
 * 同时获取分类统计信息
 * @param {number} page - 页码
 */
const loadCategories = async (page = 0) => {
  try {
    const [categoriesRes, statsRes] = await Promise.all([
      getCategoriesPaginated(page, 10),
      getCategoryStats()
    ])
    if (categoriesRes.code === 200 && categoriesRes.data) {
      categories.value = categoriesRes.data.content || []
      categoryPage.value = categoriesRes.data.currentPage
      categoryTotalPages.value = categoriesRes.data.totalPages
      categoryTotal.value = categoriesRes.data.totalElements
    }
    if (statsRes.code === 200) {
      categoryStats.value = statsRes.data || {}
    }
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

/**
 * 加载用户列表
 * @param {number} page - 页码
 */
const loadUsers = async (page = 0) => {
  try {
    const res = await getUsers(page, 20, userSearch.value)
    if (res.code === 200 && res.data) {
      users.value = res.data.content
      usersPage.value = res.data.currentPage
      usersTotalPages.value = res.data.totalPages
      usersTotal.value = res.data.totalElements
    }
  } catch (error) {
    console.error('加载用户失败:', error)
  }
}

/**
 * 加载评论列表
 * @param {number} page - 页码
 */
const loadComments = async (page = 0) => {
  try {
    const res = await getComments(page, 20)
    if (res.code === 200 && res.data) {
      comments.value = res.data.content
      commentsPage.value = res.data.currentPage
      commentsTotalPages.value = res.data.totalPages
      commentsTotal.value = res.data.totalElements
    }
  } catch (error) {
    console.error('加载评论失败:', error)
  }
}

/**
 * 查看小说详情
 * 在新窗口打开小说页面
 * @param {number} id - 小说ID
 */
const viewNovel = (id) => {
  window.open(`/novel/${id}`, '_blank')
}

/**
 * 查看章节详情
 * @param {Object} chapter - 章节对象
 */
const viewChapter = async (chapter) => {
  try {
    const res = await getChapterDetail(chapter.id)
    if (res.code === 200 && res.data) {
      selectedChapter.value = res.data
      showChapterModal.value = true
    } else {
      ElMessage.error(res.message || '获取章节详情失败')
    }
  } catch (error) {
    console.error('获取章节详情失败:', error)
    ElMessage.error('获取章节详情失败')
  }
}

/**
 * 通过章节审核
 * @param {number} id - 章节ID
 */
const approveChapter = async (id) => {
  try {
    const res = await reviewChapter(id, 'APPROVED', '')
    if (res.code === 200) {
      ElMessage.success('审核通过')
      loadChapters()
    } else {
      ElMessage.error(res.message || '审核失败')
    }
  } catch (error) {
    console.error('审核章节失败:', error)
    ElMessage.error('审核失败')
  }
}

/**
 * 拒绝章节
 * 需要输入拒绝原因
 * @param {number} id - 章节ID
 */
const rejectChapter = async (id) => {
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝章节', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '请输入拒绝原因',
      inputValidator: (value) => {
        if (!value || value.trim() === '') {
          return '请输入拒绝原因'
        }
        return true
      }
    })
    if (reason) {
      const res = await reviewChapter(id, 'PENDING', reason)
      if (res.code === 200) {
        ElMessage.success('已拒绝')
        loadChapters()
      } else {
        ElMessage.error(res.message || '操作失败')
      }
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('拒绝章节失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

/**
 * 编辑分类
 * @param {Object} cat - 分类对象
 */
const editCategory = (cat) => {
  editingCategory.value = cat
  categoryForm.value = {
    name: cat.name,
    description: cat.description || '',
    icon: cat.icon || ''
  }
  showAddCategory.value = true
}

/**
 * 关闭分类弹窗
 */
const closeCategoryModal = () => {
  showAddCategory.value = false
  editingCategory.value = null
  categoryForm.value = { name: '', description: '', icon: '' }
}

/**
 * 保存分类
 * 支持新增和编辑
 */
const saveCategory = async () => {
  try {
    if (editingCategory.value) {
      const res = await apiUpdateCategory(editingCategory.value.id, categoryForm.value)
      if (res.code === 200) {
        ElMessage.success('更新成功')
        loadCategories()
        closeCategoryModal()
      }
    } else {
      const res = await apiAddCategory(categoryForm.value)
      if (res.code === 200) {
        ElMessage.success('添加成功')
        loadCategories()
        closeCategoryModal()
      }
    }
  } catch (error) {
    console.error('保存分类失败:', error)
    ElMessage.error('操作失败')
  }
}

/**
 * 删除分类
 * @param {number} id - 分类ID
 */
const deleteCategory = async (id) => {
  if (!confirm('确定要删除这个分类吗？')) return
  try {
    const res = await apiDeleteCategory(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadCategories()
    }
  } catch (error) {
    console.error('删除分类失败:', error)
    ElMessage.error('删除失败')
  }
}

/**
 * 搜索用户
 */
const searchUsers = async () => {
  if (!userSearch.value.trim()) {
    loadUsers()
    return
  }
  try {
    const res = await getUsers(0, 20, userSearch.value)
    if (res.code === 200) {
      users.value = res.data?.content || []
    }
  } catch (error) {
    console.error('搜索用户失败:', error)
  }
}

/**
 * 封禁用户
 * @param {number} id - 用户ID
 */
const banUser = async (id) => {
  if (!confirm('确定要封禁这个用户吗？')) return
  try {
    const res = await apiBanUser(id)
    if (res.code === 200) {
      ElMessage.success('封禁成功')
      loadUsers()
    }
  } catch (error) {
    console.error('封禁用户失败:', error)
    ElMessage.error('封禁失败')
  }
}

/**
 * 解禁用户
 * @param {number} id - 用户ID
 */
const unbanUser = async (id) => {
  try {
    const res = await apiUnbanUser(id)
    if (res.code === 200) {
      ElMessage.success('解禁成功')
      loadUsers()
    }
  } catch (error) {
    console.error('解禁用户失败:', error)
    ElMessage.error('解禁失败')
  }
}

/**
 * 删除用户
 * @param {number} id - 用户ID
 */
const deleteUser = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个用户吗？', '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await apiDeleteUser(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadUsers()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除用户失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

/**
 * 添加新用户
 */
const addUser = async () => {
  if (!newUser.value.username || !newUser.value.password) {
    ElMessage.error('请填写用户名和密码')
    return
  }
  try {
    const res = await apiCreateUser(newUser.value)
    if (res.code === 200) {
      ElMessage.success('添加成功')
      showAddUser.value = false
      newUser.value = { username: '', password: '', email: '', role: 'USER' }
      loadUsers()
    } else {
      ElMessage.error(res.message || '添加失败')
    }
  } catch (error) {
    console.error('添加用户失败:', error)
    ElMessage.error('添加失败')
  }
}

/**
 * 删除评论
 * @param {number} id - 评论ID
 */
const deleteComment = async (id) => {
  if (!confirm('确定要删除这条评论吗？')) return
  try {
    const res = await apiDeleteComment(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadComments()
    }
  } catch (error) {
    console.error('删除评论失败:', error)
    ElMessage.error('删除失败')
  }
}

/**
 * 加载话题列表
 * @param {number} page - 页码
 */
const loadTopics = async (page = 0) => {
  try {
    const res = await getTopics(page, 20)
    if (res.code === 200 && res.data) {
      topics.value = res.data.content
      topicsPage.value = res.data.currentPage
      topicsTotalPages.value = res.data.totalPages
      topicsTotal.value = res.data.totalElements
    }
  } catch (error) {
    console.error('加载话题失败:', error)
    ElMessage.error('加载话题失败')
  }
}

/**
 * 删除话题
 * @param {number} id - 话题ID
 */
const deleteTopic = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个话题吗？', '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await apiDeleteTopic(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadTopics(topicsPage.value)
    }
  } catch (error) {
    if (error === 'cancel') {
      return
    }
    console.error('删除话题失败:', error)
    ElMessage.error('删除失败')
  }
}

/**
 * 加载书单列表
 * @param {number} page - 页码
 */
const loadBooklists = async (page = 0) => {
  try {
    const res = await getBooklists(page, 20)
    if (res.code === 200 && res.data) {
      booklists.value = res.data.content
      booklistsPage.value = res.data.currentPage
      booklistsTotalPages.value = res.data.totalPages
      booklistsTotal.value = res.data.totalElements
    }
  } catch (error) {
    console.error('加载书单失败:', error)
    ElMessage.error('加载书单失败')
  }
}

/**
 * 删除书单
 * @param {number} id - 书单ID
 */
const deleteBooklist = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个书单吗？', '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await apiDeleteBooklist(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadBooklists(booklistsPage.value)
    }
  } catch (error) {
    if (error === 'cancel') {
      return
    }
    console.error('删除书单失败:', error)
    ElMessage.error('删除失败')
  }
}

/**
 * 退出登录
 * 清除用户状态并跳转到首页
 */
const logout = () => {
  userStore.logout()
  ElMessage.success('已退出登录')
  window.location.href = 'http://localhost:8081/'
}

/**
 * 监听侧边栏导航切换
 * 根据选中的菜单项加载对应数据
 */
watch(activeSection, (newSection) => {
  if (newSection === 'chapters') {
    loadChapters()
  } else if (newSection === 'novelManage') {
    loadAllNovels()
  } else if (newSection === 'categories') {
    loadCategories()
  } else if (newSection === 'users') {
    loadUsers()
  } else if (newSection === 'comments') {
    loadComments()
  } else if (newSection === 'topics') {
    loadTopics()
  } else if (newSection === 'booklists') {
    loadBooklists()
  }
})

/**
 * 组件挂载时初始化
 * 检查管理员权限并加载初始数据
 */
onMounted(() => {
  if (checkAdminAuth()) {
    loadStats()
    loadTrends()
    loadChapters()
  }
})
</script>

<style scoped>
.admin-page {
  display: flex;
  min-height: calc(100vh - 140px);
  background: linear-gradient(135deg, var(--color-bg-primary) 0%, var(--color-bg-tertiary) 100%);
  position: relative;
}

.admin-sidebar {
  width: 260px;
  background: linear-gradient(180deg, #161b22 0%, #0d1117 100%);
  color: var(--color-text-primary);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  box-shadow: 4px 0 20px rgba(0, 0, 0, 0.3);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
}

.admin-logo {
  padding: var(--spacing-2xl) var(--spacing-lg);
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, transparent 100%);
}

.admin-logo h2 {
  margin: 0 0 var(--spacing-xs) 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #fff 0%, rgba(255, 255, 255, 0.8) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.admin-username {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  font-weight: 500;
}

.sidebar-nav {
  padding: var(--spacing-md) 0;
  flex: 1;
  overflow-y: auto;
}

.nav-item {
  display: block;
  padding: var(--spacing-md) var(--spacing-lg);
  margin: var(--spacing-xs) var(--spacing-md);
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  transition: all var(--transition-base);
  text-decoration: none;
  border-radius: var(--radius-lg);
  font-weight: 500;
  font-size: 15px;
  position: relative;
  overflow: hidden;
}

.nav-item:hover {
  color: white;
  background: rgba(255, 255, 255, 0.1);
  transform: translateX(4px);
}

.nav-item.active {
  color: #0d1117;
  background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-light) 100%);
  box-shadow: 0 4px 12px rgba(201, 169, 98, 0.3);
}

.nav-item.logout {
  margin-top: auto;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  margin-top: var(--spacing-lg);
  background: linear-gradient(135deg, rgba(245, 108, 108, 0.2) 0%, rgba(245, 108, 108, 0.1) 100%);
}

.nav-item.logout:hover {
  background: linear-gradient(135deg, rgba(245, 108, 108, 0.3) 0%, rgba(245, 108, 108, 0.2) 100%);
}

.admin-content {
  flex: 1;
  padding: var(--spacing-2xl);
  background: linear-gradient(135deg, var(--color-bg-primary) 0%, var(--color-bg-tertiary) 100%);
  overflow-y: auto;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: var(--spacing-lg);
  margin-bottom: var(--spacing-2xl);
}

.stat-card {
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  padding: var(--spacing-xl);
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  box-shadow: var(--shadow-md);
  border: 1px solid var(--color-border);
  transition: all var(--transition-base);
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-xl);
  border-color: var(--color-primary);
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  box-shadow: var(--shadow-md);
}

.stat-icon.users { 
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
}
.stat-icon.novels { 
  background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%);
}
.stat-icon.views { 
  background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%);
}
.stat-icon.comments { 
  background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-accent) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-family: var(--font-serif);
  margin-bottom: var(--spacing-xs);
}

.stat-label {
  color: var(--color-text-secondary);
  font-size: 14px;
  font-weight: 500;
}

.chart-section {
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  padding: var(--spacing-xl);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--color-border);
}

.chart-section h3 {
  margin-bottom: var(--spacing-xl);
  color: var(--color-text-primary);
  font-size: 20px;
  font-weight: 700;
  font-family: var(--font-serif);
}

.trend-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.trend-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  padding: var(--spacing-md);
  background: linear-gradient(135deg, rgba(26, 54, 93, 0.05) 0%, rgba(214, 158, 46, 0.05) 100%);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  transition: all var(--transition-base);
}

.trend-item:hover {
  border-color: var(--color-primary);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.trend-name {
  width: 100px;
  font-weight: 700;
  color: var(--color-text-primary);
  font-size: 15px;
}

.trend-bar {
  flex: 1;
  height: 24px;
  background: var(--color-bg-secondary);
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--color-border);
}

.trend-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--color-primary) 0%, var(--color-accent) 100%);
  border-radius: var(--radius-lg);
  transition: width var(--transition-base);
  box-shadow: 0 2px 8px rgba(201, 169, 98, 0.3);
}

.trend-count {
  width: 80px;
  text-align: right;
  color: var(--color-text-secondary);
  font-weight: 600;
  font-size: 14px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-xl);
  padding-bottom: var(--spacing-lg);
  border-bottom: 2px solid var(--color-border);
}

.section-header h2 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 24px;
  font-weight: 700;
  font-family: var(--font-serif);
}

.filter-bar {
  display: flex;
  gap: var(--spacing-md);
  align-items: center;
}

.filter-bar input,
.filter-bar select,
.search-bar input {
  padding: var(--spacing-sm) var(--spacing-md);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  font-size: 14px;
  background: rgba(255, 255, 255, 0.03);
  color: var(--color-text-primary);
  transition: all var(--transition-base);
}

.filter-bar select option {
  background: var(--color-bg-secondary);
  color: var(--color-text-primary);
  padding: 10px;
}

.filter-bar input:focus,
.filter-bar select:focus,
.search-bar input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(201, 169, 98, 0.1);
  background: rgba(201, 169, 98, 0.05);
}

.filter-bar input::placeholder {
  color: var(--color-text-tertiary);
}

.filter-bar button,
.search-bar button {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: #0d1117;
  border: none;
  border-radius: var(--radius-lg);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-base);
}

.filter-bar button:hover,
.search-bar button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(201, 169, 98, 0.3);
}

.search-bar {
  display: flex;
  gap: var(--spacing-sm);
}

.create-btn {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-light) 100%);
  color: white;
  border: none;
  border-radius: var(--radius-lg);
  cursor: pointer;
  font-weight: 600;
  transition: all var(--transition-base);
  box-shadow: var(--shadow-sm);
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.novels-table,
.users-table {
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--color-border);
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: var(--spacing-md);
  text-align: left;
  border-bottom: 1px solid var(--color-border);
}

th {
  background: linear-gradient(135deg, rgba(26, 54, 93, 0.05) 0%, rgba(214, 158, 46, 0.05) 100%);
  font-weight: 700;
  color: var(--color-text-primary);
  font-size: 15px;
  font-family: var(--font-serif);
}

tr:hover {
  background: linear-gradient(135deg, rgba(26, 54, 93, 0.03) 0%, rgba(214, 158, 46, 0.03) 100%);
}

.status-tag,
.status-badge {
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  display: inline-block;
}

.status-tag.ONGOING,
.status-tag.ACTIVE,
.status-badge.normal {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.15) 0%, rgba(59, 130, 246, 0.25) 100%);
  color: #60a5fa;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.status-tag.COMPLETED,
.status-badge.approved {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.15) 0%, rgba(34, 197, 94, 0.25) 100%);
  color: #4ade80;
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.status-badge.ongoing {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.15) 0%, rgba(59, 130, 246, 0.25) 100%);
  color: #60a5fa;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.status-badge.completed {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.15) 0%, rgba(34, 197, 94, 0.25) 100%);
  color: #4ade80;
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.status-tag.BANNED,
.status-badge.banned {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.15) 0%, rgba(239, 68, 68, 0.25) 100%);
  color: #f87171;
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.role-tag {
  padding: var(--spacing-xs) var(--spacing-sm);
  border-radius: var(--radius-md);
  font-size: 12px;
  font-weight: 600;
}

.role-tag.ADMIN {
  background: linear-gradient(135deg, #722ed1 0%, #9254de 100%);
  color: white;
}

.role-tag.USER {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.15) 100%);
  color: var(--color-text-secondary);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.action-btns {
  display: flex;
  gap: var(--spacing-xs);
  flex-wrap: wrap;
}

.action-btns button {
  padding: var(--spacing-xs) var(--spacing-sm);
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: all var(--transition-base);
  box-shadow: var(--shadow-sm);
}

.btn-view {
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  color: white;
}

.btn-view:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.btn-approve {
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
  color: white;
}

.btn-approve:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.btn-reject {
  background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
  color: white;
}

.btn-reject:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.btn-ban {
  background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
  color: white;
}

.btn-ban:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.btn-status {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: #0d1117;
}

.btn-status:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.btn-unban {
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
  color: white;
}

.btn-unban:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.btn-delete {
  background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
  color: white;
}

.btn-delete:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.header-actions {
  display: flex;
  gap: var(--spacing-md);
  align-items: center;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--spacing-md);
  margin-top: var(--spacing-xl);
  padding: var(--spacing-lg);
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  border: 1px solid var(--color-border);
}

.page-btn {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  font-weight: 600;
  transition: all var(--transition-base);
  color: var(--color-text-primary);
}

.page-btn:hover:not(:disabled) {
  border-color: var(--color-primary);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: var(--color-text-secondary);
  font-weight: 600;
  font-size: 14px;
}

.empty-data {
  text-align: center;
  padding: var(--spacing-2xl);
  color: var(--color-text-tertiary);
  font-size: 15px;
  font-weight: 500;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal-content {
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  padding: var(--spacing-2xl);
  border-radius: var(--radius-xl);
  width: 400px;
  max-width: 90%;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
  border: 1px solid var(--color-border);
}

.modal-content h2,
.modal-content h3 {
  margin: 0 0 var(--spacing-xl) 0;
  color: var(--color-text-primary);
  font-size: 22px;
  font-weight: 700;
  font-family: var(--font-serif);
}

.form-group {
  margin-bottom: var(--spacing-lg);
}

.form-group label {
  display: block;
  margin-bottom: var(--spacing-xs);
  color: var(--color-text-primary);
  font-size: 15px;
  font-weight: 600;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: var(--spacing-md);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  font-size: 14px;
  background: var(--color-bg-secondary);
  color: var(--color-text-primary);
  transition: all var(--transition-base);
  box-sizing: border-box;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(201, 169, 98, 0.1);
  background: rgba(201, 169, 98, 0.05);
  transform: translateY(-2px);
}

.form-group textarea {
  resize: vertical;
  min-height: 100px;
  font-family: var(--font-sans);
}

.modal-actions,
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-md);
  margin-top: var(--spacing-xl);
}

.btn-cancel {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  font-weight: 600;
  transition: all var(--transition-base);
  color: var(--color-text-primary);
}

.btn-cancel:hover {
  border-color: var(--color-text-tertiary);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.btn-confirm,
.submit-btn {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: white;
  border: none;
  border-radius: var(--radius-lg);
  cursor: pointer;
  font-weight: 600;
  transition: all var(--transition-base);
  box-shadow: var(--shadow-sm);
}

.btn-confirm:hover,
.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.categories-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.category-item {
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  padding: var(--spacing-lg) var(--spacing-xl);
  border-radius: var(--radius-xl);
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: var(--shadow-md);
  border: 1px solid var(--color-border);
  transition: all var(--transition-base);
}

.category-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-primary);
}

.category-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
}

.category-icon {
  font-size: 36px;
}

.category-details h4 {
  margin: 0 0 var(--spacing-xs) 0;
  color: var(--color-text-primary);
  font-size: 18px;
  font-weight: 700;
  font-family: var(--font-serif);
}

.category-details p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.novel-count {
  margin-left: var(--spacing-lg);
  padding: var(--spacing-xs) var(--spacing-md);
  background: linear-gradient(135deg, rgba(26, 54, 93, 0.1) 0%, rgba(214, 158, 46, 0.1) 100%);
  border-radius: var(--radius-lg);
  font-size: 13px;
  color: var(--color-primary);
  font-weight: 600;
  border: 1px solid transparent;
  transition: all var(--transition-base);
}

.category-item:hover .novel-count {
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: white;
  border-color: var(--color-primary);
}

.category-actions {
  display: flex;
  gap: var(--spacing-sm);
}

.action-btn {
  padding: var(--spacing-xs) var(--spacing-md);
  border: 2px solid var(--color-border);
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  border-radius: var(--radius-lg);
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all var(--transition-base);
  color: var(--color-text-primary);
}

.action-btn:hover {
  border-color: var(--color-primary);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.action-btn.danger {
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.action-btn.danger:hover {
  background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%);
  color: white;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.comment-item {
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  padding: var(--spacing-lg) var(--spacing-xl);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  border: 1px solid var(--color-border);
  transition: all var(--transition-base);
}

.comment-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-primary);
}

.comment-header {
  display: flex;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-md);
  font-size: 14px;
  align-items: center;
}

.commenter {
  font-weight: 700;
  color: var(--color-primary);
  font-size: 15px;
}

.novel-title {
  color: var(--color-text-secondary);
  font-size: 14px;
}

.comment-time {
  color: var(--color-text-tertiary);
  font-size: 13px;
  margin-left: auto;
}

.comment-content {
  margin: 0 0 var(--spacing-md) 0;
  color: var(--color-text-secondary);
  line-height: 1.8;
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-sm);
}

.topics-section,
.booklists-section {
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  padding: var(--spacing-xl);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--color-border);
}

.topics-table,
.booklists-table {
  width: 100%;
  overflow-x: auto;
}

.topics-table table,
.booklists-table table {
  width: 100%;
  border-collapse: collapse;
}

.content-cell {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text-secondary);
}

.page-input {
  width: 60px;
  padding: 4px 8px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  text-align: center;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.03);
  color: var(--color-text-primary);
}

.page-input:focus {
  border-color: var(--color-primary);
  outline: none;
}

.jump-btn {
  padding: 4px 12px;
  border: 1px solid var(--color-primary);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: #0d1117;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s;
}

.jump-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(201, 169, 98, 0.3);
}

.chapter-modal {
  max-width: 800px;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--color-border);
}

.modal-header h2 {
  margin: 0;
  color: var(--color-text-primary);
  font-family: var(--font-serif);
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: all var(--transition-base);
  padding: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
}

.close-btn:hover {
  background: var(--color-bg-tertiary);
  color: var(--color-primary);
}

.chapter-info {
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  padding: var(--spacing-md) var(--spacing-lg);
  border-radius: var(--radius-lg);
  margin-bottom: var(--spacing-lg);
  border: 1px solid var(--color-border);
}

.chapter-info p {
  margin: var(--spacing-xs) 0;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.chapter-info strong {
  color: var(--color-text-primary);
}

.chapter-content {
  margin-top: var(--spacing-lg);
}

.chapter-content h3 {
  margin: 0 0 var(--spacing-md) 0;
  color: var(--color-text-primary);
  font-size: 18px;
  font-family: var(--font-serif);
}

.content-text {
  background: linear-gradient(135deg, var(--color-bg-secondary) 0%, var(--color-bg-tertiary) 100%);
  padding: var(--spacing-lg);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  line-height: 1.8;
  color: var(--color-text-primary);
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 400px;
  overflow-y: auto;
}

.btn-save {
  padding: var(--spacing-sm) var(--spacing-lg);
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: white;
  border: none;
  border-radius: var(--radius-lg);
  cursor: pointer;
  font-weight: 600;
  transition: all var(--transition-base);
  box-shadow: var(--shadow-sm);
}

.btn-save:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.novel-status-form {
  padding: var(--spacing-xl);
}

.novel-status-form .form-group {
  margin-bottom: var(--spacing-lg);
}

.novel-status-form .form-group label {
  display: block;
  margin-bottom: var(--spacing-sm);
  font-size: 14px;
  color: var(--color-text-primary);
  font-weight: 500;
}

.novel-status-form .form-group select {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: 14px;
  background: rgba(255, 255, 255, 0.03);
  color: var(--color-text-primary);
  cursor: pointer;
  transition: all 0.3s;
}

.novel-status-form .form-group select:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(201, 169, 98, 0.1);
}

.novel-status-form .form-group select option {
  background: var(--color-bg-secondary);
  color: var(--color-text-primary);
  padding: 10px;
}

@media (max-width: 768px) {
  .admin-sidebar {
    width: 70px;
  }

  .admin-logo h2,
  .admin-username {
    display: none;
  }

  .nav-item {
    padding: var(--spacing-md);
    text-align: center;
    margin: var(--spacing-xs);
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .admin-content {
    padding: var(--spacing-md);
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-md);
  }

  .filter-bar,
  .search-bar {
    flex-wrap: wrap;
    width: 100%;
  }

  .filter-bar select,
  .search-bar input {
    flex: 1;
    min-width: 150px;
  }
}
</style>
