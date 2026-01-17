# 📅 Day 76: AuthorizationPolicy 심화

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "보안 컴플라이언스를 고려한 클라우드 인프라 설계/운영"
> "Zero Trust 보안 모델"은 마이크로서비스 환경에서 필수적인 보안 전략

Istio AuthorizationPolicy를 사용하여 서비스 간 접근을 세밀하게 제어합니다. mTLS(인증)와 결합하여 "누가 무엇을 할 수 있는지"를 정의하는 Zero Trust 보안을 구현합니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 이해 | 45분 | AuthorizationPolicy, Zero Trust |
| DENY/ALLOW 정책 | 1시간 | 기본 접근 제어 |
| 고급 정책 | 1.5시간 | HTTP 기반, 조건부 제어 |
| 실습 및 검증 | 45분 | 3-Tier 앱 보안 적용 |

---

## 📚 Part 1: AuthorizationPolicy 개념 (45분)

### 1.1 인증 vs 인가

```
┌─────────────────────────────────────────────────────────────────────┐
│  인증 (Authentication) vs 인가 (Authorization)                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  인증 (mTLS - Day 75에서 학습)                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  "당신은 누구인가?" (Identity)                               │    │
│  │                                                             │    │
│  │  • 서비스 ID 확인                                           │    │
│  │  • 인증서 기반 검증                                         │    │
│  │  • PeerAuthentication으로 설정                              │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│                              ▼                                      │
│                                                                      │
│  인가 (AuthorizationPolicy - 오늘 학습)                             │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  "당신은 무엇을 할 수 있는가?" (Permission)                  │    │
│  │                                                             │    │
│  │  • 서비스 접근 허용/거부                                    │    │
│  │  • HTTP 메서드/경로 제한                                    │    │
│  │  • 조건부 접근 제어 (헤더, IP 등)                          │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  예시:                                                               │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  인증: "이 요청은 frontend 서비스에서 왔다"                  │    │
│  │  인가: "frontend는 backend의 GET /api/* 만 호출 가능"       │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 Zero Trust 보안 모델

```
┌─────────────────────────────────────────────────────────────────────┐
│  Zero Trust 보안 모델                                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  기존 모델 (경계 기반)                                              │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  "내부 네트워크는 신뢰한다"                                  │    │
│  │                                                             │    │
│  │  외부 ──┬── 방화벽 ──┬── 내부 (모두 허용)                  │    │
│  │        │            │                                       │    │
│  │      차단         신뢰                                      │    │
│  │                                                             │    │
│  │  문제: 내부 침해 시 전체 노출                               │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Zero Trust 모델                                                     │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  "아무것도 신뢰하지 않는다. 항상 검증한다."                  │    │
│  │                                                             │    │
│  │  원칙:                                                      │    │
│  │  1. 명시적 검증 (매 요청마다 인증)                         │    │
│  │  2. 최소 권한 (필요한 것만 허용)                           │    │
│  │  3. 침해 가정 (언제든 침해될 수 있다고 가정)               │    │
│  │                                                             │    │
│  │  구현:                                                      │    │
│  │  1. DENY ALL (기본 거부)                                   │    │
│  │  2. 필요한 통신만 명시적 ALLOW                             │    │
│  │  3. mTLS로 모든 통신 암호화                                │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.3 AuthorizationPolicy 구조

```yaml
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: policy-name
  namespace: target-namespace    # 정책 적용 범위
spec:
  # 대상 워크로드 선택 (생략 시 네임스페이스 전체)
  selector:
    matchLabels:
      app: target-app
  
  # 액션: ALLOW | DENY | CUSTOM | AUDIT
  action: ALLOW
  
  # 규칙 (OR 조건)
  rules:
  - from:                        # 소스 조건
    - source:
        principals: ["..."]      # 서비스 ID
        namespaces: ["..."]      # 소스 네임스페이스
        ipBlocks: ["..."]        # 소스 IP
    to:                          # 대상 조건
    - operation:
        methods: ["GET"]         # HTTP 메서드
        paths: ["/api/*"]        # URL 경로
        ports: ["8080"]          # 포트
    when:                        # 추가 조건
    - key: request.headers[x-token]
      values: ["valid"]
```

| 필드 | 설명 | 예시 |
|------|------|------|
| **selector** | 정책 적용 대상 Pod | `app: backend` |
| **action** | 정책 동작 | `ALLOW`, `DENY`, `AUDIT` |
| **from.source** | 요청 출처 조건 | `principals`, `namespaces`, `ipBlocks` |
| **to.operation** | 요청 내용 조건 | `methods`, `paths`, `ports` |
| **when** | 추가 조건 | 헤더, JWT 클레임 등 |

