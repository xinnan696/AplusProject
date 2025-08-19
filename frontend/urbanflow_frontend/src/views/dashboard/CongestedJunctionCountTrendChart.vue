<template>
  <v-chart
    class="chart"
    :option="chartOption"
    autoresize
    renderer="svg"
  />
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent, MarkLineComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { getCongestedJunctionCountTrend } from '@/services/dashboard_api.ts'

use([CanvasRenderer, LineChart, TitleComponent, TooltipComponent, GridComponent, MarkLineComponent]);

const props = defineProps<{
  filters: {
    timeRange: string
    managedAreas?: string | null
  }
}>()

const chartOption = ref({
  tooltip: {
    trigger: 'item',
    backgroundColor: 'rgba(20, 22, 40, 0.92)',
    borderColor: '#4a4a70',
    borderWidth: 1,
    padding: [8, 12],
    textStyle: {
      color: '#ffffff',
      fontSize: 12,
      fontWeight: '500',
      fontFamily: "Inter, 'Segoe UI', Arial, 'Helvetica Neue', Roboto, sans-serif",
      lineHeight: 16,
    },
    extraCssText: 'box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3); border-radius: 4px;',
    formatter: function (params) {
      const point = params;
      return `
      ${point.name}<br/>
      ${point.seriesName}: <strong>${point.value}</strong> junctions
    `;
    }},
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
    name: 'Congested',
    type: 'line',
    smooth: 0.4,
    showSymbol: true,
    symbolSize: 1.5,
    lineStyle: {
      color: '#28a745',
      width: 2
    },
    itemStyle: {
      color: 'transparent',
      borderColor: 'transparent'
    },
    emphasis: {
      focus: 'series',
      symbolSize: 30,
      itemStyle: {
        color: '#FFFFFF',
        borderColor: '#28a745',
        borderWidth: 2,
      },
    },
    data: [],
    areaStyle: { color: 'rgba(40, 167, 69, 0.2)' },
    markLine: {},
  }],
})

async function fetchData() {
  const response = await getCongestedJunctionCountTrend({
    time_range: props.filters.timeRange,
    managedAreas: props.filters.managedAreas
  });

  if (response && response.data && response.xAxisLabels && response.yAxisConfig) {
    chartOption.value.xAxis.data = response.xAxisLabels;
    chartOption.value.yAxis.min = response.yAxisConfig.min;
    chartOption.value.yAxis.max = response.yAxisConfig.max;
    chartOption.value.yAxis.interval = response.yAxisConfig.interval;
    chartOption.value.series[0].data = response.data.map((d: any) => d.congested_junction_count);

    if (props.filters.timeRange === '24hours' && chartOption.value.xAxis.data.length >= 2) {
      const targetXAxisPoint = chartOption.value.xAxis.data[chartOption.value.xAxis.data.length - 3];
      chartOption.value.series[0].markLine = {
        symbol: ['none', 'none'],
        symbolSize: 8,
        lineStyle: {
          color: '#FFFFFF',
          type: 'dashed',
          width: 1.5
        },
        label: {
          show: true,
          position: 'end',
          formatter: 'Modified traffic light',
          color: '#FFFFFF',
          fontSize: 12,
          fontFamily: "Inter, 'Segoe UI', Arial, 'Helvetica Neue', Roboto, sans-serif",
          padding: [0, 0, 3, 0]
        },
        emphasis: {
          lineStyle: {
            width: 2.5
          },
          disabled: true,
        },
        silent: true,
        tooltip: {
          show: false
        },
        data: [
          {
            xAxis: targetXAxisPoint,
          }
        ]
      };
    } else {
      chartOption.value.series[0].markLine = { data: [] };
    }

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
