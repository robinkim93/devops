# 📅 Day 14: Week 2 복습 및 종합 실습

## 🎯 오늘의 목표

> Week 2에서 배운 Docker 지식을 종합하여 실제 애플리케이션을 컨테이너화

---

## ⏰ 예상 학습 시간: 3시간

---

## 📋 Week 2 복습 체크리스트

| Day | 주제 | 핵심 내용 | 설명 가능? |
|-----|------|----------|----------|
| 8 | Docker 기본 | run, ps, logs, exec | ☐ |
| 9 | Dockerfile | FROM, COPY, RUN, CMD, 멀티스테이지 | ☐ |
| 10 | 네트워킹 | bridge, 사용자 정의 네트워크, 컨테이너 이름 통신 | ☐ |
| 11 | Compose | docker-compose.yml, up, down | ☐ |
| 12 | 내부 원리 | Namespace, Cgroup | ☐ |
| 13 | 트러블슈팅 | Exit Code, OOMKilled, logs | ☐ |

---

## 🛠️ 종합 실습: 3-Tier 웹 애플리케이션 구축

### 목표

```
┌─────────────────────────────────────────────────────────────┐
│  구축할 아키텍처                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐                 │
│  │ Nginx   │───→│  Flask  │───→│  Redis  │                 │
│  │ (Proxy) │    │  (App)  │    │  (Cache)│                 │
│  └─────────┘    └─────────┘    └─────────┘                 │
│    :80           :5000          :6379                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### Step 1: 프로젝트 구조 생성 (10분)

```bash
mkdir -p ~/docker-practice/week2-project/{app,nginx}
cd ~/docker-practice/week2-project
```

---

### Step 2: Flask 애플리케이션 (20분)

```bash
# app/app.py
cat << 'EOF' > app/app.py
from flask import Flask, jsonify
import redis
import os
import socket

app = Flask(__name__)
redis_client = redis.Redis(host='redis', port=6379)

@app.route('/')
def home():
    return jsonify({
        "message": "Hello from Flask!",
        "hostname": socket.gethostname()
    })

@app.route('/count')
def count():
    count = redis_client.incr('page_views')
    return jsonify({
        "page_views": count,
        "hostname": socket.gethostname()
    })

@app.route('/health')
def health():
    try:
        redis_client.ping()
        return jsonify({"status": "healthy", "redis": "connected"})
    except:
        return jsonify({"status": "unhealthy", "redis": "disconnected"}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
EOF

# app/requirements.txt
cat << 'EOF' > app/requirements.txt
flask
redis
gunicorn
EOF

# app/Dockerfile
cat << 'EOF' > app/Dockerfile
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .

EXPOSE 5000

# 프로덕션용 gunicorn 사용
CMD ["gunicorn", "--bind", "0.0.0.0:5000", "--workers", "2", "app:app"]
EOF
```

---

### Step 3: Nginx 리버스 프록시 (15분)

```bash
# nginx/nginx.conf
cat << 'EOF' > nginx/nginx.conf
upstream flask_app {
    server app:5000;
}

server {
    listen 80;
    
    location / {
        proxy_pass http://flask_app;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
    
    location /health {
        proxy_pass http://flask_app/health;
    }
}
EOF

# nginx/Dockerfile
cat << 'EOF' > nginx/Dockerfile
FROM nginx:1.24-alpine

RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/

EXPOSE 80
EOF
```

---

### Step 4: Docker Compose 작성 (20분)

```bash
# docker-compose.yml
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
    restart: unless-stopped

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
    restart: unless-stopped
    deploy:
      resources:
        limits:
          memory: 256M
          cpus: '0.5'

  redis:
    image: redis:7-alpine
    volumes:
      - redis_data:/data
    networks:
      - backend
    restart: unless-stopped
    command: redis-server --appendonly yes

networks:
  frontend:
  backend:

volumes:
  redis_data:
EOF
```

---

### Step 5: 빌드 및 실행 (10분)

```bash
# 빌드 및 실행
docker-compose up -d --build

# 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f
```

---

### Step 6: 테스트 (15분)

```bash
# 기본 요청
curl http://localhost/

# 카운터 테스트
curl http://localhost/count
curl http://localhost/count
curl http://localhost/count

# 헬스체크
curl http://localhost/health

# 부하 테스트 (간단)
for i in {1..100}; do curl -s http://localhost/count > /dev/null; done
curl http://localhost/count
```

---

### Step 7: 트러블슈팅 실습 (20분)

```bash
# 1. 로그 확인
docker-compose logs app
docker-compose logs nginx

# 2. 컨테이너 내부 접속
docker-compose exec app bash
docker-compose exec redis redis-cli

# 3. 리소스 확인
docker stats

# 4. 네트워크 확인
docker network ls
docker network inspect week2-project_backend
```

---

### Step 8: 정리 (10분)

```bash
# 중지 및 삭제
docker-compose down -v

# 이미지도 삭제
docker-compose down --rmi all -v
```

---

## ✅ Week 2 최종 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | Dockerfile 작성 가능 | ☐ |
| 2 | 멀티스테이지 빌드 이해 | ☐ |
| 3 | docker-compose.yml 작성 가능 | ☐ |
| 4 | 컨테이너 간 네트워크 통신 이해 | ☐ |
| 5 | Namespace/Cgroup 개념 설명 가능 | ☐ |
| 6 | 컨테이너 트러블슈팅 가능 | ☐ |
| 7 | 3-Tier 앱 구축 완료 | ☐ |

---

## 📝 Week 2 학습 기록

```
Week 2 완료일: ____년 __월 __일
총 소요 시간: ____시간

가장 어려웠던 부분:


면접에서 설명할 수 있는 Docker 경험:


```

---

## ➡️ 다음 학습: Day 15 (Week 3)

**Week 3-4 주제**: Kubernetes 시작 전 추가 준비

**예고**:
- Day 15-21: Linux 심화 (systemd, 보안)
- Day 22-30: Month 1 프로젝트 + 정리

