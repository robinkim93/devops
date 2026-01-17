# 📅 Day 84: 트래픽 관리 (카나리 배포)

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "더 빠르고 안전하게 실험하고 배포"
> VirtualService, DestinationRule로 카나리 배포 구현

토스플레이스에서는 오프라인 결제 서비스의 안정성이 매우 중요합니다. 카나리 배포를 통해 새 버전을 점진적으로 배포하여 위험을 최소화합니다.

---

## ⏰ 예상 소요 시간: 4시간

---

## 📚 Part 1: 카나리 배포 개념 (1시간)

### 1.1 카나리 배포란?

카나리 배포는 새 버전을 일부 트래픽에만 먼저 노출하여 안전하게 검증 후 점진적으로 확대하는 배포 전략입니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│  카나리 배포 개념                                                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  "Canary in a coal mine" (탄광 속 카나리아)                         │
│  - 위험 감지를 위해 먼저 내보내는 것                                │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Stage 1: 10% 트래픽                                        │    │
│  │  ██████████░ (v1: 90%)                                      │    │
│  │  █░░░░░░░░░░ (v2: 10%)  ← 문제 발견 시 빠른 롤백            │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                         │                                           │
│                         ▼ 성공 시                                   │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Stage 2: 50% 트래픽                                        │    │
│  │  █████░░░░░░ (v1: 50%)                                      │    │
│  │  █████░░░░░░ (v2: 50%)                                      │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                         │                                           │
│                         ▼ 성공 시                                   │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Stage 3: 100% 트래픽 (완료)                                │    │
│  │  ░░░░░░░░░░░ (v1: 0%)                                       │    │
│  │  ███████████ (v2: 100%)                                     │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 배포 전략 비교

| 전략 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **Rolling Update** | 점진적 Pod 교체 | 간단, 기본 지원 | 버전 혼재, 롤백 느림 |
| **Blue/Green** | 전체 환경 교체 | 빠른 롤백, 격리 | 리소스 2배 필요 |
| **Canary** | 트래픽 비율 제어 | 위험 최소화, 유연 | 복잡, 모니터링 필수 |
| **A/B Testing** | 사용자 그룹별 분리 | 실험 가능 | 구현 복잡 |

### 1.3 Istio 트래픽 관리 구성요소

```
┌─────────────────────────────────────────────────────────────────────┐
│  Istio 트래픽 관리 흐름                                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  외부 요청                                                          │
│      │                                                              │
│      ▼                                                              │
│  ┌──────────────────┐                                               │
│  │     Gateway      │  ← 외부 트래픽 진입점                         │
│  └────────┬─────────┘                                               │
│           │                                                         │
│           ▼                                                         │
│  ┌──────────────────┐                                               │
│  │  VirtualService  │  ← 라우팅 규칙 (어디로 보낼지)                │
│  │  - 트래픽 비율   │                                               │
│  │  - 헤더 기반     │                                               │
│  │  - retry/timeout │                                               │
│  └────────┬─────────┘                                               │
│           │                                                         │
│           ▼                                                         │
│  ┌──────────────────┐                                               │
│  │ DestinationRule  │  ← 목적지 정책 (어떻게 연결할지)              │
│  │  - subset 정의   │                                               │
│  │  - 연결 풀       │                                               │
│  │  - 서킷브레이커  │                                               │
│  └────────┬─────────┘                                               │
│           │                                                         │
│      ┌────┴────┐                                                    │
│      ▼         ▼                                                    │
│  [Pod v1]  [Pod v2]                                                 │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: 기본 설정 (1시간)

### 2.1 Gateway 설정

Gateway는 외부 트래픽이 클러스터로 들어오는 진입점입니다.

```yaml
# manifests/istio/gateway.yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: api-gateway
  namespace: istio-portfolio
spec:
  selector:
    istio: ingressgateway   # Istio Ingress Gateway 선택
  servers:
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - "api.portfolio.local"
    - "*.portfolio.local"   # 서브도메인도 허용
  # HTTPS 설정 (프로덕션 권장)
  # - port:
  #     number: 443
  #     name: https
  #     protocol: HTTPS
  #   tls:
  #     mode: SIMPLE
  #     credentialName: api-tls-secret
  #   hosts:
  #   - "api.portfolio.local"
