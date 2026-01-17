# Day 20: Load Balancing 개념과 실습

## 오늘의 목표

토스플레이스 연결점: "대규모의 실시간 트래픽을 처리하는 인프라의 운영 경험"
"Network 등 다양한 레이어에서의 모니터링, 트러블슈팅 경험"

Load Balancing의 핵심 개념과 알고리즘을 이해합니다. Nginx를 사용하여 실제 로드 밸런서를 구성하고 동작을 확인합니다.

---

## 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 핵심 개념 | 1시간 | LB 종류, 알고리즘 |
| Nginx LB 실습 | 1.5시간 | 로드 밸런서 구성 |
| 헬스 체크 | 1시간 | 장애 감지, 제외 |
| 트러블슈팅 | 30분 | 일반적인 문제 |

---

## Part 1: Load Balancing 개념 (1시간)

### 1.1 왜 Load Balancing이 필요한가?

```
단일 서버의 한계:

Client ───────────────────► Server
                              │
                         트래픽 증가
                              │
                              ▼
                         응답 지연
                         서버 다운
                         서비스 중단

Load Balancing 도입:

                         ┌─────────┐
              ┌─────────►│ Server1 │
              │          └─────────┘
              │          ┌─────────┐
Client ─► LB ─┼─────────►│ Server2 │
              │          └─────────┘
              │          ┌─────────┐
              └─────────►│ Server3 │
                         └─────────┘

장점:
- 트래픽 분산
- 고가용성 (서버 하나 장애 시 다른 서버 사용)
- 수평 확장 용이
- 유지보수 시 무중단
```

### 1.2 Load Balancer 종류

```
L4 Load Balancer (Transport Layer):
- TCP/UDP 레벨에서 동작
- IP + Port 기반 라우팅
- 빠른 처리 속도
- 예: AWS NLB, HAProxy (TCP 모드)

L7 Load Balancer (Application Layer):
- HTTP/HTTPS 레벨에서 동작
- URL, 헤더, 쿠키 기반 라우팅
- SSL 종료, 캐싱 가능
- 예: AWS ALB, Nginx, HAProxy (HTTP 모드)

비교:
┌─────────────────┬─────────────────┬─────────────────┐
│     항목        │       L4        │       L7        │
├─────────────────┼─────────────────┼─────────────────┤
│ 동작 레이어     │ TCP/UDP         │ HTTP/HTTPS      │
│ 라우팅 기준     │ IP:Port         │ URL, Header     │
│ 성능            │ 매우 빠름       │ 빠름            │
│ 기능            │ 단순            │ 풍부            │
│ SSL 종료        │ 불가            │ 가능            │
│ 콘텐츠 기반     │ 불가            │ 가능            │
└─────────────────┴─────────────────┴─────────────────┘
```

### 1.3 Load Balancing 알고리즘

| 알고리즘 | 설명 | 사용 사례 |
|---------|------|----------|
| **Round Robin** | 순차적으로 분배 | 서버 성능 동일 시 |
| **Weighted RR** | 가중치 기반 분배 | 서버 성능 다를 때 |
| **Least Connections** | 연결 수 적은 곳으로 | 처리 시간 다양할 때 |
| **IP Hash** | 클라이언트 IP 기반 | 세션 유지 필요 시 |
| **Random** | 무작위 선택 | 간단한 분산 |

```
Round Robin:
Request 1 → Server A
Request 2 → Server B
Request 3 → Server C
Request 4 → Server A (반복)

Weighted Round Robin (A:3, B:2, C:1):
Request 1,2,3 → Server A
Request 4,5   → Server B
Request 6     → Server C
Request 7,8,9 → Server A (반복)

Least Connections:
서버 현재 연결: A(5), B(3), C(2)
새 요청 → Server C (가장 적음)
```

---

## Part 2: Nginx Load Balancer 실습 (1.5시간)

### 실습 1: 환경 구성

