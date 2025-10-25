import React, { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import api from "../services/api.js";
import Select from "./Select.jsx";
import {
  COUNTRY_CODES,
  PRODUCT_CATEGORIES,
} from "../constants/referenceOptions.js";

export default function TariffNewsSidebar({ limit = 8 }) {
  const [news, setNews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [collapsed, setCollapsed] = useState(false);
  const [selectedCountry, setSelectedCountry] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("");

  useEffect(() => {
    fetchNews();
  }, [selectedCountry, selectedCategory]);

  const fetchNews = async () => {
    setLoading(true);
    setError(null);

    try {
      const params = new URLSearchParams();
      if (selectedCountry && selectedCountry !== "") {
        params.append("country", selectedCountry.toLowerCase());
      }
      if (selectedCategory && selectedCategory !== "") {
        params.append("productCategory", selectedCategory.toLowerCase());
      }
      params.append("limit", limit);

      const response = await api.get(`/news/tariff?${params}`);
      setNews(response.data.articles || []);
    } catch (err) {
      console.error("Failed to fetch news:", err);
      setError(err.formattedMessage || "Failed to load news");
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return "";
    try {
      const date = new Date(dateString);
      const now = new Date();
      const diffMs = now - date;
      const diffHours = Math.floor(diffMs / (1000 * 60 * 60));

      if (diffHours < 1) return "Just now";
      if (diffHours < 24) return `${diffHours}h ago`;
      if (diffHours < 48) return "Yesterday";

      return date.toLocaleDateString("en-US", {
        month: "short",
        day: "numeric",
      });
    } catch (e) {
      return "";
    }
  };

  const truncateText = (text, maxLength = 120) => {
    if (!text) return "";
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength).trim() + "...";
  };

  return (
    <motion.div
      className="tariff-news-sidebar"
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.4 }}
      style={{
        position: "sticky",
        top: "20px",
        height: "fit-content",
        maxHeight: "calc(100vh - 40px)",
        display: "flex",
        flexDirection: "column",
      }}
    >
      <div
        className="card glass glow-border"
        style={{
          padding: collapsed ? "12px" : "16px",
          height: collapsed ? "auto" : "100%",
          maxHeight: collapsed ? "60px" : "800px",
          overflow: "hidden",
          transition: "all 0.3s ease",
        }}
      >
        {/* Header */}
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            marginBottom: collapsed ? 0 : 16,
            cursor: "pointer",
          }}
          onClick={() => setCollapsed(!collapsed)}
        >
          <h3
            className="neon-text"
            style={{
              fontSize: "16px",
              margin: 0,
              display: "flex",
              alignItems: "center",
              gap: "8px",
            }}
          >
            <span style={{ fontSize: "18px" }}>📰</span>
            Latest Tariff News
          </h3>
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              setCollapsed(!collapsed);
            }}
            style={{
              background: "none",
              border: "none",
              color: "var(--color-text-muted)",
              cursor: "pointer",
              fontSize: "20px",
              padding: "4px 8px",
              transition: "transform 0.3s ease",
              transform: collapsed ? "rotate(180deg)" : "rotate(0deg)",
            }}
            aria-label={collapsed ? "Expand news" : "Collapse news"}
          >
            ▼
          </button>
        </div>

        <AnimatePresence>
          {!collapsed && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: "auto" }}
              exit={{ opacity: 0, height: 0 }}
              transition={{ duration: 0.3 }}
              style={{
                overflowY: "auto",
                maxHeight: "calc(800px - 60px)",
              }}
            >
              {/* Country Filter */}
              <div style={{ marginBottom: 12 }}>
                <label
                  htmlFor="news-country-filter"
                  style={{
                    display: "block",
                    fontSize: "11px",
                    color: "var(--color-text-muted)",
                    marginBottom: "4px",
                    fontWeight: 600,
                    textTransform: "uppercase",
                    letterSpacing: "0.5px",
                  }}
                >
                  Filter by Country
                </label>
                <Select
                  id="news-country-filter"
                  value={selectedCountry}
                  onChange={(e) => setSelectedCountry(e.target.value)}
                  options={[
                    { value: "", label: "All Countries" },
                    ...COUNTRY_CODES.map((c) => ({
                      value: c.code,
                      label: `${c.flag} ${c.name}`,
                    })),
                  ]}
                  placeholder="Select country..."
                  style={{ fontSize: "13px" }}
                />
              </div>

              {/* Product Category Filter */}
              <div style={{ marginBottom: 16 }}>
                <label
                  htmlFor="news-category-filter"
                  style={{
                    display: "block",
                    fontSize: "11px",
                    color: "var(--color-text-muted)",
                    marginBottom: "4px",
                    fontWeight: 600,
                    textTransform: "uppercase",
                    letterSpacing: "0.5px",
                  }}
                >
                  Product Category
                </label>
                <Select
                  id="news-category-filter"
                  value={selectedCategory}
                  onChange={(e) => setSelectedCategory(e.target.value)}
                  options={[
                    { value: "", label: "All Categories" },
                    ...PRODUCT_CATEGORIES.map((c) => ({
                      value: c.value,
                      label: c.label,
                    })),
                  ]}
                  placeholder="Select category..."
                  style={{ fontSize: "13px" }}
                />
              </div>

              {/* Active Filters Display */}
              {(selectedCountry || selectedCategory) && (
                <div
                  style={{
                    marginBottom: 12,
                    padding: "8px 10px",
                    background: "rgba(129, 140, 248, 0.1)",
                    borderRadius: "6px",
                    border: "1px solid rgba(129, 140, 248, 0.2)",
                  }}
                >
                  <div
                    style={{
                      fontSize: "10px",
                      color: "var(--color-text-muted)",
                      marginBottom: 4,
                      textTransform: "uppercase",
                      fontWeight: 600,
                      letterSpacing: "0.5px",
                    }}
                  >
                    Active Filters
                  </div>
                  <div
                    style={{ display: "flex", gap: "6px", flexWrap: "wrap" }}
                  >
                    {selectedCountry && (
                      <span
                        style={{
                          fontSize: "11px",
                          padding: "2px 8px",
                          background: "rgba(129, 140, 248, 0.2)",
                          borderRadius: "10px",
                          color: "var(--color-text)",
                          display: "inline-flex",
                          alignItems: "center",
                          gap: "4px",
                        }}
                      >
                        {
                          COUNTRY_CODES.find((c) => c.code === selectedCountry)
                            ?.flag
                        }
                        {
                          COUNTRY_CODES.find((c) => c.code === selectedCountry)
                            ?.name
                        }
                        <button
                          onClick={() => setSelectedCountry("")}
                          style={{
                            background: "none",
                            border: "none",
                            color: "inherit",
                            cursor: "pointer",
                            padding: 0,
                            marginLeft: "2px",
                            fontSize: "12px",
                          }}
                        >
                          ×
                        </button>
                      </span>
                    )}
                    {selectedCategory && (
                      <span
                        style={{
                          fontSize: "11px",
                          padding: "2px 8px",
                          background: "rgba(129, 140, 248, 0.2)",
                          borderRadius: "10px",
                          color: "var(--color-text)",
                          display: "inline-flex",
                          alignItems: "center",
                          gap: "4px",
                        }}
                      >
                        {
                          PRODUCT_CATEGORIES.find(
                            (c) => c.value === selectedCategory
                          )?.label
                        }
                        <button
                          onClick={() => setSelectedCategory("")}
                          style={{
                            background: "none",
                            border: "none",
                            color: "inherit",
                            cursor: "pointer",
                            padding: 0,
                            marginLeft: "2px",
                            fontSize: "12px",
                          }}
                        >
                          ×
                        </button>
                      </span>
                    )}
                  </div>
                </div>
              )}

              {/* Loading State */}
              {loading && (
                <div style={{ textAlign: "center", padding: "24px" }}>
                  <div
                    className="spinner-small"
                    style={{ margin: "0 auto 12px" }}
                  />
                  <p
                    className="small"
                    style={{ color: "var(--color-text-muted)" }}
                  >
                    Loading news...
                  </p>
                </div>
              )}

              {/* Error State */}
              {error && !loading && (
                <div
                  className="error"
                  style={{
                    padding: "12px",
                    fontSize: "13px",
                    marginBottom: "12px",
                  }}
                >
                  <p style={{ margin: 0, marginBottom: 8 }}>{error}</p>
                  <button
                    type="button"
                    onClick={fetchNews}
                    className="btn-small"
                    style={{ fontSize: "12px", padding: "4px 12px" }}
                  >
                    Retry
                  </button>
                </div>
              )}

              {/* News List */}
              {!loading && !error && news.length > 0 && (
                <div
                  style={{
                    display: "flex",
                    flexDirection: "column",
                    gap: "12px",
                  }}
                >
                  {news.map((article, index) => (
                    <motion.article
                      key={article.articleId || index}
                      className="news-item"
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: index * 0.1 }}
                      style={{
                        padding: "12px",
                        background: "rgba(255, 255, 255, 0.03)",
                        border: "1px solid var(--color-border)",
                        borderRadius: "8px",
                        cursor: "pointer",
                        transition: "all 0.2s ease",
                      }}
                      whileHover={{
                        scale: 1.02,
                        borderColor: "var(--color-primary)",
                      }}
                      onClick={() =>
                        article.link && window.open(article.link, "_blank")
                      }
                    >
                      {/* Article Image */}
                      {article.imageUrl && (
                        <div
                          style={{
                            width: "100%",
                            height: "80px",
                            borderRadius: "6px",
                            overflow: "hidden",
                            marginBottom: "8px",
                          }}
                        >
                          <img
                            src={article.imageUrl}
                            alt={article.title}
                            referrerPolicy="no-referrer"
                            style={{
                              width: "100%",
                              height: "100%",
                              objectFit: "cover",
                            }}
                            onError={(event) => {
                              const { parentElement } = event.currentTarget;
                              if (parentElement) {
                                parentElement.remove();
                              }
                            }}
                          />
                        </div>
                      )}

                      {/* Article Title */}
                      <h4
                        style={{
                          fontSize: "13px",
                          fontWeight: 600,
                          margin: "0 0 6px 0",
                          lineHeight: "1.4",
                          color: "var(--color-text)",
                          display: "-webkit-box",
                          WebkitLineClamp: 2,
                          WebkitBoxOrient: "vertical",
                          overflow: "hidden",
                        }}
                      >
                        {article.title}
                      </h4>

                      {/* Article Description */}
                      {article.description && (
                        <p
                          style={{
                            fontSize: "12px",
                            color: "var(--color-text-muted)",
                            margin: "0 0 8px 0",
                            lineHeight: "1.4",
                            display: "-webkit-box",
                            WebkitLineClamp: 2,
                            WebkitBoxOrient: "vertical",
                            overflow: "hidden",
                          }}
                        >
                          {truncateText(article.description, 100)}
                        </p>
                      )}

                      {/* Article Meta */}
                      <div
                        style={{
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "center",
                          fontSize: "11px",
                          color: "var(--color-text-muted)",
                          marginTop: "8px",
                        }}
                      >
                        <span style={{ fontWeight: 600 }}>
                          {article.sourceId || "Unknown"}
                        </span>
                        <span>{formatDate(article.pubDate)}</span>
                      </div>

                      {/* Sentiment Badge (if available) */}
                      {article.sentiment && (
                        <div
                          style={{
                            display: "inline-block",
                            marginTop: "8px",
                            padding: "2px 8px",
                            borderRadius: "12px",
                            fontSize: "10px",
                            fontWeight: 600,
                            textTransform: "uppercase",
                            background:
                              article.sentiment === "positive"
                                ? "rgba(34, 197, 94, 0.2)"
                                : article.sentiment === "negative"
                                ? "rgba(239, 68, 68, 0.2)"
                                : "rgba(156, 163, 175, 0.2)",
                            color:
                              article.sentiment === "positive"
                                ? "#22c55e"
                                : article.sentiment === "negative"
                                ? "#ef4444"
                                : "#9ca3af",
                          }}
                        >
                          {article.sentiment}
                        </div>
                      )}
                    </motion.article>
                  ))}
                </div>
              )}

              {/* Empty State */}
              {!loading && !error && news.length === 0 && (
                <div style={{ textAlign: "center", padding: "24px" }}>
                  <p
                    className="small"
                    style={{ color: "var(--color-text-muted)" }}
                  >
                    No tariff news available at the moment.
                  </p>
                  <button
                    type="button"
                    onClick={fetchNews}
                    className="btn-small"
                    style={{
                      fontSize: "12px",
                      padding: "4px 12px",
                      marginTop: "12px",
                    }}
                  >
                    Refresh
                  </button>
                </div>
              )}

              {/* Footer */}
              {!loading && !error && news.length > 0 && (
                <div
                  style={{
                    marginTop: "16px",
                    paddingTop: "12px",
                    borderTop: "1px solid var(--color-border)",
                    textAlign: "center",
                  }}
                >
                  <button
                    type="button"
                    onClick={fetchNews}
                    className="btn-small"
                    style={{ fontSize: "12px", padding: "4px 12px" }}
                  >
                    Refresh News
                  </button>
                  <p
                    className="tiny"
                    style={{
                      marginTop: "8px",
                      color: "var(--color-text-muted)",
                      opacity: 0.7,
                    }}
                  >
                    Powered by NewsData.io
                  </p>
                </div>
              )}
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </motion.div>
  );
}
