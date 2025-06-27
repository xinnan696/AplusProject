<template>
  <div class="map-show" :class="{ 'sidebar-open': isSidebarOpen }">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="zoom-controls">
        <div class="iconfont zoom-btn-plus" @click="zoomIn">&#xeaf3;</div>
        <div class="iconfont zoom-btn-minus" @click="zoomOut">&#xeaf5;</div>
      </div>

      <!-- 搜索框 -->
      <div class="search-wrapper">
        <input
          class="search-input"
          type="text"
          placeholder="Search junction name..."
          v-model="searchInput"
          @keydown.enter="searchJunction"
        />
      </div>
      <div class="iconfont search-btn" @click="searchJunction">&#xeafe;</div>
    </div>

    <!-- 地图容器 -->
    <div class="map-container" ref="mapRef"></div>

    <!-- 底部信息栏 -->
    <div class="footer-container">
      <div class="footer-content">
        <span class="iconfont footer-icon">&#xe60b;</span>
        <span class="footer-text">
          Current Location:
          <span class="footer-link">{{ currentLocation }}</span>
        </span>
      </div>
    </div>
  </div>
</template>


<script setup lang="ts">
import { onMounted, onUnmounted, ref, defineExpose } from 'vue'
import axios from 'axios'
import 'ol/ol.css'
import OLMap from 'ol/Map'
import View from 'ol/View'
import VectorLayer from 'ol/layer/Vector'
import VectorSource from 'ol/source/Vector'
import GeoJSON from 'ol/format/GeoJSON'
import { Style, Stroke } from 'ol/style'
import { getCenter } from 'ol/extent'
import Overlay from 'ol/Overlay'

defineProps<{ isSidebarOpen: boolean }>()

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
}

const mapRef = ref<HTMLElement | null>(null)
let map: OLMap | null = null
let view: View | null = null
let vectorLayer: VectorLayer | null = null
let hasFitted = false

const currentLocation = ref('Click on a lane or signal light')
const highlightLanes = ref<{ fromLanes: string[]; toLanes: string[] } | null>(null)
const searchInput = ref('')
const junctionMap = new Map<string, Junction>()

const markerOverlays: Overlay[] = []

const vehicleCountMap = ref<Record<string, number>>({})
const laneToEdgeMap = new Map<string, string>() // laneId -> edgeId

const clearAllMarkers = () => {
  for (const overlay of markerOverlays) {
    map?.removeOverlay(overlay)
  }
  markerOverlays.length = 0
}

