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
      console.error('Failed to load query logs', err)
      setError(err?.formattedMessage || err?.message || 'Failed to load query logs')
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
                    <th style={{ textAlign: 'left', padding: 8 }}>When</th>
                    <th style={{ textAlign: 'left', padding: 8 }}>User</th>
                    <th style={{ textAlign: 'left', padding: 8 }}>Type</th>
                    <th style={{ textAlign: 'left', padding: 8 }}>Params</th>
                  </tr>
                </thead>
                <tbody>
                  {logs.map((l) => (
                    <tr key={l.id} style={{ borderTop: '1px solid rgba(255,255,255,0.04)' }}>
                      <td style={{ padding: 10, whiteSpace: 'nowrap' }}>{l.createdAt ? new Date(l.createdAt).toLocaleString() : '-'}</td>
                      <td style={{ padding: 10 }}>{l.username || (l.userRole ? `${l.userRole}` : 'Anonymous')}</td>
                      <td style={{ padding: 10 }}>{l.type}</td>
                      <td style={{ padding: 10, fontFamily: 'monospace', fontSize: 12 }}>{l.params}</td>
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
