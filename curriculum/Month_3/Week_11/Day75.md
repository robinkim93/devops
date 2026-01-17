# 📅 Day 75: Istio mTLS

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "보안 컴플라이언스를 고려한 클라우드 인프라"
> "Kubernetes와 Service Mesh에 대한 경험"

서비스 간 상호 TLS 인증(mTLS)은 제로 트러스트 보안 모델의 핵심입니다. Istio를 통해 애플리케이션 코드 변경 없이 자동으로 mTLS를 적용하여 서비스 간 통신을 보호합니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| mTLS 이론 | 45분 | 개념, 동작 원리 |
| 기본 실습 | 1시간 | PERMISSIVE, STRICT |
| 고급 설정 | 1시간 | 포트별, 워크로드별 설정 |
| 트러블슈팅 | 1.25시간 | 인증서, 연결 문제 |

---

## 📚 Part 1: mTLS란? (45분)

### 1.1 TLS vs mTLS

```
┌─────────────────────────────────────────────────────────────────────┐
│  TLS vs Mutual TLS (mTLS) 비교                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  일반 TLS (단방향)                                                  │
│  ─────────────────────────────────────────────────────────────────  │
│  Client                             Server                          │
│    │                                  │                             │
│    │  ←── Server Certificate ──       │  서버만 인증서 제시        │
│    │      (서버 인증)                 │                             │
│    │                                  │                             │
│    │  ──── 암호화된 통신 ────→        │                             │
│    │                                  │                             │
│    ✓ 서버 신원 확인                  │                             │
│    ✗ 클라이언트 신원 미확인          │                             │
│                                                                      │
│  ═══════════════════════════════════════════════════════════════   │
│                                                                      │
│  Mutual TLS (mTLS, 양방향)                                          │
│  ─────────────────────────────────────────────────────────────────  │
│  Client                             Server                          │
│    │                                  │                             │
│    │  ←── Server Certificate ──       │  서버 인증서 제시          │
│    │                                  │                             │
│    │  ─── Client Certificate ───→     │  클라이언트 인증서 제시    │
│    │                                  │                             │
│    │  ←──── 암호화된 통신 ────→       │                             │
│    │                                  │                             │
│    ✓ 서버 신원 확인                  │                             │
│    ✓ 클라이언트 신원 확인            │                             │
│    ✓ 양방향 암호화                   │                             │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 Istio mTLS의 장점

```
┌─────────────────────────────────────────────────────────────────────┐
│  Istio mTLS가 제공하는 가치                                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. 자동 인증서 관리                                                │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • istiod가 각 워크로드에 자동으로 인증서 발급              │    │
│  │  • 인증서 자동 갱신 (기본 24시간)                           │    │
│  │  • 개발자 개입 불필요                                        │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  2. 투명한 암호화                                                   │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • Envoy Sidecar가 자동으로 TLS 핸드셰이크 처리             │    │
│  │  • 애플리케이션 코드 변경 없음                               │    │
│  │  • HTTP로 개발해도 자동으로 HTTPS로 통신                     │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  3. 서비스 신원 확인 (SPIFFE)                                       │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • 각 서비스에 고유한 ID 부여                                │    │
│  │  • spiffe://cluster.local/ns/<ns>/sa/<sa>                   │    │
│  │  • AuthorizationPolicy에서 신원 기반 접근 제어              │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  4. 제로 트러스트 보안                                              │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • 네트워크 위치가 아닌 신원 기반 신뢰                       │    │
│  │  • 내부 통신도 암호화                                        │    │
│  │  • 침해 시 측면 이동 방지                                    │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.3 Istio mTLS 동작 원리

