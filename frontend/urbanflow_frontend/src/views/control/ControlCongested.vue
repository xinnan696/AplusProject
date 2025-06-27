<template>
  <div class="congested-box">
    <div class="title">Congested Roads</div>

    <div class="table-header">
      <span class="col location">Location</span>
      <span class="col queue">Queue Length</span>
    </div>

    <div class="table-body">
      <div
        class="table-row"
        v-for="(item, index) in congestedData"
        :key="index"
      >
        <span
            class="col location"
            :class="getLocationClass(index)"
            @click="handleLocationClick(item.j)"
            style="cursor: pointer;"
>
            {{ junctionNameMap[item.j] || item.j }}
        </span>
        <span class="col queue" :class="getQueueClass(item.q)">
          {{ item.q }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
const emit = defineEmits<{
  (e: 'select-junction-by-name', name: string): void
}>()

const handleLocationClick = (junctionId: string) => {
  const name = junctionNameMap.value[junctionId] || junctionId
  emit('select-junction-by-name', name)
}
interface CongestedItem {
  j: string
  q: number
}

const congestedData = ref<CongestedItem[]>([])
const junctionNameMap = ref<Record<string, string>>({})

let socket: WebSocket | null = null

onMounted(async () => {

  try {
    const response = await fetch('/api-status/junctions')
    const data = await response.json()

    const map: Record<string, string> = {}
    for (const key in data) {
      const j = data[key]
      map[j.junction_id] = j.junction_name
    }

    junctionNameMap.value = map
  } catch (err) {
    console.error(err)
  }

  // 启动 WebSocket
  socket = new WebSocket('ws://localhost:8087/api/status/ws')

  socket.onopen = () => {
    console.log('WebSocket connected.')
  }

  socket.onmessage = (event) => {
  try {
    const data = JSON.parse(event.data)
    if (data.congested) {
      // 用假数据替换 q 值
      const fakeQs = [15, 11, 10, 8, 6, 6]

      const modified = data.congested.map((item: { j: string }, idx: number) => ({
        j: item.j,
        q: fakeQs[idx % fakeQs.length] // 循环分配假数据
      }))

      congestedData.value = modified
    }
  } catch (e) {
    console.error('WebSocket JSON parse error:', e)
  }
}

  socket.onerror = (err) => {
    console.error('WebSocket error:', err)
  }

  socket.onclose = () => {
    console.log('WebSocket disconnected.')
  }
})

onUnmounted(() => {
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.close()
  }
})

const getQueueClass = (q: number) => {
  if (q >= 13) return 'danger'
  if (q >= 10) return 'warning'
  return 'normal'
}

const getLocationClass = (index: number) => {
  if (index === 0) return 'location-danger'
  if (index === 1 || index === 2) return 'location-warning'
  return 'location-normal'
}
</script>



<style scoped lang="scss">
.congested-box {
  width: 100%;
  height: 2.8rem;
  background-color: #1E1E2F;
  box-sizing: border-box;
  border-bottom: 0.01rem solid #3A3A4C;
  flex-shrink: 0;
  position: relative;
  padding: 0.2rem 0.24rem 0rem 0.24rem;
  overflow: hidden;
}

// 标题
.title {
  font-size: 0.2rem;
  font-weight: bold;
  color: #ff4c4c;
  line-height: 0.2rem;
  padding-bottom: 0.16rem;
}

// 表头
.table-header {
  display: flex;
  justify-content: space-between;
  font-size: 0.16rem;
  font-weight: bold;
  color: #FFFFFF;
  font-family: Arial, Helvetica, sans-serif;
  line-height: 0.16rem;
  padding-bottom: 0.16rem;
}

// 表体
.table-body {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

// 每一行
.table-row {
  display: flex;
  justify-content: space-between;
  flex-shrink: 0;
  height: 0.28rem;
  line-height: 0.28rem;
  padding-bottom: 0.12rem;
}

// 列
.col {
  display: flex;
  align-items: center;
}

.col.location {
  width: 75%;
  font-size: 0.14rem;
}

.col.queue {
  width: 0.9rem;
  font-size: 0.14rem;
  display: flex;
  justify-content: center;
  align-items: center;
}

// 颜色类
.danger {
  color: #ff4c4c;
}
.warning {
  color: #ffc107;
}
.normal {
  color: #ffff00;
}

.location-danger {
  color: #D9001B;
}
.location-warning {
  color: #F59A23;
}
.location-normal {
  color: #FFFF00;
}
</style>
