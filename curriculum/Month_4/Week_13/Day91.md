# 📅 Day 91: GitOps 개념 및 ArgoCD 소개

## 🎯 오늘의 목표

> **토스플레이스 최우선 요건**: "배포 자동화 파이프라인을 운영하고 CI/CD 환경을 개선"
> "ArgoCD를 이용한 CD 경험"은 토스플레이스 기술 스택의 핵심

GitOps 방법론을 이해하고 ArgoCD의 아키텍처와 설치 방법을 학습합니다. GitOps는 Kubernetes 환경에서 가장 널리 채택된 배포 패러다임입니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| GitOps 개념 | 1시간 | 원칙, 장점, 비교 |
| ArgoCD 아키텍처 | 45분 | 컴포넌트, 동작 원리 |
| ArgoCD 설치 | 1.5시간 | 설치, UI, CLI |
| 초기 설정 | 45분 | 비밀번호, 저장소 |

---

## 📚 Part 1: GitOps란? (1시간)

### 1.1 GitOps 정의와 원칙

```
┌─────────────────────────────────────────────────────────────────────┐
│  GitOps = Git + Operations                                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  핵심 원칙 (4가지)                                                   │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  1. Declarative (선언적)                                    │    │
│  │     • 시스템의 원하는 상태를 YAML/코드로 선언              │    │
│  │     • "어떻게" 아닌 "무엇"을 정의                          │    │
│  │                                                             │    │
│  │  2. Versioned and Immutable (버전 관리)                    │    │
│  │     • Git에 저장되어 모든 변경 이력 추적                   │    │
│  │     • 특정 시점으로 롤백 가능                              │    │
│  │                                                             │    │
│  │  3. Pulled Automatically (자동 동기화)                     │    │
│  │     • GitOps 에이전트가 Git 변경 감지                      │    │
│  │     • 자동으로 클러스터에 적용                             │    │
│  │                                                             │    │
│  │  4. Continuously Reconciled (지속적 조정)                  │    │
│  │     • 실제 상태 ≠ 원하는 상태 → 자동 복구                 │    │
│  │     • Self-Healing 구현                                    │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Single Source of Truth = Git Repository                             │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  "Git에 있는 것이 진실이다"                                │    │
│  │  • Git 상태 = 클러스터의 원하는 상태                       │    │
│  │  • 수동 변경(kubectl edit) → 자동으로 원래대로 복구        │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 GitOps 워크플로우

```
┌─────────────────────────────────────────────────────────────────────┐
│  GitOps 워크플로우                                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  개발자                 Git Repo                 ArgoCD             │
│    │                       │                       │                 │
│    │  1. Code 변경         │                       │                 │
│    │ ──────────────────────>                       │                 │
│    │                       │                       │                 │
│    │  2. PR & Review       │                       │                 │
│    │ ──────────────────────>                       │                 │
│    │                       │                       │                 │
│    │  3. Merge to main     │                       │                 │
│    │ ──────────────────────>                       │                 │
│    │                       │                       │                 │
│    │                       │  4. 변경 감지 (Poll/Webhook)           │
│    │                       │ ──────────────────────>                 │
│    │                       │                       │                 │
│    │                       │  5. 상태 비교         │                 │
│    │                       │        Git vs K8s    │                 │
│    │                       │                       │                 │
│    │                       │  6. 차이 있으면 Sync │                 │
│    │                       │ <──────────────────────                 │
│    │                       │                       │                 │
│    │                       │                       ▼                 │
│    │                       │               ┌─────────────┐           │
│    │                       │               │ Kubernetes  │           │
│    │                       │               │   Cluster   │           │
│    │                       │               └─────────────┘           │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.3 Push vs Pull 기반 배포 비교

