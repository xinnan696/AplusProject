import { ref } from 'vue'

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
