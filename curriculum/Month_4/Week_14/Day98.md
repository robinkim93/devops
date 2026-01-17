# 📅 Day 98: Helm 기초

## 🎯 오늘의 목표

> **토스플레이스 연결점**: Kubernetes 애플리케이션 패키지 관리
> Helm을 사용하여 복잡한 애플리케이션을 효율적으로 배포하고 관리

토스플레이스는 Kubernetes 환경에서 다양한 서비스를 운영합니다. Helm은 복잡한 애플리케이션 배포를 표준화하고 자동화하는 핵심 도구입니다.

---

## ⏰ 예상 학습 시간: 4시간

---

## 📚 Part 1: Helm이란? (1시간)

### 1.1 Helm 개념

Helm은 Kubernetes의 패키지 매니저입니다. apt, yum, npm처럼 애플리케이션을 쉽게 설치, 업그레이드, 삭제할 수 있습니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│  Helm 개념도                                                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  기존 방식 (Helm 없이):                                             │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  kubectl apply -f deployment.yaml                           │    │
│  │  kubectl apply -f service.yaml                              │    │
│  │  kubectl apply -f configmap.yaml                            │    │
│  │  kubectl apply -f secret.yaml                               │    │
│  │  kubectl apply -f ingress.yaml                              │    │
│  │  ... (매번 수동으로 여러 파일 관리)                          │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Helm 사용:                                                         │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  helm install my-app ./my-chart                             │    │
│  │  → 모든 리소스를 한 번에 배포                               │    │
│  │  → 버전 관리, 롤백 자동 지원                                │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  핵심 용어:                                                         │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  Chart    = 패키지 (YAML 템플릿 + 기본값)                   │   │
│  │  Release  = Chart의 설치된 인스턴스                         │   │
│  │  Repository = Chart 저장소 (공개/비공개)                    │   │
│  │  Values   = 사용자 정의 설정 값                             │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 Helm의 장점

| 장점 | 설명 | 토스플레이스 활용 |
|------|------|------------------|
| **템플릿화** | 환경별 설정 분리 (dev/staging/prod) | 환경별 values 파일로 관리 |
| **재사용** | Chart 공유 및 재사용 | 내부 Chart Repository 운영 |
| **버전 관리** | Release 히스토리 추적 | 배포 이력 관리 |
| **롤백** | 이전 버전으로 쉽게 복원 | 장애 시 빠른 복구 |
| **의존성 관리** | 다른 Chart 포함 가능 | 복잡한 앱 구조 관리 |
| **원자적 배포** | 모든 리소스 함께 배포 | 일관된 상태 보장 |

### 1.3 Helm 아키텍처

```
┌─────────────────────────────────────────────────────────────────────┐
│  Helm 3 아키텍처                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────┐     ┌─────────────────┐     ┌──────────────────┐   │
│  │   Helm CLI  │ ──▶ │  Kubernetes API │ ──▶ │  K8s Resources   │   │
│  └─────────────┘     └─────────────────┘     └──────────────────┘   │
│        │                                                             │
│        │                                                             │
│        ▼                                                             │
│  ┌─────────────┐                                                    │
│  │  Chart      │                                                    │
│  │  Repository │                                                    │
│  └─────────────┘                                                    │
│                                                                      │
│  Helm 2 vs Helm 3:                                                  │
│  - Helm 2: Tiller 서버 필요 (클러스터 내)                           │
│  - Helm 3: Tiller 제거, 직접 K8s API 호출 (더 안전)                 │
│                                                                      │
│  Release 정보 저장:                                                 │
│  - Helm 2: Tiller의 ConfigMap                                       │
│  - Helm 3: 각 Namespace의 Secret                                    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: Helm 설치 및 기본 사용 (1.5시간)

### 2.1 Helm 설치

```bash
# macOS (Homebrew)
brew install helm

# Linux (스크립트)
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Linux (APT - Debian/Ubuntu)
curl https://baltocdn.com/helm/signing.asc | gpg --dearmor | sudo tee /usr/share/keyrings/helm.gpg > /dev/null
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/helm.gpg] https://baltocdn.com/helm/stable/debian/ all main" | sudo tee /etc/apt/sources.list.d/helm-stable-debian.list
sudo apt-get update
sudo apt-get install helm

# 버전 확인
helm version
# version.BuildInfo{Version:"v3.14.0", ...}
```

### 2.2 Repository 관리

```bash
# 인기 Repository 추가
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo add jetstack https://charts.jetstack.io
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx

# Repository 목록 확인
helm repo list

# Repository 업데이트 (최신 Chart 정보 동기화)
helm repo update

# Repository 삭제
helm repo remove bitnami

# Chart 검색 (Repository에서)
helm search repo nginx
helm search repo prometheus

