<template>
  <div v-if="isVisible" class="tracking-panel" :class="{ 'slide-out': isClosing }">
    <div class="tracking-header">
      <div class="header-title">
        <span>Priority Vehicle Tracking</span>
      </div>
      <button class="close-button" @click="startCloseAnimation">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
          <path d="M18 6L6 18M6 6L18 18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </button>
    </div>

    <div class="tracking-content">
      <div v-if="trackedVehicleSnapshot" class="vehicle-info-section">
        <!-- 车辆和路口信息展示区 -->
        <div class="info-display-section">
          <div class="info-item">
            <label class="info-label">Vehicle ID</label>
            <div class="info-value-box">
              {{ trackedVehicleSnapshot.vehicleID }}
            </div>
          </div>

          <div class="info-item">
            <label class="info-label">Junction</label>
            <div class="info-value-box">
              {{ displayJunctionName }}
            </div>
          </div>

          <div class="info-item">
            <label class="info-label">From</label>
            <div class="info-value-box">
              {{ displayFromName }}
            </div>
          </div>

          <div class="info-item">
            <label class="info-label">To</label>
            <div class="info-value-box">
              {{ displayToName }}
            </div>
          </div>

          <div class="info-item">
            <label class="info-label">Status</label>
            <div class="info-value-box" :class="approachStatusClass">
              {{ approachStatusText }}
            </div>
          </div>
        </div>

        <!-- 简化的手动控制面板 -->
        <div class="simplified-control-section" v-if="isApproachingSignalizedJunction">
          <!-- 灯光状态 -->
          <div class="info-item">
            <label class="info-label">Light State</label>
            <div class="light-buttons">
              <button
                class="light-btn red"
                :class="{ 'active-red': selectedLight === 'RED' }"
                @click="selectLight('RED')"
              >RED</button>
              <button
                class="light-btn green"
                :class="{ 'active-green': selectedLight === 'GREEN' }"
                @click="selectLight('GREEN')"
              >GREEN</button>
            </div>
          </div>

          <!-- Duration -->
          <div class="form-row-duration">
            <div class="info-item">
              <label class="info-label">Duration</label>
              <div class="duration-custom">
                <input
                  type="text"
                  class="custom-input"
                  v-model="durationDisplay"
                  @input="validateDuration"
                  @keypress="onlyAllowNumbers"
                  @blur="handleBlur"
                  placeholder="Duration (s)"
                />
                <div class="triangle-buttons">
                  <button class="triangle-btn" @click="increaseDuration">▲</button>
                  <button class="triangle-btn" @click="decreaseDuration">▼</button>
                </div>
              </div>
            </div>
            <!-- 错误提示区域 -->
            <div class="duration-error-container">
              <div class="duration-error" v-if="durationError">⚠ The value must be between 5 and 300.</div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="info-item">
            <div class="info-label"></div>
            <div class="action-buttons">
              <button
                class="apply-btn"
                :disabled="!isFormComplete"
                @click="handleApply"
              >
                <div v-if="isApplying" class="loading-spinner"></div>
                <span>{{ isApplying ? 'APPLYING...' : 'APPLY' }}</span>
              </button>
              <button class="cancel-btn" @click="resetForm">CANCEL</button>
            </div>
          </div>
        </div>

        <!-- 当车辆不在信号路口附近时的提示 -->
        <div v-else class="waiting-message">
          <p class="waiting-text">Waiting for Junction Approach</p>
          <p class="waiting-subtext">Traffic light control will be available when the vehicle approaches a signalized junction</p>
        </div>
      </div>

      <div v-else class="completion-message">
        <div class="empty-icon">🚗</div>
        <div class="empty-text">No Priority Vehicle Being Tracked</div>
        <div class="empty-subtext">Priority vehicle tracking information will appear here when available</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, nextTick, onMounted } from 'vue'
import axios from 'axios'
import { useEmergencyStore, type VehicleTrackingData } from '@/stores/emergency'
import apiClient from '@/utils/api'
import { useOperationStore } from '@/stores/operationStore'
import { toast } from "@/utils/ToastService"

