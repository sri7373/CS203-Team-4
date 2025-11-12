import React from "react";
import MotionWrapper from "../components/MotionWrapper.jsx";
import { Link } from "react-router-dom";
import { isAuthed, getUser } from "../services/auth.js";

export default function HomePage() {
  const loggedIn = isAuthed();
  const user = loggedIn ? getUser() : null;

  if (loggedIn) {
    return (
      <MotionWrapper>
        <section className="dashboard-hero card glass glow-border">
          <div>
            <p className="tiny" style={{ margin: 0, textTransform: "uppercase", letterSpacing: "0.25em" }}>
              Welcome back
            </p>
            <h2 style={{ margin: "4px 0 8px" }}>
              {user?.username ? `${user.username},` : "Analyst,"} your tariff cockpit is ready
            </h2>
            <p className="small" style={{ margin: 0 }}>
              Access calculators, live rates, and trade insights from one place. Everything here is optimised for fast, on-the-go decisions.
            </p>
          </div>
          <Link to="/calculate">
            <button className="primary" style={{ minWidth: 160 }}>
              Quick Calculate
            </button>
          </Link>
        </section>

        <section className="dashboard-grid">
          {DASHBOARD_CARDS.map((card) => (
            <article key={card.title} className="dashboard-card">
              <div className="dashboard-card-header">
                <span aria-hidden="true" className="dashboard-card-icon">
                  {card.icon}
                </span>
                <div>
                  <h3>{card.title}</h3>
                  <p className="tiny">{card.subtitle}</p>
                </div>
              </div>
              <p className="small" style={{ flex: "1 1 auto" }}>
                {card.description}
              </p>
              <div className="dashboard-card-actions">
                <Link to={card.to}>
                  <button className={card.primary ? "primary" : ""}>{card.cta}</button>
                </Link>
                {card.note && <span className="tiny">{card.note}</span>}
              </div>
            </article>
          ))}
        </section>
      </MotionWrapper>
    );
  }

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
    title: "Tariff Calculator",
    desc: "Simplified tariff calculator with informative calculation analysis.",
  },
  {
    title: "AI Summary",
    desc: "AI-generated summaries of tariff calculations and insights.",
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

const ICON_PROPS = {
  width: 28,
  height: 28,
  viewBox: "0 0 24 24",
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 1.8,
  strokeLinecap: "round",
  strokeLinejoin: "round",
};

const CARD_ICONS = {
  calculator: (
    <svg {...ICON_PROPS}>
      <rect x="5" y="3" width="14" height="18" rx="2" />
      <line x1="8" y1="7.5" x2="16" y2="7.5" />
      <line x1="9" y1="11" x2="11" y2="11" />
      <line x1="13" y1="11" x2="15" y2="11" />
      <line x1="9" y1="15" x2="11" y2="15" />
      <line x1="13" y1="15" x2="16" y2="15" />
    </svg>
  ),
  rates: (
    <svg {...ICON_PROPS}>
      <rect x="4" y="4" width="16" height="16" rx="2" />
      <line x1="8" y1="8" x2="16" y2="8" />
      <line x1="8" y1="12" x2="16" y2="12" />
      <line x1="8" y1="16" x2="13" y2="16" />
    </svg>
  ),
  globe: (
    <svg {...ICON_PROPS}>
      <circle cx="12" cy="12" r="8" />
      <ellipse cx="12" cy="12" rx="3.5" ry="8" />
      <line x1="4" y1="12" x2="20" y2="12" />
      <path d="M5.5 16.5c2.2-1 10.8-1 13 0" />
      <path d="M5.5 7.5c2.2 1 10.8 1 13 0" />
    </svg>
  ),
  logs: (
    <svg {...ICON_PROPS}>
      <rect x="4" y="5" width="16" height="14" rx="2" />
      <line x1="8" y1="9" x2="16" y2="9" />
      <line x1="8" y1="13" x2="14" y2="13" />
      <circle cx="7" cy="9" r="0.5" />
      <circle cx="7" cy="13" r="0.5" />
    </svg>
  ),
  export: (
    <svg {...ICON_PROPS}>
      <path d="M6 17h12" />
      <path d="M12 5v9" />
      <path d="M9 8l3-3 3 3" />
      <path d="M6 17v1a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2v-1" />
    </svg>
  ),
};

const DASHBOARD_CARDS = [
  {
    title: "Tariff Calculator",
    subtitle: "Core workflow",
    description: "Run point-to-point tariff scenarios, attach HS codes, and capture AI-assisted narratives in one step.",
    cta: "Open Calculator",
    to: "/calculate",
    icon: CARD_ICONS.calculator,
    primary: true,
  },
  {
    title: "Rates Library",
    subtitle: "Reference schedules",
    description: "Browse curated tariff schedules filtered by origin, destination, and product class to validate pricing.",
    cta: "Browse Rates",
    to: "/rates",
    icon: CARD_ICONS.rates,
  },
  {
    title: "Trade Insights",
    subtitle: "Country intelligence",
    description: "Review top partners, average inbound/export duties, and partners within seconds-perfect for pre-meeting prep.",
    cta: "View Insights",
    to: "/insights",
    icon: CARD_ICONS.globe,
  },
  {
    title: "Query History",
    subtitle: "Audit & recall",
    description: "Audit every tariff run, see parameters, and reload past searches when briefing clients or compliance.",
    cta: "Open Logs",
    to: "/query-logs",
    icon: CARD_ICONS.logs,
  },
  {
    title: "Export & Share",
    subtitle: "PDF + AI summary",
    description: "Use the calculator to export polished PDFs with AI summaries. Works best on desktop, but still mobile friendly.",
    cta: "Start Export",
    to: "/calculate",
    icon: CARD_ICONS.export,
  },
];


