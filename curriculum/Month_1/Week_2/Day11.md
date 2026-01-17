# 📅 Day 11: Docker Compose

## 🎯 오늘의 목표

> **토스플레이스 연결점**: 멀티 컨테이너 애플리케이션 관리 (Kubernetes 전 단계)

Docker Compose로 여러 컨테이너를 함께 관리하는 방법을 익힙니다.

---

## ⏰ 예상 학습 시간: 3시간

---

## 📚 Part 1: 핵심 개념 (30분)

### Docker Compose란?

```
여러 컨테이너를 YAML 파일 하나로 정의하고 관리

docker run ... (반복) → docker-compose up (한 번에!)
```

### docker-compose.yml 구조

```yaml
version: "3.8"
services:
  web:           # 서비스 이름
    image: nginx
    ports:
      - "80:80"
  db:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: secret
```

---

## 🛠️ Part 2: 실습 (2시간)

### 실습 1: 기본 Compose 파일 (30분)

```bash
mkdir -p ~/docker-practice/compose-basic
cd ~/docker-practice/compose-basic

cat << 'EOF' > docker-compose.yml
version: "3.8"

services:
  web:
    image: nginx:1.24
    ports:
      - "8080:80"
    
  redis:
    image: redis:7
EOF

# 실행
docker-compose up -d

# 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs

# 중지 및 삭제
docker-compose down
```

---

### 실습 2: 웹 애플리케이션 스택 (40분)

```bash
mkdir -p ~/docker-practice/compose-app
cd ~/docker-practice/compose-app

# Flask 앱
cat << 'EOF' > app.py
from flask import Flask
import redis
import os

app = Flask(__name__)
r = redis.Redis(host='redis', port=6379)

@app.route('/')
def hello():
    count = r.incr('hits')
    return f'Hello! Count: {count}\n'

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
EOF

cat << 'EOF' > requirements.txt
flask
redis
EOF

cat << 'EOF' > Dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt
COPY . .
CMD ["python", "app.py"]
EOF

# Docker Compose 파일
cat << 'EOF' > docker-compose.yml
version: "3.8"

services:
  web:
    build: .
    ports:
      - "5000:5000"
    depends_on:
      - redis
    environment:
      - FLASK_ENV=development
    volumes:
      - .:/app  # 개발 시 코드 변경 반영
    
  redis:
    image: redis:7-alpine
EOF

# 빌드 및 실행
docker-compose up -d --build

# 테스트
curl http://localhost:5000

# 로그 확인
docker-compose logs -f web
```

---

### 실습 3: WordPress + MySQL (30분)

```bash
mkdir -p ~/docker-practice/compose-wordpress
cd ~/docker-practice/compose-wordpress

cat << 'EOF' > docker-compose.yml
version: "3.8"

services:
  wordpress:
    image: wordpress:latest
    ports:
      - "8080:80"
    environment:
      WORDPRESS_DB_HOST: db
      WORDPRESS_DB_USER: wordpress
      WORDPRESS_DB_PASSWORD: wordpress
      WORDPRESS_DB_NAME: wordpress
    depends_on:
      - db
    volumes:
      - wordpress_data:/var/www/html

  db:
    image: mysql:8
    environment:
      MYSQL_DATABASE: wordpress
      MYSQL_USER: wordpress
      MYSQL_PASSWORD: wordpress
      MYSQL_ROOT_PASSWORD: rootpassword
    volumes:
      - db_data:/var/lib/mysql

volumes:
  wordpress_data:
  db_data:
EOF

# 실행
docker-compose up -d

# 확인
docker-compose ps

# http://localhost:8080 에서 WordPress 확인

# 정리
docker-compose down -v  # -v: 볼륨도 삭제
```

---

### 실습 4: 주요 명령어 (20분)

```bash
# 실행
docker-compose up -d              # 백그라운드
docker-compose up -d --build      # 이미지 재빌드

# 상태 확인
docker-compose ps                 # 상태
docker-compose logs              # 로그
docker-compose logs -f web       # 특정 서비스 로그

# 스케일링
docker-compose up -d --scale web=3  # web 서비스 3개 실행

# 실행 중 명령어
docker-compose exec web bash     # 서비스에 접속

# 중지/삭제
docker-compose stop              # 중지
docker-compose down              # 중지 + 삭제
docker-compose down -v           # 볼륨도 삭제
docker-compose down --rmi all    # 이미지도 삭제
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | docker-compose.yml 작성 | ☐ |
| 2 | `docker-compose up -d` 실행 | ☐ |
| 3 | 멀티 서비스 (web + db) 구성 | ☐ |
| 4 | `depends_on`으로 의존성 설정 | ☐ |
| 5 | volumes로 데이터 영속화 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
docker-compose up -d             # 실행
docker-compose up -d --build     # 빌드 후 실행
docker-compose ps                # 상태
docker-compose logs -f           # 로그
docker-compose exec <svc> bash   # 접속
docker-compose down -v           # 삭제
```

---

## ➡️ 다음 학습: Day 12

**주제**: 컨테이너 내부 원리 (namespace, cgroup)

