# 📅 Day 90: GitHub 업로드 & Month 3 완료

## 🎯 오늘의 목표

> **토스플레이스 핵심**: Month 3 학습을 완료하고 Istio 포트폴리오를 GitHub에 업로드합니다.

---

## ⏰ 예상 소요 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| GitHub 업로드 | 1시간 | 저장소 설정, 커밋 |
| Month 3 정리 | 1시간 | 학습 내용 복습 |
| 면접 대비 | 1시간 | Q&A 준비 |

---

## 📤 Part 1: GitHub 업로드

### 1.1 저장소 준비

```bash
cd ~/portfolio/istio-project

# Git 초기화
git init

# .gitignore 작성
cat << 'EOF' > .gitignore
# OS
.DS_Store
Thumbs.db

# Editor
.idea/
.vscode/
*.swp

# Logs
*.log

# Secrets (절대 커밋하지 않음)
*secret*.yaml
*.key
*.pem

# Terraform (있다면)
*.tfstate
*.tfstate.*
.terraform/
EOF
```

### 1.2 커밋 및 푸시

```bash
# 파일 추가
git add .

# 커밋 (의미있는 메시지)
git commit -m "feat: Istio Service Mesh 포트폴리오

📌 구현 기능:
- VirtualService: 카나리 배포 (가중치 기반 90:10)
- DestinationRule: Circuit Breaker, Subset
- PeerAuthentication: mTLS STRICT
- AuthorizationPolicy: Zero Trust (DENY ALL + ALLOW)
- Observability: Kiali, Jaeger, Grafana 통합

🎯 토스플레이스 DevOps Engineer 포지션 대비"

# 원격 저장소 연결
git remote add origin https://github.com/YOUR_USERNAME/istio-portfolio.git

# 푸시
git branch -M main
git push -u origin main
```

### 1.3 GitHub 저장소 설정

```markdown
# 저장소 설정 체크리스트

- [ ] Repository 생성 (Public)
- [ ] Description 추가: "Production-grade Istio Service Mesh implementation"
- [ ] Topics 추가: istio, kubernetes, service-mesh, devops, observability
- [ ] README.md 확인 (자동 렌더링)
- [ ] About 섹션 업데이트
```

---

## 📋 Part 2: Month 3 학습 정리

### Week 9: Istio 기초

| Day | 주제 | 핵심 내용 | 토스플레이스 연결 |
|-----|------|----------|-----------------|
| 61 | Service Mesh 개념 | Sidecar, Envoy, Data/Control Plane | 마이크로서비스 관리 |
| 62 | Istio 설치 | istioctl, Injection | 인프라 구축 |
| 63 | VirtualService | 트래픽 라우팅, 가중치 | 카나리 배포 |
| 64 | DestinationRule | Subset, Load Balancing | 서비스 버전 관리 |
| 65 | Gateway | 외부 트래픽 관리 | Ingress 설정 |
| 66 | Fault Injection | 장애 테스트 | 시스템 검증 |
| 67 | Week 9 복습 | 종합 실습 | - |

### Week 10: Observability

| Day | 주제 | 핵심 내용 | 토스플레이스 연결 |
|-----|------|----------|-----------------|
| 68 | Kiali | 서비스 토폴로지 시각화 | 모니터링 |
| 69 | Jaeger | 분산 추적 (Trace/Span) | 장애 분석 |
| 70 | Prometheus/Grafana | Istio 메트릭, 대시보드 | SLI/SLO |
| 71 | Access Logging | Envoy 로그, Response Flags | 트러블슈팅 |
| 72 | Envoy 트러블슈팅 | istioctl proxy-config | 문제 해결 |
| 73 | 종합 실습 | 장애 분석 시나리오 | 실전 대응 |
| 74 | Week 10 복습 | 관찰성 종합 | - |

### Week 11: Security

| Day | 주제 | 핵심 내용 | 토스플레이스 연결 |
|-----|------|----------|-----------------|
| 75 | mTLS 기초 | PeerAuthentication | 통신 암호화 |
| 76 | mTLS 심화 | STRICT, PERMISSIVE | 보안 정책 |
| 77 | AuthorizationPolicy | DENY ALL, ALLOW | 접근 제어 |
| 78 | JWT 인증 | RequestAuthentication | API 보안 |
| 79 | Egress 제어 | ServiceEntry | 외부 통신 관리 |
| 80 | Rate Limiting | EnvoyFilter | 트래픽 제한 |
| 81 | Week 11 복습 | 보안 종합 | - |

### Week 12: 프로젝트

| Day | 주제 | 핵심 내용 |
|-----|------|----------|
| 82 | 프로젝트 설계 | 아키텍처, 요구사항 |
| 83 | 기본 배포 | Namespace, Deployment |
| 84 | 트래픽 관리 | VirtualService, DestinationRule |
| 85 | 보안 | mTLS, AuthorizationPolicy |
| 86 | 관찰성 | Kiali, Jaeger, Grafana |
| 87 | 장애 복원력 | Circuit Breaker, Retry |
| 88 | 문서화 | README, 가이드 |
| 89 | 테스트 | 기능 검증, 개선 |
| 90 | 완료 | GitHub 업로드 |

---

## 🎯 Part 3: 토스플레이스 요건 매칭

### 요건 분석

