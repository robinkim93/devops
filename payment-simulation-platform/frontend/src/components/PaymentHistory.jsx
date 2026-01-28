import React, { useState, useEffect } from 'react'

const PaymentHistory = ({ user, token, apiBaseUrl }) => {
  const [payments, setPayments] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchPayments()
  }, [])

  const fetchPayments = async () => {
    try {
      const response = await fetch(`${apiBaseUrl}/api/payments/user`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })

      if (response.ok) {
        const data = await response.json()
        setPayments(data)
      }
    } catch (err) {
      console.error('Failed to fetch payments:', err)
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return <div>로딩 중...</div>
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <h3>결제 내역</h3>
        <button onClick={fetchPayments} className="secondary" style={{ padding: '8px 16px' }}>새로고침</button>
      </div>
      
      {payments.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '40px', color: 'var(--text-secondary)', border: '1px dashed var(--border-color)', borderRadius: '12px' }}>
          결제 내역이 없습니다.
        </div>
      ) : (
        <div style={{ overflowX: 'auto' }}>
          <table>
            <thead>
              <tr>
                <th>날짜</th>
                <th>주문 ID</th>
                <th>결제 금액</th>
                <th>결제 수단</th>
                <th>상태</th>
              </tr>
            </thead>
            <tbody>
              {payments.map((payment) => (
                <tr key={payment.id}>
                  <td style={{ color: 'var(--text-secondary)' }}>
                    {new Date(payment.createdAt).toLocaleString('ko-KR')}
                  </td>
                  <td style={{ fontWeight: '500' }}>{payment.orderId}</td>
                  <td style={{ color: 'var(--accent-primary)', fontWeight: '700' }}>
                    {payment.amount.toLocaleString()}원
                  </td>
                  <td>
                    <span style={{ fontSize: '12px', padding: '4px 8px', backgroundColor: 'rgba(255,255,255,0.05)', borderRadius: '4px' }}>
                      {payment.paymentMethod}
                    </span>
                  </td>
                  <td>
                    <span style={{ 
                      color: payment.status === 'COMPLETED' ? 'var(--success-color)' : 'var(--error-color)',
                      fontWeight: '600'
                    }}>
                      {payment.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

export default PaymentHistory
