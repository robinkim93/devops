# Day 6: tcpdump로 패킷 분석

## 오늘의 목표

토스플레이스 연결점: "Network 레이어에서의 트러블슈팅 경험"
"장애에 대한 단기 해결뿐만 아니라 재발 방지를 위한 원인 분석"

네트워크 문제 발생 시 tcpdump로 패킷을 캡처하고 분석할 수 있어야 합니다. 서비스 간 통신 문제, 방화벽 이슈, 연결 타임아웃 등의 근본 원인을 찾는 핵심 도구입니다.

---

## 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 핵심 개념 | 45분 | TCP/IP 기초, tcpdump 원리 |
| 기본 실습 | 1시간 | 캡처, 필터링 |
| 고급 실습 | 1.5시간 | 상세 분석, 파일 저장 |
| 실전 시나리오 | 45분 | 장애 진단 실습 |

---

## Part 1: 핵심 개념 (45분)

### 1.1 tcpdump를 언제 사용하나?

```
tcpdump 사용 상황:

1. "서버 간 통신이 안 돼요"
   -> 패킷이 나가는지, 응답이 오는지 확인
   -> SYN만 보내고 SYN-ACK가 안 오면 -> 방화벽/네트워크 문제

2. "특정 요청만 느려요"
   -> 어느 구간에서 지연되는지 확인
   -> 타임스탬프 분석

3. "연결이 끊겨요"
   -> RST 패킷이 어디서 오는지 확인
   -> 서버에서? 클라이언트에서? 중간 장비에서?

4. "방화벽 문제인 것 같아요"
   -> 패킷이 도착은 하는지 확인
   -> SYN은 오는데 응답이 안 나가면 -> 서버 방화벽
   -> SYN 자체가 안 오면 -> 네트워크/외부 방화벽

5. "TLS 핸드셰이크 실패"
   -> Client Hello, Server Hello 패킷 확인
   -> 인증서 문제, 프로토콜 버전 문제 진단
```

### 1.2 TCP/IP 패킷 구조

```
TCP/IP 패킷 구조:

+------------------+
|  Ethernet 헤더   |  MAC 주소 (L2)
+------------------+
|    IP 헤더       |  출발지/목적지 IP (L3)
+------------------+
|   TCP 헤더       |  포트, 플래그, 시퀀스 (L4)
+------------------+
|      데이터      |  HTTP, DNS 등 (L7)
+------------------+

tcpdump는 이 모든 레이어를 볼 수 있음
```

### 1.3 TCP 플래그 (핵심!)

| 플래그 | tcpdump 표시 | 의미 | 상황 |
|--------|-------------|------|------|
| SYN | [S] | 연결 시작 | 3-way handshake 시작 |
| SYN-ACK | [S.] | 연결 응답 | 서버가 연결 수락 |
| ACK | [.] | 확인 응답 | 데이터 수신 확인 |
| PSH-ACK | [P.] | 데이터 전송 | 실제 데이터 전달 |
| FIN | [F.] | 연결 종료 | 정상 종료 시작 |
| RST | [R.] | 강제 종료 | 비정상 종료 (문제!) |

```
정상적인 TCP 연결 흐름:

Client              Server
   |------- SYN ------->|  [S]
   |<---- SYN-ACK ------|  [S.]
   |------- ACK ------->|  [.]
   |                    |
   |--- PSH-ACK (데이터) -->|  [P.]
   |<---- ACK ----------|  [.]
   |                    |
   |------- FIN ------->|  [F.]
   |<---- FIN-ACK ------|  [F.]
   |------- ACK ------->|  [.]
```

### 1.4 기본 문법

```bash
tcpdump [옵션] [필터 표현식]

# 주요 옵션
-i <interface>  : 인터페이스 지정 (eth0, any 등)
-n              : IP를 숫자로 표시 (DNS 조회 안 함)
-nn             : IP와 포트 모두 숫자로 표시
-c <count>      : 캡처할 패킷 수
-w <file>       : pcap 파일로 저장
-r <file>       : pcap 파일 읽기
-A              : ASCII로 내용 출력
-X              : 16진수 + ASCII 출력
-v, -vv, -vvv   : 상세 정보 레벨
-s <snaplen>    : 캡처할 바이트 수 (0 = 전체)
```

---

## Part 2: 기본 실습 (1시간)

### 실습 1: 기본 패킷 캡처 (15분)

