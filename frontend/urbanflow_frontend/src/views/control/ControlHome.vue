<template>
  <div class="control-page">
    <ControlHeader
      :isRecordPanelVisible="isRecordVisible"
      :showEmergencyIcon="hasPendingEmergencies"
      :hasNewRequests="hasPendingEmergencies"
      @toggle-nav="toggleNav"
      @toggle-record="toggleRecord"
      @emergency-icon-clicked="toggleEmergency"
      @mode-changed="handleModeChange"
      @sign-out="handleSignOut"
    />
    <ControlNav :isVisible="isNavVisible" />

    <div class="main-area">
      <div class="map-contain">
        <ControlMap ref="mapRef" :isSidebgit add .ideaarOpen="isNavVisible" @signal-light-clicked="handleSignalLightClicked" />
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

  <transition name="dialog">
    <div v-if="isEmergencyVisible && firstPendingVehicle" class="tracking-request-dialog">
      <div class="dialog-title">
        Special Vehicle Tracking Request
      </div>
      <div class="dialog-content">
        <span class="info-label">Vehicle ID</span>
        <span class="info-value">{{ firstPendingVehicle.vehicleID }}</span>

        <span class="info-label">Organization</span>
        <span class="info-value">{{ firstPendingVehicle.organization }}</span>

        <span class="info-label route-label">Estimated Route</span>
        <div class="info-value route-list">
          <div v-for="(junction, index) in firstPendingVehicle.signalizedJunctions" :key="junction" class="route-item">
            <span>{{ junction.replace(/\s*\(.*\)/, '') }}</span>
            <span v-if="index === 0" class="tag start">START</span>
            <span v-if="index === firstPendingVehicle.signalizedJunctions.length - 1" class="tag destination">DESTINATION</span>
          </div>
        </div>

        <span class="info-label request-label">Request</span>
        <span class="info-value request-value">Green Light Priority</span>
      </div>
      <div class="dialog-actions">
        <button class="btn-approve" @click="handleApprove">APPROVE</button>
        <button class="btn-reject" @click="handleReject">REJECT</button>
      </div>
    </div>
  </transition>
  </div>
</template>

<script setup lang="ts">
console.log("🔧 MOCK: Setting mock authentication for testing purposes.");
localStorage.setItem('authToken', 'mock-auth-token-for-testing');
localStorage.setItem('user', JSON.stringify({
  role: 'ADMIN',
  userName: 'Test Admin'
}));
import { useRouter } from 'vue-router'
import { useEmergencyStore } from '@/stores/emergency'
import ControlHeader from '@/views/control/ControlHeader.vue'
import ControlMap from '@/views/control/ControlMap.vue'
import ControlBoard from './ControlBoard.vue'
import ControlNav from './ControlNav.vue'
import ControlRecord from './ControlRecord.vue'
import { isNavVisible, toggleNav } from '@/utils/navState'
import PriorityVehicleTracking from '@/views/control/PriorityVehicleTracking.vue'
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const router = useRouter()
const emergencyStore = useEmergencyStore()

const mapRef = ref()
const controlBoardRef = ref()
const isRecordVisible = ref(false)
const isEmergencyVisible = ref(false)
const isPriorityVisible = ref(false)
const isAIMode = ref(false)

const hasPendingEmergencies = computed(() => emergencyStore.pendingVehicles.length > 0)

const firstPendingVehicle = computed(() => {
  return hasPendingEmergencies.value ? emergencyStore.pendingVehicles[0] : null
})

onMounted(() => {
  console.log("🚑 启动紧急车辆WebSocket连接...")
  emergencyStore.connectWebSocket()
  document.addEventListener('keydown', handleKeyDown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeyDown)
})

function handleApprove() {
  console.log("✅ 用户批准紧急车辆请求")
  if (firstPendingVehicle.value) {
    emergencyStore.approveVehicle(firstPendingVehicle.value.vehicleID)
    isEmergencyVisible.value = false
    router.push({ name: 'PriorityVehicleTracking' })
  }
}

