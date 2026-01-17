# 📅 Day 17: SSH 키 관리 및 보안 설정

## 🎯 오늘의 목표

> **토스플레이스 연결점**: 서버 접근 보안, 자동화를 위한 SSH 키 관리
> 보안 컴플라이언스를 고려한 서버 접근 제어

SSH 키 기반 인증을 설정하고 보안 모범 사례를 익힙니다. DevOps 환경에서 안전한 서버 접근은 기본 중의 기본입니다.

---

## ⏰ 예상 학습 시간: 3시간

---

## 📚 Part 1: SSH 기초 개념 (30분)

### 1.1 SSH란?

SSH(Secure Shell)는 네트워크를 통해 안전하게 원격 시스템에 접근하기 위한 프로토콜입니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│  SSH 동작 원리                                                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. 비밀번호 인증 (보안 취약)                                       │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Client ──[password]──▶ Server                              │    │
│  │          암호화되지만 brute-force 공격에 취약                │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  2. 키 기반 인증 (권장)                                             │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Client                    Server                           │    │
│  │  ┌──────────┐             ┌──────────────────┐             │    │
│  │  │ 개인키   │             │ 공개키            │             │    │
│  │  │(Private) │  ◀──인증──▶ │(authorized_keys) │             │    │
│  │  └──────────┘             └──────────────────┘             │    │
│  │                                                             │    │
│  │  - 개인키: 클라이언트만 소유 (절대 공유 금지!)              │    │
│  │  - 공개키: 서버에 등록 (안전하게 공유 가능)                 │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  키 기반 인증 과정:                                                 │
│  1. 클라이언트가 서버에 연결 요청                                   │
│  2. 서버가 랜덤 챌린지 전송                                         │
│  3. 클라이언트가 개인키로 서명                                      │
│  4. 서버가 공개키로 서명 검증                                       │
│  5. 인증 완료                                                       │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 SSH 키 알고리즘 비교

| 알고리즘 | 키 길이 | 보안 수준 | 권장 여부 | 설명 |
|---------|--------|----------|----------|------|
| **Ed25519** | 256 bit | 매우 높음 | ✅ 강력 권장 | 최신, 빠르고 안전 |
| **RSA** | 4096 bit | 높음 | ⭕ 호환성 필요 시 | 널리 사용, 레거시 지원 |
| ECDSA | 256-521 bit | 높음 | △ | NSA 관련 우려 |
| DSA | 1024 bit | 낮음 | ❌ 사용 금지 | 취약, 더 이상 지원 안 함 |

### 1.3 SSH 키 파일 구조

```
~/.ssh/
├── id_ed25519           # Ed25519 개인키 (권한: 600)
├── id_ed25519.pub       # Ed25519 공개키 (권한: 644)
├── id_rsa               # RSA 개인키 (권한: 600)
├── id_rsa.pub           # RSA 공개키 (권한: 644)
├── authorized_keys      # 서버: 허용된 공개키 목록 (권한: 600)
├── known_hosts          # 접속한 서버의 호스트키 (권한: 644)
└── config               # SSH 클라이언트 설정 (권한: 600)
```

---

## 🛠️ Part 2: SSH 키 생성 및 사용 (1시간)

### 2.1 SSH 키 쌍 생성

```bash
# Ed25519 키 생성 (강력 권장)
ssh-keygen -t ed25519 -C "your_email@example.com"

# 출력 예시:
# Generating public/private ed25519 key pair.
# Enter file in which to save the key (/home/user/.ssh/id_ed25519):
# Enter passphrase (empty for no passphrase):    ← 반드시 설정 권장!
# Enter same passphrase again:
# Your identification has been saved in /home/user/.ssh/id_ed25519
# Your public key has been saved in /home/user/.ssh/id_ed25519.pub
```

```bash
# RSA 키 생성 (레거시 시스템 호환 필요 시)
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"

# 특정 경로에 키 생성
ssh-keygen -t ed25519 -f ~/.ssh/tossplace_key -C "devops@tossplace.com"

# 키 파일 확인
ls -la ~/.ssh/
# 예상 출력:
# -rw------- 1 user user  411 Jan 12 10:00 id_ed25519       # 개인키 (600)
# -rw-r--r-- 1 user user  100 Jan 12 10:00 id_ed25519.pub   # 공개키 (644)
```

