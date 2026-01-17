# 📅 Day 65: Timeout, Retry, Fault Injection

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "장애에 흔들리지 않는 시스템 운영"
> 장애 복원력(Resiliency)은 대규모 마이크로서비스 환경에서 필수

Istio의 Timeout, Retry, Fault Injection 기능을 학습하여 장애에 강한 서비스를 설계합니다. 실제 장애 상황을 시뮬레이션하고 시스템의 복원력을 테스트하는 Chaos Engineering 기초를 익힙니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 장애 복원력 개념 | 45분 | 패턴, 필요성 |
| Timeout/Retry | 1.5시간 | 설정 및 테스트 |
| Fault Injection | 1.5시간 | Delay, Abort |
| 종합 실습 | 30분 | 복합 시나리오 |

---

## 📚 Part 1: 장애 복원력 개념 (45분)

### 1.1 왜 장애 복원력이 필요한가?

```
┌─────────────────────────────────────────────────────────────────────┐
│  마이크로서비스 환경의 장애 특성                                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  단일 서비스 (Monolith)                                             │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  장애 발생 → 전체 서비스 중단                               │    │
│  │  하지만 장애 포인트가 적음                                  │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  마이크로서비스                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  A → B → C → D (서비스 체인)                               │    │
│  │                                                             │    │
│  │  문제 1: D가 느려지면?                                      │    │
│  │  → C 대기 → B 대기 → A 대기 → 전체 응답 지연              │    │
│  │  → Cascading Failure (연쇄 장애)                           │    │
│  │                                                             │    │
│  │  문제 2: C가 간헐적으로 실패하면?                          │    │
│  │  → 사용자 일부가 에러 경험                                 │    │
│  │                                                             │    │
│  │  문제 3: 네트워크 지연/손실이 있으면?                      │    │
│  │  → 연결 타임아웃, 요청 손실                                │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  해결책: 장애 복원력 패턴 (Resiliency Patterns)                     │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 장애 복원력 패턴

| 패턴 | 설명 | Istio 구현 | 사용 시점 |
|------|------|-----------|----------|
| **Timeout** | 응답 대기 시간 제한 | VirtualService | 느린 서비스로 인한 연쇄 지연 방지 |
| **Retry** | 실패 시 자동 재시도 | VirtualService | 일시적 오류 복구 |
| **Circuit Breaker** | 연속 실패 시 요청 차단 | DestinationRule | 실패 서비스 격리 |
| **Bulkhead** | 리소스 격리 | DestinationRule | 장애 확산 방지 |
| **Fault Injection** | 의도적 장애 주입 | VirtualService | 복원력 테스트 |

```
┌─────────────────────────────────────────────────────────────────────┐
│  패턴 적용 흐름                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Client → Service A → Service B → Service C                        │
│                                                                      │
│  정상 흐름:                                                          │
│  Request → A (100ms) → B (200ms) → C (300ms) → Response (600ms)    │
│                                                                      │
│  C가 느려짐 (3초):                                                   │
│  Without Timeout: A 대기 → B 대기 → 전체 타임아웃 (30초+)          │
│  With Timeout (1초): C 타임아웃 → 빠른 실패 반환 → 다른 처리       │
│                                                                      │
│  C가 간헐적 실패:                                                    │
│  Without Retry: 사용자 에러 경험                                   │
│  With Retry (3회): 재시도 성공 → 사용자 정상 응답                   │
│                                                                      │
│  C가 지속 실패:                                                      │
│  Without Circuit Breaker: 계속 요청 → 리소스 낭비                  │
│  With Circuit Breaker: 요청 차단 → 빠른 실패 → 복구 후 재개        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: Timeout 설정 (45분)

### 2.1 Timeout 개념

```
┌─────────────────────────────────────────────────────────────────────┐
│  Timeout 동작                                                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Client ─────────────→ Service                                      │
│           요청                                                       │
│           │                                                         │
│           │ timeout: 3s                                             │
│           │                                                         │
│           ├── 정상 (1초 내 응답) → 성공 ✅                          │
│           │                                                         │
│           └── 지연 (5초 소요) → 3초 후 타임아웃 ❌                  │
│               → HTTP 504 Gateway Timeout                            │
│               → 클라이언트는 빠르게 다른 처리 가능                  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 실습 1: 기본 Timeout 설정

```yaml
# timeout-basic.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: reviews
  namespace: default
