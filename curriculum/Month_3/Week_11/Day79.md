# 📅 Day 79: Security 종합 실습

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "보안 컴플라이언스를 고려한 인프라 설계"
> Zero Trust 보안 모델을 Service Mesh에 구현

토스플레이스는 금융 서비스로서 높은 보안 수준이 요구됩니다. Zero Trust는 "절대 신뢰하지 않고 항상 검증"하는 보안 원칙입니다.

---

## ⏰ 예상 학습 시간: 4시간

---

## 📚 Part 1: Zero Trust 개념 (1시간)

### 1.1 Zero Trust란?

Zero Trust는 네트워크 내부든 외부든 모든 접근을 기본적으로 불신하고 검증하는 보안 모델입니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│  기존 보안 모델 vs Zero Trust                                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  기존 모델 (Castle-and-Moat):                                       │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  방화벽 (Moat)                                                │  │
│  │  ┌─────────────────────────────────────────────────────────┐  │  │
│  │  │  내부 네트워크 (Castle)                                  │  │  │
│  │  │  → 내부는 신뢰, 외부만 차단                              │  │  │
│  │  │  → 내부 침해 시 전체 노출                                │  │  │
│  │  └─────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  Zero Trust:                                                        │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  [Service A] ←── 인증/암호화 ──→ [Service B]                 │  │
│  │       │                               │                       │  │
│  │       └──── 인증/암호화 ──────────────┘                       │  │
│  │                    │                                          │  │
│  │              [Service C]                                      │  │
│  │                                                               │  │
│  │  → 모든 통신을 검증하고 암호화                               │  │
│  │  → 최소 권한 원칙 (필요한 접근만 허용)                       │  │
│  │  → 마이크로세그멘테이션                                      │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 Zero Trust 핵심 원칙

| 원칙 | 설명 | Istio 구현 |
|------|------|-----------|
| **항상 검증** | 모든 요청을 인증/인가 | mTLS, JWT 인증 |
| **최소 권한** | 필요한 접근만 허용 | AuthorizationPolicy |
| **세분화** | 서비스 단위 접근 제어 | DENY ALL + 선택적 ALLOW |
| **암호화** | 모든 통신 암호화 | mTLS STRICT 모드 |
| **모니터링** | 모든 접근 로깅 | Access Log, 감사 로그 |

### 1.3 Istio Security 구성요소

```
┌─────────────────────────────────────────────────────────────────────┐
│  Istio Security 아키텍처                                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  PeerAuthentication (전송 계층 보안)                        │    │
│  │  - mTLS 모드 설정 (STRICT, PERMISSIVE, DISABLE)            │    │
│  │  - 서비스 간 상호 인증                                      │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                         │                                           │
│                         ▼                                           │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  RequestAuthentication (요청 계층 보안)                     │    │
│  │  - JWT 토큰 검증                                            │    │
│  │  - 외부 사용자 인증                                         │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                         │                                           │
│                         ▼                                           │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  AuthorizationPolicy (인가 정책)                            │    │
│  │  - ALLOW: 특정 조건 허용                                    │    │
│  │  - DENY: 특정 조건 차단                                     │    │
│  │  - CUSTOM: 외부 인가 서버                                   │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  보안 처리 순서:                                                    │
│  1. mTLS 연결 검증 (PeerAuthentication)                            │
│  2. JWT 토큰 검증 (RequestAuthentication)                          │
│  3. 인가 정책 검사 (AuthorizationPolicy)                           │
│  4. 모든 통과 시에만 요청 처리                                     │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: Zero Trust 시나리오 구현 (2시간)

### 시나리오 설명

```
┌─────────────────────────────────────────────────────────────────────┐
│  3-Tier 앱에 Zero Trust 적용                                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  외부 → Gateway → Frontend → Backend → Database                     │
│                                                                      │
│  보안 요구사항:                                                     │
│  1. 모든 통신 mTLS (암호화 + 상호 인증)                            │
│  2. Frontend → Backend만 허용                                       │
│  3. Backend → Database만 허용                                       │
│  4. 외부 → Frontend만 허용 (Gateway 통해)                           │
│  5. 직접 접근 모두 차단                                             │
│                                                                      │
│  예상 결과:                                                         │
│  ✓ 외부 → Gateway → Frontend : 허용                                │
│  ✓ Frontend → Backend : 허용                                        │
│  ✓ Backend → Database : 허용                                        │
│  ✗ 외부 → Backend 직접 : 차단                                       │
│  ✗ Frontend → Database 직접 : 차단                                  │
│  ✗ Database → Backend : 차단                                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### Step 1: 테스트 환경 준비

