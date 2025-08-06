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
          <span class="footer-link" :class="{ 'no-selection': currentLocation === 'No element selected' }">{{ currentLocation }}</span>
        </span>
      </div>
      <div class="area-info" v-if="authStore.isTrafficManager()">
        <span class="area-label">Area:</span>
        <span class="area-value">{{ viewModeDescription }}</span>
      </div>
    </div>

    <EmergencyRequestDialog
      :isVisible="isEmergencyDialogVisible"
      :isSidebarOpen="isSidebarOpen"
      :pendingVehicle="firstPendingVehicle"
      :junctionIdToNameMap="junctionIdToNameMap"
      @approve="handleApprove"
      @reject="handleReject"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, defineExpose, computed, watch, nextTick, readonly, getCurrentInstance } from 'vue'
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
import EmergencyRequestDialog from '@/components/EmergencyRequestDialog.vue'
import { createApp } from 'vue'
import { emergencyVehicleApi, type EmergencyVehicleEvent } from '@/services/emergencyVehicleApi'
import { EmergencyVehicleTracker } from '@/services/specialEventApi'

defineProps<{
  isSidebarOpen: boolean
  isPriorityTrackingOpen?: boolean
}>()

const emit = defineEmits<{
  (e: 'signalLightClicked', junctionId: string): void
  (e: 'trafficLightCleared'): void
  (e: 'emergencyApproved', vehicleId: string): void
  (e: 'emergencyRejected', vehicleId: string): void
}>()

const isEmergencyDialogVisible = ref(false)
const junctionIdToNameMap = ref<Record<string, string>>({})
const showEmergencyDialog = ref(false)

let emergencyStore: ReturnType<typeof useEmergencyStore> | null = null

const getEmergencyStore = () => {
  if (!emergencyStore) {
    emergencyStore = useEmergencyStore()
  }
  return emergencyStore
}

const firstPendingVehicle = computed(() => {
  try {
    const store = getEmergencyStore()
    return store.pendingVehicles.length > 0 ? store.pendingVehicles[0] : null
  } catch (error) {
    return null
  }
})

const hasPendingEmergencyVehicles = computed(() => {
  try {
    const store = getEmergencyStore()
    return store.pendingVehicles.length > 0
  } catch (error) {
    return false
  }
})

watch(() => {
  try {
    return getEmergencyStore().pendingVehicles.length
  } catch (error) {
    return 0
  }
}, (newLength, oldLength) => {

  if (newLength === 0) {

    isEmergencyDialogVisible.value = false
    showEmergencyDialog.value = false
  }
})

watch(() => {
  const props = getCurrentInstance()?.props as { isPriorityTrackingOpen?: boolean } | undefined
  return props?.isPriorityTrackingOpen ?? false
}, (isOpen) => {
  console.log( isOpen)


})


watch(() => {
  try {
    const store = getEmergencyStore()
    return {
      pendingVehicles: store.pendingVehicles || [],
      vehicleDataMap: store.vehicleDataMap || {},
      activelyTrackedVehicle: store.activelyTrackedVehicle
    }
  } catch (error) {
    return { pendingVehicles: [], vehicleDataMap: {}, activelyTrackedVehicle: null }
  }
}, (newData, oldData) => {
  updateEmergencyVehicleMarkersFromStore()

  let vehicleIdToShow = null

  if (newData.activelyTrackedVehicle && newData.activelyTrackedVehicle.vehicleID) {
    vehicleIdToShow = newData.activelyTrackedVehicle.vehicleID
  } else if (Object.keys(newData.vehicleDataMap).length > 0) {
    vehicleIdToShow = Object.keys(newData.vehicleDataMap)[0]
  }

  if (vehicleIdToShow) {
    showPlannedRoute(vehicleIdToShow)
  } else if (newData.pendingVehicles.length === 0 && Object.keys(newData.vehicleDataMap).length === 0) {
    if (emergencyRouteLayer) {
      emergencyRouteLayer.getSource()?.clear()
    }
  }
}, { deep: true })


watch(() => {
  const props = getCurrentInstance()?.props as { isPriorityTrackingOpen?: boolean } | undefined
  return props?.isPriorityTrackingOpen ?? false
}, (isOpen) => {
  console.log( isOpen)

})

const showEmergencyRequestDialog = () => {
  if (hasPendingEmergencyVehicles.value) {
    isEmergencyDialogVisible.value = true
    showEmergencyDialog.value = true
  } else {

  }
}


const hideEmergencyRequestDialog = () => {

  isEmergencyDialogVisible.value = false
  showEmergencyDialog.value = false
}

const handleApprove = (vehicleId: string) => {
  console.log( vehicleId)

  try {
    const store = getEmergencyStore()


    store.approveVehicle(vehicleId)


    if (store.pendingVehicles.length === 0) {
      hideEmergencyRequestDialog()

    }

    showPlannedRoute(vehicleId)

    emit('emergencyApproved', vehicleId)
  } catch (error) {
    console.error('[Map] HandleApprove error:', error)

    hideEmergencyRequestDialog()
  }
}


const handleReject = (vehicleId: string) => {
  console.log(vehicleId)

  try {
    const store = getEmergencyStore()
    store.rejectVehicle(vehicleId)

    if (store.pendingVehicles.length === 0) {
      hideEmergencyRequestDialog()

    }

    emit('emergencyRejected', vehicleId)
  } catch (error) {
    console.error(error)
    hideEmergencyRequestDialog()
  }
}


const getJunctionName = (junctionId: string) => {
  try {
    const store = getEmergencyStore()
    return store.junctionIdToNameMap[junctionId] || junctionIdToNameMap.value[junctionId] || junctionId
  } catch (error) {
    return junctionIdToNameMap.value[junctionId] || junctionId
  }
}

