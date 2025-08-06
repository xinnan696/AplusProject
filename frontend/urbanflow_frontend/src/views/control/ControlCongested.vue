<template>
  <div class="congested-box">
    <div class="title">
      <span>Congested Roads</span>
    </div>

    <div class="table-header">
      <span class="col location">Location</span>
      <span class="col queue">Queue Length</span>
    </div>

    <div class="table-body">
      <!-- 显示拥堵道路列表 -->
      <div
        class="table-row"
        v-for="(item, index) in displayedCongestedData"
        :key="`${item.junctionId}-${index}`"
        v-if="displayedCongestedData.length > 0"
      >
        <span
            class="col location"
            :class="getQueueClass(item.congestionCount)"
            @click="handleLocationClick(item.junctionId)"
            style="cursor: pointer;"
>
            {{ item.junctionName }}
        </span>
        <span class="col queue" :class="getQueueClass(item.congestionCount)">
          {{ animatedValues[item.junctionId] || item.congestionCount }}
        </span>
      </div>

      <!-- 空状态显示 -->
      <div v-if="displayedCongestedData.length === 0" class="empty-state">
        <div class="empty-text">No Congested Roads</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

interface Props {
  isAIMode?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isAIMode: false
})

const emit = defineEmits<{
  (e: 'select-junction-by-name', name: string): void
}>()

const authStore = useAuthStore()

interface CongestedItem {
  junctionId: string
  junctionName: string
  congestionCount: number
  area?: string
}

const allCongestedData = ref<CongestedItem[]>([])
const junctionNameMap = ref<Record<string, string>>({})
const junctionAreaMap = ref<Record<string, string>>({})
const animatedValues = ref<Record<string, number>>({})

let socket: WebSocket | null = null
let reconnectAttempts = 0
const maxReconnectAttempts = 5
let reconnectTimer: NodeJS.Timeout | null = null

const fetchJunctionMappings = async () => {
  try {
    const laneResponse = await fetch('/api-status/lane-mappings')
    const laneData = await laneResponse.json()

    let minX = Infinity, maxX = -Infinity
    laneData.forEach((lane: any) => {
      const coordinates = lane.laneShape.trim().split(' ').map((p: string) => p.split(',').map(Number))
      coordinates.forEach((coord: number[]) => {
        minX = Math.min(minX, coord[0])
        maxX = Math.max(maxX, coord[0])
      })
    })

    const mapCenterX = (minX + maxX) / 2

    const tlsResponse = await fetch('/api-status/tls-junctions')
    const tlsData = await tlsResponse.json()

    const junctionCoordMap: Record<string, { x: number, y: number }> = {}
    tlsData.forEach((tls: any) => {
      if (tls.junctionId && tls.junctionX !== undefined && tls.junctionY !== undefined) {
        junctionCoordMap[tls.junctionId] = {
          x: tls.junctionX,
          y: tls.junctionY
        }
      }
    })

    return { mapCenterX, junctionCoordMap }
  } catch (error) {
    console.error('Failed to fetch junction mappings:', error)
    return { mapCenterX: 0, junctionCoordMap: {} }
  }
}

const displayedCongestedData = computed(() => {
  let filteredData = [...allCongestedData.value]

  // 根据用户角色和管理区域进行权限过滤
  if (authStore.isTrafficManager()) {
    const managedAreas = authStore.getManagedAreas()
    console.log('🔍 [CongestedRoads] Filtering by managed areas:', managedAreas)
    
    if (managedAreas.length > 0) {
      filteredData = filteredData.filter(item => {
        const area = junctionAreaMap.value[item.junctionId]
        const hasAccess = area && managedAreas.includes(area)
        
        if (!hasAccess) {
          console.log('🚫 [CongestedRoads] Filtered out junction:', {
            junctionId: item.junctionId,
            junctionName: item.junctionName,
            area,
            managedAreas
          })
        }
        
        return hasAccess
      })
      
      console.log('✅ [CongestedRoads] After area filtering:', {
        originalCount: allCongestedData.value.length,
        filteredCount: filteredData.length,
        managedAreas
      })
    }
  } else if (authStore.isTrafficPlanner()) {
    // Traffic Planner 可以查看所有区域，但只是查看
    console.log('👀 [CongestedRoads] Traffic Planner - showing all areas (view-only)')
  } else if (authStore.isAdmin()) {
    // Admin 可以查看所有区域
    console.log('👑 [CongestedRoads] Admin - showing all areas')
  }

  // 过滤掉队列长度小于最小非绿色值的路口（即只显示真正拥堵的路口）
  // 根据getQueueClass的逻辑，只有>=10的才是warning或danger，<10的都是normal（绿色）
  filteredData = filteredData.filter(item => item.congestionCount >= 10)

  const displayCount = props.isAIMode ? 10 : 6
  return filteredData.slice(0, displayCount)
})

