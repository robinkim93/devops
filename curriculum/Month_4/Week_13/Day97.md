# 📅 Day 97: Week 13 종합 복습 - ArgoCD 완벽 정리

## 🎯 오늘의 목표

> Week 13에서 학습한 ArgoCD 전체 내용을 종합적으로 정리하고 실전에 적용할 수 있도록 체계화합니다.

---

## ⏰ 예상 학습 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 정리 | 1시간 | 핵심 개념 복습 |
| 실습 정리 | 1시간 | 주요 명령어 |
| 면접 준비 | 1시간 | Q&A 정리 |

---

## 📋 Week 13 학습 요약

| Day | 주제 | 핵심 내용 |
|-----|------|----------|
| 91 | GitOps/ArgoCD 소개 | Pull-based CD, 설치, CLI |
| 92 | Application | 생성, Sync, 상태 확인 |
| 93 | Auto Sync | 자동 동기화, Self-Heal, Prune |
| 94 | Rollback | 히스토리, 롤백 전략 |
| 95 | App of Apps | 멀티 앱 관리 패턴 |
| 96 | Project/RBAC | 접근 제어, 권한 분리 |

---

## 📚 핵심 개념 정리

### 1. GitOps 원칙

```
GitOps = Git + Operations

핵심 원칙:
1. 선언적 (Declarative) - 원하는 상태를 코드로 정의
2. 버전 관리 (Versioned) - Git으로 모든 변경 추적
3. 자동 적용 (Automated) - 변경 감지 → 자동 배포
4. 자가 치유 (Self-Healing) - 수동 변경 자동 복구
```

### 2. ArgoCD 아키텍처

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      ArgoCD Architecture                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐              │
│  │ Git Repo    │────▶│ ArgoCD      │────▶│ Kubernetes  │              │
│  │ (Source)    │     │ Server      │     │ Cluster     │              │
│  └─────────────┘     └─────────────┘     └─────────────┘              │
│         ↑                   │                    ↑                     │
│         │                   │                    │                     │
│    Developer           Poll/Watch            Apply                     │
│    git push           (3분마다)            Manifests                   │
│                                                                         │
│  컴포넌트:                                                             │
│  - API Server: REST API, gRPC                                          │
│  - Repo Server: Git 클론, 매니페스트 생성                               │
│  - Application Controller: 상태 감시, 동기화                            │
│  - Dex: SSO 인증                                                       │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3. Application 리소스

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: myapp
  namespace: argocd
  finalizers:
    - resources-finalizer.argocd.argoproj.io
spec:
  project: default
  
  # 소스 설정
  source:
    repoURL: https://github.com/user/repo.git
    targetRevision: HEAD  # 또는 특정 브랜치/태그
    path: manifests
    
    # Helm 사용 시
    # helm:
    #   valueFiles:
    #   - values.yaml
    
    # Kustomize 사용 시
    # kustomize:
    #   images:
    #   - name=newimage:tag
  
  # 대상 설정
  destination:
    server: https://kubernetes.default.svc
    namespace: default
  
  # 동기화 정책
  syncPolicy:
    automated:
      prune: true      # 삭제된 리소스 정리
      selfHeal: true   # 수동 변경 복구
    syncOptions:
    - CreateNamespace=true
    - PruneLast=true
    retry:
      limit: 5
      backoff:
        duration: 5s
        factor: 2
        maxDuration: 3m
```

### 4. Sync Status

| Status | 설명 | 의미 |
|--------|------|------|
| **Synced** | Git = K8s | 정상 상태 |
| **OutOfSync** | Git ≠ K8s | 동기화 필요 |
| **Unknown** | 상태 확인 불가 | 연결 문제 |

### 5. Health Status

| Status | 설명 |
|--------|------|
| **Healthy** | 모든 리소스 정상 |
| **Progressing** | 배포 진행 중 |
| **Degraded** | 일부 리소스 문제 |
| **Missing** | 리소스 없음 |
| **Suspended** | 일시 중지 |

---

## 🔑 핵심 명령어 정리

### Application 관리

```bash
# 생성
argocd app create myapp \
  --repo https://github.com/user/repo.git \
  --path manifests \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace default

# 조회
argocd app list
argocd app get myapp

# 동기화
argocd app sync myapp
argocd app sync myapp --revision v1.0.0

# 삭제
argocd app delete myapp
argocd app delete myapp --cascade=false  # K8s 리소스 유지
```

### Sync 관련

```bash
# Auto Sync 설정
argocd app set myapp --sync-policy automated
argocd app set myapp --sync-policy none

# Self-Heal 설정
argocd app set myapp --self-heal

# Prune 설정
argocd app set myapp --auto-prune
```

### 롤백

```bash
# 히스토리 조회
argocd app history myapp

# 롤백
argocd app rollback myapp <ID>

# 특정 Revision으로 Sync
argocd app sync myapp --revision abc1234
```

### 디버깅

```bash
# 상태 상세
argocd app get myapp

# 매니페스트 확인
argocd app manifests myapp

# 리소스 트리
argocd app resources myapp

# 차이점 확인
argocd app diff myapp
```

---

## 📝 App of Apps 패턴

```yaml
# root-app.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: root-app
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/user/repo.git
    path: apps  # 다른 Application YAML들이 있는 디렉토리
    targetRevision: HEAD
  destination:
    server: https://kubernetes.default.svc
    namespace: argocd
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

```
apps/
├── frontend.yaml     # Application for frontend
├── backend.yaml      # Application for backend
├── database.yaml     # Application for database
└── monitoring.yaml   # Application for monitoring
```

---

## 🎯 토스플레이스 요건 매칭

| 요건 | 학습 내용 | Day |
|------|----------|-----|
| **ArgoCD 경험** | 설치, Application, Sync | 91-92 |
| **GitOps** | Auto Sync, Self-Heal | 93 |
| **안전한 배포** | Rollback, 히스토리 | 94 |
| **멀티 환경** | App of Apps, Project | 95-96 |

---

## 💡 면접 대비 Q&A

### Q: "ArgoCD를 왜 사용하나요?"

```
"GitOps CD를 구현하기 위해 사용합니다.

장점:
1. Git이 Single Source of Truth
   - 모든 변경이 Git에 기록되어 추적 가능
   
2. 자동 동기화
   - Git 변경 감지 → 자동 배포
   
3. Self-Heal
   - 수동 변경 자동 복구로 드리프트 방지
   
4. 선언적 관리
   - '어떻게' 배포가 아닌 '무엇'을 배포
   
5. 쉬운 롤백
   - git revert로 즉시 이전 상태 복구"
```

### Q: "ArgoCD vs Flux 차이점은?"

```
"둘 다 GitOps CD 도구이지만:

ArgoCD:
- UI가 강력함
- 멀티 클러스터 관리 용이
- CRD 기반 Application 관리

Flux:
- 더 가볍고 단순
- Git 기반 자동화에 특화
- Helm Controller 분리

저는 UI와 롤백 기능이 강력한 ArgoCD를 선호합니다."
```

---

## ✅ Week 13 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | ArgoCD 설치 및 접속 | ☐ |
| 2 | Application 생성 (CLI/YAML) | ☐ |
| 3 | Auto Sync 설정 | ☐ |
| 4 | Self-Heal 테스트 | ☐ |
| 5 | Rollback 실행 | ☐ |
| 6 | App of Apps 패턴 적용 | ☐ |
| 7 | Project/RBAC 설정 | ☐ |

---

## ➡️ 다음 학습: Day 98

**주제**: Helm 기초

