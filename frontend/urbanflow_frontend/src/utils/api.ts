import axios, { type InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/stores/auth';

const apiClient = axios.create({
baseURL: 'http://localhost:8081/api',
headers: {
'Content-Type': 'application/json',
},
});

// Axios Request Interceptor
apiClient.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const authStore = useAuthStore();
        const token = authStore.token;

        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  // The second function handles error responses.
  (error: AxiosError) => {
    // Check if the error has a response object and if the status is 401.
    if (error.response && error.response.status === 401) {
      console.warn('Authentication token is invalid or expired. Auto-logging out.');

      // Get an instance of the auth store.
      const authStore = useAuthStore();

      // Call the centralized logout action. This will clear user state,
      // remove the token from localStorage, and redirect to the login page.
      authStore.logout();
    }

    return Promise.reject(error);
  }
);
export default apiClient;
