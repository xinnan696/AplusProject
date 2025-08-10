
export class StorageManager {
  private static prefix = 'urbanflow_'

  static save<T>(key: string, data: T): void {
    try {
      const prefixedKey = this.prefix + key
      localStorage.setItem(prefixedKey, JSON.stringify(data))
    } catch (error) {
      // Silent fail
    }
  }

  static load<T>(key: string, defaultValue: T): T {
    try {
      const prefixedKey = this.prefix + key
      const stored = localStorage.getItem(prefixedKey)
      if (stored) {
        return JSON.parse(stored)
      }
      return defaultValue
    } catch (error) {
      return defaultValue
    }
  }


  static remove(key: string): void {
    try {
      const prefixedKey = this.prefix + key
      localStorage.removeItem(prefixedKey)
    } catch (error) {
      // Silent fail
    }
  }

  static clear(): void {
    try {
      const keysToRemove = Object.keys(localStorage)
        .filter(key => key.startsWith(this.prefix))

      keysToRemove.forEach(key => localStorage.removeItem(key))
    } catch (error) {
      // Silent fail
    }
  }


  static exists(key: string): boolean {
    const prefixedKey = this.prefix + key
    return localStorage.getItem(prefixedKey) !== null
  }


  static getAllKeys(): string[] {
    return Object.keys(localStorage)
      .filter(key => key.startsWith(this.prefix))
      .map(key => key.replace(this.prefix, ''))
  }
}

export function usePersistence<T>(
  key: string,
  data: any,
  options: { deep?: boolean } = { deep: true }
) {
  return {
    save: () => StorageManager.save(key, data.value),
    load: (defaultValue: T) => StorageManager.load(key, defaultValue),
    remove: () => StorageManager.remove(key)
  }
}
