<template>
  <div class="map-show" :class="{ 'sidebar-open': isSidebarOpen }">
    <div class="toolbar">
      <div class="zoom-controls">
        <div class="iconfont zoom-btn-plus" @click="zoomIn">&#xeaf3;</div>
        <div class="iconfont zoom-btn-minus" @click="zoomOut">&#xeaf5;</div>
      </div>

      <div class="view-switch" v-if="authStore.isTrafficManager()">
        <label class="switch">
          <input
            type="checkbox"
            :checked="viewMode === 'full'"
            @change="handleSwitchChange"
          />
          <span class="slider"></span>
        </label>
      </div>

      <div class="search-section">
        <div class="search-wrapper">
          <input
            class="search-input"
            type="text"
            placeholder="Search junction name..."
            v-model="searchInput"
            @input="updateSearchSuggestions"
            @keydown="handleKeydown"
            @keydown.enter="searchJunction"
            @focus="updateSearchSuggestions"
            @blur="setTimeout(() => showSuggestions = false, 200)"
          />
          <div v-if="showSuggestions" class="search-suggestions">
            <div
              v-for="suggestion in filteredSuggestions"
              :key="suggestion.name"
              class="suggestion-item"
              :class="{ 'suggestion-uncontrollable': !isJunctionControllable(suggestion.name) }"
              @click="selectSuggestion(suggestion.name)"
            >
              {{ suggestion.name }}
              <span v-if="!isJunctionControllable(suggestion.name)" class="readonly-indicator">
                (Read-only)
              </span>
            </div>
          </div>
        </div>
        <div class="iconfont search-btn" @click="searchJunction">&#xeafe;</div>
      </div>
    </div>

    <div class="map-container" ref="mapRef"></div>

    <TrafficLightStatusBar
      v-if="showTrafficStatus"
      :junctionId="selectedJunctionForStatus?.junction_id"
      :junctionName="selectedJunctionForStatus?.junction_name"
      :directionIndex="selectedDirectionIndex"
      :trafficLightData="getTrafficLightDataForStatusBar()"
      :lastManualControl="lastManualControl"
      :style="getStatusBarPosition()"
      class="map-traffic-status"
    />

    <div class="footer-container">
      <div class="footer-content">
        <span class="iconfont footer-icon">&#xe60b;</span>
        <span class="footer-text">
          Current Location:
          <span class="footer-link">{{ currentLocation }}</span>
        </span>
      </div>
      <div class="area-info" v-if="authStore.isTrafficManager()">
        <span class="area-label">Area:</span>
        <span class="area-value">{{ viewModeDescription }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, defineExpose, computed, watch, nextTick } from 'vue'
import axios from 'axios'
import apiClient from '@/utils/api'
import 'ol/ol.css'
import OLMap from 'ol/Map'
import View from 'ol/View'
import VectorLayer from 'ol/layer/Vector'
import VectorSource from 'ol/source/Vector'
import GeoJSON from 'ol/format/GeoJSON'
import { Style, Stroke, Fill, Circle } from 'ol/style'
import { getCenter } from 'ol/extent'
import Overlay from 'ol/Overlay'
import OLView from 'ol/View'
import { Feature } from 'ol'
import { Point, LineString } from 'ol/geom'
import { useAreaPermissions, getAreaByCoordinates, isJunctionInArea } from '@/composables/useAreaPermissions'
import { useAuthStore } from '@/stores/auth'
import { useEmergencyStore } from '@/stores/emergency'
import TrafficLightStatusBar from '@/components/TrafficLightStatusBar.vue'
import TrafficLightIcon from '@/components/TrafficLightIcon.vue'
import EmergencyVehicleMarker from '@/components/EmergencyVehicleMarker.vue'
import { createApp } from 'vue'
import { emergencyVehicleApi, type EmergencyVehicleEvent } from '@/services/emergencyVehicleApi'
import { EmergencyVehicleTracker } from '@/services/specialEventApi'
defineProps<{ isSidebarOpen: boolean }>()

const emit = defineEmits<{
  (e: 'signalLightClicked', junctionId: string): void
  (e: 'trafficLightCleared'): void
}>()
const clearTrafficStatus = () => {
  selectedJunctionForStatus.value = null
  selectedDirectionIndex.value = null
  currentTrafficLightData.value = null
  lastManualControl.value = null
  rerenderTlsOverlays()
}
const selectedJunctionForStatus = ref<Junction | null>(null)
const selectedDirectionIndex = ref<number | null>(null)
const currentTrafficLightData = ref<any>(null)
const junctionIdToTlsIdMap = ref<Map<string, string>>(new Map())
const allTrafficLightData = ref<Map<string, any>>(new Map())
const lastManualControl = ref<{
  junctionName: string
  directionInfo: string
  lightColor: string
  duration: number
  appliedTime: Date
} | null>(null)
let ws: WebSocket | null = null
let emergencyTracker: EmergencyVehicleTracker | null = null
let reconnectAttempts = 0
const maxReconnectAttempts = 5
let reconnectTimer: NodeJS.Timeout | null = null

const getStatusBarPosition = (): Record<string, string> => {
  statusBarPositionKey.value

  if (!selectedJunctionForStatus.value || !map) {
    return { display: 'none' }
  }

  const currentView = map.getView()
  if (!currentView) {
    return { display: 'none' }
  }

  const junction = selectedJunctionForStatus.value
  const coordinate = [junction.junctionX, junction.junctionY]

  try {
    const extent = currentView.calculateExtent()
    const [minX, minY, maxX, maxY] = extent

    const bufferX = (maxX - minX) * 0.05
    const bufferY = (maxY - minY) * 0.05

    if (coordinate[0] < minX - bufferX || coordinate[0] > maxX + bufferX ||
        coordinate[1] < minY - bufferY || coordinate[1] > maxY + bufferY) {
      return { display: 'none' }
    }

    const pixel = map.getPixelFromCoordinate(coordinate)

    if (!pixel || !Array.isArray(pixel) || pixel.length < 2) {
      return { display: 'none' }
    }

    const mapContainer = mapRef.value
    if (!mapContainer) {
      return { display: 'none' }
    }

    const offsetX = 30
    const offsetY = 0

    let left = pixel[0] + offsetX
    const top = pixel[1] + offsetY

    const containerRect = mapContainer.getBoundingClientRect()
    const estimatedWidth = 200
    const estimatedHeight = 50

    if (left + estimatedWidth > containerRect.width - 10) {
      left = pixel[0] - estimatedWidth - offsetX
    }

    const centeredTop = top - estimatedHeight / 2
    let finalTop = centeredTop
    if (finalTop < 10) {
      finalTop = 10
    }
    if (finalTop + estimatedHeight > containerRect.height - 10) {
      finalTop = containerRect.height - estimatedHeight - 10
    }
    if (left < 10) {
      left = 10
    }
    if (left > containerRect.width - estimatedWidth - 10) {
      left = containerRect.width - estimatedWidth - 10
    }

    const position = {
      left: `${Math.max(10, left)}px`,
      top: `${finalTop}px`
    }

    console.log('📍 [StatusBar] Position calculation:', {
      iconPixel: pixel,
      offsetX,
      offsetY,
      calculatedLeft: left,
      calculatedTop: finalTop,
      containerSize: { width: containerRect.width, height: containerRect.height }
    })

    return position
  } catch (error) {
    console.error('😱 [StatusBar] Position calculation error:', error)
    return { display: 'none' }
  }
}

let statusBarUpdateTimer: NodeJS.Timeout | null = null
const isUpdatingPosition = false

const statusBarPositionKey = ref(0)

const updateStatusBarPosition = () => {
  if (isUpdatingPosition) return

  if (statusBarUpdateTimer) {
    clearTimeout(statusBarUpdateTimer)
    statusBarUpdateTimer = null
  }

  if (showTrafficStatus.value && selectedJunctionForStatus.value) {
    statusBarPositionKey.value++
  }
}

let mapEventListeners: Array<() => void> = []

const setupViewWatchers = () => {
  if (!map) return

  const currentView = map.getView()
  if (!currentView) return

  const updatePosition = () => {
    if (showTrafficStatus.value && selectedJunctionForStatus.value) {
      updateStatusBarPosition()
    }
    // 在视图变化时更新紧急车辆标记
    updateEmergencyVehicleMarkers()
  }

  const centerListener = updatePosition
  const resolutionListener = updatePosition
  const rotationListener = updatePosition

  currentView.on('change:center', centerListener)
  currentView.on('change:resolution', resolutionListener)
  currentView.on('change:rotation', rotationListener)

  const resizeObserver = new ResizeObserver(() => {
    setTimeout(updatePosition, 10)
  })

  if (mapRef.value) {
    resizeObserver.observe(mapRef.value)
  }

  mapEventListeners.push(
    () => currentView.un('change:center', centerListener),
    () => currentView.un('change:resolution', resolutionListener),
    () => currentView.un('change:rotation', rotationListener),
    () => resizeObserver.disconnect()
  )

  watch(() => {
    try {
      return currentView.getCenter()
    } catch {
      return null
    }
  }, updatePosition, { deep: true })

  watch(() => {
    try {
      return currentView.getZoom()
    } catch {
      return null
    }
  }, updatePosition)
}

const showTrafficStatus = computed(() => {
  const hasJunction = !!selectedJunctionForStatus.value
  const hasDirection = selectedDirectionIndex.value !== null && selectedDirectionIndex.value !== undefined
  const hasSelection = hasJunction && hasDirection

  return hasSelection
})


const getTrafficLightDataForStatusBar = () => {
  if (currentTrafficLightData.value) {
    return currentTrafficLightData.value
  }

  if (selectedJunctionForStatus.value) {
    const tlsId = junctionIdToTlsIdMap.value.get(selectedJunctionForStatus.value.junctionId)
    if (tlsId && allTrafficLightData.value.has(tlsId)) {
      const data = allTrafficLightData.value.get(tlsId)
      return data
    }
  }

  return null
}

const zoomToJunction = (junctionName: string) => {
  const junction = junctionMap.get(junctionName)
  if (junction && map) {
    const currentView = map.getView()
    if (currentView) {
      selectedJunctionName.value = null
      rerenderTlsOverlays()

      currentView.animate({
        center: [junction.junctionX, junction.junctionY],
        zoom: Math.max(currentView.getZoom() || 15, 16),
        duration: 1000,
        easing: (t: number) => t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2
      })

      selectedJunctionName.value = junctionName
      rerenderTlsOverlays()

      const controllableText = isJunctionControllable(junctionName) ? '' : ' (Read-only)'
      currentLocation.value = `${junctionName}${controllableText}`
    }
  }
}

const zoomToJunctionById = (junctionId: string) => {

  const junctionEntry = Array.from(junctionMap.entries()).find(([name, junction]) =>
    junction.junction_id === junctionId
  )

  if (junctionEntry && map) {
    const [junctionName, junction] = junctionEntry
    const currentView = map.getView()
    if (currentView) {
      selectedJunctionName.value = null
      rerenderTlsOverlays()

      currentView.animate({
        center: [junction.junctionX, junction.junctionY],
        zoom: Math.max(currentView.getZoom() || 15, 16),
        duration: 1000,
        easing: (t: number) => t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2
      })

      selectedJunctionName.value = junctionName
      rerenderTlsOverlays()

      const controllableText = isJunctionControllable(junctionName) ? '' : ' (Read-only)'
      currentLocation.value = `${junctionName}${controllableText}`
    }
  }
}

