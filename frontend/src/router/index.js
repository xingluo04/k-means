import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/',
    component: () => import('../layout/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '数据看板', roles: ['admin', 'teacher', 'student', 'parent'] } },
      { path: 'user', name: 'User', component: () => import('../views/system/UserManage.vue'), meta: { title: '用户管理', roles: ['admin'] } },
      { path: 'class', name: 'Class', component: () => import('../views/system/ClassManage.vue'), meta: { title: '班级管理', roles: ['admin', 'teacher'] } },
      { path: 'student', name: 'Student', component: () => import('../views/system/StudentManage.vue'), meta: { title: '学生管理', roles: ['admin', 'teacher'] } },
      { path: 'moral', name: 'Moral', component: () => import('../views/evaluation/MoralEval.vue'), meta: { title: '德育测评', roles: ['admin', 'teacher'] } },
      { path: 'academic', name: 'Academic', component: () => import('../views/evaluation/AcademicEval.vue'), meta: { title: '智育测评', roles: ['admin', 'teacher'] } },
      { path: 'physical', name: 'Physical', component: () => import('../views/evaluation/PhysicalEval.vue'), meta: { title: '体育测评', roles: ['admin', 'teacher'] } },
      { path: 'art', name: 'Art', component: () => import('../views/evaluation/ArtEval.vue'), meta: { title: '美育测评', roles: ['admin', 'teacher'] } },
      { path: 'practice', name: 'Practice', component: () => import('../views/evaluation/PracticeEval.vue'), meta: { title: '劳动教育测评', roles: ['admin', 'teacher'] } },
      { path: 'comprehensive', name: 'Comprehensive', component: () => import('../views/evaluation/ComprehensiveEval.vue'), meta: { title: '综合素质评价', roles: ['admin', 'teacher'] } },
      { path: 'import', name: 'Import', component: () => import('../views/evaluation/ImportData.vue'), meta: { title: '数据导入', roles: ['admin', 'teacher'] } },
      { path: 'cluster', name: 'Cluster', component: () => import('../views/analysis/ClusterAnalysis.vue'), meta: { title: '聚类分析', roles: ['admin', 'teacher'] } },
      { path: 'my-evaluation', name: 'MyEvaluation', component: () => import('../views/student/MyEvaluation.vue'), meta: { title: '我的评价', roles: ['student'] } },
      { path: 'child-evaluation', name: 'ChildEvaluation', component: () => import('../views/parent/ChildEvaluation.vue'), meta: { title: '孩子评价', roles: ['parent'] } },
      { path: 'notice', name: 'Notice', component: () => import('../views/system/NoticeManage.vue'), meta: { title: '通知管理', roles: ['admin'] } },
      { path: 'notice-list', name: 'NoticeList', component: () => import('../views/NoticeList.vue'), meta: { title: '系统通知', roles: ['admin', 'teacher', 'student', 'parent'] } },
      { path: 'log', name: 'Log', component: () => import('../views/system/LogManage.vue'), meta: { title: '操作日志', roles: ['admin'] } },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/* 路由守卫 */
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path === '/login' || to.path === '/register') {
    next()
  } else if (!token) {
    next('/login')
  } else {
    /* 权限校验 */
    const role = localStorage.getItem('role')
    if (to.meta.roles && !to.meta.roles.includes(role)) {
      next('/dashboard')
    } else {
      next()
    }
  }
})

export default router
