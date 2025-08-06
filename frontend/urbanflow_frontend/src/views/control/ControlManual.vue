<template>
  <div class="manual-control">
    <div class="panel-title">
      <span>Manual Control</span>
    </div>

    <div class="form-row">
      <label class="label">Junction</label>
      <div class="select-wrapper" :class="{ 'dropdown-open': isJunctionDropdownOpen }" @click="toggleJunctionDropdown">
        <div class="custom-select" :class="{ 'open': isJunctionDropdownOpen }">
          <div class="select-display">
            {{ selectedJunctionIndex !== null && junctionDataList[selectedJunctionIndex]
               ? (junctionDataList[selectedJunctionIndex].junction_name || junctionDataList[selectedJunctionIndex].junction_id)
               : 'Please Select a Junction' }}
          </div>
          <div class="select-arrow" :class="{ 'rotated': isJunctionDropdownOpen }">
            <svg width="12" height="8" viewBox="0 0 12 8" fill="none">
              <path d="M1 1L6 6L11 1" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
        </div>
        <div class="dropdown-panel" v-show="isJunctionDropdownOpen">
          <div class="dropdown-search">
            <input
              type="text"
              v-model="junctionSearchQuery"
              placeholder="Search junctions..."
              class="search-input"
              @click.stop
            >
          </div>
          <div class="dropdown-options">
            <div
              class="dropdown-option"
              v-for="(junction, index) in filteredJunctions"
              :key="`${junction.junction_id}-${junction.originalIndex}`"
              :class="{ 'selected': selectedJunctionIndex === junction.originalIndex }"
              @click.stop="selectJunction(junction.originalIndex)"
            >
              {{ junction.junction_name || junction.junction_id }}
            </div>
            <div v-if="filteredJunctions.length === 0" class="no-results">
              No junctions found
            </div>
          </div>
        </div>
      </div>
    </div>


    <div class="form-row">
      <label class="label">Traffic Light</label>
      <div class="select-wrapper" :class="{ 'dropdown-open': isDirectionDropdownOpen }" @click="toggleDirectionDropdown">
        <div class="custom-select" :class="{ 'open': isDirectionDropdownOpen }">
          <div class="select-display">
            {{ selectedDirectionIndex !== null && currentDirections[selectedDirectionIndex]
               ? `${currentDirections[selectedDirectionIndex].fromEdgeName} → ${currentDirections[selectedDirectionIndex].toEdgeName}`
               : 'Select Traffic Light Direction' }}
          </div>
          <div class="select-arrow" :class="{ 'rotated': isDirectionDropdownOpen }">
            <svg width="12" height="8" viewBox="0 0 12 8" fill="none">
              <path d="M1 1L6 6L11 1" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
        </div>
        <div class="dropdown-panel" v-show="isDirectionDropdownOpen">
          <div class="dropdown-options">
            <div
              class="dropdown-option"
              v-for="(dir, idx) in currentDirections"
              :key="idx"
              :class="{ 'selected': selectedDirectionIndex === idx }"
              @click.stop="selectDirection(idx)"
              :title="dir.fromEdgeName + ' → ' + dir.toEdgeName"
            >
              <span class="direction-text">{{ dir.fromEdgeName }} → {{ dir.toEdgeName }}</span>
            </div>
            <div v-if="currentDirections.length === 0" class="no-results">
              Please select a junction first
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="form-row">
      <label class="label">Light State</label>
      <div class="light-buttons">
        <button
          class="light-btn red"
          :class="{ 'active-red': selectedLight === 'RED' }"
          @click="selectLight('RED')"
        >RED</button>
        <button
          class="light-btn green"
          :class="{ 'active-green': selectedLight === 'GREEN' }"
          @click="selectLight('GREEN')"
        >GREEN</button>
      </div>
    </div>

    <div class="form-row-duration">
      <div class="form-row">
        <label class="label">Duration</label>
        <div class="duration-custom">
          <input
            type="text"
            class="custom-input"
            v-model="durationDisplay"
            @input="validateDuration"
            @keypress="onlyAllowNumbers"
            @blur="handleBlur"
            placeholder="Duration (s)"
          />
          <div class="triangle-buttons">
            <button class="triangle-btn" @click="increaseDuration">▲</button>
            <button class="triangle-btn" @click="decreaseDuration">▼</button>
          </div>
        </div>
      </div>
      <div class="duration-error-container">
        <div class="duration-error" v-if="durationError">⚠ The value must be between 5 and 300.</div>
      </div>
    </div>


    <div class="action-buttons">
      <button
        class="apply-btn"
        :disabled="!isFormComplete || !canModifyLights"
        @click="onApply"
        :title="getApplyButtonTooltip()"
      >
        <div v-if="isApplying" class="loading-spinner"></div>
        <span>{{ isApplying ? 'APPLYING...' : 'APPLY' }}</span>
      </button>
      <button class="cancel-btn" @click="resetForm">CANCEL</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, watchEffect, defineEmits, onBeforeUnmount } from 'vue'
