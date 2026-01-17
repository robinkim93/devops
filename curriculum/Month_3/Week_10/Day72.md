# 📅 Day 72: Envoy 트러블슈팅

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "다양한 레이어에서의 모니터링, 트러블슈팅 경험"
> "Kubernetes와 Service Mesh에 대한 경험"

istioctl 명령어와 Envoy 관리 인터페이스를 활용하여 Service Mesh 문제를 체계적으로 진단하고 해결합니다. 실제 운영 환경에서 발생할 수 있는 다양한 트러블슈팅 시나리오를 실습합니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| istioctl 디버깅 | 1시간 | 핵심 명령어 학습 |
| Envoy Admin | 45분 | 관리 인터페이스 활용 |
| 트러블슈팅 실습 | 1.5시간 | 시나리오별 실습 |
| 정리 | 45분 | 체크리스트, 면접 준비 |

---

## 📚 Part 1: istioctl 디버깅 명령어 (1시간)

### 1.1 istioctl 개요

```
┌─────────────────────────────────────────────────────────────────────┐
│  istioctl 디버깅 명령어 체계                                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  설정 검증                                                          │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  istioctl analyze                                           │    │
│  │  → VirtualService, DestinationRule 설정 오류 검출           │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Proxy 상태                                                         │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  istioctl proxy-status                                      │    │
│  │  → 모든 Proxy의 동기화 상태 확인                            │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Proxy 설정 상세                                                    │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  istioctl proxy-config                                      │    │
│  │  ├── clusters   : 업스트림 클러스터 설정                    │    │
│  │  ├── listeners  : 리스너 설정                               │    │
│  │  ├── routes     : 라우팅 규칙                               │    │
│  │  ├── endpoints  : 실제 엔드포인트 (Pod IP)                  │    │
│  │  └── bootstrap  : 부트스트랩 설정                           │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  서비스 상세                                                        │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  istioctl describe pod/service                              │    │
│  │  → 적용된 VirtualService, DestinationRule 확인              │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 핵심 명령어 표

| 명령어 | 설명 | 주요 용도 |
|--------|------|----------|
| `istioctl analyze` | 설정 검증 | 배포 전 오류 검출 |
| `istioctl proxy-status` | Proxy 동기화 상태 | 설정 전파 확인 |
| `istioctl proxy-config` | Proxy 설정 확인 | 상세 디버깅 |
| `istioctl describe` | 서비스/Pod 상세 | 적용 규칙 확인 |
| `istioctl dashboard` | UI 대시보드 접근 | Kiali, Grafana 등 |
| `istioctl bug-report` | 버그 리포트 생성 | 지원 요청 시 |

---

## 🛠️ Part 2: 실습 - 설정 검증 (30분)

### 실습 1: istioctl analyze

```bash
# 현재 네임스페이스 설정 검증
istioctl analyze

# 출력 예시 (정상):
# ✔ No validation issues found when analyzing namespace: default.

# 특정 네임스페이스 검증
istioctl analyze -n production

# 모든 네임스페이스 검증
istioctl analyze --all-namespaces

# 파일 검증 (배포 전 확인)
istioctl analyze -f my-virtualservice.yaml

# 상세 출력
istioctl analyze --verbose

# 출력 예시 (오류):
# Error [IST0101] (VirtualService reviews.default) Referenced host not found: "reviews.default.svc.cluster.local"
# Error [IST0104] (Gateway my-gateway.default) Referenced host "*.example.com" not found in any bound VirtualService

# 경고 출력
# Warning [IST0103] (Pod productpage-xxx.default) The pod is missing the Istio proxy. This can happen if...
```

### 실습 2: 일반적인 analyze 오류 및 해결

```bash
# 오류 1: IST0101 - Referenced host not found
# 원인: VirtualService가 존재하지 않는 호스트 참조
# 해결: host 이름 수정 또는 서비스 생성

# 오류 2: IST0104 - Referenced gateway not found
# 원인: VirtualService가 존재하지 않는 Gateway 참조
# 해결: gateway 이름 확인 또는 Gateway 생성

