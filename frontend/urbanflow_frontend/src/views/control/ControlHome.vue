<template>
  <div class="control-page">
    <ControlHeader @toggle-nav="toggleSidebar" />
    <ControlNav :isVisible="showSidebar" />

    <div class="main-area">
      <div class="map-contain">
        <ControlMap ref="mapRef" :isSidebarOpen="showSidebar" />
      </div>

      <div class="control-board">
        <ControlBoard @highlight="handleHighlight" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ControlHeader from '@/views/control/ControlHeader.vue'
import ControlMap from '@/views/control/ControlMap.vue'
import ControlBoard from './ControlBoard.vue'
import ControlNav from './ControlNav.vue'

const showSidebar = ref(false)
function toggleSidebar() {
  showSidebar.value = !showSidebar.value
}


const mapRef = ref()

const handleHighlight = (fromLanes: string[], toLanes: string[]) => {
  mapRef.value?.setHighlightLanes(fromLanes, toLanes)  // ✅ 正确函数名
}

</script>


<style lang="scss">
.control-page {
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
  height: calc(100% - 0.64rem); // 减去 Header 高度
  display: flex;
}

.map-contain {
  width: 13.59rem; // 约占 70%
  height: 100%;
  position: relative;
  overflow: hidden;
}

.control-board {
  width: 5.61rem; // 约占 30%
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #1E1E2F;
  overflow: hidden;
}
</style>
