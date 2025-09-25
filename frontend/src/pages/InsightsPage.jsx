import React, { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import MotionWrapper from '../components/MotionWrapper.jsx'
import Select from '../components/Select.jsx'
import api from '../services/api.js'

const COUNTRIES = ['SGP','USA','CHN','MYS','IDN']

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
        console.log(`Loading trade insights from AWS PostgreSQL database for: ${country}`)
        const response = await api.get('/api/trade/insights', { params: { country } })
        if (!cancelled) {
          console.log('Trade insights loaded successfully:', response.data)
          setInsights(response.data)
        }
      } catch (err) {
        if (!cancelled) {
          console.error('Failed to load trade insights:', err)
          const msg = err?.response?.data?.message || err?.formattedMessage || err?.message || 'Unable to load trade insights from database'
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
    if (!items.length) return (
      <div className="small" style={{marginTop:8, padding:12, textAlign:'center', opacity:0.7}}>
        <p>No trade data available in database.</p>
        <p className="tiny">Data may need to be imported from external trade statistics.</p>
      </div>
    )
    return (
      <div style={{display:'grid', gap:12}}>
        {items.map((item, index) => (
          <motion.div key={item.code} className="metric-card"
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3, delay: index * 0.1 }}>
            <div style={{display:'flex', justifyContent:'space-between', alignItems:'center'}}>
              <div>
                <span style={{fontSize:'16px', fontWeight:600, color:'var(--color-text)'}}>{item.name}</span>
                <span className="badge subtle" style={{marginLeft:8, fontSize:'12px'}}>{item.code}</span>
              </div>
              <span className="label" style={{fontSize:'14px', fontWeight:600}}>{formatCurrency(item.totalValue)}</span>
            </div>
          </motion.div>
        ))}
      </div>
    )
  }

  const renderPartnerList = (items = []) => {
    if (!items.length) return (
      <div className="small" style={{marginTop:8, padding:12, textAlign:'center', opacity:0.7}}>
        <p>No trading partner data available in database.</p>
        <p className="tiny">Data may need to be imported from external trade statistics.</p>
      </div>
    )
    return (
      <div style={{display:'grid', gap:12}}>
        {items.map((item, index) => (
          <motion.div key={item.code} className="metric-card"
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3, delay: index * 0.1 }}>
            <div style={{display:'flex', justifyContent:'space-between', alignItems:'center'}}>
              <div>
                <span style={{fontSize:'16px', fontWeight:600, color:'var(--color-text)'}}>{item.name}</span>
                <span className="badge subtle" style={{marginLeft:8, fontSize:'12px'}}>{item.code}</span>
              </div>
              <span className="label" style={{fontSize:'14px', fontWeight:600}}>{formatCurrency(item.totalValue)}</span>
            </div>
          </motion.div>
        ))}
      </div>
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

        <div className="inline-fields field-cluster" style={{marginBottom:16}}>
          <div className="field" style={{flex: '1 1 220px', maxWidth:260}}>
            <label htmlFor="countrySelect">Country</label>
            <Select id="countrySelect" value={country} onChange={setCountry} options={COUNTRIES} />
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
          <motion.div style={{marginTop:32}} aria-live="polite"
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, ease: [0.4, 0, 0.2, 1] }}>
            
            <h3 className="neon-subtle" style={{fontWeight:600, marginBottom:16}}>Trade Analytics Overview</h3>
            
            <div style={{display:'grid', gap:24, gridTemplateColumns:'repeat(auto-fit, minmax(300px, 1fr))', marginBottom:24}}>
              <motion.section aria-labelledby="importsHeading"
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: 0.1 }}>
                <h4 id="importsHeading" className="label" style={{marginBottom:12, fontSize:'14px', fontWeight:600}}>Top Imports</h4>
                {renderProductList(insights.topImports)}
              </motion.section>
              
              <motion.section aria-labelledby="exportsHeading"
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: 0.2 }}>
                <h4 id="exportsHeading" className="label" style={{marginBottom:12, fontSize:'14px', fontWeight:600}}>Top Exports</h4>
                {renderProductList(insights.topExports)}
              </motion.section>
            </div>

            {/* Average Tariff Levels Cards */}
            <div style={{marginBottom:20}}>
              <h4 className="label" style={{marginBottom:12, fontSize:'14px', fontWeight:600}}>Average Tariff Levels</h4>
              <div style={{display:'grid', gap:16, gridTemplateColumns:'repeat(auto-fit, minmax(220px, 1fr))'}}>
                <motion.div className="metric-card"
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.3 }}>
                  <span className="label">Inbound (Imports)</span>
                  <span style={{fontSize:'18px', fontWeight:600, color:'var(--color-text)'}}>{formatPercent(insights.averageImportTariff)}</span>
                  <p className="tiny" style={{marginTop:4, opacity:0.7}}>Mean base rate across tariff schedules applied to inbound goods.</p>
                </motion.div>
                <motion.div className="metric-card"
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.35 }}>
                  <span className="label">Outbound (Exports)</span>
                  <span style={{fontSize:'18px', fontWeight:600, color:'var(--color-text)'}}>{formatPercent(insights.averageExportTariff)}</span>
                  <p className="tiny" style={{marginTop:4, opacity:0.7}}>Mean base rate negotiated on export corridors from the selected country.</p>
                </motion.div>
              </div>
            </div>

            {/* Major Trading Partners */}
            <motion.section aria-labelledby="partnersHeading"
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.4 }}>
              <h4 id="partnersHeading" className="label" style={{marginBottom:12, fontSize:'14px', fontWeight:600}}>Major Trade Partners</h4>
              {renderPartnerList(insights.majorPartners)}
            </motion.section>
          </motion.div>
        )}

        {!loading && !insights && !error && (
          <p className="small" style={{marginTop:16}}>Select a country to view detailed trade analytics.</p>
        )}
      </div>
    </MotionWrapper>
  )
}