import axios from 'axios'
import apiClient from '@/utils/api'
import { toast } from '@/utils/ToastService'
import { useOperationStore } from '@/stores/operationStore'
import { useAuthStore } from '@/stores/auth'
import { useTrafficLightPermissions } from '@/composables/useTrafficLightPermissions'
import { getJunctionPermissionStatus, isJunctionInUserManagedArea, PERMISSION_MESSAGES } from '@/utils/permissionUtils'
import PermissionIndicator from '@/components/PermissionIndicator.vue'


const emit = defineEmits<{
  (e: 'highlight', fromLanes: string[], toLanes: string[]): void
  (e: 'trafficLightSelected', junctionId: string, directionIndex: number, options?: { disableZoom?: boolean }): void
  (e: 'trafficLightCleared'): void
  (e: 'junctionSelected', junctionName: string, junctionId: string): void
  (e: 'manualControlApplied', data: { junctionName: string, directionInfo: string, lightColor: string, duration: number }): void
}>()

const authStore = useAuthStore()
const operationStore = useOperationStore()
const { canModifyTrafficLight, canApplyManualControl, getOperationPermissionStatus } = useTrafficLightPermissions()


const mapCenterX = ref(0)


const selectedJunctionPermission = ref<{
  status: 'controllable' | 'readonly' | 'forbidden'
  description: string
} | null>(null)


const isJunctionDropdownOpen = ref(false)
const isDirectionDropdownOpen = ref(false)
const junctionSearchQuery = ref('')
const isApplying = ref(false)

type LaneConnectionGroup = string[][]

interface RawJunctionData {
  tlsID: string
  junction_id: string
  junction_name: string
  connection: LaneConnectionGroup[]
  junctionX?: number
  junctionY?: number
}

interface LaneShapeInfo {
  laneId: string
  shape: string
}

interface Direction {
  fromEdgeName: string
  toEdgeName: string
}

const junctionDataList = ref<RawJunctionData[]>([])
const selectedJunctionIndex = ref<number | null>(null)
const selectedDirectionIndex = ref<number | null>(null)
const selectedLight = ref('')
const duration = ref<number | null>(null)

const laneIdToEdgeName = ref<Record<string, string>>({})
const laneIdToShape = ref<Record<string, string>>({})
const directionLanes = ref<{ from: string[]; to: string[] }[]>([])

const durationDisplay = ref(duration.value?.toString() || '')
const durationError = ref(false)

const currentJunction = computed(() =>
  selectedJunctionIndex.value !== null
    ? junctionDataList.value[selectedJunctionIndex.value]
    : null
)

const currentDirections = computed(() => {
  const directions: Direction[] = []
  const connGroups = currentJunction.value?.connection || []
  connGroups.forEach(group => {
    group.forEach(pair => {
      if (pair.length >= 2) {
        const fromLane = pair[0]
        const toLane = pair[1]
        const fromEdge = laneIdToEdgeName.value[fromLane] || fromLane
        const toEdge = laneIdToEdgeName.value[toLane] || toLane
        directions.push({ fromEdgeName: fromEdge, toEdgeName: toEdge })
      }
    })
  })
  return directions
})

const filteredJunctions = computed(() => {
  if (!junctionSearchQuery.value.trim()) {
    return junctionDataList.value.map((junction, index) => ({
      ...junction,
      originalIndex: index
    }))
  }

  const query = junctionSearchQuery.value.toLowerCase()
  return junctionDataList.value
    .map((junction, index) => ({ ...junction, originalIndex: index }))
    .filter(junction => {
      const name = junction.junction_name || junction.junction_id || ''
      const id = junction.junction_id || ''
      return name.toLowerCase().includes(query) || id.toLowerCase().includes(query)
    })
})

const selectedJunctionPermissionStatus = computed(() => {
  if (!currentJunction.value || !currentJunction.value.junctionX || !currentJunction.value.junctionY) {
    return null
  }
  return getJunctionPermissionStatus(
    currentJunction.value.junctionX,
    currentJunction.value.junctionY,
    mapCenterX.value
  )
})

const canSelectDirection = computed(() => {
  return selectedJunctionIndex.value !== null && currentDirections.value.length > 0
})

