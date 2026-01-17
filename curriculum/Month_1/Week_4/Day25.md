# 📅 Day 25: 장애 시나리오 #3 - 네트워크/연결 문제

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "장애에 대한 단기 해결뿐만 아니라 재발 방지를 위한 원인 분석과 구조적 개선"
> 연결 문제와 TIME_WAIT 폭증 상황을 분석하고 해결

네트워크 연결 문제는 분산 시스템에서 가장 흔하면서도 진단이 어려운 장애입니다. 토스플레이스의 마이크로서비스 환경에서 서비스 간 통신 문제를 빠르게 진단하고 해결하는 능력이 필요합니다.

---

## ⏰ 예상 소요 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 이론 학습 | 45분 | TCP 연결 상태, TIME_WAIT |
| 시뮬레이션 | 1.5시간 | 연결 폭증, 의존성 장애 |
| 분석 | 1시간 | 원인 분석, 도구 활용 |
| 해결 및 문서화 | 45분 | 복구, 예방 조치 |

---

## 📚 Part 1: TCP 연결 상태 이해 (45분)

### 1.1 TCP 연결 상태 다이어그램

```
┌─────────────────────────────────────────────────────────────────────┐
│  TCP 연결 상태 전이 (State Transition)                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  [연결 수립]                                                        │
│                                                                      │
│  Client                         Server                               │
│  CLOSED                         CLOSED                               │
│    │                              │                                  │
│    │                           LISTEN (서버 대기)                    │
│    │                              │                                  │
│    ├── SYN ──────────────────────>│                                  │
│  SYN_SENT                         │                                  │
│    │<─────────────── SYN+ACK ────┤                                  │
│    │                           SYN_RCVD                              │
│    ├── ACK ──────────────────────>│                                  │
│  ESTABLISHED                   ESTABLISHED                          │
│    │                              │                                  │
│    │<───── 데이터 전송 ──────>    │                                  │
│                                                                      │
│  [연결 종료]                                                        │
│                                                                      │
│  ESTABLISHED                   ESTABLISHED                          │
│    │                              │                                  │
│    ├── FIN ──────────────────────>│                                  │
│  FIN_WAIT_1                       │                                  │
│    │<─────────────── ACK ────────┤                                  │
│  FIN_WAIT_2                    CLOSE_WAIT                           │
│    │<─────────────── FIN ────────┤                                  │
│    │                           LAST_ACK                              │
│    ├── ACK ──────────────────────>│                                  │
│  TIME_WAIT                     CLOSED                               │
│    │                                                                 │
│    │ (2MSL 대기)                                                    │
│    │                                                                 │
│  CLOSED                                                              │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 주요 상태 설명

| 상태 | 설명 | 발생 시점 |
|------|------|----------|
| **LISTEN** | 연결 대기 중 | 서버가 포트 열고 대기 |
| **SYN_SENT** | SYN 전송 후 응답 대기 | 클라이언트가 연결 시도 |
| **SYN_RCVD** | SYN 수신, SYN+ACK 전송 | 서버가 연결 요청 받음 |
| **ESTABLISHED** | 연결 수립됨 | 정상 통신 중 |
| **FIN_WAIT_1** | FIN 전송 후 ACK 대기 | 연결 종료 시작 |
| **FIN_WAIT_2** | ACK 수신, 상대방 FIN 대기 | 절반 종료 상태 |
| **CLOSE_WAIT** | FIN 수신, 앱 종료 대기 | 앱이 close() 호출 안 함 |
| **TIME_WAIT** | 마지막 ACK 전송 후 대기 | 2MSL(보통 60초) 대기 |
| **LAST_ACK** | FIN 전송 후 ACK 대기 | 마지막 확인 대기 |

### 1.3 TIME_WAIT 문제

```
┌─────────────────────────────────────────────────────────────────────┐
│  TIME_WAIT 상태 이해                                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  왜 TIME_WAIT가 필요한가?                                           │
│  1. 지연된 패킷 처리: 이전 연결의 패킷이 새 연결에 영향 방지        │
│  2. 마지막 ACK 재전송: 상대방이 FIN을 재전송할 경우 대응            │
│                                                                      │
│  TIME_WAIT 폭증 원인:                                               │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • 짧은 연결 + 많은 요청 (Connection Pool 미사용)           │    │
│  │  • Keep-Alive 미설정                                        │    │
│  │  • 서버에서 먼저 연결 종료 (active close)                   │    │
│  │  • 로드밸런서 health check 빈번                             │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  TIME_WAIT 폭증 영향:                                               │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • 포트 고갈: 소스 포트 부족으로 새 연결 불가               │    │
│  │  • 메모리 사용: 소켓 구조체 메모리 점유                     │    │
│  │  • 성능 저하: 커널의 소켓 테이블 탐색 지연                  │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  해결책:                                                            │
│  1. Connection Pool 사용 (연결 재사용)                              │
│  2. Keep-Alive 활성화 (HTTP/1.1)                                    │
│  3. tcp_tw_reuse 활성화 (커널 파라미터)                             │
│  4. 클라이언트에서 먼저 종료하도록 설계                             │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.4 CLOSE_WAIT 문제

