import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

export function useAreaPermissions() {
  const authStore = useAuthStore()

  const viewMode = ref<'restricted' | 'full'>('restricted')

  const managedAreas = computed(() => authStore.getManagedAreas())

  const canToggleView = computed(() => authStore.canViewAllAreas())

  const hasAreaAccess = (areaName: string): boolean => {
    return authStore.hasAreaAccess(areaName)
  }

  const isAreaControllable = (areaName: string): boolean => {
    if (authStore.isAdmin()) return true
    if (viewMode.value === 'restricted') {
      return hasAreaAccess(areaName)
    }

    return hasAreaAccess(areaName)
  }


  const isAreaDisabled = (areaName: string): boolean => {
    if (authStore.isAdmin()) return false
    if (viewMode.value === 'restricted') return false
    return !hasAreaAccess(areaName)
  }

  const isAreaVisible = (areaName: string): boolean => {
    if (authStore.isAdmin()) return true
    if (viewMode.value === 'full') return true
    return hasAreaAccess(areaName)
  }

  const toggleViewMode = () => {
    if (canToggleView.value) {
      viewMode.value = viewMode.value === 'restricted' ? 'full' : 'restricted'
    }
  }

  const viewModeDescription = computed(() => {
    switch (viewMode.value) {
      case 'restricted':
        return `My Areas (${managedAreas.value.join(', ')})`
      case 'full':
        return 'All Areas (Read-only for non-managed areas)'
      default:
        return 'Unknown Mode'
    }
  })

  const getAreaStyleClass = (areaName: string): string[] => {
    const classes: string[] = []

    if (isAreaControllable(areaName)) {
      classes.push('area-controllable')
    }

    if (isAreaDisabled(areaName)) {
      classes.push('area-disabled')
    }

    if (hasAreaAccess(areaName)) {
      classes.push('area-managed')
    }

    return classes
  }

  const getAreaOpacity = (areaName: string): number => {
    if (authStore.isAdmin()) return 1.0
    if (viewMode.value === 'restricted') return 1.0
    return hasAreaAccess(areaName) ? 1.0 : 0.4
  }

  const canControlArea = (areaName: string): boolean => {
    return isAreaControllable(areaName) && !isAreaDisabled(areaName)
  }

  return {

    viewMode,
    managedAreas,
    canToggleView,
    viewModeDescription,

    hasAreaAccess,
    isAreaControllable,
    isAreaDisabled,
    isAreaVisible,
    toggleViewMode,
    getAreaStyleClass,
    getAreaOpacity,
    canControlArea,
  }
}


export const AREA_MAPPINGS = {
  'Left': {
    name: 'Left Area',
    description: 'Western traffic control zone',

    bounds: {
      minX: -1000,
      maxX: 0,
      minY: -1000,
      maxY: 1000
    }
  },
  'Right': {
    name: 'Right Area',
    description: 'Eastern traffic control zone',
    bounds: {
      minX: 0,
      maxX: 1000,
      minY: -1000,
      maxY: 1000
    }
  }
} as const

export function getAreaByCoordinates(x: number, y: number): string | null {
  for (const [areaKey, config] of Object.entries(AREA_MAPPINGS)) {
    const { bounds } = config
    if (x >= bounds.minX && x <= bounds.maxX && y >= bounds.minY && y <= bounds.maxY) {
      return areaKey
    }
  }
  return null
}

export function isJunctionInArea(junctionX: number, junctionY: number, areaName: string): boolean {
  const area = AREA_MAPPINGS[areaName as keyof typeof AREA_MAPPINGS]
  if (!area) return false

  const { bounds } = area
  return junctionX >= bounds.minX && junctionX <= bounds.maxX &&
         junctionY >= bounds.minY && junctionY <= bounds.maxY
}
