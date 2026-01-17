# Day 63: DestinationRule 상세

## 오늘의 목표

토스플레이스 연결점: "Kubernetes와 Service Mesh에 대한 경험"
"Istio 기반의 서비스 메시 운영"

DestinationRule로 서비스 정책을 정의합니다. subset, 로드밸런싱, Circuit Breaker, TLS 등 트래픽이 목적지에 도달하는 방식을 세밀하게 제어합니다.

---

## 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 핵심 개념 | 45분 | DestinationRule 역할, 구조 |
| Subset 실습 | 45분 | 버전별 Pod 그룹 정의 |
| 트래픽 정책 | 1시간 | LB, Connection Pool |
| Circuit Breaker | 1시간 | Outlier Detection |
| TLS 설정 | 30분 | mTLS 모드 |

---

## Part 1: DestinationRule이란? (45분)

### 1.1 VirtualService vs DestinationRule

```
트래픽 흐름에서의 역할:

Client -> VirtualService -> DestinationRule -> Pod

VirtualService: "어디로 보낼까?" (라우팅 규칙)
- 호스트 매칭
- 경로 기반 라우팅
- 버전별 트래픽 분배

DestinationRule: "어떻게 보낼까?" (목적지 정책)
- subset (버전) 정의
- 로드밸런싱 알고리즘
- 연결 풀 설정
- Circuit Breaker
- TLS 설정
```

```
예시 흐름:

1. 요청: GET /api/users
2. VirtualService: "이 요청은 users 서비스 v2로 80%" -> subset: v2
3. DestinationRule: "v2 subset = version:v2 라벨을 가진 Pod"
                    "로드밸런싱: ROUND_ROBIN"
                    "최대 연결: 100"
4. Pod 선택 및 요청 전송
```

### 1.2 DestinationRule 기능 개요

| 기능 | 설명 | 사용 사례 |
|------|------|----------|
| **Subset** | 버전별 Pod 그룹 정의 | Canary, A/B 테스트 |
| **Load Balancing** | 로드밸런싱 알고리즘 | ROUND_ROBIN, LEAST_CONN |
| **Connection Pool** | 연결 풀 설정 | 최대 연결, 대기 요청 제한 |
| **Outlier Detection** | 이상 인스턴스 제거 | Circuit Breaker, 장애 격리 |
| **TLS** | 클라이언트 TLS 설정 | mTLS, 외부 서비스 TLS |

### 1.3 DestinationRule 구조

```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: my-service
  namespace: default
spec:
  host: my-service           # 대상 서비스
  trafficPolicy:             # 기본 트래픽 정책
    loadBalancer:
      simple: ROUND_ROBIN
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        http2MaxRequests: 1000
    outlierDetection:
      consecutive5xxErrors: 5
      interval: 10s
      baseEjectionTime: 30s
    tls:
      mode: ISTIO_MUTUAL
  subsets:                   # 버전별 그룹
  - name: v1
    labels:
      version: v1
  - name: v2
    labels:
      version: v2
    trafficPolicy:           # subset별 정책 오버라이드 가능
      loadBalancer:
        simple: LEAST_CONN
```

---

## Part 2: Subset 정의 (45분)

### 실습 1: 기본 Subset 생성

Subset은 라벨로 Pod 그룹을 정의합니다.

```bash
# 예제 Deployment (v1, v2)
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: reviews-v1
spec:
  replicas: 2
  selector:
    matchLabels:
      app: reviews
      version: v1
  template:
    metadata:
      labels:
        app: reviews
        version: v1
    spec:
      containers:
      - name: reviews
        image: istio/examples-bookinfo-reviews-v1:1.16.2
        ports:
        - containerPort: 9080
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: reviews-v2
spec:
  replicas: 2
  selector:
    matchLabels:
      app: reviews
      version: v2
  template:
    metadata:
      labels:
        app: reviews
        version: v2
    spec:
      containers:
      - name: reviews
        image: istio/examples-bookinfo-reviews-v2:1.16.2
        ports:
        - containerPort: 9080
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: reviews-v3
spec:
  replicas: 2
  selector:
    matchLabels:
      app: reviews
      version: v3
  template:
    metadata:
      labels:
        app: reviews
        version: v3
    spec:
      containers:
      - name: reviews
        image: istio/examples-bookinfo-reviews-v3:1.16.2
        ports:
        - containerPort: 9080
EOF
```

