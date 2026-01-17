# 📅 Day 74: Week 10 복습 - Istio Observability 종합

## 🎯 오늘의 목표

> **토스플레이스 핵심**: Kiali, Jaeger, Prometheus/Grafana를 활용한 마이크로서비스 관찰성을 완벽히 이해합니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 복습 | 1시간 | 관찰성 3대 축 |
| 실습 복습 | 2시간 | 도구별 실습 |
| 트러블슈팅 | 1시간 | 문제 분석 연습 |

---

## 📋 Week 10 학습 요약

| Day | 주제 | 핵심 내용 | 토스플레이스 연결 |
|-----|------|----------|-----------------|
| 68 | Kiali | 서비스 메시 시각화, 토폴로지 | 서비스 간 관계 파악 |
| 69 | Jaeger | 분산 추적, Trace/Span | 지연 원인 분석 |
| 70 | Prometheus/Grafana | Istio 메트릭, 대시보드 | SLI/SLO 모니터링 |
| 71 | Access Logging | Envoy 로그, Response Flags | 상세 요청 분석 |
| 72 | Envoy 트러블슈팅 | istioctl proxy-config | 설정 문제 디버깅 |
| 73 | 종합 실습 | 장애 분석 시나리오 | 실전 문제 해결 |

---

## 📚 Part 1: 관찰성 3대 축

### 개념 정리

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     Observability 3 Pillars                                  │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐           │
│   │    Metrics      │   │     Traces      │   │      Logs       │           │
│   │   (What)        │   │    (Where)      │   │     (Why)       │           │
│   ├─────────────────┤   ├─────────────────┤   ├─────────────────┤           │
│   │ • 요청 수       │   │ • 서비스 흐름    │   │ • 상세 에러      │           │
│   │ • 에러율        │   │ • 지연 구간      │   │ • 요청 상세      │           │
│   │ • 지연 시간     │   │ • 의존성        │   │ • 디버그 정보    │           │
│   ├─────────────────┤   ├─────────────────┤   ├─────────────────┤           │
│   │ Prometheus      │   │ Jaeger          │   │ Loki            │           │
│   │ Grafana         │   │ Zipkin          │   │ EFK             │           │
│   └─────────────────┘   └─────────────────┘   └─────────────────┘           │
│                                                                              │
│   Kiali: 세 가지를 통합하여 서비스 메시 전체를 시각화                          │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 도구별 역할

| 도구 | 역할 | 확인 가능한 정보 |
|------|------|-----------------|
| **Kiali** | 서비스 메시 시각화 | 토폴로지, 트래픽 흐름, 설정 유효성 |
| **Jaeger** | 분산 추적 | 요청 경로, 서비스별 지연, 에러 위치 |
| **Prometheus** | 메트릭 수집 | 요청량, 에러율, 지연 분포 |
| **Grafana** | 대시보드 | 시각화된 메트릭, 알림 |
| **Envoy Logs** | 상세 로그 | 개별 요청 정보, Response Flag |

---

## 🛠️ Part 2: Kiali 활용

### 접속 및 기본 사용

```bash
# Kiali 포트포워드
kubectl port-forward svc/kiali -n istio-system 20001:20001 &

# 브라우저: http://localhost:20001
```

### 주요 화면

| 메뉴 | 기능 | 활용 |
|------|------|------|
| **Graph** | 서비스 토폴로지 | 전체 흐름 파악 |
| **Applications** | 앱 상태 | 앱별 상세 정보 |
| **Workloads** | 워크로드 상태 | Pod 상태 확인 |
| **Services** | 서비스 상태 | VirtualService, DestinationRule |
| **Istio Config** | 설정 유효성 | 오류/경고 확인 |

