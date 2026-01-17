# 📅 Day 67: Week 9 복습 - Istio 트래픽 관리 종합 실습

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "Kubernetes와 Service Mesh에 대한 경험"
> Week 9에서 배운 Istio 트래픽 관리 기능을 종합 실습

Week 9에서 학습한 Istio 설치, VirtualService, DestinationRule, Gateway, Timeout/Retry, Traffic Mirroring을 종합하여 실제 배포 시나리오를 실습합니다. 카나리 배포의 전체 과정을 처음부터 끝까지 경험합니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| Week 9 복습 | 45분 | 핵심 개념 정리 |
| 종합 실습 | 2시간 | 카나리 배포 시나리오 |
| 트러블슈팅 | 45분 | 일반적인 문제 해결 |
| 면접 대비 | 30분 | 핵심 Q&A |

---

## 📋 Part 1: Week 9 핵심 복습 (45분)

### 1.1 Week 9 학습 내용 요약

| Day | 주제 | 핵심 내용 | 토스플레이스 연관성 |
|-----|------|----------|-------------------|
| 61 | Istio 소개/설치 | Service Mesh, Sidecar, istiod | 대규모 트래픽 관리 |
| 62 | VirtualService | 트래픽 라우팅, 가중치, 헤더 매칭 | 카나리 배포 |
| 63 | DestinationRule | Subset, LB 정책, Circuit Breaker | 장애 격리 |
| 64 | Gateway | 외부 트래픽, HTTPS 종료 | 인그레스 관리 |
| 65 | Timeout/Retry | 장애 복원력, Fault Injection | 안정적인 서비스 |
| 66 | Mirroring | 트래픽 섀도잉, 프로덕션 테스트 | 안전한 배포 |

### 1.2 핵심 리소스 관계

```
┌─────────────────────────────────────────────────────────────────────┐
│  Istio 트래픽 관리 리소스 관계                                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  외부 트래픽                                                        │
│       │                                                              │
│       ▼                                                              │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  Gateway                                                      │   │
│  │  • hosts: ["*.example.com"]                                  │   │
│  │  • TLS 설정                                                   │   │
│  └─────────────────────────────┬────────────────────────────────┘   │
│                                │                                     │
│                                ▼                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  VirtualService                                               │   │
│  │  • 라우팅 규칙 (weight, match)                               │   │
│  │  • timeout, retries                                           │   │
│  │  • fault injection                                            │   │
│  │  • mirror                                                     │   │
│  └─────────────────────────────┬────────────────────────────────┘   │
│                                │                                     │
│                                ▼                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  DestinationRule                                              │   │
│  │  • subsets (v1, v2, v3)                                       │   │
│  │  • trafficPolicy (connectionPool, outlierDetection)          │   │
│  │  • loadBalancer                                               │   │
│  └─────────────────────────────┬────────────────────────────────┘   │
│                                │                                     │
│                                ▼                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  Service / Pod                                                │   │
│  │  • version: v1 (label)                                        │   │
│  │  • version: v2 (label)                                        │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.3 핵심 YAML 템플릿 복습

```yaml
# VirtualService 기본 구조
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: my-service
spec:
  hosts:
  - my-service                    # 서비스 이름
  - "my-service.example.com"      # 외부 호스트
  gateways:
  - my-gateway                    # Gateway 연결 (외부용)
  - mesh                          # 메시 내부 (서비스간)
  http:
  - match:                        # 조건 매칭
    - headers:
        end-user:
          exact: "tester"
    route:
    - destination:
        host: my-service
        subset: v2
  - route:                        # 기본 라우팅
    - destination:
        host: my-service
        subset: v1
      weight: 90
    - destination:
        host: my-service
        subset: v2
      weight: 10
    timeout: 5s                   # 타임아웃
    retries:                      # 재시도
      attempts: 3
      perTryTimeout: 2s
    mirror:                       # 미러링
      host: my-service
      subset: v2
    mirrorPercentage:
      value: 50.0
```

```yaml
# DestinationRule 기본 구조
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: my-service
spec:
  host: my-service
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        h2UpgradePolicy: UPGRADE
        http1MaxPendingRequests: 100
        http2MaxRequests: 1000
    outlierDetection:             # Circuit Breaker
      consecutive5xxErrors: 5
      interval: 10s
      baseEjectionTime: 30s
      maxEjectionPercent: 50
    loadBalancer:
      simple: ROUND_ROBIN         # 또는 LEAST_CONN, RANDOM
  subsets:
  - name: v1
    labels:
      version: v1
  - name: v2
    labels:
      version: v2
