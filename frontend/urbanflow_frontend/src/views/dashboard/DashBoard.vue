<template>
  <div class="dashboard-page">
    <ControlHeader 
      @toggle-nav="toggleNav"
      @toggle-record="toggleRecord"
      @toggle-emergency="toggleEmergency"
      @toggle-priority="togglePriority"
      @mode-changed="handleModeChange"
      @sign-out="handleSignOut"
    />
    <ControlNav :isVisible="isNavVisible" />

    <div class="main-area" :class="{ 'nav-expanded': isNavVisible }">
      <div class="dashboard-container">
        <DashboardCard title="Traffic Flow" class="card-full-width">
          <template #filters>
            <CustomSelect
              :options="junctionOptions"
              v-model="trafficFlowFilters.junctionId"
              class="filter-select"
            />
            <CustomSelect
              :options="timeRangeOptions"
              v-model="trafficFlowFilters.timeRange"
              class="filter-select"
            />
          </template>
          <template #default>
            <TrafficFlowChart :filters="trafficFlowFilters" />
          </template>
        </DashboardCard>

        <div class="card-row">
          <DashboardCard title="Congested Junction Count Trend" class="card-half-width">
            <template #filters>
              <CustomSelect
                :options="timeRangeOptions"
                v-model="topSegmentsFilters.timeRange"
                class="filter-select"
              />
            </template>
            <template #default>
              <CongestedJunctionCountTrendChart :filters="topSegmentsFilters" />
            </template>
          </DashboardCard>

          <DashboardCard title="Top Congested Times" class="card-half-width">
            <template #filters>
              <CustomSelect
                :options="timeRangeOptions"
                v-model="junctionCountFilters.timeRange"
                class="filter-select"
              />
            </template>
            <template #default>
              <TopCongestedTimesChart :filters="junctionCountFilters" />
            </template>
          </DashboardCard>
        </div>

        <DashboardCard title="Junction Congestion Duration Ranking" class="card-full-width">
          <template #filters>
            <CustomSelect
              :options="durationRankingTimeRangeOptions"
              v-model="durationRankingFilters.timeRange"
              class="filter-select"
            />
          </template>
          <template #default>
            <CongestionDurationRankingChart :filters="durationRankingFilters" />
          </template>
        </DashboardCard>
      </div>
    </div>

    <!-- Record Panel -->
    <ControlRecord :isVisible="isRecordVisible" @close="toggleRecord" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import ControlHeader from '@/views/control/ControlHeader.vue'
import ControlNav from '@/views/control/ControlNav.vue'
import ControlRecord from '@/views/control/ControlRecord.vue'
import DashboardCard from '@/views/dashboard/DashboardCard.vue'
import CustomSelect from '@/views/dashboard/CustomSelect.vue'
import TrafficFlowChart from '@/views/dashboard/TrafficFlowChart.vue'
import TopCongestedTimesChart from '@/views/dashboard/TopCongestedTimesChart.vue'
import CongestedJunctionCountTrendChart from '@/views/dashboard/CongestedJunctionCountTrendChart.vue'
import CongestionDurationRankingChart from '@/views/dashboard/CongestionDurationRankingChart.vue'

import { isNavVisible, toggleNav } from '@/utils/navState'
//import { getJunctions } from '@/mocks/mockDashboardData' // 模拟API
import { getJunctions } from '@/services/dashboard_api.ts'

const router = useRouter()

// Filters State
const trafficFlowFilters = reactive({
  junctionId: 'total_city',
  timeRange: '24hours',
})

const topSegmentsFilters = reactive({
  timeRange: '24hours',
})

const junctionCountFilters = reactive({
  timeRange: '24hours',
})

const durationRankingFilters = reactive({
  timeRange: '24hours',
})

// Panel states  
const isRecordVisible = ref(false)
const isEmergencyVisible = ref(false)
const isPriorityVisible = ref(false)

// Filter Options
const junctionOptions = ref([
  { value: 'total_city', label: 'Total City' },
])

const timeRangeOptions = ref([
  { value: '24hours', label: '24 hours' },
  { value: 'oneweek', label: 'One week' },
  { value: 'onemonth', label: 'One month' },
  { value: 'sixmonths', label: 'Six months' },
  { value: 'oneyear', label: 'One year' },
])

const durationRankingTimeRangeOptions = ref([
  { value: '24hours', label: '24 hours' },
  { value: 'onemonth', label: 'One month' },
  { value: 'threemonths', label: 'Three months' },
  { value: 'sixmonths', label: 'Six months' },
  { value: 'oneyear', label: 'One year' },
])

// Handle sign out
const handleSignOut = () => {
  // Remove authentication token
  localStorage.removeItem('authToken')
  
  // Redirect to login page
  router.push({ name: 'Login' })
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

// Fetch initial data for filters
onMounted(async () => {
  const junctions = await getJunctions()
  junctionOptions.value = [
    { value: 'total_city', label: 'Total City' },
    ...junctions.map(j => ({ value: j.junction_id, label: j.junction_name })),
  ]
})
</script>

<style scoped lang="scss">
.dashboard-page {
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
  position: absolute;
  top: 64px; // Header 高度
  bottom: 0;
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  justify-content: center;

  // 导航栏动画过渡
  transition: left 0.3s ease-in-out, width 0.3s ease-in-out;

  // 默认状态（导航栏收起时）
  left: 0;
  width: 100%;

  // 导航栏展开时
  &.nav-expanded {
    left: 240px; // 导航栏宽度
    width: calc(100% - 240px);
  }
}

.dashboard-container {
  width: 100%; // 使用百分比宽度
  max-width: 1400px; // 最大宽度限制
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 1.5%; // 卡片间隙用百分比
  padding: 2% 0; // 上下间距用百分比
  box-sizing: border-box;
}

.card-row {
  display: flex;
  flex-direction: row;
  gap: 1.8%; // 左右间隙用百分比
  height: 33%; // 中间行高度
}

.card-full-width {
  width: 100%;
  height: 33%; // Traffic Flow 卡片高度
  flex-shrink: 0;
}

.card-half-width {
  width: 49.1%; // 每个卡片占一半宽度（减去gap）
  height: 100%; // 继承父元素高度
  flex-shrink: 0;
}

.filter-select {
  width: 140px; // 保持固定像素宽度
  height: 32px; // 保持固定像素高度
}
</style>
