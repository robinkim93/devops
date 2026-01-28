import React, { useState, useEffect } from 'react'

const CouponList = ({ user, token, apiBaseUrl, initialCoupons, onRefresh }) => {
  const [coupons, setCoupons] = useState(initialCoupons || [])
  const [loading, setLoading] = useState(false)
  const [showActiveOnly, setShowActiveOnly] = useState(true)

  useEffect(() => {
    if (showActiveOnly) {
      setCoupons(initialCoupons || [])
    } else {
      fetchCoupons()
    }
  }, [showActiveOnly, initialCoupons])

  const fetchCoupons = async () => {
    setLoading(true)
    try {
      const url = showActiveOnly
        ? `${apiBaseUrl}/api/coupons/user/${user.userId}/active`
        : `${apiBaseUrl}/api/coupons/user/${user.userId}`
      
      const response = await fetch(url)

      if (response.ok) {
        const data = await response.json()
        setCoupons(data)
      }
    } catch (err) {
      console.error('Failed to fetch coupons:', err)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <h3>쿠폰 목록</h3>
        <button onClick={onRefresh} className="secondary" style={{ padding: '8px 16px' }}>새로고침</button>
      </div>
      
      <div style={{ marginBottom: '24px' }}>
        <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }}>
          <input
            type="checkbox"
            checked={showActiveOnly}
            onChange={(e) => setShowActiveOnly(e.target.checked)}
            style={{ width: '18px', height: '18px', margin: '0 10px 0 0' }}
          />
          <span style={{ color: 'var(--text-primary)', fontSize: '14px' }}>사용 가능한 쿠폰만 보기</span>
        </label>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-secondary)' }}>로딩 중...</div>
      ) : coupons.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-secondary)', border: '1px dashed var(--border-color)', borderRadius: '12px' }}>
          보유하신 쿠폰이 없습니다.
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '16px' }}>
          {coupons.map((coupon) => (
            <div key={coupon.couponId} className="coupon-item">
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px' }}>
                <span style={{ color: 'var(--accent-primary)', fontWeight: '700', fontSize: '12px', letterSpacing: '1px' }}>
                  {coupon.couponType}
                </span>
                <span style={{ 
                  fontSize: '11px', 
                  padding: '2px 8px', 
                  borderRadius: '4px', 
                  backgroundColor: coupon.status === 'ACTIVE' ? 'rgba(14, 203, 129, 0.1)' : 'rgba(255,255,255,0.05)',
                  color: coupon.status === 'ACTIVE' ? 'var(--success-color)' : 'var(--text-secondary)'
                }}>
                  {coupon.status}
                </span>
              </div>
              
              <div style={{ fontSize: '24px', fontWeight: '700', marginBottom: '8px' }}>
                {coupon.discountAmount ? `${coupon.discountAmount.toLocaleString()}원` : `${coupon.discountPercent}%`} 할인
              </div>
              
              <div style={{ color: 'var(--text-secondary)', fontSize: '13px', marginBottom: '16px' }}>
                {coupon.reason}
              </div>
              
              <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: '12px', color: 'var(--text-secondary)', fontSize: '12px' }}>
                만료일: {new Date(coupon.expiresAt).toLocaleDateString('ko-KR')}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export default CouponList
