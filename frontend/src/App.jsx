import React from 'react'
import { Outlet } from 'react-router-dom'
import NavBar from './components/NavBar.jsx'
import AnimatedBackground from './components/AnimatedBackground.jsx'

export default function App() {
  const year = new Date().getFullYear()
  return (
    <div className="app-shell">
      <AnimatedBackground opacity={0.25} />
      <NavBar />
      <main className="app-main" role="main">
        <div className="workspace-narrow">
          <Outlet />
        </div>
      </main>
      <footer>
        <div>© {year} TARIFFSHERIFF Platform. All rights reserved.</div>
        <div className="legal">Confidential – Internal Use Only</div>
      </footer>
    </div>
  )
}
