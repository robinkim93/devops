# 📅 Day 95: ArgoCD App of Apps

## 🎯 오늘의 목표

> **토스플레이스 연결점**: ArgoCD를 활용한 GitOps 기반의 대규모 애플리케이션 관리
> App of Apps 패턴으로 여러 앱을 효율적으로 관리

토스플레이스는 GoCD, ArgoCD를 사용하여 CI/CD 파이프라인을 운영합니다. 여러 마이크로서비스를 일관되게 관리하기 위해 App of Apps 패턴은 필수적인 기술입니다.

---

## ⏰ 예상 학습 시간: 4시간

---

## 📚 Part 1: App of Apps 패턴 이해 (1시간)

### 1.1 App of Apps란?

App of Apps는 ArgoCD에서 여러 Application을 계층적으로 관리하는 패턴입니다. 하나의 "부모" Application이 여러 "자식" Application들을 관리합니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│  App of Apps Pattern (토스플레이스 환경)                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│                    ┌─────────────────────┐                          │
│                    │   Root Application  │                          │
│                    │   (infrastructure)  │                          │
│                    └──────────┬──────────┘                          │
│                               │                                      │
│        ┌──────────────────────┼──────────────────────┐              │
│        │                      │                      │              │
│        ▼                      ▼                      ▼              │
│  ┌───────────┐         ┌───────────┐         ┌───────────┐         │
│  │ Platform  │         │ Monitoring│         │  Services │         │
│  │   Apps    │         │   Apps    │         │   Apps    │         │
│  └─────┬─────┘         └─────┬─────┘         └─────┬─────┘         │
│        │                     │                     │                │
│   ┌────┴────┐           ┌────┴────┐           ┌────┴────┐          │
│   ▼         ▼           ▼         ▼           ▼         ▼          │
│ Istio   Cert-Mgr    Prometheus Grafana    Payment   Gateway        │
│                                                                      │
│  장점:                                                               │
│  - 중앙 집중식 관리                                                  │
│  - 환경별 분리 (dev/staging/prod)                                   │
│  - 일괄 배포/롤백                                                    │
│  - 의존성 관리                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 왜 App of Apps가 필요한가?

| 문제 상황 | App of Apps 해결책 |
|----------|-------------------|
| 수십 개 앱 개별 관리 어려움 | 계층적 구조로 일괄 관리 |
| 환경별 설정 중복 | 템플릿화된 Application 정의 |
| 의존성 배포 순서 | Sync Wave로 순서 제어 |
| 새 앱 추가 복잡 | Git에 파일 추가만으로 자동 배포 |
| 전체 시스템 상태 파악 어려움 | ArgoCD UI에서 한눈에 확인 |

### 1.3 패턴 비교

