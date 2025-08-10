import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import apiClient from '@/utils/api';
import router from '@/router';

// Define the type interface for the UserVO object received from the backend.
interface User {
id: number;
accountNumber: string;
userName: string;
email: string;
role: string;
enabled: boolean;
managedAreas?: string[]; // 新增：管理区域列表
}

// 登录请求接口
interface LoginRequest {
  accountNumber: string;
  password: string;
}

// API 响应接口
interface ApiResponse<T> {
  statusCode: number;
  message: string;
  timestamp: number;
  data: T;
}

export const useAuthStore = defineStore('auth', () => {
// --- STATE ---
const token = ref<string | null>(localStorage.getItem('authToken'));
const user = ref<User | null>(JSON.parse(localStorage.getItem('user') || 'null'));
const loading = ref(false);
const error = ref('');

// Loading Transition 相关状态 - 暂时禁用动画
const showTransition = ref(false); // 设为 false 禁用过渡动画

// 🔧 新增：配置API客户端拦截器
function setupApiInterceptors() {
  // 请求拦截器 - 自动添加 Authorization header
  apiClient.interceptors.request.use(
    (config) => {
      const currentToken = token.value || localStorage.getItem('authToken');

      // 🔧 不需要认证的端点列表
      const publicEndpoints = [
        '/auth/login',
        '/auth/register',
        '/auth/forgot-password',
        '/auth/reset-password'
      ];

      // 🔧 检查是否是公开端点或明确跳过认证
      const isPublicEndpoint = publicEndpoints.some(endpoint => config.url?.includes(endpoint));
      const skipAuth = config.skipAuthHeader === true;

      console.log('🔍 [Auth] Request interceptor:', {
        url: config.url,
        method: config.method,
        isPublicEndpoint,
        skipAuth,
        hasToken: !!currentToken,
        tokenPreview: currentToken ? `${currentToken.substring(0, 20)}...` : 'none'
      });

      // 🔧 只有非公开端点且未明确跳过认证才添加 token
      if (!isPublicEndpoint && !skipAuth && currentToken) {
        config.headers.Authorization = `Bearer ${currentToken}`;
      } else {

      }

      return config;
    },
    (error) => {
      console.error(error);
      return Promise.reject(error);
    }
  );

  apiClient.interceptors.response.use(
    (response) => {

      return response;
    },
    (error) => {
      console.error('[Auth] Response error:', {
        status: error.response?.status,
        url: error.config?.url,
        message: error.response?.data?.message
      });

      // 处理 401 未授权错误
      if (error.response?.status === 401) {
        logout();
      }

      return Promise.reject(error);
    }
  );
}

// 🔧 新增：更新 token 并同步到 localStorage 和 API headers
function updateToken(newToken: string | null) {
  token.value = newToken;
  if (newToken) {
    localStorage.setItem('authToken', newToken);
    apiClient.defaults.headers.common['Authorization'] = `Bearer ${newToken}`;
  } else {
    localStorage.removeItem('authToken');
    delete apiClient.defaults.headers.common['Authorization'];
  }
}

const isAuthenticated = computed(() => !!token.value);
const userRole = computed(() => user.value?.role || 'GUEST');


    async function login(credentials: LoginRequest) {
      try {
        loading.value = true;
        error.value = '';


        const response = await apiClient.post('/auth/login', credentials);
        const apiResponse: ApiResponse<{ token: string, user: User }> = response.data;

        if (apiResponse.statusCode === 200) {
          const responseData = apiResponse.data;

          console.log('✅ [Auth] Login successful, received data:', {
            tokenPreview: responseData.token.substring(0, 20) + '...',
            user: responseData.user.userName,
            role: responseData.user.role,
            managedAreas: responseData.user.managedAreas
          });

          updateToken(responseData.token);
          user.value = responseData.user;

          localStorage.setItem('user', JSON.stringify(user.value));

          const defaultPage = getDefaultHomePage();

          if (window.__routerRedirectCount) {
            window.__routerRedirectCount = 0;
          }

          try {
            await router.push({ name: defaultPage });
          } catch (navError) {
            console.error( navError);
            window.location.href = `/${defaultPage.toLowerCase()}`;
          }

        } else {
          throw new Error(apiResponse.message);
        }
      } catch (err: any) {
        error.value = err.response?.data?.message || err.message || 'Login failed';
        console.error('[Auth] Login error:', err);
        throw err;
      } finally {
        loading.value = false;
      }
    }

    function logout() {

      updateToken(null);
      user.value = null;
      error.value = '';
      localStorage.removeItem('user');

      // 🔧 确保完全清理
      localStorage.removeItem('authToken');
      sessionStorage.clear();

      console.log('✅ [Auth] Logout completed');

      // 🔧 延迟导航，避免路由守卫冲突
      setTimeout(() => {
        router.replace({ name: 'Login' }).catch(err => {
          console.error('❌ [Auth] Logout navigation error:', err);
          window.location.href = '/login';
        });
      }, 100);
    }

    /**
     * 清除错误信息
     */
    function clearError() {
      error.value = '';
    }

    /**
     * 检查用户是否有特定权限
     */
    function hasRole(role: string): boolean {
      return user.value?.role === role;
    }

    /**
     * 检查用户是否是管理员
     */
    function isAdmin(): boolean {
      if (!user.value?.role) return false;
      const role = user.value.role.toLowerCase();
      return role === 'admin' || role === 'ADMIN';
    }

    /**
     * 检查是否为Traffic Manager
     */
    function isTrafficManager(): boolean {
      return user.value?.role === 'Traffic Manager';
    }

    /**
     * 检查是否为Traffic Planner
     */
    function isTrafficPlanner(): boolean {
      return user.value?.role === 'Traffic Planner';
    }

    /**
     * 检查用户是否有访问特定页面的权限
     */
    function hasPageAccess(pageName: string): boolean {
      if (!user.value || !user.value.role) {
        return false;
      }

      const role = user.value.role;
      console.log(`Checking page access: ${pageName} for role: ${role}`);

      const rolePermissions = {
        'ADMIN': [
          'Control', 'Dashboard', 'Help',
          'UserList', 'AddUser', 'EditUser', 'UserDetails', 'UserLog'
        ],
        'Admin': [
          'Control', 'Dashboard', 'Help',
          'UserList', 'AddUser', 'EditUser', 'UserDetails', 'UserLog'
        ],
        'admin': [
          'Control', 'Dashboard', 'Help',
          'UserList', 'AddUser', 'EditUser', 'UserDetails', 'UserLog'
        ],
        'Traffic Manager': [
          'Control', 'Dashboard', 'Help'
        ],
        'Traffic Planner': [
          'Dashboard', 'Help'
        ]
      };

      const allowedPages = rolePermissions[role as keyof typeof rolePermissions] || [];
      const hasAccess = allowedPages.includes(pageName);

      console.log(`Role ${role} has access to ${pageName}: ${hasAccess}`);
      return hasAccess;
    }


    function getDefaultHomePage(): string {
      if (!user.value || !user.value.role) {
        return 'Login';
      }

      const role = user.value.role;
      console.log('🔍 [Auth] Getting default page for role:', role);

      const defaultPages = {
        'ADMIN': 'Control',
        'Admin': 'Control',
        'admin': 'Control',
        'Traffic Manager': 'Control',
        'Traffic Planner': 'Dashboard'
      };

      const defaultPage = defaultPages[role as keyof typeof defaultPages] || 'Dashboard';

      return defaultPage;
    }

    function hasAreaAccess(areaName: string): boolean {
      if (!user.value || user.value.role !== 'Traffic Manager') {
        return false;
      }
      return user.value.managedAreas?.includes(areaName) || false;
    }

    function getManagedAreas(): string[] {
      if (!user.value || user.value.role !== 'Traffic Manager') {
        return [];
      }
      return user.value.managedAreas || [];
    }

    function canViewAllAreas(): boolean {
      return isAdmin() || (user.value?.role === 'Traffic Manager' && getManagedAreas().length > 0);
    }

    async function initAuth() {
      setupApiInterceptors();

      if (token.value && user.value) {
        console.log('[Auth] Found existing session:', {
          user: user.value.userName,
          role: user.value.role,
          tokenPreview: token.value.substring(0, 20) + '...'
        });
        apiClient.defaults.headers.common['Authorization'] = `Bearer ${token.value}`;
      } else {

      }
    }

    async function testTokenValidity() {
      if (!token.value) {
        return false;
      }

      try {

        const response = await apiClient.get('/auth/profile');

        return true;
      } catch (error) {
        console.error(error);
        if (error.response?.status === 401) {
          logout();
        }
        return false;
      }
    }

    function startTransition() {
      showTransition.value = true;
    }

    function completeTransition() {
      showTransition.value = false;
    }

    /**
   * @param payload - An object containing the user's email, e.g., { email: '...' }
   */
  async function forgotPassword(payload: object) {
    // This calls the backend API and returns the promise.
    return apiClient.post('/auth/forgot-password', payload);
  }

  /**
   * @param payload - An object containing the token and newPassword.
   */
  async function resetPassword(payload: object) {
    // This calls the backend API and returns the promise.
    return apiClient.post('/auth/reset-password', payload);
  }

    return {
      // State
      token,
      user,
      loading,
      error,
      showTransition,

      // Getters
      isAuthenticated,
      userRole,

      // Actions
      login,
      logout,
      clearError,
      hasRole,
      isAdmin,
      isTrafficManager,
      isTrafficPlanner,
      hasPageAccess,
      getDefaultHomePage,
      hasAreaAccess,
      getManagedAreas,
      canViewAllAreas,
      initAuth,
      startTransition,
      completeTransition,
      forgotPassword,
      resetPassword,
      testTokenValidity,
    };
});
