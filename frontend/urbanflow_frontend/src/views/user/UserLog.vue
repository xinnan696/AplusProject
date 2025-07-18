<template>
  <div class="user-log-page">
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
      <div class="logs-container">
        <!-- Page title -->
        <h1 class="page-title">User Logs</h1>

        <!-- Controls bar -->
        <div class="controls-bar">
          <div class="date-filter-group">
            <label class="date-label">From:</label>
            <input type="date" v-model="startDate" class="date-input" @change="handleDateChange" />
            <label class="date-label">To:</label>
            <input type="date" v-model="endDate" class="date-input" @change="handleDateChange" />
            <button @click="clearDateFilter" class="clear-btn">Clear</button>
          </div>

          <div class="search-group">
            <input
              type="text"
              class="search-input"
              placeholder="Search logs..."
              v-model="searchTerm"
              @input="handleSearchInput"
            >
            <button class="search-btn iconfont" @click="handleSearch">&#xeafe;</button>
          </div>

          <button class="export-btn" @click="exportLogs" :disabled="loading">
            Export
          </button>
        </div>

        <!-- Log entries grouped by date -->
        <div class="logs-table-container">

          <div v-if="loading" class="loading-container">
            <div class="loading-spinner"></div>
            <p class="loading-text">Loading logs...</p>
          </div>


          <div v-else-if="error" class="error-container">
            <p class="error-text">{{ error }}</p>
            <button @click="fetchUserLogs" class="retry-btn">Retry</button>
          </div>


          <div v-else-if="!loading && filteredGroupedLogs.length === 0" class="no-data-container">
            <p class="no-data-text">No logs found</p>
          </div>


          <div v-else class="logs-content">
            <div v-for="dateGroup in paginatedLogs" :key="dateGroup.date" class="date-group">
              <!-- Date header -->
              <div class="date-header">{{ formatDisplayDate(dateGroup.date) }}</div>

              <!-- Table header -->
              <div class="table-header">
              <div class="header-time" style="position: absolute; left: 0.26rem; display: flex; align-items: center; height: 40px; font-weight: bold;">Time</div>
              <div class="header-account" style="position: absolute; left: 2.0rem; display: flex; align-items: center; height: 40px; font-weight: bold;">Account</div>
              <div class="header-name" style="position: absolute; left: 4.5rem; display: flex; align-items: center; height: 40px; font-weight: bold;">Name</div>
              <div class="header-action" style="position: absolute; left: 7.0rem; display: flex; align-items: center; height: 40px; font-weight: bold;">Action</div>
              <div class="header-module" style="position: absolute; left: 9.8rem; display: flex; align-items: center; height: 40px; font-weight: bold;">Module</div>
              <div class="header-details" style="position: absolute; left: 12.8rem; display: flex; align-items: center; height: 40px; font-weight: bold;">Details</div>
              </div>

              <!-- Table body -->
              <div class="table-body">
                <div v-for="(log, index) in dateGroup.logs" :key="index" class="log-row">
                  <div class="cell-time" style="position: absolute; left: 0.26rem; display: flex; align-items: center; height: 40px;">{{ formatTime(log.timestamp) }}</div>
                  <div class="cell-account" style="position: absolute; left: 2.0rem; display: flex; align-items: center; height: 40px;">{{ log.accountNumber }}</div>
                  <div class="cell-name" style="position: absolute; left: 4.5rem; display: flex; align-items: center; height: 40px;">{{ log.userName }}</div>
                  <div class="cell-action" style="position: absolute; left: 7.0rem; display: flex; align-items: center; height: 40px;">
                    <span :class="getActionClass(log.action)">{{ formatAction(log.action) }}</span>
                  </div>
                  <div class="cell-module" style="position: absolute; left: 9.8rem; display: flex; align-items: center; height: 40px;">
                    <span :class="getModuleClass(log.module)">{{ formatModule(log.module) }}</span>
                  </div>
                  <div class="cell-details" style="position: absolute; left: 12.8rem; display: flex; align-items: center; height: 40px; width: calc(100% - 13.2rem); overflow: hidden; text-overflow: ellipsis;">{{ log.detail }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div class="pagination" v-if="!loading && totalItems > logsPerPage">
          <span class="page-info">
            Showing {{ (currentPage - 1) * logsPerPage + 1 }}-{{ Math.min(currentPage * logsPerPage, totalItems) }}
            of {{ totalItems }} logs
          </span>
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import ControlHeader from '@/views/control/ControlHeader.vue'
import ControlNav from '@/views/control/ControlNav.vue'
import ControlRecord from '@/views/control/ControlRecord.vue'
import { isNavVisible, toggleNav } from '@/utils/navState'
import UserLogApiService, {
  type UserLog,
  type DateGroupedLogs,
  type UserLogQueryParams
} from '@/services/userLogApi'

const router = useRouter()

// Reactive data
const searchTerm = ref('')
const startDate = ref('')
const endDate = ref('')
const currentPage = ref(1)
const logsPerPage = 10 // 每页显示10条日志记录
const isRecordVisible = ref(false)
const isEmergencyVisible = ref(false)
const isPriorityVisible = ref(false)
const loading = ref(false)
const error = ref('')

const originalLogs = ref<DateGroupedLogs[]>([])

// Computed properties
const filteredGroupedLogs = computed(() => {
  let filtered = [...originalLogs.value]

  if (searchTerm.value.trim()) {
    const searchLower = searchTerm.value.toLowerCase()
    filtered = filtered.map(dateGroup => ({
      ...dateGroup,
      logs: dateGroup.logs.filter(log =>
        log.accountNumber.toLowerCase().includes(searchLower) ||
        log.userName.toLowerCase().includes(searchLower) ||
        log.action.toLowerCase().includes(searchLower) ||
        log.detail.toLowerCase().includes(searchLower) ||
        log.module.toLowerCase().includes(searchLower)
      )
    })).filter(dateGroup => dateGroup.logs.length > 0)
  }

  // Apply date filter
  if (startDate.value && endDate.value) {
    const start = new Date(startDate.value)
    const end = new Date(endDate.value)
    end.setHours(23, 59, 59, 999)

    filtered = filtered.filter(dateGroup => {
      const groupDate = new Date(dateGroup.date)
      return groupDate >= start && groupDate <= end
    })
  } else if (startDate.value) {
    const start = new Date(startDate.value)
    filtered = filtered.filter(dateGroup => {
      const groupDate = new Date(dateGroup.date)
      return groupDate >= start
    })
  } else if (endDate.value) {
    const end = new Date(endDate.value)
    end.setHours(23, 59, 59, 999)
    filtered = filtered.filter(dateGroup => {
      const groupDate = new Date(dateGroup.date)
      return groupDate <= end
    })
  }

  return filtered
})

// 将所有日志展开为一个平面数组，按时间排序
const allFilteredLogs = computed(() => {
  const logs: Array<{ log: UserLog; date: string; timestamp: string }> = []
  
  filteredGroupedLogs.value.forEach(dateGroup => {
    dateGroup.logs.forEach(log => {
      logs.push({ 
        log, 
        date: dateGroup.date,
        timestamp: log.timestamp
      })
    })
  })
  
  // 按时间戳排序，最新的在前
  return logs.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
})

const totalItems = computed(() => {
  return allFilteredLogs.value.length
})

// 按日志条数分页，每页显示10条
const totalPages = computed(() => {
  return Math.ceil(totalItems.value / logsPerPage)
})

// 分页后重新按日期分组显示
const paginatedLogs = computed(() => {
  const start = (currentPage.value - 1) * logsPerPage
  const end = start + logsPerPage
  const currentPageLogs = allFilteredLogs.value.slice(start, end)
  
  // 按日期重新分组
  const groupedByDate = new Map<string, UserLog[]>()
  
  currentPageLogs.forEach(({ log, date }) => {
    if (!groupedByDate.has(date)) {
      groupedByDate.set(date, [])
    }
    groupedByDate.get(date)!.push(log)
  })
  
  // 转换为数组格式并排序
  const result: DateGroupedLogs[] = []
  groupedByDate.forEach((logs, date) => {
    // 对每个日期组内的日志按时间排序
    const sortedLogs = logs.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
    result.push({ date, logs: sortedLogs })
  })
  
  // 按日期排序（最新的在前）
  return result.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
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

const fetchUserLogs = async () => {
  try {
    loading.value = true
    error.value = ''

    console.log('🚀 开始获取用户日志数据...');

    const params: UserLogQueryParams = {}


    const response = await UserLogApiService.getUserLogs(params)

    console.log('📊 API响应:', response);
    console.log('📊 响应代码:', response.code);
    console.log('📊 数据长度:', response.data?.length);

    if (response.code === 200) {
      originalLogs.value = response.data
      currentPage.value = 1
      
      console.log('📊 Loaded logs:', {
        totalDateGroups: response.data.length,
        totalLogs: response.data.reduce((total, group) => total + group.logs.length, 0),
        firstFewDates: response.data.slice(0, 3).map(g => g.date)
      })
      
      console.log('First few logs from each date:')
      response.data.slice(0, 3).forEach(dateGroup => {
        console.log(`${dateGroup.date}: ${dateGroup.logs.length} logs`)
        if (dateGroup.logs.length > 0) {
          console.log('  Sample:', dateGroup.logs[0])
        }
      })
    } else {
      error.value = 'Failed to fetch user logs'
      console.error('❌ API Error:', response.code)
    }
  } catch (err: any) {
    error.value = err.message || 'Failed to fetch user logs'
    console.error( err)
  } finally {
    loading.value = false
    console.log('🏁 获取用户日志完成，loading:', loading.value, 'error:', error.value);
  }
}

const exportLogs = async () => {
  try {
    loading.value = true

    const params: UserLogQueryParams = {}


    if (searchTerm.value.trim()) {
      params.searchTerm = searchTerm.value.trim()
    }

    if (startDate.value) {
      params.startDate = startDate.value
    }
    if (endDate.value) {
      params.endDate = endDate.value
    }

    const blob = await UserLogApiService.exportUserLogs(params)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `user-logs-${new Date().toISOString().split('T')[0]}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (err: any) {
    error.value = err.message || 'Failed to export user logs'
    console.error('Error exporting user logs:', err)
  } finally {
    loading.value = false
  }
}

const formatDisplayDate = (dateStr: string) => {
  const date = new Date(dateStr)
  const today = new Date()
  const yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000)

  if (dateStr === today.toISOString().split('T')[0]) {
    return `Today (${date.toLocaleDateString()})`
  } else if (dateStr === yesterday.toISOString().split('T')[0]) {
    return `Yesterday (${date.toLocaleDateString()})`
  } else {
    return date.toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    })
  }
}
const formatTime = (timestamp: string) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('en-US', { hour12: false })
}

const formatAction = (action: string) => {
  const actionMap: { [key: string]: string } = {
    'MANUAL_CONTROL': 'Manual Control',
    'LOGIN': 'Login',
    'LOGOUT': 'Logout',
    'CREATE': 'Create',
    'UPDATE': 'Update',
    'DELETE': 'Delete',
    'VIEW': 'View'
  }
  return actionMap[action] || action
}

const formatModule = (module: string) => {
  const moduleMap: { [key: string]: string } = {
    'TRAFFIC_CONTROL': 'Traffic Control',
    'AUTH': 'Authentication',
    'USER_MANAGEMENT': 'User Management',
    'DASHBOARD': 'Dashboard'
  }
  return moduleMap[module] || module
}

const getActionClass = (action: string) => {
  const actionClasses: { [key: string]: string } = {
    'LOGIN': 'action-success',
    'LOGOUT': 'action-info',
    'CREATE': 'action-success',
    'UPDATE': 'action-warning',
    'DELETE': 'action-danger',
    'MANUAL_CONTROL': 'action-primary'
  }
  return actionClasses[action] || 'action-default'
}

const getModuleClass = (module: string) => {
  const moduleClasses: { [key: string]: string } = {
    'TRAFFIC_CONTROL': 'module-traffic',
    'AUTH': 'module-auth',
    'USER_MANAGEMENT': 'module-user'
  }
  return moduleClasses[module] || 'module-default'
}

const goToPage = (page: number) => {
  console.log('Going to page:', page, 'of', totalPages.value)
  currentPage.value = page
}

const previousPage = () => {
  if (currentPage.value > 1) {
    console.log('Going to previous page:', currentPage.value - 1)
    currentPage.value--
  } else {
    console.log('Already on first page')
  }
}

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    console.log('Going to next page:', currentPage.value + 1, 'total pages:', totalPages.value)
    currentPage.value++
  } else {
    console.log('Already on last page, current:', currentPage.value, 'total:', totalPages.value)
  }
}

const handleDateChange = () => {
  currentPage.value = 1
}

const clearDateFilter = () => {
  startDate.value = ''
  endDate.value = ''
  currentPage.value = 1
}

const handleSearchInput = () => {
  currentPage.value = 1
}

const handleSearch = () => {
  currentPage.value = 1
}

onMounted(() => {
  fetchUserLogs()
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
.user-log-page {
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
}

.logs-container {
  flex: 1;
  padding: 0.24rem 0.24rem 0.24rem 0;
  background-color: #1E1E2F;
  display: flex;
  flex-direction: column;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.page-title {
  color: #FFFFFF;
  font-size: 0.28rem;
  font-weight: bold;
  margin: 0 0 0.24rem 0;
  position: absolute;
  left: 0.26rem;
  top: 0.35rem;
}

.controls-bar {
  display: flex;
  align-items: center;
  gap: 0.24rem;
  margin-bottom: 0.24rem;
  margin-top: 0.9rem;
  margin-left: 0.26rem;
  flex-wrap: wrap;
}

.date-filter-group {
  display: flex;
  align-items: center;
  gap: 0.12rem;
  padding: 0.08rem;
  background-color: #2B2B3C;
  border-radius: 0.08rem;
}

.date-label {
  color: #FFFFFF;
  font-size: 0.14rem;
  font-weight: 500;
}

.date-input {
  padding: 0.06rem 0.08rem;
  background-color: #1E1E2F;
  border: 1px solid #3A3A4D;
  border-radius: 0.04rem;
  color: #FFFFFF;
  font-size: 0.14rem;
  width: 1.2rem;
  transition: all 0.3s ease;

  &:hover {
    border-color: #00B4D8;
    background-color: #2B2B3C;
  }

  &:focus {
    outline: none;
    border-color: #00B4D8;
    background-color: #2B2B3C;
    box-shadow: 0 0 0 2px rgba(0, 180, 216, 0.1);
  }
}

.clear-btn {
  padding: 0.06rem 0.12rem;
  background-color: #6c757d;
  border: none;
  border-radius: 0.04rem;
  color: #FFFFFF;
  font-size: 0.14rem;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    background-color: #5a6268;
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
  }
}

.search-group {
  display: flex;
  align-items: center;
  gap: 0.08rem;
}

.search-input {
  padding: 0.08rem 0.12rem;
  background-color: #2B2B3C;
  border: 1px solid #2B2B3C;
  border-radius: 0.04rem;
  color: #FFFFFF;
  font-size: 0.14rem;
  width: 2rem;
  height: 0.4rem;
  transition: all 0.3s ease;

  &::placeholder {
    color: #666;
  }

  &:hover {
    background-color: #3A3A4D;
    border-color: #3A3A4D;
  }

  &:focus {
    outline: none;
    border-color: #00B4D8;
    background-color: #3A3A4D;
    box-shadow: 0 0 0 2px rgba(0, 180, 216, 0.1);
  }
}

.search-btn, .export-btn {
  background-color: #1E1E2F;
  padding: 0.08rem 0.12rem;
  border: none;
  border-radius: 0.04rem;
  color: #FFFFFF;
  font-size: 0.14rem;
  cursor: pointer;
  height: 0.4rem;
  transition: all 0.3s ease;

  &:hover:not(:disabled) {
    background-color: #0096c7;
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.export-btn {
  margin-left: 0.16rem;
}



.logs-table-container {
  flex: 1;
  margin-bottom: 0.16rem;
  margin-left: 0.26rem;
  margin-right: 0.24rem;
  position: relative;
  background: #1E1E2F;
  border-radius: 0.12rem;
  overflow: hidden; // 禁止滚动条
}

.logs-content {
  flex: 1;
}

.loading-container, .error-container, .no-data-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  gap: 0.16rem;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #2B2B3C;
  border-top: 3px solid #00B4D8;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text, .no-data-text {
  color: #999;
  font-size: 0.14rem;
  margin: 0;
}

.error-text {
  color: #ff6b6b;
  font-size: 0.14rem;
  margin: 0;
  text-align: center;
}

.retry-btn {
  padding: 0.08rem 0.16rem;
  background-color: #00B4D8;
  border: none;
  border-radius: 0.04rem;
  color: #FFFFFF;
  font-size: 0.14rem;
  cursor: pointer;

  &:hover {
    background-color: #0096c7;
  }
}

.date-group {
  margin-bottom: 0.32rem;
}

.date-header {
  color: #FFFFFF;
  font-size: 0.18rem;
  font-weight: bold;
  margin-bottom: 0.16rem;
  border-bottom: 2px solid #00B4D8;
  padding-bottom: 0.08rem;
}

.table-header {
  display: grid;
  grid-template-columns: 0.8rem 1.5rem 1rem 1.5rem 1.5rem 3fr;
  gap: 0.16rem;
  color: #FFFFFF;
  font-size: 0.14rem;
  font-weight: bold;
  margin-bottom: 0.08rem;
  padding: 0.08rem 0;
  border-bottom: 1px solid #3A3A4D;
}

.table-body {
  color: #FFFFFF;
  font-size: 0.14rem;
}

.log-row {
  display: grid;
  grid-template-columns: 0.8rem 1.5rem 1rem 1.5rem 1.5rem 3fr;
  gap: 0.16rem;
  padding: 0.12rem 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  transition: background-color 0.2s ease;

  &:hover {
    background-color: rgba(255, 255, 255, 0.02);
  }

  .cell-time, .cell-account, .cell-name, .cell-action, .cell-module, .cell-details {
    display: flex;
    align-items: center;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .cell-details {
    color: #999;
    white-space: normal;
    overflow: visible;
    text-overflow: unset;
  }
}

.action-success {
  background-color: rgba(40, 167, 69, 0.2);
  color: #28a745;
  padding: 0.04rem 0.08rem;
  border-radius: 0.04rem;
  font-size: 0.12rem;
  font-weight: 500;
}

.action-info {
  background-color: rgba(23, 162, 184, 0.2);
  color: #17a2b8;
  padding: 0.04rem 0.08rem;
  border-radius: 0.04rem;
  font-size: 0.12rem;
  font-weight: 500;
}

.action-warning {
  background-color: rgba(255, 193, 7, 0.2);
  color: #ffc107;
  padding: 0.04rem 0.08rem;
  border-radius: 0.04rem;
  font-size: 0.12rem;
  font-weight: 500;
}

.action-danger {
  background-color: rgba(220, 53, 69, 0.2);
  color: #dc3545;
  padding: 0.04rem 0.08rem;
  border-radius: 0.04rem;
  font-size: 0.12rem;
  font-weight: 500;
}

.action-primary {
  background-color: rgba(0, 180, 216, 0.2);
  color: #00B4D8;
  padding: 0.04rem 0.08rem;
  border-radius: 0.04rem;
  font-size: 0.12rem;
  font-weight: 500;
}

.action-default {
  background-color: rgba(108, 117, 125, 0.2);
  color: #6c757d;
  padding: 0.04rem 0.08rem;
  border-radius: 0.04rem;
  font-size: 0.12rem;
  font-weight: 500;
}

.module-traffic {
  background-color: rgba(0, 180, 216, 0.15);
  color: #00B4D8;
  padding: 0.04rem 0.08rem;
  border-radius: 0.04rem;
  font-size: 0.12rem;
  font-weight: 500;
}

.module-auth {
  background-color: rgba(111, 66, 193, 0.15);
  color: #6f42c1;
  padding: 0.04rem 0.08rem;
  border-radius: 0.04rem;
  font-size: 0.12rem;
  font-weight: 500;
}

.module-user {
  background-color: rgba(40, 167, 69, 0.15);
  color: #28a745;
  padding: 0.04rem 0.08rem;
  border-radius: 0.04rem;
  font-size: 0.12rem;
  font-weight: 500;
}

.module-default {
  background-color: rgba(108, 117, 125, 0.15);
  color: #6c757d;
  padding: 0.04rem 0.08rem;
  border-radius: 0.04rem;
  font-size: 0.12rem;
  font-weight: 500;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  color: #FFFFFF;
  font-size: 0.14rem;
  gap: 0.3rem;
  padding: 0.16rem 0;
  margin-top: auto; // 推到底部
  border-top: 1px solid #3A3A4D;
  background-color: #1E1E2F;
  position: relative;
  flex-shrink: 0; // 防止被压缩
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

  &:hover:not(:disabled) {
    background-color: #00B4D8;
    color: #FFFFFF;
    transform: scale(1.05);
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

  // Previous/Next buttons special styles
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

    &:hover:not(:disabled) {
      background-color: transparent;
      color: #00D4F8;
      transform: none;
    }

    &:disabled {
      opacity: 0.5;
      color: #666;
      background-color: transparent;

      &:hover {
        background-color: transparent;
        transform: none;
      }
    }
  }
}

.page-dots {
  color: #FFF9F9;
  margin: 0 0.04rem;
  font-size: 0.12rem;
}

@media (max-width: 1200px) {
  .table-header, .log-row {
    grid-template-columns: 0.6rem 1.2rem 0.8rem 1.2rem 1.2rem 2fr;
    gap: 0.12rem;
  }

  .controls-bar {
    flex-wrap: wrap;
    gap: 0.12rem;
  }
}

@media (max-width: 768px) {
  .main-area {
    margin-left: 0;
  }

  .page-title {
    left: 0.16rem;
  }

  .controls-bar {
    margin-left: 0.16rem;
    flex-direction: column;
    align-items: flex-start;
  }

  .logs-table-container {
    margin-left: 0.16rem;
  }

  .table-header, .log-row {
    grid-template-columns: 1fr;
    gap: 0.08rem;
  }

  .cell-time, .cell-account, .cell-name, .cell-action, .cell-module, .cell-details {
    white-space: normal;
  }
}


.table-header {
  position: relative !important;
  height: 40px !important;
  color: #FFFFFF !important;
  font-size: 0.14rem !important;
  font-weight: 600 !important;
  display: block !important;
  grid-template-columns: unset !important;
  gap: unset !important;
  margin-bottom: 0 !important;
  padding: 0 !important;
  border-bottom: none !important;
}

.table-divider {
  height: 0.02rem;
  background: linear-gradient(90deg, transparent, #00B4D8, transparent);
  margin: 0.1rem 0;
  position: relative;
  left: -0.26rem;
  right: -0.24rem;
  width: calc(100% + 0.5rem);
}

.table-body {
  color: #FFFFFF !important;
  font-size: 0.14rem !important;
  display: flex;
  flex-direction: column;
  gap: 0.15rem; // 增加行间距从0.08rem到0.15rem
  padding: 0.2rem 0; // 增加顶部和底部的padding
}

.log-row {
  position: relative !important;
  height: 40px !important;
  color: #FFFFFF !important;
  font-size: 0.14rem !important;
  transition: all 0.3s ease !important;
  display: block !important;
  grid-template-columns: unset !important;
  gap: unset !important;
  padding: 0 !important;

  &:hover {
    transform: translateX(0.04rem) !important;
  }

  .cell-time, .cell-account, .cell-name, .cell-action, .cell-module, .cell-details {
    display: flex !important;
    align-items: center !important;
    position: absolute !important;
    height: 40px !important;
    overflow: hidden !important;
    text-overflow: ellipsis !important;
    white-space: nowrap !important;
  }

  .cell-details {
    color: #999 !important;
    width: calc(100% - 9.5rem) !important;
  }
}
</style>
