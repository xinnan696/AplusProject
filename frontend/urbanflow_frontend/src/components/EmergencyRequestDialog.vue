<template>
  <!-- 紧急车辆请求弹窗 - 响应式居中在地图区域 -->
  <transition name="dialog">
    <div v-if="isVisible && pendingVehicle" class="emergency-dialog-overlay" :class="{ 'nav-open': isSidebarOpen }">
      <div class="tracking-request-dialog">
        <div class="dialog-title">
          Priority Vehicle Tracking Request
        </div>
        <div class="dialog-content">
          <span class="info-label">Vehicle ID</span>
          <span class="info-value">{{ pendingVehicle.vehicleID }}</span>

          <span class="info-label">Organization</span>
          <span class="info-value">{{ pendingVehicle.organization }}</span>

          <span class="info-label route-label">Estimated Route</span>
          <div class="info-value route-list">
            <div v-for="(junctionId, index) in pendingVehicle.signalizedJunctions" :key="junctionId" class="route-item">
              <span>{{ getJunctionName(junctionId) }}</span>
              <span v-if="index === 0" class="tag start">START</span>
              <span v-if="index === pendingVehicle.signalizedJunctions.length - 1" class="tag destination">DESTINATION</span>
            </div>
          </div>

          <span class="info-label request-label">Request</span>
          <span class="info-value request-value">Green Light Priority</span>
        </div>
        <div class="dialog-actions">
          <button class="btn-approve" @click="handleApprove">APPROVE</button>
          <button class="btn-reject" @click="handleReject">REJECT</button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useEmergencyStore } from '@/stores/emergency'

interface Props {
  isVisible: boolean
  isSidebarOpen: boolean
  pendingVehicle: any
  junctionIdToNameMap?: Record<string, string>
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'approve', vehicleId: string): void
  (e: 'reject', vehicleId: string): void
}>()

const emergencyStore = useEmergencyStore()

// Junction名称获取函数
const getJunctionName = (junctionId: string) => {
  // 优先使用emergencyStore中的映射数据，如果没有再使用传入的
  return emergencyStore.junctionIdToNameMap[junctionId] || 
         props.junctionIdToNameMap?.[junctionId] || 
         junctionId
}

// 处理批准事件
const handleApprove = () => {
  if (props.pendingVehicle) {
    emit('approve', props.pendingVehicle.vehicleID)
  }
}

// 处理拒绝事件
const handleReject = () => {
  if (props.pendingVehicle) {
    emit('reject', props.pendingVehicle.vehicleID)
  }
}
</script>

<style scoped lang="scss">
/* 弹窗覆盖层 - 相对于地图容器定位，始终在地图中央 */
.emergency-dialog-overlay {
  position: absolute; /* 改为相对定位 */
  top: 0; /* 相对于地图容器的顶部 */
  left: 0; /* 相对于地图容器的左边 */
  right: 0; /* 相对于地图容器的右边 */
  bottom: 0; /* 相对于地图容器的底部 */
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  
  /* 不需要根据导航栏状态调整，因为是相对于地图容器定位 */
}

/* 对话框过渡动画 */
.dialog-enter-active, .dialog-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.dialog-enter-from, .dialog-leave-to {
  .emergency-dialog-overlay {
    opacity: 0;
    backdrop-filter: blur(0px);
  }
  
  .tracking-request-dialog {
    opacity: 0;
    transform: scale(0.9) translateY(20px);
  }
}

