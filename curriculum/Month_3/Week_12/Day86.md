# 📅 Day 86: Istio 관찰성 - Kiali, Jaeger, Grafana

## 🎯 오늘의 목표

> **토스플레이스 핵심**: Observability 3대 도구를 설정하고 서비스 메시를 모니터링합니다.

---

## ⏰ 예상 소요 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 설치 | 1시간 | 도구 배포 |
| Kiali | 1시간 | 서비스 시각화 |
| Jaeger | 1시간 | 분산 추적 |
| Grafana | 1시간 | 메트릭 대시보드 |

---

## 📚 Part 1: Observability 개요

### 관찰성 3대 축

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                      Istio Observability                                     │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌────────────────┐  ┌────────────────┐  ┌────────────────┐                │
│   │    Metrics     │  │     Traces     │  │      Logs      │                │
│   │  (Prometheus)  │  │    (Jaeger)    │  │  (Access Log)  │                │
│   └───────┬────────┘  └───────┬────────┘  └───────┬────────┘                │
│           │                   │                   │                         │
│           └───────────────────┼───────────────────┘                         │
│                               │                                             │
│                               ▼                                             │
│                      ┌────────────────┐                                     │
│                      │     Kiali      │                                     │
│                      │  (통합 시각화)  │                                     │
│                      └────────────────┘                                     │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 도구별 역할

| 도구 | 역할 | 주요 기능 |
|------|------|----------|
| **Prometheus** | 메트릭 수집 | 요청량, 에러율, 지연 |
| **Grafana** | 시각화 | 대시보드, 알림 |
| **Jaeger** | 분산 추적 | 요청 경로, Trace/Span |
| **Kiali** | 통합 시각화 | 토폴로지, 설정 검증 |

---

## 🛠️ Part 2: 도구 설치

### Istio Addons 설치

```bash
# Istio 설치 디렉토리에서
cd istio-1.20.0

# 모든 addons 설치
kubectl apply -f samples/addons/

# 또는 개별 설치
kubectl apply -f samples/addons/prometheus.yaml
kubectl apply -f samples/addons/grafana.yaml
kubectl apply -f samples/addons/jaeger.yaml
kubectl apply -f samples/addons/kiali.yaml
```

### Helm으로 설치 (프로덕션)

```bash
# Prometheus
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus prometheus-community/kube-prometheus-stack -n istio-system

# Kiali
helm repo add kiali https://kiali.org/helm-charts
helm install kiali kiali/kiali-server -n istio-system \
  --set auth.strategy="anonymous"

# Jaeger
helm repo add jaegertracing https://jaegertracing.github.io/helm-charts
helm install jaeger jaegertracing/jaeger -n istio-system
```

### 설치 확인

```bash
# Pod 상태 확인
kubectl get pods -n istio-system

# 서비스 확인
kubectl get svc -n istio-system
```

---

## 🛠️ Part 3: Kiali 활용

### 접속

```bash
kubectl port-forward svc/kiali -n istio-system 20001:20001 &
# 브라우저: http://localhost:20001
```

### 주요 화면

| 메뉴 | 설명 | 용도 |
|------|------|------|
| **Overview** | 네임스페이스 상태 | 전체 현황 파악 |
| **Graph** | 서비스 토폴로지 | 트래픽 흐름 시각화 |
| **Applications** | 앱 목록 | 앱별 상세 정보 |
| **Workloads** | 워크로드 목록 | Pod/Deployment 정보 |
| **Services** | 서비스 목록 | VirtualService 정보 |
| **Istio Config** | Istio 설정 | 설정 유효성 검사 |

### Graph 기능

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         Kiali Graph 해석                                     │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   노드 색상:                                                                 │
│   🟢 초록 = 정상 (에러율 < 0.1%)                                             │
│   🟡 노랑 = 경고 (0.1% < 에러율 < 1%)                                        │
│   🔴 빨강 = 에러 (에러율 > 1%)                                               │
│   ⚫ 회색 = 트래픽 없음                                                       │
│                                                                              │
│   엣지 두께:                                                                 │
│   굵음 = 높은 트래픽                                                         │
│   얇음 = 낮은 트래픽                                                         │
│                                                                              │
│   아이콘:                                                                    │
│   🔒 = mTLS 활성화                                                           │
│   ⚡ = Circuit Breaker 발동                                                  │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Graph 필터 옵션

```bash
# Display 옵션
- Traffic Animation: 트래픽 흐름 애니메이션
- Response Time: 응답 시간 표시
- Security: mTLS 상태 표시
- Traffic Distribution: 트래픽 분포 표시

# Layout 옵션
- Dagre: 계층적 레이아웃
- Cola: 힘 기반 레이아웃
```

---

## 🛠️ Part 4: Jaeger 활용

### 접속

```bash
kubectl port-forward svc/tracing -n istio-system 16686:16686 &
# 브라우저: http://localhost:16686
```

### Trace 검색

| 필터 | 설명 |
|------|------|
| **Service** | 서비스 선택 |
| **Operation** | 엔드포인트 선택 |
| **Tags** | error=true, http.status_code=500 |
| **Min Duration** | 최소 지연 시간 |
| **Max Duration** | 최대 지연 시간 |
| **Limit Results** | 결과 개수 제한 |

