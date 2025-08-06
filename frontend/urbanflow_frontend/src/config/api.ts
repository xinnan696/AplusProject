
export const API_CONFIG = {

  AI_SERVICE: {
    BASE_URL: import.meta.env.VITE_AI_SERVICE_URL || 'http://localhost:8084',
    ENDPOINTS: {
      SUGGESTION: '/api/traffic/suggestion'
    }
  },

  TRAFFIC_SERVICE: {
    BASE_URL: import.meta.env.VITE_TRAFFIC_SERVICE_URL || 'http://localhost:8083'
  },

  STATUS_SERVICE: {
    BASE_URL: import.meta.env.VITE_STATUS_SERVICE_URL || 'http://localhost:8087'
  }
}

export const getApiUrl = (service: keyof typeof API_CONFIG, endpoint: string) => {
  const serviceConfig = API_CONFIG[service] as { BASE_URL: string }
  return `${serviceConfig.BASE_URL}${endpoint}`
}


export const AI_SUGGESTION_URL = getApiUrl('AI_SERVICE', API_CONFIG.AI_SERVICE.ENDPOINTS.SUGGESTION)
