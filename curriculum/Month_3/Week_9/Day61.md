# Day 61: Istio 소개 및 설치

## 오늘의 목표

토스플레이스 연결점: "Kubernetes와 Service Mesh에 대한 경험"
"Istio 기반의 서비스 메시 운영 및 트래픽 관리"

Istio Service Mesh의 개념과 아키텍처를 이해하고 Kubernetes 클러스터에 설치합니다. 토스플레이스의 핵심 기술 스택인 Istio를 본격적으로 학습합니다.

---

## 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 핵심 개념 | 1시간 | Service Mesh, Istio 아키텍처 |
| 설치 | 1시간 | istioctl, 프로필 선택 |
| 검증 | 1시간 | 샘플 앱 배포, 사이드카 확인 |
| 기본 탐색 | 1시간 | 대시보드, 메트릭 |

---

## Part 1: Service Mesh란? (1시간)

### 1.1 왜 Service Mesh가 필요한가?

```
마이크로서비스 환경의 문제:

1. 서비스 간 통신 복잡성
   - 수십~수백 개의 서비스
   - 서로 다른 언어/프레임워크
   - 각 서비스에서 통신 로직 구현?

2. 공통 관심사 (Cross-cutting Concerns)
   - 로드 밸런싱
   - 서비스 디스커버리
   - 인증/인가
   - 암호화 (mTLS)
   - 관측성 (메트릭, 로깅, 트레이싱)
   - 장애 처리 (Retry, Timeout, Circuit Breaker)

3. 문제점
   - 각 서비스에 중복 구현
   - 언어별 라이브러리 다름
   - 코드 변경 필요
   - 일관성 유지 어려움

Service Mesh 해결:
   - 인프라 레이어에서 처리
   - 애플리케이션 코드 수정 불필요
   - 중앙 집중식 정책 관리
```

### 1.2 Service Mesh 아키텍처

```
Service Mesh 구조:

┌─────────────────────────────────────────────────────────────────┐
│                       Control Plane                              │
│                   (정책, 설정, 인증서 관리)                      │
└─────────────────────────────────────────────────────────────────┘
           │                    │                    │
           ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   Pod A         │  │   Pod B         │  │   Pod C         │
│ ┌─────────────┐ │  │ ┌─────────────┐ │  │ ┌─────────────┐ │
│ │ App         │ │  │ │ App         │ │  │ │ App         │ │
│ └─────────────┘ │  │ └─────────────┘ │  │ └─────────────┘ │
│ ┌─────────────┐ │  │ ┌─────────────┐ │  │ ┌─────────────┐ │
│ │ Sidecar     │◄├──┼►│ Sidecar     │◄├──┼►│ Sidecar     │ │
│ │ (Proxy)     │ │  │ │ (Proxy)     │ │  │ │ (Proxy)     │ │
│ └─────────────┘ │  │ └─────────────┘ │  │ └─────────────┘ │
└─────────────────┘  └─────────────────┘  └─────────────────┘
      Data Plane         Data Plane         Data Plane
```

### 1.3 Istio 아키텍처

```
Istio 컴포넌트:

Control Plane:
┌─────────────────────────────────────────────────────────────────┐
│                         istiod                                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │   Pilot     │  │   Citadel   │  │   Galley    │              │
│  │ (트래픽)    │  │ (보안/인증) │  │ (설정)      │              │
│  └─────────────┘  └─────────────┘  └─────────────┘              │
└─────────────────────────────────────────────────────────────────┘

Data Plane:
┌─────────────────────────────────────────────────────────────────┐
│                     Envoy Proxy (Sidecar)                        │
│  - 모든 Pod에 자동 주입                                          │
│  - 모든 인/아웃바운드 트래픽 처리                                │
│  - mTLS, 로드밸런싱, 관측성                                      │
└─────────────────────────────────────────────────────────────────┘

Ingress/Egress Gateway:
┌─────────────────────────────────────────────────────────────────┐
│  Ingress Gateway: 외부 → 메시 진입점                            │
│  Egress Gateway: 메시 → 외부 출구점                             │
└─────────────────────────────────────────────────────────────────┘
```

### 1.4 Istio 핵심 기능

| 기능 | 설명 |
|------|------|
| **Traffic Management** | 라우팅, 로드밸런싱, Canary, A/B 테스트 |
| **Security** | mTLS, 인증, 인가 |
| **Observability** | 메트릭, 로깅, 분산 트레이싱 |
| **Policy** | 속도 제한, 접근 제어 |