```
┌─────────────────────────────────────────────────────────────────────┐
│  CLOSE_WAIT 문제 진단                                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  CLOSE_WAIT = 상대방은 종료했는데, 우리 앱이 close() 안 함          │
│                                                                      │
│  원인:                                                               │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • 애플리케이션 버그: 소켓 close() 누락                     │    │
│  │  • 리소스 누수: 소켓을 열고 닫지 않음                       │    │
│  │  • 데드락: 앱이 블로킹 상태로 close() 호출 못함             │    │
│  │  • 잘못된 예외 처리: 예외 발생 시 정리 코드 미실행          │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  진단:                                                               │
│  • CLOSE_WAIT가 계속 쌓이면 앱 코드 문제                           │
│  • 특정 서비스와의 연결에서만 발생하면 해당 서비스 확인             │
│                                                                      │
│  해결:                                                               │
│  • 코드 리뷰: try-finally, using 패턴으로 리소스 정리              │
│  • 타임아웃 설정: 무한 대기 방지                                    │
│  • Connection Pool의 idle timeout 설정                              │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: 장애 시뮬레이션 (1.5시간)

### Step 1: 현재 연결 상태 확인

```bash
cd ~/portfolio/month1-troubleshooting

# 전체 소켓 통계 요약
ss -s

# 출력 예시:
# Total: 150
# TCP:   80 (estab 45, closed 10, orphaned 0, timewait 20)

# 상태별 TCP 소켓 개수
ss -tan | awk 'NR>1 {count[$1]++} END {for(s in count) print s, count[s]}' | sort

# TIME_WAIT 개수만 확인
ss -tn state time-wait | wc -l

# CLOSE_WAIT 개수
ss -tn state close-wait | wc -l

# ESTABLISHED 연결 확인
ss -tn state established | head -20

# 특정 포트의 연결 상태
ss -tn | grep ":80\|:443" | head -20
```

### Step 2: 연결 폭증 시뮬레이션

```bash
# 테스트 환경 준비 (Docker Compose)
cat << 'EOF' > docker-compose.yml
version: '3.8'
services:
  app:
    image: nginx:alpine
    ports:
      - "8080:80"
    depends_on:
      - redis
    networks:
      - backend

  redis:
    image: redis:7-alpine
    networks:
      - backend

networks:
  backend:
    driver: bridge
EOF

docker-compose up -d

# 연결 풀 없이 많은 요청 (TIME_WAIT 유발)
echo "=== TIME_WAIT 폭증 시뮬레이션 ==="
echo "짧은 연결을 500번 생성..."

