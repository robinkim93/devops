# 📅 Day 40: PersistentVolume (PV/PVC) - 데이터 영속성 마스터

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "서비스 안정성을 위한 데이터 영속화"

Pod가 삭제되어도 데이터가 유지되는 영속적 스토리지를 구성합니다.

---

## ⏰ 예상 학습 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 | 45분 | 스토리지 아키텍처 이해 |
| 실습 | 1.5시간 | PV/PVC 생성 및 활용 |
| 심화 | 45분 | StorageClass와 동적 프로비저닝 |

---

## 📚 Part 1: Kubernetes 스토리지 개념 (45분)

### 왜 영속적 스토리지가 필요한가?

```
┌─────────────────────────────────────────────────────────────┐
│  컨테이너 스토리지의 문제점                                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  기본 컨테이너 동작                                         │
│  ┌─────────────────────────────────────────────────┐       │
│  │ Pod 생성 → 데이터 기록 → Pod 삭제               │       │
│  │    📁           💾           💥                  │       │
│  │                                                  │       │
│  │ 결과: 모든 데이터 손실! ❌                       │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  PV/PVC 사용 시                                             │
│  ┌─────────────────────────────────────────────────┐       │
│  │ Pod 생성 → PVC 연결 → 데이터 기록 → Pod 삭제    │       │
│  │    📁        🔗          💾           💥         │       │
│  │                          ↓                       │       │
│  │ 새 Pod 생성 → PVC 연결 → 데이터 유지! ✅        │       │
│  │    📁        🔗          💾                      │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  토스플레이스 활용:                                         │
│  - 데이터베이스 (MySQL, MongoDB)                           │
│  - Redis 데이터 영속화                                     │
│  - 로그 저장                                               │
│  - 사용자 업로드 파일                                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 스토리지 컴포넌트 관계

```
┌─────────────────────────────────────────────────────────────┐
│  Kubernetes Storage Architecture                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────┐                                               │
│  │   Pod    │ ───▶ volumeMounts                            │
│  └────┬─────┘       (컨테이너 내 마운트 경로)               │
│       │                                                     │
│       ▼                                                     │
│  ┌──────────────────────────┐                              │
│  │ PersistentVolumeClaim    │  ← 사용자/개발자가 생성      │
│  │ (PVC)                    │     "500MB 스토리지 필요"    │
│  └────────────┬─────────────┘                              │
│               │ Binding (자동 매칭)                         │
│               ▼                                             │
│  ┌──────────────────────────┐                              │
│  │ PersistentVolume         │  ← 관리자가 생성 또는        │
│  │ (PV)                     │     StorageClass로 자동 생성 │
│  └────────────┬─────────────┘                              │
│               │                                             │
│               ▼                                             │
│  ┌──────────────────────────┐                              │
│  │ 실제 스토리지            │                              │
│  │ - AWS EBS                │                              │
│  │ - GCP Persistent Disk    │                              │
│  │ - NFS                    │                              │
│  │ - Local Path             │                              │
│  └──────────────────────────┘                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Access Modes

| 모드 | 약어 | 설명 | 사용 사례 |
|------|------|------|----------|
| **ReadWriteOnce** | RWO | 단일 노드에서 읽기/쓰기 | 데이터베이스 |
| **ReadOnlyMany** | ROX | 여러 노드에서 읽기 전용 | 정적 콘텐츠 |
| **ReadWriteMany** | RWX | 여러 노드에서 읽기/쓰기 | 공유 파일 시스템 |
| **ReadWriteOncePod** | RWOP | 단일 Pod에서만 읽기/쓰기 | 단일 인스턴스 DB |

### Reclaim Policy

| 정책 | 동작 | 사용 사례 |
|------|------|----------|
| **Retain** | PVC 삭제 후 PV와 데이터 유지 | 중요 데이터 |
| **Delete** | PVC 삭제 시 PV와 데이터도 삭제 | 임시 데이터 |
| **Recycle** | 데이터 삭제 후 재사용 (deprecated) | - |

