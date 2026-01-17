# 📅 Day 80: Rate Limiting

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "대규모 실시간 트래픽"을 안정적으로 처리하기 위한 트래픽 제한
> Envoy Rate Limit으로 서비스를 과부하로부터 보호

토스플레이스는 오프라인 결제라는 실시간 트래픽이 많은 서비스를 운영합니다. Rate Limiting은 시스템 안정성을 위한 필수 기술입니다.

---

## ⏰ 예상 학습 시간: 3.5시간

---

## 📚 Part 1: Rate Limiting 개념 심화 (1시간)

### 1.1 Rate Limiting이란?

Rate Limiting은 일정 시간 동안 허용되는 요청 수를 제한하는 기술입니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│  Rate Limiting 개념                                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│   Client ──▶ [Rate Limiter] ──▶ Service                             │
│                    │                                                 │
│                    ├── 허용된 요청 → 200 OK                          │
│                    └── 제한된 요청 → 429 Too Many Requests           │
│                                                                      │
│   시간별 요청 수:                                                    │
│   ┌────────────────────────────────────────────────────┐            │
│   │  ████████████████  (16 req)  ← 초과                 │            │
│   │  ██████████        (10 req)  ← 임계값 (limit)      │            │
│   │  ██████            (6 req)   ← 정상                 │            │
│   │  ████              (4 req)   ← 정상                 │            │
│   └────────────────────────────────────────────────────┘            │
│        10:00   10:01   10:02   10:03   10:04 (분)                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 왜 Rate Limiting이 필요한가?

| 목적 | 설명 | 토스플레이스 적용 |
|------|------|------------------|
| DDoS 방어 | 악의적인 과도한 요청 차단 | 결제 API 보호 |
| 리소스 보호 | 백엔드 서비스 과부하 방지 | DB 연결 보호 |
| 공정한 사용 | 사용자/테넌트별 제한 | API 사용량 제어 |
| 비용 절감 | 불필요한 리소스 사용 방지 | 클라우드 비용 최적화 |
| 서비스 품질 | 중요 요청 우선 처리 | 결제 > 조회 우선순위 |

### 1.3 Rate Limiting 알고리즘

```
┌─────────────────────────────────────────────────────────────────────┐
│  Rate Limiting 알고리즘                                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. Token Bucket (Istio/Envoy 사용)                                 │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  버킷에 토큰이 일정 속도로 채워짐                            │    │
│  │  요청마다 토큰 1개 소비                                      │    │
│  │  토큰 없으면 요청 거부                                       │    │
│  │                                                              │    │
│  │  장점: 버스트 트래픽 허용 (버킷 크기만큼)                    │    │
│  │        구현 간단, 메모리 효율적                              │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  2. Leaky Bucket                                                    │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  일정한 속도로만 요청 처리 (누출)                            │    │
│  │  초과 요청은 큐에 대기 또는 버림                             │    │
│  │                                                              │    │
│  │  장점: 출력 속도 일정                                        │    │
│  │  단점: 버스트 처리 불가                                      │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  3. Fixed Window                                                    │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  고정 시간 윈도우 내 요청 수 카운트                          │    │
│  │  예: 1분에 100개 요청 허용                                   │    │
│  │                                                              │    │
│  │  장점: 구현 매우 간단                                        │    │
│  │  단점: 윈도우 경계에서 2배 트래픽 가능                       │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  4. Sliding Window                                                  │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  이동하는 시간 윈도우                                        │    │
│  │  Fixed Window의 경계 문제 해결                               │    │
│  │                                                              │    │
│  │  장점: 더 정확한 제한                                        │    │
│  │  단점: 구현 복잡, 메모리 사용 증가                           │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.4 Istio Rate Limiting 방식

| 방식 | 설명 | 장단점 |
|------|------|--------|
| **Local Rate Limit** | 각 Envoy 프록시에서 독립적으로 처리 | 빠름, 설정 간단, 전체 제한 부정확 |
| **Global Rate Limit** | 중앙 서비스(Redis)에서 공유 상태 | 정확한 전체 제한, 지연시간 증가 |

```
┌─────────────────────────────────────────────────────────────────────┐
│  Local vs Global Rate Limiting                                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Local Rate Limit:                                                  │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Pod-A (Envoy) ──── 10 req/min limit                        │    │
│  │  Pod-B (Envoy) ──── 10 req/min limit                        │    │
│  │  Pod-C (Envoy) ──── 10 req/min limit                        │    │
│  │  ───────────────────────────────────                        │    │
│  │  총 허용: 최대 30 req/min (Pod당 독립)                      │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Global Rate Limit:                                                 │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Pod-A (Envoy) ─┐                                           │    │
│  │  Pod-B (Envoy) ─┼──▶ [Redis] ──── 10 req/min limit (공유)  │    │
│  │  Pod-C (Envoy) ─┘                                           │    │
│  │  ───────────────────────────────────                        │    │
│  │  총 허용: 정확히 10 req/min (전체 합산)                     │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: Local Rate Limit 실습 (1.5시간)