```bash
mkdir -p ~/lb-demo && cd ~/lb-demo

# Docker Compose로 웹 서버 3개 + LB 구성
cat << 'EOF' > docker-compose.yml
services:
  # 웹 서버 1
  web1:
    image: nginx:alpine
    volumes:
      - ./web1:/usr/share/nginx/html:ro

  # 웹 서버 2
  web2:
    image: nginx:alpine
    volumes:
      - ./web2:/usr/share/nginx/html:ro

  # 웹 서버 3
  web3:
    image: nginx:alpine
    volumes:
      - ./web3:/usr/share/nginx/html:ro

  # Load Balancer
  lb:
    image: nginx:alpine
    ports:
      - "8080:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - web1
      - web2
      - web3
EOF

# 각 서버별 HTML 생성
mkdir -p web1 web2 web3
echo "<h1>Server 1</h1>" > web1/index.html
echo "<h1>Server 2</h1>" > web2/index.html
echo "<h1>Server 3</h1>" > web3/index.html
```

### 실습 2: Round Robin 설정

```bash
cat << 'EOF' > nginx.conf
events {
    worker_connections 1024;
}

http {
    upstream backend {
        # Round Robin (기본값)
        server web1:80;
        server web2:80;
        server web3:80;
    }

    server {
        listen 80;

        location / {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }
}
EOF

# 실행
docker-compose up -d

# 테스트 (순차적으로 분배됨)
for i in {1..6}; do
  curl -s http://localhost:8080 | grep -o "Server [0-9]"
done
```

### 실습 3: Weighted Round Robin

```bash
cat << 'EOF' > nginx.conf
events {
    worker_connections 1024;
}

http {
    upstream backend {
        # Weighted Round Robin
        server web1:80 weight=3;  # 3배 더 많이
        server web2:80 weight=2;  # 2배 더 많이
        server web3:80 weight=1;  # 기본
    }

    server {
        listen 80;

        location / {
            proxy_pass http://backend;
            proxy_set_header Host $host;
        }
    }
}
EOF

docker-compose restart lb

# 테스트 (가중치에 따라 분배)
for i in {1..12}; do
  curl -s http://localhost:8080 | grep -o "Server [0-9]"
done | sort | uniq -c
```

### 실습 4: Least Connections

```bash
cat << 'EOF' > nginx.conf
events {
    worker_connections 1024;
}

http {
    upstream backend {
        least_conn;
        server web1:80;
        server web2:80;
        server web3:80;
    }

    server {
        listen 80;

        location / {
            proxy_pass http://backend;
        }
    }
}
EOF

docker-compose restart lb

# 테스트
for i in {1..9}; do
  curl -s http://localhost:8080 | grep -o "Server [0-9]"
done
```

### 실습 5: IP Hash

```bash
cat << 'EOF' > nginx.conf
events {
    worker_connections 1024;
}

http {
    upstream backend {
        ip_hash;  # 같은 IP는 같은 서버로
        server web1:80;
        server web2:80;
        server web3:80;
    }

    server {
        listen 80;

        location / {
            proxy_pass http://backend;
        }
    }
}
EOF

docker-compose restart lb

# 테스트 (같은 클라이언트는 같은 서버로)
for i in {1..5}; do
  curl -s http://localhost:8080 | grep -o "Server [0-9]"
done
# 모두 같은 서버로 라우팅됨
```

---

## Part 3: 헬스 체크 (1시간)

### 실습 6: Passive Health Check

```bash
cat << 'EOF' > nginx.conf
events {
    worker_connections 1024;
}

http {
    upstream backend {
        server web1:80 max_fails=3 fail_timeout=30s;
        server web2:80 max_fails=3 fail_timeout=30s;
        server web3:80 max_fails=3 fail_timeout=30s;
    }

    server {
        listen 80;

        location / {
            proxy_pass http://backend;
            proxy_connect_timeout 5s;
            proxy_read_timeout 10s;
        }
    }
}
EOF

docker-compose restart lb

# 서버 하나 중지
docker-compose stop web2

# 테스트 (web2 제외됨)
for i in {1..6}; do
  curl -s http://localhost:8080 | grep -o "Server [0-9]"
done

# 복구
docker-compose start web2
```

### 실습 7: 헬스 체크 엔드포인트

```bash
# 각 서버에 health 엔드포인트 추가
echo "OK" > web1/health
echo "OK" > web2/health
echo "OK" > web3/health

cat << 'EOF' > nginx.conf
events {
    worker_connections 1024;
}

http {
    upstream backend {
        server web1:80;
        server web2:80;
        server web3:80;
    }

    server {
        listen 80;

        location / {
            proxy_pass http://backend;
        }

        # LB 자체 헬스 체크
        location /lb-health {
            return 200 'LB OK';
            add_header Content-Type text/plain;
        }
    }
}
EOF

docker-compose restart lb

# 헬스 체크 테스트
curl http://localhost:8080/lb-health
curl http://localhost:8080/health
```

