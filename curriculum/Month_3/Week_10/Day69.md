# 📅 Day 69: Jaeger - 분산 추적

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "OS, Network 등 다양한 레이어에서의 모니터링, 트러블슈팅 경험"
> Jaeger로 마이크로서비스 요청 흐름을 추적하고 성능 병목 식별

토스플레이스는 마이크로서비스 아키텍처로 운영되어 분산 추적이 필수입니다. Jaeger는 서비스 간 요청 흐름을 시각화하여 문제 원인을 빠르게 파악하게 해줍니다.

---

## ⏰ 예상 학습 시간: 4시간

---

## 📚 Part 1: 분산 추적 개념 (1시간)

### 1.1 왜 분산 추적이 필요한가?

```
┌─────────────────────────────────────────────────────────────────────┐
│  마이크로서비스 디버깅의 어려움                                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  단일 애플리케이션 (Monolith):                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  User → [App] → Response                                    │    │
│  │  → 로그 한 곳에서 확인 가능                                  │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  마이크로서비스:                                                    │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  User → [A] → [B] → [C] → [D] → [E] → Response             │    │
│  │                 ↘                                            │    │
│  │                  [F] → [G]                                   │    │
│  │                                                             │    │
│  │  "응답이 느려요!" → 어느 서비스에서 지연?                    │    │
│  │  "에러가 발생해요!" → 어느 서비스가 원인?                    │    │
│  │  "요청이 실패해요!" → 어디서 실패?                           │    │
│  │                                                             │    │
│  │  → 분산 추적(Distributed Tracing)으로 해결!                 │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  분산 추적이 제공하는 것:                                           │
│  • 전체 요청 흐름 시각화                                           │
│  • 서비스별 응답 시간                                              │
│  • 병목 지점 식별                                                  │
│  • 에러 발생 위치 추적                                             │
│  • 서비스 의존성 파악                                              │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 핵심 개념

```
┌─────────────────────────────────────────────────────────────────────┐
│  분산 추적 용어                                                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Trace (트레이스):                                                  │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  하나의 요청이 시스템을 통과하는 전체 여정                   │    │
│  │  TraceID: abc123-xyz789 (요청 전체를 식별하는 고유 ID)       │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Span (스팬):                                                       │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  개별 작업 단위 (서비스 호출, DB 쿼리 등)                    │    │
│  │  SpanID: span-001 (개별 Span 식별)                          │    │
│  │  ParentSpanID: span-000 (부모 Span 참조)                    │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  예시:                                                              │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  TraceID: abc123                                            │    │
│  │  │                                                          │    │
│  │  ├─ Span: productpage (100ms) [root span]                  │    │
│  │  │    ├─ Span: details (20ms)                              │    │
│  │  │    │    └─ HTTP GET /details                            │    │
│  │  │    └─ Span: reviews (70ms)                              │    │
│  │  │         └─ Span: ratings (15ms)                         │    │
│  │  │              └─ HTTP GET /ratings                       │    │
│  │  │                                                          │    │
│  │  총 응답 시간: 100ms                                        │    │
│  │  병목: reviews (70ms)                                       │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Context Propagation (컨텍스트 전파):                               │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  서비스 간 TraceID, SpanID를 HTTP 헤더로 전달                │    │
│  │  예: x-request-id, x-b3-traceid, x-b3-spanid               │    │
│  │  Istio는 이를 자동으로 처리!                                 │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.3 주요 분산 추적 시스템

| 시스템 | 특징 | 사용 |
|--------|------|------|
| **Jaeger** | CNCF 프로젝트, Uber 개발 | Istio 기본 통합 |
| **Zipkin** | Twitter 개발, 가벼움 | 레거시 시스템 |
| **Tempo** | Grafana Labs, 저비용 저장 | 대용량 트레이스 |
| **AWS X-Ray** | AWS 네이티브 | AWS 환경 |

---

## 🛠️ Part 2: Jaeger 설치 및 접속 (1시간)

### 실습 1: Jaeger 설치

