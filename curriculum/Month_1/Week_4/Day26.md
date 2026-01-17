# 📅 Day 26: 장애 분석 종합 정리

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "장애에 대한 단기 해결뿐만 아니라 재발 방지를 위한 원인 분석"
> 체계적인 장애 분석 방법론과 문서화를 통한 트러블슈팅 역량 강화

3가지 장애 시나리오(메모리 누수, CPU 병목, 네트워크 문제)를 종합 정리하고, 재사용 가능한 트러블슈팅 가이드와 모니터링 스크립트를 작성합니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 장애 시나리오 종합 | 1시간 | 3가지 시나리오 분석 정리 |
| 트러블슈팅 가이드 | 1시간 | 플로우차트, 체크리스트 |
| 모니터링 스크립트 | 1시간 | 자동화 스크립트 작성 |
| 프로젝트 정리 | 1시간 | 문서화, 구조 정리 |

---

## 📋 Part 1: 장애 시나리오 종합 분석 (1시간)

### 1.1 시나리오 요약표

| # | 장애 유형 | 증상 | 핵심 분석 도구 | 근본 원인 | 해결 방법 |
|---|----------|------|--------------|----------|----------|
| 1 | 메모리 누수 | OOMKilled, Exit 137 | docker stats, dmesg, /proc | 메모리 해제 안됨 | 코드 수정, 메모리 증가 |
| 2 | CPU 병목 | 응답 지연, CPU 100% | vmstat, top, strace | 동기 처리, 무한 루프 | Worker 증가, 비동기화 |
| 3 | 연결 문제 | 연결 에러, TIME_WAIT | ss, nc, tcpdump | 연결 끊김, 풀 고갈 | 헬스체크, 연결풀, 재시도 |

### 1.2 시나리오 1: 메모리 누수 상세

```
┌─────────────────────────────────────────────────────────────────────┐
│  시나리오 1: 메모리 누수 (Memory Leak / OOMKilled)                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  증상:                                                               │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • 컨테이너 갑자기 종료                                     │    │
│  │  • docker ps -a에서 Exit Code 137                          │    │
│  │  • 메모리 사용량 지속 증가                                  │    │
│  │  • 애플리케이션 점점 느려짐                                 │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  분석 단계:                                                          │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  1. docker inspect --format '{{.State.OOMKilled}}' <id>     │    │
│  │     → true면 OOM 확정                                      │    │
│  │                                                             │    │
│  │  2. docker stats (실시간 모니터링)                          │    │
│  │     → MEM USAGE / LIMIT 확인                               │    │
│  │                                                             │    │
│  │  3. dmesg | grep -i "killed process"                        │    │
│  │     → 커널 로그에서 OOM Killer 활동 확인                   │    │
│  │                                                             │    │
│  │  4. cat /proc/<pid>/status | grep VmRSS                     │    │
│  │     → 프로세스별 메모리 사용량                             │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  해결 방법:                                                          │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  단기: 메모리 제한 증가 (docker run --memory=512m)         │    │
│  │  중기: 메모리 누수 코드 수정 (가비지 컬렉션, 객체 해제)    │    │
│  │  장기: 모니터링 알림 설정, 자동 재시작 정책                │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.3 시나리오 2: CPU 병목 상세

```
┌─────────────────────────────────────────────────────────────────────┐
│  시나리오 2: CPU 병목 (CPU Bottleneck)                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  증상:                                                               │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • API 응답 시간 급증 (>5초)                                │    │
│  │  • docker stats에서 CPU 100%                               │    │
│  │  • top에서 특정 프로세스 CPU 점유                          │    │
│  │  • 요청 타임아웃 발생                                       │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  분석 단계:                                                          │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  1. docker stats                                            │    │
│  │     → CPU % 확인                                           │    │
│  │                                                             │    │
│  │  2. vmstat 1                                                │    │
│  │     → %us (user), %sy (system) 확인                        │    │
│  │     → %us 높으면 앱 문제, %sy 높으면 시스템 콜 과다       │    │
│  │                                                             │    │
│  │  3. top -p <pid>                                            │    │
│  │     → 문제 프로세스/스레드 식별                            │    │
│  │                                                             │    │
│  │  4. strace -c -p <pid>                                      │    │
│  │     → 어떤 시스템 콜이 많은지 확인                         │    │
│  │     → read/write 많으면 I/O 문제, futex 많으면 락 경합     │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  해결 방법:                                                          │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  단기: 워커 프로세스/스레드 증가                            │    │
│  │  중기: 동기 작업 → 비동기 처리 전환                        │    │
│  │  장기: 알고리즘 최적화, 캐싱 도입                          │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.4 시나리오 3: 네트워크/연결 문제 상세

