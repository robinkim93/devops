# 📅 Day 35: Namespace - 리소스 격리와 멀티테넌시

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "여러 Kubernetes 클러스터 운영/최적화"

Namespace를 활용한 리소스 격리와 멀티테넌시 전략을 마스터합니다.

---

## ⏰ 예상 학습 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 | 45분 | Namespace 아키텍처 이해 |
| 실습 | 1.5시간 | Namespace 관리 실습 |
| 심화 | 45분 | 멀티테넌시 전략 |

---

## 📚 Part 1: Namespace 개념 (45분)

### Namespace란?

```
┌─────────────────────────────────────────────────────────────┐
│  Namespace = 클러스터 내 가상 클러스터                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  하나의 물리 클러스터                                       │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                                                     │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐           │   │
│  │  │   dev    │ │ staging  │ │   prod   │           │   │
│  │  │          │ │          │ │          │           │   │
│  │  │ Pod Pod  │ │ Pod Pod  │ │ Pod Pod  │           │   │
│  │  │ Svc Cfg  │ │ Svc Cfg  │ │ Svc Cfg  │           │   │
│  │  └──────────┘ └──────────┘ └──────────┘           │   │
│  │                                                     │   │
│  │  ✓ 리소스 격리                                     │   │
│  │  ✓ 팀/환경별 분리                                  │   │
│  │  ✓ 권한 제어 (RBAC)                                │   │
│  │  ✓ 리소스 할당량 (ResourceQuota)                   │   │
│  │                                                     │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 기본 Namespace

| Namespace | 용도 | 설명 |
|-----------|------|------|
| **default** | 기본 네임스페이스 | 명시하지 않으면 여기에 생성 |
| **kube-system** | 시스템 컴포넌트 | CoreDNS, kube-proxy 등 |
| **kube-public** | 공개 리소스 | 모든 사용자 접근 가능 |
| **kube-node-lease** | 노드 헬스체크 | 노드 heartbeat용 |

### Namespace의 범위

```
┌─────────────────────────────────────────────────────────────┐
│  Namespaced vs Cluster-scoped Resources                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Namespaced (네임스페이스 범위)                             │
│  ┌─────────────────────────────────────────────────┐       │
│  │ Pod, Deployment, Service, ConfigMap, Secret     │       │
│  │ PersistentVolumeClaim, Ingress, Role            │       │
│  │ → 네임스페이스 내에서만 유효                    │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  Cluster-scoped (클러스터 범위)                             │
│  ┌─────────────────────────────────────────────────┐       │
│  │ Node, PersistentVolume, Namespace               │       │
│  │ ClusterRole, ClusterRoleBinding, StorageClass   │       │
│  │ → 클러스터 전체에서 유효                        │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

```bash
# Namespaced 리소스 확인
kubectl api-resources --namespaced=true

# Cluster-scoped 리소스 확인
kubectl api-resources --namespaced=false
```

---

## 🛠️ Part 2: 실습 (1.5시간)

### 실습 1: Namespace 기본 관리

```bash
# 현재 Namespace 목록
kubectl get namespaces
kubectl get ns  # 축약형

# Namespace 상세 정보
kubectl describe namespace default

# 명령형으로 Namespace 생성
kubectl create namespace dev
kubectl create namespace staging
kubectl create namespace prod

# 확인
kubectl get ns
```

### 실습 2: 선언형 Namespace 생성

```bash
# YAML로 Namespace 정의
kubectl apply -f - <<EOF
apiVersion: v1
kind: Namespace
metadata:
  name: toss-payment
  labels:
    environment: production
    team: payment
    cost-center: payment-team
  annotations:
    owner: "payment-team@toss.im"
    description: "결제 서비스 네임스페이스"
EOF

# 라벨로 필터링
kubectl get ns -l environment=production
kubectl get ns -l team=payment
```

### 실습 3: Namespace에 리소스 배포

