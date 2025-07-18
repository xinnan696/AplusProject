<!-- components/common/CommonButton.vue -->
<template>
  <button
    class="common-button"
    :class="variant"
    :disabled="disabled"
    @click="handleClick"
  >
    {{ label }}
  </button>
</template>

<script setup lang="ts">
interface Props {
  label: string
  variant?: 'primary' | 'secondary'
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'primary',
  disabled: false
})

const emit = defineEmits<{
  (e: 'click'): void
}>()

const handleClick = () => {
  if (!props.disabled) {
    emit('click')
  }
}
</script>

<style scoped lang="scss">
.common-button {
  width: 1.4rem;
  height: 0.4rem;
  font-size: 0.14rem;
  font-weight: bold;
  border-radius: 0.2rem;
  border: none;
  cursor: pointer;
  transition: opacity 0.2s ease, background-color 0.2s ease;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }


  &.primary {
    background-color: #00B4D8;
    color: #FFFFFF;

    &:hover:not(:disabled) {
      background-color: #0096C7;
    }
  }


  &.secondary {
    background-color: #999999;
    color: #FFFFFF;

    &:hover:not(:disabled) {
      background-color: #777777;
    }
  }
}
</style>


<!-- 使用方法
<template>
  基本使用
  <CommonButton label="保存" @click="handleSave" />

  不同颜色变体
  <CommonButton label="Apply" variant="primary" @click="handleSubmit" />
  <CommonButton label="Cancel" variant="secondary" @click="handleCancel" />

  禁用状态
  <CommonButton
    label="提交"
    variant="primary"
    :disabled="!formValid"
    @click="handleSubmit"
  />
</template> -->
