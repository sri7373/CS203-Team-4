import React, { useState } from 'react'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import api from '../services/api.js'
import { setAuth } from '../services/auth.js'

export default function LoginPage() {
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('admin123')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const from = location.state?.from?.pathname || '/calculate'

  const submit = async (e) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const res = await api.post('/api/auth/login', { username, password })
      setAuth(res.data)
      navigate(from, { replace: true })
    } catch (err) {
      setError(err?.response?.data || 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="card" style={{maxWidth: 480, margin: '32px auto'}} aria-labelledby="loginTitle">
      <h2 id="loginTitle">Sign In</h2>
      <p className="small" style={{marginTop:-12}}>Access your tariff calculation workspace.</p>
      <form onSubmit={submit} noValidate>
        <div className="field">
          <label htmlFor="username">Username</label>
            <input id="username" className="input" value={username} onChange={e=>setUsername(e.target.value)} autoComplete="username" placeholder="username" required />
        </div>
        <div className="field">
          <label htmlFor="password">Password</label>
          <input id="password" className="input" type="password" value={password} onChange={e=>setPassword(e.target.value)} autoComplete="current-password" placeholder="password" required />
        </div>
        <div className="btn-group" style={{marginTop:4}}>
          <button className="primary" type="submit" disabled={loading}>{loading ? 'Signing in…' : 'Sign in'}</button>
          <Link to="/register"><button type="button" disabled={loading}>Register</button></Link>
        </div>
        {error && <div className="error" role="alert">{String(error)}</div>}
      </form>
    </div>
  )
}
