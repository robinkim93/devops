# 📅 Day 47: Kubernetes 디버깅 심화

## 🎯 오늘의 목표

> **토스플레이스 핵심**: K8s 환경에서 신속한 문제 진단 및 해결 역량을 확보합니다.

---

## ⏰ 예상 학습 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| Pod 상태 분석 | 1시간 | describe, events |
| 디버깅 기법 | 1시간 | exec, debug, logs |
| 네트워크 디버깅 | 1시간 | DNS, Service, 연결 테스트 |

---

## 📚 Part 1: Pod 상태 이해

### Pod 상태 종류

| 상태 | 설명 | 원인 |
|------|------|------|
| **Pending** | 스케줄 대기 | 리소스 부족, nodeSelector, taint |
| **Running** | 실행 중 | 정상 상태 |
| **Succeeded** | 완료 (Job) | 작업 성공 완료 |
| **Failed** | 실패 | 컨테이너 에러 |
| **Unknown** | 상태 불명 | 노드 통신 실패 |

### 컨테이너 상태

| 상태 | 설명 | 대응 |
|------|------|------|
| **Waiting** | 대기 중 | `describe`로 Reason 확인 |
| **Running** | 실행 중 | 정상 |
| **Terminated** | 종료됨 | Exit Code 확인 |

### 자주 보는 에러

| 상태 | 원인 | 해결 방법 |
|------|------|----------|
| **ImagePullBackOff** | 이미지 없음/인증 실패 | 이미지명, imagePullSecret 확인 |
| **CrashLoopBackOff** | 앱 반복 크래시 | `logs --previous` 확인 |
| **OOMKilled** | 메모리 초과 | limits.memory 증가 |
| **CreateContainerConfigError** | 설정 오류 | ConfigMap/Secret 확인 |
| **RunContainerError** | 실행 실패 | SecurityContext, command 확인 |

---

## 🛠️ Part 2: 기본 디버깅 명령어

### describe - 상세 정보 확인

```bash
# Pod 상세 정보
kubectl describe pod <pod-name>

# 확인 포인트:
# - Status: Pod 상태
# - Conditions: Ready, ContainersReady, Initialized, PodScheduled
# - Events: 최근 이벤트 (에러 원인)
# - Containers.State: 컨테이너 상태
```

**describe 출력 예시 분석**:

```yaml
# Events 섹션 (중요!)
Events:
  Type     Reason     Age   From               Message
  ----     ------     ----  ----               -------
  Normal   Scheduled  30s   default-scheduler  Successfully assigned...
  Normal   Pulled     28s   kubelet            Container image "nginx" already present
  Normal   Created    28s   kubelet            Created container nginx
  Normal   Started    28s   kubelet            Started container nginx
```

### events - 클러스터 이벤트

```bash
# 모든 이벤트 (최신순)
kubectl get events --sort-by='.lastTimestamp'

# 특정 네임스페이스
kubectl get events -n kube-system --sort-by='.lastTimestamp'

# 특정 리소스 관련 이벤트
kubectl get events --field-selector involvedObject.name=<pod-name>

# Warning만 필터
kubectl get events --field-selector type=Warning

# Watch 모드
kubectl get events -w
```

### logs - 컨테이너 로그

```bash
# 기본 로그
kubectl logs <pod-name>

# 실시간 follow
kubectl logs -f <pod-name> --tail=100

# 이전 컨테이너 로그 (재시작된 경우)
kubectl logs <pod-name> --previous

# 특정 컨테이너 (멀티컨테이너)
kubectl logs <pod-name> -c <container-name>

# 시간 기반 필터
kubectl logs --since=1h <pod-name>
kubectl logs --since-time='2024-01-01T00:00:00Z' <pod-name>
```

---

## 🛠️ Part 3: 고급 디버깅 기법

### exec - 컨테이너 접속

```bash
# 셸 접속
kubectl exec -it <pod-name> -- /bin/sh
kubectl exec -it <pod-name> -- /bin/bash

# 특정 컨테이너
kubectl exec -it <pod-name> -c <container> -- /bin/sh

# 단일 명령 실행
kubectl exec <pod-name> -- cat /etc/nginx/nginx.conf
kubectl exec <pod-name> -- env
kubectl exec <pod-name> -- ls -la /app
```

### debug - 디버그 컨테이너 (K8s 1.25+)

```bash
# Ephemeral 디버그 컨테이너
kubectl debug <pod-name> -it --image=busybox

# 프로세스 네임스페이스 공유
kubectl debug <pod-name> -it --image=busybox --target=<container>

# 노드 디버깅
kubectl debug node/<node-name> -it --image=busybox

# 복사본 생성하여 디버깅
kubectl debug <pod-name> -it --image=busybox --copy-to=debug-pod
```

### 유용한 디버그 이미지

| 이미지 | 용도 |
|--------|------|
| `busybox` | 기본 유틸리티 |
| `nicolaka/netshoot` | 네트워크 디버깅 (tcpdump, nslookup, curl) |
| `curlimages/curl` | HTTP 테스트 |
| `bitnami/kubectl` | kubectl 명령 실행 |

---

## 🛠️ Part 4: 네트워크 디버깅

### DNS 테스트

