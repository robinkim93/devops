# 📅 Day 78: Egress 트래픽 제어

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "보안 컴플라이언스를 고려한 인프라 설계"
> 외부로 나가는 트래픽(Egress)을 제어하여 보안 강화

토스플레이스와 같은 금융 서비스에서는 외부 API 호출에 대한 통제가 필수입니다. Istio를 통해 허용된 외부 서비스만 접근하도록 제한할 수 있습니다.

---

## ⏰ 예상 학습 시간: 4시간

---

## 📚 Part 1: Egress 트래픽 개념 (1시간)

### 1.1 Egress 트래픽이란?

```
┌─────────────────────────────────────────────────────────────────────┐
│  Ingress vs Egress                                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Ingress (들어오는 트래픽):                                         │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  외부 사용자 ──▶ [Ingress Gateway] ──▶ 내부 서비스          │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Egress (나가는 트래픽):                                            │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  내부 서비스 ──▶ [?] ──▶ 외부 API/서비스                    │    │
│  │                                                             │    │
│  │  예시:                                                      │    │
│  │  • Payment Service → 결제 게이트웨이 API                    │    │
│  │  • Notification Service → Slack/Email API                   │    │
│  │  • Backend → 외부 데이터 제공자                             │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  왜 Egress 제어가 필요한가?                                         │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  1. 보안: 허용된 외부만 접근                                 │    │
│  │     - 데이터 유출 방지                                      │    │
│  │     - 악성 서버 접근 차단                                   │    │
│  │                                                             │    │
│  │  2. 모니터링: 외부 API 호출 추적                            │    │
│  │     - 비용 관리 (API 호출 비용)                             │    │
│  │     - 의존성 파악                                           │    │
│  │                                                             │    │
│  │  3. 트래픽 관리: 외부 호출에도 정책 적용                    │    │
│  │     - Timeout, Retry                                        │    │
│  │     - Rate Limiting                                         │    │
│  │     - Circuit Breaker                                       │    │
│  │                                                             │    │
│  │  4. 컴플라이언스: 금융 규제 준수                            │    │
│  │     - 외부 연결 감사 로그                                   │    │
│  │     - 승인된 서비스만 허용                                  │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 Istio Egress 제어 방식

| 방식 | 설명 | 사용 사례 |
|------|------|----------|
| **기본 (ALLOW_ANY)** | 모든 외부 접근 허용 | 개발 환경 |
| **ServiceEntry** | 특정 외부 서비스 등록 | 외부 API 명시적 정의 |
| **REGISTRY_ONLY** | 등록된 외부만 허용 | 보안 강화 환경 |
| **Egress Gateway** | 중앙 게이트웨이 경유 | 감사 로깅, 보안 |

### 1.3 Egress 아키텍처

```
┌─────────────────────────────────────────────────────────────────────┐
│  Egress 트래픽 흐름                                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  방식 1: 직접 접근 (ServiceEntry만)                                 │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Pod ──▶ Envoy Sidecar ──▶ 외부 서비스                      │    │
│  │          (ServiceEntry로 라우팅)                            │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  방식 2: Egress Gateway 경유 (권장)                                 │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Pod ──▶ Envoy Sidecar ──▶ Egress Gateway ──▶ 외부 서비스  │    │
│  │          (VirtualService)    (중앙 제어)                    │    │
│  │                                                             │    │
│  │  장점:                                                      │    │
│  │  • 중앙에서 모든 외부 트래픽 모니터링                       │    │
│  │  • 방화벽 규칙 단순화 (Egress Gateway IP만 허용)            │    │
│  │  • 감사 로깅 용이                                           │    │
│  │  • TLS 종료/시작 중앙 관리                                  │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: ServiceEntry 실습 (1.5시간)

### 실습 1: 현재 Egress 정책 확인

```bash
# 현재 outboundTrafficPolicy 확인
kubectl get configmap istio -n istio-system -o yaml | grep -A2 outboundTrafficPolicy

# 기본값: ALLOW_ANY (모든 외부 접근 허용)
# 보안 환경: REGISTRY_ONLY (등록된 외부만 허용)
```

### 실습 2: 외부 접근 테스트 (기본)

```bash
# sleep Pod 생성 (테스트용)
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/sleep/sleep.yaml

# 외부 HTTP 요청 테스트
kubectl exec -it $(kubectl get pod -l app=sleep -o jsonpath={.items..metadata.name}) \
  -c sleep -- curl -s httpbin.org/ip

# 예상: ALLOW_ANY이면 성공, 응답에 IP 표시
```

