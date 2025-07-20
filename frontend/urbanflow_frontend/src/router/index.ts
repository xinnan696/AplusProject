import { createRouter, createWebHistory } from 'vue-router'

const routes = [

  // 紧急车辆追踪页面的路由
  {
    path: '/control/tracking',
    name: 'PriorityVehicleTracking',
    component: () => import('@/views/control/PriorityVehicleTracking.vue'),
    meta: {
      requiresAuth: true,
      // 权限与主控制页面保持一致
      roles: ['ADMIN', 'Traffic Manager'],
      title: 'Vehicle Tracking - UrbanFlow'
    }
  },

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
      roles: ['ADMIN', 'Traffic Manager'],
      title: 'Control - UrbanFlow'
    }
  },


  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/DashBoard.vue'),
    meta: {
      requiresAuth: true,
      roles: ['ADMIN', 'Traffic Manager', 'Traffic Planner'],
      title: 'Dashboard - UrbanFlow'
    }
  },

  {
    path: '/user',
    name: 'UserList',
    component: () => import('@/views/user/UserList.vue'),
    meta: {
      requiresAuth: true,
      roles: ['ADMIN'],
      title: 'User Management - UrbanFlow'
    }
  },

  {
    path: '/user/add',
    name: 'AddUser',
    component: () => import('@/views/user/AddUser.vue'),
    meta: {
      requiresAuth: true,
      roles: ['ADMIN'],
      title: 'Add User - UrbanFlow'
    }
  },

  {
    path: '/user/edit/:id',
    name: 'EditUser',
    component: () => import('@/views/user/EditUser.vue'),
    meta: {
      requiresAuth: true,
      roles: ['ADMIN'],
      title: 'Edit User - UrbanFlow'
    }
  },

  {
    path: '/user/details/:id',
    name: 'UserDetails',
    component: () => import('@/views/user/UserDetails.vue'),
    meta: {
      requiresAuth: true,
      roles: ['ADMIN'],
      title: 'User Details - UrbanFlow'
    }
  },

  {
    path: '/user/log',
    name: 'UserLog',
    component: () => import('@/views/user/UserLog.vue'),
    meta: {
      requiresAuth: true,
      roles: ['ADMIN'],
      title: 'User Logs - UrbanFlow'
    }
  },

  {
    path: '/help',
    name: 'Help',
    component: () => import('@/views/help/HelpPage.vue'),
    meta: {
      requiresAuth: true,
      roles: ['ADMIN', 'Traffic Manager', 'Traffic Planner'],
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
    'Traffic Manager': 'Control',
    'Traffic Planner': 'Dashboard'
  }

  return defaultPages[role as keyof typeof defaultPages] || 'Dashboard'
}

router.beforeEach((to, from) => {
  console.log('Route guard check:', {
    to: to.path,
    requiresAuth: to.meta?.requiresAuth,
    roles: to.meta?.roles,
    authStatus: checkAuthStatus()
  })

  if (to.meta?.title) {
    document.title = to.meta.title as string
  }

  if (isInTransition()) {
    return false
  }

  if (to.meta?.requiresAuth) {
    const isAuthenticated = checkAuthStatus()

    if (!isAuthenticated) {
      console.log('Redirecting to login: not authenticated')
      return { name: 'Login' }
    }

    if (to.meta?.roles && Array.isArray(to.meta.roles)) {
      const user = JSON.parse(localStorage.getItem('user') || 'null')

      if (!user || !user.role) {
        console.log('Redirecting to login: no user role found')
        return { name: 'Login' }
      }

      const hasPermission = (to.meta.roles as string[]).includes(user.role)

      if (!hasPermission) {
        // @ts-ignore
        console.log(`Access denied: ${user.role} cannot access ${to.name}`)

        const defaultPage = getDefaultPageForRole(user.role)
        console.log(`Redirecting to default page: ${defaultPage}`)

        return { name: defaultPage }
      }
    }
  }

  if (to.name === 'Login' && checkAuthStatus()) {
    const user = JSON.parse(localStorage.getItem('user') || 'null')
    const defaultPage = getDefaultPageForRole(user?.role)
    console.log(`Redirecting to ${defaultPage}: already authenticated`)
    return { name: defaultPage }
  }
})

function checkAuthStatus(): boolean {
  const token = localStorage.getItem('authToken')
  return !!token && token !== 'expired'
}

function isInTransition(): boolean {

  return false
}

export default router
