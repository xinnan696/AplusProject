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

  &.active {
    background: linear-gradient(135deg, #00B4D8, #0096C7);
    color: white;
    box-shadow: 0 4px 12px rgba(0, 180, 216, 0.3);
  }
}
</style>
