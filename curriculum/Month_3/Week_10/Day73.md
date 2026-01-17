# 📅 Day 73: Observability 종합 실습 - 장애 분석 마스터

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "서비스 안정성을 위한 모니터링/로깅 시스템 구축 및 운영"

Kiali, Jaeger, Grafana를 통합 활용하여 실제 장애 상황을 분석하고 해결합니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 환경 구성 | 30분 | Observability 도구 접근 설정 |
| 시나리오 실습 | 2시간 | 장애 시나리오 분석 |
| 종합 훈련 | 1시간 | 실전 트러블슈팅 |
| 정리 | 30분 | 분석 보고서 작성 |

---

## 📚 Part 1: Observability 도구 개요 (20분)

### 세 가지 관찰 가능성 요소

```
┌─────────────────────────────────────────────────────────────┐
│  Observability 3 Pillars                                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Metrics (메트릭) - Prometheus/Grafana                   │
│  ┌─────────────────────────────────────────────────┐       │
│  │ "얼마나?" - 수치로 측정                         │       │
│  │ - 요청 수, 응답 시간, 에러율                    │       │
│  │ - CPU, Memory 사용량                            │       │
│  │ - 트렌드 분석, 알람                             │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  2. Traces (트레이싱) - Jaeger                              │
│  ┌─────────────────────────────────────────────────┐       │
│  │ "어디서?" - 요청 흐름 추적                      │       │
│  │ - 서비스 간 호출 관계                           │       │
│  │ - 각 구간 소요 시간                             │       │
│  │ - 병목 지점 식별                                │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  3. Logs (로그) - Envoy Access Log                          │
│  ┌─────────────────────────────────────────────────┐       │
│  │ "무슨 일이?" - 상세 이벤트                      │       │
│  │ - 에러 메시지                                   │       │
│  │ - 요청/응답 상세                                │       │
│  │ - Response Flags                                │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  + Service Mesh 토폴로지 - Kiali                            │
│  ┌─────────────────────────────────────────────────┐       │
│  │ "전체 그림" - 실시간 서비스 맵                  │       │
│  │ - 서비스 의존성 시각화                          │       │
│  │ - 트래픽 흐름 애니메이션                        │       │
│  │ - 설정 검증                                     │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 도구 연동 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│  Istio Observability Stack                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐                │
│  │ Service │───▶│ Envoy   │───▶│ Service │                │
│  │    A    │    │ Sidecar │    │    B    │                │
│  └─────────┘    └────┬────┘    └─────────┘                │
│                      │                                      │
│         ┌────────────┼────────────┐                        │
│         ▼            ▼            ▼                        │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐               │
│  │Prometheus │ │  Jaeger   │ │  Kiali    │               │
│  │ (Metrics) │ │ (Traces)  │ │ (Graph)   │               │
│  └─────┬─────┘ └───────────┘ └───────────┘               │
│        │                                                   │
│        ▼                                                   │
│  ┌───────────┐                                            │
│  │  Grafana  │                                            │
│  │(Dashboard)│                                            │
│  └───────────┘                                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: 환경 구성 (30분)

### 포트 포워딩 설정

```bash
# 터미널 1: Kiali
kubectl port-forward -n istio-system svc/kiali 20001:20001 &

# 터미널 2: Grafana
kubectl port-forward -n istio-system svc/grafana 3000:3000 &

# 터미널 3: Jaeger
kubectl port-forward -n istio-system svc/tracing 16686:80 &

# 터미널 4: Prometheus
kubectl port-forward -n istio-system svc/prometheus 9090:9090 &

