# 📅 Day 2: /proc 파일시스템으로 프로세스 분석

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "OS 레이어에서의 모니터링, 트러블슈팅 경험"

`/proc` 파일시스템을 통해 실행 중인 프로세스의 상태를 분석하고, FD 누수나 메모리 문제를 진단할 수 있어야 합니다.

---

## ⏰ 예상 학습 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 | 30분 | /proc 구조 이해 |
| 실습 | 2시간 | 프로세스 분석 실습 |
| 정리 | 30분 | 복습 및 체크리스트 |

---

## 📚 Part 1: 핵심 개념 (30분)

### /proc 파일시스템이란?

```
/proc는 가상 파일시스템
→ 실제 디스크에 있는 게 아님
→ 커널이 실시간으로 생성하는 정보
→ 프로세스와 시스템 상태를 파일처럼 읽을 수 있음
```

### 토스플레이스에서 왜 중요한가?

```
┌─────────────────────────────────────────────────────────────┐
│  실제 장애 상황                                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  증상: "Pod가 OOMKilled로 재시작됨"                         │
│                                                             │
│  분석: /proc/[pid]/status 확인                              │
│                                                             │
│  VmRSS:    2048000 kB  ← 실제 메모리 2GB 사용              │
│  VmSize:   4096000 kB  ← 가상 메모리 4GB                   │
│  Threads:  500         ← 스레드 500개 (너무 많음!)         │
│                                                             │
│  진단: 스레드 과다 생성으로 메모리 고갈                      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 자주 사용하는 /proc 경로

| 경로 | 내용 | 활용 |
|------|------|------|
| `/proc/[pid]/status` | 프로세스 상태 | 메모리, 스레드 수 확인 |
| `/proc/[pid]/fd/` | 열린 파일 디스크립터 | FD 누수 탐지 |
| `/proc/[pid]/cmdline` | 실행 명령어 | 프로세스 식별 |
| `/proc/[pid]/environ` | 환경 변수 | 설정 확인 |
| `/proc/[pid]/maps` | 메모리 맵 | 메모리 레이아웃 분석 |

---

## 🛠️ Part 2: 실습 (2시간)

### 실습 1: 프로세스 상태 확인 (30분)

**목표**: 특정 프로세스의 메모리 사용량과 스레드 수 확인

```bash
# 1. 테스트용 프로세스 실행
sleep 1000 &
PID=$!
echo "PID: $PID"

# 2. 프로세스 상태 확인
cat /proc/$PID/status
```

**주요 필드 해석**:
```
Name:   sleep           ← 프로세스 이름
State:  S (sleeping)    ← 상태 (S=sleep, R=running, Z=zombie)
Pid:    12345           ← 프로세스 ID
PPid:   12344           ← 부모 프로세스 ID
Threads: 1              ← 스레드 수
VmSize:  2048 kB        ← 가상 메모리 크기
VmRSS:   512 kB         ← 실제 물리 메모리 사용량 (중요!)
```

**장애 분석 시 체크 포인트**:
```bash
# 메모리 관련 필드만 추출
cat /proc/$PID/status | grep -E "VmSize|VmRSS|VmSwap|Threads"

# VmRSS가 계속 증가하면 → 메모리 누수 의심
# Threads가 과도하면 → 스레드 누수 의심
```

---

### 실습 2: 열린 파일 디스크립터 확인 (30분)

**목표**: FD 누수 탐지

```bash
# 1. 현재 쉘의 FD 확인
ls -la /proc/$$/fd/

# 예상 출력:
# lrwx------ 1 user user 64 Jan  7 10:00 0 -> /dev/pts/0  (stdin)
# lrwx------ 1 user user 64 Jan  7 10:00 1 -> /dev/pts/0  (stdout)
# lrwx------ 1 user user 64 Jan  7 10:00 2 -> /dev/pts/0  (stderr)
```

```bash
# 2. FD 개수 확인
ls /proc/$$/fd/ | wc -l

