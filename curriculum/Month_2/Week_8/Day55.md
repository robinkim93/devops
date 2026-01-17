# 📅 Day 55: Backend Deployment/Service 작성

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "Kubernetes 클러스터 운영/최적화"
> Backend 서비스의 Deployment, Service, ConfigMap 연동으로 실제 운영 환경 구축

Backend 애플리케이션 코드와 Kubernetes 매니페스트를 작성합니다. Flask + Redis 구조의 Backend를 구현하고, ConfigMap/Secret 연동, Resource Limits, Health Probe 설정까지 프로덕션 수준으로 구성합니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| Backend 앱 개발 | 1시간 | Flask 앱, API 구현 |
| Dockerfile 작성 | 30분 | 최적화된 이미지 빌드 |
| K8s 매니페스트 | 1.5시간 | Deployment, Service, ConfigMap |
| 테스트 및 검증 | 1시간 | 배포 및 동작 확인 |

---

## 📚 Part 1: Backend 아키텍처 이해 (20분)

### 1.1 3-Tier 아키텍처에서 Backend 역할

```
┌─────────────────────────────────────────────────────────────────────┐
│  3-Tier Application Architecture                                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                        Ingress                                │   │
│  │                    (외부 트래픽 진입)                         │   │
│  └─────────────────────────────┬────────────────────────────────┘   │
│                                │                                     │
│                                ▼                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                     Frontend (Nginx)                          │   │
│  │                   - 정적 파일 서빙                            │   │
│  │                   - 리버스 프록시 (/api → Backend)            │   │
│  └─────────────────────────────┬────────────────────────────────┘   │
│                                │                                     │
│                                ▼                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                    Backend (Flask)         ← 오늘 구현        │   │
│  │  ┌──────────────────────────────────────────────────────┐    │   │
│  │  │  역할:                                                │    │   │
│  │  │  • REST API 제공                                      │    │   │
│  │  │  • 비즈니스 로직 처리                                 │    │   │
│  │  │  • 데이터 캐싱 (Redis)                               │    │   │
│  │  │  • Health Check 엔드포인트                           │    │   │
│  │  └──────────────────────────────────────────────────────┘    │   │
│  │                                                               │   │
│  │  Kubernetes 리소스:                                           │   │
│  │  • Deployment (Replicas: 2)                                  │   │
│  │  • Service (ClusterIP)                                        │   │
│  │  • ConfigMap (환경 설정)                                      │   │
│  │  • Resource Limits                                            │   │
│  │  • Liveness/Readiness Probe                                   │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                │                                     │
│                                ▼                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                      Redis (Cache)                            │   │
│  │                    - 세션/카운터 저장                         │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: Backend 애플리케이션 개발 (1시간)

### 2.1 프로젝트 구조

```bash
mkdir -p ~/k8s-portfolio/app/backend
cd ~/k8s-portfolio

# 전체 구조
tree ~/k8s-portfolio
# k8s-portfolio/
# ├── app/
# │   ├── backend/
# │   │   ├── app.py
# │   │   ├── Dockerfile
# │   │   └── requirements.txt
# │   └── frontend/
# └── manifests/
#     ├── namespace.yaml
#     ├── configmap.yaml
#     ├── secret.yaml
#     ├── backend/
#     │   ├── deployment.yaml
#     │   └── service.yaml
#     └── redis/
```

### 2.2 Flask 애플리케이션

```python
# app/backend/app.py
from flask import Flask, jsonify, request
import redis
import os
import socket
import time
import logging

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = Flask(__name__)

# 환경 변수에서 설정 읽기 (ConfigMap에서 주입)
REDIS_HOST = os.environ.get('REDIS_HOST', 'redis')
REDIS_PORT = int(os.environ.get('REDIS_PORT', 6379))
APP_ENV = os.environ.get('APP_ENV', 'development')
LOG_LEVEL = os.environ.get('LOG_LEVEL', 'INFO')

# Redis 연결 (지연 초기화)
redis_client = None

def get_redis_client():
    """Redis 클라이언트 싱글톤"""
    global redis_client
    if redis_client is None:
        try:
            redis_client = redis.Redis(
                host=REDIS_HOST,
                port=REDIS_PORT,
                decode_responses=True,
                socket_connect_timeout=5
            )
            logger.info(f"Connected to Redis at {REDIS_HOST}:{REDIS_PORT}")
        except Exception as e:
            logger.error(f"Failed to connect to Redis: {e}")
    return redis_client