const handleManualControlApplied = (data: { junctionName: string, directionInfo: string, lightColor: string, duration: number }) => {
  console.log('🎯 [Map] Manual control applied:', data)
  lastManualControl.value = {
    junctionName: data.junctionName,
    directionInfo: data.directionInfo,
    lightColor: data.lightColor,
    duration: data.duration,
    appliedTime: new Date()
  }

  console.log('📅 [Map] Manual control recorded for status bar:', lastManualControl.value)
  if (!selectedJunctionForStatus.value || selectedJunctionForStatus.value.junction_name !== data.junctionName) {
    const junction = Array.from(junctionMap.values()).find(j =>
      j.junction_name === data.junctionName || j.junction_id === data.junctionName
    )
    if (junction) {
      selectedJunctionForStatus.value = junction
      console.log('🎯 [Map] Set junction for status bar:', junction)
    }
  }
  const statusText = `${data.junctionName} - ${data.lightColor} light applied for ${data.duration}s`
  currentLocation.value = statusText

  setTimeout(() => {
    const controllableText = isJunctionControllable(data.junctionName) ? '' : ' (Read-only)'
    currentLocation.value = `${data.junctionName}${controllableText}`
  }, 3000)
}

const setSelectedTrafficLight = (junctionId: string, directionIndex: number) => {
  console.log('🔍 [Map] setSelectedTrafficLight called:', { junctionId, directionIndex })
  console.log('🔍 [Map] Current state:', {
    selectedJunctionForStatus: selectedJunctionForStatus.value?.junction_id,
    selectedDirectionIndex: selectedDirectionIndex.value
  })

  const junction = Array.from(junctionMap.values()).find(j => j.junction_id === junctionId)
  const junctionName = Array.from(junctionMap.entries()).find(([name, j]) => j.junction_id === junctionId)?.[0]

  if (junction && junctionName) {
    const isSameJunction = selectedJunctionForStatus.value?.junction_id === junction.junction_id
    const isSameDirection = selectedDirectionIndex.value === directionIndex
    const isJunctionAlreadySelected = selectedJunctionName.value === junctionName
    const shouldAnimate = !isSameJunction && !isJunctionAlreadySelected

    if (!isSameJunction || !isSameDirection) {
      console.log('🧹 [Map] Clearing manual control record - junction or direction changed')
      lastManualControl.value = null
    }

    selectedJunctionForStatus.value = junction
    selectedDirectionIndex.value = directionIndex

    console.log('✅ [Map] Status bar state set:', {
      junctionId: selectedJunctionForStatus.value.junction_id,
      junctionName: selectedJunctionForStatus.value.junction_name,
      directionIndex: selectedDirectionIndex.value,
      shouldShow: showTrafficStatus.value
    })

    if (!isSameJunction) {
      selectedJunctionName.value = null
      rerenderTlsOverlays()
    }

    lastUpdateTime = 0
    currentTrafficState = ''
    lastNextSwitchTime = -1

    if (shouldAnimate) {
      console.log('🎥 [Map] Executing zoom animation for new junction')
      const currentView = map?.getView()
      if (currentView) {
        currentView.animate({
          center: [junction.junctionX, junction.junctionY],
          zoom: Math.max(currentView.getZoom() || 15, 16),
          duration: 1000,
          easing: (t: number) => t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2
        })
      }
    }


    selectedJunctionName.value = junctionName
    rerenderTlsOverlays()


    fetchTrafficLightData(junction.junctionId)


    nextTick(() => {
      updateStatusBarPosition()
    })
  } else {
    console.warn('⚠️ [Map] Junction not found for ID:', junctionId)
  }
}

const setSelectedJunctionOnly = (junctionId: string) => {
  console.log('🔍 [Map] setSelectedJunctionOnly called:', { junctionId })

  const junction = Array.from(junctionMap.values()).find(j => j.junction_id === junctionId)
  const junctionName = Array.from(junctionMap.entries()).find(([name, j]) => j.junction_id === junctionId)?.[0]

  if (junction && junctionName) {
    selectedJunctionForStatus.value = junction
    selectedDirectionIndex.value = null

    lastManualControl.value = null

    selectedJunctionName.value = junctionName

    console.log(' [Map] Junction-only selection set:', {
      junctionId: selectedJunctionForStatus.value.junction_id,
      junctionName: selectedJunctionForStatus.value.junction_name,
      directionIndex: selectedDirectionIndex.value,
      shouldShowPartialSelection: true
    })


    rerenderTlsOverlays()


    fetchTrafficLightData(junction.junctionId)
  } else {
    console.warn('⚠️ [Map] Junction not found for ID:', junctionId)
  }
}


let lastUpdateTime = 0
let currentTrafficState = ''
let lastNextSwitchTime = -1

const connectWebSocket = () => {
  try {
    // 连接status-sync的WebSocket (用于交通灯和边缘数据)
    const wsProtocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${wsProtocol}//localhost:8087/api/status/ws`

    ws = new WebSocket(wsUrl)

    ws.onopen = () => {
      // WebSocket连接成功
      reconnectAttempts = 0
    }

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)

        if (data.trafficLights) {
          for (const tlsId in data.trafficLights) {
            const tlsData = data.trafficLights[tlsId]

            let parsedData
            if (typeof tlsData === 'string') {
              try {
                parsedData = JSON.parse(tlsData)
              } catch (parseError) {
                continue
              }
            } else {
              parsedData = tlsData
            }

            if (parsedData && typeof parsedData.state === 'string') {
              allTrafficLightData.value.set(tlsId, parsedData)
            }
          }

          if (selectedJunctionForStatus.value) {
            const junctionId = selectedJunctionForStatus.value.junctionId
            const tlsId = junctionIdToTlsIdMap.value.get(junctionId)

            if (tlsId && data.trafficLights[tlsId]) {
              const tlsData = data.trafficLights[tlsId]

              let parsedData
              if (typeof tlsData === 'string') {
                try {
                  parsedData = JSON.parse(tlsData)
                } catch (parseError) {
                  return
                }
              } else {
                parsedData = tlsData
              }

              if (parsedData &&
                  typeof parsedData.state === 'string' &&
                  typeof parsedData.nextSwitchTime === 'number') {

                const stateChanged = currentTrafficState !== parsedData.state
                const nextSwitchTimeChanged = lastNextSwitchTime !== parsedData.nextSwitchTime
                const isFirstTime = lastUpdateTime === 0

                if (isFirstTime || stateChanged || nextSwitchTimeChanged) {
                  currentTrafficLightData.value = parsedData
                  lastUpdateTime = Date.now()
                  currentTrafficState = parsedData.state
                  lastNextSwitchTime = parsedData.nextSwitchTime
                }
              }
            }
          }

          rerenderTlsOverlays()
        }

        if (data.edges) {
          const newMap: Record<string, number> = {}
          for (const edgeId in data.edges) {
            const edgeStr = data.edges[edgeId]
            try {
              const parsed = typeof edgeStr === 'string' ? JSON.parse(edgeStr) : edgeStr
              const count = parsed.vehicleCount ?? 0
              newMap[edgeId] = count
            } catch (error) {
            }
          }
          vehicleCountMap.value = newMap
          vectorLayer?.changed()
        }

        // 不再处理紧急车辆数据，这些数据来自special-event模块
      } catch (error) {
      }
    }

    ws.onerror = (error) => {
      // WebSocket连接错误
    }

    ws.onclose = () => {
      // WebSocket连接断开
      if (reconnectAttempts < maxReconnectAttempts) {
        reconnectAttempts++
        reconnectTimer = setTimeout(connectWebSocket, 3000)
      }
    }
  } catch (error) {
    // WebSocket创建失败
    if (reconnectAttempts < maxReconnectAttempts) {
      reconnectAttempts++
      reconnectTimer = setTimeout(connectWebSocket, 3000)
    }
  }
}

// 连接紧急车辆追踪WebSocket (这个现在由emergency store处理)
const connectEmergencyTracker = () => {
  console.log('🔗 [Map] Emergency tracker现在由emergency store管理，无需单独连接')
  // emergencyTracker = new EmergencyVehicleTracker()
  // 数据通过emergency store的watch机制自动更新到地图
}

const fetchTrafficLightData = async (junctionId: string) => {
  try {
    const tlsResponse = await axios.get('/api-status/tls-junctions')

    const tlsJunction = tlsResponse.data.find((tls: any) => {
      return tls.junctionId === junctionId ||
             tls.junction_id === junctionId ||
             tls.tlsId === junctionId
    })

    if (!tlsJunction) {
      return
    }

    const tlsId = tlsJunction.tlsId

    junctionIdToTlsIdMap.value.set(junctionId, tlsId)

    const junctionResponse = await axios.get('/api-status/junctions')

    const junctionData = junctionResponse.data[tlsId]
    if (junctionData) {
      let parsedData = junctionData
      if (typeof junctionData === 'string') {
        try {
          parsedData = JSON.parse(junctionData)
        } catch (parseError) {
          return
        }
      }

      currentTrafficLightData.value = parsedData
      allTrafficLightData.value.set(tlsId, parsedData)
    }
  } catch (error) {

  }
}

const isJunctionInManagedArea = (junctionX: number, junctionY: number): boolean => {
  if (!authStore.isTrafficManager()) return true

  const managedAreas = getUserManagedAreas()
  if (managedAreas.length === 0) return false

  const isLeftArea = junctionX < mapCenterX

  if (isLeftArea && managedAreas.includes('Left')) return true
  if (!isLeftArea && managedAreas.includes('Right')) return true

  return false
}

const getJunctionArea = (junctionName: string): string => {
  const junction = junctionMap.get(junctionName)
  if (!junction) return 'Unknown'
  const isLeftArea = junction.junctionX < mapCenterX
  return isLeftArea ? 'Left' : 'Right'
}

const isJunctionControllable = (junctionName: string): boolean => {
  if (authStore.isAdmin()) return true

  const junction = junctionMap.get(junctionName)
  if (!junction) return false

  const managedAreas = getUserManagedAreas()
  if (managedAreas.length === 0) return false

  const isLeftArea = junction.junctionX < mapCenterX

  if (isLeftArea && managedAreas.includes('Left')) return true
  if (!isLeftArea && managedAreas.includes('Right')) return true

  return false
}

const viewModeDescription = computed(() => {
  const managedAreas = getUserManagedAreas()
  switch (viewMode.value) {
    case 'restricted':
      return `${managedAreas.join(', ')} Area`
    case 'full':
      return 'Global Map'
    default:
      return 'Unknown Mode'
  }
})

let allCoordinates: number[][] = []
const authStore = useAuthStore()
const emergencyStore = useEmergencyStore()

const viewMode = ref<'restricted' | 'full'>('restricted')

const userManagedAreas = ref<string[]>([])

watch(viewMode, async (newMode) => {
  if (vectorLayer) {
    vectorLayer.changed()
  }

  await setMapViewForMode(newMode)

  rerenderTlsOverlays()
})

watch(userManagedAreas, (newAreas) => {
  if (vectorLayer) {
    vectorLayer.changed()
  }

  rerenderTlsOverlays()

  if (authStore.isTrafficManager()) {
    const managedAreas = getUserManagedAreas()
    if (managedAreas.length === 1 && viewMode.value === 'full') {
      viewMode.value = 'restricted'
    }
  }
}, { deep: true })

interface LaneMapping {
  laneId: string
  laneShape: string
  edgeId: string
  edgeName: string
}

