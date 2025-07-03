<template>
  <v-chart class="chart" :option="chartOption" autoresize />
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { getCongestedJunctionCountTrend } from '@/mocks/mockDashboardData'

use([CanvasRenderer, BarChart, TitleComponent, TooltipComponent, GridComponent]);

const props = defineProps<{
  filters: { timeRange: string }
}>()

const chartOption = ref({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    data: [],
    axisLabel: { color: '#A0A0A0', rotate: 25 },
  },
  yAxis: {
    type: 'value',
    name: 'Count',
    axisLabel: { color: '#A0A0A0' },
    splitLine: { lineStyle: { color: '#3A3A59' } },
  },
  series: [{
    name: 'Congestion Count',
    type: 'bar',
    barWidth: '60%',
    data: [],
    itemStyle: { color: '#d63384' }
  }],
})

async function fetchData() {
  const response = await getCongestedJunctionCountTrend({ time_range: props.filters.timeRange });

  if (response && response.data && response.labels) {
    chartOption.value.xAxis.data = response.labels;
    chartOption.value.series[0].data = response.data.map((d: any) => d.congestion_count);
  } else {
    chartOption.value.xAxis.data = [];
    chartOption.value.series[0].data = [];
  }
}

watch(() => props.filters, fetchData, { deep: true });
onMounted(fetchData);
</script>

<style scoped>
.chart { height: 100%; width: 100%; }
</style>
