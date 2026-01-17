# 📅 Day 42: 리소스 관리 (requests, limits) - 안정적인 클러스터 운영의 핵심

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "인프라 비용 및 리소스 효율을 분석해 최적화 전략 수립"

컨테이너 리소스 관리를 마스터하여 안정적인 클러스터 운영과 비용 최적화를 달성합니다.

---

## ⏰ 예상 학습 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 | 45분 | requests/limits 심층 이해 |
| 실습 | 1.5시간 | 리소스 설정 및 모니터링 |
| 심화 | 45분 | QoS 클래스와 비용 최적화 |

---

## 📚 Part 1: 리소스 관리 개념 (45분)

### 왜 리소스 관리가 중요한가?

```
┌─────────────────────────────────────────────────────────────┐
│  리소스 관리 없이 운영하면...                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  😱 시나리오 1: 메모리 폭주                                 │
│  ┌─────────────────────────────────────────────────┐       │
│  │ Pod A: 메모리 80% 사용                          │       │
│  │ Pod B: 메모리 누수로 계속 증가...               │       │
│  │ Pod C: OOMKilled! 결제 서비스 장애!             │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  😱 시나리오 2: CPU 경합                                    │
│  ┌─────────────────────────────────────────────────┐       │
│  │ Pod A: CPU 100% (배치 작업)                     │       │
│  │ Pod B: 응답 지연... 결제 타임아웃!              │       │
│  │ Pod C: 스케줄링 대기 상태                       │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  😱 시나리오 3: 비용 낭비                                   │
│  ┌─────────────────────────────────────────────────┐       │
│  │ 노드당 4GB 메모리                               │       │
│  │ Pod들이 각각 2GB 요청 → 노드 2개 필요           │       │
│  │ 실제 사용량은 각각 512MB → 75% 낭비!            │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### requests vs limits 완전 이해

```
┌─────────────────────────────────────────────────────────────┐
│  requests vs limits                                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  requests (요청)                                            │
│  ┌─────────────────────────────────────────────────┐       │
│  │ - 스케줄러가 노드 선택 시 사용                  │       │
│  │ - "최소 이만큼은 보장해줘"                      │       │
│  │ - 노드의 가용 리소스 > Pod requests 총합        │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  limits (제한)                                              │
│  ┌─────────────────────────────────────────────────┐       │
│  │ - 런타임에서 실제 사용량 제한                   │       │
│  │ - "최대 이만큼만 사용해"                        │       │
│  │ - CPU: 스로틀링 / Memory: OOMKilled            │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  예시:                                                      │
│  ┌─────────────────────────────────────────────────┐       │
│  │ requests:                                       │       │
│  │   cpu: 100m      # 0.1 CPU 보장                 │       │
│  │   memory: 128Mi  # 128MB 보장                   │       │
│  │ limits:                                         │       │
│  │   cpu: 500m      # 최대 0.5 CPU                 │       │
│  │   memory: 256Mi  # 최대 256MB (초과시 kill)     │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### CPU와 Memory 동작 차이

| 리소스 | 단위 | 초과 시 동작 | 특징 |
|--------|------|--------------|------|
| **CPU** | 밀리코어 (m) | 스로틀링 (느려짐) | Compressible |
| **Memory** | Mi, Gi | OOMKilled (강제 종료) | Incompressible |

```bash
# CPU 단위
1000m = 1 CPU = 1 vCPU
500m = 0.5 CPU
100m = 0.1 CPU

# Memory 단위
1Gi = 1024Mi = 1,073,741,824 bytes
1G = 1000M = 1,000,000,000 bytes (주의: 다름!)
```

### QoS (Quality of Service) 클래스

```
┌─────────────────────────────────────────────────────────────┐
│  Kubernetes QoS Classes                                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. Guaranteed (최우선 보호)                                │
│  ┌─────────────────────────────────────────────────┐       │
│  │ 조건: requests = limits (모든 컨테이너)         │       │
│  │ 특징: 메모리 부족 시 가장 마지막에 죽음         │       │
│  │ 용도: 결제 서비스, 핵심 API                     │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  2. Burstable (중간)                                        │
│  ┌─────────────────────────────────────────────────┐       │
│  │ 조건: requests < limits 또는 일부만 설정        │       │
│  │ 특징: 여유 리소스 사용 가능                     │       │
│  │ 용도: 일반 워크로드                             │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  3. BestEffort (최하위)                                     │
│  ┌─────────────────────────────────────────────────┐       │
│  │ 조건: requests/limits 모두 없음                 │       │
│  │ 특징: 메모리 부족 시 가장 먼저 죽음             │       │
│  │ 용도: 테스트, 비중요 작업                       │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  🔥 OOM 발생 시 제거 순서:                                  │
│  BestEffort → Burstable → Guaranteed                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: 실습 (1.5시간)

### 실습 1: 기본 리소스 설정

```bash
# 리소스가 설정된 Pod 생성
kubectl apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: resource-demo
  labels:
    app: resource-demo
