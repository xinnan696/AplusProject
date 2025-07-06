import axios from 'axios';

// 后端API基础地址 同时要配置在vite.config.ts
// const API_BASE_URL = 'http://192.168.83.199:8087/api';

// 定义一个通用的错误返回值，防止图表因数据null而崩溃
const errorResponse = { data: [], xAxisLabels: [], yAxisLabels: [], xAxisConfig: {}, yAxisConfig: {} };

// 获取路口列表
// export const getJunctions = async () => {
//   try {
//     // 这个接口可以保持，或者如果您有其他方式获取路口，可以修改
//     const response = await axios.get('/api-status/junctions');
//     return response.data;
//   } catch (error) {
//     console.error("Error fetching junctions:", error);
//     return [];
//   }
// };

export const getJunctions = async () => {
  console.log("MOCK API: Fetching junctions...");
  return [
    { junction_id: 'A1', junction_name: 'Main St_1st Ave' },
    { junction_id: 'B2', junction_name: 'River Rd_Oak Blvd' },
  ];
};

// 图表一 API
export const getTrafficFlow = async (params: { junction_id?: string, time_range: string }) => {
  try {
    const apiParams = {
      timeRange: params.time_range,
      junctionId: params.junction_id === 'total_city' ? undefined : params.junction_id,
    };
    // if (apiParams.junction_id === 'total_city') {
    //   delete apiParams.junction_id;
    // }
    const response = await axios.get(`/api/trafficflow/dashboard`, { params: apiParams });
    return response.data;
  } catch (error) {
    console.error(`Error fetching traffic flow for [${params.time_range}]:`, error);
    return errorResponse;
  }
};

// 图表二 API
export const getCongestedJunctionCountTrend = async (params: { time_range: string }) => {
  try {
    const apiParams = {
      timeRange: params.time_range,
    };
    const response = await axios.get(`/api/congestioncount/ranking`, { params: apiParams });
    return response.data;
  } catch (error) {
    console.error(`Error fetching congested junction count trend for [${params.time_range}]:`, error);
    return errorResponse;
  }
};

// 图表三 API
export const getTopCongestedTimes = async (params: { time_range: string }) => {
  try {
    const apiParams = {
      timeRange: params.time_range,
    };
    const response = await axios.get(`/api/congestedtimes/dashboard`, { params: apiParams });
    return response.data;
  } catch (error) {
    console.error(`Error fetching top congested times for [${params.time_range}]:`, error);
    return errorResponse;
  }
};

// 图表四 API
export const getCongestionDurationRanking = async (params: { time_range: string }) => {
  try {
    const apiParams = {
      timeRange: params.time_range,
    };
    const response = await axios.get(`/api/durationranking/dashboard`, { params: apiParams });
    return response.data;
  } catch (error) {
    console.error(`Error fetching congestion duration ranking for [${params.time_range}]:`, error);
    return errorResponse;
  }
};
