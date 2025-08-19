<template>
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
const getJunctionName = (junctionId: string) => {
  return emergencyStore.junctionIdToNameMap[junctionId] ||
         props.junctionIdToNameMap?.[junctionId] ||
         junctionId
}

const handleApprove = () => {
  if (props.pendingVehicle) {
    emit('approve', props.pendingVehicle.vehicleID)
  }
}

const handleReject = () => {
  if (props.pendingVehicle) {
    emit('reject', props.pendingVehicle.vehicleID)
  }
}
</script>

<style scoped lang="scss">
.emergency-dialog-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;

}


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

.tracking-request-dialog {
  width: 600px;
  max-width: 90%;
  background: #1E1E2F;
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
  background: #1E1E2F;
  text-align: center;
}

.dialog-content {
  padding: 0.32rem;
  display: grid;
  grid-template-columns: 150px 1fr;
  gap: 0.24rem;
  align-items: center;
  font-size: 0.16rem;
  background: #1E1E2F;
}

.info-label {
  font-weight: 600;
  color: #FFFFFF;
  text-align: right;
  font-size: 0.15rem;
}

.info-value {
  font-weight: 600;
  color: #FFFFFF;
  background: rgba(74, 85, 104, 0.2);
  padding: 0.12rem 0.16rem;
  border-radius: 6px;
  border: 1px solid rgba(74, 85, 104, 0.3);
  transition: all 0.3s ease;


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
  background: rgba(74, 85, 104, 0.2);
  border: 1px solid rgba(74, 85, 104, 0.3);
}

.route-item {
  display: flex;
  align-items: center;
  gap: 0.12rem;
  padding: 0.08rem 0.12rem;
  background: rgba(74, 85, 104, 0.2);
  border-radius: 4px;
  font-size: 0.14rem;
  transition: all 0.3s ease;


}

.route-item .tag {
  font-weight: 700;
  font-size: 0.11rem;
  padding: 0.04rem 0.08rem;
  border-radius: 3px;
}

.tag.start {
  color: #FFFFFF;
  background: #22C55E;
}

.tag.destination {
  color: #FFFFFF;
  background: #EF4444;
}

.request-label, .request-value {
  color: #FFFFFF;
}

.request-value {
  font-weight: 700;
  background: rgba(74, 85, 104, 0.2);
  border-color: rgba(74, 85, 104, 0.3);
}

.dialog-actions {
  padding: 0.24rem 0.32rem;
  display: flex;
  justify-content: space-between;
  width: 4.2rem;
  margin: 0 auto;
  background: #1E1E2F;
}

.dialog-actions button {
  width: 1.4rem;
  height: 0.4rem;
  border-radius: 0.2rem;
  border: none;
  font-size: 0.14rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.btn-approve {
  background: linear-gradient(135deg, #00B4D8 0%, #0090aa 100%);
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
  background: linear-gradient(135deg, #718096 0%, #4A5568 100%);
  color: #FFFFFF;



  &:active {
    transform: translateY(-1px) scale(1.02);
  }
}

@media (max-width: 768px) {
  .emergency-dialog-overlay {
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
