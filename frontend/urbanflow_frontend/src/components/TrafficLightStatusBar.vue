<template>
  <div
    v-if="isVisible"
    class="traffic-status-bar"
    :class="{ 'has-manual-control': shouldShowManualControl }"
  >
    <div class="status-content">
      <div class="status-info">
        <div class="status-text" :class="getLightClass(currentLight)">
          {{ getLightText(currentLight) }}
        </div>
      </div>

      <div class="countdown-section">
        <div class="countdown-display">
          <span class="countdown-number">{{ countdownSeconds }}</span>
          <span class="countdown-unit">s</span>
        </div>
      </div>

      <div v-if="shouldShowManualControl" class="manual-control-section">
        <div class="manual-control-label">Applied:</div>
        <div class="manual-control-info">
          <span class="manual-light-color" :class="`manual-${props.lastManualControl?.lightColor.toLowerCase()}`">
            {{ props.lastManualControl?.lightColor.toUpperCase() }}
          </span>
          <span class="manual-duration">{{ props.lastManualControl?.duration }}</span>
        </div>
        <div class="manual-time">{{ formatTimeAgo }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'

interface Props {
  junctionId?: string
  junctionName?: string
  directionIndex?: number | null
  trafficLightData?: any
  lastManualControl?: {
    junctionName: string
    directionInfo: string
    lightColor: string
    duration: number
    appliedTime: Date
  } | null
}

const props = defineProps<Props>()

const currentLight = ref('')
const countdownSeconds = ref(0)
let countdownTimer: NodeJS.Timeout | null = null
const previousLightState = ref('')

// 计算是否显示手动控制信息
const shouldShowManualControl = computed(() => {
  const hasManualControl = !!props.lastManualControl
  const isCorrectJunction = props.lastManualControl?.junctionName === props.junctionName



  return hasManualControl && isCorrectJunction
})

// 计算时间显示
const formatTimeAgo = computed(() => {
  if (!props.lastManualControl?.appliedTime) return ''

  const now = new Date()
  const diffMs = now.getTime() - props.lastManualControl.appliedTime.getTime()
  const diffSeconds = Math.floor(diffMs / 1000)

  if (diffSeconds < 60) {
    return `${diffSeconds}s ago`
  } else if (diffSeconds < 3600) {
    const minutes = Math.floor(diffSeconds / 60)
    return `${minutes}m ago`
  } else {
    const hours = Math.floor(diffSeconds / 3600)
    return `${hours}h ago`
  }
})

// 计算组件是否可见
const isVisible = computed(() => {
  const hasJunctionId = !!props.junctionId
  const hasJunctionName = !!props.junctionName
  const hasValidDirection = props.directionIndex !== null && props.directionIndex !== undefined
  const hasTrafficData = !!props.trafficLightData
  const hasManualControl = shouldShowManualControl.value

  // 简化条件：只要有路口ID和有效方向就显示
  const shouldShow = hasJunctionId && hasValidDirection


  return shouldShow
})




// 获取交通灯颜色
const getLightColor = (state: string, index: number): string => {
  if (!state || typeof state !== 'string' || index < 0 || index >= state.length) {
    return 'unknown'
  }

  const char = state[index]
  const lowerChar = char.toLowerCase()

  switch (lowerChar) {
    case 'g': return 'green'
    case 'r': return 'red'
    case 'y': return 'yellow'
    case 'o': return 'yellow'
    default:
      console.warn('[StatusBar] Unknown character:', {
        char: `"${char}"`,
        charCode: char.charCodeAt(0),
        fullState: `"${state}"`,
        index
      })
      return 'unknown'
  }
}

// 获取交通灯文本
const getLightText = (color: string): string => {
  switch (color) {
    case 'green': return 'GREEN'
    case 'red': return 'RED'
    case 'yellow': return 'YELLOW'
    default: return 'UNKNOWN'
  }
}

// 获取交通灯样式类
const getLightClass = (color: string): string => {
  switch (color) {
    case 'green': return 'light-green'
    case 'red': return 'light-red'
    case 'yellow': return 'light-yellow'
    default: return 'light-unknown'
  }
}

// 启动倒计时
const startCountdown = (initialSeconds: number) => {
  // 清除之前的计时器
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }

  countdownSeconds.value = initialSeconds

  countdownTimer = setInterval(() => {
    if (countdownSeconds.value > 0) {
      countdownSeconds.value--
    } else {
      // 倒计时到0时，清除计时器
      if (countdownTimer) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    }
  }, 1000)
}

