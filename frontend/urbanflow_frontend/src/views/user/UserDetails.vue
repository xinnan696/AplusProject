<template>
  <div class="user-details-page">
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
      <div class="user-details-container">
        <!-- Page title with tech effect -->
        <h1 class="page-title">User Details</h1>
        <div class="title-divider"></div>
        <div class="scan-line"></div>

        <!-- User Details Form (Read-only) -->
        <div class="form-container">
          <div class="form-grid">
            <!-- Left Column -->
            <div class="form-column">
              <!-- ID Field -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">ID</span>
                  <div class="label-glow"></div>
                </label>
                <div class="form-input-wrapper">
                  <div class="id-display">
                    <span class="id-prefix">#</span>
                    <span class="id-value">{{ userData.id }}</span>
                    <div class="data-stream"></div>
                  </div>
                </div>
              </div>

              <!-- Account Number Field -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Account Number</span>
                  <div class="label-glow"></div>
                </label>
                <div class="form-input-wrapper">
                  <div class="readonly-display">
                    <span class="readonly-value">{{ userData.username }}</span>
                    <div class="readonly-indicator">READ ONLY</div>
                  </div>
                </div>
              </div>

              <!-- Name Field -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Name</span>
                  <div class="label-glow"></div>
                </label>
                <div class="form-input-wrapper">
                  <div class="readonly-display">
                    <span class="readonly-value">{{ userData.name }}</span>
                    <div class="readonly-indicator">READ ONLY</div>
                  </div>
                </div>
              </div>

              <!-- Department Field -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Department</span>
                  <div class="label-glow"></div>
                </label>
                <div class="form-input-wrapper">
                  <div class="readonly-display">
                    <span class="readonly-value">{{ userData.department || '-' }}</span>
                    <div class="readonly-indicator">READ ONLY</div>
                  </div>
                </div>
              </div>
            </div>

            <!-- Right Column -->
            <div class="form-column">
              <!-- Email Field -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Email</span>
                  <div class="label-glow"></div>
                </label>
                <div class="form-input-wrapper">
                  <div class="readonly-display">
                    <span class="readonly-value">{{ userData.email }}</span>
                    <div class="readonly-indicator">READ ONLY</div>
                  </div>
                </div>
              </div>

              <!-- Phone Number Field -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Phone Number</span>
                  <div class="label-glow"></div>
                </label>
                <div class="form-input-wrapper">
                  <div class="readonly-display">
                    <span class="readonly-value">{{ userData.phoneNumber || '-' }}</span>
                    <div class="readonly-indicator">READ ONLY</div>
                  </div>
                </div>
              </div>

              <!-- Role Field -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Role</span>
                  <div class="label-glow"></div>
                </label>
                <div class="form-input-wrapper">
                  <div class="readonly-display">
                    <span class="readonly-value">{{ getRoleDisplay(userData.role) }}</span>
                    <div class="readonly-indicator">READ ONLY</div>
                  </div>
                </div>
              </div>

              <!-- Managed Areas Field - 仅对Traffic Manager显示 -->
              <div class="form-group" v-if="userData.role === 'Traffic Manager' || userData.role === 'ROLE_TRAFFIC_MANAGER'">
                <label class="form-label">
                  <span class="label-text">Managed Areas</span>
                  <div class="label-glow"></div>
                </label>
                <div class="form-input-wrapper">
                  <div class="readonly-display">
                    <span class="readonly-value">{{ getManagedAreasDisplay() }}</span>
                    <div class="readonly-indicator">READ ONLY</div>
                  </div>
                </div>
              </div>

              <!-- Status Field -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Status</span>
                  <div class="label-glow"></div>
                </label>
                <div class="form-input-wrapper">
                  <div class="status-display">
                    <div class="status-indicator" :class="{ 'online': userData.status === 'active' }"></div>
                    <span class="status-text" :class="{ 'active': userData.status === 'active' }">
                      {{ userData.status ? userData.status.toUpperCase() : 'INACTIVE' }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Action Buttons -->
          <div class="form-actions">
            <button class="action-btn edit-btn" @click="editUser">
              <div class="btn-content">
                <span class="btn-text">EDIT</span>
                <div class="btn-glow"></div>
              </div>
            </button>
            <button class="action-btn back-btn" @click="goBack">
              <div class="btn-content">
                <span class="btn-text">BACK</span>
                <div class="btn-glow"></div>
              </div>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Record Panel -->
    <ControlRecord :isVisible="isRecordVisible" @close="toggleRecord" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { createVNode, render } from 'vue'
import ControlHeader from '@/views/control/ControlHeader.vue'
import ControlNav from '@/views/control/ControlNav.vue'
import ControlRecord from '@/views/control/ControlRecord.vue'
import { isNavVisible, toggleNav } from '@/utils/navState'
import BaseToast from '@/components/BaseToast.vue'
import { useUserStore } from '@/stores/userStore'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// Header button states
const isRecordVisible = ref(false)
const isEmergencyVisible = ref(false)
const isPriorityVisible = ref(false)

// 页面专用的 toast 函数
const showCenterToast = (message: string, type: 'success' | 'error' | 'info' = 'success', duration = 3000) => {
  const container = document.createElement('div')
  document.body.appendChild(container)

  const vnode = createVNode(BaseToast, { message, type, duration })
  render(vnode, container)

  setTimeout(() => {
    const toastElement = container.querySelector('.toast') as HTMLElement
    if (toastElement) {
      toastElement.style.cssText = `
        position: fixed !important;
        top: .82rem !important;
        left: 50% !important;
        transform: translateX(-50%) !important;
        z-index: 9999 !important;
      `
    }
  }, 10)

  setTimeout(() => {
    render(null, container)
    document.body.removeChild(container)
  }, duration + 500)
}

// User data (all readonly)
const userData = ref({
  id: '',
  username: '',
  name: '',
  department: '',
  email: '',
  phoneNumber: '',
  role: '',
  managedAreas: [] as string[],
  status: ''
})

// Methods
const getRoleDisplay = (role: string) => {
  if (role === 'ROLE_ADMIN') return 'Admin'
  if (role === 'ROLE_USER') return 'User'
  if (role === 'ROLE_TRAFFIC_MANAGER' || role === 'Traffic Manager') return 'Traffic Manager'
  if (role === 'Traffic Planner') return 'Traffic Planner'
  return role || 'User'
}

const getManagedAreasDisplay = () => {
  if (!userData.value.managedAreas || userData.value.managedAreas.length === 0) {
    return 'No areas assigned'
  }
  return userData.value.managedAreas.map(area => `${area} Area`).join(', ')
}

const editUser = () => {
  // Navigate to Edit User page
  router.push({
    name: 'EditUser',
    params: { id: userData.value.id }
  })
}

const goBack = () => {
  // Navigate back to user list
  router.push('/user')
}

onMounted(() => {
  // Load user data from route params
  const userId = parseInt(route.params.id as string)
  const user = userStore.getUserById(userId)

  if (user) {
    userData.value = {
      id: user.id.toString(),
      username: user.accountNumber || user.username || '',
      name: user.userName || user.name || '',
      department: user.department || '',
      email: user.email,
      phoneNumber: user.phoneNumber || '',
      role: user.role,
      managedAreas: user.managedAreas || [],
      status: user.status || (user.enabled ? 'active' : 'inactive')
    }

    console.log('Viewing user details:', user)
  } else {
    console.error('User not found')
    showCenterToast('User not found', 'error')
    setTimeout(() => {
      router.push('/user')
    }, 1000)
  }
})

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
.user-details-page {
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
  height: calc(100vh - 0.64rem);
  display: flex;
  overflow: hidden;
  margin-left: 2.4rem;
  width: calc(100vw - 2.4rem);
  box-sizing: border-box;
}

.user-details-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  padding: 0.24rem;
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
  height: 100%;
  max-height: calc(100vh - 0.64rem);

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background:
      radial-gradient(circle at 20% 80%, rgba(0, 180, 216, 0.03) 0%, transparent 50%),
      radial-gradient(circle at 80% 20%, rgba(0, 212, 248, 0.03) 0%, transparent 50%);
    pointer-events: none;
  }
}

