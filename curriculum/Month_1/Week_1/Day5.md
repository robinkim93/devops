# 📅 Day 5: 네트워크 분석 기초 (ss, netstat)

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "Network 레이어에서의 트러블슈팅 경험"
> "대규모의 실시간 트래픽을 처리하는 인프라의 운영 경험"

소켓 상태 확인, 연결 문제 진단, 포트 사용 현황 분석을 할 수 있어야 합니다. 마이크로서비스 환경에서 서비스 간 통신 문제를 진단하는 핵심 스킬입니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 핵심 개념 | 45분 | TCP 연결 상태, 소켓 기초 |
| ss 명령어 실습 | 1.5시간 | 다양한 옵션과 필터 |
| 연결 분석 실습 | 1시간 | TIME_WAIT, CLOSE_WAIT |
| 실전 시나리오 | 45분 | 장애 진단 실습 |

---

## 📚 Part 1: 핵심 개념 (45분)

### 1.1 TCP 연결 상태

```
┌─────────────────────────────────────────────────────────────────────┐
│  TCP 연결 상태 다이어그램                                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│    클라이언트                              서버                      │
│        │                                    │                        │
│        │                                    │ LISTEN                 │
│        │ ──────── SYN ─────────────────>   │                        │
│        │ SYN_SENT                           │                        │
│        │ <────── SYN+ACK ──────────────    │ SYN_RECEIVED           │
│        │ ──────── ACK ─────────────────>   │                        │
│        │ ESTABLISHED                        │ ESTABLISHED            │
│        │                                    │                        │
│        │ ═══════ 데이터 통신 ═══════════   │                        │
│        │                                    │                        │
│        │ ──────── FIN ─────────────────>   │                        │
│        │ FIN_WAIT_1                         │                        │
│        │ <────── ACK ──────────────────    │ CLOSE_WAIT             │
│        │ FIN_WAIT_2                         │                        │
│        │ <────── FIN ──────────────────    │ LAST_ACK               │
│        │ ──────── ACK ─────────────────>   │                        │
│        │ TIME_WAIT (2분 대기)               │ CLOSED                 │
│        │ CLOSED                             │                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 주요 TCP 상태와 의미

| 상태 | 설명 | 정상/비정상 | 주의 사항 |
|------|------|-----------|----------|
| `LISTEN` | 연결 대기 중 (서버) | 정상 | 예상 포트가 열려있는지 확인 |
| `ESTABLISHED` | 연결됨, 통신 중 | 정상 | 개수가 적절한지 확인 |
| `TIME_WAIT` | 연결 종료 후 대기 | 정상 (적당량) | ⚠️ 과다 시 포트 고갈 |
| `CLOSE_WAIT` | 상대가 FIN 보냄, 나는 안 닫음 | ⚠️ 비정상 | 앱 버그! 소켓 누수 |
| `SYN_SENT` | SYN 보냈고 응답 대기 | 잠시 | 지속 시 연결 불가 |
| `SYN_RECEIVED` | SYN 받고 SYN+ACK 보냄 | 잠시 | 과다 시 SYN Flood 공격 |
| `FIN_WAIT_1` | FIN 보냈고 ACK 대기 | 잠시 | 지속 시 네트워크 문제 |
| `FIN_WAIT_2` | FIN의 ACK 받음 | 잠시 | 지속 시 상대방 문제 |
| `LAST_ACK` | FIN 보냈고 마지막 ACK 대기 | 잠시 | 지속 시 네트워크 문제 |

### 1.3 문제 상황별 진단

```
┌─────────────────────────────────────────────────────────────────────┐
│  문제 상황 진단 가이드                                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  🔴 TIME_WAIT가 많다 (예: 10,000개 이상)                            │
│  ├── 원인: 연결을 너무 자주 맺고 끊음                               │
│  ├── 영향: 포트 고갈 → 새 연결 불가                                 │
│  └── 해결:                                                           │
│      • 커넥션 풀 사용 (DB, HTTP)                                    │
│      • HTTP Keep-Alive 활성화                                       │
│      • net.ipv4.tcp_tw_reuse=1 설정                                 │
│                                                                      │
│  🔴 CLOSE_WAIT가 많다 (예: 100개 이상)                              │
│  ├── 원인: 애플리케이션이 소켓을 close() 안 함                      │
│  ├── 영향: FD 누수 → 서비스 장애                                    │
│  └── 해결:                                                           │
│      • 애플리케이션 코드 수정                                       │
│      • 해당 프로세스 재시작 (임시)                                  │
│                                                                      │
│  🔴 ESTABLISHED가 비정상적으로 많다                                 │
│  ├── 원인: 요청 폭증 또는 응답 지연                                 │
│  ├── 영향: 서버 과부하, 메모리 부족                                 │
│  └── 해결:                                                           │
│      • 스케일 아웃                                                   │
│      • 응답 지연 원인 분석                                          │
│                                                                      │
│  🔴 SYN_RECEIVED가 많다                                             │
│  ├── 원인: SYN Flood 공격 또는 서버 처리 지연                       │
│  ├── 영향: 정상 연결 불가                                           │
│  └── 해결:                                                           │
│      • SYN Cookies 활성화                                           │
│      • 방화벽에서 rate limit                                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.4 ss vs netstat

