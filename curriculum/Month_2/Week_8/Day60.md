# 📅 Day 60: GitHub 업로드 & Month 2 완료

## 🎯 오늘의 목표

> **토스플레이스 핵심**: Month 2 학습을 완료하고 Kubernetes 포트폴리오를 GitHub에 업로드합니다.

---

## ⏰ 예상 소요 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| GitHub 업로드 | 1시간 | 정리 및 커밋 |
| Month 2 정리 | 1시간 | 학습 내용 복습 |
| 면접 대비 | 1시간 | Q&A 준비 |

---

## 📤 Part 1: GitHub 업로드

### 프로젝트 구조

```
k8s-portfolio/
├── manifests/
│   ├── base/
│   │   ├── frontend/
│   │   │   ├── deployment.yaml
│   │   │   └── service.yaml
│   │   ├── backend/
│   │   │   ├── deployment.yaml
│   │   │   └── service.yaml
│   │   └── redis/
│   │       ├── deployment.yaml
│   │       └── service.yaml
│   ├── config/
│   │   ├── configmap.yaml
│   │   └── secret.yaml
│   └── ingress/
│       └── ingress.yaml
├── hpa/
│   └── hpa.yaml
├── docs/
│   ├── architecture.md
│   └── deployment.md
└── README.md
```

### Git 초기화 및 업로드

```bash
cd ~/portfolio/k8s-project

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

# Secrets (절대 커밋하지 않음)
*secret*.yaml
*.key
*.pem
.env

# Temporary
*.log
tmp/
EOF

# README.md 작성
cat << 'EOF' > README.md
# 🚀 Kubernetes 애플리케이션 배포 포트폴리오

토스플레이스 DevOps Engineer 포지션을 위한 Kubernetes 배포 포트폴리오입니다.

## 📌 프로젝트 개요

3-Tier 웹 애플리케이션을 Kubernetes에 배포하고 운영하는 프로젝트입니다.

## 🏗 아키텍처

```
[Ingress] → [Frontend] → [Backend] → [Redis]
```

## ✨ 구현 기능

- **Deployment**: 복제본 관리, 롤링 업데이트
- **Service**: ClusterIP, NodePort
- **ConfigMap/Secret**: 설정 분리
- **HPA**: CPU 기반 자동 스케일링
- **Ingress**: L7 라우팅, TLS
- **Probe**: Liveness, Readiness

## 🚀 배포 방법

```bash
# 네임스페이스 생성
kubectl create namespace portfolio

# 배포
kubectl apply -f manifests/ -n portfolio

# 확인
kubectl get all -n portfolio
```

## 📊 모니터링

```bash
# Pod 상태
kubectl get pods -n portfolio

# HPA 상태
kubectl get hpa -n portfolio

# 로그
kubectl logs -f deploy/backend -n portfolio
```
EOF

# 커밋
git add .
git commit -m "feat: Kubernetes 애플리케이션 배포 포트폴리오

📌 구현 기능:
- 3-Tier 아키텍처 (Frontend/Backend/Redis)
- ConfigMap, Secret으로 설정 분리
- HPA 자동 스케일링 (CPU 50%)
- Ingress L7 라우팅
- Liveness/Readiness Probe

🎯 토스플레이스 DevOps Engineer 포지션 대비"

# 원격 저장소 연결 및 푸시
git remote add origin https://github.com/YOUR_USERNAME/k8s-portfolio.git
git branch -M main
git push -u origin main
```

### GitHub 저장소 설정

```markdown
저장소 설정 체크리스트:
- [ ] Repository 생성 (Public)
- [ ] Description: "Kubernetes application deployment portfolio for DevOps Engineer"
- [ ] Topics: kubernetes, devops, hpa, ingress, configmap
- [ ] License: MIT
```

---

## 📋 Part 2: Month 2 학습 정리

### Week 5-6: Kubernetes 기초

| Day | 주제 | 핵심 내용 | 토스플레이스 연결 |
|-----|------|----------|-----------------|
| 31-32 | Pod | 최소 배포 단위, YAML | 컨테이너 운영 |
| 33-34 | Deployment | 복제본, 롤링 업데이트 | 무중단 배포 |
| 35 | ReplicaSet | Pod 복제 관리 | 가용성 |
| 36 | kubectl | 고급 명령어 | 운영 효율 |
| 38 | ConfigMap | 설정 분리 | 환경 관리 |
| 39 | Secret | 민감 데이터 | 보안 |
| 40 | Service | ClusterIP, NodePort | 서비스 통신 |
| 41 | Ingress | L7 라우팅 | 외부 노출 |
| 42 | PV/PVC | 영속 스토리지 | 데이터 관리 |

### Week 7: Kubernetes 심화

| Day | 주제 | 핵심 내용 | 토스플레이스 연결 |
|-----|------|----------|-----------------|
| 45 | HPA | 자동 스케일링 | 대규모 트래픽 |
| 46 | 로깅 | kubectl logs | 장애 분석 |
| 47 | 디버깅 | describe, events | 트러블슈팅 |
| 48 | RBAC | 권한 관리 | 보안 컴플라이언스 |
| 49 | SecurityContext | Pod 보안 | 컨테이너 보안 |
| 50 | NetworkPolicy | 네트워크 격리 | 서비스 분리 |

### Week 8: 컨테이너 런타임 & 프로젝트