---

## 🛠️ Part 2: 기본 정책 실습 (1시간)

### 실습 환경 준비

```bash
# Bookinfo 샘플 앱 배포 (이전에 배포하지 않았다면)
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/bookinfo/platform/kube/bookinfo.yaml

# 배포 확인
kubectl get pods -l app=productpage
kubectl get pods -l app=reviews

# 기존 AuthorizationPolicy 삭제 (깨끗한 상태로)
kubectl delete authorizationpolicy --all -n default 2>/dev/null
```

### 실습 1: DENY ALL (기본 거부)

```yaml
# deny-all.yaml
# 모든 접근 차단 (Zero Trust 시작점)
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: deny-all
  namespace: default
spec:
  # selector 없음 = 네임스페이스 전체 적용
  # rules 없음 = 모든 요청 거부
  {}
```

```bash
# DENY ALL 적용
kubectl apply -f - <<EOF
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: deny-all
  namespace: default
spec: {}
EOF

# 테스트 (모두 차단됨)
kubectl exec deploy/sleep -n default -- curl -s productpage:9080/productpage
# 예상: RBAC: access denied

# 확인
echo "=== DENY ALL 적용 확인 ==="
kubectl get authorizationpolicy -n default
```

### 실습 2: 특정 서비스 허용 (ALLOW)

```yaml
# allow-productpage.yaml
# Gateway → productpage 허용
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-productpage-from-gateway
  namespace: default
spec:
  selector:
    matchLabels:
      app: productpage
  action: ALLOW
  rules:
  - from:
    - source:
        # Istio Ingress Gateway의 서비스 어카운트
        principals: ["cluster.local/ns/istio-system/sa/istio-ingressgateway-service-account"]
```

```bash
kubectl apply -f - <<EOF
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-productpage-from-gateway
  namespace: default
spec:
  selector:
    matchLabels:
      app: productpage
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/istio-system/sa/istio-ingressgateway-service-account"]
EOF

# Gateway를 통한 접근 테스트
INGRESS_IP=$(kubectl get svc istio-ingressgateway -n istio-system -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
curl -s http://$INGRESS_IP/productpage | head -20
```

### 실습 3: 서비스 간 통신 허용

```yaml
# allow-backend-services.yaml
# productpage → reviews, details, ratings 허용
---
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-productpage-to-reviews
  namespace: default
spec:
  selector:
    matchLabels:
      app: reviews
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/default/sa/bookinfo-productpage"]
---
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-productpage-to-details
  namespace: default
spec:
  selector:
    matchLabels:
      app: details
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/default/sa/bookinfo-productpage"]
---
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-reviews-to-ratings
  namespace: default
spec:
  selector:
    matchLabels:
      app: ratings
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/default/sa/bookinfo-reviews"]
```

```bash
# 적용
kubectl apply -f - <<'EOF'
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-productpage-to-reviews
  namespace: default
spec:
  selector:
    matchLabels:
      app: reviews
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/default/sa/bookinfo-productpage"]
---
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-productpage-to-details
  namespace: default
spec:
  selector:
    matchLabels:
      app: details
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/default/sa/bookinfo-productpage"]
---
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-reviews-to-ratings
  namespace: default
spec:
  selector:
    matchLabels:
      app: ratings
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/default/sa/bookinfo-reviews"]
EOF

# 정책 확인
kubectl get authorizationpolicy -n default

# 전체 앱 테스트
curl -s http://$INGRESS_IP/productpage | grep -o "Reviews\|Details\|Ratings"
```

---

## 🛠️ Part 3: 고급 정책 (1.5시간)

### 실습 4: HTTP 메서드/경로 기반 제어

```yaml
# http-policy.yaml
# GET /reviews/* 만 허용, POST 차단
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-get-reviews-only
  namespace: default
spec:
  selector:
    matchLabels:
      app: reviews
  action: ALLOW
  rules:
  - from:
    - source:
        namespaces: ["default"]
    to:
    - operation:
        methods: ["GET"]
        paths: ["/reviews/*", "/health"]
```

```bash
kubectl apply -f - <<EOF
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-get-reviews-only
  namespace: default
spec:
  selector:
    matchLabels:
      app: reviews
  action: ALLOW
  rules:
  - from:
    - source:
        namespaces: ["default"]
    to:
    - operation:
        methods: ["GET"]
        paths: ["/reviews/*", "/health"]
EOF

# 테스트
kubectl exec deploy/sleep -- curl -s reviews:9080/reviews/0  # OK
kubectl exec deploy/sleep -- curl -X POST reviews:9080/reviews  # DENIED
```