```bash
# Namespace 생성
kubectl create namespace production

# Istio sidecar 주입 활성화
kubectl label namespace production istio-injection=enabled

# 테스트 앱 배포
kubectl apply -n production -f - <<EOF
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: frontend
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: backend
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: database
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
spec:
  replicas: 1
  selector:
    matchLabels:
      app: frontend
  template:
    metadata:
      labels:
        app: frontend
    spec:
      serviceAccountName: frontend
      containers:
      - name: frontend
        image: curlimages/curl
        command: ["sleep", "infinity"]
---
apiVersion: v1
kind: Service
metadata:
  name: frontend
spec:
  selector:
    app: frontend
  ports:
  - port: 80
    targetPort: 80
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
spec:
  replicas: 1
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
    spec:
      serviceAccountName: backend
      containers:
      - name: backend
        image: hashicorp/http-echo
        args: ["-text=Backend Response"]
        ports:
        - containerPort: 5678
---
apiVersion: v1
kind: Service
metadata:
  name: backend
spec:
  selector:
    app: backend
  ports:
  - port: 8080
    targetPort: 5678
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: database
spec:
  replicas: 1
  selector:
    matchLabels:
      app: database
  template:
    metadata:
      labels:
        app: database
    spec:
      serviceAccountName: database
      containers:
      - name: database
        image: hashicorp/http-echo
        args: ["-text=Database Response"]
        ports:
        - containerPort: 5678
---
apiVersion: v1
kind: Service
metadata:
  name: database
spec:
  selector:
    app: database
  ports:
  - port: 3306
    targetPort: 5678
EOF

# Pod 확인
kubectl get pods -n production
```

### Step 2: mTLS STRICT 모드 적용

```yaml
# mtls-strict.yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: strict-mtls
  namespace: production
spec:
  mtls:
    mode: STRICT   # mTLS 강제 (평문 통신 차단)
```

```bash
# mTLS 적용
kubectl apply -f mtls-strict.yaml

# mTLS 상태 확인
kubectl get peerauthentication -n production

# mTLS 연결 테스트 (sidecar 없는 Pod에서 접근 시 실패해야 함)
# kubectl run test --image=curlimages/curl --rm -it -- curl backend.production:8080
# 결과: 연결 실패 (mTLS 없음)
```

### Step 3: DENY ALL 기본 정책

```yaml
# deny-all.yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: deny-all
  namespace: production
spec: {}    # rules가 없으면 모든 요청 차단
```

```bash
# DENY ALL 적용
kubectl apply -f deny-all.yaml

# 모든 접근 차단 확인
kubectl exec -n production deploy/frontend -- curl -s backend:8080
# 결과: RBAC: access denied
```

### Step 4: Ingress Gateway → Frontend 허용

```yaml
# allow-ingress-to-frontend.yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-ingress-to-frontend
  namespace: production
spec:
  selector:
    matchLabels:
      app: frontend
  action: ALLOW
  rules:
  - from:
    - source:
        # Istio Ingress Gateway의 Service Account
        principals: ["cluster.local/ns/istio-system/sa/istio-ingressgateway-service-account"]
```

```bash
# 정책 적용
kubectl apply -f allow-ingress-to-frontend.yaml
```

### Step 5: Frontend → Backend 허용

```yaml
# allow-frontend-to-backend.yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-frontend-to-backend
  namespace: production
spec:
  selector:
    matchLabels:
      app: backend
  action: ALLOW
  rules:
  - from:
    - source:
        # Frontend의 Service Account
        principals: ["cluster.local/ns/production/sa/frontend"]
    to:
    - operation:
        methods: ["GET", "POST"]    # 특정 HTTP 메소드만 허용
        paths: ["/api/*", "/health"]  # 특정 경로만 허용
```

```bash
# 정책 적용
kubectl apply -f allow-frontend-to-backend.yaml

# Frontend → Backend 테스트 (성공해야 함)
kubectl exec -n production deploy/frontend -- curl -s backend:8080
# 결과: Backend Response
```

### Step 6: Backend → Database 허용

```yaml
# allow-backend-to-db.yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-backend-to-db
  namespace: production
spec:
  selector:
    matchLabels:
      app: database
  action: ALLOW
  rules:
  - from:
    - source:
        # Backend의 Service Account
        principals: ["cluster.local/ns/production/sa/backend"]
    to:
    - operation:
        ports: ["3306"]   # Database 포트만 허용
```

