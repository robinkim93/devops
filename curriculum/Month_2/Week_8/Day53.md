# 📅 Day 53: Containerd 기초

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "Containerd 등 컨테이너 런타임 경험"
> Kubernetes 1.24+의 기본 런타임 Containerd를 이해하고 운영할 수 있어야 합니다

Kubernetes의 기본 컨테이너 런타임인 Containerd의 아키텍처를 이해하고, ctr/crictl 명령어로 컨테이너를 관리하는 방법을 학습합니다. Docker 의존성이 제거된 K8s 환경에서 필수적인 지식입니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 런타임 계층 이해 | 45분 | CRI, OCI, 아키텍처 |
| ctr 명령어 | 1시간 | Containerd 직접 관리 |
| crictl 명령어 | 1시간 | K8s 관점 트러블슈팅 |
| 비교 및 실습 | 1.25시간 | Docker vs Containerd vs Podman |

---

## 📚 Part 1: 컨테이너 런타임 계층 구조 (45분)

### 1.1 컨테이너 런타임이란?

```
┌─────────────────────────────────────────────────────────────────────┐
│  컨테이너 런타임 = 컨테이너를 실제로 실행하는 소프트웨어            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  역할:                                                               │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • 컨테이너 이미지 Pull/Push                                │    │
│  │  • 컨테이너 생성/시작/정지/삭제                            │    │
│  │  • 컨테이너 리소스 격리 (cgroups, namespaces)              │    │
│  │  • 컨테이너 네트워킹                                        │    │
│  │  • 스토리지 관리                                            │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Kubernetes 관점:                                                    │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  kubelet ──(CRI)──> Container Runtime ──(OCI)──> Container  │    │
│  │                                                             │    │
│  │  K8s 1.20 이전: Docker (dockershim)                        │    │
│  │  K8s 1.24 이후: Containerd, CRI-O (Docker 지원 제거)       │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 컨테이너 런타임 계층

```
┌─────────────────────────────────────────────────────────────────────┐
│  컨테이너 런타임 계층 구조                                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  High-Level (오케스트레이션)                                        │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Kubernetes (kubelet)                                       │    │
│  │  • Pod 스케줄링                                             │    │
│  │  • 컨테이너 오케스트레이션                                  │    │
│  │  • 헬스 체크                                                │    │
│  └───────────────────────────┬─────────────────────────────────┘    │
│                              │                                      │
│                              │ CRI (Container Runtime Interface)    │
│                              │ gRPC 기반 표준 API                   │
│                              ▼                                      │
│  Mid-Level (런타임)                                                 │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Containerd / CRI-O                                         │    │
│  │  • 이미지 관리                                              │    │
│  │  • 컨테이너 라이프사이클                                    │    │
│  │  • 스냅샷 (스토리지)                                        │    │
│  │  • 네트워킹 (CNI 호출)                                      │    │
│  └───────────────────────────┬─────────────────────────────────┘    │
│                              │                                      │
│                              │ OCI Runtime Spec                     │
│                              │ 컨테이너 실행 표준                   │
│                              ▼                                      │
│  Low-Level (실행기)                                                 │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  runc / crun / kata-containers                              │    │
│  │  • 실제 컨테이너 프로세스 생성                              │    │
│  │  • Linux namespace/cgroups 설정                             │    │
│  │  • 컨테이너 rootfs 마운트                                   │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.3 CRI (Container Runtime Interface)

