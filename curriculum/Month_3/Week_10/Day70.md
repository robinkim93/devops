# 📅 Day 70: Istio + Prometheus/Grafana

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "서비스 안정성을 위한 모니터링 시스템 구축"
> Istio 메트릭 수집 및 Grafana 시각화

토스플레이스는 Prometheus, Thanos, Grafana로 모니터링 시스템을 운영합니다. Istio Service Mesh와 통합된 메트릭 수집은 서비스 관찰성의 핵심입니다.

---

## ⏰ 예상 학습 시간: 4시간

---

## 📚 Part 1: Istio 메트릭 이해 (1시간)

### 1.1 Istio가 자동 수집하는 메트릭

Istio의 Envoy Sidecar는 애플리케이션 코드 변경 없이 자동으로 메트릭을 수집합니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│  Istio 메트릭 수집 아키텍처                                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                      Prometheus                              │    │
│  │  ┌───────────────────────────────────────────────────────┐  │    │
│  │  │  istio_requests_total                                 │  │    │
│  │  │  istio_request_duration_milliseconds                  │  │    │
│  │  │  istio_tcp_connections_opened_total                   │  │    │
│  │  │  ...                                                  │  │    │
│  │  └───────────────────────────────────────────────────────┘  │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                      ▲                                              │
│                      │ scrape                                       │
│                      │                                              │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Pod (Envoy Sidecar)                                        │    │
│  │  └─ :15020/stats/prometheus                                 │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  메트릭 카테고리:                                                   │
│  1. Request Metrics (HTTP/gRPC)                                     │
│  2. TCP Metrics (Connection)                                        │
│  3. Control Plane Metrics (istiod)                                  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 주요 Istio 메트릭

| 메트릭 | 설명 | 사용 예 |
|--------|------|---------|
| `istio_requests_total` | 총 요청 수 | 트래픽 양, 에러율 |
| `istio_request_duration_milliseconds` | 요청 지연 시간 | P50, P95, P99 지연 |
| `istio_request_bytes` | 요청 바이트 수 | 데이터 전송량 |
| `istio_response_bytes` | 응답 바이트 수 | 데이터 수신량 |
| `istio_tcp_connections_opened_total` | TCP 연결 수 | 연결 패턴 분석 |
| `istio_tcp_sent_bytes_total` | 전송 바이트 | TCP 트래픽 분석 |

### 1.3 메트릭 레이블 이해

```promql
# Istio 메트릭의 주요 레이블
istio_requests_total{
  # 소스 정보
  source_workload="productpage",
  source_workload_namespace="default",
  source_app="productpage",
  source_version="v1",
  
  # 목적지 정보
  destination_workload="reviews",
  destination_workload_namespace="default",
  destination_app="reviews",
  destination_version="v2",
  destination_service="reviews.default.svc.cluster.local",
  
  # 응답 정보
  response_code="200",
  response_flags="-",          # Envoy 응답 플래그
  request_protocol="http",
  connection_security_policy="mutual_tls"
}
```

### 1.4 Response Flags 의미

| 플래그 | 의미 | 원인 |
|--------|------|------|
| `-` | 정상 | 문제 없음 |
| `UH` | Upstream Unhealthy | 백엔드 불가 |
| `UF` | Upstream Failure | 연결 실패 |
| `UO` | Upstream Overflow | 서킷브레이커 트리거 |
| `NR` | No Route | 라우팅 규칙 없음 |
| `URX` | Upstream Retry | 재시도 초과 |
| `DC` | Downstream Closed | 클라이언트 종료 |
| `LH` | Local Healthcheck | 로컬 헬스체크 실패 |

---

## 🛠️ Part 2: Prometheus 설치 및 설정 (1시간)

### 2.1 Istio Addon으로 Prometheus 설치

```bash
# Istio 샘플 addon에서 Prometheus 설치
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/prometheus.yaml

# 또는 로컬 Istio 설치 경로에서
kubectl apply -f $ISTIO_HOME/samples/addons/prometheus.yaml

# Pod 확인
kubectl get pods -n istio-system -l app=prometheus

# Service 확인
kubectl get svc -n istio-system -l app=prometheus
```

### 2.2 Prometheus UI 접속

```bash
# 포트 포워딩
kubectl port-forward -n istio-system svc/prometheus 9090:9090 &

# 브라우저에서 접속
# http://localhost:9090
```

### 2.3 기본 Istio 메트릭 쿼리

