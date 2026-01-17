# 🎯 토스플레이스 DevOps Engineer 180일 합격 커리큘럼

## 📋 채용 공고 기반 학습 목표

### 필수 기술 스택 (채용 공고에서 추출)
| 카테고리 | 기술 | 우선순위 |
|---------|------|---------|
| 컨테이너 오케스트레이션 | **Kubernetes** | 🔴 최우선 |
| Service Mesh | **Istio** | 🔴 최우선 |
| CI/CD | **ArgoCD**, GoCD | 🔴 최우선 |
| 모니터링 | **Prometheus, Thanos, Grafana** | 🔴 최우선 |
| 컨테이너 런타임 | Podman, Containerd | 🟡 중요 |
| 시크릿 관리 | Vault | 🟡 중요 |
| 클라우드 | AWS | 🟡 중요 |
| IaC | Terraform (추정) | 🟡 중요 |
| 기초 | Linux, Network | 🟢 기반 |

### 필수 역량 (면접에서 검증)
1. ✅ 장애 대응 + 근본 원인 분석 + **구조적 개선** 경험
2. ✅ OS, Network 레이어 **트러블슈팅**
3. ✅ 대규모 트래픽 환경 대응
4. ✅ 인프라 **오너십** + 주도적 개선

---

# 📅 Month 1: Linux & Container 기초 (Day 1-30)

> **목표**: OS/Network 트러블슈팅 역량 + 컨테이너 기초

## Week 1: Linux 핵심 (Day 1-7)

| Day | 주제 | 핵심 학습 내용 | 실습 |
|-----|------|--------------|------|
| 1 | Kernel vs User Space | System Call, Mode Switch 개념 | `strace ls`, `strace -c` |
| 2 | Process & Thread | fork, exec, PID, 좀비 프로세스 | `/proc/[pid]/status` 분석 |
| 3 | Memory Management | 가상 메모리, Page Cache, Swap | `free -h`, `/proc/meminfo` |
| 4 | File System | VFS, inode, FD, 파일 권한 | `lsof`, `/proc/[pid]/fd` |
| 5 | I/O & Storage | Block I/O, Buffer/Cache | `iostat`, `iotop` |
| 6 | Systemd | Unit 파일, 서비스 관리 | systemctl, journalctl |
| 7 | **Week 1 복습** | 트러블슈팅 시나리오 실습 | 종합 실습 |

### Week 1 핵심 명령어
```bash
# 반드시 익숙해져야 할 명령어
strace -c -p <pid>          # 시스템 콜 분석
lsof -p <pid>               # 열린 파일/소켓 확인
/proc/[pid]/status          # 프로세스 상태
free -h && cat /proc/meminfo # 메모리 분석
iostat -x 1                 # 디스크 I/O 분석
```

## Week 2: Network 핵심 (Day 8-14)

| Day | 주제 | 핵심 학습 내용 | 실습 |
|-----|------|--------------|------|
| 8 | TCP/IP 기초 | 3-way handshake, 4-way, 상태 | `ss -tlnp`, `netstat` |
| 9 | DNS | 동작 원리, 레코드 타입, 캐싱 | `dig`, `nslookup` |
| 10 | HTTP/HTTPS | Keep-alive, TLS handshake | `curl -v`, `openssl s_client` |
| 11 | Load Balancing | L4 vs L7, 알고리즘 | nginx LB 구성 |
| 12 | Network 트러블슈팅 | 패킷 분석, 지연 원인 분석 | `tcpdump`, `wireshark` |
| 13 | iptables/nftables | 방화벽, NAT, 패킷 필터링 | iptables 규칙 작성 |
| 14 | **Week 2 복습** | 네트워크 장애 시나리오 | 종합 실습 |

### Week 2 핵심 명령어
```bash
ss -tlnp                    # TCP 리스닝 포트 확인
ss -s                       # 소켓 통계
tcpdump -i eth0 port 80     # 패킷 캡처
curl -w "@curl-format.txt" -o /dev/null -s <url>  # 지연 분석
dig +trace example.com      # DNS 추적
```