// 停止倒计时
const stopCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

// 更新状态
const updateStatus = () => {

  if (!props.trafficLightData || props.directionIndex === null || props.directionIndex === undefined) {
    currentLight.value = 'unknown'
    countdownSeconds.value = 0
    stopCountdown()
    return
  }

  if (props.trafficLightData.state && typeof props.trafficLightData.state === 'string') {
    const newLight = getLightColor(props.trafficLightData.state, props.directionIndex)
    const lightChanged = previousLightState.value !== newLight

    currentLight.value = newLight

    if (lightChanged || previousLightState.value === '') {
      const steps = props.trafficLightData.nextSwitchTime || 0
      const seconds = (steps + 1) * 9



      previousLightState.value = newLight
      startCountdown(seconds)
    } else {

    }

  } else {
    currentLight.value = 'unknown'
    previousLightState.value = ''
    stopCountdown()
  }
}

watch(() => props.trafficLightData, (newData, oldData) => {

  if (newData && typeof newData.state === 'string') {
    const shouldUpdate = !oldData ||
      oldData.state !== newData.state ||
      oldData.nextSwitchTime !== newData.nextSwitchTime

    if (shouldUpdate) {
      updateStatus()
    }
  } else {
    countdownSeconds.value = 0
    currentLight.value = 'unknown'
    previousLightState.value = ''
    stopCountdown()
  }
}, { immediate: true, deep: true })

watch(() => props.directionIndex, (newIndex, oldIndex) => {
  if (newIndex !== oldIndex && newIndex !== null && newIndex !== undefined) {
    updateStatus()
  }
}, { immediate: true })

watch(() => [props.junctionName, props.lastManualControl], (newValues, oldValues) => {
  const [newJunctionName, newManualControl] = newValues
  const [oldJunctionName, oldManualControl] = oldValues || [null, null]


}, { immediate: true, deep: true })

onMounted(() => {


  if (props.trafficLightData && props.directionIndex !== null && props.directionIndex !== undefined) {
    updateStatus()
  }
})

onUnmounted(() => {
  stopCountdown()
})
</script>

<style scoped lang="scss">
.traffic-status-bar {
  background: linear-gradient(135deg, rgba(30, 33, 71, 0.75) 0%, rgba(42, 45, 74, 0.75) 100%);
  border: 1px solid rgba(0, 180, 216, 0.3);
  border-radius: 0.08rem;
  padding: 0.08rem 0.10rem;
  margin: 0.06rem 0;
  position: relative;
  overflow: hidden;
  box-shadow:
    0 0.04rem 0.2rem rgba(0, 0, 0, 0.2),
    0 0 0.15rem rgba(0, 180, 216, 0.08);
  transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  backdrop-filter: blur(8px);

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(45deg, transparent 48%, rgba(0, 180, 216, 0.03) 49%, rgba(0, 180, 216, 0.03) 51%, transparent 52%);
    pointer-events: none;
  }

  &:hover {
    border-color: rgba(0, 229, 255, 0.4);
    background: linear-gradient(135deg, rgba(30, 33, 71, 0.85) 0%, rgba(42, 45, 74, 0.85) 100%);
    box-shadow:
      0 0.06rem 0.25rem rgba(0, 0, 0, 0.3),
      0 0 0.2rem rgba(0, 229, 255, 0.15);
  }
}

.status-content {
  display: flex;
  align-items: center;
  gap: 0.12rem;
  position: relative;
  z-index: 1;
  width: fit-content;
}


.status-info {
  display: flex;
  flex-direction: column;
  gap: 0.01rem;
  flex-shrink: 0;
  min-width: 0.8rem;
  width: fit-content;
}

.status-label {
  font-size: 0.09rem;
  color: #B3E5FC;
  font-weight: 600;
  text-shadow: 0 0 0.08rem rgba(179, 229, 252, 0.3);
  white-space: nowrap;
}

