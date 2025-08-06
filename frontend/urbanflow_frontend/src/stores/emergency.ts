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
  signalizedJunctions?: string[] // 预定路线中的交叉口ID列表
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

  // 存储junction映射数据
  const junctionIdToNameMap = ref<Record<string, string>>({})

  // 存储紧急车辆路线数据
  const emergencyRoutesMap = ref<Record<string, any>>({})

  // 初始化junction映射数据
  const initializeJunctionMappings = async () => {
    try {
      const response = await axios.get('/api-status/junctions')
      const junctionData = Object.values(response.data) as any[]
      const nameMap: Record<string, string> = {}
      junctionData.forEach((j: any) => {
        nameMap[j.junction_id] = j.junction_name || j.junction_id
      })
      junctionIdToNameMap.value = nameMap
    } catch (error) {
      console.error('[Emergency Store] Failed to load junction mappings:', error)
    }
  }

  // 初始化紧急车辆路线数据
  const initializeEmergencyRoutes = async () => {
    try {
      const response = await axios.get('/api-status/emergency-routes')
      const routesData = response.data as any[]
      const routesMap: Record<string, any> = {}
      routesData.forEach((route: any) => {
        routesMap[route.vehicle_id] = route
      })
      emergencyRoutesMap.value = routesMap
    } catch (error) {
      console.error('[Emergency Store] Failed to load emergency routes:', error)
    }
  }

  // 计算属性：返回一个待处理的车辆列表（用户还未点击Approve或Reject）
  const pendingVehicles = computed(() => {
    const pending = Object.values(vehicleDataMap.value).filter(v => v.userStatus === 'pending')
    return pending
  })

  // 计算属性：返回当前正在追踪的车辆的详细信息
  const activelyTrackedVehicle = computed(() =>
    activelyTrackedVehicleId.value ? vehicleDataMap.value[activelyTrackedVehicleId.value] : null
  )

  let ws: WebSocket | null = null

  function connectWebSocket() {
    // 初始化junction映射数据和紧急车辆路线数据
    initializeJunctionMappings()
    initializeEmergencyRoutes()

    if (ws && ws.readyState === WebSocket.OPEN) {
      return;
    }

    // 请确保这里的URL和端口与您的Java后端匹配
    const wsUrl = 'ws://localhost:8085/ws/tracking';
    ws = new WebSocket(wsUrl);

    ws.onopen = () => {
    }

    ws.onmessage = (event) => {

      try {
        const rawDataMap = JSON.parse(event.data)
        const newVehicleIds = Object.keys(rawDataMap)

        if (newVehicleIds.length === 0) {
          vehicleDataMap.value = {}
          return
        }

        // 更新或添加车辆数据
        newVehicleIds.forEach(async vehicleId => {
          const rawInfo: RawVehicleData = JSON.parse(rawDataMap[vehicleId])

          if (!vehicleDataMap.value[vehicleId]) {
            // 新车辆，从 special-event-handling 模块获取静态数据
            try {
              const staticDataResponse = await axios.get(`/api/emergency-vehicles/${rawInfo.eventID}`)
              const staticData = staticDataResponse.data

              // 从紧急车辆路线数据中获取signalized_junctions（保持向后兼容）
              const routeData = emergencyRoutesMap.value[vehicleId]
              let junctionNames: string[] = []

              if (staticData.signalized_junctions && staticData.signalized_junctions.length > 0) {
                // 使用从API获取的路口ID，转换为名称
                junctionNames = staticData.signalized_junctions.map((jId: string) =>
                  junctionIdToNameMap.value[jId] || jId
                )
              } else if (routeData && routeData.signalized_junctions) {
                // 备用方案：使用本地路线数据
                junctionNames = routeData.signalized_junctions.map((jId: string) =>
                  junctionIdToNameMap.value[jId] || jId
                )
              }

              vehicleDataMap.value[vehicleId] = {
                ...rawInfo,
                userStatus: 'pending',
                organization: staticData.organization || 'Emergency Services',
                signalizedJunctions: junctionNames
              }
            } catch (error) {
              console.error(`⚠️ [Emergency Store] 获取车辆 ${vehicleId} 静态数据失败:`, error)
              
              // 失败时使用备用方案
              const routeData = emergencyRoutesMap.value[vehicleId]
              let junctionNames: string[] = []

              if (routeData && routeData.signalized_junctions) {
                junctionNames = routeData.signalized_junctions.map((jId: string) =>
                  junctionIdToNameMap.value[jId] || jId
                )
              }

              vehicleDataMap.value[vehicleId] = {
                ...rawInfo,
                userStatus: 'pending',
                organization: 'Emergency Services', // 默认值
                signalizedJunctions: junctionNames
              }
            }
          } else {
            // 现有车辆，只更新动态数据
            Object.assign(vehicleDataMap.value[vehicleId], rawInfo)
          }
        })

        for (const existingId in vehicleDataMap.value) {
          if (!newVehicleIds.includes(existingId)) {
            delete vehicleDataMap.value[existingId]
            if (activelyTrackedVehicleId.value === existingId) {
              activelyTrackedVehicleId.value = null
            }
          }
        }



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
    junctionIdToNameMap, // 导出交叉口映射数据
    emergencyRoutesMap, // 导出紧急车辆路线数据
    hasActiveSession: computed(() => activelyTrackedVehicleId.value !== null), // 新增：计算属性来检查是否有活跃会话
    connectWebSocket,
    approveVehicle,
    rejectVehicle,
    completeTracking,
    initializeJunctionMappings,
    initializeEmergencyRoutes
  }
})
