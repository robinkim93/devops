# 📅 Day 59: 문서화 및 테스트

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "개발자들이 더 빠르고 안전하게 실험하고 배포"
> 프로젝트 문서화 및 전체 테스트로 포트폴리오 완성

좋은 문서화는 팀 협업과 운영의 핵심입니다. 토스플레이스에서 요구하는 "인프라에 대한 오너십"은 문서화 능력도 포함합니다.

---

## ⏰ 예상 소요 시간: 4시간

---

## 📚 Part 1: 문서화의 중요성 (30분)

### 1.1 왜 문서화가 중요한가?

```
┌─────────────────────────────────────────────────────────────────────┐
│  문서화의 가치                                                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. 온보딩 시간 단축                                                │
│     - 새 팀원이 빠르게 프로젝트 이해                               │
│     - 반복적인 설명 시간 절약                                      │
│                                                                      │
│  2. 운영 효율화                                                     │
│     - 장애 대응 시 빠른 참조                                       │
│     - 배포/롤백 절차 표준화                                        │
│                                                                      │
│  3. 지식 보존                                                       │
│     - 팀원 이탈 시에도 지식 유지                                   │
│     - 의사결정 히스토리 기록                                       │
│                                                                      │
│  4. 포트폴리오 가치                                                 │
│     - 프로젝트 완성도 어필                                         │
│     - 기술적 깊이 증명                                             │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 좋은 README의 구성요소

| 섹션 | 내용 | 중요도 |
|------|------|--------|
| 프로젝트 개요 | 한 문장으로 설명 | ⭐⭐⭐ |
| 아키텍처 | 시스템 구성도 | ⭐⭐⭐ |
| 기술 스택 | 사용 기술 목록 | ⭐⭐⭐ |
| Quick Start | 5분 안에 실행 | ⭐⭐⭐ |
| 상세 설치 | 단계별 가이드 | ⭐⭐ |
| API 문서 | 엔드포인트 목록 | ⭐⭐ |
| 트러블슈팅 | 자주 발생하는 문제 | ⭐⭐ |
| 기여 가이드 | PR 규칙 | ⭐ |
| 라이선스 | 저작권 | ⭐ |

---

## 📝 Part 2: README.md 작성 (1시간)

### 2.1 완성된 README.md

```markdown
# 🚀 Kubernetes 3-Tier 애플리케이션 포트폴리오

## 📋 프로젝트 개요

토스플레이스 DevOps 엔지니어 지원을 위해 제작한 Kubernetes 학습 포트폴리오입니다.
3-Tier 웹 애플리케이션을 Kubernetes에 배포하고 운영하는 전체 과정을 담았습니다.

## 🏗️ 아키텍처

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Kubernetes Cluster                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│                        ┌───────────────┐                            │
│                        │    Ingress    │                            │
│                        │  Controller   │                            │
│                        └───────┬───────┘                            │
│                                │                                    │
│           ┌────────────────────┼────────────────────┐              │
│           │                    │                    │              │
│           ▼                    ▼                    ▼              │
│    ┌────────────┐       ┌────────────┐       ┌────────────┐       │
│    │  Frontend  │       │  Backend   │       │   Redis    │       │
│    │  (Nginx)   │  →→→  │  (Flask)   │  →→→  │  (Cache)   │       │
│    │  3 Pods    │       │  3 Pods    │       │  1 Pod     │       │
│    │  + HPA     │       │  + HPA     │       │  + PVC     │       │
│    └────────────┘       └────────────┘       └────────────┘       │
│                                                                      │
│  Components:                                                        │
│  • Deployment: 애플리케이션 배포 및 스케일링                        │
│  • Service: 내부 통신 및 로드 밸런싱                                │
│  • Ingress: 외부 트래픽 라우팅                                      │
│  • ConfigMap: 설정 관리                                             │
│  • Secret: 민감 정보 관리                                           │
│  • PVC: 데이터 영속화                                               │
│  • HPA: 자동 스케일링                                               │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

## 🛠️ 기술 스택

| 카테고리 | 기술 | 버전 | 용도 |
|---------|------|------|------|
| Container Orchestration | Kubernetes | 1.28+ | 컨테이너 오케스트레이션 |
| Web Server | Nginx | 1.25 | 리버스 프록시, 정적 파일 |
| Backend | Flask (Python) | 3.0 | REST API 서버 |
| Cache | Redis | 7.0 | 세션, 카운터 캐시 |
| Infrastructure | Docker | 24.0+ | 컨테이너 런타임 |
| Local K8s | Minikube | 1.32+ | 로컬 개발 환경 |

