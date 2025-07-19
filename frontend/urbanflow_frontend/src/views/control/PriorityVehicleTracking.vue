<template>
  <div class="control-page">
    <!-- 1. 复用 ControlHeader 和 ControlNav -->
    <ControlHeader
      @toggle-nav="toggleNav"
      @sign-out="handleSignOut"
      :has-pending-emergencies="hasPendingEmergencies"
    />
    <ControlNav :isVisible="isNavVisible" />

    <div class="main-area">
      <!-- 左侧地图区域 -->
      <div class="map-contain">
        <ControlMap
          ref="mapRef"
          :isSidebarOpen="isNavVisible"
          :tracked-vehicle="emergencyStore.activelyTrackedVehicle"
        />
      </div>

      <!-- 2. 右侧控制面板区域 -->
      <div class="control-board">
        <div v-if="emergencyStore.activelyTrackedVehicle" class="tracking-panel">

          <!-- 面板标题 (新样式) -->
          <div class="panel-title">
            Priority Vehicle Tracking
          </div>

          <!-- 车辆和路口信息展示区 (新样式) -->
          <div class="info-display-section">
            <div class="info-item">
              <label class="info-label">Vehicle ID</label>
              <div class="info-value-box">
                {{ emergencyStore.activelyTrackedVehicle.vehicleID }}
              </div>
            </div>

            <div class="info-item">
              <label class="info-label">Junction</label>
              <div class="info-value-box">
                {{ currentJunctionName || currentJunctionId || 'Calculating...' }}
              </div>
            </div>
          </div>

          <!-- 手动控制面板 -->
          <div class="manual-control-placeholder">
            <ControlManual ref="manualControlRef" />
          </div>

        </div>

        <div v-else class="completion-message">
          <p>{{ completionMessage || '没有正在追踪的紧急车辆。' }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useEmergencyStore } from '@/stores/emergency'
import ControlHeader from '@/views/control/ControlHeader.vue'
import ControlNav from '@/views/control/ControlNav.vue'
import { isNavVisible, toggleNav } from '@/utils/navState'
import ControlMap from '@/views/control/ControlMap.vue'
import ControlManual from './ControlManual.vue'

const router = useRouter()
const emergencyStore = useEmergencyStore()
const mapRef = ref()
const manualControlRef = ref()
const completionMessage = ref('')

const hasPendingEmergencies = computed(() => emergencyStore.pendingVehicles.length > 0)

const currentJunctionId = computed(() => emergencyStore.activelyTrackedVehicle?.upcomingJunctionID)
const currentJunctionName = computed(() => {
  if (currentJunctionId.value && manualControlRef.value) {
    return manualControlRef.value.getJunctionNameById(currentJunctionId.value) || currentJunctionId.value;
  }
  return currentJunctionId.value;
})

watch(() => emergencyStore.activelyTrackedVehicle, (currentVehicle, oldVehicle) => {
  if (!currentVehicle && oldVehicle) {
    handleTrackingComplete(oldVehicle.vehicleID)
  }
}, { deep: true })

watch(currentJunctionId, async (newJunctionId) => {
  if (newJunctionId) {
    await nextTick();
    if (manualControlRef.value) {
      manualControlRef.value.selectJunctionById(newJunctionId);
    }
  }
});


function handleTrackingComplete(vehicleId?: string) {
  completionMessage.value = `追踪完毕！车辆 ${vehicleId || ''} 已通过所有关键交叉口。`
  emergencyStore.completeTracking()

  setTimeout(() => {
    router.push({ name: 'ControlHome' })
  }, 3000)
}

function handleSignOut() {
  localStorage.removeItem('authToken')
  router.push({ name: 'Login' })
}

onMounted(() => {
  if (!emergencyStore.activelyTrackedVehicle) {
    router.push({ name: 'ControlHome' })
  } else if (currentJunctionId.value && manualControlRef.value) {
    manualControlRef.value.selectJunctionById(currentJunctionId.value);
  }
})
</script>

<style scoped lang="scss">
/* 复用 ControlHome 的页面布局样式 */
.control-page {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  width: 100%; height: 100%;
  display: flex; flex-direction: column;
  overflow: hidden;
  background-color: #1E1E2F;
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
}

/* 追踪面板专属样式 */
.tracking-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0.24rem;
  gap: 0.24rem;
}

/* ### 关键修改：标题样式 ### */
.panel-title {
  font-size: 14px; /* 要求的字号 */
  font-weight: 600;
  color: #FF4D4F; /* 要求的红色高亮 */
  padding-bottom: 0.16rem;
  border-bottom: 1px solid #3A3A4C;
  /* 默认就是左对齐，无需额外设置 */
}

.info-display-section {
  display: flex;
  flex-direction: column;
  gap: 0.16rem;
}

/* ### 关键修改：信息项布局和样式 ### */
.info-item {
  display: grid; /* 使用Grid布局实现精确对齐 */
  grid-template-columns: 100px 1fr; /* 左侧标签固定宽度，右侧自适应 */
  align-items: center;
  gap: 0.16rem;
}

.info-label {
  width: 1.6rem;
  font-size: 0.14rem;
  color: #B3E5FC;
  font-weight: 600;
  text-align: left; /* 标签左对齐 */
  padding-left: 0.24rem;
  flex-shrink: 0; /* 防止标签被压缩 */
}

.info-value-box {
  background: linear-gradient(135deg, #1E2139 0%, #2A2D4A 100%);
  border: 1px solid rgba(0, 180, 216, 0.4);
  border-radius: 0.06rem;
  padding: 0 0.1rem;
  font-size: 0.14rem;
  color: #FFFFFF;
  font-weight: 500;
  min-height: 0.38rem;
  display: flex;
  align-items: center;
  width: 100%;
  box-sizing: border-box; /* 确保padding不会影响宽度 */
}

.manual-control-placeholder {
  flex-grow: 1;
  border-top: 1px solid #3A3A4C;
  margin-top: 0.1rem;
  padding-top: 0.24rem;
  display: flex;
  flex-direction: column;
}

.completion-message {
  flex-grow: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px;
  text-align: center;

  p {
    color: #00E676;
    font-size: 18px;
    font-weight: 600;
  }
}
</style>