```
┌─────────────────────────────────────────────────────────────────────┐
│  ss vs netstat 비교                                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  netstat (구형)                                                      │
│  ├── 느림 (모든 정보를 /proc에서 파싱)                              │
│  ├── deprecated (RHEL 7+에서 제거)                                  │
│  └── 익숙한 사람이 많음                                             │
│                                                                      │
│  ss (신형) ← 이것을 사용하세요!                                     │
│  ├── 빠름 (커널에서 직접 정보 획득)                                 │
│  ├── 더 많은 정보 (TCP 내부 상태)                                   │
│  ├── 강력한 필터링                                                  │
│  └── iproute2 패키지에 포함                                         │
│                                                                      │
│  성능 비교 (10,000 소켓 기준):                                       │
│  netstat: ~3초                                                       │
│  ss: ~0.1초 (30배 빠름)                                             │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: ss 명령어 실습 (1.5시간)

### 실습 1: 리스닝 포트 확인 (30분)

```bash
# TCP 리스닝 포트 (가장 자주 사용!)
ss -tlnp

# 옵션 상세 설명:
# -t : TCP만 표시
# -l : LISTEN 상태만 표시
# -n : 숫자로 표시 (DNS 조회 안 함) → 빠름!
# -p : 프로세스 정보 포함 (root 권한 필요)
```

**출력 예시**:
```
State    Recv-Q   Send-Q   Local Address:Port    Peer Address:Port   Process
LISTEN   0        128      0.0.0.0:22            0.0.0.0:*           users:(("sshd",pid=1234,fd=3))
LISTEN   0        511      0.0.0.0:80            0.0.0.0:*           users:(("nginx",pid=5678,fd=6))
LISTEN   0        511      127.0.0.1:3306        0.0.0.0:*           users:(("mysqld",pid=9012,fd=21))
```

**출력 해석**:
```
┌─────────────────────────────────────────────────────────────────────┐
│  ss -tlnp 출력 해석                                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  State : 소켓 상태 (LISTEN = 연결 대기)                             │
│                                                                      │
│  Recv-Q (LISTEN 상태에서):                                          │
│  └── 완료된 연결 중 accept() 안 된 개수                             │
│  └── 높으면 → 앱이 연결을 빨리 처리 못함                           │
│                                                                      │
│  Send-Q (LISTEN 상태에서):                                          │
│  └── backlog (대기열 크기)                                          │
│  └── 예: 128 = 최대 128개 연결 대기 가능                            │
│                                                                      │
│  Local Address:Port:                                                 │
│  └── 0.0.0.0:22 = 모든 IP에서 22번 포트                             │
│  └── 127.0.0.1:3306 = localhost에서만 3306                          │
│                                                                      │
│  Peer Address:Port:                                                  │
│  └── 0.0.0.0:* = 모든 곳에서 연결 가능                              │
│                                                                      │
│  Process:                                                            │
│  └── 해당 포트를 사용하는 프로세스 정보                             │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

**실습 과제**:
```bash
# 1. 현재 시스템에서 열린 모든 TCP 포트 확인
ss -tlnp

# 2. UDP 포트도 포함
ss -tulnp

# 3. 특정 포트(80)가 열려있는지 확인
ss -tlnp | grep ":80"

# 4. 특정 프로세스(nginx)의 포트 확인
ss -tlnp | grep nginx

# 5. IPv4만 확인
ss -4tlnp

# 6. IPv6만 확인
ss -6tlnp
```

---

### 실습 2: 연결된 소켓 확인 (30분)

```bash
# 모든 TCP 연결 (ESTABLISHED 포함)
ss -tnp

# 특정 포트만 필터링
ss -tnp | grep ":80"

# 특정 상태만 필터링 (ss 내장 필터)
ss -tn state established
ss -tn state time-wait
ss -tn state close-wait

# 여러 상태 조합
ss -tn state established state time-wait
```