interface Junction {
  tlsId: string
  junctionId: string
  junctionX: number
  junctionY: number
  junction_id: string
  junction_name: string
  areaName?: string
}

interface TlsJunctionInfo {
  tlsId: string
  junctionId: string
  junctionName?: string
  junctionX: number
  junctionY: number
  junctionShape?: string
}

const mapRef = ref<HTMLElement | null>(null)
let map: OLMap | null = null
let view: OLView | null = null
let vectorLayer: VectorLayer | null = null
let emergencyRouteLayer: VectorLayer | null = null
let hasFitted = false

const currentLocation = ref('Click on a lane or signal light')
const highlightLanes = ref<{ fromLanes: string[]; toLanes: string[] } | null>(null)
const searchInput = ref('')
const junctionMap = new Map<string, Junction>()
const selectedJunctionName = ref<string | null>(null)
const searchSuggestions = ref<{name: string, area?: string}[]>([])
const showSuggestions = ref(false)

const filteredSuggestions = computed(() => {
  return searchSuggestions.value.filter(suggestion => {
    if (authStore.isAdmin()) return true
    if (viewMode.value === 'full') return true
    return isJunctionControllable(suggestion.name)
  })
})

const markerOverlays: Overlay[] = []
const tlsOverlays: Overlay[] = []
const emergencyVehicleOverlays: Overlay[] = []  // 新增：紧急车辆覆盖层数组

const vehicleCountMap = ref<Record<string, number>>({})
const laneToEdgeMap = new Map<string, string>()
const emergencyRoutes = ref<EmergencyVehicleEvent[]>([])
const emergencyVehicles = ref<Record<string, any>>({})
const highlightedUpcomingJunctions = ref<Set<string>>(new Set())  // 新增：高亮的即将到达路口

// ✅ 监听emergency store的车辆数据变化
watch(() => emergencyStore.vehicleDataMap, (newVehicles, oldVehicles) => {
  const newVehicleIds = Object.keys(newVehicles)
  const oldVehicleIds = oldVehicles ? Object.keys(oldVehicles) : []

  console.log('🔄 [Map] Emergency store数据变化检测')
  console.log('  新车辆数量:', newVehicleIds.length, '车辆ID:', newVehicleIds)
  console.log('  旧车辆数量:', oldVehicleIds.length, '车辆ID:', oldVehicleIds)

  // 检查是否有变化
  const hasChanges = newVehicleIds.length !== oldVehicleIds.length ||
    !newVehicleIds.every(id => oldVehicleIds.includes(id))

  if (hasChanges) {
    console.log('🆕 [Map] 车辆数据发生变化，更新地图显示')
    updateEmergencyVehicleMarkers()  // 更新地图标记
    updateHighlightedJunctions()     // 更新高亮路口
    updateEmergencyRoutesDisplay()   // 更新路线显示
  } else {
    console.log('📍 [Map] 车辆位置可能更新，刷新标记')
    updateEmergencyVehicleMarkers()  // 即使车辆数量没变，位置可能更新了
  }
}, { deep: true })

const getUserManagedAreas = (): string[] => {
  if (!authStore.isTrafficManager()) return []
  return userManagedAreas.value
}

const fetchUserAreaPermissions = async () => {
  try {
    const user = JSON.parse(localStorage.getItem('user') || '{}');

    console.log('🔍 [ControlMap] User from localStorage:', user);

    if (user.managedAreas && Array.isArray(user.managedAreas)) {
      userManagedAreas.value = user.managedAreas;
      console.log(' [ControlMap] Using cached managed areas:', userManagedAreas.value);

      if (authStore.user) {
        authStore.user.managedAreas = userManagedAreas.value;
      }

      return;
    }

    if (user.role === 'Traffic Manager') {
      userManagedAreas.value = [];
      console.log('[ControlMap] Traffic Manager found but no managed areas in localStorage');
    } else {
      userManagedAreas.value = [];
      console.log('ℹ[ControlMap] User role does not require area management:', user.role);
    }

  } catch (error) {
    console.error(' [ControlMap] Failed to get user from localStorage:', error);
    userManagedAreas.value = [];
  }
}

let mapCenterX = 0

const isLaneInManagedArea = (coordinates: number[][]): boolean => {
  if (!authStore.isTrafficManager()) return true

  const managedAreas = getUserManagedAreas()
  if (managedAreas.length === 0) return false

  const centerX = coordinates.reduce((sum, coord) => sum + coord[0], 0) / coordinates.length

  const isLeftArea = centerX < mapCenterX

  if (isLeftArea && managedAreas.includes('Left')) return true
  if (!isLeftArea && managedAreas.includes('Right')) return true

  return false
}

const getAreaBounds = (area: string): number[] | null => {
  if (!allCoordinates.length) return null

  const areaCoordinates = allCoordinates.filter(coord => {
    const isLeft = coord[0] < mapCenterX
    return (area === 'Left' && isLeft) || (area === 'Right' && !isLeft)
  })

  if (!areaCoordinates.length) return null

  let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity
  areaCoordinates.forEach(coord => {
    minX = Math.min(minX, coord[0])
    maxX = Math.max(maxX, coord[0])
    minY = Math.min(minY, coord[1])
    maxY = Math.max(maxY, coord[1])
  })

  const padding = Math.max((maxX - minX) * 0.1, (maxY - minY) * 0.1, 100)
  return [minX - padding, minY - padding, maxX + padding, maxY + padding]
}

const getManagedAreasBounds = (): number[] | null => {
  const managedAreas = getUserManagedAreas()
  if (managedAreas.length === 0) return null

  let globalMinX = Infinity, globalMaxX = -Infinity
  let globalMinY = Infinity, globalMaxY = -Infinity
  let hasValidBounds = false

  managedAreas.forEach(area => {
    const bounds = getAreaBounds(area)
    if (bounds) {
      globalMinX = Math.min(globalMinX, bounds[0])
      globalMinY = Math.min(globalMinY, bounds[1])
      globalMaxX = Math.max(globalMaxX, bounds[2])
      globalMaxY = Math.max(globalMaxY, bounds[3])
      hasValidBounds = true
    }
  })

  return hasValidBounds ? [globalMinX, globalMinY, globalMaxX, globalMaxY] : null
}

const setViewMode = async (mode: 'restricted' | 'full') => {
  viewMode.value = mode
}

const handleSwitchChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const mode = target.checked ? 'full' : 'restricted'
  setViewMode(mode)
}

const setMapViewForMode = async (mode: 'restricted' | 'full') => {
  if (!authStore.isTrafficManager()) return

  try {
    if (mode === 'restricted') {
      const bounds = getManagedAreasBounds()
      if (bounds) {
        const restrictedExtent = [
          bounds[0] - 50,
          bounds[1] - 50,
          bounds[2] + 50,
          bounds[3] + 50
        ]

        const restrictedView = new OLView({
          center: [(bounds[0] + bounds[2]) / 2, (bounds[1] + bounds[3]) / 2],
          zoom: 15,
          minZoom: 14,
          maxZoom: 18,
          extent: restrictedExtent
        })

        map?.setView(restrictedView)
        view = restrictedView

        setTimeout(() => {
          view?.fit(bounds, {
            padding: [100, 100, 100, 100],
            duration: mode === viewMode.value ? 0 : 1500,
            maxZoom: 16
          })
        }, 100)
      }
    } else {
      const extent = vectorLayer?.getSource()?.getExtent()
      if (extent) {
        const fullView = new OLView({
          center: getCenter(extent),
          zoom: 15,
          minZoom: 13,
          maxZoom: 18,
          extent: [extent[0] - 100, extent[1] - 100, extent[2] + 100, extent[3] + 100]
        })

        map?.setView(fullView)
        view = fullView

        view.fit(extent, {
          padding: [50, 50, 50, 50],
          duration: 1500,
          easing: (t: number) => t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2
        })
      }
    }
  } catch (error) {
  }
}

const setMapViewForManagedAreas = (allCoordinates: number[][]) => {
  if (!authStore.isTrafficManager() || !view) return

  const managedAreas = getUserManagedAreas()
  if (managedAreas.length === 0) return

  if (managedAreas.length === 1) {
    const area = managedAreas[0]
    const bounds = getAreaBounds(area)
    if (bounds) {
      view.fit(bounds, {
        padding: [50, 50, 50, 50],
        duration: 1000,
        maxZoom: 16
      })
    }
  } else {
    const extent = vectorLayer?.getSource()?.getExtent()
    if (extent) {
      view.fit(extent, { padding: [20, 20, 20, 20], duration: 1000 })
    }
  }
}


const getCurrentTrafficLight = (junctionId: string): string => {

  if (selectedJunctionForStatus.value?.junction_id === junctionId &&
      selectedDirectionIndex.value !== null) {

    if (currentTrafficLightData.value) {
      const state = currentTrafficLightData.value.state
      if (typeof state === 'string' && selectedDirectionIndex.value < state.length) {
        const char = state[selectedDirectionIndex.value]
        const lowerChar = char.toLowerCase()

        if (lowerChar === 'g') return 'green'
        if (lowerChar === 'y' || lowerChar === 'o') return 'yellow'
        if (lowerChar === 'r') return 'red'


        console.error('🚨 UNKNOWN CHARACTER FOUND:', {
          junctionId,
          directionIndex: selectedDirectionIndex.value,
          unknownChar: `"${char}"`,
          charCode: char.charCodeAt(0),
          fullState: `"${state}"`
        })
        return ''
      }
    }

    const tlsId = junctionIdToTlsIdMap.value.get(junctionId)
    if (tlsId && allTrafficLightData.value.has(tlsId)) {
      const data = allTrafficLightData.value.get(tlsId)
      if (data && data.state && typeof data.state === 'string' &&
          selectedDirectionIndex.value < data.state.length) {
        const char = data.state[selectedDirectionIndex.value]
        const lowerChar = char.toLowerCase()

        if (lowerChar === 'g') return 'green'
        if (lowerChar === 'y' || lowerChar === 'o') return 'yellow'
        if (lowerChar === 'r') return 'red'


        console.error(' UNKNOWN CHARACTER FOUND (cache):', {
          junctionId,
          directionIndex: selectedDirectionIndex.value,
          unknownChar: `"${char}"`,
          charCode: char.charCodeAt(0),
          fullState: `"${data.state}"`
        })
        return ''
      }
    }
  }

  return ''
}

const isJunctionSelected = (junctionId: string): boolean => {
  const hasJunctionSelected = selectedJunctionForStatus.value?.junction_id === junctionId
  const hasDirectionSelected = selectedDirectionIndex.value !== null && selectedDirectionIndex.value !== undefined

  return hasJunctionSelected && hasDirectionSelected
}

const shouldShowAllLights = (junctionId: string): boolean => {
  return false
}

const getTlsStyle = (junctionName: string): { color: string, isControllable: boolean } => {
  const isControllable = isJunctionControllable(junctionName)
  const isSelected = selectedJunctionName.value === junctionName

  // 检查是否是紧急车辆即将到达的路口
  const junction = junctionMap.get(junctionName)
  const isEmergencyUpcoming = junction && highlightedUpcomingJunctions.value.has(junction.junction_id)

  if (isEmergencyUpcoming) {
    return {
      color: '#FF6B00',  // 紧急警告色（橙红色）
      isControllable: true
    }
  }

  if (isSelected) {
    return {
      color: '#FFD700',
      isControllable: true
    }
  }

  if (authStore.isAdmin()) {
    return {
      color: '#00FFAA',
      isControllable: true
    }
  }

  if (isControllable) {
    return {
      color: '#00E5FF',
      isControllable: true
    }
  } else {
    return {
      color: '#666666',
      isControllable: false
    }
  }
}

