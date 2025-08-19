<template>
  <div class="dashboard-card">
    <div class="card-header">
      <div
        class="card-title-wrapper"
        ref="titleWrapperRef"
        @mouseenter="showTooltip"
        @mouseleave="hideTooltip"
      >
        <span class="card-title">{{ title }}</span>
        <div
          v-if="isTooltipVisible && titleTooltip"
          class="custom-tooltip-text"
          ref="tooltipRef"
          :style="tooltipStyle"
        >
          {{ titleTooltip }}
        </div>
      </div>
      <div class="card-filters">
        <slot name="filters"></slot>
      </div>
    </div>
    <div class="card-content">
      <slot></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'

defineProps<{
  title: string,
  titleTooltip?: string,
}>()

const titleWrapperRef = ref<HTMLElement | null>(null);
const tooltipRef = ref<HTMLElement | null>(null);
const tooltipStyle = ref({});
const isTooltipVisible = ref(false)

const showTooltip = async () => {
  isTooltipVisible.value = true
  await nextTick()
  calculateTooltipPosition()
}

const hideTooltip = () => {
  isTooltipVisible.value = false
}

const calculateTooltipPosition = () => {
  if (!tooltipRef.value || !titleWrapperRef.value) return;

  const titleRect = titleWrapperRef.value.getBoundingClientRect();
  const tooltipRect = tooltipRef.value.getBoundingClientRect();
  const viewportWidth = window.innerWidth;
  const margin = 10;
  let left = titleRect.left;

  if (left + tooltipRect.width > viewportWidth - margin) {
    left = viewportWidth - tooltipRect.width - margin;
  }

  if (left < margin) {
    left = margin;
  }

  tooltipStyle.value = {
    position: 'fixed',
    top: `${titleRect.bottom + 2}px`,
    left: `${left}px`,
    transform: 'none'
  };
}
</script>

<style scoped lang="scss">
.dashboard-card {
  background-color: #252A3F;
  border-radius: 8px;
  color: #E0E0E0;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  padding: 0.12rem 0.18rem;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.01rem;
}

.card-title-wrapper {
  position: relative;
  cursor: default;
}

.card-title {
  font-size: 0.24rem;
  font-weight: bold;
}

.card-filters {
  display: flex;
  gap: 0.1rem;
}

.card-content {
  flex-grow: 1;
  min-height: 0;
}

.custom-tooltip-text {
  background-color: rgba(45, 45, 45, 0.95);
  color: #ffffff;
  border: 1px solid #4a4a70;
  border-radius: 4px;
  padding: 0.08rem 0.12rem;
  font-size: 12px;
  font-weight: 500;
  font-family: 'Inter', 'Segoe UI', 'Arial', 'Helvetica Neue', 'Roboto', sans-serif !important;
  line-height: 1.3 !important;
  white-space: nowrap;
  position: absolute;
  z-index: 100;
  top: calc(100% + 10px);
  left: 50%;
  transform: translateX(-50%);
  pointer-events: none;
  opacity: 0;
  animation: fadeIn 0.2s ease-in-out forwards;

}

@keyframes fadeIn {
  to {
    opacity: 1;
  }
}
</style>