spec:
  hosts:
  - reviews
  http:
  - route:
    - destination:
        host: reviews
        subset: v1
    timeout: 3s    # 3초 내 응답 없으면 타임아웃
```

```bash
# Bookinfo 앱이 배포되어 있다고 가정
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
    timeout: 3s
EOF

# 확인
kubectl get vs reviews -o yaml
```

### 실습 2: Timeout 테스트 (Delay 주입으로)

```yaml
# timeout-test.yaml
# ratings 서비스에 5초 지연 주입
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
        fixedDelay: 5s    # 모든 요청에 5초 지연
    route:
    - destination:
        host: ratings
```

```bash
kubectl apply -f - <<EOF
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
        fixedDelay: 5s
    route:
    - destination:
        host: ratings
EOF

# 테스트 (reviews의 3초 timeout으로 인해 ratings 응답 전에 실패)
kubectl run test-timeout --rm -it --restart=Never --image=curlimages/curl -- \
  sh -c "time curl -s productpage:9080/productpage | grep -o 'Ratings\|Sorry'"

# 예상: reviews가 3초 후 타임아웃 → "Sorry" 메시지
# 실제 ratings 응답(5초)을 기다리지 않음
```

---

## 🛠️ Part 3: Retry 설정 (45분)

### 3.1 Retry 개념

```
┌─────────────────────────────────────────────────────────────────────┐
│  Retry 동작                                                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Client ─────────────→ Service                                      │
│                                                                      │
│  attempts: 3 (최대 3회 시도)                                        │
│  perTryTimeout: 2s (각 시도당 2초 타임아웃)                         │
│                                                                      │
│  시도 1: 실패 (500 에러)                                            │
│      ↓                                                               │
│  시도 2: 실패 (연결 리셋)                                           │
│      ↓                                                               │
│  시도 3: 성공 ✅                                                     │
│      ↓                                                               │
│  Client에게 성공 응답 반환                                          │
│                                                                      │
│  만약 3회 모두 실패 → Client에게 에러 반환                          │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 실습 3: Retry 설정

```yaml
# retry-config.yaml
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
    timeout: 10s
    retries:
      attempts: 3              # 최대 3회 재시도
      perTryTimeout: 2s        # 각 시도당 2초 타임아웃
      retryOn: 5xx,reset,connect-failure,retriable-4xx
```

```bash
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
    timeout: 10s
    retries:
      attempts: 3
      perTryTimeout: 2s
      retryOn: 5xx,reset,connect-failure
EOF
```

### 3.2 retryOn 옵션 상세

| 옵션 | 설명 | 재시도 조건 |
|------|------|-----------|
| `5xx` | 5xx 서버 에러 | 500, 502, 503, 504 등 |
| `reset` | 연결 리셋 | TCP RST 수신 |
| `connect-failure` | 연결 실패 | 연결 수립 실패 |
| `retriable-4xx` | 재시도 가능 4xx | 408, 409 등 |
| `gateway-error` | 게이트웨이 에러 | 502, 503, 504 |
| `refused-stream` | HTTP/2 스트림 거부 | REFUSED_STREAM |

### 실습 4: Retry와 Abort 테스트

```yaml
# abort-for-retry-test.yaml
# 50%의 요청에 503 에러 주입
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: ratings
spec:
  hosts:
  - ratings
  http:
  - fault:
      abort:
        percentage:
          value: 50
        httpStatus: 503
    route:
    - destination:
        host: ratings
```

```bash
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: ratings
spec:
  hosts:
  - ratings
  http:
  - fault:
      abort:
        percentage:
          value: 50
        httpStatus: 503
    route:
    - destination:
        host: ratings
EOF

# 테스트 (retry가 없으면 50%는 실패)
# retry가 있으면 대부분 성공 (3회 중 1회만 성공하면 됨)
for i in {1..10}; do
  kubectl run test-retry-$i --rm -it --restart=Never --image=curlimages/curl -- \
    curl -s -o /dev/null -w "%{http_code}\n" productpage:9080/productpage
done
```

