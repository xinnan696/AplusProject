import axios from 'axios'

export interface EmergencyVehicleEvent {
  id: number
  event_id: string
  event_type: string
  vehicle_id: string
  vehicle_type: string
  organization: string
  trigger_time: number
  start_edge_id: string
  end_edge_id: string
  route_edges: string[]
  signalized_junctions: string[]
  event_status: string
  created_at: string
  updated_at: string
}

export interface EdgeCoordinate {
  edgeId: string
  coordinates: [number, number][]
}

export const specialEventApi = {
  checkEmergencyEvents: () => axios.get<EmergencyVehicleEvent[]>('/api/emergency-vehicles/check'),
  processAllEmergencyEvents: () => axios.post<string>('/api/emergency-vehicles/process'),
  ignoreEvent: (eventId: string) => axios.post<string>(`/api/emergency-vehicles/${eventId}/ignore`),
  completeEvent: (eventId: string) => axios.post<string>(`/api/emergency-vehicles/${eventId}/complete`),
  checkEvents: () => axios.get('/api/events/check'),
  processAllEvents: () => axios.post('/api/events/process')
}

export class EmergencyVehicleTracker {
  private ws: WebSocket | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectTimer: NodeJS.Timeout | null = null
  private messageHandlers: Array<(data: any) => void> = []

  constructor() {
    this.connect()
  }

  private connect() {
    try {
      const wsProtocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
      const wsUrl = `${wsProtocol}//localhost:8085/ws/tracking`

      this.ws = new WebSocket(wsUrl)

      this.ws.onopen = () => {
        this.reconnectAttempts = 0
      }

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)


          this.messageHandlers.forEach(handler => {
            try {
              handler(data)
            } catch (error) {
              console.error(error)
            }
          })
        } catch (error) {
          console.error(error)
        }
      }

      this.ws.onerror = (error) => {
        console.error(error)
      }

      this.ws.onclose = () => {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
          this.reconnectAttempts++
          this.reconnectTimer = setTimeout(() => this.connect(), 3000)
        }
      }
    } catch (error) {
      if (this.reconnectAttempts < this.maxReconnectAttempts) {
        this.reconnectAttempts++
        this.reconnectTimer = setTimeout(() => this.connect(), 3000)
      }
    }
  }

  public onMessage(handler: (data: any) => void) {
    this.messageHandlers.push(handler)
  }

  public disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    if (this.ws) {
      this.ws.close()
      this.ws = null
    }

    this.messageHandlers = []
  }

  public isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN
  }
}