.page-title {
  color: #FFFFFF;
  font-size: 0.28rem;
  font-weight: bold;
  margin: 0.16rem 0 0.12rem 0;
  text-shadow: 0 0 10px rgba(255, 255, 255, 0.3);
  position: relative;
  z-index: 2;
}

.title-divider {
  width: 100%;
  max-width: 11rem;
  height: 2px;
  background: linear-gradient(90deg, transparent 0%, #00B4D8 20%, #00E5FF 50%, #00B4D8 80%, transparent 100%);
  margin-bottom: 0.32rem;
  box-shadow: 0 0 8px rgba(0, 180, 216, 0.5);
  position: relative;
  z-index: 2;
}

.scan-line {
  position: absolute;
  top: 0.44rem;
  left: 0;
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 180, 216, 0.8), transparent);
  animation: scan 3s ease-in-out infinite;
  z-index: 1;
}

@keyframes scan {
  0% { transform: translateX(-100%); opacity: 0; }
  50% { opacity: 1; }
  100% { transform: translateX(100%); opacity: 0; }
}

.form-container {
  width: 100%;
  max-width: 11rem;
  padding: 0.32rem 0.4rem;
  background: linear-gradient(135deg, rgba(43, 43, 60, 0.4) 0%, rgba(30, 30, 47, 0.6) 100%);
  border-radius: 0.16rem;
  border: 1px solid rgba(0, 180, 216, 0.2);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  animation: fadeIn 0.8s ease-out;
  position: relative;
  overflow: hidden;
  margin-bottom: 0.24rem;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 2px;
    background: linear-gradient(90deg, transparent 0%, #00B4D8 30%, #00D4F8 50%, #00B4D8 70%, transparent 100%);
    opacity: 0.8;
  }

  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, rgba(0, 180, 216, 0.02) 0%, transparent 50%, rgba(0, 212, 248, 0.02) 100%);
    pointer-events: none;
  }
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.32rem;
  margin-bottom: 0.32rem;
}

