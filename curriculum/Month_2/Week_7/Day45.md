# 📅 Day 45: HPA (Horizontal Pod Autoscaler)

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "대규모 트래픽 환경"에서 자동 스케일링
> 부하에 따라 자동으로 Pod 수를 조절하여 안정적인 서비스 운영

토스플레이스는 오프라인 결제 서비스로 트래픽 변동이 큽니다. 점심/저녁 시간대 피크, 이벤트 시 급증하는 트래픽을 자동으로 처리하는 것이 필수입니다.

---

## ⏰ 예상 학습 시간: 4시간

---

## 📚 Part 1: HPA 개념 (1시간)

### 1.1 HPA란?

HPA(Horizontal Pod Autoscaler)는 CPU/메모리 사용률 또는 커스텀 메트릭 기반으로 Pod 수를 자동 조절합니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│  HPA 동작 원리                                                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │              Metrics Server / Prometheus                      │   │
│  │  (CPU, Memory, Custom Metrics 수집)                          │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                         │                                           │
│                         ▼                                           │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                      HPA Controller                           │   │
│  │  1. 메트릭 수집 (15초마다)                                    │   │
│  │  2. 목표 사용률과 비교                                        │   │
│  │  3. 필요한 Pod 수 계산                                        │   │
│  │  4. Deployment replica 조정                                   │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                         │                                           │
│                         ▼                                           │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                    Deployment                                 │   │
│  │  replicas: 2 → 5 (scale up) 또는 2 → 1 (scale down)         │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  스케일링 공식:                                                     │
│  desiredReplicas = ceil[currentReplicas × (currentMetric/target)]  │
│                                                                      │
│  예시: 현재 2개 Pod, CPU 80%, 목표 50%                             │
│  → desiredReplicas = ceil[2 × (80/50)] = ceil[3.2] = 4개           │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 HPA vs VPA vs Cluster Autoscaler

| 유형 | 대상 | 조절 방식 | 사용 사례 |
|------|------|----------|----------|
| **HPA** | Pod 수 | 수평 확장 (Pod 추가/제거) | 트래픽 변동 대응 |
| **VPA** | Pod 리소스 | 수직 확장 (리소스 조정) | 적정 리소스 자동 설정 |
| **Cluster Autoscaler** | Node 수 | 클러스터 확장 | 노드 부족 시 자동 추가 |

```
┌─────────────────────────────────────────────────────────────────────┐
│  스케일링 계층                                                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Level 1: HPA (Pod 레벨)                                            │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  트래픽 증가 → Pod 수 증가                                   │    │
│  │  [Pod] [Pod] → [Pod] [Pod] [Pod] [Pod]                      │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Level 2: Cluster Autoscaler (Node 레벨)                            │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Pod가 스케줄링 안 됨 (리소스 부족)                          │    │
│  │  → Node 자동 추가                                           │    │
│  │  [Node] [Node] → [Node] [Node] [Node]                       │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  토스플레이스 전략:                                                 │
│  - HPA로 트래픽 대응 (피크 시간)                                   │
│  - Cluster Autoscaler로 노드 자동 관리                              │
│  - 비용 최적화: 야간에는 최소 노드로 운영                           │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.3 HPA 설정 옵션

| 옵션 | 설명 | 기본값 |
|------|------|--------|
| `minReplicas` | 최소 Pod 수 | 1 |
| `maxReplicas` | 최대 Pod 수 | 필수 지정 |
| `targetCPUUtilizationPercentage` | 목표 CPU 사용률 | - |
| `metrics` | 스케일링 메트릭 (v2) | - |
| `behavior` | 스케일링 동작 제어 | - |
| `scaleDown.stabilizationWindowSeconds` | 스케일 다운 안정화 기간 | 300초 |

---

## 🛠️ Part 2: 실습 환경 준비 (30분)

### 2.1 Metrics Server 설치

HPA는 메트릭 수집을 위해 Metrics Server가 필요합니다.

```bash
# minikube에서 metrics-server 활성화
minikube addons enable metrics-server