### 실습 1: 테스트 환경 확인

```bash
# Bookinfo 앱이 실행 중인지 확인
kubectl get pods -n default -l app=productpage

# 없다면 설치
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/bookinfo/platform/kube/bookinfo.yaml

# Pod가 Ready 될 때까지 대기
kubectl wait --for=condition=Ready pods -l app=productpage --timeout=120s
```

### 실습 2: Local Rate Limit EnvoyFilter

```yaml
# rate-limit-local.yaml
apiVersion: networking.istio.io/v1alpha3
kind: EnvoyFilter
metadata:
  name: local-ratelimit
  namespace: default
spec:
  workloadSelector:
    labels:
      app: productpage  # productpage에만 적용
  configPatches:
  # HTTP Connection Manager에 Rate Limit 필터 추가
  - applyTo: HTTP_FILTER
    match:
      context: SIDECAR_INBOUND
      listener:
        filterChain:
          filter:
            name: "envoy.filters.network.http_connection_manager"
            subFilter:
              name: "envoy.filters.http.router"
    patch:
      operation: INSERT_BEFORE
      value:
        name: envoy.filters.http.local_ratelimit
        typed_config:
          "@type": type.googleapis.com/udpa.type.v1.TypedStruct
          type_url: type.googleapis.com/envoy.extensions.filters.http.local_ratelimit.v3.LocalRateLimit
          value:
            stat_prefix: http_local_rate_limiter
            # Token Bucket 설정
            token_bucket:
              max_tokens: 10         # 버킷 최대 토큰 수
              tokens_per_fill: 10    # 리필당 토큰 수
              fill_interval: 60s     # 60초마다 리필
            # 필터 활성화 비율 (100%)
            filter_enabled:
              runtime_key: local_rate_limit_enabled
              default_value:
                numerator: 100
                denominator: HUNDRED
            # 필터 적용 비율 (100%)
            filter_enforced:
              runtime_key: local_rate_limit_enforced
              default_value:
                numerator: 100
                denominator: HUNDRED
            # Rate Limit 시 추가할 응답 헤더
            response_headers_to_add:
              - append: false
                header:
                  key: x-rate-limited
                  value: "true"
              - append: false
                header:
                  key: x-rate-limit-limit
                  value: "10"
```

```bash
# EnvoyFilter 적용
kubectl apply -f rate-limit-local.yaml

# 적용 확인
kubectl get envoyfilter -n default
```

### 실습 3: Rate Limit 테스트

