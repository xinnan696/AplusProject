import { ref, watch } from 'vue'
import { defineStore } from 'pinia'

export interface OperationRecord {
  id: string
  timestamp: string
  description: string
  source: 'manual' | 'ai'
  junctionId?: string
  junctionName?: string
  lightIndex?: number
  state?: string
  duration?: number
  status: 'success' | 'failed' | 'pending'
  errorMessage?: string
}

const STORAGE_KEY = 'urbanflow_operation_records'

const getStoredRecords = (): OperationRecord[] => {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored ? JSON.parse(stored) : []
  } catch (error) {
    return []
  }
}

export const useOperationStore = defineStore('operation', () => {
  const records = ref<OperationRecord[]>(getStoredRecords())

  watch(
    records,
    (newRecords) => {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(newRecords))
      } catch (error) {
      }
    },
    { deep: true }
  )

  const addRecord = (record: Omit<OperationRecord, 'id' | 'timestamp' | 'status'>) => {
    const newRecord: OperationRecord = {
      ...record,
      id: generateId(),
      timestamp: new Date().toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      }).replace(/\//g, '-'),
      status: 'pending'
    }

    records.value.unshift(newRecord)

    return newRecord.id
  }

  const updateRecordStatus = (id: string, status: 'success' | 'failed', errorMessage?: string) => {
    const record = records.value.find(r => r.id === id)
    if (record) {
      record.status = status
      if (errorMessage) {
        record.errorMessage = errorMessage
      }
    }
  }


  const clearRecords = () => {
    records.value = []
    localStorage.removeItem(STORAGE_KEY)
  }

  const getRecordsBySource = (source: 'manual' | 'ai') => {
    return records.value.filter(record => record.source === source)
  }


  const generateId = () => {
    return Date.now().toString(36) + Math.random().toString(36).substr(2)
  }

  return {
    records,
    addRecord,
    updateRecordStatus,
    clearRecords,
    getRecordsBySource
  }
})
