import React, { useEffect, useMemo, useRef, useState } from "react";
import api from "../services/api.js";
import MotionWrapper from "../components/MotionWrapper.jsx";
import TariffNewsSidebar from "../components/TariffNewsSidebar.jsx";
import { motion, AnimatePresence } from "framer-motion";
import Select from "../components/Select.jsx";
import { useReferenceOptions } from "../hooks/useReferenceOptions.js";
import { formatStoredPercent } from "../utils/percent.js";
import {
  DEFAULT_DESTINATION_CODE,
  DEFAULT_ORIGIN_CODE,
  DEFAULT_PRODUCT_CATEGORY,
} from "../constants/referenceOptions.js";

export default function CalculatePage() {
  const { countries, categories } = useReferenceOptions();
  const countryOptions = useMemo(
    () => (countries && countries.length ? countries : []),
    [countries]
  );
  const [origin, setOrigin] = useState(DEFAULT_ORIGIN_CODE || "");
  const [destination, setDestination] = useState(DEFAULT_DESTINATION_CODE || "");
  const [category, setCategory] = useState(DEFAULT_PRODUCT_CATEGORY || "");
  const [hsCode, setHsCode] = useState("");
  const [declared, setDeclared] = useState(1000.0);
  const [weight, setWeight] = useState("");
  const [effectiveFrom, setEffectiveFrom] = useState("");
  const [effectiveTo, setEffectiveTo] = useState("");
  const [res, setRes] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [pdfLoading, setPdfLoading] = useState(false);
  const [aiSummaryLoading, setAiSummaryLoading] = useState(false);
  const [retryCount, setRetryCount] = useState(0);
  const categoryOptions = useMemo(
    () => (categories && categories.length ? categories : []),
    [categories]
  );
  const selectedCategory = useMemo(
    () => categoryOptions.find((opt) => opt.value === category),
    [categoryOptions, category]
  );
  const categoryIsWeightBased = selectedCategory?.weightBased ?? false;
  const hsCodeOptions = useMemo(() => {
    const seen = new Set();
    return categoryOptions.reduce((acc, option) => {
      const hs = (option.hsCode || option.value || "").toUpperCase();
      if (!hs || seen.has(hs)) {
        return acc;
      }
      seen.add(hs);
      acc.push({
        value: hs,
        label: hs,
        categoryCode: option.value,
      });
      return acc;
    }, []);
  }, [categoryOptions]);
  const summaryRequestIdRef = useRef(0);
  const summaryCardData = useMemo(() => {
    if (!res) return [];
    const productLabel = res.productCategoryName
      ? `${res.productCategoryName} (${res.productCategoryCode})`
      : res.productCategoryCode || "-";
    return [
      { label: "Origin", value: res.originCountryCode || "-" },
      { label: "Destination", value: res.destinationCountryCode || "-" },
      { label: "HS Code", value: res.hsCode || "-" },
      { label: "Product Category", value: productLabel },
      {
        label: "Schedule From",
        value: res.rateEffectiveFrom || "-",
      },
      {
        label: "Schedule To",
        value: res.rateEffectiveTo || "Open",
      },
    ];
  }, [res]);

  useEffect(() => {
    if (!countryOptions.length) {
      return;
    }
    setOrigin((prev) => {
      if (prev && countryOptions.some((opt) => opt.value === prev)) {
        return prev;
      }
      const fallback =
        countryOptions.find((opt) => opt.value === DEFAULT_ORIGIN_CODE)?.value ??
        countryOptions[0].value;
      return fallback;
    });
    setDestination((prev) => {
      if (prev && countryOptions.some((opt) => opt.value === prev)) {
        return prev;
      }
      const fallback =
        countryOptions.find((opt) => opt.value === DEFAULT_DESTINATION_CODE)?.value ??
        countryOptions[Math.min(1, countryOptions.length - 1)].value;
      return fallback;
    });
  }, [countries]);

  useEffect(() => {
    if (!categoryOptions.length) {
      return;
    }
    setCategory((prev) => {
      if (prev && categoryOptions.some((opt) => opt.value === prev)) {
        return prev;
      }
      const fallback =
        categoryOptions.find((opt) => opt.value === DEFAULT_PRODUCT_CATEGORY)?.value ??
        categoryOptions[0].value;
      return fallback;
    });
  }, [categoryOptions]);

  useEffect(() => {
    if (!categoryIsWeightBased) {
      setWeight("");
    }
  }, [categoryIsWeightBased]);

  useEffect(() => {
    if (!categoryOptions.length) {
      return;
    }
    const match = categoryOptions.find((opt) => opt.value === category);
    if (match?.hsCode && match.hsCode !== hsCode) {
      setHsCode(match.hsCode);
    } else if (!match && categoryOptions[0]?.hsCode && hsCode !== categoryOptions[0].hsCode) {
      setHsCode(categoryOptions[0].hsCode);
    }
  }, [categoryOptions, category, hsCode]);

  const handleHsCodeChange = (value) => {
    setHsCode(value);
    if (!value) {
      return;
    }
    const match = categoryOptions.find(
      (opt) => (opt.hsCode || "").toUpperCase() === value.toUpperCase()
    );
    if (match) {
      setCategory(match.value);
    }
  };

  const handleCategoryChange = (value) => {
    setCategory(value);
    if (!value) {
      return;
    }
    const match = categoryOptions.find((opt) => opt.value === value);
    if (match?.hsCode) {
      setHsCode(match.hsCode);
    }
  };

  const formatCurrency = (v) => {
    const numericValue = Number(v);
    if (!Number.isFinite(numericValue)) {
      return new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "USD",
      }).format(0);
    }
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
    }).format(numericValue);
  };

  const downloadPdf = async () => {
    setError(null);
    setPdfLoading(true);
    const controller = new AbortController();
    let timeoutId;
    try {
      const payload = {
        originCountryCode: origin,
        destinationCountryCode: destination,
        hsCode,
        productCategoryCode: category || undefined,
        declaredValue: Number(declared),
        weight:
          categoryIsWeightBased && weight
            ? Number(weight)
            : undefined,
        effectiveFrom: effectiveFrom || undefined,
        effectiveTo: effectiveTo || undefined,
      };

      timeoutId = setTimeout(() => controller.abort(), 30000); // 30 second timeout

      const response = await api.post("/tariffs/calculations/pdf", payload, {
        responseType: "blob",
        signal: controller.signal,
        headers: { Accept: "application/pdf" },
      });

      const blob = response.data;
      const url = window.URL.createObjectURL(blob);

      // trigger download
      const a = document.createElement("a");
      a.href = url;
      a.download = "tariff-report.pdf";
      a.click();

      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error("PDF error:", err);
      let errorMessage;
      let isDataUnavailable = false;

      if (
        err.code === "ERR_CANCELED" ||
        err.name === "CanceledError" ||
        err.name === "AbortError"
      ) {
        errorMessage = "PDF generation timed out. Please try again.";
      } else if (err.response) {
        const status = err.response.status;
        if (status >= 500) {
          errorMessage = "Server error occurred. Please try again.";
        } else if (status === 404) {
          isDataUnavailable = true;
          errorMessage =
            "PDF generation service is currently unavailable. The calculation feature works normally, but PDF export is temporarily disabled.";
        } else {
          errorMessage = `Failed to generate PDF: ${status}`;
        }
      } else if (err.request) {
        errorMessage =
          "Network error. Please check your connection and try again.";
      } else {
        errorMessage = err.message || "PDF generation failed";
      }

      setError({ message: errorMessage, isDataUnavailable });
    } finally {
      if (timeoutId) {
        clearTimeout(timeoutId);
      }
      setPdfLoading(false);
    }
  };

  const fetchAiSummary = async (resultPayload, requestId) => {
    if (!resultPayload) {
      setAiSummaryLoading(false);
      return;
    }

    try {
      const { data } = await api.post("/tariffs/summaries", resultPayload);
      if (summaryRequestIdRef.current !== requestId) {
        return;
      }
      setRes((prev) =>
        prev
          ? { ...prev, aiSummary: data?.aiSummary ?? "AI summary unavailable." }
          : prev
      );
    } catch (err) {
      console.error("AI summary error:", err);
      if (summaryRequestIdRef.current !== requestId) {
        return;
      }
      setRes((prev) =>
        prev ? { ...prev, aiSummary: "AI summary unavailable." } : prev
      );
    } finally {
      if (summaryRequestIdRef.current === requestId) {
        setAiSummaryLoading(false);
      }
    }
  };

  const submit = async (e) => {
    e.preventDefault();
    setError(null);
    setRes(null);
    setAiSummaryLoading(true);
    setLoading(true);

    try {
      // Basic form validation
      if (!declared || declared <= 0) {
        throw new Error("Please enter a valid declared value greater than 0.");
      }
      if (!hsCode) {
        throw new Error("Please select a HS code to continue.");
      }
      if (categoryIsWeightBased) {
        if (!weight || Number(weight) <= 0) {
          throw new Error(
            "Weight is required for this HS code. Please enter a positive weight."
          );
        }
      }
      if (weight && Number(weight) > 10000) {
        throw new Error("Weight cannot exceed 10,000 kg.");
      }
      if (effectiveFrom && effectiveTo) {
        const fromDate = new Date(effectiveFrom);
        const toDate = new Date(effectiveTo);
        if (fromDate > toDate) {
          throw new Error(
            "Effective from date cannot be later than effective to date."
          );
        }
      }

      const payload = {
        originCountryCode: origin,
        destinationCountryCode: destination,
        hsCode,
        productCategoryCode: category || undefined,
        declaredValue: Number(declared),
        weight:
          weight && Number(weight) > 0
            ? Number(weight)
            : undefined,
        effectiveFrom: effectiveFrom || undefined,
        effectiveTo: effectiveTo || undefined,
      };

      const requestId = summaryRequestIdRef.current + 1;
      summaryRequestIdRef.current = requestId;

      const { data } = await api.post(
        "/tariffs/calculations?includeSummary=false",
        payload
      );
      setRes(data);
      fetchAiSummary(data, requestId);
      setRetryCount(0); // Reset retry count on success
    } catch (err) {
      console.error("Calculation error:", err);

      let errorMessage;
      let isDataUnavailable = false;

      // Check if this is a "data not found" scenario rather than an error
      if (
        err.response?.status === 404 ||
        err.response?.data?.message?.includes("not found") ||
        err.response?.data?.message?.includes("No tariff rate found") ||
        err.message?.includes("not found")
      ) {
        isDataUnavailable = true;
        errorMessage = `No tariff data available for ${origin} -> ${destination} (HS ${hsCode}${category ? ` / ${category}` : ""}). This trade route may not be covered in our current database, or no tariff schedule is active for the selected period.`;
      } else if (
        err.code === "NETWORK_ERROR" ||
        err.message.includes("Network Error")
      ) {
        errorMessage =
          "Network connection failed. Please check your internet connection and try again.";
      } else if (err.response?.status >= 500) {
        errorMessage =
          "Server error occurred. Our team has been notified. Please try again in a few moments.";
      } else if (err.response?.status === 400) {
        errorMessage =
          err.response?.data?.message ||
          "Invalid input data. Please check your entries and try again.";
      } else if (err.response?.status === 429) {
        errorMessage = "Too many requests. Please wait a moment and try again.";
      } else {
        errorMessage =
          err.formattedMessage ||
          err?.response?.data?.message ||
          err?.message ||
          "Calculation failed. Please try again.";
      }

      setAiSummaryLoading(false);
      setError({ message: errorMessage, isDataUnavailable });
    } finally {
      setLoading(false);
    }
  };

  const handleRetry = () => {
    if (retryCount < 3) {
      setRetryCount(retryCount + 1);
      const fakeEvent = { preventDefault: () => {} };
      submit(fakeEvent);
    }
  };

  return (
    <MotionWrapper>
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 380px",
          gap: "24px",
          alignItems: "start",
        }}
        className="calculate-layout"
      >
        {/* Main Content */}
        <div>
          <div
            className="card glass glow-border neon-focus"
            aria-labelledby="calcTitle"
            style={{ position: "relative", overflow: "visible" }}
          >
            <motion.h2
              id="calcTitle"
              className="neon-text"
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, ease: [0.4, 0, 0.2, 1] }}
            >
              Tariff Calculator
            </motion.h2>
            <p
              className="small neon-subtle"
              style={{ marginTop: -12, marginBottom: 24 }}
            >
              Compute estimated tariff obligations and total landed cost
              breakdown.
            </p>
            <form onSubmit={submit} noValidate className="calc-form">
              <div className="inline-fields field-cluster">
                <div className="field" style={{ flex: "1 1 220px" }}>
                  <label htmlFor="origin">
                    Origin Country{" "}
                    <span style={{ color: "#f87171" }} aria-hidden="true">
                      *
                    </span>
                  </label>
                  <Select
                    id="origin"
                    value={origin}
                    onChange={setOrigin}
                    options={countryOptions}
                  />
                </div>
                <div className="field" style={{ flex: "1 1 220px" }}>
                  <label htmlFor="destination">
                    Destination Country{" "}
                    <span style={{ color: "#f87171" }} aria-hidden="true">
                      *
                    </span>
                  </label>
                  <Select
                    id="destination"
                    value={destination}
                    onChange={setDestination}
                    options={countryOptions}
                  />
                </div>
              </div>

              <div className="inline-fields field-cluster">
                <div className="field" style={{ flex: "1 1 220px" }}>
                  <label htmlFor="hsCode">
                    HS Code <span style={{ color: "#f87171" }}>*</span>
                  </label>
                  <Select
                    id="hsCode"
                    value={hsCode}
                    onChange={handleHsCodeChange}
                    options={hsCodeOptions}
                    placeholder="Select HS code"
                  />
                </div>
                <div className="field" style={{ flex: "1 1 220px" }}>
                  <label htmlFor="category">
                    Product Category <span className="tiny">(auto)</span>
                  </label>
                  <Select
                    id="category"
                    value={category}
                    onChange={handleCategoryChange}
                    options={categoryOptions}
                  />
                  <div className="tiny" style={{ marginTop: 4, opacity: 0.7 }}>
                    Auto-populated from HS code but can be overridden if needed.
                  </div>
                </div>
              </div>

              <div className="inline-fields field-cluster">
                <div className="field" style={{ flex: "1 1 220px" }}>
                  <label htmlFor="declared">
                    Declared Value (USD){" "}
                    <span style={{ color: "#f87171" }} aria-hidden="true">
                      *
                    </span>
                  </label>
                  <input
                    id="declared"
                    className="input"
                    type="number"
                    min="0"
                    step="0.01"
                    value={declared}
                    onChange={(e) => setDeclared(e.target.value)}
                    required
                  />
                </div>
                <div className="field" style={{ flex: "1 1 220px" }}>
                  <label htmlFor="weight" style={{ display: "flex", gap: 6, alignItems: "center" }}>
                    <span>
                      Weight (kg){" "}
                      {categoryIsWeightBased ? (
                        <span style={{ color: "#f87171" }}>*</span>
                      ) : (
                        <span className="tiny">(auto-disabled)</span>
                      )}
                    </span>
                    <span
                      title="If the selected HS code is weight-based, the declared value is scaled by your entered weight relative to the schedule’s weight unit before tariffs apply."
                      style={{
                        cursor: "help",
                        fontSize: 12,
                        color: "var(--color-accent)",
                        opacity: 0.9,
                      }}
                      aria-label="Weight-based tariffs scale the declared value using the provided weight."
                    >
                      ⓘ
                    </span>
                  </label>
                  <input
                    id="weight"
                    className="input"
                    type="number"
                    min="0"
                    step="0.01"
                    value={weight}
                    onChange={(e) => setWeight(e.target.value)}
                    disabled={!categoryIsWeightBased}
                    placeholder={
                      categoryIsWeightBased
                        ? "Enter weight in kg"
                        : "Not required for this category"
                    }
                  />
                </div>
              </div>

              <div className="inline-fields field-cluster">
                <div className="field" style={{ flex: "1 1 220px" }}>
                  <label htmlFor="effectiveFrom">Effective From (optional)</label>
                  <input
                    id="effectiveFrom"
                    className="input"
                    type="date"
                    value={effectiveFrom}
                    onChange={(e) => setEffectiveFrom(e.target.value)}
                  />
                </div>
                <div className="field" style={{ flex: "1 1 220px" }}>
                  <label htmlFor="effectiveTo">Effective To (optional)</label>
                  <input
                    id="effectiveTo"
                    className="input"
                    type="date"
                    value={effectiveTo}
                    onChange={(e) => setEffectiveTo(e.target.value)}
                  />
                </div>
              </div>

              <div className="btn-group" style={{ marginTop: 8 }}>
                <button className="primary" type="submit" disabled={loading}>
                  {loading ? "Calculating..." : "Calculate"}
                </button>
                <button
                  onClick={() => {
                    setRes(null);
                    setError(null);
                    setRetryCount(0);
                    setEffectiveFrom("");
                    setEffectiveTo("");
                    setWeight("");
                  }}
                  disabled={loading}
                >
                  Reset
                </button>
              </div>
            </form>

            <AnimatePresence mode="wait">
              {error ? (
                <motion.div
                  style={{ marginTop: 32 }}
                  aria-live={error.isDataUnavailable ? "polite" : "assertive"}
                  key="error-display"
                  initial={{ opacity: 0, y: 12 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -8 }}
                  transition={{ duration: 0.4, ease: [0.4, 0.0, 0.2, 1] }}
                >
                  {error.isDataUnavailable ? (
                    <>
                      <h3
                        className="neon-subtle"
                        style={{ fontWeight: 600, marginBottom: 24 }}
                      >
                        No Tariff Data Found
                      </h3>

                      {/* Status Message */}
                      <motion.div
                        className="result-panel"
                        style={{
                          textAlign: "center",
                          padding: "20px",
                          background: "rgba(59, 130, 246, 0.1)",
                          border: "1px solid rgba(59, 130, 246, 0.2)",
                        }}
                        initial={{ opacity: 0, scale: 0.95 }}
                        animate={{ opacity: 1, scale: 1 }}
                        transition={{ delay: 0.3 }}
                      >
                        <div
                          style={{
                            fontSize: "14px",
                            color: "var(--color-text-muted)",
                            marginBottom: 12,
                            lineHeight: 1.5,
                          }}
                        >
                          {error.message}
                        </div>
                        <div
                          style={{
                            fontSize: "12px",
                            color: "var(--color-text-muted)",
                            opacity: 0.8,
                            lineHeight: 1.4,
                          }}
                        >
                          Try checking with different product categories or
                          verify the tariff data is available for your
                          selection.
                        </div>
                      </motion.div>
                    </>
                  ) : (
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
                        <div
                          className="label"
                          style={{
                            marginBottom: 8,
                            fontWeight: 600,
                            color: "var(--color-danger)",
                          }}
                        >
                          Error
                        </div>
                        <div
                          className="small"
                          style={{ marginBottom: 8, lineHeight: 1.5 }}
                        >
                          {error.message || error}
                        </div>
                        {retryCount > 0 && (
                          <div className="tiny" style={{ opacity: 0.7 }}>
                            Attempt {retryCount} of 3
                          </div>
                        )}
                      </div>
                      {(error.message?.includes("Network") ||
                        error.message?.includes("Server error")) &&
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
                </motion.div>
              ) : (
                res &&
                !loading && (
                  <motion.div
                    style={{ marginTop: 32 }}
                    aria-live="polite"
                    key={res.id || JSON.stringify(res)}
                    initial={{ opacity: 0, y: 12 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -8 }}
                    transition={{ duration: 0.4, ease: [0.4, 0.0, 0.2, 1] }}
                  >
                    <h3
                      className="neon-subtle"
                      style={{ fontWeight: 600, marginBottom: 16 }}
                    >
                      Result Breakdown
                    </h3>

                    {/* Trade Route Info Cards */}
                    <div
                      style={{
                        display: "grid",
                        gap: 16,
                        gridTemplateColumns:
                          "repeat(auto-fit, minmax(200px, 1fr))",
                        marginBottom: 16,
                      }}
                    >
                      {summaryCardData.map((card, index) => (
                        <motion.div
                          key={card.label}
                          className="metric-card"
                          initial={{ opacity: 0, y: 8 }}
                          animate={{ opacity: 1, y: 0 }}
                          transition={{ delay: 0.1 + index * 0.05 }}
                        >
                          <span className="label">{card.label}</span>
                          <span
                            style={{
                              fontSize: "16px",
                              fontWeight: 600,
                              color: "var(--color-text)",
                            }}
                          >
                            {card.value || "-"}
                          </span>
                        </motion.div>
                      ))}
                    </div>
                    {(res?.requestedEffectiveFrom ||
                      res?.requestedEffectiveTo) && (
                      <div
                        className="tiny neon-subtle"
                        style={{ marginBottom: 16 }}
                      >
                        Requested window: {res.requestedEffectiveFrom || "-"}
                        <span aria-hidden="true"> → </span>
                        {res.requestedEffectiveTo || "-"}
                      </div>
                    )}

                    {/* Financial Breakdown Cards */}
                    {(() => {
                      const cards = [
                        {
                          label: res.weightBased
                            ? "Weighted Declared Value"
                            : "Declared Value",
                          value: formatCurrency(res.declaredValue),
                        },
                        {
                          label: "Base Rate",
                          value: formatStoredPercent(res.baseRate),
                        },
                        {
                          label: "Tariff Amount",
                          value: formatCurrency(res.tariffAmount),
                        },
                        {
                          label: "Additional Fee",
                          value: formatCurrency(res.additionalFee),
                        },
                      ];
                      if (res.weightBased && res.declaredValuePerUnit != null) {
                        cards.unshift({
                          label: "Declared Value (per unit)",
                          value: formatCurrency(res.declaredValuePerUnit),
                        });
                      }
                      return (
                        <div
                          style={{
                            display: "grid",
                            gap: 16,
                            gridTemplateColumns:
                              "repeat(auto-fit, minmax(220px, 1fr))",
                            marginBottom: 20,
                          }}
                        >
                          {cards.map((card, index) => (
                            <motion.div
                              key={card.label}
                              className="metric-card"
                              initial={{ opacity: 0, y: 8 }}
                              animate={{ opacity: 1, y: 0 }}
                              transition={{ delay: 0.3 + index * 0.05 }}
                            >
                              <span className="label">{card.label}</span>
                              <span
                                style={{
                                  fontSize: "18px",
                                  fontWeight: 600,
                                  color: "var(--color-text)",
                                }}
                              >
                                {card.value}
                              </span>
                            </motion.div>
                          ))}
                        </div>
                      );
                    })()}

                    {res.weightBased && (
                      <div
                        className="tiny neon-subtle"
                        style={{ marginBottom: 16 }}
                      >
                        Weight applied:{" "}
                        {res.weight != null ? `${res.weight} kg` : "-"} ·
                        Calculation uses declared value per unit multiplied by
                        weight before tariff and fees are applied.
                      </div>
                    )}

                    {/* AI Summary Section */}
                    <motion.div
                      className="result-panel glow-border ai-summary-panel"
                      style={{
                        background: "var(--color-surface)",
                        color: "var(--color-text)",
                        border: "1px solid var(--color-border-strong)",
                        padding: "20px",
                      }}
                      initial={{ opacity: 0, scale: 0.95 }}
                      animate={{ opacity: 1, scale: 1 }}
                      transition={{ delay: 0.5 }}
                    >
                      <div
                        className="label"
                        style={{
                          marginBottom: 8,
                          fontSize: "12px",
                          textAlign: "center",
                        }}
                      >
                        AI SUMMARY
                      </div>

                      <div
                        className="ai-summary-body"
                        role="status"
                        aria-live="polite"
                        aria-busy={aiSummaryLoading}
                        style={{
                          minHeight: 120,
                          width: "100%",
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                        }}
                      >
                        {aiSummaryLoading ? (
                          <div className="ai-summary-loading">
                            <div className="spinner-small" aria-hidden="true" />
                            <span>Generating AI summary...</span>
                          </div>
                        ) : res && res.aiSummary ? (
                          <div
                            className="ai-summary-content"
                            style={{
                              fontSize: "14px",
                              lineHeight: 1.6,
                              textAlign: "justify",
                              color: "var(--color-text)",
                            }}
                            dangerouslySetInnerHTML={{ __html: res.aiSummary }}
                          />
                        ) : (
                          <div className="ai-summary-empty">
                            AI summary unavailable.
                          </div>
                        )}
                      </div>

                      <div className="small ai-summary-footnote">
                        Generated automatically based on tariff data, summary
                        might not be fully accurate.
                      </div>
                    </motion.div>

                    {/* Total Cost - Prominent Display */}
                    <motion.div
                      className="result-panel glow-border"
                      style={{
                        textAlign: "center",
                        padding: "20px",
                        marginTop: "24px",
                      }}
                      initial={{ opacity: 0, scale: 0.95 }}
                      animate={{ opacity: 1, scale: 1 }}
                      transition={{ delay: 0.5 }}
                    >
                      <div
                        className="label"
                        style={{ marginBottom: 8, fontSize: "14px" }}
                      >
                        TOTAL COST
                      </div>
                      <div
                        style={{
                          fontSize: "32px",
                          fontWeight: 700,
                          background:
                            "linear-gradient(135deg, #6366f1, #8b5cf6)",
                          backgroundClip: "text",
                          WebkitBackgroundClip: "text",
                          color: "transparent",
                        }}
                      >
                        {formatCurrency(res.totalCost)}
                      </div>
                      <div
                        className="small"
                        style={{ marginTop: 8, opacity: 0.7 }}
                      >
                        {res.notes ||
                          "Total = declaredValue + (declaredValue * baseRate) + additionalFee"}
                      </div>
                    </motion.div>

                    <div style={{ marginTop: 16, textAlign: "center" }}>
                      <button
                        className="primary"
                        type="button"
                        onClick={downloadPdf}
                        disabled={pdfLoading}
                        style={{
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          gap: 8,
                          margin: "0 auto",
                        }}
                      >
                        {pdfLoading && (
                          <div
                            className="spinner-small"
                            style={{ width: 16, height: 16 }}
                          />
                        )}
                        {pdfLoading ? "Generating PDF..." : "Download PDF"}
                      </button>
                    </div>
                  </motion.div>
                )
              )}
            </AnimatePresence>
          </div>
        </div>

        {/* News Sidebar */}
        <TariffNewsSidebar limit={8} />
      </div>
    </MotionWrapper>
  );
}
