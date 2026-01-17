# 📅 Day 66: Traffic Mirroring - 안전한 신버전 테스트

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "배포 자동화 파이프라인 운영, 대규모 트래픽 환경 대응"

Traffic Mirroring(섀도잉)을 활용하여 실제 트래픽으로 신버전을 안전하게 테스트합니다.

---

## ⏰ 예상 학습 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 | 45분 | Traffic Mirroring 원리 |
| 실습 | 1.5시간 | 미러링 설정 및 테스트 |
| 심화 | 45분 | 프로덕션 활용 패턴 |

---

## 📚 Part 1: Traffic Mirroring 개념 (45분)

### Traffic Mirroring이란?

```
┌─────────────────────────────────────────────────────────────┐
│  Traffic Mirroring (Shadow Traffic)                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  일반 배포 (위험)                                           │
│  ┌─────────────────────────────────────────────────┐       │
│  │ User → v2 (신버전)                              │       │
│  │        ↓                                        │       │
│  │     실패! → 사용자 영향 ❌                      │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  Traffic Mirroring (안전)                                   │
│  ┌─────────────────────────────────────────────────┐       │
│  │ User → ┬──────────→ v1 (실제 응답)              │       │
│  │        │                                        │       │
│  │        └── 복사 ──→ v2 (응답 무시, 테스트만)    │       │
│  │                                                  │       │
│  │  ✓ 사용자에게 영향 없음                         │       │
│  │  ✓ 실제 트래픽으로 테스트                       │       │
│  │  ✓ v2의 로그/메트릭으로 동작 검증               │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 배포 전략 비교

| 전략 | 사용자 영향 | 실제 트래픽 테스트 | 롤백 시간 | 리소스 비용 |
|------|-------------|-------------------|----------|-------------|
| **Rolling Update** | 있음 | 바로 적용 | 수 분 | 낮음 |
| **Blue/Green** | 있음 (전환 시) | 스테이징만 | 즉시 | 높음 (2배) |
| **Canary** | 일부 있음 | 일부 실제 | 빠름 | 중간 |
| **Traffic Mirroring** | 없음 ✅ | 100% 실제 ✅ | 불필요 | 중간 |

### 사용 사례

```
┌─────────────────────────────────────────────────────────────┐
│  Traffic Mirroring 사용 사례                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 신버전 사전 검증                                        │
│  ┌─────────────────────────────────────────────────┐       │
│  │ - 새 API 버전 테스트                            │       │
│  │ - 성능 비교 (v1 vs v2)                          │       │
│  │ - 에러 발생 여부 확인                           │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  2. 새로운 알고리즘 검증                                    │
│  ┌─────────────────────────────────────────────────┐       │
│  │ - 추천 알고리즘 A/B 비교                        │       │
│  │ - ML 모델 성능 비교                             │       │
│  │ - 결과 정확도 검증                              │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  3. 데이터베이스 마이그레이션                               │
│  ┌─────────────────────────────────────────────────┐       │
│  │ - 새 DB 스키마 호환성 테스트                    │       │
│  │ - 쿼리 성능 비교                                │       │
│  │ - 데이터 일관성 검증                            │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  4. 토스플레이스 활용                                       │
│  ┌─────────────────────────────────────────────────┐       │
│  │ - 결제 로직 변경 사전 검증                      │       │
│  │ - POS 단말 통신 프로토콜 변경                   │       │
│  │ - 대규모 이벤트 전 부하 테스트                  │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 동작 원리

```
┌─────────────────────────────────────────────────────────────┐
│  Istio Traffic Mirroring 동작                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                    Envoy Sidecar                            │
│                    ┌─────────────────┐                      │
│                    │                 │                      │
│  Client Request ───▶│  1. 요청 수신  │                      │
│                    │  2. 요청 복사   │                      │
│                    │                 │                      │
│                    └────────┬────────┘                      │
│                             │                               │
│              ┌──────────────┴──────────────┐               │
│              │                             │               │
│              ▼                             ▼               │
│       ┌─────────────┐             ┌─────────────┐         │
│       │   v1 (실제) │             │  v2 (미러)  │         │
│       └──────┬──────┘             └──────┬──────┘         │
│              │                            │                │
│              ▼                            ▼                │
│       Response → Client             Response → Drop       │
│       (실제 응답)                   (응답 무시)            │
│                                                             │
│  특이사항:                                                  │
│  - 미러링 요청의 Host 헤더에 '-shadow' 추가                │
│  - 예: reviews → reviews-shadow                            │
│  - 미러링 요청은 "fire and forget"                         │
│  - v2 응답은 클라이언트로 전달되지 않음                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: 실습 (1.5시간)

### 실습 환경 준비

```bash
# Bookinfo 샘플이 없다면 설치
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/bookinfo/platform/kube/bookinfo.yaml
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/bookinfo/networking/destination-rule-all.yaml

