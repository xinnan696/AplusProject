<template>
  <div class="suggested-actions">
    <div class="panel-title">
      <span>AI Suggestions</span>
    </div>

    <div class="action-box" :class="{
      'no-suggestion': !hasValidSuggestion,
      'suggestion-changing': isChanging,
      'ai-mode': isAIMode
    }">
      <Transition name="fade" mode="out-in">
        <div v-if="hasValidSuggestion" key="suggestion">
          <p class="placeholder-text">
            Set <strong>{{ displayData.junctionName }}</strong>
            light from <strong>{{ displayData.fromEdgeName }}</strong> to <strong>{{ displayData.toEdgeName }}</strong>
            to <strong>{{ displayData.stateName }}</strong> for <strong>{{ suggestionData?.duration }}s</strong>.
          </p>
        </div>
        <div v-else-if="isLoading" key="loading">
          <p class="placeholder-text">
            Loading suggestion data...
          </p>
        </div>
        <div v-else key="no-suggestion">
          <p class="placeholder-text">
            No Available Suggestion.
          </p>
        </div>
      </Transition>
    </div>

    <div class="action-buttons" v-if="!isAIMode">
      <button
        ref="applyBtnRef"
        class="apply-btn"
        :disabled="!hasValidSuggestion || isApplying || isLoading"
        @click="handleApply"
      >
        <div v-if="isApplying" class="loading-spinner"></div>
        <span>{{ isApplying ? 'APPLYING...' : 'APPLY' }}</span>
      </button>
      <button
        class="ignore-btn"
        :disabled="!hasValidSuggestion || isApplying || isLoading"
        @click="handleIgnore"
      >
        IGNORE
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import axios from 'axios'
import apiClient from '@/utils/api'
import { toast } from '@/utils/ToastService'
import { useOperationStore } from '@/stores/operationStore'

interface AISuggestion {
  junctionId: string
  fromlaneid: string
  tolaneid: string
  state: string
  duration: number
}

interface DisplayData {
  junctionName: string
  fromEdgeName: string
  toEdgeName: string
  stateName: string
  lightIndex?: number
}

interface Junction {
  tlsID: string
  junction_id: string
  junction_name: string
  timestamp: number
  phase: number
  state: string
  duration: number
  connection: string[][][]
  spendTime: number
  nextSwitchTime: number
}

interface Edge {
  edgeID: string
  edgeName: string
  timestamp: number
  laneNumber: number
  speed: number
  vehicleCount: number
  vehicleIDs: string[]
  waitTime: number
  waitingVehicleCount: number
}

interface LaneMapping {
  laneId: string
  edgeId: string
}

interface Props {
  isAIMode?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isAIMode: false
})

const operationStore = useOperationStore()
const suggestionData = ref<AISuggestion | null>(null)
const displayData = ref<DisplayData & { lightIndex: number }>({
  junctionName: '',
  fromEdgeName: '',
  toEdgeName: '',
  stateName: '',
  lightIndex: 0
})

const isApplying = ref(false)
const isChanging = ref(false)
const isLoading = ref(false)
const countdownProgress = ref(100)
const remainingTime = ref(5000)


const junctionsCache = ref<Map<string, Junction>>(new Map())
const edgesCache = ref<Map<string, Edge>>(new Map())
const laneMappingsCache = ref<Map<string, string>>(new Map())

let timer: ReturnType<typeof setTimeout> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null
let autoApplyTimer: ReturnType<typeof setTimeout> | null = null

const hasValidSuggestion = computed(() => {
  return !!(suggestionData.value &&
    displayData.value.junctionName &&
    displayData.value.fromEdgeName &&
    displayData.value.toEdgeName &&
    suggestionData.value.duration)
})


const stateMap: Record<string, string> = {
'G': 'Green',
'g': 'Green',
'R': 'Red',
'r': 'Red',
'Y': 'Yellow',
'y': 'Yellow'
}

