import React from 'react'
import MotionWrapper from '../components/MotionWrapper.jsx'
import { Link } from 'react-router-dom'

export default function HomePage() {
  return (
    <MotionWrapper>
      <section className="hero">
        <div className="pill" style={{marginBottom:18}}>TARIFFSHERIFF Platform</div>
        <h1 className="gradient-text">Seamless Cross‑Border Tariff Intelligence</h1>
        <p>Modern, auditable and responsive tooling for compliance analysts to calculate duties, explore rate schedules and accelerate trade decisions with AI-powered insights.</p>
        <div className="hero-actions">
          <Link to="/calculate"><button className="primary" style={{minWidth:160}}>Launch Calculator</button></Link>
          <Link to="/rates"><button style={{minWidth:140}}>Browse Rates</button></Link>
        </div>
      </section>
  <section className="features-grid">
        {FEATURES.map(f => (
          <div key={f.title} className="card glow-border" style={{padding:'28px 24px'}}>
            <h3 style={{margin:'0 0 8px', fontSize:'18px'}}>{f.title}</h3>
            <p className="small" style={{margin:0, lineHeight:1.5}}>{f.desc}</p>
          </div>
        ))}
      </section>
    </MotionWrapper>
  )
}

const FEATURES = [
  { title:'Calculator with AI Summary', desc:'Intelligent tariff calculator with AI-powered trade impact analysis and recommendations based on historical data and trends.' },
  { title:'Query Logs', desc:'Comprehensive audit trail capturing all user queries with timestamps, search criteria, and authenticated user context for compliance tracking.' },
  { title:'Rate Schedules', desc:'Browse and search detailed tariff rate schedules across countries and product categories with temporal validity tracking.' },
  { title:'Trade Insights', desc:'Advanced analytics dashboard visualizing trade patterns, top routes, revenue projections, and tariff trends with interactive charts.' },
  { title:'Admin Console', desc:'Role-restricted administrative interface for managing tariff rates with full CRUD operations and audit logging.' },
  { title:'Security & Authorization', desc:'JWT-based authentication with role-based access control (ADMIN/USER roles), protected endpoints, and secure session management.' }
]
