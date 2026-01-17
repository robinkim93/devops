# 📅 Day 34: Service 학습

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "Kubernetes 클러스터 운영/최적화"
> 서비스 디스커버리와 로드밸런싱은 마이크로서비스 아키텍처의 핵심

Pod에 접근하기 위한 Service의 종류와 사용법을 이해합니다. Service는 변하는 Pod IP를 추상화하여 안정적인 접근점을 제공합니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| Service 이론 | 45분 | 개념, 타입 |
| ClusterIP 실습 | 1시간 | 내부 통신 |
| NodePort 실습 | 45분 | 외부 노출 |
| DNS/Endpoints | 1.5시간 | 서비스 디스커버리 |

---

## 📚 Part 1: Service란? (45분)

### 1.1 왜 Service가 필요한가?

```
┌─────────────────────────────────────────────────────────────────────┐
│  Pod 직접 접근의 문제점                                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  문제 1: Pod IP는 변경됨                                            │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Pod 재시작 → 새 IP 할당                                    │    │
│  │  10.244.0.5 → 10.244.0.12                                   │    │
│  │  클라이언트가 새 IP를 어떻게 알지?                          │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  문제 2: 여러 Pod가 있으면?                                         │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Pod 1: 10.244.0.5                                          │    │
│  │  Pod 2: 10.244.0.6                                          │    │
│  │  Pod 3: 10.244.0.7                                          │    │
│  │  어디로 요청을 보내야 하나?                                 │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
├─────────────────────────────────────────────────────────────────────┤
│  Service가 해결하는 것                                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ✅ 고정된 접근점 (Stable IP & DNS)                                 │
│  ✅ 로드 밸런싱 (여러 Pod에 분산)                                   │
│  ✅ 서비스 디스커버리 (DNS 기반)                                    │
│  ✅ 자동 엔드포인트 업데이트                                        │
│                                                                      │
│  Client ─────→ Service ─────→ Pod1                                  │
│          (고정 IP/DNS)   ├────→ Pod2                                │
│                          └────→ Pod3                                │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 Service 동작 원리

```
┌─────────────────────────────────────────────────────────────────────┐
│  Service 동작 흐름                                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. Service 생성                                                    │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  apiVersion: v1                                          │       │
│  │  kind: Service                                           │       │
│  │  metadata:                                               │       │
│  │    name: nginx-service                                   │       │
│  │  spec:                                                   │       │
│  │    selector:                                             │       │
│  │      app: nginx         # ← 이 Label을 가진 Pod 선택    │       │
│  │    ports:                                                │       │
│  │    - port: 80           # ← Service 포트                │       │
│  │      targetPort: 80     # ← Pod 포트                    │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                      │
│  2. Endpoints 자동 생성                                             │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  Label(app=nginx)을 가진 Pod IP 수집                     │       │
│  │  Endpoints: [10.244.0.5, 10.244.0.6, 10.244.0.7]        │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                      │
│  3. kube-proxy가 iptables/IPVS 규칙 설정                            │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  Service IP (10.96.x.x) → Pod IP 로드밸런싱              │       │
│  └──────────────────────────────────────────────────────────┘       │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.3 Service 종류

| 타입 | 설명 | IP 범위 | 사용 케이스 |
|------|------|---------|------------|
| **ClusterIP** | 클러스터 내부 IP | 10.96.0.0/12 (기본) | 내부 통신 (기본값) |
| **NodePort** | 노드 포트 노출 | 30000-32767 | 외부 테스트, 개발 |
| **LoadBalancer** | 외부 로드밸런서 | 클라우드 제공 | 프로덕션 외부 노출 |
| **ExternalName** | 외부 DNS 매핑 | - | 외부 서비스 참조 |

