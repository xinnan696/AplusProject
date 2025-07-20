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
          @signal-light-clicked="handleSignalLightClicked"
          :tracked-vehicle="emergencyStore.activelyTrackedVehicle"
        />
      </div>

      <!-- 2. 右侧控制面板区域 -->
      <div class="control-board">
<!--        <div v-if="emergencyStore.activelyTrackedVehicle" class="tracking-panel">-->
        <div v-if="trackedVehicleSnapshot" class="tracking-panel">
        <!-- 面板标题 (新样式) -->
          <div class="panel-title">
            Priority Vehicle Tracking
          </div>

          <!-- 车辆和路口信息展示区 (新样式) -->
          <div class="info-display-section">
            <div class="info-item">
              <label class="info-label">Vehicle ID</label>
              <div class="info-value-box">
<!--                {{ emergencyStore.activelyTrackedVehicle.vehicleID }}-->
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

          <!-- 手动控制面板 -->
          <div class="manual-control-placeholder">
            <ControlManual
              ref="manualControlRef"
              @highlight="handleHighlight"
              @traffic-light-selected="handleTrafficLightSelected"
              @traffic-light-cleared="handleTrafficLightCleared"
              @junction-selected="handleJunctionSelected"
              @manual-control-applied="handleManualControlApplied"
            />
          </div>

        </div>

<!--        <div v-else class="completion-message">-->
<!--          <p>{{ completionMessage || '没有正在追踪的紧急车辆。' }}</p>-->
<!--        </div>-->
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed, nextTick } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import { useEmergencyStore, type VehicleTrackingData } from '@/stores/emergency'
import ControlHeader from '@/views/control/ControlHeader.vue'
import ControlNav from '@/views/control/ControlNav.vue'
import { isNavVisible, toggleNav } from '@/utils/navState'
import ControlMap from '@/views/control/ControlMap.vue'
import ControlManual from './ControlManual.vue'
import {toast} from "@/utils/ToastService";

const router = useRouter()
const emergencyStore = useEmergencyStore()
const mapRef = ref()
const manualControlRef = ref()
const controlBoardRef = ref()
const completionMessage = ref('')

// ### 新增 4: 创建一个本地ref来存储车辆数据的快照 ###
// 这个快照将在追踪结束时保持最后的状态，防止UI清空
const trackedVehicleSnapshot = ref<VehicleTrackingData | null>(null);


// 用于存储映射数据
const laneIdToEdgeName = ref<Record<string, string>>({})
const junctionIdToName = ref<Record<string, string>>({})

const hasPendingEmergencies = computed(() => emergencyStore.pendingVehicles.length > 0)

// const isApproachingSignalizedJunction = computed(() => {
//   return !!emergencyStore.activelyTrackedVehicle?.upcomingJunctionID;
// });
//
// // 计算属性用于显示名称
// const displayJunctionName = computed(() => {
//   const junctionId = emergencyStore.activelyTrackedVehicle?.upcomingJunctionID;
//   if (!junctionId) return 'Waiting...'; // 如果ID为null，直接返回 'Waiting...'
//   return junctionIdToName.value[junctionId] || junctionId;
// });
//
// const displayFromName = computed(() => {
//   // From 字段总是显示当前车道，只有在没有下一个路口时才隐藏
//   const laneId = emergencyStore.activelyTrackedVehicle?.currentLaneID;
//   if (!isApproachingSignalizedJunction.value) return 'Waiting...';
//   if (!laneId) return 'N/A'; // 如果有下一个路口但当前车道ID为空，显示N/A
//   return laneIdToEdgeName.value[laneId] || laneId;
// });
//
// const displayToName = computed(() => {
//   const laneId = emergencyStore.activelyTrackedVehicle?.nextLaneID;
//   if (!isApproachingSignalizedJunction.value) return 'Waiting...';
//   if (!laneId) return 'N/A'; // 如果有下一个路口但下一车道ID为空，显示N/A
//   return laneIdToEdgeName.value[laneId] || laneId;
// });

