# 📅 Day 4: 디스크 I/O 분석 (iostat, iotop)

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "서비스 응답 지연" 장애 분석 시 디스크 I/O 병목 진단
> "OS, Network 등 다양한 레이어에서의 모니터링, 트러블슈팅 경험"

디스크 I/O가 느려서 발생하는 장애를 진단할 수 있어야 합니다. 데이터베이스, 로깅 시스템, 파일 기반 캐시 등 다양한 상황에서 디스크 I/O 병목이 성능 저하의 원인이 될 수 있습니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 핵심 개념 | 45분 | 디스크 I/O 기초, 지표 이해 |
| iostat 실습 | 1시간 | 디스크 상태 모니터링 |
| iotop 실습 | 45분 | 프로세스별 I/O 분석 |
| 실전 시나리오 | 1시간 | 병목 진단 및 해결 |
| 정리 | 30분 | 요약 및 복습 |

---

## 📚 Part 1: 핵심 개념 (45분)

### 1.1 디스크 I/O 병목이란?

```
┌─────────────────────────────────────────────────────────────────────┐
│  디스크 I/O 병목 장애 상황                                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  장애 신고: "API 응답 시간이 갑자기 느려졌어요"                      │
│                                                                      │
│  1차 확인 - CPU: 20% (정상)                                         │
│  2차 확인 - Memory: 60% (정상)                                      │
│  3차 확인 - Network: 정상                                           │
│                                                                      │
│  그런데... iostat 확인 결과:                                        │
│  ┌────────────────────────────────────────────────────────────┐     │
│  │  %util = 99%  ← 디스크가 거의 100% 사용 중!                │     │
│  │  await = 500ms ← I/O 요청당 500ms 대기 (정상: <10ms)       │     │
│  │  avgqu-sz = 50 ← 평균 대기 큐 길이 50 (정상: <1)           │     │
│  └────────────────────────────────────────────────────────────┘     │
│                                                                      │
│  원인 분석:                                                          │
│  - 로그 파일 과다 쓰기                                              │
│  - DB 쿼리로 인한 과도한 읽기                                       │
│  - 백업 작업과 서비스가 동시 실행                                   │
│                                                                      │
│  영향:                                                               │
│  - 모든 I/O 요청이 큐에서 대기                                      │
│  - 애플리케이션 응답 시간 급증                                      │
│  - 심하면 서비스 타임아웃 발생                                      │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 디스크 I/O 기초 개념

```
┌─────────────────────────────────────────────────────────────────────┐
│  디스크 I/O 동작 원리                                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  애플리케이션                                                        │
│       │                                                              │
│       │ read()/write() 시스템 콜                                    │
│       ▼                                                              │
│  ┌─────────────┐                                                    │
│  │ Page Cache  │ ← 메모리에서 캐시 (빠름)                          │
│  └──────┬──────┘                                                    │
│         │ 캐시 미스 또는 sync                                       │
│         ▼                                                            │
│  ┌─────────────┐                                                    │
│  │ I/O 스케줄러│ ← 요청 큐잉 및 최적화                              │
│  └──────┬──────┘                                                    │
│         │                                                            │
│         ▼                                                            │
│  ┌─────────────┐                                                    │
│  │ Block Layer │ ← 실제 디스크 I/O                                  │
│  └─────────────┘                                                    │
│                                                                      │
│  지연 발생 지점:                                                     │
│  1. Page Cache 미스 → 디스크 접근 필요                              │
│  2. I/O 스케줄러 큐 대기                                            │
│  3. 디스크 실제 작업 시간                                           │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.3 핵심 지표 상세 설명