const findLightIndex = (junction: Junction | undefined, fromlaneid: string, tolaneid: string): number => {
  if (!junction || !junction.connection) {
    console.warn(`Junction ${junction?.junction_id} fail`)
    return 0
  }

  for (let i = 0; i < junction.connection.length; i++) {
    const connectionGroup = junction.connection[i]

    for (let j = 0; j < connectionGroup.length; j++) {
      const conn = connectionGroup[j]
      if (conn.length >= 2 && conn[0] === fromlaneid && conn[1] === tolaneid) {
        console.log(`找到匹配的lightIndex: ${i}, 方向: ${fromlaneid} -> ${tolaneid}`)
        return i
      }
    }
  }

  return 0
}

const initializeCache = async (): Promise<boolean> => {
  try {

    const [junctionsRes, edgesRes, laneMappingsRes] = await Promise.all([
      apiClient.get('/api-status/junctions'),
      apiClient.get('/api-status/edges'),
      apiClient.get('/api-status/lane-mappings')
    ])

    if (junctionsRes.data) {
      Object.values(junctionsRes.data).forEach((junction: any) => {
        junctionsCache.value.set(junction.junction_id, junction)
      })
    }

    if (edgesRes.data) {
      Object.values(edgesRes.data).forEach((edge: any) => {
        edgesCache.value.set(edge.edgeID, edge)
      })
    }

    if (laneMappingsRes.data && Array.isArray(laneMappingsRes.data)) {
      laneMappingsRes.data.forEach((mapping: LaneMapping) => {
        laneMappingsCache.value.set(mapping.laneId, mapping.edgeId)
      })
    }

    console.log( {
      junctions: junctionsCache.value.size,
      edges: edgesCache.value.size,
      laneMappings: laneMappingsCache.value.size
    })

    return true
  } catch (error) {
    console.error( error)
    toast.error('fail')
    return false
  }
}

const convertSuggestionToDisplay = async (suggestion: AISuggestion): Promise<DisplayData & { lightIndex: number }> => {
  try {
    const junction = junctionsCache.value.get(suggestion.junctionId)
    const junctionName = junction?.junction_name || `Junction_${suggestion.junctionId}`

    const fromEdgeId = laneMappingsCache.value.get(suggestion.fromlaneid)
    const fromEdge = fromEdgeId ? edgesCache.value.get(fromEdgeId) : null
    const fromEdgeName = fromEdge?.edgeName || suggestion.fromlaneid


    const toEdgeId = laneMappingsCache.value.get(suggestion.tolaneid)
    const toEdge = toEdgeId ? edgesCache.value.get(toEdgeId) : null
    const toEdgeName = toEdge?.edgeName || suggestion.tolaneid

    const stateName = stateMap[suggestion.state] || suggestion.state

    const lightIndex = findLightIndex(junction, suggestion.fromlaneid, suggestion.tolaneid)

    return {
      junctionName,
      fromEdgeName,
      toEdgeName,
      stateName,
      lightIndex
    }
  } catch (error) {
    console.error('转换建议数据失败:', error)
    return {
      junctionName: `Junction_${suggestion.junctionId}`,
      fromEdgeName: suggestion.fromlaneid,
      toEdgeName: suggestion.tolaneid,
      stateName: suggestion.state,
      lightIndex: 0
    }
  }
}

const getNextSuggestion = async (): Promise<AISuggestion | null> => {
  // TODO:
  // try {
  //   const response = await apiClient.get('/your-ai-endpoint')
  //   return response.data
  // } catch (error) {
  //   console.error( error)
  //   return null
  // }

  return null
}

const clearAllTimers = () => {
  if (timer) {
    clearTimeout(timer)
    timer = null
  }
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  if (autoApplyTimer) {
    clearTimeout(autoApplyTimer)
    autoApplyTimer = null
  }
}

