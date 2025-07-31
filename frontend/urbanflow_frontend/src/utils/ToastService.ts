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
  warning(message: string, duration = 3000) {
    showToast(message, 'warning', duration)
  },
}

function showToast(message: string, type: 'success' | 'error' | 'info' | 'warning', duration: number) {
  const container = document.createElement('div')
  document.body.appendChild(container)

  const vnode = createVNode(BaseToast, { message, type, duration })
  render(vnode, container)

  setTimeout(() => {
    render(null, container)
    document.body.removeChild(container)
  }, duration + 500)
}
