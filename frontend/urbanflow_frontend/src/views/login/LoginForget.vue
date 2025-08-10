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
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router'; // Import useRouter for navigation
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
.auth-box { width: 420px; padding: 40px; background-color: #2c2f48; border-radius: 16px; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5); color: #FFFFFF; text-align: center; display: flex; flex-direction: column; }
.logo-image { width: 1.74rem; height: auto; margin: 0 auto 0; }
.system-title { font-size: 26px; font-weight: 600; color: #00b4d8; margin-bottom: 15px; letter-spacing: 1px; }
.page-description { font-size: 16px; color: #FFFFFF; margin-bottom: 30px; }
.input-group { position: relative; width: 100%; margin-bottom: 35px; }
.input-icon { position: absolute; left: 18px; top: 14px; width: 20px; height: 20px; opacity: 0.6; }
.input-group input { width: 100%; padding: 14px 15px 14px 50px; border-radius: 8px; border: 1px solid #00e3ff; background-color: transparent; color: #fff; font-size: 16px; outline: none; }
.button-group { display: flex; gap: 20px; margin-top: 20px; }
.submit-button { flex: 1; padding: 14px; font-weight: bold; border: none; border-radius: 8px; cursor: pointer; transition: all 0.3s; font-size: 16px; }
.submit-button.secondary { background-color: transparent; color: #00b4d8; border: 1px solid #00b4d8; }
.submit-button:not(.secondary) { background-color: #00b4d8; color: #FFFFFF; }
.error-text { position: absolute; left: 0; bottom: -22px; color: #FF4D4F; font-size: 12px; text-align: left; }
</style>

<!-- Override toast position for forgot password page -->
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