const clearTrafficStatus = () => {
  selectedJunctionForStatus.value = null
  selectedDirectionIndex.value = null
  currentTrafficLightData.value = null
  lastManualControl.value = null
  highlightLanes.value = null
  rerenderTlsOverlays()
  vectorLayer?.changed()
}

const highlightJunctionConnectedLanes = async (junctionId: string) => {
  try {


    const response = await axios.get('/api-status/junctions')
    const junctionsData = response.data


    let junctionData = null
    for (const tlsId in junctionsData) {
      const junction = junctionsData[tlsId]
      if (junction.junction_id === junctionId) {
        junctionData = junction
        break
      }
    }

    if (!junctionData || !junctionData.connection) {

      return
    }


    const allConnectedLanes = new Set<string>()

    if (Array.isArray(junctionData.connection)) {
      junctionData.connection.forEach((connectionGroup: string[][]) => {
        if (Array.isArray(connectionGroup)) {
          connectionGroup.forEach((connection: string[]) => {
            if (Array.isArray(connection) && connection.length >= 2) {

              allConnectedLanes.add(connection[0])
              allConnectedLanes.add(connection[1])
            }
          })
        }
      })
    }

    const connectedLanesArray = Array.from(allConnectedLanes)


    if (connectedLanesArray.length > 0) {

      highlightLanes.value = {
        fromLanes: connectedLanesArray,
        toLanes: []
      }

      vectorLayer?.changed()

    }

  } catch (error) {
    console.error( error)
  }
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
let reconnectAttempts = 0
const maxReconnectAttempts = 5
let reconnectTimer: NodeJS.Timeout | null = null
let roadAnimationTimer: NodeJS.Timeout | null = null
let animationOffset = 0


let allCoordinates: number[][] = []
let mapCenterX = 0
const authStore = useAuthStore()
const viewMode = ref<'restricted' | 'full'>('restricted')
const userManagedAreas = ref<string[]>([])

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

const mapRef = ref<HTMLElement | null>(null)
let map: OLMap | null = null
let view: OLView | null = null
let vectorLayer: VectorLayer | null = null
const highlightLayer: VectorLayer | null = null
let emergencyRouteLayer: VectorLayer | null = null
let hasFitted = false

const currentLocation = ref('No element selected')
const highlightLanes = ref<{ fromLanes: string[]; toLanes: string[] } | null>(null)
const searchInput = ref('')
const junctionMap = new Map<string, Junction>()
const selectedJunctionName = ref<string | null>(null)
const searchSuggestions = ref<{name: string, area?: string}[]>([])
const showSuggestions = ref(false)

const vehicleCountMap = ref<Record<string, number>>({})
const laneToEdgeMap = new Map<string, string>()
const emergencyRoutes = ref<EmergencyVehicleEvent[]>([])
const emergencyVehicles = ref<Record<string, any>>({})
const realtimeVehicles = ref<Record<string, any>>({})
const vehicleOverlays: Overlay[] = []
const markerOverlays: Overlay[] = []
const tlsOverlays: Overlay[] = []
let vehicleTracker: EmergencyVehicleTracker | null = null
const edgeCoordinatesMap = new Map<string, number[][]>()

const showTrafficStatus = computed(() => {
  const hasJunction = !!selectedJunctionForStatus.value
  const hasDirection = selectedDirectionIndex.value !== null && selectedDirectionIndex.value !== undefined

  const shouldShow = hasJunction && hasDirection

  return shouldShow
})

const getTrafficLightDataForStatusBar = () => {
  if (currentTrafficLightData.value) {
    return currentTrafficLightData.value
  }

  if (selectedJunctionForStatus.value) {
    const junctionId = selectedJunctionForStatus.value.junction_id
    const tlsId = junctionIdToTlsIdMap.value.get(junctionId)

    if (tlsId && allTrafficLightData.value.has(tlsId)) {
      const data = allTrafficLightData.value.get(tlsId)

      return data
    }
  }

  return null
}

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

    return position
  } catch (error) {
    console.error( error)
    return { display: 'none' }
  }
}


let statusBarUpdateTimer: NodeJS.Timeout | null = null
const statusBarPositionKey = ref(0)

const updateStatusBarPosition = () => {
  if (statusBarUpdateTimer) {
    clearTimeout(statusBarUpdateTimer)
    statusBarUpdateTimer = null
  }

  if (showTrafficStatus.value && selectedJunctionForStatus.value) {
    statusBarPositionKey.value++
  }
}

const filteredSuggestions = computed(() => {
  return searchSuggestions.value.filter(suggestion => {
    if (authStore.isAdmin()) return true
    if (viewMode.value === 'full') return true
    return isJunctionControllable(suggestion.name)
  })
})

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

const getUserManagedAreas = (): string[] => {
  if (!authStore.isTrafficManager()) return []

  const managedAreas = authStore.getManagedAreas()
  return managedAreas || []
}