# === API 엔드포인트 ===

@app.route('/')
def home():
    """기본 정보 반환"""
    return jsonify({
        "message": "Hello from K8s Backend!",
        "hostname": socket.gethostname(),
        "environment": APP_ENV,
        "version": "v1.0.0"
    })


@app.route('/api/info')
def api_info():
    """서버 정보"""
    return jsonify({
        "hostname": socket.gethostname(),
        "environment": APP_ENV,
        "redis_host": REDIS_HOST,
        "timestamp": time.time()
    })


@app.route('/api/count')
def count():
    """방문 카운터 (Redis 사용)"""
    try:
        client = get_redis_client()
        count = client.incr('visit_count')
        return jsonify({
            "visits": count,
            "hostname": socket.gethostname()
        })
    except Exception as e:
        logger.error(f"Count error: {e}")
        return jsonify({
            "error": "Redis connection failed",
            "hostname": socket.gethostname()
        }), 500


@app.route('/api/data', methods=['GET', 'POST'])
def data():
    """간단한 데이터 저장/조회"""
    client = get_redis_client()
    
    if request.method == 'POST':
        try:
            json_data = request.get_json()
            key = json_data.get('key')
            value = json_data.get('value')
            
            if not key or not value:
                return jsonify({"error": "key and value required"}), 400
            
            client.set(f"data:{key}", value)
            return jsonify({
                "message": "Data saved",
                "key": key
            }), 201
        except Exception as e:
            return jsonify({"error": str(e)}), 500
    
    else:  # GET
        key = request.args.get('key')
        if not key:
            return jsonify({"error": "key parameter required"}), 400
        
        value = client.get(f"data:{key}")
        if value:
            return jsonify({"key": key, "value": value})
        else:
            return jsonify({"error": "Key not found"}), 404


# === Health Check 엔드포인트 ===

@app.route('/health')
def health():
    """
    Liveness Probe용
    - 앱 자체의 동작 여부만 확인
    - Redis 실패해도 앱은 살아있음
    """
    return jsonify({
        "status": "healthy",
        "hostname": socket.gethostname()
    })


@app.route('/ready')
def ready():
    """
    Readiness Probe용
    - 트래픽을 받을 준비가 되었는지 확인
    - Redis 연결까지 확인
    """
    try:
        client = get_redis_client()
        client.ping()
        return jsonify({
            "status": "ready",
            "redis": "connected"
        })
    except Exception as e:
        logger.warning(f"Readiness check failed: {e}")
        return jsonify({
            "status": "not ready",
            "error": str(e)
        }), 503


@app.route('/startup')
def startup():
    """
    Startup Probe용
    - 앱 시작 완료 확인
    """
    return jsonify({"status": "started"})


# === 에러 핸들러 ===

@app.errorhandler(404)
def not_found(error):
    return jsonify({"error": "Not found"}), 404


@app.errorhandler(500)
def internal_error(error):
    logger.error(f"Internal error: {error}")
    return jsonify({"error": "Internal server error"}), 500


# === 메인 ===

if __name__ == '__main__':
    logger.info(f"Starting Backend in {APP_ENV} environment")
    logger.info(f"Redis: {REDIS_HOST}:{REDIS_PORT}")
    
    # 개발 모드에서만 debug=True
    debug = APP_ENV == 'development'
    app.run(host='0.0.0.0', port=5000, debug=debug)
```

### 2.3 Requirements

```bash
# app/backend/requirements.txt
cat << 'EOF' > app/backend/requirements.txt
flask==3.0.0
redis==5.0.1
gunicorn==21.2.0
EOF
```

### 2.4 Dockerfile (최적화)

```dockerfile
# app/backend/Dockerfile

# === 빌드 스테이지 ===
FROM python:3.11-slim as builder

WORKDIR /app

# 의존성 파일 복사
COPY requirements.txt .

# 가상환경 생성 및 의존성 설치
RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"
RUN pip install --no-cache-dir -r requirements.txt

