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
//import { getTrafficFlow } from '@/mocks/mockDashboardData'
import { getTrafficFlow } from '@/services/dashboard_api.ts'

use([CanvasRenderer, LineChart, TitleComponent, TooltipComponent, GridComponent]);

const props = defineProps<{
  filters: {
    junctionId: string
    timeRange: string
  }
}>()

const chartOption = ref({
  tooltip: { trigger: 'axis',
    formatter: function (params) {
      let tooltipHtml = `${params[0].axisValueLabel}<br/>`;


      params.forEach(item => {

        tooltipHtml += `${item.marker} ${item.seriesName}: <strong>${item.value}</strong> cars<br/>`;
      });

      return tooltipHtml;
    }},
  grid: { top: '20px', left: '3%', right: '4%', bottom: '3%', containLabel: true },
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
    name: 'Traffic Flow',
    type: 'line',
    smooth: true,
    data: [],
    itemStyle: { color: '#4D7BFF' },
    areaStyle: { color: 'rgba(77, 123, 255, 0.2)' },
  }],
})

async function fetchData() {
  const params = {
    junction_id: props.filters.junctionId === 'total_city' ? undefined : props.filters.junctionId,
    time_range: props.filters.timeRange
  };

  const response = await getTrafficFlow(params);

  if (response && response.data && response.xAxisLabels && response.yAxisConfig) {

    chartOption.value.xAxis.data = response.xAxisLabels;


    chartOption.value.yAxis.min = response.yAxisConfig.min;
    chartOption.value.yAxis.max = response.yAxisConfig.max;
    chartOption.value.yAxis.interval = response.yAxisConfig.interval;


    chartOption.value.series[0].data = response.data.map((d: any) => d.flowRateHourly);
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
