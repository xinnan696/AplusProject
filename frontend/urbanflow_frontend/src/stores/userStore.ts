import { ref } from 'vue'
import { defineStore } from 'pinia'
import { UserApiService, type ApiUser, type CreateUserRequest, type UpdateUserRequest } from '@/services/userApi'
import { useAuthStore } from '@/stores/auth'


export interface User {
  id: number
  accountNumber: string | null
  userName: string
  email: string
  department?: string
  phoneNumber?: string
  role: string
  managedAreas?: string[]
  enabled: boolean

  displayName?: string
  status?: 'active' | 'inactive'
  username?: string
  name?: string
}

export const useUserStore = defineStore('user', () => {
  const users = ref<User[]>([])

  const loading = ref(false)
  const error = ref('')

  const mapApiUserToUser = (apiUser: ApiUser): User => {
    const enabled = apiUser.enabled !== undefined ? apiUser.enabled : false
    console.log(`Mapping user ${apiUser.id}: enabled=${apiUser.enabled} -> ${enabled}`)

    return {
      id: apiUser.id,
      accountNumber: apiUser.accountNumber || null,
      userName: apiUser.userName,
      email: apiUser.email,
      department: (apiUser as any).department || '',
      phoneNumber: (apiUser as any).phoneNumber || '',
      role: apiUser.role,
      managedAreas: (apiUser as any).managedAreas || [],
      enabled: enabled,
      displayName: apiUser.userName,
      status: enabled ? 'active' : 'inactive',
      username: apiUser.accountNumber || `user_${apiUser.id}`,
      name: apiUser.userName
    }
  }

  const fetchUsers = async () => {
    try {
      loading.value = true
      error.value = ''

      console.log('Calling UserApiService.getUserList()...')

      const authStore = useAuthStore()
      console.log('Auth info before API call:', {
        isAuthenticated: authStore.isAuthenticated,
        userRole: authStore.userRole,
        hasToken: !!authStore.token
      })

      const response = await UserApiService.getUserList()
      console.log('API Response:', response)

      if (response.statusCode === 200) {
        users.value = response.data.map(mapApiUserToUser)
        console.log('用户列表加载成功:', users.value)
      } else {
        console.error('API returned error:', response)
        throw new Error(response.message)
      }
    } catch (err: any) {
      error.value = err.message || '获取用户列表失败'
      console.error('获取用户列表失败:', err)
      console.error('Error details:', {
        message: err.message,
        response: err.response?.data,
        status: err.response?.status,
        url: err.config?.url
      })

      if (err.response?.status === 403) {
        console.error('权限不足 - 当前用户可能不是管理员')
        throw new Error('权限不足：您没有访问用户列表的权限')
      }
    } finally {
      loading.value = false
    }
  }


  const addUser = async (userData: CreateUserRequest): Promise<User | null> => {
    try {
      loading.value = true
      error.value = ''

      const response = await UserApiService.createUser(userData)

      if (response.statusCode === 200) {
        const newUser = mapApiUserToUser(response.data)
        users.value.push(newUser)
        console.log('User created successfully:', newUser)
        return newUser
      } else {
        throw new Error(response.message)
      }
    } catch (err: any) {
      error.value = err.message || 'Failed to create user'
      console.error('Failed to create user:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  // Update user
  const updateUser = async (id: number, userData: UpdateUserRequest): Promise<User | null> => {
    try {
      loading.value = true
      error.value = ''

      const response = await UserApiService.updateUser(id, userData)

      if (response.statusCode === 200) {
        const updatedUser = mapApiUserToUser(response.data)
        const index = users.value.findIndex(u => u.id === id)
        if (index !== -1) {

          users.value.splice(index, 1, updatedUser)
          console.log('User updated in store:', { id: updatedUser.id, enabled: updatedUser.enabled })
        }
        console.log('User updated successfully:', updatedUser)
        return updatedUser
      } else {
        throw new Error(response.message)
      }
    } catch (err: any) {
      error.value = err.message || 'Failed to update user'
      console.error('Failed to update user:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  // Delete user
  const deleteUser = async (id: number): Promise<boolean> => {
    try {
      loading.value = true
      error.value = ''

      await UserApiService.deleteUser(id)

      const index = users.value.findIndex(u => u.id === id)
      if (index !== -1) {
        users.value.splice(index, 1)
        console.log('User deleted successfully:', id)
        return true
      }
      return false
    } catch (err: any) {
      error.value = err.message || 'Failed to delete user'
      console.error('Failed to delete user:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  // Get user by ID
  const getUserById = (id: number): User | undefined => {
    return users.value.find(u => u.id === id)
  }

  // Toggle user status
  const toggleUserStatus = async (id: number): Promise<User | null> => {
    const user = getUserById(id)
    if (user) {
      const newEnabled = !user.enabled
      console.log(`toggleUserStatus: user ${id} current enabled: ${user.enabled}, will set to: ${newEnabled}`)

      try {
        const updateData = {
          userName: user.userName,
          email: user.email,
          department: user.department || '',
          phoneNumber: user.phoneNumber || '',
          role: user.role,
          enabled: newEnabled
        }
        console.log('Sending update request with data:', updateData)

        return await updateUser(id, updateData)
      } catch (error) {
        console.error('Failed to toggle user status:', error)
        throw error
      }
    }
    return null
  }

  // Generate new user ID (frontend local generation, actually generated by backend)
  const generateNewId = (): number => {
    return Math.max(...users.value.map(u => u.id), 0) + 1
  }

  // Clear error
  const clearError = () => {
    error.value = ''
  }

  return {
    // State
    users,
    loading,
    error,

    // Methods
    fetchUsers,
    addUser,
    updateUser,
    deleteUser,
    getUserById,
    toggleUserStatus,
    generateNewId,
    clearError
  }
})