# === 런타임 스테이지 ===
FROM python:3.11-slim

# 보안: non-root 사용자
RUN useradd -m -u 1000 appuser

WORKDIR /app

# 빌드 스테이지에서 가상환경 복사
COPY --from=builder /opt/venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# 애플리케이션 코드 복사
COPY app.py .

# 소유권 변경
RUN chown -R appuser:appuser /app

# non-root로 실행
USER appuser

# 포트 노출
EXPOSE 5000

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:5000/health')"

# Gunicorn으로 프로덕션 실행
# workers: CPU 코어 수 * 2 + 1 권장 (컨테이너에서는 2 정도)
CMD ["gunicorn", "--bind", "0.0.0.0:5000", "--workers", "2", "--threads", "2", \
     "--worker-class", "gthread", "--access-logfile", "-", "--error-logfile", "-", \
     "app:app"]
```

### 2.5 이미지 빌드 및 테스트

```bash
cd ~/k8s-portfolio/app/backend

# 이미지 빌드
docker build -t backend:v1 .

# 로컬 테스트 (Redis 없이)
docker run -d --name backend-test -p 5000:5000 \
  -e REDIS_HOST=host.docker.internal \
  backend:v1

# API 테스트
curl http://localhost:5000/
curl http://localhost:5000/health
curl http://localhost:5000/api/info

# 정리
docker rm -f backend-test

# Minikube에 이미지 로드 (Minikube 사용 시)
# minikube image load backend:v1
```

---

## 🛠️ Part 3: Kubernetes 매니페스트 작성 (1.5시간)

### 3.1 ConfigMap

```yaml
# manifests/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: k8s-portfolio
  labels:
    app: k8s-portfolio
data:
  # Backend 설정
  REDIS_HOST: "redis"
  REDIS_PORT: "6379"
  APP_ENV: "production"
  LOG_LEVEL: "INFO"
  
  # Frontend 설정 (나중에 사용)
  BACKEND_URL: "http://backend:80"
```

### 3.2 Secret

```yaml
# manifests/secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secret
  namespace: k8s-portfolio
  labels:
    app: k8s-portfolio
type: Opaque
data:
  # base64 인코딩된 값
  # echo -n "redis-password" | base64
  REDIS_PASSWORD: cmVkaXMtcGFzc3dvcmQ=
  # echo -n "my-api-key-12345" | base64
  API_KEY: bXktYXBpLWtleS0xMjM0NQ==
```

### 3.3 Backend Deployment

```yaml
# manifests/backend/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  namespace: k8s-portfolio
  labels:
    app: backend
    tier: backend
spec:
  replicas: 2
  selector:
    matchLabels:
      app: backend
  
  # 롤링 업데이트 전략
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1        # 최대 1개 추가 Pod
      maxUnavailable: 0  # 항상 최소 replicas 유지
  
  template:
    metadata:
      labels:
        app: backend
        tier: backend
      annotations:
        # ConfigMap 변경 시 재배포 트리거
        checksum/config: "{{ include (print $.Template.BasePath \"/configmap.yaml\") . | sha256sum }}"
    spec:
      # Graceful Shutdown
      terminationGracePeriodSeconds: 30
      
      containers:
      - name: backend
        image: backend:v1
        imagePullPolicy: IfNotPresent
        
        # 포트
        ports:
        - name: http
          containerPort: 5000
          protocol: TCP
        
        # 환경 변수 - ConfigMap에서
        envFrom:
        - configMapRef:
            name: app-config
        
        # 환경 변수 - Secret에서
        env:
        - name: REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: app-secret
              key: REDIS_PASSWORD
              optional: true
        
        # 리소스 제한
        resources:
          requests:
            cpu: "100m"
            memory: "128Mi"
          limits:
            cpu: "500m"
            memory: "512Mi"
        
        # Liveness Probe - 앱이 살아있는지
        livenessProbe:
          httpGet:
            path: /health
            port: http
          initialDelaySeconds: 10
          periodSeconds: 10
          timeoutSeconds: 3
          failureThreshold: 3
          successThreshold: 1
        
        # Readiness Probe - 트래픽 받을 준비
        readinessProbe:
          httpGet:
            path: /ready
            port: http
          initialDelaySeconds: 5
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
          successThreshold: 1
        
        # Startup Probe - 시작 완료 확인 (느린 시작 앱용)
        startupProbe:
          httpGet:
            path: /startup
            port: http
          initialDelaySeconds: 0
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 30  # 최대 150초 대기
          successThreshold: 1
        
        # 보안 컨텍스트
        securityContext:
          runAsNonRoot: true
          runAsUser: 1000
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: true
          capabilities:
            drop:
              - ALL
        
        # 볼륨 마운트 (필요시)
        volumeMounts:
        - name: tmp
          mountPath: /tmp
      
      # 볼륨
      volumes:
      - name: tmp
        emptyDir: {}
      
      # Pod 반-어피니티 (같은 노드에 여러 Pod 배치 방지)
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - backend
              topologyKey: kubernetes.io/hostname
