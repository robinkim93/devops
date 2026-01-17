# Day 27: 포트폴리오 정리

## 오늘의 목표

토스플레이스 연결점: "인프라에 대한 오너십을 가지고 주도적으로 운영/개선해온 경험"
"장애에 대한 단기 해결뿐만 아니라 재발 방지를 위한 원인 분석과 구조적 개선 경험"

Month 1에서 진행한 트러블슈팅 시나리오를 포트폴리오로 정리합니다. 실무 경험을 체계적으로 문서화하여 면접에서 활용할 수 있게 준비합니다.

---

## 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 프로젝트 구조화 | 45분 | 폴더 정리, README |
| 문서 작성 | 1.5시간 | 각 시나리오 문서화 |
| 다이어그램 작성 | 1시간 | 아키텍처, 플로우차트 |
| 최종 검토 | 45분 | 코드 정리, 테스트 |

---

## Part 1: 프로젝트 구조화 (45분)

### 실습 1: 폴더 구조 정리

```bash
cd ~/portfolio/month1-troubleshooting

# 구조 확인
tree -L 2

# 정리된 구조
mkdir -p docs diagrams scripts

# 구조 예시:
# month1-troubleshooting/
# ├── README.md
# ├── docs/
# │   ├── troubleshooting-guide.md
# │   ├── incident-memory.md
# │   ├── incident-cpu.md
# │   └── incident-network.md
# ├── diagrams/
# │   ├── architecture.md
# │   └── troubleshooting-flow.md
# ├── memory-leak/
# │   ├── app.py
# │   ├── Dockerfile
# │   └── docker-compose.yml
# ├── cpu-bottleneck/
# │   ├── app.py
# │   ├── Dockerfile
# │   └── docker-compose.yml
# ├── network-issue/
# │   ├── docker-compose.yml
# │   └── configs/
# └── scripts/
#     ├── health-check.sh
#     └── analyze.sh
```

### 실습 2: 메인 README.md 작성

```bash
cat << 'EOF' > README.md
# Linux 트러블슈팅 포트폴리오

실제 장애 상황을 시뮬레이션하고 분석하는 DevOps 트러블슈팅 프로젝트입니다.

## 🎯 프로젝트 목표

- 컨테이너 환경에서의 장애 분석 능력 향상
- 시스템 모니터링 도구 활용 경험 축적
- 인시던트 리포트 작성 능력 개발

## 📁 프로젝트 구조

```
month1-troubleshooting/
├── memory-leak/      # 메모리 누수 시나리오
├── cpu-bottleneck/   # CPU 병목 시나리오
├── network-issue/    # 네트워크 장애 시나리오
├── docs/             # 문서
├── diagrams/         # 다이어그램
└── scripts/          # 분석 스크립트
```

## 🔧 시나리오 요약

| 시나리오 | 증상 | 원인 | 분석 도구 |
|---------|------|------|----------|
| 메모리 누수 | OOMKilled, Exit 137 | 전역 리스트 누적 | docker stats, /proc |
| CPU 병목 | 응답 지연 | 동기 CPU 작업 | top, strace, vmstat |
| 네트워크 | 연결 실패 | TIME_WAIT 누적 | ss, nc, tcpdump |

## 🛠️ 사용 기술

- **Container**: Docker, Docker Compose
- **Monitoring**: docker stats, /proc filesystem
- **Analysis**: strace, ss, tcpdump, vmstat
- **Language**: Python (Flask)

## 📊 주요 학습 내용

### 1. 컨테이너 장애 분석 흐름

```
증상 발견 → docker inspect → Exit Code 확인
         → docker stats → 리소스 모니터링
         → docker logs → 애플리케이션 로그
         → 호스트 분석 → /proc, strace
```

### 2. Exit Code 해석

| Exit Code | 의미 | 원인 |
|-----------|------|------|
| 0 | 정상 종료 | - |
| 1 | 애플리케이션 에러 | 코드 오류 |
| 137 | SIGKILL | OOM Killer |
| 139 | SIGSEGV | Segmentation Fault |

### 3. 핵심 명령어

```bash
# 컨테이너 상태
docker inspect <container> --format '{{.State.ExitCode}}'
docker inspect <container> --format '{{.State.OOMKilled}}'

# 메모리 분석
cat /proc/<pid>/status | grep VmRSS
docker stats <container>

# CPU 분석
top -H -p <pid>
strace -c -p <pid>

# 네트워크 분석
ss -s
ss -tn state time-wait | wc -l
```

## 🚀 실행 방법

각 시나리오 폴더에서:

```bash
cd memory-leak/
docker-compose up -d --build

