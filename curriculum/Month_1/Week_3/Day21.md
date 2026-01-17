# 📅 Day 21: Week 3 복습 및 Month 1 정리

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "OS, Network 등 다양한 레이어에서의 모니터링, 트러블슈팅 경험"
> Month 1 전체 지식을 종합하여 체계적인 트러블슈팅 역량 완성

Week 1-3 전체를 복습하고, Month 1 프로젝트(Week 4)를 준비합니다. Linux 기초, Docker, 네트워크/시스템 관리 지식을 종합하여 실제 장애 대응 능력을 점검합니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| Week 3 복습 | 1시간 | systemd, 보안, 네트워크 |
| Month 1 종합 | 1시간 | Week 1-3 통합 정리 |
| 자가 테스트 | 1시간 | 실습 문제 풀이 |
| 프로젝트 준비 | 1시간 | Week 4 계획 |

---

## 📋 Part 1: Week 3 복습 체크리스트 (1시간)

### 1.1 Week 3 핵심 주제 정리

| Day | 주제 | 핵심 명령어 | 토스플레이스 연관성 |
|-----|------|-----------|-------------------|
| 15 | systemd 서비스 관리 | `systemctl`, `journalctl` | 서비스 운영/모니터링 |
| 16 | 권한/방화벽 | `chmod`, `ufw`, `iptables` | 보안 컴플라이언스 |
| 17 | SSH 보안 | `ssh-keygen`, `sshd_config` | 접근 제어 |
| 18 | DNS | `dig`, `nslookup`, `/etc/resolv.conf` | 서비스 디스커버리 |
| 19 | HTTP/HTTPS | `curl`, `openssl s_client` | API 트러블슈팅 |
| 20 | Load Balancing | Nginx upstream, health check | 트래픽 분산 |

### 1.2 Day 15: systemd 서비스 관리

```bash
# 핵심 명령어 복습
echo "=== systemd 핵심 명령어 ==="

# 서비스 상태
systemctl status nginx
systemctl is-active nginx
systemctl is-enabled nginx

# 서비스 제어
systemctl start nginx
systemctl stop nginx
systemctl restart nginx
systemctl reload nginx  # 설정만 재로드

# 부팅 시 자동 시작
systemctl enable nginx
systemctl disable nginx

# 로그 확인
journalctl -u nginx            # nginx 서비스 로그
journalctl -u nginx -f         # 실시간 로그
journalctl -u nginx --since "1 hour ago"
journalctl -u nginx -p err     # 에러만

# 서비스 파일 위치
ls /etc/systemd/system/
ls /usr/lib/systemd/system/

# 서비스 파일 재로드
systemctl daemon-reload
```

### 1.3 Day 16: 권한과 방화벽

```bash
# 파일 권한
echo "=== 파일 권한 ==="
ls -la /tmp/
chmod 755 file.sh      # rwxr-xr-x
chmod u+x file.sh      # 소유자 실행 권한 추가
chown user:group file  # 소유권 변경

# 특수 권한
chmod 4755 file        # SUID (실행 시 소유자 권한)
chmod 2755 dir         # SGID (디렉토리 내 파일 그룹 상속)
chmod 1777 dir         # Sticky bit (/tmp처럼)

# UFW 방화벽
echo "=== UFW 방화벽 ==="
sudo ufw status verbose
sudo ufw allow 22/tcp
sudo ufw allow from 192.168.1.0/24 to any port 80
sudo ufw deny 3306
sudo ufw enable
sudo ufw reload

# iptables (하위 레벨)
sudo iptables -L -n -v
sudo iptables -A INPUT -p tcp --dport 80 -j ACCEPT
```

### 1.4 Day 17: SSH 보안

