# Day 64: Istio Gateway 설정

## 오늘의 목표

토스플레이스 연결점: "Kubernetes와 Service Mesh에 대한 경험"
"Istio 기반의 서비스 메시 운영"

Istio Gateway로 외부 트래픽을 메시 내부로 안전하게 라우팅합니다. Ingress Gateway 설정과 HTTPS 설정을 실습합니다.

---

## 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 핵심 개념 | 45분 | Gateway vs Ingress, 아키텍처 |
| 기본 실습 | 1시간 | Gateway, VirtualService |
| HTTPS 설정 | 1시간 | TLS 인증서, HTTPS |
| 고급 설정 | 1시간 | 멀티 호스트, SNI |
| 정리 | 15분 | 체크리스트, 면접 |

---

## Part 1: Gateway란? (45분)

### 1.1 Kubernetes Ingress vs Istio Gateway

```
Kubernetes Ingress:
┌────────────────────────────────────────────────┐
│                 Ingress Controller              │
│            (nginx, traefik 등)                  │
│                                                │
│  - L7 라우팅 (호스트, 경로)                     │
│  - TLS 종료                                     │
│  - 기본적인 기능                                │
└────────────────────────────────────────────────┘
                     │
                     ▼
              Kubernetes Service

Istio Gateway:
┌────────────────────────────────────────────────┐
│               Istio Ingress Gateway             │
│                 (Envoy Proxy)                   │
│                                                │
│  - L4-L7 라우팅                                 │
│  - mTLS                                         │
│  - 세밀한 트래픽 제어                           │
│  - VirtualService와 연동                        │
└────────────────────────────────────────────────┘
                     │
                     ▼
              VirtualService
                     │
                     ▼
              DestinationRule
                     │
                     ▼
                   Pod
```

### 1.2 Gateway 역할

```
Gateway의 역할:

1. 포트 노출
   - 외부에서 접근할 포트 정의
   - HTTP (80), HTTPS (443) 등

2. 호스트 매칭
   - 어떤 도메인으로 들어오는 트래픽인지
   - 와일드카드 (*.example.com) 지원

3. TLS 설정
   - 인증서 연결
   - TLS 모드 (SIMPLE, MUTUAL)

4. VirtualService와 연동
   - Gateway는 "어디서 들어오나"
   - VirtualService는 "어디로 보내나"
```

### 1.3 Gateway + VirtualService 관계

```yaml
# Gateway: 외부 진입점 정의
Gateway:
  - 어떤 포트로 들어오나? (80, 443)
  - 어떤 호스트로 들어오나? (api.example.com)
  - TLS 설정은?

# VirtualService: 라우팅 규칙 정의
VirtualService:
  - Gateway에서 들어온 트래픽을 어디로?
  - 경로 기반 라우팅
  - 헤더 기반 라우팅
  - 가중치 분배
```

---

## Part 2: 기본 실습 (1시간)

### 실습 1: Istio Ingress Gateway 확인

```bash
# Ingress Gateway Pod 확인
kubectl get pods -n istio-system -l app=istio-ingressgateway

# Ingress Gateway Service 확인
kubectl get svc -n istio-system istio-ingressgateway

# 출력 예시:
# NAME                   TYPE           CLUSTER-IP      EXTERNAL-IP   PORT(S)
# istio-ingressgateway   LoadBalancer   10.96.xxx.xxx   <pending>     15021/TCP,80/TCP,443/TCP

# minikube인 경우 터널 시작
minikube tunnel  # 별도 터미널에서 실행
```

### 실습 2: 기본 Gateway 생성

```yaml
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: my-gateway
  namespace: default
spec:
  selector:
    istio: ingressgateway    # Istio Ingress Gateway 선택
  servers:
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - "api.example.com"      # 이 도메인으로 들어오는 트래픽
    - "web.example.com"
EOF
```

### 실습 3: VirtualService 연결

```yaml
# 먼저 테스트 앱 배포
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: httpbin
spec:
  replicas: 1
  selector:
    matchLabels:
      app: httpbin
  template:
    metadata:
      labels:
        app: httpbin
    spec:
      containers:
      - name: httpbin
        image: docker.io/kong/httpbin:latest
        ports:
        - containerPort: 80
---
apiVersion: v1
kind: Service
metadata:
  name: httpbin
spec:
  selector:
    app: httpbin
  ports:
  - port: 80
    targetPort: 80
EOF
```

