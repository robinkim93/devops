# 📅 Day 43: Probe (Health Check) - 서비스 가용성 보장

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "서비스 안정성, 장애 자동 복구"

Kubernetes Probe를 활용하여 문제 있는 Pod를 자동으로 감지하고 복구합니다.

---

## ⏰ 예상 학습 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 | 45분 | Probe 유형과 동작 원리 |
| 실습 | 1.5시간 | Probe 설정 및 테스트 |
| 심화 | 45분 | 프로덕션 패턴 |

---

## 📚 Part 1: Probe 개념 (45분)

### 왜 Health Check가 중요한가?

```
┌─────────────────────────────────────────────────────────────┐
│  Health Check 없이 운영하면...                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  시나리오 1: 애플리케이션 행 (Hang)                         │
│  ┌─────────────────────────────────────────────────┐       │
│  │ Pod 상태: Running ✓                             │       │
│  │ 애플리케이션: 무응답 상태 (데드락)              │       │
│  │ 결과: 사용자 요청 타임아웃 ❌                   │       │
│  │ K8s는 문제를 모름 → 재시작 안 함                │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  시나리오 2: 시작 중인 Pod에 트래픽 전송                    │
│  ┌─────────────────────────────────────────────────┐       │
│  │ Pod 상태: Running ✓                             │       │
│  │ 애플리케이션: 아직 초기화 중 (DB 연결 등)       │       │
│  │ 결과: 503 에러 발생 ❌                          │       │
│  │ Service가 바로 트래픽 전송                       │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  Health Check가 있으면...                                   │
│  ┌─────────────────────────────────────────────────┐       │
│  │ livenessProbe 실패 → 자동 재시작                │       │
│  │ readinessProbe 실패 → 트래픽 차단               │       │
│  │ 결과: 자동 복구, 무중단 운영 ✅                 │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Probe 세 가지 유형

```
┌─────────────────────────────────────────────────────────────┐
│  Kubernetes Probe Types                                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. livenessProbe (생존 확인)                               │
│  ┌─────────────────────────────────────────────────┐       │
│  │ 질문: "컨테이너가 살아있는가?"                  │       │
│  │ 실패 시: 컨테이너 재시작 (restartPolicy 따름)   │       │
│  │ 사용 시점: 데드락, 무한 루프 감지               │       │
│  │                                                  │       │
│  │ ⚠️ 주의: 너무 엄격하면 불필요한 재시작 발생     │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  2. readinessProbe (준비 확인)                              │
│  ┌─────────────────────────────────────────────────┐       │
│  │ 질문: "트래픽 받을 준비가 되었는가?"            │       │
│  │ 실패 시: Service 엔드포인트에서 제외            │       │
│  │ 사용 시점: 초기화 중, 일시적 과부하             │       │
│  │                                                  │       │
│  │ 트래픽 차단만, 재시작 안 함                      │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  3. startupProbe (시작 확인)                                │
│  ┌─────────────────────────────────────────────────┐       │
│  │ 질문: "애플리케이션이 시작되었는가?"            │       │
│  │ 실패 시: 컨테이너 재시작                        │       │
│  │ 사용 시점: 시작이 느린 애플리케이션             │       │
│  │                                                  │       │
│  │ 성공 전까지 liveness/readiness 비활성화          │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Probe 체크 방식

| 방식 | 설명 | 사용 사례 |
|------|------|----------|
| **httpGet** | HTTP GET 요청, 2xx-3xx 성공 | 웹 애플리케이션 |
| **tcpSocket** | TCP 연결 시도 | 데이터베이스, 메시지 큐 |
| **exec** | 컨테이너 내 명령 실행, exit 0 성공 | 복잡한 체크 로직 |
| **grpc** | gRPC 헬스 체크 | gRPC 서비스 |

### Probe 파라미터

| 파라미터 | 설명 | 기본값 |
|----------|------|--------|
| `initialDelaySeconds` | 첫 체크까지 대기 시간 | 0 |
| `periodSeconds` | 체크 간격 | 10 |
| `timeoutSeconds` | 응답 대기 시간 | 1 |
| `successThreshold` | 성공 판정 횟수 | 1 |
| `failureThreshold` | 실패 판정 횟수 | 3 |

