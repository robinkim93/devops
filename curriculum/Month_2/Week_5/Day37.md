# 📅 Day 37: Week 5 복습 - Kubernetes 기초 완전 정복

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "Kubernetes 기반의 Cloud Native 플랫폼 운영"

Week 5에서 학습한 Kubernetes 핵심 개념을 복습하고 종합 실습을 진행합니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 복습 | 1시간 | 핵심 개념 정리 |
| 종합 실습 | 2시간 | 전체 워크플로우 실습 |
| 면접 준비 | 1시간 | 예상 질문 대비 |

---

## 📋 Week 5 학습 내용 정리

### 일별 핵심 내용

| Day | 주제 | 핵심 내용 | 중요도 |
|-----|------|----------|--------|
| 31 | K8s 소개 | 아키텍처, Control Plane, Worker Node | ⭐⭐⭐ |
| 32 | Pod | YAML, 멀티컨테이너, 리소스 제한 | ⭐⭐⭐ |
| 33 | Deployment | replicas, 롤링 업데이트, 롤백 | ⭐⭐⭐ |
| 34 | Service | ClusterIP, NodePort, DNS | ⭐⭐⭐ |
| 35 | Namespace | 리소스 격리, 환경 분리 | ⭐⭐ |
| 36 | kubectl | 고급 명령어, 필터링, 디버깅 | ⭐⭐⭐ |

### 아키텍처 복습

```
┌─────────────────────────────────────────────────────────────┐
│  Kubernetes Architecture                                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Control Plane (Master)                                     │
│  ┌─────────────────────────────────────────────────┐       │
│  │ ┌───────────┐ ┌───────────┐ ┌───────────┐      │       │
│  │ │ API Server│ │ Scheduler │ │Controller │      │       │
│  │ │           │ │           │ │ Manager   │      │       │
│  │ └───────────┘ └───────────┘ └───────────┘      │       │
│  │                   ┌───────────┐                 │       │
│  │                   │   etcd    │                 │       │
│  │                   └───────────┘                 │       │
│  └─────────────────────────────────────────────────┘       │
│                          │                                  │
│                          │ kubectl                          │
│                          │                                  │
│  Worker Nodes                                               │
│  ┌─────────────────────────────────────────────────┐       │
│  │ ┌─────────────────────────────────────────────┐│       │
│  │ │ ┌──────┐ ┌──────┐ ┌──────┐               ││       │
│  │ │ │ Pod  │ │ Pod  │ │ Pod  │               ││       │
│  │ │ └──────┘ └──────┘ └──────┘               ││       │
│  │ └─────────────────────────────────────────────┘│       │
│  │ ┌──────────┐ ┌──────────┐ ┌──────────┐        │       │
│  │ │ kubelet  │ │kube-proxy│ │Container │        │       │
│  │ │          │ │          │ │ Runtime  │        │       │
│  │ └──────────┘ └──────────┘ └──────────┘        │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 핵심 리소스 관계

```
┌─────────────────────────────────────────────────────────────┐
│  리소스 관계도                                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Namespace                                                  │
│  └── Deployment                                             │
│      └── ReplicaSet (자동 생성)                             │
│          └── Pod (여러 개)                                  │
│              └── Container(s)                               │
│                                                             │
│  └── Service (Pod 그룹에 대한 네트워크 엔드포인트)          │
│                                                             │
│  요청 흐름:                                                 │
│  Client → Service → Pod(s) → Container                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠️ 종합 실습: 3-Tier 애플리케이션 배포

### 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│  Namespace: week5-app                                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │            Service (NodePort: 30080)                 │  │
│  └─────────────────────────┬────────────────────────────┘  │
│                            │                                │
│                            ▼                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Deployment (3 replicas)                 │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐          │  │
│  │  │   Pod    │  │   Pod    │  │   Pod    │          │  │
│  │  │  nginx   │  │  nginx   │  │  nginx   │          │  │
│  │  └──────────┘  └──────────┘  └──────────┘          │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  리소스 설정:                                               │
│  - CPU: 100m~200m                                          │
│  - Memory: 64Mi~128Mi                                      │
│  - 롤링 업데이트 전략                                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Step 1: 전체 YAML 작성 및 배포

```bash
cat << 'EOF' > week5-app.yaml
---
# Namespace
apiVersion: v1
kind: Namespace
metadata:
  name: week5-app
  labels:
    project: week5-review
---
# Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: webapp
  namespace: week5-app
  labels:
    app: webapp
spec:
  replicas: 3
  selector:
    matchLabels:
      app: webapp
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: webapp
        version: v1
    spec:
      containers:
      - name: nginx
        image: nginx:1.24
        ports:
        - containerPort: 80
        resources:
          requests:
            memory: "64Mi"
            cpu: "100m"
          limits:
            memory: "128Mi"
            cpu: "200m"
        livenessProbe:
          httpGet:
            path: /
            port: 80
          initialDelaySeconds: 5
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /
            port: 80
          initialDelaySeconds: 3
          periodSeconds: 5
---
# Service
apiVersion: v1
kind: Service
metadata:
  name: webapp-svc
  namespace: week5-app
spec:
  selector:
    app: webapp
  ports:
  - port: 80
    targetPort: 80
    nodePort: 30080
  type: NodePort
EOF

# 배포
kubectl apply -f week5-app.yaml

# 확인
kubectl get all -n week5-app
```

### Step 2: 배포 상태 확인