interface Props {
  isVisible: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'highlight', fromLanes: string[], toLanes: string[]): void
  (e: 'traffic-light-selected', junctionId: string, directionIndex: number, options?: { disableZoom?: boolean }): void
  (e: 'traffic-light-cleared'): void
  (e: 'junction-selected', junctionName: string, junctionId: string): void
  (e: 'manual-control-applied', data: { junctionName: string, directionInfo: string, lightColor: string, duration: number }): void
}>()

const emergencyStore = useEmergencyStore()
const operationStore = useOperationStore()

// 动画状态控制
const isClosing = ref(false)

// 用于存储映射数据
const laneIdToEdgeName = ref<Record<string, string>>({})
const junctionIdToName = ref<Record<string, string>>({})
const junctionConnectionMap = ref<Map<string, any>>(new Map())

// 创建一个本地ref来存储车辆数据的快照
const trackedVehicleSnapshot = ref<VehicleTrackingData | null>(null)

// 控制面板状态
const selectedLight = ref('')
const duration = ref<number | null>(null)
const durationDisplay = ref('')
const durationError = ref(false)
const isApplying = ref(false)

// 计算属性
const isApproachingSignalizedJunction = computed(() => {
  return !!trackedVehicleSnapshot.value?.upcomingJunctionID
})

const displayJunctionName = computed(() => {
  const junctionId = trackedVehicleSnapshot.value?.upcomingJunctionID
  if (!junctionId) return 'Waiting...'
  return junctionIdToName.value[junctionId] || junctionId
})

const displayFromName = computed(() => {
  const laneId = trackedVehicleSnapshot.value?.currentLaneID
  if (!isApproachingSignalizedJunction.value) return 'Waiting...'
  if (!laneId) return 'N/A'
  return laneIdToEdgeName.value[laneId] || laneId
})

const displayToName = computed(() => {
  const laneId = trackedVehicleSnapshot.value?.nextLaneID
  if (!isApproachingSignalizedJunction.value) return 'Waiting...'
  if (!laneId) return 'N/A'
  return laneIdToEdgeName.value[laneId] || laneId
})

const approachStatusText = computed(() => {
  return isApproachingSignalizedJunction.value ? 'Approaching Junction' : 'In Route'
})

const approachStatusClass = computed(() => {
  return isApproachingSignalizedJunction.value ? 'status-approaching' : 'status-enroute'
})

const isFormComplete = computed(() =>
  selectedLight.value !== '' &&
  duration.value !== null &&
  duration.value >= 5 &&
  duration.value <= 300 &&
  !durationError.value &&
  !isApplying.value &&
  isApproachingSignalizedJunction.value
)

// 数据获取函数
const fetchLaneMappings = async () => {
  try {
    const response = await axios.get('/api-status/lane-mappings')
    const mappings = Array.isArray(response.data) ? response.data : Object.values(response.data)
    const nameMap: Record<string, string> = {}
    mappings.forEach((m: any) => {
      nameMap[m.laneId] = m.edgeName || m.laneId
    })
    laneIdToEdgeName.value = nameMap
    console.log('[TrackingPanel] Lane to Edge Name mappings loaded.')
  } catch (error) {
    console.error('[TrackingPanel] Failed to fetch lane mappings:', error)
  }
}

const fetchJunctions = async () => {
  try {
    const response = await axios.get('/api-status/junctions')
    const junctionData = Object.values(response.data)
    const nameMap: Record<string, string> = {}
    junctionData.forEach((j: any) => {
      nameMap[j.junction_id] = j.junction_name || j.junction_id
      // 存储连接信息用于计算lightIndex
      if (j.connection) {
        junctionConnectionMap.value.set(j.junction_id, j.connection)
      }
    })
    junctionIdToName.value = nameMap
    console.log('[TrackingPanel] Junction ID to Name mappings loaded.')
  } catch (error) {
    console.error('[TrackingPanel] Failed to fetch junctions:', error)
  }
}

