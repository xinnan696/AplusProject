import apiClient from '@/utils/api'


export interface AreaInfo {
  areaName: string
  userName: string
  accountNumber: string
  createdAt: string
}

export interface ApiResponse<T> {
  statusCode: number
  message: string
  timestamp: number
  data: T
}


export class AreaApiService {

  static async getAvailableAreas(): Promise<ApiResponse<string[]>> {
    try {
      const response = await apiClient.get('/areas/available')
      return response.data
    } catch (error: any) {
      throw new Error(error.response?.data?.message )
    }
  }

  static async getOccupiedAreas(): Promise<ApiResponse<AreaInfo[]>> {
    try {
      const response = await apiClient.get('/areas/occupied')
      return response.data
    } catch (error: any) {
      throw new Error(error.response?.data?.message )
    }
  }

  static async getAllAreas(): Promise<ApiResponse<string[]>> {
    try {
      const response = await apiClient.get('/areas/all')
      return response.data
    } catch (error: any) {
      throw new Error(error.response?.data?.message)
    }
  }
}

export default AreaApiService
