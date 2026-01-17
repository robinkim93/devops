# 📅 Day 33: Deployment 학습

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "무중단 배포", "롤백", "Kubernetes 클러스터 운영/최적화"
> "개발자들이 더 빠르고 안전하게 서비스를 배포할 수 있도록"

Deployment는 Kubernetes에서 가장 많이 사용되는 워크로드 리소스입니다. 무중단 배포, 롤백, 스케일링 등 토스플레이스에서 요구하는 핵심 운영 기능을 Deployment를 통해 구현합니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| Deployment 이론 | 45분 | 구조, ReplicaSet 관계 |
| 기본 실습 | 1시간 | 생성, 스케일링 |
| 롤링 업데이트 | 1시간 | 배포 전략, 롤백 |
| 고급 설정 | 1.25시간 | 전략 커스텀, 자동 복구 |

---

## 📚 Part 1: Deployment란? (45분)

### 1.1 Pod vs Deployment

```
┌─────────────────────────────────────────────────────────────────────┐
│  Pod만 사용했을 때의 문제점                                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ❌ Pod가 죽으면 끝 (자동 복구 없음)                               │
│  ❌ 여러 개 띄우기 번거로움 (각각 YAML 작성)                       │
│  ❌ 배포/롤백 어려움 (수동 관리)                                   │
│  ❌ 스케일링 불가                                                   │
│                                                                      │
├─────────────────────────────────────────────────────────────────────┤
│  Deployment 사용 시 장점                                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ✅ 자동 복구 (Desired State 유지)                                  │
│  ✅ 쉬운 스케일링 (replicas 변경)                                   │
│  ✅ 롤링 업데이트 (무중단 배포)                                     │
│  ✅ 롤백 지원 (revision history)                                    │
│  ✅ 선언적 관리 (Desired vs Actual)                                 │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 Deployment 구조

```
┌─────────────────────────────────────────────────────────────────────┐
│  Deployment → ReplicaSet → Pod 관계                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Deployment                                                          │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  spec:                                                       │    │
│  │    replicas: 3                                               │    │
│  │    selector: {app: nginx}                                    │    │
│  │    template:                                                 │    │
│  │      containers: [nginx:1.24]                                │    │
│  └──────────────────────────┬──────────────────────────────────┘    │
│                             │                                        │
│                             │ 자동 생성                              │
│                             ▼                                        │
│  ReplicaSet (nginx-deployment-abc123)                               │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  spec:                                                       │    │
│  │    replicas: 3                                               │    │
│  │    selector: {app: nginx, pod-template-hash: abc123}         │    │
│  └──────────────────────────┬──────────────────────────────────┘    │
│                             │                                        │
│                             │ replicas 수만큼 생성                   │
│              ┌──────────────┼──────────────┐                        │
│              ▼              ▼              ▼                        │
│         ┌────────┐    ┌────────┐    ┌────────┐                      │
│         │ Pod 1  │    │ Pod 2  │    │ Pod 3  │                      │
│         │nginx-  │    │nginx-  │    │nginx-  │                      │
│         │abc-xxx │    │abc-yyy │    │abc-zzz │                      │
│         └────────┘    └────────┘    └────────┘                      │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.3 Deployment의 역할

| 컴포넌트 | 역할 | 관리 대상 |
|---------|------|----------|
| **Deployment** | 배포 전략, 롤백 히스토리 관리 | ReplicaSet |
| **ReplicaSet** | Pod 복제 수 유지 | Pod |
| **Pod** | 실제 컨테이너 실행 | Container |

---

## 🛠️ Part 2: 실습 - Deployment 기본 (1시간)

### 실습 1: Deployment 생성 (30분)

