# 📅 Day 77: JWT Authentication

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "보안 컴플라이언스를 고려한 인프라 설계"
> JWT 기반 인증으로 API 보안 강화

토스플레이스는 금융 서비스로서 API 인증/인가가 매우 중요합니다. Istio를 활용한 JWT 인증은 애플리케이션 코드 변경 없이 보안을 적용할 수 있습니다.

---

## ⏰ 예상 학습 시간: 4시간

---

## 📚 Part 1: JWT 인증 개념 (1시간)

### 1.1 JWT란?

JWT(JSON Web Token)는 JSON 형태의 클레임을 안전하게 전달하기 위한 표준(RFC 7519)입니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│  JWT 구조                                                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  JWT = Header.Payload.Signature                                     │
│                                                                      │
│  eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.                              │
│  eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRy...│
│  TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ                        │
│  └─ Header ─┘ └───────── Payload ───────────┘ └──── Signature ────┘ │
│                                                                      │
│  Header (Base64):                                                   │
│  {                                                                  │
│    "alg": "RS256",        // 서명 알고리즘                          │
│    "typ": "JWT",          // 토큰 타입                              │
│    "kid": "key-id-123"    // 키 ID (JWKS에서 찾기 위함)             │
│  }                                                                  │
│                                                                      │
│  Payload (Base64):                                                  │
│  {                                                                  │
│    "iss": "https://auth.tossplace.com",  // 발행자                  │
│    "sub": "user123",                      // 주체 (사용자 ID)       │
│    "aud": "api.tossplace.com",           // 대상                    │
│    "exp": 1735689600,                     // 만료 시간              │
│    "iat": 1735603200,                     // 발행 시간              │
│    "role": "admin",                       // 커스텀 클레임          │
│    "permissions": ["read", "write"]       // 권한                   │
│  }                                                                  │
│                                                                      │
│  Signature:                                                         │
│  HMACSHA256(                                                        │
│    base64UrlEncode(header) + "." + base64UrlEncode(payload),       │
│    secret                                                           │
│  )                                                                  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 Istio JWT 인증 흐름

```
┌─────────────────────────────────────────────────────────────────────┐
│  Istio JWT 인증 아키텍처                                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Client                                                             │
│    │                                                                │
│    │  Authorization: Bearer <JWT>                                   │
│    ▼                                                                │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  Istio Ingress Gateway                                        │   │
│  └──────────────────────────────────────────────────────────────┘   │
│    │                                                                │
│    ▼                                                                │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  RequestAuthentication                                        │   │
│  │  1. JWT 헤더에서 토큰 추출                                    │   │
│  │  2. JWKS URI에서 공개키 가져오기                              │   │
│  │  3. 서명 검증                                                 │   │
│  │  4. 만료 시간, issuer, audience 검증                         │   │
│  │  5. 검증 실패 시 401 Unauthorized                            │   │
│  └──────────────────────────────────────────────────────────────┘   │
│    │                                                                │
│    ▼                                                                │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  AuthorizationPolicy                                          │   │
│  │  1. JWT 클레임 기반 접근 제어                                 │   │
│  │  2. role, permissions 확인                                    │   │
│  │  3. 권한 없으면 403 Forbidden                                │   │
│  └──────────────────────────────────────────────────────────────┘   │
│    │                                                                │
│    ▼                                                                │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  Service                                                      │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.3 JWKS (JSON Web Key Set)

```json
// JWKS URI 응답 예시 (https://auth.example.com/.well-known/jwks.json)
{
  "keys": [
    {
      "kty": "RSA",           // 키 타입
      "kid": "key-id-123",    // 키 ID
      "use": "sig",           // 사용 목적 (서명)
      "alg": "RS256",         // 알고리즘
      "n": "0vx7agoebGcQ...",  // RSA modulus (Base64)
      "e": "AQAB"             // RSA exponent (Base64)
    }
  ]
}
```

---

## 🛠️ Part 2: RequestAuthentication 실습 (1.5시간)

### 실습 1: httpbin 테스트 앱 배포

```bash
# httpbin 배포 (기존에 없다면)
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/httpbin/httpbin.yaml

# sleep 클라이언트 배포
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/sleep/sleep.yaml

# Pod 확인
kubectl get pods -l app=httpbin
kubectl get pods -l app=sleep
```

### 실습 2: 기본 RequestAuthentication 설정

```yaml
# jwt-request-auth.yaml
apiVersion: security.istio.io/v1beta1
kind: RequestAuthentication
metadata:
  name: jwt-auth
  namespace: default
spec:
  selector:
    matchLabels:
      app: httpbin
  jwtRules:
  - issuer: "testing@secure.istio.io"
    # Istio 샘플 JWKS (테스트용)
    jwksUri: "https://raw.githubusercontent.com/istio/istio/release-1.20/security/tools/jwt/samples/jwks.json"
    # JWT 토큰 위치 (기본: Authorization 헤더)
    # fromHeaders:
    # - name: Authorization
    #   prefix: "Bearer "
    # 원본 토큰을 서비스로 전달
    forwardOriginalToken: true
    # 출력할 클레임 헤더
    outputClaimToHeaders:
    - header: "x-jwt-claim-sub"
      claim: "sub"
    - header: "x-jwt-claim-iss"
      claim: "iss"
