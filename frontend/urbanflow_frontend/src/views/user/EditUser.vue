<template>
  <div class="edit-user-page">
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
      <div class="edit-user-container">
        <!-- Page title -->
        <h1 class="page-title">Edit User Profile</h1>

        <!-- Edit User Form -->
        <div class="form-container">
          <div class="form-grid">
            <!-- Left Column -->
            <div class="form-column">
              <!-- ID Field (readonly) -->
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

              <!-- Account Number Field (readonly) -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Account Number</span>
                </label>
                <div class="form-input-wrapper">
                  <div class="readonly-display">
                    <span class="readonly-value">{{ userData.accountNumber || '-' }}</span>
                  </div>
                </div>
              </div>

              <!-- Name Field (editable) -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Name<span class="required">*</span></span>
                </label>
                <div class="form-input-wrapper">
                  <input
                    type="text"
                    class="form-input"
                    :class="{ 'error': errors.userName }"
                    v-model="editData.userName"
                    placeholder="Enter full name"
                    :disabled="userStore.loading"
                  >
                  <div v-if="errors.userName" class="error-message">
                    <span class="error-icon">⚠</span>{{ errors.userName }}
                  </div>
                </div>
              </div>

              <!-- Department Field (editable) -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Department</span>
                </label>
                <div class="form-input-wrapper">
                  <input
                    type="text"
                    class="form-input"
                    v-model="editData.department"
                    placeholder="Enter department"
                    :disabled="userStore.loading"
                  >
                </div>
              </div>
            </div>

            <!-- Right Column -->
            <div class="form-column">
              <!-- Email Field (editable) -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Email<span class="required">*</span></span>
                </label>
                <div class="form-input-wrapper">
                  <input
                    type="email"
                    class="form-input"
                    :class="{ 'error': errors.email }"
                    v-model="editData.email"
                    placeholder="Enter email address"
                    :disabled="userStore.loading"
                  >
                  <div v-if="errors.email" class="error-message">
                    <span class="error-icon">⚠</span>{{ errors.email }}
                  </div>
                </div>
              </div>

              <!-- Phone Number Field (editable) -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Phone Number</span>
                </label>
                <div class="form-input-wrapper">
                  <input
                    type="tel"
                    class="form-input"
                    v-model="editData.phoneNumber"
                    placeholder="Enter phone number"
                    :disabled="userStore.loading"
                  >
                </div>
              </div>

              <!-- Role Field (readonly) -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Role</span>
                </label>
                <div class="form-input-wrapper">
                  <div class="readonly-display">
                    <span class="readonly-value">{{ getRoleDisplay(editData.role) }}</span>
                  </div>
                </div>
              </div>

              <!-- Managed Areas Field - 仅对Traffic Manager显示 -->
              <div class="form-group" v-if="editData.role === 'Traffic Manager' || editData.role === 'ROLE_TRAFFIC_MANAGER'">
                <label class="form-label">
                  <span class="label-text">Managed Areas</span>
                </label>
                <div class="form-input-wrapper">
                  <div class="area-selection">
                    <div class="area-options">
                      <label class="area-checkbox" v-for="area in availableAreas" :key="area">
                        <input 
                          type="checkbox" 
                          :value="area" 
                          v-model="editData.managedAreas"
                          :disabled="!isAreaAvailable(area) && !editData.managedAreas.includes(area)"
                        >
                        <span class="checkbox-custom"></span>
                        <span class="area-name">{{ area }} Area</span>
                        <span v-if="!isAreaAvailable(area) && !editData.managedAreas.includes(area)" class="area-occupied">(Occupied by {{ getAreaManager(area) }})</span>
                      </label>
                    </div>
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
                      <input type="checkbox" v-model="editData.enabled" :disabled="userStore.loading">
                      <span class="switch-slider">
                        <span class="switch-core"></span>
                      </span>
                    </label>
                    <span class="status-text" :class="{ 'active': editData.enabled }">
                      {{ editData.enabled ? 'ACTIVE' : 'INACTIVE' }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="form-actions">
          <button class="action-btn save-btn" @click="saveUser" :disabled="userStore.loading">
            <div class="btn-content">
              <div v-if="userStore.loading" class="loading-spinner">
                <div class="spinner-ring"></div>
                <div class="spinner-ring"></div>
                <div class="spinner-ring"></div>
              </div>
              <span class="btn-text">{{ userStore.loading ? 'SAVING...' : 'SAVE' }}</span>
            </div>
          </button>
          <button class="action-btn cancel-btn" @click="cancelEdit" :disabled="userStore.loading">
            <div class="btn-content">
              <span class="btn-text">CANCEL</span>
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
import { AreaApiService } from '@/services/areaApi'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// Header button states
const isRecordVisible = ref(false)
const isEmergencyVisible = ref(false)
const isPriorityVisible = ref(false)

// Page-specific toast function
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

// User data (readonly fields)
const userData = ref({
  id: '',
  accountNumber: '',
  originalUserName: ''
})

// Editable data
const editData = ref({
  userName: '',
  email: '',
  department: '',
  phoneNumber: '',
  role: '',
  managedAreas: [] as string[], // 新增：管理区域
  enabled: true
})

// 区域管理相关数据
const availableAreas = ref(['Left', 'Right']) // 只支持两个区域
const occupiedAreas = ref(new Map()) // 已被占用的区域及其管理者
const loadingAreas = ref(false)

// Form validation errors
const errors = ref({
  userName: '',
  email: ''
})

// Validate form
const validateForm = () => {
  errors.value = {
    userName: '',
    email: ''
  }

  let isValid = true

  if (!editData.value.userName.trim()) {
    errors.value.userName = 'Name is required'
    isValid = false
  } else if (editData.value.userName.trim().length < 2) {
    errors.value.userName = 'Name must be at least 2 characters'
    isValid = false
  }

  if (!editData.value.email.trim()) {
    errors.value.email = 'Email is required'
    isValid = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(editData.value.email)) {
    errors.value.email = 'Please enter a valid email address'
    isValid = false
  }

  return isValid
}

// Methods
const getRoleDisplay = (role: string) => {
  if (role === 'ADMIN') return 'Admin'
  if (role === 'ROLE_USER') return 'User'
  if (role === 'ROLE_TRAFFIC_MANAGER' || role === 'Traffic Manager') return 'Traffic Manager'
  if (role === 'Traffic Planner') return 'Traffic Planner'
  return role || 'User'
}


const loadAreasInfo = async () => {
  try {
    loadingAreas.value = true
    console.log('Loading areas info...')

    const response = await AreaApiService.getOccupiedAreas()
    console.log('API response:', response)

    if (response.statusCode === 200 && response.data) {
    
      occupiedAreas.value.clear()
  
      response.data.forEach(area => {
        // Only consider areas as occupied if they have a userId AND it's different from current user
        if (area && area.userId !== null && area.userId !== undefined && area.userId !== '') {
          const areaUserId = String(area.userId)
          const currentUserId = String(userData.value.id)
          
          if (areaUserId !== currentUserId) {
            const userName = area.userName || 'Unknown User'
            const accountNumber = area.accountNumber || 'Unknown Account'
            const managerInfo = `${userName} (${accountNumber})`
            const areaName = area.areaName || 'Unknown Area'
            
            occupiedAreas.value.set(areaName, managerInfo)
            console.log(`Area ${areaName} occupied by ${managerInfo}`)
          } else {
            console.log(`Area ${area.areaName} is managed by current user`)
          }
        } else {
          console.log(`Area ${area.areaName || 'Unknown'} is available (no userId)`)
        }
      })
      
      console.log('Final occupiedAreas:', Array.from(occupiedAreas.value.entries()))
    } else {
      console.log('No occupied areas data or unsuccessful response')
    }
  } catch (error) {
    console.error('Failed to load areas info:', error)
    showCenterToast('Failed to load area information', 'error')
  } finally {
    loadingAreas.value = false
  }
}

const isAreaAvailable = (area: string): boolean => {
  return !occupiedAreas.value.has(area)
}

const getAreaManager = (area: string): string => {
  return occupiedAreas.value.get(area) || ''
}

const saveUser = async () => {
  // Validate form
  if (!validateForm()) {
    showCenterToast('Please fill in all required fields correctly', 'error')
    return
  }

  try {
    // Call API to update user
    const updatedUser = await userStore.updateUser(parseInt(userData.value.id), {
      userName: editData.value.userName.trim(),
      email: editData.value.email.trim(),
      department: editData.value.department?.trim() || '',
      phoneNumber: editData.value.phoneNumber?.trim() || '',
      role: editData.value.role,
      managedAreas: editData.value.managedAreas || [], 
      enabled: editData.value.enabled
    })

    if (updatedUser) {
      console.log('User updated successfully:', updatedUser)
      showCenterToast('User information updated successfully!', 'success')
      setTimeout(() => {
        router.push('/user')
      }, 1500)
    }
  } catch (error: any) {
    console.error('Failed to update user:', error)
    showCenterToast(error.message || 'Failed to update user', 'error')
  }
}

const cancelEdit = () => {
  router.push('/user')
}

onMounted(async () => {
  const userId = parseInt(route.params.id as string)

  if (isNaN(userId)) {
    showCenterToast('Invalid user ID', 'error')
    setTimeout(() => {
      router.push('/user')
    }, 1000)
    return
  }

  let user = userStore.getUserById(userId)

  if (!user) {
    try {
      await userStore.fetchUsers()
      user = userStore.getUserById(userId)
    } catch (error) {
      console.error('Failed to fetch user data:', error)
    }
  }

  if (user) {
    userData.value.id = user.id.toString()
    userData.value.accountNumber = user.accountNumber || ''
    userData.value.originalUserName = user.userName

    editData.value.userName = user.userName || user.name || ''
    editData.value.email = user.email
    editData.value.department = user.department || ''
    editData.value.phoneNumber = user.phoneNumber || ''
    editData.value.role = user.role
    editData.value.managedAreas = user.managedAreas || [] 
    editData.value.enabled = user.enabled

    if (user.role === 'Traffic Manager' || user.role === 'ROLE_TRAFFIC_MANAGER') {
      await loadAreasInfo()
    }

    console.log('EditUser - Loaded user data:', {
      department: user.department,
      phoneNumber: user.phoneNumber,
      editDataDepartment: editData.value.department,
      editDataPhoneNumber: editData.value.phoneNumber
    })

    console.log('Editing user:', user)
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
.edit-user-page {
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

.edit-user-container {
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

.required {
  color: #FF6B6B;
  margin-left: 0.04rem;
  font-weight: bold;
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

.form-input {
  width: 100%;
  height: 0.48rem;
  padding: 0 0.16rem;
  background: linear-gradient(135deg, #2B2C3D 0%, #32344A 100%);
  border: 2px solid rgba(0, 180, 216, 0.3);
  border-radius: 0.08rem;
  color: #FFFFFF;
  font-size: 0.15rem;
  font-weight: 500;
  box-sizing: border-box;
  transition: border-color 0.3s ease;
  outline: none;
  position: relative;
  z-index: 2;

  &::placeholder {
    color: rgba(179, 179, 179, 0.6);
  }

  &:hover {
    border-color: rgba(0, 180, 216, 0.5);
  }

  &:focus {
    border-color: rgba(0, 180, 216, 0.5);
  }

  &.error {
    border-color: #FF6B6B;

    &:focus {
      border-color: #FF8E8E;
    }
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    background: linear-gradient(135deg, #1E1E2F 0%, #2B2B3C 100%);
    border-color: rgba(105, 105, 105, 0.3);
    color: rgba(255, 255, 255, 0.5);
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
    cursor: pointer;
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
}

.status-text {
  color: #FFFFFF;
  font-size: 0.15rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.02em;
  transition: all 0.3s ease;

  &.active {
    color: #00E5FF;
  }

  &:not(.active) {
    color: #6B7280;
  }
}

.error-message {
  color: #FF6B6B;
  font-size: 0.13rem;
  margin-top: 0.08rem;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 0.04rem;

  .error-icon {
    font-size: 0.14rem;
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

  &:disabled {
    cursor: not-allowed;
    transform: none;
    box-shadow: none;
    text-shadow: none;
  }

  &.save-btn {
    background: linear-gradient(135deg, #00E5FF 0%, #00B4D8 100%);
    color: #FFFFFF;
    border-color: rgba(0, 229, 255, 0.5);

    &:not(:disabled):hover {
      background: linear-gradient(135deg, #00FFFF 0%, #00E5FF 100%);
      transform: translateY(-2px) scale(1.02);
      border-color: rgba(0, 229, 255, 0.8);
    }

    &:disabled {
      background: linear-gradient(135deg, #4A5568 0%, #2D3748 100%);
      color: #A0AEC0;
      border-color: rgba(74, 85, 104, 0.5);
    }
  }

  &.cancel-btn {
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

.loading-spinner {
  display: flex;
  gap: 0.02rem;

  .spinner-ring {
    width: 0.04rem;
    height: 0.04rem;
    border-radius: 50%;
    background: #FFFFFF;
    animation: spinnerPulse 1.4s ease-in-out infinite both;

    &:nth-child(1) { animation-delay: -0.32s; }
    &:nth-child(2) { animation-delay: -0.16s; }
    &:nth-child(3) { animation-delay: 0s; }
  }
}

@keyframes spinnerPulse {
  0%, 80%, 100% {
    transform: scale(0);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
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

/* 区域选择样式 */
.area-selection {
  width: 100%;
}

.area-options {
  display: flex;
  flex-direction: column;
  gap: 0.12rem;
}

.area-checkbox {
  display: flex;
  align-items: center;
  gap: 0.12rem;
  padding: 0.12rem 0.16rem;
  background: rgba(43, 44, 61, 0.6);
  border: 1px solid rgba(0, 180, 216, 0.2);
  border-radius: 0.08rem;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;

  &:hover {
    background: rgba(43, 44, 61, 0.8);
    border-color: rgba(0, 180, 216, 0.4);
  }

  input[type="checkbox"] {
    display: none;
  }

  .checkbox-custom {
    width: 0.18rem;
    height: 0.18rem;
    border: 2px solid rgba(0, 180, 216, 0.5);
    border-radius: 0.04rem;
    background: transparent;
    position: relative;
    transition: all 0.3s ease;
    flex-shrink: 0;

    &::after {
      content: '✓';
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%) scale(0);
      color: #00D4F8;
      font-size: 0.12rem;
      font-weight: bold;
      transition: transform 0.3s ease;
    }
  }

  input:checked + .checkbox-custom {
    background: linear-gradient(135deg, #00B4D8 0%, #00D4F8 100%);
    border-color: #00D4F8;

    &::after {
      transform: translate(-50%, -50%) scale(1);
      color: #FFFFFF;
    }
  }

  input:disabled + .checkbox-custom {
    opacity: 0.4;
    cursor: not-allowed;
  }

  &:has(input:disabled) {
    opacity: 0.6;
    cursor: not-allowed;
    background: rgba(43, 44, 61, 0.3);

    &:hover {
      background: rgba(43, 44, 61, 0.3);
    }
  }
}

.area-name {
  color: #FFFFFF;
  font-size: 0.14rem;
  font-weight: 500;
  flex: 1;
}

.area-occupied {
  color: rgba(255, 107, 107, 0.8);
  font-size: 0.12rem;
  font-style: italic;
  background: rgba(255, 107, 107, 0.1);
  padding: 0.02rem 0.08rem;
  border-radius: 0.04rem;
  border: 1px solid rgba(255, 107, 107, 0.3);
}
</style>
