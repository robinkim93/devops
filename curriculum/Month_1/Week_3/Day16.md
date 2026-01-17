# 📅 Day 16: Linux 보안 기초 (권한, 방화벽)

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "보안 컴플라이언스를 고려한 인프라 설계"
> 파일 권한과 방화벽 설정의 기초를 이해하고 보안 환경을 구성

토스플레이스는 금융 서비스로서 높은 보안 수준이 요구됩니다. Linux 보안의 기초를 탄탄히 다져야 합니다.

---

## ⏰ 예상 학습 시간: 3.5시간

---

## 📚 Part 1: 파일 권한 심화 (1.5시간)

### 1.1 권한 구조 이해

```
┌─────────────────────────────────────────────────────────────────────┐
│  Linux 파일 권한 구조                                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  -rwxr-xr-x 1 user group 1234 Jan 1 10:00 file.txt                  │
│  │└┬┘└┬┘└┬┘   │     │                                               │
│  │ │  │  │    │     └─ 파일 그룹                                    │
│  │ │  │  │    └─────── 파일 소유자                                  │
│  │ │  │  └──────────── Others (기타 사용자): r-x = 5               │
│  │ │  └─────────────── Group (그룹): r-x = 5                       │
│  │ └────────────────── User (소유자): rwx = 7                      │
│  └──────────────────── 파일 타입: - 파일, d 디렉토리, l 심볼릭링크  │
│                                                                      │
│  권한 숫자 계산:                                                    │
│  ┌────────────────────────────────────────┐                         │
│  │  r (read)    = 4                       │                         │
│  │  w (write)   = 2                       │                         │
│  │  x (execute) = 1                       │                         │
│  │                                        │                         │
│  │  예시:                                 │                         │
│  │  rwx = 4+2+1 = 7                       │                         │
│  │  r-x = 4+0+1 = 5                       │                         │
│  │  r-- = 4+0+0 = 4                       │                         │
│  │  --- = 0+0+0 = 0                       │                         │
│  └────────────────────────────────────────┘                         │
│                                                                      │
│  일반적인 권한:                                                     │
│  755 = rwxr-xr-x : 실행 파일, 디렉토리                             │
│  644 = rw-r--r-- : 일반 파일                                        │
│  600 = rw------- : 민감한 파일 (SSH 키 등)                          │
│  700 = rwx------ : 개인 디렉토리                                    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 권한 관리 실습

```bash
# 테스트 파일 및 디렉토리 생성
mkdir -p ~/security-test
cd ~/security-test
touch test.txt
mkdir testdir

# 현재 권한 확인
ls -la

# 숫자로 권한 변경
chmod 755 test.txt   # rwxr-xr-x
chmod 644 test.txt   # rw-r--r--
chmod 600 test.txt   # rw-------

# 기호로 권한 변경
chmod u+x test.txt   # 소유자에게 실행 권한 추가
chmod g-w test.txt   # 그룹에서 쓰기 권한 제거
chmod o=r test.txt   # 기타 사용자는 읽기만
chmod a+x test.txt   # 모든 사용자에게 실행 권한 추가

# 권한 확인
ls -la test.txt
```

### 1.3 소유권 관리

```bash
# 현재 사용자/그룹 확인
whoami
groups

# 소유자 변경 (root 권한 필요)
sudo chown root test.txt

# 그룹 변경
sudo chown :adm test.txt

# 소유자와 그룹 동시 변경
sudo chown root:root test.txt

# 재귀적으로 변경 (디렉토리 전체)
sudo chown -R www-data:www-data /var/www/html/

# 소유권 확인
ls -la test.txt
stat test.txt
```

### 1.4 특수 권한

```bash
# SUID (Set User ID) - 실행 시 파일 소유자 권한으로 실행
# 예: passwd 명령어가 root 권한으로 /etc/shadow 수정 가능
ls -la /usr/bin/passwd
# -rwsr-xr-x  ← 's'가 SUID

# SUID 설정
chmod u+s script.sh
chmod 4755 script.sh

