import api from './api.js'

let cache = {
  countries: null,
  categories: null,
  timestamp: null
}

const CACHE_DURATION = 5 * 60 * 1000 // 5 minutes cache

function isCacheValid() {
  if (!cache.timestamp) return false
  return (Date.now() - cache.timestamp) < CACHE_DURATION
}

export async function fetchCountries(forceRefresh = false) {
  if (!forceRefresh && cache.countries && isCacheValid()) {
    return cache.countries
  }
  
  const res = await api.get('/reference/countries')
  cache.countries = Array.isArray(res.data) ? res.data : []
  cache.timestamp = Date.now()
  return cache.countries
}

export async function fetchProductCategories(forceRefresh = false) {
  if (!forceRefresh && cache.categories && isCacheValid()) {
    return cache.categories
  }
  
  const res = await api.get('/reference/product-categories')
  cache.categories = Array.isArray(res.data) ? res.data : []
  cache.timestamp = Date.now()
  return cache.categories
}

export function resetReferenceCache() {
  cache = { countries: null, categories: null, timestamp: null }
}