# 오류 3: IST0102 - Namespace not injected
# 원인: Namespace에 istio-injection 라벨 없음
# 해결:
kubectl label namespace default istio-injection=enabled

# 오류 4: IST0106 - Schema validation error
# 원인: YAML 스키마 오류
# 해결: API 버전, 필드 이름 확인
```

---

## 🛠️ Part 3: Proxy 상태 확인 (30분)

### 실습 3: proxy-status

```bash
# 모든 Proxy 상태 확인
istioctl proxy-status

# 출력 예시:
# NAME                                  CDS        LDS        EDS        RDS        ECDS       ISTIOD                       VERSION
# productpage-xxx.default               SYNCED     SYNCED     SYNCED     SYNCED     NOT SENT   istiod-xxx.istio-system      1.19.0
# reviews-v1-xxx.default                SYNCED     SYNCED     SYNCED     SYNCED     NOT SENT   istiod-xxx.istio-system      1.19.0
# reviews-v2-xxx.default                SYNCED     SYNCED     SYNCED     SYNCED     NOT SENT   istiod-xxx.istio-system      1.19.0
# ratings-xxx.default                   SYNCED     SYNCED     SYNCED     SYNCED     NOT SENT   istiod-xxx.istio-system      1.19.0

# 상태 해석:
# SYNCED   = 설정이 정상적으로 동기화됨
# NOT SENT = 해당 설정이 전송되지 않음 (정상일 수 있음)
# STALE    = 오래된 설정 사용 중 (문제!)

# 특정 Pod 상태 확인
istioctl proxy-status productpage-xxx.default

# Istiod와의 버전 차이 확인
istioctl proxy-status | grep -v SYNCED
```

### 실습 4: 동기화 문제 진단

```bash
# 동기화 상태 열 설명:
# CDS (Cluster Discovery Service)  : 업스트림 클러스터 정보
# LDS (Listener Discovery Service) : 리스너 설정
# EDS (Endpoint Discovery Service) : 엔드포인트 (Pod IP)
# RDS (Route Discovery Service)    : 라우팅 규칙
# ECDS (Extension Config Discovery): 확장 설정

# STALE 상태 해결 방법:
# 1. Istiod 로그 확인
kubectl logs -n istio-system -l app=istiod | grep -i error

# 2. Proxy 재시작
kubectl rollout restart deployment productpage -n default

# 3. Istiod 재시작 (최후 수단)
kubectl rollout restart deployment istiod -n istio-system
```

---

## 🛠️ Part 4: Proxy 설정 상세 확인 (1시간)

### 실습 5: proxy-config clusters

```bash
# 업스트림 클러스터 확인 (어디로 트래픽을 보낼 수 있는지)
istioctl proxy-config clusters productpage-xxx.default

# 출력 예시:
# SERVICE FQDN                          PORT   SUBSET   DIRECTION   TYPE      DESTINATION RULE
# reviews.default.svc.cluster.local     9080   v1       outbound    EDS       reviews.default
# reviews.default.svc.cluster.local     9080   v2       outbound    EDS       reviews.default
# reviews.default.svc.cluster.local     9080   v3       outbound    EDS       reviews.default
# ratings.default.svc.cluster.local     9080   -        outbound    EDS       -

# 특정 서비스 필터
istioctl proxy-config clusters productpage-xxx.default \
  --fqdn reviews.default.svc.cluster.local

# JSON 출력 (상세 정보)
istioctl proxy-config clusters productpage-xxx.default -o json | jq

# 중요 확인 사항:
# - DIRECTION: inbound/outbound
# - TYPE: EDS(동적)/STATIC(고정)
# - DESTINATION RULE: 적용된 DR
# - SUBSET: 정의된 서브셋
```

### 실습 6: proxy-config routes

```bash
# 라우팅 설정 확인 (어떻게 트래픽이 라우팅되는지)
istioctl proxy-config routes productpage-xxx.default

# 출력 예시:
# NAME                                                 DOMAINS                        MATCH                  VIRTUAL SERVICE
# 9080                                                 reviews, reviews.default...     /*                     reviews.default
# 80                                                   istio-ingressgateway...         /*                     bookinfo-gateway

