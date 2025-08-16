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

const showTooltip = (event: MouseEvent) => {
  const target = event.target as HTMLElement;
  if (target.scrollWidth > target.clientWidth) {
    const rect = target.getBoundingClientRect();
    tooltip.text = target.innerText;
    tooltip.style = {
      position: 'fixed',
      top: `${rect.top + rect.height / 2}px`,
      left: `${rect.right + 8}px`,
      transform: 'translateY(-50%)',
    };
    tooltip.visible = true;
  }
};

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

.custom-tooltip {
  z-index: 1000;
  pointer-events: none;
  white-space: nowrap;
  font-size: 0.14rem;
  font-weight: 500;
  padding: 0.08rem 0.12rem;
  border-radius: 4px;
  border: 1px solid #4a4a70;
  font-family: 'Inter', 'Segoe UI', 'Arial', 'Helvetica Neue', 'Roboto', sans-serif !important;
  line-height: 1.3 !important;
  color: white;
  background-color: #2A2A45;
}
</style>
