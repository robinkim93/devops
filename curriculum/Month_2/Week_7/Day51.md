# 📅 Day 51: Week 7 복습 - 운영 역량 종합

## 🎯 오늘의 목표

> **토스플레이스 핵심**: HPA, 로깅, 디버깅, RBAC, 보안을 통합적으로 이해하고 실전에 적용합니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| HPA 복습 | 1시간 | 스케일링 테스트 |
| 로깅/디버깅 | 1시간 | 문제 분석 |
| RBAC/보안 | 1시간 | 권한 설정 |
| 통합 실습 | 1시간 | 시나리오 연습 |

---

## 📋 Week 7 학습 요약

| Day | 주제 | 핵심 내용 | 토스플레이스 연결 |
|-----|------|----------|-----------------|
| 45 | HPA | 자동 스케일링 | 대규모 트래픽 대응 |
| 46 | 로깅 | kubectl logs | 장애 원인 분석 |
| 47 | 디버깅 | describe, events | 문제 해결 |
| 48 | RBAC | Role, RoleBinding | 접근 권한 관리 |
| 49 | Pod Security | SecurityContext | 컨테이너 보안 |
| 50 | NetworkPolicy | 네트워크 격리 | 서비스 분리 |

---

## 🔑 Part 1: HPA (Horizontal Pod Autoscaler)

### 핵심 개념

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     HPA 동작 원리                                            │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌─────────────┐    메트릭 수집     ┌────────────────┐                     │
│   │ metrics-    │ ◀──────────────── │ Pod (CPU/Mem)  │                     │
│   │ server      │                   └────────────────┘                     │
│   └──────┬──────┘                                                          │
│          │                                                                  │
│          ▼ 제공                                                             │
│   ┌─────────────┐    replicas 조정   ┌────────────────┐                     │
│   │ HPA         │ ─────────────────▶ │ Deployment     │                     │
│   │ Controller  │                   └────────────────┘                     │
│   └─────────────┘                                                          │
│                                                                              │
│   공식: desiredReplicas = ceil(current * (currentMetric / desiredMetric))   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 주요 명령어

```bash
# HPA 생성
kubectl autoscale deployment myapp --cpu-percent=50 --min=2 --max=10

# HPA 상태 확인
kubectl get hpa
kubectl describe hpa myapp

# YAML로 HPA 생성
cat <<EOF | kubectl apply -f -
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: myapp-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: myapp
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 50
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 70
EOF

# 부하 테스트
kubectl run load-test --image=busybox --rm -it --restart=Never -- \
  /bin/sh -c "while true; do wget -q -O- http://myapp; done"
```

---

## 🔑 Part 2: 로깅 & 디버깅

### 로그 분석 명령어

```bash
# 기본 로그
kubectl logs <pod-name>

# 실시간 로그 follow
kubectl logs -f <pod-name> --tail=100

# 특정 시간 이후 로그
kubectl logs --since=30m <pod-name>

# 이전 컨테이너 로그 (재시작된 경우)
kubectl logs <pod-name> --previous

# 멀티 컨테이너 Pod
kubectl logs <pod-name> -c <container-name>

# 라벨 기반 여러 Pod 로그
kubectl logs -l app=myapp --all-containers=true

# stern (멀티 Pod 로그)
stern myapp -n default
```

### 디버깅 명령어

```bash
# Pod 상세 정보
kubectl describe pod <pod-name>

# 이벤트 확인 (최신순)
kubectl get events --sort-by='.lastTimestamp'
kubectl get events --field-selector involvedObject.name=<pod-name>

# 컨테이너 접속
kubectl exec -it <pod-name> -- /bin/sh

# 디버그 컨테이너 (K8s 1.25+)
kubectl debug <pod-name> -it --image=busybox

# Pod 리소스 사용량
kubectl top pod
kubectl top pod <pod-name> --containers
```

### 트러블슈팅 체크리스트

| 증상 | 확인 명령어 | 원인 |
|------|-----------|------|
| Pending | `kubectl describe pod` | 리소스 부족, nodeSelector |
| CrashLoopBackOff | `kubectl logs --previous` | 앱 에러, 설정 오류 |
| ImagePullBackOff | `kubectl describe pod` | 이미지명 오류, 권한 |
| OOMKilled | `kubectl describe pod` | 메모리 limit 부족 |
| Evicted | `kubectl describe node` | 노드 리소스 부족 |

---

## 🔑 Part 3: RBAC (Role-Based Access Control)