```bash
# SSH 키 생성
echo "=== SSH 키 관리 ==="
ssh-keygen -t ed25519 -C "email@example.com"
ssh-keygen -t rsa -b 4096 -C "email@example.com"

# 키 배포
ssh-copy-id user@server

# SSH 설정 (~/.ssh/config)
cat << 'EOF'
Host myserver
    HostName 192.168.1.100
    User admin
    Port 22
    IdentityFile ~/.ssh/id_ed25519
EOF

# sshd 보안 설정 (/etc/ssh/sshd_config)
# PasswordAuthentication no
# PermitRootLogin no
# MaxAuthTries 3
# AllowUsers admin developer
```

### 1.5 Day 18: DNS

```bash
# DNS 조회
echo "=== DNS 트러블슈팅 ==="
dig google.com +short
dig @8.8.8.8 google.com
dig +trace google.com
dig google.com MX
dig google.com NS

# nslookup
nslookup google.com
nslookup -type=MX google.com

# DNS 설정
cat /etc/resolv.conf
cat /etc/hosts

# DNS 캐시 초기화
sudo resolvectl flush-caches
```

### 1.6 Day 19: HTTP/HTTPS

```bash
# HTTP 요청
echo "=== HTTP 테스트 ==="
curl https://httpbin.org/get
curl -v https://httpbin.org/get       # 상세 (헤더 포함)
curl -I https://httpbin.org/get       # 헤더만
curl -X POST -d "name=test" https://httpbin.org/post
curl -X POST -H "Content-Type: application/json" \
  -d '{"name":"test"}' https://httpbin.org/post

# 응답 코드만
curl -s -o /dev/null -w "%{http_code}" https://httpbin.org/get

# 응답 시간 측정
curl -o /dev/null -s -w "Total: %{time_total}s\n" https://httpbin.org/get

# HTTPS 인증서 확인
echo "=== 인증서 확인 ==="
echo | openssl s_client -connect google.com:443 -servername google.com 2>/dev/null | \
  openssl x509 -noout -dates
```

### 1.7 Day 20: Load Balancing

```bash
# Nginx Load Balancer 설정
cat << 'EOF'
upstream backend {
    least_conn;  # 연결 수 기준 분배
    server 10.0.0.1:8080 weight=3;
    server 10.0.0.2:8080 weight=1;
    server 10.0.0.3:8080 backup;
}

server {
    listen 80;
    location / {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
EOF

# Nginx 설정 테스트 및 적용
sudo nginx -t
sudo systemctl reload nginx
```

---

## 📊 Part 2: Month 1 종합 정리 (1시간)

### 2.1 Week 1: Linux 트러블슈팅 도구

```bash
# 시스템 콜 분석
strace -c -p <pid>           # 시스템 콜 통계
strace -e open -p <pid>      # open 시스템 콜만

# 프로세스 분석
cat /proc/<pid>/status | grep -E "VmRSS|VmSize|Threads"
ls /proc/<pid>/fd | wc -l    # 파일 디스크립터 수
lsof -p <pid> | wc -l        # 열린 파일 수

# 메모리
free -h
vmstat 1 5
cat /proc/meminfo

# CPU
top -p <pid>
mpstat 1 5
pidstat -u 1 5

# 디스크 I/O
iostat -x 1 5
iotop -o

# 네트워크
ss -tlnp                     # TCP LISTEN 포트
ss -tn state established     # 연결된 소켓
netstat -s                   # 네트워크 통계
tcpdump -i any -nn port 80   # 패킷 캡처
```

### 2.2 Week 2: Docker

```bash
# 이미지/컨테이너
docker build -t myapp:v1 .
docker run -d -p 8080:80 --name web myapp:v1
docker ps -a
docker logs -f web
docker exec -it web /bin/sh

# 컨테이너 트러블슈팅
docker stats web                    # 리소스 사용량
docker top web                      # 프로세스 목록
docker inspect web                  # 상세 정보
docker inspect web --format '{{.State.OOMKilled}}'  # OOM 확인
docker inspect web --format '{{.State.ExitCode}}'   # 종료 코드

# Docker Compose
docker-compose up -d --build
docker-compose logs -f
docker-compose ps
docker-compose down

# 네트워킹
docker network ls
docker network inspect bridge
docker network create mynet
```

