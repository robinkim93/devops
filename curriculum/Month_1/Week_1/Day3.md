# Day 3: 메모리 분석 (free, vmstat)

## 오늘의 목표

토스플레이스 연결점: "OS, Network 등 다양한 레이어에서의 모니터링, 트러블슈팅 경험"
"장애에 대한 단기 해결뿐만 아니라 재발 방지를 위한 원인 분석"

시스템 메모리 상태를 분석하는 도구를 학습합니다. free와 vmstat으로 메모리 사용량, 스왑, 시스템 전반의 상태를 모니터링하는 방법을 익힙니다.

---

## 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| free 학습 | 1시간 | 메모리 사용량 분석 |
| vmstat 학습 | 1시간 | 시스템 상태 모니터링 |
| /proc/meminfo | 45분 | 상세 메모리 정보 |
| 종합 실습 | 1시간 15분 | 메모리 부하 시뮬레이션 |

---

## Part 1: free 명령어 (1시간)

### 1.1 free 기본 사용법

```bash
# 기본 출력
free

# 사람이 읽기 쉬운 단위 (GB, MB)
free -h

# 출력 예시:
#               total        used        free      shared  buff/cache   available
# Mem:          15Gi       4.2Gi       6.8Gi       520Mi       4.4Gi        10Gi
# Swap:         2.0Gi          0B       2.0Gi
```

### 1.2 출력 필드 이해

```
메모리 필드 설명:

total     : 전체 물리 메모리
used      : 사용 중인 메모리
free      : 완전히 비어있는 메모리
shared    : 공유 메모리 (tmpfs 등)
buff/cache: 버퍼와 캐시로 사용 중인 메모리
available : 새 프로세스에서 사용 가능한 메모리 (중요!)

핵심 포인트:
- free가 적어도 available이 충분하면 OK
- Linux는 여유 메모리를 캐시로 활용
- available = free + 회수 가능한 buff/cache
```

### 1.3 free 옵션

```bash
# 다양한 단위
free -b    # 바이트
free -k    # 킬로바이트 (기본)
free -m    # 메가바이트
free -g    # 기가바이트
free -h    # 사람이 읽기 쉬운 단위

# 주기적 출력
free -s 2      # 2초마다 출력
free -c 5      # 5번만 출력
free -s 1 -c 5 # 1초마다 5번

# 합계 표시
free -t    # 메모리 + 스왑 합계
```

### 1.4 실습: 메모리 상태 확인

```bash
# 현재 메모리 상태
free -h

# 1초마다 3번 확인
free -h -s 1 -c 3

# available이 total의 몇 %인지 계산
free -m | awk '/Mem:/ {printf "Available: %.1f%%\n", $7/$2*100}'
```

---

## Part 2: vmstat 명령어 (1시간)

### 2.1 vmstat 기본 사용법

```bash
# 기본 출력
vmstat

# 1초 간격으로 5번 출력
vmstat 1 5

# 출력 예시:
# procs -----------memory---------- ---swap-- -----io---- -system-- ------cpu-----
#  r  b   swpd   free   buff  cache   si   so    bi    bo   in   cs us sy id wa st
#  1  0      0 6800000 200000 4400000   0    0     5    10  100  200 10  5 84  1  0
```

### 2.2 출력 필드 이해

```
procs:
  r  : 실행 대기 중인 프로세스 수 (CPU 대기)
  b  : 블록된 프로세스 수 (I/O 대기)

memory:
  swpd  : 사용 중인 스왑 (KB)
  free  : 여유 메모리 (KB)
  buff  : 버퍼 메모리 (KB)
  cache : 캐시 메모리 (KB)

swap:
  si : 스왑 인 (디스크 -> 메모리, KB/s)
  so : 스왑 아웃 (메모리 -> 디스크, KB/s)

io:
  bi : 블록 인 (디스크 읽기, blocks/s)
  bo : 블록 아웃 (디스크 쓰기, blocks/s)

system:
  in : 인터럽트 수/초
  cs : 컨텍스트 스위치 수/초

cpu (% 단위):
  us : 사용자 모드
  sy : 시스템(커널) 모드
  id : 유휴 (idle)
  wa : I/O 대기
  st : 스틸 (가상화 환경에서 빼앗긴 시간)
```

