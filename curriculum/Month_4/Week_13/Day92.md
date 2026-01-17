# 📅 Day 92: ArgoCD Application 생성

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "ArgoCD" 사용, "배포 자동화 파이프라인 운영"
> "개발자들이 더 빠르고 안전하게 서비스를 배포할 수 있는 환경"

ArgoCD의 핵심 리소스인 Application을 이해하고 생성합니다. GitOps 기반 배포의 시작점인 Application 정의 방법을 마스터합니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| Application 이론 | 45분 | 구조, 필드 이해 |
| 기본 실습 | 1.5시간 | CLI/YAML 생성 |
| 고급 설정 | 1시간 | Sync Policy, Health |
| 트러블슈팅 | 45분 | 일반적인 문제 해결 |

---

## 📚 Part 1: Application 구조 (45분)

### 1.1 Application이란?

```
┌─────────────────────────────────────────────────────────────────────┐
│  ArgoCD Application 개요                                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Application = Git 저장소와 Kubernetes 클러스터를 연결하는 정의     │
│                                                                      │
│  ┌─────────────────┐                ┌─────────────────────────┐     │
│  │   Git 저장소    │                │   Kubernetes Cluster    │     │
│  │                 │                │                         │     │
│  │ ┌─────────────┐ │    ArgoCD     │ ┌─────────────────────┐ │     │
│  │ │ manifests/  │ │ ════════════> │ │   Namespace: myapp  │ │     │
│  │ │ deployment  │ │  Application  │ │   - Deployment      │ │     │
│  │ │ service     │ │               │ │   - Service         │ │     │
│  │ │ configmap   │ │               │ │   - ConfigMap       │ │     │
│  │ └─────────────┘ │               │ └─────────────────────┘ │     │
│  └─────────────────┘                └─────────────────────────┘     │
│                                                                      │
│  핵심 개념:                                                         │
│  • Source: Git 저장소 + 경로                                        │
│  • Destination: 클러스터 + 네임스페이스                             │
│  • Sync Policy: 동기화 방식 (자동/수동)                             │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 Application YAML 상세

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: myapp                    # Application 이름
  namespace: argocd              # ArgoCD가 설치된 네임스페이스
  finalizers:                    # 삭제 시 관련 리소스도 삭제
  - resources-finalizer.argocd.argoproj.io
spec:
  # ArgoCD 프로젝트 (권한 관리 단위)
  project: default
  
  # Git 소스 설정
  source:
    repoURL: https://github.com/user/repo.git  # Git 저장소 URL
    targetRevision: main         # 브랜치/태그/커밋
    path: manifests              # 매니페스트 경로
    
    # Helm 차트인 경우
    # helm:
    #   valueFiles:
    #   - values.yaml
    
    # Kustomize인 경우
    # kustomize:
    #   namePrefix: prod-
  
  # 배포 대상
  destination:
    server: https://kubernetes.default.svc  # 클러스터 API
    namespace: default           # 배포될 네임스페이스
  
  # 동기화 정책
  syncPolicy:
    automated:                   # 자동 동기화
      prune: true                # Git에 없는 리소스 삭제
      selfHeal: true             # 수동 변경 시 복구
      allowEmpty: false          # 빈 App 허용 안 함
    syncOptions:
    - CreateNamespace=true       # 네임스페이스 자동 생성
    - PrunePropagationPolicy=foreground
    retry:
      limit: 5                   # 재시도 횟수
      backoff:
        duration: 5s
        factor: 2
        maxDuration: 3m
```

### 1.3 핵심 필드 설명

| 필드 | 설명 | 예시 |
|------|------|------|
| **project** | 권한 관리 단위 | default |
| **source.repoURL** | Git 저장소 URL | https://github.com/... |
| **source.targetRevision** | 브랜치/태그/커밋 | main, v1.0.0, abc1234 |
| **source.path** | 매니페스트 경로 | manifests, helm/myapp |
| **destination.server** | 클러스터 API | https://kubernetes.default.svc |
| **destination.namespace** | 대상 네임스페이스 | production |
| **syncPolicy.automated** | 자동 동기화 여부 | prune: true, selfHeal: true |

