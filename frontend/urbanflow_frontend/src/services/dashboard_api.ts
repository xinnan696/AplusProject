import axios from 'axios';

// 后端API基础地址 同时要配置在vite.config.ts
// const API_BASE_URL = '/api';

// 定义一个通用的错误返回值，防止图表因数据null而崩溃
const errorResponse = { data: [], xAxisLabels: [], yAxisLabels: [], xAxisConfig: {}, yAxisConfig: {} };

//获取路口列表的接口
// export const getJunctions = async () => {
//   try {
//     const response = await axios.get('/api/traffic/junctions');
//     return response.data;
//   } catch (error) {
//     console.error("Error fetching junctions:", error);
//     return [];
//   }
// };
//  修改获取路口列表的接口：修改 getJunctions 函数签名，增加可选的 params 参数
export const getJunctions = async (params?: { managedAreas?: string | null }) => {
  try {
    // 如果 managedAreas 存在，则将其添加到请求参数中
    const apiParams: any = {};
    if (params?.managedAreas) {
      apiParams.managedAreas = params.managedAreas;
    }
    const response = await axios.get('/api/traffic/junctions', { params: apiParams });
    return response.data;
  } catch (error) {
    console.error("Error fetching junctions:", error);
    return [];
  }
};

//修改点：为所有图表API的参数定义一个包含 managedAreas 的通用类型
type ApiParamsWithArea = {
  time_range: string;
  managedAreas?: string | null;
};


// 图表一 API
// export const getTrafficFlow = async (params: { junction_id?: string, time_range: string }) => {
//   try {
//     const apiParams = {
//       timeRange: params.time_range,
//       junctionId: params.junction_id,
//     };
//     const response = await axios.get(`/api/dashboard/trafficflow`, { params: apiParams });
//     return response.data;
//   } catch (error) {
//     console.error(`Error fetching traffic flow for [${params.time_range}]:`, error);
//     return errorResponse;
//   }
// };
export const getTrafficFlow = async (params: ApiParamsWithArea & { junction_id?: string }) => {
  try {
    const apiParams: any = {
      timeRange: params.time_range,
      junctionId: params.junction_id,
    };
    // 添加 managedAreas 到请求参数
    if (params.managedAreas) {
      apiParams.managedAreas = params.managedAreas;
    }
    const response = await axios.get(`/api/dashboard/trafficflow`, { params: apiParams });
    return response.data;
  } catch (error) {
    console.error(`Error fetching traffic flow for [${params.time_range}]:`, error);
    return errorResponse;
  }
};

// 图表二 API
// export const getCongestedJunctionCountTrend = async (params: { time_range: string }) => {
//   try {
//     const apiParams = {
//       timeRange: params.time_range,
//     };
//     const response = await axios.get(`/api/dashboard/congestioncount`, { params: apiParams });
//     return response.data;
//   } catch (error) {
//     console.error(`Error fetching congested junction count trend for [${params.time_range}]:`, error);
//     return errorResponse;
//   }
// };
export const getCongestedJunctionCountTrend = async (params: ApiParamsWithArea) => {
  try {
    const apiParams: any = {
      timeRange: params.time_range,
    };
    if (params.managedAreas) {
      apiParams.managedAreas = params.managedAreas;
    }
    const response = await axios.get(`/api/dashboard/congestioncount`, { params: apiParams });
    return response.data;
  } catch (error) {
    console.error(`Error fetching congested junction count trend for [${params.time_range}]:`, error);
    return errorResponse;
  }
};

// 图表三 API
// export const getTopCongestedTimes = async (params: { time_range: string }) => {
//   try {
//     const apiParams = {
//       timeRange: params.time_range,
//     };
//     const response = await axios.get(`/api/dashboard/congestedtimes`, { params: apiParams });
//     return response.data;
//   } catch (error) {
//     console.error(`Error fetching top congested times for [${params.time_range}]:`, error);
//     return errorResponse;
//   }
// };
export const getTopCongestedTimes = async (params: ApiParamsWithArea) => {
  try {
    const apiParams: any = {
      timeRange: params.time_range,
    };
    if (params.managedAreas) {
      apiParams.managedAreas = params.managedAreas;
    }
    const response = await axios.get(`/api/dashboard/congestedtimes`, { params: apiParams });
    return response.data;
  } catch (error) {
    console.error(`Error fetching top congested times for [${params.time_range}]:`, error);
    return errorResponse;
  }
};

// 图表四 API
// export const getCongestionDurationRanking = async (params: { time_range: string }) => {
//   try {
//     const apiParams = {
//       timeRange: params.time_range,
//     };
//     const response = await axios.get(`/api/dashboard/durationranking`, { params: apiParams });
//     return response.data;
//   } catch (error) {
//     console.error(`Error fetching congestion duration ranking for [${params.time_range}]:`, error);
//     return errorResponse;
//   }
// };
export const getCongestionDurationRanking = async (params: ApiParamsWithArea) => {
  try {
    const apiParams: any = {
      timeRange: params.time_range,
    };
    if (params.managedAreas) {
      apiParams.managedAreas = params.managedAreas;
    }
    const response = await axios.get(`/api/dashboard/durationranking`, { params: apiParams });
    return response.data;
  } catch (error) {
    console.error(`Error fetching congestion duration ranking for [${params.time_range}]:`, error);
    return errorResponse;
  }
};
