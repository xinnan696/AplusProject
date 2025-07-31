<template>
  <div class="control-header">
    <div class="header_left">
      <div
        class="iconfont nav btn-hover-icon"
        @click="emit('toggle-nav')"
      >
        &#xe660;
        <div class="simple-tooltip">Navigation</div>
      </div>
      <div class="logo_box">
        <div class="logo">
          <img src="@\assets\images\LOGO.png" alt="">
        </div>
      </div>
    </div>

    <div class="header_right">
      <div
        v-if="showEmergencyIcon"
        class="emergency-alert-wrapper btn-hover-icon"
        :class="{ 'blinking-icon': hasNewRequests }"
        @click="emit('emergency-icon-clicked')"
      >
        <img src="@/assets/images/emergency.png" alt="Emergency Alert Icon">
<!--        <div class="simple-tooltip">Priority Vehicle Request</div>-->
        <div class="simple-tooltip">{{ tooltipText }}</div>
      </div>

      <div class="switch-wrapper">
        <label class="switch toggle-absolute btn-hover-switch">
          <input
            type="checkbox"
            v-model="isAIMode"
            @change="toggleMode"
          >
          <span class="slider"></span>
        </label>
        <div class="simple-tooltip">{{ isAIMode ? 'AI Mode' : 'Manual Mode' }}</div>
      </div>

      <div
        class="iconfont record btn-hover-icon"
        :class="{ 'btn-active': isRecordPanelVisible }"
        @click="emit('toggle-record')"
      >
        &#xe683;
        <div class="simple-tooltip">Records</div>
      </div>

      <div class="personal btn-hover-circle" @click="togglePanel">
        {{ userDisplayInfo.initial }}
        <div class="simple-tooltip">Profile</div>
      </div>

      <div v-if="showPanel" class="user-panel panel-slide-down">
        <div class="user-info">
          <div class="user-avatar shimmer-effect">{{ userDisplayInfo.initial }}</div>
          <div class="user-name">{{ userDisplayInfo.name }}</div>
          <div class="user-title">{{ userDisplayInfo.title }}</div>
          <div class="account-number">ID: {{ userDisplayInfo.accountNumber }}</div>

          <!-- 用户信息网格 -->
          <div class="user-details-grid">
            <div v-if="userDisplayInfo.department !== 'N/A'" class="detail-item">
              <div class="detail-text">{{ userDisplayInfo.department }}</div>
            </div>
            <div v-if="userDisplayInfo.phoneNumber !== 'N/A'" class="detail-item">
              <div class="detail-text">{{ userDisplayInfo.phoneNumber }}</div>
            </div>
          </div>

          <!-- 显示管理区域（仅对Traffic Manager） -->
          <div v-if="userDisplayInfo.managedAreas && userDisplayInfo.managedAreas.length > 0" class="managed-areas">
            <div class="areas-header">
              <div class="areas-label">Managed Areas:</div>
              <div class="areas-list">
                <span v-for="area in userDisplayInfo.managedAreas" :key="area" class="area-text">
                  {{ area }}
                </span>
              </div>
            </div>
          </div>

          <div class="divider"></div>

          <button class="sign-out-btn btn-hover-solid" @click="handleSignOut">
            Sign Out
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

interface Props {
  isRecordPanelVisible?: boolean
  showEmergencyIcon?: boolean
  hasNewRequests?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isRecordPanelVisible: false,

  showEmergencyIcon: false,
  hasNewRequests: false
})


const emit = defineEmits(['toggle-nav', 'toggle-record', 'emergency-icon-clicked', 'mode-changed', 'sign-out'])

const authStore = useAuthStore()
const showPanel = ref(false)
const isAIMode = ref(false)

// ### 新增 6: 动态的悬浮提示文本 ###
const tooltipText = computed(() => {
  return props.hasNewRequests ? 'Priority Vehicle Request' : 'Priority Vehicle Tracking';
});

