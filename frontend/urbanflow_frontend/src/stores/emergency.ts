import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios' // 确保您项目中有一个配置好的axios实例

/**
 * 定义从后端WebSocket接收到的原始车辆追踪数据结构
 */
interface RawVehicleData {
  eventID: string
  vehicleID: string
  organization: string
  currentEdgeID: string
  upcomingJunctionID: string | null
  nextEdgeID: string | null
  upcomingTlsID: string | null
  upcomingTlsState: string | null
  upcomingTlsCountdown: number | null
  position: {
    x: number
    y: number
  }
  timestamp: number
}

/**
 * 定义在前端Store中使用的、经过结构化处理的车辆数据接口
 */
export interface VehicleTrackingData extends RawVehicleData {
  // 前端自己添加的状态，用于UI交互
  userStatus: 'pending' | 'approved' | 'rejected'
  // 预存的路径信息，用于在弹窗中显示
  signalizedJunctions: string[]
}

export const useEmergencyStore = defineStore('emergency', () => {
  // 存储所有车辆的实时数据, key为vehicleID
  const vehicleDataMap = ref<Record<string, VehicleTrackingData>>({})

  // 当前用户正在主动追踪的车辆ID
  const activelyTrackedVehicleId = ref<string | null>(null)

  // 计算属性：返回一个待处理的车辆列表（用户还未点击Approve或Reject）
  const pendingVehicles = computed(() => {
    const pending = Object.values(vehicleDataMap.value).filter(v => v.userStatus === 'pending')
    console.log(`🚨 [Emergency Store] 当前待处理车辆数量: ${pending.length}`, pending.map(v => ({
      vehicleID: v.vehicleID,
      position: v.position,
      organization: v.organization
    })))
    return pending
  })

  // 计算属性：返回当前正在追踪的车辆的详细信息
  const activelyTrackedVehicle = computed(() =>
    activelyTrackedVehicleId.value ? vehicleDataMap.value[activelyTrackedVehicleId.value] : null
  )

  let ws: WebSocket | null = null

  function connectWebSocket() {
    if (ws && ws.readyState === WebSocket.OPEN) {
      console.log("🔗 [Emergency Store] WebSocket 已连接，无需重复连接。");
      return;
    }

    // 请确保这里的URL和端口与您的Java后端匹配
    const wsUrl = 'ws://localhost:8085/ws/tracking';
    console.log(`🚀 [Emergency Store] 正在尝试连接到 WebSocket: ${wsUrl}`);
    ws = new WebSocket(wsUrl);

    ws.onopen = () => {
      console.log('✅ [Emergency Store] WebSocket 连接成功建立！');
    }

    ws.onmessage = (event) => {
      console.log('📥 [Emergency Store] 收到来自后端的原始消息:', event.data);
      
      try {
        const rawDataMap = JSON.parse(event.data)
        const newVehicleIds = Object.keys(rawDataMap)
        console.log(`🚗 [Emergency Store] 解析到 ${newVehicleIds.length} 辆车的数据，车辆ID:`, newVehicleIds);

        if (newVehicleIds.length === 0) {
          console.log("⚪ [Emergency Store] 没有车辆数据，清空现有数据");
          vehicleDataMap.value = {}
          return
        }

        // 更新或添加车辆数据
        newVehicleIds.forEach(vehicleId => {
          const rawInfo: RawVehicleData = JSON.parse(rawDataMap[vehicleId])
          console.log(`🔍 [Emergency Store] 车辆 ${vehicleId} 详细数据:`, rawInfo);

          if (!vehicleDataMap.value[vehicleId]) {
            // 这是新出现的车辆，设置初始状态
            console.log(`🆕 [Emergency Store] 新车辆 ${vehicleId} 首次出现`);
            vehicleDataMap.value[vehicleId] = {
              ...rawInfo,
              userStatus: 'pending',
              // 在真实应用中，这个路径列表应该在事件触发时从另一个API获取
              signalizedJunctions: ['Junction A', 'Junction B', 'Junction C', 'Junction D']
            }
          } else {
            // 更新已有车辆数据
            console.log(`🔄 [Emergency Store] 更新车辆 ${vehicleId} 位置: x=${rawInfo.position.x}, y=${rawInfo.position.y}`);
            Object.assign(vehicleDataMap.value[vehicleId], rawInfo)
          }
        })

        // 移除已从Redis消失的车辆
        for (const existingId in vehicleDataMap.value) {
          if (!newVehicleIds.includes(existingId)) {
            console.log(`🗑️ [Emergency Store] 移除已消失的车辆: ${existingId}`);
            delete vehicleDataMap.value[existingId]
            if (activelyTrackedVehicleId.value === existingId) {
              activelyTrackedVehicleId.value = null
            }
          }
        }

        console.log(`📊 [Emergency Store] 当前存储的所有车辆数据:`, {
          总车辆数: Object.keys(vehicleDataMap.value).length,
          车辆列表: Object.keys(vehicleDataMap.value),
          待处理数量: pendingVehicles.value.length
        });

      } catch (error) {
        console.error('❌ [Emergency Store] 解析WebSocket消息失败:', error);
      }
    }

    ws.onclose = (event) => {
      console.warn(`⚠️ [Emergency Store] WebSocket连接已关闭 (code: ${event.code}, reason: ${event.reason})，将在5秒后尝试重连。`);
      ws = null
      setTimeout(connectWebSocket, 5000);
    }

    ws.onerror = (error) => {
      console.error('💥 [Emergency Store] WebSocket发生错误:', error);
      ws?.close();
    }
  }

  function approveVehicle(vehicleId: string) {
    console.log(`✅ [Emergency Store] 批准车辆: ${vehicleId}`);
    if (vehicleDataMap.value[vehicleId]) {
      vehicleDataMap.value[vehicleId].userStatus = 'approved'
      activelyTrackedVehicleId.value = vehicleId
    }
  }

  async function rejectVehicle(vehicleId: string) {
    console.log(`❌ [Emergency Store] 拒绝车辆: ${vehicleId}`);
    if (vehicleDataMap.value[vehicleId]) {
      const eventId = vehicleDataMap.value[vehicleId].eventID
      vehicleDataMap.value[vehicleId].userStatus = 'rejected'
      try {
        await axios.post(`/api/emergency-vehicles/${eventId}/ignore`)
        console.log(`[Emergency Store] 已拒绝事件 ${eventId}`)
      } catch (error) {
        console.error(`[Emergency Store] 拒绝事件 ${eventId} 失败`, error)
      }
    }
  }

  async function completeTracking() {
    if (activelyTrackedVehicle.value) {
      const eventId = activelyTrackedVehicle.value.eventID
      try {
        await axios.post(`/api/emergency-vehicles/${eventId}/complete`)
      } catch (error) {
        console.error(`[Emergency Store] 完成事件 ${eventId} 失败`, error)
      } finally {
        if(activelyTrackedVehicleId.value) {
          delete vehicleDataMap.value[activelyTrackedVehicleId.value]
        }
        activelyTrackedVehicleId.value = null
      }
    }
  }

  return {
    vehicleDataMap, // 导出原始数据供地图组件使用
    pendingVehicles,
    activelyTrackedVehicle,
    connectWebSocket,
    approveVehicle,
    rejectVehicle,
    completeTracking
  }
})
