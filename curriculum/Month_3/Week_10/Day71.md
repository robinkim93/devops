# 📅 Day 71: Istio Access Logging - 서비스 메시 로그 분석 마스터

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "서비스 안정성을 위한 모니터링/로깅 시스템 구축"

Envoy Access Log를 설정하고 분석하여 서비스 메시 트래픽을 완벽하게 파악합니다.

---

## ⏰ 예상 학습 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 | 45분 | Access Log 구조 이해 |
| 실습 | 1.5시간 | 로그 설정 및 분석 |
| 심화 | 45분 | 트러블슈팅 활용 |

---

## 📚 Part 1: Access Log 개념 (45분)

### Envoy Access Log란?

```
┌─────────────────────────────────────────────────────────────┐
│  Envoy Access Log                                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  모든 HTTP/TCP 트래픽 기록                                  │
│  ┌─────────────────────────────────────────────────┐       │
│  │ Client ──▶ Envoy Sidecar ──▶ Upstream Service  │       │
│  │              │                                  │       │
│  │              ▼                                  │       │
│  │         Access Log                              │       │
│  │         (stdout/file)                           │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  기록 내용:                                                 │
│  - 요청 시작 시간                                          │
│  - HTTP 메서드, 경로                                       │
│  - 응답 코드                                               │
│  - 응답 플래그 (Envoy 내부 상태)                           │
│  - 처리 시간                                               │
│  - 업스트림 호스트 정보                                    │
│                                                             │
│  토스플레이스 활용:                                         │
│  - 장애 분석의 1차 데이터                                  │
│  - 성능 문제 진단                                          │
│  - 보안 감사                                               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Access Log 필드 상세

| 필드 | 설명 | 예시 |
|------|------|------|
| `%START_TIME%` | 요청 시작 시간 | 2024-01-15T10:30:00.000Z |
| `%REQ(:METHOD)%` | HTTP 메서드 | GET, POST |
| `%REQ(X-ENVOY-ORIGINAL-PATH)%` | 원본 요청 경로 | /api/users |
| `%PROTOCOL%` | 프로토콜 | HTTP/1.1, HTTP/2 |
| `%RESPONSE_CODE%` | HTTP 응답 코드 | 200, 503 |
| `%RESPONSE_FLAGS%` | Envoy 응답 플래그 | -, UH, UT |
| `%BYTES_RECEIVED%` | 수신 바이트 | 1234 |
| `%BYTES_SENT%` | 송신 바이트 | 5678 |
| `%DURATION%` | 총 처리 시간 (ms) | 25 |
| `%RESP(X-ENVOY-UPSTREAM-SERVICE-TIME)%` | 업스트림 시간 | 20 |
| `%UPSTREAM_HOST%` | 업스트림 호스트 | 10.244.0.5:8080 |
| `%UPSTREAM_CLUSTER%` | 업스트림 클러스터 | outbound|8080||svc.ns |

### Response Flags 완전 가이드

```
┌─────────────────────────────────────────────────────────────┐
│  Envoy Response Flags                                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  연결/업스트림 관련                                         │
│  ┌─────────────────────────────────────────────────┐       │
│  │ UH  = Upstream Healthy가 아님 (No healthy)     │       │
│  │ UF  = Upstream 연결 실패                        │       │
│  │ UO  = Upstream Overflow (Circuit Breaker)      │       │
│  │ UT  = Upstream 타임아웃                         │       │
│  │ UC  = Upstream 연결 종료                        │       │
│  │ UR  = Upstream 원격 리셋                        │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  다운스트림 관련                                            │
│  ┌─────────────────────────────────────────────────┐       │
│  │ DC  = Downstream 연결 종료                      │       │
│  │ DI  = Delay Injection 적용                      │       │
│  │ FI  = Fault Injection 적용                      │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  라우팅 관련                                                │
│  ┌─────────────────────────────────────────────────┐       │
│  │ NR  = No Route (라우트 없음)                    │       │
│  │ NC  = No Cluster (클러스터 없음)                │       │
│  │ UMSDR = Upstream Max Stream Duration 도달       │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  인증/보안 관련                                             │
│  ┌─────────────────────────────────────────────────┐       │
│  │ UAEX = Upstream Auth 실패                       │       │
│  │ RLSE = Rate Limit 서비스 에러                   │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  정상 상태: - (플래그 없음)                                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: 실습 (1.5시간)

### 실습 1: Access Log 활성화

```bash
# 방법 1: Istio 설치 시 활성화
istioctl install --set meshConfig.accessLogFile=/dev/stdout

# 방법 2: ConfigMap 수정 (기존 클러스터)
kubectl edit configmap istio -n istio-system

# 추가할 내용:
# data:
#   mesh: |
#     accessLogFile: /dev/stdout
#     accessLogFormat: ""  # 기본 형식 사용

# 방법 3: IstioOperator로 설정
kubectl apply -f - <<EOF
apiVersion: install.istio.io/v1alpha1
kind: IstioOperator
metadata:
  name: istio
  namespace: istio-system
spec:
  meshConfig:
    accessLogFile: /dev/stdout
    accessLogEncoding: JSON
EOF

# 설정 적용 확인 (Pod 재시작 필요)
kubectl rollout restart deployment -n default
```