# SGID (Set Group ID) - 실행 시 파일 그룹 권한으로 실행
# 디렉토리에 설정 시 생성되는 파일이 해당 그룹 상속
chmod g+s directory/
chmod 2755 directory/

# Sticky Bit - 디렉토리에서 파일 소유자만 삭제 가능
# /tmp 디렉토리에 설정됨
ls -ld /tmp
# drwxrwxrwt  ← 't'가 Sticky Bit

chmod +t directory/
chmod 1755 directory/

# SUID 파일 찾기 (보안 감사)
find / -perm -4000 -type f 2>/dev/null
```

### 1.5 중요 시스템 파일 권한

```bash
# 반드시 확인해야 할 파일들
ls -la /etc/passwd       # 644: 사용자 정보 (누구나 읽기)
ls -la /etc/shadow       # 640: 암호 해시 (root만 읽기)
ls -la /etc/sudoers      # 440: sudo 설정 (root만 읽기)
ls -la /etc/ssh/sshd_config  # 600: SSH 서버 설정

# SSH 키 파일 권한 (매우 중요!)
ls -la ~/.ssh/
# drwx------  ~/.ssh/           (700)
# -rw-------  ~/.ssh/id_rsa     (600)
# -rw-r--r--  ~/.ssh/id_rsa.pub (644)
# -rw-------  ~/.ssh/authorized_keys (600)

# SSH 권한이 틀리면 연결 거부됨!
# 수정 방법:
chmod 700 ~/.ssh
chmod 600 ~/.ssh/id_rsa
chmod 600 ~/.ssh/authorized_keys
```

---

## 🛠️ Part 2: UFW 방화벽 (1시간)

### 2.1 UFW 개념

UFW (Uncomplicated Firewall)는 iptables를 쉽게 관리할 수 있게 해주는 도구입니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│  방화벽 개념                                                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  인터넷 ──▶ [방화벽] ──▶ 서버                                       │
│                │                                                    │
│                ├── 허용된 트래픽 → 통과                             │
│                └── 차단된 트래픽 → DROP/REJECT                      │
│                                                                      │
│  방화벽 규칙 체인:                                                  │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  INPUT   : 들어오는 트래픽 (서버로 향하는)                   │    │
│  │  OUTPUT  : 나가는 트래픽 (서버에서 나가는)                   │    │
│  │  FORWARD : 전달되는 트래픽 (라우터 역할 시)                  │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  일반적인 서버 정책:                                                │
│  - INPUT: 기본 차단, 필요한 포트만 허용                            │
│  - OUTPUT: 기본 허용 (또는 필요 시 제한)                           │
│  - FORWARD: 기본 차단 (라우터 아니면 불필요)                       │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 UFW 기본 사용

```bash
# UFW 설치 확인 (Ubuntu 기본 설치됨)
which ufw

# UFW 상태 확인
sudo ufw status
sudo ufw status verbose
sudo ufw status numbered   # 규칙 번호와 함께 표시

# UFW 활성화 (SSH 먼저 허용 후!)
sudo ufw allow 22/tcp      # SSH 먼저!
sudo ufw enable

# UFW 비활성화
sudo ufw disable

# 기본 정책 설정
sudo ufw default deny incoming   # 들어오는 것 기본 차단
sudo ufw default allow outgoing  # 나가는 것 기본 허용
```

### 2.3 포트 허용/차단

```bash
# 포트 허용
sudo ufw allow 22        # SSH (TCP/UDP 모두)
sudo ufw allow 22/tcp    # SSH (TCP만)
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 3306/tcp  # MySQL
sudo ufw allow 6379/tcp  # Redis

# 포트 범위 허용
sudo ufw allow 6000:6007/tcp

# 서비스 이름으로 허용
sudo ufw allow ssh
sudo ufw allow http
sudo ufw allow https

# 포트 차단
sudo ufw deny 23/tcp     # Telnet 차단

# 규칙 삭제
sudo ufw status numbered
sudo ufw delete 3        # 3번 규칙 삭제
sudo ufw delete allow 80/tcp
```

### 2.4 IP 기반 규칙

```bash
# 특정 IP 허용
sudo ufw allow from 192.168.1.100