```

---

## 🛠️ Part 2: 종합 실습 - 안전한 버전 업그레이드 (2시간)

### 2.1 시나리오 개요

```
┌─────────────────────────────────────────────────────────────────────┐
│  시나리오: v1 → v2 안전한 버전 업그레이드                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  현재 상태: v1이 100% 프로덕션 트래픽 처리 중                       │
│  목표: v2를 안전하게 배포하고 100% 전환                             │
│                                                                      │
│  단계별 진행:                                                        │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Stage 1: v2 배포 + Traffic Mirroring (100%)               │    │
│  │           → v2 로그/메트릭 관찰, 실제 트래픽 영향 없음      │    │
│  │                                                             │    │
│  │  Stage 2: 카나리 5%                                         │    │
│  │           → 소수 사용자로 실제 테스트                       │    │
│  │                                                             │    │
│  │  Stage 3: 카나리 25%                                        │    │
│  │           → 메트릭 정상 확인 후 확대                        │    │
│  │                                                             │    │
│  │  Stage 4: 카나리 50%                                        │    │
│  │           → 대규모 트래픽 처리 검증                         │    │
│  │                                                             │    │
│  │  Stage 5: 100% v2                                           │    │
│  │           → 완전 전환, v1 제거 예정                         │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 환경 준비

```bash
# 네임스페이스 생성 (Istio 사이드카 주입)
kubectl create namespace canary-demo
kubectl label namespace canary-demo istio-injection=enabled

# reviews 서비스 v1 배포
kubectl apply -n canary-demo -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: reviews-v1
spec:
  replicas: 2
  selector:
    matchLabels:
      app: reviews
      version: v1
  template:
    metadata:
      labels:
        app: reviews
        version: v1
    spec:
      containers:
      - name: reviews
        image: docker.io/istio/examples-bookinfo-reviews-v1:1.18.0
        ports:
        - containerPort: 9080
        resources:
          requests:
            cpu: 100m
            memory: 128Mi
---
apiVersion: v1
kind: Service
metadata:
  name: reviews
spec:
  selector:
    app: reviews
  ports:
  - port: 9080
    targetPort: 9080
EOF

# reviews 서비스 v2 배포
kubectl apply -n canary-demo -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: reviews-v2
spec:
  replicas: 2
  selector:
    matchLabels:
      app: reviews
      version: v2
  template:
    metadata:
      labels:
        app: reviews
        version: v2
    spec:
      containers:
      - name: reviews
        image: docker.io/istio/examples-bookinfo-reviews-v2:1.18.0
        ports:
        - containerPort: 9080
        resources:
          requests:
            cpu: 100m
            memory: 128Mi
EOF

# 배포 확인
kubectl get pods -n canary-demo -L version
```

### 2.3 Stage 1: Traffic Mirroring

```yaml
# stage1-mirroring.yaml
# 모든 트래픽을 v1으로 보내고, v2에 미러링
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: reviews
  namespace: canary-demo
spec:
  host: reviews
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
    outlierDetection:
      consecutive5xxErrors: 5
      interval: 10s
      baseEjectionTime: 30s
  subsets:
  - name: v1
    labels:
      version: v1
  - name: v2
    labels:
      version: v2
---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: reviews
  namespace: canary-demo
spec:
  hosts:
  - reviews
  http:
  - route:
    - destination:
        host: reviews
        subset: v1
      weight: 100
    mirror:
      host: reviews
      subset: v2
    mirrorPercentage:
      value: 100.0
    timeout: 5s
    retries:
      attempts: 3
      perTryTimeout: 2s
```

```bash
# Stage 1 적용
kubectl apply -f stage1-mirroring.yaml

# 확인
kubectl get vs,dr -n canary-demo

# 트래픽 생성 및 관찰
kubectl run -n canary-demo test-client --rm -it --restart=Never \
  --image=curlimages/curl -- sh -c \
  "for i in \$(seq 1 10); do curl -s reviews:9080/reviews/0; echo; done"

# v2 로그 확인 (미러링된 요청 확인)
kubectl logs -n canary-demo -l version=v2 -c reviews --tail=20
```