## ⚡ Quick Start (5분)

### 사전 요구사항

- Docker Desktop
- Minikube
- kubectl

### 빠른 실행

```bash
# 1. 저장소 클론
git clone https://github.com/yourusername/k8s-portfolio.git
cd k8s-portfolio

# 2. Minikube 시작
minikube start --memory=4096 --cpus=2

# 3. 전체 배포
kubectl apply -f manifests/

# 4. 상태 확인
kubectl get all -n k8s-portfolio

# 5. 접속 (Ingress 활성화 후)
minikube addons enable ingress
echo "$(minikube ip) k8s-portfolio.local" | sudo tee -a /etc/hosts
curl http://k8s-portfolio.local/
```

## 📁 프로젝트 구조

```
k8s-portfolio/
├── manifests/
│   ├── namespace.yaml          # 네임스페이스 정의
│   ├── configmap.yaml          # 애플리케이션 설정
│   ├── secret.yaml             # 민감 정보 (예시)
│   ├── frontend/
│   │   ├── deployment.yaml     # Frontend Deployment
│   │   ├── service.yaml        # Frontend Service
│   │   └── hpa.yaml            # Frontend HPA
│   ├── backend/
│   │   ├── deployment.yaml     # Backend Deployment
│   │   ├── service.yaml        # Backend Service
│   │   └── hpa.yaml            # Backend HPA
│   ├── redis/
│   │   ├── deployment.yaml     # Redis Deployment
│   │   ├── service.yaml        # Redis Service
│   │   └── pvc.yaml            # Redis PVC
│   └── ingress.yaml            # Ingress 설정
├── src/
│   ├── frontend/               # Nginx 설정
│   └── backend/                # Flask 애플리케이션
├── docs/
│   ├── architecture.md         # 상세 아키텍처
│   └── troubleshooting.md      # 문제 해결 가이드
└── README.md
```

## 🔧 상세 배포 가이드

### Step 1: Namespace 생성

```bash
kubectl apply -f manifests/namespace.yaml
kubectl get namespaces
```

### Step 2: 설정 적용

```bash
kubectl apply -f manifests/configmap.yaml
kubectl apply -f manifests/secret.yaml

# 확인
kubectl get cm,secret -n k8s-portfolio
```

### Step 3: 애플리케이션 배포

```bash
# 순서대로 배포 (Redis → Backend → Frontend)
kubectl apply -f manifests/redis/
kubectl apply -f manifests/backend/
kubectl apply -f manifests/frontend/

# 배포 상태 확인
kubectl rollout status deployment -n k8s-portfolio --all
```

### Step 4: Ingress 설정

```bash
# Ingress Controller 활성화
minikube addons enable ingress

# Ingress 배포
kubectl apply -f manifests/ingress.yaml

# /etc/hosts 설정
echo "$(minikube ip) k8s-portfolio.local" | sudo tee -a /etc/hosts
```

### Step 5: 접속 테스트

```bash
# Frontend
curl http://k8s-portfolio.local/

# Backend API
curl http://k8s-portfolio.local/api/health
curl http://k8s-portfolio.local/api/count
```

## 📊 K8s 리소스 설명

| 리소스 | 파일 | 용도 | 설정 |
|--------|------|------|------|
| Deployment | frontend/deployment.yaml | Frontend 배포 | replicas: 3, resources |
| Deployment | backend/deployment.yaml | Backend 배포 | replicas: 3, probes |
| Deployment | redis/deployment.yaml | Redis 배포 | replicas: 1, persistent |
| Service | */service.yaml | 내부 통신 | ClusterIP |
| Ingress | ingress.yaml | 외부 노출 | Host-based routing |
| ConfigMap | configmap.yaml | 설정 관리 | 환경 변수 |
| Secret | secret.yaml | 민감 정보 | 암호, API 키 |
| PVC | redis/pvc.yaml | 데이터 영속화 | 1Gi |
| HPA | */hpa.yaml | 자동 스케일링 | CPU 70% |

## 🧪 테스트

### 헬스체크 테스트

```bash
# Frontend 헬스체크
kubectl exec -n k8s-portfolio -it deploy/frontend -- curl localhost/health

# Backend 헬스체크
kubectl exec -n k8s-portfolio -it deploy/backend -- curl localhost:8080/health
```