.status-text {
  font-size: 0.16rem;
  font-weight: 700;
  text-shadow: 0 0 0.1rem currentColor;
  transition: all 0.4s ease;
  white-space: nowrap;

  &.light-red {
    color: #FF4569;
  }

  &.light-yellow {
    color: #FFEB3B;
  }

  &.light-green {
    color: #4CAF50;
  }

  &.light-unknown {
    color: #999;
  }
}

.countdown-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.01rem;
  flex-shrink: 0;
  min-width: 0.5rem;
  width: fit-content;
}

.manual-control-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.02rem;
  flex-shrink: 0;
  min-width: 0.8rem;
  width: fit-content;
  padding-left: 0.08rem;
  border-left: 1px solid rgba(0, 180, 216, 0.3);
}

.manual-control-label {
  font-size: 0.09rem;
  color: #B3E5FC;
  font-weight: 600;
  text-shadow: 0 0 0.08rem rgba(179, 229, 252, 0.3);
  white-space: nowrap;
}

.manual-control-info {
  display: flex;
  align-items: center;
  gap: 0.04rem;
}

.manual-light-color {
  font-size: 0.12rem;
  font-weight: 700;
  text-shadow: 0 0 0.1rem currentColor;
  padding: 0.02rem 0.04rem;
  border-radius: 0.03rem;

  &.manual-green {
    color: #4CAF50;
    background: rgba(76, 175, 80, 0.1);
    border: 1px solid rgba(76, 175, 80, 0.3);
  }

  &.manual-red {
    color: #FF4569;
    background: rgba(255, 69, 105, 0.1);
    border: 1px solid rgba(255, 69, 105, 0.3);
  }
}

.manual-duration {
  font-size: 0.11rem;
  color: #00E5FF;
  font-weight: 600;
  text-shadow: 0 0 0.1rem rgba(0, 229, 255, 0.4);
}

.manual-time {
  font-size: 0.08rem;
  color: #999;
  font-weight: 500;
  white-space: nowrap;
}

.countdown-label {
  font-size: 0.09rem;
  color: #B3E5FC;
  font-weight: 600;
  text-shadow: 0 0 0.08rem rgba(179, 229, 252, 0.3);
  white-space: nowrap;
}

.countdown-display {
  display: flex;
  align-items: baseline;
  gap: 0.02rem;
  justify-content: center;
}

.countdown-number {
  font-size: 0.16rem;
  font-weight: 700;
  color: #00E5FF;
  text-shadow: 0 0 0.15rem rgba(0, 229, 255, 0.6);
  animation: countdownPulse 1s ease-in-out infinite;
  white-space: nowrap;
}

.countdown-unit {
  font-size: 0.12rem;
  color: #B3E5FC;
  font-weight: 600;
}

@keyframes countdownPulse {
  0%, 100% {
    transform: scale(1);
    text-shadow: 0 0 0.15rem rgba(0, 229, 255, 0.6);
  }
  50% {
    transform: scale(1.05);
    text-shadow: 0 0 0.2rem rgba(0, 229, 255, 0.8);
  }
}

@media (max-width: 600px) {
  .status-content {
    flex-direction: column;
    gap: 0.08rem;
    text-align: center;
  }

  .traffic-light-icon {
    width: 0.24rem;
    height: 0.72rem;
  }

  .status-info, .countdown-section, .manual-control-section {
    min-width: auto;
  }

  .manual-control-section {
    border-left: none;
    border-top: 1px solid rgba(0, 180, 216, 0.3);
    padding-left: 0;
    padding-top: 0.06rem;
  }
}

.traffic-status-bar {
  animation: statusBarGlow 4s ease-in-out infinite;
}

@keyframes statusBarGlow {
  0%, 100% {
    box-shadow:
      0 0.04rem 0.2rem rgba(0, 0, 0, 0.3),
      0 0 0.15rem rgba(0, 180, 216, 0.1);
  }
  50% {
    box-shadow:
      0 0.06rem 0.25rem rgba(0, 0, 0, 0.4),
      0 0 0.2rem rgba(0, 180, 216, 0.2);
  }
}

.status-text {
  animation: statusTextPulse 3s ease-in-out infinite;
}

@keyframes statusTextPulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.02);
  }
}
</style>