# 또는 직접 설치 (클라우드/온프레미스)
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# metrics-server 상태 확인 (몇 분 대기)
kubectl get deployment metrics-server -n kube-system

# 메트릭 수집 확인
kubectl top nodes
kubectl top pods -A
```

### 2.2 테스트용 Deployment 배포

```bash
# HPA 테스트용 Deployment (CPU 부하 생성 가능한 이미지)
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: php-apache
  labels:
    app: php-apache
spec:
  replicas: 1
  selector:
    matchLabels:
      app: php-apache
  template:
    metadata:
      labels:
        app: php-apache
    spec:
      containers:
      - name: php-apache
        image: registry.k8s.io/hpa-example
        ports:
        - containerPort: 80
        resources:
          requests:
            cpu: "200m"
            memory: "64Mi"
          limits:
            cpu: "500m"
            memory: "128Mi"
---
apiVersion: v1
kind: Service
metadata:
  name: php-apache
spec:
  selector:
    app: php-apache
  ports:
  - port: 80
    targetPort: 80
EOF

# 배포 확인
kubectl get deploy,svc php-apache
```

---

## 🛠️ Part 3: HPA 실습 (1.5시간)

### 실습 1: 기본 HPA 생성 (CPU 기반)

```bash
# 방법 1: kubectl autoscale 명령어
kubectl autoscale deployment php-apache \
  --cpu-percent=50 \
  --min=1 \
  --max=10

# HPA 확인
kubectl get hpa

# 출력 예시:
# NAME         REFERENCE               TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
# php-apache   Deployment/php-apache   0%/50%    1         10        1          30s
```

```yaml
# 방법 2: YAML 정의 (권장)
# hpa-basic.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: php-apache-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: php-apache
  minReplicas: 1
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 50
```

```bash
# YAML로 HPA 생성
kubectl apply -f hpa-basic.yaml

# HPA 상세 정보
kubectl describe hpa php-apache-hpa
```

### 실습 2: 부하 테스트 - 스케일 업

```bash
# 터미널 1: HPA 모니터링
watch kubectl get hpa,pods -l app=php-apache

# 또는
kubectl get hpa -w
```

```bash
# 터미널 2: 부하 발생
kubectl run load-generator \
  --image=busybox:1.28 \
  --restart=Never \
  --rm -i --tty \
  -- /bin/sh -c "while sleep 0.01; do wget -q -O- http://php-apache; done"
```

```bash
# 관찰 포인트:
# 1. CPU 사용률 증가 (50% 초과)
# 2. HPA가 Pod 수 증가
# 3. 새 Pod가 Running 상태로 전환
# 4. CPU 사용률 안정화

# 예상 출력:
# NAME         TARGETS    MINPODS   MAXPODS   REPLICAS
# php-apache   120%/50%   1         10        1
# php-apache   120%/50%   1         10        4
# php-apache   75%/50%    1         10        4
# php-apache   55%/50%    1         10        6
```

### 실습 3: 부하 제거 - 스케일 다운

```bash
# 터미널 2에서 Ctrl+C로 부하 중지

# 몇 분 후 스케일 다운 확인 (기본 5분 대기)
kubectl get hpa -w

# 스케일 다운 과정:
# 1. CPU 사용률 감소
# 2. stabilization window 대기 (기본 5분)
# 3. Pod 수 점진적 감소

