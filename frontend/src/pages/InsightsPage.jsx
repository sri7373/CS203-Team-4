import React, { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import MotionWrapper from '../components/MotionWrapper.jsx'
import api from '../services/api.js'

const COUNTRY_OPTIONS = [
  { value: 'SGP', label: 'Singapore' },
  { value: 'USA', label: 'United States' },
  { value: 'CHN', label: 'China' },
  { value: 'MYS', label: 'Malaysia' },
  { value: 'IDN', label: 'Indonesia' }
]

const currencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  maximumFractionDigits: 0
})

const percentFormatter = new Intl.NumberFormat('en-US', {
  style: 'percent',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2
})

const formatCurrency = (value) => {
  if (value === null || value === undefined) return 'N/A'
  return currencyFormatter.format(Number(value))
}

const formatPercent = (value) => {
  if (value === null || value === undefined) return 'N/A'
  return percentFormatter.format(Number(value))
}

export default function InsightsPage() {
  const [country, setCountry] = useState('SGP')
  const [insights, setInsights] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    const load = async () => {
      if (!country) {
        setInsights(null)
        return
      }
      setLoading(true)
      setError(null)
      try {
        const response = await api.get('/api/trade/insights', { params: { country } })
        if (!cancelled) {
          setInsights(response.data)
        }
      } catch (err) {
        if (!cancelled) {
          const msg = err?.formattedMessage || err?.message || 'Unable to load trade insights'
          setError(msg)
          setInsights(null)
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    load()
    return () => { cancelled = true }
  }, [country])

  const renderProductList = (items = []) => {
    if (!items.length) return <p className="small" style={{marginTop:8}}>No product data available.</p>
    return (
      <ol className="metric-list">
        {items.map(item => (
          <li key={item.code}>
            <div className="metric-item">
              <span><strong>{item.name}</strong> <span className="badge subtle">{item.code}</span></span>
              <span className="metric-value">{formatCurrency(item.totalValue)}</span>
            </div>
          </li>
        ))}
      </ol>
    )
  }

  const renderPartnerList = (items = []) => {
    if (!items.length) return <p className="small" style={{marginTop:8}}>No partner data available.</p>
    return (
      <ol className="metric-list">
        {items.map(item => (
          <li key={item.code}>
            <div className="metric-item">
              <span><strong>{item.name}</strong> <span className="badge subtle">{item.code}</span></span>
              <span className="metric-value">{formatCurrency(item.totalValue)}</span>
            </div>
          </li>
        ))}
      </ol>
    )
  }

  return (
    <MotionWrapper>
      <div className="card glass glow-border neon-focus" aria-labelledby="insightsTitle">
        <motion.h2 id="insightsTitle" className="neon-text"
          initial={{ opacity:0, y:16 }}
          animate={{ opacity:1, y:0 }}
          transition={{ duration:.6, ease:[0.4,0,0.2,1] }}
        >Trade Insights</motion.h2>
        <p className="small neon-subtle" style={{marginTop:-12, marginBottom:24}}>
          Explore headline trade metrics by reporting market, including leading products, tariff intensity and partner exposure.
        </p>

        <div className="inline-fields" style={{marginBottom:16}}>
          <div className="field" style={{maxWidth:260}}>
            <label htmlFor="countrySelect">Country</label>
            <select id="countrySelect" value={country} onChange={e => setCountry(e.target.value)}>
              {COUNTRY_OPTIONS.map(opt => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </div>
        </div>

        {error && <div className="error" role="alert">{error}</div>}

        {loading && (
          <motion.div style={{marginTop:24, display:'flex', alignItems:'center', gap:12}} aria-live="polite"
            initial={{ opacity:0 }}
            animate={{ opacity:1 }}>
            <div className="spinner" aria-hidden="true" />
            <span className="small">Loading insights...</span>
          </motion.div>
        )}

        {!loading && insights && (
          <div style={{display:'grid', gap:24, marginTop:24}}>
            <div className="metrics-row" style={{display:'grid', gap:24, gridTemplateColumns:'repeat(auto-fit, minmax(260px, 1fr))'}}>
              <section className="metric-panel" aria-labelledby="importsHeading">
                <h3 id="importsHeading">Top Imports</h3>
                {renderProductList(insights.topImports)}
              </section>
              <section className="metric-panel" aria-labelledby="exportsHeading">
                <h3 id="exportsHeading">Top Exports</h3>
                {renderProductList(insights.topExports)}
              </section>
            </div>

            <section className="metric-panel" aria-labelledby="tariffHeading">
              <h3 id="tariffHeading">Average Tariff Levels</h3>
              <div className="metric-grid" style={{display:'grid', gap:16, gridTemplateColumns:'repeat(auto-fit, minmax(220px, 1fr))'}}>
                <div className="metric-card">
                  <span className="label">Inbound (Imports)</span>
                  <span className="metric-highlight">{formatPercent(insights.averageImportTariff)}</span>
                  <p className="tiny">Mean base rate across tariff schedules applied to inbound goods.</p>
                </div>
                <div className="metric-card">
                  <span className="label">Outbound (Exports)</span>
                  <span className="metric-highlight">{formatPercent(insights.averageExportTariff)}</span>
                  <p className="tiny">Mean base rate negotiated on export corridors from the selected country.</p>
                </div>
              </div>
            </section>

            <section className="metric-panel" aria-labelledby="partnersHeading">
              <h3 id="partnersHeading">Major Trade Partners</h3>
              {renderPartnerList(insights.majorPartners)}
            </section>
          </div>
        )}

        {!loading && !insights && !error && (
          <p className="small" style={{marginTop:16}}>Select a country to view detailed trade analytics.</p>
        )}
      </div>
    </MotionWrapper>
  )
}