```
┌─────────────────────────────────────────────────────────────────────┐
│  Service 타입별 접근 범위                                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ClusterIP (내부만)                                                 │
│  ┌─────────────────────────────────────────────┐                    │
│  │  Cluster 내부 Pod → Service → Backend Pods  │                    │
│  │  외부 접근 불가 ❌                          │                    │
│  └─────────────────────────────────────────────┘                    │
│                                                                      │
│  NodePort (노드 포트)                                               │
│  ┌─────────────────────────────────────────────┐                    │
│  │  External → Node IP:NodePort → Service → Pods │                  │
│  │  모든 노드에서 동일 포트로 접근 가능 ✅     │                    │
│  └─────────────────────────────────────────────┘                    │
│                                                                      │
│  LoadBalancer (외부 LB)                                             │
│  ┌─────────────────────────────────────────────┐                    │
│  │  External → Cloud LB → NodePort → Service → Pods │               │
│  │  클라우드 환경에서 사용 ✅                  │                    │
│  └─────────────────────────────────────────────┘                    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: ClusterIP Service 실습 (1시간)

### 실습 1: 환경 준비

```bash
mkdir -p ~/k8s-practice/day34
cd ~/k8s-practice/day34

# Deployment 생성
cat << 'EOF' > nginx-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx
  labels:
    app: nginx
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
        version: v1
    spec:
      containers:
      - name: nginx
        image: nginx:1.24
        ports:
        - containerPort: 80
        resources:
          requests:
            cpu: 50m
            memory: 64Mi
          limits:
            cpu: 100m
            memory: 128Mi
EOF

kubectl apply -f nginx-deployment.yaml

# Pod 확인
kubectl get pods -l app=nginx -o wide
```

### 실습 2: ClusterIP Service 생성

```bash
# ClusterIP Service 생성
cat << 'EOF' > nginx-clusterip.yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx-service
  labels:
    app: nginx
spec:
  type: ClusterIP      # 기본값, 생략 가능
  selector:
    app: nginx         # 이 라벨을 가진 Pod 선택
  ports:
  - name: http
    port: 80           # Service 포트
    targetPort: 80     # Pod 포트
    protocol: TCP
EOF

kubectl apply -f nginx-clusterip.yaml

# Service 확인
echo "=== Service 생성 확인 ==="
kubectl get service nginx-service

# 상세 정보
echo ""
echo "=== Service 상세 정보 ==="
kubectl describe service nginx-service

# 예상 출력:
# Name:              nginx-service
# Namespace:         default
# Labels:            app=nginx
# Selector:          app=nginx
# Type:              ClusterIP
# IP:                10.96.xxx.xxx  (Cluster IP)
# Port:              http  80/TCP
# TargetPort:        80/TCP
# Endpoints:         10.244.x.x:80,10.244.x.x:80,10.244.x.x:80
```

### 실습 3: Service 접근 테스트

```bash
# 테스트 Pod 생성 및 접근
echo "=== Service 접근 테스트 ==="

# 방법 1: 임시 Pod에서 curl
kubectl run test-client --image=curlimages/curl --rm -it --restart=Never -- \
  curl -s nginx-service

# 방법 2: Service IP로 접근
SERVICE_IP=$(kubectl get service nginx-service -o jsonpath='{.spec.clusterIP}')
echo "Service IP: $SERVICE_IP"

kubectl run test-client2 --image=curlimages/curl --rm -it --restart=Never -- \
  curl -s $SERVICE_IP

# 방법 3: 여러 번 요청하여 로드밸런싱 확인
echo ""
echo "=== 로드밸런싱 테스트 (5회 요청) ==="
for i in {1..5}; do
  kubectl run test-lb-$i --image=curlimages/curl --rm -it --restart=Never -- \
    curl -s nginx-service -o /dev/null -w "Request $i: %{http_code}\n" 2>/dev/null
done
```

### 실습 4: Service YAML 상세 분석

```yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx-service
  labels:                    # Service 자체의 라벨
    app: nginx
  annotations:               # 추가 메타데이터
    description: "Nginx web service"
