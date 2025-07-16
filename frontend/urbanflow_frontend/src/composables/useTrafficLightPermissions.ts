import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

/**
 * 红绿灯权限控制 Composable
 * 专门用于处理红绿灯操作的权限控制
 */
export function useTrafficLightPermissions() {
  const authStore = useAuthStore()
  
  // 检查用户是否可以修改特定区域的红绿灯
  const canModifyTrafficLight = (junctionX: number, junctionY: number, mapCenterX: number = 0): boolean => {
    // 管理员拥有所有权限
    if (authStore.isAdmin()) {
      return true
    }
    
    // Traffic Planner 无法修改任何红绿灯
    if (authStore.isTrafficPlanner()) {
      return false
    }
    
    // Traffic Manager 只能修改自己管理区域的红绿灯
    if (authStore.isTrafficManager()) {
      const managedAreas = authStore.getManagedAreas()
      if (managedAreas.length === 0) {
        return false
      }
      
      const isLeftArea = junctionX < mapCenterX
      
      if (isLeftArea && managedAreas.includes('Left')) {
        return true
      }
      if (!isLeftArea && managedAreas.includes('Right')) {
        return true
      }
      
      return false
    }
    
    return false
  }
  
  // 检查用户是否可以查看特定区域的红绿灯状态
  const canViewTrafficLight = (junctionX: number, junctionY: number): boolean => {
    // 所有已登录用户都可以查看所有红绿灯状态
    if (authStore.isAuthenticated) {
      return true
    }
    return false
  }
  
  // 检查用户是否可以修改特定路口的红绿灯
  const canModifyJunctionTrafficLight = (junctionName: string, junctionData?: { junctionX: number, junctionY: number }, mapCenterX: number = 0): boolean => {
    if (!junctionData) {
      return false
    }
    return canModifyTrafficLight(junctionData.junctionX, junctionData.junctionY, mapCenterX)
  }
  
  // 获取权限描述文本
  const getPermissionDescription = (): string => {
    if (authStore.isAdmin()) {
      return 'Full control over all traffic lights'
    }
    if (authStore.isTrafficPlanner()) {
      return 'View-only access to all traffic lights'
    }
    if (authStore.isTrafficManager()) {
      const managedAreas = authStore.getManagedAreas()
      if (managedAreas.length === 0) {
        return 'No traffic light modification permissions'
      }
      return `Can modify traffic lights in: ${managedAreas.join(', ')} area(s)`
    }
    return 'No access'
  }
  
  // 获取用户权限等级
  const getPermissionLevel = (): 'admin' | 'manager' | 'planner' | 'none' => {
    if (authStore.isAdmin()) return 'admin'
    if (authStore.isTrafficManager()) return 'manager'
    if (authStore.isTrafficPlanner()) return 'planner'
    return 'none'
  }
  
  // 检查是否可以应用手动控制
  const canApplyManualControl = (junctionData: { junctionX: number, junctionY: number }, mapCenterX: number = 0): boolean => {
    return canModifyTrafficLight(junctionData.junctionX, junctionData.junctionY, mapCenterX)
  }
  
  // 获取操作权限状态
  const getOperationPermissionStatus = (junctionData: { junctionX: number, junctionY: number }, mapCenterX: number = 0) => {
    const canModify = canModifyTrafficLight(junctionData.junctionX, junctionData.junctionY, mapCenterX)
    const canView = canViewTrafficLight(junctionData.junctionX, junctionData.junctionY)
    
    return {
      canModify,
      canView,
      status: canModify ? 'controllable' : canView ? 'readonly' : 'forbidden',
      description: canModify 
        ? 'You can modify this traffic light' 
        : canView 
          ? 'Read-only access - you can view but not modify' 
          : 'No access to this traffic light'
    }
  }
  
  return {
    canModifyTrafficLight,
    canViewTrafficLight,
    canModifyJunctionTrafficLight,
    canApplyManualControl,
    getPermissionDescription,
    getPermissionLevel,
    getOperationPermissionStatus
  }
}
