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

// 动态调整弹窗位置的函数
const adjustToastPosition = () => {
  // 查找页面上的 toast 元素
  const toastElements = document.querySelectorAll('.toast');
  toastElements.forEach(toast => {
    // 检查是否在登录页面（通过检查是否存在 .auth-page 元素）
    if (document.querySelector('.auth-page')) {
      // 立即隐藏元素，避免闪烁
      toast.style.visibility = 'hidden';
      toast.style.opacity = '0';
      
      // 设置位置
      toast.style.position = 'fixed';
      toast.style.top = '1rem'; // 调整弹窗位置稍微往上
      toast.style.left = '50%'; // 水平居中
      toast.style.transform = 'translateX(-50%)'; // 只进行水平偏移
      toast.style.zIndex = '9999';
      
      // 强制重绘后再显示
      requestAnimationFrame(() => {
        toast.style.visibility = 'visible';
        toast.style.opacity = '1';
      });
    }
  });
};

// 使用 MutationObserver 监听 DOM 变化
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

// 设置 DOM 监听器
onMounted(() => {
  // 创建 MutationObserver 来监听 DOM 变化
  observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      if (mutation.type === 'childList') {
        // 检查是否有新增的 toast 元素
        mutation.addedNodes.forEach((node) => {
          if (node.nodeType === Node.ELEMENT_NODE) {
            const element = node as Element;
            if (element.classList?.contains('toast') || element.querySelector?.('.toast')) {
              // 立即调整位置，减少闪烁
              adjustToastPosition();
            }
          }
        });
      }
    });
  });

  // 开始监听整个文档的变化
  observer.observe(document.body, {
    childList: true,
    subtree: true
  });

  // 初始检查是否已经有 toast 元素
  setTimeout(adjustToastPosition, 100);
});

// 清理监听器
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
/* 预设忘记密码页面中 toast 的初始状态，减少闪烁 */
.auth-page .toast {
  position: fixed !important;
  top: 1rem !important;
  left: 50% !important;
  transform: translateX(-50%) !important;
  z-index: 9999 !important;
  width: 455px !important;
  height: 40px !important;
}

/* 更强的选择器优先级，确保在忘记密码页面覆盖弹窗位置 */
body .auth-page .toast,
.auth-page .toast {
  position: fixed !important;
  top: 1rem !important; /* 调整弹窗位置稍微往上 */
  left: 50% !important; /* 水平居中 */
  transform: translateX(-50%) !important; /* 只进行水平偏移 */
  z-index: 9999 !important;
  width: 455px !important;
  height: 40px !important;
}

/* 重写动画效果以配合新的定位 */
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