# 특정 IP에서 특정 포트만 허용
sudo ufw allow from 192.168.1.100 to any port 22

# 서브넷 허용
sudo ufw allow from 192.168.1.0/24 to any port 22

# 특정 IP 차단
sudo ufw deny from 10.0.0.100

# 특정 네트워크 인터페이스만 허용
sudo ufw allow in on eth0 to any port 80
```

### 2.5 실무 시나리오

```bash
# 웹 서버 보안 설정
sudo ufw default deny incoming
sudo ufw default allow outgoing

# 필수 서비스만 허용
sudo ufw allow 22/tcp comment 'SSH'
sudo ufw allow 80/tcp comment 'HTTP'
sudo ufw allow 443/tcp comment 'HTTPS'

# 내부망에서만 관리 포트 허용
sudo ufw allow from 10.0.0.0/8 to any port 3306 comment 'MySQL internal'
sudo ufw allow from 10.0.0.0/8 to any port 6379 comment 'Redis internal'

# 방화벽 활성화
sudo ufw enable

# 설정 확인
sudo ufw status verbose
```

---

## 🔧 Part 3: iptables 기초 (30분)

### 3.1 iptables 개념

iptables는 Linux 커널의 네트워크 패킷 필터링 시스템입니다. UFW보다 세밀한 제어가 가능합니다.

```
┌─────────────────────────────────────────────────────────────────────┐
│  iptables 구조                                                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  테이블 (Tables):                                                   │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  filter : 패킷 필터링 (기본, INPUT/OUTPUT/FORWARD)          │    │
│  │  nat    : 네트워크 주소 변환 (PREROUTING/POSTROUTING)       │    │
│  │  mangle : 패킷 수정                                         │    │
│  │  raw    : 연결 추적 제외                                    │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  체인 (Chains):                                                     │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  INPUT    : 로컬 시스템으로 향하는 패킷                      │    │
│  │  OUTPUT   : 로컬 시스템에서 나가는 패킷                      │    │
│  │  FORWARD  : 시스템을 통과하는 패킷 (라우팅)                  │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  타겟 (Targets):                                                    │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  ACCEPT : 패킷 허용                                         │    │
│  │  DROP   : 패킷 버림 (응답 없음)                             │    │
│  │  REJECT : 패킷 거부 (에러 응답)                             │    │
│  │  LOG    : 로그 기록 후 다음 규칙으로                        │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.2 iptables 기본 명령어

```bash
# 현재 규칙 확인
sudo iptables -L -n -v              # 상세 정보
sudo iptables -L -n -v --line-numbers  # 줄 번호 포함

# 규칙 추가 (-A: Append)
sudo iptables -A INPUT -p tcp --dport 80 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 443 -j ACCEPT

# 규칙 삽입 (-I: Insert at position)
sudo iptables -I INPUT 1 -p tcp --dport 22 -j ACCEPT

# 특정 IP 허용
sudo iptables -A INPUT -s 192.168.1.100 -j ACCEPT

# 특정 IP 차단
sudo iptables -A INPUT -s 10.0.0.100 -j DROP

# 연결 상태 기반 규칙 (Stateful)
sudo iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

# 규칙 삭제
sudo iptables -D INPUT 3   # 3번 규칙 삭제
sudo iptables -D INPUT -p tcp --dport 80 -j ACCEPT

# 모든 규칙 삭제
sudo iptables -F

# 기본 정책 설정
sudo iptables -P INPUT DROP
sudo iptables -P FORWARD DROP
sudo iptables -P OUTPUT ACCEPT
```

### 3.3 iptables 규칙 저장

```bash
# 현재 규칙 저장
sudo iptables-save > /etc/iptables.rules

# 규칙 복원
sudo iptables-restore < /etc/iptables.rules

# 부팅 시 자동 로드 (Ubuntu)
sudo apt install iptables-persistent
sudo netfilter-persistent save
sudo netfilter-persistent reload
```

---

## 📊 Part 4: 보안 감사 (30분)

### 4.1 권한 감사

