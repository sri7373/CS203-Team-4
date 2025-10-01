import React from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { isAuthed, isAdmin } from '../services/auth.js'

export default function AdminRoute({ children }) {
  const location = useLocation()
  if (!isAuthed()) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }
  if (!isAdmin()) {
    return <Navigate to="/" replace />
  }
  return children
}
