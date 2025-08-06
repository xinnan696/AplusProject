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
  isPriorityTrackingOpen?: boolean // 新增：紧急车辆追踪面板是否打开
}>()

const emit = defineEmits<{
  (e: 'signalLightClicked', junctionId: string): void
  (e: 'trafficLightCleared'): void
  (e: 'emergencyApproved', vehicleId: string): void
  (e: 'emergencyRejected', vehicleId: string): void
}>()

// 紧急车辆弹窗相关状态
const isEmergencyDialogVisible = ref(false)
const junctionIdToNameMap = ref<Record<string, string>>({})
const showEmergencyDialog = ref(false) // 新增：控制弹窗显示的独立状态

// 延迟初始化 emergency store
let emergencyStore: ReturnType<typeof useEmergencyStore> | null = null

const getEmergencyStore = () => {
  if (!emergencyStore) {
    emergencyStore = useEmergencyStore()
  }
  return emergencyStore
}

// 计算属性
const firstPendingVehicle = computed(() => {
  try {
    const store = getEmergencyStore()
    return store.pendingVehicles.length > 0 ? store.pendingVehicles[0] : null
  } catch (error) {
    return null
  }
})

// 计算是否有待处理的紧急车辆
const hasPendingEmergencyVehicles = computed(() => {
  try {
    const store = getEmergencyStore()
    return store.pendingVehicles.length > 0
  } catch (error) {
    return false
  }
})

// 监听紧急车辆待处理列表变化 - 只用于图标闪烁，不自动弹窗
watch(() => {
  try {
    return getEmergencyStore().pendingVehicles.length
  } catch (error) {
    return 0
  }
}, (newLength, oldLength) => {
  console.log('🚨 [Map] 紧急车辆数量变化:', { newLength, oldLength })
  // 移除自动弹窗逻辑，改为由header图标点击触发
  if (newLength === 0) {
    // 如果没有待处理车辆了，关闭弹窗
    isEmergencyDialogVisible.value = false
    showEmergencyDialog.value = false
  }
})

// 监听紧急车辆侧边栏状态变化（可选的清理逼辑）
watch(() => {
  const props = getCurrentInstance()?.props as { isPriorityTrackingOpen?: boolean } | undefined
  return props?.isPriorityTrackingOpen ?? false
}, (isOpen) => {
  console.log('📱 [Map] 紧急车辆侧边栏状态变化:', isOpen)

  // 可以在这里添加一些可选的清理逻辑，但不强制清除路线
  // if (!isOpen && emergencyRouteLayer) {
  //   console.log('🧹 [Map] 侧边栏关闭，可选清除紧急路线显示')
  //   emergencyRouteLayer.getSource()?.clear()
  // }
})

// 监听所有紧急车辆数据变化（包括待处理和已批准的）
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
  console.log('🔄 [Map] 紧急车辆全部数据变化检测')
  console.log('  待处理车辆:', newData.pendingVehicles.length)
  console.log('  已批准车辆:', Object.keys(newData.vehicleDataMap).length)
  console.log('  当前追踪车辆:', newData.activelyTrackedVehicle?.vehicleID)

  // 首先清除所有旧路线，然后根据新数据决定是否显示
  if (emergencyRouteLayer) {
    emergencyRouteLayer.getSource()?.clear()
    console.log('🧹 [Map] 先清除所有旧路线')
  }

  // 更新车辆标记（只更新已批准的车辆）
  updateEmergencyVehicleMarkersFromStore()

  // 显示预定路线的逻辑：只显示已批准的车辆路线
  let vehicleIdToShow = null
  let shouldClearRoute = false
  
  // 1. 优先显示当前正在追踪的车辆
  if (newData.activelyTrackedVehicle && newData.activelyTrackedVehicle.vehicleID) {
    vehicleIdToShow = newData.activelyTrackedVehicle.vehicleID
    console.log('🎯 [Map] 显示正在追踪车辆的路线:', vehicleIdToShow)
  }
  // 2. 其次显示已批准车辆的路线
  else if (Object.keys(newData.vehicleDataMap).length > 0) {
    vehicleIdToShow = Object.keys(newData.vehicleDataMap)[0]
    console.log('🛣️ [Map] 显示已批准车辆的路线:', vehicleIdToShow)
  }
  // 3. 如果只有待处理车辆，不显示路线，并主动清除
  else if (newData.pendingVehicles.length > 0) {
    shouldClearRoute = true
    console.log('🚫 [Map] 只有待处理车辆，不显示路线，主动清除之前的路线')
  }
  // 4. 没有任何车辆时也要清除路线
  else {
    shouldClearRoute = true
    console.log('🧹 [Map] 没有任何紧急车辆，清除路线')
  }

  // 显示预定路线（只有在有已批准车辆时才显示）
  if (vehicleIdToShow) {
    console.log('✅ [Map] 显示新路线:', vehicleIdToShow)
    showPlannedRoute(vehicleIdToShow)
  } else {
    console.log('ℹ️ [Map] 没有需要显示的路线（已在开头清除）')
  }
}, { deep: true })

// 监听侧边栏状态（仅作为补充，不干扰主要逻辑）
watch(() => {
  const props = getCurrentInstance()?.props as { isPriorityTrackingOpen?: boolean } | undefined
  return props?.isPriorityTrackingOpen ?? false
}, (isOpen) => {
  console.log('📱 [Map] 紧急车辆侧边栏状态变化:', isOpen)
  // 不在这里处理路线显示，由上面的 vehicleDataMap 监听器处理
})

// 显示紧急车辆对话框 - 供header调用
const showEmergencyRequestDialog = () => {
  console.log('🚨 [Map] 显示紧急车辆请求对话框')
  if (hasPendingEmergencyVehicles.value) {
    isEmergencyDialogVisible.value = true
    showEmergencyDialog.value = true
  } else {
    console.warn('⚠️ [Map] 没有待处理的紧急车辆请求')
  }
}

// 隐藏紧急车辆对话框
const hideEmergencyRequestDialog = () => {
  console.log('🔒 [Map] 隐藏紧急车辆请求对话框')
  isEmergencyDialogVisible.value = false
  showEmergencyDialog.value = false
}