| Day | 주제 | 핵심 내용 | 토스플레이스 연결 |
|-----|------|----------|-----------------|
| 52 | Docker 복습 | Dockerfile, 이미지 | 컨테이너 기초 |
| 53 | Podman | Rootless, Docker 호환 | 보안 강화 |
| 54 | Containerd | K8s 기본 런타임 | 클러스터 운영 |
| 55-59 | 프로젝트 | 3-Tier 앱 배포 | 실전 경험 |

---

## 🎯 Part 3: 토스플레이스 요건 매칭

| 채용 요건 | Month 2 학습 | 증빙 |
|----------|-------------|------|
| **Kubernetes 운영** | Pod, Deployment, Service, HPA | Portfolio |
| **컨테이너 런타임** | Podman, Containerd | Day 53-54 |
| **트러블슈팅** | describe, logs, events | Day 46-47 |
| **보안 컴플라이언스** | RBAC, SecurityContext, NetworkPolicy | Day 48-50 |
| **대규모 트래픽** | HPA 자동 스케일링 | Day 45 |

---

## 📝 Part 4: 면접 대비 Q&A

### Q1: "Kubernetes 애플리케이션 배포 경험이 있나요?"

```
✅ 모범 답변:

"3-Tier 웹 애플리케이션을 Kubernetes에 배포했습니다.

구현 내용:
- Deployment로 Frontend, Backend, Redis 복제본 관리
- ConfigMap으로 환경 설정 분리, Secret으로 민감 정보 관리
- HPA로 CPU 50% 기준 자동 스케일링 (2-10 Pod)
- Ingress로 L7 라우팅 및 TLS 설정
- Liveness/Readiness Probe로 헬스체크

결과:
- 무중단 배포 달성 (롤링 업데이트)
- 트래픽 증가 시 자동 확장"
```

### Q2: "Pod가 CrashLoopBackOff 상태일 때 어떻게 해결하나요?"

```
✅ 모범 답변:

"단계별로 분석합니다:

1. kubectl describe pod <pod>
   - Events 섹션에서 에러 원인 확인
   - Exit Code 확인 (137=OOMKilled, 1=앱 에러)

2. kubectl logs <pod> --previous
   - 이전 컨테이너 로그 확인
   - 애플리케이션 에러 메시지 확인

3. 원인별 해결:
   - OOMKilled → resources.limits.memory 증가
   - 앱 에러 → 코드 또는 설정 수정
   - ImagePullBackOff → 이미지명, 레지스트리 인증 확인

4. kubectl exec로 컨테이너 접속하여 추가 디버깅"
```

### Q3: "HPA는 어떻게 동작하나요?"

```
✅ 모범 답변:

"HPA는 메트릭 기반으로 Pod 수를 자동 조정합니다.

동작 원리:
1. metrics-server가 Pod CPU/Memory 수집
2. HPA Controller가 주기적으로 메트릭 확인 (15초)
3. 현재 메트릭이 목표치를 초과하면 Pod 증가
4. 목표치 미만이면 Pod 감소 (cooldown 후)

공식: desiredReplicas = ceil(current * (currentMetric / desiredMetric))

설정 예시:
- CPU 50% 기준
- 최소 2개, 최대 10개 Pod
- 스케일업 즉시, 스케일다운 5분 대기"
```

### Q4: "ConfigMap과 Secret의 차이는?"

```
✅ 모범 답변:

"둘 다 설정을 저장하지만 용도가 다릅니다.

ConfigMap:
- 일반 설정 저장 (평문)
- etcd에 평문으로 저장
- 예: 앱 설정, 환경 변수

Secret:
- 민감 데이터 저장
- Base64 인코딩 (암호화는 etcd 설정 필요)
- 예: 비밀번호, API 키, 인증서

사용 시:
- 환경 변수로 주입: envFrom, env.valueFrom
- 볼륨으로 마운트: volumes.configMap/secret

실무에서는 Secret 대신 Vault + ESO를 권장합니다."
```

---

## ✅ Month 2 최종 체크리스트

| # | 항목 | 증빙 | 완료 |
|---|------|------|------|
| 1 | Pod YAML 작성 | manifests/ | ☐ |
| 2 | Deployment 롤링 업데이트 | Day 33-34 | ☐ |
| 3 | Service (ClusterIP, NodePort) | manifests/ | ☐ |
| 4 | ConfigMap, Secret 사용 | manifests/config/ | ☐ |
| 5 | Ingress L7 라우팅 | manifests/ingress/ | ☐ |
| 6 | HPA 자동 스케일링 | hpa/ | ☐ |
| 7 | RBAC 권한 관리 | Day 48 | ☐ |
| 8 | NetworkPolicy | Day 50 | ☐ |
| 9 | Podman/Containerd 이해 | Day 53-54 | ☐ |
| 10 | GitHub 업로드 | Repository | ☐ |

---

## 🎉 Month 2 완료!

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                                                                              │
│   🎊 축하합니다! Month 2 (Kubernetes 기초)를 완료했습니다!                    │
│                                                                              │
│   📊 완성된 포트폴리오:                                                       │
│      #1 Month 1: Linux/Network/AWS 기초                                      │
│      #2 Month 2: Kubernetes 애플리케이션 배포 ← 완료!                         │
│                                                                              │
│   🎯 다음 목표: Month 3 - Istio Service Mesh                                 │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## ➡️ Month 3 예고

**주제**: Istio Service Mesh

- Week 9: Istio 기초 (VirtualService, DestinationRule)
- Week 10: Observability (Kiali, Jaeger, Grafana)
- Week 11: Security (mTLS, AuthorizationPolicy)
- Week 12: 포트폴리오 프로젝트

