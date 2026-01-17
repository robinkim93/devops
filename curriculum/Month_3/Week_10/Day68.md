# 📅 Day 68: Kiali - 서비스 메시 시각화

## 🎯 오늘의 목표

> **토스플레이스 핵심**: Kiali로 서비스 간 통신을 시각화하고 Istio 설정을 검증합니다.

---

## ⏰ 예상 학습 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 | 30분 | Kiali 아키텍처 |
| 설치 | 30분 | 배포 및 접속 |
| 핵심 기능 | 1시간 | Graph, 설정 검증 |
| 실전 활용 | 1시간 | 트러블슈팅 |

---

## 📚 Part 1: Kiali 개요

### Kiali란?

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     Kiali - Service Mesh Observability                       │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                     │
│   │ Prometheus  │    │   Jaeger    │    │ Kubernetes  │                     │
│   │  (메트릭)   │    │  (트레이스) │    │   (설정)    │                     │
│   └──────┬──────┘    └──────┬──────┘    └──────┬──────┘                     │
│          │                  │                  │                            │
│          └──────────────────┴──────────────────┘                            │
│                             │                                               │
│                      ┌──────▼──────┐                                        │
│                      │    Kiali    │                                        │
│                      │  Dashboard  │                                        │
│                      └──────┬──────┘                                        │
│                             │                                               │
│   ┌─────────────────────────┼─────────────────────────┐                     │
│   │                         │                         │                     │
│   ▼                         ▼                         ▼                     │
│  Graph                  Workloads               Istio Config                │
│  (토폴로지)              (상태)                  (검증)                      │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Kiali 핵심 기능

| 기능 | 설명 | 용도 |
|------|------|------|
| **Graph** | 서비스 토폴로지 시각화 | 트래픽 흐름 파악 |
| **Applications** | 앱 단위 뷰 | 앱별 상태 확인 |
| **Workloads** | Pod/Deployment 뷰 | 워크로드 상태 |
| **Services** | 서비스 뷰 | VirtualService 정보 |
| **Istio Config** | 설정 검증 | 오류/경고 감지 |

---

## 🛠️ Part 2: Kiali 설치

### 방법 1: Istio Addons

```bash
# Istio 설치 디렉토리에서
kubectl apply -f samples/addons/kiali.yaml

# 설치 확인
kubectl get pods -n istio-system -l app=kiali
kubectl get svc -n istio-system -l app=kiali
```

### 방법 2: Helm

```bash
# Helm Repository 추가
helm repo add kiali https://kiali.org/helm-charts
helm repo update

# 설치
helm install kiali-server kiali/kiali-server \
  -n istio-system \
  --set auth.strategy="anonymous" \
  --set deployment.accessible_namespaces="{**}" \
  --set external_services.prometheus.url="http://prometheus:9090" \
  --set external_services.tracing.url="http://tracing:16686"

# 설치 확인
kubectl get pods -n istio-system -l app.kubernetes.io/name=kiali
```

### 접속

```bash
# 포트포워딩
kubectl port-forward svc/kiali -n istio-system 20001:20001 &

# 브라우저 접속
# http://localhost:20001
```

---

## 🛠️ Part 3: Graph 기능

### Graph 화면 이해

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         Kiali Graph 해석                                     │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   노드 (서비스):                                                             │
│   ┌────────────┐                                                            │
│   │  ● nginx   │  ● = 건강 상태 색상                                         │
│   │  100 rps   │  rps = 초당 요청 수                                         │
│   │  🔒        │  🔒 = mTLS 활성화                                           │
│   └────────────┘                                                            │
│                                                                              │
│   건강 상태 색상:                                                            │
│   🟢 초록 = 정상 (에러율 < 0.1%)                                             │
│   🟡 노랑 = 경고 (0.1% < 에러율 < 1%)                                        │
│   🔴 빨강 = 에러 (에러율 > 1%)                                               │
│   ⚫ 회색 = 트래픽 없음                                                       │
│                                                                              │
│   엣지 (연결선):                                                             │
│   ─────▶  트래픽 방향                                                        │
│   굵은 선 = 높은 트래픽                                                       │
│   얇은 선 = 낮은 트래픽                                                       │
│   빨간 선 = 에러 발생                                                         │
│                                                                              │
│   특수 아이콘:                                                               │
│   ⚡ = Circuit Breaker 발동                                                  │
│   ⏱️ = Timeout 설정됨                                                        │
│   🔄 = Retry 설정됨                                                          │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Graph 옵션

| 옵션 | 설명 |
|------|------|
| **Namespace** | 표시할 네임스페이스 선택 |
| **Graph Type** | App, Service, Versioned App, Workload |
| **Display** | Traffic Animation, Response Time, mTLS |
| **Layout** | Dagre (계층적), Cola (힘 기반) |
| **Time Range** | 1m, 5m, 10m, 30m, 1h, 3h |

### 트래픽 애니메이션