```bash
# Istio addon에서 Jaeger 설치
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/jaeger.yaml

# 또는 로컬 Istio 설치 경로에서
kubectl apply -f $ISTIO_HOME/samples/addons/jaeger.yaml

# Pod 확인
kubectl get pods -n istio-system -l app=jaeger

# 예상 출력:
# NAME                      READY   STATUS    RESTARTS   AGE
# jaeger-xxx-yyy            1/1     Running   0          1m

# Service 확인
kubectl get svc -n istio-system | grep jaeger

# 예상 출력:
# jaeger-collector   ClusterIP   ...   14268/TCP,14250/TCP
# tracing            ClusterIP   ...   80/TCP
```

### 실습 2: Jaeger UI 접속

```bash
# 방법 1: 포트 포워딩
kubectl port-forward -n istio-system svc/tracing 16686:80 &

# 브라우저에서 접속
# http://localhost:16686

# 방법 2: istioctl 대시보드
istioctl dashboard jaeger
```

### 실습 3: Jaeger UI 구성 이해

```
┌─────────────────────────────────────────────────────────────────────┐
│  Jaeger UI 구성                                                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Search Panel:                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Service: [productpage ▼]  ← 서비스 선택                    │    │
│  │  Operation: [all ▼]        ← 특정 엔드포인트 선택           │    │
│  │  Tags: [http.status_code=500] ← 필터링                     │    │
│  │  Lookback: [Last Hour ▼]   ← 시간 범위                     │    │
│  │  Min/Max Duration: [___]   ← 지연 시간 필터                │    │
│  │  Limit Results: [20]       ← 결과 수                       │    │
│  │  [Find Traces]             ← 검색 버튼                     │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Results Panel:                                                     │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  TraceID        Service      Duration    Spans             │    │
│  │  abc123...      productpage  234ms       12                │    │
│  │  def456...      productpage  156ms       10                │    │
│  │  ghi789...      productpage  3.2s ⚠️    8                 │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Trace Detail (클릭 시):                                            │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Timeline View (Gantt Chart)                                │    │
│  │  ├─ productpage [====================] 234ms               │    │
│  │  │   ├─ details [====] 45ms                                │    │
│  │  │   └─ reviews [===============] 180ms                    │    │
│  │  │       └─ ratings [===] 30ms                             │    │
│  │                                                             │    │
│  │  각 Span 클릭 → 상세 정보 (Tags, Logs, Process)            │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 3: 트레이스 생성 및 분석 (1.5시간)

### 실습 4: 트래픽 생성

```bash
# Bookinfo 앱 확인
kubectl get pods -n default -l app=productpage

# 없다면 설치
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/bookinfo/platform/kube/bookinfo.yaml
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/bookinfo/networking/bookinfo-gateway.yaml

# Gateway URL 설정
export INGRESS_HOST=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
export INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].port}')
export GATEWAY_URL=$INGRESS_HOST:$INGRESS_PORT

# 트래픽 생성 (100개 요청)
for i in $(seq 1 100); do
  curl -s -o /dev/null "http://$GATEWAY_URL/productpage"
  sleep 0.5
done &

echo "트래픽 생성 중... Jaeger UI에서 확인하세요"
```

### 실습 5: Jaeger에서 Trace 검색

```bash
# Jaeger UI에서:
# 1. Service: productpage.default 선택
# 2. Find Traces 클릭
# 3. 결과에서 Trace 하나 선택
# 4. Timeline 뷰에서 각 Span 확인
```

### 실습 6: Trace 상세 분석

```
Trace 상세 화면에서 확인할 것:

1. 전체 Duration (응답 시간)
   - 100ms 미만: 정상
   - 100-500ms: 주의
   - 500ms 이상: 개선 필요

2. Span 개수
   - 예상보다 많으면 불필요한 호출 확인

3. 서비스별 소요 시간
   - 가장 오래 걸리는 서비스 = 병목 후보

4. 각 Span의 Tags
   - http.method: GET/POST
   - http.status_code: 200/500
   - http.url: 요청 경로
   - upstream_cluster: 목적지 서비스
   - response_flags: Envoy 플래그

5. 에러 Span (빨간색)
   - 에러 메시지 확인
   - 원인 서비스 식별
```

### 실습 7: 인위적 지연 추가 및 분석

```yaml
# delay-fault.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: ratings
spec:
  hosts:
  - ratings
  http:
  - fault:
      delay:
        percentage:
          value: 100
        fixedDelay: 3s
    route:
    - destination:
        host: ratings
