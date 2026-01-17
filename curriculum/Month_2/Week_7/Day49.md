# 📅 Day 49: Pod Security - 컨테이너 보안의 핵심

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "보안 컴플라이언스를 고려한 클라우드 인프라 설계/운영"

Pod 보안 설정(SecurityContext)을 마스터하여 안전한 컨테이너 운영 환경을 구축합니다.

---

## ⏰ 예상 학습 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 | 45분 | 컨테이너 보안 위협과 방어 |
| 실습 | 1.5시간 | SecurityContext 실습 |
| 심화 | 45분 | Pod Security Standards |

---

## 📚 Part 1: 컨테이너 보안 개념 (45분)

### 왜 Pod 보안이 중요한가?

```
┌─────────────────────────────────────────────────────────────┐
│  컨테이너 보안 위협                                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 권한 상승 공격 (Privilege Escalation)                   │
│  ┌─────────────────────────────────────────────────┐       │
│  │ 공격자가 컨테이너 탈출 → 호스트 노드 접근       │       │
│  │ → 다른 컨테이너/데이터 접근                     │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  2. Root 권한 악용                                          │
│  ┌─────────────────────────────────────────────────┐       │
│  │ 컨테이너가 root로 실행 → 파일시스템 변조        │       │
│  │ → 악성코드 삽입, 설정 변경                      │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  3. 민감 데이터 노출                                        │
│  ┌─────────────────────────────────────────────────┐       │
│  │ /etc/shadow, 환경변수, 마운트된 Secret 노출     │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  4. 결제 시스템 위협 (토스플레이스)                         │
│  ┌─────────────────────────────────────────────────┐       │
│  │ PCI-DSS 컴플라이언스 요구사항                   │       │
│  │ - 최소 권한 원칙                                │       │
│  │ - 접근 통제                                     │       │
│  │ - 감사 로깅                                     │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### SecurityContext 구조

```
┌─────────────────────────────────────────────────────────────┐
│  SecurityContext 계층                                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Pod Level (spec.securityContext)                           │
│  ┌─────────────────────────────────────────────────┐       │
│  │ - Pod 전체에 적용                               │       │
│  │ - runAsUser, runAsGroup, fsGroup                │       │
│  │ - supplementalGroups, sysctls                   │       │
│  └─────────────────────────────────────────────────┘       │
│            │                                                │
│            ▼                                                │
│  Container Level (containers[].securityContext)             │
│  ┌─────────────────────────────────────────────────┐       │
│  │ - 개별 컨테이너에 적용                          │       │
│  │ - Pod 설정 오버라이드 가능                      │       │
│  │ - allowPrivilegeEscalation, capabilities        │       │
│  │ - readOnlyRootFilesystem, runAsNonRoot          │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 주요 SecurityContext 옵션

| 옵션 | 설명 | 권장 설정 |
|------|------|-----------|
| `runAsNonRoot` | root 실행 금지 | `true` |
| `runAsUser` | 실행 사용자 UID | `1000` 이상 |
| `runAsGroup` | 실행 그룹 GID | `1000` 이상 |
| `readOnlyRootFilesystem` | 루트 파일시스템 읽기 전용 | `true` |
| `allowPrivilegeEscalation` | 권한 상승 허용 | `false` |
| `capabilities.drop` | Linux capabilities 제거 | `["ALL"]` |

---

## 🛠️ Part 2: 실습 (1.5시간)

### 실습 1: 기본 SecurityContext

```bash
# 보안 설정된 Pod 생성
kubectl apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: secure-pod
  labels:
    app: secure-demo
spec:
  securityContext:
    runAsUser: 1000       # UID 1000으로 실행
    runAsGroup: 3000      # GID 3000으로 실행
    fsGroup: 2000         # 볼륨 파일 그룹
  containers:
  - name: app
    image: busybox:1.36
    command: ["sh", "-c", "id && sleep 3600"]
    securityContext:
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: true
    volumeMounts:
    - name: tmp
      mountPath: /tmp
  volumes:
  - name: tmp
    emptyDir: {}
EOF

# 사용자/그룹 확인
kubectl logs secure-pod
# 출력: uid=1000 gid=3000 groups=2000

# root가 아님을 확인
kubectl exec secure-pod -- whoami
# 출력: whoami: unknown uid 1000
```