```bash
# productpage 접근 URL 설정
export PRODUCTPAGE=$(kubectl get svc productpage -o jsonpath='{.spec.clusterIP}')

# 또는 Ingress Gateway 사용 시
export GATEWAY_URL=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

# 단일 요청 테스트
curl -s -o /dev/null -w "Status: %{http_code}\n" http://$PRODUCTPAGE:9080/productpage

# 빠르게 여러 요청 전송 (11개 이상 전송)
echo "=== Rate Limit 테스트 (20개 요청) ==="
for i in {1..20}; do
  status=$(curl -s -o /dev/null -w "%{http_code}" http://$PRODUCTPAGE:9080/productpage)
  echo "Request $i: $status"
  sleep 0.1
done

# 예상 결과:
# Request 1-10: 200 (정상)
# Request 11-20: 429 (Too Many Requests)
```

### 실습 4: Rate Limit 상태 확인

```bash
# Envoy 통계 확인
kubectl exec -it $(kubectl get pod -l app=productpage -o jsonpath='{.items[0].metadata.name}') -c istio-proxy -- \
  curl -s localhost:15000/stats | grep ratelimit

# 예상 출력:
# http_local_rate_limiter.http_local_rate_limit.enabled: 1
# http_local_rate_limiter.http_local_rate_limit.enforced: 1
# http_local_rate_limiter.http_local_rate_limit.ok: 10
# http_local_rate_limiter.http_local_rate_limit.rate_limited: 10

# Rate Limit 헤더 확인
curl -v http://$PRODUCTPAGE:9080/productpage 2>&1 | grep -i "x-rate"
```

### 실습 5: 정리

```bash
# EnvoyFilter 삭제
kubectl delete envoyfilter local-ratelimit -n default
```

---

## 🛠️ Part 3: 고급 Rate Limit 설정 (1시간)

### 3.1 경로별 Rate Limit

```yaml
# rate-limit-path-based.yaml
apiVersion: networking.istio.io/v1alpha3
kind: EnvoyFilter
metadata:
  name: path-based-ratelimit
  namespace: default
spec:
  workloadSelector:
    labels:
      app: productpage
  configPatches:
  - applyTo: HTTP_FILTER
    match:
      context: SIDECAR_INBOUND
      listener:
        filterChain:
          filter:
            name: "envoy.filters.network.http_connection_manager"
            subFilter:
              name: "envoy.filters.http.router"
    patch:
      operation: INSERT_BEFORE
      value:
        name: envoy.filters.http.local_ratelimit
        typed_config:
          "@type": type.googleapis.com/udpa.type.v1.TypedStruct
          type_url: type.googleapis.com/envoy.extensions.filters.http.local_ratelimit.v3.LocalRateLimit
          value:
            stat_prefix: http_local_rate_limiter
            token_bucket:
              max_tokens: 5
              tokens_per_fill: 5
              fill_interval: 60s
            filter_enabled:
              runtime_key: local_rate_limit_enabled
              default_value:
                numerator: 100
                denominator: HUNDRED
            filter_enforced:
              runtime_key: local_rate_limit_enforced
              default_value:
                numerator: 100
                denominator: HUNDRED
            # 특정 경로에만 적용
            request_headers_to_add_when_not_enforced:
              - header:
                  key: x-rate-limit-status
                  value: "not-enforced"
```

### 3.2 헤더 기반 Rate Limit

```yaml
# rate-limit-header-based.yaml
apiVersion: networking.istio.io/v1alpha3
kind: EnvoyFilter
metadata:
  name: header-based-ratelimit
  namespace: default
spec:
  workloadSelector:
    labels:
      app: productpage
  configPatches:
  - applyTo: HTTP_FILTER
    match:
      context: SIDECAR_INBOUND
      listener:
        filterChain:
          filter:
            name: "envoy.filters.network.http_connection_manager"
            subFilter:
              name: "envoy.filters.http.router"
    patch:
      operation: INSERT_BEFORE
      value:
        name: envoy.filters.http.local_ratelimit
        typed_config:
          "@type": type.googleapis.com/udpa.type.v1.TypedStruct
          type_url: type.googleapis.com/envoy.extensions.filters.http.local_ratelimit.v3.LocalRateLimit
          value:
            stat_prefix: http_local_rate_limiter
            token_bucket:
              max_tokens: 100
              tokens_per_fill: 100
              fill_interval: 60s
            filter_enabled:
              runtime_key: local_rate_limit_enabled
              default_value:
                numerator: 100
                denominator: HUNDRED
            filter_enforced:
              runtime_key: local_rate_limit_enforced
              default_value:
                numerator: 100
                denominator: HUNDRED
            # Descriptor 기반 Rate Limit (헤더별 다른 제한)
            descriptors:
              - entries:
                  - key: api-tier
                    value: premium
                token_bucket:
                  max_tokens: 1000
                  tokens_per_fill: 1000
                  fill_interval: 60s
              - entries:
                  - key: api-tier
                    value: basic
                token_bucket:
                  max_tokens: 100
                  tokens_per_fill: 100
                  fill_interval: 60s
```

