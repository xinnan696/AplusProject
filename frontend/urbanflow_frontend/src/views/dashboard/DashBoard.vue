<template>
  <div class="dashboard-page">
    <ControlHeader 
      :isRecordPanelVisible="isRecordVisible"
      @toggle-nav="toggleNav" 
      @toggle-record="toggleRecord"
      @sign-out="handleSignOut"
    />
    <ControlNav :isVisible="isNavVisible" />

    <div class="main-area" :class="{ 'nav-expanded': isNavVisible }">
      <div class="dashboard-container">
        <DashboardCard
          title="Congested Junction Count Trend"
          titleTooltip="This chart shows the trend in the number of congested junctions over time for the selected time range."
          class="card-third-height"
        >
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

        <DashboardCard
          title="Junction Congestion Duration Ranking"
          titleTooltip="This chart ranks junctions by total congestion duration, showing the junctions with the most persistent congestion."
          class="card-third-height"
        >
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

        <div class="card-row">
          <DashboardCard
            title="Traffic Flow"
            titleTooltip="This chart shows traffic flow of selected junctions or this city for the selected time range."
            class="card-half-width"
          >
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
              <TrafficFlowChart
                v-if="trafficFlowFilters.junctionId"
                :filters="trafficFlowFilters" />
            </template>
          </DashboardCard>

          <DashboardCard
            title="Top Congested Times"
            titleTooltip="This chart shows the junctions with the top-ranking number of congestion events in the selected time range."
            class="card-half-width"
          >
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
      </div>
    </div>

    <!-- Record Panel -->
    <ControlRecord :isVisible="isRecordVisible" @close="toggleRecord" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
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
import { getJunctions } from '@/services/dashboard_api'

const router = useRouter()
const authStore = useAuthStore()

// UI State
const isRecordVisible = ref(false)

// Filters State
const trafficFlowFilters = reactive({
  // 1. 将 junctionId 初始值设置为空
  junctionId: null,
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

// Filter Options
// 2. 将 junctionOptions 初始值设置为空数组
const junctionOptions = ref([])

const timeRangeOptions = ref([
  { value: '24hours', label: '24 hours' },
  { value: 'oneweek', label: 'One week' },
  { value: 'onemonth', 'label': 'One month' },
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

// Fetch initial data for filters
onMounted(async () => {
  const junctions = await getJunctions()

  // 3. 核心逻辑：获取数据后，设置默认值并填充选项
  if (junctions && junctions.length > 0) {
    // 将返回列表中的第一个路口ID，设置为 trafficFlowFilters 的默认值
    trafficFlowFilters.junctionId = junctions[0].junctionId

    // 使用获取到的路口列表，完整地构建下拉框的选项
    junctionOptions.value = junctions.map(j => ({
      value: j.junctionId,
      label: j.junctionName
    }))
  }
})

// Event Handlers
const toggleRecord = () => {
  isRecordVisible.value = !isRecordVisible.value
}

const handleSignOut = () => {
  console.log('🚪 [Dashboard] Signing out...')
  authStore.logout()
}
</script>

<style scoped lang="scss">
// 确保在全局CSS中设置了合适的根字体大小，以便rem单位生效
// 例如: html { font-size: 100px; } 这样 1rem = 100px
.dashboard-page {
  //position: fixed;
  position: relative;
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
  //height: calc(100% - 64px); // 假设Header高度为64px
  //display: flex;
  //overflow-y: auto;
  //overflow-x: hidden;
  //padding: 0 1.01rem; // 对应左右间隙 101px
  //justify-content: center;

  position: absolute;
  top: 40px; // 假设Header高度为64px
  bottom: 0;
  overflow: hidden; // 改为hidden，不允许滚动
  display: flex;
  justify-content: center;

  // 定义两个变量，用于导航栏的宽度
  $nav-collapsed-width: 0.8rem; // 导航栏【收起时】的宽度，请根据您的实际情况修改
  $nav-expanded-width: 1.0rem; // 导航栏【展开时】的宽度，请根据您的实际情况修改

  // 为位移和宽度变化添加平滑的过渡动画
  transition: left 0.3s ease-in-out, width 0.3s ease-in-out;

  // 默认状态（导航栏收起时）
  left: $nav-collapsed-width;
  width: calc(100% - #{$nav-collapsed-width});

  // 当 `nav-expanded` 这个 class 被添加时，应用以下样式
  &.nav-expanded {
    left: $nav-expanded-width;
    width: calc(100% - #{$nav-expanded-width});
  }
}

.dashboard-container {
  width: 14.80rem; // 对应 1680px
  height: 100%; // 占满父容器高度
  display: flex;
  flex-direction: column;
  gap: 0.15rem; // 中间上下间隙 15px
  padding: 0.22rem 0; // 对应上下间隙 22px
  box-sizing: border-box; // 确保padding不会撑大容器
}

.card-row {
  display: flex;
  flex-direction: row;
  gap: 0.18rem; // 中间左右间隙 18px
  height: calc(33.33% - 0.1rem); // 三分之一高度，减去gap的影响
}

// 替换原来的 .card-full-width
.card-third-height {
  height: calc(33.33% - 0.1rem); // 三分之一高度，减去gap的影响
  flex-shrink: 0;
}

.card-half-width {
  width: 50%; // Will be calculated by flex
  flex-grow: 1;
  height: 100%; // 占满父容器(.card-row)的高度
}

.filter-select {
  width: 1.40rem; // 下拉栏宽度 140px
  height: 0.32rem; // 下拉栏高度 32px
}
</style>