const handleLocationClick = (junctionId: string) => {
  emit('select-junction-by-name', junctionId)
}

const connectWebSocket = () => {
  try {
    socket = new WebSocket('ws://localhost:8087/api/status/ws')

    socket.onopen = () => {
      // WebSocket连接成功
      reconnectAttempts = 0
    }

    socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)

        if (data.congested && Array.isArray(data.congested)) {
          const processedData: CongestedItem[] = data.congested.map((item: any) => {
            const junctionId = item.j || item.junctionId
            const junctionName = junctionNameMap.value[junctionId] || junctionId
            const congestionCount = item.q || item.congestionCount || 0
            return {
              junctionId,
              junctionName,
              congestionCount,
              area: junctionAreaMap.value[junctionId]
            }
          })

          processedData.forEach((item: CongestedItem) => {
            const oldValue = animatedValues.value[item.junctionId] || 0
            const newValue = item.congestionCount
            if (oldValue !== newValue) {
              animateNumber(item.junctionId, oldValue, newValue)
            }
          })

          allCongestedData.value = processedData
        }
      } catch (e) {
        console.error('WebSocket JSON parse error:', e)
      }
    }

    socket.onerror = (err) => {
      // WebSocket连接错误
    }

    socket.onclose = () => {
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

onMounted(async () => {
  try {
    const { mapCenterX, junctionCoordMap } = await fetchJunctionMappings()

    const response = await fetch('/api-status/junctions')
    const data = await response.json()

    const nameMap: Record<string, string> = {}
    const areaMap: Record<string, string> = {}

    for (const key in data) {
      const j = data[key]
      nameMap[j.junction_id] = j.junction_name

      if (j.area) {
        areaMap[j.junction_id] = j.area
      } else {
        const coords = junctionCoordMap[j.junction_id]
        if (coords) {
          if (coords.x < mapCenterX) {
            areaMap[j.junction_id] = 'Left'
          } else {
            areaMap[j.junction_id] = 'Right'
          }
        } else {
          const junctionName = j.junction_name
          if (junctionName.includes('Left') || junctionName.includes('左')) {
            areaMap[j.junction_id] = 'Left'
          } else if (junctionName.includes('Right') || junctionName.includes('右')) {
            areaMap[j.junction_id] = 'Right'
          } else {
            areaMap[j.junction_id] = 'Left'
          }
        }
      }
    }

    junctionNameMap.value = nameMap
    junctionAreaMap.value = areaMap

    console.log('Junction area mapping completed:', {
      totalJunctions: Object.keys(nameMap).length,
      leftJunctions: Object.values(areaMap).filter(area => area === 'Left').length,
      rightJunctions: Object.values(areaMap).filter(area => area === 'Right').length,
      mapCenterX,
      userRole: authStore.userRole,
      managedAreas: authStore.getManagedAreas(),
      sampleJunctionAreas: Object.entries(areaMap).slice(0, 5)
    })
  } catch (err) {
    console.error('Failed to fetch junction data:', err)
  }

  // 连接 WebSocket
  connectWebSocket()
})

const animateNumber = (key: string, from: number, to: number) => {
  const duration = 800
  const start = Date.now()
  const animate = () => {
    const now = Date.now()
    const progress = Math.min((now - start) / duration, 1)
    const easeOut = 1 - Math.pow(1 - progress, 3)
    animatedValues.value[key] = Math.round(from + (to - from) * easeOut)

    if (progress < 1) {
      requestAnimationFrame(animate)
    }
  }
  requestAnimationFrame(animate)
}

onUnmounted(() => {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
  }
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.close()
  }
})