const updateAllTlsStyles = () => {
  rerenderTlsOverlays()
}

const updateTlsStyles = updateAllTlsStyles

const handleTrafficLightClick = async (junctionName: string, junction: Junction) => {
  selectedJunctionName.value = junctionName

  selectedJunctionForStatus.value = junction

  selectedDirectionIndex.value = null


  await fetchTrafficLightData(junction.junctionId)

  rerenderTlsOverlays()

  const currentView = map?.getView()
  if (currentView) {
    currentView.animate({
      center: [junction.junctionX, junction.junctionY],
      zoom: 18,
      duration: 1200,
      easing: (t: number) => t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2
    })
  }

  emit('signalLightClicked', junction.junction_id)

  const controllableText = isJunctionControllable(junctionName) ? '' : ' (Read-only)'
  currentLocation.value = `${junctionName}${controllableText}`
}

const rerenderTlsOverlays = () => {
  tlsOverlays.forEach(overlay => {
    map?.removeOverlay(overlay)
  })
  tlsOverlays.length = 0

  Array.from(junctionMap.entries()).forEach(([junctionName, junction]) => {
    if (authStore.isTrafficManager() && viewMode.value === 'restricted') {
      const isInArea = isJunctionInManagedArea(junction.junctionX, junction.junctionY)
      if (!isInArea) {
        return
      }
    }

    const isControllable = isJunctionControllable(junctionName)
    const junctionId = junction.junction_id

    // 检查是否是紧急车辆即将到达的路口
    const isEmergencyUpcoming = highlightedUpcomingJunctions.value.has(junctionId)


    const isFullySelected = isJunctionSelected(junctionId)
    const isJunctionOnly = selectedJunctionForStatus.value?.junction_id === junctionId &&
                          (selectedDirectionIndex.value === null || selectedDirectionIndex.value === undefined)

    const currentLight = getCurrentTrafficLight(junctionId)
    const showAllLights = shouldShowAllLights(junctionId)

    const containerEl = document.createElement('div')
    containerEl.style.position = 'relative'
    containerEl.style.cursor = 'pointer'
    containerEl.dataset['name'] = junctionName
    containerEl.dataset['junctionId'] = junctionId

    const app = createApp(TrafficLightIcon, {
      currentLight: currentLight,
      isSelected: isFullySelected,
      isPartiallySelected: isJunctionOnly,
      isControllable: isControllable,
      showAllLights: showAllLights,
      isEmergencyUpcoming: isEmergencyUpcoming
    })

    app.mount(containerEl)

    containerEl.addEventListener('click', (e) => {
      e.stopPropagation()
      handleTrafficLightClick(junctionName, junction)
    })

    const overlay = new Overlay({
      element: containerEl,
      positioning: 'center-center',
      stopEvent: false,
      offset: [0, 0],
      position: [junction.junctionX, junction.junctionY]
    })

    map?.addOverlay(overlay)
    tlsOverlays.push(overlay)
  })
}

// 更新高亮的即将到达路口
const updateHighlightedJunctions = () => {
  const newHighlightedJunctions = new Set<string>()

  // 从emergency store获取车辆数据
  const vehicleData = emergencyStore.vehicleDataMap
  console.log('🔆 [Map] 检查即将到达的路口，车辆数据:', vehicleData)

  // 遍历所有紧急车辆，收集即将到达的路口
  Object.values(vehicleData).forEach((vehicleInfo: any) => {
    if (vehicleInfo.upcomingJunctionID && vehicleInfo.upcomingJunctionID.trim() !== '') {
      newHighlightedJunctions.add(vehicleInfo.upcomingJunctionID)
      console.log('🚨 [Map] 紧急车辆即将到达路口:', vehicleInfo.upcomingJunctionID, '车辆:', vehicleInfo.vehicleID)
    }
  })

  console.log('📊 [Map] 信号灯闪烁逻辑说明:')
  console.log('  1. 从emergency store获取车辆数据：', Object.keys(vehicleData).length, '辆车')
  console.log('  2. 检查每辆车的upcomingJunctionID字段')
  console.log('  3. 如果有值，将该路口ID添加到高亮列表')
  console.log('  4. 高亮路口的交通灯会显示橙红色脉冲+闪烁效果')
  console.log('  5. 当前即将到达的路口:', Array.from(newHighlightedJunctions))

  // 只有当高亮路口发生变化时才重新渲染
  const currentHighlighted = Array.from(highlightedUpcomingJunctions.value).sort()
  const newHighlighted = Array.from(newHighlightedJunctions).sort()

  if (JSON.stringify(currentHighlighted) !== JSON.stringify(newHighlighted)) {
    highlightedUpcomingJunctions.value = newHighlightedJunctions
    console.log('🔆 [Map] 更新高亮路口:', Array.from(newHighlightedJunctions))
    // 重新渲染交通灯覆盖层以显示高亮效果
    rerenderTlsOverlays()
  } else {
    console.log('📍 [Map] 高亮路口无变化，跳过重新渲染')
  }
}

// 更新紧急车辆标记
const updateEmergencyVehicleMarkers = () => {
  console.log('🔄 [Map] 开始更新紧急车辆标记...')
  
  // 清除之前的标记
  emergencyVehicleOverlays.forEach(overlay => {
    map?.removeOverlay(overlay)
  })
  emergencyVehicleOverlays.length = 0
  console.log('🧹 [Map] 已清除之前的车辆标记')

  // 从emergency store获取车辆数据
  const vehicleData = emergencyStore.vehicleDataMap
  console.log('🚗 [Map] 从emergency store获取到车辆数据:', vehicleData)
  
  if (!vehicleData || Object.keys(vehicleData).length === 0) {
    console.log('⚪ [Map] 没有车辆数据需要显示')
    return
  }

  // 为每个紧急车辆创建标记
  Object.entries(vehicleData).forEach(([vehicleId, vehicleInfo]) => {
    console.log(`🔍 [Map] 处理车辆 ${vehicleId}:`, vehicleInfo)
    if (vehicleInfo.position && vehicleInfo.position.x && vehicleInfo.position.y) {
      const coordinate = [vehicleInfo.position.x, vehicleInfo.position.y]
      console.log(`📍 [Map] 车辆 ${vehicleId} 位置:`, coordinate)

      // 检查车辆是否在当前视图范围内
      if (map) {
        const currentView = map.getView()
        if (currentView) {
          const extent = currentView.calculateExtent()
          const [minX, minY, maxX, maxY] = extent

          // 如果车辆不在视图范围内，跳过
          if (coordinate[0] < minX || coordinate[0] > maxX ||
              coordinate[1] < minY || coordinate[1] > maxY) {
            return
          }
        }
      }

      // 创建车辆标记容器
      const vehicleContainer = document.createElement('div')
      vehicleContainer.style.position = 'relative'
      vehicleContainer.style.pointerEvents = 'auto'
      vehicleContainer.dataset['vehicleId'] = vehicleId

      // 使用 Vue 组件创建紧急车辆标记
      const vehicleApp = createApp(EmergencyVehicleMarker, {
        vehicleData: vehicleInfo,
        mapPixelPosition: [0, 0], // 这个会被 CSS 定位覆盖
        showInfo: false,
        onVehicleClick: (data: any) => {
          handleEmergencyVehicleClick(data)
        }
      })

      vehicleApp.mount(vehicleContainer)
      console.log(`✅ [Map] 已创建车辆 ${vehicleId} 的标记组件`)

      // 添加点击事件
      vehicleContainer.addEventListener('click', (e) => {
        e.stopPropagation()
        handleEmergencyVehicleClick(vehicleInfo)
      })

      // 创建覆盖层
      const overlay = new Overlay({
        element: vehicleContainer,
        positioning: 'center-center',
        stopEvent: false,
        offset: [0, 0],
        position: coordinate
      })

      map?.addOverlay(overlay)
      emergencyVehicleOverlays.push(overlay)
      console.log(`🎯 [Map] 车辆 ${vehicleId} 标记已添加到地图`)
    } else {
      console.log(`⚠️ [Map] 车辆 ${vehicleId} 缺少位置信息:`, vehicleInfo.position)
    }
  })
  
  console.log(`✨ [Map] 紧急车辆标记更新完成，共显示 ${emergencyVehicleOverlays.length} 个标记`)
}

// 处理紧急车辆点击事件
const handleEmergencyVehicleClick = (vehicleData: any) => {
  console.log('🚑 紧急车辆被点击:', vehicleData)

  // 更新位置显示，包含即将到达的路口信息
  let locationText = `Emergency Vehicle: ${vehicleData.vehicleID} (${vehicleData.organization})`
  if (vehicleData.upcomingJunctionID && vehicleData.upcomingJunctionID.trim() !== '') {
    // 查找路口名称
    const upcomingJunctionName = Array.from(junctionMap.entries())
      .find(([name, junction]) => junction.junction_id === vehicleData.upcomingJunctionID)?.[0]

    if (upcomingJunctionName) {
      locationText += ` → Next: ${upcomingJunctionName}`
    } else {
      locationText += ` → Next: ${vehicleData.upcomingJunctionID}`
    }

    if (vehicleData.upcomingTlsCountdown > 0) {
      locationText += ` (${vehicleData.upcomingTlsCountdown.toFixed(1)}s)`
    }
  }

  currentLocation.value = locationText

  // 可以在这里添加更多交互逻辑，比如显示详细信息、追踪路线等
  // 例如：缩放到车辆位置
  if (map && vehicleData.position) {
    const currentView = map.getView()
    if (currentView) {
      currentView.animate({
        center: [vehicleData.position.x, vehicleData.position.y],
        zoom: Math.max(currentView.getZoom() || 15, 16),
        duration: 1000
      })
    }
  }
}

const clearAllMarkers = () => {
  for (const overlay of markerOverlays) {
    map?.removeOverlay(overlay)
  }
  markerOverlays.length = 0

  // 清除紧急车辆标记
  for (const overlay of emergencyVehicleOverlays) {
    map?.removeOverlay(overlay)
  }
  emergencyVehicleOverlays.length = 0
}

const searchJunction = () => {
  const name = searchInput.value.trim()
  const junction = junctionMap.get(name)
  if (junction && map) {
    const currentView = map.getView()
    if (currentView) {
      selectedJunctionName.value = name

      selectedJunctionForStatus.value = junction

      selectedDirectionIndex.value = null

      fetchTrafficLightData(junction.junctionId)

      rerenderTlsOverlays()

      currentView.animate({
        center: [junction.junctionX, junction.junctionY],
        zoom: 18,
        duration: 1200,
        easing: (t: number) => t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2
      })

      clearAllMarkers()

      const marker = document.createElement('div')
      marker.className = 'iconfont search-marker'
      marker.innerHTML = '&#xe76f;'

      const overlay = new Overlay({
        element: marker,
        positioning: 'center-center',
        stopEvent: false,
        offset: [0, 0],
        position: [junction.junctionX, junction.junctionY]
      })

      map?.addOverlay(overlay)
      markerOverlays.push(overlay)

      const controllableText = isJunctionControllable(name) ? '' : ' (Read-only)'
      currentLocation.value = `${name}${controllableText}`

      emit('signalLightClicked', junction.junction_id)
    }
  } else {
    currentLocation.value = 'Junction not found'
  }
}