```

```bash
# 지연 주입
kubectl apply -f delay-fault.yaml

# 트래픽 생성
for i in $(seq 1 10); do
  curl -s -o /dev/null "http://$GATEWAY_URL/productpage"
  sleep 1
done

# Jaeger에서 확인:
# 1. productpage 서비스 선택
# 2. Find Traces
# 3. Duration이 3초 이상인 Trace 클릭
# 4. ratings Span에서 3초 지연 확인

# 정리
kubectl delete virtualservice ratings
```

### 실습 8: 에러 추적

```yaml
# error-fault.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: reviews
spec:
  hosts:
  - reviews
  http:
  - fault:
      abort:
        percentage:
          value: 50
        httpStatus: 500
    route:
    - destination:
        host: reviews
```

```bash
# 에러 주입
kubectl apply -f error-fault.yaml

# 트래픽 생성
for i in $(seq 1 20); do
  curl -s -o /dev/null "http://$GATEWAY_URL/productpage"
  sleep 0.5
done

# Jaeger에서 확인:
# 1. Tags 필터: http.status_code=500
# 2. 빨간색 에러 Span 확인
# 3. reviews 서비스에서 500 에러 확인

# 정리
kubectl delete virtualservice reviews
```

---

## 📊 Part 4: 실무 활용 (30분)

### 4.1 Trace 검색 패턴

```bash
# Jaeger UI 검색 팁

# 1. 느린 요청 찾기
# Min Duration: 1000ms

# 2. 에러 요청 찾기
# Tags: error=true

# 3. 특정 사용자 요청 찾기
# Tags: user.id=user123

# 4. 특정 HTTP 상태 찾기
# Tags: http.status_code=500

# 5. 특정 엔드포인트 찾기
# Operation: /api/checkout
```

### 4.2 서비스 의존성 그래프

```bash
# Jaeger UI에서:
# 1. 상단 메뉴 "System Architecture" 또는 "Dependencies"
# 2. DAG (Directed Acyclic Graph) 뷰
# 3. 서비스 간 호출 관계 및 지연 시간 확인
```

### 4.3 Trace 비교

```bash
# Jaeger UI에서:
# 1. 두 개의 Trace 선택
# 2. Compare 버튼
# 3. 정상 요청과 느린 요청 비교
# 4. 차이점 식별
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | 분산 추적 개념 이해 | ☐ |
| 2 | Trace, Span 개념 이해 | ☐ |
| 3 | Jaeger 설치 | ☐ |
| 4 | Jaeger UI 접속 | ☐ |
| 5 | Trace 검색 및 분석 | ☐ |
| 6 | 지연 원인 식별 | ☐ |
| 7 | 에러 추적 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# Jaeger 설치
kubectl apply -f samples/addons/jaeger.yaml

# Jaeger 접속
kubectl port-forward -n istio-system svc/tracing 16686:80
# 또는
istioctl dashboard jaeger

# 트래픽 생성
curl http://$GATEWAY_URL/productpage
```

---

## 💡 면접 대비 핵심 포인트

### Q1: 분산 추적이란?
**A**: 마이크로서비스 환경에서 하나의 요청이 여러 서비스를 거치는 전체 흐름을 추적하는 기술입니다. TraceID로 요청을 식별하고, 각 서비스에서의 처리 시간(Span)을 기록합니다.

### Q2: Trace와 Span의 차이?
**A**: 
- **Trace**: 하나의 요청이 시스템을 통과하는 전체 여정
- **Span**: Trace 내의 개별 작업 단위 (서비스 호출, DB 쿼리 등)

### Q3: 성능 문제를 어떻게 찾나요?
**A**: Jaeger에서 Duration이 긴 Trace를 찾고, Timeline 뷰에서 가장 오래 걸리는 Span을 식별합니다. 해당 서비스의 로그와 메트릭을 함께 분석하여 원인을 파악합니다.

---

## 🔗 참고 자료

- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
- [OpenTelemetry](https://opentelemetry.io/)
- [Istio Distributed Tracing](https://istio.io/latest/docs/tasks/observability/distributed-tracing/)

---

## ➡️ 다음 학습: Day 70

**주제**: Prometheus + Grafana 통합
- Istio 메트릭 수집
- Grafana 대시보드
- 메트릭 기반 모니터링