for i in {1..500}; do
    curl -s http://localhost:8080/ > /dev/null &
    
    # 100개마다 진행 상황 출력
    if [ $((i % 100)) -eq 0 ]; then
        echo "$i 요청 완료..."
    fi
done
wait

echo ""
echo "=== 소켓 상태 확인 ==="

# TIME_WAIT 확인
echo "TIME_WAIT 개수: $(ss -tn state time-wait | wc -l)"

# 전체 소켓 통계
ss -s

# 상세 상태
echo ""
echo "=== 상태별 소켓 개수 ==="
ss -tan | awk 'NR>1 {count[$1]++} END {for(s in count) print s, count[s]}' | sort -k2 -rn
```

### Step 3: Redis 연결 문제 시뮬레이션

```bash
echo "=== Redis 의존성 장애 시뮬레이션 ==="

# 앱에서 Redis를 사용하는 간단한 서비스 생성
cat << 'EOF' > app/server.py
from flask import Flask, jsonify
import redis
import os

app = Flask(__name__)

redis_host = os.environ.get('REDIS_HOST', 'redis')
redis_port = int(os.environ.get('REDIS_PORT', 6379))

@app.route('/')
def home():
    return jsonify({"status": "ok"})

@app.route('/count')
def count():
    try:
        r = redis.Redis(host=redis_host, port=redis_port, socket_timeout=5)
        count = r.incr('page_count')
        return jsonify({"count": count})
    except redis.exceptions.ConnectionError as e:
        return jsonify({"error": "Redis connection failed", "detail": str(e)}), 503

@app.route('/health')
def health():
    try:
        r = redis.Redis(host=redis_host, port=redis_port, socket_timeout=2)
        r.ping()
        return jsonify({"status": "healthy", "redis": "connected"})
    except redis.exceptions.ConnectionError:
        return jsonify({"status": "unhealthy", "redis": "disconnected"}), 503

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
EOF

# Dockerfile 생성
cat << 'EOF' > app/Dockerfile
FROM python:3.11-slim
WORKDIR /app
RUN pip install flask redis
COPY server.py .
CMD ["python", "server.py"]
EOF

# docker-compose.yml 업데이트
cat << 'EOF' > docker-compose.yml
version: '3.8'
services:
  app:
    build: ./app
    ports:
      - "5000:5000"
    environment:
      - REDIS_HOST=redis
    depends_on:
      - redis
    networks:
      - backend
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5000/health"]
      interval: 10s
      timeout: 5s
      retries: 3

  redis:
    image: redis:7-alpine
    networks:
      - backend
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 3

networks:
  backend:
    driver: bridge
EOF

mkdir -p app
docker-compose up -d --build

# 정상 동작 확인
echo "=== 정상 상태 테스트 ==="
curl http://localhost:5000/
curl http://localhost:5000/health
curl http://localhost:5000/count

# Redis 중지
echo ""
echo "=== Redis 중지 ==="
docker-compose stop redis

# 장애 상태 테스트
echo ""
echo "=== 장애 상태 테스트 ==="
echo "Homepage 요청:"
curl -w "\nHTTP Code: %{http_code}\n" http://localhost:5000/

echo ""
echo "Health 요청:"
curl -w "\nHTTP Code: %{http_code}\n" http://localhost:5000/health

echo ""
echo "Count 요청:"
curl -w "\nHTTP Code: %{http_code}\n" http://localhost:5000/count

# 앱 로그 확인
echo ""
echo "=== 앱 로그 ==="
docker-compose logs --tail 30 app
```

---

## 🔍 Part 3: 장애 분석 (1시간)

### Step 4: 네트워크 분석 도구

```bash
echo "=== 네트워크 분석 ==="

# 컨테이너 네트워크 확인
docker network ls
docker network inspect month1-troubleshooting_backend

# 컨테이너 IP 확인
docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' $(docker-compose ps -q app)