```

### 3.4 Backend Service

```yaml
# manifests/backend/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: backend
  namespace: k8s-portfolio
  labels:
    app: backend
    tier: backend
spec:
  type: ClusterIP
  selector:
    app: backend
  ports:
  - name: http
    port: 80           # Service 포트 (다른 Pod에서 접근)
    targetPort: http   # 컨테이너 포트 (5000)
    protocol: TCP
```

### 3.5 Redis Deployment/Service (Backend 의존성)

```yaml
# manifests/redis/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: k8s-portfolio
  labels:
    app: redis
    tier: cache
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
        tier: cache
    spec:
      containers:
      - name: redis
        image: redis:7-alpine
        ports:
        - name: redis
          containerPort: 6379
        resources:
          requests:
            cpu: "50m"
            memory: "64Mi"
          limits:
            cpu: "200m"
            memory: "256Mi"
        livenessProbe:
          exec:
            command: ["redis-cli", "ping"]
          initialDelaySeconds: 10
          periodSeconds: 10
        readinessProbe:
          exec:
            command: ["redis-cli", "ping"]
          initialDelaySeconds: 5
          periodSeconds: 5
---
# manifests/redis/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: redis
  namespace: k8s-portfolio
  labels:
    app: redis
    tier: cache
spec:
  type: ClusterIP
  selector:
    app: redis
  ports:
  - name: redis
    port: 6379
    targetPort: redis
```

---

## 🛠️ Part 4: 배포 및 테스트 (1시간)

### 4.1 순서대로 배포

```bash
cd ~/k8s-portfolio

# 1. Namespace 생성 (Day 54에서 했다면 스킵)
kubectl apply -f manifests/namespace.yaml

# 2. ConfigMap & Secret
kubectl apply -f manifests/configmap.yaml
kubectl apply -f manifests/secret.yaml

# 3. Redis 배포 (Backend 의존성)
kubectl apply -f manifests/redis/

# 4. Backend 배포
kubectl apply -f manifests/backend/

# 5. 상태 확인
echo "=== Pod 상태 ==="
kubectl get pods -n k8s-portfolio -w

# 6. 모든 리소스 확인
kubectl get all -n k8s-portfolio
```

### 4.2 배포 검증

```bash
# Pod 상태 상세
kubectl describe pod -l app=backend -n k8s-portfolio

# 로그 확인
kubectl logs -l app=backend -n k8s-portfolio --tail=50

# ConfigMap 확인
kubectl get configmap app-config -n k8s-portfolio -o yaml

# Endpoint 확인 (Service가 Pod와 연결됨)
kubectl get endpoints backend -n k8s-portfolio
```

### 4.3 API 테스트

```bash
# 임시 테스트 Pod에서 API 호출
kubectl run test-client --image=curlimages/curl --rm -it --restart=Never \
  -n k8s-portfolio -- sh

# Pod 내부에서:
curl http://backend/
curl http://backend/health
curl http://backend/ready
curl http://backend/api/info
curl http://backend/api/count

# 데이터 저장/조회 테스트
curl -X POST http://backend/api/data \
  -H "Content-Type: application/json" \
  -d '{"key": "test", "value": "hello k8s"}'

curl "http://backend/api/data?key=test"

exit
```

### 4.4 Port Forward로 로컬 테스트

```bash
# Backend Service로 포트 포워딩
kubectl port-forward svc/backend 8080:80 -n k8s-portfolio &

