<template>
  <div class="suggested-actions" :class="{ 'ai-mode-container': isAIMode }">
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
            <span class="suggestion-line">
              Set <strong>{{ displayData.junctionName }}</strong> light
            </span>
            <span class="suggestion-line">
              from <strong>{{ displayData.fromEdgeName }}</strong> to <strong>{{ displayData.toEdgeName }}</strong>
            </span>
            <span class="suggestion-line">
              to <strong>{{ displayData.stateName }}</strong> for <strong>{{ suggestionData?.duration }}s</strong>
            </span>
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
import { AI_SUGGESTION_URL } from '@/config/api'

interface AISuggestion {
  junction: string
  target_light_from: string
  target_light_to: string
  target_state: string
  duration: number
}

interface BatchSuggestionsResponse {
  batch_suggestions: any[][]
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

const pollingTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const batchRefreshTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const BATCH_REFRESH_INTERVAL = 10000
const processedSuggestions = ref<Set<string>>(new Set())
const currentBatchIndex = ref(0)
const currentBatchSuggestions = ref<AISuggestion[]>([])
const lastBatchHash = ref<string>('')
const currentDisplayIndex = ref(0)


// 状态持久化相关
const STORAGE_KEY = 'ai_suggestions_state'
const junctionsCache = ref<Map<string, Junction>>(new Map())
const edgesCache = ref<Map<string, Edge>>(new Map())
const laneMappingsCache = ref<Map<string, string>>(new Map())

let timer: ReturnType<typeof setTimeout> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null
let autoApplyTimer: ReturnType<typeof setTimeout> | null = null
let batchRefreshCountdownTimer: ReturnType<typeof setInterval> | null = null

// 保存状态到localStorage
const saveStateToStorage = () => {
  try {
    const state = {
      suggestionData: suggestionData.value,
      displayData: displayData.value,
      currentBatchSuggestions: currentBatchSuggestions.value,
      processedSuggestions: Array.from(processedSuggestions.value),
      currentBatchIndex: currentBatchIndex.value,
      currentDisplayIndex: currentDisplayIndex.value,
      lastBatchHash: lastBatchHash.value
    }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
  } catch (error) {
    console.error('Failed to save state to storage:', error)
  }
}

// 从localStorage恢复状态
const loadStateFromStorage = (): boolean => {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) {
      const state = JSON.parse(saved)
      
      suggestionData.value = state.suggestionData
      displayData.value = state.displayData || {
        junctionName: '',
        fromEdgeName: '',
        toEdgeName: '',
        stateName: '',
        lightIndex: 0
      }
      currentBatchSuggestions.value = state.currentBatchSuggestions || []
      processedSuggestions.value = new Set(state.processedSuggestions || [])
      currentBatchIndex.value = state.currentBatchIndex || 0
      currentDisplayIndex.value = state.currentDisplayIndex || 0
      lastBatchHash.value = state.lastBatchHash || ''
      console.log('AI suggestions state restored from storage')
      return true
    }
  } catch (error) {
    console.error('Failed to load state from storage:', error)
    localStorage.removeItem(STORAGE_KEY)
  }
  return false
}

// 清除保存的状态
const clearStoredState = () => {
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch (error) {
    console.error('Failed to clear stored state:', error)
  }
}
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
        return i
      }
    }
  }

  return 0
}

const initializeCache = async (): Promise<boolean> => {
  try {
    const [junctionsRes, edgesRes, laneMappingsRes] = await Promise.all([
      axios.get('/api-status/junctions'),
      axios.get('/api-status/edges'),
      axios.get('/api-status/lane-mappings')
    ])

    if (junctionsRes.data) {
      if (Array.isArray(junctionsRes.data)) {
        junctionsRes.data.forEach((junction: any) => {
          junctionsCache.value.set(junction.junction_id, junction)
        })
      } else if (typeof junctionsRes.data === 'object') {
        Object.values(junctionsRes.data).forEach((junction: any) => {
          junctionsCache.value.set(junction.junction_id, junction)
        })
      }
    }

    if (edgesRes.data) {
      if (Array.isArray(edgesRes.data)) {
        edgesRes.data.forEach((edge: any) => {
          edgesCache.value.set(edge.edgeID, edge)
        })
      } else if (typeof edgesRes.data === 'object') {
        Object.values(edgesRes.data).forEach((edge: any) => {
          edgesCache.value.set(edge.edgeID, edge)
        })
      }
    }

    if (laneMappingsRes.data && Array.isArray(laneMappingsRes.data)) {
      laneMappingsRes.data.forEach((mapping: LaneMapping) => {
        laneMappingsCache.value.set(mapping.laneId, mapping.edgeId)
      })
    }

    const finalStats = {
      junctions: junctionsCache.value.size,
      edges: edgesCache.value.size,
      laneMappings: laneMappingsCache.value.size
    }





    return finalStats.junctions > 0 && finalStats.edges > 0
  } catch (error) {
    console.error( error)

    return true
  }
}