# 예상 출력:
# NAME         TARGETS   MINPODS   MAXPODS   REPLICAS
# php-apache   55%/50%   1         10        6
# php-apache   10%/50%   1         10        6  (대기 중)
# php-apache   5%/50%    1         10        3
# php-apache   0%/50%    1         10        1
```

### 실습 4: 메모리 기반 HPA

```yaml
# hpa-memory.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: php-apache-memory-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: php-apache
  minReplicas: 1
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 70
```

### 실습 5: CPU + 메모리 복합 메트릭

```yaml
# hpa-combined.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: php-apache-combined-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: php-apache
  minReplicas: 2
  maxReplicas: 10
  metrics:
  # CPU 사용률
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 50
  # 메모리 사용률
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 70
```

---

## 🛠️ Part 4: 고급 HPA 설정 (1시간)

### 4.1 스케일링 동작 제어 (behavior)

```yaml
# hpa-behavior.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: php-apache-behavior-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: php-apache
  minReplicas: 2
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 50
  
  behavior:
    # 스케일 업 동작
    scaleUp:
      stabilizationWindowSeconds: 0    # 즉시 스케일 업
      policies:
      - type: Percent
        value: 100                      # 현재의 100%까지 증가 가능
        periodSeconds: 15
      - type: Pods
        value: 4                        # 한 번에 최대 4개 추가
        periodSeconds: 15
      selectPolicy: Max                 # 위 정책 중 큰 값 선택
    
    # 스케일 다운 동작
    scaleDown:
      stabilizationWindowSeconds: 300   # 5분 대기 후 스케일 다운
      policies:
      - type: Percent
        value: 10                       # 현재의 10%씩 감소
        periodSeconds: 60
      selectPolicy: Min                 # 위 정책 중 작은 값 선택 (보수적)
```

### 4.2 스케일링 정책 설명

```
┌─────────────────────────────────────────────────────────────────────┐
│  HPA Behavior 설정                                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  scaleUp:                                                           │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  stabilizationWindowSeconds: 0                              │    │
│  │  → 스케일 업은 즉시 반응 (트래픽 급증 대응)                  │    │
│  │                                                             │    │
│  │  policies:                                                  │    │
│  │  - Percent: 100% → 현재 2개면 최대 4개까지 증가             │    │
│  │  - Pods: 4 → 절대값으로 4개까지 증가                        │    │
│  │                                                             │    │
│  │  selectPolicy: Max → 둘 중 큰 값 선택 (빠른 스케일 업)      │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  scaleDown:                                                         │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  stabilizationWindowSeconds: 300 (5분)                      │    │
│  │  → 스케일 다운은 5분 대기 (플래핑 방지)                     │    │
│  │                                                             │    │
│  │  policies:                                                  │    │
│  │  - Percent: 10% → 현재 10개면 1개 감소                      │    │
│  │                                                             │    │
│  │  selectPolicy: Min → 보수적으로 감소 (안정성 중시)          │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  토스플레이스 권장 설정:                                            │
│  - 스케일 업: 빠르게 (0초 대기, 높은 증가율)                       │
│  - 스케일 다운: 천천히 (5-10분 대기, 낮은 감소율)                  │
│  - 이유: 트래픽 급증 대응 + 안정성 확보                            │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.3 외부 메트릭 기반 HPA (Prometheus)

```yaml
# hpa-external-metrics.yaml (Prometheus Adapter 필요)
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: php-apache-custom-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: php-apache
  minReplicas: 2
  maxReplicas: 10
  metrics:
  # Pod 메트릭 (커스텀)
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "100"   # Pod당 초당 100개 요청
  # 외부 메트릭
  - type: External
    external:
      metric:
        name: queue_messages_ready
        selector:
          matchLabels:
            queue: orders
      target:
        type: AverageValue
        averageValue: "50"    # 큐당 50개 메시지
```

### 4.4 실무 팁: 스케일 다운 비활성화

```yaml
# 피크 시간대에 스케일 다운 방지
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: peak-time-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: payment-api
  minReplicas: 5
  maxReplicas: 50
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 60
  behavior:
    scaleDown:
      selectPolicy: Disabled  # 스케일 다운 비활성화
```

---

## 📊 Part 5: 모니터링 및 트러블슈팅 (30분)

### 5.1 HPA 상태 확인