// 处理紧急车辆批准
const handleApprove = (vehicleId: string) => {
  console.log('✅ [Map] 批准紧急车辆请求:', vehicleId)

  try {
    const store = getEmergencyStore()

    // 调用store的批准方法
    store.approveVehicle(vehicleId)

    // 强制检查pending列表长度
    console.log('📊 [Map] 批准后pending车辆数量:', store.pendingVehicles.length)

    // 如果没有pending车辆了，关闭弹窗
    if (store.pendingVehicles.length === 0) {
      hideEmergencyRequestDialog()
      console.log('🔒 [Map] 关闭弹窗 - 没有更多pending车辆')
    }

    // 显示预定路线
    console.log('🛣️ [Map] 批准后尝试显示预定路线')
    // 直接调用，不等待 nextTick
    showPlannedRoute(vehicleId)

    // 发送事件给父组件，用于打开侧边栏进入紧急事件管理
    emit('emergencyApproved', vehicleId)
  } catch (error) {
    console.error('[Map] HandleApprove error:', error)
    // 出错时也关闭弹窗
    hideEmergencyRequestDialog()
  }
}

// 处理紧急车辆拒绝
const handleReject = (vehicleId: string) => {
  console.log('❌ [Map] 拒绝紧急车辆请求:', vehicleId)

  try {
    const store = getEmergencyStore()
    store.rejectVehicle(vehicleId)

    // 强制检查pending列表长度
    console.log('📊 [Map] 拒绝后pending车辆数量:', store.pendingVehicles.length)

    // 如果没有pending车辆了，关闭弹窗
    if (store.pendingVehicles.length === 0) {
      hideEmergencyRequestDialog()
      console.log('🔒 [Map] 关闭弹窗 - 没有更多pending车辆')
    }

    emit('emergencyRejected', vehicleId)
  } catch (error) {
    console.error('[Map] HandleReject error:', error)
    // 出错时也关闭弹窗
    hideEmergencyRequestDialog()
  }
}

// 获取路口名称的函数
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
  highlightLanes.value = null // 清除高亮车道
  rerenderTlsOverlays()
  vectorLayer?.changed() // 更新车道样式
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

// 其他必要的变量和函数声明
let allCoordinates: number[][] = []
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

// 基本的必要函数
const showTrafficStatus = computed(() => {
  const hasJunction = !!selectedJunctionForStatus.value
  const hasDirection = selectedDirectionIndex.value !== null && selectedDirectionIndex.value !== undefined

  const shouldShow = hasJunction && hasDirection

  console.log('🔍 [Map] showTrafficStatus computed:', {
    hasJunction,
    hasDirection,
    junctionId: selectedJunctionForStatus.value?.junction_id,
    junctionName: selectedJunctionForStatus.value?.junction_name,
    directionIndex: selectedDirectionIndex.value,
    shouldShow
  })

  return shouldShow
})

const getTrafficLightDataForStatusBar = () => {
  if (currentTrafficLightData.value) {
    console.log('📊 [Map] Returning current traffic light data:', currentTrafficLightData.value)
    return currentTrafficLightData.value
  }

  // 如果没有当前数据，尝试从全部数据中获取
  if (selectedJunctionForStatus.value) {
    const junctionId = selectedJunctionForStatus.value.junction_id
    const tlsId = junctionIdToTlsIdMap.value.get(junctionId)

    if (tlsId && allTrafficLightData.value.has(tlsId)) {
      const data = allTrafficLightData.value.get(tlsId)
      console.log('📊 [Map] Returning cached traffic light data:', data)
      return data
    }
  }

  console.log('⚠️ [Map] No traffic light data available')
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
    console.error('😱 [StatusBar] Position calculation error:', error)
    return { display: 'none' }
  }
}

// 状态栏位置更新相关变量
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
  return userManagedAreas.value
}

const isJunctionControllable = (junctionName: string): boolean => {
  if (authStore.isAdmin()) return true
  return true // 简化实现
}

// 基本的UI函数
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

const handleSwitchChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const mode = target.checked ? 'full' : 'restricted'
  viewMode.value = mode
}

const updateSearchSuggestions = () => {
  // 简化实现
  searchSuggestions.value = []
  showSuggestions.value = false
}

const selectSuggestion = (suggestion: string) => {
  searchInput.value = suggestion
  showSuggestions.value = false
}

const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    showSuggestions.value = false
  }
}

const searchJunction = () => {
  // 简化实现
  console.log('搜索路口:', searchInput.value)
}