---

## 🛠️ Part 2: 실습 (1.5시간)

### 실습 1: livenessProbe 설정

```bash
# livenessProbe가 있는 Pod 생성
kubectl apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: liveness-demo
spec:
  containers:
  - name: app
    image: nginx:1.24
    ports:
    - containerPort: 80
    livenessProbe:
      httpGet:
        path: /
        port: 80
      initialDelaySeconds: 5    # 5초 후 첫 체크
      periodSeconds: 10         # 10초마다 체크
      timeoutSeconds: 3         # 3초 내 응답 필요
      failureThreshold: 3       # 3번 실패 시 재시작
EOF

# Probe 설정 확인
kubectl describe pod liveness-demo | grep -A 10 Liveness

# 로그에서 Probe 결과 확인
kubectl logs liveness-demo

# 이벤트 확인
kubectl get events --field-selector involvedObject.name=liveness-demo
```

### 실습 2: livenessProbe 실패 시뮬레이션

```bash
# 의도적으로 실패하는 Probe
kubectl apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: liveness-fail
spec:
  containers:
  - name: app
    image: busybox:1.36
    command: ["/bin/sh", "-c"]
    args:
      - |
        touch /tmp/healthy
        sleep 30
        rm -f /tmp/healthy
        sleep 600
    livenessProbe:
      exec:
        command:
        - cat
        - /tmp/healthy
      initialDelaySeconds: 5
      periodSeconds: 5
      failureThreshold: 3
EOF

# 재시작 관찰
kubectl get pod liveness-fail -w

# 30초 후 /tmp/healthy 삭제 → Probe 실패 → 재시작
# RESTARTS 카운트 증가 확인
```

### 실습 3: readinessProbe 설정

```bash
kubectl apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: readiness-demo
  labels:
    app: readiness-test
spec:
  containers:
  - name: app
    image: nginx:1.24
    ports:
    - containerPort: 80
    readinessProbe:
      httpGet:
        path: /ready
        port: 80
      initialDelaySeconds: 5
      periodSeconds: 5
      failureThreshold: 3
EOF

# 처음에는 /ready 경로가 없으므로 404 → Not Ready
kubectl get pods readiness-demo
# READY: 0/1

# Service 생성
kubectl expose pod readiness-demo --port=80 --name=readiness-svc

# Endpoints 확인 - 비어있음 (Pod가 Ready가 아니므로)
kubectl get endpoints readiness-svc
# ENDPOINTS: <none>

# Pod에 /ready 경로 추가
kubectl exec readiness-demo -- sh -c 'echo "OK" > /usr/share/nginx/html/ready'

# 이제 Ready 상태가 됨
kubectl get pods readiness-demo
# READY: 1/1

# Endpoints에 Pod 추가됨
kubectl get endpoints readiness-svc
```

### 실습 4: startupProbe 설정

```bash
# 시작이 느린 애플리케이션 시뮬레이션
kubectl apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: startup-demo
spec:
  containers:
  - name: app
    image: busybox:1.36
    command: ["/bin/sh", "-c"]
    args:
      - |
        echo "Starting slow initialization..."
        sleep 60
        echo "Initialization complete"
        touch /tmp/started
        sleep 3600
    startupProbe:
      exec:
        command:
        - cat
        - /tmp/started
      initialDelaySeconds: 0
      periodSeconds: 5
      failureThreshold: 30    # 30 * 5 = 150초 동안 시작 허용
    livenessProbe:
      exec:
        command:
        - cat
        - /tmp/started
      periodSeconds: 10
      failureThreshold: 3
EOF

# startupProbe 성공 전까지 livenessProbe는 비활성화
kubectl describe pod startup-demo | grep -A 10 "Startup\|Liveness"
```

### 실습 5: 실용적인 Deployment 설정