```
┌─────────────────────────────────────────────────────────────────────┐
│  CRI (Container Runtime Interface)                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  정의:                                                               │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Kubernetes와 컨테이너 런타임 간의 표준 인터페이스          │    │
│  │  gRPC 기반 API로 정의                                       │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  주요 API:                                                           │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  RuntimeService (컨테이너 관리)                             │    │
│  │  • RunPodSandbox / StopPodSandbox / RemovePodSandbox       │    │
│  │  • CreateContainer / StartContainer / StopContainer        │    │
│  │  • RemoveContainer / ListContainers                         │    │
│  │  • ContainerStatus / ExecSync / Exec                        │    │
│  │                                                             │    │
│  │  ImageService (이미지 관리)                                 │    │
│  │  • ListImages / ImageStatus / PullImage / RemoveImage       │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  CRI 호환 런타임:                                                    │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • Containerd (CRI plugin 내장)                             │    │
│  │  • CRI-O (Red Hat, K8s 전용)                               │    │
│  │  • Docker (K8s 1.24부터 지원 제거)                         │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.4 Containerd 특징

| 특징 | 설명 |
|------|------|
| **출신** | Docker에서 분리된 런타임 |
| **표준** | OCI 이미지/런타임 표준 준수 |
| **가벼움** | Docker 데몬보다 리소스 사용 적음 |
| **안정성** | 프로덕션에서 검증됨 |
| **K8s 기본** | Kubernetes 1.24+의 기본 런타임 |
| **플러그인** | Snapshotter, CNI 등 확장 가능 |

---

## 🛠️ Part 2: ctr 명령어 (1시간)

### 2.1 ctr 소개

```
┌─────────────────────────────────────────────────────────────────────┐
│  ctr = Containerd CLI                                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  특징:                                                               │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • Containerd 직접 제어용 CLI                               │    │
│  │  • 저수준 도구 (Docker보다 사용성 낮음)                     │    │
│  │  • 디버깅/트러블슈팅용                                      │    │
│  │  • K8s가 아닌 Containerd 자체 관리에 사용                   │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  vs crictl:                                                          │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  ctr: Containerd 직접 관리 (개발/디버깅)                    │    │
│  │  crictl: CRI 인터페이스 통해 관리 (K8s 트러블슈팅)         │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 실습 1: ctr 기본 명령어 - 이미지 관리

```bash
# Containerd 상태 확인
sudo systemctl status containerd

# ctr 버전 확인
sudo ctr version

# === 이미지 관리 ===

# 이미지 목록
echo "=== 이미지 목록 ==="
sudo ctr images ls

# 이미지 Pull (Docker Hub에서)
echo ""
echo "=== 이미지 Pull ==="
sudo ctr images pull docker.io/library/nginx:latest

# 이미지 확인
sudo ctr images ls | grep nginx

# 이미지 상세 정보
sudo ctr images check docker.io/library/nginx:latest

# 이미지 태그 변경
sudo ctr images tag docker.io/library/nginx:latest nginx:v1

# 이미지 삭제
# sudo ctr images rm nginx:v1

# 이미지 Export/Import
sudo ctr images export nginx.tar docker.io/library/nginx:latest
# sudo ctr images import nginx.tar
```

### 실습 2: ctr 컨테이너 관리

```bash
# === 컨테이너 생성 및 실행 ===

# 컨테이너 생성 (실행되지 않음)
echo "=== 컨테이너 생성 ==="
sudo ctr container create docker.io/library/nginx:latest nginx-ctr

# 컨테이너 목록
sudo ctr containers ls
# 또는
sudo ctr c ls

# 컨테이너 정보
sudo ctr container info nginx-ctr

# === Task (실행 중인 컨테이너) ===

# Task 시작 (컨테이너 실행)
echo ""
echo "=== Task 시작 ==="
sudo ctr task start -d nginx-ctr
# -d: detach (백그라운드)

# Task 목록 (실행 중인 컨테이너)
sudo ctr tasks ls
# 또는
sudo ctr t ls

# Task 정지
sudo ctr task kill nginx-ctr

# Task 삭제
sudo ctr task delete nginx-ctr

# 컨테이너 삭제
sudo ctr container delete nginx-ctr
```

### 실습 3: ctr run (생성 + 실행)

```bash
# 컨테이너 생성 + 실행 (한 번에)
echo "=== ctr run ==="
sudo ctr run -d docker.io/library/nginx:latest nginx-test

# 확인
sudo ctr containers ls
sudo ctr tasks ls

# 컨테이너 내부 명령 실행
sudo ctr task exec --exec-id exec1 nginx-test ls /

# 로그 확인 (ctr은 로그 기능 제한적)
# containerd는 기본적으로 stdout/stderr를 파일로 저장하지 않음

# 정리
sudo ctr task kill nginx-test
sudo ctr task delete nginx-test
sudo ctr container delete nginx-test
```

### 실습 4: Namespace (격리)