```
┌────────────────────────────────────────────────────────────────────┐
│  패턴 비교                                                          │
├────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. 개별 Application 관리 (비권장)                                  │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  kubectl apply -f app-a.yaml                                │   │
│  │  kubectl apply -f app-b.yaml                                │   │
│  │  kubectl apply -f app-c.yaml  ← 매번 수동                   │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  2. App of Apps (권장)                                             │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  kubectl apply -f root-app.yaml  ← 한 번만!                 │   │
│  │  → 나머지는 ArgoCD가 자동 관리                              │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  3. ApplicationSet (고급)                                          │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  하나의 템플릿으로 여러 Application 자동 생성                │   │
│  │  (App of Apps의 진화 버전)                                  │   │
│  └─────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: 디렉토리 구조 설계 (30분)

### 2.1 기본 구조

```
app-of-apps/
├── apps/                    # Root Application이 참조하는 디렉토리
│   ├── platform/           # 플랫폼 관련 앱 정의
│   │   ├── istio.yaml
│   │   ├── cert-manager.yaml
│   │   └── external-secrets.yaml
│   ├── monitoring/         # 모니터링 관련 앱 정의
│   │   ├── prometheus.yaml
│   │   ├── grafana.yaml
│   │   └── loki.yaml
│   └── services/           # 비즈니스 서비스 앱 정의
│       ├── payment-api.yaml
│       ├── gateway.yaml
│       └── user-service.yaml
│
├── services/                # 실제 Kubernetes 매니페스트
│   ├── payment-api/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   ├── configmap.yaml
│   │   └── hpa.yaml
│   ├── gateway/
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   └── user-service/
│       ├── deployment.yaml
│       └── service.yaml
│
├── root-app.yaml            # Root Application 정의
└── README.md
```

### 2.2 환경별 구조 (실무)

```
gitops-repo/
├── apps/                    # Application 정의
│   ├── base/               # 기본 Application 템플릿
│   │   ├── payment-api.yaml
│   │   └── gateway.yaml
│   ├── dev/                # 개발 환경
│   │   ├── kustomization.yaml
│   │   └── patches/
│   ├── staging/            # 스테이징 환경
│   │   ├── kustomization.yaml
│   │   └── patches/
│   └── production/         # 프로덕션 환경
│       ├── kustomization.yaml
│       └── patches/
│
├── services/                # 서비스 매니페스트
│   ├── payment-api/
│   │   ├── base/
│   │   └── overlays/
│   │       ├── dev/
│   │       ├── staging/
│   │       └── production/
│   └── gateway/
│       ├── base/
│       └── overlays/
│
├── root-apps/              # 환경별 Root Application
│   ├── dev-root.yaml
│   ├── staging-root.yaml
│   └── production-root.yaml
│
└── bootstrap.yaml          # 최초 부트스트랩 Application
```

---

## 🛠️ Part 3: 실습 - App of Apps 구현 (2시간)

### 실습 1: 프로젝트 디렉토리 생성

```bash
# 프로젝트 디렉토리 생성
mkdir -p ~/app-of-apps/{apps,services/{app-a,app-b,app-c}}
cd ~/app-of-apps
```

### 실습 2: 자식 Application 정의

```yaml
# apps/app-a.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: app-a
  namespace: argocd
  # Finalizer: 삭제 시 리소스도 함께 삭제
  finalizers:
  - resources-finalizer.argocd.argoproj.io
  # 레이블로 그룹화
  labels:
    app.kubernetes.io/part-of: app-of-apps
    tier: service
  # 어노테이션으로 Sync Wave 설정
  annotations:
    argocd.argoproj.io/sync-wave: "2"  # 순서 제어
spec:
  project: default
  
  # 소스 설정
  source:
    repoURL: https://github.com/your-org/app-of-apps.git
    targetRevision: HEAD
    path: services/app-a
    
    # Kustomize 사용 시
    # kustomize:
    #   namePrefix: dev-
    
    # Helm 사용 시
    # helm:
    #   valueFiles:
    #     - values-dev.yaml
  
  # 배포 대상
  destination:
    server: https://kubernetes.default.svc
    namespace: app-a
  
  # 동기화 정책
  syncPolicy:
    automated:
      prune: true      # 불필요한 리소스 자동 삭제
      selfHeal: true   # 수동 변경 시 자동 복구
      allowEmpty: false
    syncOptions:
    - CreateNamespace=true
    - PruneLast=true
    - ApplyOutOfSyncOnly=true
    retry:
      limit: 5
      backoff:
        duration: 5s
        factor: 2
        maxDuration: 3m
```

```yaml
# apps/app-b.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: app-b
  namespace: argocd
  finalizers:
  - resources-finalizer.argocd.argoproj.io
  labels:
    app.kubernetes.io/part-of: app-of-apps
    tier: service
  annotations:
    argocd.argoproj.io/sync-wave: "2"
spec:
  project: default
  source:
    repoURL: https://github.com/your-org/app-of-apps.git
    targetRevision: HEAD
    path: services/app-b
  destination:
    server: https://kubernetes.default.svc
    namespace: app-b
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
    - CreateNamespace=true
```

```yaml
# apps/app-c.yaml (Helm Chart 사용 예시)
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: app-c
  namespace: argocd
  finalizers:
  - resources-finalizer.argocd.argoproj.io
  labels:
    app.kubernetes.io/part-of: app-of-apps
    tier: service
  annotations:
    argocd.argoproj.io/sync-wave: "2"
spec:
  project: default
  source:
    # Helm Repository 사용
    repoURL: https://charts.bitnami.com/bitnami
    chart: nginx
    targetRevision: 15.0.0
    helm:
      releaseName: app-c
      values: |
        replicaCount: 3
        service:
          type: ClusterIP
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "200m"
  destination:
    server: https://kubernetes.default.svc
    namespace: app-c
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
    - CreateNamespace=true
```

### 실습 3: 서비스 매니페스트 작성

```yaml
# services/app-a/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: app-a
  labels:
    app: app-a
