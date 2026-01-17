# 📅 Day 48: RBAC (Role-Based Access Control)

## 🎯 오늘의 목표

> **토스플레이스 핵심**: 최소 권한 원칙에 따른 Kubernetes 리소스 접근 제어를 구현합니다.

---

## ⏰ 예상 학습 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 이해 | 1시간 | RBAC 구성요소 |
| Role/RoleBinding | 1시간 | 네임스페이스 범위 |
| ClusterRole | 1시간 | 클러스터 범위, 실전 패턴 |

---

## 📚 Part 1: RBAC 개념

### 핵심 구성요소

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           RBAC 구성요소                                       │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌──────────────────┐                  ┌──────────────────┐                │
│   │     Subject      │                  │  Role/ClusterRole │                │
│   │  (누가?)         │◀──── Binding ───▶│  (무엇을 할 수    │                │
│   │  - User          │                  │   있는가?)        │                │
│   │  - Group         │                  │  - apiGroups     │                │
│   │  - ServiceAccount│                  │  - resources     │                │
│   └──────────────────┘                  │  - verbs         │                │
│                                         └──────────────────┘                │
│                                                                              │
│   Namespace 범위: Role + RoleBinding                                         │
│   Cluster 범위: ClusterRole + ClusterRoleBinding                             │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 리소스 종류

| 리소스 | 범위 | 설명 |
|--------|------|------|
| **Role** | Namespace | 네임스페이스 내 권한 정의 |
| **ClusterRole** | Cluster | 클러스터 전체 권한 정의 |
| **RoleBinding** | Namespace | Role을 Subject에 연결 |
| **ClusterRoleBinding** | Cluster | ClusterRole을 Subject에 연결 |

### Verbs (동작)

| Verb | 설명 | HTTP 메서드 |
|------|------|------------|
| `get` | 단일 리소스 조회 | GET |
| `list` | 리소스 목록 조회 | GET |
| `watch` | 변경 감시 | GET (watch) |
| `create` | 생성 | POST |
| `update` | 전체 수정 | PUT |
| `patch` | 부분 수정 | PATCH |
| `delete` | 삭제 | DELETE |
| `deletecollection` | 일괄 삭제 | DELETE |

---

## 🛠️ Part 2: Role과 RoleBinding

### Role 생성

```yaml
# pod-reader-role.yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: default
  name: pod-reader
rules:
- apiGroups: [""]           # "" = core API group
  resources: ["pods"]
  verbs: ["get", "list", "watch"]
- apiGroups: [""]
  resources: ["pods/log"]   # 하위 리소스
  verbs: ["get"]
```

```bash
# 명령어로 생성
kubectl create role pod-reader \
  --verb=get,list,watch \
  --resource=pods \
  -n default
```

### RoleBinding 생성

```yaml
# pod-reader-binding.yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: read-pods
  namespace: default
subjects:
- kind: ServiceAccount
  name: myapp-sa
  namespace: default
roleRef:
  kind: Role
  name: pod-reader
  apiGroup: rbac.authorization.k8s.io
```

```bash
# 명령어로 생성
kubectl create rolebinding read-pods \
  --role=pod-reader \
  --serviceaccount=default:myapp-sa \
  -n default
```

### 전체 예제

```bash
# 1. ServiceAccount 생성
kubectl create serviceaccount myapp-sa -n default

# 2. Role 생성
kubectl apply -f - <<EOF
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: default
  name: pod-manager
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list", "watch", "create", "delete"]
- apiGroups: [""]
  resources: ["pods/log"]
  verbs: ["get"]
- apiGroups: [""]
  resources: ["configmaps", "secrets"]
  verbs: ["get", "list"]
- apiGroups: ["apps"]
  resources: ["deployments"]
  verbs: ["get", "list", "watch"]
EOF

# 3. RoleBinding 생성
kubectl apply -f - <<EOF
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: myapp-pod-manager
  namespace: default
subjects:
- kind: ServiceAccount
  name: myapp-sa
  namespace: default
roleRef:
  kind: Role
  name: pod-manager
  apiGroup: rbac.authorization.k8s.io
EOF
```

---

## 🛠️ Part 3: ClusterRole과 ClusterRoleBinding

### ClusterRole 생성

```yaml
# cluster-viewer.yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: cluster-viewer
rules:
- apiGroups: [""]
  resources: ["nodes", "namespaces", "persistentvolumes"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["storage.k8s.io"]
  resources: ["storageclasses"]
  verbs: ["get", "list", "watch"]
```

```bash
# 명령어로 생성
kubectl create clusterrole cluster-viewer \
  --verb=get,list,watch \
  --resource=nodes,namespaces,persistentvolumes
```

### ClusterRoleBinding 생성

```yaml
# cluster-viewer-binding.yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: cluster-view-binding
subjects:
- kind: ServiceAccount
  name: monitoring-sa
  namespace: monitoring
roleRef:
  kind: ClusterRole
  name: cluster-viewer
  apiGroup: rbac.authorization.k8s.io
```

### ClusterRole + RoleBinding (패턴)

ClusterRole을 RoleBinding으로 연결하면 특정 네임스페이스에서만 권한 적용:

