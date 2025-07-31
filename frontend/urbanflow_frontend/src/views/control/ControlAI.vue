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

// 轮询相关状态
const pollingTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const batchRefreshTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const BATCH_REFRESH_INTERVAL = 10000 // 10秒主动刷新批次数据

// 已处理的建议记录
const processedSuggestions = ref<Set<string>>(new Set())
// 当前批量建议索引
const currentBatchIndex = ref(0)
// 当前批量建议数据
const currentBatchSuggestions = ref<AISuggestion[]>([])
// 上次获取的批次数据哈希，用于去重
const lastBatchHash = ref<string>('')
// 当前循环显示的索引（独立于处理索引）
const currentDisplayIndex = ref(0)

const junctionsCache = ref<Map<string, Junction>>(new Map())
const edgesCache = ref<Map<string, Edge>>(new Map())
const laneMappingsCache = ref<Map<string, string>>(new Map())

let timer: ReturnType<typeof setTimeout> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null
let autoApplyTimer: ReturnType<typeof setTimeout> | null = null
let batchRefreshCountdownTimer: ReturnType<typeof setInterval> | null = null

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

    // 按照您其他模块的成功方式，使用直接的axios
    const [junctionsRes, edgesRes, laneMappingsRes] = await Promise.all([
      axios.get('/api-status/junctions'),
      axios.get('/api-status/edges'),
      axios.get('/api-status/lane-mappings')
    ])


    // 处理 junctions 数据
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

    // 处理 edges 数据
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

    // 处理 lane mappings 数据
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

    console.log('✅ 缓存初始化完成:', finalStats)



    return finalStats.junctions > 0 && finalStats.edges > 0
  } catch (error) {
    console.error( error)

    return true
  }
}

const convertSuggestionToDisplay = async (suggestion: AISuggestion): Promise<DisplayData & { lightIndex: number }> => {
  try {


    // Junction名称转换
    const junction = junctionsCache.value.get(suggestion.junction)
    console.log('🏛️ Junction查找:', suggestion.junction, '->', junction?.junction_name)
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
    console.log('📨 发起请求到后端: /api/traffic/suggestion')
    const response = await axios.get('/api/traffic/suggestion')
    console.log('📨 收到后端响应:', response.status)
    const data = response.data
    const validSuggestions: AISuggestion[] = []

    // 提取所有有效建议（suggestion_label > 0）
    if (data.batch_suggestions && Array.isArray(data.batch_suggestions)) {
      for (const batch of data.batch_suggestions) {
        if (Array.isArray(batch) && batch.length > 0) {
          const suggestion = batch[0]

          // 🔥 关键修复：检查 suggestion_label > 0 才是有效建议
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

            // 过滤掉已处理的建议
            const suggestionId = getSuggestionId(validSuggestion)
            if (!processedSuggestions.value.has(suggestionId)) {
              validSuggestions.push(validSuggestion)
            }
          }
        }
      }
    }

    console.log(`📥 从${data.batch_suggestions?.length || 0}个建议中提取到${validSuggestions.length}个有效建议`)
    return validSuggestions
  }