```bash
mkdir -p ~/k8s-practice/day33
cd ~/k8s-practice/day33

# Deployment YAML 작성
cat << 'EOF' > nginx-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
  labels:
    app: nginx
    environment: development
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
        version: v1
    spec:
      containers:
      - name: nginx
        image: nginx:1.24
        ports:
        - containerPort: 80
        resources:
          requests:
            cpu: 100m
            memory: 64Mi
          limits:
            cpu: 200m
            memory: 128Mi
EOF

# 적용
kubectl apply -f nginx-deployment.yaml

# 확인
echo "=== Deployment 상태 ==="
kubectl get deployment nginx-deployment

echo ""
echo "=== ReplicaSet 상태 ==="
kubectl get replicaset -l app=nginx

echo ""
echo "=== Pod 상태 ==="
kubectl get pods -l app=nginx -o wide

echo ""
echo "=== Deployment 상세 ==="
kubectl describe deployment nginx-deployment
```

**출력 해석:**

```
NAME               READY   UP-TO-DATE   AVAILABLE   AGE
nginx-deployment   3/3     3            3           30s

- READY: 준비된 Pod / 원하는 Pod (3/3)
- UP-TO-DATE: 최신 템플릿으로 생성된 Pod 수
- AVAILABLE: 서비스 가능한 Pod 수
```

### 실습 2: Deployment YAML 상세 분석

```yaml
apiVersion: apps/v1       # API 그룹: apps, 버전: v1
kind: Deployment          # 리소스 종류
metadata:
  name: nginx-deployment  # Deployment 이름
  labels:                 # Deployment 자체의 라벨
    app: nginx
spec:
  replicas: 3             # 원하는 Pod 수
  
  selector:               # 어떤 Pod를 관리할지 선택
    matchLabels:          # Pod 라벨과 일치해야 함
      app: nginx
  
  template:               # Pod 템플릿
    metadata:
      labels:             # Pod에 붙는 라벨
        app: nginx        # ⚠️ selector와 일치해야 함!
    spec:
      containers:
      - name: nginx
        image: nginx:1.24
        ports:
        - containerPort: 80
```

**중요**: `selector.matchLabels`와 `template.metadata.labels`가 일치해야 합니다!

### 실습 3: 스케일링 (30분)

```bash
# 현재 상태 확인
kubectl get deployment nginx-deployment

# 스케일 업 (3 → 5)
kubectl scale deployment nginx-deployment --replicas=5

echo "=== 스케일 업 후 ==="
kubectl get pods -l app=nginx

# Pod 생성 과정 관찰 (다른 터미널에서)
# kubectl get pods -l app=nginx -w

# 스케일 다운 (5 → 2)
kubectl scale deployment nginx-deployment --replicas=2

echo "=== 스케일 다운 후 ==="
kubectl get pods -l app=nginx

# YAML 수정으로 스케일링 (선언적 방법)
kubectl patch deployment nginx-deployment -p '{"spec":{"replicas":3}}'

# 또는 YAML 파일 수정 후 apply
kubectl apply -f nginx-deployment.yaml
```

**스케일링 동작 원리:**

```
스케일 업 (3 → 5):
1. Deployment가 ReplicaSet의 replicas를 5로 변경
2. ReplicaSet이 부족한 2개 Pod 생성
3. 새 Pod는 기존 Pod와 동일한 템플릿으로 생성

스케일 다운 (5 → 2):
1. Deployment가 ReplicaSet의 replicas를 2로 변경
2. ReplicaSet이 3개 Pod를 Terminating
3. 남은 2개 Pod 유지
```

---

## 🛠️ Part 3: 롤링 업데이트 (1시간)

### 3.1 롤링 업데이트 개념

