# 📅 Day 89: Istio 포트폴리오 테스트 및 개선

## 🎯 오늘의 목표

> **토스플레이스 핵심**: 전체 기능 테스트를 통해 프로덕션 준비 상태를 검증합니다.

---

## ⏰ 예상 소요 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 트래픽 관리 테스트 | 1시간 | 카나리, 라우팅 |
| 보안 테스트 | 1시간 | mTLS, RBAC |
| 복원력 테스트 | 1시간 | Retry, Circuit Breaker |
| 관찰성 확인 | 1시간 | Kiali, Jaeger, Grafana |

---

## 🧪 Part 1: 트래픽 관리 테스트

### 1.1 카나리 배포 검증

```bash
# 트래픽 비율 테스트 (v1:90%, v2:10%)
for i in {1..100}; do 
  curl -s http://api.portfolio.local/version
done | sort | uniq -c

# 예상 결과:
#   90 v1
#   10 v2
```

### 1.2 VirtualService 설정 확인

```bash
# 현재 VirtualService 확인
kubectl get vs -n istio-portfolio -o yaml

# 라우팅 규칙 검증
istioctl analyze -n istio-portfolio

# 예상: ✔ No validation issues found
```

### 1.3 헤더 기반 라우팅 테스트

```bash
# 일반 요청 → v1
curl -s http://api.portfolio.local/version
# 예상: v1

# 베타 유저 요청 → v2
curl -s -H "x-user-type: beta" http://api.portfolio.local/version
# 예상: v2
```

### 테스트 결과 기록

```markdown
| 테스트 항목 | 예상 결과 | 실제 결과 | 통과 |
|------------|----------|----------|------|
| 카나리 비율 (90:10) | v1=90, v2=10 | | ☐ |
| 헤더 기반 라우팅 | beta→v2 | | ☐ |
| 기본 라우팅 | →v1 | | ☐ |
```

---

## 🧪 Part 2: 보안 테스트

### 2.1 mTLS 검증

```bash
# mTLS 상태 확인
istioctl x authz check <pod-name> -n istio-portfolio

# 또는 PeerAuthentication 확인
kubectl get peerauthentication -n istio-portfolio -o yaml

# 암호화 통신 확인
istioctl proxy-config endpoints <pod-name> -n istio-portfolio | grep STRICT
```

### 2.2 네임스페이스 격리 테스트

```bash
# 다른 네임스페이스에서 접근 시도
kubectl run test --image=curlimages/curl --rm -it -n default -- \
  curl -s http://api.istio-portfolio.svc.cluster.local/health

# 예상: Connection refused 또는 RBAC denied
```

### 2.3 AuthorizationPolicy 테스트

```bash
# 허용된 소스에서 접근
kubectl exec -it deploy/frontend -n istio-portfolio -- \
  curl -s http://api:8080/health
# 예상: 200 OK

# 허용되지 않은 소스에서 접근
kubectl run test --image=curlimages/curl --rm -it -n istio-portfolio -- \
  curl -s http://api:8080/admin
# 예상: 403 Forbidden
```

### 테스트 결과 기록

```markdown
| 테스트 항목 | 예상 결과 | 실제 결과 | 통과 |
|------------|----------|----------|------|
| mTLS 활성화 | STRICT | | ☐ |
| 네임스페이스 격리 | 차단됨 | | ☐ |
| AuthorizationPolicy | 403 | | ☐ |
```

---

## 🧪 Part 3: 장애 복원력 테스트

### 3.1 Retry 테스트

```bash
# Fault Injection 설정 (50% 실패)
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
        httpStatus: 503
    route:
    - destination:
        host: api
    retries:
      attempts: 3
      perTryTimeout: 2s
      retryOn: 5xx
EOF

# 요청 테스트 (Retry로 인해 대부분 성공해야 함)
for i in {1..20}; do
  curl -s -o /dev/null -w "%{http_code}\n" http://api.portfolio.local/health
done | sort | uniq -c

# 예상: 대부분 200 (Retry 덕분에)
```

### 3.2 Circuit Breaker 테스트

```bash
# 부하 생성
kubectl run fortio --image=fortio/fortio --rm -it -n istio-portfolio -- \
  load -qps 100 -n 1000 http://api:8080/health

# 결과 분석:
# - Code 200: 정상 처리
# - Code 503: Circuit Breaker 발동

# Circuit Breaker 상태 확인
kubectl exec -it deploy/frontend -n istio-portfolio -c istio-proxy -- \
  pilot-agent request GET clusters | grep outlier
```

### 3.3 Timeout 테스트

```bash
# 느린 응답 시뮬레이션
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: api-timeout-test
  namespace: istio-portfolio
spec:
  hosts:
  - api
  http:
  - fault:
      delay:
        percentage:
          value: 100
        fixedDelay: 5s
    timeout: 3s
    route:
    - destination:
        host: api
EOF

# 테스트
time curl -s http://api.portfolio.local/health
# 예상: 3초 후 timeout 에러
```

