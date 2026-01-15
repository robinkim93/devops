# Day 31: Kubernetes 소개 및 아키텍처

## 오늘의 목표

토스플레이스 연결점: "Kubernetes 기반의 컨테이너 오케스트레이션 서비스 운영"
"Kubernetes 클러스터를 운영/최적화하며 대규모 트래픽 환경에 대응"

Kubernetes가 무엇이고 왜 필요한지, 핵심 아키텍처를 깊이 있게 이해합니다.

---

## 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 학습 | 45분 | 필요성, 핵심 기능 |
| 아키텍처 | 1시간 | Control Plane, Worker Node |
| 환경 구성 | 45분 | minikube 설치 |
| 기본 실습 | 1시간 | kubectl, Pod |
| 정리 | 30분 | 요약 및 면접 대비 |

---

## Part 1: Kubernetes란? (45분)

### 1.1 왜 Kubernetes가 필요한가?

Docker만으로 운영할 때의 한계:

```
Docker만 사용할 때 문제점:

1. 자동 복구 없음
   컨테이너가 죽으면 수동으로 재시작해야 함
   야간에 장애 발생하면?

2. 수평 확장 어려움
   트래픽이 급증하면 수동으로 컨테이너 추가
   어떤 서버에 배치?

3. 로드 밸런싱 복잡
   여러 컨테이너에 트래픽 분산?
   컨테이너 추가/제거 시 수동 업데이트

4. 무중단 배포 어려움
   새 버전 배포 시 서비스 중단

5. 서비스 디스커버리
   컨테이너 IP가 동적으로 변함

Kubernetes가 이 모든 것을 해결!
```

### 1.2 Kubernetes = 컨테이너 오케스트레이션

```
Kubernetes 핵심 기능:

+------------------+------------------------------------------+
|       기능       |                   설명                   |
+------------------+------------------------------------------+
| Self-healing     | 컨테이너 죽으면 자동 재시작              |
| Auto-scaling     | CPU/메모리 기반 자동 확장                |
| Load Balancing   | 트래픽을 여러 Pod에 자동 분산            |
| Rolling Update   | 무중단 배포 (점진적 교체)                |
| Rollback         | 문제 발생 시 이전 버전으로 복구          |
| Service Discovery| DNS 기반 서비스 발견                     |
| Config Management| ConfigMap, Secret으로 설정 분리          |
| Storage          | 영구 스토리지 관리                       |
+------------------+------------------------------------------+
```

### 1.3 Kubernetes 기본 용어

| 용어 | 설명 |
|------|------|
| Cluster | K8s가 관리하는 서버 그룹 |
| Node | 클러스터 내 개별 서버 |
| Pod | 최소 배포 단위 (1개 이상의 컨테이너) |
| Deployment | Pod의 선언적 관리 |
| Service | Pod에 대한 네트워크 엔드포인트 |
| Namespace | 리소스의 논리적 분리 |
| kubectl | Kubernetes CLI 도구 |

---

## Part 2: 아키텍처 이해 (1시간)

### 2.1 클러스터 구성

```
Kubernetes Cluster 구조:

+------------------------------------------------------------------+
|                     Kubernetes Cluster                             |
+------------------------------------------------------------------+
|                                                                    |
|  +----------------------+       +---------------------------+      |
|  |   Control Plane      |       |      Worker Nodes         |      |
|  |   (Master)           |       |                           |      |
|  |                      |       |  +------+  +------+       |      |
|  |  +---------------+   |       |  | Node |  | Node |  ...  |      |
|  |  | API Server    |<--------->|  | 1    |  | 2    |       |      |
|  |  +---------------+   |       |  +------+  +------+       |      |
|  |  | etcd          |   |       |      |          |         |      |
|  |  +---------------+   |       |  +-------+  +-------+     |      |
|  |  | Scheduler     |   |       |  | Pod A |  | Pod B |     |      |
|  |  +---------------+   |       |  | Pod C |  | Pod D |     |      |
|  |  | Controller    |   |       |  +-------+  +-------+     |      |
|  |  | Manager       |   |       |                           |      |
|  |  +---------------+   |       +---------------------------+      |
|  +----------------------+                                          |
|              kubectl -----> API Server                             |
+------------------------------------------------------------------+
```

### 2.2 Control Plane 컴포넌트