```
┌─────────────────────────────────────────────────────────────────────┐
│  시나리오 3: 네트워크/연결 문제 (Connection Issues)                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  증상:                                                               │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • "Connection refused" 또는 "Connection timed out"        │    │
│  │  • 간헐적 연결 실패                                         │    │
│  │  • TIME_WAIT 상태 소켓 급증                                 │    │
│  │  • Redis/DB 연결 실패                                       │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  분석 단계:                                                          │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  1. ss -s                                                   │    │
│  │     → 전체 소켓 통계 (established, time-wait 등)           │    │
│  │                                                             │    │
│  │  2. ss -tn state time-wait | wc -l                          │    │
│  │     → TIME_WAIT 개수 확인 (1000 이상이면 문제)             │    │
│  │                                                             │    │
│  │  3. nc -zv <host> <port>                                    │    │
│  │     → 특정 포트 연결 테스트                                │    │
│  │                                                             │    │
│  │  4. tcpdump -i any -nn port <port>                          │    │
│  │     → 패킷 레벨 분석 (SYN 만 있고 SYN-ACK 없으면 차단)     │    │
│  │                                                             │    │
│  │  5. docker logs <container> | grep -i "connection"          │    │
│  │     → 연결 관련 에러 로그                                  │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  해결 방법:                                                          │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  단기: 서비스 재시작, 연결 확인                             │    │
│  │  중기: 헬스체크 구현, 연결 재시도 로직                      │    │
│  │  장기: 연결 풀 도입, Circuit Breaker 패턴                  │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 📝 Part 2: 트러블슈팅 플로우차트 작성 (1시간)

### 2.1 통합 트러블슈팅 가이드

```bash
mkdir -p ~/portfolio/month1-troubleshooting/docs
cd ~/portfolio/month1-troubleshooting/docs

cat << 'EOF' > troubleshooting-guide.md
# Linux/Docker 트러블슈팅 가이드

## 장애 대응 플로우차트

```
┌─────────────────────────────────────────────────────────────────────┐
│                        장애 발생                                     │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────────┐
│  1단계: 증상 파악                                                   │
│  • 무엇이 안 되는가? (응답 없음, 에러, 느림)                       │
│  • 언제부터? 변경 사항 있었나?                                      │
│  • 영향 범위는? (전체/일부)                                         │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ 컨테이너 종료?  │ │ 응답이 느림?    │ │ 연결 에러?      │
└────────┬────────┘ └────────┬────────┘ └────────┬────────┘
         │                   │                   │
         ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ docker inspect  │ │ docker stats    │ │ ss -s           │
│ Exit Code 확인  │ │ CPU/MEM 확인    │ │ 소켓 상태       │
└────────┬────────┘ └────────┬────────┘ └────────┬────────┘
         │                   │                   │
    ┌────┴────┐         ┌────┴────┐         ┌────┴────┐
    │         │         │         │         │         │
    ▼         ▼         ▼         ▼         ▼         ▼
┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐
│Exit 137│ │Exit 1  │ │CPU높음│ │MEM높음│ │TIME   │ │연결   │
│OOMKill │ │앱 에러 │ │CPU분석│ │MEM분석│ │WAIT   │ │실패   │
└───────┘ └───────┘ └───────┘ └───────┘ │많음   │ └───────┘
                                        └───────┘
```

## 증상별 분석 명령어

### 1. 컨테이너 상태 확인
```bash
# 컨테이너 목록 및 상태
docker ps -a

# Exit Code 확인
docker inspect <container> --format '{{.State.ExitCode}}'
# 137: OOMKilled
# 1: 일반 에러
# 0: 정상 종료

# OOMKilled 여부
docker inspect <container> --format '{{.State.OOMKilled}}'

# 컨테이너 로그
docker logs <container> --tail 100
docker logs <container> -f  # 실시간
```

### 2. 리소스 모니터링
```bash
# 실시간 리소스 모니터링
docker stats

# 특정 컨테이너만
docker stats <container>

# 시스템 전체 메모리
free -h

# 시스템 CPU/메모리/IO 상태
vmstat 1

# 디스크 I/O
iostat -x 1
```

### 3. 메모리 분석
```bash
# 컨테이너 메모리 사용량
docker stats --format "table {{.Name}}\t{{.MemUsage}}\t{{.MemPerc}}"

# OOM Killer 로그
dmesg | grep -i "killed process"
dmesg | grep -i "out of memory"

# 프로세스별 메모리 (컨테이너 내부에서)
cat /proc/<pid>/status | grep -E "VmRSS|VmSize"
```