```bash
# 정책 적용
kubectl apply -f allow-backend-to-db.yaml

# Backend → Database 테스트 (성공해야 함)
kubectl exec -n production deploy/backend -- curl -s database:3306
# 결과: Database Response
```

### Step 7: 종합 테스트

```bash
echo "=== Zero Trust 테스트 ==="

# 허용된 접근
echo "1. Frontend → Backend (허용)"
kubectl exec -n production deploy/frontend -- curl -s backend:8080 --max-time 5
echo ""

echo "2. Backend → Database (허용)"
kubectl exec -n production deploy/backend -- curl -s database:3306 --max-time 5
echo ""

# 차단된 접근
echo "3. Frontend → Database (차단)"
kubectl exec -n production deploy/frontend -- curl -s database:3306 --max-time 5 2>&1 || echo "RBAC: access denied"
echo ""

echo "4. Database → Backend (차단)"
kubectl exec -n production deploy/database -- curl -s backend:8080 --max-time 5 2>&1 || echo "RBAC: access denied"
echo ""

echo "5. Database → Frontend (차단)"
kubectl exec -n production deploy/database -- curl -s frontend:80 --max-time 5 2>&1 || echo "RBAC: access denied"
```

---

## 📊 Part 3: 고급 보안 설정 (30분)

### 3.1 특정 IP 차단

```yaml
# deny-specific-ip.yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: deny-specific-ip
  namespace: production
spec:
  selector:
    matchLabels:
      app: frontend
  action: DENY
  rules:
  - from:
    - source:
        ipBlocks: ["192.168.1.0/24"]  # 특정 IP 대역 차단
```

### 3.2 JWT 인증 추가

```yaml
# jwt-authentication.yaml
apiVersion: security.istio.io/v1beta1
kind: RequestAuthentication
metadata:
  name: jwt-auth
  namespace: production
spec:
  selector:
    matchLabels:
      app: frontend
  jwtRules:
  - issuer: "https://auth.tossplace.com"
    jwksUri: "https://auth.tossplace.com/.well-known/jwks.json"
    audiences:
    - "api.tossplace.com"
    forwardOriginalToken: true
---
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: require-jwt
  namespace: production
spec:
  selector:
    matchLabels:
      app: frontend
  action: ALLOW
  rules:
  - from:
    - source:
        requestPrincipals: ["https://auth.tossplace.com/*"]  # JWT 필수
```

### 3.3 감사 로깅

```yaml
# audit-logging.yaml
apiVersion: telemetry.istio.io/v1alpha1
kind: Telemetry
metadata:
  name: audit-logging
  namespace: production
spec:
  accessLogging:
  - providers:
    - name: envoy
    filter:
      expression: "response.code >= 400 || connection.mtls == false"
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | Zero Trust 개념 이해 | ☐ |
| 2 | mTLS STRICT 모드 적용 | ☐ |
| 3 | DENY ALL 기본 정책 적용 | ☐ |
| 4 | Frontend → Backend 허용 | ☐ |
| 5 | Backend → Database 허용 | ☐ |
| 6 | 종합 테스트 통과 | ☐ |
| 7 | 고급 보안 설정 이해 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# mTLS 상태 확인
kubectl get peerauthentication -A

# AuthorizationPolicy 확인
kubectl get authorizationpolicy -n production

# 보안 테스트
kubectl exec -n production deploy/frontend -- curl -s backend:8080
```

---

## 💡 면접 대비 핵심 포인트

### Q1: Zero Trust란?
**A**: "절대 신뢰하지 않고 항상 검증"하는 보안 모델입니다. 내부/외부 구분 없이 모든 접근을 인증하고, 최소 권한만 부여합니다.

### Q2: Istio에서 Zero Trust 구현 방법은?
**A**: 
1. PeerAuthentication으로 mTLS 강제 (STRICT)
2. AuthorizationPolicy로 DENY ALL 후 필요한 접근만 ALLOW
3. Service Account 기반 세분화된 접근 제어

### Q3: mTLS STRICT vs PERMISSIVE?
**A**:
- **STRICT**: mTLS만 허용, 평문 통신 차단
- **PERMISSIVE**: mTLS와 평문 모두 허용 (마이그레이션용)

---

## ➡️ 다음 학습: Day 80

**주제**: Rate Limiting
- Envoy Rate Limit 설정
- 트래픽 제한으로 서비스 보호
