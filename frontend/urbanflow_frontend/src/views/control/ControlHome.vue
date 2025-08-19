<template>
  <div class="control-page">
    <ControlHeader
      :isRecordPanelVisible="isRecordVisible"
      :show-emergency-icon="showEmergencyIcon"
      :has-new-requests="hasNewRequests"
      @toggle-nav="toggleNav"
      @toggle-record="toggleRecord"
      @emergency-icon-clicked="handleEmergencyIconClick"
      @toggle-emergency="toggleEmergency"
      @toggle-priority="togglePriority"
      @mode-changed="handleModeChange"
      @sign-out="handleSignOut"
    />
    <ControlNav :isVisible="isNavVisible" />

    <div class="main-area">
      <div class="map-contain">
        <ControlMap
          ref="mapRef"
          :isSidebarOpen="isNavVisible"
          :isPriorityTrackingOpen="isPriorityVisible"
          @signal-light-clicked="handleSignalLightClicked"
          @emergency-approved="handleEmergencyApproved"
          @emergency-rejected="handleEmergencyRejected"
        />
      </div>

      <div class="control-board">
        <ControlBoard
          ref="controlBoardRef"
          :isAIMode="isAIMode"
          @highlight="handleHighlight"
          @traffic-light-selected="handleTrafficLightSelected"
          @traffic-light-cleared="handleTrafficLightCleared"
          @junction-selected="handleJunctionSelected"
          @manual-control-applied="handleManualControlApplied"
        />
      </div>
    </div>

    <ControlRecord :isVisible="isRecordVisible" @close="toggleRecord" />

    <!-- 优先车辆追踪面板 -->
    <PriorityVehicleTracking
      :isVisible="isPriorityVisible"
      @close="togglePriority"
      @highlight="handleHighlight"
      @traffic-light-selected="handleTrafficLightSelected"
      @traffic-light-cleared="handleTrafficLightCleared"
      @junction-selected="handleJunctionSelected"
      @manual-control-applied="handleManualControlApplied"
    />
  </div>
</template>

<script setup lang="ts">

import { useRouter } from 'vue-router'
import axios from 'axios'
import { useEmergencyStore } from '@/stores/emergency'
import ControlHeader from '@/views/control/ControlHeader.vue'
import ControlMap from '@/views/control/ControlMap.vue'
import ControlBoard from './ControlBoard.vue'
import ControlNav from './ControlNav.vue'
import ControlRecord from './ControlRecord.vue'
import { isNavVisible, toggleNav } from '@/utils/navState'
import PriorityVehicleTracking from './PriorityVehicleTracking.vue'
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const router = useRouter()
const emergencyStore = useEmergencyStore()

const mapRef = ref()
const controlBoardRef = ref()

const isRecordVisible = ref(false)
const isPriorityVisible = ref(false)
const isAIMode = ref(false)

const junctionIdToNameMap = ref<Record<string, string>>({})

const hasPendingEmergencies = computed(() => emergencyStore.pendingVehicles.length > 0)
const hasNewRequests = computed(() => emergencyStore.pendingVehicles.length > 0)
const showEmergencyIcon = computed(() => {
  const hasNew = hasNewRequests.value
  const hasActive = emergencyStore.hasActiveSession
  return hasNew || hasActive
})

const fetchJunctions = async () => {
  try {
    const response = await axios.get('/api-status/junctions');
    const junctionData = Object.values(response.data);
    const nameMap: Record<string, string> = {};
    junctionData.forEach((j: any) => {
      nameMap[j.junction_id] = j.junction_name || j.junction_id;
    });
    junctionIdToNameMap.value = nameMap;
    console.log('[ControlHome] Junction ID to Name mappings loaded.');
  } catch (error) {
    console.error('[ControlHome] Failed to fetch junctions:', error);
  }
};

const getJunctionName = (junctionId: string) => {
  return emergencyStore.junctionIdToNameMap[junctionId] || junctionIdToNameMap.value[junctionId] || junctionId;
};


const handleEmergencyIconClick = () => {
  if (hasNewRequests.value) {
    mapRef.value?.showEmergencyRequestDialog();
    isRecordVisible.value = false;
    isPriorityVisible.value = false;
  } else if (emergencyStore.hasActiveSession) {
    isPriorityVisible.value = !isPriorityVisible.value;
    if (isPriorityVisible.value) {
      isRecordVisible.value = false;
    }
  }
}


const toggleEmergency = () => {

  handleEmergencyIconClick();
}

const handleEmergencyApproved = (vehicleId: string) => {
  emergencyStore.approveVehicle(vehicleId);
  isRecordVisible.value = false;
  isPriorityVisible.value = true;
}

const handleEmergencyRejected = (vehicleId: string) => {
  emergencyStore.rejectVehicle(vehicleId);
}


const handleHighlight = (fromLanes: string[], toLanes: string[]) => {
  mapRef.value?.setHighlightLanes(fromLanes, toLanes)
}

const handleSignalLightClicked = (junctionName: string) => {
  if (junctionName) {
    controlBoardRef.value?.selectJunctionByName(junctionName)
    mapRef.value?.setSelectedJunction(junctionName)
  } else {
    controlBoardRef.value?.clearJunctionSelection()
    mapRef.value?.setSelectedJunction(null)
  }
}

const handleTrafficLightSelected = (junctionId: string, directionIndex: number, options?: { disableZoom?: boolean }) => {

  mapRef.value?.setSelectedTrafficLight(junctionId, directionIndex, options)
}