# 앱 컨테이너에서 네트워크 도구 설치 및 테스트
docker-compose exec app sh -c "apt-get update && apt-get install -y netcat-openbsd iputils-ping curl" 2>/dev/null || true

# DNS 확인
docker-compose exec app sh -c "getent hosts redis"

# Redis로 연결 테스트
echo ""
echo "=== Redis 연결 테스트 ==="
docker-compose exec app sh -c "nc -zv redis 6379" 2>&1

# ping 테스트
docker-compose exec app sh -c "ping -c 3 redis" 2>&1

# TCP 연결 상태 확인 (컨테이너 내부)
echo ""
echo "=== 컨테이너 내 TCP 상태 ==="
docker-compose exec app sh -c "cat /proc/net/tcp" 2>/dev/null | head -10
```

### Step 5: 상세 분석

```bash
# tcpdump로 패킷 캡처 (선택, 권한 필요)
echo "=== 패킷 분석 (tcpdump) ==="

# 호스트에서 Redis 포트 트래픽 캡처
sudo timeout 10 tcpdump -i any port 6379 -c 20 -nn 2>/dev/null || \
  echo "tcpdump 권한 없음, 스킵"

# ss로 상세 연결 정보
echo ""
echo "=== 상세 소켓 정보 ==="
ss -tnp | grep -E "5000|6379" | head -20

# 연결 타이머 정보
echo ""
echo "=== 연결 타이머 ==="
ss -tno state established | head -10

# netstat 대체 (레거시 환경)
echo ""
echo "=== 연결 상태 요약 ==="
cat /proc/net/sockstat
```

### Step 6: 분석 리포트 작성

```
┌─────────────────────────────────────────────────────────────────────┐
│  장애 분석 리포트 #3: 네트워크/연결 문제                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  발생 시각: YYYY-MM-DD HH:MM:SS                                     │
│  영향 범위: App 서비스 전체                                         │
│  심각도: Critical                                                   │
│                                                                      │
│  ═══════════════════════════════════════════════════════════════   │
│                                                                      │
│  증상 A: TIME_WAIT 폭증                                             │
│  ──────────────────────────────────────────────────────────────    │
│  • 현상: 짧은 시간에 TIME_WAIT 소켓 수백 개 생성                   │
│  • 측정: ss -tn state time-wait | wc -l → 450개                    │
│  • 영향: 포트 고갈 위험, 새 연결 실패 가능                         │
│                                                                      │
│  원인 분석:                                                         │
│  1. Connection Pool 미사용                                          │
│  2. 매 요청마다 새 TCP 연결 생성                                   │
│  3. Keep-Alive 비활성화                                            │
│                                                                      │
│  해결책:                                                            │
│  • Connection Pool 적용                                             │
│  • HTTP Keep-Alive 활성화                                          │
│  • 커널 파라미터 튜닝 (net.ipv4.tcp_tw_reuse=1)                    │
│                                                                      │
│  ═══════════════════════════════════════════════════════════════   │
│                                                                      │
│  증상 B: Redis 연결 실패                                            │
│  ──────────────────────────────────────────────────────────────    │
│  • 현상: /count, /health 엔드포인트 503 반환                       │
│  • 에러: ConnectionError: Error connecting to redis                │
│  • 영향: 페이지 카운터 기능 불가, 헬스체크 실패                    │
│                                                                      │
│  진단 과정:                                                         │
│  1. docker-compose logs app → ConnectionError 확인                 │
│  2. nc -zv redis 6379 → Connection refused                         │
│  3. docker-compose ps → redis 컨테이너 Exited                      │
│                                                                      │
│  근본 원인: Redis 컨테이너 다운                                     │
│                                                                      │
│  해결: docker-compose start redis                                   │
│                                                                      │
│  ═══════════════════════════════════════════════════════════════   │
│                                                                      │
│  재발 방지 대책:                                                    │
│  ──────────────────────────────────────────────────────────────    │
│                                                                      │
│  단기:                                                              │
│  • Redis 헬스체크 설정                                             │
│  • 연결 재시도 로직 추가                                           │
│  • 타임아웃 설정 (socket_timeout=5)                                │
│                                                                      │
│  중장기:                                                            │
│  • Circuit Breaker 패턴 적용                                       │
│  • Redis Sentinel/Cluster 구성 (HA)                                │
│  • Graceful Degradation 구현                                       │
│  • 의존성 모니터링 Alert 설정                                      │
│                                                                      │
│  ═══════════════════════════════════════════════════════════════   │
│                                                                      │
│  교훈:                                                              │
│  • 외부 의존성 장애를 가정한 설계 필요                             │
│  • 헬스체크는 의존성 상태도 반영해야 함                            │
│  • 장애 격리 (Bulkhead) 패턴 검토                                  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🔧 Part 4: 해결 및 예방 조치 (45분)

