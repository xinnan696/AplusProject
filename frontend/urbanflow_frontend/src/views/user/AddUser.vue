<template>
  <div class="add-user-page">
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
      <div class="add-user-container">
        <!-- Page title with tech effect -->
        <h1 class="page-title">Add New User</h1>
        <div class="title-divider"></div>
        <div class="scan-line"></div>

        <!-- Add User Form -->
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
                    <span class="id-value">{{ nextUserId }}</span>
                    <div class="data-stream"></div>
                  </div>
                </div>
              </div>

              <!-- Account Number Field -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Account Number<span class="required">*</span></span>
                  <div class="label-glow"></div>
                </label>
                <div class="form-input-wrapper">
                  <input
                    type="text"
                    class="form-input"
                    :class="{ 'error': errors.accountNumber }"
                    v-model="formData.accountNumber"
                    placeholder="Enter account number"
                  >
                  <div class="input-border"></div>
                  <div class="input-glow"></div>
                  <div v-if="errors.accountNumber" class="error-message">
                    <span class="error-icon">⚠</span>{{ errors.accountNumber }}
                  </div>
                </div>
              </div>

              <!-- Name Field -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Name<span class="required">*</span></span>
                  <div class="label-glow"></div>
                </label>
                <div class="form-input-wrapper">
                  <input
                    type="text"
                    class="form-input"
                    :class="{ 'error': errors.name }"
                    v-model="formData.name"
                    placeholder="Enter full name"
                  >
                  <div class="input-border"></div>
                  <div class="input-glow"></div>
                  <div v-if="errors.name" class="error-message">
                    <span class="error-icon">⚠</span>{{ errors.name }}
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
                  <input
                    type="text"
                    class="form-input"
                    v-model="formData.department"
                    placeholder="Enter department"
                  >
                  <div class="input-border"></div>
                  <div class="input-glow"></div>
                </div>
              </div>
            </div>

            <!-- Right Column -->
            <div class="form-column">
              <!-- Email Field -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Email<span class="required">*</span></span>
                  <div class="label-glow"></div>
                </label>
                <div class="form-input-wrapper">
                  <input
                    type="email"
                    class="form-input"
                    :class="{ 'error': errors.email }"
                    v-model="formData.email"
                    placeholder="Enter email address"
                  >
                  <div class="input-border"></div>
                  <div class="input-glow"></div>
                  <div v-if="errors.email" class="error-message">
                    <span class="error-icon">⚠</span>{{ errors.email }}
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
                  <input
                    type="tel"
                    class="form-input"
                    v-model="formData.phoneNumber"
                    placeholder="Enter phone number"
                  >
                  <div class="input-border"></div>
                  <div class="input-glow"></div>
                </div>
              </div>

              <!-- Role Field -->
              <div class="form-group">
                <label class="form-label">
                  <span class="label-text">Role<span class="required">*</span></span>
                  <div class="label-glow"></div>
                </label>
                <div class="form-input-wrapper">
                  <select class="form-select" v-model="formData.role" :class="{ 'error': errors.role }" @change="onRoleChange">
                    <option value="">Select Role</option>
                    <option value="Traffic Manager">Traffic Manager</option>
                    <option value="Traffic Planner">Traffic Planner</option>
                  </select>
                  <div class="input-border"></div>
                  <div class="input-glow"></div>
                  <div class="select-arrow">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="6,9 12,15 18,9"></polyline>
                    </svg>
                  </div>
                  <div v-if="errors.role" class="error-message">
                    <span class="error-icon">⚠</span>{{ errors.role }}
                  </div>
                </div>
              </div>

              <!-- Area Management Field - 仅对Traffic Manager显示 -->
              <div class="form-group" v-if="formData.role === 'Traffic Manager'">
                <label class="form-label">
                  <span class="label-text">Managed Areas<span class="required">*</span></span>
                  <div class="label-glow"></div>
                </label>
                <div class="form-input-wrapper">
                  <div class="area-selection">
                    <div class="area-options">
                      <label class="area-checkbox" v-for="area in availableAreas" :key="area">
                        <input 
                          type="checkbox" 
                          :value="area" 
                          v-model="formData.managedAreas"
                          :disabled="!isAreaAvailable(area)"
                        >
                        <span class="checkbox-custom"></span>
                        <span class="area-name">{{ area }} Area</span>
                        <span v-if="!isAreaAvailable(area)" class="area-occupied">(Occupied by {{ getAreaManager(area) }})</span>
                      </label>
                    </div>
                    <div v-if="errors.managedAreas" class="error-message">
                      <span class="error-icon">⚠</span>{{ errors.managedAreas }}
                    </div>
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
                  <div class="status-toggle">
                    <label class="cyber-switch">
                      <input type="checkbox" v-model="formData.enabled">
                      <span class="switch-slider">
                        <span class="switch-core"></span>
                        <span class="switch-rail"></span>
                      </span>
                    </label>
                    <span class="status-text" :class="{ 'active': formData.enabled }">
                      {{ formData.enabled ? 'ACTIVE' : 'INACTIVE' }}
                    </span>
                    <div class="status-indicator" :class="{ 'online': formData.enabled }"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Action Buttons -->
          <div class="form-actions">
            <button class="action-btn create-btn" @click="createUser" :disabled="loading">
              <div class="btn-content">
                <div v-if="loading" class="loading-spinner">
                  <div class="spinner-ring"></div>
                  <div class="spinner-ring"></div>
                  <div class="spinner-ring"></div>
                </div>
                <span class="btn-text">{{ loading ? 'CREATING...' : 'CREATE USER' }}</span>
                <div class="btn-glow"></div>
              </div>
            </button>
            <button class="action-btn cancel-btn" @click="cancelAddUser">
              <div class="btn-content">
                <span class="btn-text">CANCEL</span>
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
import { useRouter } from 'vue-router'
import { createVNode, render } from 'vue'
import ControlHeader from '@/views/control/ControlHeader.vue'
import ControlNav from '@/views/control/ControlNav.vue'
import ControlRecord from '@/views/control/ControlRecord.vue'
import { isNavVisible, toggleNav } from '@/utils/navState'
import BaseToast from '@/components/BaseToast.vue'
import { useUserStore } from '@/stores/userStore'
import { AreaApiService } from '@/services/areaApi'

