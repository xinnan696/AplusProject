<template>
  <div v-if="isVisible" class="record-panel">
    <div class="record-header">
      <div class="header-title">
        <span>RECORDS</span>
      </div>
    </div>


    <div class="record-content">
 
      <div class="records-list">
        <div
          v-for="record in records"
          :key="record.id"
          class="record-item"
          @click="selectRecord(record)"
          :class="{ selected: selectedRecord?.id === record.id }"
        >
          <div class="record-meta">
            <div class="record-time">{{ record.timestamp }}</div>
            <div class="record-badges">
              <div class="record-source" :class="record.source">{{ record.source.toUpperCase() }}</div>
              <div class="record-status" :class="record.status">{{ record.status.toUpperCase() }}</div>
            </div>
          </div>
          <div class="record-description">{{ record.description }}</div>
        </div>
      </div>


      <div v-if="records.length === 0" class="empty-state">
        <div class="empty-icon">&#xe683;</div>
        <div class="empty-text">No operation records yet</div>
        <div class="empty-subtext">Operations from Manual Control and AI Control will appear here</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useOperationStore, type OperationRecord } from '@/stores/operationStore'

defineProps<{
  isVisible: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'recordSelected', record: OperationRecord): void
}>()

const operationStore = useOperationStore()
const selectedRecord = ref<OperationRecord | null>(null)


const records = computed(() => operationStore.records)

const selectRecord = (record: OperationRecord) => {
  selectedRecord.value = record
  emit('recordSelected', record)
}
</script>

<style scoped lang="scss">
.record-panel {
  position: fixed;
  top: 0.64rem;
  right: 0;
  width: 35%; // 与controlboard一样的宽度（35%）
  height: calc(100vh - 0.64rem);
  background: #1E1E2F;
  border-left: 0.01rem solid #4A5568;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  transform: translateX(0);
  transition: transform 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  box-shadow: -0.08rem 0 0.3rem rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(0.1rem);
}

.record-header {
  background: #1E1E2F;
  padding: 0;
  flex-shrink: 0;
  position: relative;
}

.header-title {
  display: flex;
  align-items: flex-start;
  padding: 0.16rem 0 0 0.24rem;

  span {
    font-size: 0.2rem;
    font-weight: 600;
    color: #00B4D8;
    letter-spacing: 0.02rem;
  }
}

.record-content {
  flex: 1;
  overflow: hidden;
  position: relative;
  background: #1E1E2F;
}


.records-list {
  height: 100%;
  overflow-y: auto;
  padding: 0.2rem;
  padding-right: 0.26rem;

  &::-webkit-scrollbar {
    width: 0.04rem;
  }

  &::-webkit-scrollbar-track {
    background: rgba(74, 85, 104, 0.1);
    border-radius: 0.02rem;
  }

  &::-webkit-scrollbar-thumb {
    background: linear-gradient(180deg, #4A5568 0%, #374151 100%);
    border-radius: 0.02rem;

    &:hover {
      background: linear-gradient(180deg, #6B7280 0%, #4A5568 100%);
    }
  }
}

.record-item {
  padding: 0.18rem 0.16rem;
  margin-bottom: 0.14rem;
  background: linear-gradient(135deg, rgba(74, 85, 104, 0.1) 0%, rgba(74, 85, 104, 0.05) 100%);
  border: 0.01rem solid rgba(74, 85, 104, 0.2);
  border-radius: 0.12rem;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0.0, 0.2, 1);
  position: relative;
  overflow: hidden;
  backdrop-filter: blur(0.05rem);
  box-shadow: 0 0.02rem 0.08rem rgba(0, 0, 0, 0.1);

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 0.04rem;
    background: linear-gradient(180deg, #4A5568 0%, #374151 100%);
    transform: scaleY(0);
    transition: transform 0.3s ease;
    border-radius: 0 0.02rem 0.02rem 0;
  }

  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, transparent 0%, rgba(0, 180, 216, 0.03) 50%, transparent 100%);
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  &:hover {
    background: rgba(74, 85, 104, 0.18);
    border-color: rgba(74, 85, 104, 0.4);
    transform: translateX(0.06rem) translateY(-0.02rem);
    box-shadow: 0 0.06rem 0.16rem rgba(0, 0, 0, 0.25);

    &::before {
      transform: scaleY(1);
    }

    &::after {
      opacity: 1;
    }
  }

  &.selected {
    background: rgba(74, 85, 104, 0.25);
    border-color: #4A5568;
    box-shadow: 0 0.06rem 0.2rem rgba(0, 0, 0, 0.4);
    transform: translateX(0.04rem);

    &::before {
      transform: scaleY(1);
      background: linear-gradient(180deg, #6B7280 0%, #4A5568 100%);
    }

    &::after {
      opacity: 1;
    }
  }

  &:last-child {
    margin-bottom: 0;
  }
}

.record-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.12rem;
}