const convertSuggestionToDisplay = async (suggestion: AISuggestion): Promise<DisplayData & { lightIndex: number }> => {
  try {

    const junction = junctionsCache.value.get(suggestion.junction)
    const junctionName = junction?.junction_name || `Junction_${suggestion.junction}`


    const fromEdgeId = laneMappingsCache.value.get(suggestion.target_light_from)

    const fromEdge = fromEdgeId ? edgesCache.value.get(fromEdgeId) : null

    const fromEdgeName = fromEdge?.edgeName || suggestion.target_light_from

    const toEdgeId = laneMappingsCache.value.get(suggestion.target_light_to)

    const toEdge = toEdgeId ? edgesCache.value.get(toEdgeId) : null

    const toEdgeName = toEdge?.edgeName || suggestion.target_light_to


    const stateName = stateMap[suggestion.target_state] || suggestion.target_state || 'Green'


    const lightIndex = findLightIndex(junction, suggestion.target_light_from, suggestion.target_light_to)

    const result = {
         junctionName,
         fromEdgeName,
         toEdgeName,
         stateName,
         lightIndex
       }


    return result
  } catch (error) {
    console.error( error)
    return {
      junctionName: `Junction_${suggestion.junction}`,
      fromEdgeName: suggestion.target_light_from,
      toEdgeName: suggestion.target_light_to,
      stateName: suggestion.target_state || 'Green',
      lightIndex: 0
    }
  }
}


const getSuggestionId = (suggestion: AISuggestion): string => {
  return `${suggestion.junction}-${suggestion.target_light_from}-${suggestion.target_light_to}`
}


const generateBatchHash = (suggestions: AISuggestion[]): string => {
  const sortedIds = suggestions
    .map(s => getSuggestionId(s) + '-' + s.duration + '-' + s.target_state)
    .sort()
    .join('|')
  return btoa(sortedIds)
}

const fetchBatchSuggestions = async (): Promise<AISuggestion[]> => {
    const response = await axios.get('/api/traffic/suggestion')
    const data = response.data
    const validSuggestions: AISuggestion[] = []

    if (data.batch_suggestions && Array.isArray(data.batch_suggestions)) {
      for (const batch of data.batch_suggestions) {
        if (Array.isArray(batch) && batch.length > 0) {
          const suggestion = batch[0]

          if (suggestion.suggestion_label > 0 &&
              suggestion.junction &&
              suggestion.target_light_from &&
              suggestion.target_light_to &&
              suggestion.duration > 0) {

            const validSuggestion: AISuggestion = {
              junction: suggestion.junction,
              target_light_from: suggestion.target_light_from,
              target_light_to: suggestion.target_light_to,
              target_state: suggestion.target_state || 'G',
              duration: suggestion.duration
            }
            const suggestionId = getSuggestionId(validSuggestion)
            if (!processedSuggestions.value.has(suggestionId)) {
              validSuggestions.push(validSuggestion)
            }
          }
        }
      }
    }


    return validSuggestions
  }



const getNextSuggestion = async (): Promise<AISuggestion | null> => {
  try {


    if (currentBatchSuggestions.value.length === 0 || currentBatchIndex.value >= currentBatchSuggestions.value.length) {
      const newBatchSuggestions = await fetchBatchSuggestions()
      currentBatchSuggestions.value = newBatchSuggestions
      currentBatchIndex.value = 0

      if (newBatchSuggestions.length > 0) {
        lastBatchHash.value = generateBatchHash(newBatchSuggestions)
      }

      if (currentBatchSuggestions.value.length === 0) {

        return null
      }
    }

    for (let i = currentBatchIndex.value; i < currentBatchSuggestions.value.length; i++) {
      const suggestion = currentBatchSuggestions.value[i]
      const suggestionId = getSuggestionId(suggestion)

      if (!processedSuggestions.value.has(suggestionId)) {
        currentBatchIndex.value = i + 1
        return suggestion
      }
    }


    currentBatchSuggestions.value = await fetchBatchSuggestions()
    currentBatchIndex.value = 0
    processedSuggestions.value.clear()

    if (currentBatchSuggestions.value.length > 0) {
      const suggestion = currentBatchSuggestions.value[0]
      currentBatchIndex.value = 1
      return suggestion
    }

    return null
  } catch (error) {
    console.error( error)
    return null
  }
}

