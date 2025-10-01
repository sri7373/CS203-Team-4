import React from "react";
import MotionWrapper from "../components/MotionWrapper.jsx";
import { Link } from "react-router-dom";

export default function HomePage() {
  return (
    <MotionWrapper>
      <section className="hero">
        <div className="pill" style={{ marginBottom: 18 }}>
          TARIFFSHERIFF Platform
        </div>
        <h1 className="gradient-text">
          Absolute Best Tariff Calculator
        </h1>
        <p>
          Fancy, auditable and responsive tool for analysts to
          calculate tariffs, explore tariff insights and forecast trade
          trends with AI-powered insights (WIP).
        </p>
        <div className="hero-actions">
          <Link to="/calculate">
            <button className="primary" style={{ minWidth: 160 }}>
              Launch Calculator
            </button>
          </Link>
          <Link to="/rates">
            <button style={{ minWidth: 140 }}>Browse Rates</button>
          </Link>
        </div>
      </section>
      <section className="features-grid">
        {FEATURES.map((f) => (
          <div
            key={f.title}
            className="card glow-border"
            style={{ padding: "28px 24px" }}
          >
            <h3 style={{ margin: "0 0 8px", fontSize: "18px" }}>{f.title}</h3>
            <p className="small" style={{ margin: 0, lineHeight: 1.5 }}>
              {f.desc}
            </p>
          </div>
        ))}
      </section>
    </MotionWrapper>
  );
}

const FEATURES = [
  {
    title: "Calculator with AI Summary",
    desc: "Simplified tariff calculator with AI-powered trade analysis.",
  },
  {
    title: "Export to PDF",
    desc: "Export tariff calculations and reports to PDF format for easy sharing and printing.",
  },
  {
    title: "Rate Schedules",
    desc: "Browse and search detailed tariff rate schedules across countries and product categories.",
  },
  {
    title: "Trade Insights",
    desc: "Advanced analytics dashboard visualizing trade patterns, major trade partners with fancy card UI.",
  },
  {
    title: "Query Logs",
    desc: "Detailed audit trail capturing all user queries with timestamps, search criteria, and results at time of search for compliance tracking.",
  },
  {
    title: "Admin Console",
    desc: "Role-restricted administrative interface for managing tariff rates with full CRUD operations.",
  },
  {
    title: "Security & Authorization",
    desc: "JWT-based authentication with role-based access control (ADMIN/USER roles), protected endpoints, and secure session management.",
  },
];
