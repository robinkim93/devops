# 📅 Day 58: Ingress와 HPA 설정 - 외부 노출과 자동 스케일링

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "대규모 트래픽 환경에 대응, 배포 자동화"

Ingress로 외부 트래픽을 라우팅하고, HPA로 자동 스케일링을 구현합니다.

---

## ⏰ 예상 소요 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| Ingress | 1.5시간 | 개념 및 설정 실습 |
| HPA | 1.5시간 | 자동 스케일링 구현 |
| 통합 | 1시간 | 전체 시스템 테스트 |

---

## 📚 Part 1: Ingress 개념 (30분)

### 왜 Ingress가 필요한가?

```
┌─────────────────────────────────────────────────────────────┐
│  외부 트래픽 노출 방식 비교                                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  NodePort 방식 (단순하지만 한계)                            │
│  ┌─────────────────────────────────────────────────┐       │
│  │ Client → Node:30080 → Service A                 │       │
│  │ Client → Node:30081 → Service B                 │       │
│  │                                                  │       │
│  │ 문제점:                                          │       │
│  │ - 포트 관리 복잡 (30000-32767 범위)             │       │
│  │ - TLS 처리 어려움                               │       │
│  │ - 경로 기반 라우팅 불가                         │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  Ingress 방식 (권장)                                        │
│  ┌─────────────────────────────────────────────────┐       │
│  │ Client → Ingress (80/443)                       │       │
│  │           │                                      │       │
│  │           ├─ /api → Service A                   │       │
│  │           ├─ /web → Service B                   │       │
│  │           └─ /admin → Service C                 │       │
│  │                                                  │       │
│  │ 장점:                                            │       │
│  │ - 단일 진입점 (80/443)                          │       │
│  │ - TLS 종료 (SSL 인증서 관리)                    │       │
│  │ - 경로/호스트 기반 라우팅                       │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Ingress 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│  Ingress Architecture                                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Internet                                                   │
│      │                                                      │
│      ▼                                                      │
│  ┌────────────────────────────────────────────────────┐    │
│  │          LoadBalancer / NodePort                    │    │
│  └─────────────────────────┬──────────────────────────┘    │
│                            │                                │
│                            ▼                                │
│  ┌────────────────────────────────────────────────────┐    │
│  │           Ingress Controller                        │    │
│  │        (nginx, traefik, istio 등)                   │    │
│  └─────────────────────────┬──────────────────────────┘    │
│                            │                                │
│      ┌─────────────────────┼─────────────────────┐         │
│      │                     │                     │         │
│      ▼                     ▼                     ▼         │
│  ┌────────────┐      ┌────────────┐      ┌────────────┐   │
│  │ Service A  │      │ Service B  │      │ Service C  │   │
│  │ (frontend) │      │  (backend) │      │  (admin)   │   │
│  └────────────┘      └────────────┘      └────────────┘   │
│                                                             │
│  Ingress Resource:                                          │
│  - 라우팅 규칙 정의 (YAML)                                 │
│  - 호스트/경로 기반 트래픽 분배                            │
│  - TLS 설정                                                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: Ingress 실습 (1시간)

### 실습 1: Ingress Controller 설치 (minikube)

```bash
# Ingress addon 활성화
minikube addons enable ingress

# Ingress Controller 확인
kubectl get pods -n ingress-nginx
kubectl get svc -n ingress-nginx

# Ingress Controller 준비 대기
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=120s
```

### 실습 2: 테스트 서비스 배포

```bash
# Frontend 서비스
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
  namespace: k8s-portfolio
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
      containers:
      - name: nginx
        image: nginx:1.24
        ports:
        - containerPort: 80
---
apiVersion: v1
kind: Service
metadata:
  name: frontend
  namespace: k8s-portfolio
spec:
  selector:
    app: frontend
  ports:
  - port: 80
    targetPort: 80
EOF

# Backend 서비스
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  namespace: k8s-portfolio
spec:
  replicas: 2
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
    spec:
      containers:
      - name: httpd
        image: httpd:2.4
        ports:
        - containerPort: 80
---
apiVersion: v1
kind: Service
metadata:
  name: backend
  namespace: k8s-portfolio
spec:
  selector:
    app: backend
  ports:
  - port: 80
    targetPort: 80
EOF
```

### 실습 3: Ingress 리소스 생성

```bash
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: app-ingress
  namespace: k8s-portfolio
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "false"
spec:
  ingressClassName: nginx
  rules:
  - host: k8s-portfolio.local
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend
            port:
              number: 80
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: backend
            port:
              number: 80
EOF

# Ingress 확인
kubectl get ingress -n k8s-portfolio
kubectl describe ingress app-ingress -n k8s-portfolio
```

### 실습 4: 접속 테스트

```bash
# /etc/hosts에 호스트 추가
echo "$(minikube ip) k8s-portfolio.local" | sudo tee -a /etc/hosts

# 또는 minikube tunnel 사용
minikube tunnel

# 테스트
curl http://k8s-portfolio.local/
curl http://k8s-portfolio.local/api/

# 또는 minikube service 사용
minikube service frontend -n k8s-portfolio --url
```

### 실습 5: TLS 설정

```bash
# 자체 서명 인증서 생성
openssl req -x509 -nodes -days 365 \
  -newkey rsa:2048 \
  -keyout tls.key \
  -out tls.crt \
  -subj "/CN=k8s-portfolio.local"

# TLS Secret 생성
kubectl create secret tls tls-secret \
  --cert=tls.crt \
  --key=tls.key \
  -n k8s-portfolio

# TLS Ingress 업데이트
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: app-ingress
  namespace: k8s-portfolio
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - k8s-portfolio.local
    secretName: tls-secret
  rules:
  - host: k8s-portfolio.local
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend
            port:
              number: 80
