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
  const pendingVehicles = computed(() =>
    Object.values(vehicleDataMap.value).filter(v => v.userStatus === 'pending')
  )

  // 计算属性：返回当前正在追踪的车辆的详细信息
  const activelyTrackedVehicle = computed(() =>
    activelyTrackedVehicleId.value ? vehicleDataMap.value[activelyTrackedVehicleId.value] : null
  )

  let ws: WebSocket | null = null

  function connectWebSocket() {
    if (ws && ws.readyState === WebSocket.OPEN) {
      console.log("[Store] WebSocket 已连接，无需重复连接。");
      return;
    }

    // 请确保这里的URL和端口与您的Java后端匹配
    const wsUrl = 'ws://localhost:8085/ws/tracking';
    console.log(`1. [Store] 正在尝试连接到 WebSocket: ${wsUrl}`);
    ws = new WebSocket(wsUrl);

    ws.onopen = () => {
      console.log('2. [Store] WebSocket 连接成功建立！');
    }

    ws.onmessage = (event) => {
      console.log('3. [Store] 收到来自后端的消息:', event.data);
      const rawDataMap = JSON.parse(event.data)
      const newVehicleIds = Object.keys(rawDataMap)
      console.log(`4. [Store] 解析到 ${newVehicleIds.length} 辆车的数据。`);

      // 更新或添加车辆数据
      newVehicleIds.forEach(vehicleId => {
        const rawInfo: RawVehicleData = JSON.parse(rawDataMap[vehicleId])

        if (!vehicleDataMap.value[vehicleId]) {
          // 这是新出现的车辆，设置初始状态
          vehicleDataMap.value[vehicleId] = {
            ...rawInfo,
            userStatus: 'pending',
            // 在真实应用中，这个路径列表应该在事件触发时从另一个API获取
            signalizedJunctions: ['Junction A', 'Junction B', 'Junction C', 'Junction D']
          }
        } else {
          // 更新已有车辆数据
          Object.assign(vehicleDataMap.value[vehicleId], rawInfo)
        }
      })

      // 移除已从Redis消失的车辆
      for (const existingId in vehicleDataMap.value) {
        if (!newVehicleIds.includes(existingId)) {
          delete vehicleDataMap.value[existingId]
          if (activelyTrackedVehicleId.value === existingId) {
            activelyTrackedVehicleId.value = null
          }
        }
      }
    }

    ws.onclose = () => {
      console.warn('⚠️ [Emergency Store] 追踪数据WebSocket连接已关闭，将在5秒后尝试重连。')
      ws = null
      setTimeout(connectWebSocket, 5000);
    }

    ws.onerror = (error) => {
      console.error('WebSocket 发生错误:', error);
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
    pendingVehicles,
    activelyTrackedVehicle,
    connectWebSocket,
    approveVehicle,
    rejectVehicle,
    completeTracking
  }
})



// import { defineStore } from 'pinia'
// import { ref, computed } from 'vue'
//
// /**
//  * 定义在前端Store中使用的、经过结构化处理的车辆数据接口
//  * 在模拟阶段，我们只需要部分关键字段
//  */
// export interface VehicleTrackingData {
//   eventID: string
//   vehicleID: string
//   organization: string
//   userStatus: 'pending' | 'approved' | 'rejected'
//   signalizedJunctions: string[]
// }
//
// /**
//  * 用于紧急车辆事件状态管理的 Pinia Store
//  * (模拟数据版本)
//  */
// export const useEmergencyStore = defineStore('emergency', () => {
//   // --- 模拟数据 ---
//   // 使用 ref 来创建响应式状态，模拟从WebSocket接收到的实时数据。
//   // 初始状态下，我们有两条待处理的紧急事件。
//   const vehicleDataMap = ref<Record<string, VehicleTrackingData>>({
//     'EV001': {
//       eventID: 'emergency_event_1',
//       vehicleID: 'EV001',
//       organization: 'Dublin Emergency Services',
//       userStatus: 'pending',
//       signalizedJunctions: ['Junction A (START)', 'Junction B', 'Junction C (DESTINATION)']
//     },
//     'EV002': {
//       eventID: 'emergency_event_2',
//       vehicleID: 'EV002',
//       organization: 'National Ambulance Service',
//       userStatus: 'pending',
//       signalizedJunctions: ['Junction X (START)', 'Junction Y', 'Junction Z (DESTINATION)']
//     }
//   });
//   // --- 模拟结束 ---
//
//   // 存储当前用户正在主动追踪的车辆ID
//   const activelyTrackedVehicleId = ref<string | null>(null);
//
//   // 计算属性：自动过滤出所有状态为 'pending' 的车辆
//   const pendingVehicles = computed(() =>
//     Object.values(vehicleDataMap.value).filter(v => v.userStatus === 'pending')
//   );
//
//   // 计算属性：根据 activelyTrackedVehicleId 返回当前正在追踪的车辆的详细信息
//   const activelyTrackedVehicle = computed(() =>
//     activelyTrackedVehicleId.value ? vehicleDataMap.value[activelyTrackedVehicleId.value] : null
//   );
//
//   /**
//    * 在独立测试阶段，我们注释掉真实的WebSocket连接函数。
//    * 当您进行前后端联调时，再将此函数恢复。
//    */
//   /*
//   function connectWebSocket() {
//     // ... 真实的WebSocket连接逻辑将放在这里 ...
//   }
//   */
//
//   /**
//    * 模拟用户点击 "APPROVE" 按钮的操作
//    * @param vehicleId 被批准的车辆ID
//    */
//   function approveVehicle(vehicleId: string) {
//     console.log(`MOCK: 批准车辆 ${vehicleId}`);
//     if (vehicleDataMap.value[vehicleId]) {
//       // 将车辆状态更新为 'approved'
//       vehicleDataMap.value[vehicleId].userStatus = 'approved';
//       // 设置为当前正在主动追踪的车辆
//       activelyTrackedVehicleId.value = vehicleId;
//     }
//   }
//
//   /**
//    * 模拟用户点击 "REJECT" 按钮的操作
//    * @param vehicleId 被拒绝的车辆ID
//    */
//   function rejectVehicle(vehicleId: string) {
//     console.log(`MOCK: 拒绝车辆 ${vehicleId}`);
//     if (vehicleDataMap.value[vehicleId]) {
//       // 将车辆状态更新为 'rejected'，它将从 pendingVehicles 列表中消失
//       vehicleDataMap.value[vehicleId].userStatus = 'rejected';
//     }
//   }
//
//   /**
//    * 模拟追踪完成后的清理操作
//    */
//   function completeTracking() {
//     console.log(`MOCK: 完成车辆 ${activelyTrackedVehicleId.value} 的追踪`);
//     if (activelyTrackedVehicleId.value && vehicleDataMap.value[activelyTrackedVehicleId.value]) {
//       // 从数据地图中移除已完成的车辆
//       delete vehicleDataMap.value[activelyTrackedVehicleId.value];
//     }
//     // 清空当前追踪ID
//     activelyTrackedVehicleId.value = null;
//   }
//
//   // 导出所有组件需要用到的状态和方法
//   return {
//     pendingVehicles,
//     activelyTrackedVehicle,
//     // connectWebSocket, // 在联调阶段再导出
//     approveVehicle,
//     rejectVehicle,
//     completeTracking
//   };
// });

