<template>
  <div class="auth-page">
    <div class="auth-box">
      <div class="logo">Logo</div>
      <div class="system-title">RESET PASSWORD</div>

      <p class="page-description">Enter new password for your account</p>

      <div v-if="message" class="api-message" :class="{ 'is-error': isError }">
        {{ message }}
      </div>

      <form @submit.prevent="handleResetPassword" v-if="!isSuccess">
        <div class="input-group">
          <img src="@/assets/icons/password-icon.svg" class="input-icon" alt="Password Icon" />
          <input
            v-model="form.newPassword"
            type="password"
            placeholder="Password"
          />
        </div>
        <div class="input-group">
          <img src="@/assets/icons/password-icon.svg" class="input-icon" alt="Password Icon" />
          <input
            v-model="form.confirmPassword"
            type="password"
            placeholder="Confirm Password"
          />
        </div>

        <button
          class="submit-button"
          type="submit"
          :disabled="loading || !form.token"
        >
          {{ loading ? 'RESETTING...' : 'RESET' }}
        </button>
      </form>

      <div v-if="isSuccess" class="button-group">
         <button class="submit-button" @click="$router.push('/login')">BACK TO LOGIN</button>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const form = reactive({
  token: '',
  newPassword: '',
  confirmPassword: ''
});
const loading = ref(false);
const message = ref('');
const isError = ref(false);
const isSuccess = ref(false);

onMounted(() => {
  form.token = route.query.token as string;
  if (!form.token) {
    isError.value = true;
    message.value = 'Invalid or missing reset token link.';
  }
});

const handleResetPassword = async () => {
  message.value = '';
  isError.value = false;

  if (!form.newPassword || !form.confirmPassword) {
    isError.value = true;
    message.value = 'Passwords cannot be blank.';
    return;
  }
  if (form.newPassword.length < 8) {
    isError.value = true;
    message.value = 'Password must be at least 8 characters.';
    return;
  }
  if (form.newPassword !== form.confirmPassword) {
    isError.value = true;
    message.value = 'Passwords do not match.';
    return;
  }

  loading.value = true;
  try {
    const payload = { token: form.token, newPassword: form.newPassword };
    const response = await authStore.resetPassword(payload);

    isSuccess.value = true;
    isError.value = false;
    message.value = (response.data.message || 'Password reset successfully.') + ' Redirecting to login...';

    setTimeout(() => router.push({ name: 'Login' }), 3000);

  } catch (error: any) {
    isError.value = true;
    message.value = error.response?.data?.message || 'Failed to reset password. The link may have expired.';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
/* Reusing styles from LoginForget page for consistency */
.auth-page { width: 100vw; height: 100vh; display: flex; justify-content: center; align-items: center; background-image: url('@/assets/images/LoginBg.png'); background-size: cover; background-position: center; }
.auth-box { width: 480px; padding: 40px 50px; background-color: #1E1E2F; border-radius: 24px; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3); color: #fff; text-align: center; display: flex; flex-direction: column; }
.logo { font-size: 24px; font-weight: bold; letter-spacing: 2px; margin-bottom: 20px; text-transform: uppercase; }
.system-title { font-size: 28px; font-weight: bold; color: #00e3ff; margin-bottom: 20px; }
.page-description { font-size: 16px; color: rgba(255, 255, 255, 0.7); margin-bottom: 30px; }
.input-group { position: relative; width: 100%; margin-bottom: 25px; }
.input-icon { position: absolute; left: 15px; top: 50%; transform: translateY(-50%); width: 20px; height: 20px; }
.input-group input { width: 100%; padding: 14px 15px 14px 50px; border-radius: 8px; border: 1px solid #00e3ff; background-color: transparent; color: #fff; font-size: 16px; outline: none; }
.submit-button { width: 100%; padding: 15px; background-color: #00e3ff; color: #1E1E2F; font-weight: bold; border: none; border-radius: 8px; cursor: pointer; transition: all 0.3s; font-size: 16px; margin-top: 10px; }
.button-group { display: flex; gap: 20px; margin-top: 30px; }
.api-message { padding: 10px; border-radius: 8px; margin-bottom: 20px; border: 1px solid; font-size: 14px; }
.api-message.is-error { color: #FF4D4F; background-color: rgba(255, 77, 79, 0.1); border-color: #FF4D4F; }
.api-message:not(.is-error) { color: #52c41a; background-color: #f6ffed; border-color: #b7eb8f; }
</style>