// 计算lightIndex的函数（从ControlManual和ControlAI参考而来）
const findLightIndex = (junctionId: string, fromLaneId: string, toLaneId: string): number => {
  const connections = junctionConnectionMap.value.get(junctionId)
  if (!connections) {
    console.warn(`[TrackingPanel] No connection data for junction: ${junctionId}`)
    return 0
  }

  for (let i = 0; i < connections.length; i++) {
    const connectionGroup = connections[i]
    for (let j = 0; j < connectionGroup.length; j++) {
      const conn = connectionGroup[j]
      if (conn.length >= 2 && conn[0] === fromLaneId && conn[1] === toLaneId) {
        console.log(`[TrackingPanel] Found lightIndex: ${i} for direction: ${fromLaneId} -> ${toLaneId}`)
        return i
      }
    }
  }

  console.warn(`[TrackingPanel] Could not find lightIndex for direction: ${fromLaneId} -> ${toLaneId}`)
  return 0
}

// 监听Store中的数据变化，并更新本地快照（仅更新数据，不自动触发状态）
watch(() => emergencyStore.activelyTrackedVehicle, (currentVehicle, oldVehicle) => {
  if (currentVehicle) {
    // 限制更新频率，只在重要数据变化时才更新
    const oldSnapshot = trackedVehicleSnapshot.value
    const hasSignificantChange = !oldSnapshot || 
      oldSnapshot.vehicleID !== currentVehicle.vehicleID ||
      oldSnapshot.upcomingJunctionID !== currentVehicle.upcomingJunctionID ||
      oldSnapshot.currentLaneID !== currentVehicle.currentLaneID ||
      oldSnapshot.nextLaneID !== currentVehicle.nextLaneID
    
    if (hasSignificantChange) {
      console.log('[TrackingPanel] 重要车辆数据变化，更新快照')
      trackedVehicleSnapshot.value = JSON.parse(JSON.stringify(currentVehicle))
    } else {
      console.log('[TrackingPanel] 车辆位置微小更新，跳过以避免干扰用户操作')
    }
  } else if (oldVehicle) {
    handleTrackingComplete(oldVehicle.vehicleID)
  }
}, { deep: true, immediate: true })

// 完全禁用这个watch以避免干扰用户的manual操作
// 用户可以通过紧急车辆面板中的控制按钮手动操作交通灯
/*
watch(
  () => [props.isVisible, trackedVehicleSnapshot.value],
  ([isVisible, currentVehicle]) => {
    if (isVisible && currentVehicle && currentVehicle.upcomingJunctionID) {
      console.log('[TrackingPanel] 面板已打开，但暂时禁用自动发送事件以避免干扰用户操作')
      // 略...
    } else if (!isVisible) {
      console.log('[TrackingPanel] 面板已关闭，清除交通灯状态')
      emit('traffic-light-cleared')
    }
  },
  { deep: true, immediate: false }
)
*/

// 只保留面板关闭时的清理逻辑
watch(() => props.isVisible, (isVisible) => {
  if (!isVisible) {
    console.log('[TrackingPanel] 面板已关闭，清除交通灯状态')
    emit('traffic-light-cleared')
  }
  // 完全禁用自动跳转功能，由用户手动控制
})

// 控制面板操作函数
const selectLight = (color: string) => {
  selectedLight.value = color
}

const resetForm = () => {
  selectedLight.value = ''
  duration.value = null
  durationDisplay.value = ''
  durationError.value = false
  emit('traffic-light-cleared')
}

const onlyAllowNumbers = (e: KeyboardEvent) => {
  const key = e.key
  if (!/[\d]/.test(key)) {
    e.preventDefault()
  }
}

const validateDuration = () => {
  durationDisplay.value = durationDisplay.value.replace(/[^\d]/g, '')
}

const handleBlur = () => {
  const val = parseInt(durationDisplay.value)
  if (!isNaN(val) && val >= 5 && val <= 300) {
    duration.value = val
    durationError.value = false
  } else {
    duration.value = null
    durationError.value = true
  }
}

const increaseDuration = () => {
  if (duration.value === null) duration.value = 5
  else if (duration.value < 300) duration.value++
  durationDisplay.value = duration.value.toString()
  durationError.value = false
}

const decreaseDuration = () => {
  if (duration.value && duration.value > 5) {
    duration.value--
  } else {
    duration.value = 5
  }
  durationDisplay.value = duration.value.toString()
  durationError.value = false
}

