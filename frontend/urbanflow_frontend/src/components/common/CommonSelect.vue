<template>
  <select class="common-box" v-model="modelValue" v-bind="$attrs">
    <option disabled value="">{{ placeholder }}</option>
    <option
      v-for="(item, index) in options"
      :key="index"
      :value="item.value"
    >
      {{ item.label }}
    </option>
  </select>
</template>

<script setup lang="ts">
import { defineProps, defineEmits, computed } from 'vue'

interface OptionItem {
  label: string
  value: string | number
}

const props = defineProps<{
  modelValue: string | number
  options: OptionItem[]
  placeholder?: string
}>()

const emit = defineEmits(['update:modelValue'])

const modelValue = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val)
})
</script>

<style scoped>
.common-box {
  width: 3.6rem;
  height: 0.4rem;
  background: #1a1b2c;
  border: 1px solid #00B4D8;
  border-radius: 0.08rem;
  color: white;
  font-size: 0.14rem;
  box-sizing: border-box;
  padding: 0 0.1rem;
}
</style>