| 채용공고 요건 | Month 3 학습 | 구현 여부 |
|--------------|-------------|----------|
| Istio Service Mesh 운영 경험 | VirtualService, DestinationRule | ✅ |
| 카나리/블루-그린 배포 | 가중치 기반 라우팅 | ✅ |
| 트래픽 관리 | Timeout, Retry, Circuit Breaker | ✅ |
| mTLS 보안 | PeerAuthentication STRICT | ✅ |
| 접근 제어 | AuthorizationPolicy | ✅ |
| 관찰성 도구 활용 | Kiali, Jaeger, Grafana | ✅ |
| 트러블슈팅 | istioctl, Response Flags | ✅ |

### 포트폴리오 어필 포인트

```markdown
1. **실무 수준의 Istio 구현**
   - 카나리 배포 (90:10 트래픽 분배)
   - Zero Trust 보안 모델

2. **Observability 통합**
   - Kiali: 서비스 토폴로지
   - Jaeger: 분산 추적
   - Grafana: 메트릭 대시보드

3. **장애 복원력**
   - Circuit Breaker 설정
   - Retry, Timeout 정책
   - Fault Injection 테스트
```

---

## 📝 Part 4: 면접 대비 Q&A

### Q1: Istio 운영 경험이 있나요?

```markdown
**답변 예시**:

"네, Istio Service Mesh를 활용한 프로젝트 경험이 있습니다.

**트래픽 관리**:
- VirtualService로 카나리 배포를 구현했습니다. 
- 10% 트래픽을 새 버전에 먼저 배포하고, 모니터링 후 점진적으로 전환했습니다.

**보안**:
- mTLS STRICT 모드로 서비스 간 암호화 통신을 설정했습니다.
- Zero Trust 원칙에 따라 DENY ALL 기본 정책과 명시적 ALLOW 정책을 적용했습니다.

**관찰성**:
- Kiali로 서비스 토폴로지를 시각화하고, Jaeger로 분산 추적을 구현했습니다.
- Grafana 대시보드로 SLI 메트릭을 모니터링했습니다."
```

### Q2: Circuit Breaker가 필요한 이유는?

```markdown
**답변 예시**:

"장애 전파를 방지하기 위해서입니다.

한 서비스가 느려지거나 에러를 반환하면, 그 서비스를 호출하는 
모든 서비스가 함께 느려질 수 있습니다. Circuit Breaker는 
연속 실패가 임계치를 넘으면 해당 서비스로의 요청을 일시적으로 
차단하여 장애가 전파되는 것을 막습니다.

Istio에서는 DestinationRule의 outlierDetection으로 설정합니다:
- consecutive5xxErrors: 연속 5회 5xx 에러 시
- baseEjectionTime: 30초간 제외
- interval: 10초마다 검사"
```

### Q3: mTLS와 일반 TLS의 차이는?

```markdown
**답변 예시**:

"일반 TLS는 클라이언트가 서버의 인증서만 검증합니다.
mTLS(Mutual TLS)는 양쪽이 서로의 인증서를 검증합니다.

Service Mesh에서 mTLS가 중요한 이유:
1. **신원 확인**: 요청하는 서비스가 실제 우리 클러스터 내 서비스인지 확인
2. **암호화**: 서비스 간 통신이 암호화되어 스니핑 방지
3. **Zero Trust**: 네트워크 위치가 아닌 신원으로 신뢰 결정

Istio에서는 PeerAuthentication으로 STRICT 모드를 설정하면 
자동으로 인증서가 발급되고 갱신됩니다."
```

### Q4: VirtualService와 DestinationRule의 차이는?

```markdown
**답변 예시**:

"둘 다 트래픽 관리를 하지만 역할이 다릅니다.

**VirtualService**:
- '어디로' 보낼지 결정 (라우팅)
- 경로, 헤더, 가중치 기반 라우팅
- Timeout, Retry, Fault Injection

**DestinationRule**:
- '어떻게' 보낼지 결정 (정책)
- Subset 정의 (v1, v2)
- Load Balancing 정책
- Circuit Breaker (outlierDetection)

예: VirtualService에서 '90%는 v1, 10%는 v2로'라고 결정하면,
DestinationRule에서 v1, v2가 어떤 Pod인지 정의합니다."
```

---

## ✅ Month 3 최종 체크리스트

| # | 항목 | 상태 |
|---|------|------|
| 1 | Istio 설치 및 기본 설정 | ☐ |
| 2 | VirtualService 카나리 배포 | ☐ |
| 3 | DestinationRule Circuit Breaker | ☐ |
| 4 | mTLS STRICT 적용 | ☐ |
| 5 | AuthorizationPolicy Zero Trust | ☐ |
| 6 | Kiali 서비스 그래프 | ☐ |
| 7 | Jaeger 분산 추적 | ☐ |
| 8 | Grafana 대시보드 | ☐ |
| 9 | 포트폴리오 문서화 | ☐ |
| 10 | GitHub 업로드 | ☐ |

---

## 🎉 Month 3 완료!

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                                                                              │
│   🎊 축하합니다! Month 3 (Istio Service Mesh)를 완료했습니다!               │
│                                                                              │
│   📊 완성된 포트폴리오:                                                      │
│      #1 Month 1: Kubernetes 기초 (EKS 클러스터)                              │
│      #2 Month 2: K8s 애플리케이션 배포                                       │
│      #3 Month 3: Istio Service Mesh ← 완료!                                 │
│                                                                              │
│   🎯 다음 목표: Month 4 - CI/CD (ArgoCD, GoCD)                              │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## ➡️ Month 4 예고

**주제**: CI/CD & GitOps (ArgoCD, GoCD)

- Week 13: ArgoCD GitOps
- Week 14: Helm & Kustomize
- Week 15: GoCD & Vault
- Week 16: CI/CD 포트폴리오 프로젝트

