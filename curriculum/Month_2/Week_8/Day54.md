# 📅 Day 54: Month 2 프로젝트 시작 - Kubernetes 애플리케이션 설계

## 🎯 프로젝트 목표

> **포트폴리오 프로젝트 #2**: "Kubernetes 3-Tier 애플리케이션 배포"

실제 운영 환경에서 사용되는 패턴으로 애플리케이션을 Kubernetes에 배포합니다.

---

## ⏰ 예상 소요 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 설계 | 1시간 | 아키텍처 및 구조 설계 |
| 구조화 | 1시간 | 디렉토리 및 파일 구조 |
| 기반 작업 | 1.5시간 | Namespace, ConfigMap, 기본 Deployment |
| 문서화 | 30분 | README 및 설계 문서 |

---

## 📋 프로젝트 개요

### 비즈니스 시나리오

```
┌─────────────────────────────────────────────────────────────┐
│  시나리오: 토스플레이스 결제 모듈 (간소화 버전)             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  구성 요소:                                                 │
│  - Frontend: Nginx (정적 페이지 서빙)                      │
│  - Backend: Flask API (결제 로직)                          │
│  - Cache: Redis (세션/캐시)                                │
│                                                             │
│  학습 목표:                                                 │
│  ✓ Deployment, Service 작성                                │
│  ✓ ConfigMap, Secret 분리                                  │
│  ✓ PVC로 데이터 영속화                                     │
│  ✓ Ingress로 외부 노출                                     │
│  ✓ HPA로 자동 스케일링                                     │
│  ✓ Probe로 헬스체크                                        │
│  ✓ 리소스 관리 (requests/limits)                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 아키텍처 다이어그램

```
┌─────────────────────────────────────────────────────────────┐
│  3-Tier Application on Kubernetes                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                    Ingress                           │   │
│  │              (k8s-portfolio.local)                   │   │
│  └───────────────────────┬─────────────────────────────┘   │
│                          │                                  │
│         ┌────────────────┼────────────────┐                │
│         │                │                │                │
│         ▼                ▼                ▼                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │  Frontend   │  │   Backend   │  │   Backend   │        │
│  │   (Nginx)   │  │   (Flask)   │  │   (Flask)   │        │
│  │   Service   │  │  Replica 1  │  │  Replica 2  │        │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘        │
│         │                │                │                │
│         │         ┌──────┴────────────────┘                │
│         │         │                                         │
│         │         ▼                                         │
│         │  ┌─────────────┐                                 │
│         │  │   Redis     │                                 │
│         │  │   Service   │──── PVC (데이터 영속화)         │
│         │  └─────────────┘                                 │
│         │                                                   │
│  ConfigMap: app-config, nginx-config                       │
│  Secret: app-secret (DB 비밀번호, API 키)                  │
│  HPA: backend (CPU 50% 기준 2-10 Pods)                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 프로젝트 일정

| Day | 작업 | 핵심 내용 |
|-----|------|----------|
| 54 | 아키텍처 설계 | 디렉토리 구조, Namespace |
| 55 | Backend 배포 | Deployment, Service, Probe |
| 56 | Frontend, Redis | 나머지 컴포넌트 배포 |
| 57 | 설정 분리 | ConfigMap, Secret, PVC |
| 58 | 외부 노출 | Ingress, HPA |
| 59 | 문서화 | README, 테스트 |
| 60 | 완료 | GitHub 업로드, Month 2 정리 |

---

## 🛠️ Part 1: 디렉토리 구조 설계

### 프로젝트 구조 생성

```bash
# 프로젝트 디렉토리 생성
mkdir -p ~/portfolio/k8s-project
cd ~/portfolio/k8s-project

# 전체 구조 생성
mkdir -p {app/{backend,frontend},manifests/{base,backend,frontend,redis},docs,scripts}
```

### 전체 구조