---

## 🛠️ Part 2: 실습 (1.5시간)

### 실습 1: 정적 PV/PVC 생성

```bash
# PersistentVolume 생성 (관리자 역할)
kubectl apply -f - <<EOF
apiVersion: v1
kind: PersistentVolume
metadata:
  name: my-pv
  labels:
    type: local
spec:
  storageClassName: manual
  capacity:
    storage: 1Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  hostPath:
    path: /tmp/k8s-pv
    type: DirectoryOrCreate
EOF

# PV 확인
kubectl get pv
kubectl describe pv my-pv
```

```bash
# PersistentVolumeClaim 생성 (사용자 역할)
kubectl apply -f - <<EOF
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: my-pvc
spec:
  storageClassName: manual
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 500Mi
EOF

# PVC 확인 - STATUS가 Bound인지 확인
kubectl get pvc
kubectl describe pvc my-pvc

# PV 상태 다시 확인 - my-pvc에 바인딩됨
kubectl get pv
```

### 실습 2: Pod에서 PVC 사용

```bash
# PVC를 사용하는 Pod 생성
kubectl apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: pv-pod
spec:
  containers:
  - name: app
    image: busybox:1.36
    command: ["sh", "-c"]
    args:
      - |
        echo "Hello from PV Pod - $(date)" >> /data/log.txt
        cat /data/log.txt
        sleep 3600
    volumeMounts:
    - name: data
      mountPath: /data
  volumes:
  - name: data
    persistentVolumeClaim:
      claimName: my-pvc
EOF

# 데이터 확인
kubectl exec pv-pod -- cat /data/log.txt

# Pod 삭제
kubectl delete pod pv-pod

# 새 Pod 생성 - 데이터가 유지되는지 확인
kubectl apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: pv-pod-2
spec:
  containers:
  - name: app
    image: busybox:1.36
    command: ["sh", "-c"]
    args:
      - |
        echo "New pod at $(date)" >> /data/log.txt
        echo "=== All logs ==="
        cat /data/log.txt
        sleep 3600
    volumeMounts:
    - name: data
      mountPath: /data
  volumes:
  - name: data
    persistentVolumeClaim:
      claimName: my-pvc
EOF

# 이전 데이터가 유지되었는지 확인
kubectl logs pv-pod-2
kubectl exec pv-pod-2 -- cat /data/log.txt
```

### 실습 3: StorageClass와 동적 프로비저닝

```bash
# minikube 기본 StorageClass 확인
kubectl get storageclass
kubectl describe storageclass standard

# 동적 프로비저닝 PVC (PV 자동 생성)
kubectl apply -f - <<EOF
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: dynamic-pvc
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 100Mi
  storageClassName: standard
EOF

# PVC 확인 - 자동으로 PV가 생성되고 바인딩됨
kubectl get pvc dynamic-pvc
kubectl get pv

# 상세 정보
kubectl describe pvc dynamic-pvc
```

### 실습 4: StatefulSet에서 PVC 사용

```bash
# StatefulSet + VolumeClaimTemplates
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: redis-cluster
spec:
  serviceName: redis
  replicas: 3
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
      - name: redis
        image: redis:7
        ports:
        - containerPort: 6379
        volumeMounts:
        - name: data
          mountPath: /data
  volumeClaimTemplates:
  - metadata:
      name: data
    spec:
      accessModes: ["ReadWriteOnce"]
      storageClassName: standard
      resources:
        requests:
          storage: 100Mi
EOF

# 각 Pod마다 별도의 PVC가 생성됨
kubectl get pvc
# data-redis-cluster-0
# data-redis-cluster-1
# data-redis-cluster-2

kubectl get pods -l app=redis
```

### 실습 5: 볼륨 스냅샷 (개념)

