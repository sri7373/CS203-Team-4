export function normalizeStoredPercent(value) {
  const num = Number(value);
  if (Number.isNaN(num)) return null;
  return num / 100;
}

export function formatStoredPercent(value, fallback = "-", fractionDigits = 2) {
  const decimal = normalizeStoredPercent(value);
  if (decimal === null) return fallback;
  const formatter = new Intl.NumberFormat("en-US", {
    style: "percent",
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  });
  return formatter.format(decimal);
}
