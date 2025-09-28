import React, { useState } from "react";
import api from "../services/api.js";
import MotionWrapper from "../components/MotionWrapper.jsx";
import { motion, AnimatePresence } from "framer-motion";
import Select from "../components/Select.jsx";

const COUNTRIES = ["SGP", "USA", "CHN", "MYS", "IDN"];
const CATEGORIES = ["STEEL", "ELEC", "FOOD"];

export default function CalculatePage() {
  const [origin, setOrigin] = useState("SGP");
  const [destination, setDestination] = useState("USA");
  const [category, setCategory] = useState("STEEL");
  const [declared, setDeclared] = useState(1000.0);
  const [date, setDate] = useState("");
  const [res, setRes] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [pdfLoading, setPdfLoading] = useState(false);
  const [retryCount, setRetryCount] = useState(0);

  const formatCurrency = (v) =>
    new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
    }).format(v);

  const downloadPdf = async () => {
    setError(null);
    setPdfLoading(true);
    try {
      const payload = {
        originCountryCode: origin,
        destinationCountryCode: destination,
        productCategoryCode: category,
        declaredValue: Number(declared),
        date: date || undefined,
      };

      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 30000); // 30 second timeout

      const response = await fetch(
        "http://localhost:8080/api/tariffs/calculate/pdf",
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
          signal: controller.signal,
        }
      );

      clearTimeout(timeoutId);

      if (!response.ok) {
        if (response.status >= 500) {
          throw new Error("Server error occurred. Please try again.");
        } else if (response.status === 404) {
          throw new Error("PDF generation service not found.");
        } else {
          throw new Error(`Failed to generate PDF: ${response.status}`);
        }
      }

      const blob = await response.blob();
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

      if (err.name === "AbortError") {
        errorMessage = "PDF generation timed out. Please try again.";
      } else if (
        err.message.includes("fetch") ||
        err.code === "NETWORK_ERROR"
      ) {
        errorMessage =
          "Network error. Please check your connection and try again.";
      } else if (
        err.message.includes("404") ||
        err.message.includes("not found")
      ) {
        isDataUnavailable = true;
        errorMessage =
          "PDF generation service is currently unavailable. The calculation feature works normally, but PDF export is temporarily disabled.";
      } else {
        errorMessage = err.message || "PDF generation failed";
      }

      setError({ message: errorMessage, isDataUnavailable });
    } finally {
      setPdfLoading(false);
    }
  };

  const submit = async (e) => {
    e.preventDefault();
    setError(null);
    setRes(null);
    setLoading(true);

    try {
      // Basic form validation
      if (!declared || declared <= 0) {
        throw new Error("Please enter a valid declared value greater than 0.");
      }

      const payload = {
        originCountryCode: origin,
        destinationCountryCode: destination,
        productCategoryCode: category,
        declaredValue: Number(declared),
        date: date || undefined,
      };

      const r = await api.post("/api/tariffs/calculate", payload);
      setRes(r.data);
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
        errorMessage = `No tariff data available for ${origin} → ${destination} (${category}). This trade route may not be covered in our current database, or no tariff schedule is active for the selected date.`;
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
          Compute estimated tariff obligations and total landed cost breakdown.
        </p>
        <form onSubmit={submit} noValidate className="calc-form">
          <div className="inline-fields field-cluster">
            <div className="field" style={{ flex: "1 1 220px" }}>
              <label htmlFor="origin">Origin Country</label>
              <Select
                id="origin"
                value={origin}
                onChange={setOrigin}
                options={COUNTRIES}
              />
            </div>
            <div className="field" style={{ flex: "1 1 220px" }}>
              <label htmlFor="destination">Destination Country</label>
              <Select
                id="destination"
                value={destination}
                onChange={setDestination}
                options={COUNTRIES}
              />
            </div>
            <div className="field" style={{ flex: "1 1 220px" }}>
              <label htmlFor="category">Product Category</label>
              <Select
                id="category"
                value={category}
                onChange={setCategory}
                options={CATEGORIES}
              />
            </div>
          </div>
          <div className="inline-fields field-cluster">
            <div className="field" style={{ flex: "1 1 260px" }}>
              <label htmlFor="declared">Declared Value (USD)</label>
              <input
                id="declared"
                className="input"
                type="number"
                step="0.01"
                value={declared}
                onChange={(e) => setDeclared(e.target.value)}
                required
              />
            </div>
            <div className="field" style={{ flex: "1 1 260px" }}>
              <label htmlFor="date">Effective Date (optional)</label>
              <input
                id="date"
                className="input"
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
              />
            </div>
          </div>
          <div className="btn-group" style={{ marginTop: 8 }}>
            <button className="primary" type="submit" disabled={loading}>
              {loading ? "Calculating…" : "Calculate"}
            </button>
            <button
              type="button"
              onClick={() => {
                setRes(null);
                setError(null);
                setRetryCount(0);
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
                      Try checking with different product categories or verify
                      the tariff data is available for your selection.
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
                    gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
                    marginBottom: 24,
                  }}
                >
                  <motion.div
                    className="metric-card"
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.1 }}
                  >
                    <span className="label">Origin</span>
                    <span
                      style={{
                        fontSize: "16px",
                        fontWeight: 600,
                        color: "var(--color-text)",
                      }}
                    >
                      {res.originCountryCode}
                    </span>
                  </motion.div>
                  <motion.div
                    className="metric-card"
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.15 }}
                  >
                    <span className="label">Destination</span>
                    <span
                      style={{
                        fontSize: "16px",
                        fontWeight: 600,
                        color: "var(--color-text)",
                      }}
                    >
                      {res.destinationCountryCode}
                    </span>
                  </motion.div>
                  <motion.div
                    className="metric-card"
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.2 }}
                  >
                    <span className="label">Category</span>
                    <span
                      style={{
                        fontSize: "16px",
                        fontWeight: 600,
                        color: "var(--color-text)",
                      }}
                    >
                      {res.productCategoryCode}
                    </span>
                  </motion.div>
                  <motion.div
                    className="metric-card"
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.25 }}
                  >
                    <span className="label">Effective Date</span>
                    <span
                      style={{
                        fontSize: "16px",
                        fontWeight: 600,
                        color: "var(--color-text)",
                      }}
                    >
                      {res.effectiveDate}
                    </span>
                  </motion.div>
                </div>

                {/* Financial Breakdown Cards */}
                <div
                  style={{
                    display: "grid",
                    gap: 16,
                    gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
                    marginBottom: 20,
                  }}
                >
                  <motion.div
                    className="metric-card"
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.3 }}
                  >
                    <span className="label">Declared Value</span>
                    <span
                      style={{
                        fontSize: "18px",
                        fontWeight: 600,
                        color: "var(--color-text)",
                      }}
                    >
                      {formatCurrency(res.declaredValue)}
                    </span>
                  </motion.div>
                  <motion.div
                    className="metric-card"
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.35 }}
                  >
                    <span className="label">Base Rate</span>
                    <span
                      style={{
                        fontSize: "18px",
                        fontWeight: 600,
                        color: "var(--color-text)",
                      }}
                    >
                      {(res.baseRate * 100).toFixed(2)}%
                    </span>
                  </motion.div>
                  <motion.div
                    className="metric-card"
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.4 }}
                  >
                    <span className="label">Tariff Amount</span>
                    <span
                      style={{
                        fontSize: "18px",
                        fontWeight: 600,
                        color: "var(--color-text)",
                      }}
                    >
                      {formatCurrency(res.tariffAmount)}
                    </span>
                  </motion.div>
                  <motion.div
                    className="metric-card"
                    initial={{ opacity: 0, y: 8 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.45 }}
                  >
                    <span className="label">Additional Fee</span>
                    <span
                      style={{
                        fontSize: "18px",
                        fontWeight: 600,
                        color: "var(--color-text)",
                      }}
                    >
                      {formatCurrency(res.additionalFee)}
                    </span>
                  </motion.div>
                  <motion.div
  className="small neon-subtle w-full max-w-5xl mx-auto p-8"
  style={{ marginTop: -12, marginBottom: 24 }}
  initial={{ opacity: 0, y: 10 }}
  animate={{ opacity: 1, y: 0 }}
  transition={{ duration: 0.4 }}
>
  <div className="text-lg font-semibold mb-4">AI Summary</div>
  
  <div
    className="prose max-w-none text-justify leading-relaxed"
    dangerouslySetInnerHTML={{ __html: res.aiSummary }}
  />
</motion.div>


                </div>

                {/* Total Cost - Prominent Display */}
                <motion.div
                  className="result-panel glow-border"
                  style={{ textAlign: "center", padding: "20px" }}
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
                      background: "linear-gradient(135deg, #6366f1, #8b5cf6)",
                      backgroundClip: "text",
                      WebkitBackgroundClip: "text",
                      color: "transparent",
                    }}
                  >
                    {formatCurrency(res.totalCost)}
                  </div>
                  <div className="small" style={{ marginTop: 8, opacity: 0.7 }}>
                    Total = declaredValue + (declaredValue × baseRate) +
                    additionalFee
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
    </MotionWrapper>
  );
}
