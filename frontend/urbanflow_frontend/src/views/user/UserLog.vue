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

    <div class="main-area" :class="{ 'nav-collapsed': !isNavVisible }">
      <div class="logs-container">
        <!-- Page title -->
        <h1 class="page-title">User Logs</h1>

        <!-- Controls bar -->
        <div class="controls-bar">
          <div class="date-filter-group">
            <label class="date-label">From:</label>
            <div class="date-input-wrapper">
              <input 
                type="date" 
                v-model="startDate" 
                class="date-input" 
                @change="handleDateChange"
                :class="{ 'has-value': startDate }"
                ref="startDateInput"
                @click="handleDateInputClick($event)"
              />
              <span v-if="!startDate" class="date-placeholder">dd/mm/yyyy</span>
            </div>
            <label class="date-label">To:</label>
            <div class="date-input-wrapper">
              <input 
                type="date" 
                v-model="endDate" 
                class="date-input" 
                @change="handleDateChange"
                :class="{ 'has-value': endDate }"
                ref="endDateInput"
                @click="handleDateInputClick($event)"
              />
              <span v-if="!endDate" class="date-placeholder">dd/mm/yyyy</span>
            </div>
            <button 
              @click="clearDateFilter" 
              class="clear-btn"
              :class="{ 'active': hasDateFilter, 'disabled': !hasDateFilter }"
              :disabled="!hasDateFilter"
            >
              Clear
            </button>
          </div>

          <div class="search-group">
            <input
              type="text"
              class="search-input"
              placeholder="Search by Account Number..."
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

              <!-- Table header for this date group -->
              <div class="table-header" :class="{ 'nav-collapsed': !isNavVisible }">
                <div class="header-time">Time</div>
                <div class="header-account">Account</div>
                <div class="header-name">Name</div>
                <div class="header-action">Action</div>
                <div class="header-module">Module</div>
                <div class="header-details">Details</div>
              </div>

              <!-- Table body -->
              <div class="table-body">
                <div v-for="(log, index) in dateGroup.logs" :key="index" class="log-row" :class="{ 'nav-collapsed': !isNavVisible }">
                  <div class="cell-time">{{ formatTime(log.timestamp) }}</div>
                  <div class="cell-account">{{ log.accountNumber }}</div>
                  <div class="cell-name">{{ log.userName }}</div>
                  <div class="cell-action">
                    <span :class="getActionClass(log.action)">{{ formatAction(log.action) }}</span>
                  </div>
                  <div class="cell-module">
                    <span :class="getModuleClass(log.module)">{{ formatModule(log.module) }}</span>
                  </div>
                  <div class="cell-details" 
                    @mouseenter="showTooltip($event, log.detail)"
                    @mouseleave="hideTooltip"
                  >
                    <span class="detail-text">{{ log.detail }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div class="pagination" v-if="!loading && totalItems > dynamicLogsPerPage">
          <span class="page-info">
            Showing {{ (currentPage - 1) * dynamicLogsPerPage + 1 }}-{{ Math.min(currentPage * dynamicLogsPerPage, totalItems) }}
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
    
    <!-- Global Tooltip -->
    <div v-if="tooltipVisible" 
         class="simple-tooltip"
         :style="tooltipStyle">
      {{ tooltipText }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
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
const dynamicLogsPerPage = ref(10) // 初始值，会动态调整
const isRecordVisible = ref(false)
const isEmergencyVisible = ref(false)
const isPriorityVisible = ref(false)
const loading = ref(false)
const error = ref('')
const containerHeight = ref(0) // 容器高度

// Tooltip state
const tooltipVisible = ref(false)
const tooltipText = ref('')
const tooltipStyle = ref({})

const originalLogs = ref<DateGroupedLogs[]>([])

// Computed properties
const hasDateFilter = computed(() => {
  return !!(startDate.value || endDate.value)
})
const filteredGroupedLogs = computed(() => {
  let filtered = [...originalLogs.value]

  if (searchTerm.value.trim()) {
    const searchLower = searchTerm.value.toLowerCase()
    filtered = filtered.map(dateGroup => ({
      ...dateGroup,
      logs: dateGroup.logs.filter(log =>
        log.accountNumber.toLowerCase().includes(searchLower)
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

// 按日志条数分页，动态计算每页显示条数
const totalPages = computed(() => {
  return Math.ceil(totalItems.value / dynamicLogsPerPage.value)
})

// 分页后重新按日期分组显示
const paginatedLogs = computed(() => {
  const start = (currentPage.value - 1) * dynamicLogsPerPage.value
  const end = start + dynamicLogsPerPage.value
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

// 监听导航栏状态变化
watch(() => isNavVisible.value, () => {
  // 导航栏状态改变时重新计算
  setTimeout(calculateDynamicLogsPerPage, 500)
})

// 监听数据变化
watch(filteredGroupedLogs, () => {
  // 数据过滤后重置到第一页
  currentPage.value = 1
})

// 保存上次的视口高度
let lastViewportHeight = window.innerHeight

// 动态计算每页应该显示的日志条数 - 简化版本
const calculateDynamicLogsPerPage = () => {
  try {
    const viewportHeight = window.innerHeight
    
    // 只有在视口高度变化较大时才重新计算
    if (Math.abs(viewportHeight - lastViewportHeight) < 50) {
      return
    }
    
    lastViewportHeight = viewportHeight
    
    // 基于视口高度的简单计算
    // 假设页面上下占用3分之1的空间
    const availableHeight = viewportHeight * 0.65
    
    // 每条日志预估高度（包括分组标题平均开销）
    const estimatedHeightPerLog = 55
    
    // 计算可显示的日志数
    const calculatedLogs = Math.floor(availableHeight / estimatedHeightPerLog)
    
    // 限制在合理范围内
    const targetLogsPerPage = Math.max(5, Math.min(20, calculatedLogs))
    
    // 只有当值真正改变时才更新
    if (dynamicLogsPerPage.value !== targetLogsPerPage) {
      console.log('📊 Adjusting logs per page based on viewport:', {
        viewportHeight,
        availableHeight,
        oldValue: dynamicLogsPerPage.value,
        newValue: targetLogsPerPage
      })
      dynamicLogsPerPage.value = targetLogsPerPage
    }
    
  } catch (error) {
    console.warn('Failed to calculate logs per page:', error)
  }
}



const fetchUserLogs = async () => {
  try {
    loading.value = true
    error.value = ''


    const params: UserLogQueryParams = {}


    const response = await UserLogApiService.getUserLogs(params)



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
      console.error('API Error:', response.code)
    }
  } catch (err: any) {
    error.value = err.message || 'Failed to fetch user logs'
    console.error( err)
  } finally {
    loading.value = false
    console.log(loading.value, 'error:', error.value);
  }
}

const exportLogs = async () => {
  try {
    loading.value = true

    // Get the filtered logs data that's currently displayed
    const logsToExport: UserLog[] = []

    filteredGroupedLogs.value.forEach(dateGroup => {
      dateGroup.logs.forEach(log => {
        logsToExport.push(log)
      })
    })

    if (logsToExport.length === 0) {
      error.value = 'No logs to export'
      return
    }

    // Create CSV content
    const headers = ['Date', 'Time', 'Account Number', 'User Name', 'Action', 'Module', 'Details']
    const csvContent = []

    // Add headers
    csvContent.push(headers.join(','))

    // Add data rows
    logsToExport.forEach(log => {
      const date = new Date(log.timestamp)
      const dateStr = date.toISOString().split('T')[0]
      const timeStr = date.toLocaleTimeString('en-US', { hour12: false })

      const row = [
        dateStr,
        timeStr,
        `"${log.accountNumber || ''}"`,
        `"${log.userName || ''}"`,
        `"${formatAction(log.action)}"`,
        `"${formatModule(log.module)}"`,
        `"${log.detail || ''}"`
      ]
      csvContent.push(row.join(','))
    })

    // Create and download CSV file
    const csvString = csvContent.join('\n')
    const blob = new Blob([csvString], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `user-logs-${new Date().toISOString().split('T')[0]}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    console.log(`Exported ${logsToExport.length} logs to CSV`)

  } catch (err: any) {
    error.value = err.message || 'Failed to export user logs'
    console.error('Error exporting user logs:', err)
  } finally {
    loading.value = false
  }
}

const formatDisplayDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return date.toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}
const formatTime = (timestamp: string) => {
  const date = new Date(timestamp)
  return date.toLocaleTimeString('en-US', { hour12: false })
}

const formatAction = (action: string) => {
  const actionMap: { [key: string]: string } = {
    'MANUAL_CONTROL': 'Manual Control',
    'MANUAL': 'Manual',
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
    'AUTH': 'module-auth-blue',
    'USER_MANAGEMENT': 'module-user'
  }
  return moduleClasses[module] || 'module-default'
}

// Tooltip methods
const showTooltip = (event: MouseEvent, text: string) => {
  if (!text) return
  
  tooltipText.value = text
  tooltipVisible.value = true
  
  // Position tooltip
  const rect = (event.target as HTMLElement).getBoundingClientRect()
  tooltipStyle.value = {
    position: 'fixed',
    left: `${rect.left + rect.width / 2}px`,
    top: `${rect.bottom + 8}px`,
    transform: 'translateX(-50%)',
    zIndex: 99999,
    maxWidth: '400px',
    whiteSpace: 'normal',
    wordWrap: 'break-word'
  }
}

const hideTooltip = () => {
  tooltipVisible.value = false
  tooltipText.value = ''
  tooltipStyle.value = {}
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

// 处理日期输入框点击，确保整个区域都可以打开日历
const handleDateInputClick = (event: Event) => {
  const input = event.target as HTMLInputElement
  // 强制打开日历选择器
  try {
    input.showPicker && input.showPicker()
  } catch (e) {
    // 如果showPicker不支持，就使用默认行为
    input.focus()
  }
}

onMounted(async () => {
  // 先加载数据
  try {
    await fetchUserLogs()
    // 等待DOM更新后计算分页
    await nextTick()
    // 初始计算
    calculateDynamicLogsPerPage()
  } catch (error) {
    console.error('Failed to load initial data:', error)
  }
  
  // 监听窗口大小变化 - 使用简单的防抖
  let resizeTimer: ReturnType<typeof setTimeout>
  const handleResize = () => {
    clearTimeout(resizeTimer)
    resizeTimer = setTimeout(calculateDynamicLogsPerPage, 500)
  }
  window.addEventListener('resize', handleResize)
  
  // 组件卸载时清理
  onUnmounted(() => {
    clearTimeout(resizeTimer)
    window.removeEventListener('resize', handleResize)
  })
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
  width: calc(100vw - 2.4rem);
  transition: all 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  
  // 当导航栏收起时的样式
  &.nav-collapsed {
    margin-left: 0.24rem;
    width: calc(100vw - 0.24rem);
  }
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
  min-height: 0; // 确保flex布局正常工作
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
  padding: 0.08rem 0.08rem 0.08rem 0;
  background-color: #1E1E2F;
  border-radius: 0.08rem;
}

.date-input-wrapper {
  position: relative;
  display: inline-block;
}

.date-label {
  color: #FFFFFF;
  font-size: 0.14rem;
  font-weight: 500;
}

.date-placeholder {
  position: absolute;
  left: 0.08rem;
  top: 50%;
  transform: translateY(-50%);
  color: #666;
  font-size: 0.14rem;
  pointer-events: none;
  z-index: 1;
  transition: opacity 0.2s ease;
  letter-spacing: 0.02rem; // 增加字母间距
  font-family: monospace; // 使用等宽字体确保对齐
}

.date-input {
  padding: 0.06rem 0.08rem;
  background-color: #1E1E2F;
  border: 1px solid #3A3A4D;
  border-radius: 0.04rem;
  color: #FFFFFF;
  font-size: 0.14rem;
  width: 1.6rem;
  transition: all 0.3s ease;
  position: relative;
  cursor: pointer;

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

  // 完全隐藏原生日期显示，只保持功能
  &::-webkit-datetime-edit {
    width: 100%;
    height: 100%;
    color: transparent !important;
    cursor: pointer;
    opacity: 0 !important; // 完全隐藏
  }
  
  &.has-value::-webkit-datetime-edit {
    color: #FFFFFF !important;
    opacity: 1 !important; // 有值时显示
  }
  
  &::-webkit-datetime-edit-text {
    color: transparent !important; // 隐藏分隔符
    opacity: 0 !important;
  }
  
  &.has-value::-webkit-datetime-edit-text {
    color: #666 !important;
    opacity: 1 !important;
  }
  
  &::-webkit-datetime-edit-month-field,
  &::-webkit-datetime-edit-day-field,
  &::-webkit-datetime-edit-year-field {
    color: transparent !important;
    cursor: pointer;
    opacity: 0 !important;
  }
  
  &.has-value::-webkit-datetime-edit-month-field,
  &.has-value::-webkit-datetime-edit-day-field,
  &.has-value::-webkit-datetime-edit-year-field {
    color: #FFFFFF !important;
    opacity: 1 !important;
  }
  
  &::-webkit-calendar-picker-indicator {
    filter: brightness(0) invert(1) !important;
    cursor: pointer;
    width: 0.16rem;
    height: 0.16rem;
    opacity: 1 !important;
    position: relative;
    margin-left: 0.08rem;
  }

  &::-webkit-calendar-picker-indicator:hover {
    background-color: rgba(0, 180, 216, 0.1) !important;
    border-radius: 0.02rem;
  }

  // 确保整个输入框都可以点击
  &::-webkit-datetime-edit-fields-wrapper {
    cursor: pointer;
    width: 100%;
    height: 100%;
    opacity: 0 !important; // 隐藏包装器
  }
  
  &.has-value::-webkit-datetime-edit-fields-wrapper {
    opacity: 1 !important;
  }

  // focus时隐藏placeholder
  &:focus + .date-placeholder {
    opacity: 0;
  }

  // 点击时隐藏placeholder
  &:active + .date-placeholder {
    opacity: 0;
  }
}

.clear-btn {
  padding: 0.06rem 0.12rem;
  border: none;
  border-radius: 0.04rem;
  color: #FFFFFF;
  font-size: 0.14rem;
  cursor: pointer;
  transition: all 0.3s ease;

  &.active {
    background-color: #00B4D8;
    
    &:hover {
      background-color: #0096c7;
      transform: translateY(-1px);
    }
  }

  &.disabled {
    background-color: #6c757d;
    opacity: 0.5;
    cursor: not-allowed;
    
    &:hover {
      transform: none;
      background-color: #6c757d;
    }
  }

  &:active {
    transform: translateY(0);
  }
}

.search-group {
  display: flex;
  align-items: center;
  gap: 0.1rem;
  margin-left: 0.3rem;
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

.search-btn {
  background-color: #1E1E2F;
  padding: 0.08rem 0.12rem;
  border: none;
  border-radius: 0.04rem;
  color: #FFFFFF;
  font-size: 0.3rem;
  cursor: pointer;
  transition: all 0.3s ease;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}
.export-btn{
  background-color: #00B4D8;
  padding: 0.08rem 0.12rem;
  border: none;
  border-radius: 0.04rem;
  color: #FFFFFF;
  font-size: 0.14rem;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-left: 0.16rem;

  &:hover:not(:disabled) {
    background-color: #0096c7;
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    background-color: #00B4D8;
  }
}

.search-btn {
  width: 0.3rem;
  height: 0.3rem;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
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
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
}

.logs-content {
  flex: 1;
  min-height: 0;
  padding: 0.08rem 0;
  overflow: hidden;
  height: 100%;
}

.loading-container, .error-container, .no-data-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  gap: 0.16rem;
  min-height: 200px;
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
  margin-bottom: 0.24rem;
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
  position: relative;
  height: 36px;
  color: #FFFFFF;
  font-size: 0.14rem;
  font-weight: 600;
  margin-bottom: 0.1rem;
  
  // 为所有标题元素添加平滑过渡
  .header-time,
  .header-account,
  .header-name,
  .header-action,
  .header-module,
  .header-details {
    position: absolute;
    display: flex;
    align-items: center;
    height: 36px;
    font-weight: bold;
    transition: all 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  }
  
  // 默认状态下的列位置
  .header-time { left: 0.26rem; }
  .header-account { left: 2.0rem; }
  .header-name { left: 4.5rem; }
  .header-action { left: 7.0rem; }
  .header-module { left: 9.8rem; }
  .header-details { left: 12.8rem; }
  
  // 导航栏收起时的列位置
  &.nav-collapsed {
    .header-time { left: 0.26rem; }
    .header-account { left: 2.5rem; }
    .header-name { left: 5.5rem; }
    .header-action { left: 8.5rem; }
    .header-module { left: 11.5rem; }
    .header-details { left: 14.8rem; }
  }
}

.table-body {
  color: #FFFFFF;
  font-size: 0.14rem;
}

.log-row {
  position: relative;
  height: 36px;
  color: #FFFFFF;
  font-size: 0.14rem;
  transition: all 0.3s ease;
  margin: 0.08rem 0;
  border-radius: 0.04rem;
  display: flex; // 使用flex布局
  align-items: center;

  // 为所有单元格设置基本样式
  .cell-time,
  .cell-account,
  .cell-name,
  .cell-action,
  .cell-module {
    position: absolute;
    display: flex;
    align-items: center;
    height: 36px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    transition: all 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  }
  
  // Detail列使用不同的布局方式
  .cell-details {
    position: absolute;
    height: 36px;
    color: #999;
    cursor: help;
    transition: all 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
    
    // 确保省略号正确显示
    overflow: hidden !important;
    text-overflow: ellipsis !important;
    white-space: nowrap !important;
    display: flex !important;
    align-items: center;
  }
  
  // 默认状态下的列位置
  .cell-time { left: 0.26rem; }
  .cell-account { left: 2.0rem; }
  .cell-name { left: 4.5rem; }
  .cell-action { left: 7.0rem; }
  .cell-module { left: 9.8rem; }
  .cell-details { 
    left: 12.8rem;
    width: calc(100% - 13.4rem); // 留一些边距
    right: 0.2rem;
  }
  
  // 导航栏收起时的列位置
  &.nav-collapsed {
    .cell-time { left: 0.26rem; }
    .cell-account { left: 2.5rem; }
    .cell-name { left: 5.5rem; }
    .cell-action { left: 8.5rem; }
    .cell-module { left: 11.5rem; }
    .cell-details { 
      left: 14.8rem;
      width: calc(100% - 15.0rem); // 留一些边距
      right: 0.2rem;
    }
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

.module-auth-blue {
  background-color: rgba(0, 180, 216, 0.15);
  color: #00B4D8;
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
  margin-top: 0.08rem;
  border-top: 1px solid #3A3A4D;
  background-color: #1E1E2F;
  position: relative;
  flex-shrink: 0;
  transform: translateX(-0.2rem);
  z-index: 100; // 增加z-index确保始终在最上层
  min-height: 60px; // 保证最小高度
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
  gap: 0.08rem;
  padding: 0.12rem 0;
}

// 日历图标和基本样式
:global(input[type="date"]::-webkit-calendar-picker-indicator) {
  filter: brightness(0) invert(1) !important;
  cursor: pointer !important;
  opacity: 1 !important;
}

// 设置暗色主题（尝试影响日历弹出框）
:global(input[type="date"]) {
  color-scheme: dark;
}

:global(html) {
  color-scheme: dark;
}

// Tooltip 样式
.simple-tooltip {
  position: fixed;
  padding: 8px 12px;
  background: rgba(45, 45, 45, 0.95) !important;
  color: #ffffff !important;
  font-size: 12px !important;
  font-weight: 500 !important;
  line-height: 1.4 !important;
  border-radius: 6px;
  white-space: normal;
  word-wrap: break-word;
  max-width: 400px;
  z-index: 99999;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  pointer-events: none;
  opacity: 1;
  visibility: visible;
  transition: opacity 0.2s ease, visibility 0.2s ease;
  
  // 小三角箭头
  &::before {
    content: '';
    position: absolute;
    top: -4px;
    left: 50%;
    transform: translateX(-50%);
    width: 0;
    height: 0;
    border-left: 6px solid transparent;
    border-right: 6px solid transparent;
    border-bottom: 6px solid rgba(45, 45, 45, 0.95);
  }
}

// 强制Detail列的省略号显示
.cell-details {
  min-width: 0 !important;
  flex-shrink: 1 !important;
  
  .detail-text {
    display: block;
    width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

</style>
