<template>
  <div
    class="traffic-light-container"
    :class="{
      'selected': isSelected,
      'partially-selected': isPartiallySelected,
      'all-lights-on': isAllLightsOn,
      'controllable': isControllable,
      'readonly': !isControllable,
      'emergency-upcoming': isEmergencyUpcoming
    }"
    @click="$emit('click')"
  >
    <div class="traffic-light">
      <!-- 红绿灯外壳 -->
      <div class="light-housing">
        <!-- 红灯 -->
        <div
          class="light red-light"
          :class="{
            'active': showRedLight,
            'dim': !showRedLight && !isAllLightsOn
          }"
        ></div>

        <div
          class="light yellow-light"
          :class="{
            'active': showYellowLight,
            'dim': !showYellowLight && !isAllLightsOn
          }"
        ></div>

        <div
          class="light green-light"
          :class="{
            'active': showGreenLight,
            'dim': !showGreenLight && !isAllLightsOn
          }"
        ></div>
      </div>

      <div class="light-pole"></div>
    </div>

    <div class="glow-effect" v-if="isSelected"></div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  currentLight?: string // 'red', 'yellow', 'green'
  isSelected?: boolean
  isPartiallySelected?: boolean
  isControllable?: boolean
  showAllLights?: boolean
  isEmergencyUpcoming?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  currentLight: '',
  isSelected: false,
  isPartiallySelected: false,
  isControllable: true,
  showAllLights: false,
  isEmergencyUpcoming: false
})

defineEmits<{
  (e: 'click'): void
}>()


const isAllLightsOn = computed(() => props.showAllLights)

const showRedLight = computed(() => {
  if (isAllLightsOn.value) return true
  return props.currentLight === 'red'
})

const showYellowLight = computed(() => {
  if (isAllLightsOn.value) return true
  return props.currentLight === 'yellow'
})

const showGreenLight = computed(() => {
  if (isAllLightsOn.value) return true
  return props.currentLight === 'green'
})
</script>

<style scoped lang="scss">
.traffic-light-container {
  position: relative;
  cursor: pointer;
  transform-origin: center;
  transition: all 0.3s cubic-bezier(0.4, 0.0, 0.2, 1);
  transform: scale(0.7); /* 将标记缩小到原来的70% */

  &:hover {
    transform: scale(0.8); /* 悬停时稍微放大，但仍然比原来小 */
  }

  &.selected {
    transform: scale(0.9); /* 选中时放大，但仍然比原来小 */
    z-index: 1000;
    filter: brightness(1.15) contrast(1.05);
  }

  &.partially-selected {
    filter: brightness(1.35) contrast(1.15);
  }

  &.readonly {
    opacity: 0.7;
    filter: grayscale(0.3);

    &:hover {
      transform: scale(0.75); /* 只读状态下的悬停效果也相应缩小 */
    }
  }
}

.traffic-light {
  display: flex;
  flex-direction: column;
  align-items: center;
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.3));
}

.light-housing {
  background: linear-gradient(145deg, #3a3a3a, #2a2a2a);
  border: 2px solid #555;
  border-radius: 8px;
  padding: 3px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  box-shadow:
    inset 0 1px 3px rgba(255, 255, 255, 0.15),
    0 2px 8px rgba(0, 0, 0, 0.3);
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: -1px;
    left: 50%;
    transform: translateX(-50%);
    width: 6px;
    height: 6px;
    background: #777;
    border-radius: 50%;
  }
}

.light {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.1);
  position: relative;
  transition: all 0.3s ease;

  &::before {
    content: '';
    position: absolute;
    top: 1px;
    left: 2px;
    width: 3px;
    height: 3px;
    background: rgba(255, 255, 255, 0.3);
    border-radius: 50%;
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  &.active::before {
    opacity: 1;
  }
}

