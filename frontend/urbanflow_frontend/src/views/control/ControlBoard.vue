<template>
  <div class="control-board">
    <ControlCongested @select-junction-by-name="handleJunctionSelectFromCongested" />
    <ControlAI />
    <ControlManual ref="manualControlRef" @highlight="handleHighlight" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ControlCongested from './ControlCongested.vue'
import ControlAI from './ControlAI.vue'
import ControlManual from './ControlManual.vue'

// emit highlight 事件到上级地图组件
const emit = defineEmits<{
  (e: 'highlight', fromLanes: string[], toLanes: string[]): void
}>()

// ControlManual 的组件引用
const manualControlRef = ref()

function handleHighlight(fromLanes: string[], toLanes: string[]) {
  emit('highlight', fromLanes, toLanes)
}

// 来自 ControlCongested 的点击事件
function handleJunctionSelectFromCongested(junctionName: string) {
  manualControlRef.value?.setJunctionByName(junctionName)
}
</script>


<style scoped lang="scss">
.control-board {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;

  > * {
    width: 100%;
  }

  > :nth-child(1) {
    height: 27%;
  }

  > :nth-child(2) {
    height: 31.3%;
  }

  > :nth-child(3) {
    flex: 41.7%;
  }
}
</style>