### Graph 해석

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        Kiali Graph 예시                                      │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│        ┌────────────┐                                                        │
│        │ frontend   │──────────────────┐                                     │
│        │ ● 100 rps  │                  │                                     │
│        └────────────┘                  │                                     │
│                                        ▼                                     │
│                               ┌────────────┐                                 │
│                               │    api     │──────────────┐                  │
│                               │ ● 100 rps  │              │                  │
│                               │ 🔒 mTLS    │              │                  │
│                               └────────────┘              ▼                  │
│                                                   ┌────────────┐             │
│                                                   │  database  │             │
│                                                   │ ● 50 rps   │             │
│                                                   └────────────┘             │
│                                                                              │
│   ● 초록: 정상 (< 0.1% 에러)                                                 │
│   ● 노랑: 경고 (0.1-1% 에러)                                                 │
│   ● 빨강: 에러 (> 1% 에러)                                                   │
│   🔒: mTLS 활성화                                                            │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 3: Jaeger 활용

### 접속 및 기본 사용

```bash
# Jaeger 포트포워드
kubectl port-forward svc/tracing -n istio-system 16686:16686 &

# 브라우저: http://localhost:16686
```

### Trace 분석

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        Trace 예시 (총 250ms)                                 │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│ frontend (10ms)                                                              │
│ ├─────────────────────────────────────────────────────────────────────────── │
│ │                                                                            │
│ └─▶ api (200ms) ⚠️ 병목                                                      │
│     ├──────────────────────────────────────────────────────────────────────  │
│     │                                                                        │
│     └─▶ database (40ms)                                                      │
│         ├───────────────────                                                 │
│                                                                              │
│ 분석: api 서비스에서 200ms 지연 발생 → 원인 조사 필요                          │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 검색 필터

| 필터 | 용도 |
|------|------|
| Service | 서비스별 필터 |
| Operation | 엔드포인트별 필터 |
| Tags | error=true, http.status_code=500 |
| Min/Max Duration | 지연 시간 필터 |
| Limit | 결과 개수 제한 |

---

## 🛠️ Part 4: Prometheus/Grafana 활용

### 접속

```bash
# Prometheus
kubectl port-forward svc/prometheus -n istio-system 9090:9090 &

# Grafana
kubectl port-forward svc/grafana -n istio-system 3000:3000 &
# 기본: admin / prom-operator
```

### 핵심 PromQL

```promql
# 요청 성공률 (Success Rate)
sum(rate(istio_requests_total{response_code=~"2.*"}[5m])) 
/ 
sum(rate(istio_requests_total[5m]))

# P99 지연 시간
histogram_quantile(0.99, 
  sum(rate(istio_request_duration_milliseconds_bucket[5m])) by (le))

# 에러율 (Error Rate)
sum(rate(istio_requests_total{response_code=~"5.*"}[5m])) 
/ 
sum(rate(istio_requests_total[5m]))

# 서비스별 요청량
sum(rate(istio_requests_total[5m])) by (destination_service_name)

# Circuit Breaker 발동 횟수
sum(rate(istio_requests_total{response_flags="UO"}[5m]))
```

### Grafana 주요 대시보드

| 대시보드 | ID | 내용 |
|----------|-----|------|
| Istio Mesh Dashboard | 7639 | 전체 메시 상태 |
| Istio Service Dashboard | 7636 | 서비스별 상세 |
| Istio Workload Dashboard | 7630 | 워크로드별 상세 |
| Istio Performance Dashboard | 11829 | 성능 메트릭 |

---

## 🛠️ Part 5: Envoy 디버깅

### istioctl 명령어

```bash
# 전체 분석
istioctl analyze -n <namespace>

# Proxy 상태
istioctl proxy-status

# 라우트 설정
istioctl proxy-config routes <pod-name> -n <namespace>

# 클러스터 설정
istioctl proxy-config clusters <pod-name> -n <namespace>

# 엔드포인트
istioctl proxy-config endpoints <pod-name> -n <namespace>

# 리스너
istioctl proxy-config listeners <pod-name> -n <namespace>

# 전체 설정 덤프
istioctl proxy-config all <pod-name> -n <namespace>
```