# 장애 유발
curl http://localhost:5000/leak
```

## 📝 인시던트 리포트

- [메모리 누수 인시던트](docs/incident-memory.md)
- [CPU 병목 인시던트](docs/incident-cpu.md)
- [네트워크 장애 인시던트](docs/incident-network.md)

## 🎓 토스플레이스 연결점

이 프로젝트에서 습득한 기술은 다음과 같은 토스플레이스 업무에 활용됩니다:

| 요건 | 프로젝트 경험 |
|------|--------------|
| OS 트러블슈팅 | /proc, strace 분석 |
| Network 트러블슈팅 | ss, tcpdump 분석 |
| 컨테이너 운영 | Docker 장애 분석 |
| 원인 분석 | 인시던트 리포트 |

## 📚 참고 자료

- [Linux Performance Analysis](http://www.brendangregg.com/linuxperf.html)
- [Docker Debugging](https://docs.docker.com/config/containers/logging/)

---

*DevOps Engineer 포트폴리오 - Month 1*
EOF
```

---

## Part 2: 문서 작성 (1.5시간)

### 실습 3: 트러블슈팅 가이드 작성

```bash
cat << 'EOF' > docs/troubleshooting-guide.md
# Linux 트러블슈팅 가이드

## 1. 장애 대응 기본 흐름

```
증상 발견
    │
    ├─→ 컨테이너 문제?
    │       │
    │       └─→ docker inspect → Exit Code 확인
    │               ├─→ 137: OOM → 메모리 분석
    │               ├─→ 1: 앱 에러 → 로그 확인
    │               └─→ 139: Segfault → 코드 분석
    │
    ├─→ 응답 느림?
    │       │
    │       └─→ docker stats → 리소스 확인
    │               ├─→ CPU 높음 → top, strace
    │               ├─→ MEM 높음 → /proc/meminfo
    │               └─→ 정상 → 네트워크/외부 의존성
    │
    └─→ 연결 에러?
            │
            └─→ ss -s → 소켓 상태
                    ├─→ TIME_WAIT 많음 → 연결풀
                    └─→ 연결 실패 → nc, tcpdump
```

## 2. 메모리 분석

### 2.1 컨테이너 메모리 확인

```bash
# 실시간 모니터링
docker stats <container>

# OOM 확인
docker inspect <container> --format '{{.State.OOMKilled}}'
```

### 2.2 프로세스 메모리 분석

```bash
# VmRSS: 실제 물리 메모리 사용량
cat /proc/<pid>/status | grep VmRSS

# 메모리 맵
cat /proc/<pid>/smaps | head -100
```

### 2.3 시스템 전체 메모리

```bash
# 메모리 요약
free -h

# 상세 정보
cat /proc/meminfo
```

## 3. CPU 분석

### 3.1 프로세스별 CPU

```bash
# 실시간 모니터링
top -H -p <pid>

# 정적 스냅샷
ps aux --sort=-%cpu | head
```

### 3.2 시스템 콜 분석

```bash
# 시스템 콜 통계
strace -c -p <pid>

# 실시간 시스템 콜
strace -p <pid>
```

### 3.3 시스템 부하

```bash
# CPU, 메모리, I/O 종합
vmstat 1

# 로드 평균
uptime
```

## 4. 네트워크 분석

### 4.1 소켓 상태

```bash
# 소켓 요약
ss -s

# TCP 연결 목록
ss -tnp

# TIME_WAIT 개수
ss -tn state time-wait | wc -l
```

### 4.2 연결 테스트

```bash
# 포트 연결 테스트
nc -zv <host> <port>

# 패킷 캡처
tcpdump -i any port <port> -nn
```

## 5. 디스크 분석

### 5.1 디스크 사용량

```bash
# 파일시스템 사용량
df -h