```
┌─────────────────────────────────────────────────────────────────────┐
│  Push 기반 (전통적 CI/CD)                                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Jenkins/GitHub Actions ──────────────────────> Kubernetes          │
│         (외부에서)          kubectl apply          (내부)           │
│                                                                      │
│  문제점:                                                            │
│  • CI 서버가 K8s 클러스터에 접근 권한 필요 (보안 위험)             │
│  • 클러스터 외부에 kubeconfig 저장 필요                            │
│  • 배포 후 Drift 감지 어려움                                       │
│  • CI 서버 장애 = 배포 불가                                        │
│                                                                      │
├─────────────────────────────────────────────────────────────────────┤
│  Pull 기반 (GitOps)                                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Git Repo <────────────────────────── ArgoCD (클러스터 내부)        │
│               (ArgoCD가 Git을 Pull)                                 │
│                                       │                              │
│                                       ▼                              │
│                                  Kubernetes                          │
│                                                                      │
│  장점:                                                               │
│  • 클러스터 외부에 접근 권한 불필요 (보안 강화)                    │
│  • Git 상태 = 실제 상태 자동 동기화                                │
│  • Drift 자동 감지 및 복구                                         │
│  • 감사 로그 = Git 히스토리                                        │
│  • 롤백 = git revert                                               │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.4 GitOps의 장점 (토스플레이스 관점)

| 장점 | 설명 | 토스플레이스 연관성 |
|------|------|-------------------|
| **감사 추적** | 모든 변경이 Git Commit으로 기록 | 보안 컴플라이언스 |
| **롤백 용이** | git revert로 즉시 이전 상태로 | 장애 대응 신속화 |
| **협업 향상** | PR 기반 배포 리뷰 | 안전한 배포 문화 |
| **Self-Healing** | 실제 상태 자동 복구 | 운영 안정성 |
| **보안 강화** | 클러스터 외부 접근 최소화 | 클라우드 보안 |
| **일관성** | 여러 환경에 동일 설정 | 멀티 클러스터 운영 |

---

## 📐 Part 2: ArgoCD 아키텍처 (45분)

### 2.1 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────────────┐
│  ArgoCD Architecture                                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  External (사용자 접점)                                             │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                                                             │    │
│  │  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │    │
│  │  │   Web UI     │    │   CLI        │    │   CI System  │  │    │
│  │  │  (Browser)   │    │  (argocd)    │    │  (Webhook)   │  │    │
│  │  └──────┬───────┘    └──────┬───────┘    └──────┬───────┘  │    │
│  │         │                   │                   │          │    │
│  └─────────┼───────────────────┼───────────────────┼──────────┘    │
│            │                   │                   │                │
│            ▼                   ▼                   ▼                │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                      API Server                              │    │
│  │  • REST API 제공                                            │    │
│  │  • gRPC API 제공                                            │    │
│  │  • 인증/인가 처리                                           │    │
│  │  • SSO 연동 (OIDC, LDAP, GitHub)                           │    │
│  └───────────────────────────┬─────────────────────────────────┘    │
│                              │                                      │
│  ┌───────────────────────────┼─────────────────────────────────┐    │
│  │                           ▼                                  │    │
│  │  ┌──────────────────────────────────────────────────────┐   │    │
│  │  │              Repo Server                              │   │    │
│  │  │  • Git 저장소 연결                                    │   │    │
│  │  │  • 매니페스트 생성 (Helm, Kustomize, Plain YAML)     │   │    │
│  │  │  • 캐싱으로 성능 최적화                               │   │    │
│  │  └──────────────────────────────────────────────────────┘   │    │
│  │                           │                                  │    │
│  │                           ▼                                  │    │
│  │  ┌──────────────────────────────────────────────────────┐   │    │
│  │  │          Application Controller                       │   │    │
│  │  │  • Application 리소스 감시                            │   │    │
│  │  │  • Git 상태 vs K8s 상태 비교                         │   │    │
│  │  │  • 동기화 실행                                        │   │    │
│  │  │  • Health 상태 평가                                   │   │    │
│  │  └──────────────────────────────────────────────────────┘   │    │
│  │                           │                                  │    │
│  │                           ▼                                  │    │
│  │  ┌──────────────────────────────────────────────────────┐   │    │
│  │  │                  Redis                                │   │    │
│  │  │  • 캐시 저장소                                        │   │    │
│  │  │  • 세션 관리                                          │   │    │
│  │  └──────────────────────────────────────────────────────┘   │    │
│  │                                                              │    │
│  │                    ArgoCD Namespace                          │    │
│  └──────────────────────────────────────────────────────────────┘    │
│                              │                                      │
│                              ▼                                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                   Target Kubernetes Cluster                   │   │
│  │  (in-cluster 또는 external clusters)                         │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 핵심 컴포넌트 설명

| 컴포넌트 | 역할 | 상세 |
|---------|------|------|
| **API Server** | 외부 인터페이스 | Web UI, CLI, Webhook 요청 처리 |
| **Repo Server** | Git 연동 | 저장소에서 매니페스트 읽기, 캐싱 |
| **Application Controller** | 핵심 로직 | 상태 비교, 동기화 실행 |
| **Redis** | 캐시 | 성능 향상, 세션 관리 |
| **Dex (선택)** | SSO | OAuth2/OIDC 인증 연동 |

### 2.3 핵심 개념

```
┌─────────────────────────────────────────────────────────────────────┐
│  ArgoCD Core Concepts                                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Application                                                         │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • ArgoCD가 관리하는 배포 단위                              │    │
│  │  • Source (Git) + Destination (K8s) 정의                   │    │
│  │  • YAML로 선언 가능 (Application CRD)                      │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Project                                                             │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • Application 그룹화                                       │    │
│  │  • 접근 제어 (어떤 Repo, 어떤 Cluster 사용 가능)           │    │
│  │  • 기본: "default" 프로젝트                                │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Sync Status                                                         │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • Synced: Git 상태 = K8s 상태                             │    │
│  │  • OutOfSync: Git 상태 ≠ K8s 상태                          │    │
│  │  • Unknown: 상태 확인 불가                                  │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Health Status                                                       │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • Healthy: 모든 리소스 정상                                │    │
│  │  • Progressing: 배포 진행 중                                │    │
│  │  • Degraded: 일부 리소스 문제                               │    │
│  │  • Suspended: 일시 중지 상태                                │    │
│  │  • Missing: 리소스 없음                                     │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 3: ArgoCD 설치 (1.5시간)