```
┌─────────────────────────────────────────────────────────────────────┐
│  롤링 업데이트 과정 (nginx:1.24 → nginx:1.25)                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Phase 1: 새 ReplicaSet 생성                                        │
│  ┌────────────────┐    ┌────────────────┐                           │
│  │  RS (v1.24)    │    │  RS (v1.25)    │                           │
│  │  replicas: 3   │    │  replicas: 0   │                           │
│  │  [●][●][●]     │    │                │                           │
│  └────────────────┘    └────────────────┘                           │
│                                                                      │
│  Phase 2: 점진적 전환                                               │
│  ┌────────────────┐    ┌────────────────┐                           │
│  │  RS (v1.24)    │    │  RS (v1.25)    │                           │
│  │  replicas: 2   │    │  replicas: 1   │                           │
│  │  [●][●]        │    │  [◐]           │                           │
│  └────────────────┘    └────────────────┘                           │
│                                                                      │
│  Phase 3: 전환 진행                                                 │
│  ┌────────────────┐    ┌────────────────┐                           │
│  │  RS (v1.24)    │    │  RS (v1.25)    │                           │
│  │  replicas: 1   │    │  replicas: 2   │                           │
│  │  [●]           │    │  [●][◐]        │                           │
│  └────────────────┘    └────────────────┘                           │
│                                                                      │
│  Phase 4: 완료                                                      │
│  ┌────────────────┐    ┌────────────────┐                           │
│  │  RS (v1.24)    │    │  RS (v1.25)    │                           │
│  │  replicas: 0   │    │  replicas: 3   │                           │
│  │                │    │  [●][●][●]     │                           │
│  └────────────────┘    └────────────────┘                           │
│                                                                      │
│  ● = Running, ◐ = Starting                                          │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 실습 4: 롤링 업데이트 실행

```bash
# 현재 이미지 확인
kubectl describe deployment nginx-deployment | grep Image

# 이미지 업데이트 (롤링 업데이트 시작)
kubectl set image deployment/nginx-deployment nginx=nginx:1.25

# 롤아웃 상태 확인 (실시간)
kubectl rollout status deployment/nginx-deployment

# 예상 출력:
# Waiting for deployment "nginx-deployment" rollout to finish: 1 out of 3 new replicas have been updated...
# Waiting for deployment "nginx-deployment" rollout to finish: 2 out of 3 new replicas have been updated...
# deployment "nginx-deployment" successfully rolled out

# ReplicaSet 확인 (새 RS 생성됨)
kubectl get replicaset -l app=nginx

# 예상 출력:
# NAME                          DESIRED   CURRENT   READY   AGE
# nginx-deployment-abc123       0         0         0       5m    # 이전 버전
# nginx-deployment-xyz789       3         3         3       30s   # 새 버전

# Pod 확인
kubectl get pods -l app=nginx -o wide

# 업데이트 히스토리
kubectl rollout history deployment/nginx-deployment

# 예상 출력:
# REVISION  CHANGE-CAUSE
# 1         <none>
# 2         <none>
```

### 실습 5: 롤백

```bash
# 이전 버전으로 롤백
kubectl rollout undo deployment/nginx-deployment

# 롤백 상태 확인
kubectl rollout status deployment/nginx-deployment

# 이미지 버전 확인
kubectl describe deployment nginx-deployment | grep Image
# 출력: nginx:1.24

# 특정 리비전으로 롤백
kubectl rollout history deployment/nginx-deployment
kubectl rollout undo deployment/nginx-deployment --to-revision=2

# 리비전 상세 확인
kubectl rollout history deployment/nginx-deployment --revision=2
```

### 실습 6: 변경 원인 기록 (Annotation)

```bash
# 변경 원인 기록하면서 업데이트
kubectl set image deployment/nginx-deployment nginx=nginx:1.25 --record
# 또는
kubectl annotate deployment nginx-deployment kubernetes.io/change-cause="Update to nginx 1.25 for security patch"

# 히스토리 확인
kubectl rollout history deployment/nginx-deployment

# 예상 출력:
# REVISION  CHANGE-CAUSE
# 1         <none>
# 2         Update to nginx 1.25 for security patch
```

---

## 🛠️ Part 4: 배포 전략 상세 (45분)

### 4.1 배포 전략 옵션

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
spec:
  replicas: 4
  strategy:
    type: RollingUpdate      # 또는 Recreate
    rollingUpdate:
      maxSurge: 25%          # 최대 추가 Pod (기본: 25%)
      maxUnavailable: 25%    # 최대 불가용 Pod (기본: 25%)
  # ...
```

### 4.2 전략 비교

