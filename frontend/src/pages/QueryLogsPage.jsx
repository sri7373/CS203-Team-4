import React, { useEffect, useMemo, useState } from "react";
import api from "../services/api.js";
import MotionWrapper from "../components/MotionWrapper.jsx";
import { formatStoredPercent } from "../utils/percent.js";

const currencyFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
});

function safeJsonParse(value) {
  if (!value || typeof value !== "string") return null;
  try {
    return JSON.parse(value);
  } catch (err) {
    return null;
  }
}

function CalculatedResultCard({ result }) {
  if (!result) return null;

  const productLabel = result.productCategoryName
    ? `${result.productCategoryName} (${result.productCategoryCode})`
    : result.productCategoryCode || "-";
  const scheduleWindow = `${result.rateEffectiveFrom || "-"} -> ${
    result.rateEffectiveTo || "Open"
  }`;
  const requestedWindow = `${result.requestedEffectiveFrom || "-"} -> ${
    result.requestedEffectiveTo || "-"
  }`;

  const metrics = [
    ...(result.weightBased && result.declaredValuePerUnit != null
      ? [
          {
            label: "Declared Value (per unit)",
            value: currencyFormatter.format(
              Number(result.declaredValuePerUnit || 0)
            ),
          },
        ]
      : []),
    {
      label: result.weightBased ? "Weighted Declared Value" : "Declared Value",
      value: currencyFormatter.format(Number(result.declaredValue || 0)),
    },
    {
      label: "Base Rate",
      value:
        result.baseRate != null
          ? formatStoredPercent(result.baseRate)
          : "-",
    },
    {
      label: "Tariff Amount",
      value: currencyFormatter.format(Number(result.tariffAmount || 0)),
    },
    {
      label: "Additional Fee",
      value: currencyFormatter.format(Number(result.additionalFee || 0)),
    },
    ...(result.weightBased
      ? [
          {
            label: "Weight Applied",
            value:
              result.weight != null ? `${Number(result.weight)} kg` : "0 kg",
          },
        ]
      : []),
    {
      label: "Total Cost",
      value: currencyFormatter.format(Number(result.totalCost || 0)),
    },
  ];

  const highlightStyle = {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
    gap: 12,
  };

  return (
    <div
      className="card glass"
      style={{ padding: 20, background: "rgba(255,255,255,0.02)" }}
    >
      <div
        style={{
          display: "flex",
          flexWrap: "wrap",
          gap: 16,
          padding: "14px 18px",
          borderRadius: 12,
          background:
            "linear-gradient(135deg, rgba(99,102,241,0.18), rgba(168,85,247,0.12))",
          border: "1px solid rgba(129,140,248,0.25)",
          marginBottom: 12,
        }}
      >
        <div style={{ minWidth: 180 }}>
          <div
            className="small neon-subtle"
            style={{ letterSpacing: 0.8, marginBottom: 4 }}
          >
            ROUTE
          </div>
          <div style={{ fontWeight: 600 }}>{`${
            result.originCountryCode || "-"
          } -> ${result.destinationCountryCode || "-"}`}</div>
        </div>
        <div style={{ minWidth: 160 }}>
          <div
            className="small neon-subtle"
            style={{ letterSpacing: 0.8, marginBottom: 4 }}
          >
            PRODUCT
          </div>
          <div style={{ fontWeight: 600 }}>{productLabel}</div>
        </div>
        <div style={{ minWidth: 140 }}>
          <div
            className="small neon-subtle"
            style={{ letterSpacing: 0.8, marginBottom: 4 }}
          >
            HS CODE
          </div>
          <div style={{ fontWeight: 600 }}>{result.hsCode || "-"}</div>
        </div>
        <div style={{ minWidth: 200 }}>
          <div
            className="small neon-subtle"
            style={{ letterSpacing: 0.8, marginBottom: 4 }}
          >
            SCHEDULE
          </div>
          <div style={{ fontWeight: 600 }}>{scheduleWindow}</div>
        </div>
      </div>
      <div className="tiny neon-subtle" style={{ marginBottom: 12 }}>
        Requested window: {requestedWindow}
      </div>

      <div style={highlightStyle}>
        {metrics.map((m) => (
          <div
            key={m.label}
            className="metric-card"
            style={{
              padding: 12,
              background: "rgba(255,255,255,0.04)",
              borderRadius: 10,
            }}
          >
            <div
              className="small neon-subtle"
              style={{ textTransform: "uppercase", fontSize: 11 }}
            >
              {m.label}
            </div>
            <div style={{ fontWeight: 600, marginTop: 4 }}>{m.value}</div>
          </div>
        ))}
      </div>

      {result.notes && (
        <div className="small neon-subtle" style={{ marginTop: 16 }}>
          {result.notes}
        </div>
      )}
    </div>
  );
}
function SearchResultCard({ result }) {
  if (!result) return null;
  return (
    <div
      className="card glass"
      style={{ padding: 20, background: "rgba(255,255,255,0.02)" }}
    >
      <div className="small neon-subtle" style={{ marginBottom: 8 }}>
        Search Summary
      </div>
      <div className="small" style={{ marginBottom: 6 }}>
        Matches: <strong>{result.count ?? "-"}</strong>
      </div>
      {Array.isArray(result.sampleIds) && result.sampleIds.length > 0 && (
        <div className="small">Sample IDs: {result.sampleIds.join(", ")}</div>
      )}
    </div>
  );
}