| 지표 | 전체 이름 | 의미 | 정상 범위 | 이상 시 원인 |
|------|----------|------|----------|-------------|
| `%util` | Utilization | 디스크 사용률 | < 70% | 과도한 I/O 요청 |
| `await` | Average Wait | I/O 대기 시간(ms) | < 10ms (SSD), < 20ms (HDD) | 디스크 과부하 |
| `r/s` | Reads/second | 초당 읽기 요청 수 | 워크로드 의존 | DB 풀스캔, 캐시 미스 |
| `w/s` | Writes/second | 초당 쓰기 요청 수 | 워크로드 의존 | 로깅, 배치 작업 |
| `rkB/s` | Read KB/s | 초당 읽기 KB | 워크로드 의존 | 대용량 파일 읽기 |
| `wkB/s` | Write KB/s | 초당 쓰기 KB | 워크로드 의존 | 대용량 파일 쓰기 |
| `avgqu-sz` | Average Queue Size | 평균 대기 큐 길이 | < 1 | 요청 > 처리량 |
| `svctm` | Service Time | 실제 서비스 시간(ms) | < 5ms (SSD) | 디스크 성능 문제 |
| `%iowait` | I/O Wait | CPU가 I/O 대기 비율 | < 10% | 심각한 I/O 병목 |

```
┌─────────────────────────────────────────────────────────────────────┐
│  지표 간 관계                                                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  await = svctm + 큐 대기 시간                                       │
│                                                                      │
│  정상 상태:                                                          │
│  ┌─────────────────────────────────────────┐                        │
│  │ await (10ms) ≈ svctm (10ms)             │                        │
│  │ → 큐 대기 없이 바로 처리                 │                        │
│  └─────────────────────────────────────────┘                        │
│                                                                      │
│  병목 상태:                                                          │
│  ┌─────────────────────────────────────────┐                        │
│  │ await (500ms) >> svctm (10ms)           │                        │
│  │ → 490ms를 큐에서 대기                    │                        │
│  │ → 디스크 요청이 처리량 초과              │                        │
│  └─────────────────────────────────────────┘                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: iostat 실습 (1시간)

### 사전 준비

```bash
# sysstat 패키지 설치 (iostat 포함)
sudo apt install -y sysstat

# iotop 설치
sudo apt install -y iotop

# 설치 확인
which iostat iotop
```

---

### 실습 1: iostat 기본 사용법 (20분)

```bash
# 1초 간격으로 5번 출력
iostat -x 1 5

# 옵션 설명:
# -x : 확장 통계 출력
# 1  : 1초 간격
# 5  : 5번 반복
```

**출력 예시 및 해석**:
```
Linux 5.15.0-generic (server)     01/12/26     _x86_64_    (4 CPU)

avg-cpu:  %user   %nice %system %iowait  %steal   %idle
           5.20    0.00    2.10    1.50    0.00   91.20

Device            r/s     w/s     rkB/s     wkB/s   await  %util
sda              0.50   10.20      2.00     50.50    5.20   3.10
nvme0n1         10.00  100.00    100.00   1000.00    0.80   8.50
```

**각 필드 상세 해석**:
```
┌─────────────────────────────────────────────────────────────────────┐
│  CPU 섹션                                                           │
├─────────────────────────────────────────────────────────────────────┤
│  %user    : 사용자 프로세스 CPU 사용률                              │
│  %system  : 커널 CPU 사용률                                         │
│  %iowait  : ⚠️ CPU가 I/O 대기 중인 비율 (중요!)                     │
│  %idle    : 유휴 CPU                                                │
│                                                                      │
│  %iowait가 높으면 → 디스크가 병목!                                  │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│  Device 섹션                                                        │
├─────────────────────────────────────────────────────────────────────┤
│  r/s, w/s   : 초당 읽기/쓰기 요청 수 (IOPS)                        │
│  rkB/s, wkB/s: 초당 읽기/쓰기 처리량 (KB)                          │
│  await      : ⚠️ 평균 I/O 대기 시간 (ms) - 핵심 지표!              │
│  %util      : ⚠️ 디스크 사용률 (100%에 가까우면 병목)              │
└─────────────────────────────────────────────────────────────────────┘
```

---

### 실습 2: 디스크별 상세 분석 (20분)

```bash
# 특정 디스크만 모니터링
iostat -x -d sda 1 5

# 옵션:
# -d : 디바이스 통계만 (CPU 제외)

# 모든 디스크 + I/O 없는 디바이스 제외
iostat -xz 1

# 옵션:
# -z : I/O 활동 없는 디바이스 숨김
```

**실습: 여러 디스크 환경**:
```bash
# 디스크 목록 확인
lsblk

