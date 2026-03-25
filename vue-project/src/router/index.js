import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

// 获取应用类型
const appType = import.meta.env.VITE_APP_TYPE || 'user'

// 用户端路由 - 只允许 USER 角色
const userRoutes = [
  {
    path: '/',
    name: 'home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login.vue'),
    meta: { allowedRoles: ['USER'] }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/novel/:id',
    name: 'novelDetail',
    component: () => import('../views/NovelDetail.vue')
  },
  {
    path: '/novel/:id/chapter/:chapterId',
    name: 'chapterReader',
    component: () => import('../views/ChapterReader.vue')
  },
  {
    path: '/search',
    name: 'search',
    component: () => import('../views/Search.vue')
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true, allowedRoles: ['USER'] }
  },
  {
    path: '/bookshelf',
    name: 'bookshelf',
    component: () => import('../views/Bookshelf.vue'),
    meta: { requiresAuth: true, allowedRoles: ['USER'] }
  },
  {
    path: '/history',
    name: 'history',
    component: () => import('../views/History.vue'),
    meta: { requiresAuth: true, allowedRoles: ['USER'] }
  },
  {
    path: '/category/:id',
    name: 'category',
    component: () => import('../views/Category.vue')
  },
  {
    path: '/community',
    name: 'community',
    component: () => import('../views/Community.vue')
  },
  {
    path: '/my-comments',
    name: 'myComments',
    component: () => import('../views/MyComments.vue'),
    meta: { requiresAuth: true, allowedRoles: ['USER'] }
  },
  {
    path: '/my-booklists',
    name: 'myBooklists',
    component: () => import('../views/MyBooklists.vue'),
    meta: { requiresAuth: true, allowedRoles: ['USER'] }
  },
  {
    path: '/booklist/:id',
    name: 'booklistDetail',
    component: () => import('../views/BooklistDetail.vue')
  },
  {
    path: '/my-topics',
    name: 'myTopics',
    component: () => import('../views/MyTopics.vue'),
    meta: { requiresAuth: true, allowedRoles: ['USER'] }
  },
  {
    path: '/topic/:id',
    name: 'topicDetail',
    component: () => import('../views/TopicDetail.vue')
  },
  {
    path: '/recommendations/:type',
    name: 'recommendationList',
    component: () => import('../views/RecommendationList.vue')
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'notFound',
    component: () => import('../views/NotFound.vue')
  }
]

// 作者端路由 - 只允许 AUTHOR 角色
const authorRoutes = [
  {
    path: '/',
    redirect: '/author'
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/author',
    name: 'authorCenter',
    component: () => import('../views/AuthorCenter_Optimized.vue'),
    meta: { requiresAuth: true, allowedRoles: ['AUTHOR'] }
  },
  {
    path: '/author/novel/create',
    name: 'authorNovelCreate',
    component: () => import('../views/AuthorNovelEdit.vue'),
    meta: { requiresAuth: true, allowedRoles: ['AUTHOR'] }
  },
  {
    path: '/author/novel/:id/edit',
    name: 'authorNovelEdit',
    component: () => import('../views/AuthorNovelEdit.vue'),
    meta: { requiresAuth: true, allowedRoles: ['AUTHOR'] }
  },
  {
    path: '/author/novel/:novelId/chapters',
    name: 'authorChapterManage',
    component: () => import('../views/AuthorChapterManage.vue'),
    meta: { requiresAuth: true, allowedRoles: ['AUTHOR'] }
  },

  {
    path: '/:pathMatch(.*)*',
    name: 'notFound',
    component: () => import('../views/NotFound.vue')
  }
]

// 管理后端路由 - 只允许 ADMIN 角色
const adminRoutes = [
  {
    path: '/',
    redirect: '/admin'
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login.vue'),
    meta: { allowedRoles: ['ADMIN'] }
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('../views/Admin.vue'),
    meta: { requiresAuth: true, allowedRoles: ['ADMIN'] }
  },
  {
    path: '/novel/:id',
    name: 'novelDetail',
    component: () => import('../views/NovelDetail.vue'),
    meta: { requiresAuth: true, allowedRoles: ['ADMIN'] }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'notFound',
    component: () => import('../views/NotFound.vue')
  }
]

// 根据应用类型选择路由
const routes = appType === 'author' ? authorRoutes : appType === 'admin' ? adminRoutes : userRoutes

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    // 如果有保存的位置（浏览器前进/后退），使用它
    if (savedPosition) {
      return savedPosition
    } else {
      // 否则滚动到顶部
      return { top: 0 }
    }
  }
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 根据应用类型获取对应的token（严格区分，不回退）
  let token = null
  if (appType === 'author') {
    token = localStorage.getItem('authorToken')
  } else if (appType === 'admin') {
    token = localStorage.getItem('adminToken')
  } else {
    token = localStorage.getItem('token')
  }
  const userRole = localStorage.getItem('userRole')

  // 检查是否需要登录
  if (to.meta.requiresAuth && !token) {
    ElMessage.warning('请先登录')
    next('/login')
    return
  }

  // 检查角色权限
  if (to.meta.allowedRoles && userRole) {
    if (!to.meta.allowedRoles.includes(userRole)) {
      ElMessage.error('您没有权限访问此页面')
      // 根据角色跳转到对应首页
      if (userRole === 'AUTHOR') {
        router.push('/author')
      } else if (userRole === 'ADMIN') {
        router.push('/admin')
      } else {
        router.push('/')
      }
      return
    }
  }

  // 如果已退出登录（没有 token），但有 userRole，清除 userRole
  if (!token && userRole) {
    localStorage.removeItem('userRole')
  }

  next()
})

export default router
