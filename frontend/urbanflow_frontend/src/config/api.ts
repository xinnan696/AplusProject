// API配置文件
export const API_CONFIG = {
  // AI服务配置
  AI_SERVICE: {
    BASE_URL: import.meta.env.VITE_AI_SERVICE_URL || 'http://localhost:8084',
    ENDPOINTS: {
      SUGGESTION: '/api/traffic/suggestion'
    }
  },
  
  // 其他服务配置
  TRAFFIC_SERVICE: {
    BASE_URL: import.meta.env.VITE_TRAFFIC_SERVICE_URL || 'http://localhost:8083'
  },
  
  STATUS_SERVICE: {
    BASE_URL: import.meta.env.VITE_STATUS_SERVICE_URL || 'http://localhost:8087'
  }
}

// 获取完整的API URL
export const getApiUrl = (service: keyof typeof API_CONFIG, endpoint: string) => {
  const serviceConfig = API_CONFIG[service] as { BASE_URL: string }
  return `${serviceConfig.BASE_URL}${endpoint}`
}

// AI建议接口URL
export const AI_SUGGESTION_URL = getApiUrl('AI_SERVICE', API_CONFIG.AI_SERVICE.ENDPOINTS.SUGGESTION)