import axios, { type InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '@/stores/auth';

// Create a central Axios instance with a base configuration.
const apiClient = axios.create({
// Set the base URL for all API requests to your backend server.
baseURL: 'http://localhost:8081/api', // Please adjust the port if your backend runs on a different one.
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
        }

        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

export default apiClient;
