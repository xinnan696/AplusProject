import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

export function useTrafficLightPermissions() {
  const authStore = useAuthStore()

  const canModifyTrafficLight = (junctionX: number, junctionY: number, mapCenterX: number = 0): boolean => {
    if (authStore.isAdmin()) {
      return true
    }

    if (authStore.isTrafficPlanner()) {
      return false
    }

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

  const canViewTrafficLight = (junctionX: number, junctionY: number): boolean => {
    if (authStore.isAuthenticated) {
      return true
    }
    return false
  }

  const canModifyJunctionTrafficLight = (junctionName: string, junctionData?: { junctionX: number, junctionY: number }, mapCenterX: number = 0): boolean => {
    if (!junctionData) {
      return false
    }
    return canModifyTrafficLight(junctionData.junctionX, junctionData.junctionY, mapCenterX)
  }
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

  const getPermissionLevel = (): 'admin' | 'manager' | 'planner' | 'none' => {
    if (authStore.isAdmin()) return 'admin'
    if (authStore.isTrafficManager()) return 'manager'
    if (authStore.isTrafficPlanner()) return 'planner'
    return 'none'
  }

  const canApplyManualControl = (junctionData: { junctionX: number, junctionY: number }, mapCenterX: number = 0): boolean => {
    return canModifyTrafficLight(junctionData.junctionX, junctionData.junctionY, mapCenterX)
  }

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
