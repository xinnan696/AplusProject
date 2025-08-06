<template>
  <div class="user-list-page">
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
      <div class="users-container">
        <!-- Page title -->
        <h1 class="page-title">Users</h1>

        <!-- Controls bar -->
        <div class="controls-bar">
          <button class="add-btn" @click="showAddUserModal">
            <span class="add-icon">+</span>
            Add User
          </button>

          <select class="status-filter" v-model="statusFilter">
            <option value="">Status</option>
            <option value="active">Active</option>
            <option value="inactive">Inactive</option>
          </select>

          <div class="search-group">
            <input
              type="text"
              class="search-input"
              placeholder="Search users..."
              v-model="searchTerm"
            >
            <button class="search-btn iconfont">&#xeafe;</button>
          </div>

          <!-- Delete Selected Button -->
          <button
            class="delete-selected-btn"
            @click="confirmDeleteSelected"
          >
            Delete Selected ({{ selectedUsers.length }})
          </button>
        </div>

        <!-- Users table -->
        <div class="users-table-container">
          <!-- Table header -->
          <div class="table-header" :class="{ 'nav-collapsed': !isNavVisible }">
            <div class="header-checkbox" style="position: absolute; left: 0.26rem; display: flex; align-items: center; height: 40px;">
              <label class="custom-checkbox">
                <input type="checkbox" :checked="isAllSelected" @change="toggleSelectAll">
                <span class="checkmark"></span>
              </label>
            </div>
            <div class="header-id" style="position: absolute; display: flex; align-items: center; height: 40px; font-weight: bold;">ID</div>
            <div class="header-username" style="position: absolute; display: flex; align-items: center; height: 40px; font-weight: bold;">Account Number</div>
            <div class="header-name" style="position: absolute; display: flex; align-items: center; height: 40px; font-weight: bold;">Name</div>
            <div class="header-status" style="position: absolute; display: flex; align-items: center; height: 40px; font-weight: bold;">Status</div>
            <div class="header-role" style="position: absolute; display: flex; align-items: center; height: 40px; font-weight: bold;">Role</div>
            <div class="header-actions" style="position: absolute; display: flex; align-items: center; height: 40px; font-weight: bold;">Actions</div>
          </div>

          <div class="table-divider"></div>

          <div class="table-body">
            <div v-for="user in paginatedUsers" :key="user.id" class="user-row" :class="{ 'nav-collapsed': !isNavVisible }">
              <!-- Checkbox -->
              <div class="cell-checkbox" style="position: absolute; left: 0.26rem; display: flex; align-items: center; height: 44.5px;">
                <label class="custom-checkbox">
                  <input
                    type="checkbox"
                    :checked="selectedUsers.includes(user.id)"
                    @change="toggleUserSelection(user.id)"
                  >
                  <span class="checkmark"></span>
                </label>
              </div>

              <!-- ID -->
              <div class="cell-id" style="position: absolute; display: flex; align-items: center; height: 44.5px;">{{ user.id }}</div>

              <!-- Username -->
              <div class="cell-username" style="position: absolute; display: flex; align-items: center; height: 44.5px;">{{ user.accountNumber || user.username || 'N/A' }}</div>

              <!-- Name -->
              <div class="cell-name" style="position: absolute; display: flex; align-items: center; height: 44.5px;">{{ user.userName || user.name || 'N/A' }}</div>

              <!-- Status Toggle -->
              <div class="cell-status" style="position: absolute; display: flex; align-items: center; height: 44.5px;">
                <div class="status-toggle">
                  <label class="switch">
                    <input
                      type="checkbox"
                      :checked="user.enabled"
                      @change="toggleUserStatus(user)"
                    >
                    <span class="slider"></span>
                  </label>
                </div>
              </div>

              <!-- Role -->
              <div class="cell-role" style="position: absolute; display: flex; align-items: center; height: 44.5px;">{{ user.role || 'N/A' }}</div>

              <!-- Actions -->
              <div class="cell-actions" style="position: absolute; display: flex; align-items: center; height: 44.5px; gap: 0.08rem;">
                <button class="action-btn details-btn" @click="viewUserDetails(user)">
                  Details
                </button>
                <button class="action-btn edit-btn" @click="editUser(user)">
                  Edit
                </button>
                <button class="action-btn delete-btn" @click="confirmDeleteUser(user)">
                  Delete
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div class="pagination">
          <span class="page-info">{{ startItem }}-{{ endItem }} of {{ totalItems }}</span>
          <div class="page-controls">
            <button class="page-btn previous-btn" :disabled="currentPage === 1" @click="previousPage">Previous</button>
            <span v-if="showStartEllipsis" class="page-dots">...</span>
            <button
              v-for="page in visiblePages"
              :key="page"
              class="page-btn"
              :class="{ current: page === currentPage }"
              @click="goToPage(page)"
            >
              {{ page }}
            </button>
            <span v-if="showEndEllipsis" class="page-dots">...</span>
            <button class="page-btn next-btn" :disabled="currentPage === totalPages" @click="nextPage">Next</button>
          </div>
        </div>

      </div>
    </div>

    <!-- Record Panel -->
    <ControlRecord :isVisible="isRecordVisible" @close="toggleRecord" />

    <!-- Delete confirmation modal -->
    <div v-if="showDeleteModal" class="modal-overlay" @click="closeDeleteModal">
      <div class="modal-content" @click.stop>
        <p class="modal-text">
          {{ userToDelete?.isMultiple
            ? `Are you sure to delete ${userToDelete.users.length} selected users?`
            : `Are you sure to delete ${userToDelete?.user?.accountNumber || userToDelete?.user?.username}?`
          }}
        </p>
        <div class="modal-actions">
          <button class="modal-btn delete-confirm-btn" @click="deleteUser">
            DELETE
          </button>
          <button class="modal-btn cancel-btn" @click="closeDeleteModal">
            CANCEL
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { createVNode, render } from 'vue'
import ControlHeader from '@/views/control/ControlHeader.vue'
import ControlNav from '@/views/control/ControlNav.vue'
import ControlRecord from '@/views/control/ControlRecord.vue'
import { isNavVisible, toggleNav } from '@/utils/navState'
import BaseToast from '@/components/BaseToast.vue'
import { useUserStore } from '@/stores/userStore'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const userStore = useUserStore()

