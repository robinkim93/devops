# 📅 Day 56: Frontend, Redis 배포 및 통합

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "Kubernetes 클러스터 운영/최적화"
> 3-Tier 아키텍처의 Frontend와 Redis를 배포하고 전체 시스템 통합

Frontend(Nginx)와 Redis의 Kubernetes 매니페스트를 작성하고 배포합니다. ConfigMap으로 Nginx 설정을 관리하고, PVC로 Redis 데이터를 영속화합니다. 전체 3-Tier 앱의 통합 테스트까지 수행합니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| Frontend 구현 | 1.5시간 | Nginx, ConfigMap, Deployment |
| Redis 구현 | 1시간 | StatefulSet, PVC |
| 통합 배포 | 1시간 | 전체 앱 배포 및 테스트 |
| 정리 | 30분 | 문제 해결, 문서화 |

---

## 📚 Part 1: 3-Tier 아키텍처 복습 (15분)

### 1.1 전체 구조

```
┌─────────────────────────────────────────────────────────────────────┐
│  3-Tier Application on Kubernetes                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  외부 사용자                                                        │
│       │                                                              │
│       ▼                                                              │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                        Ingress                                │   │
│  │               (외부 트래픽 진입점, HTTPS)                     │   │
│  └───────────────────────────┬──────────────────────────────────┘   │
│                              │                                       │
│                              ▼                                       │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                    Frontend (Nginx)         ← 오늘 구현       │   │
│  │  ┌────────────────────────────────────────────────────────┐  │   │
│  │  │  역할:                                                  │  │   │
│  │  │  • 정적 파일 서빙 (HTML, CSS, JS)                      │  │   │
│  │  │  • /api/* → Backend 프록시                             │  │   │
│  │  │  • 캐싱 및 압축                                         │  │   │
│  │  │                                                         │  │   │
│  │  │  K8s 리소스: Deployment, Service, ConfigMap             │  │   │
│  │  └────────────────────────────────────────────────────────┘  │   │
│  └───────────────────────────┬──────────────────────────────────┘   │
│                              │                                       │
│                              ▼                                       │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                    Backend (Flask)          (Day 55 완료)     │   │
│  │                    • REST API 제공                            │   │
│  │                    • Redis 연동                               │   │
│  └───────────────────────────┬──────────────────────────────────┘   │
│                              │                                       │
│                              ▼                                       │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                    Redis (Cache)            ← 오늘 구현       │   │
│  │  ┌────────────────────────────────────────────────────────┐  │   │
│  │  │  역할:                                                  │  │   │
│  │  │  • 세션/카운터 저장                                     │  │   │
│  │  │  • 캐싱                                                 │  │   │
│  │  │                                                         │  │   │
│  │  │  K8s 리소스: StatefulSet, Service, PVC                  │  │   │
│  │  └────────────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: Frontend (Nginx) 구현 (1.5시간)

### 2.1 Nginx 설정 ConfigMap

```yaml
# manifests/frontend/nginx-configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: nginx-config
  namespace: k8s-portfolio
data:
  default.conf: |
    upstream backend {
        server backend:80;
    }
    
    server {
        listen 80;
        server_name _;
        
        # 접근 로그
        access_log /var/log/nginx/access.log;
        error_log /var/log/nginx/error.log;
        
        # Gzip 압축
        gzip on;
        gzip_types text/plain application/json application/javascript text/css;
        gzip_min_length 1000;
        
        # 정적 파일
        location / {
            root /usr/share/nginx/html;
            index index.html;
            try_files $uri $uri/ /index.html;
        }
        
        # API 프록시 (Backend로)
        location /api/ {
            proxy_pass http://backend/api/;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            
            # 타임아웃 설정
            proxy_connect_timeout 10s;
            proxy_read_timeout 30s;
            proxy_send_timeout 30s;
        }
        
        # 헬스체크 엔드포인트
        location /health {
            access_log off;
            return 200 "healthy\n";
            add_header Content-Type text/plain;
        }
        
        # Backend 헬스체크 프록시
        location /api/health {
            proxy_pass http://backend/health;
            proxy_http_version 1.1;
        }
        
        # 정적 파일 캐싱
        location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
            root /usr/share/nginx/html;
            expires 7d;
            add_header Cache-Control "public, immutable";
        }
        
        # 에러 페이지
        error_page 500 502 503 504 /50x.html;
        location = /50x.html {
            root /usr/share/nginx/html;
        }
    }