## Week 3: Container 기초 (Day 15-21)

| Day | 주제 | 핵심 학습 내용 | 실습 |
|-----|------|--------------|------|
| 15 | 컨테이너 원리 | Namespace, Cgroup, Union FS | `unshare`, `nsenter` |
| 16 | Docker 기초 | 이미지, 컨테이너, 볼륨, 네트워크 | Docker CLI 실습 |
| 17 | Dockerfile | 멀티스테이지 빌드, 최적화 | 최적화된 이미지 빌드 |
| 18 | Docker Compose | 멀티 컨테이너 앱 구성 | 3-tier 앱 구성 |
| 19 | Container Runtime | containerd, Podman 차이 | Podman 실습 |
| 20 | 컨테이너 네트워킹 | bridge, host, overlay | 네트워크 모드 실습 |
| 21 | **Week 3 복습** | 컨테이너 트러블슈팅 | 종합 실습 |

### Week 3 핵심 개념
```bash
# 컨테이너 격리 원리 이해
ls /proc/[pid]/ns/          # 네임스페이스 확인
cat /sys/fs/cgroup/         # cgroup 확인

# Podman (Docker 대체) - 토스플레이스에서 사용
podman run -d nginx
podman build -t myapp .
```

## Week 4: 종합 실습 & 프로젝트 (Day 22-30)

| Day | 주제 | 실습 내용 |
|-----|------|----------|
| 22-24 | 미니 프로젝트 1 | Docker로 Spring Boot + MySQL + Redis 구성 |
| 25-27 | 트러블슈팅 실습 | 의도적 장애 주입 → 분석 → 해결 |
| 28-30 | **Month 1 정리** | 학습 내용 문서화, 블로그 작성 |

### Month 1 완료 기준 ✅
- [ ] strace로 시스템 콜 분석 가능
- [ ] tcpdump로 네트워크 패킷 분석 가능
- [ ] Dockerfile 최적화 가능
- [ ] 컨테이너 격리 원리 설명 가능

---

# 📅 Month 2: Kubernetes 핵심 (Day 31-60)

> **목표**: K8s 클러스터 운영 역량 확보

## Week 5: Kubernetes 아키텍처 (Day 31-37)

| Day | 주제 | 핵심 학습 내용 | 실습 |
|-----|------|--------------|------|
| 31 | K8s 아키텍처 | Control Plane, Worker Node 구성 | 아키텍처 다이어그램 |
| 32 | Pod | 생명주기, Multi-container, Init Container | Pod 생성/관리 |
| 33 | ReplicaSet & Deployment | 롤링 업데이트, 롤백 | 배포 전략 실습 |
| 34 | Service | ClusterIP, NodePort, LoadBalancer | 서비스 노출 |
| 35 | ConfigMap & Secret | 설정 분리, 시크릿 관리 | 환경별 설정 관리 |
| 36 | Namespace & RBAC | 멀티테넌시, 권한 관리 | RBAC 정책 작성 |
| 37 | **Week 5 복습** | 기본 워크로드 배포 | 종합 실습 |

### Week 5 핵심 명령어
```bash
# 반드시 익숙해져야 할 kubectl 명령어
kubectl get pods -o wide
kubectl describe pod <name>
kubectl logs -f <pod> -c <container>
kubectl exec -it <pod> -- /bin/sh
kubectl apply -f manifest.yaml
kubectl rollout status deployment/<name>
kubectl rollout undo deployment/<name>
```

## Week 6: Kubernetes 스토리지 & 네트워킹 (Day 38-44)