# 모든 버전 준비 확인
kubectl get pods -l app=reviews
# reviews-v1, reviews-v2, reviews-v3 확인
```

### 실습 1: 기본 미러링 설정

```yaml
# 100% 트래픽 미러링
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: reviews
spec:
  hosts:
  - reviews
  http:
  - route:
    - destination:
        host: reviews
        subset: v1          # 실제 트래픽은 v1으로
      weight: 100
    mirror:
      host: reviews
      subset: v2            # 복사본을 v2로 전송
    mirrorPercentage:
      value: 100.0          # 100% 미러링
EOF
```

### 실습 2: 미러링 확인

```bash
# 트래픽 생성
for i in {1..10}; do
  curl -s http://$GATEWAY_URL/productpage > /dev/null
  sleep 1
done

# v1 로그 확인 (실제 응답)
kubectl logs -l app=reviews,version=v1 -c istio-proxy --tail=5

# v2 로그 확인 (미러링된 요청)
kubectl logs -l app=reviews,version=v2 -c istio-proxy --tail=5
# Host 헤더에 '-shadow' 접미사 확인
# "reviews-shadow" 형태로 표시됨
```

### 실습 3: 부분 미러링 (10%)

```yaml
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: reviews
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
      value: 10.0           # 10%만 미러링
EOF
```

### 실습 4: 여러 버전에 미러링

```yaml
# 주의: Istio는 단일 mirror만 지원
# 여러 버전 테스트가 필요하면 EnvoyFilter 사용
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: reviews
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
      subset: v3            # v3으로 변경
    mirrorPercentage:
      value: 100.0
EOF
```

### 실습 5: 미러링 모니터링

```bash
# Kiali에서 확인
# Graph에서 "reviews" 서비스 선택
# v1으로 가는 실선 (실제 트래픽)
# v2로 가는 점선 (미러 트래픽) 확인

# Prometheus 쿼리로 확인
# 미러 요청은 별도 집계되지 않음
# v2의 메트릭을 확인하여 미러링 동작 확인

# Grafana 대시보드에서 비교
# v1과 v2의 응답 시간, 에러율 비교
```

### 실습 6: 조건부 미러링

```yaml
# 특정 조건에서만 미러링
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: reviews
spec:
  hosts:
  - reviews
  http:
  # 헤더가 있는 요청만 미러링
  - match:
    - headers:
        x-mirror-test:
          exact: "true"
    route:
    - destination:
        host: reviews
        subset: v1
    mirror:
      host: reviews
      subset: v2
    mirrorPercentage:
      value: 100.0
  # 나머지 요청은 미러링 없이 v1으로
  - route:
    - destination:
        host: reviews
        subset: v1
EOF
```

```bash
# 일반 요청 (미러링 없음)
curl http://$GATEWAY_URL/productpage

# 미러링 요청 (v2로도 전송)
curl -H "x-mirror-test: true" http://$GATEWAY_URL/productpage
```

---

## 📚 Part 3: 프로덕션 활용 패턴 (45분)

### 패턴 1: 점진적 미러링 증가

```yaml
# Day 1: 10% 미러링으로 시작
mirrorPercentage:
  value: 10.0

# Day 2: 문제 없으면 50%로 증가
mirrorPercentage:
  value: 50.0

# Day 3: 100% 미러링
mirrorPercentage:
  value: 100.0

# Day 4: 미러링 → 카나리로 전환
# 실제 트래픽 10%를 v2로
```

### 패턴 2: 미러링 + 메트릭 비교

```bash
# 미러링 전후 메트릭 비교 스크립트
cat << 'EOF' > compare_versions.sh
#!/bin/bash

echo "=== v1 Metrics ==="
kubectl exec $(kubectl get pod -l app=reviews,version=v1 -o jsonpath='{.items[0].metadata.name}') \
  -c istio-proxy -- curl -s localhost:15000/stats/prometheus | \
  grep -E "istio_request_duration|istio_requests_total" | head -10

echo ""
echo "=== v2 Metrics (Mirrored) ==="
kubectl exec $(kubectl get pod -l app=reviews,version=v2 -o jsonpath='{.items[0].metadata.name}') \
  -c istio-proxy -- curl -s localhost:15000/stats/prometheus | \
  grep -E "istio_request_duration|istio_requests_total" | head -10
EOF