### 4. CPU 분석
```bash
# CPU 사용률 상위 프로세스
top -b -n 1 | head -20

# 특정 프로세스 CPU
top -p <pid>

# 시스템 콜 분석
sudo strace -c -p <pid>  # 통계
sudo strace -e read,write -p <pid>  # 특정 시스템 콜
```

### 5. 네트워크 분석
```bash
# 소켓 통계
ss -s

# TCP 연결 상태
ss -tn state established
ss -tn state time-wait | wc -l

# 특정 포트 리스닝
ss -tlnp | grep <port>

# 연결 테스트
nc -zv <host> <port>

# 패킷 캡처
sudo tcpdump -i any -nn port <port>
```

## 장애별 체크리스트

### OOMKilled (메모리 부족)
- [ ] `docker inspect --format '{{.State.OOMKilled}}'` 확인
- [ ] `docker stats`로 메모리 사용량 모니터링
- [ ] `dmesg | grep "killed process"`로 커널 로그 확인
- [ ] 메모리 제한 적절성 검토 (`docker run --memory`)
- [ ] 애플리케이션 메모리 누수 코드 점검

### CPU 병목
- [ ] `docker stats`로 CPU 사용률 확인
- [ ] `vmstat 1`로 %us, %sy 비율 확인
- [ ] `top`으로 문제 프로세스 식별
- [ ] `strace -c`로 시스템 콜 패턴 분석
- [ ] 워커 수, 비동기 처리 검토

### 연결 문제
- [ ] `ss -s`로 소켓 상태 확인
- [ ] `docker logs`로 연결 에러 로그 확인
- [ ] `nc -zv`로 의존 서비스 연결 테스트
- [ ] DNS 해석 확인 (`dig`, `nslookup`)
- [ ] 방화벽 규칙 확인 (`ufw status`)

## 인시던트 리포트 템플릿

```markdown
# 인시던트 리포트: [제목]

## 개요
- 발생 일시: YYYY-MM-DD HH:MM
- 해결 일시: YYYY-MM-DD HH:MM
- 영향 범위: 
- 심각도: Critical / High / Medium / Low

## 타임라인
- HH:MM - 장애 감지
- HH:MM - 분석 시작
- HH:MM - 원인 파악
- HH:MM - 해결 완료

## 증상
(어떤 문제가 발생했는지)

## 원인
(왜 발생했는지)

## 해결
(어떻게 해결했는지)

## 재발 방지
(같은 문제가 재발하지 않도록 어떤 조치를 취할 것인지)
```
EOF
```

---

## 🔧 Part 3: 모니터링 스크립트 작성 (1시간)

### 3.1 헬스체크 스크립트

```bash
mkdir -p ~/portfolio/month1-troubleshooting/scripts

cat << 'EOF' > ~/portfolio/month1-troubleshooting/scripts/health_check.sh
#!/bin/bash
#
# health_check.sh - 컨테이너 및 시스템 헬스체크
#

set -e

echo "=============================================="
echo "  System Health Check - $(date)"
echo "=============================================="

# 1. 컨테이너 상태
echo ""
echo "=== Container Status ==="
if docker-compose ps 2>/dev/null; then
    RUNNING=$(docker-compose ps --services --filter "status=running" | wc -l)
    TOTAL=$(docker-compose ps --services | wc -l)
    echo "Running: $RUNNING / $TOTAL"
else
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
fi

# 2. 리소스 사용량
echo ""
echo "=== Resource Usage ==="
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}"

# 3. 시스템 메모리
echo ""
echo "=== System Memory ==="
free -h | head -2

# 4. 디스크 사용량
echo ""
echo "=== Disk Usage ==="
df -h / | tail -1

# 5. 소켓 통계
echo ""
echo "=== Socket Statistics ==="
ss -s | head -5

# 6. TIME_WAIT 소켓
TIME_WAIT=$(ss -tn state time-wait | wc -l)
echo ""
echo "=== TIME_WAIT Sockets: $TIME_WAIT ==="
if [ $TIME_WAIT -gt 1000 ]; then
    echo "⚠️  WARNING: TIME_WAIT count is high!"
fi

# 7. 헬스 엔드포인트 체크
echo ""
echo "=== Health Endpoint Check ==="
if curl -s --max-time 5 http://localhost/health > /dev/null 2>&1; then
    echo "✅ Health endpoint: OK"
    curl -s http://localhost/health | jq . 2>/dev/null || curl -s http://localhost/health
else
    echo "❌ Health endpoint: FAILED"
fi

# 8. 최근 로그 (에러)
echo ""
echo "=== Recent Errors (last 10) ==="
docker-compose logs --tail 100 2>/dev/null | grep -i "error\|exception\|failed" | tail -10 || echo "No recent errors"