function handleTrackingComplete(vehicleId?: string) {
  const message = `Tracking Finished!`
  
  // 弹出提示框
  toast.success(message)
  
  // 调用 store action 清理全局状态和localStorage
  emergencyStore.completeTracking()
  
  // 延迟3秒后，自动关闭面板
  setTimeout(() => {
    closePanel()
  }, 3000)
}

function startCloseAnimation() {
  isClosing.value = true
  // 等待动画完成后再发出关闭事件
  setTimeout(() => {
    isClosing.value = false
    emit('close')
  }, 400) // 与 CSS 动画时间一致
}

function closePanel() {
  emit('close')
}

// 应用交通灯控制
const handleApply = async () => {
  if (!trackedVehicleSnapshot.value || !isApproachingSignalizedJunction.value) {
    toast.error('No junction information available')
    return
  }

  const junctionId = trackedVehicleSnapshot.value.upcomingJunctionID!
  const fromLaneId = trackedVehicleSnapshot.value.currentLaneID
  const toLaneId = trackedVehicleSnapshot.value.nextLaneID

  if (!fromLaneId || !toLaneId) {
    toast.error('Lane information not available')
    return
  }

  isApplying.value = true

  // 计算lightIndex
  const lightIndex = findLightIndex(junctionId, fromLaneId, toLaneId)
  const state = selectedLight.value === 'GREEN' ? 'G' : 'r'

  // 构建请求体（参考ControlAI和ControlManual的实现）
  const requestBody = {
    junctionId: junctionId,
    lightIndex: lightIndex,
    duration: duration.value!,
    state: state,
    source: 'emergency' // 标记为紧急车辆控制
  }

  const junctionName = displayJunctionName.value
  const fromEdge = displayFromName.value
  const toEdge = displayToName.value
  const lightColor = selectedLight.value === 'GREEN' ? 'Green' : 'Red'

  // 记录操作
  const recordId = operationStore.addRecord({
    description: `Emergency: Set ${junctionName} light from ${fromEdge} to ${toEdge} to ${lightColor} for ${duration.value}s`,
    source: 'emergency',
    junctionId: junctionId,
    junctionName: junctionName,
    lightIndex: lightIndex,
    state: state,
    duration: duration.value!
  })

  try {
    // 发送请求到后端（使用与ControlAI和ControlManual相同的端点）
    await apiClient.post('/signalcontrol/manual', requestBody)
    
    operationStore.updateRecordStatus(recordId, 'success')
    toast.success('Emergency traffic light control applied successfully!')

    // 发出手动控制应用事件
    const directionInfo = `${fromEdge} → ${toEdge}`
    emit('manual-control-applied', {
      junctionName,
      directionInfo,
      lightColor,
      duration: duration.value!
    })

    // 部分重置表单
    selectedLight.value = ''
    duration.value = null
    durationDisplay.value = ''
    durationError.value = false
  } catch (error) {
    console.error('[TrackingPanel] Failed to apply traffic light control:', error)
    operationStore.updateRecordStatus(recordId, 'failed', 'Failed to send data to backend')
    toast.error('Failed to apply emergency traffic light control.')
  } finally {
    isApplying.value = false
  }
}

onMounted(async () => {
  await Promise.all([fetchLaneMappings(), fetchJunctions()])
  
  // 确保组件完全初始化后再进行权限检查
  await nextTick()
  
  const junctionId = trackedVehicleSnapshot.value?.upcomingJunctionID
  if (junctionId && manualControlRef.value) {
    manualControlRef.value.selectJunctionById(junctionId)
    // 等待一段时间后刷新权限，确保 mapCenterX 已初始化
    setTimeout(() => {
      if (manualControlRef.value) {
        manualControlRef.value.forceRefreshPermissions()
      }
    }, 1000)
  }
})
</script>

