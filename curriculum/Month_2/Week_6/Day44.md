# 📅 Day 44: Week 6 복습

## 🎯 오늘의 목표

> **토스플레이스 연결점**: Kubernetes 핵심 기능 종합 복습
> Week 6 (ConfigMap, Secret, Volume, Ingress, 리소스) 종합 복습

토스플레이스에서 요구하는 "Kubernetes 기반의 인프라 운영 경험"을 위해 Week 6에서 학습한 내용을 체계적으로 정리합니다.

---

## ⏰ 예상 학습 시간: 4시간

---

## 📋 Part 1: Week 6 핵심 개념 정리 (1시간)

### 1.1 학습 내용 요약

| Day | 주제 | 핵심 내용 | 토스플레이스 연관성 |
|-----|------|----------|-------------------|
| 38 | ConfigMap | 환경 변수, 파일 마운트 | 환경별 설정 분리 |
| 39 | Secret | 민감 정보 관리 | 보안 컴플라이언스 |
| 40 | PV/PVC | 영속적 스토리지 | 데이터 보존 |
| 41 | Ingress | L7 라우팅 | 외부 트래픽 관리 |
| 42 | 리소스 | requests/limits | 리소스 효율 최적화 |
| 43 | Probe | 헬스체크 | 서비스 안정성 |

### 1.2 ConfigMap 복습

```
┌─────────────────────────────────────────────────────────────────────┐
│  ConfigMap 개념                                                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ConfigMap = 애플리케이션 설정을 코드와 분리                        │
│                                                                      │
│  ┌──────────────────┐                                               │
│  │   ConfigMap      │                                               │
│  │  ┌────────────┐  │      ┌─────────────────────────────┐         │
│  │  │ APP_ENV    │──┼───▶  │  Pod                        │         │
│  │  │ LOG_LEVEL  │  │      │  환경 변수로 주입           │         │
│  │  │ config.json│  │      │  또는 파일로 마운트         │         │
│  │  └────────────┘  │      └─────────────────────────────┘         │
│  └──────────────────┘                                               │
│                                                                      │
│  사용 방식:                                                         │
│  1. envFrom: 모든 키를 환경 변수로                                  │
│  2. env.valueFrom: 특정 키만 환경 변수로                            │
│  3. volumes: 파일로 마운트                                          │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

```yaml
# ConfigMap 생성 예시
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
data:
  # 단순 키-값
  APP_ENV: "production"
  LOG_LEVEL: "info"
  # 파일 내용
  config.json: |
    {
      "database": {
        "host": "db.example.com",
        "port": 3306
      }
    }
```

### 1.3 Secret 복습

```
┌─────────────────────────────────────────────────────────────────────┐
│  Secret 개념                                                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Secret = 민감한 정보 관리 (비밀번호, API 키, 인증서 등)            │
│                                                                      │
│  ⚠️ 주의사항:                                                       │
│  - 기본적으로 Base64 인코딩 (암호화 아님!)                          │
│  - etcd 암호화 활성화 권장                                          │
│  - RBAC으로 접근 제어                                               │
│  - 외부 Secret Manager 연동 권장 (Vault, AWS Secrets Manager)       │
│                                                                      │
│  Secret 타입:                                                       │
│  - Opaque: 임의의 데이터 (기본)                                     │
│  - kubernetes.io/tls: TLS 인증서                                    │
│  - kubernetes.io/dockerconfigjson: 레지스트리 인증                  │
│  - kubernetes.io/service-account-token: SA 토큰                     │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

```yaml
# Secret 생성 예시
apiVersion: v1
kind: Secret
metadata:
  name: app-secret
type: Opaque
data:
  # Base64 인코딩된 값
  DB_PASSWORD: cGFzc3dvcmQxMjM=  # password123
  API_KEY: YXBpLWtleS12YWx1ZQ==   # api-key-value
---
# stringData 사용 (자동 Base64 인코딩)
apiVersion: v1
kind: Secret
metadata:
  name: app-secret-string
type: Opaque
stringData:
  DB_PASSWORD: "password123"
  API_KEY: "api-key-value"
```

### 1.4 PV/PVC 복습

```
┌─────────────────────────────────────────────────────────────────────┐
│  PersistentVolume & PersistentVolumeClaim                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────────┐     ┌─────────────────┐     ┌─────────────────┐  │
│  │     Pod      │ ──▶ │       PVC       │ ──▶ │       PV        │  │
│  │  (Consumer)  │     │  (요청/클레임)  │     │  (실제 스토리지)│  │
│  └──────────────┘     └─────────────────┘     └─────────────────┘  │
│                                                                      │
│  관계:                                                              │
│  - PV: 관리자가 프로비저닝한 스토리지                               │
│  - PVC: 사용자(Pod)의 스토리지 요청                                 │
│  - StorageClass: 동적 프로비저닝 정의                               │
│                                                                      │
│  Access Modes:                                                      │
│  - ReadWriteOnce (RWO): 단일 노드에서 읽기/쓰기                     │
│  - ReadOnlyMany (ROX): 여러 노드에서 읽기만                         │
│  - ReadWriteMany (RWX): 여러 노드에서 읽기/쓰기                     │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.5 Ingress 복습

```
┌─────────────────────────────────────────────────────────────────────┐
│  Ingress 개념                                                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  외부 트래픽 → Ingress Controller → Service → Pod                   │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Ingress Controller (nginx-ingress)                         │    │
│  │                                                             │    │
│  │  api.example.com/users ──▶ users-svc                       │    │
│  │  api.example.com/orders ──▶ orders-svc                     │    │
│  │  web.example.com ──▶ web-svc                               │    │
│  │                                                             │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  주요 기능:                                                         │
│  - 호스트 기반 라우팅 (Host-based)                                  │
│  - 경로 기반 라우팅 (Path-based)                                    │
│  - TLS 종료                                                         │
│  - URL Rewrite                                                      │
│  - Rate Limiting                                                    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.6 리소스 관리 복습