spec:
  type: ClusterIP           # Service 타입
  selector:                 # 대상 Pod 선택
    app: nginx              # ⚠️ Pod 라벨과 일치해야 함
  ports:
  - name: http              # 포트 이름 (선택)
    port: 80                # Service 포트 (외부에서 접근)
    targetPort: 80          # Pod 포트 (실제 컨테이너)
    protocol: TCP           # 프로토콜 (TCP/UDP)
  sessionAffinity: None     # 세션 어피니티 (None/ClientIP)
  # sessionAffinityConfig:  # ClientIP 사용 시
  #   clientIP:
  #     timeoutSeconds: 10800
```

---

## 🛠️ Part 3: NodePort Service 실습 (45분)

### 실습 5: NodePort Service 생성

```bash
# NodePort Service
cat << 'EOF' > nginx-nodeport.yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx-nodeport
spec:
  type: NodePort
  selector:
    app: nginx
  ports:
  - name: http
    port: 80           # Service 포트 (내부)
    targetPort: 80     # Pod 포트
    nodePort: 30080    # 노드 포트 (30000-32767)
    protocol: TCP
EOF

kubectl apply -f nginx-nodeport.yaml

# 확인
echo "=== NodePort Service ==="
kubectl get service nginx-nodeport

# 출력:
# NAME             TYPE       CLUSTER-IP    EXTERNAL-IP   PORT(S)        AGE
# nginx-nodeport   NodePort   10.96.x.x     <none>        80:30080/TCP   5s
```

### 실습 6: NodePort 접근 테스트

```bash
# Minikube 환경
echo "=== Minikube에서 NodePort 접근 ==="

# 방법 1: minikube service 명령어
minikube service nginx-nodeport --url

# 방법 2: 직접 접근
MINIKUBE_IP=$(minikube ip)
echo "Minikube IP: $MINIKUBE_IP"
curl http://$MINIKUBE_IP:30080

# 방법 3: 터널링 (LoadBalancer 시뮬레이션)
# minikube tunnel  # 다른 터미널에서 실행
# curl http://localhost:80  # EXTERNAL-IP로 접근

# NodePort 특징
echo ""
echo "=== NodePort 특징 ==="
echo "1. 모든 노드에서 동일 포트(30080)로 접근 가능"
echo "2. Node IP:NodePort로 외부 접근"
echo "3. 테스트/개발 환경에 적합"
echo "4. 프로덕션에서는 LoadBalancer 또는 Ingress 권장"
```

### 실습 7: NodePort 자동 할당

```bash
# nodePort 생략 시 자동 할당
cat << 'EOF' > nginx-nodeport-auto.yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx-nodeport-auto
spec:
  type: NodePort
  selector:
    app: nginx
  ports:
  - port: 80
    targetPort: 80
    # nodePort 생략 → 30000-32767 중 자동 할당
EOF

kubectl apply -f nginx-nodeport-auto.yaml

# 할당된 포트 확인
kubectl get service nginx-nodeport-auto
# PORT(S) 컬럼에서 자동 할당된 포트 확인
```

---

## 🛠️ Part 4: 서비스 디스커버리 (45분)

### 4.1 DNS 기반 서비스 디스커버리

```
┌─────────────────────────────────────────────────────────────────────┐
│  Kubernetes DNS (CoreDNS)                                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Service DNS 형식:                                                   │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  <service-name>                                             │    │
│  │  <service-name>.<namespace>                                 │    │
│  │  <service-name>.<namespace>.svc                            │    │
│  │  <service-name>.<namespace>.svc.cluster.local  (FQDN)      │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  예시:                                                               │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  nginx-service                       # 같은 네임스페이스    │    │
│  │  nginx-service.default               # default 네임스페이스 │    │
│  │  nginx-service.default.svc.cluster.local  # FQDN          │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 실습 8: DNS 테스트

```bash
# DNS 해석 테스트
echo "=== DNS 해석 테스트 ==="

# 방법 1: nslookup
kubectl run dns-test --image=busybox:1.36 --rm -it --restart=Never -- \
  nslookup nginx-service

# 출력 예시:
# Server:    10.96.0.10  (CoreDNS)
# Address:   10.96.0.10:53
# Name:      nginx-service.default.svc.cluster.local
# Address:   10.96.xxx.xxx

# 방법 2: dig (상세)
kubectl run dns-test2 --image=tutum/dnsutils --rm -it --restart=Never -- \
  dig nginx-service.default.svc.cluster.local

# FQDN 테스트
echo ""
echo "=== FQDN 접근 테스트 ==="
kubectl run test-fqdn --image=curlimages/curl --rm -it --restart=Never -- \
  curl -s nginx-service.default.svc.cluster.local
```