const canModifyLights = computed(() => {
  if (!currentJunction.value) {
    return false
  }

  if (!currentJunction.value.junctionX || !currentJunction.value.junctionY) {
    return false
  }

  if (mapCenterX.value === 0) {
    return false
  }

  const result = canModifyTrafficLight(currentJunction.value.junctionX, currentJunction.value.junctionY, mapCenterX.value)

  return result
})


const canViewLights = computed(() => {
  if (!currentJunction.value) {
    return false
  }

  return authStore.isAuthenticated
})

const showPermissionWarning = computed(() => {
  return selectedJunctionIndex.value !== null && !canModifyLights.value && canViewLights.value
})

const permissionWarningMessage = computed(() => {
  if (!showPermissionWarning.value) return ''

  if (authStore.isTrafficPlanner()) {
    return 'Traffic Planners have view-only access. You can select junctions and directions to view traffic light status.'
  }

  if (authStore.isTrafficManager()) {
    const managedAreas = authStore.getManagedAreas()
    return `You can view all areas but only modify traffic lights in: ${managedAreas.join(', ')} area(s). Select any junction and direction to view status.`
  }

  return 'You have view-only access in this area. You can select and view traffic light status but cannot make changes.'
})

const getJunctionPermission = (junction: RawJunctionData) => {
  if (!junction.junctionX || !junction.junctionY) {
    return { canControl: false, canView: true, areaName: 'Unknown', statusText: 'No Data' }
  }
  return getJunctionPermissionStatus(junction.junctionX, junction.junctionY, mapCenterX.value)
}

const getJunctionArea = (junction: RawJunctionData): string => {
  if (!junction.junctionX) return 'Unknown'
  return junction.junctionX < mapCenterX.value ? 'Left' : 'Right'
}

const getJunctionAreaClass = (junction: RawJunctionData): string => {
  const area = getJunctionArea(junction)
  const permission = getJunctionPermission(junction)
  return `area-${area.toLowerCase()} ${permission.canControl ? 'controllable' : 'readonly'}`
}

const getApplyButtonTooltip = (): string => {
  if (!canModifyLights.value) {
    return permissionWarningMessage.value
  }
  if (!isFormComplete.value) {
    return 'Please complete all required fields'
  }
  return 'Apply traffic light changes'
}


watchEffect(() => {
  const connGroups = currentJunction.value?.connection || []
  const lanePairs: { from: string[]; to: string[] }[] = []

  connGroups.forEach(group => {
    group.forEach(pair => {
      if (pair.length >= 2) {
        const fromLane = pair[0]
        const toLane = pair[1]
        lanePairs.push({ from: [fromLane], to: [toLane] })
      }
    })
  })

  directionLanes.value = lanePairs
})


watch(selectedJunctionIndex, () => {
  if (selectedJunctionIndex.value !== null && currentJunction.value) {
    const permission = getOperationPermissionStatus({
      junctionX: currentJunction.value.junctionX || 0,
      junctionY: currentJunction.value.junctionY || 0
    }, mapCenterX.value)

    selectedJunctionPermission.value = {
      status: permission.status,
      description: permission.description
    }
  } else {
    selectedJunctionPermission.value = null
  }
})

watch(selectedDirectionIndex, () => {
  const idx = selectedDirectionIndex.value

  if (idx !== null && directionLanes.value[idx]) {
    // 发送高亮信号给地图组件
    // 注意：这里会触发地图组件的 setHighlightLanes 方法
    // 地图组件会自动保持所有相连车道的紫色显示，并将选中方向的车道标记为绿色/灰色
    emit('highlight', directionLanes.value[idx].from, directionLanes.value[idx].to)

    if (currentJunction.value) {
      const junctionId = currentJunction.value.junction_id
      emit('trafficLightSelected', junctionId, idx, { disableZoom: true })
    }
  }
})

const isFormComplete = computed(() =>
  selectedJunctionIndex.value !== null &&
  selectedDirectionIndex.value !== null &&
  selectedLight.value !== '' &&
  duration.value !== null &&
  duration.value >= 5 &&
  duration.value <= 300 &&
  !durationError.value &&
  !isApplying.value &&
  canModifyLights.value
)

const isFormReadyForViewing = computed(() =>
  selectedJunctionIndex.value !== null &&
  selectedDirectionIndex.value !== null &&
  canViewLights.value
)

const selectLight = (color: string) => {
  if (!canModifyLights.value) {
    toast.warning(PERMISSION_MESSAGES.TRAFFIC_LIGHT_MODIFY_DENIED)
    return
  }
  selectedLight.value = color
}

const partialResetForm = () => {
  selectedLight.value = ''
  duration.value = null
  durationDisplay.value = ''
  durationError.value = false
}