```bash
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: webapp
spec:
  replicas: 3
  selector:
    matchLabels:
      app: webapp
  template:
    metadata:
      labels:
        app: webapp
    spec:
      containers:
      - name: app
        image: nginx:1.24
        ports:
        - containerPort: 80
        
        # 시작 체크 (느린 시작 허용)
        startupProbe:
          httpGet:
            path: /
            port: 80
          periodSeconds: 5
          failureThreshold: 30    # 150초 동안 시작 허용
        
        # 생존 체크 (데드락 감지)
        livenessProbe:
          httpGet:
            path: /
            port: 80
          initialDelaySeconds: 0   # startupProbe 후 바로 시작
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        
        # 준비 체크 (트래픽 제어)
        readinessProbe:
          httpGet:
            path: /
            port: 80
          initialDelaySeconds: 0
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        
        resources:
          requests:
            memory: "64Mi"
            cpu: "100m"
          limits:
            memory: "128Mi"
            cpu: "200m"
EOF

kubectl get pods -l app=webapp
kubectl describe deployment webapp | grep -A 15 "Containers:"
```

---

## 📚 Part 3: 프로덕션 패턴 (45분)

### 토스플레이스 결제 서비스 예시

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: payment-api
  namespace: payment
spec:
  replicas: 5
  template:
    spec:
      containers:
      - name: payment
        image: payment:v1
        ports:
        - containerPort: 8080
        
        # 시작 Probe (Spring Boot 등 느린 시작)
        startupProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          periodSeconds: 10
          failureThreshold: 30    # 5분 동안 시작 허용
        
        # 생존 Probe (애플리케이션 행 감지)
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          periodSeconds: 15
          timeoutSeconds: 5
          failureThreshold: 3
        
        # 준비 Probe (의존성 체크 포함)
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
          successThreshold: 1
```

### 헬스체크 엔드포인트 설계

```
┌─────────────────────────────────────────────────────────────┐
│  헬스체크 엔드포인트 설계                                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  /health/live (livenessProbe용)                             │
│  ┌─────────────────────────────────────────────────┐       │
│  │ - 애플리케이션 프로세스가 응답 가능한가?        │       │
│  │ - 외부 의존성 체크 안 함 (빠른 응답)            │       │
│  │ - 실패 = 재시작 필요                            │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  /health/ready (readinessProbe용)                           │
│  ┌─────────────────────────────────────────────────┐       │
│  │ - 트래픽 처리할 준비가 되었는가?                │       │
│  │ - 외부 의존성 체크 (DB, Redis, Kafka 등)        │       │
│  │ - 실패 = 트래픽 차단 (재시작 아님)              │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
│  /health/startup (startupProbe용)                           │
│  ┌─────────────────────────────────────────────────┐       │
│  │ - 애플리케이션 초기화가 완료되었는가?           │       │
│  │ - 데이터 로딩, 캐시 워밍업 등 체크              │       │
│  │ - 성공 전까지 liveness/readiness 비활성화        │       │
│  └─────────────────────────────────────────────────┘       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | livenessProbe 설정 및 테스트 | ☐ |
| 2 | readinessProbe 설정 및 Endpoint 동작 확인 | ☐ |
| 3 | startupProbe 설정 | ☐ |
| 4 | Probe 실패 시 동작 이해 | ☐ |
| 5 | 실용적인 Deployment Probe 설정 | ☐ |

---

## 🔑 핵심 설정

```yaml
livenessProbe:
  httpGet:
    path: /health
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 10
  failureThreshold: 3
```

---

## 📝 면접 대비 질문

### Q1: livenessProbe와 readinessProbe의 차이점은?
> "livenessProbe는 컨테이너가 살아있는지 확인하고, 실패 시 재시작합니다. readinessProbe는 트래픽 받을 준비가 되었는지 확인하고, 실패 시 Service 엔드포인트에서 제외합니다. readiness 실패는 재시작하지 않아 일시적 과부하나 의존성 문제에 적합합니다."

### Q2: startupProbe는 언제 사용하나요?
> "시작이 느린 애플리케이션(Spring Boot, Java 등)에서 사용합니다. startupProbe 성공 전까지 livenessProbe와 readinessProbe가 비활성화되어, 긴 초기화 시간으로 인한 불필요한 재시작을 방지합니다."

---

## ➡️ 다음 학습: Day 44

**주제**: Week 6 복습
