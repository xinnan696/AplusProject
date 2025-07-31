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
// 开发环境下的Mock认证 - 可以根据需要开启/关闭
console.log("🔧 MOCK: Setting mock authentication for testing purposes.");
localStorage.setItem('authToken', 'mock-auth-token-for-testing');
localStorage.setItem('user', JSON.stringify({
  role: 'ADMIN',
  userName: 'Test Admin'
}));

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
// 禁用手动操作保护机制
// const userIsManuallyOperating = ref(false)
// let manualOperationTimer: NodeJS.Timeout | null = null

// Junction映射表
const junctionIdToNameMap = ref<Record<string, string>>({})

// 计算属性 - 紧急车辆状态
const hasPendingEmergencies = computed(() => emergencyStore.pendingVehicles.length > 0)
const hasNewRequests = computed(() => emergencyStore.pendingVehicles.length > 0)
// 显示图标的条件：有新请求 或 有正在进行的会话 或 有正在追踪的车辆
const showEmergencyIcon = computed(() => {
  const hasNew = hasNewRequests.value
  const hasActive = emergencyStore.hasActiveSession
  const hasTracking = Object.keys(emergencyStore.vehicleDataMap || {}).length > 0

  console.log('📊 [Icon] 显示条件检查:', {
    hasNewRequests: hasNew,
    hasActiveSession: hasActive,
    hasTrackingVehicles: hasTracking,
    shouldShow: hasNew || hasActive || hasTracking
  })

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
  // 优先使用emergencyStore中的映射数据，如果没有再使用本地的
  return emergencyStore.junctionIdToNameMap[junctionId] || junctionIdToNameMap.value[junctionId] || junctionId;
};

// 智能紧急车辆图标点击处理
const handleEmergencyIconClick = () => {
  console.log("🚨 Emergency icon clicked");

  if (hasNewRequests.value) {
    // 有新的待处理事件，调用地图组件显示弹窗
    console.log('📋 有新的紧急车辆请求，显示请求弹窗');
    mapRef.value?.showEmergencyRequestDialog();
    // 关闭其他面板
    isRecordVisible.value = false;
    isPriorityVisible.value = false;
  } else if (emergencyStore.hasActiveSession || Object.keys(emergencyStore.vehicleDataMap || {}).length > 0) {
    // 没有新事件，但有正在追踪的任务，切换追踪面板显示状态
    console.log('🔄 切换紧急车辆追踪面板显示状态');
    isPriorityVisible.value = !isPriorityVisible.value;
    // 如果打开追踪面板，关闭其他面板
    if (isPriorityVisible.value) {
      isRecordVisible.value = false;
    }
  }
}

// 兼容性方法 - 简单切换显示
const toggleEmergency = () => {
  console.log("🚨 Toggle emergency - 调用智能处理");
  handleEmergencyIconClick();
}

// 紧急车辆处理方法
const handleEmergencyApproved = (vehicleId: string) => {
  console.log(`✅ [ControlHome] 批准紧急车辆: ${vehicleId}`);
  emergencyStore.approveVehicle(vehicleId);
  // 关闭其他面板，显示追踪面板
  isRecordVisible.value = false;
  isPriorityVisible.value = true;
  
  // 批准后不自动打开侧边栏，让用户自己决定是否需要打开
  console.log('📱 [ControlHome] 紧急车辆已批准，追踪面板已显示');
}

const handleEmergencyRejected = (vehicleId: string) => {
  console.log(`❌ [ControlHome] 拒绝紧急车辆: ${vehicleId}`);
  emergencyStore.rejectVehicle(vehicleId);
}

// 地图和控制板交互处理
const handleHighlight = (fromLanes: string[], toLanes: string[]) => {
  console.log('🎨 [Home] Highlight lanes:', { fromLanes, toLanes })
  mapRef.value?.setHighlightLanes(fromLanes, toLanes)
}

const handleSignalLightClicked = (junctionName: string) => {
  console.log('📍 [Home] Signal light clicked:', junctionName)
  if (junctionName) {
    controlBoardRef.value?.selectJunctionByName(junctionName)
    mapRef.value?.setSelectedJunction(junctionName)
  } else {
    controlBoardRef.value?.clearJunctionSelection()
    mapRef.value?.setSelectedJunction(null)
  }
}

const handleTrafficLightSelected = (junctionId: string, directionIndex: number, options?: { disableZoom?: boolean }) => {
  console.log('🎯 [Home] Traffic light selected:', { junctionId, directionIndex, options })
  
  // 完全禁用保护机制，确保ControlManual功能完全正常
  /*
  const isEmergencyEvent = options?.disableZoom === true
  const shouldIgnore = isEmergencyEvent && userIsManuallyOperating.value
  
  if (shouldIgnore) {
    console.log('🚫 [Home] 用户正在手动操作，忽略紧急车辆事件')
    return
  }
  */
  
  // 所有事件都正常处理，不做任何拦截
  console.log('✅ [Home] 处理交通灯选择事件')
  mapRef.value?.setSelectedTrafficLight(junctionId, directionIndex, options)
}

const handleTrafficLightCleared = () => {
  console.log('🧹 [Home] Traffic light cleared')
  mapRef.value?.clearTrafficStatus()
}

const handleJunctionSelected = (junctionName: string, junctionId: string) => {
  console.log('🎯 [Home] Junction selected (no zoom for emergency):', { junctionName, junctionId })
  // 移除 zoom 功能，紧急车辆情况下不需要 zoom 动画
  // mapRef.value?.zoomToJunctionById(junctionId) // 已移除
  mapRef.value?.setSelectedJunctionOnly(junctionId)
}

const handleManualControlApplied = (data: { junctionName: string, directionInfo: string, lightColor: string, duration: number }) => {
  console.log('🎯 [Home] Manual control applied:', data)
  // 直接传递给 ControlMap 的 lastManualControl 状态
  // 但是 ControlMap 没有这个方法，所以去掉这个调用
  // mapRef.value?.handleManualControlApplied(data)
}

// 禁用保护机制相关函数
/*
// 新增：设置用户手动操作状态
const setUserManuallyOperating = () => {
  userIsManuallyOperating.value = true
  console.log('👤 [Home] 用户开始手动操作，2秒保护期')
  
  // 清除之前的计时器
  if (manualOperationTimer) {
    clearTimeout(manualOperationTimer)
  }
  
  // 2秒后清除手动操作状态（调短保护时间）
  manualOperationTimer = setTimeout(() => {
    userIsManuallyOperating.value = false
    console.log('👤 [Home] 用户手动操作保护期结束')
  }, 2000)
}
*/

// 面板切换功能
const toggleRecord = () => {
  isRecordVisible.value = !isRecordVisible.value
  if (isRecordVisible.value) {
    isPriorityVisible.value = false
    // 当打开Record面板时，清除交通灯状态
    mapRef.value?.clearTrafficStatus()
  }
}

const togglePriority = () => {
  const wasVisible = isPriorityVisible.value
  isPriorityVisible.value = !isPriorityVisible.value
  
  if (isPriorityVisible.value) {
    isRecordVisible.value = false
  } else if (wasVisible) {
    // 当关闭紧急车辆追踪面板时，清除交通灯状态
    console.log('🧹 [ControlHome] 关闭紧急车辆追踪面板，清除交通灯状态')
    mapRef.value?.clearTrafficStatus()
  }
}

// 其他功能
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

// 显示键盘快捷键帮助
const showKeyboardHelp = () => {
  const helpMessage = `
📋 键盘快捷键帮助：

基本操作：
• Esc - 关闭所有面板，清除状态
• 1   - 切换记录面板
• 2   - 切换紧急车辆处理
• 3   - 切换优先车辆追踪面板
• N   - 切换导航面板

清除操作：
• R   - 刷新地图状态
• C   - 清除所有选择

其他：
• H   - 显示此帮助信息
• F   - 聚焦搜索（如果可用）

系统快捷键：
• Ctrl+C - 复制（正常功能）
• Ctrl+F - 查找（正常功能）
  `
  
  console.log(helpMessage)
  // 可以在这里添加toast提示
  alert('键盘快捷键帮助：\n\n基本操作：\nEsc - 关闭所有面板，清除状态\n1 - 切换记录面板\n2 - 切换紧急车辆处理\n3 - 切换优先车辆追踪面板\nN - 切换导航面板\n\n清除操作：\nR - 刷新地图状态\nC - 清除所有选择\n\n其他：\nH - 显示此帮助信息\nF - 聚焦搜索')
}

// 聚焦搜索功能
const focusSearch = () => {
  // 尝试聚焦到搜索输入框
  const searchInput = document.querySelector('input[placeholder*="search" i], input[placeholder*="搜索"]') as HTMLInputElement
  if (searchInput) {
    searchInput.focus()
    console.log('🔍 [ControlHome] 已聚焦到搜索框')
  } else {
    console.log('❌ [ControlHome] 未找到搜索框')
  }
}

// 键盘快捷键处理
const handleKeyDown = (event: KeyboardEvent) => {
  // 如果用户正在输入框中输入，不处理快捷键
  if (event.target instanceof HTMLInputElement || 
      event.target instanceof HTMLTextAreaElement ||
      event.target instanceof HTMLSelectElement) {
    return
  }

  console.log('🎹 [ControlHome] 键盘事件:', event.key)

  switch (event.key) {
    case 'Escape':
      console.log('🧹 [ControlHome] Escape键被按下，关闭所有面板')
      // 记录关闭前的状态
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
      console.log('🎹 [ControlHome] 快捷键F - 聚焦搜索')
      focusSearch()
      break
      
    default:
      // 对于未处理的按键，不做任何操作
      break
  }
}

onMounted(() => {
  console.log("🚑 启动紧急车辆WebSocket连接...")
  fetchJunctions()
  emergencyStore.connectWebSocket()
  document.addEventListener('keydown', handleKeyDown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeyDown)
  // 禁用保护机制后不再需要清理计时器
  /*
  if (manualOperationTimer) {
    clearTimeout(manualOperationTimer)
  }
  */
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