echo ""
echo "=============================================="
echo "  Health Check Complete"
echo "=============================================="
EOF

chmod +x ~/portfolio/month1-troubleshooting/scripts/health_check.sh
```

### 3.2 메모리 모니터링 스크립트

```bash
cat << 'EOF' > ~/portfolio/month1-troubleshooting/scripts/memory_monitor.sh
#!/bin/bash
#
# memory_monitor.sh - 메모리 사용량 모니터링 및 알림
#

THRESHOLD=${1:-80}  # 기본 80%
INTERVAL=${2:-5}    # 기본 5초

echo "Starting memory monitor (threshold: ${THRESHOLD}%, interval: ${INTERVAL}s)"
echo "Press Ctrl+C to stop"
echo ""

while true; do
    TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
    
    # Docker 컨테이너별 메모리 확인
    docker stats --no-stream --format "{{.Name}},{{.MemPerc}}" 2>/dev/null | while read line; do
        NAME=$(echo $line | cut -d',' -f1)
        MEM_PCT=$(echo $line | cut -d',' -f2 | tr -d '%')
        
        # 숫자인지 확인
        if [[ "$MEM_PCT" =~ ^[0-9.]+$ ]]; then
            # 소수점 제거
            MEM_INT=${MEM_PCT%.*}
            
            if [ "$MEM_INT" -ge "$THRESHOLD" ]; then
                echo "[$TIMESTAMP] ⚠️  WARNING: $NAME memory at ${MEM_PCT}%"
                # 알림 추가 가능 (예: slack webhook)
            fi
        fi
    done
    
    sleep $INTERVAL
done
EOF

chmod +x ~/portfolio/month1-troubleshooting/scripts/memory_monitor.sh
```

### 3.3 연결 모니터링 스크립트

```bash
cat << 'EOF' > ~/portfolio/month1-troubleshooting/scripts/connection_monitor.sh
#!/bin/bash
#
# connection_monitor.sh - 네트워크 연결 상태 모니터링
#

INTERVAL=${1:-10}
TIME_WAIT_THRESHOLD=1000

echo "Starting connection monitor (interval: ${INTERVAL}s)"
echo "Press Ctrl+C to stop"
echo ""

while true; do
    TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
    
    # 소켓 상태 수집
    ESTABLISHED=$(ss -tn state established | wc -l)
    TIME_WAIT=$(ss -tn state time-wait | wc -l)
    CLOSE_WAIT=$(ss -tn state close-wait | wc -l)
    
    echo "[$TIMESTAMP] ESTABLISHED: $ESTABLISHED, TIME_WAIT: $TIME_WAIT, CLOSE_WAIT: $CLOSE_WAIT"
    
    # TIME_WAIT 경고
    if [ $TIME_WAIT -gt $TIME_WAIT_THRESHOLD ]; then
        echo "[$TIMESTAMP] ⚠️  WARNING: TIME_WAIT count ($TIME_WAIT) exceeds threshold ($TIME_WAIT_THRESHOLD)"
    fi
    
    # CLOSE_WAIT 경고 (연결 제대로 종료 안됨)
    if [ $CLOSE_WAIT -gt 50 ]; then
        echo "[$TIMESTAMP] ⚠️  WARNING: CLOSE_WAIT count is high ($CLOSE_WAIT) - possible connection leak"
    fi
    
    sleep $INTERVAL
done
EOF

chmod +x ~/portfolio/month1-troubleshooting/scripts/connection_monitor.sh
```

---

## 📂 Part 4: 프로젝트 구조 정리 (1시간)

### 4.1 최종 프로젝트 구조

```bash
cd ~/portfolio/month1-troubleshooting
tree . 2>/dev/null || find . -type f | head -30

# 예상 구조:
# month1-troubleshooting/
# ├── README.md                    # 프로젝트 소개
# ├── docker-compose.yml           # 3-Tier 앱 구성
# ├── app/
# │   ├── app.py                   # Flask 애플리케이션
# │   ├── Dockerfile
# │   └── requirements.txt
# ├── nginx/
# │   ├── Dockerfile
# │   └── nginx.conf
# ├── scripts/
# │   ├── health_check.sh          # 헬스체크
# │   ├── memory_monitor.sh        # 메모리 모니터링
# │   └── connection_monitor.sh    # 연결 모니터링
# ├── docs/
# │   ├── troubleshooting-guide.md # 트러블슈팅 가이드
# │   ├── incident-1-oom.md        # OOM 인시던트 리포트
# │   ├── incident-2-cpu.md        # CPU 인시던트 리포트
# │   └── incident-3-connection.md # 연결 인시던트 리포트
# └── scenarios/
#     ├── memory-leak/             # 메모리 누수 시뮬레이션
#     ├── cpu-bottleneck/          # CPU 병목 시뮬레이션
#     └── connection-failure/      # 연결 문제 시뮬레이션
```

### 4.2 README.md 작성

```bash
cat << 'EOF' > ~/portfolio/month1-troubleshooting/README.md
# Linux 트러블슈팅 포트폴리오