```yaml
# VirtualService로 Gateway에서 들어온 트래픽 라우팅
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: httpbin-vs
spec:
  hosts:
  - "api.example.com"
  gateways:
  - my-gateway             # 위에서 만든 Gateway 참조
  http:
  - match:
    - uri:
        prefix: /api
    route:
    - destination:
        host: httpbin
        port:
          number: 80
  - match:
    - uri:
        prefix: /
    route:
    - destination:
        host: httpbin
        port:
          number: 80
EOF
```

### 실습 4: 테스트

```bash
# Ingress Gateway IP 확인
INGRESS_HOST=$(kubectl get svc -n istio-system istio-ingressgateway -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

# minikube인 경우
INGRESS_HOST=$(minikube ip)
INGRESS_PORT=$(kubectl get svc -n istio-system istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')

echo "Ingress: $INGRESS_HOST:$INGRESS_PORT"

# 테스트 (Host 헤더로 도메인 지정)
curl -H "Host: api.example.com" http://$INGRESS_HOST:$INGRESS_PORT/get

# 또는 /etc/hosts에 추가
# echo "$INGRESS_HOST api.example.com" | sudo tee -a /etc/hosts
# curl http://api.example.com:$INGRESS_PORT/get
```

---

## Part 3: HTTPS 설정 (1시간)

### 실습 5: 자체 서명 인증서 생성

```bash
# 인증서 디렉토리
mkdir -p ~/istio-certs && cd ~/istio-certs

# 자체 서명 인증서 생성
openssl req -x509 -sha256 -nodes -days 365 -newkey rsa:2048 \
  -subj "/CN=api.example.com/O=example" \
  -keyout api.example.com.key \
  -out api.example.com.crt

# 인증서 확인
openssl x509 -in api.example.com.crt -text -noout | head -20
```

### 실습 6: Kubernetes Secret 생성

```bash
# istio-system 네임스페이스에 TLS Secret 생성
kubectl create secret tls api-credential \
  --key=api.example.com.key \
  --cert=api.example.com.crt \
  -n istio-system

# 확인
kubectl get secret api-credential -n istio-system
```

### 실습 7: HTTPS Gateway 설정

```yaml
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: https-gateway
spec:
  selector:
    istio: ingressgateway
  servers:
  # HTTPS 설정
  - port:
      number: 443
      name: https
      protocol: HTTPS
    tls:
      mode: SIMPLE                    # 단방향 TLS
      credentialName: api-credential  # Secret 이름
    hosts:
    - "api.example.com"
  # HTTP를 HTTPS로 리다이렉트 (선택)
  - port:
      number: 80
      name: http
      protocol: HTTP
    tls:
      httpsRedirect: true
    hosts:
    - "api.example.com"
EOF
```

### 실습 8: VirtualService 업데이트

```yaml
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: httpbin-vs
spec:
  hosts:
  - "api.example.com"
  gateways:
  - https-gateway
  http:
  - route:
    - destination:
        host: httpbin
        port:
          number: 80
EOF
```

### 실습 9: HTTPS 테스트

```bash
# HTTPS 포트 확인
SECURE_PORT=$(kubectl get svc -n istio-system istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="https")].nodePort}')

# 테스트 (자체 서명 인증서이므로 -k 옵션 사용)
curl -k -H "Host: api.example.com" https://$INGRESS_HOST:$SECURE_PORT/get

# 인증서 확인
openssl s_client -connect $INGRESS_HOST:$SECURE_PORT -servername api.example.com < /dev/null 2>/dev/null | openssl x509 -text -noout | head -20
```

---

## Part 4: 고급 설정 (1시간)

### 실습 10: 멀티 호스트 Gateway

```yaml
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: multi-host-gateway
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - "api.example.com"
    - "web.example.com"
    - "admin.example.com"
---
# api.example.com 라우팅
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: api-vs
spec:
  hosts:
  - "api.example.com"
  gateways:
  - multi-host-gateway
  http:
  - route:
    - destination:
        host: api-service
---
# web.example.com 라우팅
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: web-vs
spec:
  hosts:
  - "web.example.com"
  gateways:
  - multi-host-gateway
  http:
  - route:
    - destination:
        host: web-service
EOF
```

