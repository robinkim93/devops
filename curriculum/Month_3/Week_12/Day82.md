# 📅 Day 82: Month 3 프로젝트 - Istio Service Mesh 설계

## 🎯 포트폴리오 프로젝트 #3

> **토스플레이스 연결점**: "Kubernetes와 Service Mesh에 대한 경험"

Production-Ready Istio Service Mesh를 설계하고 구현합니다.

---

## ⏰ 예상 소요 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 설계 | 1.5시간 | 아키텍처 설계 |
| 구조화 | 1시간 | 디렉토리 및 매니페스트 구조 |
| 기반 작업 | 1시간 | Namespace, 기본 설정 |
| 문서화 | 30분 | 설계 문서 작성 |

---

## 📋 프로젝트 개요

### 비즈니스 시나리오

```
┌─────────────────────────────────────────────────────────────┐
│  시나리오: 토스플레이스 결제 시스템 (간소화 버전)           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  서비스 구성:                                               │
│  - Frontend: 결제 단말 UI                                   │
│  - API Gateway: 요청 라우팅                                 │
│  - Payment API v1: 기존 결제 로직                           │
│  - Payment API v2: 신규 결제 로직 (카나리 배포 대상)        │
│  - Transaction DB: 거래 데이터                              │
│                                                             │
│  요구사항:                                                  │
│  ✓ 카나리 배포로 안전한 v2 전환                            │
│  ✓ mTLS로 서비스 간 통신 암호화                            │
│  ✓ Zero Trust 네트워크 보안                                │
│  ✓ 완전한 Observability (메트릭, 트레이싱, 로깅)           │
│  ✓ Rate Limiting으로 과부하 방지                           │
│  ✓ Circuit Breaker로 장애 격리                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────┐
│  Production-Ready Istio Service Mesh                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                 Istio Ingress Gateway                │   │
│  │                 (TLS Termination)                    │   │
│  └───────────────────────┬─────────────────────────────┘   │
│                          │                                  │
│                          ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              API Gateway (Envoy)                     │   │
│  │              - Rate Limiting                         │   │
│  │              - JWT Validation                        │   │
│  └───────────────────────┬─────────────────────────────┘   │
│                          │                                  │
│         ┌────────────────┼────────────────┐                │
│         │                │                │                │
│         ▼                ▼                ▼                │
│    ┌─────────┐     ┌─────────┐     ┌─────────┐            │
│    │Frontend │     │ API v1  │     │ API v2  │            │
│    │         │     │  (90%)  │     │  (10%)  │            │
│    └────┬────┘     └────┬────┘     └────┬────┘            │
│         │               │               │                  │
│         └───────────────┼───────────────┘                  │
│                         │                                   │
│                    ┌────┴────┐                             │
│                    │   DB    │                             │
│                    │ (MySQL) │                             │
│                    └─────────┘                             │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Observability Stack                     │   │
│  │  Prometheus │ Grafana │ Jaeger │ Kiali              │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  구현 내용:                                                 │
│  ✓ 카나리 배포 (API v1 90% → v2 10%)                       │
│  ✓ mTLS (STRICT mode)                                      │
│  ✓ AuthorizationPolicy (Zero Trust)                        │
│  ✓ Circuit Breaker & Retry                                 │
│  ✓ Rate Limiting                                           │
│  ✓ Distributed Tracing                                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 프로젝트 일정

| Day | 작업 | 핵심 내용 |
|-----|------|----------|
| 82 | 아키텍처 설계 | 디렉토리 구조, Namespace |
| 83 | 앱 배포 + Istio | Deployment, Service, Sidecar Injection |
| 84 | 트래픽 관리 | VirtualService, 카나리 배포 |
| 85 | 보안 설정 | mTLS, AuthorizationPolicy |
| 86 | 관찰성 구축 | Kiali, Jaeger, Grafana 연동 |
| 87 | 장애 복원력 | Circuit Breaker, Retry, Timeout |
| 88 | 문서화 | README, 아키텍처 문서 |
| 89 | 테스트 | 시나리오 테스트, 부하 테스트 |
| 90 | 완료 | GitHub 업로드, Month 3 정리 |

---

## 🛠️ Part 1: 디렉토리 구조 설계

### 프로젝트 구조

```bash
# 프로젝트 디렉토리 생성
mkdir -p ~/portfolio/istio-project
cd ~/portfolio/istio-project