const userDisplayInfo = computed(() => {
  const user = authStore.user
  if (!user) {
    return {
      initial: 'G',
      name: 'Guest User',
      title: 'Guest User',
      accountNumber: 'N/A',
      email: 'guest@urbanflow.com',
      department: 'N/A'
    }
  }

  let initial = 'U'
  if (user.userName && user.userName.trim()) {
    initial = user.userName.trim().charAt(0).toUpperCase()
  } else if (user.accountNumber && user.accountNumber.trim()) {
    initial = user.accountNumber.trim().charAt(0).toUpperCase()
  } else if (user.email && user.email.trim()) {
    initial = user.email.trim().charAt(0).toUpperCase()
  }

  const roleTitle = {
    'ADMIN': 'System Administrator',
    'Traffic Manager': 'Traffic Manager',
    'Traffic Planner': 'Traffic Planner',
    'USER': 'User'
  }[user.role] || 'User'


  const displayName = user.userName || user.accountNumber || 'Unknown User'

  return {
    initial,
    name: displayName,
    title: roleTitle,
    accountNumber: user.accountNumber || 'N/A',
    email: user.email || 'N/A',
    department: user.department || 'N/A',
    phoneNumber: user.phoneNumber || 'N/A',
    role: user.role,
    managedAreas: user.managedAreas || []
  }
})

function togglePanel() {
  showPanel.value = !showPanel.value
}

function toggleMode() {
  console.log('Mode switched to:', isAIMode.value ? 'AI Mode' : 'Manual Mode')

  emit('mode-changed', isAIMode.value)

  console.log(`Control board will ${isAIMode.value ? 'hide manual panel and expand AI/Congested panels' : 'show all panels in default layout'}`)
}

function handleSignOut() {
  showPanel.value = false
  emit('sign-out')
}