### 실습 2: 기본 로그 확인

```bash
# 트래픽 생성
kubectl run client --image=busybox --rm -it --restart=Never -- \
  wget -qO- http://productpage:9080/productpage

# Sidecar 로그 확인
kubectl logs -l app=productpage -c istio-proxy --tail=10

# 예상 출력 (TEXT 형식):
# [2024-01-15T10:30:00.000Z] "GET /productpage HTTP/1.1" 200 - 
#   via_upstream - "-" 0 4183 26 25 "10.244.0.1" 
#   "Wget" "abc123-def456" "productpage:9080" 
#   "10.244.0.5:9080" inbound|9080|| 10.244.0.5:35678 
#   10.244.0.5:9080 10.244.0.1:0 default

# Follow 모드로 실시간 확인
kubectl logs -l app=productpage -c istio-proxy -f
```

### 실습 3: JSON 형식 로그 설정

```yaml
# JSON 형식으로 설정 (Telemetry API)
kubectl apply -f - <<EOF
apiVersion: telemetry.istio.io/v1alpha1
kind: Telemetry
metadata:
  name: mesh-logging
  namespace: istio-system
spec:
  accessLogging:
  - providers:
    - name: envoy
EOF
```

```bash
# JSON 로그 확인
kubectl logs -l app=productpage -c istio-proxy --tail=1 | jq .

# 출력 예시:
# {
#   "start_time": "2024-01-15T10:30:00.000Z",
#   "method": "GET",
#   "path": "/productpage",
#   "protocol": "HTTP/1.1",
#   "response_code": "200",
#   "response_flags": "-",
#   "bytes_received": "0",
#   "bytes_sent": "4183",
#   "duration": "26",
#   "upstream_host": "10.244.0.5:9080"
# }
```

### 실습 4: 조건부 로깅 (에러만)

```yaml
# 4xx, 5xx 응답만 로깅
kubectl apply -f - <<EOF
apiVersion: telemetry.istio.io/v1alpha1
kind: Telemetry
metadata:
  name: error-only-logging
  namespace: default
spec:
  accessLogging:
  - providers:
    - name: envoy
    filter:
      expression: response.code >= 400
EOF
```

### 실습 5: 커스텀 로그 형식

```yaml
# 필요한 필드만 포함하는 커스텀 형식
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1alpha3
kind: EnvoyFilter
metadata:
  name: custom-access-log
  namespace: istio-system
spec:
  configPatches:
  - applyTo: NETWORK_FILTER
    match:
      context: ANY
      listener:
        filterChain:
          filter:
            name: "envoy.filters.network.http_connection_manager"
    patch:
      operation: MERGE
      value:
        typed_config:
          "@type": type.googleapis.com/envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager
          access_log:
          - name: envoy.access_loggers.file
            typed_config:
              "@type": type.googleapis.com/envoy.extensions.access_loggers.file.v3.FileAccessLog
              path: /dev/stdout
              log_format:
                json_format:
                  timestamp: "%START_TIME%"
                  method: "%REQ(:METHOD)%"
                  path: "%REQ(:PATH)%"
                  status: "%RESPONSE_CODE%"
                  flags: "%RESPONSE_FLAGS%"
                  duration_ms: "%DURATION%"
                  upstream: "%UPSTREAM_HOST%"
                  user_agent: "%REQ(USER-AGENT)%"
                  request_id: "%REQ(X-REQUEST-ID)%"
EOF
```

---

## 🛠️ Part 3: 로그 분석 및 트러블슈팅 (45분)

### 분석 시나리오 1: 타임아웃 찾기

```bash
# UT (Upstream Timeout) 플래그 찾기
kubectl logs -l app=productpage -c istio-proxy | grep "UT"

# 특정 시간 범위에서 타임아웃 카운트
kubectl logs -l app=reviews -c istio-proxy --since=1h | \
  grep -c "UT"

# 타임아웃 발생 시간과 요청 정보 추출
kubectl logs -l app=reviews -c istio-proxy | \
  grep "UT" | \
  jq -r '[.start_time, .method, .path, .duration_ms] | @tsv'
```

### 분석 시나리오 2: 5xx 에러 분석

```bash
# 5xx 에러 찾기
kubectl logs -l app=productpage -c istio-proxy | grep -E '"response_code":"5[0-9]{2}"'

# 에러별 카운트
kubectl logs -l app=productpage -c istio-proxy --since=1h | \
  jq -r '.response_code' | \
  grep -E "^5" | \
  sort | uniq -c | sort -rn

# 에러와 Response Flag 함께 확인
kubectl logs -l app=reviews -c istio-proxy | \
  jq 'select(.response_code | tonumber >= 500) | {time: .start_time, code: .response_code, flags: .response_flags}'
```

