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
import { getTrafficFlow } from '@/services/dashboard_api'

use([CanvasRenderer, LineChart, TitleComponent, TooltipComponent, GridComponent]);

const props = defineProps<{
  filters: {
    junctionId: string
    timeRange: string
    managedAreas?: string | null
  }
}>()

const chartOption = ref({
  tooltip: {
    trigger: 'item',
    axisPointer: {
      type: 'none'
    },
    backgroundColor: 'rgba(20, 22, 40, 0.92)',
    borderColor: '#4a4a70',
    borderWidth: 1,
    textStyle: {
      color: '#ffffff',
      fontSize: 12,
      fontWeight: '500',
      fontFamily: "Inter, 'Segoe UI', Arial, 'Helvetica Neue', Roboto, sans-serif",
      lineHeight: 16,
    },
    extraCssText: 'box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3); border-radius: 4px;',
    formatter: function (params) {
      return `${params.name}<br/>${params.seriesName}: <strong>${params.value}</strong> cars`;
    }
    },
  grid: { top: '20px', left: '3%', right: '7%', bottom: '3%', containLabel: true },
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
    smooth: 0.4,
    showSymbol: true,
    symbolSize: 1.5,
    itemStyle: {
      color: 'transparent',
      borderColor: 'transparent'
    },
    emphasis: {
      focus: 'series',
      symbolSize: 30,
      itemStyle: {
        color: '#FFFFFF',
        borderColor: '#4D7BFF',
        borderWidth: 2,
      },
    },
    data: [],
    itemStyle: { color: '#4D7BFF' },
    areaStyle: { color: 'rgba(77, 123, 255, 0.2)' },
  }],
})

async function fetchData() {
  const params = {
    junction_id: props.filters.junctionId,
    time_range: props.filters.timeRange,
    managedAreas: props.filters.managedAreas
  };

  const response = await getTrafficFlow(params);
  console.log('Received mock response for Traffic Flow:', response);

  if (response && response.data && response.xAxisLabels && response.yAxisConfig) {
    chartOption.value.xAxis.data = response.xAxisLabels;
    chartOption.value.yAxis.min = response.yAxisConfig.min;
    chartOption.value.yAxis.max = response.yAxisConfig.max;
    chartOption.value.yAxis.interval = response.yAxisConfig.interval;
    chartOption.value.series[0].data = response.data;
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