### 실습 2: 읽기 전용 파일시스템

```bash
# 읽기 전용 + 필수 쓰기 경로만 허용
kubectl apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: readonly-pod
spec:
  containers:
  - name: nginx
    image: nginx:1.24
    securityContext:
      readOnlyRootFilesystem: true
      runAsNonRoot: true
      runAsUser: 101        # nginx 사용자
    volumeMounts:
    # nginx가 쓰기가 필요한 경로들
    - name: tmp
      mountPath: /tmp
    - name: cache
      mountPath: /var/cache/nginx
    - name: run
      mountPath: /var/run
  volumes:
  - name: tmp
    emptyDir: {}
  - name: cache
    emptyDir: {}
  - name: run
    emptyDir: {}
EOF

# 파일 쓰기 시도 → 실패
kubectl exec readonly-pod -- touch /etc/test
# touch: /etc/test: Read-only file system

# tmp는 쓰기 가능
kubectl exec readonly-pod -- touch /tmp/test
kubectl exec readonly-pod -- ls /tmp
# test
```

### 실습 3: Capabilities 제어

```bash
# 모든 capabilities 제거
kubectl apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: drop-caps-pod
spec:
  containers:
  - name: app
    image: busybox:1.36
    command: ["sh", "-c", "sleep 3600"]
    securityContext:
      capabilities:
        drop:
          - ALL
      allowPrivilegeEscalation: false
      runAsNonRoot: true
      runAsUser: 1000
EOF

# capabilities 확인 (빈 목록이어야 함)
kubectl exec drop-caps-pod -- cat /proc/1/status | grep Cap
# CapInh: 0000000000000000
# CapPrm: 0000000000000000
# CapEff: 0000000000000000
```

### 실습 4: 필요한 Capabilities만 추가

```bash
# 네트워크 관련 capability만 추가
kubectl apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: net-cap-pod
spec:
  containers:
  - name: app
    image: busybox:1.36
    command: ["sh", "-c", "sleep 3600"]
    securityContext:
      capabilities:
        drop:
          - ALL
        add:
          - NET_BIND_SERVICE  # 1024 미만 포트 바인딩
      allowPrivilegeEscalation: false
EOF

kubectl describe pod net-cap-pod | grep -A 5 Capabilities
```

### 실습 5: 완전한 보안 Pod 템플릿

```bash
kubectl apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: hardened-pod
  labels:
    app: secure-app
spec:
  securityContext:
    runAsUser: 1000
    runAsGroup: 1000
    fsGroup: 1000
    seccompProfile:
      type: RuntimeDefault
  
  # 서비스 계정 토큰 자동 마운트 비활성화
  automountServiceAccountToken: false
  
  containers:
  - name: app
    image: nginx:1.24
    ports:
    - containerPort: 8080
    securityContext:
      runAsNonRoot: true
      runAsUser: 101
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: true
      capabilities:
        drop:
          - ALL
    
    resources:
      requests:
        memory: "64Mi"
        cpu: "100m"
      limits:
        memory: "128Mi"
        cpu: "200m"
    
    volumeMounts:
    - name: tmp
      mountPath: /tmp
    - name: cache
      mountPath: /var/cache/nginx
    - name: run
      mountPath: /var/run
  
  volumes:
  - name: tmp
    emptyDir: {}
  - name: cache
    emptyDir: {}
  - name: run
    emptyDir: {}
EOF

# 설정 확인
kubectl describe pod hardened-pod
```

---

## 📚 Part 3: Pod Security Standards (45분)

### Pod Security Standards 개요

```
┌─────────────────────────────────────────────────────────────┐
│  Pod Security Standards (PSS)                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Privileged (권한 있음)                                     │
│  ┌─────────────────────────────────────────────────┐       │
│  │ - 제한 없음                                     │       │
│  │ - 시스템 수준 워크로드용                        │       │
│  │ - 예: CNI, CSI 드라이버                         │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  Baseline (기본)                                            │
│  ┌─────────────────────────────────────────────────┐       │
│  │ - 알려진 권한 상승 방지                         │       │
│  │ - 대부분의 워크로드에 적합                      │       │
│  │ - hostNetwork, hostPID 금지                     │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  Restricted (제한적) ✅ 권장                                │
│  ┌─────────────────────────────────────────────────┐       │
│  │ - 가장 엄격한 보안 정책                         │       │
│  │ - 결제 시스템, 민감 데이터 처리용               │       │
│  │ - runAsNonRoot, drop ALL caps 필수              │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 네임스페이스에 PSS 적용

```bash
# Restricted 정책 적용
kubectl label namespace default \
  pod-security.kubernetes.io/enforce=restricted \
  pod-security.kubernetes.io/warn=restricted \
  pod-security.kubernetes.io/audit=restricted