spec:
  replicas: 2
  selector:
    matchLabels:
      app: app-a
  template:
    metadata:
      labels:
        app: app-a
    spec:
      containers:
      - name: app-a
        image: nginx:1.25
        ports:
        - containerPort: 80
        resources:
          requests:
            memory: "64Mi"
            cpu: "100m"
          limits:
            memory: "128Mi"
            cpu: "200m"
        readinessProbe:
          httpGet:
            path: /
            port: 80
          initialDelaySeconds: 5
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /
            port: 80
          initialDelaySeconds: 15
          periodSeconds: 20
---
# services/app-a/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: app-a
spec:
  selector:
    app: app-a
  ports:
  - port: 80
    targetPort: 80
```

### 실습 4: Root Application 정의

```yaml
# root-app.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: root-app
  namespace: argocd
  # Root App은 Finalizer 주의 (모든 자식 앱도 삭제됨)
  finalizers:
  - resources-finalizer.argocd.argoproj.io
spec:
  project: default
  
  source:
    repoURL: https://github.com/your-org/app-of-apps.git
    targetRevision: HEAD
    path: apps           # 자식 Application 정의가 있는 디렉토리
    
    # 디렉토리 내 모든 YAML 처리
    directory:
      recurse: true      # 하위 디렉토리도 포함
      exclude: '{*.md,test/*}'  # 제외 패턴
  
  destination:
    server: https://kubernetes.default.svc
    namespace: argocd    # Application 리소스는 argocd NS에 생성
  
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
    - CreateNamespace=true
```

### 실습 5: 배포 및 확인

```bash
# Git 저장소 초기화 및 푸시
cd ~/app-of-apps
git init
git add .
git commit -m "Initial app-of-apps setup"
git remote add origin https://github.com/your-org/app-of-apps.git
git push -u origin main

# Root Application 배포
kubectl apply -f root-app.yaml

# ArgoCD에서 확인
argocd app list

# 출력 예시:
# NAME       SYNC      HEALTH    NAMESPACE
# root-app   Synced    Healthy   argocd
# app-a      Synced    Healthy   app-a
# app-b      Synced    Healthy   app-b
# app-c      Synced    Healthy   app-c

# 자식 앱 상세 확인
argocd app get app-a

# 동기화 상태 확인
argocd app sync root-app

# 모든 자식 앱 동기화
argocd app sync -l app.kubernetes.io/part-of=app-of-apps
```

---

## 🛠️ Part 4: 고급 패턴 (30분)

### 4.1 Sync Wave를 활용한 배포 순서 제어

```yaml
# 인프라 먼저 배포 (wave: 0)
# apps/platform/namespace.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: namespaces
  annotations:
    argocd.argoproj.io/sync-wave: "0"
spec:
  source:
    path: base/namespaces
# ...

# 데이터베이스 배포 (wave: 1)
# apps/platform/database.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: database
  annotations:
    argocd.argoproj.io/sync-wave: "1"
spec:
  source:
    path: services/database
# ...

# 애플리케이션 배포 (wave: 2)
# apps/services/app-a.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: app-a
  annotations:
    argocd.argoproj.io/sync-wave: "2"
spec:
  source:
    path: services/app-a
# ...
```

### 4.2 환경별 분리

```yaml
# root-apps/dev-root.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: dev-root-app
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/your-org/app-of-apps.git
    targetRevision: develop  # develop 브랜치
    path: apps/dev
  destination:
    server: https://kubernetes.default.svc
    namespace: argocd
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
---
# root-apps/production-root.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: prod-root-app
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/your-org/app-of-apps.git
    targetRevision: main  # main 브랜치 (프로덕션)
    path: apps/production
  destination:
    server: https://kubernetes.default.svc
    namespace: argocd
  syncPolicy:
    automated:
      prune: true
      selfHeal: false  # 프로덕션은 수동 확인
```

### 4.3 Multi-Cluster 배포

```yaml
# apps/app-a-cluster-1.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: app-a-cluster-1
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/your-org/app-of-apps.git
    path: services/app-a
  destination:
    # 클러스터 1에 배포
    server: https://cluster-1.example.com
    namespace: app-a
  syncPolicy:
    automated:
      selfHeal: true
---
# apps/app-a-cluster-2.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: app-a-cluster-2
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/your-org/app-of-apps.git
    path: services/app-a
  destination:
    # 클러스터 2에 배포
    server: https://cluster-2.example.com
    namespace: app-a
  syncPolicy:
    automated:
      selfHeal: true
