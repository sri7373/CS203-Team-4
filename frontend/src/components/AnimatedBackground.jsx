import React, { useEffect, useRef } from 'react'
import { motion } from 'framer-motion'

/* Lightweight animated background inspired by creative component libraries.
   Uses a canvas for soft radial pulses and a motion overlay for gradient sheen. */
export default function AnimatedBackground({ opacity = 0.35 }) {
  const canvasRef = useRef(null)
  const circlesRef = useRef([])

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    let raf

    const resize = () => {
      canvas.width = window.innerWidth
      canvas.height = window.innerHeight
      // regenerate circles
      circlesRef.current = Array.from({ length: 6 }).map(() => ({
        x: Math.random() * canvas.width,
        y: Math.random() * canvas.height,
        r: 180 + Math.random() * 240,
        dx: (Math.random() - 0.5) * 0.2,
        dy: (Math.random() - 0.5) * 0.2,
        hue: Math.floor(Math.random() * 360)
      }))
    }
    resize()
    window.addEventListener('resize', resize)

    const render = () => {
      ctx.clearRect(0,0,canvas.width, canvas.height)
      circlesRef.current.forEach(c => {
        c.x += c.dx; c.y += c.dy
        if (c.x < -c.r) c.x = canvas.width + c.r
        if (c.x > canvas.width + c.r) c.x = -c.r
        if (c.y < -c.r) c.y = canvas.height + c.r
        if (c.y > canvas.height + c.r) c.y = -c.r
        const grd = ctx.createRadialGradient(c.x, c.y, 0, c.x, c.y, c.r)
        grd.addColorStop(0, `hsla(${c.hue}, 85%, 65%, 0.6)`)
        grd.addColorStop(1, 'hsla(0,0%,0%,0)')
        ctx.globalCompositeOperation = 'lighter'
        ctx.fillStyle = grd
        ctx.beginPath(); ctx.arc(c.x, c.y, c.r, 0, Math.PI * 2); ctx.fill()
      })
      raf = requestAnimationFrame(render)
    }
    raf = requestAnimationFrame(render)
    return () => { cancelAnimationFrame(raf); window.removeEventListener('resize', resize) }
  }, [])

  return (
    <div aria-hidden="true" className="creative-bg-root">
      <canvas ref={canvasRef} className="creative-bg-canvas" style={{opacity}} />
      <motion.div
        className="creative-bg-overlay"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 1.2, ease: [0.4,0,0.2,1] }}
      />
    </div>
  )
}