# 로컬에서 테스트
curl http://localhost:8080/
curl http://localhost:8080/api/count

# 종료
kill %1
```

---

## 📊 Part 5: 배포 전략 이해

### 5.1 Rolling Update 동작

```
┌─────────────────────────────────────────────────────────────────────┐
│  Rolling Update (maxSurge: 1, maxUnavailable: 0)                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Step 1: 초기 상태 (replicas: 2)                                    │
│  ┌─────────────┐  ┌─────────────┐                                   │
│  │  backend-1  │  │  backend-2  │                                   │
│  │    v1       │  │    v1       │                                   │
│  └─────────────┘  └─────────────┘                                   │
│                                                                      │
│  Step 2: 새 버전 Pod 추가 (maxSurge: 1)                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                  │
│  │  backend-1  │  │  backend-2  │  │  backend-3  │                  │
│  │    v1       │  │    v1       │  │    v2       │                  │
│  └─────────────┘  └─────────────┘  └─────────────┘                  │
│                                             ↑ Ready 대기             │
│                                                                      │
│  Step 3: v2 Ready → v1 하나 제거                                    │
│  ┌─────────────┐  ┌─────────────┐                                   │
│  │  backend-2  │  │  backend-3  │                                   │
│  │    v1       │  │    v2       │                                   │
│  └─────────────┘  └─────────────┘                                   │
│                                                                      │
│  Step 4: 반복                                                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                  │
│  │  backend-2  │  │  backend-3  │  │  backend-4  │                  │
│  │    v1       │  │    v2       │  │    v2       │                  │
│  └─────────────┘  └─────────────┘  └─────────────┘                  │
│                                                                      │
│  Step 5: 완료                                                        │
│  ┌─────────────┐  ┌─────────────┐                                   │
│  │  backend-3  │  │  backend-4  │                                   │
│  │    v2       │  │    v2       │                                   │
│  └─────────────┘  └─────────────┘                                   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 5.2 업데이트 실습

```bash
# 이미지 업데이트 (Rolling Update)
kubectl set image deployment/backend backend=backend:v2 -n k8s-portfolio

# 롤아웃 상태 확인
kubectl rollout status deployment/backend -n k8s-portfolio

# 롤아웃 기록
kubectl rollout history deployment/backend -n k8s-portfolio

# 롤백 (문제 발생 시)
kubectl rollout undo deployment/backend -n k8s-portfolio
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | Flask 앱 작성 | API, Health Check | ☐ |
| 2 | Dockerfile 최적화 | Multi-stage, Non-root | ☐ |
| 3 | ConfigMap 작성 | 환경 설정 분리 | ☐ |
| 4 | Secret 작성 | 민감 정보 분리 | ☐ |
| 5 | Deployment 작성 | Resource, Probe, Security | ☐ |
| 6 | Service 작성 | ClusterIP | ☐ |
| 7 | Redis 배포 | Backend 의존성 | ☐ |
| 8 | API 테스트 | curl 검증 | ☐ |

---

## 🔑 오늘 배운 핵심 포인트

```yaml
# Deployment 핵심 설정
spec:
  replicas: 2
  strategy:
    type: RollingUpdate
  template:
    spec:
      containers:
      - name: backend
        resources:
          requests: {cpu: 100m, memory: 128Mi}
          limits: {cpu: 500m, memory: 512Mi}
        livenessProbe: {httpGet: {path: /health}}
        readinessProbe: {httpGet: {path: /ready}}
        envFrom:
        - configMapRef: {name: app-config}
```

---

## 💡 면접 대비 핵심 포인트

### Q1: Liveness와 Readiness Probe의 차이는?

**A**: "Liveness Probe는 컨테이너가 살아있는지 확인하여 실패 시 재시작합니다. Readiness Probe는 트래픽을 받을 준비가 되었는지 확인하여 실패 시 Service Endpoint에서 제외합니다."

### Q2: ConfigMap과 Secret의 차이는?

**A**: "ConfigMap은 일반 설정을, Secret은 민감한 정보를 저장합니다. Secret은 base64 인코딩되며, RBAC으로 접근 제어가 가능합니다."

---

## ➡️ 다음 학습: Day 56

**주제**: Frontend, Redis 완성 및 Ingress 설정
