import React, { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import MotionWrapper from '../components/MotionWrapper.jsx'
import Select from '../components/Select.jsx'
import api from '../services/api.js'

const COUNTRIES = ['SGP','USA','CHN','MYS','IDN']

const currencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2
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
  // If value is already a percentage (like 5.25), divide by 100
  const percentValue = Number(value) > 1 ? Number(value) / 100 : Number(value)
  return percentFormatter.format(percentValue)
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
        <p>No tariff data available in database.</p>
        <p className="tiny">Check if tariff rates exist for this country's trade routes.</p>
      </div>
    )
    return (
      <div style={{display:'grid', gap:12}}>
        {items.map((item, index) => (
          <motion.div key={item.code} className="metric-card"
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3, delay: index * 0.1 }}>
            <div style={{display:'flex', justifyContent:'space-between', alignItems:'flex-start'}}>
              <div style={{flex: 1}}>
                <div style={{display:'flex', alignItems:'center', marginBottom:8}}>
                  <span style={{fontSize:'16px', fontWeight:600, color:'var(--color-text)'}}>{item.name}</span>
                  <span className="badge subtle" style={{marginLeft:8, fontSize:'12px'}}>{item.code}</span>
                </div>
                <div style={{display:'grid', gridTemplateColumns:'1fr 1fr', gap:12, fontSize:'14px'}}>
                  <div>
                    <span className="label" style={{fontSize:'12px', opacity:0.7}}>Base Rate</span>
                    <div style={{fontWeight:600, color:'var(--color-text)'}}>{formatPercent(item.baseRate)}</div>
                  </div>
                  <div>
                    <span className="label" style={{fontSize:'12px', opacity:0.7}}>Additional Fee</span>
                    <div style={{fontWeight:600, color:'var(--color-text)'}}>{formatCurrency(item.additionalFee)}</div>
                  </div>
                </div>
              </div>
            </div>
          </motion.div>
        ))}
      </div>
    )
  }

  const renderPartnerList = (items = []) => {
    if (!items.length) return (
      <div className="small" style={{marginTop:8, padding:12, textAlign:'center', opacity:0.7}}>
        <p>No trading partner tariff data available.</p>
        <p className="tiny">Check if tariff relationships exist for this country.</p>
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
              <div style={{textAlign:'right'}}>
                <div className="label" style={{fontSize:'12px', opacity:0.7}}>Avg Tariff Rate</div>
                <span style={{fontSize:'14px', fontWeight:600}}>{formatPercent(item.totalValue)}</span>
              </div>
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
          View tariff rates and fees by product category, including base percentage rates and additional fixed charges.
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
                <h4 id="importsHeading" className="label" style={{marginBottom:12, fontSize:'14px', fontWeight:600}}>Top Import Categories by Tariff Rate</h4>
                {renderProductList(insights.topImports)}
              </motion.section>
              
              <motion.section aria-labelledby="exportsHeading"
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: 0.2 }}>
                <h4 id="exportsHeading" className="label" style={{marginBottom:12, fontSize:'14px', fontWeight:600}}>Top Export Categories by Tariff Rate</h4>
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
              <h4 id="partnersHeading" className="label" style={{marginBottom:12, fontSize:'14px', fontWeight:600}}>Major Trade Partners by Average Tariff Rate</h4>
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