```promql
# 1. 총 요청 수
istio_requests_total

# 2. 특정 서비스의 요청 수 (2xx 성공)
sum(istio_requests_total{
  destination_workload="productpage",
  response_code=~"2.*"
})

# 3. 서비스별 요청 처리율 (초당, RPS)
sum(rate(istio_requests_total[1m])) by (destination_workload)

# 4. 서비스별 5xx 에러율
sum(rate(istio_requests_total{response_code=~"5.*"}[5m])) by (destination_workload)
/
sum(rate(istio_requests_total[5m])) by (destination_workload)

# 5. P95 응답 시간 (밀리초)
histogram_quantile(0.95,
  sum(rate(istio_request_duration_milliseconds_bucket[5m])) 
  by (destination_workload, le)
)

# 6. P50 응답 시간
histogram_quantile(0.50,
  sum(rate(istio_request_duration_milliseconds_bucket[5m])) 
  by (destination_workload, le)
)

# 7. 평균 응답 시간
sum(rate(istio_request_duration_milliseconds_sum[5m])) by (destination_workload)
/
sum(rate(istio_request_duration_milliseconds_count[5m])) by (destination_workload)
```

### 2.4 고급 쿼리

```promql
# 버전별 트래픽 비율
sum(rate(istio_requests_total{destination_app="reviews"}[5m])) by (destination_version)

# mTLS 사용 비율
sum(rate(istio_requests_total{connection_security_policy="mutual_tls"}[5m]))
/
sum(rate(istio_requests_total[5m]))

# 응답 플래그별 요청 수 (문제 진단)
sum(rate(istio_requests_total{response_flags!~"-"}[5m])) by (response_flags)

# 소스-목적지별 트래픽 맵
sum(rate(istio_requests_total[5m])) by (source_workload, destination_workload)
```

---

## 🛠️ Part 3: Grafana 설치 및 대시보드 (1.5시간)

### 3.1 Grafana 설치

```bash
# Istio 샘플 addon에서 Grafana 설치
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/grafana.yaml

# Pod 확인
kubectl get pods -n istio-system -l app.kubernetes.io/name=grafana

# Service 확인
kubectl get svc -n istio-system grafana
```

### 3.2 Grafana 접속

```bash
# 포트 포워딩
kubectl port-forward -n istio-system svc/grafana 3000:3000 &

# 브라우저에서 접속
# http://localhost:3000

# 기본 로그인 (Istio addon 설치 시 인증 없음)
```

### 3.3 기본 제공 대시보드

Istio addon으로 설치하면 기본 대시보드가 포함되어 있습니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│  Istio 기본 대시보드                                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. Istio Mesh Dashboard                                            │
│     - 전체 메시 요청량                                              │
│     - 글로벌 에러율                                                 │
│     - 서비스 수, Pod 수                                             │
│     - P50/P90/P99 지연시간                                         │
│                                                                      │
│  2. Istio Service Dashboard                                         │
│     - 서비스별 요청률                                               │
│     - 응답 시간 분포                                                │
│     - 에러 코드 분포                                                │
│     - 클라이언트/서버 워크로드                                      │
│                                                                      │
│  3. Istio Workload Dashboard                                        │
│     - Pod별 리소스 사용량                                           │
│     - 인바운드/아웃바운드 트래픽                                    │
│     - 연결 수                                                       │
│                                                                      │
│  4. Istio Control Plane Dashboard                                   │
│     - istiod 상태                                                   │
│     - xDS 푸시 지연                                                 │
│     - 프록시 동기화 상태                                            │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.4 대시보드 확인 방법

```bash
# Grafana에서:
# 1. 왼쪽 메뉴 → Dashboards → Browse
# 2. "istio" 폴더 선택
# 3. 원하는 대시보드 클릭

# 대시보드 목록:
# - Istio Mesh Dashboard
# - Istio Service Dashboard  
# - Istio Workload Dashboard
# - Istio Performance Dashboard
# - Istio Control Plane Dashboard
```

### 3.5 커스텀 대시보드 생성

```json
// 새 대시보드 패널 예시 (JSON)
{
  "title": "Service Error Rate",
  "type": "graph",
  "datasource": "Prometheus",
  "targets": [
    {
      "expr": "sum(rate(istio_requests_total{response_code=~\"5.*\"}[5m])) by (destination_service) / sum(rate(istio_requests_total[5m])) by (destination_service)",
      "legendFormat": "{{destination_service}}"
    }
  ],
  "yaxes": [
    {
      "format": "percentunit",
      "label": "Error Rate"
    }
  ]
}
```

