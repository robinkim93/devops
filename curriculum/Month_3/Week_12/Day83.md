# 📅 Day 83: 앱 배포 + Istio 활성화

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "Kubernetes와 Service Mesh에 대한 경험"
> 애플리케이션 배포 및 Sidecar 주입 확인

Istio Service Mesh 환경에서 애플리케이션을 배포하고 자동 Sidecar 주입이 정상적으로 동작하는지 확인합니다.

---

## ⏰ 예상 소요 시간: 4시간

---

## 📚 Part 1: Istio Sidecar 이해 (1시간)

### 1.1 Sidecar 패턴이란?

Sidecar는 메인 애플리케이션 옆에 배치되어 보조 기능을 제공하는 컨테이너입니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│  Istio Sidecar 아키텍처                                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Pod (논리적 호스트)                                                │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                                                               │  │
│  │  ┌─────────────────┐    ┌─────────────────┐                 │  │
│  │  │   Application   │    │  Envoy Sidecar  │                 │  │
│  │  │   Container     │◀──▶│   Container     │◀──▶ [다른 Pod] │  │
│  │  │                 │    │                 │                 │  │
│  │  │  - 비즈니스 로직│    │  - 트래픽 제어  │                 │  │
│  │  │  - 포트 8080    │    │  - mTLS        │                 │  │
│  │  │                 │    │  - 메트릭 수집  │                 │  │
│  │  └─────────────────┘    └─────────────────┘                 │  │
│  │                                                               │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  트래픽 흐름:                                                       │
│  1. 외부 요청 → Envoy Sidecar (인바운드)                           │
│  2. Envoy → Application Container (로컬)                           │
│  3. Application → Envoy Sidecar (아웃바운드)                       │
│  4. Envoy → 다른 서비스 (mTLS 암호화)                              │
│                                                                      │
│  Sidecar가 처리하는 것:                                             │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  ✓ 트래픽 라우팅 (VirtualService, DestinationRule)         │    │
│  │  ✓ 보안 (mTLS, AuthorizationPolicy)                        │    │
│  │  ✓ 관찰성 (메트릭, 트레이싱, 로깅)                          │    │
│  │  ✓ 복원력 (재시도, 타임아웃, 서킷브레이커)                  │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 Sidecar 주입 방식

| 방식 | 설명 | 사용 시점 |
|------|------|----------|
| **자동 주입** | Namespace 레이블로 자동 | 대부분의 경우 권장 |
| **수동 주입** | istioctl kube-inject | 특정 Pod만 적용 |
| **주입 제외** | annotation으로 제외 | 특수 워크로드 |

```bash
# 자동 주입 활성화 (Namespace 레이블)
kubectl label namespace istio-portfolio istio-injection=enabled

# 자동 주입 상태 확인
kubectl get namespace -L istio-injection

# 수동 주입
istioctl kube-inject -f deployment.yaml | kubectl apply -f -

# 주입 제외 (Pod annotation)
# metadata:
#   annotations:
#     sidecar.istio.io/inject: "false"
```

### 1.3 Sidecar 구성 확인

```bash
# Pod의 컨테이너 수 확인 (2개면 Sidecar 포함)
kubectl get pods -n istio-portfolio

# NAME              READY   STATUS    
# api-v1-xxx        2/2     Running   # 2/2 = app + sidecar
# api-v2-xxx        2/2     Running

# Pod 상세 정보에서 컨테이너 확인
kubectl describe pod <pod-name> -n istio-portfolio | grep -A5 "Containers:"

# Envoy 설정 확인
istioctl proxy-config cluster <pod-name> -n istio-portfolio
```

---

## 🛠️ Part 2: 애플리케이션 배포 (1.5시간)

### 2.1 디렉토리 구조

```bash
# 프로젝트 구조
mkdir -p ~/istio-portfolio/manifests/base/deployments
cd ~/istio-portfolio
```

### 2.2 ServiceAccount 생성

```yaml
# manifests/base/serviceaccounts.yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: api
  namespace: istio-portfolio
  labels:
    app: api
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: frontend
  namespace: istio-portfolio
  labels:
    app: frontend
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: database
  namespace: istio-portfolio
  labels:
    app: database
```

### 2.3 API v1 Deployment

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
        # Prometheus 메트릭 스크래핑
        prometheus.io/scrape: "true"
        prometheus.io/port: "15020"
        prometheus.io/path: "/stats/prometheus"
    spec:
      serviceAccountName: api
      containers:
      - name: api
        image: hashicorp/http-echo:latest
        args: ["-text=API v1 - 안정 버전"]
        ports:
        - containerPort: 5678
          name: http
          protocol: TCP
        resources:
          requests:
            cpu: "100m"
            memory: "64Mi"
          limits:
            cpu: "200m"
            memory: "128Mi"
        # Readiness Probe
        readinessProbe:
          httpGet:
            path: /
            port: 5678
          initialDelaySeconds: 5
          periodSeconds: 10
        # Liveness Probe
        livenessProbe:
          httpGet:
            path: /
            port: 5678
          initialDelaySeconds: 15
          periodSeconds: 20
        # 보안 컨텍스트
        securityContext:
          runAsNonRoot: true
          runAsUser: 1000
          readOnlyRootFilesystem: true
          allowPrivilegeEscalation: false
