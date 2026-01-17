# Day 23: 메모리 누수 시뮬레이션

## 오늘의 목표

토스플레이스 연결점: "장애에 대한 단기 해결뿐만 아니라 재발 방지를 위한 원인 분석"
"OS, Network 등 다양한 레이어에서의 모니터링, 트러블슈팅 경험"

메모리 누수 장애 시나리오를 직접 시뮬레이션하고 분석합니다. 컨테이너 환경에서의 OOMKilled 상황과 원인 분석 방법을 익힙니다.

---

## 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 환경 구성 | 30분 | 메모리 누수 앱 작성 |
| 장애 발생 | 30분 | OOMKilled 시뮬레이션 |
| 분석 | 1.5시간 | 다양한 도구로 원인 분석 |
| 해결 | 1시간 | 문제 해결 및 예방 |
| 정리 | 30분 | 리포트 작성 |

---

## Part 1: 환경 구성 (30분)

### 실습 1: 프로젝트 디렉토리 구성

```bash
mkdir -p ~/portfolio/month1-troubleshooting/memory-leak
cd ~/portfolio/month1-troubleshooting/memory-leak
```

### 실습 2: 메모리 누수 앱 작성

```python
# app.py - 의도적인 메모리 누수 앱
cat << 'EOF' > app.py
from flask import Flask
import time
import gc

app = Flask(__name__)

# 메모리 누수를 일으킬 리스트
memory_leak = []

@app.route('/')
def home():
    return "Memory Leak Demo - /leak 으로 메모리 누수 유발"

@app.route('/leak')
def leak():
    # 1MB씩 메모리 추가 (해제되지 않음)
    data = "X" * 1024 * 1024
    memory_leak.append(data)
    
    current_size = len(memory_leak)
    return f"Leaked: {current_size} MB (total objects: {len(memory_leak)})"

@app.route('/status')
def status():
    import os
    pid = os.getpid()
    
    # /proc에서 메모리 정보 읽기
    with open(f'/proc/{pid}/status') as f:
        for line in f:
            if 'VmRSS' in line:
                rss = line.strip()
                break
    
    return f"PID: {pid}, {rss}, Leaked Objects: {len(memory_leak)}"

@app.route('/gc')
def force_gc():
    # 강제 GC (메모리 누수는 해결 안됨)
    collected = gc.collect()
    return f"GC collected: {collected} objects. Leak still exists: {len(memory_leak)}"

@app.route('/clear')
def clear():
    # 누수 해제
    global memory_leak
    size = len(memory_leak)
    memory_leak.clear()
    gc.collect()
    return f"Cleared {size} leaked objects"

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
EOF

# requirements.txt
cat << 'EOF' > requirements.txt
flask==3.0.0
EOF
```

### 실습 3: Dockerfile 작성

```bash
cat << 'EOF' > Dockerfile
FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .

# 메모리 모니터링을 위한 procfs 접근 허용
CMD ["python", "app.py"]
EOF
```

### 실습 4: Docker Compose 작성

```bash
cat << 'EOF' > docker-compose.yml
services:
  memory-app:
    build: .
    container_name: memory-leak-app
    ports:
      - "5000:5000"
    deploy:
      resources:
        limits:
          memory: 128M    # 메모리 제한 128MB
        reservations:
          memory: 64M
    restart: on-failure:3  # OOM 시 3번까지 재시작
EOF
```

---

## Part 2: 장애 발생 시뮬레이션 (30분)

### 실습 5: 앱 실행

```bash
cd ~/portfolio/month1-troubleshooting/memory-leak

# 이미지 빌드 및 실행
docker-compose up -d --build

# 상태 확인
docker-compose ps
docker logs memory-leak-app
```

### 실습 6: 정상 동작 확인

```bash
# 기본 엔드포인트
curl http://localhost:5000/

# 현재 상태
curl http://localhost:5000/status
```

### 실습 7: 메모리 누수 유발

```bash
# 메모리 모니터링 (새 터미널)
docker stats memory-leak-app

# 메모리 누수 유발 (100번 = 약 100MB)
for i in {1..100}; do
  curl -s http://localhost:5000/leak
  echo " - $i"
  sleep 0.2
done

# 더 많이 (OOM 유발)
for i in {101..200}; do
  curl -s http://localhost:5000/leak
  echo " - $i"
  sleep 0.1
done
```

### 실습 8: OOMKilled 확인

