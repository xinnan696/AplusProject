<template>
  <v-chart class="chart" :option="chartOption" autoresize />
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { getTopCongestedSegments } from '@/mocks/mockDashboardData'
// import { getTopCongestedSegments } from '@/service/dashboard_api'

use([CanvasRenderer, LineChart, TitleComponent, TooltipComponent, GridComponent]);

const props = defineProps<{
  filters: { timeRange: string }
}>()

const chartOption = ref({
  tooltip: { trigger: 'axis',
    formatter: function (params) {
      let point = params[0];
      return `
      ${point.axisValueLabel}<br/>
      ${point.marker} ${point.seriesName}: <strong>${point.value}</strong> junctions
    `;
    }},
  grid: { top: '20px', left: '3%', right: '5%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: [],
    axisLabel: { color: '#A0A0A0' },
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: '#A0A0A0' },
    splitLine: { lineStyle: { color: '#3A3A59' } },
  },
  series: [{
    name: 'Congested Segments',
    type: 'line',
    smooth: true,
    data: [],
    itemStyle: { color: '#28a745' },
    areaStyle: { color: 'rgba(40, 167, 69, 0.2)' },
  }],
})

async function fetchData() {
  const response = await getTopCongestedSegments({ time_range: props.filters.timeRange });

  if (response && response.data && response.labels) {
    chartOption.value.xAxis.data = response.labels;
    chartOption.value.series[0].data = response.data.map((d: any) => d.congested_junction_count);
  } else {
    chartOption.value.xAxis.data = [];
    chartOption.value.series[0].data = [];
  }

  // if (response && response.data && response.xAxisLabels && response.yAxisConfig) {
  //   // 更新X轴标签
  //   chartOption.value.xAxis.data = response.xAxisLabels;
  //
  //   // 更新Y轴配置
  //   chartOption.value.yAxis.min = response.yAxisConfig.min;
  //   chartOption.value.yAxis.max = response.yAxisConfig.max;
  //   chartOption.value.yAxis.interval = response.yAxisConfig.interval;
  //
  //   // 更新图表数据
  //   chartOption.value.series[0].data = response.data.map((d: any) => d.congested_junction_count);
  // } else {
  //   // 如果接口出错或返回数据不规范，清空图表
  //   chartOption.value.xAxis.data = [];
  //   chartOption.value.series[0].data = [];
  // }
}

watch(() => props.filters, fetchData, { deep: true });
onMounted(fetchData);
</script>

<style scoped>
.chart { height: 100%; width: 100%; }
</style>
