import { useEffect, useMemo, useState } from "react";
import {
  fetchCountries,
  fetchProductCategories,
} from "../services/reference.js";
import {
  COUNTRY_CODES as FALLBACK_COUNTRIES,
  PRODUCT_CATEGORIES as FALLBACK_CATEGORIES,
} from "../constants/referenceOptions.js";

const normalizeCountry = (item) => {
  if (!item) {
    return null;
  }
  const raw = item.code ?? item.value ?? "";
  if (!raw) {
    return null;
  }
  const value = raw.toUpperCase();
  const label = item.name ?? item.label ?? value;
  return { value, label };
};

const normalizeCategory = (item) => {
  if (!item) {
    return null;
  }
  const raw = item.code ?? item.value ?? "";
  if (!raw) {
    return null;
  }
  const value = raw.toUpperCase();
  const label = item.name ?? item.label ?? value;
  return { value, label };
};

const dedupeByValue = (options) => {
  const seen = new Set();
  const result = [];
  options.forEach((option) => {
    if (!option) return;
    if (seen.has(option.value)) return;
    seen.add(option.value);
    result.push(option);
  });
  return result;
};

export function useReferenceOptions() {
  const [countries, setCountries] = useState(() =>
    dedupeByValue(FALLBACK_COUNTRIES.map(normalizeCountry))
  );
  const [categories, setCategories] = useState(() =>
    dedupeByValue(FALLBACK_CATEGORIES.map(normalizeCategory))
  );
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const [countriesResponse, categoriesResponse] = await Promise.all([
          fetchCountries(),
          fetchProductCategories(),
        ]);

        if (cancelled) return;

        const normalizedCountries = dedupeByValue(
          countriesResponse.map(normalizeCountry)
        );
        if (normalizedCountries.length) {
          setCountries(normalizedCountries);
        }

        const normalizedCategories = dedupeByValue(
          categoriesResponse.map(normalizeCategory)
        );
        if (normalizedCategories.length) {
          setCategories(normalizedCategories);
        }
      } catch (err) {
        if (!cancelled) {
          console.error("Failed to load reference options", err);
          setError(err);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    load();

    return () => {
      cancelled = true;
    };
  }, []);

  return useMemo(
    () => ({
      countries,
      categories,
      loading,
      error,
    }),
    [countries, categories, loading, error]
  );
}