---

## 🛠️ Part 4: 실습 - 트래픽 생성 및 모니터링 (30분)

### 4.1 Bookinfo 앱 트래픽 생성

```bash
# Bookinfo가 없으면 설치
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/bookinfo/platform/kube/bookinfo.yaml
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/bookinfo/networking/bookinfo-gateway.yaml

# Gateway URL 확인
export INGRESS_HOST=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
export INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].port}')
export GATEWAY_URL=$INGRESS_HOST:$INGRESS_PORT

# 또는 minikube
# minikube tunnel & 
# export GATEWAY_URL=$(minikube ip):$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')

# 트래픽 생성
for i in $(seq 1 100); do
  curl -s -o /dev/null "http://$GATEWAY_URL/productpage"
  sleep 0.5
done &
```

### 4.2 Prometheus에서 메트릭 확인

```promql
# Prometheus UI에서 확인
# http://localhost:9090

# productpage 요청 수
sum(rate(istio_requests_total{destination_app="productpage"}[1m]))

# reviews 버전별 트래픽
sum(rate(istio_requests_total{destination_app="reviews"}[1m])) by (destination_version)
```

### 4.3 Grafana 대시보드 확인

```bash
# Grafana에서 확인
# http://localhost:3000

# 확인할 대시보드:
# 1. Istio Mesh Dashboard → 전체 트래픽 현황
# 2. Istio Service Dashboard → productpage 선택
# 3. Istio Workload Dashboard → productpage-v1 선택
```

---

## 📊 Part 5: 알림 설정 (30분)

### 5.1 Prometheus Alerting Rules

```yaml
# prometheus-alerts.yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: istio-alerts
  namespace: istio-system
spec:
  groups:
  - name: istio.rules
    rules:
    # 높은 에러율 알림
    - alert: IstioHighErrorRate
      expr: |
        sum(rate(istio_requests_total{response_code=~"5.*"}[5m])) by (destination_service)
        /
        sum(rate(istio_requests_total[5m])) by (destination_service)
        > 0.05
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "High error rate for {{ $labels.destination_service }}"
        description: "Error rate is {{ $value | humanizePercentage }}"
    
    # 높은 지연시간 알림
    - alert: IstioHighLatency
      expr: |
        histogram_quantile(0.95,
          sum(rate(istio_request_duration_milliseconds_bucket[5m])) 
          by (destination_service, le)
        ) > 1000
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "High latency for {{ $labels.destination_service }}"
        description: "P95 latency is {{ $value }}ms"
```

### 5.2 Grafana Alert 설정

```bash
# Grafana UI에서:
# 1. 대시보드 → 패널 → Edit
# 2. Alert 탭
# 3. Create Alert 클릭
# 4. 조건 설정:
#    - When: avg() of query(A, 5m, now)
#    - Is Above: 0.05 (5% 에러율)
# 5. Notification 설정 (Slack, Email 등)
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | Istio 메트릭 종류 이해 | ☐ |
| 2 | 메트릭 레이블 이해 | ☐ |
| 3 | Prometheus 설치 | ☐ |
| 4 | Istio 메트릭 쿼리 | ☐ |
| 5 | Grafana 설치 | ☐ |
| 6 | 기본 대시보드 확인 | ☐ |
| 7 | 트래픽 생성 및 모니터링 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# Prometheus 접속
kubectl port-forward -n istio-system svc/prometheus 9090:9090

# Grafana 접속
kubectl port-forward -n istio-system svc/grafana 3000:3000

# Istio addon 설치
kubectl apply -f $ISTIO_HOME/samples/addons/
```

---

## 💡 면접 대비 핵심 포인트

### Q1: Istio가 자동 수집하는 메트릭은?
**A**: istio_requests_total (요청 수), istio_request_duration_milliseconds (지연), istio_tcp_connections (TCP 연결). Envoy Sidecar가 수집하여 /stats/prometheus로 노출합니다.

### Q2: 에러율 계산 쿼리는?
**A**: `sum(rate(istio_requests_total{response_code=~"5.*"}[5m])) / sum(rate(istio_requests_total[5m]))`

### Q3: Response Flags의 의미는?
**A**: Envoy가 요청 처리 중 발생한 상태를 나타냅니다. "-"는 정상, "UH"는 Upstream 불가, "UO"는 서킷브레이커 등입니다.

---

## ➡️ 다음 학습: Day 71

**주제**: Istio Access Logging
- Envoy Access Log 설정
- 로그 분석
- 디버깅 활용
