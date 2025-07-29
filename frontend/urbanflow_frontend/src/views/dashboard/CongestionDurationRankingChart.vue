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
  filters: {
    timeRange: string
    managedAreas?: string | null
  }
}>()

/**
 * 将HEX颜色字符串转换为RGB数组
 * @param hex 例如 '#FFC107'
 * @returns [255, 193, 7]
 */
function hexToRgb(hex: string): number[] {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return [r, g, b];
}

/**
 * 在两个RGB颜色之间进行线性插值
 * @param color1 起始颜色 [r, g, b]
 * @param color2 结束颜色 [r, g, b]
 * @param factor 插值因子 (0 到 1)
 * @returns 'rgb(r,g,b)' 格式的颜色字符串
 */
function interpolateColor(color1: number[], color2: number[], factor: number) {
  const result = color1.slice();
  for (let i = 0; i < 3; i++) {
    result[i] = Math.round(result[i] + factor * (color2[i] - result[i]));
  }
  return `rgb(${result[0]}, ${result[1]}, ${result[2]})`;
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
      // color: new graphic.LinearGradient(
      //   0, 0, 1, 0, // (x0, y0, x1, y1) -> 从左到右渐变
      //   [
      //     { offset: 0, color: '#43E695' },     // 青绿色
      //     { offset: 0.5, color: '#3A7FD5' },    // 科技蓝
      //     { offset: 1, color: '#D264E3' }      // 淡雅紫
      //   ]
      // )
    }
  }],
})

async function fetchData() {
  const response = await getCongestionDurationRanking({
    time_range: props.filters.timeRange,
    managedAreas: props.filters.managedAreas
  });

  // if (response && response.data && response.labels) {
  //   // For horizontal bar chart, reverse the data so the highest value is at the top
  //   chartOption.value.yAxis.data = [...response.labels].reverse();
  //   const dataInMinutes = response.data.map((d: any) => d.total_congestion_duration_seconds / 60);
  //   const reversedData = [...dataInMinutes].reverse();
  //
  //   //--- CHANGE: 新的颜色计算逻辑 ---
  //   if (reversedData.length === 0) {
  //     chartOption.value.series[0].data = [];
  //     return;
  //   }
  //
  //   const maxValue = Math.max(...reversedData);
  //   const coolColor = hexToRgb('#00ACC1');
  //   const midColor = hexToRgb('#3949AB');
  //   const warmColor = hexToRgb('#9C27B0');
  //
  //   const dataWithDynamicGradients = reversedData.map(value => {
  //     const ratio = maxValue > 0 ? value / maxValue : 0;
  //     const adjustedRatio = Math.sqrt(ratio);
  //
  //     // 1. 计算当前柱子渐变的【终点颜色】
  //     let endColor;
  //     if (adjustedRatio <= 0.5) {
  //       endColor = interpolateColor(coolColor, midColor, adjustedRatio / 0.5);
  //     } else {
  //       endColor = interpolateColor(midColor, warmColor, (adjustedRatio - 0.5) / 0.5);
  //     }
  //
  //     // 2. 为当前柱子生成一个专属的、从固定起点到动态终点的渐变对象
  //     const barGradient = new graphic.LinearGradient(0, 0, 1, 0, [
  //       { offset: 0, color: '#00ACC1' }, // 所有柱子的渐变起点色固定为冷色
  //       { offset: 1, color: endColor }     // 渐变终点色是动态计算出来的
  //     ]);
  //
  //     // 3. 返回包含值和专属样式的数据对象
  //     return {
  //       value: value,
  //       itemStyle: {
  //         color: barGradient
  //       }
  //     };
  //   });
  //
  //   chartOption.value.series[0].data = dataWithDynamicGradients;
  //   // --- END ---
  //
  // } else {
  //   chartOption.value.yAxis.data = [];
  //   chartOption.value.series[0].data = [];
  // }

  if (response && response.data && response.labels) {
    // 对于横向条形图，反转数据使最大值显示在顶部
    chartOption.value.yAxis.data = [...response.labels].reverse();
    // 假设 API 返回 'total_congestion_duration_seconds'，将其转换为分钟。
    const dataInMinutes = response.data.map((d: any) => d.total_congestion_duration_seconds);
    const reversedData = [...dataInMinutes].reverse();

    // 根据要求，保留了动态颜色计算逻辑。
    if (reversedData.length === 0) {
      chartOption.value.series[0].data = [];
      return;
    }

    const maxValue = Math.max(...reversedData);
    const coolColor = hexToRgb('#00ACC1'); // 青色
    const midColor = hexToRgb('#3949AB');  // 靛蓝
    const warmColor = hexToRgb('#9C27B0'); // 紫色

    const dataWithDynamicGradients = reversedData.map(value => {
      const ratio = maxValue > 0 ? value / maxValue : 0;
      const adjustedRatio = Math.sqrt(ratio); // 使用平方根使颜色变化更明显

      let endColor;
      if (adjustedRatio <= 0.5) {
        endColor = interpolateColor(coolColor, midColor, adjustedRatio / 0.5);
      } else {
        endColor = interpolateColor(midColor, warmColor, (adjustedRatio - 0.5) / 0.5);
      }

      const barGradient = new graphic.LinearGradient(0, 0, 1, 0, [
        { offset: 0, color: '#00ACC1' }, // 渐变起始色是固定的
        { offset: 1, color: endColor }   // 渐变终点色是动态计算的
      ]);

      return {
        value: value,
        itemStyle: {
          color: barGradient
        }
      };
    });

    chartOption.value.series[0].data = dataWithDynamicGradients;

  } else {
    // 如果 API 返回错误或格式不正确的数据，则清空图表。
    // 你的 API 服务中的 catch 块会返回一个 errorResponse，从而触发这里的逻辑。
    chartOption.value.yAxis.data = [];
    chartOption.value.series[0].data = [];
  }

  // if (response && response.data && response.yAxisLabels && response.xAxisConfig) {
  //
  //   // 更新Y轴标签
  //   chartOption.value.yAxis.data = [...response.yAxisLabels].reverse();
  //   //chartOption.value.yAxis.data = response.yAxisLabels;
  //
  //   // 更新X轴配置
  //   chartOption.value.xAxis.min = response.xAxisConfig.min;
  //   chartOption.value.xAxis.max = response.xAxisConfig.max;
  //   chartOption.value.xAxis.interval = response.xAxisConfig.interval;
  //
  //   // 更新图表数据
  //   const dataInMinutes = response.data.map((d: any) => d.total_congestion_duration_seconds);
  //   const reversedData = [...dataInMinutes].reverse();
  //   chartOption.value.series[0].data = reversedData;
  //   //chartOption.value.series[0].data = response.data.map((d: any) => d.total_congestion_duration_seconds);
  // } else {
  //   // 如果接口出错或返回数据不规范，清空图表
  //   chartOption.value.yAxis.data = [];
  //   chartOption.value.series[0].data = [];
  // }
}

watch(() => props.filters, fetchData, { deep: true });
onMounted(fetchData);
</script>

<style scoped>
.chart { height: 100%; width: 100%; }
</style>