const startAutoApplyCountdown = () => {
  remainingTime.value = 5000
  countdownProgress.value = 100

  countdownTimer = setInterval(() => {
    remainingTime.value -= 100
    countdownProgress.value = (remainingTime.value / 5000) * 100

    if (remainingTime.value <= 0) {
      clearInterval(countdownTimer!)
      countdownTimer = null
    }
  }, 100)

  autoApplyTimer = setTimeout(() => {
    if (suggestionData.value && !isApplying.value) {
      console.log('Auto-applying suggestion in AI mode')
      handleApply(true)
    }
  }, 5000)
}

const showSuggestion = async (isTimeout = true) => {
  clearAllTimers()
  isChanging.value = true
  isLoading.value = true

  await new Promise(resolve => setTimeout(resolve, 100))

  try {
    const next = await getNextSuggestion()
    suggestionData.value = next

    if (next) {
      // 转换为显示数据
      displayData.value = await convertSuggestionToDisplay(next)
    } else {
      // 清空显示数据
      displayData.value = {
        junctionName: '',
        fromEdgeName: '',
        toEdgeName: '',
        stateName: '',
        lightIndex: 0
      }
    }
  } catch (error) {
    console.error('获取建议失败:', error)
    suggestionData.value = null
    displayData.value = {
      junctionName: '',
      fromEdgeName: '',
      toEdgeName: '',
      stateName: '',
      lightIndex: 0
    }
  } finally {
    isLoading.value = false

    setTimeout(() => {
      isChanging.value = false
    }, 300)
  }

  if (suggestionData.value) {
    if (props.isAIMode) {
      startAutoApplyCountdown()
    } else {
      timer = setTimeout(() => {
        showSuggestion(true)
      }, 20000)
    }
  }
}

watch(() => props.isAIMode, (newValue, oldValue) => {
  console.log('AI mode changed:', oldValue, '->', newValue)

  if (newValue) {
    console.log('Switching to AI mode - Auto-apply enabled')
    if (suggestionData.value) {
      startAutoApplyCountdown()
    }
  } else {
    console.log('Switching to Manual mode - Auto-apply disabled')
    clearAllTimers()
    if (suggestionData.value) {
      timer = setTimeout(() => {
        showSuggestion(true)
      }, 20000)
    }
  }
})

const handleApply = async (isAutoApply = false) => {
  if (!suggestionData.value) return

  if (isAutoApply) {
    clearAllTimers()
  }

  isApplying.value = true
  const suggestion = suggestionData.value

  // TODO: 需要根据实际后端接口调整payload结构
  const payload = {
    junctionId: suggestion.junctionId,
    lightIndex: displayData.value.lightIndex,
    state: suggestion.state,
    duration: suggestion.duration,
    source: isAutoApply ? "ai" : "manual"
  }

  const description = `${isAutoApply ? '[AI]' : '[Manual]'} Set ${displayData.value.junctionName} light from ${displayData.value.fromEdgeName} to ${displayData.value.toEdgeName} to ${displayData.value.stateName} for ${suggestion.duration}s`

  const recordId = operationStore.addRecord({
    description,
    source: isAutoApply ? 'ai' : 'manual',
    junctionId: suggestion.junctionId,
    junctionName: displayData.value.junctionName,
    lightIndex: displayData.value.lightIndex, // 使用计算出的lightIndex
    state: suggestion.state,
    duration: suggestion.duration
  })

  try {
    await apiClient.post('/api/signalcontrol/manual', payload)
    operationStore.updateRecordStatus(recordId, 'success')

    if (isAutoApply) {
      console.log('AI suggestion auto-applied successfully')
    } else {
      toast.success('Traffic light settings updated successfully!')
    }
  } catch {
    operationStore.updateRecordStatus(recordId, 'failed', 'Failed to send data to backend')

    if (isAutoApply) {
      console.error('AI suggestion auto-apply failed')
    } else {
      toast.error('Failed to send data to backend.')
    }
  } finally {
    isApplying.value = false
  }

  showSuggestion(false)
}

const handleIgnore = () => {
  clearAllTimers()
  showSuggestion(false)
}

onMounted(async () => {
  const cacheInitialized = await initializeCache()
  if (cacheInitialized) {
    showSuggestion()
  }
})