# 접근 URL
# Kiali: http://localhost:20001
# Grafana: http://localhost:3000
# Jaeger: http://localhost:16686
# Prometheus: http://localhost:9090
```

### 테스트 트래픽 생성

```bash
# Bookinfo 애플리케이션이 설치되어 있다고 가정
export GATEWAY_URL=$(kubectl -n istio-system get service istio-ingressgateway \
  -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

# 지속적인 트래픽 생성
while true; do
  curl -s "http://$GATEWAY_URL/productpage" > /dev/null
  sleep 0.5
done &
```

---

## 🛠️ Part 3: 시나리오 기반 실습 (2시간)

### 시나리오 1: 성능 문제 분석

```
📋 상황: "사용자가 상품 페이지가 느리다고 신고했습니다."
→ 어떤 서비스에서 지연이 발생하는지 분석하세요.
```

#### Step 1: Kiali로 전체 상황 파악

```bash
# Kiali 접속: http://localhost:20001

# 확인 순서:
# 1. Graph 탭 → Traffic Animation
# 2. Display 옵션에서 "Response Time" 선택
# 3. 붉은색/노란색 표시된 서비스 확인
# 4. 서비스 클릭 → 상세 메트릭 확인
```

**Kiali에서 확인할 것:**
- 서비스 간 연결 상태
- 트래픽 흐름 방향
- 에러율 (빨간색 표시)
- 응답 시간 (엣지 두께)

#### Step 2: Grafana로 메트릭 분석

```bash
# Grafana 접속: http://localhost:3000

# 대시보드 확인 순서:
# 1. Istio Service Dashboard 선택
# 2. Service 드롭다운에서 문제 서비스 선택
# 3. 확인할 메트릭:
#    - Request Duration (P50, P90, P99)
#    - Request Count
#    - Error Rate (4xx, 5xx)
#    - TCP Connections
```

**핵심 PromQL 쿼리:**

```promql
# 서비스별 P99 응답 시간
histogram_quantile(0.99, 
  sum(rate(istio_request_duration_milliseconds_bucket{
    destination_service=~".*",
    reporter="destination"
  }[5m])) by (destination_service, le)
)

# 서비스별 에러율
sum(rate(istio_requests_total{
  response_code=~"5.*",
  reporter="destination"
}[5m])) by (destination_service)
/
sum(rate(istio_requests_total{
  reporter="destination"
}[5m])) by (destination_service)
```

#### Step 3: Jaeger로 Trace 분석

```bash
# Jaeger 접속: http://localhost:16686

# 분석 순서:
# 1. Service 드롭다운에서 "productpage.default" 선택
# 2. Operation: 전체 또는 특정 엔드포인트
# 3. Min Duration: 2s (느린 요청만 필터)
# 4. Find Traces 클릭
# 5. 가장 느린 Trace 선택
# 6. Span별 소요 시간 분석
```

**Trace 분석 포인트:**
```
┌─────────────────────────────────────────────────────────────┐
│  Trace 분석 예시                                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  productpage ────────────────────────────────────▶ 3.2s    │
│    │                                                        │
│    ├── details ──────▶ 50ms                                │
│    │                                                        │
│    └── reviews ─────────────────────────────────▶ 3.1s    │
│          │                                                  │
│          └── ratings ────────────────────────▶ 2.9s ❌     │
│                                                             │
│  🔍 ratings 서비스에서 2.9초 지연 발생!                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### Step 4: 로그 확인

```bash
# 병목 서비스(ratings)의 로그 확인
kubectl logs -l app=ratings -c istio-proxy --tail=100

# 특정 응답 플래그 확인
kubectl logs -l app=ratings -c istio-proxy | grep -E "5[0-9]{2}|UT|UH"

# Response Flags 의미:
# UT = Upstream Timeout
# UH = Upstream Unhealthy
# UF = Upstream Connection Failure
# NR = No Route
# DC = Downstream Connection Terminated
```

#### Step 5: 근본 원인 식별

```bash
# Envoy 설정 확인
istioctl proxy-config endpoints productpage-xxx \
  --cluster "outbound|9080||ratings.default.svc.cluster.local"

# 엔드포인트 상태 확인
# HEALTHY / UNHEALTHY 인스턴스 확인

# 서비스 상태 확인
kubectl get pods -l app=ratings -o wide
kubectl describe pod ratings-xxx

# 리소스 사용량 확인
kubectl top pods -l app=ratings
```

### 시나리오 2: 간헐적 에러 분석

```
📋 상황: "간헐적으로 503 에러가 발생합니다."
→ 에러 발생 패턴과 원인을 분석하세요.
```

#### 분석 절차

```bash
# 1. Prometheus에서 에러 패턴 확인
# Query: 5분간 503 에러 비율
sum(rate(istio_requests_total{
  response_code="503"
}[5m])) by (source_workload, destination_workload)

# 2. Kiali에서 에러 발생 서비스 식별
# Graph → Display → "Security" OFF, "Traffic Animation" ON
# 붉은색 엣지 확인

# 3. Jaeger에서 실패한 요청 추적
# Tags: error=true 또는 http.status_code=503

# 4. Envoy 로그에서 상세 원인 확인
kubectl logs -l app=reviews -c istio-proxy | grep "503"
```

### 시나리오 3: Circuit Breaker 동작 확인

```
📋 상황: "Circuit Breaker가 의도대로 동작하는지 확인하세요."
```

```bash
# Circuit Breaker 설정 확인
kubectl get destinationrule -o yaml

# 부하 테스트로 Circuit Breaker 트리거
kubectl run fortio --image=fortio/fortio --rm -it -- \
  load -c 100 -qps 100 -t 30s \
  http://reviews.default.svc.cluster.local:9080/

# Kiali에서 트래픽 변화 관찰
# Circuit이 열리면 트래픽이 다른 버전으로 리디렉션됨

# Prometheus에서 확인
# Query:
sum(rate(istio_requests_total{
  response_flags=~".*UO.*"
}[1m])) by (destination_service)
# UO = Upstream Overflow (Circuit Breaker 작동)
```

---

## 📊 Part 4: 분석 보고서 작성 (30분)

### 장애 분석 보고서 템플릿

```markdown
## 장애 분석 보고서

### 1. 개요
- 발생 시간: 2024-XX-XX HH:MM
- 영향 범위: 상품 페이지 응답 지연
- 영향 사용자: 약 XX%

### 2. 증상
- 상품 페이지 평균 응답 시간: 200ms → 3.2s
- P99 응답 시간: 500ms → 5s
- 에러율: 0.1% → 2%

### 3. 분석 과정

#### 3.1 Kiali 분석
- ratings 서비스에 빨간색 경고 표시
- reviews → ratings 간 지연 확인

#### 3.2 Grafana 메트릭
- ratings P99 = 2.9s
- ratings Pod CPU: 95%

#### 3.3 Jaeger Trace
- ratings span에서 2.8s 소요
- DB 쿼리 지연 패턴 확인

#### 3.4 로그 분석
- UT(Upstream Timeout) 플래그 다수 발견
- DB 연결 풀 고갈 로그

### 4. 근본 원인
- ratings 서비스의 데이터베이스 연결 풀 크기 부족
- 트래픽 증가로 인한 연결 대기 발생

### 5. 해결 조치
- 즉시 조치: ratings Pod 스케일 아웃 (2 → 4)
- 임시 조치: DB 연결 풀 크기 증가 (10 → 50)
- 영구 조치: 커넥션 풀링 최적화, 쿼리 튜닝

### 6. 재발 방지
- DB 연결 수 모니터링 알람 추가
- 부하 테스트 시나리오에 연결 풀 테스트 포함
- 쿼리 성능 모니터링 대시보드 추가

### 7. 타임라인
| 시간 | 이벤트 |
|------|--------|
| 14:00 | 사용자 신고 접수 |
| 14:05 | Kiali에서 이상 감지 |
| 14:10 | 근본 원인 파악 |
| 14:15 | 스케일 아웃 적용 |
| 14:20 | 서비스 정상화 확인 |
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | Kiali로 전체 토폴로지 파악 | ☐ |
| 2 | Grafana로 메트릭 분석 | ☐ |
| 3 | Jaeger로 Trace 분석 | ☐ |
| 4 | Envoy 로그 분석 | ☐ |
| 5 | 근본 원인 식별 프로세스 이해 | ☐ |
| 6 | 장애 분석 보고서 작성 | ☐ |

---

## 🔑 핵심 명령어

```bash
# 포트 포워딩
kubectl port-forward -n istio-system svc/kiali 20001:20001
kubectl port-forward -n istio-system svc/grafana 3000:3000
kubectl port-forward -n istio-system svc/tracing 16686:80

# 로그 분석
kubectl logs -l app=<service> -c istio-proxy | grep -E "5[0-9]{2}|UT|UH"

# 엔드포인트 상태
istioctl proxy-config endpoints <pod-name>
```

---

## 📝 면접 대비 질문

### Q1: Metrics, Traces, Logs의 차이와 각각 언제 사용하나요?
> "Metrics는 '얼마나'를 보여주어 트렌드와 알람에 적합합니다. Traces는 '어디서'를 보여주어 분산 시스템의 요청 흐름과 병목 식별에 사용합니다. Logs는 '무슨 일이'를 상세히 보여주어 구체적인 에러 원인 파악에 사용합니다. 장애 분석 시 Metrics로 이상 감지 → Traces로 범위 좁히기 → Logs로 원인 파악 순서로 진행합니다."

### Q2: 서비스 지연 문제를 어떻게 분석하나요?
> "먼저 Kiali에서 전체 서비스 맵을 확인하여 문제 서비스를 식별합니다. Grafana에서 해당 서비스의 P99 응답 시간과 에러율을 확인합니다. Jaeger에서 느린 요청의 Trace를 분석하여 어떤 Span에서 지연이 발생하는지 파악합니다. 마지막으로 Envoy 로그에서 Response Flags를 확인하여 근본 원인을 파악합니다."

---

## ➡️ 다음 학습: Day 74

**주제**: Week 10 복습
- Istio 핵심 개념 정리
- 실습 내용 복습
- 면접 준비