| 컴포넌트 | 역할 | 상세 설명 |
|---------|------|----------|
| **API Server** | 모든 요청의 진입점 | kubectl, 다른 컴포넌트 모두 API Server를 통해 통신 |
| **etcd** | 클러스터 상태 저장소 | 모든 클러스터 데이터 저장 (Key-Value DB) |
| **Scheduler** | Pod 배치 결정 | 새 Pod를 어느 Node에 배치할지 결정 |
| **Controller Manager** | 원하는 상태 유지 | ReplicaSet, Deployment 등 컨트롤러 실행 |

```
Control Plane 동작 흐름:

1. kubectl apply -f deployment.yaml
         |
         v
2. API Server가 요청 수신
         |
         v
3. etcd에 Deployment 정보 저장
         |
         v
4. Controller Manager가 감지
   -> ReplicaSet 생성 -> Pod 생성 필요 인식
         |
         v
5. Scheduler가 Pod 배치할 Node 결정
         |
         v
6. 해당 Node의 kubelet이 Pod 실행
```

### 2.3 Worker Node 컴포넌트

| 컴포넌트 | 역할 | 상세 설명 |
|---------|------|----------|
| **kubelet** | Pod 관리 에이전트 | API Server와 통신, Pod 생성/삭제/모니터링 |
| **Container Runtime** | 컨테이너 실행 | containerd, CRI-O 등 |
| **kube-proxy** | 네트워크 프록시 | Service의 네트워크 규칙 관리 |

```
Worker Node 내부:

+------------------------------------------+
|              Worker Node                  |
+------------------------------------------+
|  +------------+                           |
|  |  kubelet   |  <-- API Server와 통신    |
|  +------------+                           |
|       |                                   |
|       v                                   |
|  +---------------+                        |
|  | Container     |  containerd / CRI-O    |
|  | Runtime       |                        |
|  +---------------+                        |
|       |                                   |
|       v                                   |
|  +--------+  +--------+  +--------+      |
|  | Pod A  |  | Pod B  |  | Pod C  |      |
|  +--------+  +--------+  +--------+      |
|                                           |
|  +------------+                           |
|  | kube-proxy |  <-- 네트워크 규칙 관리   |
|  +------------+                           |
+------------------------------------------+
```

### 2.4 선언적 상태 관리

```
명령형 (Imperative) vs 선언형 (Declarative):

명령형:
"nginx 컨테이너를 3개 실행해라"
"2개 더 추가해라"
"1개 삭제해라"

선언형:
"nginx Pod이 3개 있어야 한다" (desired state)
-> Kubernetes가 현재 상태를 원하는 상태로 맞춤

Kubernetes는 선언형!
-> YAML로 "원하는 상태"를 정의
-> Controller가 지속적으로 모니터링하며 상태 유지
```

---

## Part 3: 실습 환경 구성 (45분)

### 실습 1: minikube 설치 (macOS)

```bash
# Homebrew로 minikube 설치
brew install minikube

# kubectl 설치
brew install kubectl

# 버전 확인
minikube version
kubectl version --client
```

### 실습 2: minikube 설치 (Linux)

```bash
# minikube 다운로드
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube

# kubectl 설치
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install kubectl /usr/local/bin/kubectl

# 버전 확인
minikube version
kubectl version --client
```

### 실습 3: 클러스터 시작

```bash
# minikube 시작
minikube start

# 상태 확인
minikube status
```

### 실습 4: kubectl 설정 확인

```bash
# kubectl 컨텍스트 확인
kubectl config current-context

# 클러스터 정보
kubectl cluster-info

# 노드 목록
kubectl get nodes
```

---

## Part 4: 기본 실습 (1시간)

### 실습 5: kubectl 기본 명령어

```bash
# 클러스터 정보
kubectl cluster-info

# 노드 목록
kubectl get nodes

# 노드 상세 정보
kubectl describe node minikube

# 모든 리소스 보기
kubectl get all

# 모든 네임스페이스의 Pod
kubectl get pods -A

# 시스템 컴포넌트 확인
kubectl get pods -n kube-system
```

### 실습 6: 첫 번째 Pod 생성

```bash
# 명령형으로 nginx Pod 생성
kubectl run nginx --image=nginx

# Pod 목록 확인
kubectl get pods

# Pod 상태 watch
kubectl get pods -w

# Pod 상세 정보
kubectl describe pod nginx

# Pod 로그
kubectl logs nginx

# Pod 내부 접속
kubectl exec -it nginx -- /bin/bash

# Pod 삭제
kubectl delete pod nginx
```

### 실습 7: YAML로 Pod 생성