```yaml
# DestinationRule로 Subset 정의
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: reviews
spec:
  host: reviews
  subsets:
  - name: v1
    labels:
      version: v1
  - name: v2
    labels:
      version: v2
  - name: v3
    labels:
      version: v3
EOF
```

### 실습 2: VirtualService에서 Subset 사용

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
        subset: v1          # DestinationRule의 subset 참조
      weight: 50
    - destination:
        host: reviews
        subset: v2
      weight: 30
    - destination:
        host: reviews
        subset: v3
      weight: 20
EOF
```

```bash
# 적용 확인
kubectl get destinationrule reviews -o yaml
kubectl get virtualservice reviews -o yaml

# 트래픽 테스트
for i in {1..10}; do
  kubectl exec deploy/sleep -- curl -s reviews:9080/reviews/0 | grep -o "reviews-v[0-9]"
done
```

---

## Part 3: 트래픽 정책 (1시간)

### 실습 3: 로드밸런싱 정책

```yaml
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: reviews
spec:
  host: reviews
  trafficPolicy:
    loadBalancer:
      simple: ROUND_ROBIN    # 기본값: 순차 분배
  subsets:
  - name: v1
    labels:
      version: v1
  - name: v2
    labels:
      version: v2
    trafficPolicy:
      loadBalancer:
        simple: LEAST_CONN   # v2는 연결 적은 곳으로
  - name: v3
    labels:
      version: v3
    trafficPolicy:
      loadBalancer:
        simple: RANDOM       # v3는 랜덤
EOF
```

**로드밸런싱 알고리즘:**

| 알고리즘 | 설명 | 사용 사례 |
|---------|------|----------|
| `ROUND_ROBIN` | 순차 분배 (기본값) | 일반적인 경우 |
| `LEAST_CONN` | 연결 수 적은 곳 | 처리 시간이 다양한 경우 |
| `RANDOM` | 랜덤 선택 | 캐시 효율 높을 때 |
| `PASSTHROUGH` | 원래 목적지 유지 | 특수한 경우 |

**Consistent Hash (세션 친화성):**
```yaml
trafficPolicy:
  loadBalancer:
    consistentHash:
      httpHeaderName: x-user-id    # 헤더 기반
      # 또는
      httpCookie:
        name: session-id
        ttl: 3600s
      # 또는
      useSourceIp: true            # 소스 IP 기반
```

### 실습 4: Connection Pool 설정

```yaml
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: reviews
spec:
  host: reviews
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100            # 최대 TCP 연결 수
        connectTimeout: 30ms           # 연결 타임아웃
        tcpKeepalive:
          time: 7200s                  # Keep-alive 시간
          interval: 75s
      http:
        h2UpgradePolicy: UPGRADE       # HTTP/2 업그레이드
        http1MaxPendingRequests: 100   # HTTP/1.1 대기 요청
        http2MaxRequests: 1000         # HTTP/2 최대 요청
        maxRequestsPerConnection: 10   # 연결당 최대 요청
        maxRetries: 3                  # 최대 재시도
        idleTimeout: 60s               # 유휴 타임아웃
  subsets:
  - name: v1
    labels:
      version: v1
EOF
```

**Connection Pool 설정 가이드:**

```
TCP 설정:
- maxConnections: 동시 TCP 연결 제한
  -> 백엔드 과부하 방지
  -> 값이 너무 낮으면 503 발생

HTTP 설정:
- http1MaxPendingRequests: 대기열 길이
  -> 초과 시 503 Circuit Breaker 트리거
- http2MaxRequests: HTTP/2 동시 요청
- maxRequestsPerConnection: 연결 재사용 횟수
  -> 낮으면 연결 자주 새로 생성
  -> 높으면 하나의 연결에 부하 집중
