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
import { graphic } from 'echarts'
import { getCongestionDurationRanking } from '@/services/dashboard_api'

use([CanvasRenderer, BarChart, TitleComponent, TooltipComponent, GridComponent]);

const props = defineProps<{
  filters: {
    timeRange: string
    managedAreas?: string | null
  }
}>()

function hexToRgb(hex: string): number[] {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return [r, g, b];
}

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
    axisPointer: { type: 'none' },
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
    formatter: (params: any) => {
      const marker = params[0].marker;
      const categoryName = params[0].name;
      const seriesName = params[0].seriesName;
      const value = params[0].value;
      return `${categoryName}<br/>${marker}${seriesName}: ${value.toFixed(1)} minutes`;
    }
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
    }
  }],
})

async function fetchData() {
  const response = await getCongestionDurationRanking({
    time_range: props.filters.timeRange,
    managedAreas: props.filters.managedAreas
  });

  if (response && response.data && response.yAxisLabels && response.xAxisConfig) {
    chartOption.value.yAxis.data = [...response.yAxisLabels].reverse();
    const dataInMinutes = response.data.map((d: any) => d.total_congestion_duration_seconds);
    const reversedData = [...dataInMinutes].reverse();
    if (reversedData.length === 0) {
      chartOption.value.series[0].data = [];
      return;
    }

    const maxValue = Math.max(...reversedData);
    const coolColor = hexToRgb('#00ACC1');
    const midColor = hexToRgb('#3949AB');
    const warmColor = hexToRgb('#9C27B0');

    const dataWithDynamicGradients = reversedData.map(value => {
      const ratio = maxValue > 0 ? value / maxValue : 0;
      const adjustedRatio = Math.sqrt(ratio);

      let endColor;
      if (adjustedRatio <= 0.5) {
        endColor = interpolateColor(coolColor, midColor, adjustedRatio / 0.5);
      } else {
        endColor = interpolateColor(midColor, warmColor, (adjustedRatio - 0.5) / 0.5);
      }

      const barGradient = new graphic.LinearGradient(0, 0, 1, 0, [
        { offset: 0, color: '#00ACC1' },
        { offset: 1, color: endColor }
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