```bash
# dev 네임스페이스에 배포
kubectl create deployment nginx --image=nginx:1.24 -n dev
kubectl expose deployment nginx --port=80 -n dev

# staging 네임스페이스에 동일 이름으로 배포 가능!
kubectl create deployment nginx --image=nginx:1.24 -n staging
kubectl expose deployment nginx --port=80 -n staging

# 확인 - 각 네임스페이스에 독립적으로 존재
kubectl get pods -n dev
kubectl get pods -n staging

# 모든 네임스페이스의 Pod 조회
kubectl get pods --all-namespaces
kubectl get pods -A  # 축약형
```

### 실습 4: Namespace 전환

```bash
# 현재 컨텍스트 확인
kubectl config current-context
kubectl config view --minify

# 기본 Namespace 변경
kubectl config set-context --current --namespace=dev

# 확인 - 이제 -n dev 없이도 dev 네임스페이스 사용
kubectl get pods

# 다른 네임스페이스 접근
kubectl get pods -n staging

# default로 복귀
kubectl config set-context --current --namespace=default
```

### 실습 5: kubens 도구 활용 (권장)

```bash
# kubens 설치 (macOS)
brew install kubectx

# 또는 krew로 설치
kubectl krew install ctx
kubectl krew install ns

# Namespace 목록 및 전환
kubens           # 목록 보기
kubens dev       # dev로 전환
kubens -         # 이전 네임스페이스로 복귀

# 현재 네임스페이스 확인
kubens -c
```

### 실습 6: 네임스페이스 간 통신

```bash
# 동일 네임스페이스 내 통신
kubectl run client -n dev --image=busybox --rm -it -- \
  wget -qO- http://nginx

# 다른 네임스페이스 서비스 접근 (FQDN 사용)
kubectl run client -n staging --image=busybox --rm -it -- \
  wget -qO- http://nginx.dev.svc.cluster.local

# DNS 형식
# <service-name>.<namespace>.svc.cluster.local
```

---

## 📚 Part 3: 멀티테넌시 전략 (45분)

### 토스플레이스 Namespace 전략

```
┌─────────────────────────────────────────────────────────────┐
│  토스플레이스 Namespace 구조                                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  환경별 분리                                                │
│  ┌─────────────────────────────────────────────────┐       │
│  │ dev-payment          # 개발 환경               │       │
│  │ staging-payment      # 스테이징 환경           │       │
│  │ prod-payment         # 운영 환경               │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  팀별 분리                                                  │
│  ┌─────────────────────────────────────────────────┐       │
│  │ team-payment         # 결제팀                  │       │
│  │ team-pos             # POS팀                   │       │
│  │ team-data            # 데이터팀                │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  기능별 분리                                                │
│  ┌─────────────────────────────────────────────────┐       │
│  │ monitoring           # Prometheus, Grafana     │       │
│  │ logging              # Elasticsearch, Fluentd  │       │
│  │ istio-system         # Service Mesh            │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### ResourceQuota로 리소스 제한

```bash
# 네임스페이스 리소스 할당량 설정
kubectl apply -f - <<EOF
apiVersion: v1
kind: ResourceQuota
metadata:
  name: dev-quota
  namespace: dev
spec:
  hard:
    # 컴퓨트 리소스
    requests.cpu: "4"
    requests.memory: "8Gi"
    limits.cpu: "8"
    limits.memory: "16Gi"
    
    # 오브젝트 수
    pods: "20"
    services: "10"
    secrets: "20"
    configmaps: "20"
    persistentvolumeclaims: "10"
    
    # 특수 제한
    count/deployments.apps: "10"
    count/replicasets.apps: "20"
EOF

# 할당량 확인
kubectl describe resourcequota dev-quota -n dev

# 할당량 사용량
kubectl get resourcequota -n dev
```

### LimitRange로 기본값 설정

```bash
kubectl apply -f - <<EOF
apiVersion: v1
kind: LimitRange
metadata:
  name: dev-limits
  namespace: dev
