# 📅 Day 81: Week 11 복습 - Istio Security 종합

## 🎯 오늘의 목표

> **토스플레이스 핵심**: Istio 보안 기능을 종합 정리하고 실전 적용 능력을 완성합니다.

---

## ⏰ 예상 학습 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 복습 | 1시간 | Week 11 핵심 정리 |
| 종합 실습 | 1시간 | 보안 시나리오 |
| 면접 대비 | 1시간 | Q&A 정리 |

---

## 📋 Week 11 학습 요약

| Day | 주제 | 핵심 내용 | 토스플레이스 연결 |
|-----|------|----------|-----------------|
| 75 | mTLS 기초 | PeerAuthentication, STRICT | 통신 암호화 |
| 76 | AuthorizationPolicy | ALLOW/DENY, 접근 제어 | Zero Trust |
| 77 | JWT Authentication | RequestAuthentication | API 인증 |
| 78 | Egress 제어 | ServiceEntry, Egress Gateway | 외부 통신 관리 |
| 79 | Zero Trust | 종합 보안 설계 | 보안 컴플라이언스 |
| 80 | Rate Limiting | EnvoyFilter, Local Rate Limit | 트래픽 제어 |

---

## 🔑 Part 1: 핵심 리소스 정리

### 1. PeerAuthentication (mTLS)

```yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: strict-mtls
  namespace: production  # 또는 istio-system (전체 적용)
spec:
  mtls:
    mode: STRICT  # 모든 통신 암호화 강제
```

**mTLS 모드**:

| 모드 | 설명 | 사용 시기 |
|------|------|----------|
| `STRICT` | mTLS만 허용 | 프로덕션 |
| `PERMISSIVE` | mTLS + 평문 모두 허용 | 마이그레이션 |
| `DISABLE` | mTLS 비활성화 | 특수 케이스 |

### 2. AuthorizationPolicy (접근 제어)

```yaml
# DENY ALL (기본 차단)
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: deny-all
  namespace: production
spec: {}  # 빈 spec = 모든 트래픽 차단

---
# ALLOW 정책 (허용)
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-frontend-to-api
  namespace: production
spec:
  selector:
    matchLabels:
      app: api
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/production/sa/frontend"]
    to:
    - operation:
        methods: ["GET", "POST"]
        paths: ["/api/*"]
```

### 3. RequestAuthentication (JWT)

```yaml
apiVersion: security.istio.io/v1beta1
kind: RequestAuthentication
metadata:
  name: jwt-auth
  namespace: production
spec:
  selector:
    matchLabels:
      app: api
  jwtRules:
  - issuer: "https://auth.example.com"
    jwksUri: "https://auth.example.com/.well-known/jwks.json"
    audiences:
    - "api.example.com"
    forwardOriginalToken: true
```

### 4. ServiceEntry (Egress)

```yaml
apiVersion: networking.istio.io/v1beta1
kind: ServiceEntry
metadata:
  name: external-api
  namespace: production
spec:
  hosts:
  - api.external.com
  ports:
  - number: 443
    name: https
    protocol: HTTPS
  location: MESH_EXTERNAL
  resolution: DNS
```

---

## 🛠️ Part 2: Zero Trust 아키텍처

### 보안 계층

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     Zero Trust Security Layers                               │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Layer 1: 전송 보안 (Transport)                                             │
│   ┌────────────────────────────────────────────────────────────────────────┐│
│   │ PeerAuthentication: mTLS STRICT                                        ││
│   │ → 모든 서비스 간 통신 암호화                                            ││
│   └────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│   Layer 2: 접근 제어 (Access Control)                                        │
│   ┌────────────────────────────────────────────────────────────────────────┐│
│   │ AuthorizationPolicy: DENY ALL + ALLOW                                  ││
│   │ → 기본 차단, 명시적 허용만 통과                                         ││
│   └────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│   Layer 3: 인증 (Authentication)                                             │
│   ┌────────────────────────────────────────────────────────────────────────┐│
│   │ RequestAuthentication: JWT 검증                                        ││
│   │ → 외부 요청의 토큰 검증                                                 ││
│   └────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│   Layer 4: 외부 통신 (Egress)                                                │
│   ┌────────────────────────────────────────────────────────────────────────┐│
│   │ ServiceEntry + AuthorizationPolicy                                     ││
│   │ → 허용된 외부 서비스만 접근 가능                                        ││
│   └────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 종합 보안 설정

```yaml
# 1. 네임스페이스 mTLS
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: production
spec:
  mtls:
    mode: STRICT
---
# 2. DENY ALL
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: deny-all
  namespace: production
spec: {}
---
# 3. Ingress Gateway → Frontend
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
        principals: ["cluster.local/ns/istio-system/sa/istio-ingressgateway-service-account"]
---
# 4. Frontend → API
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-frontend-to-api
  namespace: production
spec:
  selector:
    matchLabels:
      app: api
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/production/sa/frontend"]
---
# 5. API → Database
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: allow-api-to-db
  namespace: production
spec:
  selector:
    matchLabels:
      app: database
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/production/sa/api"]
```

---

## 🛠️ Part 3: 보안 검증

### mTLS 확인