function ResultDetailsCard({ log }) {
  const parsed = useMemo(() => safeJsonParse(log?.result), [log?.result]);

  if (!log) return null;

  if (!parsed) {
    return log.result ? (
      <pre style={{ margin: 0, whiteSpace: "pre-wrap", fontSize: 12 }}>
        {log.result}
      </pre>
    ) : (
      <div className="small neon-subtle">No result captured.</div>
    );
  }

  const action = (log.action || log.type || "").toUpperCase();
  if (action === "CALCULATE") {
    return <CalculatedResultCard result={parsed} />;
  }
  if (action === "SEARCH") {
    return <SearchResultCard result={parsed} />;
  }
  return (
    <pre style={{ margin: 0, whiteSpace: "pre-wrap", fontSize: 12 }}>
      {JSON.stringify(parsed, null, 2)}
    </pre>
  );
}

export default function QueryLogsPage() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedLog, setSelectedLog] = useState(null);

  useEffect(() => {
    fetchLogs();
  }, []);

  async function fetchLogs() {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get("/query-logs");
      setLogs(res.data || []);
    } catch (err) {
      console.error("Failed to load query logs", err);
      const httpBody = err?.response?.data;
      const bodyMsg = httpBody
        ? typeof httpBody === "string"
          ? httpBody
          : JSON.stringify(httpBody, null, 2)
        : null;
      const formatted =
        err?.formattedMessage ||
        err?.message ||
        bodyMsg ||
        "Failed to load query logs";
      setError(formatted + (bodyMsg ? `\n\nResponse body:\n${bodyMsg}` : ""));
    } finally {
      setLoading(false);
    }
  }

  const closeModal = () => setSelectedLog(null);

  return (
    <MotionWrapper>
      <div className="card glass glow-border neon-focus">
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
          }}
        >
          <div>
            <h2 className="neon-text">Query Logs</h2>
            <p
              className="small neon-subtle"
              style={{ marginTop: -12, marginBottom: 18 }}
            >
              View past queries recorded by the system.
            </p>
          </div>
          <div>
            <button
              className="primary"
              type="button"
              onClick={fetchLogs}
              disabled={loading}
              aria-label="Refresh logs"
            >
              {loading ? "Refreshing…" : "Refresh"}
            </button>
          </div>
        </div>

        {loading && <div className="small">Loading…</div>}
        {error && <div className="error">{error}</div>}

        {!loading && !error && (
          <div style={{ overflowX: "auto", marginTop: 12 }}>
            {logs.length === 0 ? (
              <div className="small neon-subtle">No query logs found.</div>
            ) : (
              <table
                className="logs-table"
                style={{ width: "100%", borderCollapse: "collapse" }}
              >
                <thead>
                  <tr>
                    <th style={{ textAlign: "left", padding: 8 }}>Timestamp</th>
                    <th style={{ textAlign: "left", padding: 8 }}>Origin</th>
                    <th style={{ textAlign: "left", padding: 8 }}>
                      Destination
                    </th>
                    <th style={{ textAlign: "left", padding: 8 }}>HS Code</th>
                    <th style={{ textAlign: "left", padding: 8 }}>Category</th>
                    <th style={{ textAlign: "left", padding: 8 }}>Declared</th>
                    <th style={{ textAlign: "left", padding: 8 }}>Weight</th>
                    <th style={{ textAlign: "left", padding: 8 }}>
                      Requested From
                    </th>
                    <th style={{ textAlign: "left", padding: 8 }}>
                      Requested To
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {logs.map((l) => (
                    <tr
                      key={l.id}
                      style={{
                        borderTop: "1px solid rgba(255,255,255,0.04)",
                        cursor: "pointer",
                      }}
                      onClick={() => setSelectedLog(l)}
                    >
                      <td style={{ padding: 10, whiteSpace: "nowrap" }}>
                        {l.createdAt
                          ? new Date(l.createdAt).toLocaleString()
                          : "-"}
                      </td>
                      <td style={{ padding: 10 }}>{l.origin || "-"}</td>
                      <td style={{ padding: 10 }}>{l.destination || "-"}</td>
                      <td style={{ padding: 10 }}>{l.hsCode || "-"}</td>
                      <td style={{ padding: 10 }}>{l.category || "-"}</td>
                      <td style={{ padding: 10 }}>{l.value || "-"}</td>
                      <td style={{ padding: 10 }}>{l.weight || "-"}</td>
                      <td style={{ padding: 10 }}>
                        {l.requestedEffectiveFrom || "-"}
                      </td>
                      <td style={{ padding: 10 }}>
                        {l.requestedEffectiveTo || "-"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}
      </div>

      {selectedLog && (
        <div
          role="dialog"
          aria-modal="true"
          className="modal-overlay"
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(4, 7, 19, 0.85)",
            zIndex: 1000,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            padding: "32px",
          }}
          onClick={closeModal}
        >
          <div
            className="card glass glow-border"
            style={{
              width: "min(720px, 95vw)",
              maxHeight: "90vh",
              overflowY: "auto",
              padding: 26,
              position: "relative",
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <button
              className="secondary"
              type="button"
              onClick={closeModal}
              style={{ position: "absolute", top: 16, right: 16 }}
              aria-label="Close"
            >
              Close
            </button>

            <h3 className="neon-text" style={{ marginBottom: 8 }}>
              Query Details
            </h3>
            <div className="small neon-subtle" style={{ marginBottom: 20 }}>
              {selectedLog.createdAt
                ? new Date(selectedLog.createdAt).toLocaleString()
                : "-"}{" "}
              | {selectedLog.username || "Anonymous"} |{" "}
              {selectedLog.action || selectedLog.type || "-"}
            </div>

            <section>
              <ResultDetailsCard log={selectedLog} />
            </section>
          </div>
        </div>
      )}
    </MotionWrapper>
  );
}