```bash
# 모든 인터페이스에서 10개 패킷 캡처
sudo tcpdump -i any -c 10

# 출력 예시:
# 10:23:45.123456 IP 192.168.1.10.45678 > 192.168.1.20.80: Flags [S], seq 123456
#   ↑ 시간          ↑ 출발지:포트        ↑ 목적지:포트     ↑ TCP 플래그

# DNS 조회 없이 (빠름!)
sudo tcpdump -i any -nn -c 10

# 상세 정보 포함
sudo tcpdump -i any -nn -v -c 10
```

### 실습 2: 포트 필터링 (15분)

```bash
# HTTP (포트 80)
sudo tcpdump -i any -nn port 80

# HTTPS (포트 443)
sudo tcpdump -i any -nn port 443

# SSH (포트 22)
sudo tcpdump -i any -nn port 22

# MySQL (포트 3306)
sudo tcpdump -i any -nn port 3306

# 여러 포트 (or 사용)
sudo tcpdump -i any -nn 'port 80 or port 443'

# 포트 범위
sudo tcpdump -i any -nn 'portrange 8000-9000'
```

### 실습 3: 호스트 필터링 (15분)

```bash
# 특정 IP로 오가는 패킷
sudo tcpdump -i any -nn host 192.168.1.10

# 출발지만
sudo tcpdump -i any -nn src host 192.168.1.10

# 목적지만
sudo tcpdump -i any -nn dst host 192.168.1.10

# 서브넷
sudo tcpdump -i any -nn net 192.168.1.0/24

# 호스트 + 포트 조합
sudo tcpdump -i any -nn 'host 192.168.1.10 and port 80'

# 복잡한 필터
sudo tcpdump -i any -nn 'host 192.168.1.10 and (port 80 or port 443)'
```

### 실습 4: 실시간 트래픽 관찰 (15분)

터미널 2개 사용:

```bash
# 터미널 1: tcpdump 실행
sudo tcpdump -i any -nn port 80

# 터미널 2: HTTP 요청 생성
curl http://httpbin.org/get
```

tcpdump 출력 분석:
```
# 1. SYN (연결 시작)
10:00:00.001 IP 192.168.1.10.54321 > 54.166.163.67.80: Flags [S], seq 1234567

# 2. SYN-ACK (연결 응답)
10:00:00.050 IP 54.166.163.67.80 > 192.168.1.10.54321: Flags [S.], seq 7654321, ack 1234568

# 3. ACK (연결 완료)
10:00:00.051 IP 192.168.1.10.54321 > 54.166.163.67.80: Flags [.], ack 1

# 4. HTTP 요청 (PSH)
10:00:00.052 IP 192.168.1.10.54321 > 54.166.163.67.80: Flags [P.], length 123

# 5. HTTP 응답 (PSH)
10:00:00.100 IP 54.166.163.67.80 > 192.168.1.10.54321: Flags [P.], length 456
```

---

## Part 3: 고급 실습 (1.5시간)

### 실습 5: HTTP 내용 보기 (20분)

```bash
# ASCII로 패킷 내용 출력
sudo tcpdump -i any -nn -A port 80 -c 20

# 출력 예시 (HTTP 요청):
# GET /index.html HTTP/1.1
# Host: example.com
# User-Agent: curl/7.68.0
# Accept: */*

# 16진수 + ASCII (더 상세)
sudo tcpdump -i any -nn -X port 80 -c 20

# 전체 패킷 캡처 (truncate 방지)
sudo tcpdump -i any -nn -A -s 0 port 80 -c 20
```

**주의**: HTTPS(443)는 암호화되어 있어 내용이 안 보임!

### 실습 6: 파일로 저장 및 분석 (30분)

```bash
# 1. 패킷을 pcap 파일로 저장
sudo tcpdump -i any -nn -c 100 -w /tmp/capture.pcap

# 2. 저장된 파일 읽기
sudo tcpdump -r /tmp/capture.pcap

# 3. 필터링해서 읽기
sudo tcpdump -r /tmp/capture.pcap 'port 80'

# 4. 상세 정보와 함께
sudo tcpdump -r /tmp/capture.pcap -vvv

# 5. 통계 정보 (-q: quiet)
sudo tcpdump -r /tmp/capture.pcap -q -n 2>/dev/null | wc -l

# 6. 특정 시간 범위 캡처
sudo timeout 60 tcpdump -i any -nn -w /tmp/1min.pcap
```