### 핵심 개념

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     RBAC 구성요소                                            │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌────────────┐                  ┌────────────────────┐                    │
│   │ Subject    │                  │ Role / ClusterRole │                    │
│   │ (User, SA) │◀─ RoleBinding ─▶│ (verb + resource)  │                    │
│   └────────────┘                  └────────────────────┘                    │
│                                                                              │
│   Namespace 범위: Role + RoleBinding                                         │
│   Cluster 범위: ClusterRole + ClusterRoleBinding                             │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 주요 명령어

```bash
# Role 생성
kubectl create role pod-reader \
  --verb=get,list,watch \
  --resource=pods \
  -n default

# RoleBinding 생성
kubectl create rolebinding read-pods \
  --role=pod-reader \
  --serviceaccount=default:myapp-sa \
  -n default

# ClusterRole 생성
kubectl create clusterrole cluster-viewer \
  --verb=get,list \
  --resource=nodes,namespaces,pods

# ClusterRoleBinding 생성
kubectl create clusterrolebinding view-all \
  --clusterrole=cluster-viewer \
  --serviceaccount=default:myapp-sa

# 권한 확인
kubectl auth can-i get pods --as=system:serviceaccount:default:myapp-sa
kubectl auth can-i create deployments --as=system:serviceaccount:default:myapp-sa
kubectl auth can-i --list --as=system:serviceaccount:default:myapp-sa
```

### ServiceAccount 생성

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: myapp-sa
  namespace: default
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: myapp-role
  namespace: default
rules:
- apiGroups: [""]
  resources: ["pods", "services", "configmaps"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["apps"]
  resources: ["deployments"]
  verbs: ["get", "list", "watch", "update", "patch"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: myapp-rb
  namespace: default
subjects:
- kind: ServiceAccount
  name: myapp-sa
  namespace: default
roleRef:
  kind: Role
  name: myapp-role
  apiGroup: rbac.authorization.k8s.io
```

---

## 🔑 Part 4: Pod Security

### SecurityContext

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: secure-pod
spec:
  securityContext:
    runAsNonRoot: true
    runAsUser: 1000
    runAsGroup: 3000
    fsGroup: 2000
  containers:
  - name: app
    image: nginx
    securityContext:
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: true
      capabilities:
        drop:
        - ALL
    volumeMounts:
    - name: tmp
      mountPath: /tmp
  volumes:
  - name: tmp
    emptyDir: {}
```

### NetworkPolicy

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: api-network-policy
  namespace: default
spec:
  podSelector:
    matchLabels:
      app: api
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: frontend
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - podSelector:
        matchLabels:
          app: database
    ports:
    - protocol: TCP
      port: 5432
```

---

## 🎯 통합 시나리오 실습

### 시나리오: 트래픽 급증 대응

```bash
# 1. HPA 상태 확인
kubectl get hpa
# TARGETS이 높아지고 있는지 확인

# 2. Pod 상태 확인
kubectl get pods -w
# 새 Pod가 생성되는지 확인

# 3. 로그 확인
kubectl logs -l app=myapp --tail=50
# 에러 로그 있는지 확인

# 4. 이벤트 확인
kubectl get events --sort-by='.lastTimestamp' | head -20
# HPA 스케일 이벤트 확인

# 5. 권한 문제 확인
kubectl auth can-i get pods --as=system:serviceaccount:default:myapp-sa
# ServiceAccount 권한 확인
```

---

## ✅ Week 7 체크리스트

| # | 항목 | 명령어 | 완료 |
|---|------|--------|------|
| 1 | HPA 설정 | `kubectl autoscale deployment` | ☐ |
| 2 | HPA 동작 확인 | `kubectl get hpa -w` | ☐ |
| 3 | Pod 로그 분석 | `kubectl logs -f --tail=100` | ☐ |
| 4 | 이벤트 분석 | `kubectl get events` | ☐ |
| 5 | Role 생성 | `kubectl create role` | ☐ |
| 6 | RoleBinding 설정 | `kubectl create rolebinding` | ☐ |
| 7 | 권한 테스트 | `kubectl auth can-i` | ☐ |
| 8 | SecurityContext | `runAsNonRoot, readOnlyRootFilesystem` | ☐ |
| 9 | NetworkPolicy | Ingress/Egress 규칙 | ☐ |

---

## 🎯 토스플레이스 연결점

| 요구사항 | Week 7 학습 | 실무 적용 |
|---------|-------------|----------|
| 대규모 트래픽 대응 | HPA | CPU/Memory 기반 자동 스케일링 |
| 장애 대응 | 로깅/디버깅 | 빠른 원인 분석 및 복구 |
| 보안 컴플라이언스 | RBAC | 최소 권한 원칙 적용 |
| 서비스 격리 | NetworkPolicy | Pod 간 네트워크 접근 제어 |

---

## ➡️ 다음: Week 8 (Day 52-60)

**주제**: Month 2 프로젝트 - K8s 애플리케이션 배포