# 특정 라우트 상세
istioctl proxy-config routes productpage-xxx.default --name 9080 -o json | jq

# 확인 항목:
# - DOMAINS: 매칭할 호스트
# - MATCH: 경로 매칭 조건
# - VIRTUAL SERVICE: 적용된 VS

# 라우팅 문제 디버깅
# Q: VirtualService 적용이 안 되는 것 같다
# A: routes에 해당 VS가 표시되는지 확인
istioctl proxy-config routes productpage-xxx.default | grep "my-virtualservice"
```

### 실습 7: proxy-config endpoints

```bash
# 엔드포인트 확인 (실제 트래픽이 갈 Pod IP)
istioctl proxy-config endpoints productpage-xxx.default

# 출력 예시:
# ENDPOINT                  STATUS    OUTLIER CHECK    CLUSTER
# 10.244.0.15:9080          HEALTHY   OK              outbound|9080|v1|reviews.default.svc.cluster.local
# 10.244.0.16:9080          HEALTHY   OK              outbound|9080|v2|reviews.default.svc.cluster.local
# 10.244.0.17:9080          UNHEALTHY FAILED          outbound|9080|v3|reviews.default.svc.cluster.local

# 특정 클러스터의 엔드포인트
istioctl proxy-config endpoints productpage-xxx.default \
  --cluster "outbound|9080||reviews.default.svc.cluster.local"

# 상태 해석:
# HEALTHY   = 정상
# UNHEALTHY = 헬스체크 실패
# DRAINING  = 연결 종료 중

# OUTLIER CHECK:
# OK     = 정상
# FAILED = Circuit Breaker에 의해 제외됨

# 엔드포인트 문제 디버깅
# Q: 트래픽이 특정 Pod로 안 간다
# A: endpoints에서 해당 Pod IP가 HEALTHY인지 확인
```

### 실습 8: proxy-config listeners

```bash
# 리스너 확인 (어떤 포트에서 트래픽을 받는지)
istioctl proxy-config listeners productpage-xxx.default

# 출력 예시:
# ADDRESS       PORT   MATCH                                                       DESTINATION
# 0.0.0.0       15001  ALL                                                         Passthrough Cluster
# 0.0.0.0       15006  Addr: *:15006                                               Inline Route: /* 
# 10.96.0.1     443    ALL                                                         Cluster: outbound|443||kubernetes.default.svc.cluster.local
# 0.0.0.0       9080   Trans: raw_buffer; App: HTTP                                Route: 9080

# Inbound 리스너만
istioctl proxy-config listeners productpage-xxx.default --type HTTP --port 9080

# 리스너 상세 (JSON)
istioctl proxy-config listeners productpage-xxx.default -o json | jq '.[] | select(.name=="0.0.0.0_9080")'
```

---

## 🛠️ Part 5: describe와 종합 디버깅 (30분)

### 실습 9: describe pod

```bash
# Pod의 Istio 설정 상세 확인
istioctl describe pod productpage-xxx -n default

# 출력 예시:
# Pod: productpage-xxx
#    Pod Revision: default
#    Pod Ports: 9080 (productpage), 15090 (istio-proxy)
# 
# ---------------------
# Service: productpage
#    Port: http 9080/HTTP targets pod port 9080
# 
# ---------------------
# Effective PeerAuthentication:
#    Workload mTLS mode: STRICT
# 
# ---------------------
# Applied PodSecurityPolicy: ...
# 
# ---------------------
# Exposed on Ingress Gateway: http://bookinfo.example.com
# VirtualService: bookinfo
#    Match: /productpage /static /login /logout /api/v1/products
# 
# ---------------------
# RBAC: (none)

# 확인 항목:
# - Sidecar 버전
# - 적용된 VirtualService
# - 적용된 DestinationRule
# - mTLS 모드
# - Ingress 노출 여부
```

### 실습 10: describe service

```bash
# 서비스의 Istio 설정 확인
istioctl describe service reviews -n default