const fetchUserAreaPermissions = async () => {
  try {
    if (!authStore.isTrafficManager()) {
      userManagedAreas.value = []
      return
    }

    const managedAreas = authStore.getManagedAreas()
    if (managedAreas && managedAreas.length > 0) {
      userManagedAreas.value = managedAreas
    } else {
      userManagedAreas.value = []
    }

  } catch (error) {
    console.error(error)
    userManagedAreas.value = []
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

const zoomIn = () => {
  const currentView = map?.getView()
  if (currentView) {
    currentView.animate({ zoom: Math.min(currentView.getZoom()! + 0.5, 20), duration: 250 }) // 最大缩放修改为20
  }
}

const zoomOut = () => {
  const currentView = map?.getView()
  if (currentView) {
    currentView.animate({ zoom: Math.max(currentView.getZoom()! - 0.5, 13), duration: 250 })
  }
}

const handleSwitchChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const mode = target.checked ? 'full' : 'restricted'
  setViewMode(mode)
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

const searchJunction = () => {
  const name = searchInput.value.trim()
  const junction = junctionMap.get(name)
  if (junction && map) {
    const currentView = map.getView()
    if (currentView) {
      selectedJunctionName.value = name
      selectedJunctionForStatus.value = junction
      selectedDirectionIndex.value = null

      fetchTrafficLightData(junction.junction_id)
      rerenderTlsOverlays()

      currentView.animate({
        center: [junction.junctionX, junction.junctionY],
        zoom: 19,
        duration: 1200
      })

      markerOverlays.forEach(overlay => {
        map?.removeOverlay(overlay)
      })
      markerOverlays.length = 0


      const marker = document.createElement('div')
      marker.className = 'iconfont search-marker'
      marker.innerHTML = '&#xe655;'
      marker.style.color = '#ff6b6b'
      marker.style.fontSize = '24px'

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

const loadLaneData = async () => {
  try {



    if (!mapRef.value) {
      throw new Error('error')
    }


    const res = await axios.get('/api-status/lane-mappings')
    const data = res.data as LaneMapping[]



    if (!data || data.length === 0) {
      throw new Error('error')
    }

    allCoordinates = []
    let minX = Infinity, maxX = -Infinity

    data.forEach(lane => {
      const coordinates = lane.laneShape.trim().split(' ').map(p => p.split(',').map(Number))
      allCoordinates.push(...coordinates)
      coordinates.forEach(coord => {
        minX = Math.min(minX, coord[0])
        maxX = Math.max(maxX, coord[0])
      })


      if (!edgeCoordinatesMap.has(lane.edgeId)) {
        edgeCoordinatesMap.set(lane.edgeId, coordinates)
      }
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
      zIndex: 10,
      style: feature => {
        const laneId = feature.get('laneId')
        const coordinates = feature.getGeometry()?.getCoordinates() as number[][] | undefined

        if (!coordinates) return null

        const edgeId = laneToEdgeMap.get(laneId)
        const count = edgeId ? vehicleCountMap.value[edgeId] ?? 0 : 0

        let opacity = 1.0
        let color = '#00B4D8'
        let width = 2.5
        let isAnimated = false
        let isDynamic = false

        if (authStore.isTrafficManager()) {
          const isInManagedArea = isLaneInManagedArea(coordinates)

          if (viewMode.value === 'restricted') {
            if (!isInManagedArea) {
              return null
            }
          } else if (viewMode.value === 'full' && !isInManagedArea) {
            opacity = 0.3
            color = '#00B4D8'
          }
        }

        const isSelectedDirectionLane = isLaneInSelectedDirection(laneId)
        const isJunctionConnectedLane = isLaneConnectedToSelectedJunction(laneId)
        const isGreenLane = isLaneInGreenDirection(laneId)
        const isCongested = count >= 4
        if (isSelectedDirectionLane) {
          if (isGreenLane && !isCongested) {
            color = '#4CAF50'
            isDynamic = true
            isAnimated = true
            width = 4
          } else {
            color = '#666666'
            width = 4
            isDynamic = false
            isAnimated = false
          }
        }
        else if (isJunctionConnectedLane) {
          color = '#4C2A9B'
          width = 3
          isDynamic = false
          isAnimated = false
        }
        else {
          if (count >= 7) {
            color = '#D9001B'
            width = 3.5
          } else if (count >= 4) {
            color = '#F59A23'
            width = 3
          } else if (count > 0) {
            color = '#FFFF00'
            width = 2.5
          }
        }

        const finalColor = opacity < 1.0 ? color + Math.floor(opacity * 255).toString(16).padStart(2, '0') : color

        const styles = []

        if (isAnimated) {
          styles.push(new Style({
            stroke: new Stroke({
              color: color + '30',
              width: width + 4,
              lineCap: 'round',
              lineJoin: 'round'
            })
          }))

          styles.push(new Style({
            stroke: new Stroke({
              color: finalColor,
              width,
              lineCap: 'round',
              lineJoin: 'round',
              lineDash: [10, 10],
              lineDashOffset: animationOffset
            })
          }))
        } else {
          if (count > 0 || opacity < 1.0) {

            styles.push(new Style({
              stroke: new Stroke({
                color: '#000000' + Math.floor(opacity * 48).toString(16).padStart(2, '0'),
                width: width + (opacity < 1.0 ? 1 : 3),
                lineCap: 'round',
                lineJoin: 'round'
              })
            }))
          }

          styles.push(new Style({
            stroke: new Stroke({
              color: finalColor,
              width,
              lineCap: 'round',
              lineJoin: 'round'
            })
          }))
        }

        return styles.length > 0 ? styles : null
      }
    })



    const extent = vectorSource.getExtent()
    const center = getCenter(extent)


    view = new OLView({
      center,
      zoom: 15,
      minZoom: 13,
      maxZoom: 20,
      extent: [extent[0] - 100, extent[1] - 100, extent[2] + 100, extent[3] + 100]
    })



    if (map) {
      map.setView(view)

    } else {
      throw new Error('error')
    }


    map.addLayer(vectorLayer)

    if (!hasFitted) {
      view.fit(extent, { padding: [20, 20, 20, 20], duration: 300 })
      hasFitted = true
    }


    await loadJunctionData()
    setupMapInteractions()
    setupViewWatchers()
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

    if (authStore.isTrafficManager()) {
      setTimeout(async () => {
        await setMapViewForMode(viewMode.value)
      }, 200)
    }

  } catch (error) {
    console.error({
      message: error.message,
      stack: error.stack,
      mapRef: !!mapRef.value,
      map: !!map
    })
  }
}

const loadJunctionData = async () => {
  try {

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
    const junctions = junctionRes.data as Array<{
      tlsId: string
      junctionId: string
      junctionName?: string
      junctionX: number
      junctionY: number
    }>

    junctionMap.clear()

    junctions.forEach((tlsJunction) => {
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
      console.warn(error)
    }

    rerenderTlsOverlays()

    connectWebSocket()

    initVehicleTracker()

  } catch (error) {
    console.error( error)
  }
}
const isLaneInSelectedDirection = (laneId: string): boolean => {
  if (!selectedJunctionForStatus.value ||
      selectedDirectionIndex.value === null ||
      selectedDirectionIndex.value === undefined ||
      !highlightLanes.value) {
    return false
  }

  return highlightLanes.value.toLanes.includes(laneId)
}

const isLaneConnectedToSelectedJunction = (laneId: string): boolean => {
  if (!selectedJunctionForStatus.value || !highlightLanes.value) {
    return false
  }

  return highlightLanes.value.fromLanes.includes(laneId)
}

const isLaneInGreenDirection = (laneId: string): boolean => {
  if (!selectedJunctionForStatus.value ||
      selectedDirectionIndex.value === null ||
      selectedDirectionIndex.value === undefined ||
      !highlightLanes.value) {
    return false
  }

  const currentLight = getCurrentTrafficLight(selectedJunctionForStatus.value.junction_id)
  if (currentLight !== 'green') {
    return false
  }
  const isInSelectedDirection = highlightLanes.value.toLanes.includes(laneId)

  return isInSelectedDirection
}

const isLaneNearSelectedJunction = (laneId: string): boolean => {
  if (!selectedJunctionForStatus.value) {
    return false
  }

  const junction = selectedJunctionForStatus.value
  const junctionX = junction.junctionX
  const junctionY = junction.junctionY
  const laneFeature = vectorLayer?.getSource()?.getFeatures().find(feature =>
    feature.get('laneId') === laneId
  )

  if (!laneFeature) {
    return false
  }

  const geometry = laneFeature.getGeometry()
  if (!geometry) {
    return false
  }

  const coordinates = geometry.getCoordinates() as number[][]

  const maxDistance = 100

  for (const coord of coordinates) {
    const distance = Math.sqrt(
      Math.pow(coord[0] - junctionX, 2) + Math.pow(coord[1] - junctionY, 2)
    )

    if (distance <= maxDistance) {
      return true
    }
  }

  return false
}

const startRoadAnimation = () => {
  if (roadAnimationTimer) {
    return
  }

  let frameCount = 0
  roadAnimationTimer = setInterval(() => {
    animationOffset -= 1
    if (animationOffset <= -20) {
      animationOffset = 0
    }

    frameCount++
    if (frameCount % 20 === 0) {
    }

    vectorLayer?.changed()
  }, 50)
}


const stopRoadAnimation = () => {
  if (roadAnimationTimer) {
    clearInterval(roadAnimationTimer)
    roadAnimationTimer = null
  }
}
const isJunctionSelected = (junctionId: string): boolean => {
  const hasJunctionSelected = selectedJunctionForStatus.value?.junction_id === junctionId
  const hasDirectionSelected = selectedDirectionIndex.value !== null && selectedDirectionIndex.value !== undefined
  return hasJunctionSelected && hasDirectionSelected
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
        return ''
      }
    }
  }

  return ''
}

const connectWebSocket = () => {
  try {
    const wsProtocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${wsProtocol}//localhost:8087/api/status/ws`

    ws = new WebSocket(wsUrl)

    ws.onopen = () => {
      reconnectAttempts = 0
    }

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)

        if (data.trafficLights) {
          let hasTrafficLightUpdate = false

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
              if (selectedJunctionForStatus.value) {
                const selectedTlsId = junctionIdToTlsIdMap.value.get(selectedJunctionForStatus.value.junction_id)
                if (tlsId === selectedTlsId) {
                  hasTrafficLightUpdate = true
                }
              }

              allTrafficLightData.value.set(tlsId, parsedData)
            }
          }

          if (selectedJunctionForStatus.value) {
            const junctionId = selectedJunctionForStatus.value.junction_id || selectedJunctionForStatus.value.junctionId
            const tlsId = junctionIdToTlsIdMap.value.get(junctionId)

            if (tlsId && data.trafficLights[tlsId]) {
              const tlsData = data.trafficLights[tlsId]

              let parsedData
              if (typeof tlsData === 'string') {
                try {
                  parsedData = JSON.parse(tlsData)
                } catch (parseError) {

                }
              } else {
                parsedData = tlsData
              }

              if (parsedData && typeof parsedData.state === 'string') {
                currentTrafficLightData.value = parsedData
              }
            }
          }

          rerenderTlsOverlays()
          if (hasTrafficLightUpdate && highlightLanes.value) {
            vectorLayer?.changed()
          }
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

        }

      } catch (error) {
        console.error( error)
      }
    }

    ws.onerror = (error) => {
      console.error(error)
    }

    ws.onclose = () => {
      if (reconnectAttempts < maxReconnectAttempts) {
        reconnectAttempts++
        reconnectTimer = setTimeout(connectWebSocket, 3000)
      }
    }
  } catch (error) {
    console.error( error)
    if (reconnectAttempts < maxReconnectAttempts) {
      reconnectAttempts++
      reconnectTimer = setTimeout(connectWebSocket, 3000)
    }
  }
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

    const isFullySelected = isJunctionSelected(junctionId)
    const isJunctionOnly = selectedJunctionForStatus.value?.junction_id === junctionId &&
                          (selectedDirectionIndex.value === null || selectedDirectionIndex.value === undefined)

    const currentLight = getCurrentTrafficLight(junctionId)

    const containerEl = document.createElement('div')
    containerEl.style.position = 'relative'
    containerEl.style.cursor = 'pointer'
    containerEl.dataset['name'] = junctionName
    containerEl.dataset['junctionId'] = junction.junction_id

    const app = createApp(TrafficLightIcon, {
      currentLight: currentLight,
      isSelected: isFullySelected,
      isPartiallySelected: isJunctionOnly,
      isControllable: isControllable,
      showAllLights: false,
      isEmergencyUpcoming: false
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

const handleTrafficLightClick = async (junctionName: string, junction: Junction) => {


  selectedJunctionName.value = junctionName
  selectedJunctionForStatus.value = junction
  selectedDirectionIndex.value = null

  markerOverlays.forEach(overlay => {
    map?.removeOverlay(overlay)
  })
  markerOverlays.length = 0

  const currentView = map?.getView()
  if (currentView) {
    currentView.animate({
    center: [junction.junctionX, junction.junctionY],
    zoom: 19,
    duration: 1000
    })
  }

  await highlightJunctionConnectedLanes(junction.junction_id)

  emit('signalLightClicked', junction.junction_id)

  const controllableText = isJunctionControllable(junctionName) ? '' : ' (Read-only)'
  currentLocation.value = `${junctionName}${controllableText}`

  rerenderTlsOverlays()
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
        zoom: Math.max(currentView.getZoom() || 15, 18),
        duration: 1000
      })

      selectedJunctionName.value = junctionName
      rerenderTlsOverlays()

      const controllableText = isJunctionControllable(junctionName) ? '' : ' (Read-only)'
      currentLocation.value = `${junctionName}${controllableText}`
    }
  } else {

  }
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
        zoom: Math.max(currentView.getZoom() || 15, 18),
        duration: 1000
      })

      selectedJunctionName.value = junctionName
      rerenderTlsOverlays()

      const controllableText = isJunctionControllable(junctionName) ? '' : ' (Read-only)'
      currentLocation.value = `${junctionName}${controllableText}`
    }
  } else {
    console.warn( junctionName)
  }
}