```bash
# 컨테이너 상태 확인
docker ps -a | grep memory

# OOMKilled 확인
docker inspect memory-leak-app --format '{{.State.OOMKilled}}'

# 상세 상태
docker inspect memory-leak-app --format '{{json .State}}' | jq

# 출력 예시:
# {
#   "Status": "exited",
#   "Running": false,
#   "OOMKilled": true,
#   "ExitCode": 137
# }
```

---

## Part 3: 원인 분석 (1.5시간)

### 실습 9: docker stats로 모니터링

```bash
# 컨테이너 재시작
docker-compose up -d

# 실시간 모니터링
docker stats memory-leak-app --format "table {{.Name}}\t{{.MemUsage}}\t{{.MemPerc}}"

# 출력 예시:
# NAME              MEM USAGE / LIMIT     MEM %
# memory-leak-app   25.5MiB / 128MiB      19.92%
```

### 실습 10: /proc 분석

```bash
# 컨테이너 PID 확인
CONTAINER_PID=$(docker inspect memory-leak-app --format '{{.State.Pid}}')
echo "Container PID: $CONTAINER_PID"

# 메모리 상세 (호스트에서)
sudo cat /proc/$CONTAINER_PID/status | grep -E "VmRSS|VmSize|VmPeak"

# 출력 예시:
# VmPeak:   123456 kB  (최대 가상 메모리)
# VmSize:   120000 kB  (현재 가상 메모리)
# VmRSS:     80000 kB  (실제 물리 메모리)

# 메모리 맵
sudo cat /proc/$CONTAINER_PID/smaps | head -50
```

### 실습 11: cgroup 메모리 제한 확인

```bash
# cgroup v2 (최신 시스템)
CONTAINER_ID=$(docker inspect memory-leak-app --format '{{.Id}}')
cat /sys/fs/cgroup/system.slice/docker-${CONTAINER_ID}.scope/memory.max
cat /sys/fs/cgroup/system.slice/docker-${CONTAINER_ID}.scope/memory.current

# cgroup v1 (구 시스템)
# cat /sys/fs/cgroup/memory/docker/${CONTAINER_ID}/memory.limit_in_bytes
# cat /sys/fs/cgroup/memory/docker/${CONTAINER_ID}/memory.usage_in_bytes
```

### 실습 12: dmesg에서 OOM 로그 확인

```bash
# OOM Killer 로그
dmesg | grep -i "oom\|killed" | tail -20

# 또는
sudo journalctl -k | grep -i "oom\|killed" | tail -20

# 출력 예시:
# memory-leak-app invoked oom-killer: ...
# oom_kill_process: pid=12345, comm=python, oom_score_adj=0
```

### 실습 13: 앱 내부 상태 확인

```bash
# 컨테이너 재시작 후
docker-compose up -d

# 상태 확인
curl http://localhost:5000/status

# 몇 번 leak 후 상태
for i in {1..50}; do curl -s http://localhost:5000/leak > /dev/null; done
curl http://localhost:5000/status

# GC 시도 (효과 없음 - 참조가 있어서)
curl http://localhost:5000/gc
curl http://localhost:5000/status
```

### 실습 14: Python 메모리 프로파일링

```bash
# 컨테이너 내부에서 분석
docker exec -it memory-leak-app bash

# 내부에서
pip install memory_profiler
python -c "
import gc
import sys

# 현재 객체들
print('Object counts by type:')
types = {}
for obj in gc.get_objects():
    t = type(obj).__name__
    types[t] = types.get(t, 0) + 1

# 상위 10개 출력
for t, count in sorted(types.items(), key=lambda x: -x[1])[:10]:
    print(f'  {t}: {count}')
"

exit
```

---

## Part 4: 문제 해결 (1시간)

### 실습 15: 메모리 정리

```bash
# 누수 정리
curl http://localhost:5000/clear
curl http://localhost:5000/status

# 메모리 확인
docker stats memory-leak-app --no-stream
```

### 실습 16: 코드 수정 (메모리 누수 방지)

```python
# app_fixed.py - 메모리 누수 수정 버전
cat << 'EOF' > app_fixed.py
from flask import Flask
from collections import deque
import gc

app = Flask(__name__)

# 최대 10개만 유지하는 deque 사용
MAX_ITEMS = 10
memory_cache = deque(maxlen=MAX_ITEMS)

@app.route('/')
def home():
    return "Memory Fixed Demo"

@app.route('/add')
def add():
    # 새 데이터 추가 (최대 10개만 유지)
    data = "X" * 1024 * 1024
    memory_cache.append(data)
    
    return f"Cache size: {len(memory_cache)} (max: {MAX_ITEMS})"

@app.route('/status')
def status():
    import os
    pid = os.getpid()
    
    with open(f'/proc/{pid}/status') as f:
        for line in f:
            if 'VmRSS' in line:
                rss = line.strip()
                break
    
    return f"PID: {pid}, {rss}, Cache: {len(memory_cache)}"

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
EOF
```

