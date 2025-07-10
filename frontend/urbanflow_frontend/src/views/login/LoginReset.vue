<template>
  <div class="auth-page">
    <div class="auth-box">
      <img src="@/assets/images/LOGO.png" alt="UrbanFlow Logo" class="logo-image" />
      <div class="system-title">RESET PASSWORD</div>
      <p class="page-description">Enter new password for your account</p>

      <form @submit.prevent="handleResetPassword" novalidate>
        <div class="input-group">
          <img src="@/assets/icons/password-icon.svg" class="input-icon" alt="Password Icon" />
          <input v-model="form.newPassword" type="password" placeholder="New Password" @input="clearError('newPassword')" />
          <p v-if="errors.newPassword" class="error-text">{{ errors.newPassword }}</p>
        </div>
        <div class="input-group">
          <img src="@/assets/icons/password-icon.svg" class="input-icon" alt="Password Icon" />
          <input v-model="form.confirmPassword" type="password" placeholder="Confirm Password" @input="clearError('confirmPassword')" />
          <p v-if="errors.confirmPassword" class="error-text">{{ errors.confirmPassword }}</p>
        </div>
        <button class="submit-button" type="submit" :disabled="loading || !form.token">
          {{ loading ? 'RESETTING...' : 'RESET' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { toast } from '@/utils/ToastService';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const form = reactive({ token: '', newPassword: '', confirmPassword: '' });
const errors = reactive({ newPassword: '', confirmPassword: '' });
const loading = ref(false);

onMounted(() => {
  form.token = route.query.token as string;
  if (!form.token) {
    toast.error('Invalid or missing reset token link.');
  }
});

const clearError = (field: 'newPassword' | 'confirmPassword') => { errors[field] = ''; };

const handleResetPassword = async () => {
  Object.keys(errors).forEach(key => (errors[key] = ''));
  let hasErrors = false;

  // --- Frontend Validation ---
  if (!form.newPassword) { errors.newPassword = "Password cannot be blank"; hasErrors = true; }
  else if (form.newPassword.length < 8) { errors.newPassword = "Password must be at least 8 characters"; hasErrors = true; }
  else if (!/^(?=.*[A-Za-z])(?=.*\d).+$/.test(form.newPassword)) { errors.newPassword = "Password must include letters and numbers!"; hasErrors = true; }

  if (form.newPassword !== form.confirmPassword) {
    errors.confirmPassword = "Passwords do not match";
    hasErrors = true;
  }
  if (hasErrors) return;
  // --- End of Frontend Validation ---

  loading.value = true;
  try {
    // This is the critical part that calls the backend.
    const payload = { token: form.token, newPassword: form.newPassword };
    const response = await authStore.resetPassword(payload);

    toast.success(response.data.message || 'Password reset successfully.');
    setTimeout(() => router.push({ name: 'Login' }), 2000);

  } catch (error: any) {
    // If the API call fails, show the error from the backend in a toast.
    const errorMessage = error.response?.data?.message || 'Failed to reset password. The link may have expired.';
    toast.error(errorMessage);
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
/* Using the same consistent styles as the other auth pages */
.auth-page { width: 100vw; height: 100vh; display: flex; justify-content: center; align-items: center; background-image: url('@/assets/images/LoginBg.png'); background-size: cover; background-position: center; }
.auth-box { width: 420px; padding: 40px; background-color: #2c2f48; border-radius: 16px; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5); color: #fff; text-align: center; display: flex; flex-direction: column; }
.logo-image { width: 80px; height: auto; margin: 0 auto 20px; }
.system-title { font-size: 26px; font-weight: 600; color: #00e3ff; margin-bottom: 15px; }
.page-description { font-size: 16px; color: rgba(255, 255, 255, 0.7); margin-bottom: 30px; }
.input-group { position: relative; width: 100%; margin-bottom: 35px; }
.input-icon { position: absolute; left: 18px; top: 14px; width: 20px; height: 20px; opacity: 0.6; }
.input-group input { width: 100%; padding: 14px 15px 14px 50px; border-radius: 8px; border: 1px solid #00e3ff; background-color: transparent; color: #fff; font-size: 16px; outline: none; }
.submit-button { width: 100%; padding: 14px; background-color: #00e3ff; color: #1E1E2F; font-weight: bold; border: none; border-radius: 8px; cursor: pointer; transition: all 0.3s; font-size: 16px; }
.error-text { position: absolute; left: 0; bottom: -22px; color: #FF4D4F; font-size: 12px; text-align: left; }
</style>