```

### 2.2 DestinationRule 설정

DestinationRule은 서비스의 subset과 트래픽 정책을 정의합니다.

```yaml
# manifests/istio/destinationrule.yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: api
  namespace: istio-portfolio
spec:
  host: api               # Kubernetes Service 이름
  
  # 전역 트래픽 정책
  trafficPolicy:
    # 연결 풀 설정 (과부하 방지)
    connectionPool:
      tcp:
        maxConnections: 100         # 최대 TCP 연결 수
        connectTimeout: 10s         # 연결 타임아웃
      http:
        h2UpgradePolicy: UPGRADE    # HTTP/2 업그레이드
        http1MaxPendingRequests: 100
        http2MaxRequests: 1000
        maxRequestsPerConnection: 100
        maxRetries: 3               # 최대 재시도
    
    # Outlier Detection (서킷브레이커)
    outlierDetection:
      consecutive5xxErrors: 5       # 5회 연속 5xx 오류
      interval: 10s                 # 검사 간격
      baseEjectionTime: 30s         # 기본 제외 시간
      maxEjectionPercent: 50        # 최대 50%까지 제외
      minHealthPercent: 30          # 최소 30% 건강한 인스턴스 유지
  
  # Subset 정의 (버전별 그룹)
  subsets:
  - name: v1
    labels:
      version: v1       # Pod label: version=v1
    trafficPolicy:
      connectionPool:
        http:
          h2UpgradePolicy: UPGRADE
  
  - name: v2
    labels:
      version: v2       # Pod label: version=v2
    trafficPolicy:
      connectionPool:
        http:
          h2UpgradePolicy: UPGRADE
```

### 2.3 VirtualService 설정 (초기: 100% v1)

```yaml
# manifests/istio/virtualservice.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: api
  namespace: istio-portfolio
spec:
  hosts:
  - "api.portfolio.local"   # Gateway에서 들어오는 외부 호스트
  - api                      # 내부 서비스 이름 (mesh 내부)
  
  gateways:
  - api-gateway             # 외부 트래픽용
  - mesh                    # 내부 트래픽용 (Sidecar 간)
  
  http:
  # 라우팅 규칙
  - match:
    - headers:
        x-canary:
          exact: "true"     # x-canary: true 헤더가 있으면 v2로
    route:
    - destination:
        host: api
        subset: v2
      weight: 100
    timeout: 5s
    retries:
      attempts: 3
      perTryTimeout: 2s
      retryOn: 5xx,reset,connect-failure,retriable-4xx
  
  # 기본 라우팅 (가중치 기반)
  - route:
    - destination:
        host: api
        subset: v1
      weight: 100           # 100% v1으로
    - destination:
        host: api
        subset: v2
      weight: 0             # 0% v2로
    
    # 타임아웃 설정
    timeout: 5s
    
    # 재시도 정책
    retries:
      attempts: 3
      perTryTimeout: 2s
      retryOn: 5xx,reset,connect-failure,retriable-4xx
```

---

## 🛠️ Part 3: 카나리 배포 실습 (1.5시간)

### 실습 1: 초기 배포

```bash
# 모든 Istio 설정 배포
kubectl apply -f manifests/istio/

# 배포 확인
kubectl get gateway,virtualservice,destinationrule -n istio-portfolio

# Pod 상태 확인
kubectl get pods -n istio-portfolio -L version

# 출력 예시:
# NAME        READY   STATUS    VERSION
# api-v1-xx   2/2     Running   v1
# api-v1-yy   2/2     Running   v1
# api-v2-xx   2/2     Running   v2
# api-v2-yy   2/2     Running   v2
```

### 실습 2: 호스트 설정 및 테스트

```bash
# /etc/hosts에 도메인 추가
echo "$(minikube ip) api.portfolio.local" | sudo tee -a /etc/hosts

# 또는 kubectl port-forward 사용
kubectl port-forward -n istio-system svc/istio-ingressgateway 8080:80 &

