<template>
  <div class="dashboard-card">
    <div class="card-header">
      <span class="card-title">{{ title }}</span>
      <div class="card-filters">
        <slot name="filters"></slot>
      </div>
    </div>
    <div class="card-content">
      <slot></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
}>()
</script>

<style scoped lang="scss">
.dashboard-card {
  background-color: #252A3F; // 根据UI图取色
  border-radius: 8px;
  color: #E0E0E0;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  padding: 0.12rem 0.18rem;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.01rem;
}

.card-title {
  font-size: 0.20rem;
  font-weight: bold;
}

.card-filters {
  display: flex;
  gap: 0.1rem;
}

.card-content {
  flex-grow: 1;
  min-height: 0; // Important for flex children with charts
}
</style>
