import { useAuthStore } from '@/stores/auth'



export interface JunctionPermissionInfo {
  canControl: boolean
  canView: boolean
  areaName: string
  statusText: string
  styleClass: string
}

export const getJunctionMapPermission = (
  junctionX: number,
  junctionY: number,
  mapCenterX: number = 0
): JunctionPermissionInfo => {
  const authStore = useAuthStore()
  const isLeftArea = junctionX < mapCenterX
  const areaName = isLeftArea ? 'Left' : 'Right'

  if (authStore.isAdmin()) {
    return {
      canControl: true,
      canView: true,
      areaName,
      statusText: 'Admin Control',
      styleClass: 'admin-controllable'
    }
  }


  if (authStore.isTrafficPlanner()) {
    return {
      canControl: false,
      canView: true,
      areaName,
      statusText: 'View Only',
      styleClass: 'planner-readonly'
    }
  }


  if (authStore.isTrafficManager()) {
    const managedAreas = authStore.getManagedAreas()
    const canControl = managedAreas.includes(areaName)

    return {
      canControl,
      canView: true,
      areaName,
      statusText: canControl ? 'Controllable' : 'Read Only',
      styleClass: canControl ? 'manager-controllable' : 'manager-readonly'
    }
  }

  return {
    canControl: false,
    canView: false,
    areaName,
    statusText: 'No Access',
    styleClass: 'no-access'
  }
}

export const canClickJunctionForControl = (
  junctionX: number,
  junctionY: number,
  mapCenterX: number = 0
): boolean => {
  const permission = getJunctionMapPermission(junctionX, junctionY, mapCenterX)
  return permission.canControl
}


export const canViewJunctionInfo = (
  junctionX: number,
  junctionY: number,
  mapCenterX: number = 0
): boolean => {
  const permission = getJunctionMapPermission(junctionX, junctionY, mapCenterX)
  return permission.canView
}


export const getJunctionDisplayStyle = (
  junctionX: number,
  junctionY: number,
  mapCenterX: number = 0,
  isSelected: boolean = false
): {
  color: string
  opacity: number
  cursor: string
  tooltip: string
} => {
  const permission = getJunctionMapPermission(junctionX, junctionY, mapCenterX)

  let color = '#666666'
  let opacity = 1.0
  let cursor = 'default'
  let tooltip = permission.statusText

  if (isSelected) {
    color = '#FFD700'
    opacity = 1.0
    cursor = 'pointer'
    tooltip = `Selected - ${permission.statusText}`
  } else if (permission.canControl) {
    color = '#00E5FF'
    opacity = 1.0
    cursor = 'pointer'
    tooltip = `${permission.areaName} Area - ${permission.statusText}`
  } else if (permission.canView) {
    color = '#FFC107'
    opacity = 0.7
    cursor = 'default'
    tooltip = `${permission.areaName} Area - ${permission.statusText}`
  } else {
    color = '#666666'
    opacity = 0.3
    cursor = 'not-allowed'
    tooltip = `${permission.areaName} Area - ${permission.statusText}`
  }

  return { color, opacity, cursor, tooltip }
}


export const getPermissionTooltipMessage = (
  junctionName: string,
  junctionX: number,
  junctionY: number,
  mapCenterX: number = 0
): string => {
  const permission = getJunctionMapPermission(junctionX, junctionY, mapCenterX)
  const authStore = useAuthStore()

  if (permission.canControl) {
    return `${junctionName} (${permission.areaName}) - Click to control traffic lights`
  }

  if (permission.canView && authStore.isTrafficManager()) {
    const managedAreas = authStore.getManagedAreas()
    return `${junctionName} (${permission.areaName}) - Read-only (You manage: ${managedAreas.join(', ')})`
  }

  if (permission.canView && authStore.isTrafficPlanner()) {
    return `${junctionName} (${permission.areaName}) - View-only access`
  }

  return `${junctionName} (${permission.areaName}) - No access`
}

export const handleJunctionClickWithPermission = (
  junctionName: string,
  junctionX: number,
  junctionY: number,
  mapCenterX: number = 0,
  onSuccess: () => void,
  onDenied?: (message: string) => void
): boolean => {
  const permission = getJunctionMapPermission(junctionX, junctionY, mapCenterX)

  if (permission.canControl) {
    onSuccess()
    return true
  }

  const message = getPermissionTooltipMessage(junctionName, junctionX, junctionY, mapCenterX)
  if (onDenied) {
    onDenied(message)
  }

  return false
}

export const getViewModePermissionDescription = (): string => {
  const authStore = useAuthStore()

  if (authStore.isAdmin()) {
    return 'Administrator - Full control over all areas'
  }

  if (authStore.isTrafficPlanner()) {
    return 'Traffic Planner - View-only access to all areas'
  }

  if (authStore.isTrafficManager()) {
    const managedAreas = authStore.getManagedAreas()
    if (managedAreas.length === 0) {
      return 'Traffic Manager - No assigned areas'
    }
    return `Traffic Manager - Control: ${managedAreas.join(', ')} area(s)`
  }

  return 'No access'
}