# 출력 예시:
# NAME   MAJ:MIN RM   SIZE RO TYPE MOUNTPOINT
# sda      8:0    0   100G  0 disk 
# ├─sda1   8:1    0    99G  0 part /
# └─sda2   8:2    0     1G  0 part [SWAP]
# nvme0n1 259:0   0   500G  0 disk /data

# 각 디스크 I/O 비교
iostat -x sda nvme0n1 1 5
```

---

### 실습 3: 지속적 모니터링 (20분)

```bash
# 실시간 모니터링 (무한 반복)
iostat -x 1 | tee /tmp/iostat.log

# 백그라운드 모니터링 (로그 저장)
nohup iostat -x 1 > /var/log/iostat_$(date +%Y%m%d).log 2>&1 &

# 특정 시간 동안만 (60초)
timeout 60 iostat -x 1 > /tmp/iostat_60s.log
```

**분석용 스크립트**:
```bash
#!/bin/bash
# disk_monitor.sh - 디스크 이상 감지 스크립트

THRESHOLD_UTIL=70
THRESHOLD_AWAIT=20

while true; do
    # iostat 결과에서 %util과 await 추출
    iostat -x 1 1 | tail -n +7 | while read line; do
        DEVICE=$(echo $line | awk '{print $1}')
        AWAIT=$(echo $line | awk '{print $10}' | cut -d'.' -f1)
        UTIL=$(echo $line | awk '{print $NF}' | cut -d'.' -f1)
        
        if [ ! -z "$UTIL" ] && [ "$UTIL" != "Device" ]; then
            if [ "$UTIL" -gt $THRESHOLD_UTIL ] 2>/dev/null; then
                echo "[WARN] $(date '+%Y-%m-%d %H:%M:%S') $DEVICE: util=$UTIL% (>$THRESHOLD_UTIL%)"
            fi
            if [ "$AWAIT" -gt $THRESHOLD_AWAIT ] 2>/dev/null; then
                echo "[WARN] $(date '+%Y-%m-%d %H:%M:%S') $DEVICE: await=${AWAIT}ms (>$THRESHOLD_AWAIT ms)"
            fi
        fi
    done
    sleep 5
done
```

---

## 🛠️ Part 3: iotop 실습 (45분)

### 실습 4: iotop 기본 사용 (20분)

```bash
# iotop 실행 (root 권한 필요)
sudo iotop

# 키보드 단축키:
# o : 실제 I/O 발생하는 프로세스만 표시 (필수!)
# a : 누적 I/O 표시
# r : 정렬 순서 반전
# Left/Right : 정렬 컬럼 변경
# q : 종료
```

**출력 예시**:
```
Total DISK READ :      10.00 M/s | Total DISK WRITE :      50.00 M/s
Actual DISK READ:       8.00 M/s | Actual DISK WRITE:      45.00 M/s
    TID  PRIO  USER     DISK READ  DISK WRITE  SWAPIN     IO>    COMMAND
   1234 be/4  mysql       10.00 M/s   50.00 M/s  0.00 %  85.00 % mysqld
   5678 be/4  nginx        0.00 B/s    5.00 M/s  0.00 %  10.00 % nginx: worker
```

**출력 해석**:
```
┌─────────────────────────────────────────────────────────────────────┐
│  iotop 출력 해석                                                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Total DISK READ/WRITE  : 전체 디스크 읽기/쓰기 속도                │
│  Actual DISK READ/WRITE : 실제 디스크 I/O (캐시 제외)               │
│                                                                      │
│  TID        : 스레드 ID                                             │
│  PRIO       : I/O 우선순위 (be=best effort, rt=realtime)            │
│  DISK READ  : 해당 프로세스의 읽기 속도                             │
│  DISK WRITE : 해당 프로세스의 쓰기 속도                             │
│  IO>        : ⚠️ I/O 대기 비율 (높으면 해당 프로세스가 병목)        │
│                                                                      │
│  분석 포인트:                                                        │
│  - IO% 높은 프로세스 = 디스크 병목 유발자                           │
│  - DISK WRITE 높은 프로세스 = 디스크 쓰기 부하 원인                 │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