# 출력 예시:
# Service: reviews
#    Port: http 9080/HTTP
# 
# ---------------------
# DestinationRule: reviews for "reviews.default.svc.cluster.local"
#    Matching subsets:
#       v1 (labels: {"version":"v1"})
#       v2 (labels: {"version":"v2"})
#       v3 (labels: {"version":"v3"})
#    Traffic Policy:
#       connectionPool:
#          tcp: maxConnections=100
#          http: h2UpgradePolicy=UPGRADE
#       loadBalancer: ROUND_ROBIN
# 
# ---------------------
# VirtualService: reviews
#    Route to host "reviews" subset "v1" with weight 80%
#    Route to host "reviews" subset "v2" with weight 20%
```

---

## 🛠️ Part 6: Envoy Admin 인터페이스 (30분)

### 실습 11: Envoy Admin API

```bash
# Envoy Admin 포트 포워딩
kubectl port-forward productpage-xxx 15000:15000 &

# 설정 덤프
curl localhost:15000/config_dump | jq

# 클러스터 정보
curl localhost:15000/clusters | head -50

# 서버 정보
curl localhost:15000/server_info | jq

# 통계
curl localhost:15000/stats | grep -E "upstream_cx|downstream_cx"

# 특정 통계
curl localhost:15000/stats?filter=cluster.outbound

# 리스너 정보
curl localhost:15000/listeners

# 라우팅 테이블
curl localhost:15000/routes

# 핫 리스타트 정보
curl localhost:15000/hot_restart_version

# 로깅 레벨 변경
curl -X POST localhost:15000/logging?level=debug
curl -X POST localhost:15000/logging?level=info  # 되돌리기

# 상태 확인
curl localhost:15000/ready
curl localhost:15000/server_info | jq '.state'
```

### 실습 12: 유용한 Admin 엔드포인트

```bash
# 연결 통계 확인 (Connection Pooling 디버깅)
curl localhost:15000/stats | grep "cx_active\|cx_total"

# 업스트림 에러 확인
curl localhost:15000/stats | grep "upstream_rq_5xx\|upstream_rq_timeout"

# Circuit Breaker 통계
curl localhost:15000/stats | grep "outlier"

# Retry 통계
curl localhost:15000/stats | grep "retry"

# 메모리 사용량
curl localhost:15000/memory | jq

# 인증서 정보
curl localhost:15000/certs | jq
```

---

## 🛠️ Part 7: 트러블슈팅 시나리오 (30분)

### 시나리오 1: 서비스 호출 실패 (503 에러)

```bash
# 증상: productpage → reviews 호출 시 503 Service Unavailable

# Step 1: 설정 검증
istioctl analyze
# 결과: No validation issues found

# Step 2: Proxy 동기화 확인
istioctl proxy-status
# 결과: 모두 SYNCED

# Step 3: 라우팅 확인
istioctl proxy-config routes productpage-xxx | grep reviews
# 결과: reviews 라우트 존재

# Step 4: 엔드포인트 확인
istioctl proxy-config endpoints productpage-xxx | grep reviews
# 결과: 10.244.0.15:9080 UNHEALTHY FAILED
# → reviews Pod가 비정상!

# Step 5: Pod 상태 확인
kubectl get pods -l app=reviews
kubectl describe pod reviews-xxx
kubectl logs reviews-xxx -c reviews

# 해결: Pod 재시작 또는 문제 해결
kubectl rollout restart deployment reviews
```

### 시나리오 2: VirtualService 미적용

```bash
# 증상: Canary 설정했는데 모든 트래픽이 v1으로만 감

# Step 1: VirtualService 확인
kubectl get vs reviews -o yaml
# 결과: weight 80/20 설정됨

# Step 2: analyze 확인
istioctl analyze
# 결과: Warning - DestinationRule subsets referenced but not defined

# Step 3: DestinationRule 확인
kubectl get dr reviews -o yaml
# 결과: DestinationRule 없음!

# 해결: DestinationRule 생성
cat << 'EOF' | kubectl apply -f -
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
EOF

