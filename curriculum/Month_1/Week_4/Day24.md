# 📅 Day 24: 장애 시나리오 #2 - CPU 병목 분석

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "운영장애 대응 및 복구 프로세스"
> CPU 집약적 작업으로 인한 성능 저하를 분석하고 해결

토스플레이스에서 요구하는 "장애에 대한 단기 해결뿐만 아니라 재발 방지를 위한 원인 분석과 구조적 개선" 능력을 키웁니다.

---

## ⏰ 예상 소요 시간: 4시간

---

## 📚 Part 1: CPU 성능 기초 (1시간)

### 1.1 CPU 관련 지표 이해

```
┌─────────────────────────────────────────────────────────────────────┐
│  CPU 성능 지표                                                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  top 명령어 출력:                                                   │
│  %Cpu(s):  5.2 us,  1.3 sy,  0.0 ni, 93.0 id,  0.3 wa,  0.2 si    │
│           │        │        │        │        │        │           │
│           │        │        │        │        │        └─ si: 소프트│
│           │        │        │        │        │           인터럽트   │
│           │        │        │        │        └─ wa: I/O 대기       │
│           │        │        │        └─ id: Idle (유휴)             │
│           │        │        └─ ni: nice (우선순위 조정된)           │
│           │        └─ sy: system (커널 영역)                        │
│           └─ us: user (사용자 영역)                                 │
│                                                                      │
│  주요 지표:                                                         │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  us 높음: 애플리케이션 CPU 사용 많음                        │    │
│  │  sy 높음: 시스템 콜, 컨텍스트 스위칭 많음                   │    │
│  │  wa 높음: 디스크 I/O 대기 (병목)                            │    │
│  │  id 낮음: CPU가 바쁨                                        │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 CPU 병목의 원인

| 원인 | 증상 | 확인 방법 |
|------|------|----------|
| 무한 루프 | CPU 100%, 단일 코어 | strace, 프로파일러 |
| 동기 작업 과다 | 응답 지연, 큐 증가 | 애플리케이션 로그 |
| 컨텍스트 스위칭 | sy 증가 | vmstat |
| 비효율적 알고리즘 | 특정 요청에서만 느림 | 프로파일링 |
| Worker 부족 | 처리량 한계 | 동시 접속 수 |

### 1.3 CPU 모니터링 도구

```bash
# 실시간 CPU 사용률
top
htop

# CPU 통계 (1초 간격, 5회)
vmstat 1 5

# 출력 해석:
#  procs -----------memory---------- ---swap-- -----io---- -system-- ------cpu-----
#  r  b   swpd   free   buff  cache   si   so    bi    bo   in   cs us sy id wa st
#  1  0      0 123456  12345 123456    0    0     0     0  100  200  5  1 94  0  0
#
# r: 실행 대기 프로세스 수 (높으면 CPU 병목)
# b: 블록된 프로세스 수 (높으면 I/O 병목)
# cs: 컨텍스트 스위칭 수

# CPU별 사용률
mpstat -P ALL 1

# 프로세스별 CPU
ps aux --sort=-%cpu | head -10
```

---

## 🛠️ Part 2: 장애 시뮬레이션 (1.5시간)

### Step 1: 테스트 환경 확인

```bash
# 프로젝트 디렉토리로 이동
cd ~/portfolio/month1-troubleshooting

# 서비스 상태 확인
docker-compose ps

# 서비스가 없으면 시작
docker-compose up -d
```

### Step 2: 정상 응답 시간 측정

```bash
# 터미널 1: 정상 엔드포인트 응답 시간 측정
echo "=== 정상 응답 시간 ===" 
for i in {1..5}; do
    start=$(date +%s.%N)
    curl -s http://localhost/ > /dev/null
    end=$(date +%s.%N)
    echo "Request $i: $(echo "$end - $start" | bc) seconds"
done

# 예상 결과: 대부분 0.01초 미만
```

### Step 3: CPU 부하 시뮬레이션

```bash
# 터미널 1: 리소스 모니터링
docker stats

# 터미널 2: CPU 집약적 요청 발생
echo "=== CPU 부하 테스트 ===" 
for i in {1..5}; do
    curl http://localhost/cpu &
done
wait