```bash
# HPA 목록
kubectl get hpa

# HPA 상세 정보
kubectl describe hpa php-apache-hpa

# 주요 확인 포인트:
# - Conditions: AbleToScale, ScalingActive, ScalingLimited
# - Events: 스케일 업/다운 이력

# 예시 출력:
# Conditions:
#   Type            Status  Reason
#   ----            ------  ------
#   AbleToScale     True    ReadyForNewScale
#   ScalingActive   True    ValidMetricFound
#   ScalingLimited  False   DesiredWithinRange
```

### 5.2 일반적인 문제와 해결

```bash
# 문제 1: TARGETS가 <unknown>으로 표시
# 원인: Metrics Server 미설치 또는 resources.requests 미설정
kubectl top pods  # 메트릭 수집 확인
kubectl describe deploy php-apache | grep -A5 "Resources"

# 문제 2: ScalingActive가 False
# 원인: 메트릭 수집 실패
kubectl describe hpa php-apache-hpa | grep -A3 "Conditions"

# 문제 3: 스케일 업이 안 됨
# 원인: maxReplicas 도달 또는 노드 리소스 부족
kubectl get hpa  # REPLICAS가 maxReplicas와 같은지 확인
kubectl describe nodes | grep -A5 "Allocated resources"

# 문제 4: 스케일 다운이 안 됨
# 원인: stabilizationWindowSeconds 대기 중
kubectl describe hpa | grep -A5 "Events"
```

### 5.3 HPA 메트릭 Prometheus 쿼리

```promql
# HPA 상태
kube_horizontalpodautoscaler_status_current_replicas
kube_horizontalpodautoscaler_status_desired_replicas
kube_horizontalpodautoscaler_spec_min_replicas
kube_horizontalpodautoscaler_spec_max_replicas

# 스케일링 이벤트 (Deployment replica 변화)
changes(kube_deployment_status_replicas[1h])
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | Metrics Server 설치 및 확인 | ☐ |
| 2 | 기본 HPA 생성 (CPU 기반) | ☐ |
| 3 | 부하 테스트로 스케일 업 확인 | ☐ |
| 4 | 부하 제거 후 스케일 다운 확인 | ☐ |
| 5 | 복합 메트릭 HPA 설정 | ☐ |
| 6 | behavior로 스케일링 동작 제어 | ☐ |
| 7 | HPA 트러블슈팅 이해 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# HPA 생성
kubectl autoscale deployment <name> --cpu-percent=50 --min=1 --max=10

# HPA 확인
kubectl get hpa
kubectl describe hpa <name>

# 메트릭 확인
kubectl top pods
kubectl top nodes

# HPA 삭제
kubectl delete hpa <name>
```

---

## 💡 면접 대비 핵심 포인트

### Q1: HPA란 무엇인가요?
**A**: Horizontal Pod Autoscaler로, CPU/메모리 사용률이나 커스텀 메트릭 기반으로 Pod 수를 자동 조절하는 Kubernetes 기능입니다.

### Q2: HPA가 동작하려면 무엇이 필요한가요?
**A**: 
1. Metrics Server (또는 Prometheus Adapter)
2. Deployment/StatefulSet의 resources.requests 설정
3. HPA 리소스 정의

### Q3: 스케일 다운이 느린 이유는?
**A**: stabilizationWindowSeconds (기본 5분) 동안 대기하여 트래픽 변동에 의한 플래핑을 방지합니다.

### Q4: CPU와 메모리 중 무엇을 기준으로 해야 하나요?
**A**: 
- CPU: 연산 집약적 워크로드 (API 서버)
- 메모리: 캐싱, 데이터 처리 워크로드
- 실무에서는 둘 다 설정하고, 먼저 임계값에 도달한 메트릭 기준으로 스케일링

---

## ➡️ 다음 학습: Day 46

**주제**: Kubernetes 로깅
- 컨테이너 로그 수집
- 로그 집계 (Fluentd/Fluent Bit)
- 중앙 로깅 시스템