const getNextSuggestionInCycle = async (): Promise<AISuggestion | null> => {
  try {


    if (currentBatchSuggestions.value.length === 0) {

      const newBatchSuggestions = await fetchBatchSuggestions()
      currentBatchSuggestions.value = newBatchSuggestions
      currentDisplayIndex.value = 0
      currentBatchIndex.value = 0

      if (newBatchSuggestions.length > 0) {
        lastBatchHash.value = generateBatchHash(newBatchSuggestions)
      }

      if (currentBatchSuggestions.value.length === 0) {

        return null
      }
    }

    let attempts = 0
    const maxAttempts = currentBatchSuggestions.value.length

    while (attempts < maxAttempts) {
      const suggestion = currentBatchSuggestions.value[currentDisplayIndex.value]
      const suggestionId = getSuggestionId(suggestion)

      currentDisplayIndex.value = (currentDisplayIndex.value + 1) % currentBatchSuggestions.value.length

      if (!processedSuggestions.value.has(suggestionId)) {
        return suggestion
      }

      attempts++
    }

    const newBatchSuggestions = await fetchBatchSuggestions()

    if (newBatchSuggestions.length > 0) {

      currentBatchSuggestions.value = newBatchSuggestions
      currentDisplayIndex.value = 0
      lastBatchHash.value = generateBatchHash(newBatchSuggestions)

      const firstSuggestion = newBatchSuggestions[0]
      currentDisplayIndex.value = 1
      return firstSuggestion
    } else {

      return null
    }

  } catch (error) {
    console.error(error)
    return null
  }
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
  if (pollingTimer.value) {
    clearTimeout(pollingTimer.value)
    pollingTimer.value = null
  }
  if (batchRefreshTimer.value) {
    clearTimeout(batchRefreshTimer.value)
    batchRefreshTimer.value = null
  }
  if (batchRefreshCountdownTimer) {
    clearInterval(batchRefreshCountdownTimer)
    batchRefreshCountdownTimer = null
  }
}


const startBatchRefreshTimer = () => {
  batchRefreshTimer.value = setTimeout(async () => {
    try {
      await refreshBatchSuggestions()
    } catch (error) {
      console.error( error)
    } finally {
      startBatchRefreshTimer()
    }
  }, BATCH_REFRESH_INTERVAL)

}

const refreshBatchSuggestions = async () => {
  try {
    const newBatchSuggestions = await fetchBatchSuggestions()

    if (newBatchSuggestions.length > 0) {

      const newBatchHash = generateBatchHash(newBatchSuggestions)

      if (newBatchHash === lastBatchHash.value) {
        return
      }

      lastBatchHash.value = newBatchHash

      const uniqueNewSuggestions = newBatchSuggestions.filter(newSuggestion => {
        const newId = getSuggestionId(newSuggestion)
        return !currentBatchSuggestions.value.some(existingSuggestion =>
          getSuggestionId(existingSuggestion) === newId
        )
      })

      if (uniqueNewSuggestions.length > 0) {
        currentBatchSuggestions.value = [...currentBatchSuggestions.value, ...uniqueNewSuggestions]



      }
      }

  } catch (error) {
    console.error(error)
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
  }, 10000)
}

const showSuggestion = async (forceRefresh = false) => {
  if (pollingTimer.value) {
    clearTimeout(pollingTimer.value)
    pollingTimer.value = null
  }

  isChanging.value = true
  isLoading.value = true

  await new Promise(resolve => setTimeout(resolve, 100))

  try {
    let next: AISuggestion | null = null

    if (forceRefresh) {

      next = await getNextSuggestion()
    } else {

      next = await getNextSuggestionInCycle()
    }

    suggestionData.value = next

    if (next) {
      displayData.value = await convertSuggestionToDisplay(next)
      console.log({
        junction: displayData.value.junctionName,
        from: displayData.value.fromEdgeName,
        to: displayData.value.toEdgeName,
        state: displayData.value.stateName,
        duration: next.duration
      })
    } else {
      displayData.value = {
        junctionName: '',
        fromEdgeName: '',
        toEdgeName: '',
        stateName: '',
        lightIndex: 0
      }

    }
  } catch (error) {
    console.error(error)
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
      pollingTimer.value = setTimeout(() => {

        showSuggestion(false)
      }, 10000)
    }
  } else {

    pollingTimer.value = setTimeout(() => {
      showSuggestion(true)
    }, 5000)
  }
}

