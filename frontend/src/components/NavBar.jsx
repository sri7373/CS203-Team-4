import React, { useEffect, useState } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { isAuthed, getUser, logout } from "../services/auth.js";

export default function NavBar() {
  const navigate = useNavigate();
  const location = useLocation();
  const loggedIn = isAuthed();
  const user = getUser();
  const [theme, setTheme] = useState(() => {
    return localStorage.getItem("ui_theme") || "light";
  });

  useEffect(() => {
    if (theme === "dark") {
      document.documentElement.setAttribute("data-theme", "dark");
    } else {
      document.documentElement.removeAttribute("data-theme");
    }
    localStorage.setItem("ui_theme", theme);
  }, [theme]);

  const toggleTheme = () => setTheme((t) => (t === "dark" ? "light" : "dark"));

  const doLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  const linkActive = (path) => location.pathname.startsWith(path);

  return (
    <nav aria-label="Primary">
      <div className="wrap">
        <Link to="/" className="brand" aria-label="Tariff Home">
          <span className="logo" />
          <span>TARIFFSHERIFF</span>
        </Link>
        {loggedIn && (
          <>
            <Link
              to="/calculate"
              className={linkActive("/calculate") ? "active" : ""}
              aria-current={linkActive("/calculate") ? "page" : undefined}
            >
              Calculate
            </Link>
            <Link
              to="/rates"
              className={linkActive("/rates") ? "active" : ""}
              aria-current={linkActive("/rates") ? "page" : undefined}
            >
              Rates
            </Link>
            <Link
              to="/insights"
              className={linkActive("/insights") ? "active" : ""}
              aria-current={linkActive("/insights") ? "page" : undefined}
            >
              Insights
            </Link>
            <Link
              to="/query-logs"
              className={linkActive("/query-logs") ? "active" : ""}
              aria-current={linkActive("/query-logs") ? "page" : undefined}
            >
              Query Logs
            </Link>
            {user?.role === "ADMIN" && (
              <Link
                to="/admin/tariffs"
                className={linkActive("/admin/tariffs") ? "active" : ""}
                aria-current={linkActive("/admin/tariffs") ? "page" : undefined}
              >
                Admin Console
              </Link>
            )}
          </>
        )}
        <div className="right">
          <button
            type="button"
            onClick={toggleTheme}
            className="theme-toggle"
            aria-label="Toggle theme"
          >
            {theme === "dark" ? "Light Theme" : "Dark Theme"}
          </button>
          {!loggedIn && <Link to="/login">Login</Link>}
          {!loggedIn && <Link to="/register">Register</Link>}
          {loggedIn && (
            <div className="user-meta" aria-live="polite">
              <span
                className="badge"
                style={{ background: "var(--color-primary)" }}
              >
                {user?.role}
              </span>{" "}
              {user?.username}
            </div>
          )}
          {loggedIn && (
            <button
              type="button"
              onClick={doLogout}
              className="danger"
              aria-label="Logout"
            >
              Logout
            </button>
          )}
        </div>
      </div>
    </nav>
  );
}