```
k8s-project/
├── README.md                      # 프로젝트 소개
├── app/                           # 애플리케이션 소스
│   ├── backend/
│   │   ├── app.py                 # Flask 애플리케이션
│   │   ├── requirements.txt       # Python 의존성
│   │   └── Dockerfile             # 컨테이너 이미지
│   └── frontend/
│       ├── html/
│       │   └── index.html         # 정적 페이지
│       ├── nginx.conf             # Nginx 설정
│       └── Dockerfile
│
├── manifests/                     # Kubernetes 매니페스트
│   ├── namespace.yaml             # Namespace 정의
│   ├── configmap.yaml             # ConfigMap
│   ├── secret.yaml                # Secret
│   │
│   ├── backend/
│   │   ├── deployment.yaml        # Backend Deployment
│   │   ├── service.yaml           # Backend Service
│   │   └── hpa.yaml               # HPA
│   │
│   ├── frontend/
│   │   ├── deployment.yaml        # Frontend Deployment
│   │   └── service.yaml           # Frontend Service
│   │
│   ├── redis/
│   │   ├── deployment.yaml        # Redis Deployment
│   │   ├── service.yaml           # Redis Service
│   │   └── pvc.yaml               # PersistentVolumeClaim
│   │
│   └── ingress.yaml               # Ingress
│
├── docs/                          # 문서
│   ├── architecture.md            # 아키텍처 설명
│   └── troubleshooting.md         # 트러블슈팅 가이드
│
└── scripts/                       # 스크립트
    ├── setup.sh                   # 환경 설정
    ├── deploy.sh                  # 배포 스크립트
    ├── cleanup.sh                 # 정리 스크립트
    └── test.sh                    # 테스트 스크립트
```

---

## 🛠️ Part 2: 기본 매니페스트 작성

### Namespace 정의

```yaml
# manifests/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: k8s-portfolio
  labels:
    project: k8s-portfolio
    environment: development
  annotations:
    description: "Kubernetes 3-Tier Application Portfolio Project"
```

```bash
# Namespace 생성
kubectl apply -f manifests/namespace.yaml

# 확인
kubectl get namespace k8s-portfolio
```

### ConfigMap 정의

```yaml
# manifests/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: k8s-portfolio
data:
  # Backend 설정
  REDIS_HOST: "redis"
  REDIS_PORT: "6379"
  APP_ENV: "production"
  LOG_LEVEL: "info"
  
  # Frontend 설정
  BACKEND_URL: "http://backend:80"

---
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
        
        # 정적 파일
        location / {
            root /usr/share/nginx/html;
            index index.html;
            try_files $uri $uri/ /index.html;
        }
        
        # API 프록시
        location /api/ {
            proxy_pass http://backend/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Request-ID $request_id;
            
            # 타임아웃 설정
            proxy_connect_timeout 10s;
            proxy_read_timeout 30s;
        }
        
        # 헬스체크
        location /health {
            return 200 'OK';
            add_header Content-Type text/plain;
        }
    }
```

### Secret 정의

```yaml
# manifests/secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secret
  namespace: k8s-portfolio
type: Opaque
data:
  # echo -n 'redis-password-123' | base64
  REDIS_PASSWORD: cmVkaXMtcGFzc3dvcmQtMTIz
  # echo -n 'api-key-secret-456' | base64
  API_KEY: YXBpLWtleS1zZWNyZXQtNDU2
```

---

## 🛠️ Part 3: 애플리케이션 코드

### Backend (Flask)

```python
# app/backend/app.py
from flask import Flask, jsonify, request
import redis
import os
import logging

app = Flask(__name__)

# 로깅 설정
logging.basicConfig(
    level=getattr(logging, os.getenv('LOG_LEVEL', 'INFO').upper()),
    format='{"time":"%(asctime)s","level":"%(levelname)s","message":"%(message)s"}'
)
logger = logging.getLogger(__name__)

# Redis 연결
redis_client = redis.Redis(
    host=os.getenv('REDIS_HOST', 'localhost'),
    port=int(os.getenv('REDIS_PORT', 6379)),
    password=os.getenv('REDIS_PASSWORD', None),
    decode_responses=True
)

@app.route('/health')
def health():
    """Liveness Probe 엔드포인트"""
    return jsonify({'status': 'healthy'}), 200

@app.route('/ready')
def ready():
    """Readiness Probe 엔드포인트"""
    try:
        redis_client.ping()
        return jsonify({'status': 'ready'}), 200
    except Exception as e:
        logger.error(f"Redis connection failed: {e}")
        return jsonify({'status': 'not ready', 'error': str(e)}), 503

@app.route('/api/counter', methods=['GET', 'POST'])
def counter():
    """간단한 카운터 API"""
    if request.method == 'POST':
        redis_client.incr('counter')
    count = redis_client.get('counter') or 0
    return jsonify({'count': int(count)})

@app.route('/api/info')
def info():
    """서버 정보"""
    return jsonify({
        'version': '1.0.0',
        'environment': os.getenv('APP_ENV', 'development'),
        'hostname': os.uname().nodename
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=80)
```

