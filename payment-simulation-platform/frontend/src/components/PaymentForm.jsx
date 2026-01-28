import React, { useState } from 'react'

const PaymentForm = ({ user, token, apiBaseUrl, initialBalance, initialCoupons, onPaymentSuccess }) => {
  const [amount, setAmount] = useState('')
  const [orderId, setOrderId] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('CARD')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')
  const [selectedCoupon, setSelectedCoupon] = useState('')
  const [usePoint, setUsePoint] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setMessage('')
    setLoading(true)

    try {
      const response = await fetch(`${apiBaseUrl}/api/payments`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          amount: parseFloat(amount || 0),
          orderId: orderId || `ORDER-${Date.now()}`,
          paymentMethod,
          couponId: selectedCoupon ? parseInt(selectedCoupon) : null,
          usePoint: usePoint ? parseFloat(usePoint) : 0
        })
      })

      const data = await response.json()

      if (response.ok) {
        setMessage(`결제 성공! 최종 결제 금액: ${data.amount}원`)
        setAmount('')
        setOrderId('')
        setSelectedCoupon('')
        setUsePoint('')
        if (onPaymentSuccess) onPaymentSuccess()
      } else {
        setMessage(`결제 실패: ${data.message || '알 수 없는 오류'}`)
      }
    } catch (err) {
      setMessage('서버 연결에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const subtotal = parseFloat(amount || 0)
  const couponDiscount = selectedCoupon ? 1000 : 0
  const pointsUsed = parseFloat(usePoint || 0)
  const totalPayment = Math.max(0, subtotal - couponDiscount - pointsUsed)

  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 400px', gap: 'var(--space-xl)', alignItems: 'start' }}>
      <div className="card">
        <h3 style={{ marginBottom: 'var(--space-xl)', fontSize: '20px', fontWeight: '700' }}>Create Order</h3>
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 'var(--space-xl)' }}>
            <label>Amount (KRW)</label>
            <input
              type="number"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              min="0.01"
              step="0.01"
              required
              placeholder="0.00"
              style={{ fontSize: '28px', fontWeight: '800', padding: '20px', marginBottom: '0' }}
            />
          </div>

          <div style={{ marginBottom: 'var(--space-xl)' }}>
            <label>Coupon</label>
            <select
              value={selectedCoupon}
              onChange={(e) => setSelectedCoupon(e.target.value)}
              style={{ marginBottom: '0' }}
            >
              <option value="">No Coupon Available</option>
              {initialCoupons && initialCoupons.map(coupon => (
                <option key={coupon.couponId} value={coupon.couponId}>
                  {coupon.couponType} ({coupon.discountAmount ? `${coupon.discountAmount.toLocaleString()}원` : `${coupon.discountPercent}%`})
                </option>
              ))}
            </select>
          </div>

          <div style={{ marginBottom: 'var(--space-xl)' }}>
            <label>Use Points (Available: {(initialBalance || 0).toLocaleString()} P)</label>
            <div style={{ position: 'relative' }}>
              <input
                type="number"
                value={usePoint}
                onChange={(e) => setUsePoint(e.target.value)}
                max={initialBalance || 0}
                min="0"
                placeholder="0"
                style={{ marginBottom: '0', paddingRight: '80px' }}
              />
              <button 
                type="button"
                onClick={() => setUsePoint(initialBalance)}
                className="secondary"
                style={{ 
                  position: 'absolute', 
                  right: '8px', 
                  top: '50%', 
                  transform: 'translateY(-50%)',
                  padding: '6px 14px', 
                  fontSize: '12px',
                  height: '32px',
                  borderRadius: '8px'
                }}
              >
                MAX
              </button>
            </div>
          </div>

          <div style={{ marginBottom: 'var(--space-2xl)' }}>
            <label>Payment Method</label>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '12px' }}>
              {['CARD', 'BANK', 'VIRTUAL'].map(method => (
                <button
                  key={method}
                  type="button"
                  onClick={() => setPaymentMethod(method)}
                  style={{ 
                    padding: '14px 8px', 
                    fontSize: '13px',
                    borderRadius: '12px',
                    border: '1px solid',
                    borderColor: paymentMethod === method ? 'var(--accent-primary)' : 'var(--border-color)',
                    backgroundColor: paymentMethod === method ? 'rgba(240, 185, 11, 0.1)' : 'var(--input-bg)',
                    color: paymentMethod === method ? 'var(--accent-primary)' : 'var(--text-secondary)',
                    boxShadow: paymentMethod === method ? 'var(--shadow-sm)' : 'none'
                  }}
                >
                  {method}
                </button>
              ))}
            </div>
          </div>

          {message && (
            <div className={message.includes('성공') ? 'success' : 'error'}>
              {message}
            </div>
          )}
          
          <button 
            type="submit" 
            disabled={loading} 
            style={{ width: '100%', height: '60px', fontSize: '16px', fontWeight: '700' }}
          >
            {loading ? 'Processing...' : 'Confirm Order'}
          </button>
        </form>
      </div>

      <div className="card" style={{ background: 'linear-gradient(180deg, rgba(255,255,255,0.03) 0%, rgba(255,255,255,0) 100%)' }}>
        <h3 style={{ marginBottom: 'var(--space-xl)', fontSize: '18px', fontWeight: '700' }}>Order Summary</h3>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span style={{ color: 'var(--text-secondary)' }}>Item Subtotal</span>
            <span style={{ fontWeight: '600' }}>{subtotal.toLocaleString()} KRW</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span style={{ color: 'var(--text-secondary)' }}>Coupon Discount</span>
            <span style={{ color: 'var(--error-color)', fontWeight: '600' }}>- {couponDiscount.toLocaleString()} KRW</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span style={{ color: 'var(--text-secondary)' }}>Points Used</span>
            <span style={{ color: 'var(--error-color)', fontWeight: '600' }}>- {pointsUsed.toLocaleString()} KRW</span>
          </div>
          <div style={{ 
            borderTop: '1px solid var(--border-color)', 
            paddingTop: '20px', 
            marginTop: '8px', 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'center' 
          }}>
            <span style={{ color: 'var(--text-primary)', fontWeight: '700', fontSize: '16px' }}>Total Payment</span>
            <div style={{ textAlign: 'right' }}>
              <div style={{ color: 'var(--accent-primary)', fontSize: '28px', fontWeight: '900', lineHeight: '1' }}>
                {totalPayment.toLocaleString()}
              </div>
              <div style={{ color: 'var(--accent-primary)', fontSize: '12px', fontWeight: '700', marginTop: '4px' }}>KRW</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default PaymentForm