const resetForm = () => {
  selectedJunctionIndex.value = null
  selectedDirectionIndex.value = null
  selectedLight.value = ''
  duration.value = null
  durationDisplay.value = ''
  durationError.value = false
  isJunctionDropdownOpen.value = false
  isDirectionDropdownOpen.value = false
  junctionSearchQuery.value = ''
  selectedJunctionPermission.value = null

  emit('trafficLightCleared')
}

const toggleJunctionDropdown = () => {
  isJunctionDropdownOpen.value = !isJunctionDropdownOpen.value
  isDirectionDropdownOpen.value = false
}

const toggleDirectionDropdown = () => {
  if (!canSelectDirection.value) {
    return
  }
  isDirectionDropdownOpen.value = !isDirectionDropdownOpen.value
  isJunctionDropdownOpen.value = false
}

const selectJunction = (index: number) => {
  const junction = junctionDataList.value[index]


  if (selectedJunctionIndex.value !== null && selectedJunctionIndex.value !== index) {
    console.log('Manual: Clearing previous selection')
    emit('trafficLightCleared')
  }

  selectedJunctionIndex.value = index
  isJunctionDropdownOpen.value = false
  junctionSearchQuery.value = ''
  selectedDirectionIndex.value = null

  if (junction) {
    const junctionName = junction.junction_name || junction.junction_id
    
    // 获取并高亮与junction相连的所有车道
    highlightJunctionConnectedLanes(junction.junction_id)
    
    emit('junctionSelected', junctionName, junction.junction_id)
  }
}

// 获取并高亮与junction相连的所有车道
const highlightJunctionConnectedLanes = async (junctionId: string) => {
  try {
    console.log('Manual: Getting junction connected lanes:', junctionId)
    
    // 获取junction数据
    const response = await axios.get('/api-status/junctions')
    const junctionsData = response.data
    
    // 找到对应的junction数据
    let junctionData = null
    for (const tlsId in junctionsData) {
      const junction = junctionsData[tlsId]
      if (junction.junction_id === junctionId) {
        junctionData = junction
        break
      }
    }
    
    if (!junctionData || !junctionData.connection) {
      console.warn('Manual: Junction connection data not found:', junctionId)
      return
    }
    
    // 提取所有相连的车道
    const allConnectedLanes = new Set<string>()
    
    if (Array.isArray(junctionData.connection)) {
      junctionData.connection.forEach((connectionGroup: string[][]) => {
        if (Array.isArray(connectionGroup)) {
          connectionGroup.forEach((connection: string[]) => {
            if (Array.isArray(connection) && connection.length >= 2) {
              // 添加from车道和to车道
              allConnectedLanes.add(connection[0])
              allConnectedLanes.add(connection[1])
            }
          })
        }
      })
    }
    
    const connectedLanesArray = Array.from(allConnectedLanes)
    console.log('Manual: Found connected lanes count:', connectedLanesArray.length)
    
    if (connectedLanesArray.length > 0) {
      // 发出高亮事件（使用紫色高亮所有相连车道，空的toLanes表示没有选中特定方向）
      emit('highlight', connectedLanesArray, [])
      
      console.log('Manual: Junction lanes highlighted')
    }
    
  } catch (error) {
    console.error('Manual: Failed to get junction lanes:', error)
  }
}

const selectDirection = (index: number) => {


  selectedDirectionIndex.value = index
  isDirectionDropdownOpen.value = false
}

const handleClickOutside = (event: MouseEvent) => {
  const target = event.target as HTMLElement
  if (!target.closest('.select-wrapper')) {
    isJunctionDropdownOpen.value = false
    isDirectionDropdownOpen.value = false
  }
}

const onApply = async () => {
  const junction = currentJunction.value
  const lightIndex = selectedDirectionIndex.value
  if (!junction || lightIndex === null || duration.value === null) return

  isApplying.value = true
  const state = selectedLight.value === 'GREEN' ? 'G' : 'r'
  const steps = Math.floor(duration.value / 9)

  const requestBody = {
    junctionId: junction.junction_id,
    lightIndex,
    duration: steps,
    state,
    source: 'manual'
  }

  const currentDirection = currentDirections.value[lightIndex]
  const fromEdge = currentDirection?.fromEdgeName || 'Unknown'
  const toEdge = currentDirection?.toEdgeName || 'Unknown'
  const lightColor = selectedLight.value === 'GREEN' ? 'Green' : 'Red'
  const junctionName = junction.junction_name || junction.junction_id


  const recordId = operationStore.addRecord({
    description: `Set ${junctionName} light from ${fromEdge} to ${toEdge} to ${lightColor} for ${duration.value}s`,
    source: 'manual',
    junctionId: junction.junction_id,
    junctionName,
    lightIndex,
    state,
    duration: duration.value
  })

  try {
    const response = await apiClient.post('/signalcontrol/manual', requestBody)
    operationStore.updateRecordStatus(recordId, 'success')
    toast.success('Traffic light settings updated successfully!')

    const directionInfo = `${fromEdge} → ${toEdge}`
    emit('manualControlApplied', {
      junctionName,
      directionInfo,
      lightColor,
      duration: duration.value
    })

    partialResetForm()
  } catch (error) {
    console.error('Failed to update traffic light:', error)
    operationStore.updateRecordStatus(recordId, 'failed', 'Failed to send data to backend')
    toast.error('Failed to send data to backend.')
  } finally {
    isApplying.value = false
  }
}


