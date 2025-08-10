import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import axios from 'axios'
import { StorageManager } from '@/utils/persistence'

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
  signalizedJunctions?: string[]
  position: {
    x: number
    y: number
  }
  timestamp: number
}

export interface VehicleTrackingData extends RawVehicleData {
  userStatus: 'pending' | 'approved' | 'rejected'
  signalizedJunctions: string[]
}

export const useEmergencyStore = defineStore('emergency', () => {
  const vehicleDataMap = ref<Record<string, VehicleTrackingData>>({})
  const activelyTrackedVehicleId = ref<string | null>(null)

  const junctionIdToNameMap = ref<Record<string, string>>({})

  const emergencyRoutesMap = ref<Record<string, any>>({})

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

  const pendingVehicles = computed(() => {
    const pending = Object.values(vehicleDataMap.value).filter(v => v.userStatus === 'pending')
    return pending
  })

  const activelyTrackedVehicle = computed(() =>
    activelyTrackedVehicleId.value ? vehicleDataMap.value[activelyTrackedVehicleId.value] : null
  )

  let ws: WebSocket | null = null

  function connectWebSocket() {
    initializeJunctionMappings()
    initializeEmergencyRoutes()

    if (ws && ws.readyState === WebSocket.OPEN) {
      return;
    }

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

        newVehicleIds.forEach(async vehicleId => {
          const rawInfo: RawVehicleData = JSON.parse(rawDataMap[vehicleId])

          if (!vehicleDataMap.value[vehicleId]) {
            try {
              const staticDataResponse = await axios.get(`/api/emergency-vehicles/${rawInfo.eventID}`)
              const staticData = staticDataResponse.data
              const routeData = emergencyRoutesMap.value[vehicleId]
              let junctionNames: string[] = []

              if (staticData.signalized_junctions && staticData.signalized_junctions.length > 0) {
                junctionNames = staticData.signalized_junctions.map((jId: string) =>
                  junctionIdToNameMap.value[jId] || jId
                )
              } else if (routeData && routeData.signalized_junctions) {
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
              console.error(error)

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
                organization: 'Emergency Services',
                signalizedJunctions: junctionNames
              }
            }
          } else {
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
        console.error( error);
      }
    }

    ws.onclose = (event) => {

      ws = null
      setTimeout(connectWebSocket, 5000);
    }

    ws.onerror = (error) => {
      console.error(error);
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
      } catch (error) {
        console.error(`[Emergency Store] ${eventId} `, error)
      }
    }
  }


  function forceCleanAllData() {

    vehicleDataMap.value = {}
    activelyTrackedVehicleId.value = null

    try {
      StorageManager.save('emergency_vehicles', {})
      StorageManager.save('active_tracked_vehicle', null)
      localStorage.removeItem('emergency_vehicles')
      localStorage.removeItem('active_tracked_vehicle')
    } catch (error) {
      console.error(error)
    }


  }

  async function completeTracking() {


    if (activelyTrackedVehicle.value) {
      const eventId = activelyTrackedVehicle.value.eventID
      const vehicleId = activelyTrackedVehicleId.value

      console.log(`[Emergency Store] 准备完成追踪: eventId=${eventId}, vehicleId=${vehicleId}`)

      try {
        await axios.post(`/api/emergency-vehicles/${eventId}/complete`)
        console.log(`[Emergency Store]  ${eventId}`)
      } catch (error) {
        console.error(`[Emergency Store]${eventId} `, error)
      }
    }

    const currentVehicleId = activelyTrackedVehicleId.value

    if (currentVehicleId && vehicleDataMap.value[currentVehicleId]) {
      delete vehicleDataMap.value[currentVehicleId]
    }

    activelyTrackedVehicleId.value = null

  }

  return {
    vehicleDataMap,
    pendingVehicles,
    activelyTrackedVehicle,
    junctionIdToNameMap,
    emergencyRoutesMap,
    hasActiveSession: computed(() => activelyTrackedVehicleId.value !== null),
    connectWebSocket,
    approveVehicle,
    rejectVehicle,
    completeTracking,
    forceCleanAllData,
    initializeJunctionMappings,
    initializeEmergencyRoutes
  }
})
