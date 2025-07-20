import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios' // 确保您项目中有一个配置好的axios实例

/**
 * 定义从后端WebSocket接收到的原始、实时的车辆追踪数据结构
 * 这个接口现在精确匹配Redis中的ev_data_packet
 */
interface RawVehicleData {
  eventID: string
  vehicleID: string
  currentEdgeID: string
  currentLaneID: string
  upcomingJunctionID: string | null
  nextEdgeID: string | null
  nextLaneID: string | null
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
 * 定义在前端Store中使用的、经过合并处理的完整车辆数据接口
 */
// 定义用于 localStorage 的键名
const ACTIVE_VEHICLE_ID_KEY = 'active_emergency_vehicle_id';
const ACTIVE_SESSION_KEY = 'active_emergency_session';
interface ActiveSession {
  vehicleId: string;
  eventId: string;
}

export interface VehicleTrackingData extends RawVehicleData {
  // 从API一次性获取的静态数据
  organization: string
  signalizedJunctions: string[]
  // 前端自己添加的、用于UI交互的状态
  userStatus: 'pending' | 'approved' | 'rejected'
}

export const useEmergencyStore = defineStore('emergency', () => {
  // 存储所有车辆的完整数据, key为vehicleID
  const vehicleDataMap = ref<Record<string, VehicleTrackingData>>({})

  // 当前用户正在主动追踪的车辆ID
  // 从 localStorage 初始化 activelyTrackedVehicleId
  // const activelyTrackedVehicleId = ref<string | null>(localStorage.getItem(ACTIVE_VEHICLE_ID_KEY));
  const activelyTrackedVehicleId = ref<string | null>(null);
  const activeEventId = ref<string | null>(null);

  // 在Store初始化时，尝试从localStorage恢复会话
  const savedSession = localStorage.getItem(ACTIVE_SESSION_KEY);
  if (savedSession) {
    const session: ActiveSession = JSON.parse(savedSession);
    activelyTrackedVehicleId.value = session.vehicleId;
    activeEventId.value = session.eventId;
  }
  // 计算属性：返回一个待处理的车辆列表
  const pendingVehicles = computed(() =>
    Object.values(vehicleDataMap.value).filter(v => v.userStatus === 'pending')
  )

  // 计算属性：返回当前正在追踪的车辆的详细信息
  const activelyTrackedVehicle = computed(() =>
    activelyTrackedVehicleId.value ? vehicleDataMap.value[activelyTrackedVehicleId.value] : null
  )

  // ### 新增：计算属性，用于判断是否存在正在进行的追踪会话 ###
  const hasActiveSession = computed(() => activelyTrackedVehicle.value !== null);

  let ws: WebSocket | null = null

  function connectWebSocket() {
    if (ws && ws.readyState === WebSocket.OPEN) return;

    const wsUrl = 'ws://localhost:8085/ws/tracking';
    console.log(`1. [Store] 正在尝试连接到 WebSocket: ${wsUrl}`);
    ws = new WebSocket(wsUrl);

    ws.onopen = () => {
      console.log('2. [Store] WebSocket 连接成功建立！');
    }

    ws.onmessage = async (event) => { // 将此方法标记为 async
      console.log('3. [Store] 收到来自后端的消息:', event.data);
      const rawDataMap: Record<string, string> = JSON.parse(event.data);
      const newVehicleIds = Object.keys(rawDataMap);
      console.log(`4. [Store] 解析到 ${newVehicleIds.length} 辆车的数据。`);

      for (const vehicleId of newVehicleIds) {
        const rawInfo: RawVehicleData = JSON.parse(rawDataMap[vehicleId]);

        if (!vehicleDataMap.value[vehicleId]) {
          // ### 关键修改：这是新出现的车辆，通过API获取其静态详情 ###
          try {
            console.log(`5. [Store] 发现新车辆 ${vehicleId} (事件ID: ${rawInfo.eventID})，正在获取其静态信息...`);
            const apiUrl = `/api/emergency-vehicles/${rawInfo.eventID}`;
            console.log(`6. [Store] 正在向后端API发送请求: GET ${apiUrl}`);
            const response = await axios.get(apiUrl);
            const staticData = response.data;
            console.log("7. [Store] 成功从API获取到静态数据:", staticData);
            const initialStatus = vehicleId === activelyTrackedVehicleId.value ? 'approved' : 'pending';
            // ### 关键修改：合并实时数据和静态数据 ###
            vehicleDataMap.value[vehicleId] = {
              ...rawInfo, // 来自WebSocket的实时数据
              organization: staticData.organization, // 来自API的静态数据
              signalizedJunctions: staticData.signalized_junctions || [], // 来自API的静态数据
              userStatus: initialStatus // 使用计算出的初始状态
            };
            console.log(`8. [Store] 成功获取并合并了车辆 ${vehicleId} 的数据。`);
          } catch (error) {
            console.error(`[Store] 获取事件 ${rawInfo.eventID} 的静态信息失败`, error);
          }
        } else {
          // 更新已有车辆的实时数据
          Object.assign(vehicleDataMap.value[vehicleId], rawInfo);
        }
      }

      // 移除已从Redis消失的车辆
      for (const existingId in vehicleDataMap.value) {
        if (!newVehicleIds.includes(existingId)) {
          delete vehicleDataMap.value[existingId];
          if (activelyTrackedVehicleId.value === existingId) {
            activelyTrackedVehicleId.value = null;
          }
        }
      }
    }

    ws.onclose = () => {
      console.warn('⚠️ [Store] 追踪数据WebSocket连接已关闭，将在5秒后尝试重连。');
      ws = null;
      setTimeout(connectWebSocket, 5000);
    }

    ws.onerror = (error) => {
      console.error('WebSocket 发生错误:', error);
      ws?.close();
    }
  }

  function approveVehicle(vehicleId: string) {
    if (vehicleDataMap.value[vehicleId]) {
      vehicleDataMap.value[vehicleId].userStatus = 'approved';
      activelyTrackedVehicleId.value = vehicleId;
      localStorage.setItem(ACTIVE_VEHICLE_ID_KEY, vehicleId);
    }
  }

  async function rejectVehicle(vehicleId: string) {
    if (vehicleDataMap.value[vehicleId]) {
      const eventId = vehicleDataMap.value[vehicleId].eventID;
      vehicleDataMap.value[vehicleId].userStatus = 'rejected';
      try {
        await axios.post(`/api/emergency-vehicles/${eventId}/ignore`);
        console.log(`[Store] 已拒绝事件 ${eventId}`);
      } catch (error) {
        console.error(`[Store] 拒绝事件 ${eventId} 失败`, error);
      }
    }
  }

  /**
   * ### 修改 ###
   * 追踪结束后的状态清理方法。
   * 它会调用后端API，然后彻底清理本地和localStorage的状态。
   */
  async function completeTracking() {
    if (activelyTrackedVehicle.value) {
      const eventId = activelyTrackedVehicle.value.eventID
      try {
        // 1. 通知后端事件已完成
        await axios.post(`/api/emergency-vehicles/${eventId}/complete`)
        console.log(`[Store] 已通知后端完成事件 ${eventId} 的追踪`);
      } catch (error) {
        console.error(`[Store] 通知后端完成事件 ${eventId} 失败`, error)
      } finally {
        // 2. 无论API调用是否成功，都清理前端状态
        localStorage.removeItem(ACTIVE_VEHICLE_ID_KEY);
        if(activelyTrackedVehicleId.value) {
          delete vehicleDataMap.value[activelyTrackedVehicleId.value]
        }
        activelyTrackedVehicleId.value = null
      }
    }
  }

  async function ensureActiveVehicleData() {
    if (activelyTrackedVehicleId.value && !activelyTrackedVehicle.value) {
      try {
        const response = await axios.get(`/api/emergency-vehicles/${activeEventId.value}`);
        const staticData = response.data;

        vehicleDataMap.value[activelyTrackedVehicleId.value] = {
          eventID: activeEventId.value!,
          vehicleID: activelyTrackedVehicleId.value,
          organization: staticData.organization,
          signalizedJunctions: staticData.signalizedJunctions || [],
          userStatus: 'approved', // 恢复时状态总是 'approved'
          currentEdgeID: '',
          position: { x: 0, y: 0 },
        } as VehicleTrackingData;
      } catch (error) {
        console.error(`[Store] 恢复车辆 ${activelyTrackedVehicleId.value} 的数据失败`, error);
        completeTracking();
      }
    }
  }

  return {
    pendingVehicles,
    activelyTrackedVehicle,
    hasActiveSession,
    connectWebSocket,
    approveVehicle,
    rejectVehicle,
    completeTracking,
    ensureActiveVehicleData
  };
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