| Day | 주제 | 핵심 학습 내용 | 실습 |
|-----|------|--------------|------|
| 38 | Volume | emptyDir, hostPath, PV/PVC | 볼륨 마운트 |
| 39 | StorageClass | Dynamic Provisioning | AWS EBS CSI |
| 40 | K8s 네트워킹 | CNI, Pod-to-Pod, Service 통신 | Calico/Cilium |
| 41 | Ingress | 라우팅, TLS Termination | Nginx Ingress |
| 42 | NetworkPolicy | Pod 간 트래픽 제어 | 정책 작성 |
| 43 | DNS in K8s | CoreDNS, Service Discovery | DNS 디버깅 |
| 44 | **Week 6 복습** | 네트워크 트러블슈팅 | 종합 실습 |

## Week 7: Kubernetes 고급 (Day 45-51)

| Day | 주제 | 핵심 학습 내용 | 실습 |
|-----|------|--------------|------|
| 45 | Resource Management | Requests/Limits, QoS | 리소스 최적화 |
| 46 | HPA & VPA | 오토스케일링 | 부하 테스트 + HPA |
| 47 | Scheduling | Node Selector, Affinity, Taint/Toleration | 스케줄링 제어 |
| 48 | DaemonSet & StatefulSet | 특수 워크로드 | StatefulSet 배포 |
| 49 | Jobs & CronJobs | 배치 워크로드 | 배치 작업 구성 |
| 50 | Helm | 패키지 매니저 | Chart 작성 |
| 51 | **Week 7 복습** | 고급 워크로드 | 종합 실습 |

### Week 7 핵심 개념
```yaml
# HPA 예시 - 토스플레이스 대규모 트래픽 대응
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: app-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: app
  minReplicas: 3
  maxReplicas: 100
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

## Week 8: K8s 클러스터 운영 (Day 52-60)

| Day | 주제 | 핵심 학습 내용 | 실습 |
|-----|------|--------------|------|
| 52 | 클러스터 설치 | kubeadm, EKS | EKS 클러스터 구축 |
| 53 | etcd | 백업/복구 | etcd 스냅샷 |
| 54 | 클러스터 업그레이드 | 버전 업그레이드 전략 | 업그레이드 실습 |
| 55 | 트러블슈팅 | Pod 장애, Node 장애 | 장애 시나리오 |
| 56-58 | **미니 프로젝트 2** | EKS에 3-tier 앱 배포 | 전체 구성 |
| 59-60 | **Month 2 정리** | CKA 수준 문제 풀이 | 복습 |

### Month 2 완료 기준 ✅
- [ ] kubectl 자유자재로 사용
- [ ] Deployment, Service, Ingress 구성 가능
- [ ] HPA로 오토스케일링 구성 가능
- [ ] K8s 네트워크 트러블슈팅 가능
- [ ] Helm Chart 작성 가능

---

# 📅 Month 3: Istio & Service Mesh (Day 61-90)

> **목표**: Istio 운영 역량 (토스플레이스 핵심 기술!)

## Week 9: Service Mesh 개념 (Day 61-67)

| Day | 주제 | 핵심 학습 내용 | 실습 |
|-----|------|--------------|------|
| 61 | Service Mesh란? | 왜 필요한가, Sidecar 패턴 | 개념 정리 |
| 62 | Istio 아키텍처 | Envoy, istiod, Control Plane | 아키텍처 분석 |
| 63 | Istio 설치 | istioctl, Operator | EKS에 설치 |
| 64 | Sidecar Injection | 자동/수동 주입 | 주입 실습 |
| 65 | Traffic Management | VirtualService, DestinationRule | 라우팅 실습 |
| 66 | Gateway | Ingress Gateway | 외부 트래픽 관리 |
| 67 | **Week 9 복습** | 기본 트래픽 관리 | 종합 실습 |

### Week 9 핵심 개념
```yaml
# VirtualService - 트래픽 라우팅
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
          exact: jason
    route:
    - destination:
        host: reviews
        subset: v2
  - route:
    - destination:
        host: reviews
        subset: v1