```bash
# === Containerd Namespace ===
# Containerd는 namespace로 리소스를 격리
# K8s는 k8s.io namespace 사용

# Namespace 목록
echo "=== Namespace 목록 ==="
sudo ctr namespaces ls

# 예상 출력:
# NAME    LABELS
# default
# k8s.io   (Kubernetes가 사용)
# moby     (Docker가 사용, Docker 설치 시)

# 특정 namespace의 컨테이너 목록
echo ""
echo "=== k8s.io namespace 컨테이너 ==="
sudo ctr -n k8s.io containers ls

# 특정 namespace의 이미지 목록
sudo ctr -n k8s.io images ls

# 특정 namespace의 Task 목록
sudo ctr -n k8s.io tasks ls

# Namespace 생성
sudo ctr namespaces create test-ns

# 특정 namespace에서 이미지 Pull
sudo ctr -n test-ns images pull docker.io/library/busybox:latest

# Namespace 삭제
# sudo ctr namespaces rm test-ns
```

---

## 🛠️ Part 3: crictl 명령어 (1시간)

### 3.1 crictl 소개

```
┌─────────────────────────────────────────────────────────────────────┐
│  crictl = CRI Command Line Interface                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  특징:                                                               │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • CRI 호환 런타임 디버깅 도구                              │    │
│  │  • Kubernetes와 동일한 인터페이스 사용                      │    │
│  │  • K8s 트러블슈팅에 권장                                    │    │
│  │  • Docker 명령어와 유사한 사용법                            │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  설정 파일: /etc/crictl.yaml                                        │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  runtime-endpoint: unix:///run/containerd/containerd.sock   │    │
│  │  image-endpoint: unix:///run/containerd/containerd.sock     │    │
│  │  timeout: 10                                                │    │
│  │  debug: false                                               │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 실습 5: crictl 설정

```bash
# crictl 설정 파일 확인/생성
cat /etc/crictl.yaml 2>/dev/null || \
sudo tee /etc/crictl.yaml << 'EOF'
runtime-endpoint: unix:///run/containerd/containerd.sock
image-endpoint: unix:///run/containerd/containerd.sock
timeout: 10
debug: false
EOF

# 또는 환경 변수로 설정
export CONTAINER_RUNTIME_ENDPOINT=unix:///run/containerd/containerd.sock

# 버전 확인
sudo crictl version
```

### 실습 6: crictl Pod/컨테이너 조회

```bash
# === Pod 관리 ===

# Pod 목록 (K8s가 생성한 Pod)
echo "=== Pod 목록 ==="
sudo crictl pods

# 출력 예시:
# POD ID          CREATED      STATE   NAME                      NAMESPACE
# 1a2b3c4d5e6f7   1 hour ago   Ready   nginx-xxx                 default
# 7f6e5d4c3b2a1   2 hours ago  Ready   kube-proxy-xxx            kube-system

# 특정 상태의 Pod
sudo crictl pods --state Ready

# 특정 namespace의 Pod
sudo crictl pods --namespace default

# 특정 이름 패턴의 Pod
sudo crictl pods --name nginx

# Pod 상세 정보
sudo crictl inspectp <pod-id>

# === 컨테이너 관리 ===

# 컨테이너 목록
echo ""
echo "=== 컨테이너 목록 ==="
sudo crictl ps

# 모든 컨테이너 (종료된 것 포함)
sudo crictl ps -a

# 특정 Pod의 컨테이너
sudo crictl ps --pod <pod-id>

# 컨테이너 상세 정보
sudo crictl inspect <container-id>
```

### 실습 7: crictl 이미지 관리

```bash
# === 이미지 관리 ===

# 이미지 목록
echo "=== 이미지 목록 ==="
sudo crictl images

# 출력 예시:
# IMAGE                      TAG       IMAGE ID       SIZE
# docker.io/library/nginx    latest    a8758716bb6a   192MB
# k8s.gcr.io/pause           3.9       7031c1b28338   744kB

# 이미지 Pull
sudo crictl pull docker.io/library/nginx:latest

# 이미지 상세 정보
sudo crictl inspecti docker.io/library/nginx:latest

# 이미지 삭제
# sudo crictl rmi <image-id>

# 사용하지 않는 이미지 정리
# sudo crictl rmi --prune
```

### 실습 8: crictl 로그 및 실행

```bash
# === 로그 확인 ===

