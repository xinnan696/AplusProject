<template>
  <div class="dashboard-page">
    <ControlHeader @toggle-nav="toggleNav" />
    <ControlNav :isVisible="isNavVisible" />

    <div class="main-area">
      <div class="dashboard-container">
        <!-- 空的容器，只显示背景色 -->
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import ControlHeader from '@/views/control/ControlHeader.vue'
import ControlNav from '@/views/control/ControlNav.vue'
import { isNavVisible, toggleNav } from '@/utils/navState'
</script>

<style scoped lang="scss">
.dashboard-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: #1E1E2F;
  z-index: 1;
}

.main-area {
  height: calc(100% - 0.64rem);
  display: flex;
  overflow: hidden;
}

.dashboard-container {
  flex: 1;
  background-color: #f5f5f5;
  // 空的容器，只显示背景色
}
</style>