// 设置选中的交通灯和方向
const setSelectedTrafficLight = (junctionId: string, directionIndex: number, options: { disableZoom?: boolean } = {}) => {

  const junction = Array.from(junctionMap.values()).find(j => j.junction_id === junctionId)
  const junctionName = Array.from(junctionMap.entries()).find(([name, j]) => j.junction_id === junctionId)?.[0]

  if (junction && junctionName) {
    selectedJunctionForStatus.value = junction
    selectedDirectionIndex.value = directionIndex
    selectedJunctionName.value = junctionName


    markerOverlays.forEach(overlay => {
      map?.removeOverlay(overlay)
    })
    markerOverlays.length = 0

    fetchTrafficLightData(junction.junction_id || junction.junctionId)

    rerenderTlsOverlays()

    if (!options.disableZoom) {
      const currentView = map?.getView()
      if (currentView) {
        currentView.animate({
          center: [junction.junctionX, junction.junctionY],
          zoom: Math.max(currentView.getZoom() || 15, 18),
          duration: 1000
        })
      }
    }

    nextTick(() => {
      updateStatusBarPosition()
    })
  } else {
    console.warn( junctionId)
  }
}

const setSelectedJunctionOnly = (junctionId: string) => {


  const junction = Array.from(junctionMap.values()).find(j => j.junction_id === junctionId)
  const junctionName = Array.from(junctionMap.entries()).find(([name, j]) => j.junction_id === junctionId)?.[0]

  if (junction && junctionName) {
    selectedJunctionForStatus.value = junction
    selectedDirectionIndex.value = null
    selectedJunctionName.value = junctionName



    markerOverlays.forEach(overlay => {
      map?.removeOverlay(overlay)
    })

    highlightJunctionConnectedLanes(junctionId)

    fetchTrafficLightData(junction.junctionId)

    rerenderTlsOverlays()
  } else {

  }
}

