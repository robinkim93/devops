# 📅 Day 50: NetworkPolicy - 네트워크 보안

## 🎯 오늘의 목표

> **토스플레이스 핵심**: 보안 컴플라이언스를 고려한 Pod 간 네트워크 트래픽 제어를 구현합니다.

---

## ⏰ 예상 학습 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 | 45분 | NetworkPolicy 이해 |
| 기본 실습 | 1시간 | Deny All, Allow |
| 고급 실습 | 45분 | Egress, 네임스페이스 |
| 시나리오 | 30분 | 실전 보안 설정 |

---

## 📚 Part 1: NetworkPolicy 개념

### NetworkPolicy란?

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     NetworkPolicy 개요                                       │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   NetworkPolicy가 없는 경우 (기본):                                          │
│   ┌──────┐     ┌──────┐     ┌──────┐                                        │
│   │ Pod A│◀───▶│ Pod B│◀───▶│ Pod C│  모든 Pod가 서로 통신 가능              │
│   └──────┘     └──────┘     └──────┘                                        │
│                                                                              │
│   NetworkPolicy 적용 후:                                                     │
│   ┌──────┐     ┌──────┐     ┌──────┐                                        │
│   │ Pod A│─────▶│ Pod B│     │ Pod C│  허용된 트래픽만 통과                   │
│   └──────┘  ✓  └──────┘  ✗  └──────┘                                        │
│                                                                              │
│   핵심: Zero Trust Network = 기본 차단 + 명시적 허용                         │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### NetworkPolicy 구성요소

| 구성요소 | 설명 |
|---------|------|
| **podSelector** | 정책이 적용될 Pod 선택 |
| **policyTypes** | Ingress, Egress 또는 둘 다 |
| **ingress** | 들어오는 트래픽 규칙 |
| **egress** | 나가는 트래픽 규칙 |

### 동작 원리

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     NetworkPolicy 동작                                       │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   1. podSelector로 대상 Pod 선택                                             │
│                                                                              │
│   2. policyTypes 지정:                                                       │
│      - Ingress: 들어오는 트래픽 제어                                         │
│      - Egress: 나가는 트래픽 제어                                            │
│                                                                              │
│   3. 규칙이 없으면 해당 방향 트래픽 전면 차단                                  │
│                                                                              │
│   4. 규칙이 있으면 명시된 트래픽만 허용                                        │
│                                                                              │
│   주의: NetworkPolicy는 누적됨 (OR 조건)                                      │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: 기본 실습

### 테스트 환경 구성

```bash
# Namespace 생성
kubectl create namespace netpol-test

# nginx Pod 배포
kubectl run nginx --image=nginx --labels="app=nginx" -n netpol-test
kubectl expose pod nginx --port=80 -n netpol-test

# 통신 테스트 (정책 없음 = 성공)
kubectl run test --image=busybox --rm -it -n netpol-test -- wget -qO- --timeout=3 nginx
# 예상: nginx 응답
```

### Deny All Ingress 정책

```yaml
# deny-all-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: deny-all-ingress
  namespace: netpol-test
spec:
  podSelector: {}           # 모든 Pod에 적용
  policyTypes:
  - Ingress                 # Ingress만 차단
  # ingress 규칙이 없음 = 모든 Ingress 차단
```

```bash
# 적용
kubectl apply -f deny-all-ingress.yaml

# 테스트 (실패해야 함)
kubectl run test --image=busybox --rm -it -n netpol-test -- wget -qO- --timeout=3 nginx
# 예상: timeout
```

### 특정 Pod만 허용

```yaml
# allow-from-frontend.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-from-frontend
  namespace: netpol-test
spec:
  podSelector:
    matchLabels:
      app: nginx            # nginx Pod에 적용
  policyTypes:
  - Ingress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          role: frontend    # frontend 라벨을 가진 Pod만 허용
    ports:
    - protocol: TCP
      port: 80
```

```bash
# 적용
kubectl apply -f allow-from-frontend.yaml

# 라벨 없는 Pod: 실패
kubectl run test1 --image=busybox --rm -it -n netpol-test -- wget -qO- --timeout=3 nginx
# timeout

# 라벨 있는 Pod: 성공
kubectl run test2 --image=busybox --labels="role=frontend" --rm -it -n netpol-test -- wget -qO- --timeout=3 nginx
# nginx 응답
```

---

## 🛠️ Part 3: 고급 설정

### Egress 정책 (외부 트래픽 제어)

```yaml
# deny-all-egress.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: deny-all-egress
  namespace: netpol-test
spec:
  podSelector: {}
  policyTypes:
  - Egress
  # egress 규칙이 없음 = 모든 Egress 차단
```

### DNS 허용 (Egress 차단 시 필수)

```yaml
# allow-dns.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-dns
  namespace: netpol-test
spec:
  podSelector: {}
  policyTypes:
  - Egress
  egress:
  - to:
    - namespaceSelector: {}
      podSelector:
        matchLabels:
          k8s-app: kube-dns
    ports:
    - protocol: UDP
      port: 53
    - protocol: TCP
      port: 53
```

### 네임스페이스 기반 정책

```yaml
# allow-from-namespace.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-from-monitoring
  namespace: netpol-test
spec:
  podSelector:
    matchLabels:
      app: nginx
  policyTypes:
  - Ingress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: monitoring      # monitoring 네임스페이스에서만 허용
    ports:
    - protocol: TCP
      port: 80
```