```

## Week 10: Istio 트래픽 관리 심화 (Day 68-74)

| Day | 주제 | 핵심 학습 내용 | 실습 |
|-----|------|--------------|------|
| 68 | Canary 배포 | 가중치 기반 라우팅 | 카나리 실습 |
| 69 | Blue-Green 배포 | 트래픽 전환 | B/G 실습 |
| 70 | A/B Testing | 헤더 기반 라우팅 | A/B 테스트 |
| 71 | Circuit Breaker | Outlier Detection | 장애 격리 |
| 72 | Retry & Timeout | 탄력성 패턴 | 재시도 정책 |
| 73 | Rate Limiting | 트래픽 제한 | 속도 제한 |
| 74 | **Week 10 복습** | 배포 전략 종합 | 종합 실습 |

## Week 11: Istio 보안 & Observability (Day 75-81)

| Day | 주제 | 핵심 학습 내용 | 실습 |
|-----|------|--------------|------|
| 75 | mTLS | 서비스 간 암호화 | mTLS 설정 |
| 76 | AuthorizationPolicy | 서비스 접근 제어 | 정책 작성 |
| 77 | RequestAuthentication | JWT 검증 | 인증 설정 |
| 78 | Kiali | 서비스 메시 시각화 | Kiali 대시보드 |
| 79 | Jaeger | 분산 트레이싱 | 트레이싱 분석 |
| 80 | Envoy 메트릭 | Prometheus 연동 | 메트릭 수집 |
| 81 | **Week 11 복습** | 보안 + 관측성 | 종합 실습 |

## Week 12: Istio 운영 & 프로젝트 (Day 82-90)

| Day | 주제 | 실습 내용 |
|-----|------|----------|
| 82-84 | Istio 트러블슈팅 | Envoy 로그 분석, 문제 해결 |
| 85-87 | **미니 프로젝트 3** | Istio 기반 마이크로서비스 배포 (카나리 + mTLS) |
| 88-90 | **Month 3 정리** | 학습 내용 정리, 블로그 작성 |

### Month 3 완료 기준 ✅
- [ ] Istio 아키텍처 설명 가능
- [ ] VirtualService/DestinationRule 작성 가능
- [ ] Canary/Blue-Green 배포 구현 가능
- [ ] mTLS 설정 가능
- [ ] Kiali/Jaeger로 트래픽 분석 가능

---

# 📅 Month 4: CI/CD & GitOps (Day 91-120)

> **목표**: ArgoCD 기반 GitOps 파이프라인 구축

## Week 13: CI 기초 (Day 91-97)

| Day | 주제 | 핵심 학습 내용 | 실습 |
|-----|------|--------------|------|
| 91 | CI/CD 개념 | 파이프라인, 자동화 원칙 | 개념 정리 |
| 92 | GitHub Actions | Workflow, Job, Step | 기본 파이프라인 |
| 93 | 빌드 자동화 | Docker 이미지 빌드 & 푸시 | ECR 연동 |
| 94 | 테스트 자동화 | Unit, Integration Test | 테스트 파이프라인 |
| 95 | 정적 분석 | SonarQube, 보안 스캔 | 코드 품질 검사 |
| 96 | 아티팩트 관리 | Container Registry, Versioning | 버전 전략 |
| 97 | **Week 13 복습** | CI 파이프라인 구축 | 종합 실습 |

## Week 14: GitOps & ArgoCD (Day 98-104)

| Day | 주제 | 핵심 학습 내용 | 실습 |
|-----|------|--------------|------|
| 98 | GitOps 원칙 | 선언적 인프라, Git 중심 | 개념 정리 |
| 99 | ArgoCD 설치 | 아키텍처, 설치 | EKS에 설치 |
| 100 | Application 관리 | App 생성, Sync | 앱 배포 |
| 101 | Sync 전략 | Auto Sync, Self Heal | Sync 설정 |
| 102 | Rollback | 버전 관리, 롤백 | 롤백 실습 |
| 103 | Multi-Cluster | 여러 클러스터 관리 | 클러스터 등록 |
| 104 | **Week 14 복습** | ArgoCD 운영 | 종합 실습 |

### Week 14 핵심 개념
```yaml
# ArgoCD Application 예시
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: myapp
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/org/repo.git
    targetRevision: HEAD
    path: k8s/overlays/prod
  destination:
    server: https://kubernetes.default.svc
    namespace: myapp
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