const router = useRouter()
const userStore = useUserStore()

const nextUserId = ref(userStore.generateNewId())
const loading = ref(false)
const isRecordVisible = ref(false)
const isEmergencyVisible = ref(false)
const isPriorityVisible = ref(false)

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

const formData = ref({
  accountNumber: '',
  name: '',
  department: '',
  email: '',
  phoneNumber: '',
  role: '',
  managedAreas: [], // 新增：管理区域列表
  enabled: true
})

const errors = ref({
  accountNumber: '',
  name: '',
  email: '',
  role: '',
  managedAreas: '' // 新增：区域错误信息
})

// 区域管理相关数据
const availableAreas = ref(['Left', 'Right']) // 只支持两个区域
const occupiedAreas = ref(new Map()) // 已被占用的区域及其管理者
const loadingAreas = ref(false)

const validateForm = () => {
  errors.value = { accountNumber: '', name: '', email: '', role: '', managedAreas: '' }
  let isValid = true

  if (!formData.value.accountNumber.trim()) {
    errors.value.accountNumber = 'Account Number is required'
    isValid = false
  }
  if (!formData.value.name.trim()) {
    errors.value.name = 'Name is required'
    isValid = false
  }
  if (!formData.value.email.trim()) {
    errors.value.email = 'Email is required'
    isValid = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.value.email)) {
    errors.value.email = 'Please enter a valid email address'
    isValid = false
  }
  if (!formData.value.role.trim()) {
    errors.value.role = 'Role is required'
    isValid = false
  }
  
  // 验证Traffic Manager的区域选择
  if (formData.value.role === 'Traffic Manager') {
    if (!formData.value.managedAreas || formData.value.managedAreas.length === 0) {
      errors.value.managedAreas = 'Please select at least one area to manage'
      isValid = false
    }
  }

  return isValid
}

