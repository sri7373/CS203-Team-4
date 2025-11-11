import React, { useState } from "react";
import { useNavigate, useLocation, Link } from "react-router-dom";
import api from "../services/api.js";
import { setAuth } from "../services/auth.js";

export default function LoginPage() {
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("admin123");
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from?.pathname || "/calculate";

  // Reusable style for field validation errors
  const fieldErrorStyle = {
    marginTop: 6,
    fontSize: "13px",
    color: "#ef4444",
    padding: "6px 10px",
    backgroundColor: "rgba(239, 68, 68, 0.1)",
    borderRadius: "4px",
    border: "1px solid rgba(239, 68, 68, 0.2)",
  };

  const parseValidationError = (errorData) => {
    // Check if validationErrors object exists
    if (errorData?.validationErrors && typeof errorData.validationErrors === 'object') {
      return errorData.validationErrors;
    }
    return {};
  };

  const submit = async (e) => {
    e.preventDefault();
    setError(null);
    setFieldErrors({});
    setLoading(true);
    try {
      const res = await api.post("/auth/login", { username, password });
      setAuth(res.data);
      navigate(from, { replace: true });
    } catch (err) {
      console.error("Login error:", err);

      // Handle different error response formats
      if (err.response) {
        const status = err.response.status;
        const data = err.response.data;

        // First, try to parse validation errors
        const parsedErrors = parseValidationError(data);
        if (Object.keys(parsedErrors).length > 0) {
          setFieldErrors(parsedErrors);
          setError("Please fix the validation errors below.");
          return;
        }

        // Check if error data has a message property
        if (data?.message) {
          setError(data.message);
        } else if (typeof data === "string") {
          setError(data);
        } else {
          // Handle based on HTTP status code
          switch (status) {
            case 401:
              setError("Invalid username or password");
              break;
            case 403:
              setError("Access forbidden. Please check your credentials.");
              break;
            case 404:
              setError("Login service not found. Please contact support.");
              break;
            case 500:
              setError("Server error. Please try again later.");
              break;
            case 503:
              setError("Service unavailable. Please try again later.");
              break;
            default:
              setError(`Login failed with status ${status}`);
          }
        }
      } else if (err.request) {
        // Request was made but no response received
        setError("No response from server. Please check your connection.");
      } else {
        // Something else happened
        setError(err.message || "Login failed. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      className="card"
      style={{ maxWidth: 480, margin: "32px auto" }}
      aria-labelledby="loginTitle"
    >
      <h2 id="loginTitle">Sign In</h2>
      <p className="small" style={{ marginTop: -12, marginBottom: 24 }}>
        Access your tariff calculation workspace.
      </p>
      {error && (
        <div className="error" role="alert" style={{ marginBottom: 20 }}>
          <strong>Error:</strong> {error}
        </div>
      )}
      <form onSubmit={submit} noValidate>
        <div className="field">
          <label htmlFor="username">Username</label>
          <input
            id="username"
            className="input"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            placeholder="username"
            required
            aria-invalid={!!fieldErrors.username}
            aria-describedby={
              fieldErrors.username ? "username-error" : undefined
            }
          />
          {fieldErrors.username && (
            <div id="username-error" role="alert" style={fieldErrorStyle}>
              ⚠ {fieldErrors.username}
            </div>
          )}
        </div>
        <div className="field">
          <label htmlFor="password">Password</label>
          <input
            id="password"
            className="input"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            placeholder="password"
            required
            aria-invalid={!!fieldErrors.password}
            aria-describedby={
              fieldErrors.password ? "password-error" : undefined
            }
          />
          {fieldErrors.password && (
            <div id="password-error" role="alert" style={fieldErrorStyle}>
              ⚠ {fieldErrors.password}
            </div>
          )}
        </div>
        <div className="btn-group" style={{ marginTop: 4 }}>
          <button className="primary" type="submit" disabled={loading}>
            {loading ? "Signing in…" : "Sign in"}
          </button>
          <Link to="/register">
            <button type="button" disabled={loading}>
              Register
            </button>
          </Link>
        </div>
      </form>
    </div>
  );
}