### CIDR 기반 정책 (외부 IP)

```yaml
# allow-external-api.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-external-api
  namespace: netpol-test
spec:
  podSelector:
    matchLabels:
      app: backend
  policyTypes:
  - Egress
  egress:
  - to:
    - ipBlock:
        cidr: 10.0.0.0/8         # 내부 네트워크
    - ipBlock:
        cidr: 203.0.113.0/24     # 특정 외부 API
        except:
        - 203.0.113.50/32        # 제외할 IP
    ports:
    - protocol: TCP
      port: 443
```

---

## 🛠️ Part 4: 실전 보안 시나리오

### 3-Tier 애플리케이션 보안

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     3-Tier Architecture                                      │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   [Ingress]                                                                  │
│       │                                                                      │
│       ▼                                                                      │
│   ┌──────────┐                                                              │
│   │ Frontend │  ← 외부에서 접근 가능                                         │
│   └────┬─────┘                                                              │
│        │                                                                     │
│        ▼                                                                     │
│   ┌──────────┐                                                              │
│   │   API    │  ← Frontend에서만 접근 가능                                   │
│   └────┬─────┘                                                              │
│        │                                                                     │
│        ▼                                                                     │
│   ┌──────────┐                                                              │
│   │ Database │  ← API에서만 접근 가능                                        │
│   └──────────┘                                                              │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 구현

```yaml
# 1. 기본 Deny All
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny
  namespace: production
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
---
# 2. DNS 허용 (필수)
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-dns
  namespace: production
spec:
  podSelector: {}
  policyTypes:
  - Egress
  egress:
  - to:
    - namespaceSelector: {}
      podSelector:
        matchLabels:
          k8s-app: kube-dns
    ports:
    - protocol: UDP
      port: 53
---
# 3. Frontend: Ingress에서 접근 허용
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: frontend-ingress
  namespace: production
spec:
  podSelector:
    matchLabels:
      tier: frontend
  policyTypes:
  - Ingress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
    ports:
    - protocol: TCP
      port: 80
---
# 4. API: Frontend에서만 접근 허용
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: api-ingress
  namespace: production
spec:
  podSelector:
    matchLabels:
      tier: api
  policyTypes:
  - Ingress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          tier: frontend
    ports:
    - protocol: TCP
      port: 8080
---
# 5. Database: API에서만 접근 허용
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: database-ingress
  namespace: production
spec:
  podSelector:
    matchLabels:
      tier: database
  policyTypes:
  - Ingress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          tier: api
    ports:
    - protocol: TCP
      port: 5432
---
# 6. Frontend → API Egress
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: frontend-egress
  namespace: production
spec:
  podSelector:
    matchLabels:
      tier: frontend
  policyTypes:
  - Egress
  egress:
  - to:
    - podSelector:
        matchLabels:
          tier: api
    ports:
    - protocol: TCP
      port: 8080
---
# 7. API → Database Egress
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: api-egress
  namespace: production
spec:
  podSelector:
    matchLabels:
      tier: api
  policyTypes:
  - Egress
  egress:
  - to:
    - podSelector:
        matchLabels:
          tier: database
    ports:
    - protocol: TCP
      port: 5432
```

---

## 📋 트러블슈팅

### NetworkPolicy가 작동하지 않을 때

```bash
# 1. CNI 플러그인 확인 (Calico, Cilium 등 필요)
kubectl get pods -n kube-system | grep -E "calico|cilium"

# 2. NetworkPolicy 확인
kubectl get networkpolicy -n <namespace>
kubectl describe networkpolicy <name> -n <namespace>

# 3. Pod 라벨 확인
kubectl get pods -n <namespace> --show-labels

# 4. Namespace 라벨 확인
kubectl get namespaces --show-labels
```

### 테스트 방법

```bash
# 임시 Pod로 연결 테스트
kubectl run test --image=nicolaka/netshoot --rm -it -n <namespace> -- \
  curl -v --connect-timeout 3 http://<service>:<port>

# tcpdump로 패킷 확인
kubectl debug <pod> -it --image=nicolaka/netshoot -- tcpdump -i any
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 명령어/설정 | 완료 |
|---|------|-----------|------|
| 1 | 테스트 환경 구성 | `kubectl run nginx` | ☐ |
| 2 | Deny All 정책 | `podSelector: {}` | ☐ |
| 3 | 특정 Pod 허용 | `podSelector.matchLabels` | ☐ |
| 4 | Egress 정책 | `policyTypes: Egress` | ☐ |
| 5 | DNS 허용 | `kube-dns` 접근 허용 | ☐ |
| 6 | 네임스페이스 기반 | `namespaceSelector` | ☐ |
| 7 | 3-Tier 보안 구현 | 전체 시나리오 | ☐ |

---

## 🔑 핵심 명령어 요약

```bash
# NetworkPolicy 조회
kubectl get networkpolicy -n <namespace>
kubectl describe networkpolicy <name>

# 테스트
kubectl run test --image=busybox --rm -it -n <namespace> -- wget -qO- --timeout=3 <service>

# 라벨 확인
kubectl get pods --show-labels
kubectl get namespaces --show-labels
```

---

## ➡️ 다음 학습: Day 51

**주제**: Week 7 복습 - 운영 역량 종합