### 2.3 핵심 지표 해석

```
주요 경고 신호:

1. r > CPU 코어 수
   -> CPU 병목, 프로세스가 CPU를 기다리고 있음

2. b > 0 (지속적으로)
   -> I/O 병목, 프로세스가 디스크를 기다리고 있음

3. si, so > 0
   -> 스와핑 발생, 메모리 부족 가능성

4. wa > 10%
   -> I/O 대기가 높음, 디스크 병목

5. cs가 급격히 증가
   -> 컨텍스트 스위칭 과다
```

### 2.4 실습: vmstat 모니터링

```bash
# 1초마다 10번 모니터링
vmstat 1 10

# 메모리 관련만 집중
vmstat 1 5 | awk '{print $3, $4, $5, $6}'

# 와이드 출력 (컬럼이 잘리지 않음)
vmstat -w 1 5

# 디스크 통계 포함
vmstat -d

# 타임스탬프 포함
vmstat -t 1 5
```

---

## Part 3: /proc/meminfo (45분)

### 3.1 meminfo 읽기

```bash
# 전체 내용
cat /proc/meminfo

# 주요 항목만
cat /proc/meminfo | grep -E "MemTotal|MemFree|MemAvailable|Buffers|Cached|SwapTotal|SwapFree"
```

### 3.2 주요 필드

```
MemTotal     : 전체 물리 메모리
MemFree      : 사용되지 않는 메모리
MemAvailable : 실제 사용 가능한 메모리 (free + 회수가능캐시)
Buffers      : 블록 디바이스 버퍼
Cached       : 파일 시스템 캐시
SwapTotal    : 전체 스왑
SwapFree     : 사용되지 않는 스왑
Active       : 최근 사용된 메모리
Inactive     : 최근 사용되지 않은 메모리
Dirty        : 디스크에 쓰기 대기 중인 메모리
```

### 3.3 실습: 메모리 상세 분석

```bash
# 메모리 요약 스크립트
cat << 'EOF' > memory_check.sh
#!/bin/bash
echo "=== Memory Summary ==="
awk '
/MemTotal/     {total=$2}
/MemAvailable/ {avail=$2}
/Buffers/      {buff=$2}
/^Cached/      {cache=$2}
/SwapTotal/    {swap_total=$2}
/SwapFree/     {swap_free=$2}
END {
    printf "Total:     %8d MB\n", total/1024
    printf "Available: %8d MB (%.1f%%)\n", avail/1024, avail/total*100
    printf "Buffers:   %8d MB\n", buff/1024
    printf "Cached:    %8d MB\n", cache/1024
    printf "Swap Used: %8d MB\n", (swap_total-swap_free)/1024
}
' /proc/meminfo
EOF

chmod +x memory_check.sh
./memory_check.sh
```

---

## Part 4: 종합 실습 (1시간 15분)

### 실습 1: 메모리 부하 시뮬레이션

```bash
# 터미널 1: 메모리 모니터링
vmstat 1

# 터미널 2: 메모리 부하 생성
stress --vm 1 --vm-bytes 500M --timeout 30s
# stress가 없으면: sudo apt install stress

# 또는 Python으로 메모리 할당
python3 -c "
import time
data = []
for i in range(50):
    data.append('x' * 10 * 1024 * 1024)  # 10MB씩
    print(f'Allocated: {(i+1)*10} MB')
    time.sleep(0.5)
time.sleep(10)
print('Done')
"
```

### 실습 2: 스왑 사용 모니터링

```bash
# 스왑 상태 확인
free -h

# 스왑 사용량 변화 모니터링
vmstat 1 | awk '{print $3, $7, $8}'  # swpd, si, so

# 스왑 사용 중인 프로세스 확인
for pid in $(ls /proc | grep -E '^[0-9]+$'); do
    if [ -f /proc/$pid/status ]; then
        swap=$(grep VmSwap /proc/$pid/status 2>/dev/null | awk '{print $2}')
        if [ -n "$swap" ] && [ "$swap" != "0" ]; then
            name=$(cat /proc/$pid/comm 2>/dev/null)
            echo "$swap kB - $name (PID: $pid)"
        fi
    fi
done | sort -rn | head -10
```