# 새 네임스페이스에 적용
kubectl apply -f - <<EOF
apiVersion: v1
kind: Namespace
metadata:
  name: secure-namespace
  labels:
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/enforce-version: latest
    pod-security.kubernetes.io/warn: restricted
    pod-security.kubernetes.io/warn-version: latest
EOF

# 정책 위반 Pod 생성 시도
kubectl run test --image=nginx -n secure-namespace
# Error: violates PodSecurity "restricted:latest"
```

### 토스플레이스 보안 템플릿

```yaml
# production-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-service
  namespace: payment
spec:
  replicas: 3
  selector:
    matchLabels:
      app: payment
  template:
    metadata:
      labels:
        app: payment
    spec:
      # Pod 레벨 보안
      securityContext:
        runAsUser: 1000
        runAsGroup: 1000
        fsGroup: 1000
        seccompProfile:
          type: RuntimeDefault
      
      # 서비스 계정 설정
      serviceAccountName: payment-service-account
      automountServiceAccountToken: false
      
      containers:
      - name: payment
        image: payment-service:v1.0.0
        
        # 컨테이너 레벨 보안
        securityContext:
          runAsNonRoot: true
          runAsUser: 1000
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: true
          capabilities:
            drop:
              - ALL
        
        # 리소스 제한
        resources:
          requests:
            memory: "256Mi"
            cpu: "200m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        
        # 헬스체크
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 10
        
        readinessProbe:
          httpGet:
            path: /ready
            port: 8080
          initialDelaySeconds: 5
        
        # 볼륨 마운트
        volumeMounts:
        - name: tmp
          mountPath: /tmp
        - name: secrets
          mountPath: /etc/secrets
          readOnly: true
      
      volumes:
      - name: tmp
        emptyDir: {}
      - name: secrets
        secret:
          secretName: payment-secrets
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | runAsUser/runAsGroup 설정 이해 | ☐ |
| 2 | readOnlyRootFilesystem 실습 | ☐ |
| 3 | capabilities drop 실습 | ☐ |
| 4 | allowPrivilegeEscalation 이해 | ☐ |
| 5 | Pod Security Standards 이해 | ☐ |
| 6 | 보안 강화 Pod 템플릿 작성 | ☐ |

---

## 🔑 오늘 배운 핵심 설정

```yaml
# 최소 권한 원칙 적용
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  readOnlyRootFilesystem: true
  allowPrivilegeEscalation: false
  capabilities:
    drop: ["ALL"]
```

---

## 📝 면접 대비 질문

### Q1: SecurityContext에서 가장 중요한 설정은?
> "runAsNonRoot: true와 allowPrivilegeEscalation: false입니다. 전자는 root 실행을 방지하고, 후자는 컨테이너 내에서 권한 상승을 차단합니다. 추가로 readOnlyRootFilesystem: true와 capabilities drop ALL을 적용하면 대부분의 공격 벡터를 차단할 수 있습니다."

### Q2: Pod Security Standards의 세 가지 레벨을 설명해주세요.
> "Privileged는 제한 없음, Baseline은 알려진 위험 방지, Restricted는 가장 엄격한 보안입니다. 토스플레이스 같은 결제 시스템에서는 Restricted를 기본으로 적용하고, CNI나 CSI 같은 시스템 컴포넌트만 Privileged를 허용합니다."

### Q3: readOnlyRootFilesystem 적용 시 주의점은?
> "애플리케이션이 쓰기를 필요로 하는 경로(임시 파일, 캐시, PID 파일 등)를 파악하고, 해당 경로에 emptyDir 볼륨을 마운트해야 합니다. nginx의 경우 /tmp, /var/cache/nginx, /var/run이 필요합니다."

---

## ➡️ 다음 학습: Day 50

**주제**: NetworkPolicy
- Pod 간 네트워크 격리
- Ingress/Egress 정책
- 마이크로세그멘테이션