<style scoped lang="scss">
.tracking-panel {
  position: fixed;
  top: 0.64rem;
  right: 0;
  width: 35vw;
  height: calc(100vh - 0.64rem);
  background: #1E1E2F;
  border-left: 0.01rem solid #3A3A4C;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  transform: translateX(0);
  transition: transform 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  box-shadow: -0.08rem 0 0.3rem rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(0.1rem);
  animation: slideInRight 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  
  // 添加统一的背景纹理效果（与ControlManual一致）
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background:
      radial-gradient(circle at 20% 20%, rgba(74, 85, 104, 0.05) 0%, transparent 50%),
      radial-gradient(circle at 80% 80%, rgba(113, 128, 150, 0.03) 0%, transparent 50%),
      linear-gradient(45deg, transparent 48%, rgba(74, 85, 104, 0.02) 49%, rgba(74, 85, 104, 0.02) 51%, transparent 52%);
    pointer-events: none;
    z-index: 0;
  }

  > * {
    position: relative;
    z-index: 1;
  }
}

.tracking-panel.slide-out {
  transform: translateX(100%);
  animation: slideOutRight 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

@keyframes slideOutRight {
  from {
    transform: translateX(0);
    opacity: 1;
  }
  to {
    transform: translateX(100%);
    opacity: 0;
  }
}

.tracking-header {
  background: #1E1E2F;
  padding: 0;
  flex-shrink: 0;
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 0.01rem solid #3A3A4C;
}

.header-title {
  display: flex;
  align-items: flex-start;
  padding: 0.16rem 0 0.16rem 0.24rem;

  span {
    font-size: 0.2rem;
    font-weight: 700;
    color: #00E5FF;
    letter-spacing: 0.02rem;
  }
}

.close-button {
  background: none;
  border: none;
  color: #FFFFFF;
  cursor: pointer;
  padding: 0.12rem;
  margin-right: 0.16rem;
  border-radius: 0.06rem;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    background: rgba(74, 85, 104, 0.1);
    color: #00E5FF;
    transform: scale(1.1);
  }

  svg {
    width: 0.2rem;
    height: 0.2rem;
  }
}

.tracking-content {
  flex: 1;
  overflow: hidden;
  position: relative;
  background: #1E1E2F;
}

.vehicle-info-section {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0.24rem;
  gap: 0.35rem;
  overflow: hidden; /* 隐藏滚动条 */
}

.info-display-section {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.info-item {
  display: grid;
  grid-template-columns: 100px 1fr;
  align-items: center;
  gap: 0.18rem;
}

.info-label {
  font-size: 0.14rem;
  color: #FFFFFF;
  font-weight: 600;
  text-align: left;
  padding-left: 0.12rem;
  flex-shrink: 0;
  letter-spacing: 0.02rem;
}

.info-value-box {
  background: linear-gradient(135deg, #1E2139 0%, #2A2D4A 100%);
  border: 1px solid rgba(74, 85, 104, 0.4);
  border-radius: 0.06rem;
  padding: 0 0.1rem;
  font-size: 0.14rem;
  color: #E3F2FD;
  font-weight: 500;
  min-height: 0.38rem;
  display: flex;
  align-items: center;
  width: 100%;
  box-sizing: border-box;
  transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(45deg, transparent 48%, rgba(74, 85, 104, 0.1) 49%, rgba(74, 85, 104, 0.1) 51%, transparent 52%);
    opacity: 0;
    transition: opacity 0.3s ease;
    pointer-events: none;
  }

  &:hover {
    border-color: rgba(113, 128, 150, 0.6);
    box-shadow: 0 0 15px rgba(0, 0, 0, 0.2);
    background: linear-gradient(135deg, #2A2D4A 0%, #1E2139 100%);

    &::before {
      opacity: 1;
    }
  }
}

.status-approaching {
  color: #F97316 !important;
  border-color: rgba(249, 115, 22, 0.5) !important;
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.1) 0%, rgba(30, 33, 57, 0.9) 100%) !important;

  &::before {
    background: linear-gradient(45deg, transparent 48%, rgba(249, 115, 22, 0.1) 49%, rgba(249, 115, 22, 0.1) 51%, transparent 52%) !important;
  }
}

.status-enroute {
  color: #00E676 !important;
  border-color: rgba(0, 230, 118, 0.5) !important;
  background: linear-gradient(135deg, rgba(0, 230, 118, 0.1) 0%, rgba(30, 33, 57, 0.9) 100%) !important;

  &::before {
    background: linear-gradient(45deg, transparent 48%, rgba(0, 230, 118, 0.1) 49%, rgba(0, 230, 118, 0.1) 51%, transparent 52%) !important;
  }
}