const updateSearchSuggestions = () => {
  const query = searchInput.value.trim().toLowerCase()
  if (query.length === 0) {
    searchSuggestions.value = []
    showSuggestions.value = false
    return
  }

  const allJunctions = Array.from(junctionMap.keys())
  const filtered = allJunctions
    .filter(name => name.toLowerCase().includes(query))
    .map(name => {
      const junctionArea = getJunctionArea(name)
      return {
        name,
        area: junctionArea
      }
    })
    .slice(0, 8)

  searchSuggestions.value = filtered
  showSuggestions.value = filtered.length > 0
}

const selectSuggestion = (suggestion: string) => {
  searchInput.value = suggestion
  showSuggestions.value = false
  searchJunction()
}

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    showSuggestions.value = false
  }
}

// 加载紧急车辆路线数据（从status-sync模块获取预定路线）
const loadEmergencyRoutes = async () => {
  try {
    console.log('📋 [Map] 开始加载预定紧急车辆路线...')
    
    // ✅ 从status-sync模块获取真实的预定路线数据
    const response = await emergencyVehicleApi.getEmergencyRoutes()
    if (response.data && response.data.length > 0) {
      emergencyRoutes.value = response.data
      console.log('📋 [Map] 成功加载预定路线数据:', emergencyRoutes.value)
      
      // ✅ 显示所有预定路线（不管是否有活跃车辆）
      const allVehicleIds = response.data.map(r => r.vehicle_id)
      console.log('🛣️ [Map] 显示预定路线，车辆ID:', allVehicleIds)
      await drawEmergencyRoutes(allVehicleIds, true) // true表示显示预定路线
    } else {
      console.log('⚪ [Map] 没有找到预定路线数据')
      // 清空路线显示
      clearEmergencyRoutes()
    }

  } catch (error) {
    console.error('❌ [Map] 加载预定路线失败:', error)
    // 如果API失败，清空路线显示
    clearEmergencyRoutes()
  }
}

// 绘制紧急车辆路线 - 支持预定路线和活跃车辆路线
const drawEmergencyRoutes = async (vehicleIds: string[] = [], isPlannedRoute: boolean = false) => {
  if (!emergencyRouteLayer || emergencyRoutes.value.length === 0) {
    console.log('⚠️ [Map] 缺少路线图层或路线数据')
    return
  }

  const source = emergencyRouteLayer.getSource()
  if (!source) return

  // 清除之前的数据
  source.clear()
  
  let routesToDraw: any[] = []
  
  if (isPlannedRoute) {
    // ✅ 预定路线模式：显示所有指定的路线（不管是否有活跃车辆）
    routesToDraw = emergencyRoutes.value.filter(event => vehicleIds.includes(event.vehicle_id))
    console.log('📋 [Map] 预定路线模式，显示', routesToDraw.length, '条路线')
  } else {
    // 活跃车辆模式：只显示有活跃车辆的路线
    routesToDraw = emergencyRoutes.value.filter(event =>
      vehicleIds.includes(event.vehicle_id) || event.event_status === 'triggered'
    )
    console.log('🎯 [Map] 活跃车辆模式，显示', routesToDraw.length, '条活跃路线')
  }

  if (routesToDraw.length === 0) {
    console.log('🚫 [Map] 没有需要显示的路线')
    return
  }

  console.log('🛣️ [Map] 开始绘制', routesToDraw.length, '条路线:', routesToDraw.map(r => r.vehicle_id))

  // 获取lane-mappings数据来获取坐标
  const laneMappingsResponse = await emergencyVehicleApi.getEdgeCoordinates([])
  const laneMappings = laneMappingsResponse.data

  // 创建edgeId到坐标的映射
  const edgeToCoordinatesMap = new Map<string, [number, number][]>()

  laneMappings.forEach((lane: any) => {
    const edgeId = lane.edgeId
    const coordinates = lane.laneShape.trim().split(' ').map((p: string) => p.split(',').map(Number))

    if (!edgeToCoordinatesMap.has(edgeId)) {
      edgeToCoordinatesMap.set(edgeId, coordinates)
    }
  })

  // 只为指定的紧急车辆事件绘制路线
  routesToDraw.forEach((event, index) => {
    console.log(`📋 [Map] 绘制路线 ${index + 1}/${routesToDraw.length}: ${event.vehicle_id}`)
    drawSingleEmergencyRoute(event, edgeToCoordinatesMap, isPlannedRoute)
  })
}

// 绘制单个紧急车辆路线
const drawSingleEmergencyRoute = (event: EmergencyVehicleEvent, edgeToCoordinatesMap: Map<string, [number, number][]>, isPlannedRoute: boolean = false) => {
  if (!emergencyRouteLayer) {
    console.warn('⚠️ [Map] Emergency route layer not initialized')
    return
  }

  const source = emergencyRouteLayer.getSource()
  if (!source) {
    console.warn('⚠️ [Map] Emergency route layer source not available')
    return
  }

  console.log(`🛣️ [Map] Drawing route for vehicle ${event.vehicle_id}:`, {
    eventId: event.event_id,
    vehicleType: event.vehicle_type,
    organization: event.organization,
    status: event.event_status,
    routeEdges: event.route_edges.length,
    isPlannedRoute
  })

  // 构建完整路线坐标
  const routeCoordinates: [number, number][] = []

  event.route_edges.forEach((edgeId) => {
    const coordinates = edgeToCoordinatesMap.get(edgeId)
    if (coordinates && coordinates.length > 0) {
      routeCoordinates.push(...coordinates)
    } else {
      console.warn(`⚠️ [Map] No coordinates found for edge: ${edgeId}`)
    }
  })

  if (routeCoordinates.length === 0) {
    console.warn(`⚠️ [Map] No valid coordinates for route ${event.vehicle_id}`)
    return
  }

  // ✅ 检查是否是活跃车辆（从emergency store获取）
  const isActiveVehicle = Object.keys(emergencyStore.vehicleDataMap).includes(event.vehicle_id)
  
  console.log(`🎯 [Map] Vehicle ${event.vehicle_id} active status:`, {
    isActive: isActiveVehicle,
    isPlanned: isPlannedRoute,
    status: event.event_status
  })

  // 创建主路线Feature
  const routeFeature = new Feature({
    geometry: new LineString(routeCoordinates),
    eventId: event.event_id,
    vehicleId: event.vehicle_id,
    vehicleType: event.vehicle_type,
    organization: event.organization,
    status: event.event_status,
    routeType: 'main',
    isActive: isActiveVehicle,  // ✅ 活跃状态标记
    isPlanned: isPlannedRoute   // ✅ 预定路线标记
  })

  // 创建起点标记
  const startCoordinates = edgeToCoordinatesMap.get(event.start_edge_id)
  if (startCoordinates && startCoordinates.length > 0) {
    const startFeature = new Feature({
      geometry: new Point(startCoordinates[0]),
      eventId: event.event_id,
      vehicleId: event.vehicle_id,
      vehicleType: event.vehicle_type,
      routeType: 'start',
      isActive: isActiveVehicle,
      isPlanned: isPlannedRoute
    })
    source.addFeature(startFeature)
  }

  // 创建终点标记
  const endCoordinates = edgeToCoordinatesMap.get(event.end_edge_id)
  if (endCoordinates && endCoordinates.length > 0) {
    const endFeature = new Feature({
      geometry: new Point(endCoordinates[endCoordinates.length - 1]),
      eventId: event.event_id,
      vehicleId: event.vehicle_id,
      vehicleType: event.vehicle_type,
      routeType: 'end',
      isActive: isActiveVehicle,
      isPlanned: isPlannedRoute
    })
    source.addFeature(endFeature)
  }

  // 添加主路线Feature
  source.addFeature(routeFeature)
  console.log(`✅ [Map] Successfully added route feature for vehicle ${event.vehicle_id}`)
}

// 创建紧急车辆路线样式 - 区分活跃和非活跃状态
const createEmergencyRouteStyle = (feature: Feature) => {
  const routeType = feature.get('routeType')
  const vehicleType = feature.get('vehicleType')
  const status = feature.get('status')
  const isActive = feature.get('isActive')  // ✅ 获取活跃状态

  if (routeType === 'start') {
    // 起点标记 - 绿色（活跃时更亮）
    return new Style({
      image: new Circle({
        radius: isActive ? 10 : 6,
        fill: new Fill({ color: isActive ? '#22C55E' : '#6B7280' }),
        stroke: new Stroke({ color: '#FFFFFF', width: 2 })
      })
    })
  } else if (routeType === 'end') {
    // 终点标记 - 红色（活跃时更亮）
    return new Style({
      image: new Circle({
        radius: isActive ? 10 : 6,
        fill: new Fill({ color: isActive ? '#EF4444' : '#6B7280' }),
        stroke: new Stroke({ color: '#FFFFFF', width: 2 })
      })
    })
  } else {
    // 路线样式 - 根据活跃状态和事件状态确定样式
    const getRouteStyle = (status: string, isActive: boolean) => {
      if (!isActive) {
        // 非活跃路线：灰色虚线，半透明
        return new Style({
          stroke: new Stroke({
            color: 'rgba(107, 114, 128, 0.4)',  // 灰色半透明
            width: 2,
            lineDash: [10, 10],  // 虚线
            opacity: 0.5
          })
        })
      }

      // 活跃路线：根据状态显示不同颜色
      switch (status) {
        case 'pending':
          return new Style({
            stroke: new Stroke({
              color: '#FFA500',    // 橙色 - 待处理
              width: 4,
              lineDash: [8, 4]     // 虚线表示待处理
            })
          })
        case 'triggered':
          return new Style({
            stroke: new Stroke({
              color: '#FF4757',    // 红色 - 已触发（实线）
              width: 5
            })
          })
        case 'completed':
          return new Style({
            stroke: new Stroke({
              color: '#10B981',    // 绿色 - 已完成
              width: 4
            })
          })
        default:
          return new Style({
            stroke: new Stroke({
              color: '#6B7280',    // 灰色 - 其他
              width: 3,
              lineDash: [5, 5]
            })
          })
      }
    }

    return getRouteStyle(status, isActive)
  }
}

// 初始化紧急车辆路线图层
const initEmergencyRouteLayer = () => {
  const emergencyRouteSource = new VectorSource()
  emergencyRouteLayer = new VectorLayer({
    source: emergencyRouteSource,
    style: createEmergencyRouteStyle,
    zIndex: 400
  })

  map?.addLayer(emergencyRouteLayer)
}

