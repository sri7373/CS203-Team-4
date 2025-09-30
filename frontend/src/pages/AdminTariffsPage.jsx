import React, { useCallback, useEffect, useMemo, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import MotionWrapper from '../components/MotionWrapper.jsx'
import Select from '../components/Select.jsx'
import {
  COUNTRY_CODES,
  DEFAULT_DESTINATION_CODE,
  DEFAULT_ORIGIN_CODE,
  DEFAULT_PRODUCT_CATEGORY,
  PRODUCT_CATEGORY_CODES
} from '../constants/referenceOptions.js'
import api from '../services/api.js'
import { fetchCountries, fetchProductCategories } from '../services/reference.js'

const toOptionValue = (option) => (typeof option === 'string' ? option : option?.value || '')
const listOptionValues = (options) => options.map(toOptionValue).filter(Boolean)
const resolveOptionValue = (current, options, fallback) => {
  const values = listOptionValues(options)
  if (current && values.includes(current)) return current
  if (fallback && values.includes(fallback)) return fallback
  return values[0] || ''
}

const EMPTY_FORM = {
  originCountryCode: '',
  destinationCountryCode: '',
  productCategoryCode: '',
  baseRate: '',
  additionalFee: '',
  effectiveFrom: '',
  effectiveTo: ''
}

export default function AdminTariffsPage() {
  const [tariffs, setTariffs] = useState([])
  const [loading, setLoading] = useState(false)
  const [listError, setListError] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [editing, setEditing] = useState(null)
  const [formError, setFormError] = useState(null)
  const [feedback, setFeedback] = useState(null)
  const [saving, setSaving] = useState(false)
  const [filter, setFilter] = useState('')
  const [deletingId, setDeletingId] = useState(null)
  const [referenceError, setReferenceError] = useState(null)
  const [refLoading, setRefLoading] = useState(true)
  const [countries, setCountries] = useState([])
  const [categories, setCategories] = useState([])

  const loadReferenceData = useCallback(async () => {
    setRefLoading(true)
    try {
      const [countryData, categoryData] = await Promise.all([
        fetchCountries(),
        fetchProductCategories()
      ])
      const countryLookup = new Map(countryData.map(item => [item.code, item]))
      const categoryLookup = new Map(categoryData.map(item => [item.code, item]))
      const allowedCountries = COUNTRY_CODES.map(code => ({
        value: code,
        label: code,
        name: countryLookup.get(code)?.name || ''
      }))
      const allowedCategories = PRODUCT_CATEGORY_CODES.map(code => ({
        value: code,
        label: code,
        name: categoryLookup.get(code)?.name || ''
      }))
      setCountries(allowedCountries)
      setCategories(allowedCategories)
      setForm(prev => ({
        ...prev,
        originCountryCode: resolveOptionValue(prev.originCountryCode, allowedCountries, DEFAULT_ORIGIN_CODE),
        destinationCountryCode: resolveOptionValue(prev.destinationCountryCode, allowedCountries, DEFAULT_DESTINATION_CODE),
        productCategoryCode: resolveOptionValue(prev.productCategoryCode, allowedCategories, DEFAULT_PRODUCT_CATEGORY)
      }))
      setReferenceError(null)
    } catch (err) {
      console.error('Failed to load reference data', err)
      const fallbackCountries = COUNTRY_CODES.map(code => ({ value: code, label: code }))
      const fallbackCategories = PRODUCT_CATEGORY_CODES.map(code => ({ value: code, label: code }))
      setCountries(fallbackCountries)
      setCategories(fallbackCategories)
      setForm(prev => ({
        ...prev,
        originCountryCode: resolveOptionValue(prev.originCountryCode, fallbackCountries, DEFAULT_ORIGIN_CODE),
        destinationCountryCode: resolveOptionValue(prev.destinationCountryCode, fallbackCountries, DEFAULT_DESTINATION_CODE),
        productCategoryCode: resolveOptionValue(prev.productCategoryCode, fallbackCategories, DEFAULT_PRODUCT_CATEGORY)
      }))
      setReferenceError('Unable to load reference data. Using default options.')
    } finally {
      setRefLoading(false)
    }
  }, [])

  useEffect(() => {
    loadReferenceData()
  }, [loadReferenceData])

  const loadTariffs = useCallback(async () => {
    setLoading(true)
    setListError(null)
    try {
      const res = await api.get('/api/tariffs')
      setTariffs(Array.isArray(res.data) ? res.data : [])
    } catch (err) {
      console.error('Failed to load tariffs', err)
      setListError(err?.formattedMessage || err?.response?.data?.message || 'Failed to load tariff rates')
      setTariffs([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadTariffs()
  }, [loadTariffs])

  const filteredTariffs = useMemo(() => {
    if (!filter.trim()) return tariffs
    const q = filter.trim().toLowerCase()
    return tariffs.filter((t) => [t.id, t.originCountryCode, t.destinationCountryCode, t.productCategoryCode]
      .filter(Boolean)
      .map(String)
      .some(value => value.toLowerCase().includes(q)))
  }, [tariffs, filter])

  const resetForm = () => {
    setEditing(null)
    setForm({
      ...EMPTY_FORM,
      originCountryCode: resolveOptionValue(DEFAULT_ORIGIN_CODE, countries, DEFAULT_ORIGIN_CODE),
      destinationCountryCode: resolveOptionValue(DEFAULT_DESTINATION_CODE, countries, DEFAULT_DESTINATION_CODE),
      productCategoryCode: resolveOptionValue(DEFAULT_PRODUCT_CATEGORY, categories, DEFAULT_PRODUCT_CATEGORY)
    })
    setFormError(null)
    setFeedback(null)
  }

  const beginEdit = (tariff) => {
    setEditing(tariff)
    setForm({
      originCountryCode: tariff.originCountryCode || '',
      destinationCountryCode: tariff.destinationCountryCode || '',
      productCategoryCode: tariff.productCategoryCode || '',
      baseRate: tariff.baseRate != null ? String(tariff.baseRate) : '',
      additionalFee: tariff.additionalFee != null ? String(tariff.additionalFee) : '',
      effectiveFrom: tariff.effectiveFrom || '',
      effectiveTo: tariff.effectiveTo || ''
    })
    setFormError(null)
    setFeedback(null)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const handleSelectChange = (key) => (value) => {
    setForm(prev => ({ ...prev, [key]: value }))
  }

  const handleChange = (event) => {
    const { name, value } = event.target
    setForm(prev => ({ ...prev, [name]: value }))
  }

  const ensureReferenceValue = (value, options) => listOptionValues(options).includes(value)

  const buildPayload = () => ({
    originCountryCode: form.originCountryCode?.trim().toUpperCase(),
    destinationCountryCode: form.destinationCountryCode?.trim().toUpperCase(),
    productCategoryCode: form.productCategoryCode?.trim().toUpperCase(),
    baseRate: form.baseRate === '' ? null : Number(form.baseRate),
    additionalFee: form.additionalFee === '' ? null : Number(form.additionalFee),
    effectiveFrom: form.effectiveFrom || null,
    effectiveTo: form.effectiveTo || null
  })

  const handleSubmit = async (event) => {
    event.preventDefault()
    setFormError(null)
    setFeedback(null)

    const payload = buildPayload()

    if (!payload.originCountryCode || !payload.destinationCountryCode || !payload.productCategoryCode) {
      setFormError('Origin, destination and category are required.')
      return
    }
    if (!ensureReferenceValue(payload.originCountryCode, countries) ||
        !ensureReferenceValue(payload.destinationCountryCode, countries)) {
      setFormError('Origin and destination must match a known country code.')
      return
    }
    if (!ensureReferenceValue(payload.productCategoryCode, categories)) {
      setFormError('Product category must match a known category code.')
      return
    }
    if (payload.baseRate == null || Number.isNaN(payload.baseRate)) {
      setFormError('Base rate is required and must be numeric.')
      return
    }
    if (payload.additionalFee == null || Number.isNaN(payload.additionalFee)) {
      setFormError('Additional fee is required and must be numeric.')
      return
    }
    if (!payload.effectiveFrom) {
      setFormError('Effective-from date is required.')
      return
    }

    setSaving(true)
    try {
      if (editing) {
        await api.put(`/api/tariffs/${editing.id}`, payload)
        setFeedback('Tariff updated successfully.')
      } else {
        await api.post('/api/tariffs', payload)
        setFeedback('Tariff created successfully.')
      }
      await loadTariffs()
      if (!editing) resetForm()
    } catch (err) {
      console.error('Save failed', err)
      setFormError(err?.formattedMessage || err?.response?.data?.message || 'Save failed')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (tariff) => {
    if (!tariff) return
    if (!window.confirm(`Delete tariff #${tariff.id}? This cannot be undone.`)) return
    setDeletingId(tariff.id)
    setFeedback(null)
    setFormError(null)
    try {
      await api.delete(`/api/tariffs/${tariff.id}`)
      await loadTariffs()
      if (editing?.id === tariff.id) {
        resetForm()
      }
      setFeedback(`Tariff #${tariff.id} deleted.`)
    } catch (err) {
      console.error('Delete failed', err)
      setFormError(err?.formattedMessage || err?.response?.data?.message || 'Delete failed')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <MotionWrapper>
      <div
        className="card glass glow-border neon-focus"
        aria-labelledby="tariffAdminTitle"
        style={{ position: 'relative', overflow: 'visible' }}
      >
        <motion.h2
          id="tariffAdminTitle"
          className="neon-text"
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, ease: [0.4, 0, 0.2, 1] }}
        >
          Tariff Administration
        </motion.h2>
        <p className="small neon-subtle" style={{ marginTop: -12, marginBottom: 24 }}>
          Manage tariff schedules that drive rate calculations. Only administrators can access this console.
        </p>

        {referenceError && (
          <div
            className="error"
            role="alert"
            style={{ marginBottom: 16, marginTop: -8 }}
          >
            {referenceError}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate className="admin-form">
          <div className="inline-fields field-cluster">
            <div className="field" style={{ flex: '1 1 220px' }}>
              <label htmlFor="originCountryCode">Origin Country Code</label>
              <Select
                id="originCountryCode"
                value={form.originCountryCode}
                onChange={handleSelectChange('originCountryCode')}
                options={countries}
                disabled={refLoading || !countries.length}
                placeholder={refLoading ? 'Loading…' : '(Select)'}
              />
            </div>
            <div className="field" style={{ flex: '1 1 220px' }}>
              <label htmlFor="destinationCountryCode">Destination Country Code</label>
              <Select
                id="destinationCountryCode"
                value={form.destinationCountryCode}
                onChange={handleSelectChange('destinationCountryCode')}
                options={countries}
                disabled={refLoading || !countries.length}
                placeholder={refLoading ? 'Loading…' : '(Select)'}
              />
            </div>
            <div className="field" style={{ flex: '1 1 220px' }}>
              <label htmlFor="productCategoryCode">Product Category</label>
              <Select
                id="productCategoryCode"
                value={form.productCategoryCode}
                onChange={handleSelectChange('productCategoryCode')}
                options={categories}
                disabled={refLoading || !categories.length}
                placeholder={refLoading ? 'Loading…' : '(Select)'}
              />
            </div>
          </div>

          <div className="inline-fields field-cluster">
            <div className="field" style={{ flex: '1 1 200px' }}>
              <label htmlFor="baseRate">Base Rate (decimal)</label>
              <input
                id="baseRate"
                className="input"
                name="baseRate"
                type="number"
                step="0.0001"
                min="0"
                value={form.baseRate}
                onChange={handleChange}
                required
                placeholder="e.g. 0.05"
              />
            </div>
            <div className="field" style={{ flex: '1 1 200px' }}>
              <label htmlFor="additionalFee">Additional Fee</label>
              <input
                id="additionalFee"
                className="input"
                name="additionalFee"
                type="number"
                step="0.01"
                min="0"
                value={form.additionalFee}
                onChange={handleChange}
                required
                placeholder="e.g. 25"
              />
            </div>
            <div className="field" style={{ flex: '1 1 200px' }}>
              <label htmlFor="effectiveFrom">Effective From</label>
              <input
                id="effectiveFrom"
                className="input"
                name="effectiveFrom"
                type="date"
                value={form.effectiveFrom}
                onChange={handleChange}
                required
              />
            </div>
            <div className="field" style={{ flex: '1 1 200px' }}>
              <label htmlFor="effectiveTo">Effective To (optional)</label>
              <input
                id="effectiveTo"
                className="input"
                name="effectiveTo"
                type="date"
                value={form.effectiveTo}
                onChange={handleChange}
              />
            </div>
          </div>

          <div className="btn-group" style={{ marginTop: 12 }}>
            <button className="primary" type="submit" disabled={saving || refLoading}>
              {saving ? 'Saving…' : editing ? 'Update Tariff' : 'Create Tariff'}
            </button>
            <button type="button" onClick={resetForm} disabled={saving}>
              Clear
            </button>
            {editing && (
              <span className="tiny neon-subtle" style={{ marginLeft: 8 }}>Editing tariff #{editing.id}</span>
            )}
          </div>
        </form>

        {formError && (
          <div className="error" role="alert" style={{ marginTop: 20 }}>
            {String(formError)}
          </div>
        )}
        {feedback && (
          <div className="success" role="status" style={{ marginTop: 12 }}>
            {feedback}
          </div>
        )}
      </div>

      <div className="card glass glow-border neon-focus" style={{ marginTop: 28 }}>
        <div
          className="card-header"
          style={{ display: 'flex', flexWrap: 'wrap', gap: 16, alignItems: 'center', justifyContent: 'space-between' }}
        >
          <h3 className="neon-subtle" style={{ margin: 0 }}>Configured Tariffs</h3>
          <input
            className="input admin-filter-input"
            type="search"
            placeholder="Filter by id, origin, destination or category"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            style={{ maxWidth: 320 }}
            aria-label="Filter tariffs"
          />
        </div>

        {listError && (
          <div className="error" role="alert" style={{ marginTop: 16 }}>
            {String(listError)}
          </div>
        )}

        {loading && (
          <div className="skeleton" style={{ marginTop: 24 }}>Loading tariffs…</div>
        )}

        <AnimatePresence>
          {!loading && filteredTariffs.length > 0 && (
            <motion.table
              key={filteredTariffs.map((t) => t.id).join('-')}
              className="table-glow"
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -12 }}
              transition={{ duration: 0.4, ease: [0.4, 0, 0.2, 1] }}
              style={{ marginTop: 16 }}
            >
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Origin</th>
                  <th>Destination</th>
                  <th>Category</th>
                  <th>Base Rate</th>
                  <th>Additional Fee</th>
                  <th>Effective From</th>
                  <th>Effective To</th>
                  <th style={{ width: 140 }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredTariffs.map((tariff) => (
                  <tr key={tariff.id} className={editing?.id === tariff.id ? 'row-highlight' : undefined}>
                    <td>{tariff.id}</td>
                    <td>{tariff.originCountryCode}</td>
                    <td>{tariff.destinationCountryCode}</td>
                    <td>{tariff.productCategoryCode}</td>
                    <td>{tariff.baseRate}</td>
                    <td>{tariff.additionalFee}</td>
                    <td>{tariff.effectiveFrom}</td>
                    <td>{tariff.effectiveTo || '-'}</td>
                    <td>
                      <div className="btn-group">
                        <button type="button" onClick={() => beginEdit(tariff)}>Edit</button>
                        <button
                          type="button"
                          className="danger"
                          onClick={() => handleDelete(tariff)}
                          disabled={deletingId === tariff.id}
                        >
                          {deletingId === tariff.id ? 'Deleting…' : 'Delete'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </motion.table>
          )}
        </AnimatePresence>

        {!loading && filteredTariffs.length === 0 && !listError && (
          <div className="empty" style={{ marginTop: 24 }}>
            No tariffs found. Create a new tariff or adjust your filter.
          </div>
        )}
      </div>
    </MotionWrapper>
  )
}
