<template>
  <div class="manual-control">
    <div class="panel-title">Manual Control</div>

    <!-- Junction 选择 -->
    <!-- Junction 选择 -->
<div class="form-row">
  <label class="label">Junction</label>
  <select class="common-box" v-model="selectedJunctionIndex">
    <option disabled value="">Please Select a Junction</option> <!-- 新增默认选项 -->
    <option
      v-for="(junction, index) in junctionDataList"
      :key="index"
      :value="index"
    >
      {{ junction.junction_name || junction.junction_id }}
    </option>
  </select>
</div>


    <!-- Traffic Light 方向选择 -->
    <div class="form-row">
      <label class="label">Traffic Light</label>
      <select class="common-box" v-model="selectedDirectionIndex">
  <option
    v-for="(dir, idx) in currentDirections"
    :key="idx"
    :value="idx"
    :title="dir.fromEdgeName + ' → ' + dir.toEdgeName"
  >
    {{ dir.fromEdgeName }} → {{ dir.toEdgeName }}
  </option>
</select>

    </div>

    <!-- 灯光状态 -->
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

    <!-- Duration -->
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
  <!-- 错误提示区域 -->
  <div class="duration-error" v-if="durationError">The value must be between 5 and 300.</div>
</div>


    <!-- 操作按钮 -->
    <div class="action-buttons">
      <button class="apply-btn" :disabled="!isFormComplete" @click="onApply">APPLY</button>
      <button class="cancel-btn" @click="resetForm">CANCEL</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, watchEffect, defineEmits } from 'vue'
import axios from 'axios'
import { toast } from '@/utils/ToastService'
import vSelect from 'vue-select'
import 'vue-select/dist/vue-select.css'


const emit = defineEmits<{
  (e: 'highlight', fromLanes: string[], toLanes: string[]): void
}>()

type LaneConnectionGroup = string[][]

interface RawJunctionData {
  tlsID: string
  junction_id: string
  junction_name: string
  connection: LaneConnectionGroup[]
}

interface LaneShapeInfo {
  laneId: string
  shape: string // e.g. "x1,y1 x2,y2 ..."
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

watch(selectedDirectionIndex, () => {
  const idx = selectedDirectionIndex.value
  if (idx !== null && directionLanes.value[idx]) {
    emit('highlight', directionLanes.value[idx].from, directionLanes.value[idx].to)
  }
})

const isFormComplete = computed(() =>
  selectedJunctionIndex.value !== null &&
  selectedDirectionIndex.value !== null &&
  selectedLight.value !== '' &&
  duration.value !== null&&
  duration.value >= 5 &&
  duration.value <= 300 &&
  !durationError.value
)

const selectLight = (color: string) => {
  selectedLight.value = color
}

const resetForm = () => {
  selectedJunctionIndex.value = null
  selectedDirectionIndex.value = null
  selectedLight.value = ''
  duration.value = null
  durationDisplay.value = ''
  durationError.value = false
}

const onApply = async () => {
  const junction = currentJunction.value
  const lightIndex = selectedDirectionIndex.value
  if (!junction || lightIndex === null || duration.value === null) return

  const state = selectedLight.value === 'GREEN' ? 'G' : 'r'
  const requestBody = {
    junctionId: junction.junction_id,
    lightIndex,
    duration: duration.value,
    state,
    source: 'manual'
  }

  try {
    await axios.post('/api/signalcontrol/manual', requestBody)
    toast.success('Traffic light settings updated successfully!')
    resetForm()
  } catch (error) {
    console.error('发送控制请求失败:', error)
    toast.error('Failed to send data to backend.')
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
    console.error('获取 lane 映射失败:', error)
  }
}

const fetchJunctions = async () => {
  try {
    const response = await axios.get('/api-status/junctions')
    junctionDataList.value = Object.values(response.data)
  } catch (error) {
    console.error('获取路口失败:', error)
  }
}

onMounted(async () => {
  await fetchLaneMappings()
  await fetchJunctions()
})

watch(selectedJunctionIndex, () => {
  selectedDirectionIndex.value = null
})

// -------- 输入限制与校验逻辑 --------

// 只允许输入数字字符
const onlyAllowNumbers = (e: KeyboardEvent) => {
  const key = e.key
  if (!/[\d]/.test(key)) {
    e.preventDefault()
  }
}

// 输入时去除非数字字符
const validateDuration = () => {
  durationDisplay.value = durationDisplay.value.replace(/[^\d]/g, '')
}

// 失去焦点时判断是否在 5~300 范围
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

// 可选：上下按钮修改时清除错误提示
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
    const index = junctionDataList.value.findIndex(j => j.junction_name === name)
    if (index !== -1) {
      selectedJunctionIndex.value = index
    }
  }
})
</script>