spec:
  containers:
  - name: app
    image: nginx:1.24
    resources:
      requests:
        memory: "64Mi"
        cpu: "100m"
      limits:
        memory: "128Mi"
        cpu: "200m"
    ports:
    - containerPort: 80
EOF

# 리소스 설정 확인
kubectl describe pod resource-demo | grep -A 10 "Limits\|Requests"

# QoS 클래스 확인
kubectl get pod resource-demo -o jsonpath='{.status.qosClass}'
# 출력: Burstable (requests ≠ limits 이므로)
```

### 실습 2: QoS 클래스별 Pod 생성

```bash
# Guaranteed QoS Pod
kubectl apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: guaranteed-pod
spec:
  containers:
  - name: app
    image: nginx:1.24
    resources:
      requests:
        memory: "128Mi"
        cpu: "200m"
      limits:
        memory: "128Mi"    # requests = limits
        cpu: "200m"        # requests = limits
EOF

# Burstable QoS Pod
kubectl apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: burstable-pod
spec:
  containers:
  - name: app
    image: nginx:1.24
    resources:
      requests:
        memory: "64Mi"
        cpu: "100m"
      limits:
        memory: "256Mi"    # requests < limits
        cpu: "500m"
EOF

# BestEffort QoS Pod (권장하지 않음)
kubectl apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: besteffort-pod
spec:
  containers:
  - name: app
    image: nginx:1.24
    # resources 섹션 없음
EOF

# QoS 클래스 확인
kubectl get pods -o custom-columns=\
'NAME:.metadata.name,QOS:.status.qosClass,MEMORY_REQ:.spec.containers[0].resources.requests.memory'
```

### 실습 3: LimitRange로 네임스페이스 기본값 설정

```bash
# 테스트 네임스페이스 생성
kubectl create namespace limit-demo

# LimitRange 생성
kubectl apply -f - <<EOF
apiVersion: v1
kind: LimitRange
metadata:
  name: default-limits
  namespace: limit-demo
spec:
  limits:
  - type: Container
    default:           # limits 기본값
      memory: "256Mi"
      cpu: "200m"
    defaultRequest:    # requests 기본값
      memory: "128Mi"
      cpu: "100m"
    max:               # 최대 허용
      memory: "1Gi"
      cpu: "1"
    min:               # 최소 허용
      memory: "32Mi"
      cpu: "50m"
  - type: Pod
    max:
      memory: "2Gi"
      cpu: "2"
EOF

# LimitRange 확인
kubectl describe limitrange default-limits -n limit-demo

# 리소스 없이 Pod 생성 → 기본값 자동 적용
kubectl run test-pod --image=nginx -n limit-demo

# 자동 적용된 리소스 확인
kubectl describe pod test-pod -n limit-demo | grep -A 5 "Limits\|Requests"
# Limits:
#   cpu:     200m
#   memory:  256Mi
# Requests:
#   cpu:     100m
#   memory:  128Mi

# 최대값 초과 시도 → 에러
kubectl apply -f - -n limit-demo <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: over-limit-pod
spec:
  containers:
  - name: app
    image: nginx
    resources:
      requests:
        memory: "2Gi"    # max 1Gi 초과!
EOF
# Error: must be less than or equal to memory limit
```

### 실습 4: ResourceQuota로 네임스페이스 총량 제한

```bash
# ResourceQuota 생성
kubectl apply -f - <<EOF
apiVersion: v1
kind: ResourceQuota
metadata:
  name: compute-quota
  namespace: limit-demo
spec:
  hard:
    # 컴퓨트 리소스
    requests.cpu: "2"
    requests.memory: "2Gi"
    limits.cpu: "4"
    limits.memory: "4Gi"
    
    # 오브젝트 수 제한
    pods: "10"
    services: "5"
    configmaps: "10"
    secrets: "10"
    persistentvolumeclaims: "5"
EOF

# 현재 사용량 확인
kubectl describe resourcequota compute-quota -n limit-demo

# 출력 예시:
# Name:            compute-quota
# Resource         Used   Hard
# --------         ----   ----
# limits.cpu       200m   4
# limits.memory    256Mi  4Gi
# pods             1      10
# requests.cpu     100m   2
# requests.memory  128Mi  2Gi
```

### 실습 5: 실제 리소스 사용량 모니터링

```bash
# Metrics Server 확인 (minikube)
minikube addons enable metrics-server

# 잠시 대기 후 메트릭 수집 확인
kubectl top nodes
kubectl top pods

# 상세 메트릭 확인
kubectl top pods --containers=true

# 특정 네임스페이스
kubectl top pods -n limit-demo