### 실습 11: 와일드카드 호스트

```yaml
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: wildcard-gateway
spec:
  selector:
    istio: ingressgateway
  servers:
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - "*.example.com"    # 모든 서브도메인
---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: wildcard-vs
spec:
  hosts:
  - "*.example.com"
  gateways:
  - wildcard-gateway
  http:
  - match:
    - headers:
        ":authority":
          prefix: "api."    # api.example.com
    route:
    - destination:
        host: api-service
  - match:
    - headers:
        ":authority":
          prefix: "web."    # web.example.com
    route:
    - destination:
        host: web-service
  - route:                   # 기본
    - destination:
        host: default-service
EOF
```

### 실습 12: 경로 기반 라우팅

```yaml
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: path-routing-vs
spec:
  hosts:
  - "api.example.com"
  gateways:
  - my-gateway
  http:
  - match:
    - uri:
        prefix: /v1/users
    route:
    - destination:
        host: users-v1
  - match:
    - uri:
        prefix: /v2/users
    route:
    - destination:
        host: users-v2
  - match:
    - uri:
        prefix: /orders
    route:
    - destination:
        host: orders-service
  - match:
    - uri:
        prefix: /products
    route:
    - destination:
        host: products-service
EOF
```

---

## 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | Gateway vs Ingress 이해 | 차이점, 장점 | |
| 2 | 기본 Gateway 생성 | HTTP Gateway | |
| 3 | VirtualService 연결 | Gateway + VirtualService | |
| 4 | HTTPS 설정 | TLS 인증서, Secret | |
| 5 | 멀티 호스트 | 여러 도메인 처리 | |
| 6 | 와일드카드 호스트 | *.example.com | |
| 7 | 경로 기반 라우팅 | URI prefix 매칭 | |

---

## 핵심 YAML 템플릿

```yaml
# Gateway 기본 템플릿
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: my-gateway
spec:
  selector:
    istio: ingressgateway    # 필수: Ingress Gateway 선택
  servers:
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - "example.com"

# HTTPS Gateway 템플릿
servers:
- port:
    number: 443
    name: https
    protocol: HTTPS
  tls:
    mode: SIMPLE
    credentialName: my-tls-secret
  hosts:
  - "example.com"

# VirtualService와 연결
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
spec:
  hosts:
  - "example.com"
  gateways:
  - my-gateway           # Gateway 이름 참조
  http:
  - route:
    - destination:
        host: my-service
```

---

## 면접 대비 핵심 포인트

**Q1: Kubernetes Ingress와 Istio Gateway의 차이는?**
> "Ingress는 L7 라우팅 기본 기능을 제공하지만, Istio Gateway는 VirtualService, DestinationRule과 연동하여 세밀한 트래픽 제어, mTLS, Canary 배포 등을 지원합니다."

**Q2: Gateway와 VirtualService의 관계는?**
> "Gateway는 '어디서 들어오는가'(포트, 호스트, TLS)를 정의하고, VirtualService는 '어디로 보내는가'(라우팅 규칙)를 정의합니다. VirtualService의 gateways 필드로 연결됩니다."

**Q3: HTTPS를 설정하려면 어떻게 하나요?**
> "TLS 인증서를 Kubernetes Secret으로 생성하고, Gateway의 tls.credentialName으로 참조합니다. mode는 SIMPLE(단방향) 또는 MUTUAL(양방향)을 선택합니다."

---

## 정리

```bash
kubectl delete gateway my-gateway https-gateway multi-host-gateway wildcard-gateway
kubectl delete virtualservice httpbin-vs api-vs web-vs wildcard-vs path-routing-vs
kubectl delete deploy httpbin
kubectl delete svc httpbin
kubectl delete secret api-credential -n istio-system
rm -rf ~/istio-certs
```

---

## 다음 학습: Day 65

주제: Timeout, Retry, Fault Injection
- 복원력 패턴 구현
- 장애 주입 테스트
- 카오스 엔지니어링 기초