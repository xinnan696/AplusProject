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

// --- CHANGE: 为 DOM 元素和动态样式创建引用 ---
const titleWrapperRef = ref<HTMLElement | null>(null);
const tooltipRef = ref<HTMLElement | null>(null);
const tooltipStyle = ref({});
const isTooltipVisible = ref(false)

// --- CHANGE: 修改 showTooltip 以调用位置计算函数 ---
const showTooltip = async () => {
  isTooltipVisible.value = true
  // 等待 DOM 更新，确保 tooltipRef.value 存在
  await nextTick()
  calculateTooltipPosition()
}

const hideTooltip = () => {
  isTooltipVisible.value = false
}

// --- CHANGE: 新增函数，动态计算并修正 Tooltip 位置 ---
const calculateTooltipPosition = () => {
  if (!tooltipRef.value || !titleWrapperRef.value) return;

  const titleRect = titleWrapperRef.value.getBoundingClientRect();
  const tooltipRect = tooltipRef.value.getBoundingClientRect();
  const viewportWidth = window.innerWidth;
  const margin = 10; // 离屏幕边缘的最小间距

  // 1. 默认期望位置：左侧与标题对齐
  let left = titleRect.left;

  // 2.【核心】检查右边界：如果提示框的右侧会超出屏幕，则向左移动
  if (left + tooltipRect.width > viewportWidth - margin) {
    // 将提示框的右边缘对齐到屏幕的右边缘（减去边距）
    left = viewportWidth - tooltipRect.width - margin;
  }

  // 3. 检查左边界：作为保险，确保在任何调整后都不会超出左边界
  if (left < margin) {
    left = margin;
  }

  // 4. 应用最终计算出的安全位置
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
  background-color: #252A3F; // 根据UI图取色
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
  cursor: default; /* 添加鼠标悬停手势，提示用户可交互 */
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
  min-height: 0; // Important for flex children with charts
}

.custom-tooltip-text {
  // --- CHANGE: 更新 Tooltip 样式 ---
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

  /* 默认样式，会被 JS 的动态样式覆盖 */
  position: absolute;
  z-index: 100;
  top: calc(100% + 10px);
  /* 恢复为标准的居中设置作为后备 */
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