### 실습 1: ArgoCD 설치

```bash
mkdir -p ~/gitops-practice/day91
cd ~/gitops-practice/day91

# 1. Namespace 생성
kubectl create namespace argocd

# 2. ArgoCD 설치 (공식 매니페스트)
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# 3. 설치 확인 (모든 Pod가 Running될 때까지 대기)
echo "=== ArgoCD Pod 상태 확인 ==="
kubectl get pods -n argocd -w

# 예상 출력 (1-2분 후):
# NAME                                               READY   STATUS    RESTARTS   AGE
# argocd-application-controller-xxx                  1/1     Running   0          60s
# argocd-dex-server-xxx                              1/1     Running   0          60s
# argocd-notifications-controller-xxx               1/1     Running   0          60s
# argocd-redis-xxx                                  1/1     Running   0          60s
# argocd-repo-server-xxx                            1/1     Running   0          60s
# argocd-server-xxx                                 1/1     Running   0          60s

# 4. CRD 확인
echo ""
echo "=== ArgoCD CRD 확인 ==="
kubectl get crd | grep argoproj
# applications.argoproj.io
# applicationsets.argoproj.io
# appprojects.argoproj.io
```

### 실습 2: ArgoCD UI 접속

```bash
# 방법 1: Port Forwarding (개발/테스트용)
echo "=== ArgoCD UI 접속 설정 ==="
kubectl port-forward svc/argocd-server -n argocd 8080:443 &
PF_PID=$!
echo "Port Forwarding PID: $PF_PID"
sleep 2

# 초기 admin 비밀번호 확인
echo ""
echo "=== 초기 비밀번호 ==="
ARGOCD_PASSWORD=$(kubectl get secret argocd-initial-admin-secret -n argocd \
  -o jsonpath="{.data.password}" | base64 -d)
echo "Username: admin"
echo "Password: $ARGOCD_PASSWORD"

# 브라우저에서 접속
echo ""
echo "브라우저에서 https://localhost:8080 접속"
echo "(인증서 경고는 무시하고 진행)"

# 테스트 후 Port Forwarding 종료
# kill $PF_PID
```

```bash
# 방법 2: NodePort (Minikube 환경)
kubectl patch svc argocd-server -n argocd -p '{"spec": {"type": "NodePort"}}'

# Minikube에서 접속
minikube service argocd-server -n argocd --url
```