Wireshark로 분석:
```bash
# pcap 파일을 로컬로 복사 후 Wireshark로 열기
# GUI에서 더 상세한 분석 가능:
# - TCP 스트림 추적
# - 통계
# - 프로토콜 분석
```

### 실습 7: TCP 플래그 필터링 (20분)

```bash
# SYN 패킷만 (연결 시도)
sudo tcpdump -i any -nn 'tcp[tcpflags] & tcp-syn != 0'

# SYN만 (SYN-ACK 제외)
sudo tcpdump -i any -nn 'tcp[tcpflags] == tcp-syn'

# RST 패킷만 (연결 강제 종료) - 중요!
sudo tcpdump -i any -nn 'tcp[tcpflags] & tcp-rst != 0'

# FIN 패킷만 (연결 종료)
sudo tcpdump -i any -nn 'tcp[tcpflags] & tcp-fin != 0'

# 연결 시작 또는 종료
sudo tcpdump -i any -nn 'tcp[tcpflags] & (tcp-syn|tcp-fin) != 0'
```

### 실습 8: 고급 필터 표현식 (20분)

```bash
# 특정 크기 이상 패킷
sudo tcpdump -i any -nn 'greater 1000'

# 특정 크기 이하 패킷
sudo tcpdump -i any -nn 'less 100'

# ICMP (ping)
sudo tcpdump -i any -nn icmp

# ARP
sudo tcpdump -i any -nn arp

# DNS
sudo tcpdump -i any -nn port 53

# 특정 호스트 제외
sudo tcpdump -i any -nn 'not host 192.168.1.1'

# 복잡한 조합
sudo tcpdump -i any -nn 'host 192.168.1.10 and port 80 and tcp[tcpflags] & tcp-syn != 0'
```

---

## Part 4: 실전 시나리오 (45분)

### 시나리오 1: 연결 타임아웃 분석

```bash
# 문제: 애플리케이션이 DB에 연결 못함 (Connection timed out)

# 1단계: 연결 시도 캡처
sudo tcpdump -i any -nn 'host <DB_IP> and port 3306'

# 2단계: 다른 터미널에서 연결 시도
mysql -h <DB_IP> -u root -p

# 분석:
# Case 1: SYN만 나가고 응답 없음
#   -> 네트워크 문제 또는 외부 방화벽
# Case 2: SYN 나가고 RST 받음
#   -> DB 서버의 방화벽이 차단
# Case 3: SYN-ACK 받고 ACK 보냈는데 연결 실패
#   -> 애플리케이션 레벨 문제
```

### 시나리오 2: RST 패킷 추적

```bash
# 문제: 간헐적으로 연결이 끊김

# RST 패킷만 캡처
sudo tcpdump -i any -nn 'tcp[tcpflags] & tcp-rst != 0' -w /tmp/rst.pcap

# 분석
sudo tcpdump -r /tmp/rst.pcap -vv

# RST 출처 확인:
# - 서버에서 보냄: 서버 애플리케이션 문제
# - 클라이언트에서 보냄: 클라이언트 타임아웃
# - 중간 장비에서 보냄: 방화벽/로드밸런서 세션 타임아웃
```

### 시나리오 3: HTTP 응답 지연 분석

```bash
# 문제: API 응답이 느림

# HTTP 트래픽 캡처 (타임스탬프 포함)
sudo tcpdump -i any -nn -ttt 'host <API_서버> and port 80'

# -ttt: 패킷 간 시간 차이 표시

# 분석:
# 요청 -> 응답 시간 차이로 지연 구간 식별
# 긴 지연 구간 = 병목
```

### 시나리오 4: DNS 문제 진단

```bash
# DNS 쿼리 캡처
sudo tcpdump -i any -nn port 53

# DNS 쿼리 내용 보기
sudo tcpdump -i any -nn -A port 53

# 분석:
# - 쿼리는 나가는데 응답 없음: DNS 서버 문제
# - NXDOMAIN 응답: 도메인 없음
# - 지연: DNS 서버 느림
```

### 시나리오 5: 마이크로서비스 통신 문제

```bash
# Service A -> Service B 통신 캡처
sudo tcpdump -i any -nn 'host <서비스A_IP> and host <서비스B_IP>'

# 또는 특정 포트 (예: gRPC 50051)
sudo tcpdump -i any -nn 'port 50051'

# 분석 포인트:
# - 연결 수립 성공 여부
# - 데이터 전송 여부
# - 응답 시간
# - 연결 종료 방식 (정상/비정상)
```