---

## 🛠️ Part 2: 실습 환경 준비 (30분)

### 실습 1: 샘플 앱 저장소 준비

```bash
# 디렉토리 생성
mkdir -p ~/argocd-demo/manifests
cd ~/argocd-demo

# Deployment 생성
cat << 'EOF' > manifests/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: demo-app
  labels:
    app: demo
spec:
  replicas: 2
  selector:
    matchLabels:
      app: demo
  template:
    metadata:
      labels:
        app: demo
    spec:
      containers:
      - name: demo
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
          initialDelaySeconds: 10
          periodSeconds: 15
EOF

# Service 생성
cat << 'EOF' > manifests/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: demo-app
  labels:
    app: demo
spec:
  selector:
    app: demo
  ports:
  - port: 80
    targetPort: 80
    protocol: TCP
  type: ClusterIP
EOF

# ConfigMap 생성
cat << 'EOF' > manifests/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: demo-config
data:
  APP_ENV: "development"
  LOG_LEVEL: "info"
EOF

# Namespace 생성
cat << 'EOF' > manifests/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: demo-app
  labels:
    app: demo
    managed-by: argocd
EOF

# Kustomization 파일 (선택)
cat << 'EOF' > manifests/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
- namespace.yaml
- configmap.yaml
- deployment.yaml
- service.yaml
EOF

# .gitignore
cat << 'EOF' > .gitignore
.DS_Store
*.secret
EOF

# Git 초기화 및 커밋
git init
git add .
git commit -m "Initial commit: demo app manifests"

# GitHub에 push (저장소 미리 생성 필요)
# git remote add origin https://github.com/YOUR_USERNAME/argocd-demo.git
# git push -u origin main

echo "=== 파일 구조 ==="
tree ~/argocd-demo
```

---

## 🛠️ Part 3: Application 생성 - CLI (45분)

### 실습 2: CLI로 Application 생성

```bash
# ArgoCD 로그인 (Day 91에서 설정)
argocd login localhost:8080 --insecure

# Application 생성 (기본)
argocd app create demo-app \
  --repo https://github.com/YOUR_USERNAME/argocd-demo.git \
  --path manifests \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace demo-app

# Application 목록 확인
argocd app list

# 예상 출력:
# NAME      CLUSTER                         NAMESPACE  PROJECT  STATUS   HEALTH   SYNCPOLICY  CONDITIONS
# demo-app  https://kubernetes.default.svc  demo-app   default  OutOfSync  Missing  Manual      <none>

# Application 상세 정보
argocd app get demo-app

# 예상 출력:
# Name:               argocd/demo-app
# Project:            default
# Server:             https://kubernetes.default.svc
# Namespace:          demo-app
# URL:                https://localhost:8080/applications/demo-app
# Repo:               https://github.com/YOUR_USERNAME/argocd-demo.git
# Target:             HEAD
# Path:               manifests
# SyncWindow:         Sync Allowed
# Sync Policy:        <none>
# Sync Status:        OutOfSync from HEAD
# Health Status:      Missing
```

### 실습 3: CLI 옵션 상세

```bash
# 자동 동기화 설정으로 생성
argocd app create demo-app-auto \
  --repo https://github.com/YOUR_USERNAME/argocd-demo.git \
  --path manifests \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace demo-app-auto \
  --sync-policy automated \
  --auto-prune \
  --self-heal \
  --sync-option CreateNamespace=true

# 특정 브랜치 지정
argocd app create demo-app-dev \
  --repo https://github.com/YOUR_USERNAME/argocd-demo.git \
  --path manifests \
  --revision develop \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace demo-app-dev

# Helm Chart로 생성
argocd app create nginx-helm \
  --repo https://charts.bitnami.com/bitnami \
  --helm-chart nginx \
  --revision 15.0.0 \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace nginx

# 유용한 CLI 명령어
argocd app diff demo-app           # Git과 클러스터 차이
argocd app logs demo-app           # 배포 로그
argocd app manifests demo-app      # 렌더링된 매니페스트
argocd app resources demo-app      # 리소스 목록
argocd app actions list demo-app   # 사용 가능한 액션
```

