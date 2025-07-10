<template>
  <div class="auth-page">
    <div class="auth-box">
      <img src="@/assets/images/LOGO.png" alt="UrbanFlow Logo" class="logo-image" />
      <div class="system-title">FORGOT PASSWORD</div>

      <p class="page-description">Enter your email to reset your password</p>

      <form @submit.prevent="handleForgotPassword" novalidate>
        <div class="input-group">
          <img src="@/assets/icons/email-icon.svg" class="input-icon" alt="Email Icon" />
          <input v-model="email" type="email" placeholder="Email" @input="error = ''"/>
          <p v-if="error" class="error-text">{{ error }}</p>
        </div>

        <div class="button-group">
          <button type="button" class="submit-button secondary" @click="$router.push('/login')">CANCEL</button>
          <button type="submit" class="submit-button" :disabled="loading">
            {{ loading ? 'SENDING...' : 'SUBMIT' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router'; // Import useRouter for navigation
import { useAuthStore } from '@/stores/auth';
import { toast } from '@/utils/ToastService';

const router = useRouter(); // Initialize router
const email = ref('');
const error = ref('');
const loading = ref(false);
const authStore = useAuthStore();

const handleForgotPassword = async () => {
  error.value = '';

  // --- Frontend Validation ---
  if (!email.value) {
    error.value = "Email cannot be blank";
    return;
  }
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(email.value)) {
    error.value = "Please enter a valid email address";
    return;
  }
  // --- End of Frontend Validation ---

  loading.value = true;
  try {
    // This is the critical part that calls the backend.
    const response = await authStore.forgotPassword({ email: email.value });

    // On success, show a toast message.
    toast.success(response.data.message || 'Reset link sent successfully! Please check your inbox.');

  } catch (err: any) {
    // If the API call fails, show the error from the backend in a toast.
    const errorMessage = err.response?.data?.message || 'Failed to send reset link. Please try again.';
    toast.error(errorMessage);
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.auth-page { width: 100vw; height: 100vh; display: flex; justify-content: center; align-items: center; background-image: url('@/assets/images/LoginBg.png'); background-size: cover; background-position: center; }
.auth-box { width: 420px; padding: 40px; background-color: #2c2f48; border-radius: 16px; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5); color: #fff; text-align: center; display: flex; flex-direction: column; }
.logo-image { width: 80px; height: auto; margin: 0 auto 25px; }
.system-title { font-size: 26px; font-weight: 600; color: #00e3ff; margin-bottom: 15px; letter-spacing: 1px; }
.page-description { font-size: 16px; color: rgba(255, 255, 255, 0.7); margin-bottom: 30px; }
.input-group { position: relative; width: 100%; margin-bottom: 35px; }
.input-icon { position: absolute; left: 18px; top: 14px; width: 20px; height: 20px; opacity: 0.6; }
.input-group input { width: 100%; padding: 14px 15px 14px 50px; border-radius: 8px; border: 1px solid #00e3ff; background-color: transparent; color: #fff; font-size: 16px; outline: none; }
.button-group { display: flex; gap: 20px; margin-top: 20px; }
.submit-button { flex: 1; padding: 14px; font-weight: bold; border: none; border-radius: 8px; cursor: pointer; transition: all 0.3s; font-size: 16px; }
.submit-button.secondary { background-color: transparent; color: #00e3ff; border: 1px solid #00e3ff; }
.submit-button:not(.secondary) { background-color: #00e3ff; color: #1E1E2F; }
.error-text { position: absolute; left: 0; bottom: -22px; color: #FF4D4F; font-size: 12px; text-align: left; }
</style>