### Step 7: Redis 복구 및 검증

```bash
echo "=== Redis 복구 ==="

# Redis 재시작
docker-compose start redis

# 연결 대기
echo "Redis 시작 대기 (5초)..."
sleep 5

# 연결 테스트
echo ""
echo "=== 연결 테스트 ==="
docker-compose exec app sh -c "nc -zv redis 6379" 2>&1

# 헬스체크
echo ""
echo "=== 헬스체크 ==="
curl -w "\nHTTP Code: %{http_code}\n" http://localhost:5000/health

# 기능 테스트
echo ""
echo "=== 기능 테스트 ==="
for i in {1..5}; do
    result=$(curl -s http://localhost:5000/count)
    echo "요청 $i: $result"
done
```

### Step 8: 헬스체크 개선

```yaml
# docker-compose.yml 개선 버전
version: '3.8'
services:
  app:
    build: ./app
    ports:
      - "5000:5000"
    environment:
      - REDIS_HOST=redis
      - REDIS_PORT=6379
    depends_on:
      redis:
        condition: service_healthy
    networks:
      - backend
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5000/health"]
      interval: 10s
      timeout: 5s
      retries: 3
      start_period: 10s
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    networks:
      - backend
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 3
    restart: unless-stopped
    volumes:
      - redis_data:/data

networks:
  backend:
    driver: bridge

volumes:
  redis_data:
```

### Step 9: 연결 재시도 로직 추가

```python
# app/server_improved.py - 개선된 버전
from flask import Flask, jsonify
import redis
from redis.exceptions import ConnectionError, TimeoutError
import os
import time
from functools import wraps

app = Flask(__name__)

# 환경 변수
redis_host = os.environ.get('REDIS_HOST', 'redis')
redis_port = int(os.environ.get('REDIS_PORT', 6379))

# Connection Pool 사용
redis_pool = redis.ConnectionPool(
    host=redis_host,
    port=redis_port,
    max_connections=10,
    socket_timeout=5,
    socket_connect_timeout=5,
    retry_on_timeout=True
)

def get_redis():
    """Redis 연결 풀에서 연결 가져오기"""
    return redis.Redis(connection_pool=redis_pool)

def retry_on_failure(max_retries=3, delay=0.5):
    """재시도 데코레이터"""
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            last_exception = None
            for attempt in range(max_retries):
                try:
                    return func(*args, **kwargs)
                except (ConnectionError, TimeoutError) as e:
                    last_exception = e
                    if attempt < max_retries - 1:
                        time.sleep(delay * (attempt + 1))  # 지수 백오프
            raise last_exception
        return wrapper
    return decorator

@app.route('/')
def home():
    return jsonify({
        "status": "ok",
        "service": "demo-app",
        "version": "1.0.0"
    })

@app.route('/count')
@retry_on_failure(max_retries=3, delay=0.5)
def count():
    try:
        r = get_redis()
        count = r.incr('page_count')
        return jsonify({
            "count": count,
            "redis": "connected"
        })
    except (ConnectionError, TimeoutError) as e:
        app.logger.error(f"Redis connection failed: {e}")
        return jsonify({
            "error": "Redis connection failed",
            "detail": str(e),
            "fallback": True
        }), 503

@app.route('/health')
def health():
    health_status = {
        "status": "healthy",
        "checks": {}
    }
    overall_healthy = True
    
    # Redis 체크
    try:
        r = get_redis()
        r.ping()
        health_status["checks"]["redis"] = {
            "status": "up",
            "response_time_ms": 0  # 실제로는 측정
        }
    except (ConnectionError, TimeoutError) as e:
        health_status["checks"]["redis"] = {
            "status": "down",
            "error": str(e)
        }
        overall_healthy = False
    
    if not overall_healthy:
        health_status["status"] = "unhealthy"
        return jsonify(health_status), 503
    
    return jsonify(health_status)

@app.route('/ready')
def ready():
    """Kubernetes readiness probe용"""
    try:
        r = get_redis()
        r.ping()
        return jsonify({"ready": True})
    except:
        return jsonify({"ready": False}), 503

@app.route('/live')
def live():
    """Kubernetes liveness probe용"""
    return jsonify({"alive": True})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=False)
```