```
┌─────────────────────────────────────────────────────────────────────┐
│  리소스 관리 (requests/limits)                                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  resources:                                                         │
│    requests:          # 최소 보장 리소스 (스케줄링 기준)            │
│      memory: "128Mi"                                                │
│      cpu: "100m"      # 0.1 코어                                    │
│    limits:            # 최대 사용 가능 리소스                       │
│      memory: "256Mi"  # 초과 시 OOMKilled                           │
│      cpu: "200m"      # 초과 시 throttling                          │
│                                                                      │
│  QoS 클래스:                                                        │
│  - Guaranteed: requests = limits (우선순위 최고)                    │
│  - Burstable: requests < limits                                     │
│  - BestEffort: 설정 없음 (먼저 종료됨)                              │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.7 Probe 복습

```
┌─────────────────────────────────────────────────────────────────────┐
│  Health Probes                                                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  livenessProbe: 컨테이너가 살아있는지 확인                          │
│  → 실패 시: 컨테이너 재시작                                         │
│                                                                      │
│  readinessProbe: 트래픽을 받을 준비가 됐는지 확인                   │
│  → 실패 시: Service 엔드포인트에서 제외                             │
│                                                                      │
│  startupProbe: 컨테이너가 시작됐는지 확인                           │
│  → 느린 시작 앱용, 성공할 때까지 다른 Probe 비활성화                │
│                                                                      │
│  Probe 방법:                                                        │
│  - httpGet: HTTP 요청 (200-399 성공)                                │
│  - tcpSocket: TCP 연결 확인                                         │
│  - exec: 컨테이너 내 명령어 실행 (exit 0 성공)                      │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: 종합 실습 - 완전한 앱 배포 (2시간)

### 실습 1: 프로젝트 구조 생성

```bash
# 디렉토리 생성
mkdir -p ~/week6-review
cd ~/week6-review
```

### 실습 2: 네임스페이스

```yaml
# 01-namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: week6-app
  labels:
    env: review
```

### 실습 3: ConfigMap

```yaml
# 02-configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: week6-app
data:
  # 환경 변수
  APP_ENV: "production"
  LOG_LEVEL: "info"
  SERVER_PORT: "8080"
  
  # Nginx 설정 파일
  nginx.conf: |
    server {
        listen 80;
        server_name localhost;
        
        location / {
            root /usr/share/nginx/html;
            index index.html;
        }
        
        location /health {
            access_log off;
            return 200 "healthy\n";
            add_header Content-Type text/plain;
        }
        
        location /api/ {
            proxy_pass http://backend:8080/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }
```

### 실습 4: Secret

```yaml
# 03-secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secret
  namespace: week6-app
type: Opaque
stringData:
  DB_PASSWORD: "SuperSecretPassword123!"
  API_KEY: "sk-tossplace-api-key-2024"
  REDIS_PASSWORD: "RedisSecretPass!"
```

### 실습 5: PersistentVolumeClaim

```yaml
# 04-pvc.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: app-data-pvc
  namespace: week6-app
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi
  # StorageClass 지정 (없으면 기본값)
  # storageClassName: standard
```

### 실습 6: Deployment (모든 기능 통합)

```yaml
# 05-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: complete-app
  namespace: week6-app
  labels:
    app: complete-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: complete-app
  template:
    metadata:
      labels:
        app: complete-app
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
    spec:
      # 서비스 계정 (선택)
      # serviceAccountName: app-sa
      
      # 보안 컨텍스트
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
      
      containers:
      - name: app
        image: nginx:1.25
        ports:
        - containerPort: 80
          name: http
        
        # ConfigMap에서 환경 변수 로드
        envFrom:
        - configMapRef:
            name: app-config
        
        # Secret에서 특정 값만 로드
        env:
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: app-secret
              key: DB_PASSWORD
        - name: API_KEY
          valueFrom:
            secretKeyRef:
              name: app-secret
              key: API_KEY
        
        # 리소스 제한
        resources:
          requests:
            memory: "64Mi"
            cpu: "100m"
          limits:
            memory: "128Mi"
            cpu: "200m"
        
        # Liveness Probe
        livenessProbe:
          httpGet:
            path: /health
            port: 80
          initialDelaySeconds: 15
          periodSeconds: 10
          failureThreshold: 3
          timeoutSeconds: 5
        
        # Readiness Probe
        readinessProbe:
          httpGet:
            path: /health
            port: 80
          initialDelaySeconds: 5
          periodSeconds: 5
          failureThreshold: 3
          timeoutSeconds: 3
        
        # Volume 마운트
        volumeMounts:
        - name: config-volume
          mountPath: /etc/nginx/conf.d/
          readOnly: true
        - name: data-volume
          mountPath: /data
        - name: tmp-volume
          mountPath: /tmp
        
        # 보안 컨텍스트 (컨테이너)
        securityContext:
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: true
          capabilities:
            drop:
              - ALL
      
      # Volumes 정의
      volumes:
      - name: config-volume
        configMap:
          name: app-config
          items:
          - key: nginx.conf
            path: default.conf
      - name: data-volume
        persistentVolumeClaim:
          claimName: app-data-pvc
      - name: tmp-volume
        emptyDir: {}
```