```text
# app/backend/requirements.txt
flask==3.0.0
redis==5.0.1
gunicorn==21.2.0
```

```dockerfile
# app/backend/Dockerfile
FROM python:3.11-slim

WORKDIR /app

# 비root 사용자 생성
RUN groupadd -r appuser && useradd -r -g appuser appuser

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .

# 권한 설정
RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 80

CMD ["gunicorn", "--bind", "0.0.0.0:80", "--workers", "2", "app:app"]
```

### Frontend (Nginx)

```html
<!-- app/frontend/html/index.html -->
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>K8s Portfolio App</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
            max-width: 600px;
            margin: 100px auto;
            padding: 20px;
            text-align: center;
        }
        .counter {
            font-size: 48px;
            color: #3498db;
            margin: 20px 0;
        }
        button {
            background: #3498db;
            color: white;
            border: none;
            padding: 15px 30px;
            font-size: 18px;
            border-radius: 5px;
            cursor: pointer;
        }
        button:hover {
            background: #2980b9;
        }
        .info {
            margin-top: 30px;
            color: #666;
        }
    </style>
</head>
<body>
    <h1>🚀 Kubernetes Portfolio</h1>
    <div class="counter" id="counter">0</div>
    <button onclick="increment()">Click Me!</button>
    <div class="info" id="info"></div>

    <script>
        async function fetchCounter() {
            const res = await fetch('/api/counter');
            const data = await res.json();
            document.getElementById('counter').textContent = data.count;
        }

        async function increment() {
            await fetch('/api/counter', { method: 'POST' });
            fetchCounter();
        }

        async function fetchInfo() {
            const res = await fetch('/api/info');
            const data = await res.json();
            document.getElementById('info').innerHTML = 
                `Version: ${data.version} | Env: ${data.environment} | Host: ${data.hostname}`;
        }

        fetchCounter();
        fetchInfo();
    </script>
</body>
</html>
```

---

## 🛠️ Part 4: 배포 스크립트

### 배포 스크립트

```bash
#!/bin/bash
# scripts/deploy.sh
set -e

NAMESPACE="k8s-portfolio"

echo "🚀 K8s Portfolio 배포 시작..."

# 1. Namespace
echo "📦 Namespace 생성..."
kubectl apply -f manifests/namespace.yaml

# 2. ConfigMap & Secret
echo "⚙️ ConfigMap & Secret 적용..."
kubectl apply -f manifests/configmap.yaml
kubectl apply -f manifests/secret.yaml

# 3. Redis
echo "🔴 Redis 배포..."
kubectl apply -f manifests/redis/

# Redis Ready 대기
kubectl wait --for=condition=ready pod -l app=redis -n $NAMESPACE --timeout=60s

# 4. Backend
echo "🔧 Backend 배포..."
kubectl apply -f manifests/backend/

# 5. Frontend
echo "🎨 Frontend 배포..."
kubectl apply -f manifests/frontend/

# 6. Ingress
echo "🌐 Ingress 적용..."
kubectl apply -f manifests/ingress.yaml

# 7. 상태 확인
echo "✅ 배포 상태 확인..."
kubectl get all -n $NAMESPACE

echo "🎉 배포 완료!"
echo "🔗 http://k8s-portfolio.local 에서 확인하세요"
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | 아키텍처 설계 완료 | ☐ |
| 2 | 디렉토리 구조 생성 | ☐ |
| 3 | Namespace YAML 작성 | ☐ |
| 4 | ConfigMap/Secret 작성 | ☐ |
| 5 | Backend 애플리케이션 코드 작성 | ☐ |
| 6 | Frontend 코드 작성 | ☐ |
| 7 | Dockerfile 작성 | ☐ |
| 8 | 배포 스크립트 작성 | ☐ |

---

## 📝 설계 결정 사항

### 왜 이 구조인가?

```
1. 관심사 분리
   - app/: 애플리케이션 코드 (개발자 담당)
   - manifests/: 인프라 코드 (DevOps 담당)
   
2. 환경별 관리 용이
   - ConfigMap/Secret 분리로 환경별 설정 관리
   - Kustomize 오버레이 확장 가능
   
3. 마이크로서비스 패턴
   - 각 컴포넌트 독립 배포 가능
   - 개별 스케일링 가능
```

---

## ➡️ 다음 학습: Day 55

**주제**: Backend Deployment/Service 작성
- Deployment YAML 상세 작성
- Service 설정
- Probe 설정
- 리소스 관리