### 실습 9: 환경 변수 기반 디스커버리

```bash
# Service 환경 변수 확인
echo "=== Service 환경 변수 ==="

# 새 Pod 생성 후 환경 변수 확인
kubectl run env-test --image=busybox:1.36 --rm -it --restart=Never -- \
  env | grep -i nginx

# 출력 예시:
# NGINX_SERVICE_SERVICE_HOST=10.96.xxx.xxx
# NGINX_SERVICE_SERVICE_PORT=80
# NGINX_SERVICE_PORT=tcp://10.96.xxx.xxx:80

# ⚠️ 환경 변수는 Service가 먼저 생성된 후 Pod가 생성되어야 주입됨
```

---

## 🛠️ Part 5: Endpoints 이해 (45분)

### 5.1 Endpoints란?

```
┌─────────────────────────────────────────────────────────────────────┐
│  Endpoints = Service가 연결된 실제 Pod IP:Port 목록                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Service                            Endpoints                        │
│  ┌─────────────────────┐           ┌─────────────────────┐          │
│  │ nginx-service       │           │ nginx-service       │          │
│  │ ClusterIP: 10.96.x  │ ────────→ │ 10.244.0.5:80      │          │
│  │ selector: app=nginx │           │ 10.244.0.6:80      │          │
│  └─────────────────────┘           │ 10.244.0.7:80      │          │
│                                    └─────────────────────┘          │
│                                                                      │
│  Pod가 추가/삭제되면 Endpoints 자동 업데이트                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 실습 10: Endpoints 확인

```bash
# Endpoints 확인
echo "=== Endpoints 확인 ==="
kubectl get endpoints nginx-service

# 예상 출력:
# NAME            ENDPOINTS                                      AGE
# nginx-service   10.244.0.5:80,10.244.0.6:80,10.244.0.7:80     10m

# 상세 정보
kubectl describe endpoints nginx-service

# Pod와 비교
echo ""
echo "=== Pod IP 비교 ==="
kubectl get pods -l app=nginx -o wide

# Endpoints와 Pod IP가 일치하는지 확인
```

### 실습 11: Endpoints 동적 업데이트

```bash
# 스케일 업
echo "=== 스케일 업 (3 → 5) ==="
kubectl scale deployment nginx --replicas=5

# Endpoints 변화 확인
sleep 5
kubectl get endpoints nginx-service

# 스케일 다운
echo ""
echo "=== 스케일 다운 (5 → 2) ==="
kubectl scale deployment nginx --replicas=2

# Endpoints 변화 확인
sleep 5
kubectl get endpoints nginx-service

# 원복
kubectl scale deployment nginx --replicas=3
```

### 실습 12: 수동 Endpoints (External Service)

```bash
# 외부 서비스를 Service로 매핑 (Selector 없이)
cat << 'EOF' > external-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: external-db
spec:
  type: ClusterIP
  ports:
  - port: 3306
    targetPort: 3306
# selector 없음 → Endpoints 수동 생성 필요
---
apiVersion: v1
kind: Endpoints
metadata:
  name: external-db    # Service 이름과 동일해야 함
subsets:
- addresses:
  - ip: 192.168.1.100  # 외부 DB IP
  - ip: 192.168.1.101
  ports:
  - port: 3306
EOF

kubectl apply -f external-service.yaml

# 확인
kubectl get service external-db
kubectl get endpoints external-db

# 사용: Pod 내에서 external-db:3306으로 접근
```

---

## 📊 Part 6: Service 고급 기능

### 6.1 Session Affinity

```yaml
# session-affinity.yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx-sticky
spec:
  selector:
    app: nginx
  ports:
  - port: 80
  sessionAffinity: ClientIP   # 같은 클라이언트는 같은 Pod로
  sessionAffinityConfig:
    clientIP:
      timeoutSeconds: 10800   # 3시간 유지