// 完整的地图加载
const loadLaneData = async () => {
  try {
    console.log('🗺️ [Map] 开始加载车道数据...')
    console.log('🗺️ [Map] API基础URL检查:', import.meta.env.VITE_API_BASE_URL || 'using default')

    // 检查地图容器
    if (!mapRef.value) {
      throw new Error('地图容器未找到')
    }
    console.log('✅ [Map] 地图容器检查通过')

    // 获取车道数据
    console.log('📡 [Map] 请求车道数据: /api-status/lane-mappings')
    const res = await axios.get('/api-status/lane-mappings')
    const data = res.data as LaneMapping[]

    console.log('📊 [Map] 车道数据加载完成，数量:', data.length)

    if (!data || data.length === 0) {
      throw new Error('车道数据为空')
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

      // 构建边缘坐标映射（用于实时车辆定位）
      if (!edgeCoordinatesMap.has(lane.edgeId)) {
        edgeCoordinatesMap.set(lane.edgeId, coordinates)
      }
    })

    console.log('📏 [Map] 坐标范围:', { minX, maxX, totalCoordinates: allCoordinates.length })

    const mapCenterX = (minX + maxX) / 2

    // 创建地图特征
    const features = data.map((lane: LaneMapping) => {
      const coordinates = lane.laneShape.trim().split(' ').map(p => p.split(',').map(Number))
      laneToEdgeMap.set(lane.laneId, lane.edgeId)

      return {
        type: 'Feature',
        geometry: { type: 'LineString', coordinates },
        properties: { laneId: lane.laneId, edgeName: lane.edgeName }
      }
    })

    console.log('🎯 [Map] 地图特征创建完成，数量:', features.length)

    // 创建矢量数据源
    const vectorSource = new VectorSource({
      features: new GeoJSON().readFeatures(
        { type: 'FeatureCollection', features },
        { dataProjection: 'EPSG:3857', featureProjection: 'EPSG:3857' }
      )
    })

    console.log('🗂️ [Map] 矢量数据源创建完成')

    // 创建矢量图层
    vectorLayer = new VectorLayer({
      source: vectorSource,
      style: feature => {
        const laneId = feature.get('laneId')
        const coordinates = feature.getGeometry()?.getCoordinates() as number[][] | undefined

        if (!coordinates) return null

        const edgeId = laneToEdgeMap.get(laneId)
        const count = edgeId ? vehicleCountMap.value[edgeId] ?? 0 : 0

        let color = '#00B4D8'  // 默认蓝色
        let width = 2.5
        let isAnimated = false
        let isDynamic = false

        // 检查该车道是否属于选中路口的某个方向
        const isSelectedDirectionLane = isLaneInSelectedDirection(laneId)
        // 检查该车道是否是绿灯方向且不拥堵
        const isGreenLane = isLaneInGreenDirection(laneId)

        // 根据车辆数量确定拥堵状态
        const isCongested = count >= 4

        // 动态逻辑：绿灯且不拥堵 = 动态（绿色动画）
        if (isGreenLane && !isCongested) {
          color = '#4CAF50'  // 绿色
          isDynamic = true
          isAnimated = true
          width = 3
        }
        // 静态灰色逻辑：选中方向但红灯，或者拥堵的选中车道
        else if (isSelectedDirectionLane) {
          color = '#666666' // 灰色 - 静态状态
          width = 3
          isDynamic = false
          isAnimated = false
        }
        // 正常车道逻辑：根据拥堵情况显示
        else {
          if (count >= 7) {
            color = '#D9001B' // 红色 - 严重拥堵
            width = 3.5
          } else if (count >= 4) {
            color = '#F59A23' // 橙色 - 中度拥堵
            width = 3
          } else if (count > 0) {
            color = '#FFFF00' // 黄色 - 轻度拥堵
            width = 2.5
          }
          // 其他车道保持默认蓝色
        }

        // 创建样式
        const styles = []

        // 如果是动态车道，添加动画效果
        if (isAnimated) {
          // 动画背景层
          styles.push(new Style({
            stroke: new Stroke({
              color: color + '30',
              width: width + 4,
              lineCap: 'round',
              lineJoin: 'round'
            })
          }))

          // 主要动画层
          styles.push(new Style({
            stroke: new Stroke({
              color: color,
              width,
              lineCap: 'round',
              lineJoin: 'round',
              lineDash: [10, 10], // 虚线效果用于动画
              lineDashOffset: animationOffset
            })
          }))
        } else {
          // 静态车道样式
          if (count > 0) {
            // 拥堵车道有阴影效果
            styles.push(new Style({
              stroke: new Stroke({
                color: '#000000' + Math.floor(48).toString(16).padStart(2, '0'),
                width: width + 3,
                lineCap: 'round',
                lineJoin: 'round'
              })
            }))
          }

          styles.push(new Style({
            stroke: new Stroke({
              color: color,
              width,
              lineCap: 'round',
              lineJoin: 'round'
            })
          }))
        }

        return styles.length > 0 ? styles : null
      }
    })

    console.log('🎨 [Map] 矢量图层创建完成')

    // 获取地图范围
    const extent = vectorSource.getExtent()
    const center = getCenter(extent)

    console.log('📐 [Map] 地图范围:', { extent, center })

    // 创建视图
    view = new OLView({
      center,
      zoom: 15,
      minZoom: 13,
      maxZoom: 18,
      extent: [extent[0] - 100, extent[1] - 100, extent[2] + 100, extent[3] + 100]
    })

    console.log('👁️ [Map] 视图创建完成')

    // 设置地图视图
    if (map) {
      map.setView(view)
      console.log('✅ [Map] 地图视图设置完成')
    } else {
      throw new Error('地图实例不存在')
    }

    // 添加图层
    map.addLayer(vectorLayer)
    console.log('🗺️ [Map] 图层添加完成')

    // 适配到地图范围
    if (!hasFitted) {
      view.fit(extent, { padding: [20, 20, 20, 20], duration: 300 })
      hasFitted = true
      console.log('🎯 [Map] 视图适配完成')
    }

    console.log('✅ [Map] 地图初始化完成')

    // 加载路口数据
    await loadJunctionData()

    // 设置地图交互事件
    setupMapInteractions()

    // 设置视图监听器
    setupViewWatchers()

  } catch (error) {
    console.error('❌ [Map] 地图加载失败:', error)
    console.error('错误详情:', {
      message: error.message,
      stack: error.stack,
      mapRef: !!mapRef.value,
      map: !!map
    })
  }
}

// 加载路口数据
const loadJunctionData = async () => {
  try {
    console.log('🚦 [Map] 开始加载路口数据...')

    // 获取路口名称映射
    const nameRes = await axios.get('/api-status/junctions')
    const raw = nameRes.data as Record<string, { junction_id: string; junction_name: string }>
    const junctionNameMap = new Map<string, string>()

    for (const tlsId in raw) {
      const item = raw[tlsId]
      if (item.junction_id && item.junction_name) {
        junctionNameMap.set(item.junction_id, item.junction_name)
      }
    }

    // 获取路口位置数据
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

    console.log('✅ [Map] 路口数据加载完成，数量:', junctions.length)

    // 获取初始交通灯数据
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
      console.log('✅ [Map] 初始交通灯数据加载完成')
    } catch (error) {
      console.warn('⚠️ [Map] 初始交通灯数据加载失败:', error)
    }

    // 渲染交通灯
    rerenderTlsOverlays()

    // 启动WebSocket连接
    connectWebSocket()

    // 初始化实时车辆追踪器
    initVehicleTracker()

  } catch (error) {
    console.error('❌ [Map] 路口数据加载失败:', error)
  }
}

// 判断车道是否属于选中方向（不管灯色）
const isLaneInSelectedDirection = (laneId: string): boolean => {
  // 必须同时有选中的路口和方向（即有 fromLanes/toLanes）
  if (!selectedJunctionForStatus.value ||
      selectedDirectionIndex.value === null ||
      selectedDirectionIndex.value === undefined ||
      !highlightLanes.value) {
    return false
  }

  // 检查该车道是否在 fromLanes 或 toLanes 中
  const isInFromLanes = highlightLanes.value.fromLanes.includes(laneId)
  const isInToLanes = highlightLanes.value.toLanes.includes(laneId)

  return isInFromLanes || isInToLanes
}