```

```bash
# RequestAuthentication 적용
kubectl apply -f jwt-request-auth.yaml

# 확인
kubectl get requestauthentication
```

### 실습 3: JWT 없이 요청 테스트

```bash
# JWT 없이 요청 - 성공 (RequestAuthentication만으로는 강제가 아님!)
kubectl exec -it $(kubectl get pod -l app=sleep -o jsonpath={.items..metadata.name}) \
  -c sleep -- curl -s httpbin:8000/headers | grep -i authorization

# 예상: 응답 성공, Authorization 헤더 없음
```

### 실습 4: 잘못된 JWT로 요청

```bash
# 잘못된 JWT로 요청 - 실패 (401)
kubectl exec -it $(kubectl get pod -l app=sleep -o jsonpath={.items..metadata.name}) \
  -c sleep -- curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer invalid.token.here" httpbin:8000/headers

# 예상: 401
```

### 실습 5: 유효한 JWT로 요청

```bash
# Istio 샘플 JWT 토큰 설정
# 이 토큰은 테스트 JWKS와 매칭되는 유효한 토큰입니다
export TOKEN="eyJhbGciOiJSUzI1NiIsImtpZCI6IkRIRmJwb0lVcXJZOHQyenBBMnFYZkNtcjVWTzVaRXI0UnpIVV8tZW52dlEiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjQ2ODU5ODk3MDAsImZvbyI6ImJhciIsImlhdCI6MTUzMjM4OTcwMCwiaXNzIjoidGVzdGluZ0BzZWN1cmUuaXN0aW8uaW8iLCJzdWIiOiJ0ZXN0aW5nQHNlY3VyZS5pc3Rpby5pbyJ9.CfNnxWP2tcnR9q0vxyxweaF3ovQYHYZl82hAUsn21bwQd9zP7c-LS9qd_vpdLG4Tn1A15NxfCjp5f7QNBUo-KC9PJqYpgGbaXhaGx7bEdFWjcwv3nZzvc7M__ZpaCERdwU7igUmJqYGBYQ51vr2njU9ZimyKkfDe3axcyiBZde7G6dabliUosJvvKOPcKIWPccCgefSj_GNfwIip3-SsFdlR7BtbVUcqR-yv-XOxJ3Uc1MI0tz3uMiiZcyPV7sNCU4KRnemRIMHVOfuvHsU60_GhGbiSFzgPTAa9WTltbnarTbxudb_YEOx12JiwYToeX0DCPb43W1tzIBxgm8NxUg"

# 유효한 JWT로 요청 - 성공
kubectl exec -it $(kubectl get pod -l app=sleep -o jsonpath={.items..metadata.name}) \
  -c sleep -- curl -s httpbin:8000/headers \
  -H "Authorization: Bearer $TOKEN" | jq .

# 예상: 200 OK, 헤더에 JWT 정보 포함
```

---

## 🛠️ Part 3: AuthorizationPolicy로 JWT 필수화 (1시간)

### 실습 6: JWT 필수화

```yaml
# jwt-required.yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: require-jwt
  namespace: default
spec:
  selector:
    matchLabels:
      app: httpbin
  action: DENY
  rules:
  - from:
    - source:
        notRequestPrincipals: ["*"]  # JWT가 없으면 거부
```

```bash
# AuthorizationPolicy 적용
kubectl apply -f jwt-required.yaml

# JWT 없이 요청 - 실패 (403)
kubectl exec -it $(kubectl get pod -l app=sleep -o jsonpath={.items..metadata.name}) \
  -c sleep -- curl -s -o /dev/null -w "%{http_code}" httpbin:8000/headers

# 예상: 403 Forbidden
```

### 실습 7: JWT 클레임 기반 권한

```yaml
# jwt-claims-policy.yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: require-specific-claim
  namespace: default
spec:
  selector:
    matchLabels:
      app: httpbin
  action: ALLOW
  rules:
  - from:
    - source:
        # 특정 issuer의 JWT만 허용
        requestPrincipals: ["testing@secure.istio.io/*"]
    when:
    # 추가 클레임 조건 (예: role=admin)
    # - key: request.auth.claims[role]
    #   values: ["admin"]
```

```bash
# 기존 정책 삭제
kubectl delete authorizationpolicy require-jwt

# 새 정책 적용
kubectl apply -f jwt-claims-policy.yaml

# JWT로 요청 - 성공
kubectl exec -it $(kubectl get pod -l app=sleep -o jsonpath={.items..metadata.name}) \
  -c sleep -- curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $TOKEN" httpbin:8000/headers