function handleReject() {
  console.log("❌ 用户拒绝紧急车辆请求")
  if (firstPendingVehicle.value) {
    emergencyStore.rejectVehicle(firstPendingVehicle.value.vehicleID)
    if (emergencyStore.pendingVehicles.length === 0) {
      isEmergencyVisible.value = false
    }
  }
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

const handleTrafficLightSelected = (junctionId: string, directionIndex: number, triggerSource?: 'junction' | 'direction') => {
  console.log('[Home] Traffic light selected:', { junctionId, directionIndex, triggerSource })
  mapRef.value?.setSelectedTrafficLight(junctionId, directionIndex)
}

const handleTrafficLightCleared = () => {
  console.log('[Home] Traffic light cleared')
  mapRef.value?.clearTrafficStatus()
}

const handleJunctionSelected = (junctionName: string, junctionId: string) => {
  console.log('[Home] Junction selected for zoom:', { junctionName, junctionId })

  mapRef.value?.zoomToJunctionById(junctionId)

  mapRef.value?.setSelectedJunctionOnly(junctionId)
}

const handleManualControlApplied = (data: { junctionName: string, directionInfo: string, lightColor: string, duration: number }) => {
  console.log('🎯 [Home] Manual control applied:', data)
  mapRef.value?.handleManualControlApplied(data)
}

const toggleRecord = () => {
  isRecordVisible.value = !isRecordVisible.value
  if (isRecordVisible.value) {
    isEmergencyVisible.value = false
    isPriorityVisible.value = false
  }
}

const toggleEmergency = () => {
  console.log("🚨 切换紧急车辆对话框显示状态")
  isEmergencyVisible.value = !isEmergencyVisible.value
  if (isEmergencyVisible.value) {
    isRecordVisible.value = false
    isPriorityVisible.value = false
  }
}

const togglePriority = () => {
  isPriorityVisible.value = !isPriorityVisible.value
  if (isPriorityVisible.value) {
    isRecordVisible.value = false
    isEmergencyVisible.value = false
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


const handleKeyDown = (event: KeyboardEvent) => {
  if (event.target instanceof HTMLInputElement || event.target instanceof HTMLTextAreaElement) {
    return
  }

  switch (event.key) {
    case 'Escape':
      isRecordVisible.value = false
      isEmergencyVisible.value = false
      isPriorityVisible.value = false
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
  }
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
  height: calc(100% - 0.64rem);
  display: flex;
  position: relative;
}

.map-contain {
  width: 13.59rem;
  height: 100%;
  position: relative;
  overflow: hidden;
  border-right: 1px solid #3A3A4C;
}

.control-board {
  width: 5.61rem;
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


.dialog-enter-active, .dialog-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.dialog-enter-from, .dialog-leave-to {
  opacity: 0;
  transform: scale(0.95) translateY(10px);
}
.tracking-request-dialog {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 480px;
  background-color: #2C2F48;
  border-radius: 12px;
  border: 1px solid rgba(74, 85, 104, 0.5);
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
  z-index: 2000;
  color: #E0E0E0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.dialog-title {
  padding: 0.16rem 0.24rem;
  font-size: 0.20rem;
  font-weight: 600;
  color: #FF4D4F;
  background-color: rgba(0, 0, 0, 0.1);
  border-bottom: 1px solid rgba(74, 85, 104, 0.5);
}
.dialog-content {
  padding: 0.24rem;
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 0.16rem;
  align-items: center;
  font-size: 0.16rem;
}
.info-label {
  font-weight: 500;
  color: #A0AEC0;
  text-align: right;
}
.info-value {
  font-weight: 600;
  color: #FFFFFF;
}
.route-label {
  align-self: start;
}
.info-value.route-list {
  display: flex;
  flex-direction: column;
  gap: 0.08rem;
}
.route-item {
  display: flex;
  align-items: center;
  gap: 0.08rem;
}
.route-item .tag {
  font-weight: 700;
  font-size: 0.12rem;
}
.tag.start, .tag.destination {
  color: #00B4D8;
  background: none;
}
.request-label, .request-value {
  color: #00B4D8;
}
.dialog-actions {
  padding: 0.16rem 0.24rem;
  display: flex;
  justify-content: center;
  gap: 0.46rem;
  background-color: rgba(0, 0, 0, 0.1);
  border-top: 1px solid rgba(74, 85, 104, 0.5);
}
.dialog-actions button {
  padding: 0.10rem 0.24rem;
  border: 1px solid transparent;
  font-size: 0.16rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}
.btn-approve {
  background-color: #007BFF;
  color: #FFFFFF;
  border-color: #007BFF;
}
.btn-approve:hover {
  background-color: #0056b3;
  transform: translateY(-1px);
}
.btn-reject {
  background-color: #6C757D;
  color: #FFFFFF;
  border-color: #6C757D;
}
.btn-reject:hover {
  background-color: #5a6268;
  transform: translateY(-1px);
}
</style>