const loadLaneData = async () => {
  const res = await axios.get('/api-status/lane-mappings')
  const data = res.data as LaneMapping[]

  allCoordinates = []
  let minX = Infinity, maxX = -Infinity

  data.forEach(lane => {
    const coordinates = lane.laneShape.trim().split(' ').map(p => p.split(',').map(Number))
    allCoordinates.push(...coordinates)
    coordinates.forEach(coord => {
      minX = Math.min(minX, coord[0])
      maxX = Math.max(maxX, coord[0])
    })
  })

  mapCenterX = (minX + maxX) / 2

  const features = data.map((lane: LaneMapping) => {
    const coordinates = lane.laneShape.trim().split(' ').map(p => p.split(',').map(Number))

    laneToEdgeMap.set(lane.laneId, lane.edgeId)

    return {
      type: 'Feature',
      geometry: { type: 'LineString', coordinates },
      properties: { laneId: lane.laneId, edgeName: lane.edgeName }
    }
  })

  const vectorSource = new VectorSource({
    features: new GeoJSON().readFeatures(
      { type: 'FeatureCollection', features },
      { dataProjection: 'EPSG:3857', featureProjection: 'EPSG:3857' }
    )
  })

  vectorLayer = new VectorLayer({
    source: vectorSource,
    style: feature => {
      const laneId = feature.get('laneId')
      const coordinates = feature.getGeometry()?.getCoordinates() as number[][] | undefined

      if (!coordinates) return null

      const edgeId = laneToEdgeMap.get(laneId)
      const count = edgeId ? vehicleCountMap.value[edgeId] ?? 0 : 0

      let opacity = 1.0
      let strokeColor = '#00B4D8'

      if (authStore.isTrafficManager()) {
        const isInManagedArea = isLaneInManagedArea(coordinates)

        if (viewMode.value === 'restricted') {
          if (!isInManagedArea) {
            return null
          }
        } else if (viewMode.value === 'full' && !isInManagedArea) {
          opacity = 0.3
          strokeColor = '#00B4D8'
        }
      }

      let color = strokeColor
      let width = 2.5
      let glow = false

      if (highlightLanes.value?.fromLanes.includes(laneId)) {
        color = '#FF6B35'
        width = 5
        glow = true
        opacity = 1.0
      } else if (highlightLanes.value?.toLanes.includes(laneId)) {
        color = '#A855F7'
        width = 5
        glow = true
        opacity = 1.0
      } else if (count >= 7) {
        color = '#D9001B'
        width = 3.5
      } else if (count >= 4) {
        color = '#F59A23'
        width = 3
      } else if (count > 0) {
        color = '#FFFF00'
        width = 2.5
      }

      const finalColor = opacity < 1.0 ? color + Math.floor(opacity * 255).toString(16).padStart(2, '0') : color

      if (glow) {
        return [
          new Style({
            stroke: new Stroke({
              color: color + '15',
              width: width + 12,
              lineCap: 'round',
              lineJoin: 'round'
            })
          }),
          new Style({
            stroke: new Stroke({
              color: color + '25',
              width: width + 8,
              lineCap: 'round',
              lineJoin: 'round'
            })
          }),
          new Style({
            stroke: new Stroke({
              color: color + '40',
              width: width + 4,
              lineCap: 'round',
              lineJoin: 'round'
            })
          }),
          new Style({
            stroke: new Stroke({
              color: finalColor,
              width,
              lineCap: 'round',
              lineJoin: 'round'
            })
          })
        ]
      }

      if (count > 0 || opacity < 1.0) {
        return [
          new Style({
            stroke: new Stroke({
              color: '#000000' + Math.floor(opacity * 48).toString(16).padStart(2, '0'),
              width: width + (opacity < 1.0 ? 1 : 3),
              lineCap: 'round',
              lineJoin: 'round'
            })
          }),
          new Style({
            stroke: new Stroke({
              color: finalColor,
              width,
              lineCap: 'round',
              lineJoin: 'round'
            })
          })
        ]
      }

      return new Style({
        stroke: new Stroke({
          color: finalColor,
          width,
          lineCap: 'round',
          lineJoin: 'round'
        })
      })
    }
  })

  const extent = vectorSource.getExtent()
  const center = getCenter(extent)

  view = new OLView({
    center,
    zoom: 15,
    minZoom: 13,
    maxZoom: 18,
    extent: [extent[0] - 100, extent[1] - 100, extent[2] + 100, extent[3] + 100]
  })

  map?.setView(view)

  map?.addLayer(vectorLayer)

  if (!hasFitted) {
    view.fit(extent, { padding: [20, 20, 20, 20], duration: 300 })
    hasFitted = true
  }

  const nameRes = await axios.get('/api-status/junctions')
  const raw = nameRes.data as Record<string, { junction_id: string; junction_name: string }>
  const junctionNameMap = new Map<string, string>()

  for (const tlsId in raw) {
    const item = raw[tlsId]
    if (item.junction_id && item.junction_name) {
      junctionNameMap.set(item.junction_id, item.junction_name)
    }
  }

  const junctionRes = await axios.get('/api-status/tls-junctions')
  const junctions = junctionRes.data as TlsJunctionInfo[]

  junctionMap.clear()

  junctions.forEach((tlsJunction, index) => {
    const junctionName = tlsJunction.junctionName ||
                        junctionNameMap.get(tlsJunction.junctionId) ||
                        tlsJunction.junctionId

    const junctionData = {
      tlsId: tlsJunction.tlsId,
      junctionId: tlsJunction.junctionId,
      junctionX: tlsJunction.junctionX,
      junctionY: tlsJunction.junctionY,
      junction_id: tlsJunction.junctionId,
      junction_name: junctionName
    }

    if (junctionMap.has(junctionName)) {
      const uniqueName = `${junctionName}_${tlsJunction.junctionId}`
      junctionMap.set(uniqueName, junctionData)
    } else {
      junctionMap.set(junctionName, junctionData)
    }

    junctionIdToTlsIdMap.value.set(tlsJunction.junctionId, tlsJunction.tlsId)
  })

  try {
    const initialDataResponse = await axios.get('/api-status/junctions')
    const initialData = initialDataResponse.data

    for (const tlsId in initialData) {
      let parsedData = initialData[tlsId]
      if (typeof parsedData === 'string') {
        try {
          parsedData = JSON.parse(parsedData)
        } catch (parseError) {
          continue
        }
      }

      if (parsedData && parsedData.state) {
        allTrafficLightData.value.set(tlsId, parsedData)
      }
    }
  } catch (error) {
  }

  rerenderTlsOverlays()

  map?.on('singleclick', evt => {
    const pixel = map!.getEventPixel(evt.originalEvent)
    const features = map!.getFeaturesAtPixel(pixel)

    if (features?.length) {
      const props = features[0].getProperties()
      if (props.edgeName) currentLocation.value = ` ${props.edgeName}`
    } else {
      const coordinate = evt.coordinate
      let found = false
      let closestTls: { overlay: Overlay, distance: number, junctionName: string } | null = null

      for (const overlay of map!.getOverlays().getArray()) {
        const pos = overlay.getPosition()
        if (pos) {
          const el = overlay.getElement()
          if (el instanceof HTMLElement && el.dataset['name']) {
            const pixel = map!.getPixelFromCoordinate(pos)
            const clickPixel = map!.getEventPixel(evt.originalEvent)
            const distance = Math.sqrt(
              Math.pow(pixel[0] - clickPixel[0], 2) + Math.pow(pixel[1] - clickPixel[1], 2)
            )
            if (distance <= 25) {
              if (!closestTls || distance < closestTls.distance) {
                closestTls = {
                  overlay,
                  distance,
                  junctionName: el.dataset['name']!
                }
              }
            }
          }
        }
      }
      if (closestTls) {
        const pos = closestTls.overlay.getPosition()!
        const junctionName = closestTls.junctionName
        const junction = junctionMap.get(junctionName)

        selectedJunctionName.value = junctionName

        if (junction) {
          selectedJunctionForStatus.value = junction
          selectedDirectionIndex.value = null
          fetchTrafficLightData(junction.junctionId)
        }

        updateAllTlsStyles()

        const currentView = map?.getView()
        if (currentView) {
          currentView.animate({
            center: pos,
            zoom: 18,
            duration: 1200,
            easing: (t: number) => t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2
          })
        }

        if (junction) {
          emit('signalLightClicked', junction.junction_id)
        }

        const controllableText = isJunctionControllable(junctionName) ? '' : ' (Read-only)'
        currentLocation.value = `${junctionName}${controllableText}`
        found = true
      } else {
        selectedJunctionName.value = null

        selectedJunctionForStatus.value = null
        selectedDirectionIndex.value = null
        currentTrafficLightData.value = null

        lastManualControl.value = null

        updateAllTlsStyles()

        emit('trafficLightCleared')

        for (const overlay of map!.getOverlays().getArray()) {
          const pos = overlay.getPosition()
          if (pos && Math.abs(pos[0] - coordinate[0]) < 5 && Math.abs(pos[1] - coordinate[1]) < 5) {
            const el = overlay.getElement()
            if (el instanceof HTMLElement && el.dataset['name']) {
              const controllableText = isJunctionControllable(el.dataset['name']) ? '' : ' (Read-only)'
              currentLocation.value = `${el.dataset['name']}${controllableText}`
              found = true
              break
            }
          }
        }
      }

      if (!found) currentLocation.value = 'No element selected'
    }

    clearAllMarkers()
    const clickEl = document.createElement('div')
    clickEl.className = 'iconfont click-marker'
    clickEl.innerHTML = '&#xe76f;'
    const overlay = new Overlay({
      element: clickEl,
      positioning: 'bottom-center',
      stopEvent: false,
      offset: [0, -12],
      position: evt.coordinate
    })
    map!.addOverlay(overlay)
    markerOverlays.push(overlay)
  })

  map?.on('contextmenu', evt => {
    evt.preventDefault()
    selectedJunctionName.value = null
    selectedJunctionForStatus.value = null
    selectedDirectionIndex.value = null
    currentTrafficLightData.value = null
    lastManualControl.value = null

    updateAllTlsStyles()
    clearAllMarkers()
    currentLocation.value = 'Click on a lane or signal light'
    emit('trafficLightCleared')
  })
}

// ✅ 新增：根据实际车辆数据更新路线显示
const updateEmergencyRoutesDisplay = () => {
  const activeVehicleIds = Object.keys(emergencyStore.vehicleDataMap)
  console.log('🔄 [Map] 更新紧急车辆路线显示，活跃车辆:', activeVehicleIds)

  if (activeVehicleIds.length > 0) {
    // 有活跃车辆时显示对应路线
    drawEmergencyRoutes(activeVehicleIds)
  } else {
    // 没有活跃车辆时清除所有路线
    clearEmergencyRoutes()
  }
}

// ✅ 新增：清除紧急车辆路线
const clearEmergencyRoutes = () => {
  if (!emergencyRouteLayer) return

  const source = emergencyRouteLayer.getSource()
  if (source) {
    source.clear()
    console.log('🧹 已清除所有紧急车辆路线')
  }
}

onMounted(async () => {
  if (!map) {
    map = new OLMap({ target: mapRef.value!, layers: [], controls: [] })
  } else {
    map.setTarget(mapRef.value!)
    setTimeout(() => map!.updateSize(), 300)
  }

  if (authStore.isTrafficManager()) {
    await fetchUserAreaPermissions()
  } else {
    console.log('🔍 [ControlMap] Non-Traffic Manager user, skipping area permissions')
    userManagedAreas.value = []
  }

  if (authStore.isTrafficManager()) {
    const managedAreas = getUserManagedAreas()
    viewMode.value = managedAreas.length === 1 ? 'restricted' : 'full'
  }

  await loadLaneData()

  // 初始化紧急车辆路线图层
  initEmergencyRouteLayer()

  // ✅ 只加载路线配置数据，不自动显示
  await loadEmergencyRoutes()

  connectWebSocket()
  connectEmergencyTracker()

  setTimeout(() => {
    setupViewWatchers()
  }, 100)

  if (authStore.isTrafficManager()) {
    setTimeout(async () => {
      await setMapViewForMode(viewMode.value)
    }, 200)
  }

  document.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  if (map) map.setTarget(undefined)
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
  }
  if (ws) {
    ws.close()
  }
  if (emergencyTracker) {
    emergencyTracker.disconnect()
  }
  if (statusBarUpdateTimer) {
    clearTimeout(statusBarUpdateTimer)
  }

  // 清理紧急车辆路线图层
  if (emergencyRouteLayer) {
    map?.removeLayer(emergencyRouteLayer)
  }

  // 清理紧急车辆标记
  emergencyVehicleOverlays.forEach(overlay => {
    map?.removeOverlay(overlay)
  })
  emergencyVehicleOverlays.length = 0

  // 清理高亮状态
  highlightedUpcomingJunctions.value.clear()

  mapEventListeners.forEach(cleanup => cleanup())
  mapEventListeners = []

  document.removeEventListener('keydown', handleKeyDown)
})

