import { generateXAxisLabels } from '@/utils/time.ts';

function sleep(ms: number) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

export const getJunctions = async () => {
  console.log("MOCK API: Fetching junctions...");
  await sleep(100);
  return [
    { junction_id: 'j001', junction_name: 'Main St_1st Ave' },
    { junction_id: 'j002', junction_name: 'River Rd_Oak Blvd' },
    { junction_id: 'j003', junction_name: 'Elm Ave_Maple Dr' },
    { junction_id: 'j004', junction_name: 'Highway 1_Park Lane' },
    { junction_id: 'j005', junction_name: 'Queen Ave_King St' },
    { junction_id: 'j006', junction_name: 'Market Rd_Broadway' },
    { junction_id: 'j007', junction_name: 'Oak St_25th Ave' },
    { junction_id: 'j008', junction_name: 'Maple Rd_30th Ave' },
  ];
};


export const getTrafficFlow = async (params: { junction_id?: string, time_range: string }) => {
  console.log(`MOCK API: Fetching traffic flow for [${params.time_range}]...`);
  await sleep(400);

  const labels = generateXAxisLabels(params.time_range);
  const data = Array.from({ length: labels.length }, () => ({
    timestamp: new Date().toISOString(),
    flow_rate_hourly: Math.floor(Math.random() * 1500 + 1000)
  }));

  return { data, labels };
};


export const getCongestedJunctionCountTrend = async (params: { time_range: string }) => {
  console.log(`MOCK API: Fetching top congested segments for [${params.time_range}]...`);
  await sleep(350);

  const labels = generateXAxisLabels(params.time_range);
  const data = Array.from({ length: labels.length }, () => ({
    timestamp: new Date().toISOString(),
    congested_junction_count: Math.floor(Math.random() * 18 + 2)
  }));

  return { data, labels };
};

export const getTopCongestedSegments = async (params: { time_range: string }) => {
  console.log(`MOCK API: Fetching congested road count trend for [${params.time_range}]...`);
  await sleep(500);

  const mockJunctions = ['Main St', 'River Rd', 'Elm Ave', 'Park Lane', 'Queen Ave', 'Market Rd', 'Oak St'];
  const data = mockJunctions.map(name => ({
    junction_name: name,
    congestion_count: Math.floor(Math.random() * 25 + 1)
  })).sort((a, b) => b.congestion_count - a.congestion_count);

  const labels = data.map(d => d.junction_name);

  return { data, labels };
};

export const getCongestionDurationRanking = async (params: { time_range: string }) => {
  console.log(`MOCK API: Fetching congestion duration ranking for [${params.time_range}]...`);
  await sleep(450);

  const mockJunctions = ['Highway 1', 'Park Lane', 'Market Rd', 'Queen Ave', 'King St'];
  const data = mockJunctions.map(name => ({
    edge_name: name,
    total_congestion_duration_seconds: Math.floor(Math.random() * 4800 + 600)
  })).sort((a, b) => b.total_congestion_duration_seconds - a.total_congestion_duration_seconds);

  const labels = data.map(d => d.edge_name);

  return { data, labels };
};
