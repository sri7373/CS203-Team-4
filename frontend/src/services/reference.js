import api from './api.js'

let cache = {
  countries: null,
  categories: null
}

export async function fetchCountries() {
  if (!cache.countries) {
    const res = await api.get('/reference/countries')
    cache.countries = Array.isArray(res.data) ? res.data : []
  }
  return cache.countries
}

export async function fetchProductCategories() {
  if (!cache.categories) {
    const res = await api.get('/reference/product-categories')
    cache.categories = Array.isArray(res.data) ? res.data : []
  }
  return cache.categories
}

export function resetReferenceCache() {
  cache = { countries: null, categories: null }
}
