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

// --- GETTERS ---
const isAuthenticated = computed(() => !!token.value);
const userRole = computed(() => user.value?.role || 'GUEST');

// --- ACTIONS ---

/**
* Handles user login by calling the backend API.
*/
async function login(credentials: LoginRequest) {
      try {
        loading.value = true;
        error.value = '';

        const response = await apiClient.post('/auth/login', credentials);
        const apiResponse: ApiResponse<{ token: string, user: User }> = response.data;

        if (apiResponse.statusCode === 200) {
          const responseData = apiResponse.data;

          token.value = responseData.token;
          user.value = responseData.user;

          localStorage.setItem('authToken', token.value);
          localStorage.setItem('user', JSON.stringify(user.value));

          console.log('Login successful:', user.value);

          // Navigate to default page based on user role
          const defaultPage = getDefaultHomePage();
          await router.push({ name: defaultPage });
        } else {
          throw new Error(apiResponse.message);
        }
      } catch (err: any) {
        error.value = err.response?.data?.message || err.message || 'Login failed';
        console.error('Login error:', err);
        throw err;
      } finally {
        loading.value = false;
      }
    }

    /**
     * Clears authentication state and redirects to the login page.
     */
    function logout() {
      token.value = null;
      user.value = null;
      error.value = '';
      localStorage.removeItem('authToken');
      localStorage.removeItem('user');
      router.push({ name: 'Login' });
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
      return hasRole('ADMIN');
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

      // 角色权限映射
      const rolePermissions = {
        'ADMIN': [
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

    /**
     * 获取用户默认首页（根据角色）
     */
    function getDefaultHomePage(): string {
      if (!user.value || !user.value.role) {
        return 'Login';
      }

      const role = user.value.role;

      // 根据角色返回默认首页
      const defaultPages = {
        'ADMIN': 'Control',
        'Traffic Manager': 'Control',
        'Traffic Planner': 'Dashboard'
      };

      return defaultPages[role as keyof typeof defaultPages] || 'Dashboard';
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
      if (token.value && user.value) {

        console.log('Logged in user:', user.value);
      }
    }


    function startTransition() {
      showTransition.value = true;
    }


    function completeTransition() {
      showTransition.value = false;
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
    };
});
