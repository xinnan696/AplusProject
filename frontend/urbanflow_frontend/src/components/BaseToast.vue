<template>
  <Transition name="toast-fade">

    <div v-if="visible" class="toast" :class="typeClass">
      <div v-if="visible" class="iconfont true">
      &#xe60c;
    </div>
      {{ message }}
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  message: string
  type?: 'success' | 'error' | 'info'
  duration?: number
}>()

const visible = ref(true)
const typeClass = props.type || 'success'

setTimeout(() => {
  visible.value = false
}, props.duration || 3000)
</script>

<style scoped lang="scss">
.true {
  color: #2ECC71;
  font-size: 0.14rem;
  padding-right: 0.08rem;
  display: inline-block;
}

.toast {
  position: fixed;
  top: 1.44rem;
  left: 4.52rem;

  width: 455px;
  height: 40px;

  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.08rem;

  background-color: #2B2C3D;
  color: #2ECC71;
  font-size: 14px;
  border-radius: 8px;

  box-shadow: 0px 5px 5px rgba(0, 0, 0, 0.3);
  z-index: 999;
}


.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity 0.4s ease, transform 0.4s ease;
}

.toast-fade-enter-from {
  opacity: 0;
  transform: translateY(-1rem) scale(0.8);
}

.toast-fade-enter-to {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.toast-fade-leave-from {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.toast-fade-leave-to {
  opacity: 0;
  transform: translateY(-0.3rem) scale(0.95);
}

</style>