# 컨테이너 로그
echo "=== 컨테이너 로그 ==="
# sudo crictl logs <container-id>

# 실시간 로그
# sudo crictl logs -f <container-id>

# 최근 100줄
# sudo crictl logs --tail 100 <container-id>

# 타임스탬프 포함
# sudo crictl logs --timestamps <container-id>

# === 컨테이너 내부 실행 ===

# 명령 실행
# sudo crictl exec <container-id> ls /

# 인터랙티브 쉘
# sudo crictl exec -it <container-id> /bin/sh

# === 통계 ===

# 컨테이너 리소스 사용량
echo ""
echo "=== 컨테이너 Stats ==="
sudo crictl stats

# 특정 컨테이너
# sudo crictl stats <container-id>
```

### 실습 9: crictl 트러블슈팅

```bash
#!/bin/bash
# k8s-container-troubleshoot.sh

echo "=== Kubernetes 컨테이너 트러블슈팅 ==="

# 1. Pod 상태 확인
echo ""
echo "[1/5] Pod 상태"
sudo crictl pods --state NotReady 2>/dev/null | head -10

# 2. 문제 컨테이너 확인
echo ""
echo "[2/5] 비정상 컨테이너"
sudo crictl ps -a | grep -v "Running" | head -10

# 3. 컨테이너 리소스 사용량
echo ""
echo "[3/5] 리소스 사용량 TOP 5"
sudo crictl stats --output table 2>/dev/null | head -6

# 4. 이미지 상태
echo ""
echo "[4/5] 이미지 목록"
sudo crictl images | head -10

# 5. Containerd 상태
echo ""
echo "[5/5] Containerd 상태"
sudo systemctl status containerd --no-pager | head -10

echo ""
echo "=== 트러블슈팅 완료 ==="

# 사용법:
# 특정 컨테이너 로그: sudo crictl logs <container-id>
# 컨테이너 상세: sudo crictl inspect <container-id>
# Pod 상세: sudo crictl inspectp <pod-id>
```

---

## 📊 Part 4: Docker vs Containerd vs Podman 비교 (30분)

### 4.1 비교 표

| 기능 | Docker | Containerd | Podman |
|------|--------|------------|--------|
| **CLI** | docker | ctr / nerdctl | podman |
| **데몬** | ✅ 필요 (dockerd) | ✅ 필요 (containerd) | ❌ 불필요 (Daemonless) |
| **K8s 지원** | ❌ (1.24+ 제거) | ✅ 기본 런타임 | ⚠️ CRI-O 통해 |
| **이미지 빌드** | ✅ docker build | ❌ (buildkit 별도) | ✅ podman build |
| **Compose** | ✅ docker-compose | ❌ (nerdctl compose) | ✅ podman-compose |
| **Rootless** | ⚠️ 제한적 | ⚠️ 제한적 | ✅ 완전 지원 |
| **보안** | 보통 | 좋음 | 매우 좋음 |
| **리소스 사용** | 높음 | 낮음 | 낮음 |
| **호환성** | Docker Hub 네이티브 | OCI 호환 | OCI 호환 |

### 4.2 토스플레이스 환경에서의 사용

```
┌─────────────────────────────────────────────────────────────────────┐
│  토스플레이스 컨테이너 런타임 환경                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Production (Kubernetes 클러스터)                                   │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  런타임: Containerd                                         │    │
│  │  관리 도구: crictl (트러블슈팅)                             │    │
│  │  이유: K8s 기본 런타임, 안정적, 가벼움                     │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  Development (로컬 환경)                                            │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  런타임: Docker 또는 Podman                                 │    │
│  │  이유: 개발자 친화적, 이미지 빌드 용이                     │    │
│  │  토스플레이스는 Podman 사용                                │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  CI/CD (빌드 환경)                                                   │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  빌드: Buildkit, Kaniko (Rootless 빌드)                    │    │
│  │  이유: 보안 (Docker 소켓 노출 회피)                        │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.3 명령어 비교