**ESTABLISHED 연결 분석**:
```bash
# 어떤 클라이언트가 연결되어 있는지
ss -tn state established | awk '{print $4}' | cut -d: -f1 | sort | uniq -c | sort -rn | head

# 출력 예시:
#    150 192.168.1.100  ← 이 IP에서 150개 연결
#     50 192.168.1.101
#     30 10.0.0.50

# 특정 서비스(80)에 연결된 클라이언트
ss -tn state established '( dport = :80 )' | awk '{print $4}' | sort | uniq -c | sort -rn
```

---

### 실습 3: 소켓 통계 요약 (15분)

```bash
# 전체 소켓 통계 요약 (필수!)
ss -s

# 출력 예시:
Total: 500
TCP:   300 (estab 150, closed 20, orphaned 5, timewait 100)
UDP:   50
```

**출력 해석**:
```
┌─────────────────────────────────────────────────────────────────────┐
│  ss -s 출력 해석                                                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Total: 500                                                          │
│  └── 전체 소켓 수                                                   │
│                                                                      │
│  TCP: 300                                                            │
│  ├── estab 150    : ESTABLISHED 연결 수 (정상 통신 중)              │
│  ├── closed 20    : CLOSED 상태                                     │
│  ├── orphaned 5   : ⚠️ 프로세스 없는 소켓 (비정상!)                 │
│  └── timewait 100 : TIME_WAIT 수 (많으면 주의)                      │
│                                                                      │
│  주의 포인트:                                                        │
│  - orphaned > 0 : 소켓 누수, 프로세스 크래시                        │
│  - timewait > 10000 : 연결 재사용 문제                              │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

### 실습 4: 고급 필터링 (15분)

```bash
# ss 필터 문법
ss -tn '( dport = :80 )'                    # 목적지 포트 80
ss -tn '( sport = :22 )'                    # 출발지 포트 22
ss -tn '( dst = 192.168.1.100 )'            # 목적지 IP
ss -tn '( src = 10.0.0.1 )'                 # 출발지 IP

# 조합
ss -tn '( dport = :80 or dport = :443 )'   # 80 또는 443
ss -tn '( dst = 192.168.1.0/24 )'          # 서브넷 필터

# 상태 + IP 조합
ss -tn state established '( dst = 192.168.1.100 )'
```

---

## 🛠️ Part 3: 연결 문제 분석 (1시간)

### 실습 5: TIME_WAIT 분석 (30분)

```bash
# TIME_WAIT 개수 확인
ss -tn state time-wait | wc -l

# TIME_WAIT가 어떤 목적지로 많은지
ss -tn state time-wait | awk '{print $4}' | cut -d: -f1 | sort | uniq -c | sort -rn | head

# 출력 예시:
#  5000 192.168.1.100    ← 이 서버로 연결을 너무 자주 맺고 끊음
#  3000 192.168.1.101
#   500 10.0.0.50
```

**TIME_WAIT 문제 해결**:
```bash
# 현재 TIME_WAIT 재사용 설정 확인
cat /proc/sys/net/ipv4/tcp_tw_reuse

# TIME_WAIT 소켓 재사용 활성화 (클라이언트 역할 시 효과)
# /etc/sysctl.conf에 추가
echo "net.ipv4.tcp_tw_reuse = 1" | sudo tee -a /etc/sysctl.conf
sudo sysctl -p

# 최대 TIME_WAIT 소켓 수 확인
cat /proc/sys/net/ipv4/tcp_max_tw_buckets

# 로컬 포트 범위 확인
cat /proc/sys/net/ipv4/ip_local_port_range
# 예: 32768 60999 → 약 28,000개 포트 사용 가능

# 포트 범위 확장
echo "1024 65535" | sudo tee /proc/sys/net/ipv4/ip_local_port_range
```

**TIME_WAIT 발생 주요 원인과 해결**:
```
┌─────────────────────────────────────────────────────────────────────┐
│  TIME_WAIT 과다 원인 및 해결                                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  원인 1: DB 커넥션 풀 미사용                                        │
│  ├── 매 쿼리마다 연결/해제                                          │
│  └── 해결: Connection Pool 설정 (HikariCP 등)                       │
│                                                                      │
│  원인 2: HTTP Keep-Alive 비활성화                                   │
│  ├── 매 요청마다 TCP 연결                                           │
│  └── 해결: Keep-Alive 활성화, 적절한 timeout 설정                   │
│                                                                      │
│  원인 3: 마이크로서비스 간 과도한 호출                              │
│  ├── 서비스 간 매번 새 연결                                         │
│  └── 해결: gRPC, HTTP/2 사용, 연결 재사용                           │
│                                                                      │
│  원인 4: 부하 테스트 후                                             │
│  ├── 대량 연결 후 종료 → TIME_WAIT 폭발                            │
│  └── 해결: 테스트 후 자연 해소 대기 (2분)                           │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

