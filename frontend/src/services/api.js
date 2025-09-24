import axios from 'axios'
import { getToken } from './auth.js'

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080'

export const api = axios.create({
  baseURL: API_BASE
})

// Request interceptor to add auth token
api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
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
      const standardError = {
        status: error.response.status,
        message: errorData.message || errorData.error || 'An error occurred',
        timestamp: errorData.timestamp,
        path: errorData.path
      }
      
      // Attach the formatted error to the error object
      error.formattedMessage = standardError.message
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
