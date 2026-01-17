# 📅 Day 88: Istio 포트폴리오 문서화

## 🎯 오늘의 목표

> **토스플레이스 핵심**: 프로젝트 문서를 체계적으로 작성하여 포트폴리오 가치를 높입니다.

---

## ⏰ 예상 소요 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| README | 1시간 30분 | 메인 문서 |
| 아키텍처 | 1시간 | 다이어그램 |
| 운영 가이드 | 1시간 | 배포, 트러블슈팅 |
| 정리 | 30분 | 최종 검토 |

---

## 📚 Part 1: README.md 작성

### 전체 구조

```markdown
# 🚀 Istio Service Mesh 포트폴리오

대규모 마이크로서비스 환경을 위한 프로덕션 수준 Istio Service Mesh 구현

## 📌 프로젝트 개요

토스플레이스 DevOps Engineer 포지션을 위한 Service Mesh 포트폴리오입니다.
Kubernetes 환경에서 Istio를 활용하여 트래픽 관리, 보안, 관찰성을 구현했습니다.

## 🏗 아키텍처

```text
                    ┌─────────────┐
                    │   Client    │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │   Ingress   │
                    │   Gateway   │
                    └──────┬──────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
    ┌─────▼─────┐    ┌─────▼─────┐    ┌─────▼─────┐
    │ Frontend  │    │    API    │    │    API    │
    │   (v1)    │    │   (v1)    │    │   (v2)    │
    └─────┬─────┘    └─────┬─────┘    └─────┬─────┘
          │                │                │
          │         ┌──────▼──────┐         │
          └────────▶│  Database   │◀────────┘
                    └─────────────┘
```

## ✨ 핵심 기능

### 1. 트래픽 관리
- **카나리 배포**: VirtualService 가중치 기반 (90:10)
- **헤더 기반 라우팅**: x-user-type: beta → v2
- **Timeout/Retry**: 3초 타임아웃, 3회 재시도

### 2. 보안
- **mTLS STRICT**: 서비스 간 암호화 통신
- **Zero Trust**: DENY ALL + ALLOW 정책
- **AuthorizationPolicy**: 명시적 접근 제어

### 3. 관찰성
- **Kiali**: 서비스 토폴로지 시각화
- **Jaeger**: 분산 추적 (Trace/Span)
- **Grafana**: 메트릭 대시보드

### 4. 장애 복원력
- **Circuit Breaker**: 연속 5회 실패 시 30초 차단
- **Retry**: 5xx 에러 시 자동 재시도
- **Fault Injection**: 장애 시뮬레이션 테스트

## 📁 프로젝트 구조

```text
istio-portfolio/
├── manifests/
│   ├── base/
│   │   ├── namespace.yaml
│   │   └── deployments/
│   │       ├── frontend.yaml
│   │       ├── api-v1.yaml
│   │       ├── api-v2.yaml
│   │       └── database.yaml
│   └── istio/
│       ├── gateway.yaml
│       ├── virtualservice.yaml
│       ├── destinationrule.yaml
│       ├── peerauthentication.yaml
│       └── authorizationpolicy.yaml
├── docs/
│   ├── architecture.md
│   ├── deployment.md
│   ├── canary-guide.md
│   └── troubleshooting.md
└── README.md
```

## 🚀 배포 방법

### 사전 요구사항
- Kubernetes 1.25+
- Istio 1.20+
- kubectl, istioctl

### 설치

```bash
# 1. Namespace 생성
kubectl apply -f manifests/base/namespace.yaml

# 2. Istio Injection 활성화
kubectl label namespace istio-portfolio istio-injection=enabled

# 3. 앱 배포
kubectl apply -f manifests/base/deployments/

# 4. Istio 리소스 적용
kubectl apply -f manifests/istio/

# 5. 확인
kubectl get pods -n istio-portfolio
kubectl get vs,dr,gw -n istio-portfolio
```

## 📊 모니터링

```bash
# Kiali
kubectl port-forward svc/kiali -n istio-system 20001:20001

# Jaeger
kubectl port-forward svc/tracing -n istio-system 16686:16686

# Grafana
kubectl port-forward svc/grafana -n istio-system 3000:3000
```

## 🎓 학습 포인트

1. **Service Mesh 필요성**: 마이크로서비스 복잡성 해결
2. **Istio 트래픽 관리**: VirtualService, DestinationRule
3. **Zero Trust 보안**: mTLS, AuthorizationPolicy
4. **Observability**: Metrics, Traces, Logs 통합

## 📝 License

MIT License
```

---

## 📚 Part 2: 아키텍처 문서

### docs/architecture.md