### 실습 3: ServiceEntry 생성

```yaml
# httpbin-serviceentry.yaml
apiVersion: networking.istio.io/v1beta1
kind: ServiceEntry
metadata:
  name: httpbin-external
spec:
  hosts:
  - httpbin.org           # 외부 호스트명
  ports:
  - number: 80
    name: http
    protocol: HTTP
  - number: 443
    name: https
    protocol: HTTPS
  location: MESH_EXTERNAL  # 메시 외부 서비스
  resolution: DNS          # DNS로 IP 해석
```

```bash
# ServiceEntry 적용
kubectl apply -f httpbin-serviceentry.yaml

# 확인
kubectl get serviceentry

# 테스트 (동일하게 동작)
kubectl exec -it $(kubectl get pod -l app=sleep -o jsonpath={.items..metadata.name}) \
  -c sleep -- curl -s httpbin.org/ip
```

### 실습 4: VirtualService로 트래픽 정책 적용

```yaml
# httpbin-virtualservice.yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: httpbin-external
spec:
  hosts:
  - httpbin.org
  http:
  - timeout: 3s           # 3초 타임아웃
    retries:
      attempts: 3
      perTryTimeout: 1s
      retryOn: gateway-error,connect-failure,refused-stream
    route:
    - destination:
        host: httpbin.org
        port:
          number: 80
```

```bash
# VirtualService 적용
kubectl apply -f httpbin-virtualservice.yaml

# 테스트 (타임아웃 적용됨)
kubectl exec -it $(kubectl get pod -l app=sleep -o jsonpath={.items..metadata.name}) \
  -c sleep -- curl -s -w "\n%{time_total}s\n" httpbin.org/delay/5

# 예상: 3초 후 타임아웃으로 실패
```

### 실습 5: DestinationRule 적용

```yaml
# httpbin-destinationrule.yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: httpbin-external
spec:
  host: httpbin.org
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        h2UpgradePolicy: UPGRADE
        http1MaxPendingRequests: 10
        http2MaxRequests: 100
    tls:
      mode: SIMPLE         # TLS 활성화 (외부 HTTPS)
```

```bash
# DestinationRule 적용
kubectl apply -f httpbin-destinationrule.yaml

# HTTPS 테스트
kubectl exec -it $(kubectl get pod -l app=sleep -o jsonpath={.items..metadata.name}) \
  -c sleep -- curl -s https://httpbin.org/ip
```

---

## 🛠️ Part 3: REGISTRY_ONLY 모드 (1시간)

### 실습 6: REGISTRY_ONLY 모드 활성화

```bash
# 방법 1: istioctl로 설정
istioctl install --set meshConfig.outboundTrafficPolicy.mode=REGISTRY_ONLY

# 방법 2: ConfigMap 직접 수정
kubectl edit configmap istio -n istio-system
# meshConfig:
#   outboundTrafficPolicy:
#     mode: REGISTRY_ONLY

# 변경 확인
kubectl get configmap istio -n istio-system -o yaml | grep -A2 outboundTrafficPolicy
```

### 실습 7: 등록되지 않은 외부 접근 테스트

```bash
# 등록된 httpbin.org - 성공
kubectl exec -it $(kubectl get pod -l app=sleep -o jsonpath={.items..metadata.name}) \
  -c sleep -- curl -s -o /dev/null -w "%{http_code}" httpbin.org/ip

# 예상: 200

# 등록되지 않은 google.com - 실패
kubectl exec -it $(kubectl get pod -l app=sleep -o jsonpath={.items..metadata.name}) \
  -c sleep -- curl -s -o /dev/null -w "%{http_code}" google.com

# 예상: 502 또는 연결 실패
```

### 실습 8: 추가 외부 서비스 등록

```yaml
# google-serviceentry.yaml
apiVersion: networking.istio.io/v1beta1
kind: ServiceEntry
metadata:
  name: google
spec:
  hosts:
  - "*.google.com"
  - "*.googleapis.com"
  ports:
  - number: 443
    name: https
    protocol: HTTPS
  - number: 80
    name: http
    protocol: HTTP
  location: MESH_EXTERNAL
  resolution: DNS
```