---

## 🛠️ Part 4: Fault Injection (1시간)

### 4.1 Fault Injection 개념

```
┌─────────────────────────────────────────────────────────────────────┐
│  Fault Injection = Chaos Engineering의 기초                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  목적:                                                               │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • 시스템의 장애 복원력 테스트                              │    │
│  │  • 프로덕션 환경 장애 시나리오 시뮬레이션                   │    │
│  │  • 취약점 사전 발견                                         │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Istio Fault Injection 유형:                                        │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  1. Delay: 응답 지연 주입                                   │    │
│  │     → 네트워크 지연, 서버 과부하 시뮬레이션                │    │
│  │                                                             │    │
│  │  2. Abort: HTTP 에러 주입                                   │    │
│  │     → 서버 에러, 서비스 장애 시뮬레이션                    │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  장점 (vs 실제 장애):                                               │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • 안전하게 테스트 (실제 서비스 영향 없음)                 │    │
│  │  • 특정 비율만 영향 (percentage 설정)                       │    │
│  │  • 특정 조건만 영향 (헤더 매칭)                             │    │
│  │  • 즉시 제거 가능 (VirtualService 삭제)                    │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 실습 5: Delay Injection (지연 주입)

```yaml
# delay-injection.yaml
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
          value: 50        # 50%의 요청에
        fixedDelay: 7s     # 7초 지연 주입
    route:
    - destination:
        host: ratings
```

```bash
kubectl apply -f - <<EOF
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
          value: 50
        fixedDelay: 7s
    route:
    - destination:
        host: ratings
EOF

# 테스트 (50%는 7초, 50%는 정상)
for i in {1..5}; do
  echo "Request $i:"
  time kubectl run delay-test-$i --rm -it --restart=Never --image=curlimages/curl -- \
    curl -s -o /dev/null productpage:9080/productpage
done
```

### 실습 6: Abort Injection (에러 주입)

```yaml
# abort-injection.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: ratings
spec:
  hosts:
  - ratings
  http:
  - fault:
      abort:
        percentage:
          value: 10        # 10%의 요청에
        httpStatus: 500    # 500 에러 반환
    route:
    - destination:
        host: ratings
```

```bash
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: ratings
spec:
  hosts:
  - ratings
  http:
  - fault:
      abort:
        percentage:
          value: 10
        httpStatus: 500
    route:
    - destination:
        host: ratings
EOF

# 테스트 (10%는 500 에러)
for i in {1..20}; do
  kubectl run abort-test-$i --rm -it --restart=Never --image=curlimages/curl -- \
    curl -s -o /dev/null -w "%{http_code}\n" ratings:9080/ratings/0
done 2>/dev/null | sort | uniq -c
```

### 실습 7: 조건부 Fault Injection

```yaml
# conditional-fault.yaml
# 특정 사용자에게만 지연 주입
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: ratings
spec:
  hosts:
  - ratings
  http:
  # 테스트 사용자에게만 지연 주입
  - match:
    - headers:
        end-user:
          exact: "tester"
    fault:
      delay:
        percentage:
          value: 100
        fixedDelay: 7s
    route:
    - destination:
        host: ratings
  # 일반 사용자는 정상
  - route:
    - destination:
        host: ratings
```

```bash
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: ratings
spec:
  hosts:
  - ratings
  http:
  - match:
    - headers:
        end-user:
          exact: "tester"
    fault:
      delay:
        percentage:
          value: 100
        fixedDelay: 7s
    route:
    - destination:
        host: ratings
  - route:
    - destination:
        host: ratings
EOF

# 일반 사용자 (정상)
kubectl run normal-test --rm -it --restart=Never --image=curlimages/curl -- \
  sh -c "time curl -s -o /dev/null ratings:9080/ratings/0"

# 테스트 사용자 (7초 지연)
kubectl run tester-test --rm -it --restart=Never --image=curlimages/curl -- \
  sh -c "time curl -s -o /dev/null -H 'end-user: tester' ratings:9080/ratings/0"