```

### 2.2 간단한 HTML 페이지 ConfigMap

```yaml
# manifests/frontend/html-configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: frontend-html
  namespace: k8s-portfolio
data:
  index.html: |
    <!DOCTYPE html>
    <html lang="ko">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>K8s Portfolio App</title>
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                min-height: 100vh;
                display: flex;
                justify-content: center;
                align-items: center;
            }
            .container {
                background: white;
                border-radius: 16px;
                padding: 40px;
                box-shadow: 0 20px 60px rgba(0,0,0,0.3);
                text-align: center;
                max-width: 500px;
            }
            h1 { color: #333; margin-bottom: 20px; }
            .info { margin: 20px 0; padding: 15px; background: #f5f5f5; border-radius: 8px; }
            .counter { font-size: 48px; font-weight: bold; color: #667eea; margin: 20px 0; }
            button {
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
                border: none;
                padding: 12px 30px;
                border-radius: 8px;
                font-size: 16px;
                cursor: pointer;
                transition: transform 0.2s;
            }
            button:hover { transform: scale(1.05); }
            .status { margin-top: 20px; font-size: 14px; color: #666; }
        </style>
    </head>
    <body>
        <div class="container">
            <h1>🚀 K8s Portfolio App</h1>
            <div class="info">
                <p>Backend: <span id="hostname">Loading...</span></p>
                <p>Environment: <span id="env">Loading...</span></p>
            </div>
            <div class="counter" id="counter">0</div>
            <button onclick="incrementCounter()">Count +1</button>
            <div class="status" id="status"></div>
        </div>
        <script>
            async function fetchInfo() {
                try {
                    const res = await fetch('/api/info');
                    const data = await res.json();
                    document.getElementById('hostname').textContent = data.hostname;
                    document.getElementById('env').textContent = data.environment;
                } catch (e) {
                    document.getElementById('hostname').textContent = 'Error';
                }
            }
            
            async function incrementCounter() {
                try {
                    const res = await fetch('/api/count');
                    const data = await res.json();
                    document.getElementById('counter').textContent = data.visits;
                    document.getElementById('status').textContent = 
                        'Served by: ' + data.hostname;
                } catch (e) {
                    document.getElementById('status').textContent = 'Error: ' + e.message;
                }
            }
            
            fetchInfo();
            incrementCounter();
        </script>
    </body>
    </html>
```

### 2.3 Frontend Deployment

```yaml
# manifests/frontend/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
  namespace: k8s-portfolio
  labels:
    app: frontend
    tier: frontend
spec:
  replicas: 2
  selector:
    matchLabels:
      app: frontend
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    metadata:
      labels:
        app: frontend
        tier: frontend
    spec:
      containers:
      - name: nginx
        image: nginx:1.24-alpine
        ports:
        - name: http
          containerPort: 80
        
        # ConfigMap 마운트
        volumeMounts:
        - name: nginx-config
          mountPath: /etc/nginx/conf.d
          readOnly: true
        - name: html-content
          mountPath: /usr/share/nginx/html
          readOnly: true
        
        # 리소스 제한
        resources:
          requests:
            cpu: "50m"
            memory: "64Mi"
          limits:
            cpu: "200m"
            memory: "128Mi"
        
        # Liveness Probe
        livenessProbe:
          httpGet:
            path: /health
            port: http
          initialDelaySeconds: 5
          periodSeconds: 10
          timeoutSeconds: 3
          failureThreshold: 3
        
        # Readiness Probe
        readinessProbe:
          httpGet:
            path: /health
            port: http
          initialDelaySeconds: 3
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        
        # 보안 컨텍스트
        securityContext:
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: false  # Nginx 캐시 필요
          capabilities:
            drop:
              - ALL
            add:
              - NET_BIND_SERVICE  # 80 포트 바인딩
      
      # 볼륨 정의
      volumes:
      - name: nginx-config
        configMap:
          name: nginx-config
      - name: html-content
        configMap:
          name: frontend-html
```

### 2.4 Frontend Service

```yaml
# manifests/frontend/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: frontend
  namespace: k8s-portfolio
  labels:
    app: frontend
    tier: frontend
spec:
  type: ClusterIP
  selector:
    app: frontend
  ports:
  - name: http
    port: 80
    targetPort: http
    protocol: TCP
```

---

## 🛠️ Part 3: Redis 구현 (1시간)

### 3.1 Redis StatefulSet

```yaml
# manifests/redis/statefulset.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: redis
  namespace: k8s-portfolio
  labels:
    app: redis
    tier: cache
spec:
  serviceName: redis-headless
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
        
        # AOF 지속성 활성화
        command:
        - redis-server
        - --appendonly
        - "yes"
        - --appendfsync
        - everysec
        - --maxmemory
        - "128mb"
        - --maxmemory-policy
        - allkeys-lru
        
        # 볼륨 마운트
        volumeMounts:
        - name: redis-data
          mountPath: /data
        
        # 리소스 제한
        resources:
          requests:
            cpu: "100m"
            memory: "128Mi"
          limits:
            cpu: "300m"
            memory: "256Mi"
        
        # Liveness Probe
        livenessProbe:
          exec:
            command:
            - redis-cli
            - ping
          initialDelaySeconds: 10
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        
        # Readiness Probe
        readinessProbe:
          exec:
            command:
            - redis-cli
            - ping
          initialDelaySeconds: 5
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 3
        
        # 보안 컨텍스트
        securityContext:
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: false
          runAsNonRoot: true
          runAsUser: 999  # redis user
  
  # 볼륨 클레임 템플릿 (동적 프로비저닝)
  volumeClaimTemplates:
  - metadata:
      name: redis-data
    spec:
      accessModes:
      - ReadWriteOnce
      storageClassName: standard  # Minikube default
      resources:
        requests:
          storage: 1Gi
```

### 3.2 Redis Service

```yaml
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
---
# Headless Service (StatefulSet용)
apiVersion: v1
kind: Service
metadata:
  name: redis-headless
  namespace: k8s-portfolio
  labels:
    app: redis
spec:
  type: ClusterIP
  clusterIP: None  # Headless
  selector:
    app: redis
  ports:
  - name: redis
    port: 6379
    targetPort: redis
```

### 3.3 Redis PVC (수동 프로비저닝용)

```yaml
# manifests/redis/pvc.yaml (StatefulSet 미사용 시)
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: redis-pvc
  namespace: k8s-portfolio
spec:
  accessModes:
  - ReadWriteOnce
  storageClassName: standard
  resources:
    requests:
      storage: 1Gi
```

---

## 🛠️ Part 4: 통합 배포 및 테스트 (1시간)

### 4.1 전체 배포 순서

```bash
cd ~/k8s-portfolio

# 1. Namespace (이미 생성되어 있다면 스킵)
kubectl apply -f manifests/namespace.yaml

# 2. ConfigMap & Secret
kubectl apply -f manifests/configmap.yaml
kubectl apply -f manifests/secret.yaml
kubectl apply -f manifests/frontend/nginx-configmap.yaml
kubectl apply -f manifests/frontend/html-configmap.yaml

# 3. Redis (Backend 의존성)
kubectl apply -f manifests/redis/

# 4. Backend
kubectl apply -f manifests/backend/

# 5. Frontend
kubectl apply -f manifests/frontend/

# 6. 상태 확인
echo "=== 전체 리소스 확인 ==="
kubectl get all -n k8s-portfolio

# 7. Pod 준비 상태 대기
kubectl wait --for=condition=ready pod -l app=frontend -n k8s-portfolio --timeout=120s
kubectl wait --for=condition=ready pod -l app=backend -n k8s-portfolio --timeout=120s
kubectl wait --for=condition=ready pod -l app=redis -n k8s-portfolio --timeout=120s
```

### 4.2 통합 테스트

```bash
# === 서비스별 테스트 ===

# Redis 테스트
echo "=== Redis 테스트 ==="
kubectl exec -n k8s-portfolio redis-0 -- redis-cli ping
kubectl exec -n k8s-portfolio redis-0 -- redis-cli set test "hello k8s"
kubectl exec -n k8s-portfolio redis-0 -- redis-cli get test

# Backend 테스트 (Service 통해)
echo ""
echo "=== Backend 테스트 ==="
kubectl run test-client --rm -it --restart=Never \
  -n k8s-portfolio --image=curlimages/curl -- sh -c "
    echo '--- Health Check ---'
    curl -s backend/health
    echo ''
    echo '--- API Info ---'
    curl -s backend/api/info
    echo ''
    echo '--- Counter ---'
    curl -s backend/api/count
"

# Frontend 테스트
echo ""
echo "=== Frontend 테스트 ==="
kubectl run test-client2 --rm -it --restart=Never \
  -n k8s-portfolio --image=curlimages/curl -- sh -c "
    echo '--- Frontend Health ---'
    curl -s frontend/health
    echo ''
    echo '--- Frontend -> Backend Proxy ---'
    curl -s frontend/api/info
    echo ''
    echo '--- HTML ---'
    curl -s frontend/ | head -20
"
```

### 4.3 Port Forward로 브라우저 테스트

```bash
# Frontend를 로컬에 노출
kubectl port-forward svc/frontend 8080:80 -n k8s-portfolio &
PF_PID=$!

echo "브라우저에서 http://localhost:8080 접속"
echo "종료하려면: kill $PF_PID"

# 또는 Minikube 환경에서 NodePort 사용
kubectl patch svc frontend -n k8s-portfolio -p '{"spec":{"type":"NodePort"}}'
minikube service frontend -n k8s-portfolio --url
```

### 4.4 E2E 테스트 스크립트

```bash
#!/bin/bash
# e2e-test.sh

echo "=== K8s Portfolio E2E Test ==="

NAMESPACE="k8s-portfolio"
FRONTEND_SVC="frontend"
BACKEND_SVC="backend"

# 1. 모든 Pod Ready 확인
echo "[1/5] Pod 상태 확인..."
if kubectl get pods -n $NAMESPACE | grep -v Running | grep -v NAME | grep -v Completed; then
    echo "⚠️ 일부 Pod가 Ready 상태가 아닙니다"
    kubectl get pods -n $NAMESPACE
    exit 1
fi
echo "✅ 모든 Pod Ready"

# 2. Redis 연결 테스트
echo "[2/5] Redis 연결 테스트..."
REDIS_PING=$(kubectl exec -n $NAMESPACE redis-0 -- redis-cli ping 2>/dev/null)
if [ "$REDIS_PING" = "PONG" ]; then
    echo "✅ Redis 정상"
else
    echo "⚠️ Redis 연결 실패"
    exit 1
fi

# 3. Backend Health 테스트
echo "[3/5] Backend Health 테스트..."
BACKEND_HEALTH=$(kubectl run health-test --rm -it --restart=Never -n $NAMESPACE \
    --image=curlimages/curl -- curl -s $BACKEND_SVC/health 2>/dev/null | grep -o "healthy")
if [ "$BACKEND_HEALTH" = "healthy" ]; then
    echo "✅ Backend 정상"
else
    echo "⚠️ Backend Health Check 실패"
fi

# 4. Frontend → Backend 프록시 테스트
echo "[4/5] Frontend → Backend 프록시 테스트..."
PROXY_TEST=$(kubectl run proxy-test --rm -it --restart=Never -n $NAMESPACE \
    --image=curlimages/curl -- curl -s $FRONTEND_SVC/api/info 2>/dev/null | grep -o "hostname")
if [ "$PROXY_TEST" = "hostname" ]; then
    echo "✅ Frontend → Backend 프록시 정상"
else
    echo "⚠️ 프록시 테스트 실패"
fi

# 5. Counter 기능 테스트
echo "[5/5] Counter 기능 테스트..."
COUNT1=$(kubectl run counter-test --rm -it --restart=Never -n $NAMESPACE \
    --image=curlimages/curl -- curl -s $FRONTEND_SVC/api/count 2>/dev/null | grep -o '"visits":[0-9]*')
echo "Counter: $COUNT1"
if [ -n "$COUNT1" ]; then
    echo "✅ Counter 기능 정상"
else
    echo "⚠️ Counter 테스트 실패"
fi

echo ""
echo "=== E2E 테스트 완료 ==="
```

---

## 📊 Part 5: 문제 해결 및 정리

### 5.1 일반적인 문제

| 문제 | 증상 | 확인 | 해결 |
|------|------|------|------|
| ConfigMap 미적용 | 설정 반영 안됨 | Pod 재시작 필요 | `kubectl rollout restart deploy frontend` |
| PVC Pending | Pod 시작 안됨 | StorageClass 확인 | `kubectl get sc` |
| Backend 연결 실패 | 502 에러 | Service/Endpoints | `kubectl get endpoints backend` |
| Redis 연결 실패 | 500 에러 | Redis Pod 상태 | `kubectl logs redis-0` |

### 5.2 리소스 정리

```bash
# 전체 정리
kubectl delete namespace k8s-portfolio

# 또는 개별 정리
kubectl delete -f manifests/frontend/
kubectl delete -f manifests/backend/
kubectl delete -f manifests/redis/
kubectl delete -f manifests/configmap.yaml
kubectl delete -f manifests/secret.yaml
kubectl delete -f manifests/namespace.yaml
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | Nginx ConfigMap | 프록시 설정 | ☐ |
| 2 | HTML ConfigMap | 정적 페이지 | ☐ |
| 3 | Frontend Deployment | Nginx 배포 | ☐ |
| 4 | Frontend Service | ClusterIP | ☐ |
| 5 | Redis StatefulSet | 데이터 영속성 | ☐ |
| 6 | Redis Service | 내부 접근 | ☐ |
| 7 | 통합 테스트 | E2E 검증 | ☐ |
| 8 | 브라우저 테스트 | Port Forward | ☐ |

---

## 🔑 오늘 배운 핵심 포인트

```yaml
# ConfigMap으로 Nginx 설정 관리
volumeMounts:
- name: nginx-config
  mountPath: /etc/nginx/conf.d

# StatefulSet으로 Redis 데이터 영속성
volumeClaimTemplates:
- metadata:
    name: redis-data
  spec:
    storage: 1Gi
```

---

## 💡 면접 대비 핵심 포인트

### Q1: StatefulSet과 Deployment의 차이는?

**A**: "StatefulSet은 Pod에 고정된 네트워크 ID와 영속적 스토리지를 제공합니다. Redis, MySQL 같은 상태를 저장하는 워크로드에 사용합니다. Deployment는 무상태(stateless) 앱에 적합합니다."

### Q2: ConfigMap을 Pod에 어떻게 전달하나요?

**A**: "volumeMounts로 파일로 마운트하거나, env로 환경 변수로 주입할 수 있습니다. 파일은 설정 파일용, 환경 변수는 단순 값 전달용으로 사용합니다."

---

## ➡️ 다음 학습: Day 57

**주제**: ConfigMap, Secret 심화 및 환경별 설정 관리
