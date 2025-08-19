<template>
  <div class="control-board" :class="{ 'ai-mode': isAIMode }">
    <ControlCongested :isAIMode="isAIMode" @select-junction-by-name="handleJunctionSelectFromCongested" />
    <ControlAI :isAIMode="isAIMode" />
    <ControlManual
      ref="manualControlRef"
      @highlight="handleHighlight"
      @traffic-light-selected="handleTrafficLightSelected"
      @traffic-light-cleared="handleTrafficLightCleared"
      @junction-selected="handleJunctionSelected"
      @manual-control-applied="handleManualControlApplied"
      :class="{ 'hidden': isAIMode }"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, toRefs } from 'vue'
import ControlCongested from './ControlCongested.vue'
import ControlAI from './ControlAI.vue'
import ControlManual from './ControlManual.vue'

interface Props {
  isAIMode?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isAIMode: false
})


const emit = defineEmits<{
  (e: 'highlight', fromLanes: string[], toLanes: string[]): void
  (e: 'trafficLightSelected', junctionName: string, directionIndex: number, triggerSource?: 'junction' | 'direction'): void
  (e: 'trafficLightCleared'): void
  (e: 'junctionSelected', junctionName: string, junctionId: string): void
  (e: 'manualControlApplied', data: { junctionName: string, directionInfo: string, lightColor: string, duration: number }): void
}>()

const manualControlRef = ref()
const { isAIMode } = toRefs(props)

function handleHighlight(fromLanes: string[], toLanes: string[]) {
  console.log('🔥 [BOARD] FORWARDING:', { fromLanes, toLanes })
  emit('highlight', fromLanes, toLanes)
}

function handleTrafficLightSelected(junctionName: string, directionIndex: number, triggerSource?: 'junction' | 'direction') {
  emit('trafficLightSelected', junctionName, directionIndex, triggerSource)
}

function handleTrafficLightCleared() {
  emit('trafficLightCleared')
}

function handleJunctionSelected(junctionName: string, junctionId: string) {
  emit('junctionSelected', junctionName, junctionId)
}

function handleManualControlApplied(data: { junctionName: string, directionInfo: string, lightColor: string, duration: number }) {
  emit('manualControlApplied', data)
}

function handleJunctionSelectFromCongested(junctionName: string) {
  manualControlRef.value?.setJunctionByName(junctionName)
}

function selectJunctionByName(junctionName: string) {
  manualControlRef.value?.setJunctionByName(junctionName)
}

function clearJunctionSelection() {
  manualControlRef.value?.clearSelection()
}

defineExpose({
  selectJunctionByName,
  clearJunctionSelection
})
</script>


<style scoped lang="scss">
.control-board {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  background-color: #1e1e2f;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 2px;
    height: 100%;
    background: linear-gradient(180deg, #4A5568 0%, transparent 50%, #4A5568 100%);
    opacity: 0.6;
    z-index: 1;
  }

  > * {
    width: 100%;
    position: relative;
    transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  }

  > :nth-child(1) {
    height: 27%;

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0.24rem;
      right: 0.24rem;
      height: 1px;
      background: linear-gradient(90deg, transparent, #3A3A4C, transparent);
    }
  }

  > :nth-child(2) {
    height: 31.3%;

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0.24rem;
      right: 0.24rem;
      height: 1px;
      background: linear-gradient(90deg, transparent, #3A3A4C, transparent);
    }
  }

  > :nth-child(3) {
    flex: 41.7%;

    &.hidden {
      height: 0;
      opacity: 0;
      overflow: hidden;
      transform: translateY(-20px);
      pointer-events: none;
    }
  }

  &.ai-mode {
    > :nth-child(1) {
      height: 45%;
    }

    > :nth-child(2) {
      height: 55%;
    }

    > :nth-child(3) {
      height: 0;
      opacity: 0;
      overflow: hidden;
      transform: translateY(-20px);
      pointer-events: none;
    }
  }
}
</style>