```
┌─────────────────────────────────────────────────────────────────────┐
│  Istio mTLS 동작 흐름                                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. 인증서 발급                                                     │
│                                                                      │
│  ┌──────────┐                    ┌──────────┐                       │
│  │  istiod  │                    │   Pod    │                       │
│  │  (CA)    │                    │ (Envoy)  │                       │
│  └────┬─────┘                    └────┬─────┘                       │
│       │                               │                              │
│       │  ←── CSR (인증서 요청) ──     │                              │
│       │                               │                              │
│       │  ─── Certificate ───────→     │                              │
│       │  (X.509 인증서 + 개인키)      │                              │
│                                                                      │
│  2. mTLS 핸드셰이크                                                 │
│                                                                      │
│  ┌──────────┐                    ┌──────────┐                       │
│  │ Service A│                    │ Service B│                       │
│  │ (Client) │                    │ (Server) │                       │
│  └────┬─────┘                    └────┬─────┘                       │
│       │                               │                              │
│       │ ──── ClientHello ────────→    │                              │
│       │ ←─── ServerHello + Cert ──    │                              │
│       │ ──── Client Cert ────────→    │ (양방향 인증!)               │
│       │ ←─── Finished ───────────     │                              │
│       │                               │                              │
│       │ ←──── 암호화된 HTTP ────→     │                              │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.4 mTLS 모드

| 모드 | 설명 | 사용 케이스 |
|------|------|------------|
| **PERMISSIVE** | mTLS + 평문 모두 허용 | 마이그레이션 중, 외부 연동 |
| **STRICT** | mTLS만 허용 | 프로덕션 보안 강화 |
| **DISABLE** | mTLS 비활성화 | 특정 워크로드 예외 |

---

## 🛠️ Part 2: 기본 실습 (1시간)

### 실습 1: 현재 mTLS 상태 확인

```bash
# Istio 설치 시 기본 mTLS 모드 확인
kubectl get meshconfig -n istio-system -o yaml | grep -A 5 mtls

# PeerAuthentication 정책 확인
kubectl get peerauthentication -A

# 특정 네임스페이스의 mTLS 상태
istioctl x check-inject -n default

# Pod 간 mTLS 상태 확인
istioctl x authz check productpage-xxx -n default

# Envoy 설정에서 TLS 확인
istioctl proxy-config clusters productpage-xxx -n default -o json | \
  jq '.[] | select(.name | contains("reviews")) | .transportSocket'
```

### 실습 2: PERMISSIVE 모드 (기본)

```yaml
# permissive-mtls.yaml
# mTLS + 평문 모두 허용 (마이그레이션에 적합)
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: default
spec:
  mtls:
    mode: PERMISSIVE
```

```bash
# 적용
kubectl apply -f permissive-mtls.yaml

# 확인
kubectl get peerauthentication -n default

# 테스트: Sidecar 없는 Pod에서 요청
kubectl run test-no-sidecar --image=curlimages/curl --rm -it \
  --overrides='{"metadata":{"annotations":{"sidecar.istio.io/inject":"false"}}}' \
  -- curl http://productpage:9080/productpage -s -o /dev/null -w "%{http_code}"

# PERMISSIVE에서는 200 OK (평문 허용)
```

### 실습 3: STRICT 모드 적용

```yaml
# strict-mtls.yaml
# mTLS만 허용 (프로덕션 권장)
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: default
spec:
  mtls:
    mode: STRICT
```

```bash
# 적용
kubectl apply -f strict-mtls.yaml

# 확인
kubectl get peerauthentication -n default

# 테스트: Sidecar 없는 Pod에서 요청
kubectl run test-no-sidecar --image=curlimages/curl --rm -it \
  --overrides='{"metadata":{"annotations":{"sidecar.istio.io/inject":"false"}}}' \
  -- curl http://productpage:9080/productpage -s -o /dev/null -w "%{http_code}"

# STRICT에서는 실패 (connection refused 또는 56)
# → mTLS 없는 요청 거부됨

# Sidecar 있는 Pod에서는 정상
kubectl run test-with-sidecar --image=curlimages/curl --rm -it \
  -- curl http://productpage:9080/productpage -s -o /dev/null -w "%{http_code}"
# → 200 OK (자동 mTLS)
```

### 실습 4: 메시 전체 STRICT 적용

```yaml
# mesh-strict-mtls.yaml
# 전체 메시에 STRICT 적용
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: istio-system  # istio-system에 적용 = 전체 메시
spec:
  mtls:
    mode: STRICT