const searchJunction = () => {
  const name = searchInput.value.trim()
  const junction = junctionMap.get(name)
  if (junction && view) {
    view.animate({
      center: [junction.junctionX, junction.junctionY],
      zoom: 17,
      duration: 500
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

    currentLocation.value = ` ${name}`
  } else {
    console.warn('Junction not found')
    currentLocation.value = 'Junction not found'
  }
}

const loadLaneData = async () => {
  const res = await axios.get('/api-status/lane-mappings')
  const data = res.data as LaneMapping[]

  const features = data.map((lane: LaneMapping) => {
    const coordinates = lane.laneShape.trim().split(' ').map(p => p.split(',').map(Number))

    // ✨ 保存 laneId → edgeId 映射
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

      const edgeId = laneToEdgeMap.get(laneId)
      const count = edgeId ? vehicleCountMap.value[edgeId] ?? 0 : 0

      let color = '#00B4D8' //
      if (highlightLanes.value?.fromLanes.includes(laneId)) {
        color = '#002cd9' //
      } else if (highlightLanes.value?.toLanes.includes(laneId)) {
        color = '#d900d8' //
      } else if (count >= 7) {
        color = '#D9001B' //
      } else if (count >= 4) {
        color = '#F59A23' //
      } else if (count > 0) {
        color = '#FFFF00' //
      }

      return new Style({ stroke: new Stroke({ color, width: 3 }) })
    }
  })

  const extent = vectorSource.getExtent()
  const center = getCenter(extent)

  view = new View({
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

  // junction name map
  const nameRes = await axios.get('/api-status/junctions')
  const raw = nameRes.data as Record<string, { junction_id: string; junction_name: string }>
  const junctionNameMap = new Map<string, string>()
  for (const key in raw) {
    const item = raw[key]
    if (item.junction_id && item.junction_name) {
      junctionNameMap.set(item.junction_id, item.junction_name)
    }
  }

  const junctionRes = await axios.get('/api-status/tls-junctions')
  const junctions = junctionRes.data as Junction[]
  junctions.forEach(junction => {
    const iconEl = document.createElement('div')
    iconEl.className = 'iconfont tls'
    iconEl.innerHTML = '&#xe615;'
    iconEl.dataset['name'] = junctionNameMap.get(junction.junctionId) || junction.junctionId

    const overlay = new Overlay({
      element: iconEl,
      positioning: 'center-center',
      stopEvent: false,
      offset: [0, 0],
      position: [junction.junctionX, junction.junctionY]
    })
    map?.addOverlay(overlay)

    const name = junctionNameMap.get(junction.junctionId) || junction.junctionId
    junctionMap.set(name, junction)
  })

  // 点击地图显示信息
  map?.on('singleclick', evt => {
    const pixel = map!.getEventPixel(evt.originalEvent)
    const features = map!.getFeaturesAtPixel(pixel)

    if (features?.length) {
      const props = features[0].getProperties()
      if (props.edgeName) currentLocation.value = ` ${props.edgeName}`
    } else {
      const coordinate = evt.coordinate
      let found = false
      for (const overlay of map!.getOverlays().getArray()) {
        const pos = overlay.getPosition()
        if (pos && Math.abs(pos[0] - coordinate[0]) < 5 && Math.abs(pos[1] - coordinate[1]) < 5) {
          const el = overlay.getElement()
          if (el instanceof HTMLElement && el.dataset['name']) {
            currentLocation.value = `Junction: ${el.dataset['name']}`
            found = true
            break
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
}

onMounted(async () => {
  if (!map) {
    map = new OLMap({ target: mapRef.value!, layers: [], controls: [] })
  } else {
    map.setTarget(mapRef.value!)
    setTimeout(() => map!.updateSize(), 300)
  }

  await loadLaneData()

  // ✨ 启动 WebSocket 监听
  const ws = new WebSocket('ws://localhost:8087/api/status/ws')

  ws.onmessage = (event) => {
  try {
    const raw = JSON.parse(event.data)

    if (raw.edges) {
      const newMap: Record<string, number> = {}

      for (const edgeId in raw.edges) {
        const edgeStr = raw.edges[edgeId]
        const parsed = JSON.parse(edgeStr)
        const count = parsed.vehicleCount ?? 0
        newMap[edgeId] = count
      }

      vehicleCountMap.value = newMap
      vectorLayer?.changed()
    }
  } catch (e) {
    console.error('[❌] WebSocket 数据解析失败:', e)
  }
}
})

onUnmounted(() => {
  if (map) map.setTarget(undefined)
})

defineExpose({
  setHighlightLanes: (fromLanes: string[], toLanes: string[]) => {
    highlightLanes.value = { fromLanes, toLanes }
    vectorLayer?.changed()
  }
})

const zoomIn = () => view?.animate({ zoom: Math.min(view.getZoom()! + 0.5, 18), duration: 250 })
const zoomOut = () => view?.animate({ zoom: Math.max(view.getZoom()! - 0.5, 13), duration: 250 })
</script>


<style scoped lang="scss">
.map-show {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  position: relative;
  background-color: #1e1e2f;
  transition: width 0.3s ease, transform 0.3s ease;
}
.map-show.sidebar-open {
  width: 11.2rem;
  transform: translateX(2.4rem);
}
.toolbar {
  position: relative;
  height: 0.64rem;
  background-color: #1e1e2f;
  display: flex;
  align-items: center;
  padding: 0 0.24rem;
  border-bottom: 0.01rem solid #3a3a4c;
  z-index: 999;
  flex-shrink: 0;
}
.iconfont {
  font-size: 0.4rem;
  width: 0.3rem;
  height: 0.3rem;
  line-height: 0.3rem;
  color: #cfcfcf;
  text-align: center;
  cursor: pointer;
}
.zoom-controls {
  display: flex;
  align-items: center;
}
.zoom-btn-plus {
  margin-right: 0.4rem;
}
.search-wrapper {
  margin-left: 3rem;
  width: 8.73rem;
  height: 0.4rem;
  display: flex;
  align-items: center;
  border: 0.01rem solid #00b4d8;
  background-color: #2b2c3d;
  border-radius: 0.2rem;
  padding: 0 0.12rem;
  box-sizing: border-box;

  .search-input {
    flex: 1;
    height: 100%;
    background-color: transparent;
    border: none;
    outline: none;
    color: white;
    font-size: 0.13rem;
    padding: 0 0.08rem;
  }
}
.search-btn {
  position: absolute;
  right: 0.24rem;
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
  background-color: #1e1e2f;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-shrink: 0;
  font-size: 0.16rem;
}
.footer-content {
  width: 10.57rem;
  height: 0.57rem;
  display: flex;
  justify-content: center;
  align-items: center;
  color: white;
  font-family: Arial, sans-serif;
}
.footer-icon {
  color: #00b4d8;
  font-size: 0.18rem;
  margin-right: 0.08rem;
}
.footer-link {
  color: #00b4d8;
  margin-left: 0.04rem;
}

</style>
