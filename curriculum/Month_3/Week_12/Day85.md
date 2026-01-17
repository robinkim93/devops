# 📅 Day 85: Istio 보안 - mTLS & AuthorizationPolicy

## 🎯 오늘의 목표

> **토스플레이스 핵심**: Zero Trust 보안 모델을 Istio로 구현하여 서비스 간 통신을 보호합니다.

---

## ⏰ 예상 소요 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| mTLS 개념 | 1시간 | 암호화 통신 |
| PeerAuthentication | 1시간 | mTLS 정책 |
| AuthorizationPolicy | 1시간 30분 | 접근 제어 |
| 통합 테스트 | 30분 | 보안 검증 |

---

## 📚 Part 1: mTLS 개념

### mTLS (Mutual TLS)란?

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         mTLS (Mutual TLS)                                    │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   일반 TLS:                                                                  │
│   Client ──────────────────▶ Server                                         │
│          (서버 인증서만 검증)                                                 │
│                                                                              │
│   mTLS:                                                                      │
│   Client ◀─────────────────▶ Server                                         │
│          (양쪽 인증서 상호 검증)                                              │
│                                                                              │
│   ┌────────────┐              ┌────────────┐                                │
│   │ Service A  │◀───mTLS────▶│ Service B  │                                │
│   │ (Envoy)    │   암호화     │ (Envoy)    │                                │
│   │ 인증서: A  │   상호인증   │ 인증서: B  │                                │
│   └────────────┘              └────────────┘                                │
│                                                                              │
│   Istio가 자동으로:                                                          │
│   - 인증서 발급 (citadel)                                                    │
│   - 인증서 갱신                                                              │
│   - 암호화 통신 설정                                                         │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### mTLS 모드

| 모드 | 설명 | 용도 |
|------|------|------|
| **PERMISSIVE** | mTLS와 평문 모두 허용 | 마이그레이션 |
| **STRICT** | mTLS만 허용 | 프로덕션 권장 |
| **DISABLE** | mTLS 비활성화 | 특수 케이스 |

---

## 🛠️ Part 2: PeerAuthentication

### 네임스페이스 전체 STRICT mTLS

```yaml
# peerauthentication-strict.yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: strict-mtls
  namespace: istio-portfolio  # 특정 네임스페이스
spec:
  mtls:
    mode: STRICT
```

### 메시 전체 STRICT mTLS

```yaml
# mesh-strict-mtls.yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: istio-system  # istio-system = 메시 전체
spec:
  mtls:
    mode: STRICT
```

### 워크로드별 설정

```yaml
# 특정 워크로드만 예외
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: api-mtls
  namespace: istio-portfolio
spec:
  selector:
    matchLabels:
      app: api
  mtls:
    mode: STRICT
  portLevelMtls:
    8080:
      mode: STRICT
    9090:
      mode: PERMISSIVE  # 메트릭 포트는 PERMISSIVE
```

### mTLS 상태 확인

```bash
# mTLS 상태 확인
istioctl x authz check <pod-name> -n istio-portfolio

# 또는
kubectl get peerauthentication -n istio-portfolio
kubectl get peerauthentication -A

# Kiali에서 확인
# - 자물쇠 아이콘 = mTLS 활성화
```

---

## 🛠️ Part 3: AuthorizationPolicy

### Zero Trust 접근 제어

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     AuthorizationPolicy 동작                                  │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   1. DENY ALL (기본 차단)                                                    │
│      ┌──────┐     ✗     ┌──────┐                                           │
│      │  A   │ ─────────▶│  B   │  모든 트래픽 차단                           │
│      └──────┘           └──────┘                                            │
│                                                                              │
│   2. ALLOW 정책 추가                                                         │
│      ┌──────┐     ✓     ┌──────┐                                           │
│      │  A   │ ─────────▶│  B   │  명시적으로 허용된 트래픽만 통과            │
│      └──────┘           └──────┘                                            │
│                                                                              │
│   3. DENY 정책 (세부 차단)                                                   │
│      특정 경로, IP, 헤더 등을 명시적으로 차단                                  │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### DENY ALL (기본 차단)

```yaml
# authorizationpolicy-deny-all.yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: deny-all
  namespace: istio-portfolio
spec: {}  # 빈 spec = 모든 트래픽 차단
```

### ALLOW 정책 (허용)

```yaml
# authorizationpolicy-allow-api.yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-api-access
  namespace: istio-portfolio
spec:
  selector:
    matchLabels:
      app: api
  action: ALLOW
  rules:
  # Ingress Gateway에서 오는 트래픽 허용
  - from:
    - source:
        principals: ["cluster.local/ns/istio-system/sa/istio-ingressgateway-service-account"]
  # 같은 네임스페이스 내 서비스에서 오는 트래픽 허용
  - from:
    - source:
        namespaces: ["istio-portfolio"]
```

