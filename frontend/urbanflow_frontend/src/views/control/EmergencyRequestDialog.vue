<template>
  <div class="dialog-overlay">
    <div class="dialog-box">
      <div class="dialog-title">Special Vehicle Tracking Request</div>
      <div class="dialog-content">
        <div class="info-row">
          <span class="label">Vehicle ID:</span>
          <span class="value">{{ event.vehicleId }}</span>
        </div>
        <div class="info-row">
          <span class="label">Organization:</span>
          <span class="value">{{ event.organization }}</span>
        </div>
        <div class="info-row">
          <span class="label">Estimated Route:</span>
          <span class="value">{{ event.route.join(' → ') }}</span>
        </div>
        <div class="info-row">
          <span class="label">Request:</span>
          <span class="value priority">{{ event.requestType }}</span>
        </div>
      </div>
      <div class="dialog-actions">
        <button class="btn approve-btn" @click="$emit('approve')">APPROVE</button>
        <button class="btn reject-btn" @click="$emit('reject')">REJECT</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  event: {
    vehicleId: string
    organization: string
    route: string[]
    requestType: string
  }
}>()

defineEmits(['approve', 'reject'])
</script>

<style scoped lang="scss">
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
}

.dialog-box {
  width: 5.8rem;
  background-color: #2c2d3c;
  border-radius: 0.1rem;
  border: 1px solid #00b4d8;
  color: white;
  padding: 0.3rem;
  box-shadow: 0 0.1rem 0.3rem rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
}

.dialog-title {
  font-size: 0.22rem;
  font-weight: bold;
  text-align: center;
  margin-bottom: 0.25rem;
  color: #00b4d8;
}

.dialog-content {
  display: flex;
  flex-direction: column;
  gap: 0.18rem;
  margin-bottom: 0.3rem;
}

.info-row {
  display: flex;
  font-size: 0.16rem;
}

.label {
  width: 1.8rem;
  font-weight: bold;
  color: #cfcfcf;
}

.value {
  flex: 1;
}

.priority {
  color: #f59a23;
  font-weight: bold;
}

.dialog-actions {
  display: flex;
  justify-content: center;
  gap: 0.4rem;
}

.btn {
  width: 1.6rem;
  height: 0.45rem;
  border: none;
  border-radius: 0.225rem;
  font-size: 0.16rem;
  font-weight: bold;
  color: white;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn:hover {
  opacity: 0.85;
}

.approve-btn {
  background-color: #00b4d8;
}

.reject-btn {
  background-color: #999999;
}
</style>
