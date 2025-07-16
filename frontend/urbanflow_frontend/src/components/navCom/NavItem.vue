<template>
  <div
    class="menu-item"
    :class="{ active }"
    @click="$emit('click')"
  >
    {{ label }}
  </div>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  active?: boolean
}>()

defineEmits<{
  click: []
}>()
</script>

<style scoped lang="scss">
.menu-item {
  width: 2.24rem;
  height: 0.48rem;
  background-color: #2B2C3D;
  margin-bottom: 0.12rem;
  margin-left: 0.08rem;
  font-size: 0.16rem;
  color: white;
  text-align: center;
  line-height: 0.48rem;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 0.04rem;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.1), transparent);
    transition: left 0.5s ease;
  }

  &:hover {
    background-color: #2E2F41;
    transform: translateY(-1px);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
    
    &::before {
      left: 100%;
    }
  }

  &.active {
    background: linear-gradient(135deg, #00B4D8, #0096C7);
    color: white;
    box-shadow: 0 4px 12px rgba(0, 180, 216, 0.3);
    border-left: 3px solid #00E3FF;
    
    &::after {
      content: '';
      position: absolute;
      top: 50%;
      right: 8px;
      width: 4px;
      height: 4px;
      background-color: #00E3FF;
      border-radius: 50%;
      transform: translateY(-50%);
      animation: pulse 2s infinite;
    }
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: translateY(-50%) scale(1);
  }
  50% {
    opacity: 0.6;
    transform: translateY(-50%) scale(1.2);
  }
}
</style>