import React, { useState, useEffect } from 'react'
import PaymentForm from './PaymentForm'
import PaymentHistory from './PaymentHistory'
import PointBalance from './PointBalance'
import CouponList from './CouponList'

const Dashboard = ({ user, token, onLogout, apiBaseUrl }) => {
  const [activeTab, setActiveTab] = useState('payment')
  const [balance, setBalance] = useState(0)
  const [coupons, setCoupons] = useState([])
  const [loadingData, setLoadingData] = useState(false)

  const fetchCommonData = async () => {
    setLoadingData(true)
    try {
      const [balanceRes, couponsRes] = await Promise.all([
        fetch(`${apiBaseUrl}/api/points/balance/${user.userId}`),
        fetch(`${apiBaseUrl}/api/coupons/user/${user.userId}/active`)
      ])

      if (balanceRes.ok) {
        const balanceData = await balanceRes.json()
        setBalance(balanceData.balance)
      }
      if (couponsRes.ok) {
        const couponsData = await couponsRes.json()
        setCoupons(couponsData)
      }
    } catch (err) {
      console.error('Failed to fetch dashboard data:', err)
    } finally {
      setLoadingData(false)
    }
  }

  useEffect(() => {
    if (user?.userId) {
      fetchCommonData()
    }
  }, [user?.userId])

  return (
    <div className="container">
      <header className="header">
        <h1>PAYMENT PRO</h1>
        <div className="user-info">
          <div style={{ textAlign: 'right', marginRight: 'var(--space-md)' }}>
            <div style={{ color: 'var(--text-primary)', fontWeight: '600', fontSize: '15px' }}>{user?.email}</div>
            <div style={{ fontSize: '11px', color: 'var(--success-color)', fontWeight: '700', textTransform: 'uppercase' }}>Verified Pro</div>
          </div>
          <button 
            onClick={onLogout}
            className="danger"
            style={{ padding: '10px 20px', fontSize: '13px' }}
          >
            Sign Out
          </button>
        </div>
      </header>

      <main>
        <div style={{ display: 'flex', justifyContent: 'center' }}>
          <div className="tab-container">
            <button
              onClick={() => setActiveTab('payment')}
              className={`tab-button ${activeTab === 'payment' ? 'active' : ''}`}
            >
              Trade
            </button>
            <button
              onClick={() => setActiveTab('history')}
              className={`tab-button ${activeTab === 'history' ? 'active' : ''}`}
            >
              History
            </button>
            <button
              onClick={() => setActiveTab('point')}
              className={`tab-button ${activeTab === 'point' ? 'active' : ''}`}
            >
              Assets
            </button>
            <button
              onClick={() => setActiveTab('coupon')}
              className={`tab-button ${activeTab === 'coupon' ? 'active' : ''}`}
            >
              Rewards
            </button>
          </div>
        </div>

        <div className="content-area">
          {activeTab === 'payment' && (
            <PaymentForm 
              user={user} 
              token={token} 
              apiBaseUrl={apiBaseUrl} 
              initialBalance={balance}
              initialCoupons={coupons}
              onPaymentSuccess={fetchCommonData}
            />
          )}
          {activeTab === 'history' && (
            <div className="card">
              <PaymentHistory user={user} token={token} apiBaseUrl={apiBaseUrl} />
            </div>
          )}
          {activeTab === 'point' && (
            <div className="card balance-card">
              <PointBalance 
                user={user} 
                token={token} 
                apiBaseUrl={apiBaseUrl} 
                balance={balance}
                onRefresh={fetchCommonData}
              />
            </div>
          )}
          {activeTab === 'coupon' && (
            <div className="card">
              <CouponList 
                user={user} 
                token={token} 
                apiBaseUrl={apiBaseUrl} 
                initialCoupons={coupons}
                onRefresh={fetchCommonData}
              />
            </div>
          )}
        </div>
      </main>
    </div>
  )
}

export default Dashboard
