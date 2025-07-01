<template>
  <Teleport to="body">
    <div class="toast-container" v-if="toasts.length > 0">
      <TransitionGroup name="toast" tag="div" class="toast-list">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          :class="[
            'toast',
            `toast--${toast.type}`
          ]"
          @click="removeToast(toast.id)"
        >
          <div class="toast__icon">
            <component :is="getIcon(toast.type)" />
          </div>
          
          <div class="toast__content">
            <div v-if="toast.title" class="toast__title">
              {{ toast.title }}
            </div>
            <div class="toast__message">
              {{ toast.message }}
            </div>
          </div>
          
          <button 
            class="toast__close"
            @click.stop="removeToast(toast.id)"
            aria-label="Close notification"
          >
            ×
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import type { ToastMessage } from '@/utils/ToastService'
import { useToasts } from '@/utils/ToastService'

// 使用 toast 服务
const { toasts: toastList, removeToast } = useToasts()

// 响应式的 toasts 数组
const toasts = computed(() => toastList)

// 获取图标组件
const getIcon = (type: ToastMessage['type']) => {
  const icons = {
    success: () => h('svg', {
      width: '20',
      height: '20',
      viewBox: '0 0 20 20',
      fill: 'currentColor'
    }, [
      h('path', {
        fillRule: 'evenodd',
        d: 'M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z',
        clipRule: 'evenodd'
      })
    ]),
    
    error: () => h('svg', {
      width: '20',
      height: '20',
      viewBox: '0 0 20 20',
      fill: 'currentColor'
    }, [
      h('path', {
        fillRule: 'evenodd',
        d: 'M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z',
        clipRule: 'evenodd'
      })
    ]),
    
    warning: () => h('svg', {
      width: '20',
      height: '20',
      viewBox: '0 0 20 20',
      fill: 'currentColor'
    }, [
      h('path', {
        fillRule: 'evenodd',
        d: 'M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z',
        clipRule: 'evenodd'
      })
    ]),
    
    info: () => h('svg', {
      width: '20',
      height: '20',
      viewBox: '0 0 20 20',
      fill: 'currentColor'
    }, [
      h('path', {
        fillRule: 'evenodd',
        d: 'M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z',
        clipRule: 'evenodd'
      })
    ])
  }
  
  return icons[type] || icons.info
}
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: 1rem;
  right: 1rem;
  z-index: 9999;
  pointer-events: none;
}

.toast-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-width: 400px;
}

.toast {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 1rem;
  border-radius: 8px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  pointer-events: auto;
  transition: all 0.3s ease;
  backdrop-filter: blur(8px);
}

.toast:hover {
  transform: translateY(-2px);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.toast--success {
  background-color: rgba(16, 185, 129, 0.95);
  color: white;
  border-left: 4px solid #059669;
}

.toast--error {
  background-color: rgba(239, 68, 68, 0.95);
  color: white;
  border-left: 4px solid #dc2626;
}

.toast--warning {
  background-color: rgba(245, 158, 11, 0.95);
  color: white;
  border-left: 4px solid #d97706;
}

.toast--info {
  background-color: rgba(59, 130, 246, 0.95);
  color: white;
  border-left: 4px solid #2563eb;
}

.toast__icon {
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.toast__content {
  flex: 1;
  min-width: 0;
}

.toast__title {
  font-weight: 600;
  font-size: 0.875rem;
  margin-bottom: 0.25rem;
}

.toast__message {
  font-size: 0.875rem;
  line-height: 1.4;
  opacity: 0.95;
}

.toast__close {
  flex-shrink: 0;
  background: none;
  border: none;
  color: currentColor;
  font-size: 1.25rem;
  line-height: 1;
  cursor: pointer;
  opacity: 0.7;
  transition: opacity 0.2s ease;
  padding: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.toast__close:hover {
  opacity: 1;
}

/* 动画效果 */
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(100%) scale(0.95);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(100%) scale(0.95);
}

.toast-move {
  transition: transform 0.3s ease;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .toast-container {
    top: 0.5rem;
    right: 0.5rem;
    left: 0.5rem;
  }
  
  .toast-list {
    max-width: none;
  }
  
  .toast {
    padding: 0.875rem;
  }
}
</style>