### 기능 테스트

```bash
# 카운터 테스트 (Redis 연동)
for i in {1..5}; do
    curl http://k8s-portfolio.local/api/count
    echo ""
done
# 예상: {"count": 1}, {"count": 2}, ...
```

### HPA 테스트

```bash
# HPA 상태 확인
kubectl get hpa -n k8s-portfolio

# 부하 테스트
kubectl run load-test --rm -i --tty --image=busybox -- \
    /bin/sh -c "while true; do wget -q -O- http://backend.k8s-portfolio:8080/; done"

# 스케일 아웃 확인 (다른 터미널)
kubectl get pods -n k8s-portfolio -w
```

## 🐛 트러블슈팅

### Pod가 Pending 상태

```bash
# 원인 확인
kubectl describe pod <pod-name> -n k8s-portfolio

# 일반적인 원인
# - 리소스 부족: 노드 리소스 확인
# - PVC 바인딩 실패: StorageClass 확인
```

### ImagePullBackOff

```bash
# 이미지 확인
kubectl describe pod <pod-name> | grep -A5 "Events"

# 해결: 이미지 태그 확인, 레지스트리 접근 권한
```

### 서비스 연결 실패

```bash
# 서비스 엔드포인트 확인
kubectl get endpoints -n k8s-portfolio

# DNS 확인
kubectl run test --rm -i --tty --image=busybox -- nslookup backend.k8s-portfolio
```

## 📈 학습 내용

이 프로젝트를 통해 학습한 Kubernetes 핵심 개념:

1. **Pod & Deployment**: 컨테이너 배포 및 관리
2. **Service**: 서비스 디스커버리 및 로드 밸런싱
3. **ConfigMap & Secret**: 설정과 민감 정보 분리
4. **PersistentVolume**: 영속적 스토리지
5. **Ingress**: 외부 트래픽 라우팅
6. **HPA**: 자동 스케일링
7. **Probe**: 헬스체크 및 자가 치유

## 📝 라이선스

MIT License

## 👤 작성자

