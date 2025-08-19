export function clearMockData() {
  console.log('🧹 [Cleanup] Clearing mock data...')


  const authToken = localStorage.getItem('authToken')
  const user = localStorage.getItem('user')


  if (authToken === 'mock-auth-token-for-testing') {
    console.log('🗑️ [Cleanup] Removing mock auth token')
    localStorage.removeItem('authToken')
  }

  if (user) {
    try {
      const userData = JSON.parse(user)
      if (userData.userName === 'Test Admin' || userData.role === 'ADMIN') {
        console.log('🗑️ [Cleanup] Removing mock user data')
        localStorage.removeItem('user')
      }
    } catch (e) {
      console.warn('⚠️ [Cleanup] Error parsing user data, removing it')
      localStorage.removeItem('user')
    }
  }

  const keysToCheck = [
    'dev-',
    'test-',
    'mock-',
    'debug-'
  ]

  Object.keys(localStorage).forEach(key => {
    if (keysToCheck.some(prefix => key.startsWith(prefix))) {
      console.log(`🗑️ [Cleanup] Removing development key: ${key}`)
      localStorage.removeItem(key)
    }
  })

  console.log('✅ [Cleanup] Mock data cleanup completed')
}

export function autoCleanupOnStart() {

  if (import.meta.env.PROD) {
    clearMockData()
  }
}