defineExpose({
  setHighlightLanes: (fromLanes: string[], toLanes: string[]) => {
    highlightLanes.value = { fromLanes, toLanes }
    vectorLayer?.changed()
  },
  setSelectedJunction: (junctionName: string | null) => {
    selectedJunctionName.value = junctionName
    updateAllTlsStyles()
  },
  setSelectedTrafficLight,
  setSelectedJunctionOnly,
  clearTrafficStatus,
  zoomToJunction,
  zoomToJunctionById,
  handleManualControlApplied
})

const zoomIn = () => {
  const currentView = map?.getView()
  if (currentView) {
    currentView.animate({ zoom: Math.min(currentView.getZoom()! + 0.5, 18), duration: 250 })
  }
}

const zoomOut = () => {
  const currentView = map?.getView()
  if (currentView) {
    currentView.animate({ zoom: Math.max(currentView.getZoom()! - 0.5, 13), duration: 250 })
  }
}

const handleKeyDown = (event: KeyboardEvent) => {
  if (event.target instanceof HTMLInputElement || event.target instanceof HTMLTextAreaElement) {
    return
  }

  if (event.key === 'F11' || (event.key === 'f' && event.ctrlKey)) {
    event.preventDefault()
    toggleFullscreen()
    return
  }

  const currentView = map?.getView()
  if (!currentView) return

  const currentCenter = currentView.getCenter()
  if (!currentCenter) return

  const currentZoom = currentView.getZoom() || 15
  const moveDistance = 3000 / Math.pow(2, currentZoom - 12)

  const newCenter = [...currentCenter]
  let shouldMove = false

  switch (event.key) {
    case 'ArrowUp':
    case 'w':
    case 'W':
      newCenter[1] += moveDistance
      shouldMove = true
      break
    case 'ArrowDown':
    case 's':
    case 'S':
      newCenter[1] -= moveDistance
      shouldMove = true
      break
    case 'ArrowLeft':
    case 'a':
    case 'A':
      newCenter[0] -= moveDistance
      shouldMove = true
      break
    case 'ArrowRight':
    case 'd':
    case 'D':
      newCenter[0] += moveDistance
      shouldMove = true
      break
  }

  if (shouldMove) {
    event.preventDefault()
    currentView.animate({
      center: newCenter,
      duration: 200,
      easing: (t: number) => t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2
    })
  }
}

const toggleFullscreen = () => {
  const mapElement = mapRef.value?.parentElement
  if (!mapElement) return

  if (!document.fullscreenElement) {
    if (mapElement.requestFullscreen) {
      mapElement.requestFullscreen()
    }
  } else {
    if (document.exitFullscreen) {
      document.exitFullscreen()
    }
  }
}
</script>

<style scoped lang="scss">
.map-show {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  position: relative;
  background-color: #1e1e2f !important;
  transition: width 0.3s ease, transform 0.3s ease;
  border-right: 0.01rem solid #3a3a4c;

  > * {
    position: relative;
    z-index: 1;
  }
}
.map-show.sidebar-open {
  width: 11.2rem;
  transform: translateX(2.4rem);
}
.toolbar {
  position: relative;
  height: 0.64rem;
  background-color: #1e1e2f !important;
  display: flex;
  align-items: center;
  padding: 0 0.24rem;
  border-bottom: 0.01rem solid #3a3a4c;
  z-index: 999;
  flex-shrink: 0;

  > * {
    position: relative;
    z-index: 1;
  }
}
.iconfont {
  font-size: 0.4rem;
  width: 0.4rem;
  height: 0.4rem;
  line-height: 0.4rem;
  color: #FFFFFF;
  text-align: center;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  border-radius: 0.06rem;
  position: relative;
  overflow: hidden;
  margin: 0.05rem;
  background-color: #1e1e2f;
  border: none;
  text-shadow: 0 0 8px rgba(0, 229, 255, 0.4);

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(45deg, transparent 48%, rgba(0, 180, 216, 0.1) 49%, rgba(0, 180, 216, 0.1) 51%, transparent 52%);
    opacity: 0;
    transition: opacity 0.3s ease;
    pointer-events: none;
  }

  &::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 0;
    height: 0;
    background: rgba(0, 229, 255, 0.2);
    border-radius: 50%;
    transform: translate(-50%, -50%);
    transition: all 0.4s ease;
  }
}

.iconfont:hover {
  color: #00FFFF;
  background-color: #1e1e2f;
  transform: translateY(-2px) scale(1.05);
  box-shadow:
    0 6px 20px rgba(0, 229, 255, 0.3),
    0 0 15px rgba(0, 229, 255, 0.2),
    inset 0 1px 3px rgba(0, 229, 255, 0.1);
  text-shadow: 0 0 12px rgba(0, 255, 255, 0.6);

  &::before {
    opacity: 1;
  }
}

.iconfont:active {
  transform: translateY(-1px) scale(1.02);
  box-shadow:
    0 4px 15px rgba(0, 229, 255, 0.3),
    0 0 15px rgba(0, 229, 255, 0.2),
    inset 0 2px 6px rgba(0, 229, 255, 0.2);

  &::after {
    width: 300%;
    height: 300%;
  }
}

.zoom-controls {
  display: flex;
  align-items: center;
  padding: 0.05rem;
}
.zoom-btn-plus {
  margin-right: 0.4rem;
}

.search-section {
  margin-left: auto;
  margin-right: 1rem;
  display: flex;
  align-items: center;
  gap: 0.12rem;
  flex-shrink: 0;
  max-width: calc(100% - 4rem);
}

.search-wrapper {
  width: 6rem;
  height: 0.4rem;
  display: flex;
  align-items: center;
  border: 0.01rem solid #00b4d8;
  background-color: #1e1e2f;
  border-radius: 0.2rem;
  padding: 0 0.12rem;
  box-sizing: border-box;
  transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  position: relative;
  flex-shrink: 0;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(45deg, transparent 48%, rgba(0, 180, 216, 0.1) 49%, rgba(0, 180, 216, 0.1) 51%, transparent 52%);
    opacity: 0;
    transition: opacity 0.3s ease;
    pointer-events: none;
  }

  .search-input {
    flex: 1;
    height: 100%;
    background-color: transparent;
    border: none;
    outline: none;
    color: #E3F2FD;
    font-size: 0.13rem;
    padding: 0 0.08rem;
    transition: all 0.3s ease;
    font-weight: 500;
    text-shadow: 0 0 8px rgba(227, 242, 253, 0.3);
  }

  .search-input::placeholder {
    color: rgba(179, 229, 252, 0.6);
    transition: color 0.3s ease;
  }
}

.search-wrapper:focus-within {
  border-color: #00E5FF;
  box-shadow:
    0 0 15px rgba(0, 229, 255, 0.2),
    inset 0 1px 3px rgba(0, 229, 255, 0.1);
  background-color: #1e1e2f;
  transform: translateY(-1px);

  &::before {
    opacity: 1;
  }

  .search-input::placeholder {
    color: rgba(179, 229, 252, 0.8);
  }
}

.search-wrapper:hover:not(:focus-within) {
  border-color: #00E5FF;
  background-color: #1e1e2f;
  transform: translateY(-0.5px);
  box-shadow:
    0 4px 12px rgba(0, 229, 255, 0.15),
    inset 0 1px 3px rgba(0, 229, 255, 0.1);

  &::before {
    opacity: 0.5;
  }
}

.search-suggestions {
  position: absolute;
  top: calc(100% + 0.02rem);
  left: -0.01rem;
  right: -0.01rem;
  background: #1e1e2f;
  border: 0.01rem solid #00b4d8;
  border-radius: 0.08rem;
  max-height: 2.4rem;
  overflow: hidden;
  z-index: 1000;
  box-shadow: 0 0.08rem 0.24rem rgba(0, 180, 216, 0.15),
              0 0.04rem 0.12rem rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(0.1rem);
  animation: slideDown 0.2s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-0.1rem);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.suggestion-item {
  padding: 0.12rem 0.16rem;
  color: #E8E8E8;
  font-size: 0.13rem;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0.0, 0.2, 1);
  border-bottom: 0.01rem solid rgba(0, 180, 216, 0.1);
  position: relative;
  overflow: hidden;
  display: flex;
  justify-content: space-between;
  align-items: center;

  &.suggestion-uncontrollable {
    color: #999;

    .readonly-indicator {
      font-size: 0.11rem;
      color: #666;
    }
  }
}

.suggestion-item:last-child {
  border-bottom: none;
  border-radius: 0 0 0.08rem 0.08rem;
}

.suggestion-item:first-child {
  border-radius: 0.08rem 0.08rem 0 0;
}

.suggestion-item:only-child {
  border-radius: 0.08rem;
}

.suggestion-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(0, 180, 216, 0.1), transparent);
  transition: left 0.5s ease;
}

.suggestion-item:hover {
  background: linear-gradient(135deg, #00b4d8 0%, #0090aa 100%);
  color: #FFFFFF;
  transform: translateX(0.04rem);
  box-shadow: inset 0 0 0.2rem rgba(255, 255, 255, 0.2);
}

.suggestion-item:hover::before {
  left: 100%;
}

.search-suggestions:hover {
  overflow-y: auto;
}

.search-suggestions::-webkit-scrollbar {
  width: 0.04rem;
}

.search-suggestions::-webkit-scrollbar-track {
  background: rgba(0, 180, 216, 0.1);
  border-radius: 0.02rem;
}

.search-suggestions::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #00b4d8 0%, #0090aa 100%);
  border-radius: 0.02rem;
  transition: background 0.2s ease;
}

.search-suggestions::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #00d4f8 0%, #00b4d8 100%);
}

.search-btn {
  flex-shrink: 0;
  width: 0.4rem;
  height: 0.4rem;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.08rem;
  border-radius: 0.06rem;
  background-color: #1e1e2f;
  border: none;
  transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  margin: 0.05rem;
  color: #00E5FF;
  text-shadow: 0 0 8px rgba(0, 229, 255, 0.4);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 0;
    height: 0;
    background: rgba(0, 229, 255, 0.2);
    border-radius: 50%;
    transform: translate(-50%, -50%);
    transition: all 0.4s ease;
  }

  &:hover {
    background-color: #1e1e2f;
    transform: translateY(-2px) scale(1.08) rotate(8deg);
    box-shadow:
      0 8px 25px rgba(0, 229, 255, 0.3),
      0 0 20px rgba(0, 229, 255, 0.2);
    color: #00FFFF;
    text-shadow: 0 0 12px rgba(0, 255, 255, 0.6);
  }

  &:active {
    transform: translateY(-1px) scale(1.02) rotate(4deg);
    background-color: #1e1e2f;

    &::before {
      width: 300%;
      height: 300%;
    }
  }
}