### 2.2 키 관련 옵션 상세

```bash
# 옵션 설명
ssh-keygen \
  -t ed25519          # 알고리즘 타입 (-t)
  -b 4096             # 비트 길이 (-b) RSA에서만 사용
  -C "comment"        # 주석 (이메일/용도 등)
  -f /path/to/key     # 키 파일 경로
  -N "passphrase"     # 패스프레이즈 (비대화식)
  -q                  # 조용한 모드

# 기존 키의 패스프레이즈 변경
ssh-keygen -p -f ~/.ssh/id_ed25519

# 키의 지문(fingerprint) 확인
ssh-keygen -l -f ~/.ssh/id_ed25519.pub
# 출력: 256 SHA256:xxxxx... your_email@example.com (ED25519)

# 공개키 내용 확인
cat ~/.ssh/id_ed25519.pub
# 출력: ssh-ed25519 AAAA... your_email@example.com
```

### 2.3 공개키 서버에 등록

```bash
# 방법 1: ssh-copy-id 사용 (가장 간편, 권장)
ssh-copy-id user@server

# 특정 키 파일 지정
ssh-copy-id -i ~/.ssh/tossplace_key.pub user@server

# 특정 포트 지정
ssh-copy-id -i ~/.ssh/id_ed25519.pub -p 2222 user@server
```

```bash
# 방법 2: 수동 등록 (ssh-copy-id 없는 경우)
# 공개키 내용 복사 후 서버에 붙여넣기
cat ~/.ssh/id_ed25519.pub | ssh user@server "mkdir -p ~/.ssh && chmod 700 ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"

# 또는 단계별로
ssh user@server
mkdir -p ~/.ssh
chmod 700 ~/.ssh
echo "ssh-ed25519 AAAA... your_email@example.com" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
exit
```

```bash
# 방법 3: 클라우드 환경 (AWS EC2 예시)
# EC2 인스턴스 생성 시 공개키 등록
# 또는 user-data로 자동 설정
cat << 'EOF' > user-data.sh
#!/bin/bash
echo "ssh-ed25519 AAAA... your_email@example.com" >> /home/ec2-user/.ssh/authorized_keys
chmod 600 /home/ec2-user/.ssh/authorized_keys
EOF
```

### 2.4 키 기반 인증 테스트

```bash
# 기본 접속 (기본 키 사용)
ssh user@server

# 특정 키 파일 지정
ssh -i ~/.ssh/tossplace_key user@server

# 상세 로그 출력 (-v, -vv, -vvv)
ssh -v user@server 2>&1 | grep -E "(Offering|Accepted)"
# 출력 예시:
# debug1: Offering public key: /home/user/.ssh/id_ed25519 ED25519 SHA256:xxx
# debug1: Server accepts key: /home/user/.ssh/id_ed25519 ED25519 SHA256:xxx

# 연결 테스트 (실제 접속 없이)
ssh -T user@server echo "Success!"
```

---

## 🔐 Part 3: SSH 보안 강화 (1시간)

### 3.1 sshd_config 보안 설정

```bash
# SSH 서버 설정 파일 편집
sudo vi /etc/ssh/sshd_config
```

**권장 보안 설정:**