// 监听状态变化并自动保存
watch([suggestionData, displayData, currentBatchSuggestions, currentBatchIndex, currentDisplayIndex], () => {
  saveStateToStorage()
}, { deep: true })

watch(() => props.isAIMode, (newValue, oldValue) => {
  console.log('AI mode changed:', oldValue, '->', newValue)

  if (newValue) {
    if (suggestionData.value) {
      startAutoApplyCountdown()
    }
  } else {
    if (pollingTimer.value) {
      clearTimeout(pollingTimer.value)
      pollingTimer.value = null
    }
    if (autoApplyTimer) {
      clearTimeout(autoApplyTimer)
      autoApplyTimer = null
    }
    if (countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }

    if (suggestionData.value) {
      pollingTimer.value = setTimeout(() => {
        showSuggestion(false)
      }, 10000)
    }
  }
})

const handleApply = async (isAutoApply = false) => {
  if (!suggestionData.value) return

  isApplying.value = true
  const suggestion = suggestionData.value

  const payload = {
    junctionId: suggestion.junction,
    lightIndex: displayData.value.lightIndex,
    state: suggestion.target_state,
    duration: suggestion.duration,
    source: isAutoApply ? "ai" : "manual"
  }

  const description = `Set ${displayData.value.junctionName} light from ${displayData.value.fromEdgeName} to ${displayData.value.toEdgeName} to ${displayData.value.stateName} for ${suggestion.duration}s`

  const recordId = operationStore.addRecord({
    description,
    source: isAutoApply ? 'ai' : 'manual',
    junctionId: suggestion.junction,
    junctionName: displayData.value.junctionName,
    lightIndex: displayData.value.lightIndex,
    state: suggestion.target_state,
    duration: suggestion.duration
  })

  try {
    await apiClient.post('/signalcontrol/manual', payload)
    operationStore.updateRecordStatus(recordId, 'success')

    if (isAutoApply) {
      console.log('AI suggestion auto-applied successfully')
      toast.success('AI suggestion applied successfully!')
    } else {
      toast.success('Traffic light settings updated successfully!')
    }
  } catch {
    operationStore.updateRecordStatus(recordId, 'failed', 'Failed to send data to backend')

    if (isAutoApply) {
      console.error('AI suggestion auto-apply failed')
      toast.error('AI suggestion failed to apply!')
    } else {
      toast.error('Failed to send data to backend.')
    }
  } finally {
    isApplying.value = false
  }

  if (suggestion) {
    const suggestionId = getSuggestionId(suggestion)
    processedSuggestions.value.add(suggestionId)

    // 保存状态
    saveStateToStorage()
  }

  showSuggestion(false)
}

const handleIgnore = () => {
  if (suggestionData.value) {
    const suggestionId = getSuggestionId(suggestionData.value)
    processedSuggestions.value.add(suggestionId)

    // 保存状态
    saveStateToStorage()
  }

  showSuggestion(false)
}

onMounted(async () => {
  // 尝试恢复状态
  const hasRestoredState = loadStateFromStorage()
  
  const cacheInitialized = await initializeCache()
  if (cacheInitialized) {
    if (hasRestoredState && hasValidSuggestion.value) {
      // 有保存的有效状态，继续从上次位置开始
      console.log('Continuing from restored state')
      if (props.isAIMode) {
        startAutoApplyCountdown()
      } else {
        pollingTimer.value = setTimeout(() => {
          showSuggestion(false)
        }, 10000)
      }
    } else {
      // 没有保存的状态或状态无效，正常初始化
      showSuggestion(true)
    }

    startBatchRefreshTimer()
  }
})

onBeforeUnmount(() => {
  clearAllTimers()
  // 组件卸载时保存当前状态
  saveStateToStorage()
})