// 判断车道是否属于选中路口的绿灯方向
const isLaneInGreenDirection = (laneId: string): boolean => {
  // 必须同时有选中的路口和方向（即有 fromLanes/toLanes）
  if (!selectedJunctionForStatus.value ||
      selectedDirectionIndex.value === null ||
      selectedDirectionIndex.value === undefined ||
      !highlightLanes.value) {
    return false
  }

  // 检查选中方向是否为绿灯
  const currentLight = getCurrentTrafficLight(selectedJunctionForStatus.value.junction_id)
  if (currentLight !== 'green') {
    return false
  }

  // 检查该车道是否在 fromLanes 或 toLanes 中
  const isInFromLanes = highlightLanes.value.fromLanes.includes(laneId)
  const isInToLanes = highlightLanes.value.toLanes.includes(laneId)
  const isInDirection = isInFromLanes || isInToLanes

  // 调试信息（仅对选中方向的车道输出）
  if (isInDirection) {
    console.log('🚦 [Map] 车道绿灯检查:', {
      laneId,
      currentLight,
      isGreen: currentLight === 'green',
      isInFromLanes,
      isInToLanes
    })
  }

  return isInDirection
}

// 判断车道是否靠近选中路口
const isLaneNearSelectedJunction = (laneId: string): boolean => {
  if (!selectedJunctionForStatus.value) {
    return false
  }

  // 获取选中路口的坐标
  const junction = selectedJunctionForStatus.value
  const junctionX = junction.junctionX
  const junctionY = junction.junctionY

  // 获取车道的几何信息
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

  // 获取车道的坐标点
  const coordinates = geometry.getCoordinates() as number[][]

  // 检查车道的任何一点是否在路口附近（比如100米范围内）
  const maxDistance = 100 // 米

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

// 启动车道动画
const startRoadAnimation = () => {
  // 避免重复启动
  if (roadAnimationTimer) {
    return
  }

  console.log('🎬 [Map] 启动车道动画')
  let frameCount = 0
  roadAnimationTimer = setInterval(() => {
    animationOffset -= 1 // 降低动画速度，让动画更温和
    if (animationOffset <= -20) {
      animationOffset = 0
    }

    frameCount++
    if (frameCount % 20 === 0) { // 每秒输出一次调试信息
      console.log('🔄 [Map] 动画帧:', frameCount, '偏移量:', animationOffset)
    }

    // 更新图层样式
    vectorLayer?.changed()
  }, 50) // 提高帧率到 50ms 更新一次
}

// 停止车道动画
const stopRoadAnimation = () => {
  if (roadAnimationTimer) {
    console.log('⏹️ [Map] 停止车道动画')
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

// WebSocket 连接
const connectWebSocket = () => {
  try {
    const wsProtocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${wsProtocol}//localhost:8087/api/status/ws`

    console.log('🔗 [Map] 连接WebSocket:', wsUrl)
    ws = new WebSocket(wsUrl)

    ws.onopen = () => {
      console.log('✅ [Map] WebSocket连接成功')
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
              // 检查是否是当前选中路口的交通灯更新
              if (selectedJunctionForStatus.value) {
                const selectedTlsId = junctionIdToTlsIdMap.value.get(selectedJunctionForStatus.value.junction_id)
                if (tlsId === selectedTlsId) {
                  hasTrafficLightUpdate = true
                }
              }

              allTrafficLightData.value.set(tlsId, parsedData)
            }
          }

          // 更新当前选中路口的交通灯数据
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
                  // 忽略解析错误
                }
              } else {
                parsedData = tlsData
              }

              if (parsedData && typeof parsedData.state === 'string') {
                currentTrafficLightData.value = parsedData
              }
            }
          }

          // 重新渲染交通灯和车道
          rerenderTlsOverlays()

          // 如果有选中路口的交通灯更新，更新车道样式
          if (hasTrafficLightUpdate && highlightLanes.value) {
            console.log('🚦 [Map] 选中路口交通灯更新，刷新车道样式')
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
              // 忽略解析错误
            }
          }

          vehicleCountMap.value = newMap
          // 减少频繁更新，避免干扰动画
          // vectorLayer?.changed()
        }

      } catch (error) {
        console.error('WebSocket消息解析失败:', error)
      }
    }

    ws.onerror = (error) => {
      console.error('❌ [Map] WebSocket错误:', error)
    }

    ws.onclose = () => {
      console.warn('⚠️ [Map] WebSocket连接断开')
      if (reconnectAttempts < maxReconnectAttempts) {
        reconnectAttempts++
        reconnectTimer = setTimeout(connectWebSocket, 3000)
      }
    }
  } catch (error) {
    console.error('❌ [Map] WebSocket创建失败:', error)
    if (reconnectAttempts < maxReconnectAttempts) {
      reconnectAttempts++
      reconnectTimer = setTimeout(connectWebSocket, 3000)
    }
  }
}

const rerenderTlsOverlays = () => {
  // 清除之前的覆盖层
  tlsOverlays.forEach(overlay => {
    map?.removeOverlay(overlay)
  })
  tlsOverlays.length = 0

  console.log('🚦 [Map] 开始渲染交通灯覆盖层...')

  // 为每个路口创建交通灯图标
  Array.from(junctionMap.entries()).forEach(([junctionName, junction]) => {
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

    // 使用 Vue 组件创建交通灯图标
    const app = createApp(TrafficLightIcon, {
      currentLight: currentLight,
      isSelected: isFullySelected,
      isPartiallySelected: isJunctionOnly,
      isControllable: isControllable,
      showAllLights: false,
      isEmergencyUpcoming: false
    })

    app.mount(containerEl)

    // 添加点击事件
    containerEl.addEventListener('click', (e) => {
      e.stopPropagation()
      handleTrafficLightClick(junctionName, junction)
    })

    // 创建覆盖层
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

  console.log('🚦 [Map] 交通灯覆盖层渲染完成，数量:', tlsOverlays.length)
}

// 处理交通灯点击
const handleTrafficLightClick = async (junctionName: string, junction: Junction) => {
  console.log('🚦 [Map] 交通灯被点击:', junctionName)

  selectedJunctionName.value = junctionName
  selectedJunctionForStatus.value = junction
  selectedDirectionIndex.value = null

  // 缩放到路口
  const currentView = map?.getView()
  if (currentView) {
    currentView.animate({
      center: [junction.junctionX, junction.junctionY],
      zoom: 18,
      duration: 1000
    })
  }

  emit('signalLightClicked', junction.junction_id)

  const controllableText = isJunctionControllable(junctionName) ? '' : ' (Read-only)'
  currentLocation.value = `${junctionName}${controllableText}`

  // 重新渲染交通灯以更新选中状态
  rerenderTlsOverlays()
}

// 根据路口ID缩放到路口
const zoomToJunctionById = (junctionId: string) => {
  console.log('🎯 [Map] 根据ID缩放到路口:', junctionId)

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
        duration: 1000
      })

      selectedJunctionName.value = junctionName
      rerenderTlsOverlays()

      const controllableText = isJunctionControllable(junctionName) ? '' : ' (Read-only)'
      currentLocation.value = `${junctionName}${controllableText}`
    }
  } else {
    console.warn('⚠️ [Map] 未找到路口ID:', junctionId)
  }
}