<style scoped lang="scss">
.manual-control {
  width: 100%;
  height: 4.3rem;
  box-sizing: border-box;
  background-color: #1E1E2F;
  display: flex;
  flex-direction: column;
  padding-top: 0.2rem;
  padding-bottom: 0.4rem;
  gap: 0.24rem;
  flex-shrink: 0;
}

.panel-title {
  font-size: 0.18rem;
  font-weight: bold;
  color: #00B4D8;
  margin-bottom: 0.1rem;
  padding-left: 0.24rem;
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
  color: white;
  font-weight: bold;
  padding-left: 0.24rem;
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
  font-weight: bold;
  font-size: 0.14rem;
  color: #FFFFFF;
  cursor: pointer;
  background-color: #2B2C3D;
  transition: background-color 0.2s ease;
}

.red {
  color: #E63946;
}

.green {
  color: #2A9D8F;
}

.active-red {
  background-color: #E63946;
  color: #FFFFFF;
  border: none;
}

.active-green {
  background-color: #2A9D8F;
  color: #FFFFFF;
  border: none;
}

// hover
.light-btn:hover:not(.active-red):not(.active-green){
  background-color: #41425A;
}

.action-buttons {
  display: flex;
  justify-content: space-between;
  width: 4.2rem;
  margin: 0 auto;
}

.apply-btn,
.cancel-btn {
  width: 1.4rem;
  height: 0.4rem;
  font-size: 0.14rem;
  font-weight: bold;
  border-radius: 0.2rem;
  border: none;
  cursor: pointer;
}

.apply-btn {
  background-color: #00B4D8;
  color: #FFFFFF;
}

.apply-btn:disabled {
  background-color: #00B4D8;
  color: #FFFFFF;
  cursor: not-allowed;
}

.cancel-btn {
  background-color: #999999;
  color: #FFFFFF;
}


select.common-box {
  width: 3.6rem; // 固定宽度
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

select.common-box option {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}


.duration-custom {
  display: flex;
  align-items: center;
  height: 0.4rem;
  border: 0.01rem solid #00B4D8;
  border-radius: 0.06rem;
  background-color: #2B2C3D;
  overflow: hidden;
}

.custom-input {
  height: inherit;
  width: 1.2rem;
  background: transparent;
  border: none;
  color: #FFFFFF;
  font-size: 0.14rem;
  padding: 0.1rem 0.2rem;
  text-align: center;
  outline: none;
}

.triangle-buttons {
  display: flex;
  flex-direction: column;
  justify-content: center;
  height: 100%;
  background-color: #2B2C3D;
}

.triangle-btn {
  flex: 1;
  width: 0.4rem;
  border: none;
  background: none;
  color: #FFFFFF;
  font-size: 0.14rem;
  cursor: pointer;
  line-height: 1;
}

.triangle-btn:hover {
  background-color: #3A3B4C;
}
.duration-error {
  color: #d9003c;
  font-size: 0.08rem;
  margin-left: 0.1rem;
  white-space: nowrap;
}


</style>