// Toast function
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

// Reactive data
const searchTerm = ref('')
const statusFilter = ref('')
const selectedUsers = ref<number[]>([])
const currentPage = ref(1)
const itemsPerPage = 10
const showDeleteModal = ref(false)
const userToDelete = ref<any>(null)
const isRecordVisible = ref(false)
const isEmergencyVisible = ref(false)
const isPriorityVisible = ref(false)

// Store data
const users = computed(() => userStore.users)

// Computed properties
const filteredUsers = computed(() => {
  let filtered = users.value

  // 🔥 隐藏admin用户：过滤掉所有admin相关的用户
  filtered = filtered.filter(user => {
    const accountNumber = user.accountNumber?.toLowerCase() || ''
    const username = user.username?.toLowerCase() || ''
    const userName = user.userName?.toLowerCase() || ''
    const role = user.role?.toLowerCase() || ''

    // 排除包含admin的账号、用户名或角色为admin的用户
    return !(
      accountNumber.includes('admin') ||
      username.includes('admin') ||
      userName.includes('admin') ||
      role === 'admin' ||
      role === 'ADMIN'
    )
  })

  if (searchTerm.value) {
    filtered = filtered.filter(user =>
      (user.accountNumber && user.accountNumber.toLowerCase().includes(searchTerm.value.toLowerCase())) ||
      (user.username && user.username.toLowerCase().includes(searchTerm.value.toLowerCase())) ||
      (user.userName && user.userName.toLowerCase().includes(searchTerm.value.toLowerCase())) ||
      (user.name && user.name.toLowerCase().includes(searchTerm.value.toLowerCase())) ||
      (user.email && user.email.toLowerCase().includes(searchTerm.value.toLowerCase()))
    )
  }

  if (statusFilter.value) {
    filtered = filtered.filter(user => {
      if (statusFilter.value === 'active') {
        return user.enabled
      } else if (statusFilter.value === 'inactive') {
        return !user.enabled
      }
      return true
    })
  }

  return filtered
})