### 실습 5: 헤더 기반 접근 제어

```yaml
# header-policy.yaml
# 특정 헤더가 있을 때만 허용
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: require-api-key
  namespace: default
spec:
  selector:
    matchLabels:
      app: ratings
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/default/sa/bookinfo-reviews"]
    when:
    - key: request.headers[x-api-key]
      values: ["secret-key-123"]
```

```bash
kubectl apply -f - <<EOF
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: require-api-key
  namespace: default
spec:
  selector:
    matchLabels:
      app: ratings
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/default/sa/bookinfo-reviews"]
    when:
    - key: request.headers[x-api-key]
      values: ["secret-key-123"]
EOF

# 테스트 (헤더 없음 - 실패)
kubectl exec deploy/sleep -- curl -s ratings:9080/ratings/0

# 헤더 포함 (성공) - reviews에서 ratings로는 자동으로 헤더 추가 필요
```

### 실습 6: IP 기반 차단 (DENY)

```yaml
# deny-ip.yaml
# 특정 IP 대역 차단
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: deny-bad-ips
  namespace: default
spec:
  selector:
    matchLabels:
      app: productpage
  action: DENY
  rules:
  - from:
    - source:
        # 내부 테스트 IP 차단 예시
        ipBlocks: ["10.0.100.0/24", "192.168.200.0/24"]
```

```bash
kubectl apply -f - <<EOF
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: deny-bad-ips
  namespace: default
spec:
  selector:
    matchLabels:
      app: productpage
  action: DENY
  rules:
  - from:
    - source:
        ipBlocks: ["10.0.100.0/24"]
EOF

# 현재 Pod IP 확인
kubectl get pod -l app=sleep -o jsonpath='{.items[0].status.podIP}'
```

### 실습 7: 복합 조건 정책

```yaml
# complex-policy.yaml
# 여러 조건 조합
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: complex-reviews-policy
  namespace: default
spec:
  selector:
    matchLabels:
      app: reviews
  action: ALLOW
  rules:
  # Rule 1: productpage에서 GET만
  - from:
    - source:
        principals: ["cluster.local/ns/default/sa/bookinfo-productpage"]
    to:
    - operation:
        methods: ["GET"]
        paths: ["/reviews/*"]
  
  # Rule 2: 관리자 네임스페이스에서 모든 메서드
  - from:
    - source:
        namespaces: ["admin"]
    when:
    - key: request.headers[x-admin-token]
      values: ["admin-secret"]
```

```bash
kubectl apply -f - <<EOF
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: complex-reviews-policy
  namespace: default
spec:
  selector:
    matchLabels:
      app: reviews
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/default/sa/bookinfo-productpage"]
    to:
    - operation:
        methods: ["GET"]
        paths: ["/reviews/*"]
  - from:
    - source:
        namespaces: ["admin"]
    when:
    - key: request.headers[x-admin-token]
      values: ["admin-secret"]
EOF
```

---

## 🛠️ Part 4: 3-Tier 앱 Zero Trust 구현 (45분)

### 4.1 완전한 Zero Trust 정책 세트

```yaml
# zero-trust-complete.yaml
# 3-Tier 앱 (Frontend - Backend - Database) Zero Trust 구현

# 1. DENY ALL - 기본 거부
---
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: deny-all
  namespace: production
spec: {}

# 2. Gateway → Frontend 허용
---
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-gateway-to-frontend
  namespace: production
spec:
  selector:
    matchLabels:
      app: frontend
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/istio-system/sa/istio-ingressgateway-service-account"]

# 3. Frontend → Backend 허용 (GET, POST만)
---
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
        principals: ["cluster.local/ns/production/sa/frontend"]
    to:
    - operation:
        methods: ["GET", "POST"]
        paths: ["/api/*", "/health", "/ready"]

# 4. Backend → Database 허용
---
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-backend-to-database
  namespace: production
spec:
  selector:
    matchLabels:
      app: database
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/production/sa/backend"]
    to:
    - operation:
        ports: ["3306"]

# 5. 모니터링 접근 허용 (Prometheus)
---
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-prometheus
  namespace: production
spec:
  action: ALLOW
  rules:
  - from:
    - source:
        namespaces: ["monitoring"]
    to:
    - operation:
        methods: ["GET"]
        paths: ["/metrics"]
```

### 4.2 정책 검증