# CPU 사용률 급증 확인 (docker stats에서)
```

### Step 4: 시스템 상태 분석

```bash
# 호스트 CPU 사용률
top -b -n 1 | head -20

# vmstat으로 상세 분석
vmstat 1 10

# 주목할 지표:
# - r 칼럼: 실행 대기 프로세스 수 (높으면 CPU 포화)
# - us 칼럼: 사용자 CPU (애플리케이션)
# - sy 칼럼: 시스템 CPU (커널)

# 프로세스별 CPU 사용률
ps aux --sort=-%cpu | head -10

# 특정 컨테이너 상세 분석
docker top month1-troubleshooting_app_1
```

---

## 🔍 Part 3: 상세 분석 (1시간)

### Step 5: strace를 이용한 시스템 콜 분석

```bash
# 앱 컨테이너의 메인 프로세스 PID 찾기
CONTAINER_PID=$(docker inspect --format '{{.State.Pid}}' month1-troubleshooting_app_1)
echo "Container PID: $CONTAINER_PID"

# 시스템 콜 통계 (5초간)
echo "=== 시스템 콜 분석 ===" 
sudo timeout 5 strace -c -p $CONTAINER_PID 2>&1

# 예상 결과:
# CPU 작업 중에는 syscall이 적음 (순수 계산)
# I/O 작업 중에는 read/write syscall 많음
```

### Step 6: CPU 요청 중 분석

```bash
# 터미널 1: strace 실행 (출력 관찰)
CONTAINER_PID=$(docker inspect --format '{{.State.Pid}}' month1-troubleshooting_app_1)
sudo strace -c -p $CONTAINER_PID

# 터미널 2: CPU 집약적 요청 발생
curl http://localhost/cpu

# 터미널 1: Ctrl+C로 종료하고 결과 확인

# 분석 포인트:
# - calls 수가 적음 = 순수 CPU 연산
# - read/write가 많음 = I/O 작업
# - futex 많음 = 락 경쟁
```

### Step 7: 컨테이너 내부 분석

```bash
# 컨테이너 내부 프로세스 확인
docker exec -it month1-troubleshooting_app_1 sh -c "ps aux"

# Python 프로파일링 (Flask 앱인 경우)
# 앱 코드에 프로파일러 추가 필요
```

---

## 📊 Part 4: 장애 분석 리포트 (30분)

### Step 8: 분석 결과 정리

```
┌─────────────────────────────────────────────────────────────────────┐
│  장애 분석 리포트 #2: CPU 병목                                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. 장애 개요                                                       │
│  ───────────────────────────────────────────────────────────────    │
│  - 발생 시간: 20XX-XX-XX HH:MM                                     │
│  - 영향 범위: API 서버 전체                                         │
│  - 증상: 응답 시간 급증 (평소 10ms → 5초)                          │
│                                                                      │
│  2. 탐지 과정                                                       │
│  ───────────────────────────────────────────────────────────────    │
│  - 모니터링 알림: P95 latency > 3초                                │
│  - 사용자 신고: "서비스가 느려요"                                   │
│                                                                      │
│  3. 분석 과정                                                       │
│  ───────────────────────────────────────────────────────────────    │
│  Step 1: docker stats → CPU 100%                                   │
│  Step 2: vmstat → %us(user) 높음, %wa(wait) 정상                   │
│  Step 3: ps aux --sort=-%cpu → app 프로세스                        │
│  Step 4: strace -c → syscall 적음 (순수 CPU)                       │
│                                                                      │
│  4. 근본 원인 (Root Cause)                                         │
│  ───────────────────────────────────────────────────────────────    │
│  - /cpu 엔드포인트의 동기적 계산 작업                               │
│  - 단일 Worker가 요청을 블로킹                                      │
│  - 다른 요청도 대기 상태로 전환                                     │
│                                                                      │
│  5. 임시 조치 (Mitigation)                                         │
│  ───────────────────────────────────────────────────────────────    │
│  - 서비스 재시작                                                    │
│  - 문제 엔드포인트 임시 비활성화                                    │
│                                                                      │
│  6. 영구 해결책 (Resolution)                                       │
│  ───────────────────────────────────────────────────────────────    │
│  - 비동기 처리 (Celery + Redis)                                    │
│  - Worker 수 증가 (gunicorn --workers 4)                           │
│  - 타임아웃 설정 (30초)                                            │
│  - Rate Limiting (/cpu 엔드포인트)                                 │
│                                                                      │
│  7. 재발 방지책                                                     │
│  ───────────────────────────────────────────────────────────────    │
│  - CPU 사용률 알림 추가 (80% 경고, 95% 위험)                       │
│  - 응답 시간 모니터링 강화                                          │
│  - 부하 테스트 정기 실행                                            │
│  - 코드 리뷰에 성능 항목 추가                                       │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🔧 Part 5: 해결 방안 구현 (30분)