const totalItems = computed(() => filteredUsers.value.length)
const totalPages = computed(() => Math.ceil(totalItems.value / itemsPerPage))

const paginatedUsers = computed(() => {
  const start = (currentPage.value - 1) * itemsPerPage
  const end = start + itemsPerPage
  return filteredUsers.value.slice(start, end)
})

const startItem = computed(() => {
  if (totalItems.value === 0) return 0
  return (currentPage.value - 1) * itemsPerPage + 1
})

const endItem = computed(() => {
  return Math.min(currentPage.value * itemsPerPage, totalItems.value)
})

const isAllSelected = computed(() => {
  return paginatedUsers.value.length > 0 &&
         paginatedUsers.value.every(user => selectedUsers.value.includes(user.id))
})

const visiblePages = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = currentPage.value

  if (total <= 7) {
    for (let i = 1; i <= total; i++) {
      pages.push(i)
    }
  } else {
    if (current <= 4) {
      for (let i = 1; i <= 5; i++) {
        pages.push(i)
      }
    } else if (current >= total - 3) {
      for (let i = total - 4; i <= total; i++) {
        pages.push(i)
      }
    } else {
      for (let i = current - 2; i <= current + 2; i++) {
        pages.push(i)
      }
    }
  }

  return pages
})

const showStartEllipsis = computed(() => {
  return totalPages.value > 7 && currentPage.value > 4
})

const showEndEllipsis = computed(() => {
  return totalPages.value > 7 && currentPage.value < totalPages.value - 3
})

// Methods
const toggleSelectAll = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.checked) {
    const currentPageUserIds = paginatedUsers.value.map(user => user.id)
    const newSelected = [...new Set([...selectedUsers.value, ...currentPageUserIds])]
    selectedUsers.value = newSelected
  } else {
    const currentPageUserIds = paginatedUsers.value.map(user => user.id)
    selectedUsers.value = selectedUsers.value.filter(id => !currentPageUserIds.includes(id))
  }
}

const toggleUserSelection = (userId: number) => {
  const index = selectedUsers.value.indexOf(userId)
  if (index > -1) {
    selectedUsers.value.splice(index, 1)
  } else {
    selectedUsers.value.push(userId)
  }
}

const toggleUserStatus = async (user: any) => {
  console.log('Toggling status for user:', user.id, 'current enabled:', user.enabled)

  try {
    const updatedUser = await userStore.toggleUserStatus(user.id)
    console.log('Status toggled successfully, new enabled:', updatedUser?.enabled)

    if (updatedUser) {
      user.enabled = updatedUser.enabled
      user.status = updatedUser.status
      console.log('Local user state updated:', { id: user.id, enabled: user.enabled, status: user.status })
    }
  } catch (error) {
    console.error('Failed to toggle user status:', error)
    showCenterToast('Failed to toggle user status', 'error')
  }
}

const showAddUserModal = () => {
  router.push('/user/add')
}

const viewUserDetails = (user: any) => {
  router.push({
    name: 'UserDetails',
    params: { id: user.id }
  })
}

const editUser = (user: any) => {
  router.push({
    name: 'EditUser',
    params: { id: user.id }
  })
}

const confirmDeleteUser = (user: any) => {
  userToDelete.value = { isMultiple: false, user: user }
  showDeleteModal.value = true
}

