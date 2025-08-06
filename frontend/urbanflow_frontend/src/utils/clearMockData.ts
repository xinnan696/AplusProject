/**
 * 清理开发环境遗留的Mock数据
 * 在生产环境部署前调用此函数
 */
export function clearMockData() {
  console.log('🧹 [Cleanup] Clearing mock data...')
  
  // 清理localStorage中的Mock数据
  const authToken = localStorage.getItem('authToken')
  const user = localStorage.getItem('user')
  
  // 检查是否是Mock token
  if (authToken === 'mock-auth-token-for-testing') {
    console.log('🗑️ [Cleanup] Removing mock auth token')
    localStorage.removeItem('authToken')
  }
  
  // 检查是否是Mock用户数据
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
  
  // 清理其他可能的开发数据
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

/**
 * 在应用启动时自动检查并清理Mock数据
 */
export function autoCleanupOnStart() {
  // 只在非开发环境下自动清理
  if (import.meta.env.PROD) {
    clearMockData()
  }
}