// 根据路口名称缩放到路口
const zoomToJunction = (junctionName: string) => {
  console.log('🎯 [Map] 根据名称缩放到路口:', junctionName)

  const junction = junctionMap.get(junctionName)
  if (junction && map) {
    const currentView = map.getView()
    if (currentView) {
      selectedJunctionName.value = null
      rerenderTlsOverlays()

      currentView.animate({
        center: [junction.junctionX, junction.junctionY],
        zoom: Math.max(currentView.getZoom() || 15, 16),
        duration: 1000
      })

      selectedJunctionName.value = junctionName
      rerenderTlsOverlays()

      const controllableText = isJunctionControllable(junctionName) ? '' : ' (Read-only)'
      currentLocation.value = `${junctionName}${controllableText}`
    }
  } else {
    console.warn('⚠️ [Map] 未找到路口:', junctionName)
  }
}

// 设置选中的交通灯和方向
const setSelectedTrafficLight = (junctionId: string, directionIndex: number, options: { disableZoom?: boolean } = {}) => {
  console.log('🔍 [Map] 设置选中的交通灯:', { junctionId, directionIndex, options })

  const junction = Array.from(junctionMap.values()).find(j => j.junction_id === junctionId)
  const junctionName = Array.from(junctionMap.entries()).find(([name, j]) => j.junction_id === junctionId)?.[0]

  if (junction && junctionName) {
    selectedJunctionForStatus.value = junction
    selectedDirectionIndex.value = directionIndex
    selectedJunctionName.value = junctionName

    console.log('✅ [Map] 交通灯状态设置完成:', {
      junctionId: selectedJunctionForStatus.value.junction_id,
      junctionName: selectedJunctionForStatus.value.junction_name,
      directionIndex: selectedDirectionIndex.value
    })

    // 立即获取交通灯数据
    fetchTrafficLightData(junction.junction_id || junction.junctionId)

    // 重新渲染交通灯覆盖层
    rerenderTlsOverlays()

    // 只有在未禁用zoom的情况下才缩放到路口
    if (!options.disableZoom) {
      const currentView = map?.getView()
      if (currentView) {
        currentView.animate({
          center: [junction.junctionX, junction.junctionY],
          zoom: Math.max(currentView.getZoom() || 15, 16),
          duration: 1000
        })
      }
    }

    // 强制更新状态栏位置
    nextTick(() => {
      updateStatusBarPosition()
    })
  } else {
    console.warn('⚠️ [Map] 未找到路口ID:', junctionId)
  }
}

// 只设置选中的路口（不设置方向）
const setSelectedJunctionOnly = (junctionId: string) => {
  console.log('🔍 [Map] 只设置选中路口:', { junctionId })

  const junction = Array.from(junctionMap.values()).find(j => j.junction_id === junctionId)
  const junctionName = Array.from(junctionMap.entries()).find(([name, j]) => j.junction_id === junctionId)?.[0]

  if (junction && junctionName) {
    selectedJunctionForStatus.value = junction
    selectedDirectionIndex.value = null
    selectedJunctionName.value = junctionName

    console.log('✅ [Map] 路口选择设置完成:', {
      junctionId: selectedJunctionForStatus.value.junction_id,
      junctionName: selectedJunctionForStatus.value.junction_name,
      directionIndex: selectedDirectionIndex.value
    })

    // 获取交通灯数据
    fetchTrafficLightData(junction.junctionId)

    // 重新渲染交通灯覆盖层
    rerenderTlsOverlays()
  } else {
    console.warn('⚠️ [Map] 未找到路口ID:', junctionId)
  }
}

// 获取交通灯数据
const fetchTrafficLightData = async (junctionId: string) => {
  try {
    console.log('📡 [Map] 获取交通灯数据:', junctionId)

    const tlsResponse = await axios.get('/api-status/tls-junctions')
    const tlsJunction = tlsResponse.data.find((tls: any) => {
      return tls.junctionId === junctionId ||
             tls.junction_id === junctionId ||
             tls.tlsId === junctionId
    })

    if (!tlsJunction) {
      console.warn('⚠️ [Map] 未找到TLS路口:', junctionId)
      return
    }

    const tlsId = tlsJunction.tlsId
    junctionIdToTlsIdMap.value.set(junctionId, tlsId)

    // 获取当前交通灯状态
    const junctionResponse = await axios.get('/api-status/junctions')
    const junctionData = junctionResponse.data[tlsId]

    if (junctionData) {
      let parsedData = junctionData
      if (typeof junctionData === 'string') {
        try {
          parsedData = JSON.parse(junctionData)
        } catch (parseError) {
          console.error('解析交通灯数据失败:', parseError)
          return
        }
      }

      currentTrafficLightData.value = parsedData
      allTrafficLightData.value.set(tlsId, parsedData)

      console.log('✅ [Map] 交通灯数据获取成功:', parsedData)
    }
  } catch (error) {
    console.error('❌ [Map] 获取交通灯数据失败:', error)
  }
}

