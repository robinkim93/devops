# 📅 Day 96: ArgoCD Project & RBAC

## 🎯 오늘의 목표

> **토스플레이스 핵심**: AppProject로 앱을 그룹화하고 RBAC으로 팀별 접근을 제어합니다.

---

## ⏰ 예상 학습 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 | 30분 | Project 이해 |
| Project 설정 | 1시간 | 소스/목적지 제한 |
| RBAC | 1시간 | 역할 기반 접근 |
| 실습 | 30분 | 멀티 팀 시나리오 |

---

## 📚 Part 1: AppProject 개념

### Project의 역할

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                      ArgoCD AppProject                                       │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Project: production                                                        │
│   ┌────────────────────────────────────────────────────────────────────────┐│
│   │                                                                        ││
│   │   SourceRepos (허용된 Git)          Destinations (허용된 배포 대상)     ││
│   │   • https://github.com/myorg/*      • namespace: prod-*               ││
│   │   • https://github.com/myorg-helm/* • cluster: production             ││
│   │                                                                        ││
│   │   Applications:                                                        ││
│   │   ┌──────────┐ ┌──────────┐ ┌──────────┐                              ││
│   │   │ api-prod │ │ web-prod │ │ db-prod  │                              ││
│   │   └──────────┘ └──────────┘ └──────────┘                              ││
│   │                                                                        ││
│   │   Roles:                                                               ││
│   │   • admin: 전체 권한                                                   ││
│   │   • developer: get, sync                                               ││
│   │   • viewer: get only                                                   ││
│   │                                                                        ││
│   └────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
│   Project: staging                                                           │
│   ┌────────────────────────────────────────────────────────────────────────┐│
│   │   ...                                                                  ││
│   └────────────────────────────────────────────────────────────────────────┘│
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Project 기능

| 기능 | 설명 |
|------|------|
| **sourceRepos** | 허용된 Git 저장소만 사용 |
| **destinations** | 허용된 클러스터/네임스페이스만 배포 |
| **clusterResourceWhitelist** | 허용된 클러스터 리소스 |
| **namespaceResourceBlacklist** | 금지된 네임스페이스 리소스 |
| **roles** | Project별 RBAC |
| **orphanedResources** | 고아 리소스 모니터링 |

---

## 🛠️ Part 2: AppProject 생성

### 기본 Project

```yaml
apiVersion: argoproj.io/v1alpha1
kind: AppProject
metadata:
  name: production
  namespace: argocd
spec:
  description: Production environment applications
  
  # 허용된 소스 저장소
  sourceRepos:
  - https://github.com/myorg/*         # myorg 조직 전체
  - https://github.com/myorg-helm/*    # Helm Charts
  
  # 허용된 배포 대상
  destinations:
  - namespace: prod-*                   # prod- 접두사 네임스페이스
    server: https://kubernetes.default.svc
  - namespace: production
    server: https://kubernetes.default.svc
  
  # 허용된 클러스터 리소스
  clusterResourceWhitelist:
  - group: ''
    kind: Namespace
  - group: 'rbac.authorization.k8s.io'
    kind: ClusterRole
  - group: 'rbac.authorization.k8s.io'
    kind: ClusterRoleBinding
  
  # 금지된 네임스페이스 리소스
  namespaceResourceBlacklist:
  - group: ''
    kind: ResourceQuota     # ResourceQuota 배포 금지
  - group: ''
    kind: LimitRange        # LimitRange 배포 금지
  
  # 고아 리소스 경고
  orphanedResources:
    warn: true
```

### 개발 환경 Project

```yaml
apiVersion: argoproj.io/v1alpha1
kind: AppProject
metadata:
  name: development
  namespace: argocd
spec:
  description: Development environment
  
  sourceRepos:
  - '*'  # 모든 저장소 허용 (개발 환경)
  
  destinations:
  - namespace: dev-*
    server: https://kubernetes.default.svc
  
  # 모든 리소스 허용
  clusterResourceWhitelist:
  - group: '*'
    kind: '*'
```

---

## 🛠️ Part 3: RBAC 설정

### Project 내 Role 정의

```yaml
apiVersion: argoproj.io/v1alpha1
kind: AppProject
metadata:
  name: production
  namespace: argocd
spec:
  # ... 다른 설정 ...
  
  roles:
  # Admin 역할
  - name: admin
    description: Full admin access
    policies:
    - p, proj:production:admin, applications, *, production/*, allow
    - p, proj:production:admin, repositories, *, production/*, allow
    groups:
    - platform-team        # OIDC 그룹
    - argocd-admins
  
  # Developer 역할
  - name: developer
    description: Can view and sync applications
    policies:
    - p, proj:production:developer, applications, get, production/*, allow
    - p, proj:production:developer, applications, sync, production/*, allow
    - p, proj:production:developer, applications, action/*, production/*, allow
    groups:
    - dev-team
    - backend-team
  
  # Viewer 역할
  - name: viewer
    description: Read-only access
    policies:
    - p, proj:production:viewer, applications, get, production/*, allow
    - p, proj:production:viewer, logs, get, production/*, allow
    groups:
    - all-employees
```