### 2.4 Stage 2: 카나리 5%

```yaml
# stage2-canary-5.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: reviews
  namespace: canary-demo
spec:
  hosts:
  - reviews
  http:
  - route:
    - destination:
        host: reviews
        subset: v1
      weight: 95
    - destination:
        host: reviews
        subset: v2
      weight: 5
    timeout: 5s
    retries:
      attempts: 3
      perTryTimeout: 2s
```

```bash
# Stage 2 적용
kubectl apply -f stage2-canary-5.yaml

# 트래픽 분배 확인 (100회 요청)
kubectl run -n canary-demo traffic-test --rm -it --restart=Never \
  --image=curlimages/curl -- sh -c \
  'for i in $(seq 1 100); do curl -s reviews:9080/reviews/0 | grep -o "reviews-v[0-9]"; done | sort | uniq -c'

# 예상 출력: 약 95개 v1, 5개 v2
```

### 2.5 Stage 3-4: 카나리 25% → 50%

```yaml
# stage3-canary-25.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: reviews
  namespace: canary-demo
spec:
  hosts:
  - reviews
  http:
  - route:
    - destination:
        host: reviews
        subset: v1
      weight: 75
    - destination:
        host: reviews
        subset: v2
      weight: 25
    timeout: 5s
    retries:
      attempts: 3
---
# stage4-canary-50.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: reviews
  namespace: canary-demo
spec:
  hosts:
  - reviews
  http:
  - route:
    - destination:
        host: reviews
        subset: v1
      weight: 50
    - destination:
        host: reviews
        subset: v2
      weight: 50
    timeout: 5s
    retries:
      attempts: 3
```

```bash
# 단계별 적용 및 확인
kubectl apply -f stage3-canary-25.yaml
# ... 메트릭 확인 후 ...
kubectl apply -f stage4-canary-50.yaml

# 각 단계에서 트래픽 분배 확인
kubectl run -n canary-demo traffic-test --rm -it --restart=Never \
  --image=curlimages/curl -- sh -c \
  'for i in $(seq 1 100); do curl -s reviews:9080/reviews/0 | grep -o "reviews-v[0-9]"; done | sort | uniq -c'
```

### 2.6 Stage 5: 100% v2 전환

```yaml
# stage5-full-v2.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: reviews
  namespace: canary-demo
spec:
  hosts:
  - reviews
  http:
  - route:
    - destination:
        host: reviews
        subset: v2
      weight: 100
    timeout: 5s
    retries:
      attempts: 3
```

```bash
# 100% v2 적용
kubectl apply -f stage5-full-v2.yaml

# 확인
kubectl run -n canary-demo traffic-test --rm -it --restart=Never \
  --image=curlimages/curl -- sh -c \
  'for i in $(seq 1 20); do curl -s reviews:9080/reviews/0 | grep -o "reviews-v[0-9]"; done | sort | uniq -c'

# v1 Deployment 정리 (선택)
# kubectl delete deployment reviews-v1 -n canary-demo
```

---

## 🛠️ Part 3: 트러블슈팅 (45분)

### 3.1 일반적인 문제와 해결

| 문제 | 증상 | 확인 | 해결 |
|------|------|------|------|
| **라우팅 안됨** | 503 에러 | VirtualService hosts | host 이름 확인 |
| **Subset 매칭 실패** | no healthy upstream | DestinationRule subsets | label 일치 확인 |
| **Circuit Breaker 작동** | overflow | istio-proxy 로그 | connectionPool 조정 |
| **Timeout** | upstream timeout | VirtualService timeout | 값 증가 또는 앱 최적화 |

### 3.2 디버깅 명령어

```bash
# Istio 설정 분석
istioctl analyze -n canary-demo

# Proxy 상태 확인
istioctl proxy-status

# 특정 Pod의 Envoy 설정 확인
istioctl proxy-config routes $(kubectl get pod -n canary-demo -l app=reviews -o jsonpath='{.items[0].metadata.name}') -n canary-demo

# 클러스터 설정
istioctl proxy-config clusters $(kubectl get pod -n canary-demo -l app=reviews -o jsonpath='{.items[0].metadata.name}') -n canary-demo

# 엔드포인트 확인
istioctl proxy-config endpoints $(kubectl get pod -n canary-demo -l app=reviews -o jsonpath='{.items[0].metadata.name}') -n canary-demo | grep reviews

# Envoy 로그 확인
kubectl logs -n canary-demo -l app=reviews -c istio-proxy --tail=50

# Pod 상세 (Istio 설정 포함)
istioctl describe pod $(kubectl get pod -n canary-demo -l app=reviews -o jsonpath='{.items[0].metadata.name}') -n canary-demo
```