// 显示预定路线（移除侧边栏检查，直接显示）
const showPlannedRoute = async (vehicleId: string) => {
  console.log('🚗 [Map] 请求显示预定路线:', vehicleId)

  try {
    // 使用正确的API路径获取预定路线数据
    const response = await axios.get('/api-status/emergency-routes')
    const routes = response.data

    console.log('📍 [Map] 获取到路线数据:', routes)

    // 找到对应车辆的路线
    const vehicleRoute = routes.find((route: any) => {
      // 尝试多种匹配方式
      return route.vehicle_id === vehicleId ||
             route.vehicleId === vehicleId ||
             route.event_id === vehicleId ||
             route.eventId === vehicleId
    })

    console.log('🔍 [Map] 查找车辆路线结果:', {
      vehicleId,
      totalRoutes: routes.length,
      foundRoute: !!vehicleRoute,
      routeData: vehicleRoute
    })

    if (vehicleRoute && vehicleRoute.route_edges) {
      console.log('📍 [Map] 找到车辆路线，路段数量:', vehicleRoute.route_edges.length)

      // 创建路线图层（简单白色虚线样式）
      if (!emergencyRouteLayer) {
        emergencyRouteLayer = new VectorLayer({
          source: new VectorSource(),
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
        console.log('✅ [Map] 创建紧急路线图层（白色虚线样式）')
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
          console.log(`🗺️ [Map] 添加路段: ${edgeId}`)
        } else {
          console.warn(`⚠️ [Map] 未找到路段 ${edgeId} 的坐标数据`)
        }
      })

      if (routeFeatures.length > 0) {
        emergencyRouteLayer.getSource()?.addFeatures(routeFeatures)
        console.log('✅ [Map] 预定路线显示完成，路段数量:', routeFeatures.length)
        // 移除自动缩放功能，让用户手动控制地图视图
      } else {
        console.warn('⚠️ [Map] 没有有效的路段坐标，无法显示路线')
      }
    } else {
      console.warn('⚠️ [Map] 未找到车辆路线数据或路线为空:', vehicleId)
    }
  } catch (error) {
    console.error('❌ [Map] 显示预定路线失败:', error)
  }
}

// 初始化实时车辆追踪器
const initVehicleTracker = () => {
  console.log('🚑 [Map] 初始化实时车辆追踪器...')

  try {
    vehicleTracker = new EmergencyVehicleTracker()

    // 监听车辆追踪数据
    vehicleTracker.onMessage((data) => {
      console.log('🚑 [Map] 收到实时车辆数据:', data)
      console.log('🚑 [Map] 数据类型:', typeof data)

      let hasUpdate = false

      // 后端发送的数据格式是 Map<vehicleId, vehicleJsonString>
      // 需要解析每个车辆的JSON字符串
      if (data && typeof data === 'object') {
        console.log('🚑 [Map] 开始处理车辆数据，车辆数量:', Object.keys(data).length)

        // 清空之前的数据
        realtimeVehicles.value = {}
        emergencyVehicles.value = {}

        Object.entries(data).forEach(([vehicleId, vehicleJsonString]) => {
          try {
            // 解析车辆JSON数据
            const vehicleData = typeof vehicleJsonString === 'string'
              ? JSON.parse(vehicleJsonString)
              : vehicleJsonString

            console.log(`🚗 [Map] 处理车辆 ${vehicleId}:`, vehicleData)

            // 检查车辆数据是否有位置信息
            if (vehicleData && vehicleData.position) {
              // 判断是否为紧急车辆（根据eventID或其他标识）
              if (vehicleData.eventID || vehicleData.isEmergency) {
                emergencyVehicles.value[vehicleId] = vehicleData
                console.log(`🚑 [Map] 添加紧急车辆: ${vehicleId}`)
              } else {
                realtimeVehicles.value[vehicleId] = vehicleData
                console.log(`🚗 [Map] 添加普通车辆: ${vehicleId}`)
              }
              hasUpdate = true
            } else {
              console.warn(`⚠️ [Map] 车辆 ${vehicleId} 没有位置数据:`, vehicleData)
            }
          } catch (parseError) {
            console.error(`❌ [Map] 解析车辆 ${vehicleId} 数据失败:`, parseError, vehicleJsonString)
          }
        })
      }

      if (hasUpdate) {
        console.log('🔄 [Map] 触发车辆标记更新，紧急车辆:', Object.keys(emergencyVehicles.value).length, '，普通车辆:', Object.keys(realtimeVehicles.value).length)
        updateVehicleMarkers()
      } else {
        console.warn('⚠️ [Map] 没有有效的车辆数据需要更新')
      }
    })

    console.log('✅ [Map] 实时车辆追踪器初始化完成')
  } catch (error) {
    console.error('❌ [Map] 实时车辆追踪器初始化失败:', error)
  }
}

// 从Emergency Store更新紧急车辆标记
const updateEmergencyVehicleMarkersFromStore = () => {
  console.log('🚑 [Map] 从Emergency Store更新车辆标记...')

  try {
    const store = getEmergencyStore()
    const storeVehicles = store.vehicleDataMap || {}

    console.log('📊 [Map] Store中的车辆数据:', {
      vehicleCount: Object.keys(storeVehicles).length,
      vehicles: storeVehicles
    })

    // 将store中的数据同步到本地状态
    emergencyVehicles.value = { ...storeVehicles }

    // 更新标记
    updateVehicleMarkers()

  } catch (error) {
    console.error('❌ [Map] 从Emergency Store更新车辆标记失败:', error)
  }
}

// 更新车辆标记
const updateVehicleMarkers = () => {
  console.log('🚗 [Map] 开始更新车辆标记...')

  if (!map) {
    console.warn('⚠️ [Map] 地图实例不存在，无法更新车辆标记')
    return
  }

  console.log('📊 [Map] 当前车辆数据状态:', {
    emergencyVehiclesCount: Object.keys(emergencyVehicles.value).length,
    realtimeVehiclesCount: Object.keys(realtimeVehicles.value).length,
    emergencyVehicles: emergencyVehicles.value,
    realtimeVehicles: realtimeVehicles.value
  })

  // 清除之前的车辆覆盖层
  console.log('🧽 [Map] 清除现有车辆标记，数量:', vehicleOverlays.length)
  vehicleOverlays.forEach(overlay => {
    map?.removeOverlay(overlay)
  })
  vehicleOverlays.length = 0

  let markerCount = 0

  // 渲染紧急车辆标记
  Object.entries(emergencyVehicles.value).forEach(([vehicleId, vehicleData]) => {
    console.log('🚑 [Map] 处理紧急车辆:', vehicleId, vehicleData)

    if (vehicleData && vehicleData.position) {
      console.log('📍 [Map] 紧急车辆位置:', vehicleData.position)

      if (vehicleData.position.x && vehicleData.position.y) {
        createVehicleMarker(vehicleId, vehicleData, true)
        markerCount++
      } else {
        console.warn('⚠️ [Map] 紧急车辆位置数据无效:', vehicleData.position)
      }
    } else {
      console.warn('⚠️ [Map] 紧急车辆没有位置数据:', vehicleData)
    }
  })

  // 渲染普通实时车辆标记（红色闪烁点）
  Object.entries(realtimeVehicles.value).forEach(([vehicleId, vehicleData]) => {
    console.log('🚗 [Map] 处理普通车辆:', vehicleId, vehicleData)

    if (vehicleData && vehicleData.position) {
      console.log('📍 [Map] 普通车辆位置:', vehicleData.position)

      if (vehicleData.position.x && vehicleData.position.y) {
        createVehicleMarker(vehicleId, vehicleData, false)
        markerCount++
      } else {
        console.warn('⚠️ [Map] 普通车辆位置数据无效:', vehicleData.position)
      }
    } else {
      console.warn('⚠️ [Map] 普通车辆没有位置数据:', vehicleData)
    }
  })

  console.log('✅ [Map] 车辆标记更新完成，创建标记数量:', markerCount, '，紧急车辆:', Object.keys(emergencyVehicles.value).length, '，普通车辆:', Object.keys(realtimeVehicles.value).length)
}