const confirmDeleteSelected = () => {
  if (selectedUsers.value.length > 0) {
    userToDelete.value = { isMultiple: true, users: selectedUsers.value }
    showDeleteModal.value = true
  }
}

const deleteUser = async () => {
  if (userToDelete.value) {
    try {
      if (userToDelete.value.isMultiple) {
        let deletedCount = 0
        for (const userId of userToDelete.value.users) {
          try {
            await userStore.deleteUser(userId)
            deletedCount++
          } catch (error) {
            console.error(`Failed to delete user ${userId}:`, error)
          }
        }

        if (deletedCount > 0) {
          selectedUsers.value = []
          showCenterToast(`${deletedCount} users deleted successfully!`, 'success')
        }
      } else {
        await userStore.deleteUser(userToDelete.value.user.id)
        const index = selectedUsers.value.indexOf(userToDelete.value.user.id)
        if (index > -1) {
          selectedUsers.value.splice(index, 1)
        }
        showCenterToast('User deleted successfully!', 'success')
      }
    } catch (error) {
      console.error('Failed to delete user(s):', error)
      showCenterToast('Failed to delete user(s)', 'error')
    }
  }
  closeDeleteModal()
}

const closeDeleteModal = () => {
  showDeleteModal.value = false
  userToDelete.value = null
}

const goToPage = (page: number) => {
  currentPage.value = page
}

const previousPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}

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

onMounted(async () => {
  console.log('UserList mounted - timestamp:', Date.now())

  const authStore = useAuthStore()
  console.log('Auth check:', {
    isAuthenticated: authStore.isAuthenticated,
    user: authStore.user,
    userRole: authStore.userRole,
    token: authStore.token ? 'Token exists' : 'No token'
  })

  if (!authStore.isAuthenticated) {
    console.error('User not authenticated, redirecting to login')
    await router.push('/login')
    return
  }

  if (!authStore.isAdmin()) {
    console.error('User does not have admin privileges, redirecting to control')
    console.log('Current user role:', authStore.userRole)
    await router.push('/control')
    return
  }

  try {
    console.log('Starting to fetch users...')
    await userStore.fetchUsers()
    console.log('Users fetched successfully, count:', userStore.users.length)
  } catch (error: any) {
    console.error('Failed to load users:', error)


  }
})
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
  margin-left: 2.4rem;
  width: calc(100vw - 2.4rem);
  transition: all 0.3s ease;

  &.nav-collapsed {
    margin-left: 0.24rem;
    width: calc(100vw - 0.24rem);
  }
}

.users-container {
  flex: 1;
  padding: 0.24rem 0.48rem 0.24rem 0.24rem;
  background-color: #1E1E2F;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
  width: 100%;
  box-sizing: border-box;
}

.page-title {
  color: #FFFFFF;
  font-size: 0.28rem;
  font-weight: bold;
  margin: 0rem 0 0.5rem 0.02rem;
  flex-shrink: 0;
}

.controls-bar {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-bottom: 0.58rem;
  margin-left: 0.02rem;
  flex-shrink: 0;
}

.add-btn {
  width: 1.4rem;
  height: 0.4rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.08rem;
  background-color: #2B2C3D;
  color: #00B4D8;
  border: none;
  border-radius: 0.08rem;
  cursor: pointer;
  font-size: 0.14rem;
  font-weight: bold;
  transition: all 0.3s ease;

  &:hover {
    background-color: #2B2C3D;
  }

  &:active {
    background-color: #2B2C3D;
  }

  .add-icon {
    font-size: 0.16rem;
    font-weight: bold;
  }
}

.delete-selected-btn {
  height: 0.4rem;
  padding: 0 0.16rem;
  background-color: #00B4D8;
  color: #FFFFFF;
  border: none;
  border-radius: 0.08rem;
  cursor: pointer;
  font-size: 0.14rem;
  font-weight: bold;
  transition: all 0.3s ease;

  &:hover {
    background-color: #00B4D8;
  }

  &:active {
    background-color: #00B4D8;
  }
}