### 실습 5: iotop 배치 모드 (15분)

```bash
# 배치 모드 (스크립트/로그용)
sudo iotop -b -n 5

# 옵션:
# -b : 배치 모드 (비대화형)
# -n 5 : 5번 반복 후 종료

# 실제 I/O만 + 배치 모드
sudo iotop -o -b -n 5

# 특정 사용자의 I/O만
sudo iotop -u mysql -b -n 5

# 특정 PID의 I/O만
sudo iotop -p 1234 -b -n 5
```

**로그 저장 예시**:
```bash
# I/O 헤비 프로세스 로그 저장
sudo iotop -o -b -n 60 > /tmp/iotop_$(date +%Y%m%d_%H%M%S).log 2>&1
```

---

### 실습 6: I/O 부하 시뮬레이션 및 분석 (10분)

```bash
# 터미널 1: iostat 모니터링
watch -n 1 "iostat -x | tail -5"

# 터미널 2: iotop 모니터링
sudo iotop -o

# 터미널 3: 쓰기 부하 발생 (500MB 파일)
dd if=/dev/zero of=/tmp/testfile bs=1M count=500 oflag=direct

# 터미널 3: 읽기 부하 발생
dd if=/tmp/testfile of=/dev/null bs=1M iflag=direct

# 정리
rm /tmp/testfile
```

**`oflag=direct, iflag=direct` 의미**:
- Page Cache 우회
- 실제 디스크 I/O 직접 발생
- 테스트/벤치마크에 유용

---

## 🛠️ Part 4: 실전 시나리오 (1시간)

### 시나리오 1: 데이터베이스 I/O 병목

```
┌─────────────────────────────────────────────────────────────────────┐
│  시나리오: MySQL 쿼리로 인한 디스크 병목                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  증상: 특정 시간대에 API 응답 느림                                  │
│                                                                      │
│  iostat 결과:                                                        │
│  Device    r/s     w/s    await  %util                              │
│  sda     1500.0   100.0   150.0   99.5  ← 읽기가 매우 많음!        │
│                                                                      │
│  iotop 결과:                                                         │
│  TID   USER    DISK READ   IO%   COMMAND                            │
│  1234  mysql   100 M/s     90%   mysqld                             │
│                                                                      │
│  원인 분석:                                                          │
│  - MySQL에서 대량 읽기 발생                                         │
│  - 풀 테이블 스캔 쿼리 의심                                         │
│  - 인덱스 미사용 또는 캐시 효율 낮음                                │
│                                                                      │
│  해결 방안:                                                          │
│  1. MySQL 슬로우 쿼리 로그 확인                                     │
│  2. EXPLAIN으로 쿼리 실행 계획 분석                                 │
│  3. 인덱스 추가                                                     │
│  4. 쿼리 캐시 또는 Redis 캐시 도입                                  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

**진단 명령어**:
```bash
# MySQL I/O 상태 확인
sudo iotop -p $(pgrep -d',' mysqld) -b -n 5

# MySQL 슬로우 쿼리 로그 (있다면)
tail -f /var/log/mysql/slow-query.log

# MySQL 현재 실행 중인 쿼리
mysql -e "SHOW FULL PROCESSLIST"
```

---

### 시나리오 2: 로그 파일 과다 쓰기

```
┌─────────────────────────────────────────────────────────────────────┐
│  시나리오: 애플리케이션 로그로 인한 쓰기 병목                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  증상: 갑자기 전체 서버 성능 저하                                   │
│                                                                      │
│  iostat 결과:                                                        │
│  Device    r/s      w/s    wkB/s   await  %util                     │
│  sda       10.0   5000.0   50000   200.0   99.9                     │
│            ↑         ↑                                               │
│          정상    비정상 (쓰기 폭발)                                  │
│                                                                      │
│  iotop 결과:                                                         │
│  TID   USER      DISK WRITE  IO%   COMMAND                          │
│  5678  java      50 M/s      95%   java -jar app.jar                │
│                                                                      │
│  추가 확인:                                                          │
│  $ ls -lh /var/log/app/                                             │
│  -rw-r--r-- app.log    500M   ← 최근 10분에 500MB 로그!            │
│                                                                      │
│  원인 분석:                                                          │
│  - DEBUG 레벨 로그가 활성화됨                                       │
│  - 특정 오류로 반복 로깅                                            │
│  - 로그 로테이션 미설정                                             │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