const fetchLaneMappings = async () => {
  try {
    const response = await axios.get('/api-status/lane-mappings')
    const mappings = Array.isArray(response.data) ? response.data : Object.values(response.data)

    const nameMap: Record<string, string> = {}
    const shapeMap: Record<string, string> = {}

    mappings.forEach((m: LaneShapeInfo & { edgeName?: string }) => {
      nameMap[m.laneId] = m.edgeName || m.laneId
      shapeMap[m.laneId] = m.shape || ''
    })

    laneIdToEdgeName.value = nameMap
    laneIdToShape.value = shapeMap
  } catch (error) {
    console.error('Manual: Error:', error)
  }
}

const fetchJunctions = async () => {
  try {
    console.log('Manual: Fetching junctions')

    const [junctionResponse, tlsJunctionResponse] = await Promise.all([
      axios.get('/api-status/junctions'),
      axios.get('/api-status/tls-junctions')
    ])

    const junctionData = Object.values(junctionResponse.data)
    const tlsJunctionData = tlsJunctionResponse.data



    const junctionIdToCoords = new Map()
    tlsJunctionData.forEach((tls: any) => {
      if (tls.junctionId && tls.junctionX !== undefined && tls.junctionY !== undefined) {
        junctionIdToCoords.set(tls.junctionId, {
          junctionX: tls.junctionX,
          junctionY: tls.junctionY
        })
      }
    })

    junctionDataList.value = junctionData.map((junction: any) => {
      const coords = junctionIdToCoords.get(junction.junction_id) || { junctionX: 0, junctionY: 0 }
      return {
        tlsID: junction.tlsID || junction.tlsId,
        junction_id: junction.junction_id,
        junction_name: junction.junction_name,
        connection: junction.connection || [],
        junctionX: coords.junctionX,
        junctionY: coords.junctionY
      }
    })



    const junctionsWithCoords = junctionDataList.value.filter(j => j.junctionX !== 0 || j.junctionY !== 0)
    console.log('Manual: Junctions with coordinates count:', junctionsWithCoords.length)

  } catch (error) {
    console.error( error)
  }
}

const initMapCenter = async () => {
  try {
    const response = await axios.get('/api-status/lane-mappings')
    const data = Array.isArray(response.data) ? response.data : Object.values(response.data)

    let minX = Infinity, maxX = -Infinity
    data.forEach((lane: any) => {
      if (lane.laneShape) {
        const coordinates = lane.laneShape.trim().split(' ').map((p: string) => p.split(',').map(Number))
        coordinates.forEach((coord: number[]) => {
          if (coord.length >= 2) {
            minX = Math.min(minX, coord[0])
            maxX = Math.max(maxX, coord[0])
          }
        })
      }
    })

    if (minX !== Infinity && maxX !== -Infinity) {
      mapCenterX.value = (minX + maxX) / 2

    } else {
      mapCenterX.value = 0
    }
  } catch (error) {
    console.error( error)
    mapCenterX.value = 0
  }
}

onMounted(async () => {
  await initMapCenter()

  await fetchLaneMappings()
  await fetchJunctions()
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})

watch(selectedJunctionIndex, (newIndex, oldIndex) => {
  // 只有当真正改变时才处理
  if (newIndex !== oldIndex) {
    selectedDirectionIndex.value = null
    if (newIndex === null) {
      emit('trafficLightCleared')
    }
  }
})

const onlyAllowNumbers = (e: KeyboardEvent) => {
  const key = e.key
  if (!/[\d]/.test(key)) {
    e.preventDefault()
  }
}

const validateDuration = () => {
  durationDisplay.value = durationDisplay.value.replace(/[^\d]/g, '')
}

const handleBlur = () => {
  const val = parseInt(durationDisplay.value)
  if (!isNaN(val) && val >= 5 && val <= 300) {
    duration.value = val
    durationError.value = false
  } else {
    duration.value = null
    durationError.value = true
  }
}

const increaseDuration = () => {
  if (duration.value === null) duration.value = 5
  else if (duration.value < 300) duration.value++
  durationDisplay.value = duration.value.toString()
  durationError.value = false
}

