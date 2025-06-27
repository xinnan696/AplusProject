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
  transition: background-color 0.2s;

  &:hover {
    background-color: #2E2F41;
  }

  &.active {
    background-color: #00B4D8;
    color: white;
  }
}
</style>
