import { createRouter, createWebHistory } from 'vue-router'

// 添加全局类型声明
declare global {
  interface Window {
    __routerRedirectCount: number
  }
}

const routes = [
  {
    path: '/',
    redirect: '/login'
  },


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

  {
    path: '/control',
    name: 'Control',
    component: () => import('@/views/control/ControlHome.vue'),
    meta: {
      requiresAuth: true,
      roles: ['Admin', 'Traffic Manager'],
      title: 'Control - UrbanFlow'
    }
  },


  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/DashBoard.vue'),
    meta: {
      requiresAuth: true,
      roles: ['Admin', 'Traffic Manager', 'Traffic Planner'],
      title: 'Dashboard - UrbanFlow'
    }
  },

  {
    path: '/user',
    name: 'UserList',
    component: () => import('@/views/user/UserList.vue'),
    meta: {
      requiresAuth: true,
      roles: ['Admin'],
      title: 'User Management - UrbanFlow'
    }
  },

  {
    path: '/user/add',
    name: 'AddUser',
    component: () => import('@/views/user/AddUser.vue'),
    meta: {
      requiresAuth: true,
      roles: ['Admin'],
      title: 'Add User - UrbanFlow'
    }
  },

  {
    path: '/user/edit/:id',
    name: 'EditUser',
    component: () => import('@/views/user/EditUser.vue'),
    meta: {
      requiresAuth: true,
      roles: ['Admin'],
      title: 'Edit User - UrbanFlow'
    }
  },

  {
    path: '/user/details/:id',
    name: 'UserDetails',
    component: () => import('@/views/user/UserDetails.vue'),
    meta: {
      requiresAuth: true,
      roles: ['Admin'],
      title: 'User Details - UrbanFlow'
    }
  },

  {
    path: '/user/log',
    name: 'UserLog',
    component: () => import('@/views/user/UserLog.vue'),
    meta: {
      requiresAuth: true,
      roles: ['Admin'],
      title: 'User Logs - UrbanFlow'
    }
  },

  {
    path: '/help',
    name: 'Help',
    component: () => import('@/views/help/HelpPage.vue'),
    meta: {
      requiresAuth: true,
      roles: ['Admin', 'Traffic Manager', 'Traffic Planner'],
      title: 'Help - UrbanFlow'
    }
  },

  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: {
      title: 'Page Not Found - UrbanFlow'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})


function getDefaultPageForRole(role: string): string {
  const defaultPages = {
    'ADMIN': 'Control',
    'Admin': 'Control',
    'admin': 'Control',
    'Traffic Manager': 'Control',
    'Traffic Planner': 'Dashboard'
  }

  console.log('🔍 [Router] Getting default page for role:', role)
  const defaultPage = defaultPages[role as keyof typeof defaultPages] || 'Dashboard'
  console.log('✅ [Router] Default page determined:', defaultPage)

  return defaultPage
}

router.beforeEach((to, from) => {
  console.log('🔍 Route guard check:', {
    to: to.path,
    from: from.path,
    toName: to.name,
    fromName: from.name,
    requiresAuth: to.meta?.requiresAuth,
    roles: to.meta?.roles
  })

  if (to.meta?.title) {
    document.title = to.meta.title as string
  }

  if (isInTransition()) {
    console.log('🚫 Navigation blocked: in transition')
    return false
  }

  if (!window.__routerRedirectCount) window.__routerRedirectCount = 0

  if (window.__routerRedirectCount > 5) {
    localStorage.clear()
    window.__routerRedirectCount = 0
    if (to.name !== 'Login') {
      return { name: 'Login' }
    }
    return true
  }

  const token = localStorage.getItem('authToken')
  const userStr = localStorage.getItem('user')

  if (token === 'mock-auth-token-for-testing' ||
      (userStr && userStr.includes('Test Admin'))) {
    console.log('🗑️ Clearing mock data immediately')
    localStorage.removeItem('authToken')
    localStorage.removeItem('user')
  }

  const isAuthenticated = checkAuthStatus()
  console.log('🔐 Auth status:', isAuthenticated)

  if (to.name === 'Login') {
    if (isAuthenticated) {
      if (from.name === 'Login') {
        console.log('🛑 Login->Login cycle detected, clearing auth and allowing')
        localStorage.clear()
        window.__routerRedirectCount = 0
        return true
      }

      const user = JSON.parse(localStorage.getItem('user') || 'null')
      if (user && user.role) {
        const defaultPage = getDefaultPageForRole(user.role)
        window.__routerRedirectCount++
        return { name: defaultPage }
      }
    }
    console.log('✅ Allowing access to login page')
    window.__routerRedirectCount = 0
    return true
  }

  if (to.meta?.requiresAuth) {
    if (!isAuthenticated) {
      console.log('🚫 Not authenticated, redirecting to login')
      window.__routerRedirectCount++
      return { name: 'Login' }
    }

    if (to.meta?.roles && Array.isArray(to.meta.roles)) {
      const user = JSON.parse(localStorage.getItem('user') || 'null')

      if (!user || !user.role) {
        console.log('🚫 No user role, redirecting to login')
        localStorage.removeItem('authToken')
        localStorage.removeItem('user')
        window.__routerRedirectCount++
        return { name: 'Login' }
      }

      const roleMapping = {
        'ADMIN': 'Admin',
        'Admin': 'Admin',
        'admin': 'Admin',
        'TRAFFIC_MANAGER': 'Traffic Manager',
        'Traffic Manager': 'Traffic Manager',
        'traffic_manager': 'Traffic Manager',
        'TRAFFIC_PLANNER': 'Traffic Planner',
        'Traffic Planner': 'Traffic Planner',
        'traffic_planner': 'Traffic Planner'
      }

      const mappedRole = roleMapping[user.role as keyof typeof roleMapping] || user.role

      const hasPermission = (to.meta.roles as string[]).includes(mappedRole)

      if (!hasPermission) {
        const defaultPage = getDefaultPageForRole(user.role)

        if (to.name === defaultPage) {
          window.__routerRedirectCount = 0
          return true
        }

        window.__routerRedirectCount++
        return { name: defaultPage }
      }
    }
  }

  window.__routerRedirectCount = 0
  return true
})

function checkAuthStatus(): boolean {
  const token = localStorage.getItem('authToken')
  const user = localStorage.getItem('user')
  const isValidToken = token &&
                      token !== 'expired' &&
                      token !== 'mock-auth-token-for-testing' &&
                      token.length > 10

  const isValidUser = user &&
                     user !== 'null' &&
                     !user.includes('Test Admin')

  const isAuthenticated = !!(isValidToken && isValidUser)


  return isAuthenticated
}

function isInTransition(): boolean {

  return false
}

export default router