```bash
# 방법 3: LoadBalancer (클라우드 환경)
kubectl patch svc argocd-server -n argocd -p '{"spec": {"type": "LoadBalancer"}}'

# External IP 확인
kubectl get svc argocd-server -n argocd
```

### 실습 3: ArgoCD CLI 설치

```bash
# macOS
brew install argocd

# Linux (amd64)
curl -sSL -o argocd-linux-amd64 https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64
chmod +x argocd-linux-amd64
sudo mv argocd-linux-amd64 /usr/local/bin/argocd

# 버전 확인
argocd version --client

# CLI 로그인
echo "=== ArgoCD CLI 로그인 ==="
argocd login localhost:8080 --insecure \
  --username admin \
  --password $ARGOCD_PASSWORD

# 클러스터 목록 확인
argocd cluster list
```

### 실습 4: 비밀번호 변경 및 보안 설정

```bash
# 비밀번호 변경
echo "=== 비밀번호 변경 ==="
argocd account update-password \
  --current-password $ARGOCD_PASSWORD \
  --new-password "NewSecurePassword123!"

# 초기 비밀번호 Secret 삭제 (보안)
kubectl delete secret argocd-initial-admin-secret -n argocd

# 계정 목록 확인
argocd account list
```

---

## 🛠️ Part 4: 초기 설정 (45분)

### 4.1 Git 저장소 연결

```bash
# 공개 저장소 추가
argocd repo add https://github.com/argoproj/argocd-example-apps.git

# 저장소 목록 확인
argocd repo list

# 비공개 저장소 (SSH 키)
# argocd repo add git@github.com:your-org/private-repo.git \
#   --ssh-private-key-path ~/.ssh/id_rsa

# 비공개 저장소 (HTTPS + Token)
# argocd repo add https://github.com/your-org/private-repo.git \
#   --username <username> \
#   --password <token>
```

### 4.2 프로젝트 설정

```yaml
# project.yaml - 프로젝트 정의
apiVersion: argoproj.io/v1alpha1
kind: AppProject
metadata:
  name: my-project
  namespace: argocd
spec:
  description: "My Application Project"
  
  # 허용된 소스 저장소
  sourceRepos:
  - https://github.com/my-org/*
  - https://github.com/argoproj/argocd-example-apps.git
  
  # 배포 가능한 대상
  destinations:
  - namespace: default
    server: https://kubernetes.default.svc
  - namespace: staging
    server: https://kubernetes.default.svc
  - namespace: production
    server: https://kubernetes.default.svc
  
  # 허용된 클러스터 리소스
  clusterResourceWhitelist:
  - group: ''
    kind: Namespace
  
  # 네임스페이스 리소스 허용
  namespaceResourceWhitelist:
  - group: ''
    kind: '*'
  - group: 'apps'
    kind: '*'
```

```bash
kubectl apply -f project.yaml

# 프로젝트 목록
argocd proj list
```

### 4.3 기본 Application 생성 테스트

```yaml
# guestbook-app.yaml - 샘플 Application
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: guestbook
  namespace: argocd
spec:
  project: default
  
  # Git 소스
  source:
    repoURL: https://github.com/argoproj/argocd-example-apps.git
    targetRevision: HEAD
    path: guestbook
  
  # 배포 대상
  destination:
    server: https://kubernetes.default.svc
    namespace: guestbook
  
  # 동기화 정책
  syncPolicy:
    automated:
      prune: true        # 삭제된 리소스 자동 제거
      selfHeal: true     # 수동 변경 자동 복구
    syncOptions:
    - CreateNamespace=true  # 네임스페이스 자동 생성
```

```bash
# Application 생성
kubectl apply -f guestbook-app.yaml

# 또는 CLI로 생성
argocd app create guestbook \
  --repo https://github.com/argoproj/argocd-example-apps.git \
  --path guestbook \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace guestbook

# Application 목록
argocd app list

# Application 상태 확인
argocd app get guestbook

# 수동 동기화 (auto sync가 아닌 경우)
argocd app sync guestbook

# 배포된 리소스 확인
kubectl get all -n guestbook
```

---