### 실습 6: CLOSE_WAIT 분석 (30분)

```bash
# CLOSE_WAIT 개수 확인
ss -tn state close-wait | wc -l

# CLOSE_WAIT를 가진 프로세스 확인 (핵심!)
ss -tnp state close-wait

# 출력 예시:
# CLOSE-WAIT  0  0  192.168.1.10:45678  192.168.1.20:80  users:(("java",pid=1234,fd=25))
```

**CLOSE_WAIT 문제 진단**:
```bash
# 어떤 프로세스가 CLOSE_WAIT를 많이 가지고 있는지
ss -tnp state close-wait | awk -F'"' '{print $2}' | sort | uniq -c | sort -rn

# 출력 예시:
#   50 java      ← java 프로세스에서 소켓 누수 발생!
#    5 python

# 해당 프로세스의 열린 파일(소켓 포함) 확인
lsof -p 1234 | grep -E "IPv4|IPv6"

# 해당 프로세스의 FD 수 확인
ls /proc/1234/fd | wc -l
```

**CLOSE_WAIT 문제 원인**:
```
┌─────────────────────────────────────────────────────────────────────┐
│  CLOSE_WAIT 문제 (심각!)                                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  상황:                                                               │
│  1. 상대방이 연결을 종료 (FIN 전송)                                 │
│  2. 우리 쪽에서 ACK는 보냄                                          │
│  3. 하지만 우리 애플리케이션이 close()를 안 함!                     │
│  4. 소켓이 CLOSE_WAIT 상태로 계속 유지                              │
│                                                                      │
│  원인 (대부분 애플리케이션 버그):                                    │
│  ├── 소켓/스트림 close() 누락                                       │
│  ├── 예외 처리에서 finally 블록 누락                                │
│  └── 커넥션 풀 리소스 해제 실패                                     │
│                                                                      │
│  영향:                                                               │
│  ├── 파일 디스크립터(FD) 누수                                       │
│  ├── 메모리 누수                                                    │
│  └── 결국 "Too many open files" 에러 발생                           │
│                                                                      │
│  해결:                                                               │
│  ├── 단기: 해당 프로세스 재시작                                     │
│  └── 근본: 코드 수정 (try-with-resources 등)                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 4: 실전 시나리오 (45분)

### 시나리오 1: 서비스 연결 불가

```bash
# 문제: 애플리케이션이 DB에 연결 못함
# "Connection refused" 에러

# 1단계: DB 포트가 열려있는지 확인
ss -tlnp | grep 3306

# 결과 1: 아무것도 없음 → MySQL이 실행 안 됨
sudo systemctl status mysql
sudo systemctl start mysql

# 결과 2: 127.0.0.1:3306만 LISTEN → localhost만 허용
# 해결: MySQL bind-address 설정 변경

# 2단계: 방화벽 확인
sudo iptables -L -n | grep 3306
sudo ufw status | grep 3306

# 3단계: 실제 연결 테스트
nc -zv db-server 3306
```

### 시나리오 2: 연결 지연/타임아웃

```bash
# 문제: 특정 서비스로의 연결이 느림

# 1단계: SYN_SENT 상태 확인
ss -tn state syn-sent

# 결과: SYN_SENT가 많음 → 상대방이 응답 안 함
#  원인: 상대 서버 과부하, 네트워크 문제, 방화벽

# 2단계: Recv-Q 확인 (서버 측)
ss -tlnp

# Recv-Q가 높음 → 서버가 accept()를 빨리 못함
# 원인: 애플리케이션 처리 지연

# 3단계: RTT 확인
ss -ti state established '( dst = 192.168.1.100 )'

# 출력에서 rtt 확인
# rtt:100/50  → 100ms RTT (높으면 네트워크 지연)
```

### 시나리오 3: 연결 수 모니터링

```bash
#!/bin/bash
# connection_monitor.sh - 연결 상태 실시간 모니터링

while true; do
    clear
    echo "=== $(date '+%Y-%m-%d %H:%M:%S') ==="
    echo ""
    
    # 전체 통계
    echo "=== Socket Summary ==="
    ss -s | head -4
    echo ""
    
    # 상태별 카운트
    echo "=== TCP States ==="
    echo "ESTABLISHED: $(ss -tn state established | wc -l)"
    echo "TIME_WAIT:   $(ss -tn state time-wait | wc -l)"
    echo "CLOSE_WAIT:  $(ss -tn state close-wait | wc -l)"
    echo "SYN_SENT:    $(ss -tn state syn-sent | wc -l)"
    echo "SYN_RECV:    $(ss -tn state syn-recv | wc -l)"
    echo ""
    
    # 포트별 연결 수 (상위 5개)
    echo "=== Top Ports by Connection ==="
    ss -tn state established | awk '{print $4}' | cut -d: -f2 | sort | uniq -c | sort -rn | head -5
    
    sleep 5
