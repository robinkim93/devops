# 📅 Day 30: Month 1 완료 & Month 2 준비

## 🎯 오늘의 목표

> **토스플레이스 연결점**: Month 1의 기초 역량을 점검하고 Kubernetes 학습 환경을 준비
> "OS, Network 등 다양한 레이어에서의 모니터링, 트러블슈팅 경험"의 기반 완성

Month 1을 마무리하고 Month 2 Kubernetes 학습을 위한 환경을 설정합니다. 그동안 배운 Linux, Docker, 네트워크 지식을 종합 정리하고, Kubernetes 학습 환경을 구축합니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| Month 1 회고 | 1시간 | 성과 정리, 부족한 부분 확인 |
| K8s 환경 설정 | 1.5시간 | minikube, kubectl 설치 |
| 기본 테스트 | 1시간 | 클러스터 동작 확인 |
| Month 2 계획 | 30분 | 학습 로드맵 확인 |

---

## 🎉 Part 1: Month 1 성과 정리 (1시간)

### 1.1 달성한 것들

```
┌─────────────────────────────────────────────────────────────────────┐
│  Month 1 성과 요약                                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Week 1: Linux 트러블슈팅 도구                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  ✅ strace: 시스템 콜 분석                                  │    │
│  │  ✅ /proc: 프로세스 정보 수집                               │    │
│  │  ✅ vmstat, free: 메모리 분석                               │    │
│  │  ✅ iostat, iotop: 디스크 I/O 분석                          │    │
│  │  ✅ ss, netstat: 네트워크 연결 분석                         │    │
│  │  ✅ tcpdump: 패킷 캡처                                      │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Week 2: Docker 기본~중급                                           │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  ✅ Dockerfile 작성 및 빌드                                 │    │
│  │  ✅ 컨테이너 라이프사이클 관리                              │    │
│  │  ✅ Docker Compose 멀티 컨테이너                            │    │
│  │  ✅ Docker 네트워킹 (bridge, host)                          │    │
│  │  ✅ 컨테이너 트러블슈팅 (logs, inspect, exec)              │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Week 3: 시스템/네트워크 관리                                       │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  ✅ systemd 서비스 관리 (systemctl, journalctl)            │    │
│  │  ✅ 파일 권한/방화벽 (chmod, ufw, iptables)                │    │
│  │  ✅ SSH 키 기반 인증 설정                                   │    │
│  │  ✅ DNS 트러블슈팅 (dig, nslookup)                         │    │
│  │  ✅ HTTP/HTTPS 분석 (curl, openssl)                        │    │
│  │  ✅ Nginx 로드밸런싱                                        │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Week 4: 종합 프로젝트                                              │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  ✅ 3-Tier 앱 구축 (Nginx + App + Redis)                   │    │
│  │  ✅ 장애 시나리오 분석 및 해결                              │    │
│  │  ✅ 인시던트 리포트 작성                                    │    │
│  │  ✅ GitHub 포트폴리오 업로드                                │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 토스플레이스 요건 대비 현황

| 채용 요건 | Month 1 학습 내용 | 달성도 |
|----------|------------------|--------|
| OS 레이어 트러블슈팅 | strace, /proc, vmstat, iostat | ⭐⭐⭐ |
| Network 트러블슈팅 | ss, tcpdump, DNS, HTTP | ⭐⭐⭐ |
| 컨테이너 경험 | Docker, Compose | ⭐⭐⭐ |
| Kubernetes 운영 | (Month 2에서 학습) | ☆☆☆ |
| Service Mesh | (Month 3에서 학습) | ☆☆☆ |
| CI/CD | (Month 4에서 학습) | ☆☆☆ |
| 모니터링 | (Month 5에서 학습) | ☆☆☆ |

### 1.3 자가 점검 체크리스트

```bash
# 다음 질문에 자신 있게 답할 수 있는가?

# 1. 프로세스 문제 분석
"서버가 느릴 때 어떻게 분석하나요?"
# 답: top → vmstat → iostat → ss → strace

# 2. 메모리 분석
"메모리 누수가 의심될 때 어떻게 확인하나요?"
# 답: free -h, /proc/<pid>/status, vmstat, docker stats