## 프로젝트 개요

Docker 기반 3-Tier 애플리케이션의 장애 시나리오 분석 및 해결 경험을 정리한 프로젝트입니다.

## 학습 목표

1. Linux 시스템 트러블슈팅 역량 강화
2. Docker 컨테이너 장애 분석 능력 배양
3. 체계적인 문제 해결 방법론 습득

## 기술 스택

- **OS**: Linux (Ubuntu)
- **Container**: Docker, Docker Compose
- **App**: Python (Flask), Nginx, Redis
- **Tools**: strace, vmstat, ss, tcpdump, dmesg

## 장애 시나리오

| # | 시나리오 | 증상 | 분석 도구 |
|---|---------|------|----------|
| 1 | 메모리 누수 | OOMKilled, Exit 137 | docker stats, dmesg |
| 2 | CPU 병목 | 응답 지연, CPU 100% | vmstat, strace |
| 3 | 연결 문제 | 연결 에러, TIME_WAIT | ss, nc, tcpdump |

## 프로젝트 구조

```
├── app/              # Flask 애플리케이션
├── nginx/            # Nginx 설정
├── scripts/          # 모니터링 스크립트
├── docs/             # 트러블슈팅 가이드
└── scenarios/        # 장애 시뮬레이션
```

## 사용 방법

```bash
# 1. 환경 실행
docker-compose up -d

# 2. 헬스체크
./scripts/health_check.sh

# 3. 메모리 모니터링
./scripts/memory_monitor.sh 80 5

# 4. 연결 모니터링
./scripts/connection_monitor.sh 10
```

## 학습 내용

- [트러블슈팅 가이드](docs/troubleshooting-guide.md)
- [인시던트 리포트 1: OOMKilled](docs/incident-1-oom.md)
- [인시던트 리포트 2: CPU 병목](docs/incident-2-cpu.md)
- [인시던트 리포트 3: 연결 문제](docs/incident-3-connection.md)

## 토스플레이스 연관성

| 채용 요건 | 학습 내용 |
|----------|----------|
| OS 레이어 트러블슈팅 | strace, /proc, vmstat, dmesg |
| Network 트러블슈팅 | ss, tcpdump, nc |
| 장애 원인 분석 및 구조적 개선 | 인시던트 리포트, 재발 방지 |
EOF
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | 장애 시나리오 요약표 | 3가지 시나리오 정리 | ☐ |
| 2 | 트러블슈팅 가이드 | 플로우차트, 명령어 | ☐ |
| 3 | health_check.sh | 헬스체크 스크립트 | ☐ |
| 4 | memory_monitor.sh | 메모리 모니터링 | ☐ |
| 5 | connection_monitor.sh | 연결 모니터링 | ☐ |
| 6 | README.md | 프로젝트 소개 | ☐ |
| 7 | 프로젝트 구조 정리 | 폴더 정리 | ☐ |

---

## 🔑 오늘 배운 핵심 포인트

```bash
# 장애 유형별 핵심 분석 명령어
# 메모리: docker inspect --format '{{.State.OOMKilled}}', dmesg
# CPU: vmstat 1, strace -c -p <pid>
# 네트워크: ss -s, nc -zv, tcpdump
```

---

## 💡 면접 대비 핵심 포인트

### Q1: 컨테이너가 갑자기 종료되면 어떻게 분석하나요?

**A**: "먼저 `docker ps -a`로 Exit Code를 확인합니다. 137이면 OOMKilled이므로 `docker inspect --format '{{.State.OOMKilled}}'`로 확인 후 `dmesg | grep 'killed process'`로 커널 로그를 봅니다. Exit Code 1이면 앱 에러이므로 `docker logs`를 확인합니다."

### Q2: 체계적인 트러블슈팅 방법은?

**A**: "증상 파악 → 데이터 수집 → 가설 수립 → 검증 → 해결 → 문서화 순서로 진행합니다. 단기 해결 후에는 재발 방지를 위한 근본 원인 분석과 모니터링 개선이 필요합니다."

---

## ➡️ 다음: Day 27

**주제**: 블로그 포스트 작성 (포트폴리오용)