# Chart 검색 (Artifact Hub - 공개 Hub)
helm search hub nginx
```

### 2.3 Chart 설치

```bash
# 기본 설치
helm install my-nginx bitnami/nginx
# my-nginx: Release 이름
# bitnami/nginx: Repository/Chart 이름

# 네임스페이스 지정
helm install my-nginx bitnami/nginx -n web-app --create-namespace

# 설치 확인
helm list
helm list -A  # 모든 네임스페이스

# Release 상태 확인
helm status my-nginx

# 설치된 리소스 확인
kubectl get all -l app.kubernetes.io/instance=my-nginx
```

### 2.4 Values 커스터마이징

```bash
# 기본 values 확인
helm show values bitnami/nginx | head -100

# values.yaml 파일 생성
cat << 'EOF' > nginx-values.yaml
# 레플리카 수
replicaCount: 3

# 서비스 타입
service:
  type: ClusterIP
  port: 80

# 리소스 제한
resources:
  requests:
    memory: "128Mi"
    cpu: "100m"
  limits:
    memory: "256Mi"
    cpu: "200m"

# Ingress 설정
ingress:
  enabled: true
  hostname: nginx.local
  annotations:
    kubernetes.io/ingress.class: nginx

# Pod 보안 컨텍스트
podSecurityContext:
  fsGroup: 1001

# 컨테이너 보안 컨텍스트
containerSecurityContext:
  runAsUser: 1001
  runAsNonRoot: true
EOF

# values 파일로 설치
helm install my-nginx bitnami/nginx -f nginx-values.yaml

# 여러 values 파일 사용 (후자가 우선)
helm install my-nginx bitnami/nginx \
  -f values-common.yaml \
  -f values-prod.yaml

# --set으로 개별 값 지정
helm install my-nginx bitnami/nginx \
  --set replicaCount=3 \
  --set service.type=ClusterIP \
  --set resources.requests.memory=128Mi

# --set과 -f 혼합 사용 (--set이 우선)
helm install my-nginx bitnami/nginx \
  -f nginx-values.yaml \
  --set replicaCount=5
```

### 2.5 업그레이드 & 롤백

```bash
# 설정 변경 후 업그레이드
helm upgrade my-nginx bitnami/nginx -f nginx-values.yaml

# 값 변경만 (--reuse-values)
helm upgrade my-nginx bitnami/nginx --set replicaCount=5 --reuse-values

# Chart 버전 업그레이드
helm upgrade my-nginx bitnami/nginx --version 15.0.0

# 히스토리 확인
helm history my-nginx

# 출력 예시:
# REVISION  STATUS      CHART         DESCRIPTION
# 1         superseded  nginx-14.0.0  Install complete
# 2         deployed    nginx-15.0.0  Upgrade complete

# 롤백 (이전 리비전으로)
helm rollback my-nginx 1

# 롤백 확인
helm history my-nginx

# Release 삭제
helm uninstall my-nginx

# 삭제 시 히스토리 유지
helm uninstall my-nginx --keep-history
```

---

## 🛠️ Part 3: Chart 구조 이해 (1시간)

### 3.1 Chart 디렉토리 구조

```bash
# Chart 생성
helm create myapp

# 생성된 구조
myapp/
├── Chart.yaml          # Chart 메타데이터
├── values.yaml         # 기본 설정 값
├── charts/             # 의존성 Chart
├── templates/          # K8s 매니페스트 템플릿
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── serviceaccount.yaml
│   ├── ingress.yaml
│   ├── hpa.yaml
│   ├── NOTES.txt       # 설치 후 메시지
│   ├── _helpers.tpl    # 템플릿 헬퍼 함수
│   └── tests/          # Chart 테스트
│       └── test-connection.yaml
└── .helmignore         # 패키징 제외 파일
```

### 3.2 Chart.yaml

```yaml
# Chart.yaml
apiVersion: v2          # Helm 3
name: myapp
description: My Application Chart
type: application       # application 또는 library
version: 1.0.0          # Chart 버전
appVersion: "2.0.0"     # 애플리케이션 버전

# 메인테이너 정보
maintainers:
  - name: DevOps Team
    email: devops@tossplace.com

# 의존성
dependencies:
  - name: redis
    version: "17.x.x"
    repository: https://charts.bitnami.com/bitnami
    condition: redis.enabled

# 키워드 (검색용)
keywords:
  - payment
  - api
  - tossplace

# 홈페이지
home: https://github.com/tossplace/myapp

# 소스 코드
sources:
  - https://github.com/tossplace/myapp
```

### 3.3 values.yaml

```yaml
# values.yaml - 기본값 정의
# 이미지 설정
image:
  repository: nginx
  tag: "1.25"
  pullPolicy: IfNotPresent

# 레플리카 수
replicaCount: 1

# 서비스 계정
serviceAccount:
  create: true
  name: ""
  annotations: {}

# 서비스
service:
  type: ClusterIP
  port: 80

