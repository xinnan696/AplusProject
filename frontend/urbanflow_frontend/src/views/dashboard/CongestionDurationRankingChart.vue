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
//import { getCongestionDurationRanking } from '@/mocks/mockDashboardData'
import { graphic } from 'echarts'
import { getCongestionDurationRanking } from '@/services/dashboard_api'

use([CanvasRenderer, BarChart, TitleComponent, TooltipComponent, GridComponent]);

const props = defineProps<{
  filters: { timeRange: string }
}>()

const chartOption = ref({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    formatter: (params: any) => `${params[0].name}<br/>${params[0].seriesName}: ${params[0].value.toFixed(1)} minutes`
  },
  grid: { top: '20px', left: '3%', right: '7%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'value',
    name: 'Minutes',
    nameTextStyle: { color: '#A0A0A0' },
    boundaryGap: [0, 0.01],
    axisLabel:
      { color: '#A0A0A0',
        formatter: function (value) {
          return Math.round(value);
        }
      },
    splitLine: { lineStyle: { color: '#3A3A59' } },
  },
  yAxis: {
    type: 'category',
    data: [],
    axisLabel: { color: '#A0A0A0' },
  },
  series: [{
    name: 'Congestion Duration',
    type: 'bar',
    data: [],
    itemStyle: {
      borderRadius: [0, 5, 5, 0],
      color: new graphic.LinearGradient(1, 0, 0, 0, [
        { offset: 0, color: '#FFC107' },
        { offset: 0.5, color: '#6610F2' },
        { offset: 1, color: '#0D6EFD' }
      ])
    }
  }],
})

async function fetchData() {
  const response = await getCongestionDurationRanking({ time_range: props.filters.timeRange });

  // if (response && response.data && response.labels) {
  //   // For horizontal bar chart, reverse the data so the highest value is at the top
  //   chartOption.value.yAxis.data = [...response.labels].reverse();
  //   const dataInMinutes = response.data.map((d: any) => d.total_congestion_duration_seconds / 60);
  //   chartOption.value.series[0].data = [...dataInMinutes].reverse();
  // } else {
  //   chartOption.value.yAxis.data = [];
  //   chartOption.value.series[0].data = [];
  // }

  if (response && response.data && response.yAxisLabels && response.xAxisConfig) {

    // 更新Y轴标签
    chartOption.value.yAxis.data = [...response.yAxisLabels].reverse();
    //chartOption.value.yAxis.data = response.yAxisLabels;

    // 更新X轴配置
    chartOption.value.xAxis.min = response.xAxisConfig.min;
    chartOption.value.xAxis.max = response.xAxisConfig.max;
    chartOption.value.xAxis.interval = response.xAxisConfig.interval;

    // 更新图表数据
    const dataInMinutes = response.data.map((d: any) => d.total_congestion_duration_seconds);
    const reversedData = [...dataInMinutes].reverse();
    chartOption.value.series[0].data = reversedData;
    //chartOption.value.series[0].data = response.data.map((d: any) => d.total_congestion_duration_seconds);
  } else {
    // 如果接口出错或返回数据不规范，清空图表
    chartOption.value.yAxis.data = [];
    chartOption.value.series[0].data = [];
  }
}

watch(() => props.filters, fetchData, { deep: true });
onMounted(fetchData);
</script>

<style scoped>
.chart { height: 100%; width: 100%; }
</style>