# JSON으로 상세 정보
kubectl get --raw /apis/metrics.k8s.io/v1beta1/pods | jq
```

---

## 🛠️ Part 3: 토스플레이스 실무 패턴 (45분)

### 서비스 유형별 권장 설정

```yaml
# 결제 서비스 (Guaranteed QoS - 안정성 최우선)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-service
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: payment
        image: payment:v1
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "512Mi"   # requests = limits
            cpu: "500m"       # Guaranteed QoS
        
---
# API 게이트웨이 (Burstable - 버스트 허용)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
spec:
  template:
    spec:
      containers:
      - name: gateway
        image: gateway:v1
        resources:
          requests:
            memory: "256Mi"
            cpu: "200m"
          limits:
            memory: "512Mi"   # 버스트 허용
            cpu: "1000m"

---
# 배치 작업 (낮은 우선순위)
apiVersion: batch/v1
kind: Job
metadata:
  name: data-batch
spec:
  template:
    spec:
      containers:
      - name: batch
        image: batch:v1
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "1Gi"     # 충분한 여유
            cpu: "2000m"      # CPU 집중 작업
```

### 리소스 최적화 전략

```
┌─────────────────────────────────────────────────────────────┐
│  리소스 최적화 전략                                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 모니터링 기반 설정                                      │
│  ┌─────────────────────────────────────────────────┐       │
│  │ Prometheus/Grafana로 실제 사용량 분석           │       │
│  │ → P95 기준으로 requests 설정                    │       │
│  │ → 최대값 + 20% 마진으로 limits 설정             │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  2. 서비스 중요도별 차등 적용                               │
│  ┌─────────────────────────────────────────────────┐       │
│  │ Tier 1 (결제): Guaranteed, 높은 리소스          │       │
│  │ Tier 2 (API): Burstable, 적절한 오버커밋        │       │
│  │ Tier 3 (배치): Burstable, 공유 리소스           │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  3. VPA (Vertical Pod Autoscaler) 활용                     │
│  ┌─────────────────────────────────────────────────┐       │
│  │ 자동으로 적정 리소스 추천                       │       │
│  │ → 초기 설정의 어려움 해결                       │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### VPA (Vertical Pod Autoscaler) 맛보기

```yaml
# VPA 리소스 (참고용)
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: my-app-vpa
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: my-app
  updatePolicy:
    updateMode: "Off"  # 추천만, 자동 적용 안 함
  resourcePolicy:
    containerPolicies:
    - containerName: "*"
      minAllowed:
        cpu: 100m
        memory: 128Mi
      maxAllowed:
        cpu: 2
        memory: 4Gi
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | requests/limits 차이 이해 | ☐ |
| 2 | CPU/Memory 동작 차이 이해 | ☐ |
| 3 | QoS 클래스 이해 | ☐ |
| 4 | LimitRange 설정 실습 | ☐ |
| 5 | ResourceQuota 설정 실습 | ☐ |
| 6 | kubectl top으로 모니터링 | ☐ |
| 7 | 서비스 유형별 설정 패턴 이해 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# 리소스 확인
kubectl describe pod <pod> | grep -A 10 Limits
kubectl get pod <pod> -o jsonpath='{.status.qosClass}'
kubectl top pods

# 네임스페이스 리소스 관리
kubectl describe limitrange <name>
kubectl describe resourcequota <name>
kubectl get resourcequota -o wide
```

---

## 📝 면접 대비 질문

### Q1: requests와 limits의 차이점은?
> "requests는 스케줄링 시 노드 선택 기준이며, Pod가 보장받는 최소 리소스입니다. limits는 런타임에 최대 사용량을 제한합니다. CPU는 초과 시 스로틀링되고, Memory는 초과 시 OOMKilled됩니다."

### Q2: QoS 클래스와 OOM 순서를 설명해주세요.
> "Kubernetes는 Guaranteed, Burstable, BestEffort 세 가지 QoS 클래스가 있습니다. 메모리 부족 시 BestEffort가 가장 먼저 제거되고, 그 다음 Burstable, 마지막으로 Guaranteed입니다. 중요한 서비스는 requests=limits로 설정하여 Guaranteed QoS를 얻습니다."

### Q3: 리소스 설정 시 고려사항은?
> "첫째, 실제 모니터링 데이터 기반으로 설정합니다. 둘째, requests는 P95 사용량, limits는 최대값 + 마진으로 설정합니다. 셋째, 서비스 중요도에 따라 QoS를 차등 적용합니다. 넷째, LimitRange와 ResourceQuota로 네임스페이스 수준 제어를 합니다."

---

## ➡️ 다음 학습: Day 43

**주제**: Probe (Health Check)
- livenessProbe, readinessProbe, startupProbe
- 헬스체크 설계 패턴
- 장애 자동 복구 구현
