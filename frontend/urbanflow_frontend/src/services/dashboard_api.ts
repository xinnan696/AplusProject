import axios from 'axios';

const errorResponse = { data: [], xAxisLabels: [], yAxisLabels: [], xAxisConfig: {}, yAxisConfig: {} };

export const getJunctions = async (params?: { managedAreas?: string | null }) => {
  try {
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

type ApiParamsWithArea = {
  time_range: string;
  managedAreas?: string | null;
};

export const getTrafficFlow = async (params: ApiParamsWithArea & { junction_id?: string }) => {
  try {
    const apiParams: any = {
      timeRange: params.time_range,
      junctionId: params.junction_id,
    };
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