| 전략 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **RollingUpdate** | 점진적 교체 | 무중단, 리소스 효율 | 두 버전 공존 |
| **Recreate** | 전체 삭제 후 재생성 | 단순, 버전 혼재 없음 | 다운타임 발생 |

### 4.3 maxSurge와 maxUnavailable

```yaml
# 예시: replicas=4, maxSurge=1, maxUnavailable=1
spec:
  replicas: 4
  strategy:
    rollingUpdate:
      maxSurge: 1          # 최대 5개까지 가능 (4+1)
      maxUnavailable: 1    # 최소 3개 유지 (4-1)
```

```
┌─────────────────────────────────────────────────────────────────────┐
│  maxSurge=1, maxUnavailable=1 예시 (replicas=4)                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Step 1: 새 Pod 1개 추가 (총 5개)                                   │
│  [●v1][●v1][●v1][●v1][◐v2]                                         │
│                                                                      │
│  Step 2: v2 Ready, 구 Pod 1개 제거 (총 4개)                         │
│  [●v1][●v1][●v1][●v2]                                               │
│                                                                      │
│  Step 3: 새 Pod 1개 추가 (총 5개)                                   │
│  [●v1][●v1][●v1][●v2][◐v2]                                         │
│                                                                      │
│  Step 4-7: 반복...                                                  │
│                                                                      │
│  Final: 모든 Pod가 v2                                               │
│  [●v2][●v2][●v2][●v2]                                               │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 실습 7: 배포 전략 테스트

```bash
# 전략 설정된 Deployment
cat << 'EOF' > deployment-strategy.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: strategy-test
spec:
  replicas: 4
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0      # 무중단 보장!
  selector:
    matchLabels:
      app: strategy-test
  template:
    metadata:
      labels:
        app: strategy-test
    spec:
      containers:
      - name: nginx
        image: nginx:1.24
        ports:
        - containerPort: 80
EOF

kubectl apply -f deployment-strategy.yaml

# 업데이트 과정 관찰 (터미널 1)
kubectl get pods -l app=strategy-test -w

# 업데이트 실행 (터미널 2)
kubectl set image deployment/strategy-test nginx=nginx:1.25

# maxUnavailable=0이므로 기존 Pod 종료 전 새 Pod Ready 보장
```

---

## 🛠️ Part 5: 자동 복구 및 고급 기능 (30분)

### 실습 8: 자동 복구 테스트

```bash
# Pod 목록 확인
kubectl get pods -l app=nginx-deployment -o name

# Pod 하나 강제 삭제
POD_NAME=$(kubectl get pods -l app=nginx -o jsonpath='{.items[0].metadata.name}')
kubectl delete pod $POD_NAME

# 즉시 새 Pod 생성됨 확인
kubectl get pods -l app=nginx -w

# Deployment 상태 확인 (READY 유지)
kubectl get deployment nginx-deployment
```

### 실습 9: minReadySeconds 설정

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
spec:
  minReadySeconds: 10    # Pod가 Ready 후 10초 대기
  progressDeadlineSeconds: 600  # 10분 내 완료 못하면 실패
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    # ...
```

```bash
# minReadySeconds 설정
kubectl patch deployment nginx-deployment -p '{"spec":{"minReadySeconds":10}}'

# 업데이트 시 각 Pod 준비 후 10초 대기
kubectl set image deployment/nginx-deployment nginx=nginx:1.26
kubectl rollout status deployment/nginx-deployment
```

### 실습 10: 배포 일시 정지/재개

```bash
# 배포 일시 정지
kubectl rollout pause deployment/nginx-deployment

# 여러 변경 적용 (정지 중에는 롤아웃 안 됨)
kubectl set image deployment/nginx-deployment nginx=nginx:1.27
kubectl set resources deployment/nginx-deployment -c=nginx --limits=cpu=200m,memory=256Mi

# 배포 재개 (모든 변경 한 번에 적용)
kubectl rollout resume deployment/nginx-deployment

# 상태 확인
kubectl rollout status deployment/nginx-deployment
```

---

