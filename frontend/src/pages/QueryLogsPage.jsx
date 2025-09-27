import React, { useEffect, useState } from 'react'
import api from '../services/api.js'
import MotionWrapper from '../components/MotionWrapper.jsx'

export default function QueryLogsPage() {
  const [logs, setLogs] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    fetchLogs()
  }, [])

  async function fetchLogs() {
    setLoading(true)
    setError(null)
    try {
      const res = await api.get('/api/query-logs')
      setLogs(res.data || [])
    } catch (err) {
      // Log full error for debugging in browser console
      console.error('Failed to load query logs', err)
      // Prefer formattedMessage (set by api interceptor), then HTTP response body if present, then generic message
      const httpBody = err?.response?.data
      const bodyMsg = httpBody ? (typeof httpBody === 'string' ? httpBody : JSON.stringify(httpBody, null, 2)) : null
      const status = err?.response?.status
      const formatted = err?.formattedMessage || err?.message || bodyMsg || 'Failed to load query logs'
      setError(formatted + (bodyMsg ? `\n\nResponse body:\n${bodyMsg}` : '') )
    } finally {
      setLoading(false)
    }
  }

  async function testEndpoint() {
    setLoading(true)
    setError(null)
    try {
      const res = await api.get('/api/query-logs/test')
      setError(`Test OK: ${JSON.stringify(res.data)}`)
    } catch (err) {
      const body = err?.response?.data ? JSON.stringify(err.response.data, null, 2) : err?.message
      setError(`Test failed: ${err?.formattedMessage || err?.message}\n\n${body}`)
    } finally {
      setLoading(false)
    }
  }

  return (
    <MotionWrapper>
      <div className="card glass glow-border neon-focus">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <h2 className="neon-text">Query Logs</h2>
            <p className="small neon-subtle" style={{ marginTop: -12, marginBottom: 18 }}>
              View past queries recorded by the system.
            </p>
          </div>
          <div>
            <button className="primary" type="button" onClick={fetchLogs} disabled={loading} aria-label="Refresh logs">
              {loading ? 'Refreshing…' : 'Refresh'}
            </button>
            <button className="secondary" type="button" onClick={testEndpoint} disabled={loading} style={{marginLeft:8}} aria-label="Test endpoint">
              Test
            </button>
          </div>
        </div>

        {loading && <div className="small">Loading…</div>}
        {error && <div className="error">{error}</div>}

        {!loading && !error && (
          <div style={{ overflowX: 'auto', marginTop: 12 }}>
            {logs.length === 0 ? (
              <div className="small neon-subtle">No query logs found.</div>
            ) : (
              <table className="logs-table" style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr>
                    <th style={{ textAlign: 'left', padding: 8 }}>Timestamp</th>
                    <th style={{ textAlign: 'left', padding: 8 }}>User</th>
                    <th style={{ textAlign: 'left', padding: 8 }}>Action</th>
                    
                    <th style={{ textAlign: 'left', padding: 8 }}>Category</th>
                    <th style={{ textAlign: 'left', padding: 8 }}>Value</th>
                    <th style={{ textAlign: 'left', padding: 8 }}>Date</th>
                    <th style={{ textAlign: 'left', padding: 8 }}>Details</th>
                  </tr>
                </thead>
                <tbody>
                  {logs.map((l) => (
                    <tr key={l.id} style={{ borderTop: '1px solid rgba(255,255,255,0.04)' }}>
                      <td style={{ padding: 10, whiteSpace: 'nowrap' }}>{l.createdAt ? new Date(l.createdAt).toLocaleString() : '-'}</td>
                      <td style={{ padding: 10 }}>{l.username || 'Anonymous'}</td>
                      <td style={{ padding: 10 }}>{l.action || l.type || '-'}</td>
                      <td style={{ padding: 10 }}>{l.category || '-'}</td>
                      <td style={{ padding: 10 }}>{l.value || '-'}</td>
                      <td style={{ padding: 10 }}>{l.date || '-'}</td>
                      <td style={{ padding: 10 }}>
                        <details>
                          <summary style={{ cursor: 'pointer' }}>Raw</summary>
                          <pre style={{ margin: 0, whiteSpace: 'pre-wrap', fontSize: 12 }}>{l.params || l.rawParams || '-'}</pre>
                        </details>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}

      </div>
    </MotionWrapper>
  )
}
