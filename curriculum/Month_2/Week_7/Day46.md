# 📅 Day 46: Kubernetes 로깅 심화

## 🎯 오늘의 목표

> **토스플레이스 핵심**: Pod 로그 확인 및 중앙 로깅 시스템 이해로 장애 분석 역량을 강화합니다.

---

## ⏰ 예상 학습 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 기본 로깅 | 1시간 | kubectl logs |
| 고급 로깅 | 1시간 | stern, 필터링 |
| 로깅 아키텍처 | 1시간 | EFK/PLG 스택 |

---

## 📚 Part 1: Kubernetes 로깅 개요

### 로깅 계층 구조

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     Kubernetes 로깅 아키텍처                                  │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                        Application Layer                             │   │
│   │   Container → stdout/stderr → /var/log/containers/                  │   │
│   └────────────────────────────────────────────────────────────────────┬┘   │
│                                                                         │    │
│   ┌─────────────────────────────────────────────────────────────────────▼   │
│   │                        Node Layer                                    │   │
│   │   /var/log/pods/ → kubelet → Node Agent (Fluentd/Fluent Bit)        │   │
│   └────────────────────────────────────────────────────────────────────┬┘   │
│                                                                         │    │
│   ┌─────────────────────────────────────────────────────────────────────▼   │
│   │                        Cluster Layer                                 │   │
│   │   Elasticsearch/Loki → Kibana/Grafana                               │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### 로그 저장 경로

| 경로 | 설명 |
|------|------|
| `/var/log/containers/` | 컨테이너 로그 (심볼릭 링크) |
| `/var/log/pods/` | Pod 로그 |
| `/var/log/messages` | 시스템 메시지 |
| `/var/log/kube-apiserver.log` | API 서버 로그 |

---

## 🛠️ Part 2: kubectl logs 명령어

### 기본 사용법

```bash
# Pod 로그 확인
kubectl logs <pod-name>

# 네임스페이스 지정
kubectl logs <pod-name> -n <namespace>

# 실시간 로그 follow
kubectl logs -f <pod-name>

# 최근 N줄만 출력
kubectl logs --tail=100 <pod-name>

# 특정 시간 이후 로그
kubectl logs --since=1h <pod-name>
kubectl logs --since=30m <pod-name>

# 특정 시간 이전 로그
kubectl logs --since-time='2024-01-01T00:00:00Z' <pod-name>
```

### 멀티 컨테이너 Pod 로그

```bash
# 컨테이너 지정
kubectl logs <pod-name> -c <container-name>

# 모든 컨테이너 로그
kubectl logs <pod-name> --all-containers=true

# 컨테이너 목록 확인
kubectl get pod <pod-name> -o jsonpath='{.spec.containers[*].name}'
```

### 이전 컨테이너 로그

```bash
# 재시작된 컨테이너의 이전 로그
kubectl logs <pod-name> --previous

# 특정 컨테이너의 이전 로그
kubectl logs <pod-name> -c <container> --previous
```

### 라벨 기반 다중 Pod 로그

```bash
# 라벨로 여러 Pod 로그
kubectl logs -l app=nginx

# 모든 컨테이너 포함
kubectl logs -l app=nginx --all-containers=true

# 최대 Pod 수 제한
kubectl logs -l app=nginx --max-log-requests=10
```

---

## 🛠️ Part 3: 고급 로깅 도구

### stern - 멀티 Pod 로그

```bash
# 설치
brew install stern  # macOS
# 또는
kubectl krew install stern

# 기본 사용
stern <pod-name-prefix>

# 네임스페이스 지정
stern myapp -n production

# 정규식 매칭
stern "api-.*" -n default

# 컨테이너 필터
stern myapp -c sidecar

# 시간 포맷
stern myapp --timestamps

# 출력 포맷 (json)
stern myapp -o json

# 특정 시간 이후
stern myapp --since 10m

# 라벨 기반
stern -l app=myapp
```

### 로그 필터링

```bash
# grep과 조합
kubectl logs <pod> | grep ERROR

# 여러 패턴 검색
kubectl logs <pod> | grep -E "ERROR|WARN"

# 특정 패턴 제외
kubectl logs <pod> | grep -v DEBUG

# JSON 로그 파싱 (jq)
kubectl logs <pod> | jq 'select(.level=="error")'

# 타임스탬프 기반 필터
kubectl logs <pod> --since-time='2024-01-01T10:00:00Z' | head -100
```

---

## 🛠️ Part 4: 중앙 로깅 시스템