```

```bash
# 적용
kubectl apply -f mesh-strict-mtls.yaml

# 모든 네임스페이스에 영향
kubectl get peerauthentication -A

# 네임스페이스별로 오버라이드 가능
```

---

## 🛠️ Part 3: 고급 설정 (1시간)

### 실습 5: 워크로드별 설정

```yaml
# workload-mtls.yaml
# 특정 워크로드만 다른 모드 적용
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: reviews-permissive
  namespace: default
spec:
  selector:
    matchLabels:
      app: reviews    # reviews 앱만 적용
  mtls:
    mode: PERMISSIVE  # 이 워크로드만 PERMISSIVE
```

```bash
# 네임스페이스 기본: STRICT
kubectl apply -f - <<EOF
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: default
spec:
  mtls:
    mode: STRICT
EOF

# reviews만 PERMISSIVE
kubectl apply -f workload-mtls.yaml

# 확인
kubectl get peerauthentication -n default

# reviews는 평문도 허용, 나머지는 STRICT
```

### 실습 6: 포트별 mTLS 설정

```yaml
# port-level-mtls.yaml
# 포트별로 다른 mTLS 모드 적용
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: legacy-app-mtls
  namespace: default
spec:
  selector:
    matchLabels:
      app: legacy-app
  mtls:
    mode: STRICT       # 기본은 STRICT
  portLevelMtls:
    8080:
      mode: PERMISSIVE  # 8080 포트만 평문 허용
    8443:
      mode: STRICT      # 8443은 mTLS 필수
    9090:
      mode: DISABLE     # 9090은 mTLS 완전 비활성화
```

```bash
# 적용
kubectl apply -f port-level-mtls.yaml

# 레거시 시스템 연동이나 헬스체크용 포트에 유용
```

### 실습 7: DestinationRule과 연동

```yaml
# destination-rule-mtls.yaml
# 클라이언트 측 mTLS 설정
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: reviews-mtls
  namespace: default
spec:
  host: reviews.default.svc.cluster.local
  trafficPolicy:
    tls:
      mode: ISTIO_MUTUAL  # Istio의 자동 mTLS 사용
      # 다른 옵션:
      # mode: SIMPLE      # 클라이언트만 TLS (서버 인증서 검증)
      # mode: MUTUAL      # 수동 mTLS (인증서 직접 지정)
      # mode: DISABLE     # TLS 비활성화
```

**TLS 모드 설명:**

| 모드 | 설명 |
|------|------|
| DISABLE | 평문 통신 |
| SIMPLE | 단방향 TLS (서버 인증서만) |
| MUTUAL | 양방향 TLS (인증서 직접 지정) |
| ISTIO_MUTUAL | Istio 자동 mTLS |

### 실습 8: 외부 서비스 연동

```yaml
# 외부 서비스에 TLS 설정
apiVersion: networking.istio.io/v1beta1
kind: ServiceEntry
metadata:
  name: external-api
spec:
  hosts:
  - api.external.com
  ports:
  - number: 443
    name: https
    protocol: HTTPS
  resolution: DNS
  location: MESH_EXTERNAL
---
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: external-api-tls
spec:
  host: api.external.com
  trafficPolicy:
    tls:
      mode: SIMPLE  # 외부 서비스는 단방향 TLS
      sni: api.external.com
```

---

## 🛠️ Part 4: 인증서 관리 (30분)

### 4.1 인증서 확인

```bash
# Pod의 인증서 정보 확인
istioctl proxy-config secret productpage-xxx -n default

# Envoy에서 인증서 직접 확인
kubectl exec productpage-xxx -c istio-proxy -- \
  curl localhost:15000/certs | jq

# 인증서 체인 확인
kubectl exec productpage-xxx -c istio-proxy -- \
  openssl s_client -connect reviews:9080 -showcerts

# SPIFFE ID 확인
kubectl exec productpage-xxx -c istio-proxy -- \
  curl localhost:15000/certs | jq '.[0].cert_chain[0].subject_alt_names'
