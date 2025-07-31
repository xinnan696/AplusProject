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

    <div class="main-area" :class="{ 'nav-collapsed': !isNavVisible }">
      <div class="user-details-container">
        <!-- Page title -->
        <h1 class="page-title">User Details</h1>

        <!-- User Details Form (Read-only) -->
        <div class="form-container">
          <div class="form-grid">
            <!-- Left Column -->
            <div class="form-column">
              <!-- ID Field -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">ID</span>
                </label>
                <div class="form-input-wrapper">
                  <div class="id-display">
                    <span class="id-prefix">#</span>
                    <span class="id-value">{{ userData.id }}</span>
                  </div>
                </div>
              </div>

              <!-- Account Number Field -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Account Number</span>
                </label>
                <div class="form-input-wrapper">
                  <div class="readonly-display">
                    <span class="readonly-value">{{ userData.username }}</span>
                  </div>
                </div>
              </div>

              <!-- Name Field -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Name</span>
                </label>
                <div class="form-input-wrapper">
                  <div class="readonly-display">
                    <span class="readonly-value">{{ userData.name }}</span>
                  </div>
                </div>
              </div>

              <!-- Department Field -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Department</span>
                </label>
                <div class="form-input-wrapper">
                  <div class="readonly-display">
                    <span class="readonly-value">{{ userData.department || '-' }}</span>
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
                </label>
                <div class="form-input-wrapper">
                  <div class="readonly-display">
                    <span class="readonly-value">{{ userData.email }}</span>
                  </div>
                </div>
              </div>

              <!-- Phone Number Field -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Phone Number</span>
                </label>
                <div class="form-input-wrapper">
                  <div class="readonly-display">
                    <span class="readonly-value">{{ userData.phoneNumber || '-' }}</span>
                  </div>
                </div>
              </div>

              <!-- Role Field -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Role</span>
                </label>
                <div class="form-input-wrapper">
                  <div class="readonly-display">
                    <span class="readonly-value">{{ getRoleDisplay(userData.role) }}</span>
                  </div>
                </div>
              </div>

              <!-- Managed Areas Field - 仅对Traffic Manager显示 -->
              <div class="form-group" v-if="userData.role === 'Traffic Manager' || userData.role === 'ROLE_TRAFFIC_MANAGER'">
                <label class="form-label">
                  <span class="label-text">Managed Areas</span>
                </label>
                <div class="form-input-wrapper">
                  <div class="readonly-display">
                    <span class="readonly-value">{{ getManagedAreasDisplay() }}</span>
                  </div>
                </div>
              </div>

              <!-- Status Field -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Status</span>
                </label>
                <div class="form-input-wrapper">
                  <div class="status-toggle">
                    <label class="cyber-switch">
                      <input type="checkbox" :checked="userData.status === 'active'" disabled>
                      <span class="switch-slider">
                        <span class="switch-core"></span>
                      </span>
                    </label>
                    <span class="status-text" :class="{ 'active': userData.status === 'active' }">
                      {{ userData.status ? userData.status.toUpperCase() : 'INACTIVE' }}
                    </span>
                  </div>
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
            </div>
          </button>
          <button class="action-btn back-btn" @click="goBack">
            <div class="btn-content">
              <span class="btn-text">BACK</span>
            </div>
          </button>
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
  transition: margin-left 0.3s ease, width 0.3s ease;
}

/* 导航栏收起时的样式 */
.main-area.nav-collapsed {
  margin-left: 0;
  width: 100vw;
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
}

.page-title {
  color: #FFFFFF;
  font-size: 0.28rem;
  font-weight: bold;
  margin: 0.16rem 0 0.12rem 0;
  position: relative;
  z-index: 2;
}