const getNextSuggestion = async (): Promise<AISuggestion | null> => {
  try {


    if (currentBatchSuggestions.value.length === 0 || currentBatchIndex.value >= currentBatchSuggestions.value.length) {
      console.log('📥 重新获取批量建议数据')
      const newBatchSuggestions = await fetchBatchSuggestions()
      currentBatchSuggestions.value = newBatchSuggestions
      currentBatchIndex.value = 0

      // 记录首次获取的哈希值
      if (newBatchSuggestions.length > 0) {
        lastBatchHash.value = generateBatchHash(newBatchSuggestions)
      }

      if (currentBatchSuggestions.value.length === 0) {
        console.log('⚠️ 没有可用的建议')
        return null
      }
    }

    // 从当前索引开始，寻找未处理的建议
    for (let i = currentBatchIndex.value; i < currentBatchSuggestions.value.length; i++) {
      const suggestion = currentBatchSuggestions.value[i]
      const suggestionId = getSuggestionId(suggestion)

      if (!processedSuggestions.value.has(suggestionId)) {
        currentBatchIndex.value = i + 1
        return suggestion
      }
    }

    // 如果所有建议都已处理，重新获取新的批量数据
    console.log('🔄 所有建议已处理，获取新批量数据')
    currentBatchSuggestions.value = await fetchBatchSuggestions()
    currentBatchIndex.value = 0
    processedSuggestions.value.clear() // 清空已处理记录

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

// 循环播放模式：从当前批次中循环获取建议
const getNextSuggestionInCycle = async (): Promise<AISuggestion | null> => {
  try {
    console.log('🔄 getNextSuggestionInCycle 开始，当前池大小:', currentBatchSuggestions.value.length)
    console.log('🔄 已处理建议数量:', processedSuggestions.value.size)
    
    // 如果当前批次为空，获取新批次
    if (currentBatchSuggestions.value.length === 0) {
      console.log('📥 当前批次为空，获取新批次建议')
      const newBatchSuggestions = await fetchBatchSuggestions()
      currentBatchSuggestions.value = newBatchSuggestions
      currentDisplayIndex.value = 0
      currentBatchIndex.value = 0

      if (newBatchSuggestions.length > 0) {
        lastBatchHash.value = generateBatchHash(newBatchSuggestions)
      }

      if (currentBatchSuggestions.value.length === 0) {
        console.log('⚠️ 没有可用的建议')
        return null
      }
    }

    // 🔄 循环查找下一个未处理的建议
    let attempts = 0
    const maxAttempts = currentBatchSuggestions.value.length
    
    while (attempts < maxAttempts) {
      const suggestion = currentBatchSuggestions.value[currentDisplayIndex.value]
      const suggestionId = getSuggestionId(suggestion)
      
      // 移动到下一个显示索引（循环）
      currentDisplayIndex.value = (currentDisplayIndex.value + 1) % currentBatchSuggestions.value.length
      
      // 如果这个建议未被处理，返回它
      if (!processedSuggestions.value.has(suggestionId)) {
        console.log(`🔄 循环显示: 找到未处理建议 ${attempts + 1}/${currentBatchSuggestions.value.length}, 下次索引${currentDisplayIndex.value}`)
        return suggestion
      }
      
      attempts++
    }
    
    // 🔥 关键修复：如果所有建议都已处理，尝试获取新建议
    console.log('🔄 当前批次所有建议已处理，尝试获取新建议')
    const newBatchSuggestions = await fetchBatchSuggestions()
    
    if (newBatchSuggestions.length > 0) {
      // 有新建议，更新建议池
      currentBatchSuggestions.value = newBatchSuggestions
      currentDisplayIndex.value = 0
      lastBatchHash.value = generateBatchHash(newBatchSuggestions)
      
      const firstSuggestion = newBatchSuggestions[0]
      currentDisplayIndex.value = 1
      console.log('🆕 获取到新建议，显示第一个')
      return firstSuggestion
    } else {
      // 🔥 后端返回空建议，且所有建议已处理，返回null显示"No Available Suggestion"
      console.log('🔄 后端无新建议且所有建议已处理，显示"No Available Suggestion"')
      return null
    }
    
    console.log('⚠️ 真的没有任何建议可用')
    return null
  } catch (error) {
    console.error('循环获取建议失败:', error)
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
    console.log('🔄 定时检查（每10秒）：获取新批次建议数据')
    try {
      await refreshBatchSuggestions()
    } catch (error) {
      console.error('🔄 批次刷新失败:', error)
    } finally {
      // 🔥 关键：无论成功还是失败，都要重新启动定时器
      console.log('🔄 重新启动批次刷新定时器')
      startBatchRefreshTimer()
    }
  }, BATCH_REFRESH_INTERVAL)
  
  console.log('🔄 批次刷新定时器已启动，间隔:', BATCH_REFRESH_INTERVAL, 'ms')
}

const refreshBatchSuggestions = async () => {
  try {
    const newBatchSuggestions = await fetchBatchSuggestions()

    if (newBatchSuggestions.length > 0) {

      const newBatchHash = generateBatchHash(newBatchSuggestions)

      // 如果和上次数据相同，直接返回，不更新队列
      if (newBatchHash === lastBatchHash.value) {
        console.log('🔄 批次数据未变化，跳过更新')
        return
      }

      // 更新哈希值
      lastBatchHash.value = newBatchHash

      // 🔥 关键修正：合并新建议到现有建议池，但不影响当前显示
      const uniqueNewSuggestions = newBatchSuggestions.filter(newSuggestion => {
        const newId = getSuggestionId(newSuggestion)
        return !currentBatchSuggestions.value.some(existingSuggestion => 
          getSuggestionId(existingSuggestion) === newId
        )
      })

      if (uniqueNewSuggestions.length > 0) {
        // 将新建议添加到现有建议池中
        currentBatchSuggestions.value = [...currentBatchSuggestions.value, ...uniqueNewSuggestions]
        
        console.log(`🆕 新增${uniqueNewSuggestions.length}个建议到建议池，总计${currentBatchSuggestions.value.length}个建议`)
        
        // 🔥 不立即打断当前显示，让用户继续看当前建议
        // 新建议将在循环中自然显示
      } else {
        console.log('🔄 没有新的唯一建议，保持现有池')
      }
    } else {
      console.log('⚠️ 后端返回空建议列表')
    }
  } catch (error) {
    console.error('批次刷新失败:', error)
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
  // 🔥 不再清理所有定时器，只清理显示相关的定时器
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
      // 🔄 强制刷新：重新获取数据（初始化时）
      console.log('🔄 强制刷新：重新获取数据')
      next = await getNextSuggestion()
    } else {
      // 🔄 循环模式：从当前池中循环显示建议
      console.log('🔄 循环模式：显示下一个建议')
      next = await getNextSuggestionInCycle()
    }
    
    suggestionData.value = next

    if (next) {
      displayData.value = await convertSuggestionToDisplay(next)
      console.log('✅ 建议已加载:', {
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
      console.log('⚠️ 没有可用的建议')
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

  // 🔄 只启动显示循环定时器，不管理批次刷新定时器
  if (suggestionData.value) {
    if (props.isAIMode) {
      startAutoApplyCountdown()
    } else {
      // 🔄 手动模式：每10秒循环显示下一个建议
      pollingTimer.value = setTimeout(() => {
        console.log('🔄 10秒循环：显示下一个建议')
        showSuggestion(false) // 继续循环模式
      }, 10000)
    }
  } else {
    // 没有建议时，5秒后强制刷新
    pollingTimer.value = setTimeout(() => {
      console.log('🔄 无建议，5秒后强制刷新')
      showSuggestion(true) // 强制获取新建议
    }, 5000)
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
    // 🔥 只清理显示相关的定时器，不清理批次刷新定时器
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
      // 手动模式下使用循环模式
      pollingTimer.value = setTimeout(() => {
        showSuggestion(false) // 继续循环模式
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

  // 标记当前建议为已处理
  if (suggestion) {
    const suggestionId = getSuggestionId(suggestion)
    processedSuggestions.value.add(suggestionId)
    console.log('✅ 建议已应用并标记为已处理:', suggestionId)
    console.log('✅ 当前已处理建议数量:', processedSuggestions.value.size)
  }

  // 🔄 用户处理后立即显示下一个建议
  showSuggestion(false) // 不强制刷新，继续循环
}

const handleIgnore = () => {
  // 标记当前建议为已处理（忽略）
  if (suggestionData.value) {
    const suggestionId = getSuggestionId(suggestionData.value)
    processedSuggestions.value.add(suggestionId)
    console.log('❌ 建议已忽略并标记为已处理:', suggestionId)
    console.log('❌ 当前已处理建议数量:', processedSuggestions.value.size)
  }

  // 🔄 用户处理后立即显示下一个建议
  showSuggestion(false) // 不强制刷新，继续循环
}

onMounted(async () => {
  console.log('🚀 组件初始化开始')
  const cacheInitialized = await initializeCache()
  if (cacheInitialized) {
    console.log('🚀 缓存初始化完成，开始获取建议')
    
    // 首次获取建议
    showSuggestion(true) // 初始化时强制刷新获取数据
    
    // 🔥 立即启动定时器
    console.log('🚀 立即启动批次刷新定时器')
    startBatchRefreshTimer()
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
    background: linear-gradient(45deg, transparent 48%, rgba(74, 85, 104, 0.1) 49%, rgba(74, 85, 104, 0.1) 51%, transparent 52%);
    opacity: 0;
    transition: opacity 0.3s ease;
    pointer-events: none;
  }

  &:hover {
    border-color: rgba(113, 128, 150, 0.6);
    box-shadow: 0 0 15px rgba(0, 0, 0, 0.2);
    background: linear-gradient(135deg, #2A2D4A 0%, #1E2139 100%);

    &::before {
      opacity: 1;
    }
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
      background: linear-gradient(45deg, transparent 48%, rgba(113, 128, 150, 0.1) 49%, rgba(113, 128, 150, 0.1) 51%, transparent 52%);
    }
  }


  &.ai-mode {
    height: 2.2rem; // AI模式下增加高度
    border-color: rgba(74, 85, 104, 0.4); // 使用普通的边框颜色，去掉紫色
    background: linear-gradient(135deg, #1E2139 0%, #2A2D4A 100%); // 使用普通的背景，去掉紫色
    box-shadow: none; // 移除紫色荧光效果

    &::before {
      background: linear-gradient(45deg, transparent 48%, rgba(74, 85, 104, 0.1) 49%, rgba(74, 85, 104, 0.1) 51%, transparent 52%); // 使用普通的渐变
      opacity: 0.3; // 降低透明度
    }

    .placeholder-text {
      color: #E3F2FD; // 使用普通的文字颜色

      strong {
        color:#00E5FF;
        // color: #6366F1; // 保持蓝色强调，但去掉text-shadow
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

  // 确保在容器中垂直居中
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

  // AI模式下的特殊样式
  .action-box.ai-mode & {
    font-size: 0.18rem; // AI模式下字体稍大一些
    
    .suggestion-line {
      margin: 0.06rem 0;
      line-height: 1.3;
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
    font-weight: 700; // 加粗APPLY文字
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

.ignore-btn {
  background: linear-gradient(135deg, #718096 0%, #4A5568 100%);
  color: #FFFFFF;
  border-color: rgba(113, 128, 150, 0.5);

  &:hover {
    background: linear-gradient(135deg, #A0AEC0 0%, #718096 100%);
    transform: translateY(-2px) scale(1.02);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
    border-color: rgba(113, 128, 150, 0.8);
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