```

### 4.2 인증서 갱신

```bash
# 인증서 만료 시간 확인
kubectl exec productpage-xxx -c istio-proxy -- \
  cat /var/run/secrets/istio/root-cert.pem | \
  openssl x509 -noout -dates

# 인증서 강제 갱신 (Pod 재시작)
kubectl rollout restart deployment productpage -n default

# istiod CA 상태 확인
kubectl logs -n istio-system -l app=istiod | grep -i cert
```

### 4.3 사용자 정의 CA

```yaml
# 사용자 정의 CA 사용 (Vault, cert-manager 연동)
apiVersion: install.istio.io/v1alpha1
kind: IstioOperator
spec:
  meshConfig:
    certificates:
    - secretName: cacerts
      dnsNames:
      - istiod.istio-system.svc
```

---

## 🛠️ Part 5: 트러블슈팅 (45분)

### 5.1 일반적인 mTLS 문제

```bash
# 문제 1: 연결 거부 (STRICT 모드)
# 증상: connection refused, EOF
# 원인: 클라이언트에 Sidecar 없음

# 진단
kubectl get pod <pod> -o jsonpath='{.spec.containers[*].name}'
# istio-proxy가 없으면 Sidecar 없음

# 해결
kubectl label namespace <ns> istio-injection=enabled
kubectl rollout restart deployment <deployment>

# 문제 2: TLS 핸드셰이크 실패
# 증상: SSL handshake failed

# 진단
istioctl proxy-config clusters <pod> -o json | \
  jq '.[] | select(.name | contains("target")) | .transportSocket'

# DestinationRule의 tls 모드 확인
kubectl get dr -n <namespace> -o yaml | grep -A 5 tls

# 해결: PeerAuthentication과 DestinationRule 일치시키기

# 문제 3: 인증서 만료
# 증상: certificate has expired

# 진단
istioctl proxy-config secret <pod> -n <namespace>

# 해결
kubectl rollout restart deployment <deployment>
```

### 5.2 디버깅 명령어

```bash
# mTLS 상태 종합 확인
istioctl authn tls-check <pod> <service>

# 예시:
istioctl authn tls-check productpage-xxx.default reviews.default.svc.cluster.local

# 출력:
# HOST:PORT                         STATUS  SERVER   CLIENT   AUTHN POLICY  DESTINATION RULE
# reviews.default.svc.cluster.local OK      STRICT   STRICT   default/      reviews/default

# Envoy 로그에서 TLS 에러 확인
kubectl logs <pod> -c istio-proxy | grep -i "tls\|ssl\|cert"

# istiod 로그 확인
kubectl logs -n istio-system -l app=istiod | grep -i "tls\|cert\|auth"

