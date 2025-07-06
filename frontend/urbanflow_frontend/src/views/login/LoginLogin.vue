<template>
  <div class="auth-page">
    <div class="auth-box">
      <div class="logo">logo</div>
      <div class="system-title">URBANFLOW SYSTEM</div>

      <div v-if="errors.api" class="api-error-toast-style">
        <span class="iconfont icon-error">&#xe60b;</span>
        <span>{{ errors.api }}</span>
      </div>

      <form @submit.prevent="handleLogin">
        <div class="input-group">
          <img src="@/assets/icons/user-icon.svg" class="input-icon" alt="Account Number Icon" />
          <input
            v-model="loginForm.accountNumber"
            type="text"
            placeholder="Account Number"
            @input="clearError('accountNumber')"
            :class="{ 'input-error': errors.accountNumber }"
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
            :class="{ 'input-error': errors.password }"
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

const router = useRouter();
const authStore = useAuthStore();

const loginForm = reactive({
  accountNumber: '',
  password: ''
});

const errors = reactive({
  accountNumber: '',
  password: '',
  api: ''
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
    errors.api = error.response?.data?.message || 'Login failed. Please try again later.';
  } finally {
    loading.value = false;
  }
};

const goToForgot = () => {
  router.push({ name: 'ForgetPassword' });
};
</script>

<style scoped>
.auth-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-image: url('@/assets/images/LoginBg.png');
  background-size: cover;
  background-position: center;
}
.auth-box {
  width: 480px;
  padding: 40px 50px;
  background-color: #1E1E2F;
  border-radius: 24px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  color: #fff;
  text-align: center;
  display: flex;
  flex-direction: column;
}
.logo {
  font-size: 24px;
  font-weight: bold;
  letter-spacing: 2px;
  margin-bottom: 20px;
  text-transform: uppercase;
}
.system-title {
  font-size: 28px;
  font-weight: bold;
  color: #00e3ff;
  margin-bottom: 40px;
}
.input-group {
  position: relative;
  width: 100%;
  margin-bottom: 35px;
}
.input-icon {
  position: absolute;
  left: 15px;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
}
.input-group input {
  width: 100%;
  padding: 14px 15px 14px 50px;
  border-radius: 8px;
  border: 1px solid #00e3ff;
  background-color: transparent;
  color: #fff;
  font-size: 16px;
  outline: none;
}
.input-group input::placeholder {
  color: rgba(255, 255, 255, 0.7);
}
.submit-button {
  width: 100%;
  padding: 15px;
  background-color: #00e3ff;
  color: #1E1E2F;
  font-weight: bold;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.3s;
  font-size: 16px;
  margin-top: 10px;
}
.submit-button:hover {
  background-color: #00bcd4;
}
.forgot-password {
  margin-top: 20px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
}
.api-error-message {
  color: #FF4D4F;
  margin-bottom: 20px;
  padding: 10px;
  border: 1px solid #FF4D4F;
  border-radius: 8px;
}
.error-text {
  position: absolute;
  left: 0;
  bottom: -22px; /* Positions it perfectly below the input group */
  color: #FF4D4F;
  font-size: 12px;
  text-align: left;
}

.api-error-toast-style {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 15px;
  margin-bottom: 20px;
  border-radius: 8px;
  background-color: #2B2C3D;
  color: #E74C3C; /* Error color */
  border: 1px solid #E74C3C;
  font-size: 14px;
}

.api-error-toast-style .iconfont {
  font-size: 18px;
  margin-right: 10px;
}
</style>