```bash
cat << 'EOF' > nginx-pod.yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx-yaml
  labels:
    app: nginx
spec:
  containers:
  - name: nginx
    image: nginx:1.24
    ports:
    - containerPort: 80
EOF

kubectl apply -f nginx-pod.yaml
kubectl get pods
kubectl delete -f nginx-pod.yaml
```

### 실습 8: 시스템 컴포넌트 확인

```bash
# Control Plane 컴포넌트 확인
kubectl get pods -n kube-system

# 각 컴포넌트 확인
kubectl get pods -n kube-system | grep -E "etcd|api|scheduler|controller"

# etcd 상세
kubectl describe pod etcd-minikube -n kube-system | head -50
```

### 실습 9: Namespace 이해

```bash
# 네임스페이스 목록
kubectl get namespaces

# 새 네임스페이스 생성
kubectl create namespace dev

# 특정 네임스페이스에 Pod 생성
kubectl run nginx-dev --image=nginx -n dev

# 확인
kubectl get pods -n dev

# 삭제
kubectl delete namespace dev
```

---

## Part 5: 정리 및 면접 대비

### 핵심 요약

```
Kubernetes 핵심 개념:

1. 선언적 상태 관리
   - YAML로 원하는 상태 정의
   - Controller가 현재 -> 원하는 상태로 조정

2. Control Plane
   - API Server: 모든 통신의 중심
   - etcd: 상태 저장소
   - Scheduler: Pod 배치 결정
   - Controller Manager: 상태 유지

3. Worker Node
   - kubelet: Pod 관리
   - Container Runtime: 컨테이너 실행
   - kube-proxy: 네트워크 관리

4. 핵심 리소스
   - Pod: 최소 배포 단위
   - Deployment: Pod 관리
   - Service: 네트워크 엔드포인트
```

### 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | Kubernetes 필요성 이해 | 자가 치유, 스케일링 등 | |
| 2 | Control Plane 컴포넌트 | API Server, etcd, Scheduler, CM | |
| 3 | Worker Node 컴포넌트 | kubelet, Runtime, kube-proxy | |
| 4 | minikube 클러스터 시작 | minikube start | |
| 5 | kubectl 기본 명령어 | get, describe, logs | |
| 6 | 첫 번째 Pod 생성/삭제 | kubectl run/delete | |
| 7 | YAML로 Pod 생성 | kubectl apply -f | |
| 8 | Namespace 이해 | 리소스 분리 | |

### 핵심 명령어

```bash
# 클러스터 정보
kubectl cluster-info
kubectl get nodes

# Pod 관리
kubectl get pods
kubectl describe pod <name>
kubectl logs <pod>
kubectl exec -it <pod> -- /bin/bash
kubectl delete pod <name>

# YAML 적용
kubectl apply -f <file.yaml>
kubectl delete -f <file.yaml>

# 네임스페이스
kubectl get namespaces
kubectl get pods -n <namespace>
kubectl get pods -A
```

### 면접 대비 핵심 포인트

**Q1: Kubernetes가 Docker Compose와 다른 점은?**
> "Docker Compose는 단일 호스트에서 컨테이너를 관리하지만, Kubernetes는 여러 호스트(클러스터)에서 컨테이너를 관리합니다. 자가 치유, 자동 스케일링, 롤링 업데이트 등 프로덕션에 필요한 기능을 제공합니다."

**Q2: etcd의 역할은?**
> "etcd는 Kubernetes의 모든 클러스터 상태를 저장하는 분산 키-값 저장소입니다. 고가용성을 위해 3개 이상의 노드로 구성합니다."

**Q3: kubelet과 kube-proxy의 차이는?**
> "kubelet은 각 노드에서 Pod의 생명주기를 관리합니다. kube-proxy는 Service의 네트워크 규칙을 관리하고 로드밸런싱을 수행합니다."

---

## 학습 기록

```
학습일: ____년 __월 __일
실제 소요 시간: ____시간

완료한 실습:
- [ ] minikube 설치 및 시작
- [ ] kubectl 기본 명령어
- [ ] Pod 생성/삭제
- [ ] YAML로 Pod 생성
- [ ] 시스템 컴포넌트 확인

이해가 어려웠던 부분:

추가 학습 필요 항목:
```

---

## 정리

```bash
# minikube 중지
minikube stop
```

---

## 다음 학습: Day 32

주제: Pod 상세 학습
- Pod 구조와 생명주기
- 멀티 컨테이너 Pod
- 리소스 제한