// 暴露清除状态的方法（可选，用于调试或重置）
const resetAndClearState = () => {
  clearAllTimers()
  suggestionData.value = null
  displayData.value = {
    junctionName: '',
    fromEdgeName: '',
    toEdgeName: '',
    stateName: '',
    lightIndex: 0
  }
  currentBatchSuggestions.value = []
  processedSuggestions.value.clear()
  currentBatchIndex.value = 0
  currentDisplayIndex.value = 0
  lastBatchHash.value = ''
  clearStoredState()
  console.log('AI suggestions state reset and cleared')
}

// 在开发环境中暴露到window对象（便于调试）
if (import.meta.env.DEV) {
  (window as any).resetAISuggestions = resetAndClearState
}


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

  &.ai-mode-container {
    display: flex;
    flex-direction: column;

    .action-box {
      margin-top: 0.9rem;
      margin-bottom: 0.2rem;
      margin-left: 0.24rem;
      margin-right: 0.24rem;
    }
  }

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

.panel-title {
  font-size: 0.2rem;
  font-weight: 700;
  color: #00E5FF;
  margin-bottom: 0.1rem;
  padding-left: 0.24rem;
  padding-top: 0.2rem;
  padding-bottom: 0.16rem;
  line-height: 0.2rem;

  position: relative;
}

.action-box {
  height: 1.5rem;
  background: linear-gradient(135deg, #1E2139 0%, #2A2D4A 100%);
  border: 1px solid rgba(74, 85, 104, 0.4);
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
    background: transparent;
    opacity: 0;
    transition: opacity 0.3s ease;
    pointer-events: none;
  }


  &:not(.no-suggestion) {
    box-shadow:
      0 0 12px rgba(74, 85, 104, 0.2),
      inset 0 1px 3px rgba(113, 128, 150, 0.15);

    &::before {
      opacity: 0.3;
    }
  }


  &.suggestion-changing {
    transform: scale(0.98);
    box-shadow:
      0 0 18px rgba(74, 85, 104, 0.25),
      inset 0 1px 3px rgba(113, 128, 150, 0.2);

    &::before {
      opacity: 0.5;
    }
  }


  &.no-suggestion {
    background: linear-gradient(135deg, #2A2D4A 0%, #1E2139 100%);
    border-color: rgba(113, 128, 150, 0.3);

    &::before {
      background: transparent;
    }
  }


  &.ai-mode {
    height: 2.2rem;
    border-color: rgba(74, 85, 104, 0.4);
    background: linear-gradient(135deg, #1E2139 0%, #2A2D4A 100%);
    box-shadow: none;

    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0.2rem 0.24rem;

    &::before {
      background: transparent;
      opacity: 0.3;
    }

    .placeholder-text {
      color: #E3F2FD;
      width: 100%;
      height: 100%;

      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;

      strong {
        color:#00E5FF;
      }
    }
  }
}





.placeholder-text {
  color: #E3F2FD;
  font-size: 0.16rem;
  line-height: 1.8;
  text-align: center;
  margin: 0.1rem 0;
  padding: 0 0.2rem;
  word-break: keep-all;
  white-space: normal;
  overflow-wrap: break-word;
  position: relative;
  z-index: 3;

  font-weight: 500;
  letter-spacing: 0.3px;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  transform: translateY(0);
  opacity: 1;

  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100%;
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;

  .suggestion-line {
    display: block;
    margin: 0.04rem 0;
    line-height: 1.4;
    white-space: normal;
    word-wrap: break-word;
    overflow-wrap: break-word;
    max-width: 100%;
  }

  strong {
    font-weight: 700;
    color: #00E5FF;
    position: relative;
    font-size: 0.17rem;
    margin: 0 0.03rem;
    display: inline;
  }

  .action-box.ai-mode & {
    font-size: 0.18rem;

    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
    margin: 0;
    padding: 0;

    .suggestion-line {
      margin: 0.08rem 0;
      line-height: 1.4;
      text-align: center;
    }

    strong {
      font-size: 0.19rem;
    }
  }

  .action-box.no-suggestion & {
    color: rgba(156, 163, 175, 0.6);
    font-style: italic;

    strong {
      color: rgba(156, 163, 175, 0.6);
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
  }

  &:disabled {
    background: linear-gradient(135deg, #4A5568 0%, #2D3748 100%);
    cursor: not-allowed;
    color: #A0AEC0;
    opacity: 0.5;
    transform: none;
    box-shadow: none;
  }
}
</style>