### Step 10: 커널 파라미터 튜닝

```bash
# TIME_WAIT 관련 커널 파라미터 확인
echo "=== 현재 커널 파라미터 ==="
sysctl net.ipv4.tcp_tw_reuse
sysctl net.ipv4.tcp_fin_timeout
sysctl net.ipv4.tcp_max_tw_buckets
sysctl net.core.somaxconn

# 권장 설정 (프로덕션 환경)
cat << 'EOF'
# /etc/sysctl.d/99-tcp-tuning.conf

# TIME_WAIT 소켓 재사용 (클라이언트 측)
net.ipv4.tcp_tw_reuse = 1

# FIN_WAIT 타임아웃 줄이기 (기본 60초)
net.ipv4.tcp_fin_timeout = 30

# TIME_WAIT 최대 개수
net.ipv4.tcp_max_tw_buckets = 65536

# 연결 백로그 (SYN 대기열)
net.core.somaxconn = 65535
net.ipv4.tcp_max_syn_backlog = 65535

# Keep-Alive 설정
net.ipv4.tcp_keepalive_time = 600
net.ipv4.tcp_keepalive_intvl = 60
net.ipv4.tcp_keepalive_probes = 3
EOF

# 적용 (테스트 환경에서만)
# sudo sysctl -p /etc/sysctl.d/99-tcp-tuning.conf
```

---

## 📊 Part 5: 토스플레이스 관점의 연결 문제

### 5.1 Kubernetes에서의 연결 문제

```bash
# Pod 간 통신 테스트
kubectl exec -it <pod-name> -- curl http://<service-name>:<port>/health

# Service 엔드포인트 확인
kubectl get endpoints <service-name>

# DNS 해석 확인
kubectl exec -it <pod-name> -- nslookup <service-name>

# 네트워크 정책 확인
kubectl get networkpolicy -A

# Istio sidecar 로그 (서비스 메시 환경)
kubectl logs <pod-name> -c istio-proxy | tail -50
```

### 5.2 서비스 메시에서의 연결 문제

```bash
# Istio destination rule 확인
kubectl get destinationrule -A

# Circuit Breaker 설정 확인
kubectl get destinationrule <name> -o yaml | grep -A 20 "trafficPolicy"

# 연결 풀 설정
cat << 'EOF'
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: redis-connection-pool
spec:
  host: redis.default.svc.cluster.local
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
        connectTimeout: 5s
      http:
        h2UpgradePolicy: UPGRADE
        http1MaxPendingRequests: 1024
        http2MaxRequests: 1024
    outlierDetection:
      consecutive5xxErrors: 5
      interval: 30s
      baseEjectionTime: 30s
      maxEjectionPercent: 100
EOF
```