onBeforeUnmount(() => {
  clearAllTimers()
})


</script>

<style scoped lang="scss">
.suggested-actions {
  width: 100%;
  height: 3.08rem;
  box-sizing: border-box;
  background-color: #1E1E2F;
  border-bottom: 0.01rem solid #3A3A4C;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background:
      radial-gradient(circle at 20% 20%, rgba(0, 180, 216, 0.05) 0%, transparent 50%),
      radial-gradient(circle at 80% 80%, rgba(0, 200, 255, 0.03) 0%, transparent 50%),
      linear-gradient(45deg, transparent 48%, rgba(0, 180, 216, 0.02) 49%, rgba(0, 180, 216, 0.02) 51%, transparent 52%);
    pointer-events: none;
    z-index: 0;
  }

  > * {
    position: relative;
    z-index: 1;
  }
}

.panel-title {
  font-size: 0.2rem;
  font-weight: 700;
  color: #00E5FF;
  margin-bottom: 0.1rem;
  padding-left: 0.24rem;
  padding-top: 0.2rem;
  padding-bottom: 0.16rem;
  line-height: 0.2rem;
  text-shadow: 0 0 10px rgba(0, 229, 255, 0.5);
  position: relative;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 3px;
    height: 0.2rem;
    background: linear-gradient(to bottom, #00E5FF, #00B4D8);
    border-radius: 2px;
    box-shadow: 0 0 8px rgba(0, 229, 255, 0.6);
  }
}

