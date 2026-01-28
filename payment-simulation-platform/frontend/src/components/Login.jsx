import React, { useState } from 'react'

const Login = ({ onLogin, apiBaseUrl }) => {
  const [isLogin, setIsLogin] = useState(true)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      const url = isLogin 
        ? `${apiBaseUrl}/api/auth/login`
        : `${apiBaseUrl}/api/auth/register`
      
      const body = isLogin
        ? { email, password }
        : { email, password, name }

      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(body)
      })

      const data = await response.json()

      if (response.ok) {
        onLogin(
          { userId: data.userId, email: data.email, name: data.name },
          data.token
        )
      } else {
        setError(data.message || '로그인에 실패했습니다.')
      }
    } catch (err) {
      setError('서버 연결에 실패했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="container" style={{ maxWidth: '440px', marginTop: '80px' }}>
      <div className="card" style={{ padding: '40px' }}>
        <h2 style={{ textAlign: 'center', color: 'var(--accent-primary)', fontSize: '28px', marginBottom: '32px' }}>
          {isLogin ? '로그인' : '회원가입'}
        </h2>
        <form onSubmit={handleSubmit}>
          {!isLogin && (
            <>
              <label>이름</label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                placeholder="이름을 입력하세요"
              />
            </>
          )}
          <label>이메일</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            placeholder="email@example.com"
          />
          <label>비밀번호</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={6}
            placeholder="비밀번호를 입력하세요"
          />
          {error && <div className="error">{error}</div>}
          <button type="submit" disabled={loading} style={{ width: '100%', marginTop: '12px', height: '48px' }}>
            {loading ? '처리 중...' : (isLogin ? '로그인' : '회원가입')}
          </button>
        </form>
        <div style={{ textAlign: 'center', marginTop: '24px' }}>
          <span style={{ color: 'var(--text-secondary)', fontSize: '14px' }}>
            {isLogin ? '계정이 없으신가요?' : '이미 계정이 있으신가요?'}
          </span>
          <button
            type="button"
            onClick={() => {
              setIsLogin(!isLogin)
              setError('')
            }}
            className="secondary"
            style={{ background: 'transparent', color: 'var(--accent-primary)', padding: '0 8px', fontSize: '14px' }}
          >
            {isLogin ? '회원가입' : '로그인'}
          </button>
        </div>
      </div>
    </div>
  )
}

export default Login