.red-light {
  background: radial-gradient(circle at 30% 30%, #ff6b6b, #d63031);

  &.dim {
    background: radial-gradient(circle at 30% 30%, #6a3a3a, #4a2525);
  }

  &.active {
    background: radial-gradient(circle at 30% 30%, #ff6b6b, #e74c3c);
  }
}

.yellow-light {
  background: radial-gradient(circle at 30% 30%, #ffd93d, #f39c12);

  &.dim {
    background: radial-gradient(circle at 30% 30%, #7a6035, #5a4020);
  }

  &.active {
    background: radial-gradient(circle at 30% 30%, #ffd93d, #f1c40f);
  }
}

.green-light {
  background: radial-gradient(circle at 30% 30%, #00b894, #27ae60);

  &.dim {
    background: radial-gradient(circle at 30% 30%, #3a5a4a, #254035);
  }

  &.active {
    background: radial-gradient(circle at 30% 30%, #00b894, #2ecc71);
  }
}

.light-pole {
  width: 2px;
  height: 8px;
  background: linear-gradient(to bottom, #555, #333);
  border-radius: 1px;
  margin-top: 1px;
}

.glow-effect {
  display: none;
}

@keyframes pulse {
  0%, 100% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 0.6;
  }
  50% {
    transform: translate(-50%, -50%) scale(1.2);
    opacity: 0.3;
  }
}

.controllable .light-housing {
  border-color: #FFFFFF;

  &:hover {
    border-color: #FFFFFF;
    box-shadow:
      inset 0 1px 3px rgba(255, 255, 255, 0.1),
      0 2px 8px rgba(0, 0, 0, 0.3);
  }
}

.readonly .light-housing {
  border-color: rgba(102, 102, 102, 0.5);
}

.selected .light-housing {
  border-color: #FFFFFF;
  box-shadow:
    inset 0 1px 3px rgba(255, 255, 255, 0.2),
    0 2px 12px rgba(0, 0, 0, 0.4);
  background: linear-gradient(145deg, #4a4a4a, #3a3a3a);
}

.partially-selected .light-housing {
  border-color: rgba(0, 180, 216, 0.8);
  box-shadow:
    inset 0 1px 3px rgba(255, 255, 255, 0.15),
    0 2px 10px rgba(0, 0, 0, 0.3);
  background: linear-gradient(145deg, #4a4a4a, #3a3a3a);
}

// 紧急车辆即将到达的路口样式
.emergency-upcoming {
  .light-housing {
    border-color: rgba(255, 107, 0, 0.9) !important;
    background: linear-gradient(145deg, #4a2a1a, #3a1a0a);
    box-shadow:
      inset 0 1px 3px rgba(255, 107, 0, 0.2),
      0 2px 12px rgba(0, 0, 0, 0.4),
      0 0 20px rgba(255, 107, 0, 0.6),
      0 0 30px rgba(255, 107, 0, 0.3) !important;
    animation: emergencyPulse 2s ease-in-out infinite;
  }

  .light {
    animation: emergencyFlash 1s ease-in-out infinite alternate;
  }

  &:hover {
    transform: scale(1.3) !important;

    .light-housing {
      box-shadow:
        inset 0 1px 3px rgba(255, 107, 0, 0.3),
        0 2px 16px rgba(0, 0, 0, 0.5),
        0 0 25px rgba(255, 107, 0, 0.8),
        0 0 40px rgba(255, 107, 0, 0.4) !important;
    }
  }
}

@keyframes emergencyPulse {
  0%, 100% {
    border-color: rgba(255, 107, 0, 0.9);
    box-shadow:
      inset 0 1px 3px rgba(255, 107, 0, 0.2),
      0 2px 12px rgba(0, 0, 0, 0.4),
      0 0 20px rgba(255, 107, 0, 0.6),
      0 0 30px rgba(255, 107, 0, 0.3);
  }
  50% {
    border-color: rgba(255, 107, 0, 1);
    box-shadow:
      inset 0 1px 3px rgba(255, 107, 0, 0.3),
      0 2px 16px rgba(0, 0, 0, 0.5),
      0 0 30px rgba(255, 107, 0, 0.8),
      0 0 40px rgba(255, 107, 0, 0.5);
  }
}

@keyframes emergencyFlash {
  0% {
    filter: brightness(1) saturate(1);
  }
  100% {
    filter: brightness(1.5) saturate(1.3);
  }
}

@media (max-width: 768px) {
  .light {
    width: 8px;
    height: 8px;
  }

  .light-housing {
    padding: 2px;
    gap: 1px;
  }

  .light-pole {
    height: 5px;
  }
}
</style>