## Week 15: 고급 CD 패턴 (Day 105-111)

| Day | 주제 | 핵심 학습 내용 | 실습 |
|-----|------|--------------|------|
| 105 | Kustomize | Base/Overlay 구조 | 환경별 관리 |
| 106 | Helm + ArgoCD | Helm Chart 배포 | Helm 연동 |
| 107 | ApplicationSet | 동적 앱 생성 | Generator 사용 |
| 108 | Progressive Delivery | Argo Rollouts | Canary with Rollouts |
| 109 | Notification | Slack 알림 | 배포 알림 |
| 110 | RBAC in ArgoCD | 권한 관리 | 접근 제어 |
| 111 | **Week 15 복습** | 고급 패턴 | 종합 실습 |

## Week 16: CI/CD 프로젝트 (Day 112-120)

| Day | 주제 | 실습 내용 |
|-----|------|----------|
| 112-116 | **미니 프로젝트 4** | GitHub Actions + ArgoCD 전체 파이프라인 구축 |
| 117-118 | Vault 연동 | 시크릿 관리 자동화 |
| 119-120 | **Month 4 정리** | 파이프라인 문서화 |

### Month 4 완료 기준 ✅
- [ ] GitHub Actions 워크플로우 작성 가능
- [ ] ArgoCD로 GitOps 배포 가능
- [ ] Kustomize/Helm 환경 분리 가능
- [ ] Progressive Delivery (Canary) 구현 가능

---

# 📅 Month 5: Monitoring & Observability (Day 121-150)

> **목표**: Prometheus + Thanos + Grafana 스택 구축 (토스플레이스 핵심!)

## Week 17: Prometheus 기초 (Day 121-127)

| Day | 주제 | 핵심 학습 내용 | 실습 |
|-----|------|--------------|------|
| 121 | 모니터링 개념 | Metrics, Logs, Traces | 3 Pillars |
| 122 | Prometheus 아키텍처 | Pull 모델, Storage | 설치 |
| 123 | PromQL 기초 | 쿼리 언어, Selectors | 기본 쿼리 |
| 124 | PromQL 심화 | 함수, Aggregation | 복잡한 쿼리 |
| 125 | Exporters | Node, Application Exporter | Exporter 구성 |
| 126 | ServiceMonitor | K8s 연동 | Prometheus Operator |
| 127 | **Week 17 복습** | PromQL 마스터 | 종합 실습 |

### Week 17 핵심 PromQL
```promql
# 반드시 익혀야 할 쿼리들
# CPU 사용률
100 - (avg by(instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# 메모리 사용률
(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100

# HTTP 요청 성공률
sum(rate(http_requests_total{status=~"2.."}[5m])) / sum(rate(http_requests_total[5m])) * 100

# P99 레이턴시
histogram_quantile(0.99, sum(rate(http_request_duration_seconds_bucket[5m])) by (le))
```

## Week 18: Grafana & Alerting (Day 128-134)

| Day | 주제 | 핵심 학습 내용 | 실습 |
|-----|------|--------------|------|
| 128 | Grafana 기초 | 대시보드, 패널 | 설치 & 연동 |
| 129 | 대시보드 설계 | 시각화 베스트 프랙티스 | 대시보드 구축 |
| 130 | Variables | 동적 대시보드 | 변수 활용 |
| 131 | Alerting | AlertManager 연동 | 알림 규칙 |
| 132 | Alert Routing | 라우팅, 그룹화 | 알림 경로 설정 |
| 133 | Notification | Slack, PagerDuty | 알림 연동 |
| 134 | **Week 18 복습** | 모니터링 대시보드 | 종합 실습 |