function handleClickOutside(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (!target.closest('.personal') && !target.closest('.user-panel')) {
    showPanel.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped lang="scss">
.control-header {
  position: relative;
  width: 100%;
  height: 0.64rem;
  background-color: #1E1E2F;
  border-bottom: 0.01rem solid #3A3A4C;
  z-index: 1000;
  display: flex;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg, transparent 0%, #3A3A4C 50%, transparent 100%);
    opacity: 0.6;
  }

  .header_left,
  .header_right {
    position: relative;
    height: 100%;
    overflow: visible;
  }

  .header_left {
    width: 13.59rem;
  }

  .header_right {
    width: 5.61rem;
  }

  .iconfont {
    position: absolute;
    font-family: 'iconfont';
    font-size: 0.25rem;
    width: 0.33rem;
    height: 0.4rem;
    line-height: 0.4rem;
    color: #FFFFFF;
    text-align: center;
    cursor: pointer;
  }

  .nav {
    top: 0.13rem;
    left: 0.26rem;
  }

  .switch {
    position: relative;
    display: inline-block;
    width: 0.5rem;
    height: 0.27rem;
  }

  .switch input {
    opacity: 0;
    width: 0;
    height: 0;
  }

  .slider {
    position: absolute;
    cursor: pointer;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    border-radius: 0.68rem;
    background: #2a2a3a;
    border: 0.02rem solid #444;
    transition: all 0.3s ease;
  }

  .slider::before {
    position: absolute;
    content: "";
    height: 0.25rem;
    width: 0.25rem;
    left: 0.01rem;
    top: 0.01rem;
    border-radius: 50%;
    background: #ffffff;
    transition: all 0.3s ease;
  }

  input:checked + .slider {
    background: #00b4d8;
    border-color: #00d4f8;
  }

  input:checked + .slider::before {
    transform: translateX(0.23rem);
  }

  .switch-wrapper {
    position: absolute;
    top: 0.21rem;
    right: 2.58rem;
    z-index: 999;

    &:hover .simple-tooltip {
      opacity: 1;
      visibility: visible;
      transform: translateX(-50%);
    }

    .simple-tooltip {
      top: calc(100% + 12px);
    }

    /* 强制移除所有发光效果 */
    .slider {
      box-shadow: none !important;
      filter: none !important;
      -webkit-filter: none !important;
    }

    &:hover .slider {
      box-shadow: none !important;
      filter: none !important;
      -webkit-filter: none !important;
      transform: none !important;
    }

    input:checked + .slider {
      box-shadow: none !important;
      filter: none !important;
      -webkit-filter: none !important;
    }

    input:focus + .slider {
      box-shadow: none !important;
      filter: none !important;
      -webkit-filter: none !important;
    }
  }

  .toggle-absolute {
    position: relative;
  }

  .record {
    top: 0.12rem;
    right: 1.45rem;
    font-size: 0.33rem;
  }

  .personal {
    position: absolute;
    width: 0.4rem;
    height: 0.4rem;
    border-radius: 50%;
    top: 0.12rem;
    right: 0.25rem;
    font-size: 0.2rem;
    color: #FFFFFF;
    font-weight: bold;
    display: flex;
    justify-content: center;
    align-items: center;
    cursor: pointer;
    user-select: none;
  }

  .user-panel {
    position: absolute;
    top: 0.64rem;
    right: 0.25rem;
    width: 3.2rem;
    min-height: 2.6rem;
    background: #1a1a2e;
    border: 1px solid rgba(74, 85, 104, 0.2);
    border-radius: 0.16rem;
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
    z-index: 9999;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 2px;
      background: linear-gradient(90deg, transparent 0%, #3A3A4C 30%, #4A5568 50%, #3A3A4C 70%, transparent 100%);
      opacity: 0.8;
    }

    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(135deg, rgba(74, 85, 104, 0.03) 0%, transparent 50%, rgba(113, 128, 150, 0.03) 100%);
      pointer-events: none;
    }
  }

  .user-info {
    padding: 0.24rem;
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    position: relative;
  }

  .user-avatar {
    width: 0.6rem;
    height: 0.6rem;
    background: #4A5568;
    border-radius: 50%;
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 0.28rem;
    font-weight: bold;
    color: #FFFFFF;
    margin-bottom: 0.2rem;
    position: relative;
  }

  .user-name {
    font-size: 0.18rem;
    font-weight: 600;
    color: #FFFFFF;
    margin-bottom: 0.08rem;
  
    letter-spacing: 0.02em;
  }

  .user-title {
    font-size: 0.13rem;
    font-weight: 400;
    color: rgba(160, 174, 192, 0.8);
    margin-bottom: 0.08rem;
    text-transform: uppercase;
    letter-spacing: 0.03em;
  }

  .user-details-grid {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 0.06rem;
    margin-bottom: 0.12rem;
  }

  .detail-item {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0.04rem 0.08rem;
    background: rgba(255, 255, 255, 0.03);
    border-radius: 0.06rem;
    border: 1px solid rgba(255, 255, 255, 0.08);
    transition: all 0.2s ease;

    &:hover {
      border-color: rgba(255, 255, 255, 0.1);
    }
  }

  .detail-text {
    font-size: 0.11rem;
    color: rgba(255, 255, 255, 0.8);
    font-weight: 500;
    letter-spacing: 0.01em;
  }

  .managed-areas {
    width: 100%;
    margin-bottom: 0.16rem;

    &:empty {
      display: none;
    }
  }

  .areas-header {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-wrap: wrap;
    gap: 0.06rem;
    padding: 0.04rem 0.08rem;
    background: linear-gradient(135deg, rgba(0, 180, 216, 0.05) 0%, rgba(0, 212, 248, 0.03) 100%);
    border-radius: 0.08rem;
    border: 1px solid rgba(74, 85, 104, 0.15);
    transition: all 0.2s ease;

    &:hover {
      border-color: rgba(74, 85, 104, 0.25);
    }
  }

  .areas-label {
    font-size: 0.1rem;
    color: rgba(160, 174, 192, 0.9);
    font-weight: 500;
    white-space: nowrap;
  }

  .areas-list {
    display: flex;
    flex-wrap: wrap;
    gap: 0.03rem;
  }

  .area-text {
    font-size: 0.1rem;
    color: rgba(255, 255, 255, 0.85);
    font-weight: 500;
    letter-spacing: 0.01em;
  }

  .account-number {
    font-size: 0.11rem;
    color: rgba(245, 154, 35, 0.9);
    margin-bottom: 0.12rem;
    padding: 0.03rem 0.08rem;
    background: rgba(245, 154, 35, 0.1);
    border-radius: 0.06rem;
    border: 1px solid rgba(245, 154, 35, 0.2);
    font-family: 'Monaco', 'Consolas', monospace;
    letter-spacing: 0.02em;
    transition: all 0.2s ease;

    &:hover {
      border-color: rgba(245, 154, 35, 0.3);
    }
  }

  .divider {
    width: 85%;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(74, 85, 104, 0.3), transparent);
    margin-bottom: 0.2rem;
    position: relative;

    &::after {
      content: '';
      position: absolute;
      top: 1px;
      left: 0;
      right: 0;
      height: 1px;
      background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.05), transparent);
    }
  }

  .sign-out-btn {
    width: 100%;
    height: 0.32rem;
    border: none;
    border-radius: 0.08rem;
    color: #FFFFFF;
    font-size: 0.12rem;
    font-weight: 600;
    cursor: pointer;
    text-transform: uppercase;
    letter-spacing: 0.02em;
  }

  .logo_box {
    position: absolute;
    top: 0.13rem;
    left: 0.8rem;
    height: 0.4rem;
    display: flex;
    align-items: center;
  }

  .logo {
    position: relative;

    img {
      height: 0.9rem;
      width: auto;
      display: block;
      transition: all 0.3s ease;
    }

    &:hover img {
      transform: scale(1.05);
    }
  }

  /* ### 新增样式 ### */
  .emergency-alert-wrapper {
    position: absolute;
    top: 0.18rem; /* 微调垂直位置 */
    right: 3.8rem;
    width: 0.33rem;
    height: 0.33rem;
    display: flex;
    justify-content: center;
    align-items: center;
    cursor: pointer;

    img {
      height: 0.42rem;
      width: auto;
    }
  }

  @keyframes blink {
    50% { opacity: 0.6; }
  }
  .blinking-icon {
    animation: blink 1.2s linear infinite;
  }

  /* 强制移除所有按钮的发光效果和背景色变化 */
  .btn-hover-icon,
  .nav,
  .record,
  .emergency-alert-wrapper {
    box-shadow: none !important;
    filter: none !important;
    -webkit-filter: none !important;

    &:hover {
      background: transparent !important;
      box-shadow: none !important;
      filter: none !important;
      -webkit-filter: none !important;
      transform: translateY(-1px) scale(1.02) !important;
    }

    &:active {
      transform: translateY(0) scale(0.98) !important;
    }
  }

  .btn-hover-circle,
  .personal {
    box-shadow: none !important;
    filter: none !important;
    -webkit-filter: none !important;

    &:hover {
      background: #00B4D8 !important;
      box-shadow: none !important;
      filter: none !important;
      -webkit-filter: none !important;
      transform: translateY(-1px) scale(1.02) !important;
    }

    &:active {
      transform: translateY(0) scale(0.98) !important;
    }
  }

  .btn-hover-solid,
  .sign-out-btn {
    box-shadow: none !important;
    filter: none !important;
    -webkit-filter: none !important;

    &:hover {
      background: #00B4D8 !important;
      box-shadow: none !important;
      filter: none !important;
      -webkit-filter: none !important;
      transform: translateY(-1px) !important;
    }

    &:active {
      transform: translateY(0) !important;
    }
  }

  .btn-active {
    background: transparent !important;
    box-shadow: none !important;
    filter: none !important;
    -webkit-filter: none !important;
    transform: translateY(-1px) scale(1.02) !important;
  }
}
</style>
