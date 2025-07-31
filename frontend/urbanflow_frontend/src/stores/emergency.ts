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
      console.log('[Emergency Store] Junction mappings loaded:', nameMap)
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
      console.log('[Emergency Store] Emergency routes loaded:', routesMap)
    } catch (error) {
      console.error('[Emergency Store] Failed to load emergency routes:', error)
    }
  }

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
    // 初始化junction映射数据和紧急车辆路线数据
    initializeJunctionMappings()
    initializeEmergencyRoutes()
    
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
            
            // 从紧急车辆路线数据中获取signalized_junctions
            const routeData = emergencyRoutesMap.value[vehicleId]
            let junctionNames: string[] = []
            
            if (routeData && routeData.signalized_junctions) {
              // 将junction ID转换为名称
              junctionNames = routeData.signalized_junctions.map((jId: string) => 
                junctionIdToNameMap.value[jId] || jId
              )
              console.log(`📍 [Emergency Store] 车辆 ${vehicleId} 路线信号灯路口:`, junctionNames)
            } else {
              console.warn(`⚠️ [Emergency Store] 车辆 ${vehicleId} 没有找到路线数据`)
            }
            
            vehicleDataMap.value[vehicleId] = {
              ...rawInfo,
              userStatus: 'pending',
              signalizedJunctions: junctionNames
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