---

## Part 2: Istio 설치 (1시간)

### 실습 1: istioctl 설치

```bash
# istioctl 다운로드
curl -L https://istio.io/downloadIstio | sh -

# 또는 특정 버전
curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.20.0 sh -

# 디렉토리 이동
cd istio-*

# PATH 추가
export PATH=$PWD/bin:$PATH

# 영구 설정 (bash)
echo "export PATH=$PWD/bin:\$PATH" >> ~/.bashrc
source ~/.bashrc

# 버전 확인
istioctl version
```

### 실습 2: 프로필 확인

```bash
# 사용 가능한 프로필 목록
istioctl profile list

# 출력:
# Istio configuration profiles:
#     default
#     demo
#     minimal
#     remote
#     empty
#     preview

# 프로필 상세 확인
istioctl profile dump demo

# 프로필 비교
istioctl profile diff default demo
```

**프로필 설명:**

| 프로필 | 설명 | 사용 |
|--------|------|------|
| default | 프로덕션 권장 | 운영 환경 |
| demo | 모든 기능 활성화 | 학습, 테스트 |
| minimal | Control Plane만 | 리소스 제한 |
| remote | 멀티 클러스터 | 외부 Control Plane |

### 실습 3: Istio 설치 (demo 프로필)

```bash
# 클러스터 준비 확인
kubectl cluster-info

# Istio 설치 (demo 프로필)
istioctl install --set profile=demo -y

# 설치 확인
kubectl get pods -n istio-system

# 출력 예시:
# NAME                                    READY   STATUS    RESTARTS   AGE
# istio-egressgateway-xxx                 1/1     Running   0          1m
# istio-ingressgateway-xxx                1/1     Running   0          1m
# istiod-xxx                              1/1     Running   0          1m

# 서비스 확인
kubectl get svc -n istio-system
```

### 실습 4: Sidecar 자동 주입 설정

```bash
# 네임스페이스에 라벨 추가 (사이드카 자동 주입)
kubectl label namespace default istio-injection=enabled

# 확인
kubectl get namespace default --show-labels

# 출력:
# NAME      STATUS   AGE   LABELS
# default   Active   1d    istio-injection=enabled,...
```

---

## Part 3: 샘플 애플리케이션 배포 (1시간)

### 실습 5: Bookinfo 샘플 앱 배포

```bash
# Bookinfo 샘플 앱 배포
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/bookinfo/platform/kube/bookinfo.yaml

# Pod 확인 (사이드카 주입 확인)
kubectl get pods

# 출력 예시:
# NAME                              READY   STATUS    RESTARTS   AGE
# details-v1-xxx                    2/2     Running   0          1m
# productpage-v1-xxx                2/2     Running   0          1m
# ratings-v1-xxx                    2/2     Running   0          1m
# reviews-v1-xxx                    2/2     Running   0          1m
# reviews-v2-xxx                    2/2     Running   0          1m
# reviews-v3-xxx                    2/2     Running   0          1m

# 2/2 = 앱 컨테이너 + Envoy 사이드카

# 서비스 확인
kubectl get svc
```

### 실습 6: 사이드카 확인

```bash
# Pod 상세 확인
kubectl describe pod -l app=productpage

# 컨테이너 목록 확인
kubectl get pod -l app=productpage -o jsonpath='{.items[0].spec.containers[*].name}'
# 출력: productpage istio-proxy

# 사이드카 버전 확인
kubectl exec -it deploy/productpage-v1 -c istio-proxy -- pilot-agent version

# Envoy 설정 확인
kubectl exec -it deploy/productpage-v1 -c istio-proxy -- curl localhost:15000/config_dump | head -100
```

### 실습 7: Gateway 및 VirtualService 적용

```bash
# Gateway와 VirtualService 적용
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/bookinfo/networking/bookinfo-gateway.yaml

# 확인
kubectl get gateway
kubectl get virtualservice

# Ingress Gateway IP 확인
kubectl get svc istio-ingressgateway -n istio-system

# minikube인 경우
minikube tunnel  # 별도 터미널에서 실행

# 또는 NodePort 사용
INGRESS_PORT=$(kubectl get svc istio-ingressgateway -n istio-system -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')
INGRESS_HOST=$(minikube ip)
echo "http://$INGRESS_HOST:$INGRESS_PORT/productpage"
```