```yaml
# ClusterRole을 특정 네임스페이스에서만 사용
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: dev-admin
  namespace: development  # 이 네임스페이스에서만
subjects:
- kind: Group
  name: dev-team
  apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: ClusterRole      # ClusterRole 참조
  name: admin            # 기본 제공 ClusterRole
  apiGroup: rbac.authorization.k8s.io
```

---

## 🛠️ Part 4: 기본 제공 ClusterRole

### 주요 기본 ClusterRole

| ClusterRole | 설명 |
|-------------|------|
| `cluster-admin` | 모든 권한 (슈퍼유저) |
| `admin` | 네임스페이스 관리자 (RBAC 제외) |
| `edit` | 읽기/쓰기 (RBAC, Secrets 제외) |
| `view` | 읽기 전용 |

```bash
# 기본 ClusterRole 확인
kubectl get clusterroles | grep -E "^(cluster-admin|admin|edit|view)"

# 상세 확인
kubectl describe clusterrole view
```

### 기본 ClusterRole 활용

```bash
# 특정 네임스페이스에 view 권한 부여
kubectl create rolebinding myapp-view \
  --clusterrole=view \
  --serviceaccount=default:myapp-sa \
  -n default
```

---

## 🛠️ Part 5: 권한 테스트

### auth can-i

```bash
# 현재 사용자 권한 확인
kubectl auth can-i create pods
kubectl auth can-i get secrets
kubectl auth can-i --list

# 특정 ServiceAccount로 확인
kubectl auth can-i get pods --as=system:serviceaccount:default:myapp-sa
kubectl auth can-i create deployments --as=system:serviceaccount:default:myapp-sa
kubectl auth can-i --list --as=system:serviceaccount:default:myapp-sa

# 특정 네임스페이스에서
kubectl auth can-i get pods -n production --as=system:serviceaccount:default:myapp-sa
```

### 실제 테스트

```bash
# ServiceAccount로 Pod 실행
kubectl run rbac-test \
  --image=bitnami/kubectl \
  --serviceaccount=myapp-sa \
  --restart=Never \
  -- sleep 3600

# Pod에서 권한 테스트
kubectl exec -it rbac-test -- kubectl get pods
# 성공 (권한 있음)

kubectl exec -it rbac-test -- kubectl get secrets
# Error: secrets is forbidden (권한 없음)

kubectl exec -it rbac-test -- kubectl delete deployment nginx
# Error: deployments.apps is forbidden (권한 없음)

# 정리
kubectl delete pod rbac-test
```

---

## 📋 실전 RBAC 패턴

### 패턴 1: 개발자용 (특정 네임스페이스)

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: developer
  namespace: development
rules:
- apiGroups: ["", "apps", "batch"]
  resources: ["pods", "deployments", "services", "configmaps", "jobs"]
  verbs: ["get", "list", "watch", "create", "update", "delete"]
- apiGroups: [""]
  resources: ["pods/log", "pods/exec"]
  verbs: ["get", "create"]
```

### 패턴 2: 모니터링용 (클러스터 전체 읽기)

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: monitoring-reader
rules:
- apiGroups: [""]
  resources: ["pods", "nodes", "services", "endpoints"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["apps"]
  resources: ["deployments", "replicasets", "statefulsets"]
  verbs: ["get", "list", "watch"]
- apiGroups: [""]
  resources: ["pods/log"]
  verbs: ["get"]
```

### 패턴 3: CI/CD용 (배포 권한)

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: deployer
  namespace: production
rules:
- apiGroups: ["apps"]
  resources: ["deployments", "replicasets"]
  verbs: ["get", "list", "watch", "update", "patch"]
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list", "watch", "delete"]
- apiGroups: ["apps"]
  resources: ["deployments/scale"]
  verbs: ["update", "patch"]
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 명령어/리소스 | 완료 |
|---|------|-------------|------|
| 1 | ServiceAccount 생성 | `kubectl create sa` | ☐ |
| 2 | Role 생성 | `kubectl create role` | ☐ |
| 3 | RoleBinding 생성 | `kubectl create rolebinding` | ☐ |
| 4 | ClusterRole 이해 | `kubectl get clusterroles` | ☐ |
| 5 | 권한 테스트 | `kubectl auth can-i` | ☐ |
| 6 | SA로 Pod 실행 | `--serviceaccount=` | ☐ |
| 7 | 권한 검증 | Pod 내에서 kubectl 실행 | ☐ |

---

## 🔑 핵심 명령어 요약

```bash
# ServiceAccount
kubectl create serviceaccount myapp-sa

# Role/RoleBinding
kubectl create role pod-reader --verb=get,list --resource=pods
kubectl create rolebinding read-pods --role=pod-reader --serviceaccount=default:myapp-sa

# ClusterRole/ClusterRoleBinding
kubectl create clusterrole cluster-viewer --verb=get,list --resource=nodes,namespaces
kubectl create clusterrolebinding view-all --clusterrole=cluster-viewer --serviceaccount=default:myapp-sa

# 권한 확인
kubectl auth can-i get pods --as=system:serviceaccount:default:myapp-sa
kubectl auth can-i --list --as=system:serviceaccount:default:myapp-sa
```

---

## ➡️ 다음 학습: Day 49

**주제**: Pod Security (SecurityContext)

