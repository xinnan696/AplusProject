<template>
  <div class="auth-page">
    <div class="auth-box">
      <div class="logo">Logo</div>
      <div class="system-title">FORGOT PASSWORD</div>

      <p class="page-description">Enter your email to reset your password</p>

      <div v-if="message" class="api-message" :class="{ 'is-error': isError }">
        {{ message }}
      </div>

      <form @submit.prevent="handleForgotPassword" v-if="!isSuccess">
        <div class="input-group">
          <img src="@/assets/icons/email-icon.svg" class="input-icon" alt="Email Icon" />
          <input
            v-model="email"
            type="email"
            placeholder="Email"
            @input="message = ''"
          />
        </div>

        <div class="button-group">
          <button type="button" class="submit-button secondary" @click="$router.push('/login')">CANCEL</button>
          <button type="submit" class="submit-button" :disabled="loading">
            {{ loading ? 'SENDING...' : 'SUBMIT' }}
          </button>
        </div>
      </form>

      <div v-if="isSuccess" class="button-group">
         <button class="submit-button" @click="$router.push('/login')">BACK TO LOGIN</button>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useAuthStore } from '@/stores/auth';

const email = ref('');
const loading = ref(false);
const message = ref('');
const isError = ref(false);
const isSuccess = ref(false);
const authStore = useAuthStore();

const handleForgotPassword = async () => {
  message.value = '';
  isError.value = false;

  if (!email.value) {
    isError.value = true;
    message.value = 'Email cannot be blank.';
    return;
  }

  loading.value = true;
  try {
    const response = await authStore.forgotPassword({ email: email.value });
    message.value = response.data.message || 'Request successful. Please check your email.';
    isSuccess.value = true;
  } catch (error: any) {
    message.value = error.response?.data?.message || 'Failed to send reset link.';
    isError.value = true;
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
/* Reusing styles from Login page and adding specifics */
.auth-page { width: 100vw; height: 100vh; display: flex; justify-content: center; align-items: center; background-image: url('@/assets/images/LoginBg.png'); background-size: cover; background-position: center; }
.auth-box { width: 480px; padding: 40px 50px; background-color: #1E1E2F; border-radius: 24px; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3); color: #fff; text-align: center; display: flex; flex-direction: column; }
.logo { font-size: 24px; font-weight: bold; letter-spacing: 2px; margin-bottom: 20px; text-transform: uppercase; }
.system-title { font-size: 28px; font-weight: bold; color: #00e3ff; margin-bottom: 20px; }
.page-description { font-size: 16px; color: rgba(255, 255, 255, 0.7); margin-bottom: 30px; }
.input-group { position: relative; width: 100%; margin-bottom: 25px; }
.input-icon { position: absolute; left: 15px; top: 50%; transform: translateY(-50%); width: 20px; height: 20px; }
.input-group input { width: 100%; padding: 14px 15px 14px 50px; border-radius: 8px; border: 1px solid #00e3ff; background-color: transparent; color: #fff; font-size: 16px; outline: none; }
.button-group { display: flex; gap: 20px; margin-top: 30px; }
.submit-button { flex: 1; padding: 15px; background-color: #00e3ff; color: #1E1E2F; font-weight: bold; border: none; border-radius: 8px; cursor: pointer; transition: all 0.3s; font-size: 16px; }
.submit-button.secondary { background-color: #404060; color: #fff; }
.submit-button:hover:not(.secondary) { background-color: #00bcd4; }
.submit-button.secondary:hover { background-color: #555; }
.api-message { padding: 10px; border-radius: 8px; margin-bottom: 20px; border: 1px solid; font-size: 14px; }
.api-message.is-error { color: #FF4D4F; background-color: rgba(255, 77, 79, 0.1); border-color: #FF4D4F; }
.api-message:not(.is-error) { color: #52c41a; background-color: #f6ffed; border-color: #b7eb8f; }
</style>