---

## 🛠️ Part 4: Application 생성 - YAML (45분)

### 실습 4: YAML로 Application 생성

```yaml
# application.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: demo-app
  namespace: argocd
  labels:
    app: demo
    team: platform
  annotations:
    notifications.argoproj.io/subscribe.on-deployed.slack: deployments
  finalizers:
  - resources-finalizer.argocd.argoproj.io
spec:
  project: default
  
  source:
    repoURL: https://github.com/YOUR_USERNAME/argocd-demo.git
    targetRevision: HEAD
    path: manifests
    
    # 디렉토리 옵션 (plain YAML)
    directory:
      recurse: false
      jsonnet: {}
  
  destination:
    server: https://kubernetes.default.svc
    namespace: demo-app
  
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
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
  
  # 무시할 차이점
  ignoreDifferences:
  - group: apps
    kind: Deployment
    jsonPointers:
    - /spec/replicas  # HPA와 충돌 방지
  
  # 리소스 상태 체크 커스텀
  # 기본 Health Check 외 추가 조건
```

```bash
# Application 생성
kubectl apply -f application.yaml

# 상태 확인
kubectl get application -n argocd
kubectl describe application demo-app -n argocd

# ArgoCD CLI로도 확인
argocd app get demo-app
```

### 실습 5: Helm Application

```yaml
# helm-application.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: nginx-helm
  namespace: argocd
spec:
  project: default
  
  source:
    repoURL: https://charts.bitnami.com/bitnami
    chart: nginx
    targetRevision: 15.0.0
    
    helm:
      releaseName: my-nginx
      valueFiles:
      - values.yaml
      values: |
        replicaCount: 3
        service:
          type: ClusterIP
        resources:
          limits:
            cpu: 200m
            memory: 256Mi
          requests:
            cpu: 100m
            memory: 128Mi
      parameters:
      - name: image.tag
        value: "1.25"
  
  destination:
    server: https://kubernetes.default.svc
    namespace: nginx
  
  syncPolicy:
    syncOptions:
    - CreateNamespace=true
```

### 실습 6: Kustomize Application

```yaml
# kustomize-application.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: demo-app-prod
  namespace: argocd
spec:
  project: default
  
  source:
    repoURL: https://github.com/YOUR_USERNAME/argocd-demo.git
    targetRevision: HEAD
    path: overlays/production
    
    kustomize:
      namePrefix: prod-
      nameSuffix: ""
      images:
      - nginx:1.25
      commonLabels:
        environment: production
      commonAnnotations:
        owner: platform-team
  
  destination:
    server: https://kubernetes.default.svc
    namespace: production
  
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
    - CreateNamespace=true
```

---

## 🛠️ Part 5: Sync 동작 (30분)

### 실습 7: 수동 Sync

```bash
# 현재 상태 확인
argocd app get demo-app

# 수동 Sync
argocd app sync demo-app

# Sync 진행 상황 (실시간)
argocd app sync demo-app --watch

# 예상 출력:
# TIMESTAMP                  GROUP        KIND         NAMESPACE   NAME      STATUS   HEALTH   HOOK  MESSAGE
# 2024-01-15T10:00:00+09:00            Namespace                  demo-app  Synced            
# 2024-01-15T10:00:01+09:00            ConfigMap    demo-app     demo-config  Synced            
# 2024-01-15T10:00:02+09:00  apps       Deployment   demo-app     demo-app  Synced   Progressing
# 2024-01-15T10:00:03+09:00            Service      demo-app     demo-app  Synced            

# K8s에서 확인
kubectl get all -n demo-app

# Sync 상태 확인
argocd app get demo-app

# 예상 출력:
# Sync Status:        Synced
# Health Status:      Healthy
```

