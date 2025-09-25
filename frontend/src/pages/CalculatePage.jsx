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

  const downloadPdf = async () => {
    setError(null);
    try {
      const payload = {
        originCountryCode: origin,
        destinationCountryCode: destination,
        productCategoryCode: category,
        declaredValue: Number(declared),
        date: date || undefined
      };
  
      const response = await fetch("http://localhost:8080/api/tariffs/calculate/pdf", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
  
      if (!response.ok) {
        throw new Error("Failed to generate PDF");
      }
  
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
  
      // trigger download
      const a = document.createElement("a");
      a.href = url;
      a.download = "tariff-report.pdf";
      a.click();
  
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error("PDF error:", err);
      setError(err.message || "PDF generation failed");
    }
  };
  

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
          <button type="button" onClick={downloadPdf} disabled={loading}>Download PDF</button>
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
            <h3 className="neon-subtle" style={{fontWeight:600, marginBottom:12}}>Result Breakdown</h3>
            <div className="result-panel glow-border">
              <motion.table aria-label="Tariff calculation breakdown" className="table-glow smart-table"
                initial={{ opacity:0 }}
                animate={{ opacity:1 }}
                transition={{ delay:.1 }}>
                <tbody>
                  <tr><th>Origin</th><td>{res.originCountryCode}</td></tr>
                  <tr><th>Destination</th><td>{res.destinationCountryCode}</td></tr>
                  <tr><th>Category</th><td>{res.productCategoryCode}</td></tr>
                  <tr><th>Effective Date</th><td>{res.effectiveDate}</td></tr>
                  <tr><th>Declared Value</th><td>{formatCurrency(res.declaredValue)}</td></tr>
                  <tr><th>Base Rate</th><td>{res.baseRate}</td></tr>
                  <tr><th>Tariff Amount</th><td>{formatCurrency(res.tariffAmount)}</td></tr>
                  <tr><th>Additional Fee</th><td>{formatCurrency(res.additionalFee)}</td></tr>
                  <tr className="total-row"><th>Total Cost</th><td><b>{formatCurrency(res.totalCost)}</b></td></tr>
                </tbody>
              </motion.table>
              <div className="panel-foot small">Total = declaredValue + (declaredValue * baseRate) + additionalFee</div>
            </div>
            {res.notes && <div className="small" style={{marginTop:12}}>{res.notes}</div>}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
    </MotionWrapper>
  )
}