.form-container {
  width: 100%;
  max-width: 11rem;
  padding: 0.32rem 0.4rem;
  background: linear-gradient(135deg, rgba(43, 43, 60, 0.4) 0%, rgba(30, 30, 47, 0.6) 100%);
  border-radius: 0.16rem;
  border: 1px solid rgba(0, 180, 216, 0.2);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(10px);
  position: relative;
  overflow: hidden;
  margin-bottom: 0.24rem;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.32rem;
  margin-bottom: 0;
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
}

.form-label {
  position: relative;
  display: block;
  margin-bottom: 0.12rem;

  .label-text {
    color: #E3F2FD;
    font-size: 0.15rem;
    font-weight: 600;
    letter-spacing: 0.5px;
    position: relative;
    z-index: 2;
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
  color: #FFFFFF;
  font-size: 0.16rem;
  font-weight: bold;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.3);
  position: relative;
  overflow: hidden;

  .id-prefix {
    color: #FFFFFF;
    margin-right: 0.04rem;
  }

  .id-value {
    color: #FFFFFF;
  }
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
}

.status-toggle {
  display: flex;
  align-items: center;
  gap: 0.16rem;
  padding: 0.12rem 0;
}

.cyber-switch {
  position: relative;
  display: inline-block;
  width: 0.56rem;
  height: 0.28rem;

  input {
    opacity: 0;
    width: 0;
    height: 0;
  }

  .switch-slider {
    position: absolute;
    cursor: not-allowed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, #6B7280 0%, #9CA3AF 100%);
    transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
    border-radius: 0.28rem;
    box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.3);
    overflow: hidden;

    .switch-core {
      position: absolute;
      height: 0.20rem;
      width: 0.20rem;
      left: 0.04rem;
      bottom: 0.04rem;
      background: linear-gradient(135deg, #FFFFFF 0%, #F0F0F0 100%);
      transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
      border-radius: 50%;
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3);
    }
  }

  input:checked + .switch-slider {
    background: linear-gradient(135deg, #00B4D8 0%, #00E5FF 100%);

    .switch-core {
      transform: translateX(0.28rem);
    }
  }

  input:disabled + .switch-slider {
    opacity: 0.8;
    cursor: not-allowed;
  }
}

.status-text {
  color: #6B7280;
  font-size: 0.15rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.02em;
  transition: all 0.3s ease;

  &.active {
    color: #00E5FF;
  }
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 0.4rem;
  margin-top: 0.32rem;
  width: 100%;
  max-width: 11rem;
}

.action-btn {
  width: 1.4rem;
  height: 0.4rem;
  font-size: 0.14rem;
  font-weight: 700;
  border-radius: 0.2rem;
  border: 1px solid;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  position: relative;
  overflow: hidden;
  text-shadow: 0 0 8px rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: 0.5px;
  text-transform: uppercase;

  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 0;
    height: 0;
    background: rgba(255, 255, 255, 0.2);
    border-radius: 50%;
    transform: translate(-50%, -50%);
    transition: all 0.4s ease;
  }

  &:active::before {
    width: 300%;
    height: 300%;
  }

  .btn-content {
    position: relative;
    z-index: 2;
    display: flex;
    align-items: center;
    gap: 0.08rem;
  }

  &.edit-btn {
    background: linear-gradient(135deg, #00E5FF 0%, #00B4D8 100%);
    color: #FFFFFF;
    border-color: rgba(0, 229, 255, 0.5);

    &:hover {
      background: linear-gradient(135deg, #00FFFF 0%, #00E5FF 100%);
      transform: translateY(-2px) scale(1.02);
      border-color: rgba(0, 229, 255, 0.8);
    }
  }

  &.back-btn {
    background: linear-gradient(135deg, #718096 0%, #4A5568 100%);
    color: #FFFFFF;
    border-color: rgba(113, 128, 150, 0.5);

    &:hover {
      background: linear-gradient(135deg, #A0AEC0 0%, #718096 100%);
      transform: translateY(-2px) scale(1.02);
      border-color: rgba(113, 128, 150, 0.8);
    }
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
