<template>
  <div class="map-page">
    <MapEdge />
  </div>
</template>

<script setup lang="ts">
import MapEdge from './MapEdge.vue'
</script>

<style scoped>
.map-page {
  width: 100%;
  height: 100%;
  flex: 1;                /* ✅ 让它填满父级flex容器 */
  display: flex;          /* ✅ 为后续子元素布局做准备 */
  flex-direction: column; /* ✅ 可选，用于子组件垂直布局 */
}
</style>