### 실습 8: 백업 서버 설정

```bash
cat << 'EOF' > nginx.conf
events {
    worker_connections 1024;
}

http {
    upstream backend {
        server web1:80;
        server web2:80;
        server web3:80 backup;  # 메인 서버 모두 다운 시에만 사용
    }

    server {
        listen 80;

        location / {
            proxy_pass http://backend;
        }
    }
}
EOF

docker-compose restart lb

# 테스트 (web3은 backup이므로 사용 안됨)
for i in {1..4}; do
  curl -s http://localhost:8080 | grep -o "Server [0-9]"
done

# web1, web2 중지
docker-compose stop web1 web2

# 이제 web3 (backup)이 사용됨
curl -s http://localhost:8080 | grep -o "Server [0-9]"

# 복구
docker-compose start web1 web2
```

---

## Part 4: 트러블슈팅 (30분)

### 실습 9: 일반적인 LB 문제

```bash
# 문제 1: 502 Bad Gateway
# 원인: 백엔드 서버 다운 또는 응답 없음
# 진단:
docker-compose logs lb
curl -v http://localhost:8080

# 문제 2: 503 Service Unavailable
# 원인: 모든 백엔드 서버 다운
# 진단:
docker-compose ps
docker-compose exec lb nginx -t

# 문제 3: 불균형 분배
# 원인: Keepalive 연결, IP Hash 사용
# 진단:
# 요청별로 어느 서버로 갔는지 로깅 추가

cat << 'EOF' > nginx.conf
events {
    worker_connections 1024;
}

http {
    log_format upstream '$remote_addr - $upstream_addr - $request';
    
    upstream backend {
        server web1:80;
        server web2:80;
        server web3:80;
    }

    server {
        listen 80;
        access_log /var/log/nginx/access.log upstream;

        location / {
            proxy_pass http://backend;
            add_header X-Upstream-Server $upstream_addr;
        }
    }
}
EOF

docker-compose restart lb

# 응답 헤더에서 실제 서버 확인
curl -v http://localhost:8080 2>&1 | grep X-Upstream
```

---

## 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | LB 필요성 이해 | 트래픽 분산, 고가용성 | |
| 2 | L4 vs L7 이해 | 동작 레이어 차이 | |
| 3 | 알고리즘 이해 | RR, WRR, LC, IP Hash | |
| 4 | Nginx upstream 설정 | Round Robin 실습 | |
| 5 | Weighted RR 실습 | 가중치 설정 | |
| 6 | Least Connections | 연결 수 기반 | |
| 7 | IP Hash | 세션 유지 | |
| 8 | 헬스 체크 | max_fails, backup | |

---

## 핵심 Nginx 설정

```nginx
upstream backend {
    # 알고리즘
    # (기본값: Round Robin)
    # least_conn;
    # ip_hash;
    
    # 서버 목록
    server web1:80 weight=3;
    server web2:80 weight=2;
    server web3:80 backup;
    
    # 헬스 체크
    server web4:80 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    
    location / {
        proxy_pass http://backend;
        proxy_connect_timeout 5s;
        proxy_read_timeout 10s;
    }
}
```

---

## 면접 대비

**Q: L4와 L7 로드 밸런서의 차이는?**
> "L4는 TCP/UDP 레벨에서 IP와 포트 기반으로 라우팅하고, L7은 HTTP 레벨에서 URL, 헤더, 쿠키 기반으로 라우팅합니다. L7이 더 많은 기능을 제공하지만 L4가 성능이 좋습니다."

**Q: 세션을 유지하면서 로드 밸런싱하려면?**
> "IP Hash를 사용하거나, Sticky Session(쿠키 기반)을 설정합니다. 다만 이는 부하 분산 효율을 떨어뜨릴 수 있어 무상태(Stateless) 설계가 권장됩니다."

---

## 정리

```bash
cd ~/lb-demo
docker-compose down
cd ~
rm -rf ~/lb-demo
```

---

## 다음 학습: Day 21

주제: Week 1-3 종합 복습
- 트러블슈팅 도구 정리
- 면접 대비