spec:
  limits:
  - type: Container
    default:
      cpu: "200m"
      memory: "256Mi"
    defaultRequest:
      cpu: "100m"
      memory: "128Mi"
    max:
      cpu: "2"
      memory: "2Gi"
    min:
      cpu: "50m"
      memory: "64Mi"
  - type: Pod
    max:
      cpu: "4"
      memory: "4Gi"
EOF

kubectl describe limitrange dev-limits -n dev
```

### RBAC으로 네임스페이스 접근 제어

```bash
# 네임스페이스 관리자 Role
kubectl apply -f - <<EOF
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: namespace-admin
  namespace: dev
rules:
- apiGroups: ["", "apps", "networking.k8s.io"]
  resources: ["*"]
  verbs: ["*"]
EOF

# RoleBinding
kubectl apply -f - <<EOF
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: dev-admin-binding
  namespace: dev
subjects:
- kind: User
  name: developer@toss.im
  apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: Role
  name: namespace-admin
  apiGroup: rbac.authorization.k8s.io
EOF
```

### NetworkPolicy로 네임스페이스 격리

```bash
# 기본 거부 정책
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-all
  namespace: prod
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
EOF

# 같은 네임스페이스 내 통신만 허용
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-same-namespace
  namespace: prod
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  ingress:
  - from:
    - podSelector: {}
EOF
```

---

## 📊 Namespace 관리 베스트 프랙티스

```
┌─────────────────────────────────────────────────────────────┐
│  Namespace 베스트 프랙티스                                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 명명 규칙 통일                                          │
│     - 환경-팀-서비스: prod-payment-api                      │
│     - 팀-서비스: payment-api                                │
│                                                             │
│  2. 필수 라벨 적용                                          │
│     - environment: dev/staging/prod                         │
│     - team: payment/pos/data                                │
│     - cost-center: 비용 추적용                              │
│                                                             │
│  3. 리소스 제한 필수 적용                                   │
│     - ResourceQuota: 총량 제한                              │
│     - LimitRange: 기본값 설정                               │
│                                                             │
│  4. 접근 제어                                               │
│     - RBAC: 팀별 권한 분리                                  │
│     - NetworkPolicy: 네트워크 격리                          │
│                                                             │
│  5. default 네임스페이스 사용 금지                          │
│     - 명시적 네임스페이스 지정                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | Namespace 생성 (명령형/선언형) | ☐ |
| 2 | 특정 Namespace에 리소스 배포 | ☐ |
| 3 | 기본 Namespace 전환 | ☐ |
| 4 | 네임스페이스 간 DNS 통신 이해 | ☐ |
| 5 | ResourceQuota 설정 | ☐ |
| 6 | LimitRange 설정 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# Namespace 관리
kubectl get namespaces
kubectl create namespace <name>
kubectl describe namespace <name>
kubectl delete namespace <name>

# 네임스페이스 지정
kubectl get pods -n <namespace>
kubectl get all -A  # 모든 네임스페이스

# 기본 네임스페이스 변경
kubectl config set-context --current --namespace=<name>

# 리소스 제한
kubectl describe resourcequota -n <namespace>
kubectl describe limitrange -n <namespace>
```

---

## 📝 면접 대비 질문

### Q1: Namespace의 용도를 설명해주세요.
> "Namespace는 클러스터 내 가상 클러스터로, 리소스 격리, 팀/환경별 분리, RBAC 권한 제어, ResourceQuota 리소스 할당에 사용됩니다. 토스플레이스에서는 dev/staging/prod 환경 분리와 팀별 리소스 할당에 활용합니다."

### Q2: 다른 Namespace의 Service에 접근하는 방법은?
> "FQDN을 사용합니다. 형식은 `<service-name>.<namespace>.svc.cluster.local`입니다. 예를 들어 dev 네임스페이스의 payment 서비스는 `payment.dev.svc.cluster.local`로 접근합니다."

---

## ➡️ 다음 학습: Day 36

**주제**: kubectl 고급 명령어
