# Day 10: Docker Compose

## 오늘의 목표

토스플레이스 연결점: "컨테이너 오케스트레이션 서비스 운영"
"배포 자동화 파이프라인을 운영하고 CI/CD 환경을 개선"

Docker Compose로 멀티 컨테이너 애플리케이션을 정의하고 관리합니다. 실제 웹 애플리케이션과 데이터베이스를 함께 구성하는 방법을 익힙니다.

---

## 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 핵심 개념 | 45분 | Compose 구조, YAML 문법 |
| 기본 실습 | 1시간 | 간단한 서비스 구성 |
| 심화 실습 | 1.5시간 | 멀티 서비스 애플리케이션 |
| 운영 명령어 | 45분 | 로그, 스케일링 |

---

## Part 1: Docker Compose 핵심 개념 (45분)

### 1.1 왜 Docker Compose가 필요한가?

```
단일 컨테이너 실행:
docker run -d --name web -p 80:80 nginx
docker run -d --name db -e MYSQL_ROOT_PASSWORD=pass mysql
docker run -d --name redis redis

문제점:
- 여러 docker run 명령어 관리
- 컨테이너 간 네트워크 설정
- 의존성 순서 관리
- 환경변수, 볼륨 등 설정 복잡

Docker Compose 해결:
- 하나의 YAML 파일로 정의
- docker-compose up 한 번으로 전체 실행
- 네트워크 자동 구성
- 의존성 관리
```

### 1.2 docker-compose.yml 구조

```yaml
# docker-compose.yml 기본 구조
services:        # 컨테이너 정의
  web:
    image: nginx
    ports:
      - "80:80"
  db:
    image: mysql
    environment:
      MYSQL_ROOT_PASSWORD: secret

volumes:         # 볼륨 정의 (선택)
  db-data:

networks:        # 네트워크 정의 (선택)
  backend:
```

### 1.3 주요 서비스 옵션

| 옵션 | 설명 | 예시 |
|------|------|------|
| image | 사용할 이미지 | nginx:1.24 |
| build | Dockerfile 경로 | ./app |
| ports | 포트 매핑 | "8080:80" |
| environment | 환경변수 | MYSQL_ROOT_PASSWORD: pass |
| volumes | 볼륨 마운트 | - db-data:/var/lib/mysql |
| depends_on | 의존성 | - db |
| restart | 재시작 정책 | unless-stopped |
| networks | 연결할 네트워크 | - backend |

---

## Part 2: 기본 실습 (1시간)

### 실습 1: 간단한 Compose 파일

```bash
mkdir -p ~/compose-demo && cd ~/compose-demo

cat << 'EOF' > docker-compose.yml
services:
  web:
    image: nginx:alpine
    ports:
      - "8080:80"
    volumes:
      - ./html:/usr/share/nginx/html:ro
EOF

# HTML 파일 생성
mkdir -p html
echo "<h1>Hello Docker Compose!</h1>" > html/index.html

# 실행
docker-compose up -d

# 확인
docker-compose ps
curl http://localhost:8080

# 로그 확인
docker-compose logs web

# 종료
docker-compose down
```

### 실습 2: 여러 서비스 구성

```bash
cat << 'EOF' > docker-compose.yml
services:
  web:
    image: nginx:alpine
    ports:
      - "8080:80"
    depends_on:
      - api
    networks:
      - frontend

  api:
    image: hashicorp/http-echo:latest
    command: ["-text=Hello from API"]
    networks:
      - frontend

networks:
  frontend:
EOF

docker-compose up -d
docker-compose ps

# 네트워크 확인
docker network ls | grep compose

docker-compose down
```

### 실습 3: 환경변수 사용

```bash
cat << 'EOF' > docker-compose.yml
services:
  db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD:-defaultpass}
      MYSQL_DATABASE: ${DB_NAME:-myapp}
    ports:
      - "${DB_PORT:-3306}:3306"
EOF

# 기본값으로 실행
docker-compose up -d
docker-compose ps

# 환경변수 지정
DB_PASSWORD=securepass DB_PORT=3307 docker-compose up -d

docker-compose down -v
```