# 3. 특정 프로세스의 FD 개수 확인
ls /proc/$(pgrep nginx)/fd/ 2>/dev/null | wc -l
```

**FD 누수 탐지 방법**:
```bash
# FD 개수가 시간이 지나면서 계속 증가하면 누수!
watch -n 5 "ls /proc/\$(pgrep java)/fd/ 2>/dev/null | wc -l"
```

---

### 실습 3: 소켓 연결 확인 (30분)

**목표**: 프로세스가 어떤 네트워크 연결을 가지고 있는지 확인

```bash
# 1. 웹서버 실행 (테스트용)
python3 -m http.server 8080 &
SERVER_PID=$!

# 2. FD 중 소켓 확인
ls -la /proc/$SERVER_PID/fd/ | grep socket

# 3. 더 자세한 정보는 ss 명령어로
ss -tlnp | grep $SERVER_PID
```

---

### 실습 4: 환경 변수 확인 (15분)

**목표**: 프로세스의 환경 변수 확인

```bash
# 환경 변수 확인 (NULL 문자로 구분됨)
cat /proc/$$/environ | tr '\0' '\n'

# 특정 환경 변수 검색
cat /proc/$$/environ | tr '\0' '\n' | grep PATH
```

**활용**: 컨테이너에서 환경 변수가 제대로 주입되었는지 확인

---

### 실습 5: 컨텍스트 스위칭 확인 (15분)

**목표**: 프로세스의 컨텍스트 스위칭 횟수 확인

```bash
cat /proc/$$/status | grep -E "ctxt_switches"

# voluntary_ctxt_switches:    100   ← I/O 대기로 자발적 양보
# nonvoluntary_ctxt_switches: 10    ← 타임슬라이스 만료로 강제 전환
```

**분석 기준**:
- `voluntary`가 높음 → I/O 병목 (디스크/네트워크 대기 많음)
- `nonvoluntary`가 높음 → CPU 경쟁 심함

---

## 📊 Part 3: 종합 실습 - 장애 시나리오 (선택)

### 시나리오: FD 누수 탐지 시뮬레이션

```bash
# 1. FD 누수를 일으키는 스크립트 작성
cat << 'EOF' > /tmp/fd_leak.sh
#!/bin/bash
while true; do
    exec 3>/tmp/leak_$RANDOM.txt
    sleep 0.1
done
EOF
chmod +x /tmp/fd_leak.sh

# 2. 실행 (5초 후 Ctrl+C로 중단)
/tmp/fd_leak.sh &
LEAK_PID=$!

# 3. FD 개수 모니터링
watch -n 1 "ls /proc/$LEAK_PID/fd/ 2>/dev/null | wc -l"

# 4. 정리
kill $LEAK_PID 2>/dev/null
rm -f /tmp/leak_*.txt /tmp/fd_leak.sh
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | `/proc/[pid]/status`에서 VmRSS 확인함 | ☐ |
| 2 | `/proc/[pid]/fd/`로 FD 개수 확인함 | ☐ |
| 3 | FD 중 소켓 연결 확인함 | ☐ |
| 4 | 컨텍스트 스위칭 횟수 확인함 | ☐ |
| 5 | FD 누수 시뮬레이션 실습함 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# 반드시 외울 것!
cat /proc/[pid]/status              # 프로세스 상태
cat /proc/[pid]/status | grep VmRSS # 실제 메모리 사용량
ls /proc/[pid]/fd/ | wc -l          # FD 개수
cat /proc/[pid]/cmdline             # 실행 명령어
cat /proc/[pid]/environ | tr '\0' '\n'  # 환경 변수
```

---

## 📝 학습 기록

```
학습일: ____년 __월 __일
실제 소요 시간: ____시간
어려웠던 점:


추가로 궁금한 점:


```

---

## ➡️ 다음 학습: Day 3

**주제**: 메모리 분석 (free, /proc/meminfo, vmstat)

**예고**:
- 시스템 메모리 상태 분석
- Buffer vs Cache 차이
- 메모리 부족 상황 진단