### 테스트 결과 기록

```markdown
| 테스트 항목 | 예상 결과 | 실제 결과 | 통과 |
|------------|----------|----------|------|
| Retry (50% 실패) | 대부분 200 | | ☐ |
| Circuit Breaker | 503 발생 | | ☐ |
| Timeout (3s) | 3초 후 실패 | | ☐ |
```

---

## 🧪 Part 4: 관찰성 확인

### 4.1 Kiali 대시보드

```bash
# Kiali 접속
kubectl port-forward svc/kiali -n istio-system 20001:20001 &

# 확인 항목:
# - Graph: 서비스 간 트래픽 흐름
# - Applications: 앱 상태
# - Workloads: Pod 상태
# - Services: 서비스 상태
# - Istio Config: 설정 유효성
```

**Kiali 확인 체크리스트**:

| 항목 | 확인 내용 | 통과 |
|------|----------|------|
| 트래픽 흐름 | frontend → api → db | ☐ |
| mTLS 표시 | 자물쇠 아이콘 | ☐ |
| 에러율 | < 1% | ☐ |
| 설정 유효성 | 경고 없음 | ☐ |

### 4.2 Jaeger 분산 추적

```bash
# Jaeger 접속
kubectl port-forward svc/tracing -n istio-system 16686:16686 &

# 확인 항목:
# - Services: istio-portfolio 서비스들
# - Operations: 엔드포인트별 트레이스
# - 지연 시간: 각 서비스별 소요 시간
```

**Jaeger 확인 체크리스트**:

| 항목 | 확인 내용 | 통과 |
|------|----------|------|
| 서비스 목록 | 모든 서비스 표시 | ☐ |
| Trace 연결 | Span 연결 확인 | ☐ |
| 지연 분포 | P99 < 500ms | ☐ |

### 4.3 Grafana 메트릭

```bash
# Grafana 접속
kubectl port-forward svc/grafana -n istio-system 3000:3000 &

# 기본 ID: admin / prom-operator
```

**Grafana 확인 대시보드**:

| 대시보드 | 확인 메트릭 | 통과 |
|----------|-----------|------|
| Istio Mesh | 전체 요청량 | ☐ |
| Istio Service | 서비스별 성공률 | ☐ |
| Istio Workload | Pod별 리소스 | ☐ |
| Istio Performance | P50, P90, P99 | ☐ |

### 4.4 핵심 PromQL 쿼리

```promql
# 요청 성공률
sum(rate(istio_requests_total{response_code=~"2.*",destination_workload_namespace="istio-portfolio"}[5m])) 
/ 
sum(rate(istio_requests_total{destination_workload_namespace="istio-portfolio"}[5m]))

# P99 지연 시간
histogram_quantile(0.99, sum(rate(istio_request_duration_milliseconds_bucket{destination_workload_namespace="istio-portfolio"}[5m])) by (le, destination_workload))

# 에러율
sum(rate(istio_requests_total{response_code=~"5.*",destination_workload_namespace="istio-portfolio"}[5m])) 
/ 
sum(rate(istio_requests_total{destination_workload_namespace="istio-portfolio"}[5m]))
```

---

## 🔧 Part 5: 문제 해결

### 발견된 문제 기록 템플릿

```markdown
## 문제 1: [문제 설명]

**증상**:
- 

**원인 분석**:
1. 로그 확인: `kubectl logs ...`
2. 이벤트 확인: `kubectl get events ...`
3. 설정 확인: `istioctl analyze ...`

**해결 방법**:
1. 
2. 

**확인**:
- [ ] 테스트 통과

---

## 문제 2: [문제 설명]

**증상**:
- 

**원인 분석**:
- 

**해결 방법**:
- 

**확인**:
- [ ] 테스트 통과
```

---

## ✅ 오늘의 체크리스트

| # | 카테고리 | 항목 | 완료 |
|---|---------|------|------|
| 1 | 트래픽 | 카나리 비율 검증 | ☐ |
| 2 | 트래픽 | 헤더 기반 라우팅 | ☐ |
| 3 | 보안 | mTLS 활성화 확인 | ☐ |
| 4 | 보안 | 네임스페이스 격리 | ☐ |
| 5 | 보안 | AuthorizationPolicy | ☐ |
| 6 | 복원력 | Retry 동작 | ☐ |
| 7 | 복원력 | Circuit Breaker | ☐ |
| 8 | 복원력 | Timeout | ☐ |
| 9 | 관찰성 | Kiali 대시보드 | ☐ |
| 10 | 관찰성 | Jaeger 트레이스 | ☐ |
| 11 | 관찰성 | Grafana 메트릭 | ☐ |
| 12 | 정리 | 발견된 문제 해결 | ☐ |

---

## ➡️ 다음 학습: Day 90

**주제**: GitHub 업로드 및 Month 3 정리