### 실습 4: .env 파일 사용

```bash
cat << 'EOF' > .env
DB_PASSWORD=mypassword
DB_NAME=production_db
DB_PORT=3308
EOF

cat << 'EOF' > docker-compose.yml
services:
  db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: ${DB_NAME}
    ports:
      - "${DB_PORT}:3306"
EOF

docker-compose config  # 적용된 설정 확인
docker-compose up -d
docker-compose down -v
```

---

## Part 3: 심화 실습 - 웹 애플리케이션 (1.5시간)

### 실습 5: Flask + Redis 애플리케이션

```bash
mkdir -p ~/flask-redis && cd ~/flask-redis

# Flask 앱
cat << 'EOF' > app.py
from flask import Flask
import redis
import os

app = Flask(__name__)
cache = redis.Redis(host=os.getenv('REDIS_HOST', 'redis'), port=6379)

@app.route('/')
def hello():
    count = cache.incr('hits')
    return f'Hello! This page has been viewed {count} times.\n'

@app.route('/health')
def health():
    return 'OK'

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
EOF

# requirements.txt
cat << 'EOF' > requirements.txt
flask==3.0.0
redis==5.0.0
EOF

# Dockerfile
cat << 'EOF' > Dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app.py .
CMD ["python", "app.py"]
EOF

# docker-compose.yml
cat << 'EOF' > docker-compose.yml
services:
  web:
    build: .
    ports:
      - "5000:5000"
    environment:
      - REDIS_HOST=redis
    depends_on:
      - redis
    restart: unless-stopped

  redis:
    image: redis:alpine
    volumes:
      - redis-data:/data
    restart: unless-stopped

volumes:
  redis-data:
EOF

# 빌드 및 실행
docker-compose up -d --build

# 테스트
curl http://localhost:5000
curl http://localhost:5000
curl http://localhost:5000
# 카운터가 증가하는 것 확인

# 로그 확인
docker-compose logs -f web

# 정리
docker-compose down -v
```

### 실습 6: Nginx + PHP + MySQL (3-Tier)

```bash
mkdir -p ~/lamp-stack && cd ~/lamp-stack

# 디렉토리 구조
mkdir -p {php,nginx}

# PHP 파일
cat << 'EOF' > php/index.php
<?php
$host = getenv('MYSQL_HOST') ?: 'db';
$user = getenv('MYSQL_USER') ?: 'root';
$pass = getenv('MYSQL_PASSWORD') ?: 'secret';
$db   = getenv('MYSQL_DATABASE') ?: 'test';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$db", $user, $pass);
    echo "<h1>Connected to MySQL!</h1>";
    echo "<p>Database: $db</p>";
} catch (PDOException $e) {
    echo "<h1>Connection failed</h1>";
    echo "<p>" . $e->getMessage() . "</p>";
}
?>
EOF

# Nginx 설정
cat << 'EOF' > nginx/default.conf
server {
    listen 80;
    root /var/www/html;
    index index.php;

    location ~ \.php$ {
        fastcgi_pass php:9000;
        fastcgi_param SCRIPT_FILENAME $document_root$fastcgi_script_name;
        include fastcgi_params;
    }
}
EOF

# docker-compose.yml
cat << 'EOF' > docker-compose.yml
services:
  nginx:
    image: nginx:alpine
    ports:
      - "8080:80"
    volumes:
      - ./php:/var/www/html
      - ./nginx/default.conf:/etc/nginx/conf.d/default.conf
    depends_on:
      - php

  php:
    image: php:8.2-fpm
    volumes:
      - ./php:/var/www/html
    environment:
      - MYSQL_HOST=db
      - MYSQL_USER=root
      - MYSQL_PASSWORD=secret
      - MYSQL_DATABASE=test

  db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: secret
      MYSQL_DATABASE: test
    volumes:
      - db-data:/var/lib/mysql

volumes:
  db-data:
EOF

# PHP PDO 확장 필요 - 커스텀 이미지 생성
cat << 'EOF' > Dockerfile.php
FROM php:8.2-fpm
RUN docker-php-ext-install pdo pdo_mysql
EOF

# docker-compose.yml 수정
cat << 'EOF' > docker-compose.yml
services:
  nginx:
    image: nginx:alpine
    ports:
      - "8080:80"
    volumes:
      - ./php:/var/www/html
      - ./nginx/default.conf:/etc/nginx/conf.d/default.conf
    depends_on:
      - php

  php:
    build:
      context: .
      dockerfile: Dockerfile.php
    volumes:
      - ./php:/var/www/html
    environment:
      - MYSQL_HOST=db
      - MYSQL_USER=root
      - MYSQL_PASSWORD=secret
      - MYSQL_DATABASE=test
    depends_on:
      - db

  db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: secret
      MYSQL_DATABASE: test
    volumes:
      - db-data:/var/lib/mysql

volumes:
  db-data:
EOF

docker-compose up -d --build

# DB 초기화 대기
sleep 30

# 테스트
curl http://localhost:8080/index.php

docker-compose down -v
```