## Week 19: Thanos & 로깅 (Day 135-141)

| Day | 주제 | 핵심 학습 내용 | 실습 |
|-----|------|--------------|------|
| 135 | Thanos 아키텍처 | Global View, Long-term Storage | 개념 이해 |
| 136 | Thanos 구성 | Sidecar, Query, Store | 설치 |
| 137 | Multi-Cluster 모니터링 | 여러 클러스터 통합 | 통합 구성 |
| 138 | Loki | 로그 수집 아키텍처 | 설치 |
| 139 | LogQL | 로그 쿼리 | 쿼리 실습 |
| 140 | Log + Metrics 연동 | Grafana 통합 뷰 | 대시보드 연동 |
| 141 | **Week 19 복습** | Thanos + Loki | 종합 실습 |

## Week 20: Observability 프로젝트 (Day 142-150)

| Day | 주제 | 실습 내용 |
|-----|------|----------|
| 142-146 | **미니 프로젝트 5** | Prometheus + Thanos + Grafana + Loki 전체 스택 구축 |
| 147-148 | SLO/SLI 정의 | 서비스 레벨 목표 설정 |
| 149-150 | **Month 5 정리** | 모니터링 문서화, 블로그 |

### Month 5 완료 기준 ✅
- [ ] PromQL 자유자재로 작성 가능
- [ ] Grafana 대시보드 설계 가능
- [ ] AlertManager 알림 구성 가능
- [ ] Thanos로 멀티 클러스터 모니터링 가능
- [ ] Loki로 로그 분석 가능

---

# 📅 Month 6: AWS, IaC & 종합 프로젝트 (Day 151-180)

> **목표**: 실무 역량 완성 + 포트폴리오

## Week 21: AWS 핵심 서비스 (Day 151-157)

| Day | 주제 | 핵심 학습 내용 | 실습 |
|-----|------|--------------|------|
| 151 | AWS 기초 | VPC, Subnet, Security Group | 네트워크 구성 |
| 152 | EKS | 관리형 K8s | 클러스터 생성 |
| 153 | IAM | Role, Policy, IRSA | 권한 관리 |
| 154 | ECR & S3 | 이미지/스토리지 | 저장소 관리 |
| 155 | RDS & ElastiCache | 관리형 DB | DB 연동 |
| 156 | CloudWatch | AWS 모니터링 | 로그/메트릭 |
| 157 | **Week 21 복습** | AWS 서비스 연동 | 종합 실습 |

## Week 22: IaC with Terraform (Day 158-164)

| Day | 주제 | 핵심 학습 내용 | 실습 |
|-----|------|--------------|------|
| 158 | Terraform 기초 | HCL, Provider, Resource | 기본 문법 |
| 159 | State 관리 | Remote State, Locking | S3 Backend |
| 160 | Module | 재사용 가능한 코드 | 모듈 작성 |
| 161 | Workspace | 환경 분리 | dev/prod 분리 |
| 162 | EKS with Terraform | 클러스터 IaC | EKS 프로비저닝 |
| 163 | GitOps + Terraform | Atlantis | PR 기반 인프라 |
| 164 | **Week 22 복습** | IaC 베스트 프랙티스 | 종합 실습 |

### Week 22 핵심 코드
```hcl
# EKS 클러스터 Terraform
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 19.0"

  cluster_name    = "tossplace-eks"
  cluster_version = "1.28"

  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets

  eks_managed_node_groups = {
    general = {
      desired_size = 3
      min_size     = 3
      max_size     = 10

      instance_types = ["m5.large"]
    }
  }
}
```

## Week 23-24: 종합 프로젝트 (Day 165-180)

### 🎯 최종 프로젝트: 토스플레이스 인프라 클론