EOF

# HTTPS 테스트
curl -k https://k8s-portfolio.local/
```

---

## 📚 Part 3: HPA 개념 (30분)

### HPA (Horizontal Pod Autoscaler)

```
┌─────────────────────────────────────────────────────────────┐
│  HPA 동작 원리                                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                   Metrics Server                            │
│                       │                                     │
│                       ▼ 메트릭 수집                         │
│  ┌────────────────────────────────────────────────────┐    │
│  │                      HPA                            │    │
│  │   CPU 70% 초과? → Scale Out (Pod 추가)              │    │
│  │   CPU 30% 미만? → Scale In (Pod 감소)               │    │
│  └─────────────────────────┬──────────────────────────┘    │
│                            │ 스케일 조정                    │
│                            ▼                                │
│  ┌────────────────────────────────────────────────────┐    │
│  │                   Deployment                        │    │
│  │  ┌──────┐ ┌──────┐ ┌──────┐        ┌──────┐       │    │
│  │  │ Pod  │ │ Pod  │ │ Pod  │  ...   │ Pod  │       │    │
│  │  └──────┘ └──────┘ └──────┘        └──────┘       │    │
│  │     min: 2                           max: 10       │    │
│  └────────────────────────────────────────────────────┘    │
│                                                             │
│  스케일링 알고리즘:                                         │
│  desiredReplicas = ceil(currentReplicas * (currentMetric / │
│                          targetMetric))                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 4: HPA 실습 (1시간)

### 실습 1: Metrics Server 확인

```bash
# minikube에서 metrics-server 활성화
minikube addons enable metrics-server

# 잠시 대기 후 메트릭 확인
kubectl top nodes
kubectl top pods -n k8s-portfolio
```

### 실습 2: HPA 생성

```bash
# Backend Deployment에 리소스 설정 추가
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  namespace: k8s-portfolio
spec:
  replicas: 2
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
    spec:
      containers:
      - name: httpd
        image: httpd:2.4
        ports:
        - containerPort: 80
        resources:
          requests:
            cpu: 100m
            memory: 128Mi
          limits:
            cpu: 200m
            memory: 256Mi
EOF

# HPA 생성
kubectl apply -f - <<EOF
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: backend-hpa
  namespace: k8s-portfolio
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: backend
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 50
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 70
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300  # 5분 안정화
      policies:
      - type: Percent
        value: 50
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100
        periodSeconds: 15
EOF

# HPA 상태 확인
kubectl get hpa -n k8s-portfolio
kubectl describe hpa backend-hpa -n k8s-portfolio
```

### 실습 3: 부하 테스트

```bash
# 부하 생성 Pod
kubectl run load-generator --image=busybox --rm -it -- \
  /bin/sh -c "while true; do wget -q -O- http://backend.k8s-portfolio.svc/; done"

# 다른 터미널에서 HPA 모니터링
kubectl get hpa -n k8s-portfolio -w

# Pod 증가 확인
kubectl get pods -n k8s-portfolio -l app=backend -w
```

### 실습 4: 명령형 HPA 생성

```bash
# 간단한 HPA 생성
kubectl autoscale deployment backend \
  --cpu-percent=50 \
  --min=2 \
  --max=10 \
  -n k8s-portfolio

# HPA 삭제
kubectl delete hpa backend -n k8s-portfolio
```

---

## 📚 Part 5: 토스플레이스 패턴 (30분)

### 프로덕션 Ingress 설정

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: payment-ingress
  namespace: payment
  annotations:
    kubernetes.io/ingress.class: "nginx"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "30"
    nginx.ingress.kubernetes.io/rate-limit: "100"
    nginx.ingress.kubernetes.io/rate-limit-window: "1m"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - payment.toss.im
    secretName: payment-tls
  rules:
  - host: payment.toss.im
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: payment-api
            port:
              number: 80
```

### 프로덕션 HPA 설정

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: payment-hpa
  namespace: payment
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: payment-api
  minReplicas: 5
  maxReplicas: 50
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Pods
        value: 4
        periodSeconds: 15
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 25
        periodSeconds: 60
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | Ingress Controller 설치 | ☐ |
| 2 | Ingress 리소스 생성 | ☐ |
| 3 | 경로 기반 라우팅 테스트 | ☐ |
| 4 | TLS 설정 | ☐ |
| 5 | HPA 생성 | ☐ |
| 6 | 부하 테스트로 스케일 아웃 확인 | ☐ |

---

## 🔑 핵심 명령어

```bash
# Ingress
kubectl get ingress
kubectl describe ingress <name>

# HPA
kubectl get hpa
kubectl describe hpa <name>
kubectl autoscale deployment <name> --cpu-percent=50 --min=2 --max=10
```

---

## 📝 면접 대비 질문

### Q1: Ingress와 LoadBalancer Service의 차이점은?
> "LoadBalancer는 각 Service마다 별도의 외부 IP가 필요합니다. Ingress는 단일 진입점에서 호스트/경로 기반으로 여러 Service를 라우팅합니다. 비용 효율적이고 TLS 종료, 라우팅 규칙 등 L7 기능을 제공합니다."

### Q2: HPA의 스케일링 알고리즘을 설명해주세요.
> "현재 메트릭과 목표 메트릭의 비율로 필요한 Pod 수를 계산합니다. 예를 들어 현재 CPU 80%, 목표 40%이면 Pod를 2배로 늘립니다. 급격한 변동 방지를 위해 stabilizationWindowSeconds와 policies로 속도를 제어합니다."

---

## ➡️ 다음 학습: Day 59

**주제**: 문서화 및 테스트