// 获取交通灯数据
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
          console.error( parseError)
          return
        }
      }

      currentTrafficLightData.value = parsedData
      allTrafficLightData.value.set(tlsId, parsedData)

      console.log('[Map] Get traffic light', parsedData)
    }
  } catch (error) {
    console.error( error)
  }
}

const showPlannedRoute = async (vehicleId: string) => {


  try {

    const response = await axios.get('/api-status/emergency-routes')
    const routes = response.data


    const vehicleRoute = routes.find((route: any) => {
      return route.vehicle_id === vehicleId ||
             route.vehicleId === vehicleId ||
             route.event_id === vehicleId ||
             route.eventId === vehicleId
    })



    if (vehicleRoute && vehicleRoute.route_edges) {

      if (!emergencyRouteLayer) {
        emergencyRouteLayer = new VectorLayer({
          source: new VectorSource(),
          zIndex: 12,
          style: new Style({
            stroke: new Stroke({
              color: '#FFFFFF',
              width: 4,
              lineDash: [10, 5],
              lineCap: 'round',
              lineJoin: 'round'
            })
          })
        })
        map?.addLayer(emergencyRouteLayer)
      }

      // 清除之前的路线
      emergencyRouteLayer.getSource()?.clear()

      // 绘制新路线
      const routeFeatures: Feature[] = []

      vehicleRoute.route_edges.forEach((edgeId: string) => {
        const coordinates = edgeCoordinatesMap.get(edgeId)
        if (coordinates && coordinates.length > 0) {
          const feature = new Feature({
            geometry: new LineString(coordinates)
          })
          feature.setProperties({ edgeId, isEmergencyRoute: true })
          routeFeatures.push(feature)
        } else {

        }
      })

      if (routeFeatures.length > 0) {
        emergencyRouteLayer.getSource()?.addFeatures(routeFeatures)

      } else {

      }
    } else {
    }
  } catch (error) {
    console.error( error)
  }
}


