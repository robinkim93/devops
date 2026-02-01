import React, { useState, useEffect } from 'react'
import Login from './components/Login'
import Dashboard from './components/Dashboard'
import './App.css'

const API_BASE_URL = 'http://localhost:30081'

function App() {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(localStorage.getItem('token'))

  useEffect(() => {
    if (token) {
      validateToken(token)
    }
  }, [])

  const validateToken = async (tokenToValidate) => {
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/validate`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${tokenToValidate}`
        }
      })
      const data = await response.json()
      if (data.valid) {
        setUser({ userId: data.userId, email: data.email })
        setToken(tokenToValidate)
      } else {
        localStorage.removeItem('token')
        setToken(null)
      }
    } catch (error) {
      console.error('Token validation failed:', error)
      localStorage.removeItem('token')
      setToken(null)
    }
  }

  const handleLogin = (userData, authToken) => {
    setUser(userData)
    setToken(authToken)
    localStorage.setItem('token', authToken)
  }

  const handleLogout = async () => {
    if (token) {
      try {
        await fetch(`${API_BASE_URL}/api/auth/logout`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`
          }
        })
      } catch (error) {
        console.error('Logout error:', error)
      }
    }
    setUser(null)
    setToken(null)
    localStorage.removeItem('token')
  }

  return (
    <div className="App">
      {!token ? (
        <Login onLogin={handleLogin} apiBaseUrl={API_BASE_URL} />
      ) : (
        <Dashboard 
          user={user} 
          token={token} 
          onLogout={handleLogout}
          apiBaseUrl={API_BASE_URL}
        />
      )}
    </div>
  )
}

export default App
