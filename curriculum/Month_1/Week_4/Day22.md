# 📅 Day 22: Month 1 프로젝트 시작 - 환경 구축

## 🎯 프로젝트 목표

> **포트폴리오 프로젝트 #1**: "Linux 트러블슈팅 포트폴리오"
> 
> 실제 장애 상황을 시뮬레이션하고, 분석-해결하는 과정을 문서화

---

## ⏰ 예상 소요 시간: 3시간

---

## 📋 프로젝트 개요

### 만들 것

```
┌─────────────────────────────────────────────────────────────┐
│  3-Tier 웹 애플리케이션 + 장애 시뮬레이션                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐                 │
│  │  Nginx  │───→│  Flask  │───→│  Redis  │                 │
│  │ (Proxy) │    │  (App)  │    │ (Cache) │                 │
│  └─────────┘    └─────────┘    └─────────┘                 │
│                                                             │
│  장애 시나리오:                                             │
│  1. 메모리 누수 → OOMKilled                                │
│  2. 과도한 시스템 콜 → CPU 100%                            │
│  3. 연결 누수 → TIME_WAIT 폭증                             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 프로젝트 산출물

1. **Docker Compose로 구축한 3-Tier 앱**
2. **장애 시뮬레이션 스크립트**
3. **트러블슈팅 가이드 문서**
4. **블로그 포스트 (면접용)**

---

## 🛠️ 오늘의 작업: 환경 구축

### Step 1: 프로젝트 디렉토리 생성

```bash
mkdir -p ~/portfolio/month1-troubleshooting/{app,nginx,scripts}
cd ~/portfolio/month1-troubleshooting
```

### Step 2: Flask 애플리케이션 (메모리 누수 가능)

```bash
cat << 'EOF' > app/app.py
from flask import Flask, jsonify
import redis
import socket
import os
import gc

app = Flask(__name__)
redis_client = redis.Redis(host='redis', port=6379)

# 메모리 누수 시뮬레이션용 리스트
memory_leak = []

@app.route('/')
def home():
    return jsonify({
        "status": "ok",
        "hostname": socket.gethostname()
    })

@app.route('/count')
def count():
    count = redis_client.incr('hits')
    return jsonify({"hits": count})

@app.route('/health')
def health():
    try:
        redis_client.ping()
        return jsonify({"status": "healthy"})
    except:
        return jsonify({"status": "unhealthy"}), 500

# 장애 시뮬레이션 엔드포인트
@app.route('/leak')
def leak():
    """메모리 누수 시뮬레이션 - 1MB씩 누수"""
    data = "X" * (1024 * 1024)  # 1MB
    memory_leak.append(data)
    return jsonify({
        "leaked_mb": len(memory_leak),
        "message": "Memory leaked"
    })

@app.route('/leak/clear')
def clear_leak():
    """메모리 누수 해제"""
    global memory_leak
    memory_leak = []
    gc.collect()
    return jsonify({"message": "Memory cleared"})

@app.route('/cpu')
def cpu_intensive():
    """CPU 집약적 작업 시뮬레이션"""
    result = 0
    for i in range(10000000):
        result += i
    return jsonify({"result": result})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
EOF

cat << 'EOF' > app/requirements.txt
flask
redis
gunicorn
EOF

cat << 'EOF' > app/Dockerfile
FROM python:3.11-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app.py .

EXPOSE 5000
CMD ["gunicorn", "--bind", "0.0.0.0:5000", "--workers", "2", "app:app"]
EOF
```

### Step 3: Nginx 설정

```bash
cat << 'EOF' > nginx/nginx.conf
events {
    worker_connections 1024;
}

http {
    upstream app {
        server app:5000;
    }

    server {
        listen 80;

        location / {
            proxy_pass http://app;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }

        location /nginx_status {
            stub_status on;
            allow 127.0.0.1;
            allow 172.0.0.0/8;
            deny all;
        }
    }
}
EOF

cat << 'EOF' > nginx/Dockerfile
FROM nginx:1.24-alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/nginx.conf
EOF
```

### Step 4: Docker Compose

```bash
cat << 'EOF' > docker-compose.yml
version: "3.8"

services:
  nginx:
    build: ./nginx
    ports:
      - "80:80"
    depends_on:
      - app
    networks:
      - frontend

  app:
    build: ./app
    expose:
      - "5000"
    depends_on:
      - redis
    environment:
      - FLASK_ENV=production
    networks:
      - frontend
      - backend
    # 메모리 제한 (장애 시뮬레이션용)
    deploy:
      resources:
        limits:
          memory: 128M

  redis:
    image: redis:7-alpine
    networks:
      - backend
    volumes:
      - redis_data:/data

networks:
  frontend:
  backend:

volumes:
  redis_data:
EOF
```

### Step 5: 빌드 및 실행

```bash
# 빌드
docker-compose build

# 실행
docker-compose up -d

# 상태 확인
docker-compose ps

# 테스트
curl http://localhost/
curl http://localhost/count
curl http://localhost/health
```

### Step 6: 기본 동작 확인

```bash
# 여러 번 요청
for i in {1..10}; do curl -s http://localhost/count; echo; done

# 로그 확인
docker-compose logs -f app
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | 프로젝트 디렉토리 구조 생성 | ☐ |
| 2 | Flask 앱 작성 (장애 엔드포인트 포함) | ☐ |
| 3 | Nginx 설정 | ☐ |
| 4 | Docker Compose 작성 | ☐ |
| 5 | 기본 동작 테스트 완료 | ☐ |

---

## 📝 프로젝트 기록 시작

```
프로젝트 시작일: ____년 __월 __일

오늘 구축한 환경:
- 

발생한 문제와 해결:
- 

내일 할 일:
- 메모리 누수 장애 시뮬레이션
```

---

## ➡️ 다음: Day 23

**주제**: 장애 시나리오 #1 - 메모리 누수 분석

