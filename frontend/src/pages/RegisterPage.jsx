import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../services/api.js'
import { setAuth } from '../services/auth.js'

export default function RegisterPage() {
  const [username, setUsername] = useState('analyst')
  const [email, setEmail] = useState('analyst@local')
  const [password, setPassword] = useState('analyst123')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const submit = async (e) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const res = await api.post('/api/auth/register', { username, email, password })
      setAuth(res.data)
      navigate('/calculate', { replace: true })
    } catch (err) {
      setError(err?.response?.data || 'Registration failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="card" style={{maxWidth: 520, margin: '32px auto'}} aria-labelledby="registerTitle">
      <h2 id="registerTitle">Create Account</h2>
      <p className="small" style={{marginTop:-12}}>Provision a new analyst profile for tariff computations.</p>
      <form onSubmit={submit} noValidate>
        <div className="field">
          <label htmlFor="username">Username</label>
          <input id="username" className="input" value={username} onChange={e=>setUsername(e.target.value)} placeholder="username" required />
        </div>
        <div className="field">
          <label htmlFor="email">Email</label>
          <input id="email" className="input" value={email} onChange={e=>setEmail(e.target.value)} type="email" placeholder="email" required />
        </div>
        <div className="field">
          <label htmlFor="password">Password</label>
          <input id="password" className="input" type="password" value={password} onChange={e=>setPassword(e.target.value)} placeholder="password" required />
        </div>
        <div className="btn-group" style={{marginTop:4}}>
          <button className="primary" type="submit" disabled={loading}>{loading ? 'Creating…' : 'Create account'}</button>
          <Link to="/login"><button type="button" disabled={loading}>Back to login</button></Link>
        </div>
        {error && <div className="error" role="alert">{String(error)}</div>}
      </form>
    </div>
  )
}