.simplified-control-section {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}





/* Light Buttons - 与 ControlManual 完全一致 */
.light-buttons {
  display: flex;
  gap: 0.3rem;
}

.light-btn {
  width: 1rem;
  height: 0.4rem;
  border: none;
  border-radius: 0.08rem;
  font-weight: 700;
  font-size: 0.14rem;
  color: #FFFFFF;
  cursor: pointer;
  background: linear-gradient(135deg, #1E2139 0%, #2A2D4A 100%);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
    transition: left 0.6s;
  }

  &:hover::before {
    left: 100%;
  }
}

.red {
  color: #FF4569;
  border-color: rgba(255, 69, 105, 0.3);

  &:hover:not(.active-red) {
    background: linear-gradient(135deg, #FF4569 20%, #2A2D4A 80%);
    color: #FFFFFF;
    transform: translateY(-2px) scale(1.02);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.4);
    border-color: rgba(255, 69, 105, 0.6);
  }
}

.green {
  color: #00E676;
  border-color: rgba(0, 230, 118, 0.3);

  &:hover:not(.active-green) {
    background: linear-gradient(135deg, #00E676 20%, #2A2D4A 80%);
    color: #FFFFFF;
    transform: translateY(-2px) scale(1.02);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.4);
    border-color: rgba(0, 230, 118, 0.6);
  }
}

.active-red {
  background: linear-gradient(135deg, #FF4569 0%, #E91E63 100%);
  color: #FFFFFF;
  border-color: rgba(255, 69, 105, 0.8);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.5);
  transform: translateY(-1px);
}

.active-green {
  background: linear-gradient(135deg, #00E676 0%, #4CAF50 100%);
  color: #FFFFFF;
  border-color: rgba(0, 230, 118, 0.8);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.5);
  transform: translateY(-1px);
}

/* Duration 输入框 - 与 ControlManual 完全一致 */
.form-row-duration {
  display: flex;
  flex-direction: column;
  gap: 0.08rem;
}