// 区域管理相关方法
const loadAreasInfo = async () => {
  try {
    loadingAreas.value = true
    console.log('Loading areas info...')
    
    // 获取已占用的区域信息
    const response = await AreaApiService.getOccupiedAreas()
    console.log('API response:', response)
    
    // 修复：检查 statusCode 而不是 success
    if (response.statusCode === 200 && response.data) {
      // 清空现有数据
      occupiedAreas.value.clear()
      
      // 填充已占用的区域信息
      response.data.forEach(area => {
        const managerInfo = `${area.userName} (${area.accountNumber})`
        occupiedAreas.value.set(area.areaName, managerInfo)
        console.log(`Area ${area.areaName} occupied by ${managerInfo}`)
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

// 检查区域是否可用
const isAreaAvailable = (area: string): boolean => {
  return !occupiedAreas.value.has(area)
}

// 获取区域管理者
const getAreaManager = (area: string): string => {
  return occupiedAreas.value.get(area) || ''
}

// 组件挂载时加载区域信息
onMounted(() => {
  loadAreasInfo()
})

// 角色变更时的处理
const onRoleChange = () => {
  if (formData.value.role !== 'Traffic Manager') {
    // 如果不是Traffic Manager，清空区域选择
    formData.value.managedAreas = []
    errors.value.managedAreas = ''
  } else {
    // 如果是Traffic Manager，重新加载区域信息
    loadAreasInfo()
  }
}

const createUser = async () => {
  if (!validateForm()) {
    showCenterToast('Please fill in all required fields correctly', 'error')
    return
  }

  try {
    loading.value = true

    const userData = {
      accountNumber: formData.value.accountNumber.trim(),
      userName: formData.value.name.trim(),
      email: formData.value.email.trim(),
      password: 'TempPass123!',
      department: formData.value.department?.trim() || '',
      phoneNumber: formData.value.phoneNumber?.trim() || '',
      role: formData.value.role,
      managedAreas: formData.value.role === 'Traffic Manager' ? formData.value.managedAreas : [],
      enabled: formData.value.enabled
    }

    const newUser = await userStore.addUser(userData)

    if (newUser) {
      showCenterToast('User created successfully!', 'success')
      setTimeout(() => {
        router.push('/user')
      }, 1000)
    }
  } catch (error: any) {
    console.error('Error creating user:', error)
    showCenterToast(error.message || 'Failed to create user', 'error')
  } finally {
    loading.value = false
  }
}

const cancelAddUser = () => {
  router.push('/user')
}

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
.add-user-page {
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

.add-user-container {
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

.required {
  color: #FF6B6B;
  margin-left: 0.04rem;
  font-weight: bold;
  text-shadow: 0 0 6px rgba(255, 107, 107, 0.6);
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

.form-input,
.form-select {
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
  transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
  text-shadow: 0 0 8px rgba(255, 255, 255, 0.3);
  outline: none;
  position: relative;
  z-index: 2;

  &::placeholder {
    color: rgba(179, 229, 252, 0.5);
    font-style: italic;
    transition: all 0.3s ease;
  }

  &:hover {
    border-color: rgba(0, 180, 216, 0.5);
    box-shadow: 0 0 16px rgba(0, 180, 216, 0.3);
    transform: translateY(-1px);

    &::placeholder {
      color: rgba(179, 229, 252, 0.7);
    }

    & + .input-glow {
      opacity: 0.5;
    }
  }

  &:focus {
    border-color: #00E5FF;
    box-shadow: 0 0 20px rgba(0, 229, 255, 0.4), inset 0 2px 4px rgba(0, 229, 255, 0.1);
    background: linear-gradient(135deg, #2B2C3D 0%, #363850 100%);
    transform: translateY(-2px);

    &::placeholder {
      color: rgba(179, 229, 252, 0.8);
    }

    & + .input-border {
      opacity: 1;
      transform: scaleX(1);
    }

    & + .input-border + .input-glow {
      opacity: 1;
    }
  }

  &.error {
    border-color: #FF6B6B;
    box-shadow: 0 0 16px rgba(255, 107, 107, 0.4);

    &:focus {
      border-color: #FF8E8E;
      box-shadow: 0 0 20px rgba(255, 107, 107, 0.5);
    }
  }
}

.form-select {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%2300E5FF' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6,9 12,15 18,9'%3e%3c/polyline%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-position: right 0.12rem center;
  background-size: 0.16rem;
  padding-right: 0.36rem;

  option {
    background-color: #2B2C3D;
    color: #FFFFFF;
    font-weight: 500;
    padding: 0.08rem;
  }
}

.select-arrow {
  position: absolute;
  right: 0.12rem;
  top: 50%;
  transform: translateY(-50%);
  width: 0.16rem;
  height: 0.16rem;
  color: rgba(0, 180, 216, 0.7);
  pointer-events: none;
  z-index: 3;
}

.input-border {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, #00B4D8, #00D4F8, #00B4D8);
  border-radius: 1px;
  opacity: 0;
  transform: scaleX(0);
  transition: all 0.3s ease;
  z-index: 1;
}

.input-glow {
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  background: linear-gradient(135deg, rgba(0, 180, 216, 0.1), rgba(0, 212, 248, 0.1));
  border-radius: 0.1rem;
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: 0;
  filter: blur(4px);
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
    background: linear-gradient(135deg, #FF6B6B 0%, #FF8E8E 100%);
    transition: all 0.4s cubic-bezier(0.4, 0.0, 0.2, 1);
    border-radius: 0.28rem;
    box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.3), 0 0 8px rgba(255, 107, 107, 0.3);
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

    .switch-rail {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(45deg, transparent 30%, rgba(255,255,255,0.05) 50%, transparent 70%);
      transform: translateX(-100%);
      transition: transform 0.6s ease;
    }

    &:hover .switch-rail {
      transform: translateX(100%);
    }
  }

  input:checked + .switch-slider {
    background: linear-gradient(135deg, #00B4D8 0%, #00E5FF 100%);
    box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.3), 0 0 12px rgba(0, 180, 216, 0.5);

    .switch-core {
      transform: translateX(0.28rem) rotate(360deg);
    }
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

@keyframes statusPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.error-message {
  color: #FF6B6B;
  font-size: 0.13rem;
  margin-top: 0.08rem;
  font-weight: 500;
  text-shadow: 0 0 6px rgba(255, 107, 107, 0.6);
  animation: errorPulse 0.3s ease-out;
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

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none !important;

    .btn-glow {
      left: -100% !important;
    }
  }

  &.create-btn {
    background: linear-gradient(135deg, #00B4D8 0%, #00E5FF 100%);
    color: #FFFFFF;
    box-shadow: 0 4px 15px rgba(0, 180, 216, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.2);
    text-shadow: 0 0 8px rgba(255, 255, 255, 0.3);

    &:hover:not(:disabled) {
      background: linear-gradient(135deg, #00D4F8 0%, #40E0FF 100%);
      transform: translateY(-3px);
      box-shadow: 0 8px 25px rgba(0, 180, 216, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.3);
    }

    &:active:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 4px 15px rgba(0, 180, 216, 0.6), inset 0 2px 4px rgba(0, 0, 0, 0.2);
    }
  }

  &.cancel-btn {
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

@keyframes errorPulse {
  0% {
    opacity: 0;
    transform: translateY(-8px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
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
    transform: translateY(-1px);
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
    box-shadow: 0 0 0.08rem rgba(0, 180, 216, 0.5);

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
      transform: none;
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
