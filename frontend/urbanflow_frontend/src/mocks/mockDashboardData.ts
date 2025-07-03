import { generateXAxisLabels } from '@/utils/time';

/**
 * 模拟网络请求的延迟
 * @param ms 延迟的毫秒数
 */
function sleep(ms: number) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

// 模拟获取路口列表API
export const getJunctions = async () => {
  console.log("MOCK API: Fetching junctions...");
  await sleep(100); // 模拟延迟
  return [
    { junction_id: 'j001', junction_name: 'Main St & 1st Ave' },
    { junction_id: 'j002', junction_name: 'River Rd & Oak Blvd' },
    { junction_id: 'j003', junction_name: 'Elm Ave & Maple Dr' },
    { junction_id: 'j004', junction_name: 'Highway 1 & Park Lane' },
    { junction_id: 'j005', junction_name: 'Queen Ave & King St' },
    { junction_id: 'j006', junction_name: 'Market Rd & Broadway' },
    { junction_id: 'j007', junction_name: 'Oak St & 25th Ave' },
    { junction_id: 'j008', junction_name: 'Maple Rd & 30th Ave' },
  ];
};

// 模拟获取车流量API
export const getTrafficFlow = async (params: { junction_id?: string, time_range: string }) => {
  console.log(`MOCK API: Fetching traffic flow for [${params.time_range}]...`);
  await sleep(400); // 模拟延迟

  const labels = generateXAxisLabels(params.time_range);
  const data = Array.from({ length: labels.length }, () => ({
    timestamp: new Date().toISOString(),
    flow_rate_hourly: Math.floor(Math.random() * 1500 + 1000) // 随机数据
  }));

  return { data, labels };
};

// 模拟获取拥堵路口数量API
export const getTopCongestedSegments = async (params: { time_range: string }) => {
  console.log(`MOCK API: Fetching top congested segments for [${params.time_range}]...`);
  await sleep(350); // 模拟延迟

  const labels = generateXAxisLabels(params.time_range);
  const data = Array.from({ length: labels.length }, () => ({
    timestamp: new Date().toISOString(),
    congested_junction_count: Math.floor(Math.random() * 18 + 2) // 随机数据
  }));

  return { data, labels };
};

// 模拟近期拥堵交叉口API
export const getCongestedJunctionCountTrend = async (params: { time_range: string }) => {
  console.log(`MOCK API: Fetching congested road count trend for [${params.time_range}]...`);
  await sleep(500); // 模拟延迟

  // 模拟7条数据
  const mockJunctions = ['Main St', 'River Rd', 'Elm Ave', 'Park Lane', 'Queen Ave', 'Market Rd', 'Oak St'];
  const data = mockJunctions.map(name => ({
    junction_name: name,
    congestion_count: Math.floor(Math.random() * 25 + 1) // 随机拥堵次数
  })).sort((a, b) => b.congestion_count - a.congestion_count); // 降序排列

  const labels = data.map(d => d.junction_name); // 标签就是路口名

  return { data, labels };
};

// 模拟各拥堵交叉口堵塞时间API
export const getCongestionDurationRanking = async (params: { time_range: string }) => {
  console.log(`MOCK API: Fetching congestion duration ranking for [${params.time_range}]...`);
  await sleep(450); // 模拟延迟

  // 模拟5条数据
  const mockJunctions = ['Highway 1', 'Park Lane', 'Market Rd', 'Queen Ave', 'King St'];
  const data = mockJunctions.map(name => ({
    edge_name: name,
    // 随机生成10到90分钟的拥堵时长（单位：秒）
    total_congestion_duration_seconds: Math.floor(Math.random() * 4800 + 600)
  })).sort((a, b) => b.total_congestion_duration_seconds - a.total_congestion_duration_seconds); // 降序排列

  const labels = data.map(d => d.edge_name);

  return { data, labels };
};