## 📊 Part 5: ArgoCD vs 다른 GitOps 도구

| 기능 | ArgoCD | Flux CD | Jenkins X |
|------|--------|---------|-----------|
| **UI** | ✅ 풍부 | ❌ 없음 | ⚠️ 제한적 |
| **CLI** | ✅ 강력 | ✅ 있음 | ✅ 있음 |
| **멀티 클러스터** | ✅ 네이티브 | ⚠️ 제한적 | ✅ 지원 |
| **Helm 지원** | ✅ 완벽 | ✅ 지원 | ✅ 지원 |
| **Kustomize** | ✅ 완벽 | ✅ 지원 | ⚠️ 제한적 |
| **RBAC** | ✅ 세밀 | ⚠️ 제한적 | ✅ 지원 |
| **학습 곡선** | 중간 | 높음 | 높음 |
| **커뮤니티** | 매우 활발 | 활발 | 활발 |

---

## ✅ 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | GitOps 4가지 원칙 이해 | 선언적, 버전관리, 자동동기화, 지속조정 | ☐ |
| 2 | Push vs Pull 배포 비교 | 장단점 설명 가능 | ☐ |
| 3 | ArgoCD 아키텍처 이해 | 컴포넌트 역할 | ☐ |
| 4 | ArgoCD 설치 완료 | Pod 모두 Running | ☐ |
| 5 | UI 접속 확인 | localhost:8080 | ☐ |
| 6 | CLI 설치 및 로그인 | argocd login | ☐ |
| 7 | 비밀번호 변경 | 초기 비밀번호 삭제 | ☐ |
| 8 | 샘플 Application 배포 | guestbook | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# ArgoCD 설치
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# 초기 비밀번호
kubectl get secret argocd-initial-admin-secret -n argocd \
  -o jsonpath="{.data.password}" | base64 -d

# CLI 로그인
argocd login localhost:8080 --insecure

# Application 관리
argocd app list
argocd app get <app-name>
argocd app sync <app-name>
argocd app delete <app-name>

# 저장소 관리
argocd repo add <url>
argocd repo list
```

---

## 💡 면접 대비 핵심 포인트

### Q1: GitOps란 무엇인가요?

**A**: "GitOps는 Git 저장소를 인프라와 애플리케이션의 Single Source of Truth로 사용하는 방법론입니다. 4가지 원칙이 있습니다: 선언적 정의, 버전 관리, 자동 동기화, 지속적 조정. ArgoCD 같은 도구가 Git 상태를 감시하여 Kubernetes 클러스터를 자동으로 동기화합니다."

### Q2: Push 기반과 Pull 기반 배포의 차이는?

**A**: "Push 기반은 CI 서버에서 kubectl로 직접 클러스터에 배포하므로 외부에 클러스터 접근 권한이 필요합니다. Pull 기반은 클러스터 내부의 ArgoCD가 Git을 감시하여 배포하므로 보안이 강화되고, Drift 자동 감지/복구가 가능합니다."

### Q3: ArgoCD의 주요 컴포넌트는?

**A**: 
- **API Server**: Web UI, CLI, Webhook 요청 처리
- **Repo Server**: Git 저장소에서 매니페스트 읽기
- **Application Controller**: Git vs K8s 상태 비교, 동기화 실행
- **Redis**: 캐시 및 세션 관리

### Q4: Application의 Sync Status와 Health Status 차이는?

**A**: "Sync Status는 Git 상태와 클러스터 상태의 일치 여부(Synced/OutOfSync)를 나타내고, Health Status는 배포된 리소스의 실제 상태(Healthy/Degraded/Progressing)를 나타냅니다."

---

## 📝 학습 기록

```
학습일: ____년 __월 __일
실제 소요 시간: ____시간

실습 완료 항목:
- [ ] ArgoCD 설치
- [ ] UI 접속
- [ ] CLI 설치/로그인
- [ ] 비밀번호 변경
- [ ] 샘플 Application 배포

이해가 어려웠던 부분:


추가 학습 필요 항목:

```

---

## ➡️ 다음 학습: Day 92

**주제**: ArgoCD Application 생성
- Application YAML 구조
- 수동/자동 동기화
- Sync Options
