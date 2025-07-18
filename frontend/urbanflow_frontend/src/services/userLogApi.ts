import apiClient from '@/utils/api'


export interface UserLog {
  id: number
  userId: number
  accountNumber: string
  userName: string
  action: string
  detail: string
  module: string
  timestamp: string //
}


export interface DateGroupedLogs {
  date: string
  logs: UserLog[]
}


export interface UserLogResponse {
  code: number // 200
  data: DateGroupedLogs[]
}


export interface UserLogQueryParams {

  searchTerm?: string
  startDate?: string
  endDate?: string
}

function mapBackendLogToFrontend(backendLog: any): UserLog {
  return {
    id: backendLog.id,
    userId: backendLog.userId || backendLog.user_id,
    accountNumber: backendLog.accountNumber || backendLog.account_number,
    userName: backendLog.userName || backendLog.user_name,
    action: backendLog.action,
    detail: backendLog.detail,
    module: backendLog.module,
    timestamp: backendLog.timestamp || backendLog.created_at
  };
}

export class UserLogApiService {

  static async getUserLogs(params: UserLogQueryParams = {}): Promise<UserLogResponse> {
    try {
      console.log('🔧 使用直接fetch方式绕过代理问题');

      const response = await fetch('/api/logs/userLogs', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',

          ...(localStorage.getItem('authToken') ? {
            'Authorization': `Bearer ${localStorage.getItem('authToken')}`
          } : {})
        }
      });

      console.log('🔍 fetch响应状态:', response.status);
      console.log('🔍 fetch响应头:', [...response.headers.entries()]);

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const responseData = await response.json();



      if (responseData.code === 200 && responseData.data) {

        return responseData;
      }

      if (Array.isArray(responseData)) {
        console.log('🔄 检测到数组格式响应，转换为标准格式');
        return {
          code: 200,
          data: responseData
        };
      }

      console.error(responseData);
      console.error(typeof responseData);


      return {
        code: 200,
        data: []
      };

    } catch (error: any) {
      console.error(error);
      throw new Error(error.message)
    }
  }


  static async exportUserLogs(params: UserLogQueryParams = {}): Promise<Blob> {
    try {
      const response = await apiClient.get('/api/logs/userLogs/export', {
        params,
        responseType: 'blob'
      })
      return response.data
    } catch (error: any) {
      throw new Error(error.response?.data?.message || 'Failed to export user logs')
    }
  }
}

export default UserLogApiService
