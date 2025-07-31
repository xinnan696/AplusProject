<template>
  <div 
    class="emergency-vehicle" 
    :style="vehicleStyle"
    @click="handleClick"
    :title="tooltipText"
  >
    <div class="vehicle-core">
      <div class="pulse-ring"></div>
      <div class="pulse-ring-2"></div>
      <div class="vehicle-dot">
        <div class="inner-glow"></div>
      </div>
    </div>
    
    <!-- 车辆信息标签 -->
    <div class="vehicle-info" v-if="showInfo">
      <div class="info-header">
        <span class="vehicle-id">{{ vehicleData.vehicleID }}</span>
        <span class="organization">{{ vehicleData.organization }}</span>
      </div>
      <div class="info-details">
        <div class="detail-item">
          <span class="label">Event:</span>
          <span class="value">{{ vehicleData.eventID }}</span>
        </div>
        <div class="detail-item" v-if="vehicleData.upcomingTlsCountdown > 0">
          <span class="label">Next TLS:</span>
          <span class="value">{{ vehicleData.upcomingTlsCountdown.toFixed(1) }}s</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

interface EmergencyVehicleData {
  eventID: string
  vehicleID: string
  organization: string
  currentEdgeID: string
  upcomingJunctionID?: string
  nextEdgeID?: string
  upcomingTlsID?: string
  upcomingTlsState?: string
  upcomingTlsCountdown: number
  position: {
    x: number
    y: number
    timestamp: number
  }
}

const props = defineProps<{
  vehicleData: EmergencyVehicleData
  mapPixelPosition: [number, number]
  showInfo?: boolean
}>()

const emit = defineEmits<{
  (e: 'vehicleClick', vehicleData: EmergencyVehicleData): void
}>()

const vehicleStyle = computed(() => ({
  position: 'absolute',
  left: `${props.mapPixelPosition[0]}px`,
  top: `${props.mapPixelPosition[1]}px`,
  transform: 'translate(-50%, -50%)',
  zIndex: '1500'
}))

const tooltipText = computed(() => {
  return `${props.vehicleData.vehicleID} - ${props.vehicleData.organization}\nEvent: ${props.vehicleData.eventID}`
})

const handleClick = () => {
  emit('vehicleClick', props.vehicleData)
}
</script>

<style scoped lang="scss">
.emergency-vehicle {
  position: relative;
  cursor: pointer;
  pointer-events: auto;
}

.vehicle-core {
  position: relative;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pulse-ring {
  position: absolute;
  width: 100%;
  height: 100%;
  border: 2px solid #ff0000;
  border-radius: 50%;
  animation: emergencyPulse 2s cubic-bezier(0.25, 0.46, 0.45, 0.94) infinite;
  opacity: 0.6;
}

.pulse-ring-2 {
  position: absolute;
  width: 140%;
  height: 140%;
  border: 1px solid #ff4444;
  border-radius: 50%;
  animation: emergencyPulse 2s cubic-bezier(0.25, 0.46, 0.45, 0.94) infinite 0.5s;
  opacity: 0.4;
}

.vehicle-dot {
  position: relative;
  width: 12px;
  height: 12px;
  background: radial-gradient(circle, #ff0000 0%, #cc0000 50%, #990000 100%);
  border-radius: 50%;
  box-shadow: 
    0 0 10px #ff0000,
    0 0 20px #ff0000,
    0 0 30px #ff0000,
    inset 0 0 5px rgba(255, 255, 255, 0.3);
  animation: emergencyBlink 1s ease-in-out infinite alternate;
}

.inner-glow {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 8px;
  height: 8px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.8) 0%, transparent 70%);
  border-radius: 50%;
  animation: innerShine 2s ease-in-out infinite;
}

.vehicle-info {
  position: absolute;
  top: -60px;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(135deg, rgba(20, 20, 30, 0.95) 0%, rgba(40, 40, 60, 0.95) 100%);
  border: 1px solid #ff0000;
  border-radius: 8px;
  padding: 8px 12px;
  min-width: 200px;
  box-shadow: 
    0 4px 16px rgba(0, 0, 0, 0.6),
    0 0 20px rgba(255, 0, 0, 0.3),
    inset 0 1px 3px rgba(255, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  pointer-events: auto;
  z-index: 1000;
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  border-bottom: 1px solid rgba(255, 0, 0, 0.3);
  padding-bottom: 4px;
}

.vehicle-id {
  font-size: 14px;
  font-weight: bold;
  color: #ff4444;
  text-shadow: 0 0 5px rgba(255, 68, 68, 0.5);
}

.organization {
  font-size: 11px;
  color: #ccc;
}

.info-details {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
}

.label {
  color: #aaa;
}

.value {
  color: #fff;
  font-weight: 500;
}

// 动画效果
@keyframes emergencyPulse {
  0% {
    transform: scale(1);
    opacity: 0.6;
  }
  50% {
    transform: scale(1.8);
    opacity: 0.1;
  }
  100% {
    transform: scale(2.5);
    opacity: 0;
  }
}

@keyframes emergencyBlink {
  0% {
    box-shadow: 
      0 0 10px #ff0000,
      0 0 20px #ff0000,
      0 0 30px #ff0000,
      inset 0 0 5px rgba(255, 255, 255, 0.3);
    filter: brightness(1);
  }
  100% {
    box-shadow: 
      0 0 15px #ff4444,
      0 0 30px #ff4444,
      0 0 45px #ff4444,
      inset 0 0 8px rgba(255, 255, 255, 0.5);
    filter: brightness(1.5);
  }
}

@keyframes innerShine {
  0%, 100% {
    opacity: 0.6;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.2);
  }
}

// 悬停效果
.emergency-vehicle:hover {
  .vehicle-dot {
    animation-duration: 0.3s;
    transform: scale(1.2);
  }
  
  .pulse-ring,
  .pulse-ring-2 {
    animation-duration: 1s;
    border-color: #ff4444;
  }
  
  .vehicle-info {
    border-color: #ff4444;
    box-shadow: 
      0 6px 20px rgba(0, 0, 0, 0.8),
      0 0 30px rgba(255, 68, 68, 0.5),
      inset 0 1px 5px rgba(255, 68, 68, 0.2);
  }
}
</style>