### 실습 8: Selective Sync

```bash
# 특정 리소스만 Sync
argocd app sync demo-app --resource apps:Deployment:demo-app

# 특정 리소스 제외하고 Sync
argocd app sync demo-app --prune=false

# Dry Run (실제 적용 안 함)
argocd app sync demo-app --dry-run

# Force Sync (충돌 무시)
argocd app sync demo-app --force

# Replace로 Sync (apply 대신)
argocd app sync demo-app --replace
```

### 실습 9: Refresh와 Hard Refresh

```bash
# Refresh: Git에서 최신 상태 가져오기
argocd app refresh demo-app

# Hard Refresh: 캐시 무효화 후 완전히 새로 가져오기
argocd app get demo-app --hard-refresh

# 자동 Refresh 간격은 ArgoCD 설정에서 관리
# 기본: 3분
```

---

## 🛠️ Part 6: Application 상태 및 Health (30분)

### 6.1 Sync Status

| 상태 | 설명 |
|------|------|
| **Synced** | Git과 클러스터가 일치 |
| **OutOfSync** | Git과 클러스터가 다름 |
| **Unknown** | 상태 확인 불가 |

### 6.2 Health Status

| 상태 | 설명 |
|------|------|
| **Healthy** | 모든 리소스 정상 |
| **Progressing** | 리소스 생성/업데이트 중 |
| **Degraded** | 일부 리소스 문제 |
| **Suspended** | 일시 중지됨 (HPA 0 등) |
| **Missing** | 리소스 없음 |
| **Unknown** | 상태 확인 불가 |

### 실습 10: Health 확인

```bash
# 상세 Health 상태
argocd app get demo-app

# 리소스별 상태
argocd app resources demo-app

# 출력 예시:
# GROUP  KIND        NAMESPACE  NAME         STATUS  HEALTH   HOOK  MESSAGE
#        ConfigMap   demo-app   demo-config  Synced  Healthy
# apps   Deployment  demo-app   demo-app     Synced  Healthy
#        Service     demo-app   demo-app     Synced  Healthy

# Unhealthy 원인 확인
kubectl describe deployment demo-app -n demo-app
kubectl get events -n demo-app --sort-by=.lastTimestamp
```

---

## 🛠️ Part 7: 트러블슈팅 (30분)

### 7.1 일반적인 문제

```bash
# 문제 1: OutOfSync 상태 지속
# 원인: ignoreDifferences 설정 필요
# 해결:
argocd app diff demo-app  # 차이점 확인

# ignoreDifferences 추가
kubectl edit application demo-app -n argocd
# spec.ignoreDifferences 추가

# 문제 2: Sync 실패
# 원인: Git 접근 오류, 매니페스트 오류
# 해결:
argocd app get demo-app  # 상세 에러 확인
argocd app manifests demo-app  # 렌더링 확인

# 문제 3: Health가 Progressing에서 멈춤
# 원인: Pod Ready 실패
# 해결:
kubectl get pods -n demo-app
kubectl describe pod <pod-name> -n demo-app
kubectl logs <pod-name> -n demo-app

# 문제 4: 리소스 삭제 안 됨
# 원인: finalizers 또는 prune 설정
# 해결:
argocd app sync demo-app --prune  # prune 강제
# 또는 finalizers 제거
kubectl patch application demo-app -n argocd -p '{"metadata":{"finalizers":null}}' --type=merge
```

### 7.2 디버깅 명령어