.dialog-enter-to, .dialog-leave-from {
  .emergency-dialog-overlay {
    opacity: 1;
    backdrop-filter: blur(8px);
  }
  
  .tracking-request-dialog {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* 紧急车辆请求对话框样式 - 简洁统一风格 */
.tracking-request-dialog {
  width: 600px;
  max-width: 90%;
  background: #1E1E2F; /* 使用指定背景色 */
  border-radius: 16px;
  border: 1px solid rgba(74, 85, 104, 0.4);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
  color: #E0E0E0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: all 0.3s ease;
}

.dialog-title {
  padding: 0.24rem 0.32rem;
  font-size: 0.22rem;
  font-weight: 700;
  color: #00B4D8;
  background: #1E1E2F; /* 统一背景色 */
  text-align: center;
  /* 移除分割线 */
}

.dialog-content {
  padding: 0.32rem;
  display: grid;
  grid-template-columns: 150px 1fr;
  gap: 0.24rem;
  align-items: center;
  font-size: 0.16rem;
  background: #1E1E2F; /* 统一背景色 */
}

.info-label {
  font-weight: 600;
  color: #00B4D8; /* 统一小标题颜色 */
  text-align: right;
  font-size: 0.15rem;
  /* 移除文字阴影 */
}

.info-value {
  font-weight: 600;
  color: #FFFFFF;
  background: rgba(74, 85, 104, 0.2); /* 统一背景 */
  padding: 0.12rem 0.16rem;
  border-radius: 6px;
  border: 1px solid rgba(74, 85, 104, 0.3); /* 简化边框 */
  transition: all 0.3s ease;
  
  &:hover {
    background: rgba(74, 85, 104, 0.35); /* 去除荧光，只是稍微变亮 */
    border-color: rgba(74, 85, 104, 0.4);
  }
}

.route-label {
  align-self: start;
  margin-top: 0.08rem;
}

.info-value.route-list {
  display: flex;
  flex-direction: column;
  gap: 0.12rem;
  padding: 0.16rem;
  background: rgba(74, 85, 104, 0.2); /* 统一背景 */
  border: 1px solid rgba(74, 85, 104, 0.3);
}

.route-item {
  display: flex;
  align-items: center;
  gap: 0.12rem;
  padding: 0.08rem 0.12rem;
  background: rgba(74, 85, 104, 0.2); /* 统一背景 */
  border-radius: 4px;
  font-size: 0.14rem;
  transition: all 0.3s ease;
  
  &:hover {
    background: rgba(74, 85, 104, 0.35); /* 去除荧光，只是稍微变亮 */
    transform: translateX(4px);
  }
}

.route-item .tag {
  font-weight: 700;
  font-size: 0.11rem;
  padding: 0.04rem 0.08rem;
  border-radius: 3px;
  /* 移除文字阴影 */
}

.tag.start {
  color: #FFFFFF;
  background: #22C55E; /* 简化背景 */
  /* 移除阴影效果 */
}

.tag.destination {
  color: #FFFFFF;
  background: #EF4444; /* 简化背景 */
  /* 移除阴影效果 */
}

.request-label, .request-value {
  color: #00B4D8; /* 统一颜色 */
  /* 移除文字阴影 */
}

.request-value {
  font-weight: 700;
  background: rgba(74, 85, 104, 0.2); /* 统一背景 */
  border-color: rgba(74, 85, 104, 0.3);
}

.dialog-actions {
  padding: 0.24rem 0.32rem;
  display: flex;
  justify-content: space-between; /* 与 ControlManual 一致 */
  width: 4.2rem; /* 与 ControlManual 一致 */
  margin: 0 auto; /* 居中 */
  background: #1E1E2F; /* 统一背景色 */
  /* 移除顶部分割线 */
}

.dialog-actions button {
  width: 1.4rem; /* 与 ControlManual 保持一致 */
  height: 0.4rem; /* 与 ControlManual 保持一致 */
  border-radius: 0.2rem; /* 与 ControlManual 保持一致 */
  border: none; /* 移除边框 */
  font-size: 0.14rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.btn-approve {
  background: linear-gradient(135deg, #00B4D8 0%, #0090aa 100%); /* 与 ControlManual 保持一致 */
  color: #FFFFFF;
  
  &:hover {
    background: linear-gradient(135deg, #00d4f8 0%, #00B4D8 100%);
    transform: translateY(-2px) scale(1.02);
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
  }
  
  &:active {
    transform: translateY(-1px) scale(1.02);
  }
}

.btn-reject {
  background: linear-gradient(135deg, #718096 0%, #4A5568 100%); /* 与 ControlManual 保持一致 */
  color: #FFFFFF;
  
  &:hover {
    background: linear-gradient(135deg, #A0AEC0 0%, #718096 100%);
    transform: translateY(-2px) scale(1.02);
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
  }
  
  &:active {
    transform: translateY(-1px) scale(1.02);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .emergency-dialog-overlay {
    /* 移动设备上不需要特殊处理，因为是相对定位 */
  }
  
  .tracking-request-dialog {
    width: 95%;
    margin: 0.2rem;
  }
  
  .dialog-content {
    grid-template-columns: 1fr;
    gap: 0.16rem;
    text-align: center;
  }
  
  .info-label {
    text-align: center;
    margin-bottom: 0.08rem;
  }
  
  .dialog-actions {
    flex-direction: column;
    gap: 0.16rem;
  }
}
</style>