### 3.3 롤백 절차

```bash
# 문제 발생 시 즉시 롤백

# 방법 1: 이전 VirtualService 적용
kubectl apply -f stage2-canary-5.yaml  # 또는 원하는 단계

# 방법 2: VirtualService 삭제 (기본 라우팅)
kubectl delete vs reviews -n canary-demo

# 방법 3: 트래픽 완전 차단 후 복구
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: reviews
  namespace: canary-demo
spec:
  hosts:
  - reviews
  http:
  - route:
    - destination:
        host: reviews
        subset: v1
      weight: 100
EOF
```

---

## ✅ Week 9 최종 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | Istio 설치 및 구조 이해 | istiod, sidecar injection | ☐ |
| 2 | VirtualService 작성 | 라우팅, 가중치, 매칭 | ☐ |
| 3 | DestinationRule 작성 | subset, trafficPolicy | ☐ |
| 4 | Gateway 설정 | 외부 트래픽 진입점 | ☐ |
| 5 | Timeout/Retry 설정 | 장애 복원력 | ☐ |
| 6 | Fault Injection | delay, abort | ☐ |
| 7 | Traffic Mirroring | 안전한 테스트 | ☐ |
| 8 | 카나리 배포 구현 | 단계별 롤아웃 | ☐ |
| 9 | 트러블슈팅 | istioctl 명령어 | ☐ |

---

## 🔑 핵심 명령어 정리

```bash
# === Istio 리소스 관리 ===
kubectl get vs,dr,gw -A              # 모든 Istio 리소스
kubectl describe vs <name>           # VirtualService 상세

# === istioctl 디버깅 ===
istioctl analyze                     # 설정 분석
istioctl proxy-status                # Proxy 동기화 상태
istioctl proxy-config routes <pod>   # 라우팅 설정
istioctl proxy-config clusters <pod> # 클러스터 설정
istioctl proxy-config endpoints <pod> # 엔드포인트
istioctl describe pod <pod>          # Pod Istio 설정

# === 로그 ===
kubectl logs -l app=reviews -c istio-proxy  # Envoy 로그
kubectl logs -l app=reviews -c reviews      # 앱 로그
```

---

## 💡 면접 대비 핵심 Q&A

### Q1: Istio에서 카나리 배포를 어떻게 구현하나요?

**A**: "VirtualService의 weight를 사용합니다. 먼저 DestinationRule에서 버전별 subset(v1, v2)을 정의하고, VirtualService에서 각 subset에 트래픽 비율을 지정합니다. 5% → 25% → 50% → 100% 순으로 점진적으로 늘리며, 각 단계에서 메트릭을 모니터링합니다."

### Q2: Traffic Mirroring의 장점은?

**A**: "실제 프로덕션 트래픽의 복사본을 새 버전으로 보내 테스트할 수 있습니다. 사용자에게 영향 없이 실제 트래픽 패턴으로 새 버전을 검증할 수 있어, 카나리 배포 전 단계로 활용합니다."

### Q3: VirtualService와 DestinationRule의 역할 차이는?

**A**: "VirtualService는 '어디로 보낼지'(라우팅)를 정의하고, DestinationRule은 '어떻게 보낼지'(연결 정책, 로드밸런싱)를 정의합니다. VirtualService는 트래픽 매칭과 분배를, DestinationRule은 서비스 버전 정의와 연결 관리를 담당합니다."

---

## 📝 학습 기록

```
Week 9 완료일: ____년 __월 __일
실제 소요 시간: ____시간

종합 실습 완료:
- [ ] Traffic Mirroring
- [ ] 카나리 5%/25%/50%
- [ ] 100% 전환
- [ ] 롤백 테스트

가장 어려웠던 부분:


추가 학습 필요 항목:

```

---

## ➡️ 다음 학습: Day 68

**주제**: Istio Observability - Kiali
- Kiali 설치 및 접속
- 서비스 맵 시각화
- 트래픽 흐름 모니터링
