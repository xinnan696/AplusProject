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

// 组件引用
const mapRef = ref()
const controlBoardRef = ref()

// 状态管理
const isRecordVisible = ref(false)
const isPriorityVisible = ref(false)
const isAIMode = ref(false)

const junctionIdToNameMap = ref<Record<string, string>>({})

// 计算属性 - 紧急车辆状态
const hasPendingEmergencies = computed(() => emergencyStore.pendingVehicles.length > 0)
const hasNewRequests = computed(() => emergencyStore.pendingVehicles.length > 0)
// 显示图标的条件：有新请求 或 有正在进行的会话 或 有正在追踪的车辆
const showEmergencyIcon = computed(() => {
  const hasNew = hasNewRequests.value
  const hasActive = emergencyStore.hasActiveSession
  const hasTracking = Object.keys(emergencyStore.vehicleDataMap || {}).length > 0


  return hasNew || hasActive || hasTracking
})

// Junction数据获取和转换
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
  } else if (emergencyStore.hasActiveSession || Object.keys(emergencyStore.vehicleDataMap || {}).length > 0) {

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
  mapRef.value?.setSelectedJunctionOnly(junctionId)
}

const handleManualControlApplied = (data: { junctionName: string, directionInfo: string, lightColor: string, duration: number }) => {
  console.log('🎯 [Home] Manual control applied:', data)
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


  alert('键盘快捷键帮助：\n\n基本操作：\nEsc - 关闭所有面板，清除状态\n1 - 切换记录面板\n2 - 切换紧急车辆处理\n3 - 切换优先车辆追踪面板\nN - 切换导航面板\n\n清除操作：\nR - 刷新地图状态\nC - 清除所有选择\n\n其他：\nH - 显示此帮助信息\nF - 聚焦搜索')
}


const focusSearch = () => {
  const searchInput = document.querySelector('input[placeholder*="search" i], input[placeholder*="搜索"]') as HTMLInputElement
  if (searchInput) {
    searchInput.focus()
  } else {
    console.log('❌ [ControlHome] 未找到搜索框')
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

      // 关闭所有面板
      isRecordVisible.value = false
      isPriorityVisible.value = false

      // 如果紧急车辆追踪面板之前是打开的，清除交通灯状态
      if (wasPriorityVisible) {
        console.log('🧹 [ControlHome] Escape键关闭车辆追踪面板，清除交通灯状态')
        mapRef.value?.clearTrafficStatus()
      }

      // 如果记录面板之前是打开的，也清除交通灯状态
      if (wasRecordVisible) {
        console.log('🧹 [ControlHome] Escape键关闭记录面板，清除交通灯状态')
        mapRef.value?.clearTrafficStatus()
      }

      // 清除地图上的所有选择状态
      controlBoardRef.value?.clearJunctionSelection?.()

      console.log('✅ [ControlHome] 所有面板已关闭，状态已清理')
      break

    case '1':
      event.preventDefault()
      console.log('🎹 [ControlHome] 快捷键1 - 切换记录面板')
      toggleRecord()
      break

    case '2':
      event.preventDefault()
      console.log('🎹 [ControlHome] 快捷键2 - 切换紧急车辆处理')
      toggleEmergency()
      break

    case '3':
      event.preventDefault()
      console.log('🎹 [ControlHome] 快捷键3 - 切换优先车辆追踪面板')
      togglePriority()
      break

    case 'n':
    case 'N':
      event.preventDefault()
      console.log('🎹 [ControlHome] 快捷键N - 切换导航面板')
      toggleNav()
      break

    case 'r':
    case 'R':
      event.preventDefault()
      console.log('🎹 [ControlHome] 快捷键R - 刷新地图状态')
      mapRef.value?.clearTrafficStatus()
      controlBoardRef.value?.clearJunctionSelection?.()
      console.log('✅ [ControlHome] 地图状态已刷新')
      break

    case 'c':
    case 'C':
      if (event.ctrlKey || event.metaKey) {
        // 让Ctrl+C正常工作，不阻止
        return
      }
      event.preventDefault()
      console.log('🎹 [ControlHome] 快捷键C - 清除所有选择')
      mapRef.value?.clearTrafficStatus()
      controlBoardRef.value?.clearJunctionSelection?.()
      console.log('✅ [ControlHome] 所有选择已清除')
      break

    case 'h':
    case 'H':
      event.preventDefault()
      console.log('🎹 [ControlHome] 快捷键H - 显示帮助信息')
      showKeyboardHelp()
      break

    case 'f':
    case 'F':
      if (event.ctrlKey || event.metaKey) {
        // 让Ctrl+F正常工作
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
