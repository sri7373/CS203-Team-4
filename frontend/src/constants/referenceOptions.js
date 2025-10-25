export const COUNTRY_CODES = [
  { value: "SGP", label: "Singapore", code: "SGP" },
  { value: "USA", label: "United States", code: "USA" },
  { value: "CHN", label: "China", code: "CHN" },
  { value: "MYS", label: "Malaysia", code: "MYS" },
  { value: "IDN", label: "Indonesia", code: "IDN" },
  { value: "GBR", label: "United Kingdom", code: "GBR" },
  { value: "JPN", label: "Japan", code: "JPN" },
  { value: "KOR", label: "South Korea", code: "KOR" },
  { value: "IND", label: "India", code: "IND" },
  { value: "DEU", label: "Germany", code: "DEU" },
  { value: "FRA", label: "France", code: "FRA" },
  { value: "CAN", label: "Canada", code: "CAN" },
  { value: "AUS", label: "Australia", code: "AUS" },
  { value: "MEX", label: "Mexico", code: "MEX" },
  { value: "BRA", label: "Brazil", code: "BRA" },
];

export const PRODUCT_CATEGORIES = [
  { value: "STEEL", label: "Steel & Metals", code: "STEEL" },
  { value: "ELEC", label: "Electronics", code: "ELEC" },
  { value: "AUTO", label: "Automotive", code: "AUTO" },
  { value: "AGRI", label: "Agriculture", code: "AGRI" },
  { value: "TEXT", label: "Textiles & Clothing", code: "TEXT" },
  { value: "PHAR", label: "Pharmaceuticals", code: "PHAR" },
  { value: "CHEM", label: "Chemicals", code: "CHEM" },
  { value: "MACH", label: "Machinery", code: "MACH" },
  { value: "ENER", label: "Energy", code: "ENER" },
  { value: "WOOD", label: "Lumber & Wood", code: "WOOD" },
];

export const COUNTRY_CODES_LEGACY = COUNTRY_CODES.map((country) => country.value);
export const PRODUCT_CATEGORY_CODES = PRODUCT_CATEGORIES.map((category) => category.value);

export const DEFAULT_ORIGIN_CODE = COUNTRY_CODES_LEGACY[0] || "";
export const DEFAULT_DESTINATION_CODE = COUNTRY_CODES_LEGACY[1] || COUNTRY_CODES_LEGACY[0] || "";
export const DEFAULT_PRODUCT_CATEGORY = PRODUCT_CATEGORY_CODES[0] || "";
