<template>
  <div class="dashboard-page">
    <ControlHeader @toggle-nav="toggleNav" />
    <ControlNav :isVisible="isNavVisible" />

    <div class="main-area">
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
          <DashboardCard title="Top Congested Segments" class="card-half-width">
            <template #filters>
              <CustomSelect
                :options="timeRangeOptions"
                v-model="topSegmentsFilters.timeRange"
                class="filter-select"
              />
            </template>
            <template #default>
              <TopCongestedSegmentsChart :filters="topSegmentsFilters" />
            </template>
          </DashboardCard>

          <DashboardCard title="Congested Junction Count Trend" class="card-half-width">
            <template #filters>
              <CustomSelect
                :options="timeRangeOptions"
                v-model="junctionCountFilters.timeRange"
                class="filter-select"
              />
            </template>
            <template #default>
              <CongestedJunctionCountTrendChart :filters="junctionCountFilters" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import ControlHeader from '@/views/control/ControlHeader.vue'
import ControlNav from '@/views/control/ControlNav.vue'
import DashboardCard from '@/views/dashboard/DashboardCard.vue'
import CustomSelect from '@/views/dashboard/CustomSelect.vue'
import TrafficFlowChart from '@/views/dashboard/TrafficFlowChart.vue'
import TopCongestedSegmentsChart from '@/views/dashboard/TopCongestedSegmentsChart.vue'
import CongestedJunctionCountTrendChart from '@/views/dashboard/CongestedJunctionCountTrendChart.vue'
import CongestionDurationRankingChart from '@/views/dashboard/CongestionDurationRankingChart.vue'

import { isNavVisible, toggleNav } from '@/utils/navState'
import { getJunctions } from '@/mocks/mockDashboardData' // 模拟API

// Filters State
const trafficFlowFilters = reactive({
  junctionId: 'total_city',
  timeRange: '24 hours',
})

const topSegmentsFilters = reactive({
  timeRange: '24 hours',
})

const junctionCountFilters = reactive({
  timeRange: '24 hours',
})

const durationRankingFilters = reactive({
  timeRange: '24 hours',
})

// Filter Options
const junctionOptions = ref([
  { value: 'total_city', label: 'Total City' },
])

const timeRangeOptions = ref([
  { value: '24 hours', label: '24 hours' },
  { value: 'one week', label: 'One week' },
  { value: 'one month', label: 'One month' },
  { value: 'six months', label: 'Six months' },
  { value: 'one year', label: 'One year' },
])

const durationRankingTimeRangeOptions = ref([
  { value: '24 hours', label: '24 hours' },
  { value: 'one month', label: 'One month' },
  { value: 'three months', label: 'Three months' },
  { value: 'six months', label: 'Six months' },
  { value: 'one year', label: 'One year' },
])

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
// 确保在全局CSS中设置了合适的根字体大小，以便rem单位生效
// 例如: html { font-size: 100px; } 这样 1rem = 100px
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
  height: calc(100% - 64px); // 假设Header高度为64px
  display: flex;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0 1.01rem; // 对应左右间隙 101px
  justify-content: center;
}

.dashboard-container {
  width: 16.80rem; // 对应 1680px
  min-height: 10.16rem; // 对应 1016px
  display: flex;
  flex-direction: column;
  gap: 0.15rem; // 中间上下间隙 15px
  padding: 0.22rem 0; // 对应上下间隙 22px
}

.card-row {
  display: flex;
  flex-direction: row;
  gap: 0.18rem; // 中间左右间隙 18px
}

.card-full-width {
  height: 3.25rem; // Traffic Flow & Duration Ranking 高度
}

.card-half-width {
  width: 50%; // Will be calculated by flex
  flex-grow: 1;
  height: 2.92rem; // Top Congested & Count Trend 高度
}

.filter-select {
  width: 1.40rem; // 下拉栏宽度 140px
  height: 0.32rem; // 下拉栏高度 32px
}
</style>