# Step 4: 적용 확인
istioctl describe service reviews
istioctl proxy-config clusters productpage-xxx | grep reviews
```

### 시나리오 3: mTLS 인증 실패

```bash
# 증상: 서비스 간 통신 실패, TLS handshake error

# Step 1: mTLS 상태 확인
istioctl describe pod reviews-xxx | grep -A 5 "PeerAuthentication"
# 결과: Workload mTLS mode: STRICT

# Step 2: 소스 Pod mTLS 확인
istioctl describe pod productpage-xxx | grep -A 5 "PeerAuthentication"
# 결과: Workload mTLS mode: PERMISSIVE
# → mTLS 모드 불일치!

# Step 3: 클라이언트의 TLS 설정 확인
istioctl proxy-config clusters productpage-xxx \
  --fqdn reviews.default.svc.cluster.local -o json | \
  jq '.[].transportSocket'

# 해결: mTLS 모드 통일
kubectl apply -f - <<EOF
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: default
spec:
  mtls:
    mode: STRICT
EOF
```

### 시나리오 4: 타임아웃 문제

```bash
# 증상: 간헐적으로 요청 타임아웃 발생

# Step 1: VirtualService 타임아웃 확인
kubectl get vs reviews -o yaml | grep -A 3 timeout
# 결과: timeout: 1s (너무 짧음)

# Step 2: 실제 응답 시간 확인 (Envoy 통계)
kubectl port-forward productpage-xxx 15000:15000 &
curl localhost:15000/stats | grep "upstream_rq_time"

# Step 3: 백엔드 응답 시간 확인
kubectl exec -it productpage-xxx -c istio-proxy -- \
  curl -w "%{time_total}" -o /dev/null -s http://reviews:9080/
# 결과: 1.5초

# 해결: 타임아웃 늘리기
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: reviews
spec:
  hosts:
  - reviews
  http:
  - timeout: 5s
    route:
    - destination:
        host: reviews
EOF
```

---

## 📊 Part 8: 트러블슈팅 체크리스트

### 8.1 표준 디버깅 플로우

```
┌─────────────────────────────────────────────────────────────────────┐
│  Istio 트러블슈팅 표준 플로우                                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  [Step 1] 설정 검증                                                 │
│  $ istioctl analyze                                                 │
│  → 설정 오류 확인                                                   │
│                                                                      │
│            │                                                         │
│            ▼                                                         │
│                                                                      │
│  [Step 2] Proxy 동기화 확인                                         │
│  $ istioctl proxy-status                                            │
│  → SYNCED/STALE 확인                                                │
│                                                                      │
│            │                                                         │
│            ▼                                                         │
│                                                                      │
│  [Step 3] 라우팅 확인                                               │
│  $ istioctl proxy-config routes <pod>                               │
│  → VirtualService 적용 확인                                         │
│                                                                      │
│            │                                                         │
│            ▼                                                         │
│                                                                      │
│  [Step 4] 엔드포인트 확인                                           │
│  $ istioctl proxy-config endpoints <pod>                            │
│  → 백엔드 Pod 상태 확인 (HEALTHY/UNHEALTHY)                         │
│                                                                      │
│            │                                                         │
│            ▼                                                         │
│                                                                      │
│  [Step 5] 로그 확인                                                 │
│  $ kubectl logs <pod> -c istio-proxy | tail -100                    │
│  → Envoy 에러 로그 확인                                             │
│                                                                      │
│            │                                                         │
│            ▼                                                         │
│                                                                      │
│  [Step 6] 상세 디버깅                                               │
│  $ istioctl describe pod <pod>                                      │
│  $ curl localhost:15000/stats (Envoy Admin)                         │
│  → mTLS, 통계 확인                                                  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 8.2 빠른 참조 명령어