.status-filter {
  padding: 0;
  background-color: #2B2B3C;
  border: 2px solid #2B2B3C;
  border-radius: 0.08rem;
  color: #FFFFFF;
  font-size: 0.14rem;
  font-weight: bold;
  cursor: pointer;
  width: 1.4rem;
  height: 0.4rem;
  box-sizing: border-box;
  text-align: center;
  text-align-last: center;
  transition: all 0.3s ease;
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23FFFFFF' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6,9 12,15 18,9'%3e%3c/polyline%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-position: right 0.08rem center;
  background-size: 0.14rem;
  padding-left: 0.08rem;
  padding-right: 0.26rem;

  &:hover {
    background-color: #3A3A4D;
    border-color: #3A3A4D;
  }

  &:focus {
    outline: none;
    border-color: #3A3A4D;
    background-color: #3A3A4D;
  }

  &:active {
    background-color: #3A3A4D;
  }

  option {
    background-color: #2B2B3C !important;
    color: #FFFFFF !important;
    padding-top: 0.08rem;
    padding-bottom: 0.08rem;
    padding-left: 0.2rem !important;
    padding-right: 0.08rem;
    border: none;
    font-weight: normal;
    text-align: left !important;

    &:hover {
      background-color: #1E1E2F !important;
      color: #FFFFFF !important;
    }

    &:focus {
      background-color: #1E1E2F !important;
      color: #FFFFFF !important;
    }

    &:checked {
      background-color: #00B4D8 !important;
      color: #FFFFFF !important;
    }

    &:selected {
      background-color: #00B4D8 !important;
      color: #FFFFFF !important;
    }
  }
}

.search-group {
  display: flex;
  align-items: center;
  gap: 0.2rem;
}

.search-input {
  padding: 0.08rem 0.12rem;
  background-color: #2B2B3C;
  border: 1px solid #2B2B3C;
  border-radius: 0.2rem;
  color: #FFFFFF;
  font-size: 0.14rem;
  width: 2.36rem;
  height: 0.4rem;
  box-sizing: border-box;
  transition: all 0.3s ease;

  &::placeholder {
    color: #666;
    transition: color 0.3s ease;
  }

  &:hover {
    background-color: #3A3A4D;
    border-color: #3A3A4D;

    &::placeholder {
      color: #888;
    }
  }

  &:focus {
    outline: none;
    border-color: #3A3A4D;
    background-color: #3A3A4D;

    &::placeholder {
      color: #999;
    }
  }

  &:active {
    background-color: #3A3A4D;
  }
}

.search-btn {
  background: none;
  border: none;
  color: #FFFFFF;
  cursor: pointer;
  font-size: 0.4rem;
  width: 0.4rem;
  height: 0.4rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'iconfont';
  transition: all 0.3s ease;
  border-radius: 0.04rem;
  padding: 0.04rem;

  &:hover {
    color: #00B4D8;
  }

  &:active {
    color: #00B4D8;
  }
}

.users-table-container {
  flex: 1;
  min-height: 0;
  margin-bottom: 0.16rem;
  margin-right: 0;
  position: relative;
  background: #1E1E2F;
  border-radius: 0.12rem;
  overflow: hidden;
}

.table-header {
  position: relative;
  height: 40px;
  color: #FFFFFF;
  font-size: 0.14rem;
  font-weight: 600;

  .header-id,
  .header-username,
  .header-name,
  .header-status,
  .header-role,
  .header-actions {
    transition: all 0.3s ease;
  }

  .header-id { left: 1.14rem; }
  .header-username { left: 3.2rem; }
  .header-name { left: 6.0rem; }
  .header-status { left: 8.8rem; }
  .header-role { left: 10.71rem; }
  .header-actions { left: 14.2rem; }

  &.nav-collapsed {
    .header-id { left: 1.4rem; }
    .header-username { left: 3.7rem; }
    .header-name { left: 6.8rem; }
    .header-status { left: 9.5rem; }
    .header-role { left: 11.8rem; }
    .header-actions { left: calc(100% - 2.6rem); }
  }
}