```

---

## Part 4: Circuit Breaker (1시간)

### 실습 5: Outlier Detection (Circuit Breaker)

Outlier Detection은 문제가 있는 인스턴스를 자동으로 격리합니다.

```yaml
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: reviews
spec:
  host: reviews
  trafficPolicy:
    outlierDetection:
      consecutive5xxErrors: 5      # 연속 5xx 에러 5회
      interval: 10s                # 10초 간격으로 검사
      baseEjectionTime: 30s        # 30초간 제외
      maxEjectionPercent: 50       # 최대 50%만 제외
      minHealthPercent: 30         # 최소 30% 건강해야 함
      consecutiveGatewayErrors: 5  # 게이트웨이 에러도 포함
  subsets:
  - name: v1
    labels:
      version: v1
EOF
```

**Outlier Detection 파라미터:**

| 파라미터 | 설명 | 권장값 |
|---------|------|--------|
| `consecutive5xxErrors` | 연속 에러 횟수 | 5 |
| `interval` | 검사 간격 | 10s |
| `baseEjectionTime` | 제외 시간 | 30s |
| `maxEjectionPercent` | 최대 제외 비율 | 50 |
| `minHealthPercent` | 최소 건강 비율 | 30 |

**동작 원리:**
```
1. reviews-v1-pod-a가 연속 5번 5xx 반환
2. Outlier Detection이 감지
3. pod-a를 30초간 트래픽에서 제외
4. 30초 후 다시 포함
5. 또 실패하면 60초 제외 (exponential backoff)
6. 최대 50%까지만 제외 (전체 장애 방지)
```

### 실습 6: Circuit Breaker 테스트

```bash
# 장애 주입으로 테스트
kubectl apply -f - <<EOF
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
          value: 50            # 50% 요청에 500 에러
        httpStatus: 500
    route:
    - destination:
        host: reviews
        subset: v1
EOF

# 여러 번 요청
for i in {1..20}; do
  kubectl exec deploy/sleep -- curl -s -o /dev/null -w "%{http_code}\n" reviews:9080/reviews/0
  sleep 1
done

# Envoy 통계에서 ejection 확인
kubectl exec deploy/productpage -c istio-proxy -- \
  curl -s localhost:15000/stats | grep outlier

# 출력 예시:
# cluster.outbound|9080|v1|reviews.default.svc.cluster.local.outlier_detection.ejections_active: 1
# cluster.outbound|9080|v1|reviews.default.svc.cluster.local.outlier_detection.ejections_total: 2
```

### 실습 7: Connection Pool + Outlier Detection 조합

실제 프로덕션에서는 두 가지를 함께 사용합니다.

```yaml
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: reviews-production
spec:
  host: reviews
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
        connectTimeout: 100ms
      http:
        http1MaxPendingRequests: 100
        http2MaxRequests: 1000
        maxRequestsPerConnection: 100
        maxRetries: 3
    outlierDetection:
      consecutive5xxErrors: 3
      interval: 5s
      baseEjectionTime: 30s
      maxEjectionPercent: 30
      minHealthPercent: 50
    loadBalancer:
      simple: LEAST_CONN
  subsets:
  - name: v1
    labels:
      version: v1
  - name: v2
    labels:
      version: v2
EOF
```

---

## Part 5: TLS 설정 (30분)

### 실습 8: mTLS 모드 설정

```yaml
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: reviews
spec:
  host: reviews
  trafficPolicy:
    tls:
      mode: ISTIO_MUTUAL     # Istio의 mTLS 사용
  subsets:
  - name: v1
    labels:
      version: v1
EOF
```

**TLS 모드:**

| 모드 | 설명 | 사용 사례 |
|------|------|----------|
| `DISABLE` | TLS 비활성화 | 레거시 서비스 |
| `SIMPLE` | 단방향 TLS | 외부 HTTPS 서비스 |
| `MUTUAL` | 양방향 TLS (클라이언트 인증서 필요) | 외부 mTLS 서비스 |
| `ISTIO_MUTUAL` | Istio의 자동 mTLS | 메시 내 서비스 (권장) |

### 실습 9: 외부 서비스 TLS

```yaml
# 외부 서비스 등록
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: ServiceEntry
metadata:
  name: external-api
