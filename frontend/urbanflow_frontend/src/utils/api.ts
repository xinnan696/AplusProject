import axios, { type InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/stores/auth';

const apiClient = axios.create({

baseURL: import.meta.env.DEV ? '/api' : 'http://localhost:8081/api',
headers: {
'Content-Type': 'application/json',
},
});

// Axios Request Interceptor
// This function will execute BEFORE every request is sent by this apiClient instance.
apiClient.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        // Get a reference to the Pinia auth store.
        // Note: Pinia store must be instantiated inside the interceptor, not outside.
        const authStore = useAuthStore();
        const token = authStore.token;



        // If a token exists in the store (meaning the user is logged in),
        // add the Authorization header to the request.
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
            console.log('✅ [API Interceptor] Authorization header added');
        } else {
            if (config.url && !config.url.includes('/api-status/')) {
                console.warn('⚠️ [API Interceptor] No token found for authenticated endpoint:', config.url);
            }
        }

        return config;
    },
    (error) => {
        console.error('❌ [API Interceptor] Request error:', error);
        return Promise.reject(error);
    }
);

apiClient.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    console.log('API Error intercepted:', {
      status: error.response?.status,
      url: error.config?.url,
      method: error.config?.method,
      data: error.response?.data
    })

    if (error.response?.status === 401) {
      console.log('401 Unauthorized - logging out user')
      const authStore = useAuthStore();
      authStore.logout();
    }

    if (error.response?.data?.message?.includes('JWT expired') ||
        error.response?.data?.error?.includes('ExpiredJwtException')) {
      console.log('JWT expired - automatically logging out user')
      const authStore = useAuthStore();
      authStore.logout();
    }

    return Promise.reject(error)
  }
)

export default apiClient;