# 3. 네트워크 문제
"서비스 접속이 안 될 때 어떻게 진단하나요?"
# 답: ping → dig → curl -v → ss -tlnp → tcpdump

# 4. 컨테이너 트러블슈팅
"컨테이너가 계속 재시작할 때 어떻게 분석하나요?"
# 답: docker logs → docker inspect (ExitCode, OOMKilled) → docker stats

# 5. 서비스 관리
"서비스가 시작 안 될 때 어떻게 확인하나요?"
# 답: systemctl status → journalctl -u <service>
```

---

## 🚀 Part 2: Kubernetes 환경 설정 (1.5시간)

### 2.1 사전 요구사항 확인

```bash
# 시스템 사양 확인
echo "=== 시스템 사양 확인 ==="
echo "CPU: $(nproc) cores"
echo "Memory: $(free -h | awk '/Mem:/ {print $2}')"
echo "Disk: $(df -h / | awk 'NR==2 {print $4}') available"

# 권장 사양:
# - CPU: 2+ cores
# - Memory: 4GB+ (8GB 권장)
# - Disk: 20GB+ 여유 공간

# 가상화 지원 확인
egrep -c '(vmx|svm)' /proc/cpuinfo
# 0이 아니면 가상화 지원
```

### 2.2 minikube 설치

```bash
# === macOS ===
brew install minikube

# === Linux (Ubuntu/Debian) ===
echo "=== minikube 설치 ==="

# 바이너리 다운로드
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64

# 설치
sudo install minikube-linux-amd64 /usr/local/bin/minikube

# 정리
rm minikube-linux-amd64

# 버전 확인
minikube version
# minikube version: v1.32.0
```

### 2.3 kubectl 설치

```bash
# === macOS ===
brew install kubectl

# === Linux (Ubuntu/Debian) ===
echo "=== kubectl 설치 ==="

# 최신 버전 다운로드
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"

# 체크섬 검증 (선택)
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl.sha256"
echo "$(cat kubectl.sha256)  kubectl" | sha256sum --check

# 설치
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# 정리
rm kubectl kubectl.sha256

# 버전 확인
kubectl version --client
# Client Version: v1.29.0
```

### 2.4 kubectl 자동 완성 설정

```bash
# Bash
echo 'source <(kubectl completion bash)' >> ~/.bashrc
echo 'alias k=kubectl' >> ~/.bashrc
echo 'complete -o default -F __start_kubectl k' >> ~/.bashrc
source ~/.bashrc

# Zsh
echo 'source <(kubectl completion zsh)' >> ~/.zshrc
echo 'alias k=kubectl' >> ~/.zshrc
source ~/.zshrc

# 확인
k version --client
```

### 2.5 minikube 클러스터 시작

```bash
# === 클러스터 시작 ===
echo "=== minikube 클러스터 시작 ==="

# 기본 설정으로 시작
minikube start

# 또는 리소스 지정
minikube start --cpus=2 --memory=4096 --disk-size=20g

# 드라이버 지정 (Docker 사용)
minikube start --driver=docker

# 예상 출력:
# 😄  minikube v1.32.0 on Ubuntu 22.04
# ✨  Using the docker driver based on existing profile
# 👍  Starting control plane node minikube in cluster minikube
# 🚜  Pulling base image ...
# 🔥  Creating docker container (CPUs=2, Memory=4096MB) ...
# 🐳  Preparing Kubernetes v1.28.3 on Docker 24.0.7 ...
# 🔎  Verifying Kubernetes components...
# 🌟  Enabled addons: storage-provisioner, default-storageclass
# 🏄  Done! kubectl is now configured to use "minikube" cluster
```

### 2.6 클러스터 상태 확인

```bash
# 클러스터 정보
echo "=== 클러스터 정보 ==="
kubectl cluster-info

# 예상 출력:
# Kubernetes control plane is running at https://192.168.49.2:8443
# CoreDNS is running at https://192.168.49.2:8443/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy

# 노드 확인
echo ""
echo "=== 노드 목록 ==="
kubectl get nodes

# 예상 출력:
# NAME       STATUS   ROLES           AGE   VERSION
# minikube   Ready    control-plane   2m    v1.28.3

