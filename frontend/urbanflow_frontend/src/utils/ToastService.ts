
// utils/ToastService.ts
import { ref } from 'vue'

// Toast 类型定义
export interface ToastMessage {
  id: string
  type: 'success' | 'error' | 'warning' | 'info'
  title?: string
  message: string
  duration?: number
  timestamp: number
}

// 全局 toast 状态
const toasts = ref<ToastMessage[]>([])

// 生成唯一ID
const generateId = (): string => {
  return Date.now().toString(36) + Math.random().toString(36).substr(2)
}

// 添加 toast
const addToast = (toast: Omit<ToastMessage, 'id' | 'timestamp'>): void => {
  const newToast: ToastMessage = {
    ...toast,
    id: generateId(),
    timestamp: Date.now(),
    duration: toast.duration || 3000
  }
  
  toasts.value.push(newToast)
  
  // 自动移除
  if (newToast.duration && newToast.duration > 0) {
    setTimeout(() => {
      removeToast(newToast.id)
    }, newToast.duration)
  }
}

// 移除 toast
const removeToast = (id: string): void => {
  const index = toasts.value.findIndex(toast => toast.id === id)
  if (index > -1) {
    toasts.value.splice(index, 1)
  }
}

// 清空所有 toast
const clearToasts = (): void => {
  toasts.value = []
}

// Toast 服务对象
export const toast = {
  success: (message: string, title?: string, duration?: number) => {
    addToast({ type: 'success', message, title, duration })
  },
  
  error: (message: string, title?: string, duration?: number) => {
    addToast({ type: 'error', message, title, duration })
  },
  
  warning: (message: string, title?: string, duration?: number) => {
    addToast({ type: 'warning', message, title, duration })
  },
  
  info: (message: string, title?: string, duration?: number) => {
    addToast({ type: 'info', message, title, duration })
  }
}

// 导出响应式的 toasts 数组供组件使用
export const useToasts = () => {
  return {
    toasts: toasts.value,
    removeToast,
    clearToasts
  }
}

// 默认导出
export default toast

import { createVNode, render } from 'vue'
import BaseToast from '@/components/BaseToast.vue'

export const toast = {
  success(message: string, duration = 3000) {
    showToast(message, 'success', duration)
  },
  error(message: string, duration = 3000) {
    showToast(message, 'error', duration)
  },
  info(message: string, duration = 3000) {
    showToast(message, 'info', duration)
  },
}

function showToast(message: string, type: 'success' | 'error' | 'info', duration: number) {
  const container = document.createElement('div')
  document.body.appendChild(container)

  const vnode = createVNode(BaseToast, { message, type, duration })
  render(vnode, container)

  setTimeout(() => {
    render(null, container)
    document.body.removeChild(container)
  }, duration + 500)
}