.map-container {
  flex: 1;
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
  display: flex;
  background-color: #1e1e2f;
}
.footer-container {
  height: 0.92rem;
  background-color: #1e1e2f !important;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-shrink: 0;
  font-size: 0.16rem;
  border-top: 0.01rem solid #3a3a4c;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent 0%, #00B4D8 50%, transparent 100%);
    opacity: 0.6;
    z-index: 1;
  }
}

.footer-content {
  width: 10.57rem;
  height: 0.57rem;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #E3F2FD;
  font-family: Arial, sans-serif;
  position: relative;
  z-index: 2;
  text-shadow: 0 0 8px rgba(227, 242, 253, 0.3);
  transition: all 0.4s ease;
  font-weight: 500;

  &:hover {
    text-shadow: 0 0 12px rgba(227, 242, 253, 0.5);
    transform: translateY(-1px);
  }
}

.area-info {
  position: absolute;
  right: 1rem;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  gap: 0.08rem;
  z-index: 2;

  .area-label {
    color: #B3E5FC;
    font-size: 0.14rem;
  }

  .area-value {
    color: #00E5FF;
    font-size: 0.14rem;
    font-weight: 600;
  }
}

.footer-icon {
  color: #00E5FF;
  font-size: 0.18rem;
  margin-right: 0.08rem;
  animation: locationPulse 3s ease-in-out infinite;
  text-shadow: 0 0 10px rgba(0, 229, 255, 0.6);
  filter: drop-shadow(0 0 8px rgba(0, 229, 255, 0.4));
}

@keyframes locationPulse {
  0%, 100% {
    color: #00E5FF;
    text-shadow: 0 0 10px rgba(0, 229, 255, 0.6);
    transform: scale(1);
    filter: drop-shadow(0 0 8px rgba(0, 229, 255, 0.4));
  }
  50% {
    color: #00FFFF;
    text-shadow: 0 0 15px rgba(0, 255, 255, 0.8);
    transform: scale(1.1);
    filter: drop-shadow(0 0 12px rgba(0, 255, 255, 0.6));
  }
}

.footer-link {
  color: #00E5FF;
  margin-left: 0.04rem;
  font-weight: 600;
  transition: all 0.4s ease;
  text-shadow: 0 0 8px rgba(0, 229, 255, 0.5);
  position: relative;

  &::before {
    content: '';
    position: absolute;
    bottom: -2px;
    left: 0;
    width: 0;
    height: 2px;
    background: linear-gradient(90deg, #00E5FF, #00FFFF);
    transition: width 0.4s ease;
    box-shadow: 0 0 4px rgba(0, 229, 255, 0.6);
  }

  &:hover {
    color: #00FFFF;
    text-shadow: 0 0 12px rgba(0, 255, 255, 0.7);
    transform: translateY(-1px);

    &::before {
      width: 100%;
    }
  }
}



:global(.search-marker),
:global(.click-marker) {
  color: #ff6b6b !important;
  font-size: 0.4rem !important;
  text-shadow:
    0 0 0.1rem rgba(255, 107, 107, 1),
    0 0 0.2rem rgba(255, 107, 107, 0.8),
    0 0 0.3rem rgba(255, 107, 107, 0.6) !important;
  filter: brightness(1.2) drop-shadow(0 0 0.15rem rgba(255, 107, 107, 0.7)) !important;
  animation: markerPulse 1.5s infinite !important;
  z-index: 1000 !important;
}

@keyframes markerPulse {
  0%, 100% {
    transform: scale(1) rotate(0deg);
    opacity: 1;
    filter: brightness(1.2) drop-shadow(0 0 0.15rem rgba(255, 107, 107, 0.7));
  }
  25% {
    transform: scale(1.1) rotate(5deg);
    opacity: 0.9;
    filter: brightness(1.3) drop-shadow(0 0 0.2rem rgba(255, 107, 107, 0.8));
  }
  50% {
    transform: scale(1.15) rotate(0deg);
    opacity: 0.8;
    filter: brightness(1.4) drop-shadow(0 0 0.25rem rgba(255, 107, 107, 0.9));
  }
  75% {
    transform: scale(1.1) rotate(-5deg);
    opacity: 0.9;
    filter: brightness(1.3) drop-shadow(0 0 0.2rem rgba(255, 107, 107, 0.8));
  }
}

.view-switch {
  display: flex;
  align-items: center;
  margin-left: 0.8rem;
  height: 100%;
}

.switch {
  position: relative;
  display: inline-block;
  width: 0.8rem;
  height: 0.34rem;
}

.switch input {
  display: none;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #1e1e2f;
  transition: .4s;
  border-radius: 0.34rem;
  border: 1px solid #3a3a4c;
}

.slider:before {
  position: absolute;
  content: "";
  height: 0.26rem;
  width: 0.26rem;
  left: 0.04rem;
  bottom: 0.04rem;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

input:checked + .slider {
  background-color: #00b4d8;
}

input:focus + .slider {
  box-shadow: 0 0 0.01rem #00E5FF;
}

input:checked + .slider:before {
  transform: translateX(0.46rem);
}

.slider:after {
  content: 'FOCUS';
  color: white;
  display: block;
  position: absolute;
  transform: translate(-50%, -50%);
  top: 50%;
  left: 50%;
  font-size: 0.08rem;
  font-family: Arial, sans-serif;
  font-weight: 600;
  text-shadow: 0 0 2px rgba(0, 0, 0, 0.5);
}

input:checked + .slider:after {
  content: 'GLOBAL';
}

@keyframes hoverGlow {
  0% {
    transform: scale(1.05);
  }
  50% {
    transform: scale(1.25);
  }
  100% {
    transform: scale(1.2);
  }
}

@keyframes normalPulse {
  0%, 100% {
    transform: scale(1);
    filter: brightness(1.2) drop-shadow(0 0 0.1rem rgba(0, 229, 255, 0.5));
    text-shadow:
      0 0 0.08rem rgba(0, 229, 255, 0.7),
      0 0 0.15rem rgba(0, 229, 255, 0.5),
      0 0 0.22rem rgba(0, 229, 255, 0.3);
  }
  50% {
    transform: scale(1.05);
    filter: brightness(1.3) drop-shadow(0 0 0.15rem rgba(0, 229, 255, 0.7));
    text-shadow:
      0 0 0.1rem rgba(0, 229, 255, 0.8),
      0 0 0.2rem rgba(0, 229, 255, 0.6),
      0 0 0.3rem rgba(0, 229, 255, 0.4);
  }
}

@keyframes cornerPulse {
  0%, 100% {
    opacity: 0.2;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(1.05);
  }
}

.map-traffic-status {
  position: absolute;
  z-index: 1000;
  width: fit-content;
  max-width: 4.5rem;
  pointer-events: none;

  :deep(.traffic-status-bar) {
    background: linear-gradient(135deg, rgba(30, 33, 47, 0.9) 0%, rgba(42, 45, 74, 0.9) 100%);
    border: 1px solid rgba(0, 180, 216, 0.5);
    box-shadow:
      0 0.08rem 0.32rem rgba(0, 0, 0, 0.3),
      0 0 0.24rem rgba(0, 180, 216, 0.15),
      inset 0 1px 3px rgba(0, 229, 255, 0.1);
    backdrop-filter: blur(12px);
    border-radius: 0.12rem;
    pointer-events: auto;
    position: relative;


    &:hover {
      border-color: rgba(0, 229, 255, 0.7);
      background: linear-gradient(135deg, rgba(30, 33, 47, 0.95) 0%, rgba(42, 45, 74, 0.95) 100%);
      box-shadow:
        0 0.12rem 0.4rem rgba(0, 0, 0, 0.4),
        0 0 0.32rem rgba(0, 229, 255, 0.2),
        inset 0 1px 3px rgba(0, 229, 255, 0.15);
    }
  }

  @media (max-width: 768px) {
    top: 0.4rem;
    right: 0.4rem;
    width: fit-content;
    max-width: 3.2rem;

    :deep(.status-content) {
      flex-direction: column;
      gap: 0.06rem;

      .traffic-light-icon {
        width: 0.18rem;
        height: 0.54rem;
      }

      .countdown-number {
        font-size: 0.16rem;
      }

      .status-text {
        font-size: 0.12rem;
      }

      .status-info, .countdown-section {
        min-width: auto;
        width: fit-content;
      }
    }
  }
}

.tech-decoration {
  position: absolute;
  pointer-events: none;
  z-index: 1;

  &.corner-tl {
    top: 0.2rem;
    left: 0.2rem;
    width: 0.3rem;
    height: 0.3rem;
    border-top: 2px solid rgba(0, 180, 216, 0.5);
    border-left: 2px solid rgba(0, 180, 216, 0.5);
    animation: cornerPulse 3s ease-in-out infinite;
  }

  &.corner-tr {
    top: 0.2rem;
    right: 0.2rem;
    width: 0.3rem;
    height: 0.3rem;
    border-top: 2px solid rgba(0, 180, 216, 0.5);
    border-right: 2px solid rgba(0, 180, 216, 0.5);
    animation: cornerPulse 3s ease-in-out infinite 1s;
  }

  &.corner-bl {
    bottom: 0.2rem;
    left: 0.2rem;
    width: 0.3rem;
    height: 0.3rem;
    border-bottom: 2px solid rgba(0, 180, 216, 0.5);
    border-left: 2px solid rgba(0, 180, 216, 0.5);
    animation: cornerPulse 3s ease-in-out infinite 2s;
  }

  &.corner-br {
    bottom: 0.2rem;
    right: 0.2rem;
    width: 0.3rem;
    height: 0.3rem;
    border-bottom: 2px solid rgba(0, 180, 216, 0.5);
    border-right: 2px solid rgba(0, 180, 216, 0.5);
    animation: cornerPulse 3s ease-in-out infinite 1.5s;
  }
}

@keyframes footerGlow {
  0%, 100% {
    opacity: 0.3;
    transform: scaleX(0.9);
  }
  50% {
    opacity: 0.5;
    transform: scaleX(1.1);
  }
}

@keyframes borderGlow {
  0%, 100% {
    opacity: 0.3;
  }
  50% {
    opacity: 0.6;
  }
}

@keyframes backgroundPulse {
  0%, 100% {
    opacity: 0.8;
  }
  50% {
    opacity: 0.5;
  }
}

// 紧急车辆路线样式
:global(.emergency-route-line) {
  animation: routeFlow 2s linear infinite;
}

@keyframes routeFlow {
  0% {
    stroke-dashoffset: 0;
  }
  100% {
    stroke-dashoffset: 15;
  }
}

:global(.emergency-start-marker) {
  animation: startMarkerPulse 2s ease-in-out infinite;
}

@keyframes startMarkerPulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.8;
  }
}

:global(.emergency-end-marker) {
  animation: endMarkerPulse 2s ease-in-out infinite;
}

@keyframes endMarkerPulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.8;
  }
}

// 紧急车辆实时标记样式优化
:global(.emergency-vehicle-overlay) {
  z-index: 1500 !important;
  pointer-events: auto;

  .emergency-vehicle {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;

    // 确保在所有元素之上
    z-index: 1500;

    // 响应式大小调整
    @media (max-width: 768px) {
      transform: scale(0.8);
    }
  }

  // 悬停时显示详细信息
  &:hover {
    .vehicle-info {
      display: block;
      animation: fadeInUp 0.3s ease-out;
    }
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}
</style>