# 전체 구조 생성
mkdir -p {app/{frontend,api-v1,api-v2,database},manifests/{base,istio,observability},docs,scripts,tests}
```

```
istio-project/
├── README.md                      # 프로젝트 소개
├── app/                           # 애플리케이션 소스
│   ├── frontend/
│   │   ├── Dockerfile
│   │   ├── nginx.conf
│   │   └── html/
│   ├── api-v1/
│   │   ├── Dockerfile
│   │   ├── main.go
│   │   └── go.mod
│   ├── api-v2/
│   │   ├── Dockerfile
│   │   ├── main.go
│   │   └── go.mod
│   └── database/
│       └── init.sql
│
├── manifests/                     # Kubernetes 매니페스트
│   ├── base/                      # 기본 리소스
│   │   ├── namespace.yaml
│   │   ├── configmap.yaml
│   │   ├── secret.yaml
│   │   └── deployments/
│   │       ├── frontend.yaml
│   │       ├── api-v1.yaml
│   │       ├── api-v2.yaml
│   │       └── database.yaml
│   │
│   ├── istio/                     # Istio 설정
│   │   ├── gateway.yaml
│   │   ├── virtualservice.yaml
│   │   ├── destinationrule.yaml
│   │   ├── peerauthentication.yaml
│   │   ├── authorizationpolicy.yaml
│   │   └── envoyfilter.yaml
│   │
│   └── observability/             # 모니터링 설정
│       ├── telemetry.yaml
│       └── servicemonitor.yaml
│
├── docs/                          # 문서
│   ├── architecture.md
│   ├── canary-deployment.md
│   ├── security.md
│   └── troubleshooting.md
│
├── scripts/                       # 유틸리티 스크립트
│   ├── setup.sh
│   ├── deploy.sh
│   ├── canary-promote.sh
│   └── cleanup.sh
│
└── tests/                         # 테스트
    ├── load-test.sh
    ├── canary-test.sh
    └── security-test.sh
```

---

## 🛠️ Part 2: 기본 매니페스트 작성

### Namespace 정의

```yaml
# manifests/base/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: istio-portfolio
  labels:
    istio-injection: enabled    # 사이드카 자동 주입
    project: istio-portfolio
    environment: production
  annotations:
    owner: "devops-team"
    description: "Istio Service Mesh Portfolio Project"
```

### ConfigMap 정의

```yaml
# manifests/base/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: istio-portfolio
data:
  # 애플리케이션 설정
  APP_ENV: "production"
  LOG_LEVEL: "info"
  LOG_FORMAT: "json"
  
  # 데이터베이스 설정
  DB_HOST: "database"
  DB_PORT: "3306"
  DB_NAME: "payments"
  
  # 서비스 URL
  API_V1_URL: "http://api-v1:8080"
  API_V2_URL: "http://api-v2:8080"
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: frontend-config
  namespace: istio-portfolio
data:
  nginx.conf: |
    server {
        listen 80;
        server_name _;
        
        location / {
            root /usr/share/nginx/html;
            index index.html;
            try_files $uri $uri/ /index.html;
        }
        
        location /health {
            return 200 'OK';
            add_header Content-Type text/plain;
        }
        
        location /api/ {
            proxy_pass http://api-gateway:8080/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Request-ID $request_id;
        }
    }
```

### Secret 정의

```yaml
# manifests/base/secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
  namespace: istio-portfolio
type: Opaque
data:
  # echo -n 'your-db-password' | base64
  DB_PASSWORD: cGFzc3dvcmQxMjM=
  # echo -n 'your-api-key' | base64
  API_KEY: YXBpLWtleS1zZWNyZXQ=
---
apiVersion: v1
kind: Secret
metadata:
  name: db-credentials
  namespace: istio-portfolio
type: Opaque
data:
  MYSQL_ROOT_PASSWORD: cm9vdHBhc3N3b3Jk
  MYSQL_USER: YXBwdXNlcg==
  MYSQL_PASSWORD: YXBwcGFzc3dvcmQ=
```

### 기본 Deployment 템플릿

```yaml
# manifests/base/deployments/api-v1.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-v1
  namespace: istio-portfolio
  labels:
    app: api
    version: v1
spec:
  replicas: 2
  selector:
    matchLabels:
      app: api
      version: v1
  template:
    metadata:
      labels:
        app: api
        version: v1
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/metrics"
    spec:
      containers:
      - name: api
        image: api:v1
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: VERSION
          value: "v1"
        - name: DB_HOST
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: DB_HOST
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: DB_PASSWORD
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "200m"
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /ready
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5
        securityContext:
          runAsNonRoot: true
          runAsUser: 1000
          readOnlyRootFilesystem: true
          allowPrivilegeEscalation: false
      volumes:
      - name: tmp
        emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: api
  namespace: istio-portfolio
  labels:
    app: api