### Trace 분석

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        Jaeger Trace 예시                                     │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Trace: abc123 (총 350ms)                                                   │
│                                                                              │
│   frontend (15ms)                                                            │
│   ├─────────────────                                                         │
│   │                                                                          │
│   └─▶ api (300ms) ⚠️                                                         │
│       ├──────────────────────────────────────────────────────────────────    │
│       │                                                                      │
│       ├─▶ database (30ms)                                                    │
│       │   ├───────────                                                       │
│       │                                                                      │
│       └─▶ cache (5ms)                                                        │
│           ├──                                                                │
│                                                                              │
│   분석: api 서비스에서 300ms 지연 (병목)                                       │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 유용한 검색 패턴

```bash
# 에러 트레이스만
Service: api
Tags: error=true

# 느린 요청만
Service: api
Min Duration: 500ms

# 특정 상태 코드
Tags: http.status_code=500
```

---

## 🛠️ Part 5: Grafana 활용

### 접속

```bash
kubectl port-forward svc/grafana -n istio-system 3000:3000 &
# 브라우저: http://localhost:3000
# 기본: admin / admin (또는 prom-operator)
```

### Istio 기본 대시보드

| 대시보드 | 내용 |
|----------|------|
| **Istio Mesh Dashboard** | 전체 메시 상태 |
| **Istio Service Dashboard** | 서비스별 상세 |
| **Istio Workload Dashboard** | 워크로드별 상세 |
| **Istio Performance Dashboard** | 성능 메트릭 |

### 주요 메트릭

```promql
# 요청 성공률
sum(rate(istio_requests_total{response_code=~"2.*", destination_workload_namespace="istio-portfolio"}[5m])) 
/ 
sum(rate(istio_requests_total{destination_workload_namespace="istio-portfolio"}[5m]))

# P99 지연 시간
histogram_quantile(0.99, 
  sum(rate(istio_request_duration_milliseconds_bucket{destination_workload_namespace="istio-portfolio"}[5m])) 
  by (le, destination_workload))

# 에러율
sum(rate(istio_requests_total{response_code=~"5.*", destination_workload_namespace="istio-portfolio"}[5m])) 
/ 
sum(rate(istio_requests_total{destination_workload_namespace="istio-portfolio"}[5m]))

# 서비스별 요청량
sum(rate(istio_requests_total{destination_workload_namespace="istio-portfolio"}[5m])) 
by (destination_service_name)
```

---

## 🧪 Part 6: 트래픽 생성 및 확인

### 트래픽 생성

```bash
# 지속적인 트래픽 생성
while true; do
  curl -s http://api.portfolio.local/ > /dev/null
  sleep 0.5
done &

# 또는 fortio 사용
kubectl run fortio --image=fortio/fortio -it --rm -n istio-portfolio -- \
  load -qps 10 -t 60s http://api:8080/
```

### 확인 체크리스트

**Kiali**:
- [ ] Graph에서 서비스 토폴로지 확인
- [ ] v1, v2 트래픽 분포 확인
- [ ] mTLS 아이콘 (자물쇠) 확인
- [ ] Istio Config 경고 확인

**Jaeger**:
- [ ] api 서비스 Trace 검색
- [ ] Span 연결 확인
- [ ] 지연 시간 분포 확인

**Grafana**:
- [ ] Istio Mesh Dashboard 확인
- [ ] 요청률, 에러율, 지연 시간 확인
- [ ] 알림 설정

---

## 📸 문서화용 스크린샷

```markdown
# docs/observability.md

## Kiali 서비스 그래프
![Kiali Graph](./images/kiali-graph.png)
- 서비스 간 연결 관계
- mTLS 상태 (자물쇠 아이콘)
- 트래픽 분포

## Jaeger Trace
![Jaeger Trace](./images/jaeger-trace.png)
- 요청 흐름 (frontend → api → database)
- 각 서비스별 지연 시간
- 에러 발생 지점

## Grafana 대시보드
![Grafana Dashboard](./images/grafana-istio.png)
- 요청 성공률
- P50, P90, P99 지연 시간
- 에러율 추이
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 확인 방법 | 완료 |
|---|------|----------|------|
| 1 | Prometheus 설치 | `kubectl get pods -l app=prometheus` | ☐ |
| 2 | Grafana 설치 | `kubectl get pods -l app.kubernetes.io/name=grafana` | ☐ |
| 3 | Jaeger 설치 | `kubectl get pods -l app=jaeger` | ☐ |
| 4 | Kiali 설치 | `kubectl get pods -l app=kiali` | ☐ |
| 5 | Kiali Graph 확인 | 토폴로지, mTLS | ☐ |
| 6 | Jaeger Trace 확인 | Span 연결 | ☐ |
| 7 | Grafana 대시보드 | Istio Mesh Dashboard | ☐ |
| 8 | 스크린샷 저장 | 문서화용 | ☐ |

---

## 🔑 핵심 명령어 요약

```bash
# 포트포워드
kubectl port-forward svc/kiali -n istio-system 20001:20001 &
kubectl port-forward svc/tracing -n istio-system 16686:16686 &
kubectl port-forward svc/grafana -n istio-system 3000:3000 &
kubectl port-forward svc/prometheus -n istio-system 9090:9090 &

# 트래픽 생성
while true; do curl -s http://api.portfolio.local/; sleep 0.5; done &
```

---

## ➡️ 다음 학습: Day 87

**주제**: 장애 복원력 (Circuit Breaker, Retry, Timeout)