### 5.3 모니터링 메트릭

```promql
# TIME_WAIT 소켓 수 (node-exporter)
node_sockstat_TCP_tw

# CLOSE_WAIT 소켓 수
node_sockstat_TCP_closewait

# 전체 소켓 사용량
node_sockstat_sockets_used

# 연결 실패율 (Istio)
sum(rate(istio_tcp_connections_closed_total{reporter="source"}[5m])) by (destination_service)

# 서비스 간 연결 지연
histogram_quantile(0.99, sum(rate(istio_request_duration_milliseconds_bucket[5m])) by (le, destination_service))
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | TCP 상태 이해 | TIME_WAIT, CLOSE_WAIT | ☐ |
| 2 | TIME_WAIT 상태 확인 | ss -tn state time-wait | ☐ |
| 3 | 연결 폭증 시뮬레이션 | 500개 연결 생성 | ☐ |
| 4 | 의존성(Redis) 장애 시뮬레이션 | docker-compose stop | ☐ |
| 5 | ss, nc로 연결 분석 | 네트워크 진단 | ☐ |
| 6 | 분석 리포트 작성 | 원인/해결/예방 | ☐ |
| 7 | 헬스체크 추가 | depends_on + healthcheck | ☐ |
| 8 | 연결 재시도 로직 | retry_on_failure | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# 소켓 상태 확인
ss -s                           # 전체 요약
ss -tn state time-wait          # TIME_WAIT만
ss -tn state close-wait         # CLOSE_WAIT만
ss -tnp                         # 프로세스 포함

# 연결 테스트
nc -zv <host> <port>            # TCP 연결 테스트
telnet <host> <port>            # 대화형 연결

# Docker 네트워크
docker network inspect <network>
docker-compose logs --tail 30 <service>

# 커널 파라미터
sysctl net.ipv4.tcp_tw_reuse
sysctl net.ipv4.tcp_fin_timeout
```

---

## 💡 면접 대비 핵심 포인트

### Q1: TIME_WAIT가 폭증하면 어떤 문제가 발생하나요?
**A**: 
1. **포트 고갈**: 클라이언트 측에서 소스 포트가 부족해져 새 연결 불가
2. **메모리 사용**: 소켓 구조체가 메모리 점유
3. **성능 저하**: 커널의 소켓 테이블 탐색 시간 증가

해결책: Connection Pool 사용, Keep-Alive 활성화, tcp_tw_reuse 설정

### Q2: CLOSE_WAIT가 쌓이는 원인은?
**A**: CLOSE_WAIT는 상대방이 FIN을 보냈는데 우리 애플리케이션이 close()를 호출하지 않은 상태입니다.
- 원인: 소켓 close() 누락, 리소스 누수, 데드락, 잘못된 예외 처리
- 해결: 코드 리뷰, try-finally로 리소스 정리, 타임아웃 설정

### Q3: 마이크로서비스에서 의존성 장애를 어떻게 처리하나요?
**A**: 
1. **타임아웃**: 무한 대기 방지 (socket_timeout)
2. **재시도**: 일시적 오류 복구 (지수 백오프)
3. **Circuit Breaker**: 장애 전파 방지
4. **Fallback**: 대체 응답 제공
5. **Bulkhead**: 리소스 격리

---

## 📝 학습 기록

```
학습일: ____년 __월 __일
실제 소요 시간: ____시간

시뮬레이션 결과:
- TIME_WAIT 개수: ____개
- Redis 장애 시 응답: ____

이해가 어려웠던 부분:


추가 학습 필요 항목:

```

---

## ➡️ 다음: Day 26

**주제**: 장애 분석 종합 정리 및 문서화
- 3가지 장애 시나리오 회고
- 포트폴리오용 문서 작성
- Month 1 종합 정리