# 100% v1 테스트 (10회 요청)
echo "=== 초기 상태: 100% v1 ==="
for i in {1..10}; do
  curl -s http://api.portfolio.local/ 2>/dev/null || curl -s http://localhost:8080/ -H "Host: api.portfolio.local"
done

# 예상 출력: 모두 "API v1"
```

### 실습 3: 카나리 배포 Stage 1 (10% v2)

```yaml
# virtualservice-canary-10.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: api
  namespace: istio-portfolio
spec:
  hosts:
  - "api.portfolio.local"
  - api
  gateways:
  - api-gateway
  - mesh
  http:
  - match:
    - headers:
        x-canary:
          exact: "true"
    route:
    - destination:
        host: api
        subset: v2
      weight: 100
    timeout: 5s
    retries:
      attempts: 3
      perTryTimeout: 2s
  - route:
    - destination:
        host: api
        subset: v1
      weight: 90            # 90% v1
    - destination:
        host: api
        subset: v2
      weight: 10            # 10% v2
    timeout: 5s
    retries:
      attempts: 3
      perTryTimeout: 2s
```

```bash
# Stage 1 적용
kubectl apply -f virtualservice-canary-10.yaml

# 테스트 (20회 요청)
echo "=== Stage 1: 90% v1, 10% v2 ==="
for i in {1..20}; do
  curl -s http://localhost:8080/ -H "Host: api.portfolio.local"
done | sort | uniq -c

# 예상 출력:
# 18 API v1
# 2 API v2

# 강제로 v2 테스트 (헤더 기반)
curl -s http://localhost:8080/ -H "Host: api.portfolio.local" -H "x-canary: true"
# 출력: API v2
```

### 실습 4: 카나리 배포 Stage 2 (50% v2)

```yaml
# virtualservice-canary-50.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: api
  namespace: istio-portfolio
spec:
  hosts:
  - "api.portfolio.local"
  - api
  gateways:
  - api-gateway
  - mesh
  http:
  - match:
    - headers:
        x-canary:
          exact: "true"
    route:
    - destination:
        host: api
        subset: v2
      weight: 100
    timeout: 5s
    retries:
      attempts: 3
      perTryTimeout: 2s
  - route:
    - destination:
        host: api
        subset: v1
      weight: 50            # 50% v1
    - destination:
        host: api
        subset: v2
      weight: 50            # 50% v2
    timeout: 5s
    retries:
      attempts: 3
      perTryTimeout: 2s
```

```bash
# Stage 2 적용
kubectl apply -f virtualservice-canary-50.yaml

# 테스트 (20회 요청)
echo "=== Stage 2: 50% v1, 50% v2 ==="
for i in {1..20}; do
  curl -s http://localhost:8080/ -H "Host: api.portfolio.local"
done | sort | uniq -c

# 예상 출력:
# 10 API v1
# 10 API v2
```

### 실습 5: 카나리 배포 완료 (100% v2)

```yaml
# virtualservice-canary-100.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: api
  namespace: istio-portfolio
spec:
  hosts:
  - "api.portfolio.local"
  - api
  gateways:
  - api-gateway
  - mesh
  http:
  - route:
    - destination:
        host: api
        subset: v1
      weight: 0             # 0% v1
    - destination:
        host: api
        subset: v2
      weight: 100           # 100% v2
    timeout: 5s
    retries:
      attempts: 3
      perTryTimeout: 2s
```

```bash
# 완료 단계 적용
kubectl apply -f virtualservice-canary-100.yaml

# 테스트
echo "=== 완료: 100% v2 ==="
for i in {1..10}; do
  curl -s http://localhost:8080/ -H "Host: api.portfolio.local"
done

# 예상 출력: 모두 "API v2"
```

### 실습 6: 롤백 (문제 발생 시)

```bash
# 문제 발생 시 즉시 롤백
kubectl apply -f virtualservice-canary-10.yaml   # 10%로 축소
# 또는
kubectl apply -f manifests/istio/virtualservice.yaml  # 100% v1으로 롤백

