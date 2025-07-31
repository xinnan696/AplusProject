<template>
  <input
    class="common-box"
    v-bind="$attrs"
    v-model="modelValue"
    :type="type"
  />
</template>

<script setup lang="ts">
import { defineProps, defineEmits, computed } from 'vue'

const props = defineProps<{
  modelValue: string | number
  type?: string
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
