import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../services/api.js'
import { setAuth } from '../services/auth.js'

export default function RegisterPage() {
  const [username, setUsername] = useState('analyst')
  const [email, setEmail] = useState('analyst@local')
  const [password, setPassword] = useState('analyst123')
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const parseValidationError = (message) => {
    const errors = {}
    
    // Parse validation message format: "Validation failed: {field=message, field=message}"
    if (message && message.includes('Validation failed:')) {
      const errorPart = message.split('Validation failed:')[1]
      
      // Extract field-specific errors
      if (errorPart.includes('password=')) {
        const passwordMatch = errorPart.match(/password=([^,}]+)/)
        if (passwordMatch) {
          errors.password = passwordMatch[1].trim()
        }
      }
      if (errorPart.includes('email=')) {
        const emailMatch = errorPart.match(/email=([^,}]+)/)
        if (emailMatch) {
          errors.email = emailMatch[1].trim()
        }
      }
      if (errorPart.includes('username=')) {
        const usernameMatch = errorPart.match(/username=([^,}]+)/)
        if (usernameMatch) {
          errors.username = usernameMatch[1].trim()
        }
      }
    }
    
    return errors
  }

  const submit = async (e) => {
    e.preventDefault()
    setError(null)
    setFieldErrors({})
    setLoading(true)
    try {
      const res = await api.post('/api/auth/register', { username, email, password })
      setAuth(res.data)
      navigate('/calculate', { replace: true })
    } catch (err) {
      console.error('Registration error:', err)

      // Handle different error response formats
      if (err.response) {
        const status = err.response.status
        const data = err.response.data

        // Check if error data has a message property
        if (data?.message) {
          // Check if it's a validation error with field-specific messages
          if (data.message.includes('Validation failed:')) {
            const parsedErrors = parseValidationError(data.message)
            if (Object.keys(parsedErrors).length > 0) {
              setFieldErrors(parsedErrors)
              setError('Please fix the validation errors below.')
            } else {
              setError(data.message)
            }
          } else {
            setError(data.message)
          }
        } else if (typeof data === 'string') {
          // Check if string contains validation errors
          if (data.includes('Validation failed:')) {
            const parsedErrors = parseValidationError(data)
            if (Object.keys(parsedErrors).length > 0) {
              setFieldErrors(parsedErrors)
              setError('Please fix the validation errors below.')
            } else {
              setError(data)
            }
          } else {
            setError(data)
          }
        } else {
          // Handle based on HTTP status code
          switch (status) {
            case 400:
              setError('Invalid registration data. Please check your inputs.')
              break
            case 409:
              setError('Username or email already exists. Please choose different credentials.')
              break
            case 422:
              setError('Validation failed. Please ensure all fields are filled correctly.')
              break
            case 500:
              setError('Server error. Please try again later.')
              break
            case 503:
              setError('Service unavailable. Please try again later.')
              break
            default:
              setError(`Registration failed with status ${status}`)
          }
        }
      } else if (err.request) {
        // Request was made but no response received
        setError('No response from server. Please check your connection.')
      } else {
        // Something else happened
        setError(err.message || 'Registration failed. Please try again.')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="card" style={{maxWidth: 520, margin: '32px auto'}} aria-labelledby="registerTitle">
      <h2 id="registerTitle">Create Account</h2>
      <p className="small" style={{marginTop:-12, marginBottom: 24}}>Provision a new analyst profile for tariff computations.</p>
      {error && (
        <div className="error" role="alert" style={{ marginBottom: 20 }}>
          <strong>Error:</strong> {error}
        </div>
      )}
      <form onSubmit={submit} noValidate>
        <div className="field">
          <label htmlFor="username">Username</label>
          <input 
            id="username" 
            className="input" 
            value={username} 
            onChange={e=>setUsername(e.target.value)} 
            placeholder="username" 
            required 
            aria-invalid={!!fieldErrors.username}
            aria-describedby={fieldErrors.username ? "username-error" : undefined}
          />
          {fieldErrors.username && (
            <div id="username-error" role="alert" style={{ marginTop: 6, fontSize: '13px', color: '#ef4444', padding: '6px 10px', backgroundColor: 'rgba(239, 68, 68, 0.1)', borderRadius: '4px', border: '1px solid rgba(239, 68, 68, 0.2)' }}>
              ⚠ Username {fieldErrors.username}
            </div>
          )}
        </div>
        <div className="field">
          <label htmlFor="email">Email</label>
          <input 
            id="email" 
            className="input" 
            value={email} 
            onChange={e=>setEmail(e.target.value)} 
            type="email" 
            placeholder="email" 
            required 
            aria-invalid={!!fieldErrors.email}
            aria-describedby={fieldErrors.email ? "email-error" : undefined}
          />
          {fieldErrors.email && (
            <div id="email-error" role="alert" style={{ marginTop: 6, fontSize: '13px', color: '#ef4444', padding: '6px 10px', backgroundColor: 'rgba(239, 68, 68, 0.1)', borderRadius: '4px', border: '1px solid rgba(239, 68, 68, 0.2)' }}>
              ⚠ Email {fieldErrors.email}
            </div>
          )}
        </div>
        <div className="field">
          <label htmlFor="password">Password</label>
          <input 
            id="password" 
            className="input" 
            type="password" 
            value={password} 
            onChange={e=>setPassword(e.target.value)} 
            placeholder="password" 
            required 
            aria-invalid={!!fieldErrors.password}
            aria-describedby={fieldErrors.password ? "password-error" : undefined}
          />
          {fieldErrors.password && (
            <div id="password-error" role="alert" style={{ marginTop: 6, fontSize: '13px', color: '#ef4444', padding: '6px 10px', backgroundColor: 'rgba(239, 68, 68, 0.1)', borderRadius: '4px', border: '1px solid rgba(239, 68, 68, 0.2)' }}>
              ⚠ Password {fieldErrors.password}
            </div>
          )}
        </div>
        <div className="btn-group" style={{marginTop:4}}>
          <button className="primary" type="submit" disabled={loading}>{loading ? 'Creating…' : 'Create account'}</button>
          <Link to="/login"><button type="button" disabled={loading}>Back to login</button></Link>
        </div>
      </form>
    </div>
  )
}