# Ingress
ingress:
  enabled: false
  className: nginx
  annotations: {}
  hosts:
    - host: chart-example.local
      paths:
        - path: /
          pathType: Prefix
  tls: []

# 리소스
resources:
  requests:
    cpu: 100m
    memory: 128Mi
  limits:
    cpu: 200m
    memory: 256Mi

# Probe
livenessProbe:
  httpGet:
    path: /
    port: http
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /
    port: http
  initialDelaySeconds: 5
  periodSeconds: 10

# HPA
autoscaling:
  enabled: false
  minReplicas: 1
  maxReplicas: 10
  targetCPUUtilizationPercentage: 80

# Node 선택
nodeSelector: {}
tolerations: []
affinity: {}

# 환경 변수
env: []

# ConfigMap 데이터
config:
  APP_ENV: production
  LOG_LEVEL: info

# Secret (실제 값은 여기에 넣지 않음!)
secretName: ""
```

### 3.4 템플릿 기본

```yaml
# templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "myapp.fullname" . }}
  labels:
    {{- include "myapp.labels" . | nindent 4 }}
spec:
  {{- if not .Values.autoscaling.enabled }}
  replicas: {{ .Values.replicaCount }}
  {{- end }}
  selector:
    matchLabels:
      {{- include "myapp.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      labels:
        {{- include "myapp.selectorLabels" . | nindent 8 }}
    spec:
      containers:
        - name: {{ .Chart.Name }}
          image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
          imagePullPolicy: {{ .Values.image.pullPolicy }}
          ports:
            - name: http
              containerPort: 80
              protocol: TCP
          {{- with .Values.resources }}
          resources:
            {{- toYaml . | nindent 12 }}
          {{- end }}
```

---

## 🔧 Part 4: 실무 활용 (30분)

### 4.1 환경별 배포

```bash
# values 파일 구성
values/
├── values-common.yaml   # 공통 설정
├── values-dev.yaml      # 개발 환경
├── values-staging.yaml  # 스테이징 환경
└── values-prod.yaml     # 프로덕션 환경

# 개발 환경 배포
helm install myapp ./myapp \
  -f values/values-common.yaml \
  -f values/values-dev.yaml \
  -n dev

# 프로덕션 환경 배포
helm install myapp ./myapp \
  -f values/values-common.yaml \
  -f values/values-prod.yaml \
  -n prod
```

### 4.2 Chart 검증

```bash
# 문법 검사
helm lint ./myapp

# 템플릿 렌더링 (실제 적용 전 확인)
helm template myapp ./myapp -f values.yaml

# 특정 템플릿만 확인
helm template myapp ./myapp -s templates/deployment.yaml

# Dry-run (서버 검증)
helm install myapp ./myapp --dry-run --debug
```

### 4.3 유용한 명령어

```bash
# 설치된 values 확인
helm get values my-nginx

# 모든 values 확인 (computed)
helm get values my-nginx -a

# 매니페스트 확인
helm get manifest my-nginx

# Release 정보 (Notes 포함)
helm get notes my-nginx

# Chart 다운로드
helm pull bitnami/nginx
helm pull bitnami/nginx --untar

# Chart 패키징
helm package ./myapp
# 출력: myapp-1.0.0.tgz
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | Helm 설치 | ☐ |
| 2 | Repository 추가 | ☐ |
| 3 | Chart 설치 | ☐ |
| 4 | values 커스터마이징 | ☐ |
| 5 | 업그레이드/롤백 | ☐ |
| 6 | Chart 구조 이해 | ☐ |
| 7 | helm template으로 검증 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# Repository
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
helm search repo nginx

# 설치/관리
helm install my-nginx bitnami/nginx -f values.yaml
helm upgrade my-nginx bitnami/nginx -f values.yaml
helm rollback my-nginx 1
helm uninstall my-nginx

# 확인
helm list
helm status my-nginx
helm history my-nginx

# 디버깅
helm template my-nginx ./chart -f values.yaml
helm lint ./chart
```

---

## 💡 면접 대비 핵심 포인트

### Q1: Helm이란?
**A**: Kubernetes의 패키지 매니저로, 복잡한 애플리케이션을 Chart로 패키징하여 쉽게 배포, 업그레이드, 롤백할 수 있게 해주는 도구입니다.

### Q2: Chart, Release, Repository의 차이?
**A**:
- **Chart**: 패키지 (템플릿 + 기본값)
- **Release**: Chart의 설치된 인스턴스
- **Repository**: Chart 저장소

### Q3: Helm 2와 3의 차이?
**A**: Helm 3에서 Tiller 서버가 제거되어 보안이 향상되었고, Release 정보가 각 Namespace의 Secret에 저장됩니다.

---

## ➡️ 다음 학습: Day 99

**주제**: Helm Chart 작성
- 커스텀 Chart 생성
- 템플릿 함수 활용
- 의존성 관리
