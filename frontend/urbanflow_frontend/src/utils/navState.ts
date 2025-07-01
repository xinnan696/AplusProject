import { ref } from 'vue'

// 全局导航栏状态
export const isNavVisible = ref(false)

export const toggleNav = () => {
  isNavVisible.value = !isNavVisible.value
}

export const showNav = () => {
  isNavVisible.value = true
}

export const hideNav = () => {
  isNavVisible.value = false
}