const initVehicleTracker = () => {


  try {
    vehicleTracker = new EmergencyVehicleTracker()

    vehicleTracker.onMessage((data) => {

      let hasUpdate = false


      if (data && typeof data === 'object') {



        realtimeVehicles.value = {}
        emergencyVehicles.value = {}

        Object.entries(data).forEach(([vehicleId, vehicleJsonString]) => {
          try {

            const vehicleData = typeof vehicleJsonString === 'string'
              ? JSON.parse(vehicleJsonString)
              : vehicleJsonString



            if (vehicleData && vehicleData.position) {

              if (vehicleData.eventID || vehicleData.isEmergency) {
                emergencyVehicles.value[vehicleId] = vehicleData

              } else {
                realtimeVehicles.value[vehicleId] = vehicleData

              }
              hasUpdate = true
            } else {

            }
          } catch (parseError) {
            console.error( parseError, vehicleJsonString)
          }
        })
      }

      if (hasUpdate) {

        updateVehicleMarkers()
      } else {

      }
    })

  } catch (error) {
    console.error(error)
  }
}

const updateEmergencyVehicleMarkersFromStore = () => {

  try {
    const store = getEmergencyStore()
    const storeVehicles = store.vehicleDataMap || {}



    emergencyVehicles.value = { ...storeVehicles }


    updateVehicleMarkers()

  } catch (error) {
    console.error( error)
  }
}

const updateVehicleMarkers = () => {


  if (!map) {

    return
  }



  vehicleOverlays.forEach(overlay => {
    map?.removeOverlay(overlay)
  })
  vehicleOverlays.length = 0

  let markerCount = 0


  Object.entries(emergencyVehicles.value).forEach(([vehicleId, vehicleData]) => {

    if (vehicleData && vehicleData.position) {

      if (vehicleData.position.x && vehicleData.position.y) {
        createVehicleMarker(vehicleId, vehicleData, true)
        markerCount++
      } else {
        console.warn(vehicleData.position)
      }
    } else {
      console.warn(vehicleData)
    }
  })

  Object.entries(realtimeVehicles.value).forEach(([vehicleId, vehicleData]) => {


    if (vehicleData && vehicleData.position) {


      if (vehicleData.position.x && vehicleData.position.y) {
        createVehicleMarker(vehicleId, vehicleData, false)
        markerCount++
      } else {
        console.warn(vehicleData.position)
      }
    } else {
      console.warn( vehicleData)
    }
  })


}

const createVehicleMarker = (vehicleId: string, vehicleData: any, isEmergency: boolean = false) => {
  console.log( vehicleId, vehicleData)

  if (!map) {
    return
  }


  const currentView = map.getView()
  if (currentView) {
    const extent = currentView.calculateExtent()

  }


  let position

  if (vehicleData.position.x !== undefined && vehicleData.position.y !== undefined) {

    position = [vehicleData.position.x, vehicleData.position.y]
  } else if (vehicleData.position.lon !== undefined && vehicleData.position.lat !== undefined) {

    position = [vehicleData.position.lon, vehicleData.position.lat]
  } else if (Array.isArray(vehicleData.position) && vehicleData.position.length >= 2) {

    position = vehicleData.position
  } else {
    console.error(vehicleData.position)
    return
  }


  if (currentView) {
    const extent = currentView.calculateExtent()
    const [minX, minY, maxX, maxY] = extent

    const bufferX = (maxX - minX) * 0.5
    const bufferY = (maxY - minY) * 0.5

    console.log('[Map] 地图范围检查:', {
      mapExtent: { minX, minY, maxX, maxY },
      vehiclePosition: position,
      bufferX, bufferY,
      inExtendedRange: {
        x: position[0] >= (minX - bufferX) && position[0] <= (maxX + bufferX),
        y: position[1] >= (minY - bufferY) && position[1] <= (maxY + bufferY)
      }
    })

    if (position[0] < (minX - bufferX) || position[0] > (maxX + bufferX) ||
        position[1] < (minY - bufferY) || position[1] > (maxY + bufferY)) {


      if (Math.abs(position[0]) <= 180 && Math.abs(position[1]) <= 90) {
        const lon = position[0]
        const lat = position[1]
        const x = lon * 20037508.34 / 180
        const y = Math.log(Math.tan((90 + lat) * Math.PI / 360)) / (Math.PI / 180) * 20037508.34 / 180
        position = [x, y]
      }
    }


    if (position[0] < minX || position[0] > maxX || position[1] < minY || position[1] > maxY) {

    }
  }

  const containerEl = document.createElement('div')
  containerEl.style.position = 'relative'
  containerEl.style.cursor = 'pointer'
  containerEl.dataset['vehicleId'] = vehicleId


  if (isEmergency) {

    try {
      const app = createApp(EmergencyVehicleMarker, {
        vehicleData: {
          eventID: vehicleData.eventID || vehicleId,
          vehicleID: vehicleId,
          organization: vehicleData.organization || 'Emergency Services',
          currentEdgeID: vehicleData.currentEdgeID || '',
          upcomingJunctionID: vehicleData.upcomingJunctionID,
          nextEdgeID: vehicleData.nextEdgeID,
          upcomingTlsID: vehicleData.upcomingTlsID,
          upcomingTlsState: vehicleData.upcomingTlsState,
          upcomingTlsCountdown: vehicleData.upcomingTlsCountdown || 0,
          position: {
            x: vehicleData.position.x || position[0],
            y: vehicleData.position.y || position[1],
            timestamp: vehicleData.position.timestamp || Date.now()
          }
        },
        mapPixelPosition: [0, 0],
        showInfo: false
      })

      app.mount(containerEl)


      containerEl.addEventListener('click', (e) => {
        e.stopPropagation()
        handleVehicleClick(vehicleId, vehicleData)
      })
    } catch (error) {
      console.error( error)
      const emergencyDot = document.createElement('div')
      emergencyDot.style.cssText = `
        width: 20px;
        height: 20px;
        background: #ff0000;
        border-radius: 50%;
        border: 2px solid white;
        box-shadow: 0 0 10px red;
        animation: emergencyBlink 1s infinite;
        z-index: 1000;
      `
      emergencyDot.title = `Emergency Vehicle: ${vehicleId}`
      containerEl.appendChild(emergencyDot)
    }
  } else {

    const vehicleDot = document.createElement('div')
    vehicleDot.className = 'realtime-vehicle-marker'
    vehicleDot.innerHTML = `
      <div class="vehicle-dot">
        <div class="pulse-ring"></div>
        <div class="inner-dot"></div>
      </div>
    `
    vehicleDot.title = `Vehicle: ${vehicleId}`



    vehicleDot.addEventListener('click', (e) => {
      e.stopPropagation()
      handleVehicleClick(vehicleId, vehicleData)
    })

    containerEl.appendChild(vehicleDot)
  }

  const overlay = new Overlay({
    element: containerEl,
    positioning: 'center-center',
    stopEvent: false,
    offset: [0, 0],
    position: position
  })



  map.addOverlay(overlay)
  vehicleOverlays.push(overlay)

}