```bash
# Pod 상태 상세 확인
kubectl get pods -n week5-app -o wide

# Deployment 상태
kubectl describe deployment webapp -n week5-app

# Service 엔드포인트 확인
kubectl get endpoints webapp-svc -n week5-app

# 이벤트 확인
kubectl get events -n week5-app --sort-by='.lastTimestamp'
```

### Step 3: 접속 테스트

```bash
# minikube 환경
minikube service webapp-svc -n week5-app --url

# 또는 직접 접근
curl $(minikube ip):30080

# Pod 직접 접근 테스트
kubectl run test --image=busybox --rm -it -n week5-app -- \
  wget -qO- http://webapp-svc
```

### Step 4: 롤링 업데이트 및 롤백

```bash
# 이미지 업데이트
kubectl set image deployment/webapp nginx=nginx:1.25 -n week5-app

# 업데이트 상태 확인
kubectl rollout status deployment/webapp -n week5-app

# 롤아웃 히스토리
kubectl rollout history deployment/webapp -n week5-app

# 이전 버전으로 롤백
kubectl rollout undo deployment/webapp -n week5-app

# 특정 리비전으로 롤백
kubectl rollout undo deployment/webapp --to-revision=1 -n week5-app
```

### Step 5: 스케일링 테스트

```bash
# 수동 스케일 아웃
kubectl scale deployment webapp --replicas=5 -n week5-app

# 확인
kubectl get pods -n week5-app -w

# 스케일 인
kubectl scale deployment webapp --replicas=2 -n week5-app
```

### Step 6: 디버깅 실습

```bash
# Pod 로그 확인
kubectl logs -l app=webapp -n week5-app

# 특정 Pod 진입
kubectl exec -it $(kubectl get pod -l app=webapp -n week5-app -o jsonpath='{.items[0].metadata.name}') -n week5-app -- /bin/bash

# Pod 상세 정보
kubectl describe pod -l app=webapp -n week5-app

# 리소스 사용량 확인
kubectl top pods -n week5-app
```

---

## 📚 kubectl 필수 명령어 정리

```bash
# 리소스 조회
kubectl get pods/deployments/services/all [-n namespace]
kubectl get pods -o wide  # 상세 정보
kubectl get pods -o yaml  # YAML 출력

# 리소스 생성/수정
kubectl apply -f file.yaml
kubectl create -f file.yaml
kubectl edit deployment <name>

# 리소스 삭제
kubectl delete -f file.yaml
kubectl delete pod/deployment/service <name>

# 디버깅
kubectl describe <resource> <name>
kubectl logs <pod> [-f] [-c container]
kubectl exec -it <pod> -- /bin/sh
kubectl port-forward <pod> 8080:80

# 스케일/업데이트
kubectl scale deployment <name> --replicas=N
kubectl set image deployment/<name> container=image:tag
kubectl rollout status/history/undo deployment/<name>

# 필터링
kubectl get pods -l app=webapp
kubectl get pods --field-selector status.phase=Running
```

---

## 📝 면접 예상 질문

### Q1: Pod와 Container의 차이점은?
> "Pod는 Kubernetes의 최소 배포 단위로, 하나 이상의 컨테이너를 포함합니다. 같은 Pod 내 컨테이너들은 네트워크 네임스페이스를 공유하여 localhost로 통신하고, 볼륨도 공유할 수 있습니다. 보통 밀접하게 연관된 프로세스(예: 앱 + 사이드카)를 같은 Pod에 배치합니다."

### Q2: Deployment의 롤링 업데이트를 설명해주세요.
> "Deployment는 새 ReplicaSet을 생성하고 점진적으로 Pod를 교체합니다. maxSurge는 동시에 추가 생성할 Pod 수, maxUnavailable은 동시에 종료할 Pod 수를 제어합니다. 이를 통해 무중단 배포가 가능하며, 문제 발생 시 kubectl rollout undo로 즉시 롤백할 수 있습니다."

### Q3: Service의 ClusterIP와 NodePort 차이점은?
> "ClusterIP는 클러스터 내부에서만 접근 가능한 가상 IP를 제공합니다. NodePort는 모든 노드의 특정 포트(30000-32767)로 외부 접근을 허용합니다. 프로덕션에서는 LoadBalancer 타입이나 Ingress를 사용하여 외부 트래픽을 관리합니다."

### Q4: Namespace를 사용하는 이유는?
> "리소스 격리, 팀/환경별 분리, RBAC 권한 제어, ResourceQuota 리소스 할당에 사용합니다. 토스플레이스에서는 dev/staging/prod 환경을 분리하고, 팀별로 리소스를 할당할 때 활용합니다."

---

## ✅ Week 5 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | K8s 아키텍처 설명 가능 | ☐ |
| 2 | Pod YAML 작성 가능 | ☐ |
| 3 | Deployment 생성 및 롤링 업데이트 가능 | ☐ |
| 4 | Service로 Pod 노출 가능 | ☐ |
| 5 | Namespace로 리소스 격리 가능 | ☐ |
| 6 | kubectl 명령어 숙달 | ☐ |
| 7 | 종합 실습 완료 | ☐ |

---

## 🔧 정리

```bash
# 실습 환경 정리
kubectl delete namespace week5-app
rm week5-app.yaml
```

---

## ➡️ 다음: Week 6 (Day 38-44)

**주제**: ConfigMap, Secret, Volume
- 설정과 민감 정보 분리
- 영속적 스토리지
- 고급 Pod 설정
