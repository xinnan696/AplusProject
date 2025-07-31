import axios from 'axios';

// 后端API基础地址 同时要配置在vite.config.ts
// const API_BASE_URL = '/api';

// 定义一个通用的错误返回值，防止图表因数据null而崩溃
const errorResponse = { data: [], xAxisLabels: [], yAxisLabels: [], xAxisConfig: {}, yAxisConfig: {} };

//获取路口列表
export const getJunctions = async () => {
  try {
    const response = await axios.get('/api/traffic/junctions');
    return response.data;
  } catch (error) {
    console.error("Error fetching junctions:", error);
    return [];
  }
};


// 图表一 API
export const getTrafficFlow = async (params: { junction_id?: string, time_range: string }) => {
  try {
    const apiParams = {
      timeRange: params.time_range,
      junctionId: params.junction_id,
    };
    const response = await axios.get(`/api/dashboard/trafficflow`, { params: apiParams });
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
    const response = await axios.get(`/api/dashboard/congestioncount`, { params: apiParams });
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
    const response = await axios.get(`/api/dashboard/congestedtimes`, { params: apiParams });
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
    const response = await axios.get(`/api/dashboard/durationranking`, { params: apiParams });
    return response.data;
  } catch (error) {
    console.error(`Error fetching congestion duration ranking for [${params.time_range}]:`, error);
    return errorResponse;
  }
};