```bash
# 트래픽 생성
while true; do
  curl -s http://$INGRESS_HOST:$INGRESS_PORT/productpage > /dev/null
  sleep 0.5
done &

# Kiali Graph에서 확인:
# Display → Traffic Animation 활성화
# 점들이 서비스 간 이동하는 것을 확인
```

---

## 🛠️ Part 4: Istio Config 검증

### 설정 검증 기능

```bash
# Kiali에서 Istio Config 탭 선택
# 각 리소스의 상태 표시:
# ✅ Valid - 정상
# ⚠️ Warning - 경고
# ❌ Error - 오류
```

### 자주 발생하는 오류

| 오류 | 원인 | 해결 |
|------|------|------|
| **No matching workload** | selector가 Pod와 불일치 | labels 확인 |
| **Subset not found** | DestinationRule에 subset 없음 | subset 추가 |
| **Host not found** | 존재하지 않는 서비스 참조 | 서비스 이름 확인 |
| **Multiple VirtualServices** | 같은 host에 여러 VS | VS 통합 |

### 오류 예제 및 수정

```yaml
# 오류: 존재하지 않는 subset 참조
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: reviews-broken
spec:
  hosts:
  - reviews
  http:
  - route:
    - destination:
        host: reviews
        subset: v99   # ❌ 존재하지 않는 subset

# Kiali에서 경고 표시됨
# 수정: DestinationRule에 subset 추가하거나 올바른 subset 사용
```

```bash
# CLI로도 검증 가능
istioctl analyze -n default

# 예상 출력:
# Warning [IST0101] VirtualService reviews-broken
# Referenced subset "v99" not found in destination rule
```

---

## 🛠️ Part 5: 워크로드 및 서비스 뷰

### Workloads 탭

```
확인 가능한 정보:
- Pod 상태 (Running, Pending, Failed)
- 리소스 사용량 (CPU, Memory)
- 트래픽 메트릭 (Inbound/Outbound)
- 관련 Istio 설정
- 로그 링크 (Jaeger)
```

### Services 탭

```
확인 가능한 정보:
- Service 엔드포인트
- 연결된 VirtualService
- 연결된 DestinationRule
- Traffic 정책
- mTLS 상태
```

### Applications 탭

```
확인 가능한 정보:
- 관련 Workload 목록
- 전체 트래픽 상태
- 에러율
- 응답 시간 분포
```

---

## 🛠️ Part 6: 실전 트러블슈팅

### 시나리오 1: 503 에러 발생

```bash
# 1. Kiali Graph에서 빨간색 엣지 확인
# 2. 해당 서비스 클릭 → Service Details
# 3. Inbound Metrics에서 에러율 확인
# 4. Workloads 탭에서 Pod 상태 확인
# 5. Istio Config 탭에서 설정 오류 확인
```

### 시나리오 2: 느린 응답

```bash
# 1. Graph에서 Display → Response Time 활성화
# 2. 느린 서비스 식별 (노란/빨간 숫자)
# 3. 서비스 클릭 → Traces 링크 → Jaeger
# 4. 가장 느린 Span 확인
```

### 시나리오 3: mTLS 확인

```bash
# 1. Graph에서 Display → Security 활성화
# 2. 자물쇠 아이콘 확인 (🔒 = mTLS)
# 3. mTLS가 없는 연결 식별
# 4. Istio Config에서 PeerAuthentication 확인
```

---

## 📋 Kiali API 활용

```bash
# Kiali API로 정보 조회
curl -s http://localhost:20001/kiali/api/namespaces/default/graph | jq

# 앱 목록
curl -s http://localhost:20001/kiali/api/namespaces/default/apps | jq

# Istio 설정 목록
curl -s http://localhost:20001/kiali/api/namespaces/default/istio/config | jq
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 확인 방법 | 완료 |
|---|------|----------|------|
| 1 | Kiali 설치 | `kubectl get pods -l app=kiali` | ☐ |
| 2 | 대시보드 접속 | http://localhost:20001 | ☐ |
| 3 | Graph 확인 | 서비스 토폴로지 시각화 | ☐ |
| 4 | Traffic Animation | 트래픽 흐름 확인 | ☐ |
| 5 | Istio Config 검증 | 오류/경고 확인 | ☐ |
| 6 | mTLS 상태 확인 | 자물쇠 아이콘 | ☐ |
| 7 | Workloads 확인 | Pod 상태 확인 | ☐ |

---

## 🔑 핵심 명령어 요약

```bash
# 설치
kubectl apply -f samples/addons/kiali.yaml

# 접속
kubectl port-forward svc/kiali -n istio-system 20001:20001

# 설정 검증 (CLI)
istioctl analyze -n <namespace>

# 트래픽 생성
while true; do curl -s http://<host>/ > /dev/null; sleep 0.5; done &
```

---

## ➡️ 다음 학습: Day 69

**주제**: Jaeger - 분산 추적

