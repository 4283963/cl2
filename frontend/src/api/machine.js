import axios from 'axios'

function withRetry(fn, retries = 2, delay = 500) {
  return async function(...args) {
    let lastError
    for (let i = 0; i <= retries; i++) {
      try {
        return await fn.apply(this, args)
      } catch (e) {
        lastError = e
        if (e.response && e.response.status < 500) throw e
        if (i < retries) {
          await new Promise(r => setTimeout(r, delay * (i + 1)))
        }
      }
    }
    throw lastError
  }
}

const api = axios.create({
  baseURL: '/api',
  timeout: 5000
})

api.interceptors.response.use(
  res => res,
  err => {
    console.warn(`[API] 请求失败: ${err.config?.method?.toUpperCase()} ${err.config?.url} -> ${err.message}`)
    return Promise.reject(err)
  }
)

export const fetchMachines = withRetry(async function() {
  const res = await api.get('/machines')
  return res.data.data
}, 2, 600)

export const fetchMachineDetail = withRetry(async function(machineId) {
  const res = await api.get(`/machines/${machineId}`)
  return res.data.data
}, 1, 500)

export const fetchTrackPoints = withRetry(async function(machineId) {
  const res = await api.get(`/machines/${machineId}/track`)
  return res.data.data
}, 1, 500)

export const fetchHealth = withRetry(async function() {
  const res = await api.get('/health', { timeout: 3000 })
  return res.data.data
}, 0)