# 노드 상세 정보
kubectl describe node minikube | head -40

# 시스템 Pod 확인
echo ""
echo "=== 시스템 Pod ==="
kubectl get pods -n kube-system
```

---

## 🛠️ Part 3: 기본 테스트 (1시간)

### 3.1 첫 번째 Pod 배포

```bash
# === nginx 배포 ===
echo "=== nginx 배포 테스트 ==="

# Deployment 생성
kubectl create deployment nginx --image=nginx

# 상태 확인
kubectl get deployments
kubectl get pods

# Pod 상세 정보
kubectl describe pod -l app=nginx

# 로그 확인
kubectl logs -l app=nginx
```

### 3.2 Service 노출

```bash
# Service 생성 (NodePort)
kubectl expose deployment nginx --port=80 --type=NodePort

# Service 확인
kubectl get services

# minikube에서 접속 URL 확인
minikube service nginx --url

# 또는 직접 접속
curl $(minikube service nginx --url)
```

### 3.3 kubectl 기본 명령어 연습

```bash
# === 주요 명령어 연습 ===

# 리소스 조회
kubectl get pods                    # Pod 목록
kubectl get pods -o wide            # 상세 (IP, 노드)
kubectl get pods -o yaml            # YAML 출력
kubectl get all                     # 모든 리소스

# 리소스 상세
kubectl describe pod <pod-name>
kubectl describe deployment nginx

# 로그
kubectl logs <pod-name>
kubectl logs -f <pod-name>          # 실시간
kubectl logs --tail=50 <pod-name>   # 최근 50줄

# 컨테이너 접속
kubectl exec -it <pod-name> -- /bin/sh
kubectl exec -it <pod-name> -- ls /

# 포트 포워딩
kubectl port-forward <pod-name> 8080:80

# 스케일링
kubectl scale deployment nginx --replicas=3
kubectl get pods -w                 # 변화 관찰

# 삭제
kubectl delete deployment nginx
kubectl delete service nginx
```

### 3.4 minikube 대시보드 (선택)

```bash
# 대시보드 활성화 및 실행
minikube dashboard

# 또는 URL만 확인
minikube dashboard --url
```

### 3.5 애드온 확인

```bash
# 사용 가능한 애드온 목록
minikube addons list

# 필수 애드온 활성화
minikube addons enable metrics-server
minikube addons enable ingress

# 활성화된 애드온 확인
minikube addons list | grep enabled
```

---

## 📊 Part 4: Month 2 학습 로드맵 (30분)

### 4.1 Month 2 전체 계획

```
┌─────────────────────────────────────────────────────────────────────┐
│  Month 2: Kubernetes 기초~중급 (30일)                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Week 5 (Day 31-37): Kubernetes 기초                                │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • Kubernetes 아키텍처 이해                                 │    │
│  │  • Pod 개념 및 생성                                         │    │
│  │  • Deployment로 앱 배포                                     │    │
│  │  • Service로 네트워크 노출                                  │    │
│  │  • Namespace로 리소스 격리                                  │    │
│  │  • kubectl 명령어 마스터                                    │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Week 6 (Day 38-44): Kubernetes 설정 관리                           │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • ConfigMap으로 설정 분리                                  │    │
│  │  • Secret으로 민감 정보 관리                                │    │
│  │  • PersistentVolume/PVC로 데이터 영속화                    │    │
│  │  • Probe로 헬스체크 구현                                    │    │
│  │  • Resource 제한 (requests/limits)                         │    │
│  │  • Ingress로 외부 노출                                      │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Week 7 (Day 45-51): Kubernetes 운영                                │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • HPA로 자동 스케일링                                      │    │
│  │  • Rolling Update/Rollback                                  │    │
│  │  • Pod 스케줄링 (Node Selector, Affinity)                  │    │
│  │  • SecurityContext                                          │    │
│  │  • NetworkPolicy                                            │    │
│  │  • Containerd 기초                                          │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Week 8 (Day 52-60): Kubernetes 프로젝트                            │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • 3-Tier 앱 Kubernetes 배포                                │    │
│  │  • Helm 기초                                                │    │
│  │  • 프로젝트 문서화                                          │    │
│  │  • GitHub 포트폴리오 업로드                                 │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.2 핵심 학습 목표