const decreaseDuration = () => {
  if (duration.value && duration.value > 5) {
    duration.value--
  } else {
    duration.value = 5
  }
  durationDisplay.value = duration.value.toString()
  durationError.value = false
}
defineExpose({
  setJunctionByName: (name: string) => {

    const index = junctionDataList.value.findIndex(j =>
      j.junction_name === name || j.junction_id === name
    )
    if (index !== -1) {
      console.log('Manual: Found junction at index:', index)
      selectJunction(index)
    } else {
      console.warn('Manual: Junction not found:', name)

    }
  },

  setJunctionById: (id: string) => {

    const index = junctionDataList.value.findIndex(j => j.junction_id === id)
    if (index !== -1) {

      selectJunction(index)
    } else {
      console.warn('Manual: Junction ID not found:', id)
    }
  },

  clearSelection: () => {

    resetForm()
  },

  getJunctionNameById: (id: string) => {
    const junction = junctionDataList.value.find(j => j.junction_id === id)
    return junction ? (junction.junction_name || junction.junction_id) : null
  },

  selectJunctionById: (id: string) => {
    const index = junctionDataList.value.findIndex(j => j.junction_id === id)
    if (index !== -1) {
      selectJunction(index)
      console.log('Manual: Auto-selected junction:', junctionDataList.value[index].junction_name || id)
    }
  },

  forceRefreshPermissions: () => {
  }
})
</script>

<style scoped lang="scss">
.manual-control {
  width: 100%;
  height: 4.5rem;
  box-sizing: border-box;
  background-color: #1E1E2F;
  display: flex;
  flex-direction: column;
  padding-top: 0.2rem;
  padding-bottom: 0.4rem;
  gap: 0.24rem;
  flex-shrink: 0;
  position: relative;
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
}

@keyframes borderGlow {
  0% {
    opacity: 0.5;
    filter: blur(1px);
  }
  100% {
    opacity: 0.8;
    filter: blur(0px);
  }
}

.panel-title {
  font-size: 0.2rem;
  font-weight: 700;
  color: #00E5FF;
  margin-bottom: 0.1rem;
  padding-left: 0.24rem;
  line-height: 0.2rem;

  position: relative;
}

.form-row {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  padding-right: 0.24rem;
}

.label {
  width: 1.6rem;
  font-size: 0.14rem;
  color: #FFFFFF;
  font-weight: 600;
  padding-left: 0.4rem;

  letter-spacing: 0.02rem;
}

.light-buttons {
  display: flex;
  gap: 0.3rem;
}

.light-btn {
  width: 1rem;
  height: 0.4rem;
  border: none;
  border-radius: 0.08rem;
  font-weight: 700;
  font-size: 0.14rem;
  color: #FFFFFF;
  cursor: pointer;
  background: linear-gradient(135deg, #1E2139 0%, #2A2D4A 100%);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  position: relative;
  overflow: hidden;


  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
    transition: left 0.6s;
  }

  &:hover::before {
    left: 100%;
  }
}

.red {
  color: #FF4569;
  border-color: rgba(255, 69, 105, 0.3);

  &:hover:not(.active-red) {
    background: linear-gradient(135deg, #FF4569 20%, #2A2D4A 80%);
    color: #FFFFFF;
    transform: translateY(-2px) scale(1.02);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.4);
    border-color: rgba(255, 69, 105, 0.6);
  }
}

.green {
  color: #00E676;
  border-color: rgba(0, 230, 118, 0.3);

  &:hover:not(.active-green) {
    background: linear-gradient(135deg, #00E676 20%, #2A2D4A 80%);
    color: #FFFFFF;
    transform: translateY(-2px) scale(1.02);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.4);
    border-color: rgba(0, 230, 118, 0.6);
  }
}

.active-red {
  background: linear-gradient(135deg, #FF4569 0%, #E91E63 100%);
  color: #FFFFFF;
  border-color: rgba(255, 69, 105, 0.8);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.5);
  transform: translateY(-1px);
}

.active-green {
  background: linear-gradient(135deg, #00E676 0%, #4CAF50 100%);
  color: #FFFFFF;
  border-color: rgba(0, 230, 118, 0.8);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.5);
  transform: translateY(-1px);
}

.action-buttons {
  display: flex;
  justify-content: space-between;
  width: 4.2rem;
  margin: 0 auto;
  margin-top: -0.2rem;
}

