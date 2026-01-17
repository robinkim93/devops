# 📅 Day 87: Istio 장애 복원력 테스트

## 🎯 오늘의 목표

> **토스플레이스 핵심**: Circuit Breaker, Retry, Timeout으로 서비스의 장애 복원력을 강화합니다.

대규모 트래픽 환경에서 장애 전파를 방지하고 시스템 안정성을 확보합니다.

---

## ⏰ 예상 소요 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 이론 | 1시간 | 복원력 패턴 이해 |
| Circuit Breaker | 1시간 30분 | 구현 및 테스트 |
| Retry/Timeout | 1시간 | 구현 및 테스트 |
| 통합 테스트 | 30분 | 전체 검증 |

---

## 📚 Part 1: 장애 복원력 패턴

### 패턴 개요

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     Resilience Patterns in Istio                             │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   1. Timeout - 응답 시간 제한                                                │
│   ┌─────────┐   요청   ┌─────────┐                                          │
│   │ Client  │ ────────▶│ Service │  3초 내 응답 없으면 실패                  │
│   └─────────┘          └─────────┘                                          │
│                                                                              │
│   2. Retry - 자동 재시도                                                     │
│   ┌─────────┐   실패   ┌─────────┐                                          │
│   │ Client  │ ────────▶│ Service │  5xx 에러 시 최대 3회 재시도              │
│   └─────────┘ ◀──재시도─┘─────────┘                                          │
│                                                                              │
│   3. Circuit Breaker - 장애 격리                                             │
│   ┌─────────┐          ┌─────────┐                                          │
│   │ Client  │    ✗     │ Service │  연속 5회 실패 → 30초간 차단              │
│   └─────────┘──────────┘─────────┘                                          │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 왜 필요한가?

| 문제 | 해결책 | 효과 |
|------|--------|------|
| 느린 서비스가 전체 지연 유발 | **Timeout** | 빠른 실패, 리소스 해제 |
| 일시적 네트워크 오류 | **Retry** | 자동 복구 |
| 장애 서비스로 트래픽 전파 | **Circuit Breaker** | 장애 격리 |

---

## 🛠️ Part 2: Timeout 설정

### VirtualService에서 Timeout

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: backend
  namespace: istio-portfolio
spec:
  hosts:
  - backend
  http:
  - timeout: 3s  # 3초 내 응답 없으면 실패
    route:
    - destination:
        host: backend
        subset: v1
```

### 테스트

```bash
# 느린 응답을 시뮬레이션하는 서비스 배포
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: slow-service
spec:
  hosts:
  - backend
  http:
  - fault:
      delay:
        percentage:
          value: 100
        fixedDelay: 5s  # 5초 지연
    timeout: 3s         # 3초 timeout
    route:
    - destination:
        host: backend
EOF

# 요청 테스트
kubectl exec -it deploy/frontend -n istio-portfolio -- \
  curl -w "\nTime: %{time_total}s\n" http://backend/api

# 예상: 3초 후 timeout 에러
```

---

## 🛠️ Part 3: Retry 설정

### VirtualService에서 Retry

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: api
  namespace: istio-portfolio
spec:
  hosts:
  - api
  http:
  - route:
    - destination:
        host: api
        subset: v1
    retries:
      attempts: 3           # 최대 3회 재시도
      perTryTimeout: 2s     # 각 시도당 2초 제한
      retryOn: 5xx,reset,connect-failure,retriable-4xx
```

### retryOn 옵션

| 옵션 | 설명 |
|------|------|
| `5xx` | 5xx 서버 에러 |
| `reset` | 연결 리셋 |
| `connect-failure` | 연결 실패 |
| `retriable-4xx` | 재시도 가능한 4xx |
| `gateway-error` | 502, 503, 504 |

### Retry 테스트

```bash
# 50% 확률로 500 에러 주입
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: api-fault-test
  namespace: istio-portfolio
spec:
  hosts:
  - api
  http:
  - fault:
      abort:
        percentage:
          value: 50
        httpStatus: 500
    route:
    - destination:
        host: api
        subset: v1
    retries:
      attempts: 3
      perTryTimeout: 2s
      retryOn: 5xx
EOF

# 100회 요청 테스트
for i in $(seq 1 100); do
  curl -s -o /dev/null -w "%{http_code}\n" http://api.istio-portfolio.svc/health
done | sort | uniq -c

# 예상: 재시도 덕분에 대부분 성공 (200)
```

