<template>
  <div class="user-list-page">
    <ControlHeader @toggle-nav="toggleNav" />
    <ControlNav :isVisible="isNavVisible" />

    <div class="main-area">

    </div>
  </div>
</template>

<script setup lang="ts">
import ControlHeader from '@/views/control/ControlHeader.vue'
import ControlNav from '@/views/control/ControlNav.vue'
import { isNavVisible, toggleNav } from '@/utils/navState'
</script>

<style scoped lang="scss">
.user-list-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: #1E1E2F;
  z-index: 1;
}

.main-area {
  height: calc(100% - 0.64rem);
  display: flex;
  overflow: hidden;
}

.user-container {
  flex: 1;
  padding: 2rem;
  background-color: #f5f5f5;
  overflow-y: auto;
}

.page-header {
  margin-bottom: 2rem;
}

.page-header h1 {
  color: #2d3748;
  margin-bottom: 0.5rem;
  font-size: 2rem;
}

.page-header p {
  color: #718096;
  font-size: 1.1rem;
}

.user-content {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.actions-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.add-btn {
  padding: 0.75rem 1.5rem;
  background-color: #00e3ff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
}

.add-btn:hover {
  background-color: #00bcd4;
}

.search-input {
  padding: 0.75rem;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  width: 300px;
}

.users-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

.user-card {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 1.5rem;
  transition: box-shadow 0.2s;
}

.user-card:hover {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.user-avatar {
  width: 60px;
  height: 60px;
  background-color: #00e3ff;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  font-weight: bold;
  margin-bottom: 1rem;
}

.user-info h3 {
  margin: 0 0 0.5rem 0;
  color: #2d3748;
}

.user-email {
  color: #718096;
  margin: 0 0 0.25rem 0;
}

.user-role {
  color: #4a5568;
  font-weight: 500;
  margin: 0 0 1rem 0;
}

.user-actions {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.edit-btn, .delete-btn {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
}

.edit-btn {
  background-color: #3182ce;
  color: white;
}

.edit-btn:hover {
  background-color: #2c5aa0;
}

.delete-btn {
  background-color: #e53e3e;
  color: white;
}

.delete-btn:hover {
  background-color: #c53030;
}

.user-status {
  text-align: right;
}

.status {
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  font-size: 0.875rem;
  font-weight: 500;
}

.status.active {
  background-color: #c6f6d5;
  color: #22543d;
}

.status.inactive {
  background-color: #fed7d7;
  color: #742a2a;
}
</style>
