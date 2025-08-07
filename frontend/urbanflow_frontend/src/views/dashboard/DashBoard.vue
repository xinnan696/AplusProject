<template>
  <div class="dashboard-page">
    <ControlHeader @toggle-nav="toggleNav"/>
    <ControlNav :isVisible="isNavVisible" />

    <div class="main-area" :class="{ 'nav-expanded': isNavVisible }">
      <div class="dashboard-container">
        <DashboardCard
          title="Congested Junction Count Trend"
          titleTooltip="This chart shows the trend in the number of congested junctions over time for the selected time range."
          class="card-full-width"
        >
          <template #filters>
            <CustomSelect
              :options="timeRangeOptions"
              v-model="topSegmentsFilters.timeRange"
              class="filter-select"
              :show-search="false"
            />
          </template>
          <template #default>
            <CongestedJunctionCountTrendChart :filters="topSegmentsFilters" />
          </template>
        </DashboardCard>

        <DashboardCard
          title="Junction Congestion Duration Ranking"
          titleTooltip="This chart ranks junctions by total congestion duration, showing the junctions with the most persistent congestion."
          class="card-full-width"
        >
          <template #filters>
            <CustomSelect
              :options="durationRankingTimeRangeOptions"
              v-model="durationRankingFilters.timeRange"
              class="filter-select"
              :show-search="false"
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
                :show-search="false"
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
                :show-search="false"
              />
            </template>
            <template #default>
              <TopCongestedTimesChart :filters="junctionCountFilters" />
            </template>
          </DashboardCard>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import ControlHeader from '@/views/control/ControlHeader.vue'
import ControlNav from '@/views/control/ControlNav.vue'
import DashboardCard from '@/views/dashboard/DashboardCard.vue'
import CustomSelect from '@/views/dashboard/CustomSelect.vue'
import TrafficFlowChart from '@/views/dashboard/TrafficFlowChart.vue'
import TopCongestedTimesChart from '@/views/dashboard/TopCongestedTimesChart.vue'
import CongestedJunctionCountTrendChart from '@/views/dashboard/CongestedJunctionCountTrendChart.vue'
import CongestionDurationRankingChart from '@/views/dashboard/CongestionDurationRankingChart.vue'

import { isNavVisible, toggleNav } from '@/utils/navState'
import { useAuthStore } from '@/stores/auth'
//import { getJunctions } from '@/mocks/mockDashboardData' // 模拟API
import { getJunctions } from '@/services/dashboard_api'

// 修改点：初始化 Store 并获取 managedAreas
const authStore = useAuthStore()
// 使用 computed 确保当 store 中的状态变化时，这里的值也能响应式更新
const managedAreas = computed(() => authStore.getManagedAreas())
console.log('managedAreas:', managedAreas.value);
//模拟测试
//const managedAreas = ['Left']

// 修改点：将 managedAreas 添加到所有 filters 对象中
// Filters State
const trafficFlowFilters = reactive({
  // 1. 将 junctionId 初始值设置为空
  junctionId: null,
  timeRange: '24hours',
  managedAreas: managedAreas.value[0]
  //managedAreas: managedAreas[0], // 模拟测试代码
})

const topSegmentsFilters = reactive({
  timeRange: '24hours',
  managedAreas: managedAreas.value[0]
  //managedAreas: managedAreas[0],
})

const junctionCountFilters = reactive({
  timeRange: '24hours',
  managedAreas: managedAreas.value[0]
  //managedAreas: managedAreas[0],
})

const durationRankingFilters = reactive({
  timeRange: '24hours',
  managedAreas: managedAreas.value[0]
  //managedAreas: managedAreas[0],
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
  // 修改点：在获取路口列表时，传入管辖区域参数
  //const junctions = await getJunctions()
  //修改后代码
  const junctions = await getJunctions({ managedAreas: managedAreas.value[0] })
  //模拟测试代码
  //const junctions = await getJunctions({ managedAreas: managedAreas[0] })

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

  //模拟
  // if (junctions && junctions.length > 0) {
  //   // 将返回列表中的第一个路口ID，设置为 trafficFlowFilters 的默认值
  //   trafficFlowFilters.junctionId = junctions[0].junction_id
  //
  //   // 使用获取到的路口列表，完整地构建下拉框的选项
  //   junctionOptions.value = junctions.map(j => ({
  //     value: j.junction_id,
  //     label: j.junction_name
  //   }))
  // }
})
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
  top: 40px;
  bottom: 0;
  overflow-y: auto;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  right: 0;

  // 定义两个变量，用于导航栏的宽度
  $nav-collapsed-width: 0.8rem; // 导航栏【收起时】的宽度，请根据您的实际情况修改
  $nav-expanded-width: 2.2rem; // 导航栏【展开时】的宽度，请根据您的实际情况修改

  // 为位移和宽度变化添加平滑的过渡动画
  transition: width 0.3s ease-out;

  // 默认状态（导航栏收起时）
  left: 0;
  //width: calc(100% - #{$nav-collapsed-width});

  // 当 `nav-expanded` 这个 class 被添加时，应用以下样式
  &.nav-expanded {
    left: $nav-expanded-width;
    width: calc(100% - #{$nav-expanded-width});
  }
}


.dashboard-container {
  width: 14.80rem; // 对应 1680px
  min-height: 10.16rem; // 对应 1016px
  display: flex;
  flex-direction: column;
  gap: 0.15rem; // 中间上下间隙 15px
  padding: 0.22rem 0; // 对应上下间隙 22px
  height: 100%;
  margin: 0;
  flex-shrink: 0;
}

.card-row {
  display: flex;
  flex-direction: row;
  gap: 0.18rem; // 中间左右间隙 18px
  flex: 1;
  min-height: 0;
}

.card-full-width {
  //height: 3.25rem; // Traffic Flow & Duration Ranking 高度
  flex-shrink: 0;
  flex: 1;
  min-height: 0;
}

.card-half-width {
  width: 50%; // Will be calculated by flex
  flex-grow: 1;
  //height: 3.25rem; // Top Congested & Count Trend 高度
}

.filter-select {
  width: 1.40rem; // 下拉栏宽度 140px
  height: 0.32rem; // 下拉栏高度 32px
}
</style>
