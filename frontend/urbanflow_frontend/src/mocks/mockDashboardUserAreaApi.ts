import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';

// 创建一个 Mock Adapter 实例，并设置500ms的延迟，让数据加载看起来更真实
const mock = new MockAdapter(axios, { delayResponse: 500 });

// ===================================================================================
// 1. 定义模拟数据 (Mock Data)
// ===================================================================================

// --- 交叉口数据 (Junctions) ---
const mockLeftJunctions = [
  { junctionId: 'J-L-01', junctionName: 'Liberty Crossing' },
  { junctionId: 'J-L-02', junctionName: 'Phoenix Park Way' },
  { junctionId: 'J-L-03', junctionName: 'O\'Connell Bridge North' },
  { junctionId: 'J-L-04', junctionName: 'Heuston Station Entrance' },
  { junctionId: 'J-L-05', junctionName: 'Chapelizod Bypass' },
];

const mockRightJunctions = [
  { junctionId: 'J-R-01', junctionName: 'Grand Canal Dock' },
  { junctionId: 'J-R-02', junctionName: 'Silicon Plaza' },
  { junctionId: 'J-R-03', junctionName: 'Docklands Interchange' },
  { junctionId: 'J-R-04', junctionName: 'Pearse Street Junction' },
  { junctionId: 'J-R-05', junctionName: 'Ringsend Main Street' },
];

const mockGlobalJunctions = [...mockLeftJunctions, ...mockRightJunctions];

// --- 图表通用 X 轴标签 ---
const timeLabels24h = ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00'];

// --- 图表数据 (Chart Data) ---

// TrafficFlow 数据
const leftTrafficData = { data: [120, 132, 180, 234, 190, 230, 210], xAxisLabels: timeLabels24h, yAxisConfig: { min: 0, max: 300, interval: 50 } };
const rightTrafficData = { data: [350, 410, 501, 434, 390, 330, 420], xAxisLabels: timeLabels24h, yAxisConfig: { min: 300, max: 600, interval: 50 } };
const globalTrafficData = { data: [470, 542, 681, 668, 580, 560, 630], xAxisLabels: timeLabels24h, yAxisConfig: { min: 400, max: 800, interval: 100 } };

// CongestedJunctionCountTrend 数据
const leftCongestionCountData = { data: [{ congested_junction_count: 1 }, { congested_junction_count: 3 }, { congested_junction_count: 5 }, { congested_junction_count: 2 }, { congested_junction_count: 4 }, { congested_junction_count: 3 }, { congested_junction_count: 1 }], xAxisLabels: timeLabels24h, yAxisConfig: { min: 0, max: 10, interval: 2 } };
const rightCongestionCountData = { data: [{ congested_junction_count: 5 }, { congested_junction_count: 4 }, { congested_junction_count: 6 }, { congested_junction_count: 8 }, { congested_junction_count: 5 }, { congested_junction_count: 6 }, { congested_junction_count: 5 }], xAxisLabels: timeLabels24h, yAxisConfig: { min: 0, max: 15, interval: 3 } };
const globalCongestionCountData = { data: [{ congested_junction_count: 6 }, { congested_junction_count: 7 }, { congested_junction_count: 11 }, { congested_junction_count: 10 }, { congested_junction_count: 9 }, { congested_junction_count: 9 }, { congested_junction_count: 6 }], xAxisLabels: timeLabels24h, yAxisConfig: { min: 0, max: 20, interval: 5 } };

// --- TopCongestedTimes 数据 (添加排序逻辑) ---
const generateSortedCongestedTimes = (junctions: {junctionName: string}[]) => {
  // 1. 将名称和随机值组合在一起
  const combined = junctions.map(j => ({
    name: j.junctionName,
    value: Math.floor(Math.random() * 30) + 5
  }));

  // 2. 按数值(value)降序排序
  combined.sort((a, b) => b.value - a.value);

  // 3. 将排好序的数据分离回API所需的格式
  const data = combined.map(item => ({ congestion_count: item.value }));
  const xAxisLabels = combined.map(item => item.name);
  return { data, xAxisLabels, yAxisConfig: { min: 0, max: 40, interval: 10 } };
};

const leftCongestedTimesData = generateSortedCongestedTimes(mockLeftJunctions);
const rightCongestedTimesData = generateSortedCongestedTimes(mockRightJunctions);
const globalCongestedTimesData = generateSortedCongestedTimes(mockGlobalJunctions);


// --- CongestionDurationRanking 数据 (添加排序逻辑) ---
const generateSortedDurationRanking = (junctions: {junctionName: string}[]) => {
  // 1. 将名称和随机值组合在一起
  const combined = junctions.map(j => ({
    name: j.junctionName,
    value: Math.floor(Math.random() * 300) + 50
  }));

  // 2. 按数值(value)降序排序
  combined.sort((a, b) => b.value - a.value);

  // 3. 将排好序的数据分离回API所需的格式
  const data = combined.map(item => ({ total_congestion_duration_seconds: item.value }));
  const labels = combined.map(item => item.name);
  return { data, labels };
};

const leftDurationData = generateSortedDurationRanking(mockLeftJunctions);
const rightDurationData = generateSortedDurationRanking(mockRightJunctions);
const globalDurationData = generateSortedDurationRanking(mockGlobalJunctions);

// ===================================================================================
// 2. 定义模拟接口的响应逻辑 (Mock API Responses)
// ===================================================================================

// ★★★ 修改点 2：为 createReply 函数添加更精确的类型注解 ★★★
const createReply = (name: string, leftData: any, rightData: any, globalData: any) =>
  (config: AxiosRequestConfig): [number, any] => { // <-- 明确参数和返回值类型

    console.log(`[Mock] API triggered: ${name}`, { params: config.params });

    if (config.params?.managedAreas === 'Left') {
      console.log(`[Mock] Responding with 'Left' area data for ${name}.`);
      return [200, leftData];
    }
    if (config.params?.managedAreas === 'Right') {
      console.log(`[Mock] Responding with 'Right' area data for ${name}.`);
      return [200, rightData];
    }

    console.log(`[Mock] Responding with 'Global' data for ${name}.`);
    return [200, globalData];
  };

// --- Mock API endpoints (此部分无变化，但现在错误会消失) ---

// 1. Mock: GET /api/traffic/junctions
mock.onGet('/api/traffic/junctions').reply(
  createReply('getJunctions', mockLeftJunctions, mockRightJunctions, mockGlobalJunctions)
);

// 2. Mock: GET /api/dashboard/trafficflow
mock.onGet('/api/dashboard/trafficflow').reply(
  createReply('getTrafficFlow', leftTrafficData, rightTrafficData, globalTrafficData)
);

// 3. Mock: GET /api/dashboard/congestioncount
mock.onGet('/api/dashboard/congestioncount').reply(
  createReply('getCongestedJunctionCountTrend', leftCongestionCountData, rightCongestionCountData, globalCongestionCountData)
);

// 4. Mock: GET /api/dashboard/congestedtimes
mock.onGet('/api/dashboard/congestedtimes').reply(
  createReply('getTopCongestedTimes', leftCongestedTimesData, rightCongestedTimesData, globalCongestedTimesData)
);

// 5. Mock: GET /api/dashboard/durationranking
mock.onGet('/api/dashboard/durationranking').reply(
  createReply('getCongestionDurationRanking', leftDurationData, rightDurationData, globalDurationData)
);

export default mock;