```

```bash
kubectl apply -f session-affinity.yaml

# 테스트: 같은 Pod로 연결되는지 확인
for i in {1..5}; do
  kubectl run test-sticky-$i --image=curlimages/curl --rm -it --restart=Never -- \
    curl -s nginx-sticky -H "Host: test" 2>/dev/null
done
```

### 6.2 Headless Service

```yaml
# headless-service.yaml
# ClusterIP: None → DNS가 Pod IP 직접 반환
apiVersion: v1
kind: Service
metadata:
  name: nginx-headless
spec:
  clusterIP: None            # Headless!
  selector:
    app: nginx
  ports:
  - port: 80
```

```bash
kubectl apply -f headless-service.yaml

# DNS 확인 (모든 Pod IP 반환)
kubectl run dns-test --image=busybox:1.36 --rm -it --restart=Never -- \
  nslookup nginx-headless

# 출력: 모든 Pod의 IP가 A 레코드로 반환됨
# StatefulSet과 함께 사용하여 개별 Pod 접근에 유용
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | Service 필요성 이해 | Pod IP 변경 문제 해결 | ☐ |
| 2 | ClusterIP Service 생성 | 내부 통신용 | ☐ |
| 3 | NodePort Service 생성 | 외부 노출용 | ☐ |
| 4 | DNS로 서비스 접근 | nslookup, FQDN | ☐ |
| 5 | Endpoints 확인 | Pod IP 목록 | ☐ |
| 6 | 스케일링과 Endpoints | 동적 업데이트 | ☐ |
| 7 | Session Affinity | 클라이언트 고정 | ☐ |
| 8 | Headless Service | StatefulSet용 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# Service 관리
kubectl get service
kubectl describe service <name>
kubectl expose deployment <name> --port=80 --type=ClusterIP

# Endpoints 확인
kubectl get endpoints <service>
kubectl describe endpoints <service>

# DNS 테스트
kubectl run dns-test --image=busybox --rm -it -- nslookup <service>

# Minikube 외부 접근
minikube service <name> --url
minikube tunnel  # LoadBalancer용
```

---

## 💡 면접 대비 핵심 포인트

### Q1: Kubernetes Service가 필요한 이유는?

**A**: "Pod IP는 재시작 시 변경되고, 여러 Pod가 있을 때 로드밸런싱이 필요합니다. Service는 고정된 ClusterIP와 DNS를 제공하여 안정적인 접근점을 만들고, 자동으로 트래픽을 분산합니다."

### Q2: ClusterIP, NodePort, LoadBalancer의 차이는?

**A**: 
- **ClusterIP**: 클러스터 내부에서만 접근 가능. 내부 서비스 간 통신용
- **NodePort**: 모든 노드의 특정 포트(30000-32767)로 외부 접근 가능. 개발/테스트용
- **LoadBalancer**: 클라우드 로드밸런서 생성. 프로덕션 외부 노출용

### Q3: Service와 Endpoints의 관계는?

**A**: "Service의 selector와 일치하는 Pod IP:Port를 모아 Endpoints가 자동 생성됩니다. Pod가 추가/삭제되면 Endpoints도 자동 업데이트됩니다. kube-proxy는 이 Endpoints를 보고 iptables 규칙을 설정합니다."

---

## 📝 학습 기록

```
학습일: ____년 __월 __일
실제 소요 시간: ____시간

실습 완료 항목:
- [ ] ClusterIP Service 생성
- [ ] NodePort Service 생성
- [ ] DNS 테스트
- [ ] Endpoints 확인
- [ ] 스케일링 테스트

이해가 어려웠던 부분:


추가 학습 필요 항목:

```

---

## ➡️ 다음 학습: Day 35

**주제**: Namespace
- 리소스 격리
- 환경 분리 (dev, staging, prod)
- ResourceQuota