```bash
# Application 이벤트
kubectl get events -n argocd --field-selector involvedObject.name=demo-app

# ArgoCD Controller 로그
kubectl logs -n argocd -l app.kubernetes.io/component=application-controller -f

# ArgoCD Server 로그
kubectl logs -n argocd -l app.kubernetes.io/component=server -f

# Git 연결 테스트
argocd repo list
argocd repo get https://github.com/YOUR_USERNAME/argocd-demo.git

# 클러스터 연결 테스트
argocd cluster list
argocd cluster get https://kubernetes.default.svc
```

---

## 📊 Part 8: Application 관리

### 8.1 Application 수정

```bash
# CLI로 수정
argocd app set demo-app --sync-policy automated --auto-prune --self-heal

# YAML 수정 후 적용
kubectl apply -f application.yaml

# 특정 필드 수정
argocd app set demo-app --revision v1.0.0  # 리비전 변경
argocd app set demo-app --path overlays/prod  # 경로 변경
```

### 8.2 Application 삭제

```bash
# Application만 삭제 (리소스 유지)
argocd app delete demo-app --cascade=false

# Application과 리소스 모두 삭제
argocd app delete demo-app

# 강제 삭제
argocd app delete demo-app --cascade --propagation-policy foreground
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | Application 구조 이해 | source, destination, syncPolicy | ☐ |
| 2 | Git 저장소 준비 | manifests 디렉토리 구성 | ☐ |
| 3 | Application 생성 (CLI) | argocd app create | ☐ |
| 4 | Application 생성 (YAML) | kubectl apply | ☐ |
| 5 | Sync 실행 | argocd app sync | ☐ |
| 6 | Health 상태 확인 | Healthy, Progressing | ☐ |
| 7 | 트러블슈팅 | diff, logs, events | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# Application 관리
argocd app create <name> --repo <url> --path <path> --dest-server <cluster> --dest-namespace <ns>
argocd app list
argocd app get <name>
argocd app sync <name>
argocd app delete <name>

# 상태 확인
argocd app diff <name>
argocd app resources <name>
argocd app manifests <name>
argocd app refresh <name>

# 설정 변경
argocd app set <name> --sync-policy automated
argocd app set <name> --revision <branch/tag>
```

---

## 💡 면접 대비 핵심 포인트

### Q1: ArgoCD Application이란?

**A**: "ArgoCD Application은 Git 저장소와 Kubernetes 클러스터를 연결하는 선언적 정의입니다. source로 Git 위치를, destination으로 배포 대상을 지정하고, syncPolicy로 동기화 방식을 설정합니다. Application을 생성하면 ArgoCD가 Git 변경을 감지하여 자동으로 클러스터에 배포합니다."

### Q2: Sync Policy의 prune과 selfHeal의 차이는?

**A**: 
- **prune**: Git에서 삭제된 리소스를 클러스터에서도 삭제. false면 Git에서 제거해도 클러스터에 남음
- **selfHeal**: kubectl 등으로 수동 변경된 리소스를 Git 상태로 복구. 드리프트 방지

### Q3: OutOfSync 상태가 지속되는 이유는?

**A**: "HPA, 메타데이터 변경 등으로 클러스터 상태가 Git과 다를 수 있습니다. ignoreDifferences로 특정 필드를 무시하거나, 해당 필드를 Git에서도 관리하지 않도록 설정합니다. `argocd app diff`로 정확한 차이를 확인하고 대응합니다."

---

## 📝 학습 기록

```
학습일: ____년 __월 __일
실제 소요 시간: ____시간

실습 완료 항목:
- [ ] Git 저장소 준비
- [ ] CLI로 Application 생성
- [ ] YAML로 Application 생성
- [ ] Sync 실행
- [ ] 트러블슈팅

이해가 어려웠던 부분:


추가 학습 필요 항목:

```

---

## ➡️ 다음 학습: Day 93

**주제**: Auto Sync & Self Heal
- 자동 동기화 설정
- Self-Healing 동작
- Sync Window
