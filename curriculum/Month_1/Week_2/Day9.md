# 📅 Day 9: Dockerfile 작성

## 🎯 오늘의 목표

> **토스플레이스 연결점**: 효율적인 컨테이너 이미지 빌드

Dockerfile을 작성하고 최적화된 이미지를 빌드할 수 있어야 합니다.

---

## ⏰ 예상 학습 시간: 3시간

---

## 📚 Part 1: 핵심 개념 (30분)

### Dockerfile이란?

```
Dockerfile = 이미지를 만드는 레시피

1. 베이스 이미지 선택
2. 필요한 패키지 설치
3. 애플리케이션 코드 복사
4. 실행 명령어 지정
```

### 주요 명령어

| 명령어 | 설명 | 예시 |
|--------|------|------|
| `FROM` | 베이스 이미지 | `FROM nginx:1.24` |
| `RUN` | 명령어 실행 (빌드 시) | `RUN apt-get update` |
| `COPY` | 파일 복사 | `COPY ./app /app` |
| `WORKDIR` | 작업 디렉토리 | `WORKDIR /app` |
| `ENV` | 환경 변수 | `ENV NODE_ENV=production` |
| `EXPOSE` | 포트 문서화 | `EXPOSE 8080` |
| `CMD` | 실행 명령어 | `CMD ["nginx", "-g", "daemon off;"]` |

---

## 🛠️ Part 2: 실습 (2시간)

### 실습 1: 간단한 Dockerfile (30분)

```bash
# 작업 디렉토리 생성
mkdir -p ~/docker-practice/simple
cd ~/docker-practice/simple

# index.html 생성
cat << 'EOF' > index.html
<!DOCTYPE html>
<html>
<head><title>My App</title></head>
<body><h1>Hello from Docker!</h1></body>
</html>
EOF

# Dockerfile 생성
cat << 'EOF' > Dockerfile
FROM nginx:1.24

COPY index.html /usr/share/nginx/html/index.html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
EOF

# 이미지 빌드
docker build -t my-nginx:v1 .

# 실행 및 확인
docker run -d -p 8080:80 --name test my-nginx:v1
curl http://localhost:8080

# 정리
docker rm -f test
```

---

### 실습 2: Python 애플리케이션 (30분)

```bash
mkdir -p ~/docker-practice/python-app
cd ~/docker-practice/python-app

# 애플리케이션 코드
cat << 'EOF' > app.py
from flask import Flask
app = Flask(__name__)

@app.route('/')
def hello():
    return 'Hello from Python!'

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
EOF

# requirements.txt
cat << 'EOF' > requirements.txt
flask==2.3.0
EOF

# Dockerfile
cat << 'EOF' > Dockerfile
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .

EXPOSE 5000

CMD ["python", "app.py"]
EOF

# 빌드 및 실행
docker build -t python-app:v1 .
docker run -d -p 5000:5000 --name pyapp python-app:v1
curl http://localhost:5000

# 정리
docker rm -f pyapp
```

---

### 실습 3: 멀티스테이지 빌드 (40분)

**문제**: 빌드 도구가 포함되면 이미지가 커짐
**해결**: 멀티스테이지 빌드로 최종 이미지 최소화

```bash
mkdir -p ~/docker-practice/multi-stage
cd ~/docker-practice/multi-stage

# Go 애플리케이션
cat << 'EOF' > main.go
package main

import (
    "fmt"
    "net/http"
)

func main() {
    http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
        fmt.Fprintf(w, "Hello from Go!")
    })
    http.ListenAndServe(":8080", nil)
}
EOF

# 멀티스테이지 Dockerfile
cat << 'EOF' > Dockerfile
# Stage 1: 빌드
FROM golang:1.21 AS builder

WORKDIR /app
COPY main.go .
RUN CGO_ENABLED=0 GOOS=linux go build -o server main.go

# Stage 2: 실행 (최소 이미지)
FROM alpine:3.18

WORKDIR /app
COPY --from=builder /app/server .

EXPOSE 8080
CMD ["./server"]
EOF

# 빌드 및 크기 비교
docker build -t go-app:v1 .
docker images | grep -E "go-app|golang"
# golang 이미지: ~800MB
# go-app 이미지: ~10MB (멀티스테이지 효과!)

# 실행 테스트
docker run -d -p 8080:8080 --name goapp go-app:v1
curl http://localhost:8080

# 정리
docker rm -f goapp
```

---

### 실습 4: Dockerfile 최적화 팁 (20분)

```dockerfile
# ❌ 나쁜 예
FROM ubuntu:22.04
RUN apt-get update
RUN apt-get install -y python3
RUN apt-get install -y python3-pip
COPY . /app

# ✅ 좋은 예
FROM ubuntu:22.04
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    && rm -rf /var/lib/apt/lists/*
COPY requirements.txt /app/
RUN pip install -r /app/requirements.txt
COPY . /app
```

**최적화 원칙**:
1. **RUN 명령어 합치기**: 레이어 수 감소
2. **캐시 활용**: 변경 적은 것을 먼저 COPY
3. **불필요한 파일 삭제**: apt 캐시 등
4. **slim/alpine 이미지 사용**: 베이스 이미지 최소화

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | 간단한 Dockerfile 작성 | ☐ |
| 2 | `docker build`로 이미지 빌드 | ☐ |
| 3 | 멀티스테이지 빌드 이해 | ☐ |
| 4 | 이미지 크기 최적화 원칙 이해 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
docker build -t <name>:<tag> .         # 빌드
docker build -t app:v1 -f Dockerfile.prod .  # 다른 파일 지정
docker images                          # 이미지 목록
docker history <image>                 # 레이어 확인
```

---

## ➡️ 다음 학습: Day 10

**주제**: Docker 네트워킹

