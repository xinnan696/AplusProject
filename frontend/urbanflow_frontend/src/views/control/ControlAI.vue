<template>
  <div class="suggested-actions">
    <div class="panel-title">Suggested Actions</div>

    <div class="action-box">
      <div v-if="hasValidSuggestion">
        <p class="placeholder-text">
          Suggest changing the light at <strong>{{ suggestionData?.junctionName }}</strong>
          from <strong>{{ suggestionData?.fromEdge }}</strong> to <strong>{{ suggestionData?.toEdge }}</strong>
          to <strong>{{ mappedState }}</strong> for <strong>{{ suggestionData?.duration }}s </strong>.
        </p>
        </div>
        <p class="placeholder-text" v-else>
          No Avaliable Suggestion.
        </p>
    </div>

    <div class="action-buttons">
      <button
        ref="applyBtnRef"
        class="apply-btn"
        :disabled="!hasValidSuggestion"
        @click="handleApply"
      >
        <span>APPLY</span>
      </button>
      <button
        class="ignore-btn"
        :disabled="!hasValidSuggestion"
        @click="handleIgnore"
        >
          IGNORE
        </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import axios from 'axios'
import { toast } from '@/utils/ToastService'
import { suggestionList, type Suggestion } from '@/mocks/mockSuggestions'

const suggestionData = ref<null | Suggestion>(null)
const availableSuggestions = ref<Suggestion[]>([...suggestionList])
const ignoredSuggestions = ref<Suggestion[]>([])
let timer: ReturnType<typeof setTimeout> | null = null

const hasValidSuggestion = computed(() => {
  const s = suggestionData.value
  return !!(s && s.state && s.junctionName && s.fromEdge && s.toEdge && s.duration)
})

const getNextSuggestion = (): Suggestion | null => {
  if (availableSuggestions.value.length > 0) {
    const next = availableSuggestions.value.shift()!
    return next
  } else if (ignoredSuggestions.value.length > 0) {
    availableSuggestions.value = [...ignoredSuggestions.value]
    ignoredSuggestions.value = []
    return getNextSuggestion()
  } else {
    return null
  }
}

const showSuggestion = (isTimeout = true) => {
  if (isTimeout && suggestionData.value) {
    ignoredSuggestions.value.push(suggestionData.value)
  }

  const next = getNextSuggestion()
  suggestionData.value = next

  if (timer) clearTimeout(timer)

  if (next) {
    timer = setTimeout(() => {
      showSuggestion(true)
    }, 20000)
  }
}

showSuggestion()

const handleApply = async () => {
  if (!suggestionData.value) return

  const payload = {
    junctionId: suggestionData.value.junctionId,
    lightIndex: suggestionData.value.lightIndex,
    state: suggestionData.value.state,
    duration: suggestionData.value.duration,
    source: "manual"
  }

  try {
    await axios.post('/api/signalcontrol/manual', payload)
    toast.success('Traffic light settings updated successfully!')
  } catch {
    toast.error('Failed to send data to backend.')
  }

  showSuggestion(false)
}

const handleIgnore = () => {
  showSuggestion(false)
}

// ==== ✅ 状态映射逻辑 ====
type StateChar = 'G' | 'g' | 'R' | 'r'

const stateMap: Record<StateChar, string> = {
  G: "Green",
  g: "green",
  R: "Red",
  r: "red"
}

// ✅ 供模板使用的 computed 映射状态文字
const mappedState = computed(() => {
  const s = suggestionData.value?.state
  if (!s) return ''
  return stateMap[s as StateChar] || s.toUpperCase()
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
}

.panel-title {
  font-size: 0.18rem;
  font-weight: bold;
  color: #00B4D8;
  padding-left: 0.24rem;
  padding-top: 0.2rem;
  padding-bottom: 0.16rem;
}

.action-box {
  height: 1.5rem;
  border: 1px solid #00B4D8;
  border-radius: 0.2rem;
  background-color: #1A1B2C;
  padding: 0.16rem 0.24rem;
  margin: 0 0.24rem 0.26rem 0.24rem;
  box-sizing: border-box;

  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
}

.placeholder-text {
  color: #ccc;
  font-size: 0.14rem;
  line-height: 1.4;
  text-align: center;
  margin: 0.1rem 0;
  padding: 0 0.24rem;
  word-break: break-word;

  strong {
    font-weight: bold;
    color: #00B4D8;
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
  border: none;
  border-radius: 0.2rem;
  position: relative;
  overflow: hidden;
  cursor: pointer;
}

.apply-btn {
  background-color: #00B4D8;
  color: #FFFFFF;
  transition: background-color 0.3s ease;

  // disabled
  &:disabled {
    background-color: #00B4D8;
    cursor: not-allowed;
  }

  // 文字置顶
  span {
    position: relative;
    z-index: 2;
  }
}

// 禁用状态显示灰色鼠标
.apply-btn:disabled {
  cursor: not-allowed;
}

.ignore-btn {
  background-color: #999999;
  color: #FFFFFF;

  &:disabled {
    background-color: #999999;
    cursor: not-allowed;
  }
}

</style>