```markdown
# 아키텍처 문서

## 시스템 구성요소

### 서비스 구조

| 서비스 | 역할 | 포트 |
|--------|------|------|
| frontend | 웹 UI | 80 |
| api-v1 | API (안정 버전) | 8080 |
| api-v2 | API (카나리 버전) | 8080 |
| database | 데이터 저장소 | 5432 |

### Istio 리소스

| 리소스 | 용도 |
|--------|------|
| Gateway | 외부 트래픽 진입점 |
| VirtualService | 트래픽 라우팅 규칙 |
| DestinationRule | Subset, Circuit Breaker |
| PeerAuthentication | mTLS 정책 |
| AuthorizationPolicy | 접근 제어 |

### 트래픽 흐름

```text
1. Client → Ingress Gateway
2. Gateway → VirtualService (라우팅 결정)
3. VirtualService → DestinationRule (Subset 선택)
4. Envoy Sidecar → Target Service
```

## 보안 모델

### Zero Trust 구현

```text
┌─────────────────────────────────────────────────────┐
│                    istio-portfolio                   │
├─────────────────────────────────────────────────────┤
│                                                     │
│  PeerAuthentication: mTLS STRICT                    │
│  ─────────────────────────────────────────────────  │
│                                                     │
│  AuthorizationPolicy: DENY ALL (기본)               │
│  ─────────────────────────────────────────────────  │
│                                                     │
│  ALLOW 정책:                                        │
│  - Ingress Gateway → Frontend ✓                    │
│  - Ingress Gateway → API ✓                         │
│  - Frontend → API ✓                                │
│  - API → Database ✓                                │
│                                                     │
│  그 외 모든 트래픽: DENY ✗                          │
│                                                     │
└─────────────────────────────────────────────────────┘
```
```

---

## 📚 Part 3: 배포 가이드

### docs/deployment.md

```markdown
# 배포 가이드

## 환경별 배포

### Development

```bash
# 개발 환경 배포
kubectl apply -k manifests/overlays/dev/
```

### Production

```bash
# 프로덕션 배포
kubectl apply -k manifests/overlays/prod/
```

## 카나리 배포

### 1. 새 버전 배포

```bash
# v2 Deployment 적용
kubectl apply -f manifests/base/deployments/api-v2.yaml
```

### 2. 트래픽 전환 (10%)

```yaml
# virtualservice.yaml
spec:
  http:
  - route:
    - destination:
        host: api
        subset: v1
      weight: 90
    - destination:
        host: api
        subset: v2
      weight: 10
```

### 3. 모니터링

```bash
# 에러율 확인
kubectl port-forward svc/grafana -n istio-system 3000:3000
# Istio Service Dashboard 확인

# Kiali에서 트래픽 분포 확인
kubectl port-forward svc/kiali -n istio-system 20001:20001
```

### 4. 점진적 롤아웃

```yaml
# 50% 전환
weight: 50 / 50

# 100% 전환 (롤아웃 완료)
weight: 0 / 100
```

### 5. 롤백

```yaml
# 문제 발생 시 즉시 롤백
weight: 100 / 0
```
```

---

## 📚 Part 4: 트러블슈팅 가이드

### docs/troubleshooting.md

```markdown
# 트러블슈팅 가이드

## 자주 발생하는 문제

### 1. 503 Service Unavailable

**증상**: 서비스 접근 시 503 에러

**원인 확인**:
```bash
# Envoy 로그 확인
kubectl logs <pod> -c istio-proxy -n istio-portfolio

# Response Flags 확인
# UF: Upstream connection failure
# UO: Circuit breaker
# NR: No route
```

**해결**:
```bash
# Endpoint 확인
kubectl get endpoints -n istio-portfolio

# DestinationRule subset 확인
kubectl get dr -n istio-portfolio -o yaml
```

### 2. mTLS 연결 실패

**증상**: Sidecar 없는 Pod에서 접근 불가

**원인**: PeerAuthentication STRICT 모드

**해결**:
```bash
# Sidecar Injection 확인
kubectl get pod <pod> -o jsonpath='{.spec.containers[*].name}'

# 네임스페이스 레이블 확인
kubectl get ns istio-portfolio --show-labels
```

### 3. AuthorizationPolicy 차단

**증상**: 403 RBAC: access denied

**원인 확인**:
```bash
istioctl x authz check <pod> -n istio-portfolio
```

**해결**:
```bash
# ALLOW 정책 확인
kubectl get authorizationpolicy -n istio-portfolio -o yaml
```

## 디버깅 명령어

```bash
# 전체 분석
istioctl analyze -n istio-portfolio

# Proxy 상태
istioctl proxy-status

# 라우트 설정
istioctl proxy-config routes <pod> -n istio-portfolio

# 클러스터 설정
istioctl proxy-config clusters <pod> -n istio-portfolio
```
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 파일 | 완료 |
|---|------|------|------|
| 1 | README.md 작성 | README.md | ☐ |
| 2 | 아키텍처 문서 | docs/architecture.md | ☐ |
| 3 | 배포 가이드 | docs/deployment.md | ☐ |
| 4 | 트러블슈팅 가이드 | docs/troubleshooting.md | ☐ |
| 5 | 카나리 배포 가이드 | docs/canary-guide.md | ☐ |
| 6 | 스크린샷 추가 | docs/images/ | ☐ |

---

## 📁 최종 프로젝트 구조

```
istio-portfolio/
├── README.md              # 메인 문서
├── manifests/
│   ├── base/
│   │   ├── namespace.yaml
│   │   └── deployments/
│   └── istio/
│       ├── gateway.yaml
│       ├── virtualservice.yaml
│       ├── destinationrule.yaml
│       ├── peerauthentication.yaml
│       └── authorizationpolicy.yaml
└── docs/
    ├── architecture.md    # 아키텍처
    ├── deployment.md      # 배포 가이드
    ├── canary-guide.md    # 카나리 배포
    ├── troubleshooting.md # 트러블슈팅
    └── images/            # 스크린샷
```

---

## ➡️ 다음 학습: Day 89

**주제**: 테스트 및 개선