.apply-btn,
.cancel-btn {
  width: 1.4rem;
  height: 0.4rem;
  font-size: 0.14rem;
  font-weight: 700;
  border-radius: 0.2rem;
  border: 1px solid;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  position: relative;
  overflow: hidden;
  text-shadow: 0 0 8px rgba(255, 255, 255, 0.3);

  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 0;
    height: 0;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 50%;
    transform: translate(-50%, -50%);
    transition: all 0.4s ease;
  }

  &:active::before {
    width: 300%;
    height: 300%;
  }
}

.apply-btn {
  background: linear-gradient(135deg, #00B4D8 0%, #0090aa 100%);
  color: #FFFFFF;
  border-color: rgba(0, 180, 216, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.08rem;

  .loading-spinner {
    width: 0.16rem;
    height: 0.16rem;
    border: 2px solid rgba(255, 255, 255, 0.3);
    border-top: 2px solid #FFFFFF;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  span {
    position: relative;
    z-index: 2;
    font-weight: 700;
  }

  &:not(:disabled):hover {
    background: linear-gradient(135deg, #00d4f8 0%, #00B4D8 100%);
    transform: translateY(-2px) scale(1.02);
    box-shadow: 0 8px 25px rgba(0, 180, 216, 0.4);
    border-color: rgba(0, 180, 216, 0.8);
  }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.apply-btn:disabled {
  background: linear-gradient(135deg, #4A5568 0%, #2D3748 100%);
  color: #A0AEC0;
  border-color: rgba(74, 85, 104, 0.5);
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
  text-shadow: none;
}

.cancel-btn {
  background: linear-gradient(135deg, #718096 0%, #4A5568 100%);
  color: #FFFFFF;
  border-color: rgba(113, 128, 150, 0.5);

  &:hover {
    background: linear-gradient(135deg, #A0AEC0 0%, #718096 100%);
    transform: translateY(-2px) scale(1.02);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
    border-color: rgba(113, 128, 150, 0.8);
  }
}

.select-wrapper {
  position: relative;
  flex: 1;
  max-width: calc(100% - 2rem - 0.3rem - 0.24rem);
  cursor: pointer;
  z-index: 100;

  &.dropdown-open {
    z-index: 1000;
  }
}

.custom-select {
  width: 100%;
  height: 0.4rem;
  background: linear-gradient(135deg, #1E2139 0%, #2A2D4A 100%);
  border: 1px solid rgba(74, 85, 104, 0.4);
  border-radius: 0.06rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 0.1rem;
  transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: transparent; /* 移除斜线条条效果 */
    opacity: 0;
    transition: opacity 0.3s ease;
    pointer-events: none;
  }

  &.open,
  &:hover {
    border-color: rgba(113, 128, 150, 0.6);
    box-shadow: 0 0 15px rgba(0, 0, 0, 0.2);
    background: linear-gradient(135deg, #2A2D4A 0%, #1E2139 100%);

    &::before {
      opacity: 1;
    }
  }

  &.open {
    box-shadow: 0 0 20px rgba(0, 0, 0, 0.3);
  }
}

.select-display {
  flex: 1;
  color: #E3F2FD;
  font-size: 0.14rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding-right: 0.1rem;
  font-weight: 500;

}

.select-arrow {
  width: 0.15rem;
  height: 0.15rem;
  color: #9CA3AF;
  transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  flex-shrink: 0;

  &.rotated {
    transform: rotate(180deg);
    color: #D1D5DB;
  }

  svg {
    width: 100%;
    height: 100%;
  }
}

.dropdown-panel {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: linear-gradient(135deg, #1E2139 0%, #2A2D4A 100%);
  border: 1px solid rgba(74, 85, 104, 0.4);
  border-top: none;
  border-radius: 0 0 0.06rem 0.06rem;
  z-index: 10;
  max-height: 2rem;
  overflow: hidden;
  box-shadow:
    0 8px 25px rgba(0, 0, 0, 0.4),
    0 0 20px rgba(0, 0, 0, 0.2);
  animation: slideDown 0.3s cubic-bezier(0.4, 0.0, 0.2, 1);
  backdrop-filter: blur(10px);
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.dropdown-search {
  padding: 0.08rem;
  border-bottom: 1px solid rgba(0, 180, 216, 0.2);
  background: linear-gradient(135deg, #0F1423 0%, #1A1F35 100%);
}

.search-input {
  width: 100%;
  height: 0.3rem;
  background: linear-gradient(135deg, #1E2139 0%, #2A2D4A 100%);
  border: 1px solid rgba(74, 85, 104, 0.3);
  border-radius: 0.06rem;
  color: #E3F2FD;
  font-size: 0.12rem;
  padding: 0 0.08rem;
  box-sizing: border-box;
  outline: none;
  transition: all 0.3s ease;
  font-weight: 500;

  &:focus {
    border-color: rgba(113, 128, 150, 0.6);
    box-shadow: 0 0 10px rgba(0, 0, 0, 0.2);
    background: linear-gradient(135deg, #2A2D4A 0%, #1E2139 100%);
  }

  &::placeholder {
    color: rgba(156, 163, 175, 0.6);
  }
}

.dropdown-options {
  max-height: 1.6rem;
  overflow-y: auto;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-track {
    background: rgba(15, 20, 35, 0.5);
  }

  &::-webkit-scrollbar-thumb {
    background: linear-gradient(to bottom, #4A5568, #374151);
    border-radius: 2px;
    box-shadow: 0 0 4px rgba(0, 0, 0, 0.4);
  }
}

.dropdown-option {
  padding: 0.08rem 0.12rem;
  color: #E3F2FD;
  font-size: 0.14rem;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0.0, 0.2, 1);
  border-left: 2px solid transparent;
  position: relative;
  font-weight: 500;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 0;
    background: linear-gradient(90deg, rgba(113, 128, 150, 0.1), transparent);
    transition: width 0.3s ease;
  }

  &:hover {
    background: rgba(74, 85, 104, 0.15);
    border-left-color: #9CA3AF;
    transform: translateX(4px);
    color: #FFFFFF;

    &::before {
      width: 100%;
    }
  }

  &.selected {
    background: rgba(113, 128, 150, 0.2);
    border-left-color: #D1D5DB;
    color: #FFFFFF;
    font-weight: 700;
    box-shadow: inset 0 1px 3px rgba(255, 255, 255, 0.2);

    &::before {
      width: 100%;
      background: linear-gradient(90deg, rgba(209, 213, 219, 0.2), transparent);
    }
  }
}

.direction-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.no-results {
  padding: 0.12rem;
  color: rgba(179, 229, 252, 0.6);
  font-size: 0.12rem;
  text-align: center;
  font-style: italic;
  font-weight: 500;
}

.duration-custom {
  display: flex;
  align-items: center;
  height: 0.4rem;
  border: 1px solid rgba(74, 85, 104, 0.4);
  border-radius: 0.06rem;
  background: linear-gradient(135deg, #1E2139 0%, #2A2D4A 100%);
  overflow: hidden;
  transition: all 0.4s ease;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: transparent; /* 移除斜线条条效果 */
    opacity: 0;
    transition: opacity 0.3s ease;
    pointer-events: none;
  }

  &:focus-within {
    border-color: rgba(113, 128, 150, 0.6);
    box-shadow: 0 0 15px rgba(0, 0, 0, 0.2);
    background: linear-gradient(135deg, #2A2D4A 0%, #1E2139 100%);

    &::before {
      opacity: 1;
    }
  }
}

.custom-input {
  height: inherit;
  width: 1.2rem;
  background: transparent;
  border: none;
  color: #E3F2FD;
  font-size: 0.14rem;
  padding: 0.1rem 0.2rem;
  text-align: center;
  outline: none;
  transition: all 0.3s ease;
  font-weight: 500;
  text-shadow: 0 0 8px rgba(227, 242, 253, 0.3);
  position: relative;
  z-index: 1;

  &::placeholder {
    color: rgba(156, 163, 175, 0.6);
    transition: color 0.3s ease;
  }
}

.triangle-buttons {
  display: flex;
  flex-direction: column;
  justify-content: center;
  height: 100%;
  background: linear-gradient(135deg, #1E2139 0%, #2A2D4A 100%);
  border-left: 1px solid rgba(74, 85, 104, 0.2);
}

.triangle-btn {
  flex: 1;
  width: 0.4rem;
  border: none;
  background: transparent;
  color: #9CA3AF;
  font-size: 0.14rem;
  cursor: pointer;
  line-height: 1;
  transition: all 0.4s ease;
  position: relative;
  font-weight: 700;

  &:hover {
    background: rgba(113, 128, 150, 0.15);
    color: #D1D5DB;
    transform: scale(1.2);
  }

  &:active {
    transform: scale(1.05);
    background: rgba(209, 213, 219, 0.2);
  }

  &:first-child {
    border-bottom: 1px solid rgba(74, 85, 104, 0.2);
  }
}

.form-row-duration {
  display: flex;
  flex-direction: column;
  gap: 0.06rem;
}

.duration-error-container {
  height: 0.18rem;
  display: flex;
  align-items: flex-start;
}

.duration-error {
  color: #EF4444;
  font-size: 0.1rem;
  padding: 0.02rem 0.06rem;
  margin-left: 1.9rem;
  white-space: nowrap;
  font-weight: 600;
  display: inline-block;
  line-height: 1;
  height: auto;
  max-width: 3rem;
}

select.common-box {
  display: none;
}
</style>