.form-column {
  display: flex;
  flex-direction: column;
  gap: 0.24rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  position: relative;
  animation: slideInUp 0.6s ease-out;
  animation-fill-mode: both;

  &:nth-child(1) { animation-delay: 0.1s; }
  &:nth-child(2) { animation-delay: 0.2s; }
  &:nth-child(3) { animation-delay: 0.3s; }
  &:nth-child(4) { animation-delay: 0.4s; }
  &:nth-child(5) { animation-delay: 0.5s; }
}

.form-label {
  position: relative;
  display: block;
  margin-bottom: 0.12rem;

  .label-text {
    color: #E3F2FD;
    font-size: 0.15rem;
    font-weight: 600;
    text-shadow: 0 0 8px rgba(227, 242, 253, 0.4);
    letter-spacing: 0.5px;
    position: relative;
    z-index: 2;
  }

  .label-glow {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 180, 216, 0.1);
    border-radius: 0.04rem;
    opacity: 0;
    transition: opacity 0.3s ease;
    z-index: 1;
  }

  &:hover .label-glow {
    opacity: 1;
  }
}

.form-input-wrapper {
  position: relative;
  width: 100%;
}

.id-display {
  display: flex;
  align-items: center;
  height: 0.48rem;
  padding: 0 0.16rem;
  background: linear-gradient(135deg, #2B2C3D 0%, #32344A 100%);
  border: 2px solid rgba(0, 180, 216, 0.3);
  border-radius: 0.08rem;
  color: #00E5FF;
  font-size: 0.16rem;
  font-weight: bold;
  text-shadow: 0 0 8px rgba(0, 229, 255, 0.5);
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.3);
  position: relative;
  overflow: hidden;

  .id-prefix {
    color: rgba(0, 180, 216, 0.8);
    margin-right: 0.04rem;
  }

  .id-value {
    color: #00E5FF;
  }

  .data-stream {
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(0, 180, 216, 0.3), transparent);
    animation: dataFlow 2s ease-in-out infinite;
  }
}

@keyframes dataFlow {
  0% { left: -100%; }
  100% { left: 100%; }
}