// ### 修改 5: 所有计算属性现在都基于本地快照 ###
const isApproachingSignalizedJunction = computed(() => {
  return !!trackedVehicleSnapshot.value?.upcomingJunctionID;
});

const displayJunctionName = computed(() => {
  const junctionId = trackedVehicleSnapshot.value?.upcomingJunctionID;
  if (!junctionId) return 'Waiting...';
  return junctionIdToName.value[junctionId] || junctionId;
});

const displayFromName = computed(() => {
  const laneId = trackedVehicleSnapshot.value?.currentLaneID;
  if (!isApproachingSignalizedJunction.value) return 'Waiting...';
  if (!laneId) return 'N/A';
  return laneIdToEdgeName.value[laneId] || laneId;
});

const displayToName = computed(() => {
  const laneId = trackedVehicleSnapshot.value?.nextLaneID;
  if (!isApproachingSignalizedJunction.value) return 'Waiting...';
  if (!laneId) return 'N/A';
  return laneIdToEdgeName.value[laneId] || laneId;
});

const currentJunctionId = computed(() => emergencyStore.activelyTrackedVehicle?.upcomingJunctionID)
// const currentJunctionName = computed(() => {
//   if (currentJunctionId.value && manualControlRef.value) {
//     return manualControlRef.value.getJunctionNameById(currentJunctionId.value) || currentJunctionId.value;
//   }
//   return currentJunctionId.value;
// })


const approachStatusText = computed(() => {
  return isApproachingSignalizedJunction.value ? 'Approaching Junction' : 'In Route';
});

const approachStatusClass = computed(() => {
  return isApproachingSignalizedJunction.value ? 'status-approaching' : 'status-enroute';
});

// 数据获取函数，与 ControlManual.vue 逻辑一致
const fetchLaneMappings = async () => {
  try {
    const response = await axios.get('/api-status/lane-mappings');
    const mappings = Array.isArray(response.data) ? response.data : Object.values(response.data);
    const nameMap: Record<string, string> = {};
    mappings.forEach((m: any) => {
      nameMap[m.laneId] = m.edgeName || m.laneId;
    });
    laneIdToEdgeName.value = nameMap;
    console.log('[TrackingPage] Lane to Edge Name mappings loaded.');
  } catch (error) {
    console.error('[TrackingPage] Failed to fetch lane mappings:', error);
  }
};

const fetchJunctions = async () => {
  try {
    const response = await axios.get('/api-status/junctions');
    const junctionData = Object.values(response.data);
    const nameMap: Record<string, string> = {};
    junctionData.forEach((j: any) => {
      nameMap[j.junction_id] = j.junction_name || j.junction_id;
    });
    junctionIdToName.value = nameMap;
    console.log('[TrackingPage] Junction ID to Name mappings loaded.');
  } catch (error) {
    console.error('[TrackingPage] Failed to fetch junctions:', error);
  }
};

// watch(() => emergencyStore.activelyTrackedVehicle, (currentVehicle, oldVehicle) => {
//   if (!currentVehicle && oldVehicle) {
//     handleTrackingComplete(oldVehicle.vehicleID)
//   }
// }, { deep: true })
//
// watch(currentJunctionId, async (newJunctionId) => {
//   if (newJunctionId) {
//     await nextTick();
//     if (manualControlRef.value) {
//       manualControlRef.value.selectJunctionById(newJunctionId);
//     }
//   }
// });

// ### 修改 6: 监听Store中的数据变化，并更新本地快照 ###
watch(() => emergencyStore.activelyTrackedVehicle, (currentVehicle, oldVehicle) => {
  if (currentVehicle) {
    // 只要有新数据，就用深拷贝更新快照，防止意外的响应式副作用
    trackedVehicleSnapshot.value = JSON.parse(JSON.stringify(currentVehicle));
  } else if (oldVehicle) {
    // 当Store中的数据从有变为null时，代表追踪结束
    // 此时不再更新快照，UI将保持最后的状态
    handleTrackingComplete(oldVehicle.vehicleID);
  }
}, { deep: true, immediate: true }); // immediate: true 确保组件加载时立即执行一次