```bash
# 정책 적용
kubectl apply -f zero-trust-complete.yaml

# 정책 목록
kubectl get authorizationpolicy -n production

# 허용된 통신 테스트
kubectl exec deploy/frontend -n production -- curl -s backend:80/api/info

# 차단된 통신 테스트 (frontend → database 직접 접근)
kubectl exec deploy/frontend -n production -- curl -s database:3306
# 예상: connection refused 또는 RBAC denied

# 차단된 통신 테스트 (외부 → backend 직접 접근)
kubectl exec deploy/sleep -- curl -s backend.production:80/api/info
# 예상: RBAC denied
```

---

## 📊 Part 5: 정책 우선순위와 디버깅

### 5.1 정책 평가 순서

```
┌─────────────────────────────────────────────────────────────────────┐
│  AuthorizationPolicy 평가 순서                                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. CUSTOM 정책 (외부 인가 서버)                                    │
│                     │                                                │
│                     ▼                                                │
│  2. DENY 정책                                                        │
│     • 하나라도 매치되면 → 거부                                      │
│                     │                                                │
│                     ▼                                                │
│  3. ALLOW 정책                                                       │
│     • 하나라도 매치되면 → 허용                                      │
│     • 매치 없으면 → 다음 단계                                       │
│                     │                                                │
│                     ▼                                                │
│  4. ALLOW 정책이 있는데 매치 안 됨 → 거부                           │
│     ALLOW 정책이 없음 → 허용 (기본)                                 │
│                                                                      │
│  예외: spec: {} (빈 정책) = 모든 요청 거부                          │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 5.2 디버깅 명령어

```bash
# 정책 목록
kubectl get authorizationpolicy -A

# 정책 상세
kubectl describe authorizationpolicy <name> -n <namespace>

# Envoy 로그에서 RBAC 확인
kubectl logs -l app=productpage -c istio-proxy | grep "rbac"

# istioctl로 정책 분석
istioctl analyze -n default

# Kiali에서 시각적으로 확인
# - Security 탭에서 정책 확인 가능
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | 인증 vs 인가 이해 | mTLS vs AuthorizationPolicy | ☐ |
| 2 | Zero Trust 원칙 | DENY ALL + 명시적 ALLOW | ☐ |
| 3 | DENY ALL 정책 | 기본 거부 설정 | ☐ |
| 4 | ALLOW 정책 | 서비스 간 통신 허용 | ☐ |
| 5 | HTTP 메서드/경로 제어 | GET /api/* 만 허용 | ☐ |
| 6 | 헤더 기반 제어 | x-api-key 검증 | ☐ |
| 7 | IP 차단 | DENY with ipBlocks | ☐ |
| 8 | 복합 조건 | 여러 조건 조합 | ☐ |

---

## 🔑 오늘 배운 핵심 YAML

```yaml
# DENY ALL
spec: {}

# 서비스 ID 기반 ALLOW
spec:
  selector:
    matchLabels:
      app: backend
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/default/sa/frontend"]

# HTTP 메서드/경로 제한
    to:
    - operation:
        methods: ["GET", "POST"]
        paths: ["/api/*"]

# 헤더 조건
    when:
    - key: request.headers[x-api-key]
      values: ["secret"]
```

---

## 💡 면접 대비 핵심 포인트

### Q1: Istio에서 Zero Trust 보안을 어떻게 구현하나요?

**A**: "먼저 빈 spec의 AuthorizationPolicy로 DENY ALL을 설정하여 모든 접근을 거부합니다. 그 후 필요한 서비스 간 통신만 ALLOW 정책으로 명시적으로 허용합니다. mTLS와 함께 사용하면 서비스 ID 기반 인증과 인가가 완성됩니다."

### Q2: AuthorizationPolicy의 from, to, when의 차이는?

**A**: 
- **from**: 요청의 출처 (누가) - principals, namespaces, ipBlocks
- **to**: 요청의 대상 (무엇을) - methods, paths, ports
- **when**: 추가 조건 (언제) - 헤더, JWT 클레임 등

### Q3: DENY와 ALLOW 정책이 충돌하면?

**A**: "DENY가 ALLOW보다 우선합니다. 평가 순서는 CUSTOM → DENY → ALLOW입니다. DENY에 매치되면 ALLOW와 관계없이 거부됩니다."

---

## 📝 학습 기록

```
학습일: ____년 __월 __일
실제 소요 시간: ____시간

실습 완료 항목:
- [ ] DENY ALL 정책
- [ ] 서비스 간 ALLOW 정책
- [ ] HTTP 메서드/경로 제한
- [ ] 헤더 기반 제어
- [ ] IP 차단

이해가 어려웠던 부분:


추가 학습 필요 항목:

```

---

## ➡️ 다음 학습: Day 77

**주제**: JWT Authentication
- RequestAuthentication
- JWT 검증
- JWT 클레임 기반 인가
