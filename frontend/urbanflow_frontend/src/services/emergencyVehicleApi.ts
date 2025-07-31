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

export const emergencyVehicleApi = {
  getEmergencyEvents: () => axios.get<EmergencyVehicleEvent[]>('/api/emergency-vehicles/check'),
  getEmergencyRoutes: () => axios.get<EmergencyVehicleEvent[]>('/api-status/emergency-routes'),

  getEdgeCoordinates: (edgeIds: string[]) => {
    return axios.get<any[]>('/api-status/lane-mappings')
  }
}