```

### 2.4 API v2 Deployment

```yaml
# manifests/base/deployments/api-v2.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-v2
  namespace: istio-portfolio
  labels:
    app: api
    version: v2
spec:
  replicas: 2
  selector:
    matchLabels:
      app: api
      version: v2
  template:
    metadata:
      labels:
        app: api
        version: v2
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "15020"
        prometheus.io/path: "/stats/prometheus"
    spec:
      serviceAccountName: api
      containers:
      - name: api
        image: hashicorp/http-echo:latest
        args: ["-text=API v2 - 신규 버전 (카나리)"]
        ports:
        - containerPort: 5678
          name: http
          protocol: TCP
        resources:
          requests:
            cpu: "100m"
            memory: "64Mi"
          limits:
            cpu: "200m"
            memory: "128Mi"
        readinessProbe:
          httpGet:
            path: /
            port: 5678
          initialDelaySeconds: 5
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /
            port: 5678
          initialDelaySeconds: 15
          periodSeconds: 20
        securityContext:
          runAsNonRoot: true
          runAsUser: 1000
          readOnlyRootFilesystem: true
          allowPrivilegeEscalation: false
```

### 2.5 Service

```yaml
# manifests/base/deployments/api-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: api
  namespace: istio-portfolio
  labels:
    app: api
spec:
  selector:
    app: api        # v1, v2 모두 선택 (version 없음)
  ports:
  - name: http
    port: 80
    targetPort: 5678
    protocol: TCP
  type: ClusterIP
```

### 2.6 Frontend Deployment

```yaml
# manifests/base/deployments/frontend.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
  namespace: istio-portfolio
  labels:
    app: frontend
spec:
  replicas: 2
  selector:
    matchLabels:
      app: frontend
  template:
    metadata:
      labels:
        app: frontend
    spec:
      serviceAccountName: frontend
      containers:
      - name: frontend
        image: nginx:1.25-alpine
        ports:
        - containerPort: 80
          name: http
        resources:
          requests:
            cpu: "50m"
            memory: "32Mi"
          limits:
            cpu: "100m"
            memory: "64Mi"
        readinessProbe:
          httpGet:
            path: /
            port: 80
          initialDelaySeconds: 5
          periodSeconds: 10
        volumeMounts:
        - name: nginx-config
          mountPath: /etc/nginx/conf.d/default.conf
          subPath: default.conf
      volumes:
      - name: nginx-config
        configMap:
          name: frontend-config
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: frontend-config
  namespace: istio-portfolio