// 创建车辆标记
const createVehicleMarker = (vehicleId: string, vehicleData: any, isEmergency: boolean = false) => {
  console.log(`🚗 [Map] 创建${isEmergency ? '紧急' : '普通'}车辆标记:`, vehicleId, vehicleData)

  if (!map) {
    console.warn('⚠️ [Map] 地图实例不存在，无法创建车辆标记')
    return
  }

  // 检查原始位置数据
  console.log('📍 [Map] 原始车辆位置数据:', vehicleData.position)

  // 获取地图的坐标系统信息
  const currentView = map.getView()
  if (currentView) {
    const extent = currentView.calculateExtent()
    console.log('🗺️ [Map] 当前地图范围:', extent)
    console.log('🗺️ [Map] 地图中心:', currentView.getCenter())
    console.log('🗺️ [Map] 地图缩放级别:', currentView.getZoom())
  }

  // 使用后端提供的原始坐标
  let position

  if (vehicleData.position.x !== undefined && vehicleData.position.y !== undefined) {
    // 格式1: {x: number, y: number} - 直接使用原始坐标
    position = [vehicleData.position.x, vehicleData.position.y]  // 直接使用x,y
    console.log('📍 [Map] 使用原始坐标 [x,y] 格式，位置:', position)
  } else if (vehicleData.position.lon !== undefined && vehicleData.position.lat !== undefined) {
    // 格式2: {lon: number, lat: number}
    position = [vehicleData.position.lon, vehicleData.position.lat]
    console.log('📍 [Map] 使用 lon,lat 格式，位置:', position)
  } else if (Array.isArray(vehicleData.position) && vehicleData.position.length >= 2) {
    // 格式3: [x, y] 数组
    position = vehicleData.position
    console.log('📍 [Map] 使用数组格式，位置:', position)
  } else {
    console.error('❌ [Map] 无法识别的位置数据格式:', vehicleData.position)
    return
  }

  console.log('📍 [Map] 最终使用的坐标:', position)

  // 检查坐标是否在合理范围内
  if (currentView) {
    const extent = currentView.calculateExtent()
    const [minX, minY, maxX, maxY] = extent

    // 扩大检查范围，允许车辆在地图边界外一定距离
    const bufferX = (maxX - minX) * 0.5  // 50%的缓冲区
    const bufferY = (maxY - minY) * 0.5

    console.log('🗺️ [Map] 地图范围检查:', {
      mapExtent: { minX, minY, maxX, maxY },
      vehiclePosition: position,
      bufferX, bufferY,
      inExtendedRange: {
        x: position[0] >= (minX - bufferX) && position[0] <= (maxX + bufferX),
        y: position[1] >= (minY - bufferY) && position[1] <= (maxY + bufferY)
      }
    })

    // 如果车辆位置完全超出扩展范围，可能需要坐标转换
    if (position[0] < (minX - bufferX) || position[0] > (maxX + bufferX) ||
        position[1] < (minY - bufferY) || position[1] > (maxY + bufferY)) {
      console.warn('⚠️ [Map] 车辆位置可能需要坐标转换:', {
        vehiclePos: position,
        mapRange: [minX, minY, maxX, maxY],
        suggestion: '检查是否需要从WGS84转换为Web Mercator或其他投影'
      })

      // 尝试简单的坐标变换（如果车辆坐标是经纬度格式）
      if (Math.abs(position[0]) <= 180 && Math.abs(position[1]) <= 90) {
        console.log('🌍 [Map] 检测到可能的经纬度坐标，尝试转换为Web Mercator')
        // 简单的经纬度到Web Mercator转换
        const lon = position[0]
        const lat = position[1]
        const x = lon * 20037508.34 / 180
        const y = Math.log(Math.tan((90 + lat) * Math.PI / 360)) / (Math.PI / 180) * 20037508.34 / 180
        position = [x, y]
        console.log('📍 [Map] 转换后的坐标:', position)
      }
    }

    // 如果车辆不在当前视图范围内，提示但不自动调整地图
    if (position[0] < minX || position[0] > maxX || position[1] < minY || position[1] > maxY) {
      console.log('📍 [Map] 车辆位置不在当前视图范围内，但不自动调整地图视图')
      // 移除自动缩放功能，让用户手动控制地图
    }
  }

  // 创建车辆标记容器
  const containerEl = document.createElement('div')
  containerEl.style.position = 'relative'
  containerEl.style.cursor = 'pointer'
  containerEl.dataset['vehicleId'] = vehicleId

  console.log(`🎨 [Map] 创建${isEmergency ? '紧急' : '普通'}车辆容器元素`)

  if (isEmergency) {
    // 直接使用EmergencyVehicleMarker组件
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
        mapPixelPosition: [0, 0], // 在Overlay中不需要像素位置
        showInfo: false
      })

      app.mount(containerEl)
      console.log('✅ [Map] EmergencyVehicleMarker组件挂载成功')

      // 添加点击事件
      containerEl.addEventListener('click', (e) => {
        e.stopPropagation()
        handleVehicleClick(vehicleId, vehicleData)
      })
    } catch (error) {
      console.error('❌ [Map] EmergencyVehicleMarker组件创建失败:', error)
      // 如果组件创建失败，使用简单的DOM元素
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
      console.log('✅ [Map] 使用简单DOM元素作为紧急车辆标记')
    }
  } else {
    // 普通车辆标记（红色闪烁点）
    const vehicleDot = document.createElement('div')
    vehicleDot.className = 'realtime-vehicle-marker'
    vehicleDot.innerHTML = `
      <div class="vehicle-dot">
        <div class="pulse-ring"></div>
        <div class="inner-dot"></div>
      </div>
    `
    vehicleDot.title = `Vehicle: ${vehicleId}`

    console.log('✅ [Map] 普通车辆标记元素创建成功')

    // 添加点击事件
    vehicleDot.addEventListener('click', (e) => {
      e.stopPropagation()
      handleVehicleClick(vehicleId, vehicleData)
    })

    containerEl.appendChild(vehicleDot)
  }

  // 创建覆盖层
  const overlay = new Overlay({
    element: containerEl,
    positioning: 'center-center',
    stopEvent: false,
    offset: [0, 0],
    position: position
  })

  console.log('🎨 [Map] 创建覆盖层，位置:', position)

  map.addOverlay(overlay)
  vehicleOverlays.push(overlay)

  console.log(`✅ [Map] ${isEmergency ? '紧急' : '普通'}车辆标记创建完成:`, vehicleId, '，总覆盖层数量:', vehicleOverlays.length)
}