**진단 및 해결**:
```bash
# 로그 파일 크기 확인
find /var/log -type f -size +100M -exec ls -lh {} \;

# 최근 변경된 큰 파일
find /var/log -type f -mmin -10 -size +10M -exec ls -lh {} \;

# 실시간 로그 쓰기 속도 확인
watch -n 1 "ls -lh /var/log/app/app.log"

# 로그 레벨 변경 (애플리케이션에 따라)
# 예: Spring Boot
curl -X POST http://localhost:8080/actuator/loggers/ROOT \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "WARN"}'

# 로그 파일 비우기 (임시 조치)
truncate -s 0 /var/log/app/app.log
```

---

### 시나리오 3: 백업과 서비스 I/O 충돌

```
┌─────────────────────────────────────────────────────────────────────┐
│  시나리오: 백업 작업과 서비스 I/O 경쟁                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  증상: 매일 새벽 2시에 서비스 느려짐                                │
│                                                                      │
│  crontab 확인:                                                       │
│  0 2 * * * tar czf /backup/db.tar.gz /var/lib/mysql                 │
│                                                                      │
│  새벽 2시 iostat:                                                    │
│  Device    r/s      w/s    await  %util                             │
│  sda     2000.0   500.0    300.0   99.9                             │
│                                                                      │
│  iotop 결과:                                                         │
│  TID    USER    DISK READ  DISK WRITE  COMMAND                      │
│  9999   root    100 M/s    50 M/s      tar                          │
│  1234   mysql    10 M/s     5 M/s      mysqld                       │
│                                                                      │
│  문제:                                                               │
│  - tar가 대량 읽기 → mysqld I/O 대기                                │
│  - 서비스 응답 지연                                                 │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

**해결 방안**:
```bash
# 방법 1: ionice로 백업 우선순위 낮춤
# 백업 스크립트 수정
ionice -c3 tar czf /backup/db.tar.gz /var/lib/mysql
# -c3 = idle 클래스 (다른 I/O 없을 때만 실행)

# 방법 2: 백업 I/O 속도 제한 (pv 사용)
tar cf - /var/lib/mysql | pv -L 50m | gzip > /backup/db.tar.gz
# -L 50m = 50MB/s로 제한

# 방법 3: 서비스 트래픽 없는 시간대로 변경
# crontab 수정
0 4 * * * ionice -c3 /usr/local/bin/backup.sh

# 방법 4: 별도 디스크로 백업 대상 분리
```

---

### 실습 7: /proc에서 I/O 통계 확인 (20분)

```bash
# 프로세스별 I/O 통계 (정밀 분석용)
cat /proc/$$/io

# 출력 예시:
# rchar: 1000000     ← 읽은 바이트 (캐시 포함)
# wchar: 500000      ← 쓴 바이트 (캐시 포함)
# syscr: 100         ← read 시스템 콜 횟수
# syscw: 50          ← write 시스템 콜 횟수
# read_bytes: 50000  ← 실제 디스크에서 읽은 바이트
# write_bytes: 25000 ← 실제 디스크에 쓴 바이트
```

**프로세스 I/O 분석 스크립트**:
```bash
#!/bin/bash
# process_io.sh - 프로세스 I/O 분석

PID=$1
if [ -z "$PID" ]; then
    echo "Usage: $0 <pid>"
    exit 1
fi

echo "=== Process $PID I/O Statistics ==="
echo ""

# 기본 프로세스 정보
echo "Process: $(cat /proc/$PID/comm 2>/dev/null)"
echo ""

# I/O 통계
if [ -f /proc/$PID/io ]; then
    echo "=== I/O Stats ==="
    cat /proc/$PID/io
    echo ""
    
    # 읽기/쓰기 비율 계산
    READ_BYTES=$(grep read_bytes /proc/$PID/io | awk '{print $2}')
    WRITE_BYTES=$(grep write_bytes /proc/$PID/io | awk '{print $2}')
    
    echo "=== Summary ==="
    echo "Actual Disk Read:  $(numfmt --to=iec $READ_BYTES 2>/dev/null || echo $READ_BYTES)"
    echo "Actual Disk Write: $(numfmt --to=iec $WRITE_BYTES 2>/dev/null || echo $WRITE_BYTES)"