### 3.3 VirtualService를 활용한 Rate Limit

```yaml
# rate-limit-virtualservice.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: productpage-vs
  namespace: default
spec:
  hosts:
  - productpage
  http:
  # /api/* 경로에 대해 더 엄격한 제한
  - match:
    - uri:
        prefix: /api/
    headers:
      x-rate-limit-tier:
        exact: premium
    route:
    - destination:
        host: productpage
        port:
          number: 9080
    # Premium tier: 높은 제한
    retries:
      attempts: 3
      perTryTimeout: 2s
  - match:
    - uri:
        prefix: /api/
    route:
    - destination:
        host: productpage
        port:
          number: 9080
    # Basic tier: 낮은 제한 (EnvoyFilter와 함께 사용)
    retries:
      attempts: 1
      perTryTimeout: 1s
  - route:
    - destination:
        host: productpage
        port:
          number: 9080
```

### 3.4 Global Rate Limit 개요

Global Rate Limit은 외부 Rate Limit 서비스(Redis)를 사용하여 전체 클러스터에서 공유되는 제한을 적용합니다.

```yaml
# global-ratelimit-service.yaml (참고용)
apiVersion: v1
kind: ConfigMap
metadata:
  name: ratelimit-config
  namespace: istio-system
data:
  config.yaml: |
    domain: productpage-ratelimit
    descriptors:
      - key: PATH
        value: "/api/"
        rate_limit:
          unit: minute
          requests_per_unit: 100
      - key: PATH
        rate_limit:
          unit: minute
          requests_per_unit: 500
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ratelimit
  namespace: istio-system
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ratelimit
  template:
    metadata:
      labels:
        app: ratelimit
    spec:
      containers:
      - name: ratelimit
        image: envoyproxy/ratelimit:v1.4.0
        env:
        - name: LOG_LEVEL
          value: debug
        - name: REDIS_SOCKET_TYPE
          value: tcp
        - name: REDIS_URL
          value: redis:6379
        - name: USE_STATSD
          value: "false"
        - name: RUNTIME_ROOT
          value: /data
        - name: RUNTIME_SUBDIRECTORY
          value: ratelimit
        ports:
        - containerPort: 8080
        - containerPort: 8081
        - containerPort: 6070
        volumeMounts:
        - name: config
          mountPath: /data/ratelimit/config
      volumes:
      - name: config
        configMap:
          name: ratelimit-config
```

---

## 📊 Part 4: 모니터링 및 알림 (30분)

### 4.1 Rate Limit 메트릭

```bash
# Envoy Rate Limit 메트릭 확인
kubectl exec -it $(kubectl get pod -l app=productpage -o jsonpath='{.items[0].metadata.name}') -c istio-proxy -- \
  curl -s localhost:15000/stats | grep -E "ratelimit|429"

# Prometheus 쿼리 예시
# 429 응답 수
istio_requests_total{response_code="429"}

# Rate Limited 비율
sum(rate(istio_requests_total{response_code="429"}[5m])) / 
sum(rate(istio_requests_total[5m])) * 100
```

### 4.2 Grafana 대시보드