// 处理车辆点击事件
const handleVehicleClick = (vehicleId: string, vehicleData: any) => {
  console.log('🚗 [Map] 车辆被点击:', vehicleId, vehicleData)

  // 可以在这里添加车辆详情显示逻辑
  currentLocation.value = `Vehicle: ${vehicleId}`

  // 不管是什么车辆，都尝试显示路线来调试坐标问题
  console.log('🛣️ [Map] 尝试显示路线进行坐标调试')
  showPlannedRoute(vehicleId)
}
const setupMapInteractions = () => {
  if (!map) return

  // 单击事件
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

      // 检查是否点击了交通灯附近
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
        // 清除选中状态
        selectedJunctionName.value = null
        selectedJunctionForStatus.value = null
        selectedDirectionIndex.value = null
        currentTrafficLightData.value = null

        rerenderTlsOverlays()
        emit('trafficLightCleared')
        currentLocation.value = 'No element selected'
      }
    }

    // 清除之前的标记
    markerOverlays.forEach(overlay => {
      map?.removeOverlay(overlay)
    })
    markerOverlays.length = 0

    // 添加点击标记
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

  // 右键菜单事件
  map.on('contextmenu', evt => {
    evt.preventDefault()
    selectedJunctionName.value = null
    selectedJunctionForStatus.value = null
    selectedDirectionIndex.value = null
    currentTrafficLightData.value = null

    rerenderTlsOverlays()

    // 清除所有标记
    markerOverlays.forEach(overlay => {
      map?.removeOverlay(overlay)
    })
    markerOverlays.length = 0

    currentLocation.value = 'No element selected'
    emit('trafficLightCleared')
  })

  console.log('✅ [Map] 地图交互事件设置完成')
}

// 设置视图监听器
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

  console.log('✅ [Map] 视图监听器设置完成')
}

// 在组件挂载时启动动画
onMounted(async () => {
  console.log('🚀 [ControlMap] 组件挂载，开始初始化...')

  try {
    // 首先创建地图实例
    if (!map) {
      console.log('🗺️ [Map] 创建地图实例...')
      map = new OLMap({
        target: mapRef.value!,
        layers: [],
        controls: []
      })
      console.log('✅ [Map] 地图实例创建成功')
    }

    // 然后加载地图数据
    await loadLaneData()

    // 启动车道动画
    startRoadAnimation()

    // 在这里初始化emergency store，避免循环依赖
    setTimeout(() => {
      try {
        emergencyStore = useEmergencyStore()
        console.log('✅ [ControlMap] Emergency store 初始化完成')
      } catch (error) {
        console.error('❌ [ControlMap] Emergency store 初始化失败:', error)
      }
    }, 100)

  } catch (error) {
    console.error('❌ [Map] 组件初始化失败:', error)
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

  // 停止车道动画
  stopRoadAnimation()

  // 清理地图事件监听器
  mapEventListeners.forEach(cleanup => cleanup())
  mapEventListeners = []

  // 清理车辆追踪器
  if (vehicleTracker) {
    vehicleTracker.disconnect()
    vehicleTracker = null
  }

  // 清理车辆覆盖层
  vehicleOverlays.forEach(overlay => {
    map?.removeOverlay(overlay)
  })
  vehicleOverlays.length = 0
})

defineExpose({
  setHighlightLanes: (fromLanes: string[], toLanes: string[]) => {
    console.log('🎨 [Map] 设置高亮车道:', { fromLanes, toLanes })
    highlightLanes.value = { fromLanes, toLanes }

  },
  setSelectedJunction: (junctionName: string | null) => {
    selectedJunctionName.value = junctionName
    rerenderTlsOverlays() // 重新渲染交通灯
  },
  setSelectedTrafficLight,
  setSelectedJunctionOnly,
  clearTrafficStatus,
  zoomToJunction,
  zoomToJunctionById,
  showPlannedRoute,
  // 暴露紧急车辆对话框控制方法
  showEmergencyRequestDialog,
  hideEmergencyRequestDialog,
  // 暴露紧急车辆状态
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
  /* 在导航栏打开时，地图区域向右移动且缩小 */
  width: calc(100% - 2.4rem); /* 给导航栏留出空间 */
  transform: translateX(2.4rem); /* 向右移动导航栏的宽度 */
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
  position: relative; /* 确保相对定位 */
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
  color: #00E5FF ;
  font-family: Arial, sans-serif;
}

.footer-icon {
  color: #00E5FF !important;
  font-size: 0.18rem !important;
  margin-right: 0.08rem;
}

.footer-link {
  color: #00E5FF;
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
  color: #00E5FF;
  font-size: 0.14rem;
  font-weight: 600;
}

// 基本按钮样式
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
    color: #00FFFF;
    /* 移除背景色变化 */
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
    color: #00FFFF;
    /* 移除背景色变化 */
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
  color: #00E5FF;
}

// View switch 样式
.view-switch {
  margin-left: 1rem;
}

.switch {
  position: relative;
  display: inline-block;
  width: 60px;
  height: 34px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: .4s;
  border-radius: 34px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 26px;
  width: 26px;
  left: 4px;
  bottom: 4px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

input:checked + .slider {
  background-color: #00E5FF;
}

input:checked + .slider:before {
  transform: translateX(26px);
}

// 添加标记样式
:global(.search-marker),
:global(.click-marker) {
  color: #ff6b6b !important;
  font-size: 24px !important;
  z-index: 1000 !important;
  font-weight: bold;
  text-shadow: 0 0 8px rgba(255, 107, 107, 0.8);
  animation: markerPulse 2s ease-in-out infinite;
}

// 地图交通状态栏样式
.map-traffic-status {
  position: absolute;
  z-index: 1000;
  pointer-events: auto;
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