### 세부 ALLOW 정책

```yaml
# 경로 및 메서드 기반 허용
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-api-read
  namespace: istio-portfolio
spec:
  selector:
    matchLabels:
      app: api
  action: ALLOW
  rules:
  - from:
    - source:
        namespaces: ["istio-portfolio"]
    to:
    - operation:
        methods: ["GET"]
        paths: ["/api/*", "/health"]
  - from:
    - source:
        principals: ["cluster.local/ns/istio-portfolio/sa/admin-sa"]
    to:
    - operation:
        methods: ["GET", "POST", "PUT", "DELETE"]
        paths: ["/api/*", "/admin/*"]
```

### DENY 정책 (명시적 차단)

```yaml
# 민감한 경로 차단
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: deny-admin-external
  namespace: istio-portfolio
spec:
  selector:
    matchLabels:
      app: api
  action: DENY
  rules:
  - from:
    - source:
        notNamespaces: ["istio-portfolio"]  # 외부 네임스페이스
    to:
    - operation:
        paths: ["/admin/*", "/internal/*"]
```

---

## 🛠️ Part 4: 실전 보안 설정

### 완전한 보안 설정 예제

```yaml
# 1. 네임스페이스에 mTLS STRICT 적용
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: strict-mtls
  namespace: istio-portfolio
spec:
  mtls:
    mode: STRICT
---
# 2. 기본 DENY ALL
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: deny-all
  namespace: istio-portfolio
spec: {}
---
# 3. Frontend 서비스 허용 정책
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-frontend
  namespace: istio-portfolio
spec:
  selector:
    matchLabels:
      app: frontend
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/istio-system/sa/istio-ingressgateway-service-account"]
---
# 4. API 서비스 허용 정책
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-api
  namespace: istio-portfolio
spec:
  selector:
    matchLabels:
      app: api
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/istio-portfolio/sa/frontend"]
  - from:
    - source:
        principals: ["cluster.local/ns/istio-system/sa/istio-ingressgateway-service-account"]
---
# 5. Database 서비스 허용 정책
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-database
  namespace: istio-portfolio
spec:
  selector:
    matchLabels:
      app: database
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/istio-portfolio/sa/api"]
```

---

## 🧪 Part 5: 보안 테스트

### mTLS 검증

```bash
# Sidecar 없는 Pod에서 접근 시도
kubectl run test-no-sidecar \
  --image=curlimages/curl \
  --rm -it \
  -n default \
  -- curl -v http://api.istio-portfolio.svc.cluster.local

# 예상: Connection refused (mTLS STRICT)
```

### AuthorizationPolicy 검증

```bash
# 허용된 소스에서 접근
kubectl exec -it deploy/frontend -n istio-portfolio -- \
  curl -s http://api:8080/health
# 예상: 200 OK

# 허용되지 않은 경로 접근
kubectl exec -it deploy/frontend -n istio-portfolio -- \
  curl -s http://api:8080/admin
# 예상: 403 Forbidden (DENY 정책)
```

### 디버깅 명령어

```bash
# Authorization 상태 확인
istioctl x authz check <pod-name> -n istio-portfolio

# Policy 목록
kubectl get authorizationpolicies -n istio-portfolio

# 상세 확인
kubectl describe authorizationpolicy allow-api -n istio-portfolio

# 설정 분석
istioctl analyze -n istio-portfolio
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 리소스/명령어 | 완료 |
|---|------|-------------|------|
| 1 | mTLS 개념 이해 | PERMISSIVE, STRICT | ☐ |
| 2 | PeerAuthentication 적용 | STRICT mTLS | ☐ |
| 3 | DENY ALL 정책 | `spec: {}` | ☐ |
| 4 | ALLOW 정책 | from, to, action | ☐ |
| 5 | mTLS 검증 | Sidecar 없는 Pod 테스트 | ☐ |
| 6 | AuthorizationPolicy 검증 | 허용/차단 테스트 | ☐ |
| 7 | Kiali에서 확인 | 자물쇠 아이콘 | ☐ |

---

## 🔑 핵심 설정 요약

```yaml
# mTLS STRICT
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: strict-mtls
  namespace: istio-portfolio
spec:
  mtls:
    mode: STRICT

---
# DENY ALL
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: deny-all
  namespace: istio-portfolio
spec: {}

---
# ALLOW 정책
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-service
spec:
  selector:
    matchLabels:
      app: myapp
  action: ALLOW
  rules:
  - from:
    - source:
        namespaces: ["istio-portfolio"]
```

---

## ➡️ 다음 학습: Day 86

**주제**: 관찰성 설정 (Kiali, Jaeger, Grafana)

