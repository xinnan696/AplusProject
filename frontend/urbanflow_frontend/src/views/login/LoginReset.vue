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
import { reactive, ref, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { toast } from '@/utils/ToastService';
import apiClient from '@/utils/api';

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

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const form = reactive({ token: '', newPassword: '', confirmPassword: '' });
const errors = reactive({ newPassword: '', confirmPassword: '' });
const loading = ref(false);

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
    console.log('🚀 [ResetPassword] Sending reset request:', {
      token: form.token,
      newPasswordLength: form.newPassword.length
    });

    const payload = {
      token: form.token,
      newPassword: form.newPassword
    };

    const response = await apiClient.post('/auth/reset-password', payload, {
      headers: {
        'Content-Type': 'application/json',
      },

      skipAuthHeader: true
    });

    console.log('[ResetPassword] Reset successful:', response.data);

    const message = response.data?.message || response.data?.data?.message || 'Password reset successfully.';
    toast.success(message);

    setTimeout(() => {
      router.push({ name: 'Login' });
    }, 2000);

  } catch (error: any) {
    console.error('[ResetPassword] Reset failed:', {
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data,
      message: error.message
    });

    // If the API call fails, show the error from the backend in a toast.
    const errorMessage = error.response?.data?.message ||
                        error.response?.data?.error ||
                        'Failed to reset password. The link may have expired.';
    toast.error(errorMessage);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  form.token = route.query.token as string;
  console.log('🔐 [ResetPassword] Token from URL:', form.token);

  if (!form.token) {
    toast.error('Invalid or missing reset token link.');
  }

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
/* Using the same consistent styles as the other auth pages */
.auth-page { width: 100vw; height: 100vh; display: flex; justify-content: center; align-items: center; background-image: url('@/assets/images/LoginBg.png'); background-size: cover; background-position: center; }
.auth-box { width: 420px; padding: 40px; background-color: #2c2f48; border-radius: 16px; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5); color: #FFFFFF; text-align: center; display: flex; flex-direction: column; }
.logo-image { width: 1.74rem; height: auto; margin: 0 auto 0; }
.system-title { font-size: 26px; font-weight: 600; color: #00b4d8; margin-bottom: 15px; }
.page-description { font-size: 16px; color: #FFFFFF; margin-bottom: 30px; }
.input-group { position: relative; width: 100%; margin-bottom: 35px; }
.input-icon { position: absolute; left: 18px; top: 14px; width: 20px; height: 20px; opacity: 0.6; }
.input-group input { width: 100%; padding: 14px 15px 14px 50px; border-radius: 8px; border: 1px solid #00b4d8; background-color: transparent; color: #FFFFFF; font-size: 16px; outline: none; }
.submit-button { width: 100%; padding: 14px; background-color: #00b4d8; color: #FFFFFF; font-weight: bold; border: none; border-radius: 8px; cursor: pointer; transition: all 0.3s; font-size: 16px; }
.error-text { position: absolute; left: 0; bottom: -22px; color: #FF4D4F; font-size: 12px; text-align: left; }
</style>

<!-- Override toast position for reset password page -->
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