spec:
  ports:
  - port: 8080
    targetPort: 8080
    name: http
  selector:
    app: api
```

---

## 🛠️ Part 3: Istio 기본 설정

### Gateway 정의

```yaml
# manifests/istio/gateway.yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: main-gateway
  namespace: istio-portfolio
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - "payment.portfolio.local"
    - "*.portfolio.local"
  # TLS 설정 (프로덕션용)
  # - port:
  #     number: 443
  #     name: https
  #     protocol: HTTPS
  #   tls:
  #     mode: SIMPLE
  #     credentialName: payment-cert
  #   hosts:
  #   - "payment.portfolio.local"
```

### DestinationRule 정의

```yaml
# manifests/istio/destinationrule.yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: api-destination
  namespace: istio-portfolio
spec:
  host: api
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        h2UpgradePolicy: UPGRADE
        http1MaxPendingRequests: 100
        http2MaxRequests: 1000
    outlierDetection:
      consecutive5xxErrors: 5
      interval: 10s
      baseEjectionTime: 30s
      maxEjectionPercent: 50
  subsets:
  - name: v1
    labels:
      version: v1
  - name: v2
    labels:
      version: v2
```

---

## 🛠️ Part 4: 설정 스크립트

### 배포 스크립트

```bash
# scripts/deploy.sh
#!/bin/bash
set -e

NAMESPACE="istio-portfolio"

echo "🚀 Istio Portfolio 배포 시작..."

# 1. Namespace 생성
echo "📦 Namespace 생성..."
kubectl apply -f manifests/base/namespace.yaml

# Istio 사이드카 주입 확인
kubectl get namespace $NAMESPACE -L istio-injection

# 2. ConfigMap & Secret
echo "⚙️ ConfigMap & Secret 적용..."
kubectl apply -f manifests/base/configmap.yaml
kubectl apply -f manifests/base/secret.yaml

# 3. 애플리케이션 배포
echo "🐳 애플리케이션 배포..."
kubectl apply -f manifests/base/deployments/

# 4. Istio 설정
echo "🌐 Istio 설정 적용..."
kubectl apply -f manifests/istio/

# 5. 배포 상태 확인
echo "✅ 배포 상태 확인..."
kubectl get pods -n $NAMESPACE
kubectl get svc -n $NAMESPACE
kubectl get virtualservice -n $NAMESPACE
kubectl get destinationrule -n $NAMESPACE

echo "🎉 배포 완료!"
```

### 정리 스크립트

```bash
# scripts/cleanup.sh
#!/bin/bash
set -e

NAMESPACE="istio-portfolio"

echo "🧹 리소스 정리 시작..."

kubectl delete -f manifests/istio/ --ignore-not-found
kubectl delete -f manifests/base/deployments/ --ignore-not-found
kubectl delete -f manifests/base/configmap.yaml --ignore-not-found
kubectl delete -f manifests/base/secret.yaml --ignore-not-found
kubectl delete namespace $NAMESPACE --ignore-not-found

echo "✅ 정리 완료!"
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | 프로젝트 아키텍처 설계 | ☐ |
| 2 | 디렉토리 구조 생성 | ☐ |
| 3 | Namespace YAML 작성 | ☐ |
| 4 | ConfigMap/Secret 작성 | ☐ |
| 5 | 기본 Deployment 템플릿 작성 | ☐ |
| 6 | Gateway/DestinationRule 기본 설정 | ☐ |
| 7 | 배포 스크립트 작성 | ☐ |

---

## 📝 설계 결정 사항

### 왜 이 구조인가?

```
1. 관심사 분리
   - app/: 애플리케이션 코드
   - manifests/: 인프라 코드
   - docs/: 문서
   
2. 환경별 구성 용이
   - base/: 공통 설정
   - istio/: 네트워크 정책
   - 환경별 오버레이 가능 (Kustomize)
   
3. GitOps 친화적
   - 선언적 매니페스트
   - 버전 관리 가능
   - ArgoCD 연동 용이
```

---

## ➡️ 다음 학습: Day 83

**주제**: 앱 배포 + Istio 활성화
- 실제 애플리케이션 빌드
- Deployment/Service 배포
- Sidecar Injection 확인
- 기본 통신 테스트
