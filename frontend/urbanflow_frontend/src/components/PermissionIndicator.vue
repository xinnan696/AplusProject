<template>
  <div class="permission-indicator" :class="permissionClass">
    <div class="permission-icon">
      <svg v-if="status === 'controllable'" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
        <path d="M8 0L10.5 5H16L11.5 8L13 13L8 10L3 13L4.5 8L0 5H5.5L8 0Z"/>
      </svg>
      <svg v-else-if="status === 'readonly'" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
        <path d="M8 2C5.5 2 3.5 4 3.5 6.5V7H3C2.5 7 2 7.5 2 8V13C2 13.5 2.5 14 3 14H13C13.5 14 14 13.5 14 13V8C14 7.5 13.5 7 13 7H12.5V6.5C12.5 4 10.5 2 8 2ZM8 3C10 3 11.5 4.5 11.5 6.5V7H4.5V6.5C4.5 4.5 6 3 8 3ZM8 9.5C8.8 9.5 9.5 10.2 9.5 11S8.8 12.5 8 12.5 6.5 11.8 6.5 11 7.2 9.5 8 9.5Z"/>
      </svg>
      <svg v-else width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
        <path d="M8 0C3.6 0 0 3.6 0 8S3.6 16 8 16 16 12.4 16 8 12.4 0 8 0ZM8 14C4.7 14 2 11.3 2 8S4.7 2 8 2 14 4.7 14 8 11.3 14 8 14ZM11 5L5 11L4 10L10 4L11 5Z"/>
      </svg>
    </div>
    <span class="permission-text">{{ statusText }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  status: 'controllable' | 'readonly' | 'forbidden'
  customText?: string
}

const props = defineProps<Props>()

const permissionClass = computed(() => {
  return `permission-${props.status}`
})

const statusText = computed(() => {
  if (props.customText) return props.customText
  
  switch (props.status) {
    case 'controllable':
      return 'Controllable'
    case 'readonly':
      return 'Read Only'
    case 'forbidden':
      return 'No Access'
    default:
      return 'Unknown'
  }
})
</script>

<style scoped lang="scss">
.permission-indicator {
  display: inline-flex;
  align-items: center;
  gap: 0.06rem;
  padding: 0.04rem 0.08rem;
  border-radius: 0.04rem;
  font-size: 0.12rem;
  font-weight: 600;
  transition: all 0.3s ease;
}

.permission-controllable {
  background: linear-gradient(135deg, rgba(0, 230, 118, 0.2) 0%, rgba(76, 175, 80, 0.1) 100%);
  color: #00E676;
  border: 1px solid rgba(0, 230, 118, 0.3);
  
  .permission-icon {
    color: #00E676;
  }
}

.permission-readonly {
  background: linear-gradient(135deg, rgba(255, 193, 7, 0.2) 0%, rgba(255, 152, 0, 0.1) 100%);
  color: #FFC107;
  border: 1px solid rgba(255, 193, 7, 0.3);
  
  .permission-icon {
    color: #FFC107;
  }
}

.permission-forbidden {
  background: linear-gradient(135deg, rgba(244, 67, 54, 0.2) 0%, rgba(211, 47, 47, 0.1) 100%);
  color: #F44336;
  border: 1px solid rgba(244, 67, 54, 0.3);
  
  .permission-icon {
    color: #F44336;
  }
}

.permission-icon {
  width: 0.16rem;
  height: 0.16rem;
  display: flex;
  align-items: center;
  justify-content: center;
  
  svg {
    width: 100%;
    height: 100%;
  }
}

.permission-text {
  white-space: nowrap;
  text-shadow: 0 0 4px currentColor;
}
</style>