# 메시 전체 mTLS 상태 시각화 (Kiali)
istioctl dashboard kiali
# Graph → Display → Security 활성화
```

### 5.3 단계별 디버깅 플로우

```
┌─────────────────────────────────────────────────────────────────────┐
│  mTLS 문제 디버깅 플로우                                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  [Step 1] 연결 테스트                                               │
│  $ kubectl exec <client-pod> -- curl <service>:<port>               │
│  → 성공하면 mTLS 정상, 실패하면 다음 단계                           │
│                                                                      │
│  [Step 2] Sidecar 확인                                              │
│  $ kubectl get pod <pod> -o jsonpath='{.spec.containers[*].name}'   │
│  → istio-proxy 있는지 확인                                          │
│                                                                      │
│  [Step 3] PeerAuthentication 확인                                   │
│  $ kubectl get peerauthentication -A                                │
│  → STRICT/PERMISSIVE 모드 확인                                      │
│                                                                      │
│  [Step 4] DestinationRule 확인                                      │
│  $ kubectl get dr -A -o yaml | grep -A 5 tls                        │
│  → tls.mode 확인                                                     │
│                                                                      │
│  [Step 5] TLS 검사                                                  │
│  $ istioctl authn tls-check <pod> <service>                         │
│  → SERVER, CLIENT, AUTHN POLICY 일치 확인                           │
│                                                                      │
│  [Step 6] 인증서 확인                                               │
│  $ istioctl proxy-config secret <pod>                               │
│  → 인증서 만료, 유효성 확인                                         │
│                                                                      │
│  [Step 7] 로그 확인                                                 │
│  $ kubectl logs <pod> -c istio-proxy | grep -i tls                  │
│  → TLS 핸드셰이크 에러 확인                                         │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | mTLS 개념 이해 | 단방향 vs 양방향 TLS | ☐ |
| 2 | PERMISSIVE vs STRICT 차이 | 모드별 동작 | ☐ |
| 3 | PeerAuthentication 설정 | 네임스페이스, 워크로드별 | ☐ |
| 4 | STRICT 모드 테스트 | Sidecar 없는 요청 거부 | ☐ |
| 5 | 포트별 mTLS 설정 | portLevelMtls | ☐ |
| 6 | DestinationRule 연동 | 클라이언트 측 TLS | ☐ |
| 7 | 인증서 확인 | istioctl proxy-config secret | ☐ |
| 8 | 트러블슈팅 | tls-check, 로그 분석 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# mTLS 상태 확인
istioctl authn tls-check <pod> <service>
istioctl proxy-config secret <pod>
kubectl get peerauthentication -A
kubectl get destinationrule -A

# PeerAuthentication 적용
kubectl apply -f - <<EOF
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: default
spec:
  mtls:
    mode: STRICT
EOF

# 디버깅
kubectl logs <pod> -c istio-proxy | grep -i tls
istioctl proxy-config clusters <pod> -o json | jq '.[] | .transportSocket'
```

---

## 💡 면접 대비 핵심 포인트

### Q1: Istio에서 서비스 간 통신을 어떻게 보호하나요?

**A**: "Istio는 mTLS(상호 TLS)를 자동으로 적용합니다.

1. **인증서 발급**: istiod(CA)가 각 서비스에 X.509 인증서 자동 발급
2. **자동 암호화**: Envoy Sidecar가 모든 서비스 간 통신을 암호화
3. **양방향 인증**: 클라이언트와 서버 모두 인증서로 신원 확인
4. **SPIFFE ID**: 각 서비스에 고유 ID 부여 (spiffe://cluster.local/ns/...)

STRICT 모드로 설정하면 mTLS 없는 요청은 거부되어 제로 트러스트 보안을 구현합니다."

### Q2: PERMISSIVE와 STRICT 모드의 차이는?

**A**: 
- **PERMISSIVE**: mTLS와 평문 모두 허용. 마이그레이션 기간이나 외부 연동에 사용
- **STRICT**: mTLS만 허용. 프로덕션 보안 강화에 사용

점진적으로 PERMISSIVE에서 STRICT로 전환하는 것이 권장됩니다."

### Q3: mTLS 연결이 실패할 때 어떻게 디버깅하나요?

**A**: "단계적으로 확인합니다:

1. `istioctl authn tls-check`로 TLS 상태 확인
2. Sidecar 존재 여부 확인 (istio-proxy 컨테이너)
3. PeerAuthentication과 DestinationRule 일치 확인
4. `istioctl proxy-config secret`로 인증서 유효성 확인
5. Envoy 로그에서 TLS 에러 확인

대부분 Sidecar 누락, 정책 불일치, 인증서 만료가 원인입니다."

---

## 📝 학습 기록

```
학습일: ____년 __월 __일
실제 소요 시간: ____시간

실습 완료 항목:
- [ ] PERMISSIVE 모드 테스트
- [ ] STRICT 모드 테스트
- [ ] 워크로드별 설정
- [ ] 인증서 확인
- [ ] 트러블슈팅 실습

이해가 어려웠던 부분:


추가 학습 필요 항목:

```

---

## ➡️ 다음 학습: Day 76

**주제**: AuthorizationPolicy
- RBAC 기반 접근 제어
- 서비스 간 권한 관리
- 제로 트러스트 구현