```bash
# PeerAuthentication 확인
kubectl get peerauthentication -n production

# mTLS 상태 확인
istioctl x authz check <pod-name> -n production

# Kiali에서 확인 (자물쇠 아이콘)
kubectl port-forward svc/kiali -n istio-system 20001:20001
```

### AuthorizationPolicy 테스트

```bash
# 허용된 접근 (성공해야 함)
kubectl exec deploy/frontend -n production -- curl -s http://api:8080/health
# 200 OK

# 비허용 접근 (실패해야 함)
kubectl exec deploy/database -n production -- curl -s http://api:8080/health
# RBAC: access denied

# 외부에서 접근 (실패해야 함)
kubectl run test --image=curlimages/curl --rm -it -n default -- \
  curl -s http://api.production.svc:8080/health
# Connection refused (mTLS) 또는 RBAC denied
```

### 분석 명령어

```bash
# 전체 설정 분석
istioctl analyze -n production

# AuthorizationPolicy 디버깅
istioctl x authz check <pod> -n production

# Proxy 설정 확인
istioctl proxy-config listeners <pod> -n production
```

---

## 📝 Part 4: 면접 대비 Q&A

### Q1: "Istio에서 보안을 어떻게 구현하나요?"

```
✅ 모범 답변:

"Istio에서 세 가지 계층으로 보안을 구현합니다.

1. **mTLS (PeerAuthentication)**
   - 서비스 간 모든 통신을 암호화합니다.
   - STRICT 모드로 Sidecar 없는 Pod의 접근을 차단합니다.

2. **AuthorizationPolicy**
   - Zero Trust 모델: 기본 DENY ALL + 명시적 ALLOW
   - ServiceAccount 기반으로 서비스 간 접근을 제어합니다.

3. **RequestAuthentication**
   - 외부 요청의 JWT 토큰을 검증합니다.
   - JWKS를 통해 토큰 서명을 확인합니다.

이 세 계층을 조합하여 네트워크 위치가 아닌 신원 기반의 
Zero Trust 보안을 구현합니다."
```

### Q2: "mTLS STRICT와 PERMISSIVE의 차이는?"

```
✅ 모범 답변:

"STRICT 모드는 mTLS 통신만 허용합니다. 
Sidecar가 없는 Pod에서의 요청은 차단됩니다.

PERMISSIVE 모드는 mTLS와 평문 통신 모두 허용합니다.
레거시 서비스 마이그레이션 시 사용합니다.

프로덕션 환경에서는 반드시 STRICT를 사용해야 합니다.
마이그레이션 기간에만 PERMISSIVE를 사용하고,
완료 후 STRICT로 전환합니다."
```

### Q3: "AuthorizationPolicy의 우선순위는?"

```
✅ 모범 답변:

"AuthorizationPolicy는 다음 순서로 평가됩니다:

1. CUSTOM - 가장 먼저 평가
2. DENY - 명시적 거부
3. ALLOW - 명시적 허용
4. 기본값 - 정책 없으면 모두 허용

보안을 위해 DENY ALL 정책을 먼저 적용하고,
필요한 트래픽만 ALLOW로 허용하는 것이 권장됩니다.
이것이 Zero Trust의 핵심입니다."
```

### Q4: "Egress 트래픽을 어떻게 제어하나요?"

```
✅ 모범 답변:

"기본적으로 Istio는 외부 트래픽을 허용하지만,
REGISTRY_ONLY 모드로 변경하면 ServiceEntry에 등록된
외부 서비스만 접근 가능합니다.

1. meshConfig.outboundTrafficPolicy를 REGISTRY_ONLY로 설정
2. 허용할 외부 서비스를 ServiceEntry로 등록
3. AuthorizationPolicy로 어떤 내부 서비스가 접근 가능한지 제어

이렇게 하면 내부 서비스가 임의의 외부 서비스에
접근하는 것을 방지할 수 있습니다."
```

---

## ✅ Week 11 체크리스트

| # | 항목 | 확인 명령어 | 완료 |
|---|------|-----------|------|
| 1 | mTLS STRICT 설정 | `kubectl get pa -n <ns>` | ☐ |
| 2 | DENY ALL 정책 | `kubectl get authorizationpolicy` | ☐ |
| 3 | ALLOW 정책 | 허용된 접근 테스트 | ☐ |
| 4 | JWT 인증 설정 | `kubectl get ra -n <ns>` | ☐ |
| 5 | ServiceEntry 설정 | `kubectl get se -n <ns>` | ☐ |
| 6 | 보안 검증 | `istioctl analyze` | ☐ |
| 7 | Kiali에서 mTLS 확인 | 자물쇠 아이콘 | ☐ |

---

## 🔑 핵심 명령어 요약

```bash
# 리소스 확인
kubectl get peerauthentication -A
kubectl get authorizationpolicy -A
kubectl get requestauthentication -A
kubectl get serviceentry -A

# 분석 및 디버깅
istioctl analyze -n <namespace>
istioctl x authz check <pod> -n <namespace>

# 테스트
kubectl exec <pod> -- curl -s http://<service>:<port>
```

---

## ➡️ 다음 학습: Day 82

**주제**: Month 3 포트폴리오 프로젝트 시작