const handleTrafficLightCleared = () => {
  mapRef.value?.clearTrafficStatus()
}

const handleJunctionSelected = (junctionName: string, junctionId: string) => {
  mapRef.value?.zoomToJunctionById(junctionId)
  mapRef.value?.setSelectedJunctionOnly(junctionId)
}

const handleManualControlApplied = (data: { junctionName: string, directionInfo: string, lightColor: string, duration: number }) => {

}

const toggleRecord = () => {
  isRecordVisible.value = !isRecordVisible.value
  if (isRecordVisible.value) {
    isPriorityVisible.value = false
    mapRef.value?.clearTrafficStatus()
  }
}

const togglePriority = () => {
  const wasVisible = isPriorityVisible.value
  isPriorityVisible.value = !isPriorityVisible.value

  if (isPriorityVisible.value) {
    isRecordVisible.value = false
  } else if (wasVisible) {

    mapRef.value?.clearTrafficStatus()
  }
}


const handleModeChange = (isAI: boolean) => {
  console.log('Mode changed to:', isAI ? 'AI Mode' : 'Manual Mode')
  isAIMode.value = isAI
}

const handleTrackVehicle = (vehicle: any) => {
  console.log('Tracking vehicle:', vehicle)
  mapRef.value?.highlightVehicle(vehicle)
}

const handleSetPriority = (vehicle: any) => {
  console.log('Setting priority for vehicle:', vehicle)
}

const handleSignOut = () => {
  localStorage.removeItem('authToken')
  router.push({ name: 'Login' })
}

const showKeyboardHelp = () => {
  const helpMessage =


  alert('Keyboard shortcut help:\n\nBasic operations:\nEsc - Close all panels, clear status\n1 - Toggle log panel\n2 - Toggle emergency vehicle handling\n3 - Toggle priority vehicle tracking panel\nN - Toggle navigation panel\n\nClear operations:\nR - Refresh map status\nC - Clear all selections\n\nOther:\nH - Show this help information\nF - Focus search')
}


const focusSearch = () => {
  const searchInput = document.querySelector('input[placeholder*="search" i], input[placeholder*="搜索"]') as HTMLInputElement
  if (searchInput) {
    searchInput.focus()
  } else {
  }
}

const handleKeyDown = (event: KeyboardEvent) => {
  if (event.target instanceof HTMLInputElement ||
      event.target instanceof HTMLTextAreaElement ||
      event.target instanceof HTMLSelectElement) {
    return
  }


  switch (event.key) {
    case 'Escape':

      const wasPriorityVisible = isPriorityVisible.value
      const wasRecordVisible = isRecordVisible.value

      isRecordVisible.value = false
      isPriorityVisible.value = false

      if (wasPriorityVisible) {
        mapRef.value?.clearTrafficStatus()
      }

      if (wasRecordVisible) {
        mapRef.value?.clearTrafficStatus()
      }

      controlBoardRef.value?.clearJunctionSelection?.()

      break

    case '1':
      event.preventDefault()
      toggleRecord()
      break

    case '2':
      event.preventDefault()
      toggleEmergency()
      break

    case '3':
      event.preventDefault()
      togglePriority()
      break

    case 'n':
    case 'N':
      event.preventDefault()
      toggleNav()
      break

    case 'r':
    case 'R':
      event.preventDefault()
      mapRef.value?.clearTrafficStatus()
      controlBoardRef.value?.clearJunctionSelection?.()
      break

    case 'c':
    case 'C':
      if (event.ctrlKey || event.metaKey) {
        return
      }
      event.preventDefault()
      mapRef.value?.clearTrafficStatus()
      controlBoardRef.value?.clearJunctionSelection?.()
      break

    case 'h':
    case 'H':
      event.preventDefault()
      showKeyboardHelp()
      break

    case 'f':
    case 'F':
      if (event.ctrlKey || event.metaKey) {
        return
      }
      event.preventDefault()
      focusSearch()
      break

    default:
      break
  }
}

onMounted(() => {
  fetchJunctions()
  emergencyStore.connectWebSocket()
  document.addEventListener('keydown', handleKeyDown)

  if (typeof window !== 'undefined') {
    window.debugEmergency = {
      store: emergencyStore,
      forceClean: () => {
        emergencyStore.forceCleanAllData()
      },
      checkState: () => {
        console.log('=== Emergency Store 状态 ===')
        console.log('vehicleDataMap:', emergencyStore.vehicleDataMap)
        console.log('activelyTrackedVehicleId:', emergencyStore.activelyTrackedVehicleId)
        console.log('hasActiveSession:', emergencyStore.hasActiveSession)
        console.log('pendingVehicles:', emergencyStore.pendingVehicles)
        console.log('showEmergencyIcon should be:', hasNewRequests.value || emergencyStore.hasActiveSession)
      },
      reload: () => window.location.reload()
    }
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeyDown)

})
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
  height: calc(100% - 0.64rem);
  display: flex;
  position: relative;
}

.map-contain {
  width: 65%;
  height: 100%;
  position: relative;
  overflow: hidden;
  border-right: 1px solid #3A3A4C;
}

.control-board {
  width: 35%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #1E1E2F;
  overflow: hidden;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    right: 0;
    width: 1px;
    height: 100%;
    background: linear-gradient(180deg, transparent 0%, #00B4D8 50%, transparent 100%);
    opacity: 0.3;
  }
}
</style>
