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
//import { getTopCongestedTimes } from '@/mocks/mockDashboardData'
import {getTopCongestedTimes} from '@/services/dashboard_api'

use([CanvasRenderer, BarChart, TitleComponent, TooltipComponent, GridComponent]);

const props = defineProps<{
  filters: {
    timeRange: string
    managedAreas?: string | null
  }
}>()

const gradientColors = ref<string[]>([]);

/**
 * 生成颜色渐变数组
 * @param startColor 起始颜色
 * @param endColor 结束颜色
 * @param steps 渐变的步数 (柱子的数量)
 * @returns 返回一个包含HEX颜色字符串的数组
 */
function generateGradientColors(startColor: string, endColor: string, steps: number): string[] {
  // NEW: 处理 steps 为 0 或 1 的边界情况
  if (steps === 0) {
    return [];
  }
  if (steps === 1) {
    return [startColor];
  }

  const startRGB = parseInt(startColor.slice(1), 16);
  const startR = (startRGB >> 16) & 255;
  const startG = (startRGB >> 8) & 255;
  const startB = startRGB & 255;

  const endRGB = parseInt(endColor.slice(1), 16);
  const endR = (endRGB >> 16) & 255;
  const endG = (endRGB >> 8) & 255;
  const endB = endRGB & 255;

  const colors: string[] = [];
  // i < steps 保证了循环次数正确
  for (let i = 0; i < steps; i++) {
    // 分母 steps - 1 在 steps > 1 时是安全的
    const ratio = i / (steps - 1);
    const r = Math.round(startR + (endR - startR) * ratio);
    const g = Math.round(startG + (endG - startG) * ratio);
    const b = Math.round(startB + (endB - startB) * ratio);
    colors.push(`rgb(${r},${g},${b})`);
  }
  return colors;
}

const chartOption = ref({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
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
  },
  grid: { top: '20px', left: '3%', right: '4%', bottom: '1%', containLabel: true },
  xAxis: {
    type: 'category',
    data: [],
    fontSize: 14,
    axisLabel: { color: '#A0A0A0', rotate: 0,interval: 0,
      formatter: function (value) {
        const maxLength = 8; // 设置最大显示长度
        if (value.length > maxLength) {
          return value.substring(0, maxLength) + '...';
        }
        return value;
      }},

  },
  yAxis: {
    type: 'value',
    axisLabel: { color: '#A0A0A0' },
    fontSize: 14,
    splitLine: { lineStyle: { color: '#3A3A59' } },
  },
  series: [{
    name: 'Congested Times',
    type: 'bar',
    barWidth: '60%',
    data: [],
    itemStyle: {
      borderRadius: [4, 4, 0, 0],
      color: (params: any) => {
        return gradientColors.value[params.dataIndex] || '#ccc';
      }
    }
  }],
});

const allLabels = ref<string[]>([]);

async function fetchData() {
  const response = await getTopCongestedTimes({
    time_range: props.filters.timeRange,
    managedAreas: props.filters.managedAreas
  });

  // if (response && response.data && response.labels) {
  //   const startColor = '#6a11cb';
  //   const endColor = '#2af598';
  //   gradientColors.value = generateGradientColors(startColor, endColor, response.data.length);
  //
  //   chartOption.value.xAxis.data = response.labels;
  //   chartOption.value.series[0].data = response.data.map((d: any) => d.congestion_count);
  // } else {
  //   chartOption.value.xAxis.data = [];
  //   chartOption.value.series[0].data = [];
  // }

  if (response && response.data && response.xAxisLabels && response.yAxisConfig) {
    // 更新X轴标签

    const startColor = '#6a11cb';
    const endColor = '#2af598';
    gradientColors.value = generateGradientColors(startColor, endColor, response.data.length);
    allLabels.value = response.xAxisLabels;
    chartOption.value.xAxis.data = response.xAxisLabels;

    // 更新Y轴配置
    chartOption.value.yAxis.min = response.yAxisConfig.min;
    chartOption.value.yAxis.max = response.yAxisConfig.max;
    chartOption.value.yAxis.interval = response.yAxisConfig.interval;

    // 更新图表数据
    chartOption.value.series[0].data = response.data.map((d: any) => d.congestion_count);
  } else {
    // 如果接口出错或返回数据不规范，清空图表
    chartOption.value.xAxis.data = [];
    chartOption.value.series[0].data = [];
  }
}

watch(() => props.filters, fetchData, { deep: true });
onMounted(fetchData);
</script>
