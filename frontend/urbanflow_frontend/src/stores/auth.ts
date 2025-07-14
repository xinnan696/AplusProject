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
}

export const useAuthStore = defineStore('auth', () => {
// --- STATE ---
const token = ref<string | null>(localStorage.getItem('authToken'));
const user = ref<User | null>(JSON.parse(localStorage.getItem('user') || 'null'));

// --- GETTERS ---
const isAuthenticated = computed(() => !!token.value);
const userRole = computed(() => user.value?.role || 'GUEST');

// --- ACTIONS ---

/**
* Handles user login by calling the backend API.
*/
async function login(credentials: object) {
      const response = await apiClient.post('/auth/login', credentials);
      const responseData = response.data.data;

      token.value = responseData.token;
      user.value = responseData.user;

      localStorage.setItem('authToken', token.value);
      localStorage.setItem('user', JSON.stringify(user.value));

      await router.push({ name: 'Control' });
    }

    /**
     * Clears authentication state and redirects to the login page.
     */
    function logout() {
      token.value = null;
      user.value = null;
      localStorage.removeItem('authToken');
      localStorage.removeItem('user');
      router.push({ name: 'Login' });
    }

    // ... other actions (forgotPassword, resetPassword) remain the same ...

    return {
      token,
      user,
      isAuthenticated,
      userRole,
      login,
      logout,
    };
});