| 작업 | Docker | ctr | crictl | Podman |
|------|--------|-----|--------|--------|
| 이미지 목록 | docker images | ctr images ls | crictl images | podman images |
| 이미지 Pull | docker pull nginx | ctr images pull nginx | crictl pull nginx | podman pull nginx |
| 컨테이너 실행 | docker run -d nginx | ctr run -d nginx | - | podman run -d nginx |
| 컨테이너 목록 | docker ps | ctr tasks ls | crictl ps | podman ps |
| 로그 | docker logs | - | crictl logs | podman logs |
| 삭제 | docker rm | ctr c rm | - | podman rm |

---

## ✅ 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | 런타임 계층 이해 | CRI, OCI, High/Mid/Low Level | ☐ |
| 2 | Containerd 특징 | K8s 기본 런타임 이유 | ☐ |
| 3 | ctr 이미지 관리 | pull, ls, rm | ☐ |
| 4 | ctr 컨테이너 관리 | run, task, container | ☐ |
| 5 | ctr namespace | k8s.io, default | ☐ |
| 6 | crictl pods/ps | K8s 컨테이너 조회 | ☐ |
| 7 | crictl logs/exec | 트러블슈팅 | ☐ |
| 8 | 런타임 비교 | Docker vs Containerd vs Podman | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# === ctr (Containerd 직접) ===
sudo ctr images ls
sudo ctr images pull docker.io/library/nginx:latest
sudo ctr run -d docker.io/library/nginx:latest nginx-test
sudo ctr containers ls
sudo ctr tasks ls
sudo ctr -n k8s.io containers ls  # K8s namespace

# === crictl (K8s 트러블슈팅) ===
sudo crictl pods                   # Pod 목록
sudo crictl ps                     # 컨테이너 목록
sudo crictl ps -a                  # 모든 컨테이너
sudo crictl images                 # 이미지 목록
sudo crictl logs <container-id>    # 로그
sudo crictl exec -it <id> /bin/sh  # 쉘 접속
sudo crictl inspect <container-id> # 상세 정보
sudo crictl stats                  # 리소스 사용량
```

---

## 💡 면접 대비 핵심 포인트

### Q1: Kubernetes에서 Docker 대신 Containerd를 사용하는 이유는?

**A**: "K8s 1.24부터 dockershim이 제거되어 Docker를 CRI로 직접 지원하지 않습니다. Containerd는 CRI를 네이티브로 구현하여 Docker 데몬 없이 더 가볍고 효율적으로 컨테이너를 실행합니다. Docker의 불필요한 기능(이미지 빌드, swarm 등)이 제거되어 보안과 성능이 향상됩니다."

### Q2: ctr과 crictl의 차이는?

**A**: "ctr은 Containerd를 직접 제어하는 저수준 도구로, 개발/디버깅용입니다. crictl은 CRI 인터페이스를 통해 컨테이너를 관리하며, Kubernetes 환경 트러블슈팅에 권장됩니다. crictl은 kubelet과 동일한 방식으로 런타임과 통신합니다."

### Q3: Containerd의 namespace란?

**A**: "Containerd는 namespace로 이미지와 컨테이너를 격리합니다. Kubernetes는 'k8s.io' namespace를 사용하고, Docker는 'moby' namespace를 사용합니다. `ctr -n k8s.io containers ls`로 K8s 컨테이너를 조회할 수 있습니다."

### Q4: K8s에서 컨테이너 트러블슈팅 순서는?

**A**:
1. `crictl pods` - Pod 상태 확인
2. `crictl ps -a` - 컨테이너 상태 확인 (종료된 것 포함)
3. `crictl logs <container-id>` - 로그 확인
4. `crictl inspect <container-id>` - 상세 정보
5. `crictl exec -it <container-id> /bin/sh` - 내부 진입

---

## 📝 학습 기록

```
학습일: ____년 __월 __일
실제 소요 시간: ____시간

실습 완료 항목:
- [ ] ctr images pull/ls
- [ ] ctr run / containers / tasks
- [ ] ctr namespace 확인
- [ ] crictl pods / ps
- [ ] crictl logs / exec
- [ ] crictl stats

이해가 어려웠던 부분:


추가 학습 필요 항목:

```

---

## ➡️ 다음 학습: Day 54

**주제**: Month 2 프로젝트 설계
- 3-Tier 애플리케이션 아키텍처
- Kubernetes 리소스 계획
- 프로젝트 구조 설계
