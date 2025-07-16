import { useAuthStore } from '@/stores/auth'
export const isJunctionInUserManagedArea = (
  junctionX: number,
  junctionY: number,
  mapCenterX: number = 0
): boolean => {
  const authStore = useAuthStore()

  if (authStore.isAdmin()) {
    return true
  }

  if (!authStore.isTrafficManager()) {
    return false
  }

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

export const getJunctionPermissionStatus = (
  junctionX: number,
  junctionY: number,
  mapCenterX: number = 0
): {
  canControl: boolean
  canView: boolean
  areaName: string
  statusText: string
} => {
  const authStore = useAuthStore()
  const isLeftArea = junctionX < mapCenterX
  const areaName = isLeftArea ? 'Left' : 'Right'

  if (authStore.isAdmin()) {
    return {
      canControl: true,
      canView: true,
      areaName,
      statusText: 'Full Control'
    }
  }

  if (authStore.isTrafficPlanner()) {
    return {
      canControl: false,
      canView: true,
      areaName,
      statusText: 'View Only'
    }
  }

  if (authStore.isTrafficManager()) {
    const canControl = isJunctionInUserManagedArea(junctionX, junctionY, mapCenterX)
    return {
      canControl,
      canView: true,
      areaName,
      statusText: canControl ? 'Controllable' : 'Read Only'
    }
  }

  return {
    canControl: false,
    canView: false,
    areaName,
    statusText: 'No Access'
  }
}


export const withPermissionCheck = <T extends any[], R>(
  fn: (...args: T) => R,
  permissionChecker: (...args: T) => boolean,
  errorMessage: string = 'Permission denied'
) => {
  return (...args: T): R => {
    if (!permissionChecker(...args)) {
      throw new Error(errorMessage)
    }
    return fn(...args)
  }
}


export const PERMISSION_MESSAGES = {
  TRAFFIC_LIGHT_MODIFY_DENIED: 'You do not have permission to modify traffic lights in this area',
  AREA_ACCESS_DENIED: 'Access to this area is restricted',
  FUNCTION_DISABLED: 'This function is disabled for your user role',
  VIEW_ONLY_MODE: 'You are in view-only mode for this area'
} as const