---

## 🛠️ Part 4: Circuit Breaker 설정

### DestinationRule에서 Circuit Breaker

```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: api-circuit-breaker
  namespace: istio-portfolio
spec:
  host: api
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100       # 최대 TCP 연결 수
      http:
        h2UpgradePolicy: UPGRADE
        http1MaxPendingRequests: 100  # 대기 요청 수
        http2MaxRequests: 1000        # 최대 동시 요청
        maxRequestsPerConnection: 10  # 연결당 최대 요청
    
    outlierDetection:
      consecutive5xxErrors: 5     # 연속 5회 5xx 에러 시
      interval: 10s               # 10초 간격으로 검사
      baseEjectionTime: 30s       # 30초간 제외
      maxEjectionPercent: 100     # 최대 100%까지 제외 가능
      minHealthPercent: 0         # 최소 헬스 비율
```

### Circuit Breaker 테스트

```bash
# 부하 생성 도구 설치
kubectl run fortio --image=fortio/fortio --rm -it \
  -n istio-portfolio -- load -qps 50 -n 1000 \
  http://api:80/

# 결과 분석:
# - Code 200 : 정상 응답
# - Code 503 : Circuit Breaker 발동
```

### Circuit Breaker 상태 확인

```bash
# Proxy 상태 확인
kubectl exec -it deploy/frontend -n istio-portfolio -c istio-proxy -- \
  pilot-agent request GET clusters | grep api | grep outlier

# Kiali에서 시각적으로 확인
# - Circuit이 열린 Pod는 빨간색으로 표시
```

---

## 🛠️ Part 5: 통합 Fault Injection 테스트

### 종합 테스트 설정

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: api-resilience-test
  namespace: istio-portfolio
spec:
  hosts:
  - api
  http:
  - fault:
      delay:
        percentage:
          value: 10    # 10% 요청에 지연
        fixedDelay: 3s
      abort:
        percentage:
          value: 10    # 10% 요청에 500 에러
        httpStatus: 500
    timeout: 5s
    route:
    - destination:
        host: api
        subset: v1
    retries:
      attempts: 3
      perTryTimeout: 2s
      retryOn: 5xx
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: api-dr
  namespace: istio-portfolio
spec:
  host: api
  trafficPolicy:
    outlierDetection:
      consecutive5xxErrors: 3
      interval: 5s
      baseEjectionTime: 30s
```

### 결과 분석 (Kiali, Jaeger)

```bash
# Kiali에서 확인
kubectl port-forward svc/kiali -n istio-system 20001:20001
# 브라우저: http://localhost:20001

# 확인 사항:
# - 에러율 (Response Flags)
# - 트래픽 흐름
# - Circuit Breaker 상태

# Jaeger에서 트레이스 확인
kubectl port-forward svc/tracing -n istio-system 16686:16686
# 브라우저: http://localhost:16686

# 확인 사항:
# - 재시도 횟수
# - 각 시도별 응답 시간
# - 에러 발생 지점
```

---

## 📊 모니터링 메트릭

```promql
# 요청 성공률
sum(rate(istio_requests_total{response_code=~"2.*"}[5m])) 
/ sum(rate(istio_requests_total[5m]))

# Circuit Breaker 발동 횟수
sum(rate(istio_requests_total{response_flags="UO"}[5m]))

# Retry 횟수
sum(rate(envoy_cluster_upstream_rq_retry[5m]))

# Timeout 횟수
sum(rate(istio_requests_total{response_flags="UT"}[5m]))
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | Timeout 설정 | ☐ |
| 2 | Retry 설정 | ☐ |
| 3 | Circuit Breaker 설정 | ☐ |
| 4 | Fault Injection 테스트 | ☐ |
| 5 | Kiali에서 확인 | ☐ |
| 6 | Jaeger에서 트레이스 확인 | ☐ |

---

## 🔑 핵심 설정 요약

```yaml
# Timeout, Retry → VirtualService
spec:
  http:
  - timeout: 3s
    retries:
      attempts: 3
      perTryTimeout: 2s
      retryOn: 5xx

# Circuit Breaker → DestinationRule
spec:
  trafficPolicy:
    outlierDetection:
      consecutive5xxErrors: 5
      interval: 10s
      baseEjectionTime: 30s
```

---

## ➡️ 다음 학습: Day 88

**주제**: 포트폴리오 문서화

