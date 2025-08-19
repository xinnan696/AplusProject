<template>
  <div class="dashboard-page">
    <ControlHeader @toggle-nav="toggleNav" @sign-out="handleSignOut"/>
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
import { getJunctions } from '@/services/dashboard_api'

const authStore = useAuthStore()
const managedAreas = computed(() => authStore.getManagedAreas())
console.log('managedAreas:', managedAreas.value);

const trafficFlowFilters = reactive({
  junctionId: null,
  timeRange: '24hours',
  managedAreas: managedAreas.value[0]
})

const topSegmentsFilters = reactive({
  timeRange: '24hours',
  managedAreas: managedAreas.value[0]
})

const junctionCountFilters = reactive({
  timeRange: '24hours',
  managedAreas: managedAreas.value[0]
})

const durationRankingFilters = reactive({
  timeRange: '24hours',
  managedAreas: managedAreas.value[0]
})

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


onMounted(async () => {
  const junctions = await getJunctions({ managedAreas: managedAreas.value[0] })

  if (junctions && junctions.length > 0) {
    trafficFlowFilters.junctionId = junctions[0].junctionId
    junctionOptions.value = junctions.map(j => ({
      value: j.junctionId,
      label: j.junctionName
    }))
  }
})

function handleSignOut() {
  console.log('Dashboard: Handling sign out')
  authStore.logout()
}
</script>

<style scoped lang="scss">
.dashboard-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: #1E1E2F;
  z-index: 1;
}

.main-area {
  position: absolute;
  top: 0.64rem;
  bottom: 0;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  right: 0;
  $nav-collapsed-width: 0.8rem;
  $nav-expanded-width: 2.2rem;
  transition: width 0.3s ease-out;
  left: 0;
  &.nav-expanded {
    left: $nav-expanded-width;
    width: calc(100% - #{$nav-expanded-width});
  }
}


.dashboard-container {
  width: 90%;
  max-width: 14.80rem;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 0.8%;
  padding: 0.3% 0 0.8% 0;
  margin: 0;
  overflow: hidden;
}

.card-row {
  display: flex;
  flex-direction: row;
  gap: 1.2%;
  flex-shrink: 0;
  min-height: 0;
  height: 33%;
}

.card-full-width {
  height: 33%;
  flex-shrink: 0;
  min-height: 0;
}

.card-half-width {
  width: 49.4%;
  height: 100%;
  flex-grow: 0;
  flex-shrink: 0;
}

.filter-select {
  width: 1.40rem;
  height: 0.32rem;
}
</style>
