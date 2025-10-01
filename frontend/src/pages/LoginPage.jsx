import React, { useState } from "react";
import { useNavigate, useLocation, Link } from "react-router-dom";
import api from "../services/api.js";
import { setAuth } from "../services/auth.js";

export default function LoginPage() {
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("admin123");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from?.pathname || "/calculate";

  const submit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await api.post("/api/auth/login", { username, password });
      setAuth(res.data);
      navigate(from, { replace: true });
    } catch (err) {
      console.error("Login error:", err);

      // Handle different error response formats
      if (err.response) {
        const status = err.response.status;
        const data = err.response.data;

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
      <p className="small" style={{ marginTop: -12 }}>
        Access your tariff calculation workspace.
      </p>
      <form onSubmit={submit} noValidate>
        {error && (
          <div className="error" role="alert" style={{ marginTop: 16 }}>
            <strong>Error:</strong> {error}
          </div>
        )}
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
          />
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
          />
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