### 2.3 Week 3: 시스템/네트워크 관리

```bash
# 서비스 관리
systemctl status nginx
journalctl -u nginx -f

# 보안
chmod 755 file
ufw allow 80/tcp
ssh-keygen -t ed25519

# 네트워크
dig +short google.com
curl -v https://api.example.com
```

### 2.4 통합 트러블슈팅 플로우

```
┌─────────────────────────────────────────────────────────────────────┐
│  장애 발생 시 체계적 분석 플로우                                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1단계: 현상 파악                                                    │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • 어떤 증상인가? (느림, 에러, 접속 불가)                   │    │
│  │  • 언제부터? 변경 사항 있었나?                              │    │
│  │  • 영향 범위는? (전체/일부 사용자)                          │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                              │                                      │
│                              ▼                                      │
│  2단계: 시스템 리소스 확인                                          │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • top/htop: CPU, 메모리 사용률                             │    │
│  │  • free -h: 메모리 상세                                     │    │
│  │  • iostat: 디스크 I/O                                       │    │
│  │  • df -h: 디스크 용량                                       │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                              │                                      │
│                              ▼                                      │
│  3단계: 프로세스/서비스 확인                                        │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • systemctl status <service>: 서비스 상태                  │    │
│  │  • journalctl -u <service>: 서비스 로그                     │    │
│  │  • ps aux | grep <process>: 프로세스 확인                   │    │
│  │  • docker ps / docker logs: 컨테이너 확인                   │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                              │                                      │
│                              ▼                                      │
│  4단계: 네트워크 확인                                               │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • ping/curl: 연결 테스트                                   │    │
│  │  • dig: DNS 확인                                            │    │
│  │  • ss -tlnp: 포트 리스닝 확인                               │    │
│  │  • tcpdump: 패킷 분석                                       │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                              │                                      │
│                              ▼                                      │
│  5단계: 상세 분석                                                   │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • strace: 시스템 콜 분석                                   │    │
│  │  • /proc/<pid>/: 프로세스 상세 정보                         │    │
│  │  • 애플리케이션 로그: 에러 메시지                           │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                              │                                      │
│                              ▼                                      │
│  6단계: 조치 및 문서화                                              │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • 원인 해결                                                │    │
│  │  • 인시던트 리포트 작성                                     │    │
│  │  • 재발 방지 대책 수립                                      │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 3: 자가 테스트 (1시간)

### 3.1 실습 문제

다음 문제들을 직접 해결해보세요. 정답은 하단에 있습니다.

```bash
# === 문제 1: 프로세스 분석 ===
# 특정 프로세스의 메모리 사용량(VmRSS)을 확인하세요
# 답: 

# === 문제 2: 네트워크 분석 ===
# TCP 80번 포트로 LISTEN 중인 프로세스를 찾으세요
# 답: 

# === 문제 3: Docker 트러블슈팅 ===
# 컨테이너가 OOMKilled인지 확인하세요
# 답: 

# === 문제 4: Docker Compose ===
# docker-compose로 앱을 빌드하고 백그라운드 실행하세요
# 답: 

# === 문제 5: 서비스 로그 ===
# nginx 서비스 로그를 실시간으로 확인하세요
# 답: 

# === 문제 6: DNS 문제 진단 ===
# 외부 DNS 서버(8.8.8.8)로 google.com을 조회하세요
# 답: 

# === 문제 7: HTTP 응답 코드 확인 ===
# curl로 응답 코드만 확인하세요
# 답: 

# === 문제 8: 인증서 만료일 확인 ===
# google.com의 SSL 인증서 만료일을 확인하세요
# 답: 

# === 문제 9: 시스템 콜 분석 ===
# 특정 프로세스의 시스템 콜 통계를 확인하세요
# 답: 

