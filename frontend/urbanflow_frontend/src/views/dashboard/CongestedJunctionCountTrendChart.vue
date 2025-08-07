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
//import { getCongestedJunctionCountTrend } from '@/mocks/mockDashboardData'
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
    // axisPointer: {
    //   type: 'none'
    // },
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
      // let point = params[0]; // 旧的写法
      let point = params; // 新的写法
      return `
      ${point.axisValueLabel}<br/>
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
    symbolSize: 0.5,
    lineStyle: {
      color: '#28a745', // 这是您原来的折线颜色
      width: 2
    },
    itemStyle: {
      color: 'transparent',
      borderColor: 'transparent'
    },
    emphasis: {
      focus: 'series',
      symbolSize: 20,
      itemStyle: {
        color: '#FFFFFF',
        borderColor: '#28a745',
        borderWidth: 2,
      },
    },
    data: [],
    //itemStyle: { color: '#28a745' },
    areaStyle: { color: 'rgba(40, 167, 69, 0.2)' },
    markLine: {},
  }],
})

async function fetchData() {
  const response = await getCongestedJunctionCountTrend({
    time_range: props.filters.timeRange,
    managedAreas: props.filters.managedAreas
  });

  // if (response && response.data && response.labels) {
  //   chartOption.value.xAxis.data = response.labels;
  //   chartOption.value.series[0].data = response.data.map((d: any) => d.congested_junction_count);
  //   // <<< 3. 新增核心逻辑：为"24hours"视图添加信号灯修改的假数据标记
  //   if (props.filters.timeRange === '24hours' && chartOption.value.xAxis.data.length >= 2) {
  //     // (a). 获取X轴倒数第二个时间点作为标记位置
  //     const targetXAxisPoint = chartOption.value.xAxis.data[chartOption.value.xAxis.data.length - 2];
  //
  //     // (b). 配置 ECharts 标记线 (markLine)
  //     chartOption.value.series[0].markLine = {
  //       symbol: ['none', 'none'], // 线段起始点无符号，结束点为圆点
  //       symbolSize: 8,
  //       lineStyle: {
  //         color: '#FFFFFF',       // 标记线为白色
  //         type: 'dashed',
  //         width: 1.5
  //       },
  //       label: {
  //         show: true,
  //         position: 'end', // 'end' 代表在线条的末端（即顶部）
  //         formatter: 'Modified traffic light',
  //         color: '#FFFFFF',
  //         fontSize: 12,
  //         fontFamily: "Inter, 'Segoe UI', Arial, 'Helvetica Neue', Roboto, sans-serif",
  //         padding: [0, 0, 3, 0] // 底部内边距为3，将文字向上推离线条
  //       },
  //       emphasis: { // 高亮样式
  //         lineStyle: {
  //           width: 2.5
  //         },
  //         disabled: true,
  //       },
  //       silent: true,
  //       tooltip: {
  //         show: false
  //       },
  //       data: [
  //         {
  //           xAxis: targetXAxisPoint,
  //         }
  //       ]
  //     };
  //   } else {
  //     // (d). 如果不是"24hours"视图，或者数据点不够，则清空标记线，防止残留
  //     chartOption.value.series[0].markLine = { data: [] };
  //   }
  // } else {
  //   chartOption.value.xAxis.data = [];
  //   chartOption.value.series[0].data = [];
  // }

  if (response && response.data && response.xAxisLabels && response.yAxisConfig) {
    // 更新X轴标签
    chartOption.value.xAxis.data = response.xAxisLabels;

    // 更新Y轴配置
    chartOption.value.yAxis.min = response.yAxisConfig.min;
    chartOption.value.yAxis.max = response.yAxisConfig.max;
    chartOption.value.yAxis.interval = response.yAxisConfig.interval;

    // 更新图表数据
    chartOption.value.series[0].data = response.data.map((d: any) => d.congested_junction_count);

    // 为"24hours"视图添加信号灯修改的假数据标记
    if (props.filters.timeRange === '24hours' && chartOption.value.xAxis.data.length >= 2) {
      // (a). 获取X轴倒数第二个时间点作为标记位置
      const targetXAxisPoint = chartOption.value.xAxis.data[chartOption.value.xAxis.data.length - 2];

      // (b). 配置 ECharts 标记线 (markLine)
      chartOption.value.series[0].markLine = {
        symbol: ['none', 'none'], // 线段起始点无符号，结束点为圆点
        symbolSize: 8,
        lineStyle: {
          color: '#FFFFFF',       // 标记线为白色
          type: 'dashed',
          width: 1.5
        },
        label: {
          show: true,
          position: 'end', // 'end' 代表在线条的末端（即顶部）
          formatter: 'Modified traffic light',
          color: '#FFFFFF',
          fontSize: 12,
          fontFamily: "Inter, 'Segoe UI', Arial, 'Helvetica Neue', Roboto, sans-serif",
          padding: [0, 0, 3, 0] // 底部内边距为3，将文字向上推离线条
        },
        emphasis: { // 高亮样式
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
      // (d). 如果不是"24hours"视图，或者数据点不够，则清空标记线，防止残留
      chartOption.value.series[0].markLine = { data: [] };
    }

  } else {
    // 如果接口出错或返回数据不规范，清空图表
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