### 실습 3: 캐시 동작 확인

```bash
# 현재 캐시 상태
free -h

# 큰 파일 읽기 (캐시에 로드)
dd if=/dev/zero of=/tmp/testfile bs=1M count=500

# 캐시 증가 확인
free -h

# 파일 다시 읽기 (캐시에서)
time cat /tmp/testfile > /dev/null
# 첫 번째 읽기보다 빠름

# 캐시 정리 (root 필요)
sync
echo 3 | sudo tee /proc/sys/vm/drop_caches

# 캐시 감소 확인
free -h

# 정리
rm /tmp/testfile
```

### 실습 4: 메모리 사용량 상위 프로세스

```bash
# 메모리 사용량 상위 10개
ps aux --sort=-%mem | head -11

# 더 상세하게
ps -eo pid,ppid,cmd,%mem,%cpu --sort=-%mem | head -11

# /proc에서 직접 확인
for pid in $(ps -eo pid --sort=-%mem | tail -n +2 | head -5); do
    name=$(cat /proc/$pid/comm 2>/dev/null)
    rss=$(grep VmRSS /proc/$pid/status 2>/dev/null | awk '{print $2}')
    echo "PID $pid ($name): $rss kB"
done
```

---

## 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | free 기본 | free -h 출력 이해 | |
| 2 | available 이해 | free vs available 차이 | |
| 3 | vmstat 기본 | 각 컬럼 의미 이해 | |
| 4 | vmstat 경고 신호 | r, b, si, so, wa | |
| 5 | /proc/meminfo | 상세 메모리 정보 | |
| 6 | 메모리 부하 실습 | stress 또는 Python | |
| 7 | 캐시 동작 이해 | 캐시 생성/정리 | |
| 8 | 상위 프로세스 확인 | ps --sort=-%mem | |

---

## 핵심 명령어 정리

```bash
# 메모리 상태
free -h                    # 기본 확인
free -h -s 1              # 1초마다 갱신

# 시스템 상태
vmstat 1                   # 1초 간격 모니터링
vmstat -w 1               # 와이드 출력

# 상세 정보
cat /proc/meminfo | grep -E "MemTotal|MemAvailable|Cached"

# 프로세스별
ps aux --sort=-%mem | head
```

---

## 면접 대비 핵심 포인트

**Q1: free에서 available과 free의 차이는?**
> "free는 완전히 비어있는 메모리이고, available은 새 프로세스가 실제로 사용할 수 있는 메모리입니다. Linux는 여유 메모리를 캐시로 사용하므로, available = free + 회수 가능한 캐시입니다."

**Q2: vmstat에서 주의해야 할 지표는?**
> "r(실행 대기)이 CPU 코어 수보다 많으면 CPU 병목, si/so가 지속적으로 0보다 크면 스와핑으로 메모리 부족, wa가 높으면 I/O 대기가 많다는 의미입니다."

**Q3: 메모리가 부족할 때 어떻게 분석하나요?**
> "free -h로 전체 상태를 확인하고, ps --sort=-%mem으로 메모리 사용량이 높은 프로세스를 찾습니다. /proc/[pid]/status의 VmRSS로 실제 물리 메모리 사용량을 확인하고, vmstat으로 스와핑이 발생하는지 모니터링합니다."

---

## 학습 기록

```
학습일: 2026년 01월 11일
실제 소요 시간: 1시간
```
[블로그 정리 링크](https://dev-robinkim-93.tistory.com/36)

---

완료한 실습:
- [ ] free 명령어 실습
- [ ] vmstat 모니터링
- [ ] /proc/meminfo 분석
- [ ] 메모리 부하 테스트
- [ ] 캐시 동작 확인

이해가 어려웠던 부분:

추가 학습 필요 항목:
```

---

## 정리

```bash
# 생성한 파일 정리
rm -f memory_check.sh
rm -f /tmp/testfile
```

---

## 다음 학습: Day 4

주제: 디스크 I/O 분석 (iostat, iotop)
- iostat으로 디스크 성능 분석
- iotop으로 I/O 사용 프로세스 확인
- I/O 병목 진단