.table-divider {
  height: 0.02rem;
  background-color: #FFFFFF;
}

.table-body {
  flex: 1;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
  padding-top: 0.2rem;
  padding-right: 0.5rem;
}

.user-row {
  position: relative;
  height: 44.5px;
  color: #FFFFFF;
  font-size: 0.14rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  transition: all 0.3s ease;

  &:not(:first-child) {
    margin-top: 0.18rem;
  }

  &:last-child {
    border-bottom: none;
  }
}

.custom-checkbox {
  position: relative;
  display: inline-block;
  width: 0.18rem;
  height: 0.18rem;

  input[type="checkbox"] {
    opacity: 0;
    position: absolute;
    cursor: pointer;
    width: 100%;
    height: 100%;
    margin: 0;
  }

  .checkmark {
    position: absolute;
    top: 0;
    left: 0;
    width: 0.18rem;
    height: 0.18rem;
    background-color: #2B2B3C;
    border: 2px solid #00B4D8;
    border-radius: 0.04rem;
    transition: all 0.3s ease;
    display: flex;
    align-items: center;
    justify-content: center;

    &::after {
      content: '✓';
      color: #FFFFFF;
      font-size: 0.1rem;
      font-weight: bold;
      opacity: 0;
      transform: scale(0);
      transition: all 0.3s ease;
    }
  }

  input:checked ~ .checkmark {
    background-color: #00B4D8;
    border-color: #00B4D8;

    &::after {
      opacity: 1;
      transform: scale(1);
    }
  }

  input:hover ~ .checkmark {
    border-color: #00B4D8;
    background-color: #2B2B3C;
  }

  input:checked:hover ~ .checkmark {
    background-color: #00B4D8;
    border-color: #00B4D8;
  }

  input:focus ~ .checkmark {
    border-color: #00B4D8;
  }
}

