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
import { getTrafficFlow } from '@/mocks/mockDashboardData'
//import { getTrafficFlow } from '@/services/dashboard_api'

use([CanvasRenderer, LineChart, TitleComponent, TooltipComponent, GridComponent]);

const props = defineProps<{
  filters: {
    junctionId: string
    timeRange: string
  }
}>()

const chartOption = ref({
  tooltip: {
    trigger: 'axis',
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
      // params 是一个数组，我们通常取第一个点来获取X轴信息
      let tooltipHtml = `${params[0].axisValueLabel}<br/>`;
      // 遍历所有系列的数据点
      params.forEach(item => {
        // item.marker 是颜色小圆点, item.seriesName 是系列名, item.value 是值
        tooltipHtml += `${item.seriesName}: <strong>${item.value}</strong> cars<br/>`;
      });
      return tooltipHtml;
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
    name: 'Traffic Flow',
    type: 'line',
    smooth: true,
    showSymbol: false, // 默认不显示数据点
    emphasis: {
      focus: 'series',
      // 在高亮（悬浮）时显示数据点
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
    time_range: props.filters.timeRange
  };

  const response = await getTrafficFlow(params);

  if (response && response.data && response.labels) {
    chartOption.value.xAxis.data = response.labels;
    // NOTE: Backend returns the full data object, we extract the value here.
    chartOption.value.series[0].data = response.data.map((d: any) => d.flow_rate_hourly);
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
  //   chartOption.value.series[0].data = response.data;
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