const getQueueClass = (congestionCount: number) => {
  if (congestionCount >= 13) return 'danger'
  if (congestionCount >= 10) return 'warning'
  return 'normal'
}
</script>

<style scoped lang="scss">
.congested-box {
  width: 100%;
  height: 2.8rem;
  background-color: #1E1E2F;
  box-sizing: border-box;
  border-bottom: 0.01rem solid #3A3A4C;
  flex-shrink: 0;
  position: relative;
  padding: 0.2rem 0.24rem 0rem 0.24rem;
  overflow: visible;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background:
      radial-gradient(circle at 20% 20%, rgba(74, 85, 104, 0.05) 0%, transparent 50%),
      radial-gradient(circle at 80% 80%, rgba(113, 128, 150, 0.03) 0%, transparent 50%),
      linear-gradient(45deg, transparent 48%, rgba(74, 85, 104, 0.02) 49%, rgba(74, 85, 104, 0.02) 51%, transparent 52%);
    pointer-events: none;
    z-index: 0;
  }

  > * {
    position: relative;
    z-index: 1;
  }
}

.title {
  font-size: 0.2rem;
  font-weight: 700;
  color: #00E5FF;
  line-height: 0.2rem;
  padding-bottom: 0.16rem;
  display: flex;
  justify-content: space-between;
  align-items: center;

  position: relative;

  span {
    position: relative;
  }
}

.table-header {
  display: flex;
  justify-content: space-between;
  font-size: 0.16rem;
  font-weight: 600;
  color: #FFFFFF;
  line-height: 0.16rem;
  padding-bottom: 0.16rem;

  letter-spacing: 0.02rem;

  .col {
    display: flex;
    align-items: center;
  }

  .col:first-child {
    width: 75%;
  }

  .col:last-child {
    width: 25%;
    justify-content: center;
    white-space: nowrap;
  }
}

.table-body {
  display: flex;
  flex-direction: column;
  overflow: visible;
  gap: 0; // 移除所有行间距
}

.table-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
  height: 0.28rem; // 保持原有高度
  // 移除 padding-bottom 和 line-height
  margin: 0 -0.05rem; // 保持水平margin
  padding-left: 0.05rem;
  padding-right: 0.05rem;
  transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  border-radius: 0.06rem;
  position: relative;
  overflow: visible;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0; // 回到正常范围
    background: linear-gradient(45deg, transparent 48%, rgba(74, 85, 104, 0.1) 49%, rgba(74, 85, 104, 0.1) 51%, transparent 52%);
    opacity: 0;
    transition: opacity 0.3s ease;
    pointer-events: none;
  }

  &:hover {
    background: rgba(74, 85, 104, 0.15);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
    z-index: 10;
    position: relative;

    &::before {
      opacity: 1;
    }
  }
}

.col {
  display: flex;
  align-items: center; // 确保内容垂直居中
  height: 100%; // 占满父容器高度
}

.col.location {
  width: 75%;
  font-size: 0.14rem;
  transition: all 0.3s ease;
  white-space: nowrap;
  overflow: visible;
  font-weight: 500;
  // 继承父级的 flex 和 align-items: center
}

.col.queue {
  width: 25%;
  font-size: 0.14rem;
  justify-content: center;
  font-weight: 700;
  position: relative;
  white-space: nowrap;
  // 继承父级的 flex 和 align-items: center
}

.danger {
  color: #FF4569;

}

.warning {
  color: #FFC107;

}

.normal {
  color: #00E676;

}

// 空状态样式，与AI面板保持一致
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  min-height: 1.2rem; // 保证有足够的高度

  .empty-text {
    color: rgba(156, 163, 175, 0.6); // 与AI面板no-suggestion相同的灰色
    font-size: 0.16rem;
    font-style: italic;
    font-weight: 500;
    text-align: center;
  }
}


</style>