done
```

### 시나리오 4: 특정 서비스 디버깅

```bash
# nginx 관련 모든 소켓 정보
ss -tlnp | grep nginx                       # 리스닝 포트
ss -tn state established | grep nginx       # 활성 연결
ss -tn '( sport = :80 or sport = :443 )'    # 80, 443 연결

# 연결된 클라이언트 IP 목록 (상위 10개)
ss -tn '( sport = :80 )' | awk '{print $5}' | cut -d: -f1 | sort | uniq -c | sort -rn | head -10

# 특정 클라이언트의 연결 수
ss -tn '( dst = 192.168.1.100 )' | wc -l
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | TCP 연결 상태 이해 | LISTEN, ESTABLISHED, TIME_WAIT, CLOSE_WAIT | ☐ |
| 2 | `ss -tlnp` 사용 | 리스닝 포트 확인 | ☐ |
| 3 | `ss -tnp` 사용 | 활성 연결 확인 | ☐ |
| 4 | `ss -s` 사용 | 소켓 통계 요약 | ☐ |
| 5 | TIME_WAIT 분석 | 개수 확인, 원인 파악 | ☐ |
| 6 | CLOSE_WAIT 분석 | 개수 확인, 프로세스 찾기 | ☐ |
| 7 | ss 필터 사용 | state, dport, dst 필터 | ☐ |
| 8 | 실전 시나리오 실습 | 장애 진단 프로세스 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# 리스닝 포트 확인 (필수!)
ss -tlnp                          # TCP 리스닝 포트 + 프로세스

# 활성 연결 확인
ss -tnp                           # 모든 TCP 연결

# 소켓 통계 (필수!)
ss -s                             # 요약 통계

# 상태별 필터링 (필수!)
ss -tn state established          # ESTABLISHED만
ss -tn state time-wait            # TIME_WAIT만
ss -tn state close-wait           # CLOSE_WAIT만 (⚠️ 앱 버그!)

# 고급 필터
ss -tn '( dport = :80 )'          # 목적지 포트 80
ss -tn '( dst = 192.168.1.100 )'  # 목적지 IP

# 실시간 모니터링
watch -n 1 "ss -s"                # 1초마다 통계
```

---

## 💡 면접 대비 핵심 포인트

### Q1: TIME_WAIT가 많을 때 어떻게 대응하시겠습니까?

**A**: "먼저 `ss -tn state time-wait`로 어떤 목적지에 TIME_WAIT가 많은지 확인합니다. 주로 커넥션 풀 미사용이나 Keep-Alive 비활성화가 원인입니다. 단기적으로는 `tcp_tw_reuse=1` 설정하고, 근본적으로는 커넥션 풀 도입이나 HTTP Keep-Alive 활성화로 해결합니다."

### Q2: CLOSE_WAIT와 TIME_WAIT의 차이는?

**A**: "TIME_WAIT는 연결을 먼저 끊은 쪽(active close)에서 발생하고, 2분 후 자동 해제됩니다. CLOSE_WAIT는 연결 종료 요청을 받았지만 애플리케이션이 close()를 호출하지 않아 발생하며, 앱 버그입니다. CLOSE_WAIT가 많으면 코드 수정이 필요합니다."

### Q3: 특정 서비스로 연결이 안 될 때 진단 순서는?

**A**: "1) `ss -tlnp`로 서버 포트가 LISTEN인지 확인, 2) 방화벽(iptables/ufw) 확인, 3) `nc -zv`로 실제 연결 테스트, 4) `ss -tn state syn-sent`로 SYN만 보내고 응답 못 받는지 확인합니다."

---

## 📝 학습 기록

```
학습일: ____년 __월 __일
실제 소요 시간: ____시간

완료한 실습:
- [ ] ss -tlnp로 리스닝 포트 확인
- [ ] ss -s로 통계 확인
- [ ] TIME_WAIT 분석
- [ ] CLOSE_WAIT 분석
- [ ] 필터링 실습
- [ ] 모니터링 스크립트 작성

이해가 어려웠던 부분:


추가 학습 필요 항목:

```

---

## ➡️ 다음 학습: Day 6

**주제**: tcpdump로 패킷 분석
- 패킷 캡처 기초
- 필터링 문법
- 실제 트래픽 분석
