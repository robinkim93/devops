# 📅 Day 93: ArgoCD Auto Sync & Self Heal

## 🎯 오늘의 목표

> **토스플레이스 핵심**: GitOps의 핵심인 자동 동기화와 자가 복구를 구현하여 운영 효율성을 극대화합니다.

---

## ⏰ 예상 학습 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 | 45분 | Sync Policy 이해 |
| Auto Sync 실습 | 1시간 | 자동 배포 |
| Self Heal 실습 | 45분 | 자가 복구 |
| 고급 설정 | 30분 | Sync Options |

---

## 📚 Part 1: Sync Policy 개념

### GitOps 동기화 원리

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     GitOps Sync Flow                                         │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   1. Manual Sync (기본)                                                      │
│   ┌─────────┐         ┌─────────┐         ┌─────────┐                       │
│   │   Git   │ ──────▶ │ ArgoCD  │ ──버튼──▶ │   K8s   │                       │
│   │ (변경)  │  감지   │ OutOfSync│  클릭   │ (배포)  │                       │
│   └─────────┘         └─────────┘         └─────────┘                       │
│                                                                              │
│   2. Auto Sync (자동)                                                        │
│   ┌─────────┐         ┌─────────┐         ┌─────────┐                       │
│   │   Git   │ ──────▶ │ ArgoCD  │ ──자동──▶ │   K8s   │                       │
│   │ (변경)  │  감지   │ (자동)  │  동기화  │ (배포)  │                       │
│   └─────────┘         └─────────┘         └─────────┘                       │
│                                                                              │
│   3. Self Heal (K8s 변경 감지)                                               │
│   ┌─────────┐         ┌─────────┐         ┌─────────┐                       │
│   │   Git   │ ◀──────│ ArgoCD  │ ◀──감지──│   K8s   │                       │
│   │ (원본)  │  비교   │ (자동)  │  수동변경│ (변경됨)│                       │
│   └─────────┘         └─────────┘  복구▶  └─────────┘                       │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Sync Policy 옵션

| 옵션 | 설명 | 권장 환경 |
|------|------|----------|
| **automated** | 자동 Sync 활성화 | Dev, Staging |
| **selfHeal** | K8s 수동 변경 시 Git으로 복구 | 모든 환경 |
| **prune** | Git에서 삭제 시 K8s에서도 삭제 | 주의 필요 |

---

## 🛠️ Part 2: Sync Policy 설정

### 전체 옵션

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: myapp
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/myorg/repo.git
    path: manifests
    targetRevision: HEAD
  destination:
    server: https://kubernetes.default.svc
    namespace: default
  
  syncPolicy:
    # 자동 동기화
    automated:
      prune: true        # Git에서 삭제된 리소스를 K8s에서도 삭제
      selfHeal: true     # K8s 수동 변경 시 Git 상태로 복구
      allowEmpty: false  # 빈 디렉토리 허용 안함
    
    # 동기화 옵션
    syncOptions:
    - CreateNamespace=true      # Namespace 자동 생성
    - PruneLast=true            # 삭제는 마지막에 수행
    - ApplyOutOfSyncOnly=true   # 변경된 리소스만 적용
    - PrunePropagationPolicy=foreground  # 삭제 전파 정책
    - Validate=true             # 유효성 검증
    - ServerSideApply=true      # 서버 사이드 적용 (큰 리소스)
    
    # 재시도 설정
    retry:
      limit: 5                  # 최대 재시도 횟수
      backoff:
        duration: 5s            # 초기 대기 시간
        factor: 2               # 대기 시간 증가 배수
        maxDuration: 3m         # 최대 대기 시간
```

### 환경별 권장 설정

```yaml
# Dev 환경: 완전 자동화
syncPolicy:
  automated:
    prune: true
    selfHeal: true
  syncOptions:
  - CreateNamespace=true

---
# Staging 환경: 자동 + 신중한 삭제
syncPolicy:
  automated:
    prune: false    # 삭제는 수동
    selfHeal: true
  syncOptions:
  - CreateNamespace=true
  - PruneLast=true

---
# Production 환경: 수동 Sync + Self Heal만
syncPolicy:
  automated:
    selfHeal: true  # 수동 변경만 복구
    prune: false
  # automated 없이 selfHeal만 설정하려면:
  # automated: {} 로 설정
```

---

## 🛠️ Part 3: Auto Sync 실습

### 실습 환경 준비

```bash
# Application 생성 (Auto Sync 비활성)
argocd app create demo-app \
  --repo https://github.com/myorg/demo-repo.git \
  --path manifests \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace demo

# 초기 동기화
argocd app sync demo-app
```

### Auto Sync 활성화

```bash
# CLI로 Auto Sync 설정
argocd app set demo-app --sync-policy automated

# Self Heal 추가
argocd app set demo-app --self-heal

# Prune 추가
argocd app set demo-app --auto-prune

# 또는 kubectl patch 사용
kubectl patch app demo-app -n argocd --type merge -p '
spec:
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
'
```

### Git 변경 → 자동 배포 확인

```bash
# Git에서 replicas 변경
# manifests/deployment.yaml
# replicas: 2 → 3

