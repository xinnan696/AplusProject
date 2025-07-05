import axios from 'axios';

// 后端API基础地址
const API_BASE_URL = '/api/dashboard'; // 使用代理

// 定义一个通用的错误返回值，防止图表因数据null而崩溃
const errorResponse = { data: [], xAxisLabels: [], yAxisLabels: [], xAxisConfig: {}, yAxisConfig: {} };

// 获取路口列表
export const getJunctions = async () => {
  try {
    // 这个接口可以保持，或者如果您有其他方式获取路口，可以修改
    const response = await axios.get('/api-status/junctions');
    return response.data;
  } catch (error) {
    console.error("Error fetching junctions:", error);
    return [];
  }
};

// 图表一 API
export const getTrafficFlow = async (params: { junction_id?: string, time_range: string }) => {
  try {
    const apiParams: any = { ...params };
    if (apiParams.junction_id === 'total_city') {
      delete apiParams.junction_id;
    }
    const response = await axios.get(`${API_BASE_URL}/traffic-flow`, { params: apiParams });
    return response.data;
  } catch (error) {
    console.error(`Error fetching traffic flow for [${params.time_range}]:`, error);
    return errorResponse;
  }
};

// 图表二 API
export const getTopCongestedSegments = async (params: { time_range: string }) => {
  try {
    const response = await axios.get(`${API_BASE_URL}/congested-segments`, { params });
    return response.data;
  } catch (error) {
    console.error(`Error fetching top congested segments for [${params.time_range}]:`, error);
    return errorResponse;
  }
};

// 图表三 API
export const getCongestedRoadCountTrend = async (params: { time_range: string }) => {
  try {
    const response = await axios.get(`${API_BASE_URL}/congestion-count-trend`, { params });
    return response.data;
  } catch (error) {
    console.error(`Error fetching congested road count trend for [${params.time_range}]:`, error);
    return errorResponse;
  }
};

// 图表四 API
export const getCongestionDurationRanking = async (params: { time_range: string }) => {
  try {
    const response = await axios.get(`${API_BASE_URL}/congestion-duration-ranking`, { params });
    return response.data;
  } catch (error) {
    console.error(`Error fetching congestion duration ranking for [${params.time_range}]:`, error);
    return errorResponse;
  }
};