### 해결책 1: Worker 수 증가

```dockerfile
# Dockerfile 수정
FROM python:3.9-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt
COPY . .
# Worker 수 4개로 증가
CMD ["gunicorn", "--bind", "0.0.0.0:5000", "--workers", "4", "--timeout", "30", "app:app"]
```

```bash
# 재빌드 및 배포
docker-compose build app
docker-compose up -d app

# 테스트
for i in {1..4}; do curl http://localhost/cpu & done
wait
```

### 해결책 2: 타임아웃 설정

```nginx
# nginx.conf
upstream app {
    server app:5000;
}

server {
    listen 80;
    
    location / {
        proxy_pass http://app;
        proxy_read_timeout 30s;   # 30초 타임아웃
        proxy_connect_timeout 10s;
        proxy_send_timeout 30s;
    }
}
```

### 해결책 3: 비동기 처리 (Celery)

```python
# tasks.py - Celery 비동기 작업
from celery import Celery

celery = Celery('tasks', broker='redis://redis:6379/0')

@celery.task
def cpu_intensive_task():
    # 시간이 오래 걸리는 작업
    result = sum(i*i for i in range(10000000))
    return result

# app.py에서 호출
@app.route('/cpu-async')
def cpu_async():
    task = cpu_intensive_task.delay()
    return jsonify({"task_id": task.id, "status": "processing"})

@app.route('/cpu-result/<task_id>')
def cpu_result(task_id):
    task = cpu_intensive_task.AsyncResult(task_id)
    if task.ready():
        return jsonify({"result": task.get()})
    return jsonify({"status": "processing"})
```

### 해결책 4: Rate Limiting

```python
# Flask-Limiter 사용
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address

limiter = Limiter(
    app,
    key_func=get_remote_address,
    default_limits=["100 per minute"]
)

@app.route('/cpu')
@limiter.limit("5 per minute")  # /cpu는 분당 5회로 제한
def cpu_intensive():
    result = sum(i*i for i in range(10000000))
    return jsonify({"result": result})
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | CPU 성능 지표 이해 | ☐ |
| 2 | 정상 상태 응답 시간 측정 | ☐ |
| 3 | CPU 부하 시뮬레이션 | ☐ |
| 4 | docker stats/vmstat 분석 | ☐ |
| 5 | strace로 프로세스 분석 | ☐ |
| 6 | 장애 분석 리포트 작성 | ☐ |
| 7 | 해결 방안 구현 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# CPU 모니터링
top
vmstat 1 5
ps aux --sort=-%cpu | head

# 컨테이너 분석
docker stats
docker top <container>

# 프로세스 분석
strace -c -p <pid>
```

---

## 💡 면접 대비 핵심 포인트

### Q1: CPU 병목을 어떻게 진단하나요?
**A**: 
1. `top`으로 CPU 사용률 확인 (us/sy/wa)
2. `vmstat`으로 실행 대기 프로세스(r) 확인
3. `ps aux`로 CPU 많이 쓰는 프로세스 식별
4. `strace`로 시스템 콜 분석

### Q2: us(user)와 sy(system) CPU의 차이는?
**A**: us는 애플리케이션 코드 실행, sy는 커널 코드(시스템 콜) 실행입니다. sy가 높으면 I/O나 컨텍스트 스위칭이 많다는 의미입니다.

### Q3: CPU 집약적 작업의 해결 방법은?
**A**: Worker 수 증가, 비동기 처리 (Celery), 타임아웃 설정, Rate Limiting, 알고리즘 최적화

---

## ➡️ 다음: Day 25

**주제**: 장애 시나리오 #3 - 네트워크/연결 문제
- DNS 해석 실패
- 연결 타임아웃
- 포트 점유 문제