# 예상: 200
```

### 실습 8: 경로별 다른 권한

```yaml
# jwt-path-based.yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: path-based-jwt
  namespace: default
spec:
  selector:
    matchLabels:
      app: httpbin
  action: ALLOW
  rules:
  # /health는 JWT 없이 허용
  - to:
    - operation:
        paths: ["/health", "/ready"]
  # 그 외는 JWT 필수
  - from:
    - source:
        requestPrincipals: ["*"]
```

```bash
# 정책 적용
kubectl delete authorizationpolicy require-specific-claim
kubectl apply -f jwt-path-based.yaml

# /health는 JWT 없이 접근 가능
kubectl exec -it $(kubectl get pod -l app=sleep -o jsonpath={.items..metadata.name}) \
  -c sleep -- curl -s -o /dev/null -w "%{http_code}" httpbin:8000/health

# 예상: 200

# /headers는 JWT 필요
kubectl exec -it $(kubectl get pod -l app=sleep -o jsonpath={.items..metadata.name}) \
  -c sleep -- curl -s -o /dev/null -w "%{http_code}" httpbin:8000/headers

# 예상: 403
```

---

## 📊 Part 4: 실무 시나리오 (30분)

### 4.1 프로덕션 JWT 설정 예시

```yaml
# production-jwt-auth.yaml
apiVersion: security.istio.io/v1beta1
kind: RequestAuthentication
metadata:
  name: tossplace-jwt-auth
  namespace: payment
spec:
  selector:
    matchLabels:
      app: payment-api
  jwtRules:
  - issuer: "https://auth.tossplace.com"
    jwksUri: "https://auth.tossplace.com/.well-known/jwks.json"
    # 여러 audience 허용
    audiences:
    - "api.tossplace.com"
    - "payment.tossplace.com"
    # JWKS 캐싱 시간 (기본 20분)
    # jwksCacheConfig:
    #   duration: 30m
    forwardOriginalToken: true
    outputClaimToHeaders:
    - header: "x-user-id"
      claim: "sub"
    - header: "x-user-role"
      claim: "role"
---
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: tossplace-jwt-policy
  namespace: payment
spec:
  selector:
    matchLabels:
      app: payment-api
  action: ALLOW
  rules:
  # 헬스체크는 인증 없이
  - to:
    - operation:
        paths: ["/health", "/ready", "/metrics"]
  # 결제 API는 admin 또는 payment role 필요
  - from:
    - source:
        requestPrincipals: ["https://auth.tossplace.com/*"]
    to:
    - operation:
        paths: ["/api/payment/*"]
        methods: ["POST"]
    when:
    - key: request.auth.claims[role]
      values: ["admin", "payment-service"]
  # 조회 API는 일반 인증만
  - from:
    - source:
        requestPrincipals: ["https://auth.tossplace.com/*"]
    to:
    - operation:
        paths: ["/api/transactions/*"]
        methods: ["GET"]
```

### 4.2 정리

```bash
# 테스트 리소스 정리
kubectl delete requestauthentication jwt-auth
kubectl delete authorizationpolicy --all
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | JWT 구조 이해 | ☐ |
| 2 | RequestAuthentication 설정 | ☐ |
| 3 | 유효한/무효한 JWT 테스트 | ☐ |
| 4 | AuthorizationPolicy로 JWT 필수화 | ☐ |
| 5 | JWT 클레임 기반 권한 | ☐ |
| 6 | 경로별 다른 권한 설정 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# RequestAuthentication 적용
kubectl apply -f jwt-request-auth.yaml

# AuthorizationPolicy 적용
kubectl apply -f jwt-required.yaml

# JWT로 요청 테스트
curl -H "Authorization: Bearer $TOKEN" http://service/

# 정책 확인
kubectl get requestauthentication,authorizationpolicy
```

---

## 💡 면접 대비 핵심 포인트

### Q1: RequestAuthentication과 AuthorizationPolicy의 차이?
**A**: 
- **RequestAuthentication**: JWT 토큰을 검증합니다. 잘못된 토큰은 거부하지만, 토큰이 없으면 통과합니다.
- **AuthorizationPolicy**: 접근 권한을 제어합니다. JWT 필수화, 클레임 기반 권한 등을 설정합니다.

### Q2: JWKS란?
**A**: JSON Web Key Set의 약자로, JWT 서명을 검증하기 위한 공개키 세트입니다. Istio는 JWKS URI에서 공개키를 가져와 JWT 서명을 검증합니다.

### Q3: JWT 인증의 장점은?
**A**: 
- 애플리케이션 코드 변경 없이 인증 적용
- 중앙에서 일관된 인증 정책 관리
- Stateless (서버 세션 불필요)
- 클레임 기반 세밀한 권한 제어

---

## ➡️ 다음 학습: Day 78

**주제**: Egress 트래픽 제어
- ServiceEntry로 외부 서비스 등록
- Egress Gateway 설정