---

## 실전 팁

### 자주 사용하는 원라이너

```bash
# 1. 연결 문제 빠른 진단 (SYN, RST만)
sudo tcpdump -i any -nn 'tcp[tcpflags] & (tcp-syn|tcp-rst) != 0'

# 2. 특정 서비스 모니터링 (30초간)
sudo timeout 30 tcpdump -i any -nn 'host <IP> and port <PORT>' -c 100

# 3. HTTP 요청 URL만 보기
sudo tcpdump -i any -nn -A 'port 80 and tcp[((tcp[12:1] & 0xf0) >> 2):4] = 0x47455420'

# 4. 패킷 카운트 (통계)
sudo tcpdump -i any -nn -c 1000 -q 2>/dev/null | awk '{print $3}' | cut -d. -f1-4 | sort | uniq -c | sort -rn

# 5. 실시간 연결 모니터링
watch -n 1 "sudo tcpdump -i any -nn -c 10 2>/dev/null | head -10"
```

---

## 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | 기본 캡처 | tcpdump -i any -nn | |
| 2 | 포트 필터링 | port 80, port 443 | |
| 3 | 호스트 필터링 | host, src, dst | |
| 4 | 파일 저장/읽기 | -w, -r | |
| 5 | TCP 플래그 이해 | SYN, ACK, RST, FIN | |
| 6 | 내용 보기 | -A, -X | |
| 7 | 플래그 필터 | tcp[tcpflags] | |
| 8 | 실전 시나리오 | 연결 문제 진단 | |

---

## 핵심 명령어

```bash
# 기본 캡처
sudo tcpdump -i any -nn -c 100

# 포트 필터
sudo tcpdump -i any -nn port 80

# 호스트 필터
sudo tcpdump -i any -nn host 192.168.1.10

# 조합
sudo tcpdump -i any -nn 'host 192.168.1.10 and port 80'

# 파일 저장
sudo tcpdump -i any -nn -w capture.pcap

# 파일 읽기
sudo tcpdump -r capture.pcap

# 내용 보기
sudo tcpdump -i any -nn -A port 80

# RST 패킷만 (장애 진단)
sudo tcpdump -i any -nn 'tcp[tcpflags] & tcp-rst != 0'

# SYN 패킷만 (연결 시도)
sudo tcpdump -i any -nn 'tcp[tcpflags] & tcp-syn != 0'
```

---

## 면접 대비 핵심 포인트

### Q1: 연결 타임아웃 시 tcpdump로 어떻게 진단하나요?

A: 해당 호스트와 포트에 대해 tcpdump를 실행하고 SYN 패킷이 나가는지 확인합니다. SYN만 나가고 SYN-ACK가 안 오면 네트워크나 방화벽 문제입니다. SYN-ACK가 오면 애플리케이션 레벨 문제입니다.

### Q2: RST 패킷이 많이 보일 때 원인은?

A: RST 패킷의 출처를 확인합니다. 서버에서 보내면 포트가 닫혀있거나 서버 애플리케이션 문제입니다. 중간에서 보내면 방화벽이나 로드밸런서의 세션 타임아웃일 수 있습니다.

### Q3: tcpdump와 Wireshark의 차이는?

A: tcpdump는 CLI 도구로 서버에서 직접 실행하며, 실시간 캡처와 간단한 필터링에 적합합니다. Wireshark는 GUI 도구로 복잡한 프로토콜 분석과 시각화에 적합합니다. 서버에서 tcpdump로 pcap 파일을 저장하고 Wireshark로 분석하는 방식이 일반적입니다.

---

## 학습 기록

```
학습일: ____년 __월 __일
실제 소요 시간: ____시간

완료한 실습:
- [ ] 기본 캡처
- [ ] 포트/호스트 필터링
- [ ] 파일 저장 및 읽기
- [ ] TCP 플래그 분석
- [ ] 실전 시나리오

이해가 어려웠던 부분:

추가 학습 필요 항목:
```

---

## 다음 학습: Day 7

주제: Week 1 복습 및 종합 실습
- top, vmstat, iostat, ss, tcpdump 종합
- 장애 시나리오 실습
- 면접 대비 질문