# === 문제 10: 방화벽 설정 ===
# 192.168.1.0/24 대역에서만 SSH 접속을 허용하세요
# 답: 
```

### 3.2 정답

```bash
# 문제 1
cat /proc/<pid>/status | grep VmRSS

# 문제 2
ss -tlnp | grep :80
# 또는
lsof -i :80

# 문제 3
docker inspect <container> --format '{{.State.OOMKilled}}'

# 문제 4
docker-compose up -d --build

# 문제 5
journalctl -u nginx -f
# 또는
sudo tail -f /var/log/nginx/error.log

# 문제 6
dig @8.8.8.8 google.com
# 또는
nslookup google.com 8.8.8.8

# 문제 7
curl -s -o /dev/null -w "%{http_code}" https://example.com

# 문제 8
echo | openssl s_client -connect google.com:443 -servername google.com 2>/dev/null | \
  openssl x509 -noout -dates

# 문제 9
sudo strace -c -p <pid>

# 문제 10
sudo ufw allow from 192.168.1.0/24 to any port 22
```

### 3.3 자가 체크리스트

| # | 항목 | 가능 여부 |
|---|------|----------|
| 1 | top/htop으로 CPU/메모리 병목 식별 | ☐ |
| 2 | strace로 시스템 콜 분석 | ☐ |
| 3 | /proc에서 프로세스 정보 확인 | ☐ |
| 4 | Docker 컨테이너 트러블슈팅 | ☐ |
| 5 | docker-compose 운영 | ☐ |
| 6 | systemctl/journalctl 사용 | ☐ |
| 7 | 파일 권한 관리 (chmod, chown) | ☐ |
| 8 | SSH 키 기반 인증 설정 | ☐ |
| 9 | DNS 문제 진단 (dig) | ☐ |
| 10 | HTTP 요청/응답 분석 (curl) | ☐ |
| 11 | SSL 인증서 확인 | ☐ |
| 12 | Nginx 로드밸런서 구성 | ☐ |
| 13 | 방화벽 규칙 설정 (ufw) | ☐ |
| 14 | 네트워크 연결 분석 (ss, tcpdump) | ☐ |

---

## 📚 Part 4: 면접 대비 핵심 Q&A

### Q1: 서버가 느릴 때 어떻게 분석하나요?

```
1. top/htop으로 CPU, 메모리 전체 사용률 확인
2. 특정 프로세스가 리소스를 많이 쓰는지 확인
3. iostat으로 디스크 I/O 병목 확인
4. ss -s로 네트워크 연결 상태 확인 (TIME_WAIT 과다 등)
5. 문제 프로세스 발견 시 strace로 시스템 콜 분석
6. /proc/<pid>/status로 상세 메모리 정보 확인
```

### Q2: 컨테이너가 계속 재시작할 때 어떻게 분석하나요?

```
1. docker logs로 에러 메시지 확인
2. docker inspect로 Exit Code 확인
   - 137: OOMKilled → 메모리 부족
   - 1: 일반 오류 → 앱 로직/설정 문제
   - 0: 정상 종료 (잘못된 CMD?)
3. docker stats로 리소스 사용량 확인
4. docker exec로 내부 진입하여 직접 확인
5. Dockerfile, docker-compose.yml 검토
```

### Q3: DNS가 안 될 때 어떻게 분석하나요?

```
1. dig <domain>으로 DNS 응답 확인
2. 실패 시 dig @8.8.8.8 <domain>으로 외부 DNS 테스트
3. 성공 → 로컬 DNS 서버 문제
   실패 → 도메인 자체 또는 네트워크 문제