else
    echo "Cannot read /proc/$PID/io (permission denied or process not exists)"
fi
```

**사용 예시**:
```bash
# MySQL 프로세스 I/O 확인
sudo ./process_io.sh $(pgrep mysqld)

# 모든 Java 프로세스 I/O 확인
for pid in $(pgrep java); do
    echo "--- PID: $pid ---"
    sudo ./process_io.sh $pid
done
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | `iostat -x` 출력 이해 | %util, await, r/s, w/s | ☐ |
| 2 | %util과 await 관계 이해 | 병목 판단 기준 | ☐ |
| 3 | `iotop` 기본 사용 | 프로세스별 I/O 확인 | ☐ |
| 4 | 배치 모드 iotop | -b 옵션, 로그 저장 | ☐ |
| 5 | `dd`로 I/O 부하 테스트 | oflag/iflag=direct | ☐ |
| 6 | `/proc/[pid]/io` 확인 | 프로세스별 정밀 분석 | ☐ |
| 7 | ionice 이해 | I/O 우선순위 제어 | ☐ |
| 8 | 실전 시나리오 분석 | DB, 로그, 백업 병목 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# 디스크 I/O 모니터링 (필수!)
iostat -x 1                     # 1초 간격, 확장 통계

# 프로세스별 I/O (필수!)
sudo iotop -o                   # 실제 I/O 발생 프로세스만

# 배치 모드 (스크립트용)
sudo iotop -o -b -n 5           # 5번 반복 후 종료

# 프로세스별 정밀 분석
cat /proc/<pid>/io              # 읽기/쓰기 바이트

# 디스크 부하 테스트
dd if=/dev/zero of=/tmp/test bs=1M count=100 oflag=direct

# I/O 우선순위 제어
ionice -c3 <command>            # 가장 낮은 우선순위로 실행
```

---

## 💡 면접 대비 핵심 포인트

### Q1: 서버 응답이 느린데 CPU/메모리는 정상입니다. 어떻게 진단하시겠습니까?

**A**: "먼저 `iostat -x`로 디스크 I/O 상태를 확인합니다. `%util`이 높거나 `await`가 길면 디스크 병목입니다. `iotop`으로 어떤 프로세스가 I/O를 많이 발생시키는지 확인하고, 원인에 따라 쿼리 최적화, 로그 레벨 조정, 캐싱 도입 등으로 해결합니다."

### Q2: iostat의 %util과 await의 차이는?

**A**: "`%util`은 디스크가 작업 중인 시간 비율이고, `await`는 I/O 요청당 평균 대기 시간입니다. %util이 높아도 await가 낮으면 괜찮지만, %util이 높고 await도 높으면 심각한 병목입니다."

### Q3: 백업 작업이 서비스 성능에 영향을 줄 때 어떻게 하시겠습니까?

**A**: "`ionice -c3`으로 백업 I/O 우선순위를 낮추거나, `pv -L`로 I/O 속도를 제한합니다. 장기적으로는 별도 디스크나 스냅샷 기반 백업으로 전환합니다."

---

## 📝 학습 기록

```
학습일: 2026년 01월 10일
실제 소요 시간: 1시간
```
[블로그 정리 링크](https://dev-robinkim-93.tistory.com/37)

---

완료한 실습:
- [ ] iostat 기본 사용
- [ ] iotop 프로세스별 I/O 분석
- [ ] dd로 I/O 부하 테스트
- [ ] /proc/[pid]/io 분석
- [ ] 시나리오별 분석

이해가 어려웠던 부분:


추가 학습 필요 항목:

```

---

## ➡️ 다음 학습: Day 5

**주제**: 네트워크 분석 기초 (ss, netstat)
- TCP 연결 상태 이해
- ss 명령어 활용
- TIME_WAIT, CLOSE_WAIT 분석