```bash
# ========================================
# SSH 서버 보안 설정 (/etc/ssh/sshd_config)
# ========================================

# 1. 인증 관련 설정
# -----------------
# 비밀번호 인증 비활성화 (키만 허용)
PasswordAuthentication no

# 빈 비밀번호 금지
PermitEmptyPasswords no

# root 로그인 비활성화
PermitRootLogin no
# 또는 키 인증만 허용: PermitRootLogin prohibit-password

# 공개키 인증 활성화
PubkeyAuthentication yes

# authorized_keys 파일 위치
AuthorizedKeysFile .ssh/authorized_keys

# 최대 인증 시도 횟수
MaxAuthTries 3

# 로그인 유예 시간 (초)
LoginGraceTime 60

# 2. 접근 제어
# -----------
# 특정 사용자만 SSH 접근 허용
AllowUsers ubuntu admin deploy

# 특정 그룹만 허용
AllowGroups sshusers admins

# 특정 사용자 차단
DenyUsers root guest

# 3. 네트워크 설정
# ---------------
# SSH 포트 변경 (보안을 위해 기본 22번 대신)
Port 22
# Port 2222  # 운영 환경에서 변경 고려

# IPv4만 사용
AddressFamily inet

# 특정 IP에서만 접속
ListenAddress 0.0.0.0
# ListenAddress 10.0.0.0  # 내부 네트워크만

# 4. 세션 관련
# -----------
# 클라이언트 활성 확인 간격 (초)
ClientAliveInterval 300

# 클라이언트 응답 없을 시 최대 시도 횟수
ClientAliveCountMax 2

# 최대 동시 세션 수
MaxSessions 10

# X11 포워딩 비활성화 (필요 없으면)
X11Forwarding no

# TCP 포워딩 비활성화 (필요 없으면)
AllowTcpForwarding no

# 에이전트 포워딩 비활성화 (필요 없으면)
AllowAgentForwarding no

# 5. 로깅
# -------
# 로그 레벨 (VERBOSE 권장)
LogLevel VERBOSE

# 로그 저장 위치
SyslogFacility AUTH

# 6. 암호화 알고리즘 (강력한 것만 허용)
# ------------------------------------
# 키 교환 알고리즘
KexAlgorithms curve25519-sha256,curve25519-sha256@libssh.org,diffie-hellman-group16-sha512,diffie-hellman-group18-sha512

# 암호화 알고리즘
Ciphers chacha20-poly1305@openssh.com,aes256-gcm@openssh.com,aes128-gcm@openssh.com,aes256-ctr,aes192-ctr,aes128-ctr

# MAC 알고리즘
MACs hmac-sha2-512-etm@openssh.com,hmac-sha2-256-etm@openssh.com,umac-128-etm@openssh.com

# 호스트키 알고리즘
HostKeyAlgorithms ssh-ed25519,ssh-ed25519-cert-v01@openssh.com,rsa-sha2-512,rsa-sha2-256
```

```bash
# 설정 문법 검사
sudo sshd -t

# 설정 적용
sudo systemctl restart sshd

# 서비스 상태 확인
sudo systemctl status sshd
```

### 3.2 SSH 클라이언트 설정 (~/.ssh/config)

```bash
# SSH 클라이언트 설정 파일 생성
cat << 'EOF' > ~/.ssh/config
# ========================================
# SSH 클라이언트 설정 (~/.ssh/config)
# ========================================

# 전역 설정
Host *
    # 호스트키 검증 (TOFU: Trust On First Use)
    StrictHostKeyChecking ask
    # known_hosts 해싱 (보안 강화)
    HashKnownHosts yes
    # 연결 유지 설정
    ServerAliveInterval 60
    ServerAliveCountMax 3
    # 연결 재사용 (속도 향상)
    ControlMaster auto
    ControlPath ~/.ssh/sockets/%r@%h-%p
    ControlPersist 600
    # SSH 키 추가
    AddKeysToAgent yes
    IdentitiesOnly yes

# 개발 서버
Host dev-server
    HostName 192.168.1.100
    User ubuntu
    IdentityFile ~/.ssh/id_ed25519
    Port 22

# 스테이징 서버
Host staging
    HostName staging.example.com
    User deploy
    IdentityFile ~/.ssh/staging_key
    Port 2222

# 프로덕션 서버 (Bastion 경유)
Host bastion
    HostName bastion.example.com
    User ubuntu
    IdentityFile ~/.ssh/bastion_key

Host prod-*
    ProxyJump bastion
    User deploy
    IdentityFile ~/.ssh/prod_key

Host prod-web
    HostName 10.0.1.10

Host prod-api
    HostName 10.0.1.20

Host prod-db
    HostName 10.0.2.10

# AWS EC2
Host aws-*
    User ec2-user
    IdentityFile ~/.ssh/aws_key.pem

# GitHub
Host github.com
    HostName github.com
    User git
    IdentityFile ~/.ssh/github_key
    IdentitiesOnly yes
EOF

# 소켓 디렉토리 생성
mkdir -p ~/.ssh/sockets

# 권한 설정
chmod 600 ~/.ssh/config
```

### 3.3 접속 예시