### 실습 17: 메모리 제한 조정

```yaml
# docker-compose-prod.yml
cat << 'EOF' > docker-compose-prod.yml
services:
  memory-app:
    build: .
    container_name: memory-leak-app
    ports:
      - "5000:5000"
    deploy:
      resources:
        limits:
          memory: 256M    # 여유있게 설정
        reservations:
          memory: 128M
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5000/status"]
      interval: 30s
      timeout: 10s
      retries: 3
    restart: unless-stopped
EOF
```

---

## Part 5: 인시던트 리포트 (30분)

### 실습 18: 리포트 작성

```markdown
cat << 'EOF' > incident-report-memory.md
# 인시던트 리포트: 메모리 누수로 인한 OOMKilled

## 개요
- **발생일시**: YYYY-MM-DD HH:MM
- **영향범위**: memory-leak-app 컨테이너
- **심각도**: High
- **해결시간**: XX분

## 증상
1. 컨테이너가 반복적으로 재시작됨
2. Exit Code 137 (SIGKILL)
3. OOMKilled: true

## 타임라인
| 시간 | 이벤트 |
|------|--------|
| T+0 | 메모리 사용량 증가 시작 |
| T+5m | 메모리 80% 도달 |
| T+8m | OOM Killer 동작, 컨테이너 종료 |
| T+9m | 재시작 후 동일 현상 반복 |

## 분석

### 모니터링 결과
```bash
docker stats memory-leak-app
# MEM USAGE: 127.5MiB / 128MiB (99.6%)

docker inspect memory-leak-app --format '{{.State.OOMKilled}}'
# true
```

### 근본 원인
- /leak 엔드포인트에서 전역 리스트에 데이터 추가
- 참조가 유지되어 GC 대상이 되지 않음
- 요청마다 1MB씩 누적

### 코드 분석
```python
memory_leak = []  # 전역 리스트

@app.route('/leak')
def leak():
    data = "X" * 1024 * 1024
    memory_leak.append(data)  # 계속 추가, 삭제 없음
```

## 해결
1. 즉시 조치: 컨테이너 재시작
2. 근본 해결: deque(maxlen=N) 사용으로 메모리 제한
3. 메모리 제한 256MB로 상향 조정

## 예방책
1. 메모리 사용량 모니터링 알림 설정 (80%)
2. 코드 리뷰 시 전역 변수 사용 검토
3. 부하 테스트 시 메모리 프로파일링 포함

## 학습점
- Exit Code 137 = SIGKILL (보통 OOM)
- docker inspect --format '{{.State.OOMKilled}}' 로 확인
- /proc/<pid>/status의 VmRSS로 실제 메모리 확인
EOF
```

---

## 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | 메모리 누수 앱 작성 | Python Flask + leak | |
| 2 | OOMKilled 시뮬레이션 | 128MB 제한, 누수 유발 | |
| 3 | docker stats 분석 | 실시간 메모리 모니터링 | |
| 4 | /proc 분석 | VmRSS, smaps | |
| 5 | cgroup 확인 | memory.max, memory.current | |
| 6 | dmesg OOM 로그 | oom-killer 로그 | |
| 7 | 문제 해결 | deque(maxlen) 사용 | |
| 8 | 리포트 작성 | 원인, 해결, 예방 | |

---

## 핵심 명령어

```bash
# OOM 확인
docker inspect <container> --format '{{.State.OOMKilled}}'
docker inspect <container> --format '{{.State.ExitCode}}'

# 메모리 모니터링
docker stats <container>

# /proc 분석
cat /proc/<pid>/status | grep VmRSS

# OOM 로그
dmesg | grep -i oom
```

---

## 면접 대비

**Q: 컨테이너가 갑자기 재시작될 때 어떻게 분석하나요?**
> "먼저 docker inspect로 Exit Code와 OOMKilled를 확인합니다. Exit Code 137은 SIGKILL로 OOM일 가능성이 높습니다. docker stats로 메모리 사용량을 모니터링하고, dmesg에서 OOM Killer 로그를 확인합니다."

---

## 정리

```bash
cd ~/portfolio/month1-troubleshooting/memory-leak
docker-compose down
```

---

## 다음 학습: Day 24

주제: CPU 병목 시뮬레이션
- CPU 부하 유발 및 분석
- strace, perf 활용
- 인시던트 리포트