### EFK 스택 (Elasticsearch + Fluentd + Kibana)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           EFK Stack                                          │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Node 1          Node 2          Node 3                                    │
│   ┌──────┐        ┌──────┐        ┌──────┐                                  │
│   │Fluentd│        │Fluentd│        │Fluentd│  ← DaemonSet                   │
│   └───┬───┘        └───┬───┘        └───┬───┘                                │
│       │                │                │                                    │
│       └────────────────┴────────────────┘                                    │
│                        │                                                     │
│                        ▼                                                     │
│                ┌──────────────┐                                              │
│                │ Elasticsearch │  ← StatefulSet                              │
│                └───────┬──────┘                                              │
│                        │                                                     │
│                        ▼                                                     │
│                ┌──────────────┐                                              │
│                │    Kibana    │  ← Deployment                                │
│                └──────────────┘                                              │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### PLG 스택 (Promtail + Loki + Grafana)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           PLG Stack (경량)                                   │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Node 1          Node 2          Node 3                                    │
│   ┌────────┐      ┌────────┐      ┌────────┐                                │
│   │Promtail│      │Promtail│      │Promtail│  ← DaemonSet                   │
│   └────┬───┘      └────┬───┘      └────┬───┘                                │
│        │               │               │                                     │
│        └───────────────┴───────────────┘                                     │
│                        │                                                     │
│                        ▼                                                     │
│                  ┌──────────┐                                                │
│                  │   Loki   │  ← StatefulSet (라벨 인덱싱만)                  │
│                  └─────┬────┘                                                │
│                        │                                                     │
│                        ▼                                                     │
│                  ┌──────────┐                                                │
│                  │ Grafana  │  ← Deployment                                  │
│                  └──────────┘                                                │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Loki 쿼리 (LogQL)

```bash
# 기본 쿼리
{app="nginx"}

# 라벨 필터
{namespace="production", app="api"}

# 텍스트 검색
{app="api"} |= "error"

# 정규식 매칭
{app="api"} |~ "status=[45].*"

# JSON 파싱
{app="api"} | json | level="error"

# 집계
count_over_time({app="api"} |= "error" [5m])
rate({app="api"} |= "error" [5m])
```

---

## 🛠️ Part 5: 로깅 베스트 프랙티스

### 구조화된 로깅 (JSON)

```go
// 좋은 예: 구조화된 로그
log.Info().
    Str("user_id", userID).
    Str("action", "login").
    Int("duration_ms", 150).
    Msg("user login successful")

// 출력: {"level":"info","user_id":"u123","action":"login","duration_ms":150,"message":"user login successful"}
```

### 로그 레벨

| 레벨 | 용도 |
|------|------|
| DEBUG | 개발/디버깅 정보 |
| INFO | 일반 운영 정보 |
| WARN | 잠재적 문제 |
| ERROR | 에러 발생 |
| FATAL | 치명적 에러 |

### Kubernetes 로깅 권장사항

```yaml
# Pod 로그 설정
apiVersion: v1
kind: Pod
metadata:
  name: myapp
spec:
  containers:
  - name: app
    image: myapp:1.0
    # stdout/stderr로 출력
    # 파일 로그는 사이드카로 수집
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 명령어 | 완료 |
|---|------|--------|------|
| 1 | kubectl logs 기본 | `kubectl logs <pod>` | ☐ |
| 2 | 실시간 follow | `kubectl logs -f <pod> --tail=100` | ☐ |
| 3 | 시간 기반 필터 | `kubectl logs --since=1h` | ☐ |
| 4 | 이전 컨테이너 로그 | `kubectl logs --previous` | ☐ |
| 5 | 멀티 컨테이너 | `kubectl logs -c <container>` | ☐ |
| 6 | stern 설치 및 사용 | `stern myapp` | ☐ |
| 7 | 로깅 아키텍처 이해 | EFK/PLG 스택 | ☐ |

---

## 🔑 핵심 명령어 요약

```bash
# 기본
kubectl logs <pod>
kubectl logs -f <pod> --tail=100
kubectl logs --since=30m <pod>
kubectl logs --previous <pod>

# 멀티 컨테이너
kubectl logs <pod> -c <container>
kubectl logs <pod> --all-containers

# 라벨 기반
kubectl logs -l app=myapp

# stern
stern <pod-prefix> -n <namespace>
stern -l app=myapp --since 10m
```

---

## ➡️ 다음 학습: Day 47

**주제**: Kubernetes 디버깅 - describe, events, exec

