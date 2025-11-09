import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { AnimatePresence, motion } from "framer-motion";
import MotionWrapper from "../components/MotionWrapper.jsx";
import Select from "../components/Select.jsx";
import {
  COUNTRY_CODES,
  PRODUCT_CATEGORIES,
  DEFAULT_DESTINATION_CODE,
  DEFAULT_ORIGIN_CODE,
  DEFAULT_PRODUCT_CATEGORY,
  PRODUCT_CATEGORY_CODES,
} from "../constants/referenceOptions.js";
import api from "../services/api.js";
import {
  fetchCountries,
  fetchProductCategories,
} from "../services/reference.js";

const toOptionValue = (option) =>
  typeof option === "string" ? option : option?.value || "";
const listOptionValues = (options) =>
  options.map(toOptionValue).filter(Boolean);
const resolveOptionValue = (current, options, fallback) => {
  const values = listOptionValues(options);
  if (current && values.includes(current)) return current;
  if (fallback && values.includes(fallback)) return fallback;
  return values[0] || "";
};

const EMPTY_FORM = {
  originCountryCode: "",
  destinationCountryCode: "",
  productCategoryCode: "",
  baseRate: "",
  additionalFee: "",
  effectiveFrom: "",
  effectiveTo: "",
};

export default function AdminTariffsPage() {
  const [tariffs, setTariffs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [listError, setListError] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [editing, setEditing] = useState(null);
  const [formError, setFormError] = useState(null);
  const [feedback, setFeedback] = useState(null);
  const [feedbackType, setFeedbackType] = useState("success"); // 'success', 'error', 'info'
  const [saving, setSaving] = useState(false);
  const [filter, setFilter] = useState("");
  const [deletingId, setDeletingId] = useState(null);
  const [operationStatus, setOperationStatus] = useState(null); // Track last operation
  const [referenceError, setReferenceError] = useState(null);
  const [refLoading, setRefLoading] = useState(true);
  const [countries, setCountries] = useState([]);
  const [categories, setCategories] = useState([]);
  const [deleteConfirmation, setDeleteConfirmation] = useState(null); // { tariff, show }
  const feedbackTimeoutRef = useRef(null);

  const loadReferenceData = useCallback(async () => {
    setRefLoading(true);
    try {
      const [countryData, categoryData] = await Promise.all([
        fetchCountries(),
        fetchProductCategories(),
      ]);
      const allowedCountries = countryData
        .map((item) => {
          const code = (item.code || "").toUpperCase();
          if (!code) return null;
          const name = item.name ? item.name.trim() : "";
          return {
            value: code,
            label: name ? `${code} — ${name}` : code,
            name: name || code,
          };
        })
        .filter(Boolean);
      const allowedCategories = categoryData
        .map((item) => {
          const code = (item.code || "").toUpperCase();
          if (!code) return null;
          const name = item.name ? item.name.trim() : "";
          return {
            value: code,
            label: name ? `${code} — ${name}` : code,
            name: name || code,
          };
        })
        .filter(Boolean);
      setCountries(allowedCountries);
      setCategories(allowedCategories);
      setForm((prev) => ({
        ...prev,
        originCountryCode: resolveOptionValue(
          prev.originCountryCode,
          allowedCountries,
          DEFAULT_ORIGIN_CODE
        ),
        destinationCountryCode: resolveOptionValue(
          prev.destinationCountryCode,
          allowedCountries,
          DEFAULT_DESTINATION_CODE
        ),
        productCategoryCode: resolveOptionValue(
          prev.productCategoryCode,
          allowedCategories,
          DEFAULT_PRODUCT_CATEGORY
        ),
      }));
      setReferenceError(null);
    } catch (err) {
      console.error("Failed to load reference data", err);
      const fallbackCountries = COUNTRY_CODES.map((country) => ({
        value: country.value ?? country.code,
        label: country.label ?? country.name ?? country.value ?? country.code,
        name: country.name ?? country.label ?? country.value ?? country.code,
      }));
      const fallbackCategories = PRODUCT_CATEGORIES.map((category) => ({
        value: category.value ?? category,
        label: category.label ?? category.value ?? category,
        name: category.label ?? category.value ?? category,
      }));
      setCountries(fallbackCountries);
      setCategories(fallbackCategories);
      setForm((prev) => ({
        ...prev,
        originCountryCode: resolveOptionValue(
          prev.originCountryCode,
          fallbackCountries,
          DEFAULT_ORIGIN_CODE
        ),
        destinationCountryCode: resolveOptionValue(
          prev.destinationCountryCode,
          fallbackCountries,
          DEFAULT_DESTINATION_CODE
        ),
        productCategoryCode: resolveOptionValue(
          prev.productCategoryCode,
          fallbackCategories,
          DEFAULT_PRODUCT_CATEGORY
        ),
      }));
      setReferenceError(
        "Unable to load reference data. Using default options."
      );
    } finally {
      setRefLoading(false);
    }
  }, []);

  useEffect(() => {
    loadReferenceData();
  }, [loadReferenceData]);

  const loadTariffs = useCallback(async () => {
    setLoading(true);
    setListError(null);
    try {
      const res = await api.get("/tariffs");
      setTariffs(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      console.error("Failed to load tariffs", err);
      setListError(
        err?.formattedMessage ||
          err?.response?.data?.message ||
          "Failed to load tariff rates"
      );
      setTariffs([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadTariffs();
  }, [loadTariffs]);

  // Cleanup feedback timeout on unmount
  useEffect(() => {
    return () => {
      if (feedbackTimeoutRef.current) {
        clearTimeout(feedbackTimeoutRef.current);
      }
    };
  }, []);

  const filteredTariffs = useMemo(() => {
    if (!filter.trim()) return tariffs;
    const q = filter.trim().toLowerCase();
    return tariffs.filter((t) =>
      [
        t.id,
        t.originCountryCode,
        t.destinationCountryCode,
        t.productCategoryCode,
      ]
        .filter(Boolean)
        .map(String)
        .some((value) => value.toLowerCase().includes(q))
    );
  }, [tariffs, filter]);

  const resetForm = () => {
    setEditing(null);
    setForm({
      ...EMPTY_FORM,
      originCountryCode: resolveOptionValue(
        DEFAULT_ORIGIN_CODE,
        countries,
        DEFAULT_ORIGIN_CODE
      ),
      destinationCountryCode: resolveOptionValue(
        DEFAULT_DESTINATION_CODE,
        countries,
        DEFAULT_DESTINATION_CODE
      ),
      productCategoryCode: resolveOptionValue(
        DEFAULT_PRODUCT_CATEGORY,
        categories,
        DEFAULT_PRODUCT_CATEGORY
      ),
    });
    setFormError(null);
    setFeedback(null);
    setOperationStatus(null);
  };

  const showFeedback = (message, type = "success") => {
    // Clear any existing timeout
    if (feedbackTimeoutRef.current) {
      clearTimeout(feedbackTimeoutRef.current);
    }

    setFeedback(message);
    setFeedbackType(type);

    // Auto-hide after 5 seconds
    feedbackTimeoutRef.current = setTimeout(() => {
      setFeedback(null);
      feedbackTimeoutRef.current = null;
    }, 5000);
  };

  const beginEdit = (tariff) => {
    setEditing(tariff);
    setForm({
      originCountryCode: tariff.originCountryCode || "",
      destinationCountryCode: tariff.destinationCountryCode || "",
      productCategoryCode: tariff.productCategoryCode || "",
      baseRate: tariff.baseRate != null ? String(tariff.baseRate) : "",
      additionalFee:
        tariff.additionalFee != null ? String(tariff.additionalFee) : "",
      effectiveFrom: tariff.effectiveFrom || "",
      effectiveTo: tariff.effectiveTo || "",
    });
    setFormError(null);
    setFeedback(null);
    setOperationStatus(null);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const handleSelectChange = (key) => (value) => {
    setForm((prev) => ({ ...prev, [key]: value }));
  };

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const ensureReferenceValue = (value, options) =>
    listOptionValues(options).includes(value);

  const buildPayload = () => ({
    originCountryCode: form.originCountryCode?.trim().toUpperCase(),
    destinationCountryCode: form.destinationCountryCode?.trim().toUpperCase(),
    productCategoryCode: form.productCategoryCode?.trim().toUpperCase(),
    baseRate: form.baseRate === "" ? null : Number(form.baseRate),
    additionalFee:
      form.additionalFee === "" ? null : Number(form.additionalFee),
    effectiveFrom: form.effectiveFrom || null,
    effectiveTo: form.effectiveTo || null,
  });

  const handleSubmit = async (event) => {
    event.preventDefault();
    setFormError(null);
    setFeedback(null);

    const payload = buildPayload();

    if (
      !payload.originCountryCode ||
      !payload.destinationCountryCode ||
      !payload.productCategoryCode
    ) {
      setFormError("Origin, destination and category are required.");
      return;
    }
    if (
      !ensureReferenceValue(payload.originCountryCode, countries) ||
      !ensureReferenceValue(payload.destinationCountryCode, countries)
    ) {
      setFormError("Origin and destination must match a known country code.");
      return;
    }
    if (!ensureReferenceValue(payload.productCategoryCode, categories)) {
      setFormError("Product category must match a known category code.");
      return;
    }
    if (payload.baseRate == null || Number.isNaN(payload.baseRate)) {
      setFormError("Base rate is required and must be numeric.");
      return;
    }
    if (payload.additionalFee == null || Number.isNaN(payload.additionalFee)) {
      setFormError("Additional fee is required and must be numeric.");
      return;
    }
    if (!payload.effectiveFrom) {
      setFormError("Effective-from date is required.");
      return;
    }

    setSaving(true);
    try {
      if (editing) {
        await api.put(`/tariffs/${editing.id}`, payload);
        setOperationStatus({ type: "update", id: editing.id });
        showFeedback(`✁ETariff #${editing.id} updated successfully`, "success");
        resetForm();
      } else {
        const response = await api.post("/tariffs", payload);
        const newId = response.data?.id || "new";
        setOperationStatus({ type: "create", id: newId });
        showFeedback(`✁ETariff created successfully (ID: ${newId})`, "success");
        resetForm();
      }
      await loadTariffs();
    } catch (err) {
      console.error("Save failed", err);
      const errorMsg =
        err?.formattedMessage || err?.response?.data?.message || "Save failed";
      setFormError(errorMsg);
      showFeedback(`✁E${errorMsg}`, "error");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (tariff) => {
    if (!tariff) return;
    setDeleteConfirmation({ tariff, show: true });
  };

  const confirmDelete = async () => {
    const tariff = deleteConfirmation?.tariff;
    if (!tariff) return;

    setDeleteConfirmation(null);
    setDeletingId(tariff.id);
    setFormError(null);
    try {
      await api.delete(`/tariffs/${tariff.id}`);
      setOperationStatus({ type: "delete", id: tariff.id });
      showFeedback(`✁ETariff #${tariff.id} deleted successfully`, "success");
      await loadTariffs();
      if (editing?.id === tariff.id) {
        resetForm();
      }
    } catch (err) {
      console.error("Delete failed", err);
      const errorMsg =
        err?.formattedMessage ||
        err?.response?.data?.message ||
        "Delete failed";
      setFormError(errorMsg);
      showFeedback(`✗${errorMsg}`, "error");
    } finally {
      setDeletingId(null);
    }
  };

  const cancelDelete = () => {
    setDeleteConfirmation(null);
  };

  return (
    <MotionWrapper>
      <div
        className="card glass glow-border neon-focus"
        aria-labelledby="tariffAdminTitle"
        style={{ position: "relative", overflow: "visible" }}
      >
        <motion.h2
          id="tariffAdminTitle"
          className="neon-text"
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, ease: [0.4, 0, 0.2, 1] }}
        >
          Tariff Administration
        </motion.h2>
        <p
          className="small neon-subtle"
          style={{ marginTop: -12, marginBottom: 24 }}
        >
          Manage tariff schedules that drive rate calculations. Only
          administrators can access this console.
        </p>

        {referenceError && (
          <div
            className="error"
            role="alert"
            style={{ marginBottom: 16, marginTop: -8 }}
          >
            {referenceError}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate className="admin-form">
          <div className="inline-fields field-cluster">
            <div className="field" style={{ flex: "1 1 220px" }}>
              <label htmlFor="originCountryCode">Origin Country Code</label>
              <Select
                id="originCountryCode"
                value={form.originCountryCode}
                onChange={handleSelectChange("originCountryCode")}
                options={countries}
                disabled={refLoading || !countries.length}
                placeholder={refLoading ? "Loading…" : "(Select)"}
              />
            </div>
            <div className="field" style={{ flex: "1 1 220px" }}>
              <label htmlFor="destinationCountryCode">
                Destination Country Code
              </label>
              <Select
                id="destinationCountryCode"
                value={form.destinationCountryCode}
                onChange={handleSelectChange("destinationCountryCode")}
                options={countries}
                disabled={refLoading || !countries.length}
                placeholder={refLoading ? "Loading…" : "(Select)"}
              />
            </div>
            <div className="field" style={{ flex: "1 1 220px" }}>
              <label htmlFor="productCategoryCode">Product Category</label>
              <Select
                id="productCategoryCode"
                value={form.productCategoryCode}
                onChange={handleSelectChange("productCategoryCode")}
                options={categories}
                disabled={refLoading || !categories.length}
                placeholder={refLoading ? "Loading…" : "(Select)"}
              />
            </div>
          </div>

          <div className="inline-fields field-cluster">
            <div className="field" style={{ flex: "1 1 200px" }}>
              <label htmlFor="baseRate">Base Rate (decimal)</label>
              <input
                id="baseRate"
                className="input"
                name="baseRate"
                type="number"
                step="0.0001"
                min="0"
                value={form.baseRate}
                onChange={handleChange}
                required
                placeholder="e.g. 0.05"
              />
            </div>
            <div className="field" style={{ flex: "1 1 200px" }}>
              <label htmlFor="additionalFee">Additional Fee</label>
              <input
                id="additionalFee"
                className="input"
                name="additionalFee"
                type="number"
                step="0.01"
                min="0"
                value={form.additionalFee}
                onChange={handleChange}
                required
                placeholder="e.g. 25"
              />
            </div>
            <div className="field" style={{ flex: "1 1 200px" }}>
              <label htmlFor="effectiveFrom">Effective From</label>
              <input
                id="effectiveFrom"
                className="input"
                name="effectiveFrom"
                type="date"
                value={form.effectiveFrom}
                onChange={handleChange}
                required
              />
            </div>
            <div className="field" style={{ flex: "1 1 200px" }}>
              <label htmlFor="effectiveTo">Effective To (optional)</label>
              <input
                id="effectiveTo"
                className="input"
                name="effectiveTo"
                type="date"
                value={form.effectiveTo}
                onChange={handleChange}
              />
            </div>
          </div>

          <div className="btn-group" style={{ marginTop: 12 }}>
            <button
              className="primary"
              type="submit"
              disabled={saving || refLoading}
            >
              {saving ? (
                <>
                  <span
                    className="spinner"
                    style={{ width: 14, height: 14, marginRight: 8 }}
                    aria-hidden="true"
                  />
                  {editing ? "Updating…" : "Creating…"}
                </>
              ) : editing ? (
                "Update Tariff"
              ) : (
                "Create Tariff"
              )}
            </button>
            <button type="button" onClick={resetForm} disabled={saving}>
              {editing ? "Cancel Edit" : "Clear"}
            </button>
          </div>

          {editing && (
            <motion.div
              className="badge"
              style={{
                marginTop: 12,
                padding: "8px 12px",
                background: "rgba(99, 102, 241, 0.2)",
                border: "1px solid rgba(99, 102, 241, 0.4)",
                borderRadius: 6,
                display: "inline-block",
              }}
              initial={{ opacity: 0, y: -5 }}
              animate={{ opacity: 1, y: 0 }}
            >
              Editing Tariff #{editing.id}
            </motion.div>
          )}
        </form>

        {formError && (
          <motion.div
            className="error"
            role="alert"
            style={{ marginTop: 20 }}
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <strong>Error:</strong> {String(formError)}
          </motion.div>
        )}

        <AnimatePresence>
          {feedback && (
            <motion.div
              className={
                feedbackType === "success"
                  ? "success"
                  : feedbackType === "error"
                  ? "error"
                  : "info"
              }
              role={feedbackType === "success" ? "status" : "alert"}
              style={{
                marginTop: 12,
                padding: "12px 16px",
                borderRadius: 8,
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                gap: 12,
                boxShadow:
                  feedbackType === "success"
                    ? "0 4px 12px rgba(34, 197, 94, 0.3)"
                    : feedbackType === "error"
                    ? "0 4px 12px rgba(239, 68, 68, 0.3)"
                    : "0 4px 12px rgba(99, 102, 241, 0.3)",
              }}
              initial={{ opacity: 0, y: -10, scale: 0.95 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: -10, scale: 0.95 }}
              transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
            >
              <span style={{ flex: 1 }}>{feedback}</span>
              <button
                type="button"
                onClick={() => setFeedback(null)}
                style={{
                  background: "transparent",
                  border: "none",
                  color: "inherit",
                  cursor: "pointer",
                  padding: "4px 8px",
                  opacity: 0.7,
                  fontSize: "16px",
                }}
                aria-label="Dismiss notification"
              >
                ✕              </button>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Delete Confirmation Modal */}
      <AnimatePresence>
        {deleteConfirmation?.show && (
          <motion.div
            style={{
              position: "fixed",
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              backgroundColor: "rgba(0, 0, 0, 0.7)",
              backdropFilter: "blur(8px)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              zIndex: 9999,
              padding: "20px",
            }}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            onClick={cancelDelete}
          >
            <motion.div
              className="card glass glow-border"
              style={{
                maxWidth: "500px",
                width: "100%",
                position: "relative",
                boxShadow: "0 8px 32px rgba(198, 40, 40, 0.3)",
              }}
              initial={{ scale: 0.9, opacity: 0, y: 20 }}
              animate={{ scale: 1, opacity: 1, y: 0 }}
              exit={{ scale: 0.9, opacity: 0, y: 20 }}
              transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
              onClick={(e) => e.stopPropagation()}
            >
              <div className="card-header" style={{ marginBottom: 20 }}>
                <h3 className="neon-text" style={{ margin: 0 }}>
                  Confirm Deletion
                </h3>
              </div>

              <p
                className="small neon-subtle"
                style={{ marginTop: -12, marginBottom: 24 }}
              >
                Are you sure you want to delete Tariff #
                {deleteConfirmation?.tariff?.id || "N/A"}?
              </p>

              <div className="btn-group" style={{ justifyContent: "flex-end" }}>
                <button type="button" className="btn" onClick={cancelDelete}>
                  Cancel
                </button>
                <button
                  type="button"
                  className="btn"
                  onClick={confirmDelete}
                  style={{
                    backgroundColor: "rgba(198, 40, 40, 0.8)",
                    borderColor: "rgba(198, 40, 40, 1)",
                  }}
                >
                  Delete Tariff
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <div
        className="card glass glow-border neon-focus"
        style={{ marginTop: 28 }}
      >
        <div
          className="card-header"
          style={{
            display: "flex",
            flexWrap: "wrap",
            gap: 16,
            alignItems: "center",
            justifyContent: "space-between",
          }}
        >
          <h3 className="neon-subtle" style={{ margin: 0 }}>
            Configured Tariffs
          </h3>
          <input
            className="input admin-filter-input"
            type="search"
            placeholder="Filter by id, origin, destination or category"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            style={{ maxWidth: 320 }}
            aria-label="Filter tariffs"
          />
        </div>

        {listError && (
          <div className="error" role="alert" style={{ marginTop: 16 }}>
            {String(listError)}
          </div>
        )}

        {loading && (
          <motion.div
            className="skeleton"
            style={{
              marginTop: 24,
              padding: 24,
              textAlign: "center",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              gap: 12,
            }}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
          >
            <div className="spinner" aria-hidden="true" />
            <span>Loading tariffs…</span>
          </motion.div>
        )}

        <AnimatePresence>
          {!loading && filteredTariffs.length > 0 && (
            <motion.table
              key={filteredTariffs.map((t) => t.id).join("-")}
              className="table-glow"
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -12 }}
              transition={{ duration: 0.4, ease: [0.4, 0, 0.2, 1] }}
              style={{ marginTop: 16 }}
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
                  <th style={{ width: 140 }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredTariffs.map((tariff) => {
                  const isHighlighted = editing?.id === tariff.id;
                  const wasRecentlyModified =
                    operationStatus?.id === tariff.id &&
                    (operationStatus.type === "update" ||
                      operationStatus.type === "create");

                  return (
                    <motion.tr
                      key={tariff.id}
                      className={isHighlighted ? "row-highlight" : undefined}
                      style={{
                        backgroundColor: wasRecentlyModified
                          ? "rgba(34, 197, 94, 0.1)"
                          : undefined,
                        transition: "background-color 2s ease-out",
                      }}
                      initial={
                        operationStatus?.type === "create" &&
                        operationStatus.id === tariff.id
                          ? { opacity: 0, x: -20 }
                          : false
                      }
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ duration: 0.5, ease: [0.4, 0, 0.2, 1] }}
                    >
                      <td>
                        {wasRecentlyModified && (
                          <span style={{ marginRight: 6 }}>
                            {operationStatus.type === "create" ? "✨" : "💾"}
                          </span>
                        )}
                        {tariff.id}
                      </td>
                      <td>{tariff.originCountryCode}</td>
                      <td>{tariff.destinationCountryCode}</td>
                      <td>{tariff.productCategoryCode}</td>
                      <td>{tariff.baseRate}</td>
                      <td>{tariff.additionalFee}</td>
                      <td>{tariff.effectiveFrom}</td>
                      <td>{tariff.effectiveTo || "-"}</td>
                      <td>
                        <div className="btn-group">
                          <button
                            type="button"
                            onClick={() => beginEdit(tariff)}
                            disabled={saving}
                          >
                            Edit
                          </button>
                          <button
                            type="button"
                            className="danger"
                            onClick={() => handleDelete(tariff)}
                            disabled={deletingId === tariff.id || saving}
                          >
                            {deletingId === tariff.id ? (
                              <>
                                <span
                                  className="spinner"
                                  style={{
                                    width: 12,
                                    height: 12,
                                    marginRight: 6,
                                  }}
                                  aria-hidden="true"
                                />
                                Deleting…
                              </>
                            ) : (
                              "Delete"
                            )}
                          </button>
                        </div>
                      </td>
                    </motion.tr>
                  );
                })}
              </tbody>
            </motion.table>
          )}
        </AnimatePresence>

        {!loading && filteredTariffs.length === 0 && !listError && (
          <motion.div
            className="empty"
            style={{
              marginTop: 24,
              padding: 32,
              textAlign: "center",
              background: "rgba(255,255,255,0.03)",
              borderRadius: 12,
              border: "1px dashed rgba(255,255,255,0.1)",
            }}
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
          >
            <div style={{ fontSize: 48, marginBottom: 16, opacity: 0.5 }}>
              📋
            </div>
            <p style={{ margin: 0, fontSize: 16, fontWeight: 500 }}>
              {filter
                ? "No tariffs match your filter"
                : "No tariffs configured yet"}
            </p>
            <p className="small" style={{ marginTop: 8, opacity: 0.7 }}>
              {filter
                ? "Try adjusting your search terms"
                : "Create your first tariff rate using the form above"}
            </p>
          </motion.div>
        )}
      </div>
    </MotionWrapper>
  );
}