```

---

## 🛠️ Part 5: 종합 실습 (30분)

### 실습 8: Timeout + Retry + Fault 조합

```yaml
# combined-resiliency.yaml
---
# reviews 서비스: timeout + retry
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
    timeout: 10s               # 전체 타임아웃 10초
    retries:
      attempts: 3              # 최대 3회 재시도
      perTryTimeout: 3s        # 각 시도당 3초
      retryOn: 5xx,reset,connect-failure
---
# ratings 서비스: 장애 주입 (테스트용)
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
          value: 20            # 20% 지연
        fixedDelay: 5s
      abort:
        percentage:
          value: 10            # 10% 에러
        httpStatus: 503
    route:
    - destination:
        host: ratings
```

```bash
# 적용
kubectl apply -f combined-resiliency.yaml

# 테스트 (여러 번 실행)
for i in {1..20}; do
  echo -n "Request $i: "
  time kubectl run combined-test-$i --rm -it --restart=Never --image=curlimages/curl -- \
    curl -s -o /dev/null -w "%{http_code}" productpage:9080/productpage
  echo ""
done 2>&1 | grep -E "Request|real"
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | 장애 복원력 패턴 이해 | Timeout, Retry, CB | ☐ |
| 2 | Timeout 설정 | 응답 대기 시간 제한 | ☐ |
| 3 | Retry 설정 | 자동 재시도 | ☐ |
| 4 | retryOn 옵션 이해 | 5xx, reset 등 | ☐ |
| 5 | Fault Injection - Delay | 지연 주입 | ☐ |
| 6 | Fault Injection - Abort | 에러 주입 | ☐ |
| 7 | 조건부 Fault Injection | 헤더 매칭 | ☐ |
| 8 | 종합 테스트 | 조합 테스트 | ☐ |

---

## 🔑 오늘 배운 핵심 YAML

```yaml
# Timeout
timeout: 3s

# Retry
retries:
  attempts: 3
  perTryTimeout: 2s
  retryOn: 5xx,reset,connect-failure

# Fault Injection - Delay
fault:
  delay:
    percentage: {value: 50}
    fixedDelay: 7s

# Fault Injection - Abort
fault:
  abort:
    percentage: {value: 10}
    httpStatus: 500
```

---

## 💡 면접 대비 핵심 포인트

### Q1: Istio에서 Chaos Engineering을 어떻게 수행하나요?

**A**: "Istio의 Fault Injection을 사용합니다. VirtualService에서 delay(지연)나 abort(에러)를 특정 비율의 요청에 주입하여 시스템의 장애 복원력을 테스트합니다. 프로덕션 환경에서도 특정 사용자나 헤더를 매칭하여 안전하게 테스트할 수 있습니다."

### Q2: Timeout과 Retry의 관계는?

**A**: "Timeout은 전체 요청의 최대 대기 시간이고, Retry의 perTryTimeout은 각 시도당 타임아웃입니다. 예를 들어 timeout: 10s, attempts: 3, perTryTimeout: 3s면 각 시도는 3초 내 완료해야 하고, 전체 재시도 포함 10초 내 응답해야 합니다."

### Q3: Circuit Breaker와 Retry의 차이는?

**A**: "Retry는 일시적 오류에 대해 재시도하고, Circuit Breaker는 연속 실패 시 더 이상 요청을 보내지 않고 빠르게 실패합니다. Retry는 VirtualService에서, Circuit Breaker는 DestinationRule의 outlierDetection에서 설정합니다."

---

## 📝 학습 기록

```
학습일: ____년 __월 __일
실제 소요 시간: ____시간

실습 완료 항목:
- [ ] Timeout 설정 및 테스트
- [ ] Retry 설정 및 테스트
- [ ] Delay Injection
- [ ] Abort Injection
- [ ] 조건부 Fault Injection
- [ ] 종합 테스트

이해가 어려웠던 부분:


추가 학습 필요 항목:

```

---

## ➡️ 다음 학습: Day 66

**주제**: Traffic Mirroring (섀도잉)
- 실제 트래픽 복사
- 새 버전 안전 테스트
- 미러링 vs 카나리
