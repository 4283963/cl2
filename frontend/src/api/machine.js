import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

export function fetchMachines() {
  return api.get('/machines').then(res => res.data.data)
}

export function fetchMachineDetail(machineId) {
  return api.get(`/machines/${machineId}`).then(res => res.data.data)
}

export function fetchTrackPoints(machineId) {
  return api.get(`/machines/${machineId}/track`).then(res => res.data.data)
}