4. /etc/resolv.conf에서 DNS 서버 설정 확인
5. /etc/hosts에 잘못된 엔트리 있는지 확인
6. 네트워크 연결 자체 확인 (ping 8.8.8.8)
```

### Q4: 웹 서비스가 응답하지 않을 때?

```
1. curl -v로 응답 상태 확인
2. ss -tlnp로 서비스가 포트 리스닝 중인지 확인
3. systemctl status로 서비스 상태 확인
4. journalctl -u <service>로 로그 확인
5. 방화벽 규칙 확인 (ufw status)
6. 백엔드/DB 연결 문제인지 확인
```

---

## 🛠️ Part 5: Month 1 프로젝트 준비 (Week 4)

### 5.1 프로젝트 개요

```
┌─────────────────────────────────────────────────────────────────────┐
│  Month 1 프로젝트: "트러블슈팅 시나리오 해결 및 문서화"             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  목표:                                                               │
│  1. Docker 기반 3-Tier 앱 구축 (Nginx + App + Redis)                │
│  2. 의도적 장애 주입 → 분석 → 해결                                  │
│  3. 분석 과정 및 해결책 문서화                                      │
│                                                                      │
│  Week 4 일정:                                                        │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Day 22-23: 환경 구축 (Docker Compose 3-Tier)              │    │
│  │  Day 24-25: 장애 시나리오 실습                              │    │
│  │  Day 26-27: 트러블슈팅 및 해결                              │    │
│  │  Day 28-30: 문서화 및 GitHub 업로드                         │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  장애 시나리오:                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  1. 메모리 누수 시뮬레이션 (OOMKilled)                      │    │
│  │  2. CPU 병목 시뮬레이션 (무한 루프)                         │    │
│  │  3. 디스크 I/O 병목                                         │    │
│  │  4. 네트워크 연결 문제 (Redis 연결 실패)                    │    │
│  │  5. 설정 오류 (환경 변수, 권한)                             │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  결과물:                                                             │
│  1. 동작하는 3-Tier 앱 (docker-compose.yml)                         │
│  2. 장애별 인시던트 리포트 (5개)                                    │
│  3. GitHub 저장소 (README, 문서화)                                  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 5.2 사전 준비 사항

```bash
# 필수 소프트웨어 확인
docker --version
docker-compose --version

# 프로젝트 디렉토리 구조
mkdir -p ~/portfolio/month1-troubleshooting
cd ~/portfolio/month1-troubleshooting

# 예상 구조
tree .
# .
# ├── docker-compose.yml
# ├── app/
# │   ├── Dockerfile
# │   └── app.py
# ├── nginx/
# │   └── nginx.conf
# ├── docs/
# │   ├── incident-1-oom.md
# │   ├── incident-2-cpu.md
# │   └── ...
# └── README.md
```

---

## ✅ Week 3 최종 체크리스트

| # | 항목 | 자신 있게 설명 가능? |
|---|------|-------------------|
| 1 | systemd로 서비스 관리 (systemctl, journalctl) | ☐ |
| 2 | 파일 권한 관리 (chmod, chown, 특수 권한) | ☐ |
| 3 | 방화벽 설정 (ufw, iptables) | ☐ |
| 4 | SSH 키 기반 인증 설정 | ☐ |
| 5 | DNS 문제 진단 (dig, nslookup) | ☐ |
| 6 | HTTP 요청/응답 분석 (curl -v) | ☐ |
| 7 | SSL 인증서 확인 (openssl) | ☐ |
| 8 | Nginx 로드밸런서 구성 | ☐ |
| 9 | Week 1-3 내용 면접에서 설명 가능 | ☐ |
| 10 | Month 1 프로젝트 계획 수립 | ☐ |

---

## 📝 학습 기록

```
Week 3 완료일: ____년 __월 __일
총 소요 시간: ____시간

Month 1에서 가장 중요하게 배운 것:


가장 자신 있는 부분:


면접에서 설명할 수 있는 경험:


추가 학습이 필요한 부분:


Week 4 프로젝트 목표:

```

---

## ➡️ 다음 학습: Day 22-30 (Week 4)

**주제**: Month 1 종합 프로젝트

- Day 22-23: Docker Compose 3-Tier 환경 구축
- Day 24-25: CPU/메모리 장애 시나리오
- Day 26-27: 네트워크/설정 장애 시나리오
- Day 28-30: 문서화 및 GitHub 업로드
