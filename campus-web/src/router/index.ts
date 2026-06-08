import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/',
      name: 'Chat',
      component: () => import('@/views/chat/ChatView.vue'),
      meta: { title: 'AI 对话', requiresAuth: true },
    },
    {
      path: '/history',
      name: 'History',
      component: () => import('@/views/history/HistoryView.vue'),
      meta: { title: '历史会话', requiresAuth: true },
    },
    {
      path: '/courses',
      name: 'Courses',
      component: () => import('@/views/course/CourseListView.vue'),
      meta: { title: '课程学习' },
    },
    {
      path: '/courses/:id',
      name: 'CourseDetail',
      component: () => import('@/views/course/CourseDetailView.vue'),
      meta: { title: '课程详情' },
    },
    {
      path: '/square',
      name: 'Square',
      component: () => import('@/views/square/SquareView.vue'),
      meta: { title: '问答广场' },
    },
    {
      path: '/square/:id',
      name: 'SquareDetail',
      component: () => import('@/views/square/SquareDetailView.vue'),
      meta: { title: '问答详情' },
    },
    {
      path: '/knowledge',
      name: 'Knowledge',
      component: () => import('@/views/knowledge/KnowledgeView.vue'),
      meta: { title: '知识库管理', requiresAuth: true, adminOnly: true },
    },
    {
      path: '/announcements',
      name: 'Announcements',
      component: () => import('@/views/announcement/AnnouncementListView.vue'),
      meta: { title: '公告通知' },
    },
    {
      path: '/announcements/:id',
      name: 'AnnouncementDetail',
      component: () => import('@/views/announcement/AnnouncementDetailView.vue'),
      meta: { title: '公告详情' },
    },
    {
      path: '/lost-found',
      name: 'LostFound',
      component: () => import('@/views/lostfound/LostFoundListView.vue'),
      meta: { title: '失物招领' },
    },
    {
      path: '/lost-found/:id',
      name: 'LostFoundDetail',
      component: () => import('@/views/lostfound/LostFoundDetailView.vue'),
      meta: { title: '失物招领详情' },
    },
    {
      path: '/my',
      name: 'My',
      component: () => import('@/views/my/MyView.vue'),
      meta: { title: '我的', requiresAuth: true },
    },
    {
      path: '/messages',
      name: 'Messages',
      component: () => import('@/views/message/MessageView.vue'),
      meta: { title: '消息', requiresAuth: true },
    },
    {
      path: '/admin/announcements',
      name: 'AdminAnnouncements',
      component: () => import('@/views/admin/AdminAnnouncementView.vue'),
      meta: { title: '公告管理', requiresAuth: true, adminOnly: true },
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('accessToken')
  const role = localStorage.getItem('role')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.meta.adminOnly && role !== 'ADMIN') {
    next('/')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
