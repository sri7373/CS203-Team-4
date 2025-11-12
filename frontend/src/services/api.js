import axios from 'axios'
import { getToken } from './auth.js'

// Use /api as baseURL for all environments
// - In local dev (pnpm dev): axios will use http://localhost:5173/api which gets proxied by Vite
// - In Docker: nginx proxies /api/ to backend:8080
const API_BASE = '/api'

export const api = axios.create({
  baseURL: API_BASE
})

// Request interceptor to add auth token
api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  // Debug: log whether Authorization header is present (helps trace why protected endpoints return 401)
  if (import.meta.env.DEV) {
    try {
      // Use console.debug so it is easy to spot in DevTools network/console
      console.debug('API Request:', config.method?.toUpperCase(), config.url, 'Auth:', !!config.headers['Authorization'])
    } catch (e) {
      // ignore logging errors
    }
  }
  return config
})

// Response interceptor to handle errors properly
api.interceptors.response.use(
  (response) => {
    return response
  },
  (error) => {
    if (error.response) {
      // Server responded with error status
      const errorData = error.response.data
      
      // Create a standardized error object
      const status = error.response.status
      const raw = errorData
      const message = errorData?.message || errorData?.error || (typeof raw === 'string' ? raw : JSON.stringify(raw || {})) || 'An error occurred'
      const standardError = {
        status: status,
        message: message,
        raw: raw,
        timestamp: errorData?.timestamp,
        path: errorData?.path
      }
      
      // Attach the formatted error to the error object
      // Include HTTP status in the formatted message to make it obvious in the UI
      error.formattedMessage = `${standardError.status}: ${standardError.message}`
      error.errorDetails = standardError
      
      console.error('API Error:', standardError)
    } else if (error.request) {
      // Network error
      error.formattedMessage = 'Network error - please check your connection'
      console.error('Network Error:', error.request)
    } else {
      // Other error
      error.formattedMessage = error.message || 'An unexpected error occurred'
      console.error('Error:', error.message)
    }
    
    return Promise.reject(error)
  }
)

export default api
