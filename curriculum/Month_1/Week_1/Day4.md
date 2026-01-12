# 📅 Day 4: 디스크 I/O 분석 (iostat, iotop)

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "서비스 응답 지연" 장애 분석 시 디스크 I/O 병목 진단

디스크 I/O가 느려서 발생하는 장애를 진단할 수 있어야 합니다.

---

## ⏰ 예상 학습 시간: 3시간

---

## 📚 Part 1: 핵심 개념 (30분)

### 디스크 I/O 병목이란?

```
┌─────────────────────────────────────────────────────────────┐
│  장애 상황                                                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  증상: API 응답 시간 급증 (평소 100ms → 5초)                 │
│  CPU: 정상 (20%)                                            │
│  메모리: 정상                                               │
│                                                             │
│  iostat 확인:                                               │
│  %util = 99%  ← 디스크가 거의 100% 사용 중!                 │
│  await = 500ms ← I/O 요청당 500ms 대기                      │
│                                                             │
│  원인: 로그 파일 과다 쓰기로 디스크 병목                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 핵심 지표

| 지표 | 의미 | 정상 범위 |
|------|------|----------|
| `%util` | 디스크 사용률 | < 70% |
| `await` | I/O 대기 시간(ms) | < 10ms (SSD), < 20ms (HDD) |
| `r/s, w/s` | 초당 읽기/쓰기 요청 수 | - |
| `rkB/s, wkB/s` | 초당 읽기/쓰기 KB | - |

---

## 🛠️ Part 2: 실습 (2시간)

### 사전 준비

```bash
# sysstat 패키지 설치 (iostat 포함)
sudo apt install -y sysstat iotop
```

---

### 실습 1: iostat 기본 사용 (30분)

```bash
# 1초 간격으로 5번 출력
iostat -x 1 5
```

**출력 예시**:
```
Device   r/s   w/s   rkB/s   wkB/s  await  %util
sda      0.5   10.2  2.0     50.5   5.2    3.1
```

**핵심 필드 해석**:
```
r/s, w/s   : 초당 읽기/쓰기 요청 수
rkB/s, wkB/s: 초당 읽기/쓰기 KB
await      : 평균 I/O 대기 시간 (ms)
%util      : 디스크 사용률 (100%에 가까우면 병목!)
```

---

### 실습 2: 디스크별 상세 분석 (20분)

```bash
# 특정 디스크만 모니터링
iostat -x -d sda 1 5

# 모든 디스크 + 확장 통계
iostat -xz 1
```

---

### 실습 3: iotop으로 프로세스별 I/O 확인 (30분)

```bash
# iotop 실행 (root 필요)
sudo iotop

# 배치 모드 (스크립트에서 사용)
sudo iotop -b -n 5

# 실제 I/O 발생하는 프로세스만
sudo iotop -o
```

**출력 예시**:
```
TID  PRIO USER     DISK READ  DISK WRITE  COMMAND
123  be/4 mysql    10.00 M/s   50.00 M/s  mysqld
456  be/4 nginx    0.00 B/s    5.00 M/s   nginx
```

**분석**: 어떤 프로세스가 I/O를 많이 발생시키는지 확인

---

### 실습 4: I/O 부하 시뮬레이션 (30분)

```bash
# 1. 현재 I/O 상태 확인
iostat -x 1 &
IOSTAT_PID=$!

# 2. 쓰기 부하 발생 (100MB 파일 생성)
dd if=/dev/zero of=/tmp/testfile bs=1M count=100 oflag=direct

# 3. 읽기 부하 발생
dd if=/tmp/testfile of=/dev/null bs=1M iflag=direct

# 4. iostat 종료
kill $IOSTAT_PID

# 5. 정리
rm /tmp/testfile
```

**`oflag=direct, iflag=direct`**: 캐시 우회, 실제 디스크 I/O 발생

---

### 실습 5: /proc에서 I/O 통계 확인 (10분)

```bash
# 프로세스별 I/O 통계
cat /proc/$$/io

# 출력 예시:
# rchar: 1000000     ← 읽은 바이트 (캐시 포함)
# wchar: 500000      ← 쓴 바이트 (캐시 포함)
# read_bytes: 50000  ← 실제 디스크에서 읽은 바이트
# write_bytes: 25000 ← 실제 디스크에 쓴 바이트
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | `iostat -x`로 디스크 상태 확인 | ☐ |
| 2 | `%util`과 `await` 지표 이해 | ☐ |
| 3 | `iotop`으로 프로세스별 I/O 확인 | ☐ |
| 4 | `dd`로 I/O 부하 시뮬레이션 | ☐ |
| 5 | `/proc/[pid]/io` 확인 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
iostat -x 1                  # 디스크 상태 모니터링
sudo iotop -o                # 프로세스별 I/O (실시간)
cat /proc/[pid]/io           # 프로세스 I/O 통계
dd if=/dev/zero of=test bs=1M count=100  # 쓰기 테스트
```

---

## ➡️ 다음 학습: Day 5

**주제**: 네트워크 분석 기초 (ss, netstat)