const handleVehicleClick = (vehicleId: string, vehicleData: any) => {
  console.log( vehicleId, vehicleData)


  currentLocation.value = `Vehicle: ${vehicleId}`

  showPlannedRoute(vehicleId)
}
const setupMapInteractions = () => {
  if (!map) return

  map.on('singleclick', evt => {
    const pixel = map!.getEventPixel(evt.originalEvent)
    const features = map!.getFeaturesAtPixel(pixel)

    if (features?.length) {
      const props = features[0].getProperties()
      if (props.edgeName) {
        currentLocation.value = `${props.edgeName}`
      }
    } else {
      const coordinate = evt.coordinate
      let found = false

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
              const junctionName = el.dataset['name']!
              const junction = junctionMap.get(junctionName)
              if (junction) {
                handleTrafficLightClick(junctionName, junction)
                found = true
                break
              }
            }
          }
        }
      }

      if (!found) {
        selectedJunctionName.value = null
        selectedJunctionForStatus.value = null
        selectedDirectionIndex.value = null
        currentTrafficLightData.value = null

        rerenderTlsOverlays()
        emit('trafficLightCleared')
        currentLocation.value = 'No element selected'
      }
    }

    markerOverlays.forEach(overlay => {
      map?.removeOverlay(overlay)
    })
    markerOverlays.length = 0

    const clickEl = document.createElement('div')
    clickEl.className = 'iconfont click-marker'
    clickEl.innerHTML = '&#xe655;'
    clickEl.style.color = '#ff6b6b'
    clickEl.style.fontSize = '20px'

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


  map.on('contextmenu', evt => {
    evt.preventDefault()
    selectedJunctionName.value = null
    selectedJunctionForStatus.value = null
    selectedDirectionIndex.value = null
    currentTrafficLightData.value = null

    rerenderTlsOverlays()


    markerOverlays.forEach(overlay => {
      map?.removeOverlay(overlay)
    })
    markerOverlays.length = 0

    currentLocation.value = 'No element selected'
    emit('trafficLightCleared')
  })


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
          maxZoom: 20,
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
          maxZoom: 20,
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
    console.error(error)
  }
}

onMounted(async () => {

  try {
    if (!map) {
      map = new OLMap({
        target: mapRef.value!,
        layers: [],
        controls: []
      })

    }

    if (authStore.isTrafficManager()) {
      await fetchUserAreaPermissions()
    } else {
      userManagedAreas.value = []
    }
    if (authStore.isTrafficManager()) {
      const managedAreas = getUserManagedAreas()
      viewMode.value = managedAreas.length === 1 ? 'restricted' : 'full'
    }
    await loadLaneData()
    startRoadAnimation()

    setTimeout(() => {
      try {
        emergencyStore = useEmergencyStore()
      } catch (error) {
        console.error(error)
      }
    }, 100)

  } catch (error) {
    console.error( error)
  }
})

onUnmounted(() => {
  if (map) map.setTarget(undefined)
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
  }
  if (ws) {
    ws.close()
  }
  if (statusBarUpdateTimer) {
    clearTimeout(statusBarUpdateTimer)
  }


  stopRoadAnimation()

  mapEventListeners.forEach(cleanup => cleanup())
  mapEventListeners = []

  if (vehicleTracker) {
    vehicleTracker.disconnect()
    vehicleTracker = null
  }

  vehicleOverlays.forEach(overlay => {
    map?.removeOverlay(overlay)
  })
  vehicleOverlays.length = 0
})

