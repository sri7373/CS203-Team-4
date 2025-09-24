import React from 'react'
import MotionWrapper from '../components/MotionWrapper.jsx'
import { Link } from 'react-router-dom'

export default function HomePage() {
  return (
    <MotionWrapper>
      <section className="hero">
        <div className="pill" style={{marginBottom:18}}>Intelligent Tariff Platform</div>
        <h1 className="gradient-text">Seamless Cross‑Border Tariff Intelligence</h1>
        <p>Modern, auditable and responsive tooling for compliance analysts to calculate duties, explore rate schedules and accelerate trade decisions.</p>
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
  { title:'Regulatory Accuracy', desc:'Deterministic calculations leveraging curated tariff schedules and temporal validity windows.' },
  { title:'Transparent Breakdown', desc:'Full cost decomposition: base rate, additional fees and total landed cost with effective date context.' },
  { title:'Secure Access', desc:'Role‑based JWT authentication ensures only authorized analysts can access sensitive trade data.' },
  { title:'Responsive UX', desc:'Adaptive interface with animated feedback and dark mode for extended analyst sessions.' }
]