| Day | 단계 | 구현 내용 |
|-----|------|----------|
| 165-167 | 인프라 구축 | Terraform으로 AWS 인프라 + EKS 프로비저닝 |
| 168-170 | K8s 구성 | Istio, ArgoCD, Prometheus 스택 설치 |
| 171-173 | 앱 배포 | 3-tier 앱 GitOps 배포 (Canary) |
| 174-176 | 모니터링 | Grafana 대시보드 + 알림 구성 |
| 177-178 | 장애 시뮬레이션 | Chaos Engineering, 장애 대응 연습 |
| 179-180 | **포트폴리오 완성** | GitHub README, 아키텍처 문서화 |

### 최종 프로젝트 아키텍처
```
┌─────────────────────────────────────────────────────────────────┐
│                    최종 프로젝트 구성도                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  GitHub ──→ GitHub Actions ──→ ECR ──→ ArgoCD ──→ EKS          │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                      EKS Cluster                          │  │
│  │  ┌─────────────────────────────────────────────────────┐ │  │
│  │  │                 Istio Service Mesh                   │ │  │
│  │  │  ┌─────────┐  ┌─────────┐  ┌─────────┐             │ │  │
│  │  │  │Frontend │──│ Backend │──│   DB    │             │ │  │
│  │  │  │(Canary) │  │(mTLS)   │  │(MySQL)  │             │ │  │
│  │  │  └─────────┘  └─────────┘  └─────────┘             │ │  │
│  │  └─────────────────────────────────────────────────────┘ │  │
│  │                                                           │  │
│  │  Prometheus ──→ Thanos ──→ Grafana                       │  │
│  │  Loki ──────────────────────┘                            │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  Terraform으로 전체 인프라 코드화                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

# 📊 180일 커리큘럼 요약

| Month | 주제 | 핵심 산출물 |
|-------|------|-----------|
| **1** | Linux, Network, Container | strace/tcpdump 분석 능력 |
| **2** | Kubernetes 핵심 | EKS 클러스터 운영 |
| **3** | Istio Service Mesh | Canary 배포 + mTLS |
| **4** | CI/CD & GitOps | ArgoCD 파이프라인 |
| **5** | Monitoring | Prometheus + Thanos + Grafana |
| **6** | AWS, IaC, 종합 | **포트폴리오 완성** |

---

# 🎯 면접 대비 체크리스트

## 기술 면접 예상 질문

### Kubernetes
- [ ] "Pod가 Pending 상태일 때 어떻게 트러블슈팅하나요?"
- [ ] "HPA와 VPA의 차이점과 적절한 사용 케이스는?"
- [ ] "StatefulSet은 언제 사용하나요?"

### Istio
- [ ] "Service Mesh를 도입하면 어떤 장점이 있나요?"
- [ ] "Canary 배포를 Istio로 어떻게 구현하나요?"
- [ ] "mTLS가 무엇이고 왜 필요한가요?"

### CI/CD
- [ ] "GitOps의 장점은 무엇인가요?"
- [ ] "ArgoCD와 Jenkins의 차이점은?"
- [ ] "롤백은 어떻게 처리하나요?"

### 모니터링
- [ ] "SLI/SLO/SLA의 차이점은?"
- [ ] "알림 설계 시 고려사항은?"
- [ ] "Thanos가 왜 필요한가요?"

### 장애 대응
- [ ] "실제 장애 대응 경험을 말씀해주세요"
- [ ] "장애 발생 시 어떤 순서로 대응하나요?"
- [ ] "재발 방지를 위해 어떤 노력을 했나요?"

---

# 📚 추천 자료

## 필수 자격증 (선택)
- CKA (Certified Kubernetes Administrator)
- AWS Solutions Architect Associate

## 추천 강의
- Kubernetes 공식 튜토리얼
- Istio 공식 문서
- Prometheus 공식 문서

## 추천 도서
- "Kubernetes in Action"
- "The Site Reliability Workbook"
- "Terraform Up & Running"

---

*생성일: 2026년 1월 7일*
*목표: 토스플레이스 DevOps Engineer 합격*

