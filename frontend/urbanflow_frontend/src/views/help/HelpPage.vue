<template>
  <div class="help-page">
    <ControlHeader
      @toggle-nav="toggleNav"
      @toggle-record="toggleRecord"
      @toggle-emergency="toggleEmergency"
      @toggle-priority="togglePriority"
      @mode-changed="handleModeChange"
      @sign-out="handleSignOut"
    />
    <ControlNav :isVisible="isNavVisible" />

    <div class="main-area">
      <div class="help-container">
        <div class="page-header">
          <h1>Help & Documentation</h1>
          <p>Get help with UrbanFlow system features</p>
        </div>

        <div class="help-content">
          <div class="help-sections">
            <div class="help-section">
              <h2>🚦 Traffic Control</h2>
              <p>Learn how to manage traffic signals and optimize flow patterns.</p>
              <ul>
                <li>Manual signal control</li>
                <li>AI-powered optimization</li>
                <li>Real-time monitoring</li>
                <li>Emergency override</li>
              </ul>
            </div>

            <div class="help-section">
              <h2>📊 Dashboard</h2>
              <p>Understanding your system dashboard and analytics.</p>
              <ul>
                <li>Traffic flow metrics</li>
                <li>System performance</li>
                <li>Historical data</li>
                <li>Alert management</li>
              </ul>
            </div>

            <div class="help-section">
              <h2>👥 User Management</h2>
              <p>Manage users, roles, and system permissions.</p>
              <ul>
                <li>Adding new users</li>
                <li>Role assignments</li>
                <li>Access control</li>
                <li>Activity monitoring</li>
              </ul>
            </div>

            <div class="help-section">
              <h2>⚙️ System Settings</h2>
              <p>Configure system preferences and parameters.</p>
              <ul>
                <li>Signal timing</li>
                <li>AI parameters</li>
                <li>Notification settings</li>
                <li>System maintenance</li>
              </ul>
            </div>
          </div>


        </div>
      </div>
    </div>

    <!-- Record Panel -->
    <ControlRecord :isVisible="isRecordVisible" @close="toggleRecord" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import ControlHeader from '@/views/control/ControlHeader.vue'
import ControlNav from '@/views/control/ControlNav.vue'
import ControlRecord from '@/views/control/ControlRecord.vue'
import { isNavVisible, toggleNav } from '@/utils/navState'

const router = useRouter()
const isRecordVisible = ref(false)
const isEmergencyVisible = ref(false)
const isPriorityVisible = ref(false)

// Header button handlers
const toggleRecord = () => {
  isRecordVisible.value = !isRecordVisible.value
  if (isRecordVisible.value) {
    isEmergencyVisible.value = false
    isPriorityVisible.value = false
  }
}

const toggleEmergency = () => {
  isEmergencyVisible.value = !isEmergencyVisible.value
  if (isEmergencyVisible.value) {
    isRecordVisible.value = false
    isPriorityVisible.value = false
  }
}

const togglePriority = () => {
  isPriorityVisible.value = !isPriorityVisible.value
  if (isPriorityVisible.value) {
    isRecordVisible.value = false
    isEmergencyVisible.value = false
  }
}

const handleModeChange = (isAI: boolean) => {
  console.log('Mode changed to:', isAI ? 'AI Mode' : 'Manual Mode')
}

const handleSignOut = () => {
  localStorage.removeItem('authToken')
  router.push({ name: 'Login' })
}
</script>

<style scoped lang="scss">
.help-page {
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
  color: #FFFFFF;
  background-color: #1E1E2F;
  z-index: 1;
}

.main-area {
  height: calc(100% - 0.64rem);
  display: flex;
  overflow: hidden;
  margin-left: 2.66rem;
}

.help-container {
  flex: 1;
  padding: .2rem;
  color: #FFFFFF;
  background-color: #1E1E2F;
  overflow-y: auto;
}

.page-header {
  margin-bottom: .2rem;
}

.page-header h1 {
  color: #FFFFFF;
  margin-bottom: 0.05rem;
  font-size: .3rem;
}

.page-header p {
  color: #FFFFFF;
  font-size: .15rem;
}

.help-content {
  display: grid;
  grid-template-columns: 1fr .3rem;
  gap: .2rem;
}

.help-sections {
  display: grid;
  gap: .15rem;
}

.help-section {
  color: #FFFFFF;
  background-color: #252A3F;
  padding: .2rem;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.help-section h2 {
  color: #FFFFFF;
  background-color: #252A3F;
  margin-bottom: .1rem;
  font-size: .125rem;
}

.help-section p {
  color: #FFFFFF;
  background-color: #252A3F;
  margin-bottom: .1rem;
  line-height: 1.6;
}

.help-section ul {
  list-style: none;
  padding: 0;
}

.help-section li {
  padding: 0.05rem 0;
  border-bottom: 1px solid #f1f5f9;
  color: #FFFFFF;
}

.help-section li:before {
  content: "✓ ";
  color: #00e3ff;
  font-weight: bold;
  margin-right: 0.05rem;
}

.contact-info {
  position: sticky;
  top: .2rem;
  height: fit-content;
}

.contact-info h2 {
  color: #FFFFFF;
  background-color: #252A3F;
  margin-bottom: .1rem;
}

.contact-card {
  background: white;
  padding: 0.15rem;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.contact-item {
  display: flex;
  flex-direction: column;
  gap: 0.025rem;
  margin-bottom: .1rem;
}

.contact-item:last-child {
  margin-bottom: 0;
}

.contact-item strong {
  color: #2d3748;
  font-size: 0.0875rem;
}

.contact-item span {
  color: #4a5568;
}

@media (max-width: 76.8px) {
  .help-content {
    grid-template-columns: 1fr;
  }

  .contact-info {
    position: static;
  }
}
</style>