- **이름**: 홍길동
- **이메일**: hongildong@example.com
- **GitHub**: [@hongildong](https://github.com/hongildong)

## 🙏 감사의 글

토스플레이스 DevOps 엔지니어 채용에 지원하며 이 포트폴리오를 준비했습니다.
Kubernetes 기반의 인프라 운영 경험을 쌓기 위해 노력했습니다.
```

---

## 🧪 Part 3: 전체 테스트 (1.5시간)

### 3.1 배포 테스트

```bash
# 클린 시작 (기존 리소스 삭제)
kubectl delete namespace k8s-portfolio --ignore-not-found
sleep 10

# 전체 배포
kubectl apply -f manifests/namespace.yaml
kubectl apply -f manifests/

# 배포 상태 확인
kubectl get all -n k8s-portfolio

# Pod가 모두 Running이 될 때까지 대기
kubectl wait --for=condition=Ready pods --all -n k8s-portfolio --timeout=120s
```

### 3.2 Pod 상태 확인

```bash
# Pod 상세 상태
kubectl get pods -n k8s-portfolio -o wide

# 각 Pod 로그 확인
kubectl logs -n k8s-portfolio -l app=frontend --tail=10
kubectl logs -n k8s-portfolio -l app=backend --tail=10
kubectl logs -n k8s-portfolio -l app=redis --tail=10

# Pod 내부 상태 확인
kubectl describe pods -n k8s-portfolio | grep -A5 "State:"
```

### 3.3 서비스 연결 테스트

```bash
# Service 상태 확인
kubectl get svc -n k8s-portfolio

# Endpoints 확인 (실제 Pod IP)
kubectl get endpoints -n k8s-portfolio

# 서비스 간 DNS 테스트
kubectl run dns-test --rm -i --tty --image=busybox -n k8s-portfolio -- \
    nslookup backend.k8s-portfolio.svc.cluster.local
```

### 3.4 기능 테스트

```bash
# Backend 헬스체크
kubectl exec -n k8s-portfolio deploy/backend -- \
    curl -s localhost:8080/health

# Redis 연동 테스트 (카운터)
kubectl exec -n k8s-portfolio deploy/backend -- \
    curl -s localhost:8080/count

# 여러 번 호출하여 카운터 증가 확인
for i in {1..5}; do
    kubectl exec -n k8s-portfolio deploy/backend -- \
        curl -s localhost:8080/count
    echo ""
done
```

### 3.5 Ingress 테스트

```bash
# Ingress Controller 상태 확인
kubectl get pods -n ingress-nginx

# Ingress 상태 확인
kubectl get ingress -n k8s-portfolio

# 외부 접속 테스트
curl -H "Host: k8s-portfolio.local" http://$(minikube ip)/
curl -H "Host: k8s-portfolio.local" http://$(minikube ip)/api/health
```

### 3.6 HPA 테스트

```bash
# HPA 상태 확인
kubectl get hpa -n k8s-portfolio

# 부하 생성
kubectl run load-generator --rm -i --tty \
    --image=busybox \
    --restart=Never \
    -n k8s-portfolio \
    -- /bin/sh -c "while sleep 0.01; do wget -q -O- http://backend:8080/; done"

# 다른 터미널에서 HPA 모니터링
kubectl get hpa -n k8s-portfolio -w
kubectl get pods -n k8s-portfolio -w
```

---

## 📊 Part 4: 테스트 결과 문서화 (30분)

### 4.1 테스트 결과 템플릿

```markdown
# 테스트 결과 보고서

## 테스트 일시
- 날짜: 20XX-XX-XX
- 환경: Minikube v1.32.0, Kubernetes v1.28.0

## 배포 테스트

| 항목 | 예상 | 결과 | 상태 |
|------|------|------|------|
| Namespace 생성 | 1개 | 1개 | ✅ |
| Deployment 생성 | 3개 | 3개 | ✅ |
| Pod Running | 7개 | 7개 | ✅ |
| Service 생성 | 3개 | 3개 | ✅ |
| Ingress 설정 | 1개 | 1개 | ✅ |

## 기능 테스트

| 항목 | 예상 | 결과 | 상태 |
|------|------|------|------|
| 헬스체크 | 200 OK | 200 OK | ✅ |
| 카운터 증가 | 1→2→3 | 1→2→3 | ✅ |
| Ingress 라우팅 | 응답 | 응답 | ✅ |

## HPA 테스트

| 항목 | 예상 | 결과 | 상태 |
|------|------|------|------|
| 초기 Pod 수 | 3개 | 3개 | ✅ |
| 부하 시 스케일 업 | 6개+ | 6개 | ✅ |
| 부하 제거 후 스케일 다운 | 3개 | 3개 | ✅ |

## 발견된 이슈

1. (없음 또는 이슈 기록)

## 결론

모든 테스트 통과. 프로덕션 배포 준비 완료.
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | README.md 작성 완료 | ☐ |
| 2 | 배포 테스트 통과 | ☐ |
| 3 | 기능 테스트 통과 | ☐ |
| 4 | HPA 테스트 통과 | ☐ |
| 5 | 테스트 결과 문서화 | ☐ |
| 6 | 트러블슈팅 가이드 작성 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# 전체 배포
kubectl apply -f manifests/

# 상태 확인
kubectl get all -n k8s-portfolio

# Pod 대기
kubectl wait --for=condition=Ready pods --all -n k8s-portfolio

# 기능 테스트
kubectl exec -n k8s-portfolio deploy/backend -- curl localhost:8080/health
```

---

## 💡 면접 대비 핵심 포인트

### Q1: 이 프로젝트의 아키텍처를 설명해주세요
**A**: 3-Tier 구조로, Frontend(Nginx), Backend(Flask), Redis(Cache)로 구성됩니다. Ingress로 외부 트래픽을 받고, Service로 내부 통신하며, HPA로 자동 스케일링합니다.

### Q2: ConfigMap과 Secret을 왜 분리했나요?
**A**: 일반 설정은 ConfigMap, 민감 정보(비밀번호, API 키)는 Secret으로 분리합니다. Secret은 Base64 인코딩되고 RBAC으로 접근 제어됩니다.

### Q3: HPA는 어떤 메트릭으로 스케일링하나요?
**A**: CPU 사용률 70%를 기준으로 스케일링합니다. 메모리나 커스텀 메트릭도 추가 가능합니다.

---

## ➡️ 다음 학습: Day 60

**주제**: GitHub 업로드 및 Month 2 정리
- Git 저장소 생성 및 푸시
- Month 2 학습 내용 정리
- 포트폴리오 완성
