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
import { reactive, ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { toast } from '@/utils/ToastService';
const adjustToastPosition = () => {
  const toastElements = document.querySelectorAll('.toast');
  toastElements.forEach(toast => {
    if (document.querySelector('.auth-page')) {
      toast.style.visibility = 'hidden';
      toast.style.opacity = '0';

      toast.style.position = 'fixed';
      toast.style.top = '1rem';
      toast.style.left = '50%';
      toast.style.transform = 'translateX(-50%)';
      toast.style.zIndex = '9999';

      requestAnimationFrame(() => {
        toast.style.visibility = 'visible';
        toast.style.opacity = '1';
      });
    }
  });
};

let observer: MutationObserver | null = null;


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

onMounted(() => {
  observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      if (mutation.type === 'childList') {
        mutation.addedNodes.forEach((node) => {
          if (node.nodeType === Node.ELEMENT_NODE) {
            const element = node as Element;
            if (element.classList?.contains('toast') || element.querySelector?.('.toast')) {
              adjustToastPosition();
            }
          }
        });
      }
    });
  });

  observer.observe(document.body, {
    childList: true,
    subtree: true
  });

  setTimeout(adjustToastPosition, 100);
});

onUnmounted(() => {
  if (observer) {
    observer.disconnect();
    observer = null;
  }
});
</script>

<style scoped>
.auth-page { width: 100vw; height: 100vh; display: flex; justify-content: center; align-items: center; background-image: url('@/assets/images/LoginBg.png'); background-size: cover; background-position: center; }
.auth-box {
  width: 420px;
  padding: 35px;
  padding-left: 45px;
  padding-right: 45px;
  padding-top: 20px;
  background-color: #2c2f48;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
  color: #fff;
  text-align: center;
  display: flex;
  flex-direction: column;
}
.logo-image {
  width: 1.74rem; /* Adjusted logo size */
  height: auto;
  margin: 0 auto 0;
}
.system-title { font-size: 26px; font-weight: 600; color: #00b4d8; margin-bottom: 40px; letter-spacing: 1px; }
.input-group { position: relative; width: 100%; margin-bottom: 35px; }
.input-icon { position: absolute; left: 18px; top: 14px; width: 20px; height: 20px; opacity: 0.6; }
.input-group input { width: 100%; padding: 14px 15px 14px 50px; border-radius: 8px; border: 1px solid #00b4d8; background-color: transparent; color: #FFFFFF; font-size: 16px; outline: none; }
.input-group input::placeholder { color: #999999; }
.submit-button { width: 100%; padding: 14px; background-color: #00b4d8; color: #FFFFFF; font-weight: bold; border: none; border-radius: 8px; cursor: pointer; transition: background-color 0.3s; font-size: 16px; margin-top: 10px; }
.forgot-password { margin-top: 25px; font-size: 14px; color: rgba(255, 255, 255, 0.8); cursor: pointer; }
.error-text { position: absolute; left: 0; bottom: -22px; color: #FF4D4F; font-size: 12px; text-align: left; }
</style>

<!-- Override toast position for login page only -->
<style>

.auth-page .toast {
  position: fixed !important;
  top: 1rem !important;
  left: 50% !important;
  transform: translateX(-50%) !important;
  z-index: 9999 !important;
  width: 455px !important;
  height: 40px !important;
}

body .auth-page .toast,
.auth-page .toast {
  position: fixed !important;
  top: 1rem !important;
  left: 50% !important;
  transform: translateX(-50%) !important;
  z-index: 9999 !important;
  width: 455px !important;
  height: 40px !important;
}


body .auth-page .toast-fade-enter-active,
body .auth-page .toast-fade-leave-active,
.auth-page .toast-fade-enter-active,
.auth-page .toast-fade-leave-active {
  transition: opacity 0.4s ease, transform 0.4s ease !important;
}

body .auth-page .toast-fade-enter-from,
.auth-page .toast-fade-enter-from {
  opacity: 0 !important;
  transform: translateX(-50%) translateY(-1rem) scale(0.8) !important;
}

body .auth-page .toast-fade-enter-to,
.auth-page .toast-fade-enter-to {
  opacity: 1 !important;
  transform: translateX(-50%) translateY(0) scale(1) !important;
}

body .auth-page .toast-fade-leave-from,
.auth-page .toast-fade-leave-from {
  opacity: 1 !important;
  transform: translateX(-50%) translateY(0) scale(1) !important;
}

body .auth-page .toast-fade-leave-to,
.auth-page .toast-fade-leave-to {
  opacity: 0 !important;
  transform: translateX(-50%) translateY(-0.3rem) scale(0.95) !important;
}
</style>