chmod +x compare_versions.sh
./compare_versions.sh
```

### 패턴 3: 미러링 결과 자동 분석

```yaml
# Prometheus Alert Rule 예시
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: mirror-comparison
spec:
  groups:
  - name: mirror-alerts
    rules:
    - alert: MirrorVersionHigherErrorRate
      expr: |
        (
          sum(rate(istio_requests_total{destination_service="reviews.default.svc.cluster.local",destination_version="v2",response_code=~"5.."}[5m]))
          /
          sum(rate(istio_requests_total{destination_service="reviews.default.svc.cluster.local",destination_version="v2"}[5m]))
        )
        >
        (
          sum(rate(istio_requests_total{destination_service="reviews.default.svc.cluster.local",destination_version="v1",response_code=~"5.."}[5m]))
          /
          sum(rate(istio_requests_total{destination_service="reviews.default.svc.cluster.local",destination_version="v1"}[5m]))
        ) * 1.5
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: "미러링 버전(v2)의 에러율이 v1보다 50% 이상 높음"
```

### 토스플레이스 실무 워크플로우

```
┌─────────────────────────────────────────────────────────────┐
│  결제 서비스 배포 워크플로우                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Stage 1: Development                                       │
│  ┌─────────────────────────────────────────────────┐       │
│  │ - 단위 테스트, 통합 테스트                      │       │
│  │ - 개발 환경 배포                                │       │
│  └─────────────────────────────────────────────────┘       │
│                    ↓                                        │
│  Stage 2: Staging                                           │
│  ┌─────────────────────────────────────────────────┐       │
│  │ - 스테이징 환경 배포                            │       │
│  │ - QA 테스트                                     │       │
│  └─────────────────────────────────────────────────┘       │
│                    ↓                                        │
│  Stage 3: Production Mirroring ✨                           │
│  ┌─────────────────────────────────────────────────┐       │
│  │ - 10% 트래픽 미러링 시작                        │       │
│  │ - 24시간 동안 모니터링                          │       │
│  │ - 에러율, 응답시간 비교                         │       │
│  │ - 자동 알람 확인                                │       │
│  └─────────────────────────────────────────────────┘       │
│                    ↓                                        │
│  Stage 4: Canary Release                                    │
│  ┌─────────────────────────────────────────────────┐       │
│  │ - 미러링 통과 후 5% 실제 트래픽                 │       │
│  │ - 점진적으로 트래픽 증가                        │       │
│  └─────────────────────────────────────────────────┘       │
│                    ↓                                        │
│  Stage 5: Full Rollout                                      │
│  ┌─────────────────────────────────────────────────┐       │
│  │ - 100% 트래픽 전환                              │       │
│  │ - 구버전 정리                                   │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | Traffic Mirroring 개념 이해 | ☐ |
| 2 | 기본 미러링 설정 실습 | ☐ |
| 3 | 부분 미러링 (percentage) 설정 | ☐ |
| 4 | 미러링 결과 로그 확인 | ☐ |
| 5 | 조건부 미러링 설정 | ☐ |
| 6 | 미러링 vs 카나리 차이 이해 | ☐ |

---

## 🔑 핵심 설정

```yaml
# Traffic Mirroring 핵심 설정
mirror:
  host: reviews
  subset: v2
mirrorPercentage:
  value: 100.0  # 0-100
```

---

## 📝 면접 대비 질문

### Q1: 카나리 배포와 Traffic Mirroring의 차이는?
> "카나리 배포는 실제 사용자 일부가 신버전을 사용합니다. 신버전에 문제가 있으면 해당 사용자가 영향을 받습니다. Traffic Mirroring은 모든 사용자가 구버전을 사용하면서, 트래픽 복사본만 신버전으로 전송합니다. 미러링은 사용자에게 전혀 영향 없이 신버전을 실제 트래픽으로 테스트할 수 있습니다."

### Q2: Traffic Mirroring의 한계점은?
> "첫째, 쓰기 작업(POST, PUT, DELETE)은 중복 실행될 수 있어 주의가 필요합니다. 신버전이 DB에 쓰기를 수행하면 데이터 중복이 발생할 수 있습니다. 둘째, 미러링된 응답은 클라이언트에 전달되지 않아, 응답 내용 자체의 정확성은 로그로만 확인해야 합니다. 셋째, 네트워크 대역폭과 백엔드 리소스가 약 2배 필요합니다."

### Q3: 언제 Traffic Mirroring을 사용하나요?
> "결제 시스템처럼 장애 시 비즈니스 영향이 큰 서비스에서 신버전 검증에 사용합니다. 특히 알고리즘 변경, DB 마이그레이션, 성능 최적화 작업 후 프로덕션 트래픽으로 사전 검증할 때 유용합니다. 미러링으로 충분히 검증한 후 카나리 배포로 전환합니다."

---

## ➡️ 다음 학습: Day 67

**주제**: Week 9 복습 및 종합 실습
- 트래픽 관리 전략 종합 정리
- 실습 내용 복습
- 시나리오 기반 문제 해결