```

---

## 📊 Part 5: 모니터링 및 트러블슈팅 (30분)

### 5.1 상태 확인 명령어

```bash
# 모든 Application 상태 확인
argocd app list

# 특정 앱 상세 상태
argocd app get app-a

# 동기화 상태가 OutOfSync인 앱 찾기
argocd app list -o wide | grep OutOfSync

# 앱의 리소스 트리 확인
argocd app resources app-a

# 앱 히스토리 확인
argocd app history app-a

# 실시간 로그 확인
argocd app logs app-a --follow

# Diff 확인 (Git vs Cluster)
argocd app diff app-a
```

### 5.2 일반적인 문제와 해결

```bash
# 문제 1: OutOfSync 상태가 지속
# 원인: 리소스 충돌 또는 권한 문제
argocd app sync app-a --prune
argocd app get app-a --hard-refresh

# 문제 2: 자식 앱이 생성되지 않음
# 확인: Root App 상태 확인
argocd app get root-app
kubectl get applications -n argocd

# 문제 3: Finalizer로 인한 삭제 지연
# Finalizer 제거 후 삭제
kubectl patch application app-a -n argocd \
  --type json \
  -p '[{"op": "remove", "path": "/metadata/finalizers"}]'

# 문제 4: Sync Wave 순서 문제
# 특정 Wave만 동기화
argocd app sync root-app --sync-wave 0
```

### 5.3 Notification 설정

```yaml
# argocd-notifications-cm ConfigMap
apiVersion: v1
kind: ConfigMap
metadata:
  name: argocd-notifications-cm
  namespace: argocd
data:
  trigger.on-sync-failed: |
    - when: app.status.operationState.phase in ['Error', 'Failed']
      send: [app-sync-failed]
  template.app-sync-failed: |
    message: |
      Application {{.app.metadata.name}} sync failed.
      Error: {{.app.status.operationState.message}}
  service.slack: |
    token: $slack-token
    channel: argocd-alerts
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | App of Apps 패턴 개념 이해 | ☐ |
| 2 | 디렉토리 구조 설계 완료 | ☐ |
| 3 | 자식 Application 정의 작성 | ☐ |
| 4 | Root Application 배포 | ☐ |
| 5 | 자식 Apps 자동 생성 확인 | ☐ |
| 6 | Sync Wave로 순서 제어 테스트 | ☐ |
| 7 | 환경별 분리 구조 이해 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# Root App 배포
kubectl apply -f root-app.yaml

# 모든 앱 상태 확인
argocd app list

# 특정 레이블의 앱 모두 동기화
argocd app sync -l app.kubernetes.io/part-of=app-of-apps

# 앱 상세 정보
argocd app get <app-name>

# 앱 삭제 (cascade)
argocd app delete root-app --cascade
```

---

## 💡 면접 대비 핵심 포인트

### Q1: App of Apps 패턴의 장점은?
**A**: 
- 수십~수백 개의 애플리케이션을 계층적으로 관리
- Git에 Application 정의만 추가하면 자동 배포
- Sync Wave로 의존성 있는 배포 순서 제어
- 환경별(dev/staging/prod) 일관된 관리

### Q2: ApplicationSet과의 차이점은?
**A**:
- App of Apps: 명시적 Application YAML 파일 필요
- ApplicationSet: 템플릿 기반으로 동적 생성
- ApplicationSet이 더 유연하지만, App of Apps가 더 직관적

### Q3: 프로덕션에서 주의할 점은?
**A**:
- Finalizer 설정 주의 (삭제 시 연쇄 삭제)
- 프로덕션은 selfHeal: false 권장 (수동 검토)
- Sync Wave로 DB → 앱 순서 보장
- RBAC으로 환경별 접근 제어

---

## 🔗 참고 자료

- [ArgoCD App of Apps](https://argo-cd.readthedocs.io/en/stable/operator-manual/cluster-bootstrapping/)
- [ApplicationSet](https://argo-cd.readthedocs.io/en/stable/operator-manual/applicationset/)
- [Sync Waves](https://argo-cd.readthedocs.io/en/stable/user-guide/sync-waves/)

---

## ➡️ 다음 학습: Day 96

**주제**: ArgoCD Project & RBAC
- Project로 애플리케이션 그룹화
- RBAC으로 팀별 권한 관리
- SSO 연동
