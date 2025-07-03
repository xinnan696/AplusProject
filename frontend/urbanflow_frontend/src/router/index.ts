import { createRouter, createWebHistory } from 'vue-router'

// 路由配置
const routes = [
  // 🔸 根路径重定向到登录页
  {
    path: '/',
    redirect: '/login'
  },

  // 🔸 登录相关页面
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginLogin.vue'),
    meta: {
      requiresAuth: false,
      title: 'Login - UrbanFlow'
    }
  },

  {
    path: '/forget',
    name: 'ForgetPassword',
    component: () => import('@/views/login/LoginForget.vue'),
    meta: {
      requiresAuth: false,
      title: 'Forgot Password - UrbanFlow'
    }
  },

  {
    path: '/reset',
    name: 'ResetPassword',
    component: () => import('@/views/login/LoginReset.vue'),
    meta: {
      requiresAuth: false,
      title: 'Reset Password - UrbanFlow'
    }
  },

  // 🔸 控制台主页
  {
    path: '/control',
    name: 'Control',
    component: () => import('@/views/control/ControlHome.vue'),
    meta: {
      requiresAuth: true,
      title: 'Traffic Control - UrbanFlow'
    }
  },

  // 🔸 仪表板页面
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/DashBoard.vue'),
    meta: {
      requiresAuth: true,
      title: 'Dashboard - UrbanFlow'
    }
  },

  // 🔸 用户管理页面
  {
    path: '/user',
    name: 'UserList',
    component: () => import('@/views/user/UserList.vue'),
    meta: {
      requiresAuth: true,
      title: 'User Management - UrbanFlow'
    }
  },

  // 🔸 用户日志页面
  {
    path: '/user/log',
    name: 'UserLog',
    component: () => import('@/views/user/UserLog.vue'),
    meta: {
      requiresAuth: true,
      title: 'User Logs - UrbanFlow'
    }
  },
  
  // 🔸 帮助页面
  {
    path: '/help',
    name: 'Help',
    component: () => import('@/views/help/HelpPage.vue'),
    meta: {
      requiresAuth: true,
      title: 'Help - UrbanFlow'
    }
  },

  // 🔸 404错误页面处理
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: {
      title: 'Page Not Found - UrbanFlow'
    }
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(), // 使用History模式
  routes
})

// 🔸 全局前置守卫
router.beforeEach((to) => {
  // 设置页面标题
  if (to.meta?.title) {
    document.title = to.meta.title as string
  }

  // 检查是否需要认证
  if (to.meta?.requiresAuth) {
    const isAuthenticated = checkAuthStatus()

    if (!isAuthenticated) {
      return { name: 'Login' }
    }
  }

  // 如果已登录用户访问登录页，重定向到控制台
  if (to.name === 'Login' && checkAuthStatus()) {
    return { name: 'Control' }
  }
})

// 🔸 认证状态检查 - 使用localStorage
function checkAuthStatus(): boolean {
  const token = localStorage.getItem('authToken')
  return !!token && token !== 'expired'
}

export default router
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import ControlHome from '@/views/control/ControlHome.vue'
import MapPage from '@/views/Map/MapPage.vue'
import LoginLogin from '@/views/login/LoginLogin.vue'
import LoginReset from '@/views/login/LoginReset.vue'
import LoginForget from '@/views/login/LoginForget.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'LoginLogin',
    component: LoginLogin
  },
  {
    path: '/forget',
    name: 'LoginForget',
    component: LoginForget
  },
  {
    path: '/reset',
    name: 'LoginReset',
    component: LoginReset
  },
  {
    path: '/',
    redirect: '/control'
  },
  {
    path: '/control',
    name: 'ControlHome',
    component: ControlHome
  },
  {
  path: '/map',
  name: 'MapEdge',
  component: MapPage
}

]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