```bash
# 기본 진단
istioctl analyze                              # 설정 검증
istioctl proxy-status                         # 동기화 상태
istioctl describe pod <pod>                   # Pod 상세

# Proxy 설정
istioctl proxy-config clusters <pod>          # 클러스터
istioctl proxy-config routes <pod>            # 라우트
istioctl proxy-config endpoints <pod>         # 엔드포인트
istioctl proxy-config listeners <pod>         # 리스너

# 로그
kubectl logs <pod> -c istio-proxy             # Envoy 로그
kubectl logs -n istio-system -l app=istiod    # Istiod 로그

# Envoy Admin
kubectl port-forward <pod> 15000:15000        # Admin 접근
curl localhost:15000/config_dump              # 전체 설정
curl localhost:15000/stats                    # 통계
curl localhost:15000/clusters                 # 클러스터 상태
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | istioctl analyze 사용 | 설정 검증 | ☐ |
| 2 | proxy-status 확인 | 동기화 상태 | ☐ |
| 3 | proxy-config clusters | 업스트림 클러스터 | ☐ |
| 4 | proxy-config routes | 라우팅 규칙 | ☐ |
| 5 | proxy-config endpoints | Pod 상태 | ☐ |
| 6 | describe pod/service | 적용 규칙 확인 | ☐ |
| 7 | Envoy Admin API | 통계, 설정 덤프 | ☐ |
| 8 | 트러블슈팅 시나리오 | 4가지 시나리오 실습 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# 설정 검증
istioctl analyze
istioctl analyze -f <file.yaml>

# Proxy 상태
istioctl proxy-status
istioctl proxy-status <pod>

# Proxy 설정
istioctl proxy-config clusters <pod>
istioctl proxy-config routes <pod>
istioctl proxy-config endpoints <pod>
istioctl proxy-config listeners <pod>

# 상세 정보
istioctl describe pod <pod>
istioctl describe service <svc>

# Envoy Admin
kubectl port-forward <pod> 15000:15000
curl localhost:15000/config_dump
curl localhost:15000/stats
curl localhost:15000/clusters
```

---

## 💡 면접 대비 핵심 포인트

### Q1: Istio에서 서비스 호출이 실패할 때 어떻게 디버깅하나요?

**A**: "체계적인 단계로 진행합니다:

1. **istioctl analyze**로 설정 오류 검출
2. **proxy-status**로 Proxy 동기화 확인 (SYNCED vs STALE)
3. **proxy-config routes**로 VirtualService 적용 확인
4. **proxy-config endpoints**로 백엔드 Pod 상태 확인 (HEALTHY/UNHEALTHY)
5. **Envoy 로그**로 상세 에러 확인

대부분의 문제는 엔드포인트 상태(Pod 비정상)나 설정 미적용(DestinationRule 누락)입니다."

### Q2: Proxy 설정이 동기화되지 않을 때 어떻게 하나요?

**A**: "STALE 상태가 보이면:

1. Istiod 로그 확인 (`kubectl logs -n istio-system -l app=istiod`)
2. 해당 Pod의 Proxy 재시작 (`kubectl rollout restart`)
3. 네트워크 정책 확인 (Istiod ↔ Proxy 통신)
4. 최후 수단으로 Istiod 재시작

일반적으로 Pod 재시작으로 해결됩니다."

### Q3: Envoy Admin API는 어떤 상황에서 사용하나요?

**A**: "상세 디버깅이 필요할 때 사용합니다:

- `/stats`: 연결 수, 에러율, retry 횟수 등 통계
- `/clusters`: Circuit Breaker 상태, 헬스체크 결과
- `/config_dump`: 전체 Envoy 설정 확인
- `/logging?level=debug`: 로그 레벨 변경

특히 간헐적 에러나 성능 문제 분석에 유용합니다."

---

## 📝 학습 기록

```
학습일: ____년 __월 __일
실제 소요 시간: ____시간

실습 완료 항목:
- [ ] istioctl analyze
- [ ] proxy-status
- [ ] proxy-config (clusters/routes/endpoints)
- [ ] describe pod/service
- [ ] Envoy Admin API
- [ ] 트러블슈팅 시나리오

이해가 어려웠던 부분:


추가 학습 필요 항목:

```

---

## ➡️ 다음 학습: Day 73

**주제**: Observability 종합 실습
- Kiali, Grafana, Jaeger 통합
- 종합 장애 진단 실습
- Month 3 Week 10 정리