```yaml
# VolumeSnapshot (참고용 - CSI 드라이버 필요)
apiVersion: snapshot.storage.k8s.io/v1
kind: VolumeSnapshot
metadata:
  name: my-snapshot
spec:
  volumeSnapshotClassName: csi-hostpath-snapclass
  source:
    persistentVolumeClaimName: my-pvc
---
# 스냅샷에서 복원
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: restored-pvc
spec:
  dataSource:
    name: my-snapshot
    kind: VolumeSnapshot
    apiGroup: snapshot.storage.k8s.io
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 500Mi
```

---

## 📚 Part 3: 심화 - 프로덕션 패턴 (45분)

### AWS EBS 기반 StorageClass

```yaml
# EKS에서 EBS CSI 사용
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: ebs-gp3
provisioner: ebs.csi.aws.com
parameters:
  type: gp3
  iops: "3000"
  throughput: "125"
  encrypted: "true"
  kmsKeyId: alias/my-key  # 암호화 키
volumeBindingMode: WaitForFirstConsumer
reclaimPolicy: Delete
allowVolumeExpansion: true
```

### 토스플레이스 패턴: 데이터베이스 PVC

```yaml
# MySQL StatefulSet with PVC
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: mysql
  namespace: payment
spec:
  serviceName: mysql
  replicas: 1
  selector:
    matchLabels:
      app: mysql
  template:
    metadata:
      labels:
        app: mysql
    spec:
      containers:
      - name: mysql
        image: mysql:8.0
        env:
        - name: MYSQL_ROOT_PASSWORD
          valueFrom:
            secretKeyRef:
              name: mysql-secret
              key: root-password
        ports:
        - containerPort: 3306
        volumeMounts:
        - name: data
          mountPath: /var/lib/mysql
        resources:
          requests:
            memory: "1Gi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "1"
  volumeClaimTemplates:
  - metadata:
      name: data
    spec:
      accessModes: ["ReadWriteOnce"]
      storageClassName: ebs-gp3
      resources:
        requests:
          storage: 50Gi
```

### PVC 확장 (Resize)

```bash
# StorageClass가 allowVolumeExpansion: true인 경우
kubectl patch pvc my-pvc -p '{"spec":{"resources":{"requests":{"storage":"1Gi"}}}}'

# 확인
kubectl get pvc my-pvc
kubectl describe pvc my-pvc | grep -A 5 Conditions
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | PV/PVC 개념 이해 | ☐ |
| 2 | Access Modes 이해 | ☐ |
| 3 | 정적 PV/PVC 생성 | ☐ |
| 4 | Pod에서 PVC 사용 | ☐ |
| 5 | Pod 삭제 후 데이터 유지 확인 | ☐ |
| 6 | 동적 프로비저닝 이해 | ☐ |
| 7 | StatefulSet + VolumeClaimTemplates | ☐ |

---

## 🔑 핵심 명령어

```bash
# PV/PVC 관리
kubectl get pv
kubectl get pvc
kubectl describe pv <name>
kubectl describe pvc <name>

# StorageClass
kubectl get storageclass
kubectl describe storageclass <name>
```

---

## 📝 면접 대비 질문

### Q1: PV와 PVC의 차이점은?
> "PV는 관리자가 프로비저닝한 실제 스토리지 리소스입니다. PVC는 사용자/개발자가 스토리지를 요청하는 추상화입니다. PVC가 생성되면 Kubernetes가 조건에 맞는 PV를 찾아 바인딩합니다. 이 분리로 사용자는 스토리지 구현 세부사항을 알 필요 없이 필요한 용량과 접근 모드만 지정하면 됩니다."

### Q2: StorageClass의 역할은?
> "StorageClass는 동적 프로비저닝을 가능하게 합니다. PVC 생성 시 지정된 StorageClass에 따라 자동으로 PV가 생성됩니다. 스토리지 유형(SSD, HDD), 복제 정책, 성능 설정 등을 정의할 수 있습니다. 관리자가 미리 PV를 생성할 필요가 없어 운영이 편리해집니다."

---

## ➡️ 다음 학습: Day 41

**주제**: Ingress
- 외부 트래픽 라우팅
- Ingress Controller
- TLS 설정
