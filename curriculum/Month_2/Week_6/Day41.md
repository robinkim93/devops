# 📅 Day 41: Ingress - L7 라우팅

## 🎯 오늘의 목표

> **토스플레이스 핵심**: Ingress로 L7 로드밸런싱과 트래픽 라우팅을 구현합니다.

---

## ⏰ 예상 학습 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 | 30분 | Ingress 이해 |
| Controller | 30분 | 설치 |
| 라우팅 | 1시간 | 경로/호스트 기반 |
| TLS | 1시간 | HTTPS 설정 |

---

## 📚 Part 1: Ingress 개념

### Service 유형 비교

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     Kubernetes 외부 노출 방법                                 │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   NodePort                                                                   │
│   ┌──────────────────────────────────────────────────────┐                  │
│   │ • 각 노드의 특정 포트로 노출                         │                  │
│   │ • 서비스당 하나의 포트 필요 (30000-32767)            │                  │
│   │ • 단점: 포트 관리 복잡, L4만 가능                    │                  │
│   └──────────────────────────────────────────────────────┘                  │
│                                                                              │
│   LoadBalancer                                                               │
│   ┌──────────────────────────────────────────────────────┐                  │
│   │ • 클라우드 LB 프로비저닝                             │                  │
│   │ • 서비스당 하나의 외부 IP/LB                         │                  │
│   │ • 단점: 비용 증가, L4만 가능                         │                  │
│   └──────────────────────────────────────────────────────┘                  │
│                                                                              │
│   Ingress (권장)                                                             │
│   ┌──────────────────────────────────────────────────────┐                  │
│   │ • L7 로드밸런싱 (HTTP/HTTPS)                         │                  │
│   │ • 하나의 IP로 여러 서비스 라우팅                     │                  │
│   │ • URL 경로, 호스트 기반 라우팅                       │                  │
│   │ • TLS 종료 (SSL Offloading)                          │                  │
│   └──────────────────────────────────────────────────────┘                  │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Ingress 아키텍처

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        Ingress 동작 원리                                     │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Client                                                                     │
│     │                                                                        │
│     │ https://api.example.com/users                                         │
│     ▼                                                                        │
│   ┌────────────────────────────────────────────────────────────────────┐    │
│   │                      Ingress Controller                            │    │
│   │               (NGINX, Traefik, HAProxy 등)                         │    │
│   └───────────────────────────┬────────────────────────────────────────┘    │
│                               │ 라우팅 규칙 적용                             │
│           ┌───────────────────┼───────────────────┐                         │
│           │                   │                   │                         │
│           ▼                   ▼                   ▼                         │
│   ┌───────────────┐   ┌───────────────┐   ┌───────────────┐                 │
│   │ Service: user │   │Service: order │   │Service: product│                │
│   │ /users/*      │   │ /orders/*     │   │ /products/*   │                 │
│   └───────────────┘   └───────────────┘   └───────────────┘                 │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: Ingress Controller 설치

### NGINX Ingress Controller

```bash
# Helm 설치
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update

helm install ingress-nginx ingress-nginx/ingress-nginx \
  -n ingress-nginx --create-namespace \
  --set controller.replicaCount=2 \
  --set controller.service.type=LoadBalancer

# 또는 manifest 설치
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.9.0/deploy/static/provider/cloud/deploy.yaml

# 확인
kubectl get pods -n ingress-nginx
kubectl get svc -n ingress-nginx
```

### Minikube에서 활성화

```bash
# Ingress addon 활성화
minikube addons enable ingress

# 확인
kubectl get pods -n ingress-nginx
```

---

## 🛠️ Part 3: 경로 기반 라우팅

### 테스트 서비스 준비

```bash
# 서비스 1: nginx
kubectl create deployment nginx --image=nginx
kubectl expose deployment nginx --port=80

# 서비스 2: httpd
kubectl create deployment httpd --image=httpd
kubectl expose deployment httpd --port=80
```

### Ingress 생성

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: path-based-ingress
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  ingressClassName: nginx
  rules:
  - host: myapp.local
    http:
      paths:
      - path: /nginx
        pathType: Prefix
        backend:
          service:
            name: nginx
            port:
              number: 80
      - path: /httpd
        pathType: Prefix
        backend:
          service:
            name: httpd
            port:
              number: 80
```

### pathType 종류

| pathType | 설명 | 예시 매칭 |
|----------|------|----------|
| `Exact` | 정확히 일치 | /foo만 매칭 |
| `Prefix` | 접두사 일치 | /foo, /foo/, /foo/bar |
| `ImplementationSpecific` | 구현에 따라 | Controller에 따라 다름 |

---

## 🛠️ Part 4: 호스트 기반 라우팅

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: host-based-ingress
spec:
  ingressClassName: nginx
  rules:
  # api.example.com → api 서비스
  - host: api.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: api
            port:
              number: 8080
  # web.example.com → web 서비스
  - host: web.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: web
            port:
              number: 80
  # 기본 (호스트 미지정) → default 서비스
  - http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: default-service
            port:
              number: 80
```

---

## 🛠️ Part 5: TLS 설정

### TLS Secret 생성

```bash
# 자체 서명 인증서 생성 (테스트용)
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout tls.key -out tls.crt \
  -subj "/CN=myapp.example.com"

# TLS Secret 생성
kubectl create secret tls myapp-tls \
  --cert=tls.crt \
  --key=tls.key
```

### TLS Ingress

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: tls-ingress
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - myapp.example.com
    secretName: myapp-tls
  rules:
  - host: myapp.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: myapp
            port:
              number: 80
```

### cert-manager로 자동 인증서

```yaml
# cert-manager 설치 후
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: auto-tls-ingress
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  ingressClassName: nginx
  tls:
  - hosts:
    - myapp.example.com
    secretName: myapp-tls-auto  # 자동 생성됨
  rules:
  - host: myapp.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: myapp
            port:
              number: 80
```

---

## 🛠️ Part 6: 유용한 Annotations

### NGINX Ingress Annotations

| Annotation | 설명 |
|------------|------|
| `nginx.ingress.kubernetes.io/rewrite-target` | URL 재작성 |
| `nginx.ingress.kubernetes.io/ssl-redirect` | HTTPS 리다이렉트 |
| `nginx.ingress.kubernetes.io/proxy-body-size` | 요청 본문 크기 제한 |
| `nginx.ingress.kubernetes.io/proxy-connect-timeout` | 연결 타임아웃 |
| `nginx.ingress.kubernetes.io/limit-rps` | Rate Limiting |

```yaml
metadata:
  annotations:
    # URL 재작성
    nginx.ingress.kubernetes.io/rewrite-target: /$2
    # 요청 본문 크기
    nginx.ingress.kubernetes.io/proxy-body-size: "10m"
    # Rate Limiting
    nginx.ingress.kubernetes.io/limit-rps: "10"
    # CORS
    nginx.ingress.kubernetes.io/enable-cors: "true"
spec:
  rules:
  - host: api.example.com
    http:
      paths:
      - path: /api(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: api
            port:
              number: 8080
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 명령어/설정 | 완료 |
|---|------|-----------|------|
| 1 | Ingress Controller 설치 | `helm install ingress-nginx` | ☐ |
| 2 | 경로 기반 라우팅 | `path: /api` | ☐ |
| 3 | 호스트 기반 라우팅 | `host: api.example.com` | ☐ |
| 4 | TLS Secret 생성 | `kubectl create secret tls` | ☐ |
| 5 | HTTPS Ingress | `spec.tls` | ☐ |
| 6 | Annotations 활용 | rewrite, ssl-redirect | ☐ |

---

## 🔑 핵심 명령어 요약

```bash
# Ingress Controller 설치
helm install ingress-nginx ingress-nginx/ingress-nginx -n ingress-nginx --create-namespace

# Ingress 확인
kubectl get ingress
kubectl describe ingress <name>

# TLS Secret
kubectl create secret tls myapp-tls --cert=tls.crt --key=tls.key

# 테스트
curl -H "Host: myapp.local" http://<ingress-ip>/
```

---

## ➡️ 다음 학습: Day 42

**주제**: 리소스 관리 (requests, limits)