defineExpose({
  setHighlightLanes: (fromLanes: string[], toLanes: string[]) => {

    if (selectedJunctionForStatus.value && highlightLanes.value && highlightLanes.value.fromLanes.length > 0) {

      const allConnectedLanes = [...highlightLanes.value.fromLanes]

      fromLanes.forEach(lane => {
        if (!allConnectedLanes.includes(lane)) {
          allConnectedLanes.push(lane)
        }
      })
      toLanes.forEach(lane => {
        if (!allConnectedLanes.includes(lane)) {
          allConnectedLanes.push(lane)
        }
      })

      highlightLanes.value = {
        fromLanes: allConnectedLanes,
        toLanes: [...fromLanes, ...toLanes]
      }
    } else {
      highlightLanes.value = { fromLanes, toLanes }
    }

    vectorLayer?.changed()
  },
  setSelectedJunction: (junctionName: string | null) => {
    selectedJunctionName.value = junctionName
    rerenderTlsOverlays()
  },
  setSelectedTrafficLight,
  setSelectedJunctionOnly,
  clearTrafficStatus,
  zoomToJunction,
  zoomToJunctionById,
  showPlannedRoute,
  showEmergencyRequestDialog,
  hideEmergencyRequestDialog,
  hasPendingEmergencyVehicles: readonly(hasPendingEmergencyVehicles)
})
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
}

.map-show.sidebar-open {
  width: calc(100% - 2.4rem);
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
}

.footer-content {
  display: flex;
  justify-content: center;
  align-items: center;
  color: #00b4d8 ;
  font-family: Arial, sans-serif;
}

.footer-icon {
  color: #00b4d8 !important;
  font-size: 0.18rem !important;
  margin-right: 0.08rem;
}

.footer-link {
  color: #00b4d8;
  margin-left: 0.04rem;
  font-weight: 600;

  &.no-selection {
    color: #9CA3AF !important;
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
}

.area-label {
  color: #B3E5FC;
  font-size: 0.14rem;
}

.area-value {
  color: #00b4d8;
  font-size: 0.14rem;
  font-weight: 600;
}

.iconfont {
  font-size: 0.4rem;
  width: 0.4rem;
  height: 0.4rem;
  line-height: 0.4rem;
  color: #FFFFFF;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 0.06rem;
  margin: 0.05rem;
  background-color: #1e1e2f;
  border: none;

  &:hover {
    color: #00b4d8;
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
  position: relative;
  flex-shrink: 0;
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
}

.search-input::placeholder {
  color: rgba(179, 229, 252, 0.6);
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
}

.suggestion-item {
  padding: 0.12rem 0.16rem;
  color: #E8E8E8;
  font-size: 0.13rem;
  cursor: pointer;
  border-bottom: 0.01rem solid rgba(0, 180, 216, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;

  &:hover {
    color: #00b4d8;
  }

  &.suggestion-uncontrollable {
    color: #999;
  }
}

.readonly-indicator {
  font-size: 0.11rem;
  color: #666;
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
  margin: 0.05rem;
  color: #00b4d8;
}

// View switch 样式
.view-switch {
  margin-left: 1rem;
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

input:focus + .slider {
  box-shadow: 0 0 0.01rem #00b4d8;
}

input:checked + .slider {
  background-color: #00b4d8;
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

// 添加标记样式
:global(.search-marker),
:global(.click-marker) {
  color: #ff6b6b !important;
  font-size: 24px !important;
  z-index: 1000 !important;
  font-weight: bold;
  text-shadow: none;
  /* 移除动画 */
}

// 地图交通状态栏样式
.map-traffic-status {
  position: absolute;
  z-index: 1000;
  pointer-events: auto;
}

// 自定义 tooltip 样式
:global([title]) {
  position: relative;
}

:global([title]:hover::after) {
  content: attr(title);
  position: absolute;
  left: 50%;
  top: calc(100% + 8px);
  transform: translateX(-50%);
  padding: 6px 10px;
  background: rgba(45, 45, 45, 0.95) !important;
  color: #ffffff !important;
  font-size: 12px !important;
  font-weight: 500 !important;
  font-family: 'Inter', 'Segoe UI', 'Arial', 'Helvetica Neue', 'Roboto', sans-serif !important;
  line-height: 1.2 !important;
  border-radius: 4px;
  white-space: nowrap;
  z-index: 99999;
  pointer-events: none;
  min-height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: 0.01em;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  animation: tooltipFadeIn 0.2s ease;
}

@keyframes tooltipFadeIn {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-2px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}

// 实时车辆标记样式
:global(.realtime-vehicle-marker) {
  .vehicle-dot {
    position: relative;
    width: 16px;
    height: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .pulse-ring {
    position: absolute;
    width: 100%;
    height: 100%;
    border: 2px solid #ff4444;
    border-radius: 50%;
    animation: realtimePulse 2s cubic-bezier(0.25, 0.46, 0.45, 0.94) infinite;
    opacity: 0.6;
  }

  .inner-dot {
    position: relative;
    width: 8px;
    height: 8px;
    background: radial-gradient(circle, #ff4444 0%, #cc0000 50%, #990000 100%);
    border-radius: 50%;
    box-shadow:
      0 0 6px #ff4444,
      0 0 12px #ff4444;
    animation: realtimeBlink 1.5s ease-in-out infinite alternate;
  }
}

@keyframes realtimePulse {
  0% {
    transform: scale(1);
    opacity: 0.6;
  }
  50% {
    transform: scale(1.5);
    opacity: 0.3;
  }
  100% {
    transform: scale(2);
    opacity: 0;
  }
}

@keyframes realtimeBlink {
  0% {
    box-shadow:
      0 0 6px #ff4444,
      0 0 12px #ff4444;
    filter: brightness(1);
  }
  100% {
    box-shadow:
      0 0 10px #ff6666,
      0 0 20px #ff6666;
    filter: brightness(1.3);
  }
}

/* 移除 markerPulse 动画
@keyframes markerPulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.8;
  }
}
*/

@keyframes emergencyBlink {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.7;
    transform: scale(1.1);
  }
}
</style>