```bash
# config 설정 후 간단하게 접속
ssh dev-server         # = ssh -i ~/.ssh/id_ed25519 ubuntu@192.168.1.100
ssh staging            # = ssh -i ~/.ssh/staging_key -p 2222 deploy@staging.example.com
ssh prod-web           # Bastion 경유하여 접속

# 명령어 직접 실행
ssh dev-server "hostname && uptime"

# 파일 전송 (scp)
scp file.txt dev-server:/tmp/
scp -r directory/ staging:/home/deploy/

# 파일 전송 (rsync, 권장)
rsync -avz ./project/ dev-server:/home/ubuntu/project/

# 포트 포워딩 (로컬 → 원격)
ssh -L 8080:localhost:80 dev-server
# 로컬 8080 접속 → 원격 80 포트로 연결

# 포트 포워딩 (원격 → 로컬)
ssh -R 9090:localhost:3000 dev-server
# 원격 9090 접속 → 로컬 3000 포트로 연결
```

---

## 🔧 Part 4: 고급 SSH 기술 (30분)

### 4.1 SSH Agent

```bash
# SSH Agent 시작
eval "$(ssh-agent -s)"

# 키 추가
ssh-add ~/.ssh/id_ed25519

# 추가된 키 목록 확인
ssh-add -l

# macOS: 키체인에 영구 저장
ssh-add --apple-use-keychain ~/.ssh/id_ed25519

# 모든 키 제거
ssh-add -D
```

### 4.2 SSH Agent Forwarding

```bash
# Bastion 서버 경유 시 로컬 키 사용
# config 설정
Host bastion
    ForwardAgent yes

# 또는 명령줄 옵션
ssh -A bastion

# 주의: 신뢰할 수 있는 서버에서만 사용!
# Bastion에서 에이전트 소켓 탈취 가능
```

### 4.3 SSH 터널링

```bash
# SOCKS 프록시 (동적 포트 포워딩)
ssh -D 1080 dev-server
# 브라우저 프록시 설정: SOCKS5, localhost:1080

# 여러 포트 포워딩
ssh -L 8080:localhost:80 -L 3306:localhost:3306 dev-server

# 백그라운드 터널 (서비스 형태로)
ssh -fN -L 8080:localhost:80 dev-server
# -f: 백그라운드
# -N: 명령어 실행 안 함
```

### 4.4 Bastion Host (Jump Server)

```
┌─────────────────────────────────────────────────────────────────────┐
│  Bastion Host 아키텍처                                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Internet                     Private Network                       │
│  ┌─────────┐                 ┌───────────────────────────────────┐  │
│  │ DevOps  │                 │                                   │  │
│  │ Client  │ ──SSH──▶ ┌─────────────┐                           │  │
│  └─────────┘          │  Bastion    │ ──SSH──▶ [Web Server]     │  │
│                       │  Host       │ ──SSH──▶ [API Server]     │  │
│                       │  (Jump Box) │ ──SSH──▶ [DB Server]      │  │
│                       └─────────────┘                           │  │
│                       Public IP:     Private IP만               │  │
│                       203.0.113.10   (10.0.x.x)                 │  │
│                                                                   │  │
│  보안 이점:                                                       │  │
│  - 단일 진입점 (감사 용이)                                        │  │
│  - 내부 서버 직접 노출 방지                                       │  │
│  - MFA 적용 가능                                                  │  │
│  - 세션 로깅                                                      │  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

```bash
# ProxyJump를 이용한 직접 접속
ssh -J bastion@bastion.example.com user@10.0.1.10

# config 설정으로 간편화
Host prod-internal
    HostName 10.0.1.10
    User deploy
    ProxyJump bastion

ssh prod-internal
```

---

## 🔒 Part 5: SSH 보안 감사 및 모니터링 (30분)

### 5.1 SSH 로그 분석

```bash
# SSH 인증 로그 확인
sudo tail -f /var/log/auth.log     # Ubuntu/Debian
sudo tail -f /var/log/secure       # CentOS/RHEL

# 성공한 로그인 찾기
sudo grep "Accepted" /var/log/auth.log

# 실패한 로그인 찾기
sudo grep "Failed password" /var/log/auth.log
sudo grep "Invalid user" /var/log/auth.log