.record-time {
  font-size: 0.14rem;
  color: #B8C4CE;
  font-family: Arial, sans-serif;
  font-weight: 500;
  letter-spacing: 0.01rem;
}

.record-badges {
  display: flex;
  gap: 0.08rem;
  align-items: center;
}

.record-source {
  font-size: 0.12rem;
  padding: 0.04rem 0.08rem;
  border-radius: 0.06rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.01rem;
  box-shadow: 0 0.02rem 0.04rem rgba(0, 0, 0, 0.2);
  
  &.manual {
    color: #4A5568;
    background: linear-gradient(135deg, rgba(74, 85, 104, 0.2) 0%, rgba(74, 85, 104, 0.1) 100%);
    border: 0.01rem solid rgba(74, 85, 104, 0.4);
    box-shadow: 0 0.02rem 0.04rem rgba(74, 85, 104, 0.2);
  }
  
  &.ai {
    color: #7C3AED;
    background: linear-gradient(135deg, rgba(124, 58, 237, 0.2) 0%, rgba(124, 58, 237, 0.1) 100%);
    border: 0.01rem solid rgba(124, 58, 237, 0.4);
    box-shadow: 0 0.02rem 0.04rem rgba(124, 58, 237, 0.2);
  }
}

.record-status {
  font-size: 0.12rem;
  padding: 0.04rem 0.08rem;
  border-radius: 0.06rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.01rem;
  box-shadow: 0 0.02rem 0.04rem rgba(0, 0, 0, 0.2);
  
  &.success {
    color: #10B981;
    background: linear-gradient(135deg, rgba(16, 185, 129, 0.2) 0%, rgba(16, 185, 129, 0.1) 100%);
    border: 0.01rem solid rgba(16, 185, 129, 0.4);
    box-shadow: 0 0.02rem 0.04rem rgba(16, 185, 129, 0.2);
  }
  
  &.failed {
    color: #EF4444;
    background: linear-gradient(135deg, rgba(239, 68, 68, 0.2) 0%, rgba(239, 68, 68, 0.1) 100%);
    border: 0.01rem solid rgba(239, 68, 68, 0.4);
    box-shadow: 0 0.02rem 0.04rem rgba(239, 68, 68, 0.2);
  }
  
  &.pending {
    color: #F59E0B;
    background: linear-gradient(135deg, rgba(245, 158, 11, 0.2) 0%, rgba(245, 158, 11, 0.1) 100%);
    border: 0.01rem solid rgba(245, 158, 11, 0.4);
    box-shadow: 0 0.02rem 0.04rem rgba(245, 158, 11, 0.2);
  }
}

.record-description {
  font-size: 0.14rem;
  color: #ffffff;
  line-height: 1.5;
  font-weight: 400;
  font-family: Arial, sans-serif;
  letter-spacing: 0.005rem;
}



.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #6b7280;
  text-align: center;
  padding: 0 0.24rem;

  .empty-icon {
    font-size: 0.48rem;
    margin-bottom: 0.16rem;
    opacity: 0.5;
  }

  .empty-text {
    font-size: 0.14rem;
    font-weight: 500;
    margin-bottom: 0.08rem;
  }

  .empty-subtext {
    font-size: 0.12rem;
    color: #9ca3af;
    line-height: 1.4;
  }
}



@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.record-panel {
  animation: slideInRight 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
}

.record-item {
  animation: fadeInUp 0.3s ease;
  animation-fill-mode: both;

  @for $i from 1 through 10 {
    &:nth-child(#{$i}) {
      animation-delay: #{$i * 0.05}s;
    }
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(0.2rem);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1200px) {
  .record-panel {
    width: 40%; // 小屏幕下稍微放大一点
  }
}

@media (max-width: 768px) {
  .record-panel {
    width: 100vw; // 手机端占满屏幕
    right: 0;
  }
}</style>
