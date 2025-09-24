import React from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { isAuthed } from '../services/auth.js'

export default function ProtectedRoute({ children }) {
  const location = useLocation()
  if (!isAuthed()) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }
  return children
}