```bash
# Grafana 접근
kubectl port-forward svc/grafana -n istio-system 3000:3000

# Rate Limit 관련 패널 추가
# - 429 응답 수 (시간별)
# - Rate Limited 비율
# - 서비스별 Rate Limit 상태
```

### 4.3 알림 설정 (Alertmanager)

```yaml
# rate-limit-alerts.yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: rate-limit-alerts
  namespace: istio-system
spec:
  groups:
  - name: rate-limit
    rules:
    - alert: HighRateLimitRate
      expr: |
        sum(rate(istio_requests_total{response_code="429"}[5m])) by (destination_service) /
        sum(rate(istio_requests_total[5m])) by (destination_service) > 0.1
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "High rate limit rate for {{ $labels.destination_service }}"
        description: "Rate limited requests exceed 10% for 5 minutes"
    - alert: RateLimitSpikeDetected
      expr: |
        sum(increase(istio_requests_total{response_code="429"}[1m])) by (destination_service) > 100
      for: 1m
      labels:
        severity: critical
      annotations:
        summary: "Rate limit spike detected for {{ $labels.destination_service }}"
        description: "More than 100 rate limited requests in 1 minute"
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | Rate Limiting 개념 이해 | ☐ |
| 2 | Token Bucket 알고리즘 이해 | ☐ |
| 3 | Local vs Global Rate Limit 차이 이해 | ☐ |
| 4 | Local Rate Limit EnvoyFilter 설정 | ☐ |
| 5 | Rate Limit 테스트 (429 응답 확인) | ☐ |
| 6 | Envoy 통계로 Rate Limit 모니터링 | ☐ |
| 7 | 고급 설정 (경로별, 헤더별) 이해 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# EnvoyFilter 적용
kubectl apply -f rate-limit-local.yaml

# Rate Limit 테스트
for i in {1..20}; do curl -s -o /dev/null -w "%{http_code}\n" http://service/; done

# Envoy Rate Limit 통계 확인
kubectl exec -it <pod> -c istio-proxy -- curl localhost:15000/stats | grep ratelimit

# EnvoyFilter 확인
kubectl get envoyfilter -A
```

---

## 💡 면접 대비 핵심 포인트

### Q1: Rate Limiting이 왜 필요한가요?
**A**: 
- DDoS 공격 방어
- 백엔드 서비스 과부하 방지
- 사용자별/테넌트별 공정한 리소스 사용
- 비용 절감 (불필요한 트래픽 차단)

### Q2: Local vs Global Rate Limit의 차이점은?
**A**:
- **Local**: 각 Pod에서 독립적 처리, 빠르지만 전체 제한 부정확
- **Global**: Redis 등 공유 상태 사용, 정확하지만 지연시간 증가
- 정확성이 중요하면 Global, 성능이 중요하면 Local 선택

### Q3: Token Bucket 알고리즘은 어떻게 동작하나요?
**A**:
- 버킷에 토큰이 일정 속도로 채워짐 (예: 60초마다 10개)
- 요청마다 토큰 1개 소비
- 토큰이 없으면 요청 거부 (429)
- 버스트 트래픽 허용 (버킷 크기만큼)

### Q4: 429 응답을 받는 클라이언트는 어떻게 대응해야 하나요?
**A**:
- Retry-After 헤더 확인 후 재시도
- Exponential Backoff 적용
- Circuit Breaker 패턴 함께 사용
- 클라이언트 측 요청 큐잉

---

## 🔗 참고 자료

- [Envoy Rate Limiting](https://www.envoyproxy.io/docs/envoy/latest/intro/arch_overview/other_features/global_rate_limiting)
- [Istio EnvoyFilter](https://istio.io/latest/docs/reference/config/networking/envoy-filter/)
- [Rate Limiting Best Practices](https://cloud.google.com/architecture/rate-limiting-strategies-techniques)

---

## ➡️ 다음 학습: Day 81

**주제**: Week 11 복습
- Rate Limiting, Circuit Breaker, Retry 종합 복습
- 트래픽 제어 전략 통합
- 실전 시나리오 테스트