---

## Part 4: 운영 명령어 (45분)

### 실습 7: 주요 명령어

```bash
cd ~/flask-redis
docker-compose up -d --build

# 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs              # 전체 로그
docker-compose logs web          # 특정 서비스
docker-compose logs -f --tail=20 # 실시간, 마지막 20줄

# 서비스 재시작
docker-compose restart web

# 서비스 중지/시작
docker-compose stop web
docker-compose start web

# 설정 검증
docker-compose config

# 컨테이너 접속
docker-compose exec web sh

# 일회성 명령 실행
docker-compose run --rm web python --version

# 스케일링
docker-compose up -d --scale web=3
docker-compose ps

docker-compose down
```

### 실습 8: 빌드 관리

```bash
# 이미지 빌드
docker-compose build

# 캐시 없이 빌드
docker-compose build --no-cache

# 빌드 후 실행
docker-compose up -d --build

# 특정 서비스만 빌드
docker-compose build web
```

---

## 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | Compose 필요성 이해 | 멀티 컨테이너 관리 | |
| 2 | YAML 구조 이해 | services, volumes, networks | |
| 3 | 기본 명령어 | up, down, ps, logs | |
| 4 | 환경변수 | environment, .env | |
| 5 | 서비스 의존성 | depends_on | |
| 6 | Flask + Redis 실습 | 멀티 서비스 앱 | |
| 7 | 운영 명령어 | restart, exec, scale | |

---

## 핵심 명령어

```bash
# 실행/종료
docker-compose up -d
docker-compose down
docker-compose down -v          # 볼륨도 삭제

# 상태 확인
docker-compose ps
docker-compose logs -f

# 관리
docker-compose restart
docker-compose exec <service> sh
docker-compose build --no-cache
```

---

## 면접 대비

**Q: Docker Compose의 장점은?**
> "여러 컨테이너를 하나의 YAML 파일로 정의하여 관리할 수 있습니다. 네트워크 자동 구성, 의존성 관리, 환경별 설정 분리가 가능하여 개발 환경 구성과 로컬 테스트에 유용합니다."

**Q: depends_on의 한계는?**
> "컨테이너 시작 순서만 보장하고, 실제 서비스가 준비될 때까지 기다리지 않습니다. DB가 실제로 연결 가능한 상태가 될 때까지 대기하려면 healthcheck와 함께 사용하거나 애플리케이션에서 재시도 로직이 필요합니다."

---

## 정리

```bash
cd ~
rm -rf ~/compose-demo ~/flask-redis ~/lamp-stack
docker system prune -f
```

---

## 다음 학습: Day 11

주제: Docker 네트워킹 기초
- 네트워크 드라이버
- 컨테이너 간 통신
- DNS 서비스 디스커버리
