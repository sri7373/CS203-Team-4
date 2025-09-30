export const COUNTRY_CODES = [
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

export const DEFAULT_ORIGIN_CODE = COUNTRY_CODES[0] || ''
export const DEFAULT_DESTINATION_CODE = COUNTRY_CODES[1] || COUNTRY_CODES[0] || ''
export const DEFAULT_PRODUCT_CATEGORY = PRODUCT_CATEGORY_CODES[0] || ''