```bash
# DNS 확인
kubectl run dns-test --image=busybox --rm -it --restart=Never -- nslookup kubernetes
kubectl run dns-test --image=busybox --rm -it --restart=Never -- nslookup <service-name>

# CoreDNS 로그
kubectl logs -n kube-system -l k8s-app=kube-dns

# DNS 설정 확인
kubectl exec <pod> -- cat /etc/resolv.conf
```

### Service 연결 테스트

```bash
# ClusterIP 서비스 테스트
kubectl run curl-test --image=curlimages/curl --rm -it --restart=Never -- \
  curl -v http://<service-name>:<port>

# 전체 DNS명
kubectl run curl-test --image=curlimages/curl --rm -it --restart=Never -- \
  curl -v http://<service-name>.<namespace>.svc.cluster.local:<port>
```

### Pod 간 통신 테스트

```bash
# Pod IP 확인
kubectl get pods -o wide

# ping 테스트
kubectl exec <pod-name> -- ping -c 3 <another-pod-ip>

# 포트 연결 테스트
kubectl exec <pod-name> -- nc -zv <target-ip> <port>

# tcpdump (netshoot 이미지)
kubectl debug <pod-name> -it --image=nicolaka/netshoot -- tcpdump -i eth0
```

### Endpoint 확인

```bash
# Service의 Endpoints
kubectl get endpoints <service-name>

# Endpoint 상세
kubectl describe endpoints <service-name>

# Endpoints가 비어있다면:
# - Pod가 Running 상태가 아님
# - Service selector와 Pod label 불일치
# - Pod의 readinessProbe 실패
```

---

## 🛠️ Part 5: 노드 문제 디버깅

### 노드 상태 확인

```bash
# 노드 목록
kubectl get nodes

# 노드 상세
kubectl describe node <node-name>

# 확인 포인트:
# - Conditions: Ready, MemoryPressure, DiskPressure, PIDPressure
# - Allocatable: 할당 가능 리소스
# - Non-terminated Pods: 실행 중인 Pod

# 노드 리소스 사용량
kubectl top nodes
```

### 노드 Conditions

| Condition | True일 때 의미 |
|-----------|---------------|
| Ready | 노드 정상 |
| MemoryPressure | 메모리 부족 |
| DiskPressure | 디스크 부족 |
| PIDPressure | 프로세스 수 초과 |
| NetworkUnavailable | 네트워크 문제 |

### Taints 확인

```bash
# 노드의 Taints
kubectl describe node <node> | grep Taints

# Taints 제거
kubectl taint nodes <node> key:NoSchedule-
```

---

## 📊 트러블슈팅 플로우차트

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Pod 트러블슈팅 플로우                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   Pod 문제 발생                                                              │
│       │                                                                     │
│       ▼                                                                     │
│   kubectl get pods                                                          │
│       │                                                                     │
│       ├─▶ Pending ─▶ describe pod (Events 확인)                             │
│       │              └─▶ 리소스 부족? nodeSelector? Taints?                  │
│       │                                                                     │
│       ├─▶ ImagePullBackOff ─▶ describe pod                                  │
│       │                       └─▶ 이미지명 오류? 인증 실패?                   │
│       │                                                                     │
│       ├─▶ CrashLoopBackOff ─▶ kubectl logs --previous                       │
│       │                       └─▶ 앱 에러 확인                               │
│       │                                                                     │
│       ├─▶ Running but unhealthy ─▶ describe pod (Conditions)                │
│       │                            └─▶ readiness/liveness probe?             │
│       │                                                                     │
│       └─▶ Running but not responding ─▶ kubectl exec 접속                   │
│                                        └─▶ 앱 상태, 네트워크 확인            │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 명령어 | 완료 |
|---|------|--------|------|
| 1 | Pod 상태 확인 | `kubectl get pods` | ☐ |
| 2 | Pod 상세 정보 | `kubectl describe pod` | ☐ |
| 3 | 이벤트 확인 | `kubectl get events --sort-by='.lastTimestamp'` | ☐ |
| 4 | 로그 확인 | `kubectl logs --previous` | ☐ |
| 5 | 컨테이너 접속 | `kubectl exec -it -- /bin/sh` | ☐ |
| 6 | 디버그 컨테이너 | `kubectl debug -it --image=busybox` | ☐ |
| 7 | DNS 테스트 | `nslookup <service>` | ☐ |
| 8 | Service 연결 | `curl http://<service>` | ☐ |
| 9 | Endpoint 확인 | `kubectl get endpoints` | ☐ |
| 10 | 노드 상태 | `kubectl describe node` | ☐ |

---

## 🔑 핵심 명령어 요약

```bash
# 상태 확인
kubectl get pods
kubectl describe pod <pod>
kubectl get events --sort-by='.lastTimestamp'

# 로그
kubectl logs <pod> -f --tail=100
kubectl logs <pod> --previous

# 접속 및 디버깅
kubectl exec -it <pod> -- /bin/sh
kubectl debug <pod> -it --image=busybox

# 네트워크
kubectl run test --image=busybox --rm -it -- nslookup <service>
kubectl get endpoints <service>
```

---

## ➡️ 다음 학습: Day 48

**주제**: RBAC (권한 관리)