```bash
# 등록
kubectl apply -f google-serviceentry.yaml

# 테스트 - 이제 성공
kubectl exec -it $(kubectl get pod -l app=sleep -o jsonpath={.items..metadata.name}) \
  -c sleep -- curl -s -o /dev/null -w "%{http_code}" https://www.google.com
```

---

## 🛠️ Part 4: Egress Gateway (30분)

### 4.1 Egress Gateway 개념

```
┌─────────────────────────────────────────────────────────────────────┐
│  Egress Gateway 아키텍처                                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Kubernetes Cluster                                         │    │
│  │                                                             │    │
│  │  ┌───────────┐      ┌─────────────────┐                    │    │
│  │  │   Pod     │      │ Egress Gateway  │                    │    │
│  │  │ (Sidecar) │ ───▶ │    (istio)      │ ───▶ 외부 API     │    │
│  │  └───────────┘      └─────────────────┘                    │    │
│  │                            │                                │    │
│  │                            │                                │    │
│  │                     ┌──────┴──────┐                        │    │
│  │                     │ 감사 로깅    │                        │    │
│  │                     │ 트래픽 모니터│                        │    │
│  │                     │ 정책 적용    │                        │    │
│  │                     └─────────────┘                        │    │
│  │                                                             │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  장점:                                                              │
│  • 중앙 집중식 Egress 관리                                         │
│  • 방화벽 규칙 단순화                                              │
│  • 모든 외부 트래픽 로깅                                           │
│  • 네트워크 정책과 통합                                            │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.2 Egress Gateway 설정 예시

```yaml
# egress-gateway-config.yaml
apiVersion: networking.istio.io/v1beta1
kind: Gateway
metadata:
  name: egress-gateway
spec:
  selector:
    istio: egressgateway
  servers:
  - port:
      number: 80
      name: http
      protocol: HTTP
    hosts:
    - httpbin.org
---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: httpbin-egress
spec:
  hosts:
  - httpbin.org
  gateways:
  - mesh                     # 메시 내부 (Sidecar)
  - egress-gateway           # Egress Gateway
  http:
  # 1. 메시 내부에서 Egress Gateway로
  - match:
    - gateways:
      - mesh
    route:
    - destination:
        host: istio-egressgateway.istio-system.svc.cluster.local
        port:
          number: 80
  # 2. Egress Gateway에서 외부로
  - match:
    - gateways:
      - egress-gateway
    route:
    - destination:
        host: httpbin.org
        port:
          number: 80
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | Egress 트래픽 개념 이해 | ☐ |
| 2 | ServiceEntry 생성 | ☐ |
| 3 | VirtualService로 정책 적용 | ☐ |
| 4 | REGISTRY_ONLY 모드 테스트 | ☐ |
| 5 | 외부 서비스 등록/차단 확인 | ☐ |
| 6 | Egress Gateway 개념 이해 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# ServiceEntry 적용
kubectl apply -f serviceentry.yaml

# 외부 접근 테스트
kubectl exec -it <sleep-pod> -c sleep -- curl external.api.com

# Egress 정책 확인
kubectl get serviceentry,virtualservice,destinationrule

# outboundTrafficPolicy 확인
kubectl get configmap istio -n istio-system -o yaml | grep outboundTrafficPolicy
```

---

## 💡 면접 대비 핵심 포인트

### Q1: Egress 트래픽 제어가 왜 필요한가요?
**A**: 보안(데이터 유출 방지, 승인된 외부만 접근), 모니터링(외부 API 호출 추적), 트래픽 관리(타임아웃, 재시도), 컴플라이언스(감사 로깅) 등의 이유로 필요합니다.

### Q2: ServiceEntry란?
**A**: 메시 외부 서비스를 Istio 서비스 레지스트리에 등록하여 메시 내 서비스처럼 관리할 수 있게 하는 리소스입니다. VirtualService, DestinationRule 정책을 외부 서비스에도 적용할 수 있습니다.

### Q3: REGISTRY_ONLY 모드의 장점은?
**A**: 명시적으로 등록된 외부 서비스만 접근 가능하여 보안이 강화됩니다. 등록되지 않은 외부 접근은 자동 차단됩니다.

---

## ➡️ 다음 학습: Day 79

**주제**: Security 종합 실습
- Zero Trust 보안 모델
- mTLS, AuthorizationPolicy 통합
- 실제 시나리오 구현