### 정책 문법

```
p, <role/group>, <resource>, <action>, <object>, <allow/deny>
```

| 요소 | 설명 | 예시 |
|------|------|------|
| role/group | 역할 또는 그룹 | `proj:production:developer` |
| resource | 리소스 유형 | `applications`, `repositories` |
| action | 동작 | `get`, `sync`, `create`, `delete`, `*` |
| object | 대상 객체 | `production/*`, `default/myapp` |

### 글로벌 RBAC (argocd-rbac-cm)

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: argocd-rbac-cm
  namespace: argocd
data:
  policy.default: role:readonly
  
  policy.csv: |
    # 기본 역할 정의
    p, role:readonly, applications, get, */*, allow
    p, role:readonly, logs, get, */*, allow
    
    p, role:admin, applications, *, */*, allow
    p, role:admin, clusters, *, *, allow
    p, role:admin, repositories, *, *, allow
    p, role:admin, projects, *, *, allow
    
    # 그룹 매핑
    g, platform-team, role:admin
    g, dev-team, role:readonly
    
    # SSO 그룹 매핑 (예: Okta, OIDC)
    g, okta-admins, role:admin
    g, okta-developers, role:readonly
  
  scopes: '[groups, email]'
```

---

## 🛠️ Part 4: Application에 Project 적용

### Application 정의

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: api-prod
  namespace: argocd
spec:
  project: production    # Project 지정 (필수)
  
  source:
    repoURL: https://github.com/myorg/api.git  # sourceRepos에 있어야 함
    targetRevision: main
    path: manifests/prod
  
  destination:
    server: https://kubernetes.default.svc
    namespace: prod-api   # destinations에 있어야 함
  
  syncPolicy:
    automated:
      selfHeal: true
```

### 검증

```bash
# Project 확인
kubectl get appproject -n argocd

# Project 상세
kubectl describe appproject production -n argocd

# Application이 Project 제한을 준수하는지 확인
argocd app create test-app \
  --project production \
  --repo https://github.com/other-org/repo.git  # 실패: sourceRepos에 없음
```

---

## 🛠️ Part 5: 멀티 팀 시나리오

### 팀별 Project 설계

```yaml
# Team A - Backend
apiVersion: argoproj.io/v1alpha1
kind: AppProject
metadata:
  name: team-a-backend
  namespace: argocd
spec:
  description: Team A Backend Services
  sourceRepos:
  - https://github.com/myorg/backend-*
  destinations:
  - namespace: team-a-*
    server: https://kubernetes.default.svc
  roles:
  - name: developer
    policies:
    - p, proj:team-a-backend:developer, applications, *, team-a-backend/*, allow
    groups:
    - team-a
---
# Team B - Frontend
apiVersion: argoproj.io/v1alpha1
kind: AppProject
metadata:
  name: team-b-frontend
  namespace: argocd
spec:
  description: Team B Frontend Services
  sourceRepos:
  - https://github.com/myorg/frontend-*
  destinations:
  - namespace: team-b-*
    server: https://kubernetes.default.svc
  roles:
  - name: developer
    policies:
    - p, proj:team-b-frontend:developer, applications, *, team-b-frontend/*, allow
    groups:
    - team-b
```

### 권한 분리 검증

```bash
# Team A 개발자가 Team B 앱에 접근 시도
argocd app sync team-b-frontend/web-app --as team-a-user
# Error: permission denied

# Team A 개발자가 Team A 앱 동기화
argocd app sync team-a-backend/api-service --as team-a-user
# 성공
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 확인 방법 | 완료 |
|---|------|----------|------|
| 1 | AppProject 생성 | `kubectl get appproject` | ☐ |
| 2 | sourceRepos 제한 | 허용되지 않은 repo 테스트 | ☐ |
| 3 | destinations 제한 | 허용되지 않은 ns 테스트 | ☐ |
| 4 | Project Role 정의 | roles 섹션 | ☐ |
| 5 | RBAC ConfigMap | argocd-rbac-cm | ☐ |
| 6 | 권한 테스트 | `argocd app sync --as` | ☐ |

---

## 🔑 핵심 설정 요약

```yaml
# AppProject 핵심 구조
apiVersion: argoproj.io/v1alpha1
kind: AppProject
spec:
  sourceRepos:
  - https://github.com/myorg/*
  destinations:
  - namespace: prod-*
    server: https://kubernetes.default.svc
  roles:
  - name: developer
    policies:
    - p, proj:<project>:developer, applications, get, <project>/*, allow
    - p, proj:<project>:developer, applications, sync, <project>/*, allow
    groups:
    - dev-team
```

```bash
# 명령어
kubectl get appproject -n argocd
kubectl describe appproject <name> -n argocd
argocd proj list
argocd proj get <name>
```

---

## ➡️ 다음 학습: Day 97

**주제**: Week 13 복습 - ArgoCD 종합