# 디렉토리별 용량
du -sh /*
```

### 5.2 I/O 분석

```bash
# 디스크 I/O 통계
iostat -x 1

# 프로세스별 I/O
iotop
```

## 6. 체크리스트

### 컨테이너 재시작 시

- [ ] Exit Code 확인
- [ ] OOMKilled 확인
- [ ] docker logs 확인
- [ ] docker stats 모니터링
- [ ] 호스트 dmesg 확인

### 응답 지연 시

- [ ] CPU 사용률 확인
- [ ] 메모리 사용률 확인
- [ ] 네트워크 상태 확인
- [ ] 디스크 I/O 확인
- [ ] 외부 의존성 확인

### 연결 실패 시

- [ ] 서비스 상태 확인
- [ ] 포트 리스닝 확인
- [ ] 방화벽 규칙 확인
- [ ] DNS 해석 확인
- [ ] 네트워크 경로 확인
EOF
```

### 실습 4: 각 인시던트 문서 이동

```bash
# 기존 리포트 이동
mv ~/portfolio/month1-troubleshooting/memory-leak/incident-report-memory.md docs/incident-memory.md 2>/dev/null || true
mv ~/portfolio/month1-troubleshooting/cpu-bottleneck/incident-report-cpu.md docs/incident-cpu.md 2>/dev/null || true
mv ~/portfolio/month1-troubleshooting/network-issue/incident-report-network.md docs/incident-network.md 2>/dev/null || true
```

---

## Part 3: 다이어그램 작성 (1시간)

### 실습 5: 아키텍처 다이어그램

```bash
cat << 'EOF' > diagrams/architecture.md
# 시스템 아키텍처

## 전체 구조

```
┌─────────────────────────────────────────────────────────────┐
│                        Host System                           │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                   Docker Engine                      │   │
│  │                                                     │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌───────────┐   │   │
│  │  │ memory-leak │  │ cpu-bottle  │  │ network   │   │   │
│  │  │    :5000    │  │   :5001     │  │  :5002    │   │   │
│  │  └─────────────┘  └─────────────┘  └───────────┘   │   │
│  │         │                │               │          │   │
│  │         └────────────────┼───────────────┘          │   │
│  │                          │                          │   │
│  │                    docker network                   │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Monitoring: docker stats, /proc, strace, ss               │
└─────────────────────────────────────────────────────────────┘
```

## 메모리 누수 시나리오

```
┌─────────────────────────────────────────────────────────────┐
│                     memory-leak-app                          │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                    Flask App                         │   │
│  │                                                     │   │
│  │  memory_leak = []  ← 전역 리스트 (계속 증가)        │   │
│  │                                                     │   │
│  │  /leak → memory_leak.append(1MB)                   │   │
│  │                                                     │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                  │
│                          ▼                                  │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Container Memory Limit                  │   │
│  │                     128 MB                          │   │
│  └─────────────────────────────────────────────────────┘   │
│                          │                                  │
│                          ▼                                  │
│                    OOM Killer                               │
│                    Exit Code: 137                           │
└─────────────────────────────────────────────────────────────┘
```
EOF
```

### 실습 6: 트러블슈팅 플로우

```bash
cat << 'EOF' > diagrams/troubleshooting-flow.md
# 트러블슈팅 플로우차트

## 컨테이너 장애 분석

```
                    ┌─────────────────┐
                    │   증상 발견     │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  docker ps -a   │
                    └────────┬────────┘
                             │
              ┌──────────────┴──────────────┐
              │                              │
              ▼                              ▼
     ┌─────────────────┐           ┌─────────────────┐
     │  Running 상태   │           │  Exited 상태    │
     └────────┬────────┘           └────────┬────────┘
              │                              │
              ▼                              ▼
     ┌─────────────────┐           ┌─────────────────┐
     │  docker stats   │           │ docker inspect  │
     │  docker logs    │           │ (Exit Code)     │
     └────────┬────────┘           └────────┬────────┘
              │                              │
              ▼                              │
     ┌─────────────────┐            ┌────────┴────────┐
     │  리소스 분석    │            │                 │
     │  CPU/MEM/NET    │            ▼                 ▼
     └─────────────────┘    ┌──────────────┐  ┌──────────────┐
                            │ Code: 137    │  │ Code: 1      │
                            │ (OOM)        │  │ (App Error)  │
                            └──────┬───────┘  └──────┬───────┘
                                   │                  │
                                   ▼                  ▼
                            ┌──────────────┐  ┌──────────────┐
                            │ 메모리 분석  │  │ 로그 분석    │
                            │ /proc/status │  │ docker logs  │
                            └──────────────┘  └──────────────┘
```

## 메모리 분석 상세

```
┌─────────────────────────────────────────────────────────────┐
│                      메모리 분석 플로우                      │
└─────────────────────────────────────────────────────────────┘

     ┌─────────────────┐
     │  OOMKilled?     │
     └────────┬────────┘
              │
      ┌───────┴───────┐
      │               │
      ▼               ▼
  ┌───────┐       ┌───────┐
  │  Yes  │       │  No   │
  └───┬───┘       └───┬───┘
      │               │
      ▼               ▼
┌───────────┐   ┌───────────┐
│ 메모리    │   │ 다른 원인 │
│ 제한 확인 │   │ 분석      │
└─────┬─────┘   └───────────┘
      │
      ▼
┌───────────────────────────────┐
│ docker stats                  │
│ - MEM USAGE / LIMIT           │
│ - 100% 도달 시 OOM            │
└─────────────┬─────────────────┘
              │
              ▼
┌───────────────────────────────┐
│ /proc/<pid>/status            │
│ - VmRSS: 실제 메모리 사용     │
│ - VmPeak: 최대 메모리         │
└─────────────┬─────────────────┘
              │
              ▼
┌───────────────────────────────┐
│ 해결책                        │
│ - 메모리 제한 상향            │
│ - 메모리 누수 수정            │
│ - GC 튜닝                     │
└───────────────────────────────┘
```
EOF
```

---

## Part 4: 스크립트 및 최종 검토 (45분)

### 실습 7: 분석 스크립트

```bash
cat << 'EOF' > scripts/health-check.sh
#!/bin/bash

echo "=== Container Health Check ==="
echo ""

for container in $(docker ps --format '{{.Names}}'); do
    echo "Container: $container"
    
    # 상태
    status=$(docker inspect $container --format '{{.State.Status}}')
    echo "  Status: $status"
    
    # 메모리
    mem=$(docker stats $container --no-stream --format '{{.MemUsage}}')
    echo "  Memory: $mem"
    
    # CPU
    cpu=$(docker stats $container --no-stream --format '{{.CPUPerc}}')
    echo "  CPU: $cpu"
    
    # 재시작 횟수
    restarts=$(docker inspect $container --format '{{.RestartCount}}')
    echo "  Restarts: $restarts"
    
    echo ""
done
EOF

chmod +x scripts/health-check.sh
```

### 실습 8: 분석 스크립트

```bash
cat << 'EOF' > scripts/analyze.sh
#!/bin/bash

if [ -z "$1" ]; then
    echo "Usage: $0 <container_name>"
    exit 1
fi

CONTAINER=$1

echo "=== Analyzing Container: $CONTAINER ==="
echo ""

# 기본 정보
echo "[1] Basic Info"
docker inspect $CONTAINER --format 'Status: {{.State.Status}}'
docker inspect $CONTAINER --format 'Exit Code: {{.State.ExitCode}}'
docker inspect $CONTAINER --format 'OOMKilled: {{.State.OOMKilled}}'
echo ""

# 리소스
echo "[2] Resources"
docker stats $CONTAINER --no-stream
echo ""

# 최근 로그
echo "[3] Recent Logs"
docker logs $CONTAINER --tail 10
echo ""

# PID 정보
PID=$(docker inspect $CONTAINER --format '{{.State.Pid}}')
if [ "$PID" != "0" ]; then
    echo "[4] Process Info (PID: $PID)"
    cat /proc/$PID/status 2>/dev/null | grep -E "VmRSS|VmPeak|Threads"
fi
EOF

chmod +x scripts/analyze.sh
```

### 실습 9: 최종 구조 확인

```bash
cd ~/portfolio/month1-troubleshooting

# 최종 구조 확인
tree -L 2

# Git 초기화 (아직 안했다면)
git init
git add .
git commit -m "feat: Month 1 트러블슈팅 포트폴리오 완성"
```

---

## 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | 폴더 구조 정리 | docs, diagrams, scripts | |
| 2 | README.md 작성 | 프로젝트 개요 | |
| 3 | 트러블슈팅 가이드 | 분석 흐름 문서화 | |
| 4 | 아키텍처 다이어그램 | 시스템 구조 | |
| 5 | 플로우차트 | 분석 흐름 | |
| 6 | 분석 스크립트 | health-check, analyze | |
| 7 | Git 커밋 | 최종 저장 | |

---

## 면접 대비

**Q: 포트폴리오 프로젝트를 설명해주세요.**
> "컨테이너 환경에서 발생할 수 있는 메모리 누수, CPU 병목, 네트워크 장애를 직접 시뮬레이션하고 분석한 프로젝트입니다. docker inspect, /proc 파일시스템, strace 등의 도구로 원인을 분석하고 인시던트 리포트를 작성했습니다."

---

## 정리

```bash
# 최종 구조 확인
tree ~/portfolio/month1-troubleshooting -L 2
```

---

## 다음 학습: Day 28

주제: GitHub 업로드 및 포트폴리오 완성
- GitHub 레포지토리 생성
- README 최종 정리
- Month 1 완료
