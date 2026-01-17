# Day 7: 주간 복습 및 종합 실습

## 오늘의 목표

토스플레이스 연결점: "OS, Network 등 다양한 레이어에서의 모니터링, 트러블슈팅 경험"
"장애에 대한 단기 해결뿐만 아니라 재발 방지를 위한 원인 분석"

Week 1에서 배운 모든 도구를 종합하여 복습합니다. 실제 장애 시나리오를 시뮬레이션하고 분석하는 능력을 키웁니다.

---

## 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 복습 | 1시간 | Week 1 핵심 정리 |
| 종합 실습 | 2시간 | 시나리오 기반 분석 |
| 셀프 테스트 | 30분 | 핵심 명령어 테스트 |
| 다음 주 준비 | 30분 | Week 2 예습 |

---

## Part 1: Week 1 핵심 복습 (1시간)

### 1.1 배운 내용 요약

| Day | 주제 | 핵심 도구 | 사용 상황 |
|-----|------|----------|----------|
| Day 1 | 프로세스 분석 | strace, ltrace | 시스템 콜 추적 |
| Day 2 | /proc 분석 | /proc/[pid]/* | 프로세스 상세 정보 |
| Day 3 | 메모리 분석 | free, vmstat | 메모리 사용량 모니터링 |
| Day 4 | 디스크 I/O | iostat, iotop | I/O 병목 분석 |
| Day 5 | 네트워크 소켓 | ss, netstat | 연결 상태 확인 |
| Day 6 | 패킷 분석 | tcpdump | 네트워크 트래블슈팅 |

### 1.2 상황별 도구 선택

```
문제 상황별 도구 맵:

[프로세스가 느리다]
    ├── CPU 문제? → top, strace -c
    ├── 메모리 문제? → free -h, /proc/[pid]/status
    ├── I/O 문제? → iostat, iotop
    └── 네트워크 문제? → ss, tcpdump

[서버 응답이 없다]
    ├── 프로세스 확인 → ps aux | grep
    ├── 포트 확인 → ss -tlnp
    ├── 연결 상태 → ss -s
    └── 패킷 확인 → tcpdump

[메모리 부족]
    ├── 전체 상태 → free -h
    ├── 프로세스별 → ps aux --sort=-%mem
    ├── 상세 정보 → /proc/meminfo
    └── 캐시 정리 → sync && echo 3 > /proc/sys/vm/drop_caches

[디스크 I/O 느림]
    ├── 전체 상태 → iostat -x 1
    ├── 프로세스별 → iotop
    └── 대기 분석 → vmstat 1 (wa 컬럼)
```

### 1.3 핵심 명령어 모음

```bash
# === 프로세스 분석 ===
strace -c -p <pid>                     # 시스템 콜 통계
strace -e trace=network -p <pid>       # 네트워크 콜만
cat /proc/<pid>/status | grep VmRSS    # 메모리 사용량
ls /proc/<pid>/fd | wc -l              # 열린 파일 수

# === 메모리 분석 ===
free -h                                # 메모리 요약
vmstat 1                               # 1초 간격 모니터링
cat /proc/meminfo | grep -E "MemFree|Cached|Buffers"

# === 디스크 I/O ===
iostat -x 1                            # 디스크 I/O 통계
iotop -o                               # I/O 사용 프로세스
dd if=/dev/zero of=test bs=1M count=100  # I/O 테스트

# === 네트워크 ===
ss -tlnp                               # 리스닝 포트
ss -s                                  # 소켓 통계
ss -tn state time-wait | wc -l         # TIME_WAIT 개수
tcpdump -i any port 80 -nn -c 10       # 패킷 캡처
```

---

## Part 2: 종합 실습 - 시나리오 분석 (2시간)

### 시나리오 1: 웹 서버 응답 지연

**상황**: Nginx 웹 서버가 설치된 서버에서 응답이 느립니다.

```bash
# 1. 프로세스 확인
ps aux | grep nginx
pidof nginx

# 2. 포트 확인
ss -tlnp | grep :80

# 3. 연결 상태 확인
ss -tn | grep :80 | wc -l
ss -tn state established | wc -l
ss -tn state time-wait | wc -l

# 4. 시스템 리소스 확인
top -bn1 | head -20
free -h
iostat -x 1 3

# 5. 시스템 콜 분석
NGINX_PID=$(pgrep -o nginx)
strace -c -p $NGINX_PID -t 5 2>&1

# 6. 네트워크 패킷 확인
tcpdump -i any port 80 -nn -c 20
```

**분석 포인트:**
- 연결 수가 너무 많은가?
- TIME_WAIT이 쌓이고 있는가?
- CPU/메모리 사용량은 정상인가?
- 디스크 I/O 대기가 있는가?

### 시나리오 2: 프로세스 메모리 누수 의심

**상황**: 특정 프로세스의 메모리 사용량이 계속 증가합니다.

```bash
# 테스트용 메모리 누수 프로세스 시뮬레이션
python3 -c "
import time
data = []
while True:
    data.append('x' * 1024 * 1024)  # 1MB씩 추가
    print(f'Allocated: {len(data)} MB')
    time.sleep(2)
" &
LEAK_PID=$!
echo "Leak PID: $LEAK_PID"

# 분석 시작
# 1. 초기 메모리 확인
cat /proc/$LEAK_PID/status | grep VmRSS

# 2. 10초 후 다시 확인
sleep 10
cat /proc/$LEAK_PID/status | grep VmRSS

# 3. 메모리 증가 추이 모니터링
for i in {1..5}; do
    cat /proc/$LEAK_PID/status | grep VmRSS
    sleep 5
done

# 4. 시스템 전체 메모리
free -h
vmstat 1 5

# 5. 정리
kill $LEAK_PID 2>/dev/null
```

**분석 포인트:**
- VmRSS가 시간에 따라 증가하는가?
- 시스템 전체 free 메모리가 감소하는가?
- swap 사용량이 증가하는가?

### 시나리오 3: 디스크 I/O 병목

**상황**: 서버가 전반적으로 느리고 디스크 I/O가 의심됩니다.

```bash
# I/O 부하 시뮬레이션
dd if=/dev/zero of=/tmp/testfile bs=1M count=500 &
DD_PID=$!

# 분석 시작
# 1. vmstat으로 I/O wait 확인
vmstat 1 5
# wa 컬럼이 높으면 I/O 대기

# 2. iostat으로 디스크별 분석
iostat -x 1 5
# %util이 100%에 가까우면 병목

# 3. 어떤 프로세스가 I/O를 많이 사용하는지
iotop -o -b -n 3

# 4. /proc으로 특정 프로세스 I/O 확인
cat /proc/$DD_PID/io 2>/dev/null

# 5. 정리
kill $DD_PID 2>/dev/null
rm -f /tmp/testfile
```

**분석 포인트:**
- vmstat의 wa(I/O wait)가 높은가?
- iostat의 %util이 높은가?
- iotop에서 특정 프로세스가 I/O를 독점하는가?

### 시나리오 4: 네트워크 연결 문제

**상황**: 외부 API 호출이 실패합니다.

```bash
# 1. DNS 확인
dig google.com +short
nslookup google.com

# 2. 연결 테스트
nc -zv google.com 443 -w 5

# 3. 패킷 캡처 (HTTPS)
tcpdump -i any host google.com -nn -c 10

# 4. 현재 연결 상태
ss -tn | head -20

# 5. 로컬 포트 고갈 확인
ss -s
cat /proc/sys/net/ipv4/ip_local_port_range

# 6. TIME_WAIT 확인
ss -tn state time-wait | wc -l
```

**분석 포인트:**
- DNS 해석이 되는가?
- TCP 연결이 되는가?
- SYN 패킷이 나가는가? SYN-ACK이 돌아오는가?
- TIME_WAIT이 너무 많이 쌓여있지 않은가?

---

## Part 3: 셀프 테스트 (30분)

### 문제 1: 프로세스 분석

```bash
# 문제: nginx 프로세스의 메모리 사용량(VmRSS)을 확인하세요
# 답: cat /proc/$(pgrep -o nginx)/status | grep VmRSS
```

### 문제 2: 메모리

```bash
# 문제: 현재 시스템의 free 메모리와 cache 메모리를 확인하세요
# 답: free -h  또는  cat /proc/meminfo | grep -E "MemFree|Cached"
```

### 문제 3: 디스크 I/O

```bash
# 문제: 디스크의 사용률(%util)을 1초 간격으로 확인하세요
# 답: iostat -x 1
```

### 문제 4: 네트워크

```bash
# 문제: 80번 포트로 LISTEN 중인 프로세스를 찾으세요
# 답: ss -tlnp | grep :80
```

### 문제 5: 패킷 분석

```bash
# 문제: 특정 호스트(8.8.8.8)로 가는 패킷을 10개만 캡처하세요
# 답: tcpdump -i any host 8.8.8.8 -nn -c 10
```

---

## Part 4: 핵심 요약

### 분석 도구 선택 가이드

```
증상 → 도구 매핑:

┌─────────────────┬────────────────────────────────────┐
│      증상       │              도구                  │
├─────────────────┼────────────────────────────────────┤
│ CPU 높음        │ top, strace -c                     │
│ 메모리 부족     │ free -h, /proc/meminfo            │
│ 디스크 느림     │ iostat -x, iotop                  │
│ 네트워크 장애   │ ss, tcpdump                       │
│ 프로세스 이상   │ /proc/[pid]/*, strace             │
│ 연결 문제       │ ss -s, nc -zv                     │
└─────────────────┴────────────────────────────────────┘
```

### 자주 사용하는 조합

```bash
# 서버 전반 상태 확인
uptime && free -h && df -h && ss -s

# 프로세스 깊이 분석
PID=<target>
cat /proc/$PID/status | grep -E "VmRSS|Threads"
ls /proc/$PID/fd | wc -l
strace -c -p $PID

# 네트워크 문제 분석
ss -tlnp
ss -tn state time-wait | wc -l
tcpdump -i any port <port> -nn -c 20
```

---

## 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | Day 1-6 내용 복습 | |
| 2 | 시나리오 1: 웹 서버 분석 실습 | |
| 3 | 시나리오 2: 메모리 누수 분석 실습 | |
| 4 | 시나리오 3: 디스크 I/O 분석 실습 | |
| 5 | 시나리오 4: 네트워크 분석 실습 | |
| 6 | 셀프 테스트 완료 | |

---

## 면접 대비 핵심 포인트

**Q1: 서버가 느릴 때 어떻게 분석하나요?**
> "먼저 top으로 CPU, 메모리 전반을 확인합니다. CPU가 높으면 어떤 프로세스인지 확인하고 strace로 시스템 콜을 분석합니다. I/O wait이 높으면 iostat과 iotop으로 디스크 병목을 확인합니다."

**Q2: 네트워크 연결 문제를 어떻게 디버깅하나요?**
> "ss -tlnp로 서비스 포트가 열려있는지 확인하고, nc로 연결 테스트를 합니다. 필요시 tcpdump로 패킷을 캡처하여 SYN-ACK 핸드셰이크가 정상인지 확인합니다."

**Q3: 메모리 누수를 어떻게 발견하나요?**
> "/proc/[pid]/status의 VmRSS를 주기적으로 확인하여 지속적으로 증가하는지 봅니다. free -h로 시스템 전체 메모리 감소 추이도 함께 확인합니다."

---

## 학습 기록

```
Week 1 완료일: ____년 __월 __일
실제 소요 시간: ____시간

자신있는 부분:

추가 학습 필요 부분:
```

---

## 다음 학습: Week 2

주제: Docker 기초
- Day 8: Docker 설치 및 기본 명령어
- Day 9: Dockerfile 작성
- Day 10: Docker Compose
- Day 11-14: 컨테이너 네트워킹, 볼륨, 트러블슈팅