# 롤백 확인
for i in {1..10}; do
  curl -s http://localhost:8080/ -H "Host: api.portfolio.local"
done
```

---

## 📊 Part 4: 모니터링 및 검증 (30분)

### 4.1 Kiali로 트래픽 확인

```bash
# Kiali 접속
istioctl dashboard kiali

# 확인 포인트:
# 1. Graph에서 트래픽 비율 시각화
# 2. v1, v2로 분기되는 화살표 확인
# 3. 에러율, 지연시간 확인
```

### 4.2 Prometheus 메트릭

```promql
# v1, v2별 요청 수
sum(istio_requests_total{destination_workload=~"api-v.*"}) by (destination_workload)

# 버전별 에러율
sum(rate(istio_requests_total{destination_workload=~"api-v.*",response_code=~"5.*"}[5m])) by (destination_workload) /
sum(rate(istio_requests_total{destination_workload=~"api-v.*"}[5m])) by (destination_workload)

# 버전별 평균 응답 시간
histogram_quantile(0.95,
  sum(rate(istio_request_duration_milliseconds_bucket{destination_workload=~"api-v.*"}[5m])) 
  by (destination_workload, le)
)
```

### 4.3 자동화 스크립트

```bash
#!/bin/bash
# canary-deploy.sh - 자동화된 카나리 배포

set -e

NAMESPACE="istio-portfolio"
SERVICE="api"

# 현재 상태 확인
echo "현재 VirtualService 상태:"
kubectl get vs $SERVICE -n $NAMESPACE -o yaml | grep -A5 "route:"

# 단계별 배포
read -p "Stage 1 (10% v2) 배포? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    kubectl apply -f virtualservice-canary-10.yaml
    echo "10분 대기 후 메트릭 확인..."
    sleep 600
    
    # 에러율 확인 (Prometheus 쿼리)
    # ERROR_RATE=$(curl -s "http://prometheus:9090/api/v1/query?query=..." | jq '.data.result[0].value[1]')
    # if [ $(echo "$ERROR_RATE > 0.01" | bc) -eq 1 ]; then
    #     echo "에러율 높음! 롤백합니다."
    #     kubectl apply -f manifests/istio/virtualservice.yaml
    #     exit 1
    # fi
fi

# 이하 생략...
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | 카나리 배포 개념 이해 | ☐ |
| 2 | Gateway 설정 완료 | ☐ |
| 3 | DestinationRule 설정 완료 | ☐ |
| 4 | VirtualService 설정 완료 | ☐ |
| 5 | Stage 1 (10%) 테스트 | ☐ |
| 6 | Stage 2 (50%) 테스트 | ☐ |
| 7 | Stage 3 (100%) 완료 | ☐ |
| 8 | 롤백 테스트 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# Istio 리소스 확인
kubectl get gateway,vs,dr -n istio-portfolio

# 트래픽 테스트
for i in {1..20}; do curl -s http://api.portfolio.local/; done | sort | uniq -c

# 헤더 기반 라우팅 테스트
curl -H "x-canary: true" http://api.portfolio.local/

# Kiali 대시보드
istioctl dashboard kiali
```

---

## 💡 면접 대비 핵심 포인트

### Q1: 카나리 배포란?
**A**: 새 버전을 일부 트래픽에만 먼저 노출하여 검증 후 점진적으로 확대하는 배포 전략입니다. Istio의 VirtualService로 트래픽 비율을 제어합니다.

### Q2: VirtualService와 DestinationRule의 차이?
**A**: 
- **VirtualService**: "어디로" 보낼지 (라우팅 규칙, 가중치, 매칭)
- **DestinationRule**: "어떻게" 연결할지 (subset, 연결 풀, 서킷브레이커)

### Q3: 카나리 배포의 롤백 방법은?
**A**: VirtualService의 weight를 즉시 수정하여 v1으로 100% 전환합니다. Kubernetes 리소스 변경만으로 트래픽이 즉시 전환됩니다.

---

## ➡️ 다음 학습: Day 85

**주제**: 보안 (mTLS, AuthorizationPolicy)
- mTLS 강제 모드
- 서비스 간 접근 제어
- JWT 인증