.duration-custom {
  display: flex;
  align-items: center;
  height: 0.4rem;
  border: 1px solid rgba(74, 85, 104, 0.4);
  border-radius: 0.06rem;
  background: linear-gradient(135deg, #1E2139 0%, #2A2D4A 100%);
  overflow: hidden;
  transition: all 0.4s ease;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(45deg, transparent 48%, rgba(74, 85, 104, 0.1) 49%, rgba(74, 85, 104, 0.1) 51%, transparent 52%);
    opacity: 0;
    transition: opacity 0.3s ease;
    pointer-events: none;
  }

  &:focus-within {
    border-color: rgba(113, 128, 150, 0.6);
    box-shadow: 0 0 15px rgba(0, 0, 0, 0.2);
    background: linear-gradient(135deg, #2A2D4A 0%, #1E2139 100%);

    &::before {
      opacity: 1;
    }
  }
}

.custom-input {
  height: inherit;
  width: 1.2rem;
  background: transparent;
  border: none;
  color: #E3F2FD;
  font-size: 0.14rem;
  padding: 0.1rem 0.2rem;
  text-align: center;
  outline: none;
  transition: all 0.3s ease;
  font-weight: 500;
  text-shadow: 0 0 8px rgba(227, 242, 253, 0.3);
  position: relative;
  z-index: 1;

  &::placeholder {
    color: rgba(156, 163, 175, 0.6);
    transition: color 0.3s ease;
  }
}

.triangle-buttons {
  display: flex;
  flex-direction: column;
  justify-content: center;
  height: 100%;
  background: linear-gradient(135deg, #1E2139 0%, #2A2D4A 100%);
  border-left: 1px solid rgba(74, 85, 104, 0.2);
}

.triangle-btn {
  flex: 1;
  width: 0.4rem;
  border: none;
  background: transparent;
  color: #9CA3AF;
  font-size: 0.14rem;
  cursor: pointer;
  line-height: 1;
  transition: all 0.4s ease;
  position: relative;
  font-weight: 700;

  &:hover {
    background: rgba(113, 128, 150, 0.15);
    color: #D1D5DB;
    transform: scale(1.2);
  }

  &:active {
    transform: scale(1.05);
    background: rgba(209, 213, 219, 0.2);
  }

  &:first-child {
    border-bottom: 1px solid rgba(74, 85, 104, 0.2);
  }
}

.duration-error-container {
  height: 0.20rem;
  display: flex;
  align-items: flex-start;
}

.duration-error {
  color: #EF4444;
  font-size: 0.1rem;
  padding: 0.02rem 0.06rem;
  margin-left: 1.16rem;
  white-space: nowrap;
  font-weight: 600;
  display: inline-block;
  line-height: 1;
  height: auto;
  max-width: 3rem;
}

/* Action Buttons - 与 ControlManual 完全一致 */
.action-buttons {
  display: flex;
  justify-content: space-between;
  gap: 0.3rem;
  width: 100%;
}

.apply-btn,
.cancel-btn {
  width: 1.4rem;
  height: 0.4rem;
  font-size: 0.14rem;
  font-weight: 700;
  border-radius: 0.2rem;
  border: 1px solid;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  position: relative;
  overflow: hidden;
  text-shadow: 0 0 8px rgba(255, 255, 255, 0.3);

  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 0;
    height: 0;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 50%;
    transform: translate(-50%, -50%);
    transition: all 0.4s ease;
  }

  &:active::before {
    width: 300%;
    height: 300%;
  }
}

.apply-btn {
  background: linear-gradient(135deg, #00B4D8 0%, #0090aa 100%);
  color: #FFFFFF;
  border-color: rgba(0, 180, 216, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.08rem;

  .loading-spinner {
    width: 0.16rem;
    height: 0.16rem;
    border: 2px solid rgba(255, 255, 255, 0.3);
    border-top: 2px solid #FFFFFF;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  span {
    position: relative;
    z-index: 2;
    font-weight: 700;
  }

  &:not(:disabled):hover {
    background: linear-gradient(135deg, #00d4f8 0%, #00B4D8 100%);
    transform: translateY(-2px) scale(1.02);
    box-shadow: 0 8px 25px rgba(0, 180, 216, 0.4);
    border-color: rgba(0, 180, 216, 0.8);
  }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.apply-btn:disabled {
  background: linear-gradient(135deg, #4A5568 0%, #2D3748 100%);
  color: #A0AEC0;
  border-color: rgba(74, 85, 104, 0.5);
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
  text-shadow: none;
}

.cancel-btn {
  background: linear-gradient(135deg, #718096 0%, #4A5568 100%);
  color: #FFFFFF;
  border-color: rgba(113, 128, 150, 0.5);

  &:hover {
    background: linear-gradient(135deg, #A0AEC0 0%, #718096 100%);
    transform: translateY(-2px) scale(1.02);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
    border-color: rgba(113, 128, 150, 0.8);
  }
}

/* Waiting message 样式 - 简洁的文字提示 */
.waiting-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 1rem 0.5rem;
}

.waiting-text {
  color: rgba(156, 163, 175, 0.8);
  font-size: 0.16rem;
  font-weight: 600;
  font-style: italic;
  margin: 0 0 0.12rem 0;
  line-height: 1.4;
}

.waiting-subtext {
  color: rgba(156, 163, 175, 0.6);
  font-size: 0.14rem;
  font-style: italic;
  line-height: 1.5;
  margin: 0;
  max-width: 300px;
}

.completion-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #6b7280;
  text-align: center;
  padding: 0 0.24rem;

  .empty-icon {
    font-size: 0.48rem;
    margin-bottom: 0.16rem;
    opacity: 0.5;
  }

  .empty-text {
    font-size: 0.14rem;
    font-weight: 500;
    margin-bottom: 0.08rem;
    color: #FFFFFF;
  }

  .empty-subtext {
    font-size: 0.12rem;
    color: #9ca3af;
    line-height: 1.4;
  }
}

@media (max-width: 1200px) {
  .tracking-panel {
    width: 40vw;
  }
}

@media (max-width: 768px) {
  .tracking-panel {
    width: 100vw;
    right: 0;
  }
}
</style>