.action-box {
  height: 1.5rem;
  background: linear-gradient(135deg, #1E2139 0%, #2A2D4A 100%);
  border: 1px solid rgba(0, 180, 216, 0.4);
  border-radius: 0.08rem;
  padding: 0.16rem 0.24rem;
  margin: 0 0.24rem 0.26rem 0.24rem;
  box-sizing: border-box;
  position: relative;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);

  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;

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

  &:hover {
    border-color: rgba(0, 229, 255, 0.6);
    box-shadow:
      0 0 15px rgba(0, 180, 216, 0.2),
      inset 0 1px 3px rgba(0, 229, 255, 0.1);
    background: linear-gradient(135deg, #2A2D4A 0%, #1E2139 100%);

    &::before {
      opacity: 1;
    }
  }


  &:not(.no-suggestion) {
    box-shadow:
      0 0 12px rgba(0, 229, 255, 0.2),
      inset 0 1px 3px rgba(0, 229, 255, 0.15);
    animation: aiPulse 3s ease-in-out infinite;

    &::before {
      opacity: 0.3;
    }
  }


  &.suggestion-changing {
    transform: scale(0.98);
    box-shadow:
      0 0 18px rgba(0, 229, 255, 0.25),
      inset 0 1px 3px rgba(0, 229, 255, 0.2);

    &::before {
      opacity: 0.5;
    }
  }


  &.no-suggestion {
    background: linear-gradient(135deg, #2A2D4A 0%, #1E2139 100%);
    border-color: rgba(113, 128, 150, 0.3);

    &::before {
      background: linear-gradient(45deg, transparent 48%, rgba(113, 128, 150, 0.1) 49%, rgba(113, 128, 150, 0.1) 51%, transparent 52%);
    }
  }


  &.ai-mode {
    border-color: rgba(0, 150, 255, 0.6);
    background: linear-gradient(135deg, #1E2A39 0%, #2A3A4A 100%);
    box-shadow:
      0 0 15px rgba(0, 150, 255, 0.3),
      inset 0 1px 3px rgba(0, 150, 255, 0.15);
    animation: aiModeGlow 2s ease-in-out infinite;

    &::before {
      background: linear-gradient(45deg, transparent 48%, rgba(0, 150, 255, 0.1) 49%, rgba(0, 150, 255, 0.1) 51%, transparent 52%);
      opacity: 0.5;
    }

    .placeholder-text {
      color: #e8f0ff;

      strong {
        color: #66b3ff;
        text-shadow: 0 0 8px rgba(102, 179, 255, 0.4);
      }
    }
  }
}

@keyframes aiPulse {
  0%, 100% {
    box-shadow:
      0 0 12px rgba(0, 229, 255, 0.2),
      inset 0 1px 3px rgba(0, 229, 255, 0.15);
  }
  50% {
    box-shadow:
      0 0 20px rgba(0, 229, 255, 0.3),
      inset 0 1px 3px rgba(0, 229, 255, 0.2);
  }
}

@keyframes aiModeGlow {
  0%, 100% {
    box-shadow:
      0 0 15px rgba(0, 150, 255, 0.3),
      inset 0 1px 3px rgba(0, 150, 255, 0.15);
  }
  50% {
    box-shadow:
      0 0 25px rgba(0, 150, 255, 0.4),
      inset 0 1px 3px rgba(0, 150, 255, 0.25);
  }
}

.placeholder-text {
  color: #E3F2FD;
  font-size: 0.16rem;
  line-height: 1.5;
  text-align: center;
  margin: 0.1rem 0;
  padding: 0 0.24rem;
  word-break: break-word;
  position: relative;
  z-index: 3;
  text-shadow: 0 0 8px rgba(227, 242, 253, 0.3);
  font-weight: 500;
  letter-spacing: 0.5px;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  transform: translateY(0);
  opacity: 1;

  strong {
    font-weight: 700;
    color: #00FFFF;
    text-shadow: 0 0 6px rgba(0, 255, 255, 0.3);
    position: relative;
    animation: textGlow 2s ease-in-out infinite alternate;
    font-size: 0.17rem;
  }


  .action-box.no-suggestion & {
    color: rgba(179, 229, 252, 0.6);
    font-style: italic;
    text-shadow: none;

    strong {
      color: rgba(179, 229, 252, 0.6);
      text-shadow: none;
      animation: none;
    }
  }
}

.fade-enter-active {
  transition: all 0.6s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.fade-leave-active {
  transition: all 0.4s cubic-bezier(0.55, 0.055, 0.675, 0.19);
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(15px) scale(0.9);
  filter: blur(3px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-15px) scale(0.9);
  filter: blur(3px);
}

.fade-enter-to,
.fade-leave-from {
  opacity: 1;
  transform: translateY(0) scale(1);
  filter: blur(0);
}

@keyframes textGlow {
  0% {
    text-shadow: 0 0 6px rgba(0, 255, 255, 0.3);
  }
  100% {
    text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
  }
}

.action-buttons {
  display: flex;
  justify-content: space-between;
  width: 4.2rem;
  margin: 0 auto;
}

.apply-btn,
.ignore-btn {
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
  background: linear-gradient(135deg, #00E5FF 0%, #00B4D8 100%);
  color: #FFFFFF;
  border-color: rgba(0, 229, 255, 0.5);
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
  }

  &:not(:disabled):hover {
    background: linear-gradient(135deg, #00FFFF 0%, #00E5FF 100%);
    transform: translateY(-2px) scale(1.02);
    box-shadow:
      0 8px 25px rgba(0, 229, 255, 0.4),
      0 0 30px rgba(0, 229, 255, 0.3);
    border-color: rgba(0, 229, 255, 0.8);
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

.ignore-btn {
  background: linear-gradient(135deg, #718096 0%, #4A5568 100%);
  color: #FFFFFF;
  border-color: rgba(113, 128, 150, 0.5);

  &:hover {
    background: linear-gradient(135deg, #A0AEC0 0%, #718096 100%);
    transform: translateY(-2px) scale(1.02);
    box-shadow:
      0 6px 20px rgba(113, 128, 150, 0.3),
      0 0 20px rgba(113, 128, 150, 0.2);
    border-color: rgba(113, 128, 150, 0.8);
  }

  &:disabled {
    background: linear-gradient(135deg, #4A5568 0%, #2D3748 100%);
    cursor: not-allowed;
    color: #A0AEC0;
    opacity: 0.5;
    transform: none;
    box-shadow: none;
    text-shadow: none;
  }
}
</style>
