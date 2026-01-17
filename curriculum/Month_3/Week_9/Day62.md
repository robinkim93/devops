# 📅 Day 62: VirtualService 기초

## 🎯 오늘의 목표

> VirtualService로 트래픽 라우팅 제어

---

## ⏰ 예상 학습 시간: 3시간

---

## 📚 Part 1: VirtualService란? (30분)

### K8s Service vs Istio VirtualService

```
K8s Service:
- 기본 로드밸런싱
- 라벨 기반 Pod 선택

Istio VirtualService:
- 고급 트래픽 라우팅
- URL, 헤더, 가중치 기반 라우팅
- 타임아웃, 재시도 설정
```

### VirtualService 구조

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: my-service
spec:
  hosts:              # 적용 대상 호스트
  - my-service
  http:               # HTTP 라우팅 규칙
  - match:            # 매칭 조건 (선택)
    route:            # 목적지
    - destination:
        host: my-service
        subset: v1    # DestinationRule의 subset
```

---

## 🛠️ Part 2: 실습 (2.5시간)

### 실습 1: 모든 트래픽을 v1으로 (30분)

```bash
# 현재 Bookinfo에는 reviews가 v1, v2, v3 세 버전 있음
# 새로고침마다 랜덤하게 다른 버전으로 라우팅됨

# reviews v1만 사용하도록 설정
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: reviews
spec:
  hosts:
  - reviews
  http:
  - route:
    - destination:
        host: reviews
        subset: v1
EOF

# DestinationRule 필요 (subset 정의)
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: reviews
spec:
  host: reviews
  subsets:
  - name: v1
    labels:
      version: v1
  - name: v2
    labels:
      version: v2
  - name: v3
    labels:
      version: v3
EOF

# 브라우저에서 확인 - 항상 별점 없음 (v1)
```

### 실습 2: 가중치 기반 라우팅 (카나리 배포) (40분)

```bash
# 80% v1, 20% v2로 트래픽 분배
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: reviews
spec:
  hosts:
  - reviews
  http:
  - route:
    - destination:
        host: reviews
        subset: v1
      weight: 80
    - destination:
        host: reviews
        subset: v2
      weight: 20
EOF

# 브라우저에서 여러 번 새로고침
# 80%는 별점 없음 (v1), 20%는 검은 별 (v2)
```

### 실습 3: 헤더 기반 라우팅 (30분)

```bash
# 특정 사용자에게만 v3 제공
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: reviews
spec:
  hosts:
  - reviews
  http:
  - match:
    - headers:
        end-user:
          exact: jason    # jason 사용자만
    route:
    - destination:
        host: reviews
        subset: v3        # v3 (빨간 별) 제공
  - route:
    - destination:
        host: reviews
        subset: v1        # 나머지는 v1
EOF

# Bookinfo에서 jason으로 로그인하면 v3 (빨간 별) 표시
```

### 실습 4: URL 기반 라우팅 (30분)

```bash
# /api/v1/* 요청은 v1으로, /api/v2/* 요청은 v2로
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: my-service
spec:
  hosts:
  - my-service
  http:
  - match:
    - uri:
        prefix: /api/v1
    route:
    - destination:
        host: my-service
        subset: v1
  - match:
    - uri:
        prefix: /api/v2
    route:
    - destination:
        host: my-service
        subset: v2
EOF
```

### 실습 5: 여러 조건 조합 (20분)

```bash
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: reviews
spec:
  hosts:
  - reviews
  http:
  # 조건 1: jason 사용자 + v2 헤더 → v2
  - match:
    - headers:
        end-user:
          exact: jason
        x-version:
          exact: v2
    route:
    - destination:
        host: reviews
        subset: v2
  # 조건 2: jason 사용자 → v3
  - match:
    - headers:
        end-user:
          exact: jason
    route:
    - destination:
        host: reviews
        subset: v3
  # 기본: v1
  - route:
    - destination:
        host: reviews
        subset: v1
EOF
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | VirtualService 개념 이해 | ☐ |
| 2 | 단일 버전 라우팅 | ☐ |
| 3 | 가중치 기반 라우팅 (카나리) | ☐ |
| 4 | 헤더 기반 라우팅 | ☐ |
| 5 | URL 기반 라우팅 | ☐ |

---

## 🔑 VirtualService 핵심 필드

```yaml
spec:
  hosts: [...]        # 적용 대상 서비스
  http:
  - match:            # 매칭 조건
    - uri:            # URL 매칭
    - headers:        # 헤더 매칭
    route:            # 목적지
    - destination:
        host: xxx
        subset: v1    # DestinationRule의 subset
      weight: 80      # 가중치 (%)
```

---

## 📝 면접 대비

**Q: Istio에서 카나리 배포는 어떻게 구현하나요?**
> "VirtualService의 weight 필드를 사용합니다. 예를 들어 v1에 90%, v2에 10%를 설정하면 트래픽의 10%만 새 버전으로 라우팅됩니다. 문제가 없으면 점진적으로 비율을 늘립니다."

---

## ➡️ 다음 학습: Day 63

**주제**: DestinationRule 상세
