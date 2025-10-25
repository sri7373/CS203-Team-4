import React, { useState } from "react";
import api from "../services/api.js";
import MotionWrapper from "../components/MotionWrapper.jsx";
import { motion, AnimatePresence } from "framer-motion";
import Select from "../components/Select.jsx";
import {
  COUNTRY_CODES,
  PRODUCT_CATEGORY_CODES,
} from "../constants/referenceOptions.js";

export default function RatesPage() {
  const [origin, setOrigin] = useState("");
  const [destination, setDestination] = useState("");
  const [category, setCategory] = useState("");
  const [rows, setRows] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const search = async (e) => {
    e?.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (origin) params.set("origin", origin);
      if (destination) params.set("destination", destination);
      if (category) params.set("category", category);
      const r = await api.get("/tariffs/rates?" + params.toString());
      setRows(r.data);
    } catch (err) {
      setError(err?.response?.data || "Search failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <MotionWrapper>
      <div
        className="card glass glow-border neon-focus"
        aria-labelledby="ratesTitle"
        style={{ position: "relative" }}
      >
        <motion.h2
          id="ratesTitle"
          className="neon-text"
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, ease: [0.4, 0, 0.2, 1] }}
        >
          Rates Search
        </motion.h2>
        <p
          className="small neon-subtle"
          style={{ marginTop: -12, marginBottom: 24 }}
        >
          Query configured tariff rate schedules by origin, destination and
          product category.
        </p>
        <form onSubmit={search} noValidate>
          <div className="inline-fields field-cluster">
            <div className="field" style={{ flex: "1 1 200px" }}>
              <label htmlFor="origin">Origin</label>
              <Select
                id="origin"
                value={origin}
                onChange={setOrigin}
                options={COUNTRY_CODES}
                placeholder="(Any)"
              />
            </div>
            <div className="field" style={{ flex: "1 1 200px" }}>
              <label htmlFor="destination">Destination</label>
              <Select
                id="destination"
                value={destination}
                onChange={setDestination}
                options={COUNTRY_CODES}
                placeholder="(Any)"
              />
            </div>
            <div className="field" style={{ flex: "1 1 200px" }}>
              <label htmlFor="category">Category</label>
              <Select
                id="category"
                value={category}
                onChange={setCategory}
                options={PRODUCT_CATEGORY_CODES}
                placeholder="(Any)"
              />
            </div>
          </div>
          <div className="btn-group" style={{ marginTop: 8 }}>
            <button className="primary" type="submit" disabled={loading}>
              {loading ? "Searching…" : "Search"}
            </button>
            <button
              type="button"
              onClick={() => {
                setOrigin("");
                setDestination("");
                setCategory("");
                setRows([]);
                setError(null);
              }}
              disabled={loading}
            >
              Reset
            </button>
          </div>
        </form>

        {error && (
          <div className="error" role="alert">
            {String(error)}
          </div>
        )}

        {loading && (
          <motion.div
            style={{
              marginTop: 24,
              display: "flex",
              alignItems: "center",
              gap: 12,
            }}
            aria-live="polite"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
          >
            <div className="spinner" aria-hidden="true" />
            <span className="small">Fetching matching tariff rates…</span>
          </motion.div>
        )}

        <div style={{ marginTop: 32 }}>
          <AnimatePresence mode="wait">
            {rows.length > 0 && !loading && (
              <motion.table
                aria-label="Tariff rates results"
                className="table-glow"
                key={rows.map((r) => r.id).join(",")}
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -8 }}
                transition={{ duration: 0.45, ease: [0.4, 0.0, 0.2, 1] }}
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
                  </tr>
                </thead>
                <tbody>
                  {rows.map((r) => (
                    <tr key={r.id}>
                      <td>{r.id}</td>
                      <td>{r.originCountryCode}</td>
                      <td>{r.destinationCountryCode}</td>
                      <td>{r.productCategoryCode}</td>
                      <td>{r.baseRate}</td>
                      <td>{r.additionalFee}</td>
                      <td>{r.effectiveFrom}</td>
                      <td>{r.effectiveTo || "-"}</td>
                    </tr>
                  ))}
                </tbody>
              </motion.table>
            )}
          </AnimatePresence>
          {!loading && rows.length === 0 && (
            <div className="small" style={{ marginTop: 8 }}>
              No results yet. Run a search to view configured tariff rates.
            </div>
          )}
        </div>
      </div>
    </MotionWrapper>
  );
}
