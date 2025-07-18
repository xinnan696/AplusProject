import axios from 'axios';

const errorResponse = { data: [], xAxisLabels: [], yAxisLabels: [], xAxisConfig: {}, yAxisConfig: {} };

const convertTimeRangeForBackend = (frontendTimeRange: string): string => {
  switch (frontendTimeRange.toLowerCase()) {
    case '24hours':
      return '24 hours';
    case 'oneweek':
      return 'one week';
    case 'onemonth':
      return 'one month';
    case 'sixmonths':
      return 'six months';
    case 'oneyear':
      return 'one year';
    default:
      return '24 hours'; 
  }
};

export const getJunctions = async () => {
  try {
    const response = await axios.get('/api/junctions');
    return response.data;
  } catch (error) {
    console.error("Error fetching junctions:", error);
    return [
      { junction_id: 'A1', junction_name: 'Central Plaza' },
      { junction_id: 'B2', junction_name: 'Train Station' },
      { junction_id: 'C3', junction_name: 'Shopping Mall' },
      { junction_id: 'D4', junction_name: 'University Gate' },
      { junction_id: 'E5', junction_name: 'City Hospital' },
      { junction_id: 'F6', junction_name: 'Industrial Park' },
      { junction_id: 'G7', junction_name: 'Residential District' },
      { junction_id: 'H8', junction_name: 'Sports Complex' },
      { junction_id: 'I9', junction_name: 'Business Center' },
      { junction_id: 'J10', junction_name: 'Airport Road' },
      { junction_id: 'K11', junction_name: 'Highway Entrance' },
      { junction_id: 'L12', junction_name: 'Entertainment District' },
    ];
  }
};


export const getTrafficFlow = async (params: { junction_id?: string, time_range: string }) => {
  try {
    const apiParams = {
      timeRange: convertTimeRangeForBackend(params.time_range),
      junctionId: params.junction_id === 'total_city' ? undefined : params.junction_id,
    };

    const response = await axios.get(`/api/trafficflow/dashboard`, { params: apiParams });
    return response.data;
  } catch (error) {
    console.error(`Error fetching traffic flow for [${params.time_range}]:`, error);
    return errorResponse;
  }
};


export const getCongestedJunctionCountTrend = async (params: { time_range: string }) => {
  try {
    const apiParams = {
      timeRange: convertTimeRangeForBackend(params.time_range),
    };
    const response = await axios.get(`/api/congestioncount/ranking`, { params: apiParams });
    return response.data;
  } catch (error) {
    console.error(`Error fetching congested junction count trend for [${params.time_range}]:`, error);
    return errorResponse;
  }
};


export const getTopCongestedTimes = async (params: { time_range: string }) => {
  try {
    const apiParams = {
      timeRange: convertTimeRangeForBackend(params.time_range),
    };
    const response = await axios.get(`/api/congestedtimes/dashboard`, { params: apiParams });
    return response.data;
  } catch (error) {
    console.error(`Error fetching top congested times for [${params.time_range}]:`, error);
    return errorResponse;
  }
};


export const getCongestionDurationRanking = async (params: { time_range: string }) => {
  try {
    const apiParams = {
      timeRange: convertTimeRangeForBackend(params.time_range),
    };
    const response = await axios.get(`/api/durationranking/dashboard`, { params: apiParams });
    return response.data;
  } catch (error) {
    console.error(`Error fetching congestion duration ranking for [${params.time_range}]:`, error);
    return errorResponse;
  }
};
