import React, { useState, useEffect } from 'react'

const PointBalance = ({ user, token, apiBaseUrl, balance, onRefresh }) => {
  return (
    <div>
      <h3>포인트 잔액</h3>
      <div className="balance-amount">
        {balance !== null && balance !== undefined ? `${balance.toLocaleString()} P` : '0 P'}
      </div>
      <button onClick={onRefresh} className="secondary">새로고침</button>
      <div style={{ marginTop: '24px', padding: '16px', backgroundColor: 'rgba(255,255,255,0.03)', borderRadius: '8px' }}>
        <p style={{ color: 'var(--text-secondary)', fontSize: '14px', lineHeight: '1.6' }}>
          • 결제 시 결제 금액의 1%가 포인트로 적립됩니다.<br/>
          • 적립된 포인트는 다음 결제 시 현금처럼 사용 가능합니다.<br/>
          • 1 P는 1원의 가치를 가집니다.
        </p>
      </div>
    </div>
  )
}

export default PointBalance
