<template>
  <div class="custom-select" ref="selectRef">
    <div class="select-selected" @click="toggleDropdown">
      <span class="select-label">{{ selectedLabel }}</span>
      <span class="arrow" :class="{ 'open': isOpen }"></span>
    </div>
    <div v-if="isOpen" class="select-items">
      <input
        v-if="props.showSearch"
        type="text"
        v-model="searchTerm"
        class="search-input"
        placeholder="Search..."
        @keydown.enter.prevent="handleSearch"
      />
      <ul class="options-list">
        <li
          v-for="option in filteredOptions"
          :key="option.value"
          @click="selectOption(option)"
          class="option-item"
          @mouseenter="showTooltip"
          @mouseleave="hideTooltip"
        >
          {{ option.label }}
        </li>
      </ul>
    </div>

    <div
      v-if="tooltip.visible"
      class="custom-tooltip"
      :style="tooltip.style"
    >
      {{ tooltip.text }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'

const props = withDefaults(defineProps<{
  options: { value: any; label: string }[]
  modelValue: any
  showSearch?: boolean
}>(), {
  showSearch: true,
})

const emit = defineEmits(['update:modelValue'])

const isOpen = ref(false)
const searchTerm = ref('')
const searchAppliedTerm = ref('')
const selectRef = ref<HTMLElement | null>(null)

// ★★★ 新增修改 2：为自定义悬浮框创建响应式状态 ★★★
const tooltip = reactive({
  visible: false,
  text: '',
  style: {}
})

const selectedLabel = computed(() => {
  const selected = props.options.find(opt => opt.value === props.modelValue)
  return selected ? selected.label : 'Select an option'
})

const filteredOptions = computed(() => {
  if (!searchAppliedTerm.value) {
    return props.options;
  }
  return props.options.filter(option =>
    option.label.toLowerCase().includes(searchAppliedTerm.value.toLowerCase())
  );
});

// ★★★ 新增修改 3：创建显示和隐藏悬浮框的函数 ★★★

/**
 * 当鼠标悬停在选项上时触发
 * @param event 鼠标事件对象
 */
const showTooltip = (event: MouseEvent) => {
  const target = event.target as HTMLElement;
  // 检查文本是否真的被截断了 (滚动宽度 > 可见宽度)
  if (target.scrollWidth > target.clientWidth) {
    const rect = target.getBoundingClientRect();
    tooltip.text = target.innerText;
    tooltip.style = {
      // 使用 fixed 定位，使其能溢出滚动容器
      position: 'fixed',
      // 定位在元素的右侧，并垂直居中
      top: `${rect.top + rect.height / 2}px`,
      left: `${rect.right + 8}px`, // 在元素右边 8px 处
      transform: 'translateY(-50%)', // 垂直居中
    };
    tooltip.visible = true;
  }
};

/**
 * 当鼠标离开时触发
 */
const hideTooltip = () => {
  tooltip.visible = false;
};


function toggleDropdown() {
  isOpen.value = !isOpen.value
}

function selectOption(option: { value: any; label: string }) {
  emit('update:modelValue', option.value)
  isOpen.value = false
  searchTerm.value = ''
  searchAppliedTerm.value = ''
}

function handleSearch() {
  searchAppliedTerm.value = searchTerm.value;
}

function closeDropdown(event: MouseEvent) {
  if (selectRef.value && !selectRef.value.contains(event.target as Node)) {
    isOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', closeDropdown)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeDropdown)
})

watch(() => props.modelValue, () => {
  searchTerm.value = '';
  searchAppliedTerm.value = '';
});

</script>

<style scoped lang="scss">
.custom-select {
  position: relative;
  width: 100%;
  font-size: 0.14rem;
  color: #fff;
  background-color: #3B3B5D;
  border-radius: 4px;
}

.select-selected {
  padding: 0 0.3rem 0 0.1rem;
  height: 100%;
  display: flex;
  align-items: center;
  cursor: pointer;
  user-select: none;
  position: relative;
}

.select-label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-grow: 1;
  width: 0;
}

.arrow {
  position: absolute;
  right: 0.1rem;
  top: 50%;
  transform: translateY(-50%);
  width: 0;
  height: 0;
  border-left: 5px solid transparent;
  border-right: 5px solid transparent;
  border-top: 6px solid #fff;
  transition: transform 0.2s ease;
}

.arrow.open {
  transform: translateY(-50%) rotate(180deg);
}

.select-items {
  position: absolute;
  top: calc(100% + 5px);
  left: 0;
  right: 0;
  background-color: #3B3B5D;
  border-radius: 4px;
  z-index: 10;
  border: 1px solid #4a4a70;
  padding: 0.05rem;
}

.search-input {
  width: calc(100% - 0.1rem);
  padding: 0.08rem 0.05rem;
  margin-bottom: 0.05rem;
  background-color: #2A2A45;
  border: 1px solid #4a4a70;
  color: #fff;
  border-radius: 3px;
}

.options-list {
  list-style: none;
  padding: 0;
  margin: 0;
  max-height: 100px;
  overflow-y: auto;

  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-thumb {
    background: #4a4a70;
    border-radius: 3px;
  }
  &::-webkit-scrollbar-track {
    background: #2A2A45;
  }
}

.option-item {
  padding: 0.08rem 0.1rem;
  cursor: pointer;
  transition: background-color 0.2s;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  &:hover {
    background-color: #007BFF;
  }
}

/* ★★★ 新增修改 4：添加自定义悬浮框的样式 ★★★ */
.custom-tooltip {
  /* 定位与行为 */
  z-index: 1000; /* 确保在最顶层 */
  pointer-events: none; /* 确保鼠标可以穿透它，不会影响下方的悬停事件 */
  white-space: nowrap; /* 确保悬浮框本身内容不换行 */

  /* 样式 (部分样式与DashboardCard的悬浮框保持一致) */
  font-size: 0.14rem;
  font-weight: 500;
  padding: 0.08rem 0.12rem;
  border-radius: 4px;
  border: 1px solid #4a4a70;
  font-family: 'Inter', 'Segoe UI', 'Arial', 'Helvetica Neue', 'Roboto', sans-serif !important;
  line-height: 1.3 !important;

  /* ★★★ 应用您要求的颜色 ★★★ */
  color: white;
  background-color: #2A2A45;
}
</style>