git add .
git commit -m "Scale to 3 replicas"
git push

# ArgoCD 상태 확인 (약 3분 내 자동 Sync)
argocd app get demo-app

# Pod 수 확인
kubectl get pods -n demo
# 3개의 Pod 확인

# 이벤트 확인
argocd app history demo-app
```

### Refresh 주기 설정

```yaml
# argocd-cm ConfigMap
apiVersion: v1
kind: ConfigMap
metadata:
  name: argocd-cm
  namespace: argocd
data:
  timeout.reconciliation: 180s  # 기본 3분
```

```bash
# 즉시 Refresh (수동)
argocd app get demo-app --refresh
```

---

## 🛠️ Part 4: Self Heal 실습

### Self Heal 테스트

```bash
# 현재 상태 확인
kubectl get deployment demo-app -n demo
# replicas: 3

# K8s에서 직접 수정 (Git을 거치지 않음)
kubectl scale deployment demo-app -n demo --replicas=1

# 잠시 대기 후 확인
sleep 30
kubectl get deployment demo-app -n demo
# replicas: 3 (Self Heal로 복구됨!)

# ArgoCD 로그 확인
argocd app get demo-app
# Health: Healthy
# Sync: Synced
```

### Self Heal 동작 원리

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     Self Heal 동작                                           │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   1. K8s에서 수동 변경 감지                                                   │
│      kubectl scale deployment myapp --replicas=1                            │
│                                                                              │
│   2. ArgoCD가 Git과 비교                                                     │
│      Git: replicas=3                                                        │
│      K8s: replicas=1                                                        │
│      → OutOfSync 상태                                                        │
│                                                                              │
│   3. selfHeal=true면 자동으로 Git 상태로 복구                                 │
│      K8s: replicas=3 (복구됨)                                                │
│                                                                              │
│   핵심: "Git이 Single Source of Truth"                                       │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 5: Prune 실습

### Prune 테스트

```bash
# 현재 리소스 확인
kubectl get svc demo-app -n demo
# demo-app Service 존재

# Git에서 service.yaml 삭제
rm manifests/service.yaml
git add .
git commit -m "Remove service"
git push

# prune=true면 자동 삭제
# prune=false면 ArgoCD UI에서 Orphaned로 표시

# 확인
kubectl get svc demo-app -n demo
# No resources found (삭제됨)
```

### Prune 주의사항

```yaml
# 특정 리소스 Prune 제외
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: important-data
  annotations:
    argocd.argoproj.io/sync-options: Prune=false
```

---

## 🛠️ Part 6: Sync Options 상세

### 주요 Sync Options

| 옵션 | 설명 | 사용 사례 |
|------|------|----------|
| `CreateNamespace=true` | Namespace 자동 생성 | 새 환경 배포 |
| `PruneLast=true` | 삭제는 마지막에 | 의존성 있는 리소스 |
| `ApplyOutOfSyncOnly=true` | 변경된 것만 적용 | 대규모 배포 |
| `ServerSideApply=true` | 서버 사이드 적용 | CRD 충돌 방지 |
| `SkipDryRunOnMissingResource=true` | CRD 없어도 진행 | CRD + 리소스 동시 배포 |
| `Validate=false` | 유효성 검증 건너뛰기 | 특수 리소스 |

### 리소스별 옵션

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
  annotations:
    # 이 리소스만 특정 옵션 적용
    argocd.argoproj.io/sync-options: Prune=false,SkipDryRunOnMissingResource=true
```

### Sync Wave (순서 제어)

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: myapp
  annotations:
    argocd.argoproj.io/sync-wave: "-1"  # 먼저 생성
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
  annotations:
    argocd.argoproj.io/sync-wave: "0"   # 그 다음
---
apiVersion: v1
kind: Service
metadata:
  name: myapp
  annotations:
    argocd.argoproj.io/sync-wave: "1"   # 마지막
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 확인 방법 | 완료 |
|---|------|----------|------|
| 1 | Auto Sync 설정 | `argocd app set --sync-policy automated` | ☐ |
| 2 | Git 변경 → 자동 배포 | Pod 수 변경 확인 | ☐ |
| 3 | Self Heal 테스트 | `kubectl scale` 후 복구 확인 | ☐ |
| 4 | Prune 테스트 | 리소스 삭제 확인 | ☐ |
| 5 | Sync Options 적용 | `CreateNamespace=true` | ☐ |
| 6 | Sync Wave 설정 | 순서대로 배포 확인 | ☐ |

---

## 🔑 핵심 설정 요약

```yaml
# 기본 권장 설정
syncPolicy:
  automated:
    prune: true
    selfHeal: true
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

```bash
# CLI 명령어
argocd app set <app> --sync-policy automated
argocd app set <app> --self-heal
argocd app set <app> --auto-prune
argocd app get <app> --refresh
```

---

## ➡️ 다음 학습: Day 94

**주제**: ArgoCD Rollback