### 분석 시나리오 3: 느린 요청 분석

```bash
# 1초 이상 걸린 요청 찾기
kubectl logs -l app=productpage -c istio-proxy | \
  jq 'select(.duration | tonumber > 1000) | {path: .path, duration: .duration, upstream: .upstream_host}'

# P95 응답 시간 계산
kubectl logs -l app=productpage -c istio-proxy --since=1h | \
  jq -r '.duration' | \
  sort -n | \
  awk 'BEGIN{c=0} {d[c++]=$1} END{print "P95:", d[int(c*0.95)]}'
```

### 분석 시나리오 4: 업스트림 문제 진단

```bash
# Upstream 연결 실패 (UF) 찾기
kubectl logs -l app=productpage -c istio-proxy | grep "UF"

# Unhealthy Upstream (UH) 찾기
kubectl logs -l app=reviews -c istio-proxy | grep "UH"

# Circuit Breaker (UO) 발동 확인
kubectl logs -l app=ratings -c istio-proxy | grep "UO"

# 종합 분석 스크립트
cat << 'EOF' > analyze-logs.sh
#!/bin/bash
APP=${1:-productpage}
echo "=== Analyzing logs for $APP ==="

echo -e "\n📊 Response Code Distribution:"
kubectl logs -l app=$APP -c istio-proxy --since=1h | \
  jq -r '.response_code' 2>/dev/null | sort | uniq -c | sort -rn

echo -e "\n🚩 Response Flags:"
kubectl logs -l app=$APP -c istio-proxy --since=1h | \
  jq -r '.response_flags' 2>/dev/null | sort | uniq -c | sort -rn

echo -e "\n⏱️ Slowest Requests (top 5):"
kubectl logs -l app=$APP -c istio-proxy --since=1h | \
  jq -r '[.path, .duration + "ms"] | @tsv' 2>/dev/null | sort -t$'\t' -k2 -rn | head -5
EOF
chmod +x analyze-logs.sh
./analyze-logs.sh productpage
```

### 로그 수집 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│  Production Log Collection                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐                    │
│  │ Pod A   │  │ Pod B   │  │ Pod C   │                    │
│  │(Sidecar)│  │(Sidecar)│  │(Sidecar)│                    │
│  └────┬────┘  └────┬────┘  └────┬────┘                    │
│       │            │            │                          │
│       └────────────┼────────────┘                          │
│                    │ stdout                                 │
│                    ▼                                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                  Fluentd/Fluent Bit                  │   │
│  │                  (DaemonSet)                         │   │
│  └───────────────────────┬─────────────────────────────┘   │
│                          │                                  │
│           ┌──────────────┼──────────────┐                  │
│           ▼              ▼              ▼                  │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │
│  │Elasticsearch│ │    Loki     │ │    S3       │          │
│  └─────────────┘ └─────────────┘ └─────────────┘          │
│                          │                                  │
│                          ▼                                  │
│                   ┌─────────────┐                          │
│                   │   Grafana   │                          │
│                   └─────────────┘                          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | Access Log 활성화 | ☐ |
| 2 | 기본 로그 형식 이해 | ☐ |
| 3 | Response Flags 의미 숙지 | ☐ |
| 4 | JSON 로그 설정 | ☐ |
| 5 | 조건부 로깅 설정 | ☐ |
| 6 | 로그 분석 실습 | ☐ |
| 7 | 트러블슈팅 활용 | ☐ |

---

## 🔑 핵심 명령어

```bash
# 로그 확인
kubectl logs -l app=<app> -c istio-proxy --tail=100
kubectl logs -l app=<app> -c istio-proxy -f

# 로그 분석
kubectl logs -l app=<app> -c istio-proxy | grep "UT"
kubectl logs -l app=<app> -c istio-proxy | jq '.response_code'
```

---

## 📝 면접 대비 질문

### Q1: Envoy Access Log에서 UH 플래그의 의미는?
> "UH는 'Upstream Healthy가 아님'을 의미합니다. 이는 Envoy가 업스트림 서비스의 모든 인스턴스가 unhealthy하다고 판단했음을 나타냅니다. 원인으로는 헬스체크 실패, Circuit Breaker 발동, 모든 Pod가 NotReady 상태인 경우가 있습니다."

### Q2: Access Log를 프로덕션에서 어떻게 활용하나요?
> "첫째, 실시간 장애 분석에 활용합니다. Response Flags로 장애 유형을 빠르게 파악합니다. 둘째, 성능 모니터링에 사용합니다. Duration 필드로 느린 요청을 식별합니다. 셋째, 보안 감사에 활용합니다. 비정상 요청 패턴을 탐지합니다. Fluentd로 수집하여 Elasticsearch나 Loki에 저장하고 Grafana로 시각화합니다."

---

## ➡️ 다음 학습: Day 72

**주제**: Envoy 트러블슈팅
- istioctl proxy-config 활용
- Envoy Admin API
- 설정 문제 진단