# 특정 IP의 접속 시도
sudo grep "192.168.1.100" /var/log/auth.log

# 로그인 실패 IP 집계
sudo grep "Failed password" /var/log/auth.log | awk '{print $(NF-3)}' | sort | uniq -c | sort -rn | head -10
```

### 5.2 fail2ban 설정

```bash
# fail2ban 설치
sudo apt install fail2ban  # Ubuntu
sudo yum install fail2ban  # CentOS

# SSH 보호 설정
sudo cat << 'EOF' > /etc/fail2ban/jail.local
[sshd]
enabled = true
port = ssh
filter = sshd
logpath = /var/log/auth.log
maxretry = 3
findtime = 600
bantime = 3600
EOF

# 서비스 시작
sudo systemctl enable fail2ban
sudo systemctl start fail2ban

# 상태 확인
sudo fail2ban-client status sshd
```

### 5.3 SSH 키 관리 자동화

```bash
# 조직 내 SSH 키 수집 스크립트
#!/bin/bash
# collect_ssh_keys.sh

SERVERS=("web1" "web2" "api1" "api2" "db1")

for server in "${SERVERS[@]}"; do
    echo "=== $server ==="
    ssh "$server" "cat ~/.ssh/authorized_keys" 2>/dev/null
    echo ""
done
```

```bash
# 키 배포 자동화 (Ansible 예시)
# ssh_keys.yml
- hosts: all
  tasks:
    - name: Ensure .ssh directory exists
      file:
        path: /home/{{ ansible_user }}/.ssh
        state: directory
        mode: '0700'

    - name: Add authorized keys
      authorized_key:
        user: "{{ ansible_user }}"
        key: "{{ lookup('file', item) }}"
      with_fileglob:
        - files/ssh_keys/*.pub
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | SSH 키 쌍 생성 (Ed25519) | ☐ |
| 2 | 공개키 서버 등록 | ☐ |
| 3 | 키 기반 인증 테스트 | ☐ |
| 4 | sshd_config 보안 설정 | ☐ |
| 5 | ~/.ssh/config 설정 | ☐ |
| 6 | SSH Agent 사용 | ☐ |
| 7 | Bastion Host 개념 이해 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# 키 생성
ssh-keygen -t ed25519 -C "email@example.com"

# 공개키 등록
ssh-copy-id user@server

# 특정 키로 접속
ssh -i ~/.ssh/custom_key user@server

# SSH Agent
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519

# ProxyJump (Bastion 경유)
ssh -J bastion user@internal-server

# 설정 문법 검사
sudo sshd -t
```

---

## 💡 면접 대비 핵심 포인트

### Q1: SSH 키 기반 인증의 장점은?
**A**:
- 비밀번호 brute-force 공격 방지
- 자동화/스크립트에서 안전하게 사용 가능
- 2048+ 비트 암호화로 보안성 높음
- 패스프레이즈로 추가 보안 가능

### Q2: Ed25519 vs RSA?
**A**:
- **Ed25519**: 256비트로 빠르고 안전, 최신 시스템 권장
- **RSA**: 4096비트 필요, 레거시 호환성 높음
- 가능하면 Ed25519 사용, 구형 시스템은 RSA

### Q3: Bastion Host의 보안 이점은?
**A**:
- 단일 진입점으로 감사 용이
- 내부 서버 IP 비노출
- MFA 적용 가능
- 세션 로깅으로 추적성 확보

### Q4: SSH 보안 강화 방법은?
**A**:
- 비밀번호 인증 비활성화
- root 로그인 금지
- 포트 변경 (기본 22 → 2222 등)
- fail2ban으로 brute-force 차단
- 강력한 암호화 알고리즘만 허용

---

## 🔗 참고 자료

- [OpenSSH Manual](https://www.openssh.com/manual.html)
- [SSH Academy](https://www.ssh.com/academy/ssh)
- [Mozilla SSH Guidelines](https://infosec.mozilla.org/guidelines/openssh)

---

## ➡️ 다음 학습: Day 18

**주제**: DNS 기초와 트러블슈팅
- DNS 동작 원리
- 레코드 타입 (A, CNAME, MX 등)
- dig, nslookup 사용법