### 실습 8: 앱 접속 테스트

```bash
# 애플리케이션 접속 테스트
curl -s "http://$INGRESS_HOST:$INGRESS_PORT/productpage" | grep -o "<title>.*</title>"

# 여러 번 요청 (reviews 버전이 바뀌는 것 확인)
for i in {1..10}; do
  curl -s "http://$INGRESS_HOST:$INGRESS_PORT/productpage" | grep -o "reviews-v[0-9]" | head -1
done

# 브라우저에서 확인
echo "브라우저에서 열기: http://$INGRESS_HOST:$INGRESS_PORT/productpage"
```

---

## Part 4: 관측성 도구 설치 (1시간)

### 실습 9: 관측성 애드온 설치

```bash
# Kiali, Prometheus, Grafana, Jaeger 설치
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/prometheus.yaml
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/grafana.yaml
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/jaeger.yaml
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/kiali.yaml

# Pod 확인
kubectl get pods -n istio-system

# 서비스 확인
kubectl get svc -n istio-system | grep -E "kiali|grafana|jaeger|prometheus"
```

### 실습 10: 대시보드 접속

```bash
# Kiali 대시보드 (서비스 메시 시각화)
istioctl dashboard kiali

# Grafana (메트릭 대시보드)
istioctl dashboard grafana

# Jaeger (분산 트레이싱)
istioctl dashboard jaeger

# Prometheus (메트릭 쿼리)
istioctl dashboard prometheus
```

### 실습 11: 트래픽 생성 및 모니터링

```bash
# 트래픽 생성
for i in {1..100}; do
  curl -s "http://$INGRESS_HOST:$INGRESS_PORT/productpage" > /dev/null
  sleep 0.5
done

# Kiali에서 확인:
# 1. Graph 메뉴 선택
# 2. Namespace: default 선택
# 3. 서비스 간 트래픽 흐름 확인
```

---

## 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | Service Mesh 이해 | 필요성, 아키텍처 | |
| 2 | Istio 아키텍처 | istiod, Envoy, Gateway | |
| 3 | istioctl 설치 | PATH 설정 | |
| 4 | Istio 설치 | demo 프로필 | |
| 5 | Sidecar 주입 | 라벨 설정, 확인 | |
| 6 | Bookinfo 배포 | 샘플 앱 | |
| 7 | Gateway 설정 | 외부 접속 | |
| 8 | 관측성 도구 | Kiali, Grafana, Jaeger | |

---

## 핵심 명령어

```bash
# 설치/관리
istioctl install --set profile=demo
istioctl verify-install
istioctl uninstall --purge

# 사이드카 주입
kubectl label namespace <ns> istio-injection=enabled

# 대시보드
istioctl dashboard kiali
istioctl dashboard grafana
istioctl dashboard jaeger

# 분석
istioctl analyze
istioctl proxy-status
```

---

## 면접 대비 핵심 포인트

**Q1: Service Mesh란 무엇인가요?**
> "마이크로서비스 간 통신을 인프라 레이어에서 처리하는 아키텍처입니다. 각 Pod에 사이드카 프록시를 주입하여 mTLS, 로드밸런싱, 관측성 등을 애플리케이션 코드 수정 없이 구현합니다."

**Q2: Istio의 주요 컴포넌트는?**
> "Control Plane인 istiod와 Data Plane인 Envoy 프록시입니다. istiod는 설정과 인증서를 관리하고, Envoy는 각 Pod에서 실제 트래픽을 처리합니다. Ingress/Egress Gateway는 메시의 진입/출구점입니다."

**Q3: 사이드카 주입은 어떻게 동작하나요?**
> "네임스페이스에 istio-injection=enabled 라벨을 추가하면, 새로 생성되는 Pod에 자동으로 Envoy 사이드카가 주입됩니다. MutatingWebhook을 통해 Pod 생성 시 컨테이너가 추가됩니다."

---

## 정리

```bash
# 샘플 앱 삭제 (나중에)
# kubectl delete -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/bookinfo/platform/kube/bookinfo.yaml

# Istio 삭제 (필요시)
# istioctl uninstall --purge
# kubectl delete namespace istio-system
```

---

## 다음 학습: Day 62

주제: VirtualService 상세
- 트래픽 라우팅 규칙
- 헤더/경로 기반 라우팅
- 가중치 기반 트래픽 분배
