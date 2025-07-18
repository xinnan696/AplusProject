import apiClient from '@/utils/api'

// User API data type definitions
export interface ApiUser {
  id: number
  accountNumber?: string | null
  userName: string
  email: string
  department?: string
  phoneNumber?: string
  role: string
  enabled: boolean
}

export interface CreateUserRequest {
  accountNumber: string
  userName: string
  email: string
  password: string
  department?: string
  phoneNumber?: string
  role: string
  managedAreas?: string[]
  enabled: boolean
}

export interface UpdateUserRequest {
  userName: string
  email: string
  department?: string
  phoneNumber?: string
  role: string
  managedAreas?: string[]
  enabled: boolean
}

export interface LoginRequest {
  accountNumber: string
  password: string
}

export interface ApiResponse<T> {
  statusCode: number
  message: string
  timestamp: number
  data: T
}


export class UserApiService {


  static async login(credentials: LoginRequest): Promise<ApiResponse<{
    token: string
    user: ApiUser
  }>> {
    try {
      const response = await apiClient.post('/auth/login', credentials)
      return response.data
    } catch (error: any) {
      throw new Error(error.response?.data?.message || '登录失败')
    }
  }

  static async getUserList(): Promise<ApiResponse<ApiUser[]>> {
    try {
      const response = await apiClient.get('/users/list')
      return response.data
    } catch (error: any) {
      throw new Error(error.response?.data?.message )
    }
  }

  static async createUser(userData: CreateUserRequest): Promise<ApiResponse<ApiUser>> {
    try {
      const response = await apiClient.post('/users/create', userData)
      return response.data
    } catch (error: any) {
      throw new Error(error.response?.data?.message )
    }
  }


  static async updateUser(userId: number, userData: UpdateUserRequest): Promise<ApiResponse<ApiUser>> {
    try {
      const response = await apiClient.post(`/users/update/${userId}`, userData)
      return response.data
    } catch (error: any) {
      throw new Error(error.response?.data?.message )
    }
  }


  static async getUserById(userId: number): Promise<ApiResponse<ApiUser>> {
    try {
      const response = await apiClient.get(`/users/detail/${userId}`)
      return response.data
    } catch (error: any) {
      throw new Error(error.response?.data?.message )
    }
  }


  static async deleteUser(userId: number): Promise<ApiResponse<string>> {
    try {
      const response = await apiClient.post(`/users/delete/${userId}`)
      return response.data
    } catch (error: any) {
      throw new Error(error.response?.data?.message || '删除用户失败')
    }
  }
}

export default UserApiService