### Response Flags

| 플래그 | 의미 |
|--------|------|
| `UF` | Upstream connection failure |
| `UO` | Upstream overflow (circuit breaker) |
| `NR` | No route configured |
| `URX` | Upstream retry limit exceeded |
| `UT` | Upstream request timeout |
| `DC` | Downstream connection termination |

---

## 📊 트러블슈팅 플로우차트

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        트러블슈팅 플로우                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   문제 발생                                                                  │
│       │                                                                     │
│       ▼                                                                     │
│   ┌──────────────────────────────────────────────────────┐                  │
│   │ 1. Kiali: 전체 상황 파악                              │                  │
│   │    - 어느 서비스에서 에러?                            │                  │
│   │    - 트래픽이 정상적으로 흐르는가?                    │                  │
│   └───────────────────────┬──────────────────────────────┘                  │
│                           ▼                                                 │
│   ┌──────────────────────────────────────────────────────┐                  │
│   │ 2. Grafana: 메트릭 분석                              │                  │
│   │    - 에러율은 얼마인가?                              │                  │
│   │    - 지연 시간은 정상인가?                           │                  │
│   └───────────────────────┬──────────────────────────────┘                  │
│                           ▼                                                 │
│   ┌──────────────────────────────────────────────────────┐                  │
│   │ 3. Jaeger: Trace 분석                                │                  │
│   │    - 어느 서비스에서 지연이 발생하는가?               │                  │
│   │    - 에러가 발생하는 Span은?                         │                  │
│   └───────────────────────┬──────────────────────────────┘                  │
│                           ▼                                                 │
│   ┌──────────────────────────────────────────────────────┐                  │
│   │ 4. Envoy 로그/설정 확인                              │                  │
│   │    - Response Flag 확인                              │                  │
│   │    - istioctl proxy-config                          │                  │
│   └───────────────────────┬──────────────────────────────┘                  │
│                           ▼                                                 │
│                        해결                                                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## ✅ Week 10 체크리스트

| # | 도구 | 항목 | 명령어/방법 | 완료 |
|---|------|------|-----------|------|
| 1 | Kiali | 포트포워드 | `kubectl port-forward svc/kiali 20001` | ☐ |
| 2 | Kiali | Graph 해석 | 색상, mTLS 아이콘 | ☐ |
| 3 | Kiali | Istio Config 검증 | 경고/에러 확인 | ☐ |
| 4 | Jaeger | 포트포워드 | `kubectl port-forward svc/tracing 16686` | ☐ |
| 5 | Jaeger | Trace 분석 | 병목 구간 식별 | ☐ |
| 6 | Prometheus | PromQL 쿼리 | 성공률, 지연, 에러율 | ☐ |
| 7 | Grafana | 대시보드 활용 | Istio Mesh/Service/Workload | ☐ |
| 8 | Envoy | proxy-config | routes, clusters, endpoints | ☐ |
| 9 | Envoy | Response Flags | UF, UO, NR, UT 해석 | ☐ |
| 10 | 통합 | 트러블슈팅 플로우 | Kiali→Grafana→Jaeger→Envoy | ☐ |

---

## 🔑 핵심 명령어 요약

```bash
# 관찰성 도구 접속
kubectl port-forward svc/kiali -n istio-system 20001:20001 &
kubectl port-forward svc/tracing -n istio-system 16686:16686 &
kubectl port-forward svc/prometheus -n istio-system 9090:9090 &
kubectl port-forward svc/grafana -n istio-system 3000:3000 &

# istioctl 디버깅
istioctl analyze -n <namespace>
istioctl proxy-status
istioctl proxy-config routes <pod> -n <namespace>
istioctl proxy-config endpoints <pod> -n <namespace>
istioctl proxy-config clusters <pod> -n <namespace>
```

---

## ➡️ 다음 학습: Week 11 (Day 75-81)

**주제**: Istio Security - mTLS, AuthorizationPolicy, PeerAuthentication

