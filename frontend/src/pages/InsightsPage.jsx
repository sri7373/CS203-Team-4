import React, { useEffect, useState } from "react";
import { motion } from "framer-motion";
import MotionWrapper from "../components/MotionWrapper.jsx";
import Select from "../components/Select.jsx";
import api from "../services/api.js";

const COUNTRIES = ["SGP", "USA", "CHN", "MYS", "IDN"];

const currencyFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const percentFormatter = new Intl.NumberFormat("en-US", {
  style: "percent",
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const formatCurrency = (value) => {
  if (value === null || value === undefined) return "N/A";
  return currencyFormatter.format(Number(value));
};

const formatPercent = (value) => {
  if (value === null || value === undefined) return "N/A";
  // If value is already a percentage (like 5.25), divide by 100
  const percentValue = Number(value) > 1 ? Number(value) / 100 : Number(value);
  return percentFormatter.format(percentValue);
};

export default function InsightsPage() {
  const [country, setCountry] = useState("SGP");
  const [insights, setInsights] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [retryCount, setRetryCount] = useState(0);
  const [lastLoadedCountry, setLastLoadedCountry] = useState(null);

  const loadInsights = async () => {
    if (!country) {
      setError("Please select a country");
      return;
    }

    setLoading(true);
    setError(null);
    const controller = new AbortController();

    try {
      console.log(
        `Loading trade insights from AWS PostgreSQL database for: ${country}`
      );

      // Add timeout to the request
      const timeoutId = setTimeout(() => controller.abort(), 15000); // 15 second timeout

      const response = await api.get("/api/trade/insights", {
        params: { country },
        signal: controller.signal,
      });

      clearTimeout(timeoutId);

      console.log("Trade insights loaded successfully:", response.data);
      setInsights(response.data);
      setLastLoadedCountry(country);
      setRetryCount(0); // Reset retry count on success
    } catch (err) {
      if (err.name !== "AbortError") {
        console.error("Failed to load trade insights:", err);

        let errorMessage;
        if (err.name === "AbortError") {
          errorMessage =
            "Request timed out. The server may be busy. Please try again.";
        } else if (
          err.code === "NETWORK_ERROR" ||
          err.message.includes("Network Error")
        ) {
          errorMessage =
            "Network connection failed. Please check your internet connection.";
        } else if (err?.response?.status >= 500) {
          errorMessage =
            "Server error occurred. Our team has been notified. Please try again in a few moments.";
        } else if (err?.response?.status === 404) {
          errorMessage = `No trade data found for ${country}. This country may not have tariff relationships in our database.`;
        } else if (err?.response?.status === 429) {
          errorMessage =
            "Too many requests. Please wait a moment before trying again.";
        } else {
          errorMessage =
            err?.response?.data?.message ||
            err?.formattedMessage ||
            err?.message ||
            "Unable to load trade insights from database";
        }

        setError(errorMessage);
        setInsights(null);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    setRetryCount(0);
    loadInsights();
  };

  const handleClear = () => {
    setCountry("SGP");
    setInsights(null);
    setError(null);
    setRetryCount(0);
    setLastLoadedCountry(null);
  };

  const handleRetry = () => {
    if (retryCount < 3) {
      setRetryCount(retryCount + 1);
      loadInsights();
    }
  };

  const renderProductList = (items = [], type = "import") => {
    if (!items.length)
      return (
        <motion.div
          className="small"
          style={{
            marginTop: 8,
            padding: 16,
            textAlign: "center",
            opacity: 0.7,
            background: "rgba(255,255,255,0.05)",
            borderRadius: 8,
          }}
          initial={{ opacity: 0 }}
          animate={{ opacity: 0.7 }}
        >
          <p style={{ marginBottom: 8 }}>No {type} tariff data available.</p>
          <p className="tiny" style={{ opacity: 0.8 }}>
            This could mean:
          </p>
          <ul
            style={{
              fontSize: "11px",
              textAlign: "left",
              marginTop: 4,
              opacity: 0.8,
            }}
          >
            <li>No active tariff schedules for this trade direction</li>
            <li>Country not in our current database coverage</li>
            <li>Data is being updated</li>
          </ul>
        </motion.div>
      );
    return (
      <div style={{ display: "grid", gap: 12 }}>
        {items.map((item, index) => (
          <motion.div
            key={item.code}
            className="metric-card"
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3, delay: index * 0.1 }}
          >
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "flex-start",
              }}
            >
              <div style={{ flex: 1 }}>
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    marginBottom: 8,
                  }}
                >
                  <span
                    style={{
                      fontSize: "16px",
                      fontWeight: 600,
                      color: "var(--color-text)",
                    }}
                  >
                    {item.name}
                  </span>
                  <span
                    className="badge subtle"
                    style={{ marginLeft: 8, fontSize: "12px" }}
                  >
                    {item.code}
                  </span>
                </div>
                <div
                  style={{
                    display: "grid",
                    gridTemplateColumns: "1fr 1fr",
                    gap: 12,
                    fontSize: "14px",
                  }}
                >
                  <div>
                    <span
                      className="label"
                      style={{ fontSize: "12px", opacity: 0.7 }}
                    >
                      Base Rate
                    </span>
                    <div
                      style={{ fontWeight: 600, color: "var(--color-text)" }}
                    >
                      {formatPercent(item.baseRate)}
                    </div>
                  </div>
                  <div>
                    <span
                      className="label"
                      style={{ fontSize: "12px", opacity: 0.7 }}
                    >
                      Additional Fee
                    </span>
                    <div
                      style={{ fontWeight: 600, color: "var(--color-text)" }}
                    >
                      {formatCurrency(item.additionalFee)}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </motion.div>
        ))}
      </div>
    );
  };

  const renderPartnerList = (items = []) => {
    if (!items.length)
      return (
        <motion.div
          className="small"
          style={{
            marginTop: 8,
            padding: 16,
            textAlign: "center",
            opacity: 0.7,
            background: "rgba(255,255,255,0.05)",
            borderRadius: 8,
          }}
          initial={{ opacity: 0 }}
          animate={{ opacity: 0.7 }}
        >
          <p style={{ marginBottom: 8 }}>No trading partner data available.</p>
          <p className="tiny" style={{ opacity: 0.8 }}>
            This typically indicates:
          </p>
          <ul
            style={{
              fontSize: "11px",
              textAlign: "left",
              marginTop: 4,
              opacity: 0.8,
            }}
          >
            <li>Limited bilateral trade agreements</li>
            <li>Country operates under multilateral frameworks only</li>
            <li>Data collection in progress</li>
          </ul>
        </motion.div>
      );
    return (
      <div style={{ display: "grid", gap: 12 }}>
        {items.map((item, index) => (
          <motion.div
            key={item.code}
            className="metric-card"
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3, delay: index * 0.1 }}
          >
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
              }}
            >
              <div>
                <span
                  style={{
                    fontSize: "16px",
                    fontWeight: 600,
                    color: "var(--color-text)",
                  }}
                >
                  {item.name}
                </span>
                <span
                  className="badge subtle"
                  style={{ marginLeft: 8, fontSize: "12px" }}
                >
                  {item.code}
                </span>
              </div>
              <div style={{ textAlign: "right" }}>
                <div
                  className="label"
                  style={{ fontSize: "12px", opacity: 0.7 }}
                >
                  Avg Tariff Rate
                </div>
                <span style={{ fontSize: "14px", fontWeight: 600 }}>
                  {formatPercent(item.totalValue)}
                </span>
              </div>
            </div>
          </motion.div>
        ))}
      </div>
    );
  };

  return (
    <MotionWrapper>
      <div
        className="card glass glow-border neon-focus"
        aria-labelledby="insightsTitle"
      >
        <motion.h2
          id="insightsTitle"
          className="neon-text"
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, ease: [0.4, 0, 0.2, 1] }}
        >
          Trade Insights
        </motion.h2>
        <p
          className="small neon-subtle"
          style={{ marginTop: -12, marginBottom: 24 }}
        >
          View tariff rates and fees by product category, including base
          percentage rates and additional fixed charges.
        </p>

        <form
          onSubmit={(e) => {
            e.preventDefault();
            handleSearch();
          }}
          noValidate
        >
          <div className="inline-fields field-cluster">
            <div className="field" style={{ flex: "1 1 220px" }}>
              <label htmlFor="countrySelect">Country</label>
              <Select
                id="countrySelect"
                value={country}
                onChange={setCountry}
                options={COUNTRIES}
              />
            </div>
          </div>
          <div className="btn-group" style={{ marginTop: 8 }}>
            <button
              type="submit"
              className="primary"
              disabled={loading || !country}
            >
              {loading ? "Searching…" : "Search"}
            </button>
            <button type="button" onClick={handleClear} disabled={loading}>
              Clear
            </button>
          </div>
        </form>

        {error && (
          <motion.div
            className="error"
            role="alert"
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            style={{
              display: "flex",
              alignItems: "flex-start",
              justifyContent: "space-between",
              gap: 12,
            }}
          >
            <div style={{ flex: 1 }}>
              <strong>Unable to load insights:</strong> {error}
              {retryCount > 0 && (
                <div style={{ fontSize: "12px", marginTop: 4, opacity: 0.8 }}>
                  Attempt {retryCount} of 3
                </div>
              )}
              {lastLoadedCountry && lastLoadedCountry !== country && (
                <div style={{ fontSize: "12px", marginTop: 4, opacity: 0.8 }}>
                  Last successful load: {lastLoadedCountry}
                </div>
              )}
            </div>
            {(error.includes("Network") ||
              error.includes("Server error") ||
              error.includes("timed out")) &&
              retryCount < 3 && (
                <button
                  type="button"
                  className="btn-small"
                  onClick={handleRetry}
                  disabled={loading}
                  style={{
                    padding: "4px 8px",
                    fontSize: "12px",
                    minWidth: "auto",
                  }}
                >
                  Retry
                </button>
              )}
          </motion.div>
        )}

        {loading && (
          <motion.div
            style={{ marginTop: 24 }}
            aria-live="polite"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
          >
            <div
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                gap: 12,
                marginBottom: 24,
                padding: 16,
                background: "rgba(99, 102, 241, 0.1)",
                border: "1px solid rgba(99, 102, 241, 0.2)",
                borderRadius: 8,
              }}
            >
              <div className="spinner" aria-hidden="true" />
              <div style={{ textAlign: "center" }}>
                <div
                  className="small"
                  style={{ fontWeight: 600, marginBottom: 4 }}
                >
                  Loading Trade Insights for {country}...
                </div>
                <div style={{ fontSize: "12px", opacity: 0.7 }}>
                  Analyzing tariff schedules and trade relationships
                </div>
              </div>
            </div>

            {/* Skeleton loader */}
            <div
              style={{
                display: "grid",
                gap: 24,
                gridTemplateColumns: "repeat(auto-fit, minmax(300px, 1fr))",
                marginBottom: 24,
              }}
            >
              {[1, 2].map((i) => (
                <div key={i} style={{ opacity: 0.6 }}>
                  <div
                    style={{
                      height: 16,
                      background: "rgba(255,255,255,0.1)",
                      borderRadius: 4,
                      marginBottom: 12,
                    }}
                  />
                  {[1, 2, 3].map((j) => (
                    <div
                      key={j}
                      className="metric-card"
                      style={{ opacity: 0.5, marginBottom: 12 }}
                    >
                      <div
                        style={{
                          height: 12,
                          background: "rgba(255,255,255,0.1)",
                          borderRadius: 4,
                          marginBottom: 8,
                          width: "70%",
                        }}
                      />
                      <div
                        style={{
                          height: 10,
                          background: "rgba(255,255,255,0.1)",
                          borderRadius: 4,
                          width: "40%",
                        }}
                      />
                    </div>
                  ))}
                </div>
              ))}
            </div>
          </motion.div>
        )}

        {!loading && insights && (
          <motion.div
            style={{ marginTop: 32 }}
            aria-live="polite"
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, ease: [0.4, 0, 0.2, 1] }}
          >
            <h3
              className="neon-subtle"
              style={{ fontWeight: 600, marginBottom: 16 }}
            >
              Trade Analytics Overview
            </h3>

            <div
              style={{
                display: "grid",
                gap: 24,
                gridTemplateColumns: "repeat(auto-fit, minmax(300px, 1fr))",
                marginBottom: 24,
              }}
            >
              <motion.section
                aria-labelledby="importsHeading"
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: 0.1 }}
              >
                <h4
                  id="importsHeading"
                  className="label"
                  style={{
                    marginBottom: 12,
                    fontSize: "14px",
                    fontWeight: 600,
                  }}
                >
                  Top Import Categories by Tariff Rate
                </h4>
                {renderProductList(insights.topImports, "import")}
              </motion.section>

              <motion.section
                aria-labelledby="exportsHeading"
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, delay: 0.2 }}
              >
                <h4
                  id="exportsHeading"
                  className="label"
                  style={{
                    marginBottom: 12,
                    fontSize: "14px",
                    fontWeight: 600,
                  }}
                >
                  Top Export Categories by Tariff Rate
                </h4>
                {renderProductList(insights.topExports, "export")}
              </motion.section>
            </div>

            {/* Average Tariff Levels Cards */}
            <div style={{ marginBottom: 20 }}>
              <h4
                className="label"
                style={{ marginBottom: 12, fontSize: "14px", fontWeight: 600 }}
              >
                Average Tariff Levels
              </h4>
              <div
                style={{
                  display: "grid",
                  gap: 16,
                  gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
                }}
              >
                <motion.div
                  className="metric-card"
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.3 }}
                >
                  <span className="label">Inbound (Imports)</span>
                  <span
                    style={{
                      fontSize: "18px",
                      fontWeight: 600,
                      color: "var(--color-text)",
                    }}
                  >
                    {formatPercent(insights.averageImportTariff)}
                  </span>
                  <p className="tiny" style={{ marginTop: 4, opacity: 0.7 }}>
                    Mean base rate across tariff schedules applied to inbound
                    goods.
                  </p>
                </motion.div>
                <motion.div
                  className="metric-card"
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.35 }}
                >
                  <span className="label">Outbound (Exports)</span>
                  <span
                    style={{
                      fontSize: "18px",
                      fontWeight: 600,
                      color: "var(--color-text)",
                    }}
                  >
                    {formatPercent(insights.averageExportTariff)}
                  </span>
                  <p className="tiny" style={{ marginTop: 4, opacity: 0.7 }}>
                    Mean base rate negotiated on export corridors from the
                    selected country.
                  </p>
                </motion.div>
              </div>
            </div>

            {/* Major Trading Partners */}
            <motion.section
              aria-labelledby="partnersHeading"
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.4 }}
            >
              <h4
                id="partnersHeading"
                className="label"
                style={{ marginBottom: 12, fontSize: "14px", fontWeight: 600 }}
              >
                Major Trade Partners by Average Tariff Rate
              </h4>
              {renderPartnerList(insights.majorPartners)}
            </motion.section>
          </motion.div>
        )}

        {!loading && !insights && !error && (
          <div className="small" style={{ marginTop: 32 }}>
            No results yet. Run a search to view configured tariff rates.
          </div>
        )}
      </div>
    </MotionWrapper>
  );
}
