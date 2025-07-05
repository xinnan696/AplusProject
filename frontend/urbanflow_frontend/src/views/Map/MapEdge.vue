<template>
  <div id="map" class="sumo-map-container"></div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import 'ol/ol.css'
import Map from 'ol/Map'
import View from 'ol/View'
import VectorLayer from 'ol/layer/Vector'
import VectorSource from 'ol/source/Vector'
import GeoJSON from 'ol/format/GeoJSON'
import { Style, Stroke } from 'ol/style'
import Feature from 'ol/Feature'

// ✅ 默认道路样式
const defaultStyle = new Style({
  stroke: new Stroke({ color: '#888', width: 1 })
})

// ✅ 点击后高亮样式
const highlightStyle = new Style({
  stroke: new Stroke({ color: '#ff0000', width: 3 })
})

onMounted(() => {
  const vectorSource = new VectorSource({
    url: '/edges.geojson',
    format: new GeoJSON()
  })

  const vectorLayer = new VectorLayer({
    source: vectorSource,
    style: defaultStyle
  })

  const map = new Map({
    target: 'map',
    layers: [vectorLayer],
    view: new View({
      center: [0, 0],
      zoom: 1
    })
  })

  // ✅ 自动缩放到 edges 范围
  vectorSource.on('change', () => {
    if (vectorSource.getState() === 'ready') {
      const extent = vectorSource.getExtent()
      map.getView().fit(extent, {
        padding: [20, 20, 20, 20],
        duration: 1000
      })
    }
  })

  // ✅ 点击高亮功能
  map.on('click', (evt) => {
    // 点击前清空所有高亮
    vectorSource.getFeatures().forEach(f => f.setStyle(defaultStyle))

    map.forEachFeatureAtPixel(evt.pixel, (feature) => {
      const realFeature = feature as Feature
      realFeature.setStyle(highlightStyle)
      console.log('点击了 edge ID:', realFeature.get('id'))
    })
  })
})
</script>

<style scoped>
/* ✅ 地图容器，确保撑满整个页面 */
.sumo-map-container {
  width: 100%;
  height: 100vh;
  margin: 0;
  padding: 0;
   /* 背景黑更清晰 */
}

/* ✅ 缩小放大控件的大小 */
.sumo-map-container .ol-control {
  transform: scale(0.7);
  transform-origin: top left;
}
</style>