.status-toggle {
  .switch {
    position: relative;
    display: inline-block;
    width: 0.48rem;
    height: 0.24rem;
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
    background-color: #ccc;
    transition: 0.3s;
    border-radius: 0.24rem;

    &::before {
      position: absolute;
      content: "";
      height: 0.18rem;
      width: 0.18rem;
      left: 0.03rem;
      bottom: 0.03rem;
      background-color: white;
      transition: 0.3s;
      border-radius: 50%;
    }
  }

  input:checked + .slider {
    background-color: #00B4D8;
  }

  input:checked + .slider:before {
    transform: translateX(0.24rem);
  }

  input:disabled + .slider {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.cell-actions {
  display: flex;
  gap: 0.08rem;
  align-items: center;
  position: relative;
}

.action-btn {
  padding: 0.06rem 0.33rem;
  border: none;
  border-radius: 0.04rem;
  cursor: pointer;
  font-size: 0.14rem;
  font-weight: bold;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  vertical-align: middle;
  line-height: 1;
  height: 0.28rem;
  box-sizing: border-box;

  &.details-btn {
    background-color: transparent;
    color: #00B4D8;

    &:hover {
      color: #00B4D8;
    }

    &:active {
      color: #00B4D8;
    }
  }

  &.edit-btn {
    background-color: transparent;
    color: #00B4D8;

    &:hover {
      color: #00B4D8;
    }

    &:active {
      color: #00B4D8;
    }
  }

  &.delete-btn {
    background-color: transparent;
    color: #00B4D8;

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }
}

.table-divider {
  position: absolute;
  top: 42px;
  left: -0.26rem;
  right: -0.24rem;
  height: .01rem;
  background-color: #FFFFFF;
  z-index: 10;
}

// Modal styles
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  border-radius: 0.08rem;
}

.modal-content {
  background-color: #2B2C3D;
  border-radius: 0.08rem;
  width: 5.6rem;
  height: 2.5rem;
  position: relative;

  .modal-text {
    position: absolute;
    left: 50%;
    top: 40%;
    transform: translate(-50%, -50%);
    color: #FFFFFF;
    font-size: 0.2rem;
    margin: 0;
    width: 4.18rem;
    height: 0.27rem;
    text-align: center;
  }
}

.modal-actions {
  position: absolute;
  bottom: 0.4rem;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 0.4rem;
}

.modal-btn {
  width: 1.4rem;
  height: 0.4rem;
  font-size: 0.14rem;
  font-weight: 700;
  border-radius: 0.2rem;
  border: 1px solid;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  text-transform: uppercase;

  &.delete-confirm-btn {
    background-color: #00B4D8;
    color: #FFFFFF;
    border-color: #00B4D8;

    &:disabled {
      background-color: #4A5568;
      color: #A0AEC0;
      border-color: #4A5568;
      cursor: not-allowed;
    }
  }

  &.cancel-btn {
    background-color: #718096;
    color: #FFFFFF;
    border-color: #718096;
  }
}

.pagination-wrapper {
  position: fixed;
  bottom: 0.56rem;
  left: 2.4rem;
  right: 0;
  height: 0.6rem;
  background-color: #1E1E2F;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

// Pagination styles
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  color: #FFFFFF;
  font-size: 0.14rem;
  gap: 0.3rem;
  transform: translateX(-0.2rem); // 左移0.2rem
}

.page-info {
  color: #999;
  margin-right: 0.2rem;
}

.page-controls {
  display: flex;
  align-items: center;
  gap: 0.12rem;
}

.page-btn {
  width: 0.3rem;
  height: 0.3rem;
  background-color: #1E1E2F;
  border: none;
  border-radius: 0.1rem;
  color: #FFF9F9;
  cursor: pointer;
  font-size: 0.12rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;

  &:hover:not(:disabled):not(.previous-btn):not(.next-btn) {
    background-color: #00B4D8;
    color: #FFFFFF;
  }

  &.current {
    background-color: #00B4D8;
    color: #FFFFFF;
  }

  &:disabled {
    opacity: 0.3;
    cursor: not-allowed;
    color: #666;

    &:hover {
      transform: none;
      background-color: #2B2C3D;
    }
  }

  &.previous-btn,
  &.next-btn {
    width: auto;
    padding: 0 0.12rem;
    font-size: 0.12rem;
    font-weight: bold;
    min-width: 0.6rem;
    background-color: transparent;
    color: #00B4D8;
    border: none;

    &:hover {
      background-color: transparent !important;
      color: #00B4D8 !important;
    }

    &:disabled {
      opacity: 0.5;
      color: #666;
      background-color: transparent;

      &:hover {
        background-color: transparent !important;
        color: #666 !important;
      }
    }
  }
}

.page-dots {
  color: #FFF9F9;
  margin: 0 0.04rem;
  font-size: 0.12rem;
}

.user-row {
  .cell-id,
  .cell-username,
  .cell-name,
  .cell-status,
  .cell-role,
  .cell-actions {
    transition: all 0.3s ease;
  }

  .cell-id { left: 1.14rem; }
  .cell-username { left: 3.2rem; }
  .cell-name { left: 6.0rem; }
  .cell-status { left: 8.8rem; }
  .cell-role { left: 10.71rem; }
  .cell-actions { left: 12.77rem; }

  &.nav-collapsed {
    .cell-id { left: 1.4rem; }
    .cell-username { left: 3.7rem; }
    .cell-name { left: 6.8rem; }
    .cell-status { left: 9.5rem; }
    .cell-role { left: 11.8rem; }
    .cell-actions { left: calc(100% - 3.5rem); }
  }
}
</style>
