import React, { useState } from 'react'
import api from '../services/api.js'
import MotionWrapper from '../components/MotionWrapper.jsx'
import { motion, AnimatePresence } from 'framer-motion'
import Select from '../components/Select.jsx'

const COUNTRIES = ['SGP','USA','CHN','MYS','IDN']
const CATEGORIES = ['STEEL','ELEC','FOOD']

export default function CalculatePage() {
  const [origin, setOrigin] = useState('SGP')
  const [destination, setDestination] = useState('USA')
  const [category, setCategory] = useState('STEEL')
  const [declared, setDeclared] = useState(1000.00)
  const [date, setDate] = useState('')
  const [res, setRes] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  const formatCurrency = (v) => new Intl.NumberFormat('en-US', { style:'currency', currency:'USD' }).format(v)

  const submit = async (e) => {
    e.preventDefault()
    setError(null)
    setRes(null)
    setLoading(true)
    try {
      const payload = {
        originCountryCode: origin,
        destinationCountryCode: destination,
        productCategoryCode: category,
        declaredValue: Number(declared),
        date: date || undefined
      }
      const r = await api.post('/api/tariffs/calculate', payload)
      setRes(r.data)
    } catch (err) {
      console.error('Calculation error:', err)
      const errorMessage = err.formattedMessage || err?.response?.data?.message || err?.message || 'Calculation error'
      setError(errorMessage)
    } finally {
      setLoading(false)
    }
  }

  return (
    <MotionWrapper>
  <div className="card glass glow-border neon-focus" aria-labelledby="calcTitle" style={{position:'relative', overflow:'visible'}}>
      <motion.h2 id="calcTitle" className="neon-text"
        initial={{ opacity:0, y:16 }}
        animate={{ opacity:1, y:0 }}
        transition={{ duration:.6, ease:[0.4,0,0.2,1] }}
      >Tariff Calculator</motion.h2>
      <p className="small neon-subtle" style={{marginTop:-12, marginBottom:24}}>Compute estimated tariff obligations and total landed cost breakdown.</p>
      <form onSubmit={submit} noValidate className="calc-form">
        <div className="inline-fields field-cluster">
          <div className="field" style={{flex: '1 1 220px'}}>
            <label htmlFor="origin">Origin Country</label>
            <Select id="origin" value={origin} onChange={setOrigin} options={COUNTRIES} />
          </div>
          <div className="field" style={{flex: '1 1 220px'}}>
            <label htmlFor="destination">Destination Country</label>
            <Select id="destination" value={destination} onChange={setDestination} options={COUNTRIES} />
          </div>
          <div className="field" style={{flex: '1 1 220px'}}>
            <label htmlFor="category">Product Category</label>
            <Select id="category" value={category} onChange={setCategory} options={CATEGORIES} />
          </div>
        </div>
        <div className="inline-fields field-cluster">
          <div className="field" style={{flex:'1 1 260px'}}>
            <label htmlFor="declared">Declared Value (USD)</label>
            <input id="declared" className="input" type="number" step="0.01" value={declared} onChange={e=>setDeclared(e.target.value)} required />
          </div>
          <div className="field" style={{flex:'1 1 260px'}}>
            <label htmlFor="date">Effective Date (optional)</label>
            <input id="date" className="input" type="date" value={date} onChange={e=>setDate(e.target.value)} />
          </div>
        </div>
        <div className="btn-group" style={{marginTop:8}}>
          <button className="primary" type="submit" disabled={loading}>{loading ? 'Calculating…' : 'Calculate'}</button>
          <button type="button" onClick={()=>{ setRes(null); setError(null); }} disabled={loading}>Reset</button>
        </div>
      </form>

      {error && <div className="error" role="alert">{error}</div>}

      {loading && (
        <motion.div style={{marginTop:24, display:'flex', alignItems:'center', gap:12}} aria-live="polite"
          initial={{ opacity:0 }} animate={{ opacity:1 }}>
          <div className="spinner" aria-hidden="true" />
          <span className="small">Running tariff computation…</span>
        </motion.div>
      )}

      <AnimatePresence mode="wait">
        {res && !loading && (
          <motion.div style={{marginTop:32}} aria-live="polite"
            key={res.id || JSON.stringify(res)}
            initial={{ opacity:0, y:12 }}
            animate={{ opacity:1, y:0 }}
            exit={{ opacity:0, y:-8 }}
            transition={{ duration:.4, ease:[0.4,0.0,0.2,1] }}
          >
            <h3 className="neon-subtle" style={{fontWeight:600, marginBottom:16}}>Result Breakdown</h3>
            
            {/* Trade Route Info Cards */}
            <div style={{display:'grid', gap:16, gridTemplateColumns:'repeat(auto-fit, minmax(200px, 1fr))', marginBottom:24}}>
              <motion.div className="metric-card" 
                initial={{ opacity:0, y:8 }} animate={{ opacity:1, y:0 }} transition={{ delay:.1 }}>
                <span className="label">Origin</span>
                <span style={{fontSize:'16px', fontWeight:600, color:'var(--color-text)'}}>{res.originCountryCode}</span>
              </motion.div>
              <motion.div className="metric-card"
                initial={{ opacity:0, y:8 }} animate={{ opacity:1, y:0 }} transition={{ delay:.15 }}>
                <span className="label">Destination</span>
                <span style={{fontSize:'16px', fontWeight:600, color:'var(--color-text)'}}>{res.destinationCountryCode}</span>
              </motion.div>
              <motion.div className="metric-card"
                initial={{ opacity:0, y:8 }} animate={{ opacity:1, y:0 }} transition={{ delay:.2 }}>
                <span className="label">Category</span>
                <span style={{fontSize:'16px', fontWeight:600, color:'var(--color-text)'}}>{res.productCategoryCode}</span>
              </motion.div>
              <motion.div className="metric-card"
                initial={{ opacity:0, y:8 }} animate={{ opacity:1, y:0 }} transition={{ delay:.25 }}>
                <span className="label">Effective Date</span>
                <span style={{fontSize:'16px', fontWeight:600, color:'var(--color-text)'}}>{res.effectiveDate}</span>
              </motion.div>
            </div>

            {/* Financial Breakdown Cards */}
            <div style={{display:'grid', gap:16, gridTemplateColumns:'repeat(auto-fit, minmax(220px, 1fr))', marginBottom:20}}>
              <motion.div className="metric-card"
                initial={{ opacity:0, y:8 }} animate={{ opacity:1, y:0 }} transition={{ delay:.3 }}>
                <span className="label">Declared Value</span>
                <span style={{fontSize:'18px', fontWeight:600, color:'var(--color-text)'}}>{formatCurrency(res.declaredValue)}</span>
              </motion.div>
              <motion.div className="metric-card"
                initial={{ opacity:0, y:8 }} animate={{ opacity:1, y:0 }} transition={{ delay:.35 }}>
                <span className="label">Base Rate</span>
                <span style={{fontSize:'18px', fontWeight:600, color:'var(--color-text)'}}>{(res.baseRate * 100).toFixed(2)}%</span>
              </motion.div>
              <motion.div className="metric-card"
                initial={{ opacity:0, y:8 }} animate={{ opacity:1, y:0 }} transition={{ delay:.4 }}>
                <span className="label">Tariff Amount</span>
                <span style={{fontSize:'18px', fontWeight:600, color:'var(--color-text)'}}>{formatCurrency(res.tariffAmount)}</span>
              </motion.div>
              <motion.div className="metric-card"
                initial={{ opacity:0, y:8 }} animate={{ opacity:1, y:0 }} transition={{ delay:.45 }}>
                <span className="label">Additional Fee</span>
                <span style={{fontSize:'18px', fontWeight:600, color:'var(--color-text)'}}>{formatCurrency(res.additionalFee)}</span>
              </motion.div>
            </div>

            {/* Total Cost - Prominent Display */}
            <motion.div className="result-panel glow-border" style={{textAlign:'center', padding:'20px'}}
              initial={{ opacity:0, scale:0.95 }} animate={{ opacity:1, scale:1 }} transition={{ delay:.5 }}>
              <div className="label" style={{marginBottom:8, fontSize:'14px'}}>TOTAL COST</div>
              <div style={{fontSize:'32px', fontWeight:700, background:'linear-gradient(135deg, #6366f1, #8b5cf6)', backgroundClip:'text', WebkitBackgroundClip:'text', color:'transparent'}}>
                {formatCurrency(res.totalCost)}
              </div>
              <div className="small" style={{marginTop:8, opacity:0.7}}>
                Total = declaredValue + (declaredValue × baseRate) + additionalFee
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
    </MotionWrapper>
  )
}