## 📊 Part 6: Deployment 모니터링

### 6.1 상태 확인 명령어 모음

```bash
# 기본 상태
kubectl get deployment
kubectl get deployment -o wide

# 상세 정보
kubectl describe deployment nginx-deployment

# Pod 상태
kubectl get pods -l app=nginx -o wide
kubectl get pods -l app=nginx --show-labels

# ReplicaSet 상태
kubectl get replicaset -l app=nginx

# 롤아웃 상태
kubectl rollout status deployment/nginx-deployment

# 이벤트 확인
kubectl get events --field-selector involvedObject.name=nginx-deployment
```

### 6.2 YAML 출력 및 비교

```bash
# 현재 설정 확인
kubectl get deployment nginx-deployment -o yaml

# 특정 필드만 확인
kubectl get deployment nginx-deployment -o jsonpath='{.spec.template.spec.containers[0].image}'

# 두 리비전 비교
kubectl rollout history deployment/nginx-deployment --revision=1
kubectl rollout history deployment/nginx-deployment --revision=2
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | Deployment YAML 작성 | 구조 이해, spec 작성 | ☐ |
| 2 | 스케일 업/다운 | kubectl scale 사용 | ☐ |
| 3 | 롤링 업데이트 | set image, rollout status | ☐ |
| 4 | 롤백 | rollout undo, --to-revision | ☐ |
| 5 | 자동 복구 테스트 | Pod 삭제 후 자동 생성 | ☐ |
| 6 | 배포 전략 이해 | maxSurge, maxUnavailable | ☐ |
| 7 | 배포 일시정지/재개 | pause, resume | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# Deployment 관리
kubectl create deployment nginx --image=nginx
kubectl scale deployment <name> --replicas=5
kubectl set image deployment/<name> <container>=<image>
kubectl rollout status deployment/<name>
kubectl rollout history deployment/<name>
kubectl rollout undo deployment/<name>
kubectl rollout undo deployment/<name> --to-revision=N
kubectl rollout pause deployment/<name>
kubectl rollout resume deployment/<name>

# 상태 확인
kubectl get deployment
kubectl describe deployment <name>
kubectl get replicaset -l app=<label>
kubectl get pods -l app=<label>
```

---

## 💡 면접 대비 핵심 포인트

### Q1: Deployment와 ReplicaSet의 관계는?

**A**: "Deployment는 ReplicaSet을 관리하는 상위 리소스입니다. Deployment가 배포 전략과 롤백 히스토리를 관리하고, ReplicaSet이 실제 Pod 복제 수를 유지합니다. 이미지 업데이트 시 새 ReplicaSet을 생성하고 점진적으로 전환합니다."

### Q2: 롤링 업데이트 중 문제가 발생하면?

**A**: "progressDeadlineSeconds 내에 완료되지 않으면 자동으로 실패 상태가 됩니다. kubectl rollout undo로 이전 버전으로 즉시 롤백할 수 있습니다. 롤백 시 이전 ReplicaSet이 그대로 남아있으므로 빠르게 복구됩니다."

### Q3: maxSurge와 maxUnavailable의 차이는?

**A**: 
- **maxSurge**: 원하는 replicas보다 추가로 생성할 수 있는 최대 Pod 수
- **maxUnavailable**: 업데이트 중 불가용 상태로 둘 수 있는 최대 Pod 수
- 둘 다 0이면 업데이트 불가, 무중단 배포를 위해 maxUnavailable=0 설정

---

## 📝 학습 기록

```
학습일: ____년 __월 __일
실제 소요 시간: ____시간

실습 완료 항목:
- [ ] Deployment 생성
- [ ] 스케일링
- [ ] 롤링 업데이트
- [ ] 롤백
- [ ] 배포 전략 테스트

이해가 어려웠던 부분:


추가 학습 필요 항목:

```

---

## ➡️ 다음 학습: Day 34

**주제**: Service 학습
- ClusterIP, NodePort, LoadBalancer
- 서비스 디스커버리
- 엔드포인트
