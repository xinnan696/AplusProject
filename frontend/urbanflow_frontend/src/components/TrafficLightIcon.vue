<template>
  <div
    class="traffic-light-container"
    :class="{
      'selected': isSelected,
      'partially-selected': isPartiallySelected,
      'all-lights-on': isAllLightsOn,
      'controllable': isControllable,
      'readonly': !isControllable
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
  isPartiallySelected?: boolean // 新增：表示只选中了 junction，还没选择 direction
  isControllable?: boolean
  showAllLights?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  currentLight: '',
  isSelected: false,
  isPartiallySelected: false,
  isControllable: true,
  showAllLights: false
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

  &:hover {
    transform: scale(1.1);
  }

  &.selected {
    transform: scale(1.2);
    z-index: 1000;
  }

  &.readonly {
    opacity: 0.7;
    filter: grayscale(0.3);

    &:hover {
      transform: scale(1.05);
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
  background: linear-gradient(145deg, #2a2a2a, #1a1a1a);
  border: 2px solid #444;
  border-radius: 8px;
  padding: 3px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  box-shadow:
    inset 0 1px 3px rgba(255, 255, 255, 0.1),
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
    background: #666;
    border-radius: 50%;
  }
}

.light {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.1);
  position: relative;
  transition: all 0.3s ease;

  &::before {
    content: '';
    position: absolute;
    top: 2px;
    left: 3px;
    width: 4px;
    height: 4px;
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
    background: radial-gradient(circle at 30% 30%, #4a1f1f, #2d1b1b);
  }

  &.active {
    background: radial-gradient(circle at 30% 30%, #ff6b6b, #e74c3c);
    box-shadow:
      0 0 8px rgba(255, 107, 107, 0.6),
      0 0 16px rgba(255, 107, 107, 0.3);
  }
}

.yellow-light {
  background: radial-gradient(circle at 30% 30%, #ffd93d, #f39c12);

  &.dim {
    background: radial-gradient(circle at 30% 30%, #4a4020, #2d2516);
  }

  &.active {
    background: radial-gradient(circle at 30% 30%, #ffd93d, #f1c40f);
    box-shadow:
      0 0 8px rgba(255, 217, 61, 0.6),
      0 0 16px rgba(255, 217, 61, 0.3);
  }
}

.green-light {
  background: radial-gradient(circle at 30% 30%, #00b894, #27ae60);

  &.dim {
    background: radial-gradient(circle at 30% 30%, #1f4a3a, #1b2d26);
  }

  &.active {
    background: radial-gradient(circle at 30% 30%, #00b894, #2ecc71);
    box-shadow:
      0 0 8px rgba(0, 184, 148, 0.6),
      0 0 16px rgba(0, 184, 148, 0.3);
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
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  background: radial-gradient(circle, rgba(0, 229, 255, 0.2) 0%, transparent 70%);
  border-radius: 50%;
  animation: pulse 2s ease-in-out infinite;
  pointer-events: none;
  z-index: -1;
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
  border-color: rgba(0, 180, 216, 0.5);

  &:hover {
    border-color: rgba(0, 229, 255, 0.8);
    box-shadow:
      inset 0 1px 3px rgba(255, 255, 255, 0.1),
      0 2px 8px rgba(0, 0, 0, 0.3),
      0 0 12px rgba(0, 229, 255, 0.3);
  }
}

.readonly .light-housing {
  border-color: rgba(102, 102, 102, 0.5);
}

.selected .light-housing {
  border-color: rgba(255, 215, 0, 0.8);
  box-shadow:
    inset 0 1px 3px rgba(255, 255, 255, 0.2),
    0 2px 12px rgba(0, 0, 0, 0.4),
    0 0 16px rgba(255, 215, 0, 0.5);
}

.partially-selected .light-housing {
  border-color: rgba(0, 180, 216, 0.8);
  box-shadow:
    inset 0 1px 3px rgba(255, 255, 255, 0.1),
    0 2px 10px rgba(0, 0, 0, 0.3),
    0 0 12px rgba(0, 180, 216, 0.4);
}


@media (max-width: 768px) {
  .light {
    width: 10px;
    height: 10px;
  }

  .light-housing {
    padding: 2px;
    gap: 1px;
  }

  .light-pole {
    height: 6px;
  }
}
</style>