### 실습 7: Service

```yaml
# 06-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: complete-app-svc
  namespace: week6-app
  labels:
    app: complete-app
spec:
  selector:
    app: complete-app
  ports:
  - name: http
    port: 80
    targetPort: 80
  type: ClusterIP
```

### 실습 8: Ingress

```yaml
# 07-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: complete-app-ingress
  namespace: week6-app
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "false"
spec:
  ingressClassName: nginx
  rules:
  - host: complete.local
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: complete-app-svc
            port:
              number: 80
```

### 실습 9: HPA

```yaml
# 08-hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: complete-app-hpa
  namespace: week6-app
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: complete-app
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### 실습 10: 배포 및 확인

```bash
# 모든 리소스 배포
kubectl apply -f 01-namespace.yaml
kubectl apply -f 02-configmap.yaml
kubectl apply -f 03-secret.yaml
kubectl apply -f 04-pvc.yaml
kubectl apply -f 05-deployment.yaml
kubectl apply -f 06-service.yaml
kubectl apply -f 07-ingress.yaml
kubectl apply -f 08-hpa.yaml

# 또는 한 번에
kubectl apply -f .

# 상태 확인
kubectl get all -n week6-app

# Pod 상세 확인
kubectl describe pod -n week6-app -l app=complete-app

# 환경 변수 확인
kubectl exec -n week6-app -it deploy/complete-app -- env | grep -E "APP_|DB_|API_"

# ConfigMap 마운트 확인
kubectl exec -n week6-app -it deploy/complete-app -- cat /etc/nginx/conf.d/default.conf

# PVC 마운트 확인
kubectl exec -n week6-app -it deploy/complete-app -- df -h /data

# 접속 테스트
kubectl port-forward -n week6-app svc/complete-app-svc 8080:80
# 다른 터미널에서: curl localhost:8080/health
```

---

## 📊 Part 3: 트러블슈팅 연습 (30분)

### 일반적인 문제와 해결

```bash
# Pod가 Pending 상태
kubectl describe pod <pod-name> -n week6-app
# 원인: 리소스 부족, PVC 바인딩 실패, Node 문제

# Pod가 CrashLoopBackOff
kubectl logs <pod-name> -n week6-app
kubectl logs <pod-name> -n week6-app --previous
# 원인: 애플리케이션 에러, Probe 실패

# Probe 실패
kubectl describe pod <pod-name> -n week6-app | grep -A5 "Events"
# 원인: 잘못된 경로, 포트, 타임아웃

# ConfigMap/Secret 마운트 실패
kubectl get cm,secret -n week6-app
kubectl describe pod <pod-name> -n week6-app | grep -A10 "Volumes"
```

---

## ✅ Week 6 종합 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | ConfigMap으로 설정 관리 | ☐ |
| 2 | Secret으로 민감 정보 관리 | ☐ |
| 3 | PV/PVC로 영속 스토리지 사용 | ☐ |
| 4 | Ingress로 외부 트래픽 라우팅 | ☐ |
| 5 | resources로 리소스 제한 | ☐ |
| 6 | Probe로 헬스체크 설정 | ☐ |
| 7 | HPA로 자동 스케일링 | ☐ |
| 8 | 통합 앱 배포 완료 | ☐ |

---

## 🔑 핵심 명령어 요약

```bash
# ConfigMap/Secret
kubectl create configmap <name> --from-literal=key=value
kubectl create secret generic <name> --from-literal=key=value
kubectl get cm,secret -n <namespace>

# PV/PVC
kubectl get pv,pvc -n <namespace>
kubectl describe pvc <name> -n <namespace>

# Ingress
kubectl get ingress -n <namespace>
kubectl describe ingress <name> -n <namespace>

# 리소스 확인
kubectl top pods -n <namespace>
kubectl describe pod <name> | grep -A10 "Resources"

# HPA
kubectl get hpa -n <namespace>
kubectl describe hpa <name> -n <namespace>
```

---

## ➡️ 다음: Week 7 (Day 45-51)

**주제**: HPA, 로깅, 모니터링
- Day 45: HPA (Horizontal Pod Autoscaler)
- Day 46: Kubernetes 로깅
- Day 47: Prometheus 기초