spec:
  hosts:
  - api.external.com
  ports:
  - number: 443
    name: https
    protocol: HTTPS
  resolution: DNS
  location: MESH_EXTERNAL
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: external-api
spec:
  host: api.external.com
  trafficPolicy:
    tls:
      mode: SIMPLE              # 서버만 인증 (일반 HTTPS)
      sni: api.external.com     # SNI 설정
EOF
```

---

## 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | VirtualService vs DestinationRule 이해 | 역할 구분 | |
| 2 | Subset 정의 | 버전별 Pod 그룹 | |
| 3 | 로드밸런싱 정책 | ROUND_ROBIN, LEAST_CONN | |
| 4 | Consistent Hash | 세션 친화성 | |
| 5 | Connection Pool 설정 | TCP, HTTP 제한 | |
| 6 | Outlier Detection | Circuit Breaker | |
| 7 | TLS 모드 | ISTIO_MUTUAL, SIMPLE | |
| 8 | Circuit Breaker 테스트 | 장애 주입, ejection 확인 | |

---

## 핵심 필드 정리

```yaml
spec:
  host: xxx                        # 대상 서비스 (필수)
  trafficPolicy:
    loadBalancer:
      simple: ROUND_ROBIN | RANDOM | LEAST_CONN
      consistentHash:
        httpHeaderName: x-user-id  # 또는 httpCookie, useSourceIp
    connectionPool:
      tcp:
        maxConnections: 100
        connectTimeout: 100ms
      http:
        http1MaxPendingRequests: 100
        http2MaxRequests: 1000
        maxRequestsPerConnection: 100
    outlierDetection:
      consecutive5xxErrors: 5
      interval: 10s
      baseEjectionTime: 30s
      maxEjectionPercent: 50
    tls:
      mode: ISTIO_MUTUAL | SIMPLE | MUTUAL | DISABLE
  subsets:
  - name: v1
    labels:
      version: v1
    trafficPolicy:                 # subset별 오버라이드 가능
      loadBalancer:
        simple: LEAST_CONN
```

---

## 면접 대비 핵심 포인트

**Q1: VirtualService와 DestinationRule의 차이는?**
> "VirtualService는 '어디로 보낼지' 라우팅 규칙을 정의하고, DestinationRule은 '어떻게 보낼지' 목적지 정책을 정의합니다. VirtualService에서 subset을 참조하면, DestinationRule에서 해당 subset이 어떤 Pod 그룹인지 정의합니다."

**Q2: Istio에서 Circuit Breaker는 어떻게 구현하나요?**
> "DestinationRule의 outlierDetection을 사용합니다. 연속 에러 횟수(consecutive5xxErrors), 검사 간격(interval), 제외 시간(baseEjectionTime)을 설정하면 Envoy가 자동으로 문제 있는 인스턴스를 격리합니다."

**Q3: Connection Pool 설정이 왜 중요한가요?**
> "maxConnections와 http2MaxRequests로 백엔드 과부하를 방지합니다. 제한을 초과하면 503을 반환하여 빠른 실패를 유도하고, 연쇄 장애를 방지합니다. Outlier Detection과 함께 사용하면 더 견고한 시스템을 만들 수 있습니다."

---

## 학습 기록

```
학습일: ____년 __월 __일
실제 소요 시간: ____시간

완료한 실습:
- [ ] Subset 정의
- [ ] 로드밸런싱 정책
- [ ] Connection Pool
- [ ] Outlier Detection (Circuit Breaker)
- [ ] TLS 설정

이해가 어려웠던 부분:

추가 학습 필요 항목:
```

---

## 정리

```bash
# 리소스 삭제
kubectl delete virtualservice reviews
kubectl delete destinationrule reviews
kubectl delete destinationrule reviews-production
kubectl delete destinationrule external-api
kubectl delete serviceentry external-api
kubectl delete deploy reviews-v1 reviews-v2 reviews-v3
```

---

## 다음 학습: Day 64

주제: Gateway 설정
- Istio Ingress Gateway
- Gateway 리소스 정의
- 외부 트래픽 라우팅