.readonly-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 0.48rem;
  padding: 0 0.16rem;
  background: linear-gradient(135deg, #2B2C3D 0%, #32344A 100%);
  border: 2px solid rgba(105, 105, 105, 0.3);
  border-radius: 0.08rem;
  position: relative;
  overflow: hidden;

  .readonly-value {
    color: rgba(255, 255, 255, 0.7);
    font-size: 0.15rem;
    font-weight: 500;
    flex: 1;
  }

  .readonly-indicator {
    color: rgba(105, 105, 105, 0.8);
    font-size: 0.11rem;
    font-weight: 600;
    letter-spacing: 0.5px;
    padding: 0.02rem 0.06rem;
    background: rgba(105, 105, 105, 0.1);
    border-radius: 0.04rem;
    border: 1px solid rgba(105, 105, 105, 0.2);
  }
}

.status-display {
  display: flex;
  align-items: center;
  gap: 0.16rem;
  padding: 0.12rem 0.16rem;
  background: linear-gradient(135deg, #2B2C3D 0%, #32344A 100%);
  border: 2px solid rgba(105, 105, 105, 0.3);
  border-radius: 0.08rem;
  height: 0.48rem;
}

.status-indicator {
  width: 0.08rem;
  height: 0.08rem;
  border-radius: 50%;
  background: #666;
  transition: all 0.3s ease;
  box-shadow: 0 0 0.04rem rgba(102, 102, 102, 0.5);

  &.online {
    background: #00E5FF;
    box-shadow: 0 0 0.12rem rgba(0, 229, 255, 0.8);
    animation: statusPulse 2s ease-in-out infinite;
  }
}

.status-text {
  color: #FFFFFF;
  font-size: 0.15rem;
  font-weight: 600;
  text-shadow: 0 0 8px rgba(255, 255, 255, 0.3);
  text-transform: uppercase;
  letter-spacing: 0.02em;
  transition: all 0.3s ease;

  &.active {
    color: #00E5FF;
    text-shadow: 0 0 8px rgba(0, 229, 255, 0.5);
  }
}

@keyframes statusPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 0.24rem;
  margin-top: 0.32rem;
  padding-top: 0.24rem;
  border-top: 1px solid rgba(0, 180, 216, 0.2);
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.08rem;
  padding: 0.14rem 0.32rem;
  border: none;
  border-radius: 0.12rem;
  cursor: pointer;
  font-size: 0.16rem;
  font-weight: bold;
  transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  position: relative;
  overflow: hidden;
  letter-spacing: 0.5px;
  text-transform: uppercase;

  .btn-content {
    position: relative;
    z-index: 2;
    display: flex;
    align-items: center;
    gap: 0.08rem;
  }

  .btn-glow {
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
    transition: left 0.6s ease;
    z-index: 1;
  }

  &:hover .btn-glow {
    left: 100%;
  }

  &.edit-btn {
    background: linear-gradient(135deg, #00B4D8 0%, #00E5FF 100%);
    color: #FFFFFF;
    box-shadow: 0 4px 15px rgba(0, 180, 216, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.2);
    text-shadow: 0 0 8px rgba(255, 255, 255, 0.3);

    &:hover {
      background: linear-gradient(135deg, #00D4F8 0%, #40E0FF 100%);
      transform: translateY(-3px);
      box-shadow: 0 8px 25px rgba(0, 180, 216, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.3);
    }

    &:active {
      transform: translateY(-1px);
      box-shadow: 0 4px 15px rgba(0, 180, 216, 0.6), inset 0 2px 4px rgba(0, 0, 0, 0.2);
    }
  }

  &.back-btn {
    background: linear-gradient(135deg, #6B7280 0%, #9CA3AF 100%);
    color: #FFFFFF;
    box-shadow: 0 4px 15px rgba(107, 114, 128, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.1);
    text-shadow: 0 0 8px rgba(255, 255, 255, 0.2);

    &:hover {
      background: linear-gradient(135deg, #8B92A0 0%, #B5BCC9 100%);
      transform: translateY(-3px);
      box-shadow: 0 8px 25px rgba(107, 114, 128, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.2);
    }

    &:active {
      transform: translateY(-1px);
      box-shadow: 0 4px 15px rgba(107, 114, 128, 0.5), inset 0 2px 4px rgba(0, 0, 0, 0.2);
    }
  }
}

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@media (max-width: 1200px) {
  .form-grid {
    grid-template-columns: 1fr;
    gap: 0.24rem;
  }

  .form-container {
    max-width: 8rem;
    padding: 0.24rem 0.32rem;
  }
}
</style>
