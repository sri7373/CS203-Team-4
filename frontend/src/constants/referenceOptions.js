export const COUNTRY_CODES = [
  { code: 'SG', name: 'Singapore', flag: '🇸🇬' },
  { code: 'US', name: 'United States', flag: '🇺🇸' },
  { code: 'CN', name: 'China', flag: '🇨🇳' },
  { code: 'MY', name: 'Malaysia', flag: '🇲🇾' },
  { code: 'ID', name: 'Indonesia', flag: '🇮🇩' },
  { code: 'GB', name: 'United Kingdom', flag: '🇬🇧' },
  { code: 'JP', name: 'Japan', flag: '🇯🇵' },
  { code: 'KR', name: 'South Korea', flag: '🇰🇷' },
  { code: 'IN', name: 'India', flag: '🇮🇳' },
  { code: 'DE', name: 'Germany', flag: '🇩🇪' },
  { code: 'FR', name: 'France', flag: '🇫🇷' },
  { code: 'CA', name: 'Canada', flag: '🇨🇦' },
  { code: 'AU', name: 'Australia', flag: '🇦🇺' },
  { code: 'MX', name: 'Mexico', flag: '🇲🇽' },
  { code: 'BR', name: 'Brazil', flag: '🇧🇷' }
]

export const PRODUCT_CATEGORIES = [
  { value: 'steel', label: '🏗️ Steel & Metals', keywords: 'steel, iron, aluminum, copper, metals' },
  { value: 'electronics', label: '📱 Electronics', keywords: 'electronics, semiconductors, chips, computers' },
  { value: 'automotive', label: '🚗 Automotive', keywords: 'automotive, cars, vehicles, auto parts' },
  { value: 'agriculture', label: '🌾 Agriculture', keywords: 'agriculture, farming, crops, food products' },
  { value: 'textiles', label: '👔 Textiles & Clothing', keywords: 'textiles, clothing, apparel, fashion' },
  { value: 'pharmaceuticals', label: '💊 Pharmaceuticals', keywords: 'pharmaceuticals, drugs, medicines, medical' },
  { value: 'chemicals', label: '⚗️ Chemicals', keywords: 'chemicals, plastics, petrochemicals' },
  { value: 'machinery', label: '⚙️ Machinery', keywords: 'machinery, equipment, industrial machinery' },
  { value: 'energy', label: '⚡ Energy', keywords: 'energy, oil, gas, renewable energy, solar' },
  { value: 'lumber', label: '🪵 Lumber & Wood', keywords: 'lumber, wood, timber, forestry' }
]

// Legacy exports for backward compatibility
export const COUNTRY_CODES_LEGACY = [
  'SGP',
  'USA',
  'CHN',
  'MYS',
  'IDN'
]

export const PRODUCT_CATEGORY_CODES = [
  'STEEL',
  'ELEC',
  'FOOD'
]

export const DEFAULT_ORIGIN_CODE = COUNTRY_CODES_LEGACY[0] || ''
export const DEFAULT_DESTINATION_CODE = COUNTRY_CODES_LEGACY[1] || COUNTRY_CODES_LEGACY[0] || ''
export const DEFAULT_PRODUCT_CATEGORY = PRODUCT_CATEGORY_CODES[0] || ''

