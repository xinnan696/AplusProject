<template>
  <div class="auth-page">
    <div class="auth-box">
      <img src="@/assets/images/LOGO.png" alt="UrbanFlow Logo" class="logo-image" />
      <div class="system-title">URBANFLOW SYSTEM</div>

      <form @submit.prevent="handleLogin">
        <div class="input-group">
          <img src="@/assets/icons/user-icon.svg" class="input-icon" alt="Account Number Icon" />
          <input
            v-model="loginForm.accountNumber"
            type="text"
            placeholder="Account Number"
            @input="clearError('accountNumber')"
          />
          <p v-if="errors.accountNumber" class="error-text">{{ errors.accountNumber }}</p>
        </div>

        <div class="input-group">
          <img src="@/assets/icons/password-icon.svg" class="input-icon" alt="Password Icon" />
          <input
            v-model="loginForm.password"
            type="password"
            placeholder="Password"
            @input="clearError('password')"
          />
          <p v-if="errors.password" class="error-text">{{ errors.password }}</p>
        </div>

        <button
          class="submit-button"
          type="submit"
          :disabled="loading"
        >
          {{ loading ? 'LOGGING IN...' : 'LOGIN' }}
        </button>
      </form>

      <div class="forgot-password" @click="goToForgot">Forgot password?</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { toast } from '@/utils/ToastService';


const router = useRouter();
const authStore = useAuthStore();

const loginForm = reactive({
  accountNumber: '',
  password: ''
});

const errors = reactive({
  accountNumber: '',
  password: '',
});

const loading = ref(false);

const clearError = (field: 'accountNumber' | 'password') => {
  if (errors[field]) {
    errors[field] = '';
  }
  errors.api = '';
};

const handleLogin = async () => {
  Object.keys(errors).forEach(key => errors[key] = '');

  let hasValidationErrors = false;
  if (!loginForm.accountNumber) {
    errors.accountNumber = "AccountNumber cannot be blanked";
    hasValidationErrors = true;
  }
  if (!loginForm.password) {
    errors.password = "Password cannot be blanked";
    hasValidationErrors = true;
  }

  // If there are validation errors, stop the submission process.
  if (hasValidationErrors) {
    return;
  }

  loading.value = true;
  try {
    await authStore.login(loginForm);
  } catch (error: any) {
    const errorMessage = error.response?.data?.message || 'Login failed. Please try again later.';
    toast.error(errorMessage);
  } finally {
    loading.value = false;
  }
};

const goToForgot = () => {
  router.push({ name: 'ForgetPassword' });
};
</script>

<style scoped>
.auth-page { width: 100vw; height: 100vh; display: flex; justify-content: center; align-items: center; background-image: url('@/assets/images/LoginBg.png'); background-size: cover; background-position: center; }
.auth-box {
  width: 420px;
  padding: 40px;
  background-color: #2c2f48;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
  color: #fff;
  text-align: center;
  display: flex;
  flex-direction: column;
}
.logo-image {
  width: 100px; /* Adjusted logo size */
  height: auto;
  margin: 0 auto 25px;
}
.system-title { font-size: 26px; font-weight: 600; color: #00e3ff; margin-bottom: 40px; letter-spacing: 1px; }
.input-group { position: relative; width: 100%; margin-bottom: 35px; }
.input-icon { position: absolute; left: 18px; top: 14px; width: 20px; height: 20px; opacity: 0.6; }
.input-group input { width: 100%; padding: 14px 15px 14px 50px; border-radius: 8px; border: 1px solid #00e3ff; background-color: transparent; color: #fff; font-size: 16px; outline: none; }
.input-group input::placeholder { color: rgba(255, 255, 255, 0.7); }
.submit-button { width: 100%; padding: 14px; background-color: #00e3ff; color: #1E1E2F; font-weight: bold; border: none; border-radius: 8px; cursor: pointer; transition: background-color 0.3s; font-size: 16px; margin-top: 10px; }
.forgot-password { margin-top: 25px; font-size: 14px; color: rgba(255, 255, 255, 0.8); cursor: pointer; }
.error-text { position: absolute; left: 0; bottom: -22px; color: #FF4D4F; font-size: 12px; text-align: left; }
</style>