// 监听下一个路口ID的变化，自动在手控面板中选中
watch(() => trackedVehicleSnapshot.value?.upcomingJunctionID, async (newJunctionId) => {
  if (newJunctionId) {
    await nextTick();
    if (manualControlRef.value) {
      manualControlRef.value.selectJunctionById(newJunctionId);
    }
  }
});


// function handleTrackingComplete(vehicleId?: string) {
//   completionMessage.value = `追踪完毕！车辆 ${vehicleId || ''} 已通过所有关键交叉口。`
//   emergencyStore.completeTracking()
//
//   setTimeout(() => {
//     router.push({ name: 'ControlHome' })
//   }, 3000)
// }

/**
 * ### 修改 7: 更新追踪完成处理函数 ###
 * 它不再修改任何本地UI状态，只负责弹出提示框、清理全局状态和跳转页面
 */
function handleTrackingComplete(vehicleId?: string) {
  const message = `Tracking Finished!`;

  // 步骤 1: 弹出提示框
  toast.success(message);

  // 步骤 2: 调用 store action 清理全局状态和localStorage
  emergencyStore.completeTracking();

  // 步骤 3: 延迟4秒后，自动跳转回主页
  setTimeout(() => {
    router.push({ name: 'Control' });
  }, 4000);
}

function handleSignOut() {
  localStorage.removeItem('authToken')
  router.push({ name: 'Login' })
}

onMounted(async () => {
  await Promise.all([fetchLaneMappings(), fetchJunctions()]);
  // 修改 8
  if (!emergencyStore.activelyTrackedVehicle) {
    router.push({ name: 'ControlHome' })
  } else if (trackedVehicleSnapshot.value && manualControlRef.value) {
    manualControlRef.value.selectJunctionById(trackedVehicleSnapshot.value);
  }

  // if (!emergencyStore.activelyTrackedVehicle) {
  //   router.push({ name: 'ControlHome' })
  // } else if (currentJunctionId.value && manualControlRef.value) {
  //   manualControlRef.value.selectJunctionById(currentJunctionId.value);
  // }
})

const handleSignalLightClicked = (junctionId: string) => {
  if (junctionId) {
    // 根据ID查找名称
    const junctionName = junctionIdToName.value[junctionId] || junctionId;
    // 更新控制面板
    manualControlRef.value?.setJunctionByName(junctionName);
    // 更新地图高亮
    mapRef.value?.setSelectedJunction(junctionName);
  }
};

// ### 新增 3: 实现从 ControlManual 组件发出的事件的处理函数 ###

/**
 * Handles highlighting lanes when a direction is selected in the manual panel.
 */
const handleHighlight = (fromLanes: string[], toLanes: string[]) => {
  mapRef.value?.setHighlightLanes(fromLanes, toLanes);
}

/**
 * Handles zooming to and selecting a junction on the map when it's chosen from the manual panel dropdown.
 */
const handleJunctionSelected = (junctionName: string, junctionId: string) => {
  console.log('Junction selected from panel, telling map to zoom:', { junctionName, junctionId });
  mapRef.value?.zoomToJunctionById(junctionId);
  mapRef.value?.setSelectedJunctionOnly(junctionId);
}

/**
 * Updates the traffic light status bar on the map when a specific direction is selected.
 */
const handleTrafficLightSelected = (junctionId: string, directionIndex: number) => {
  console.log('Traffic light direction selected:', { junctionId, directionIndex });
  mapRef.value?.setSelectedTrafficLight(junctionId, directionIndex);
}

/**
 * Clears the traffic light status bar on the map.
 */
const handleTrafficLightCleared = () => {
  console.log('Clearing traffic light status on map.');
  mapRef.value?.clearTrafficStatus();
}

/**
 * Updates the map's status bar after a manual control action is applied.
 */
const handleManualControlApplied = (data: { junctionName: string, directionInfo: string, lightColor: string, duration: number }) => {
  console.log('Manual control applied, updating map status:', data);
  mapRef.value?.handleManualControlApplied(data);
}

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
  font-size: 0.16rem;
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