| 주차 | 핵심 목표 | 결과물 |
|------|----------|--------|
| Week 5 | K8s 기본 리소스 이해 | Pod, Deploy, Service 배포 |
| Week 6 | 설정/스토리지 관리 | ConfigMap, Secret, PVC 활용 |
| Week 7 | 운영 기능 습득 | HPA, 롤링 업데이트, 보안 |
| Week 8 | 실전 프로젝트 | 3-Tier 앱 K8s 배포 |

---

## ✅ Month 2 시작 전 체크리스트

| # | 항목 | 명령어 | 완료 |
|---|------|--------|------|
| 1 | minikube 설치 | `minikube version` | ☐ |
| 2 | kubectl 설치 | `kubectl version --client` | ☐ |
| 3 | 클러스터 시작 | `minikube start` | ☐ |
| 4 | 노드 확인 | `kubectl get nodes` | ☐ |
| 5 | nginx 배포 테스트 | `kubectl create deployment nginx --image=nginx` | ☐ |
| 6 | 서비스 노출 테스트 | `kubectl expose deploy nginx --port=80 --type=NodePort` | ☐ |
| 7 | kubectl 자동완성 | 설정 완료 | ☐ |
| 8 | Month 1 포트폴리오 GitHub 업로드 | URL 확인 | ☐ |

---

## 🔑 minikube 유용한 명령어

```bash
# 클러스터 관리
minikube start           # 시작
minikube stop            # 정지
minikube delete          # 삭제
minikube status          # 상태
minikube pause           # 일시정지
minikube unpause         # 재개

# 정보 확인
minikube ip              # 클러스터 IP
minikube ssh             # 노드 SSH 접속
minikube logs            # 로그

# 서비스 접근
minikube service <name>          # 브라우저로 열기
minikube service <name> --url    # URL만 출력
minikube tunnel                  # LoadBalancer용 터널

# 애드온
minikube addons list
minikube addons enable <addon>
minikube addons disable <addon>

# 리소스 조정
minikube config set cpus 4
minikube config set memory 8192
```

---

## 💡 면접 대비: Month 1 복습 질문

### Q1: Month 1에서 가장 중요하게 배운 것은?

**A**: "Linux 시스템의 다양한 레이어(CPU, 메모리, 디스크 I/O, 네트워크)에서 문제를 체계적으로 분석하는 방법을 배웠습니다. 특히 strace로 시스템 콜을 분석하고, /proc 파일시스템으로 프로세스 상태를 확인하며, tcpdump로 네트워크 문제를 진단하는 경험이 실제 트러블슈팅에 중요합니다."

### Q2: Docker 컨테이너 문제를 어떻게 분석하나요?

**A**: 
1. `docker logs`로 에러 메시지 확인
2. `docker inspect`로 Exit Code 확인 (137=OOM, 1=앱 에러)
3. `docker stats`로 리소스 사용량 확인
4. `docker exec`로 컨테이너 내부 진입
5. 필요시 Dockerfile, 설정 파일 검토

### Q3: Kubernetes를 왜 배우나요?

**A**: "토스플레이스는 Kubernetes 기반의 Cloud Native 플랫폼을 운영합니다. 여러 Kubernetes 클러스터를 운영하고 최적화하는 것이 핵심 업무이므로, Pod, Deployment, Service 등 기본 리소스부터 HPA, Resource Management 같은 운영 기능까지 숙지해야 합니다."

---

## 📝 학습 기록

```
Month 1 완료일: ____년 __월 __일
총 학습 시간: ____시간

가장 크게 배운 것:


가장 어려웠던 것:


Month 2에서 기대하는 것:


포트폴리오 GitHub URL:


minikube 환경 설정 완료: ☐

```

---

## ➡️ 다음 학습: Day 31 (Month 2 시작)

**주제**: Kubernetes 소개 및 아키텍처

- Kubernetes란?
- Control Plane / Worker Node
- 핵심 컴포넌트 (kube-apiserver, etcd, kubelet 등)
- 첫 번째 Pod 생성