data:
  default.conf: |
    server {
        listen 80;
        server_name localhost;
        
        location / {
            root /usr/share/nginx/html;
            index index.html;
        }
        
        location /api/ {
            proxy_pass http://api/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
        
        location /health {
            access_log off;
            return 200 "healthy\n";
        }
    }
---
apiVersion: v1
kind: Service
metadata:
  name: frontend
  namespace: istio-portfolio
spec:
  selector:
    app: frontend
  ports:
  - name: http
    port: 80
    targetPort: 80
```

---

## 🛠️ Part 3: 배포 및 확인 (1시간)

### 3.1 배포 순서

```bash
# 1. Namespace 생성 (istio-injection 레이블 포함)
kubectl apply -f - <<EOF
apiVersion: v1
kind: Namespace
metadata:
  name: istio-portfolio
  labels:
    istio-injection: enabled
EOF

# Namespace 확인
kubectl get namespace istio-portfolio --show-labels

# 2. ServiceAccount 생성
kubectl apply -f manifests/base/serviceaccounts.yaml

# 3. Deployment 및 Service 생성
kubectl apply -f manifests/base/deployments/

# 또는 전체 한 번에
kubectl apply -f manifests/base/ --recursive
```

### 3.2 Sidecar 주입 확인

```bash
# Pod 상태 확인 (READY 2/2 확인)
kubectl get pods -n istio-portfolio

# 예상 출력:
# NAME                        READY   STATUS    RESTARTS   AGE
# api-v1-xxx-yyy              2/2     Running   0          1m
# api-v1-xxx-zzz              2/2     Running   0          1m
# api-v2-xxx-yyy              2/2     Running   0          1m
# api-v2-xxx-zzz              2/2     Running   0          1m
# frontend-xxx-yyy            2/2     Running   0          1m
# frontend-xxx-zzz            2/2     Running   0          1m

# READY가 1/1이면 Sidecar 주입 실패!
# → Namespace 레이블 확인
# → Pod를 삭제하면 재생성 시 Sidecar 주입됨
```

### 3.3 상세 확인

```bash
# Pod 상세 정보
kubectl describe pod -n istio-portfolio -l app=api,version=v1 | grep -A10 "Containers:"

# 예상 출력:
# Containers:
#   api:
#     Image: hashicorp/http-echo:latest
#   istio-proxy:
#     Image: docker.io/istio/proxyv2:1.20.0

# Envoy Sidecar 버전 확인
kubectl get pods -n istio-portfolio -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{range .spec.containers[*]}{.image}{"\t"}{end}{"\n"}{end}'
```

### 3.4 서비스 동작 확인

```bash
# API Service 엔드포인트 확인
kubectl get endpoints api -n istio-portfolio

# 예상 출력:
# NAME   ENDPOINTS                                         AGE
# api    10.244.0.5:5678,10.244.0.6:5678,10.244.1.3:5678   1m

# 서비스 내부 테스트 (curl Pod 생성)
kubectl run curl-test --rm -i --tty \
  --image=curlimages/curl \
  --namespace=istio-portfolio \
  -- /bin/sh

# curl 테스트 (Pod 내부에서)
curl http://api/
# 출력: API v1 또는 API v2 (라운드로빈)

# 여러 번 테스트
for i in $(seq 1 10); do curl -s http://api/; done
```

### 3.5 Istio 프록시 상태 확인

```bash
# istioctl로 프록시 상태 확인
istioctl proxy-status

# 특정 Pod의 Envoy 설정 확인
istioctl proxy-config cluster api-v1-xxx-yyy -n istio-portfolio

# 라우팅 설정 확인
istioctl proxy-config route api-v1-xxx-yyy -n istio-portfolio

# 리스너 확인
istioctl proxy-config listener api-v1-xxx-yyy -n istio-portfolio
```

---

## 📊 Part 4: 트러블슈팅 (30분)

### 4.1 일반적인 문제

```bash
# 문제 1: Sidecar가 주입되지 않음 (READY 1/1)
# 원인: Namespace 레이블 누락
kubectl label namespace istio-portfolio istio-injection=enabled
# Pod 삭제하여 재생성
kubectl delete pod -l app=api -n istio-portfolio

# 문제 2: Pod가 Running이 아님
# 상태 확인
kubectl get pods -n istio-portfolio
kubectl describe pod <pod-name> -n istio-portfolio

# 문제 3: Envoy Sidecar CrashLoopBackOff
# 로그 확인
kubectl logs <pod-name> -c istio-proxy -n istio-portfolio

# 문제 4: 서비스 간 통신 실패
# mTLS 상태 확인
istioctl authn tls-check api.istio-portfolio.svc.cluster.local
```

### 4.2 로그 확인

```bash
# 애플리케이션 로그
kubectl logs -n istio-portfolio -l app=api,version=v1 -c api

# Envoy Sidecar 로그
kubectl logs -n istio-portfolio -l app=api,version=v1 -c istio-proxy

# 실시간 로그
kubectl logs -n istio-portfolio -l app=api -c istio-proxy -f
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | Sidecar 패턴 이해 | ☐ |
| 2 | Namespace에 istio-injection 레이블 | ☐ |
| 3 | ServiceAccount 생성 | ☐ |
| 4 | API v1, v2 Deployment 작성 | ☐ |
| 5 | Service 작성 | ☐ |
| 6 | 배포 확인 | ☐ |
| 7 | Sidecar 주입 확인 (2/2) | ☐ |
| 8 | 서비스 통신 테스트 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# Sidecar 주입 활성화
kubectl label namespace <ns> istio-injection=enabled

# Pod 상태 확인
kubectl get pods -n istio-portfolio

# Envoy 설정 확인
istioctl proxy-config cluster <pod> -n <ns>

# 프록시 상태
istioctl proxy-status
```

---

## 💡 면접 대비 핵심 포인트

### Q1: Istio Sidecar란?
**A**: 애플리케이션 Pod에 자동 주입되는 Envoy 프록시 컨테이너입니다. 트래픽 관리, mTLS, 메트릭 수집 등을 애플리케이션 코드 변경 없이 처리합니다.

### Q2: Sidecar가 2/2가 아니라 1/1인 이유는?
**A**: Namespace에 `istio-injection=enabled` 레이블이 없거나, Pod 생성 후 레이블을 추가한 경우입니다. Pod를 삭제하면 재생성 시 Sidecar가 주입됩니다.

### Q3: Sidecar가 처리하는 것은?
**A**: 트래픽 라우팅, mTLS 암호화, 메트릭 수집, 트레이싱, 재시도/타임아웃/서킷브레이커 등 인프라 수준의 기능을 처리합니다.

---

## ➡️ 다음 학습: Day 84

**주제**: 트래픽 관리 (VirtualService, 카나리)
- VirtualService로 트래픽 라우팅
- DestinationRule로 subset 정의
- 카나리 배포 구현