```bash
# SUID/SGID 파일 찾기 (권한 상승 취약점)
find / -perm -4000 -type f 2>/dev/null  # SUID
find / -perm -2000 -type f 2>/dev/null  # SGID
find / -perm -6000 -type f 2>/dev/null  # SUID+SGID

# 쓰기 가능한 파일 찾기 (누구나)
find / -perm -002 -type f 2>/dev/null

# 소유자 없는 파일 찾기
find / -nouser -o -nogroup 2>/dev/null

# 홈 디렉토리 권한 확인
ls -la /home/
```

### 4.2 열린 포트 확인

```bash
# 리스닝 포트 확인
sudo netstat -tlnp
sudo ss -tlnp

# 특정 포트 프로세스 확인
sudo lsof -i :80
sudo fuser 80/tcp

# 외부에서 열린 포트 스캔 (nmap)
nmap -sT localhost
```

### 4.3 로그 모니터링

```bash
# 인증 로그 (SSH 시도 등)
sudo tail -f /var/log/auth.log

# 시스템 로그
sudo tail -f /var/log/syslog

# 최근 로그인 기록
last
lastlog

# 실패한 로그인 시도
sudo grep "Failed password" /var/log/auth.log | tail -20
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | chmod로 파일 권한 변경 | ☐ |
| 2 | chown으로 소유권 변경 | ☐ |
| 3 | 특수 권한 (SUID, SGID) 이해 | ☐ |
| 4 | 중요 파일 권한 확인 (/etc/shadow 등) | ☐ |
| 5 | UFW로 방화벽 규칙 추가 | ☐ |
| 6 | 특정 포트 허용/차단 | ☐ |
| 7 | IP 기반 규칙 설정 | ☐ |
| 8 | iptables 기초 이해 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# 권한 관리
chmod 755 <file>
chmod u+x <file>
chown user:group <file>

# UFW 방화벽
sudo ufw status
sudo ufw allow 22/tcp
sudo ufw allow from 192.168.1.0/24 to any port 22
sudo ufw enable

# iptables
sudo iptables -L -n -v
sudo iptables -A INPUT -p tcp --dport 80 -j ACCEPT
sudo iptables-save > /etc/iptables.rules
```

---

## 💡 면접 대비 핵심 포인트

### Q1: 파일 권한 755와 644의 차이는?
**A**: 
- **755 (rwxr-xr-x)**: 소유자는 읽기/쓰기/실행, 그룹과 기타는 읽기/실행. 실행 파일이나 디렉토리에 사용.
- **644 (rw-r--r--)**: 소유자는 읽기/쓰기, 그룹과 기타는 읽기만. 일반 파일에 사용.

### Q2: SUID란 무엇이고 왜 보안에 중요한가?
**A**: SUID(Set User ID)는 실행 시 파일 소유자 권한으로 실행되게 합니다. 예: `passwd` 명령어가 일반 사용자도 `/etc/shadow`를 수정 가능. 악용 시 권한 상승 취약점이 될 수 있어 SUID 파일 정기 감사가 필요합니다.

### Q3: UFW와 iptables의 관계는?
**A**: UFW는 iptables의 프론트엔드입니다. 복잡한 iptables 명령어를 쉽게 사용할 수 있게 해줍니다. 내부적으로는 iptables 규칙을 생성합니다.

### Q4: 방화벽 기본 정책을 "deny incoming"으로 하는 이유는?
**A**: 최소 권한 원칙에 따라 필요한 서비스만 명시적으로 허용하고 나머지는 차단합니다. 이렇게 하면 실수로 서비스가 노출되는 것을 방지할 수 있습니다.

---

## 🔗 참고 자료

- [Linux Permissions](https://www.linux.com/training-tutorials/understanding-linux-file-permissions/)
- [UFW Documentation](https://help.ubuntu.com/community/UFW)
- [iptables Tutorial](https://www.frozentux.net/iptables-tutorial/iptables-tutorial.html)

---

## ➡️ 다음 학습: Day 17

**주제**: SSH 키 관리 및 보안 설정
- SSH 키 생성 및 관리
- sshd_config 보안 설정
- SSH config 파일 활용
