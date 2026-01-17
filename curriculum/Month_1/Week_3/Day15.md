# Day 15: systemd 서비스 관리

## 오늘의 목표

토스플레이스 연결점: 서비스 관리, 장애 시 자동 복구 설정
"OS, Network 등 다양한 레이어에서의 모니터링, 트러블슈팅 경험"

systemd로 서비스를 관리하고 모니터링하는 방법을 익힙니다. Docker, kubelet, 모니터링 에이전트 등 모든 주요 서비스가 systemd로 관리됩니다.

---

## 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 핵심 개념 | 45분 | systemd 구조, Unit 파일 |
| 기본 실습 | 1시간 | 서비스 제어, 로그 확인 |
| 고급 실습 | 1.5시간 | 서비스 생성, 자동 복구 |
| 트러블슈팅 | 45분 | 장애 진단 및 해결 |

---

## Part 1: 핵심 개념 (45분)

### 1.1 systemd란?

systemd는 Linux의 init 시스템이자 시스템 및 서비스 관리자입니다.

```
systemd 역할:
- PID 1로 실행 (시스템의 첫 번째 프로세스)
- 시스템 부팅 시 모든 서비스 시작/관리
- 서비스 간 의존성 관리
- 자동 복구 (restart) 기능
- 로그 수집 (journald)
- 소켓 활성화, 타이머 등 고급 기능

관리 대상:
- Docker/containerd (컨테이너 런타임)
- kubelet (Kubernetes 노드 에이전트)
- sshd (SSH 서버)
- nginx, mysql 등 모든 서버 소프트웨어
- prometheus-node-exporter (모니터링)
```

### 1.2 systemd 구조

```
systemd 아키텍처

systemd (PID 1)
    |
    +-- Unit 관리
    |   +-- Service (.service)   # 데몬 프로세스
    |   +-- Socket (.socket)     # 소켓 활성화
    |   +-- Timer (.timer)       # 예약 작업 (cron 대체)
    |   +-- Mount (.mount)       # 마운트 포인트
    |   +-- Target (.target)     # Unit 그룹
    |
    +-- journald                 # 로그 수집
    |
    +-- logind                   # 로그인 관리
    |
    +-- networkd                 # 네트워크 관리 (선택)


Unit 파일 위치:
/etc/systemd/system/        # 관리자 생성 (최우선)
/run/systemd/system/        # 런타임 생성
/lib/systemd/system/        # 패키지 설치 (기본)
```

### 1.3 주요 명령어

| 명령어 | 설명 | 예시 |
|--------|------|------|
| systemctl start | 서비스 시작 | systemctl start docker |
| systemctl stop | 서비스 중지 | systemctl stop docker |
| systemctl restart | 재시작 | systemctl restart docker |
| systemctl reload | 설정 리로드 (재시작 없이) | systemctl reload nginx |
| systemctl status | 상태 확인 | systemctl status docker |
| systemctl enable | 부팅 시 자동 시작 | systemctl enable docker |
| systemctl disable | 자동 시작 해제 | systemctl disable docker |
| systemctl is-active | 활성 상태 확인 | systemctl is-active docker |
| systemctl is-enabled | 자동 시작 설정 확인 | systemctl is-enabled docker |
| systemctl daemon-reload | Unit 파일 변경 적용 | systemctl daemon-reload |
| systemctl list-units | Unit 목록 | systemctl list-units --type=service |

---

## Part 2: 기본 실습 (1시간)

### 실습 1: 서비스 상태 확인 (20분)

```bash
# Docker 서비스 상태 확인
sudo systemctl status docker

# 출력 해석:
# docker.service - Docker Application Container Engine
#    Loaded: loaded (/lib/systemd/system/docker.service; enabled; ...)
#    Active: active (running) since Mon 2026-01-12 10:00:00 UTC; 5h ago
#      Docs: https://docs.docker.com
#  Main PID: 1234 (dockerd)
#     Tasks: 50
#    Memory: 100.5M
#    CGroup: /system.slice/docker.service
#            |-- 1234 /usr/bin/dockerd -H fd:// ...

# 핵심 필드:
# Loaded: Unit 파일 위치, enabled/disabled
# Active: active (running) = 실행 중
#         inactive (dead) = 중지됨
#         failed = 실패
# Main PID: 메인 프로세스 ID
# Memory: 메모리 사용량
# CGroup: 프로세스 그룹 정보
```

여러 서비스 상태 확인:
```bash
# SSH 서비스
sudo systemctl status sshd

# containerd (컨테이너 런타임)
sudo systemctl status containerd

# kubelet (K8s 노드에서)
sudo systemctl status kubelet

# 간단한 상태 확인
systemctl is-active docker
systemctl is-enabled docker
```

### 실습 2: 서비스 제어 (20분)

```bash
# 서비스 시작
sudo systemctl start docker

# 서비스 중지
sudo systemctl stop docker

# 서비스 재시작
sudo systemctl restart docker

# 설정만 리로드 (프로세스 재시작 없이)
sudo systemctl reload nginx

# 부팅 시 자동 시작 설정
sudo systemctl enable docker

# 자동 시작 해제
sudo systemctl disable docker

# 활성화 + 시작 동시에
sudo systemctl enable --now docker

# 비활성화 + 중지 동시에
sudo systemctl disable --now docker
```

### 실습 3: 로그 확인 - journalctl (20분)

journald는 systemd의 로그 수집 시스템입니다.

```bash
# 특정 서비스 로그
sudo journalctl -u docker

# 최근 N줄만
sudo journalctl -u docker -n 50

# 실시간 로그 follow
sudo journalctl -u docker -f

# 시간 범위 지정
sudo journalctl -u docker --since "1 hour ago"
sudo journalctl -u docker --since "2026-01-12 00:00:00" --until "2026-01-12 12:00:00"
sudo journalctl -u docker --since today

# 우선순위 필터 (에러 이상만)
sudo journalctl -u docker -p err

# 역순 (최신 먼저)
sudo journalctl -u docker -r

# JSON 형식
sudo journalctl -u docker -o json-pretty

# 부팅 이후 로그만
sudo journalctl -u docker -b
```

우선순위 레벨:
| 레벨 | 숫자 | 의미 |
|------|------|------|
| emerg | 0 | 시스템 사용 불가 |
| alert | 1 | 즉시 조치 필요 |
| crit | 2 | 심각한 상태 |
| err | 3 | 에러 |
| warning | 4 | 경고 |
| notice | 5 | 정상이지만 중요 |
| info | 6 | 정보 |
| debug | 7 | 디버그 |

```bash
# 에러 이상만 (-p err = 0~3)
sudo journalctl -u docker -p err

# 경고 이상만 (-p warning = 0~4)
sudo journalctl -u docker -p warning
```

---

## Part 3: 고급 실습 (1.5시간)

### 실습 4: Unit 파일 구조 이해 (30분)

기존 서비스 Unit 파일 분석:
```bash
# Docker Unit 파일 확인
cat /lib/systemd/system/docker.service
```

Unit 파일 구조:
```ini
[Unit]
Description=Docker Application Container Engine
Documentation=https://docs.docker.com
After=network-online.target firewalld.service containerd.service
Wants=network-online.target
Requires=containerd.service

[Service]
Type=notify
ExecStart=/usr/bin/dockerd -H fd://
ExecReload=/bin/kill -s HUP $MAINPID
TimeoutSec=0
RestartSec=2
Restart=always
LimitNOFILE=infinity

[Install]
WantedBy=multi-user.target
```

각 섹션 설명:
```
[Unit] 섹션:
- Description: 서비스 설명
- Documentation: 문서 URL
- After: 이 유닛 이후에 시작
- Wants: 약한 의존성 (없어도 시작)
- Requires: 강한 의존성 (없으면 실패)

[Service] 섹션:
- Type: 서비스 타입
  - simple: 기본, ExecStart가 메인 프로세스
  - forking: fork 후 부모 종료
  - notify: 준비 완료 시 systemd에 알림
  - oneshot: 한 번 실행 후 종료
- ExecStart: 시작 명령
- ExecStop: 중지 명령
- ExecReload: 리로드 명령
- Restart: 재시작 정책
  - no, always, on-failure, on-abnormal
- RestartSec: 재시작 전 대기 시간

[Install] 섹션:
- WantedBy: enable 시 연결할 target
  - multi-user.target: 일반적인 서버 모드
  - graphical.target: GUI 모드
```

### 실습 5: 사용자 정의 서비스 생성 (40분)

간단한 애플리케이션 서비스 만들기:

```bash
# 1. 애플리케이션 스크립트 생성
sudo tee /usr/local/bin/myapp.sh << 'EOF'
#!/bin/bash
LOG_FILE=/var/log/myapp.log

echo "$(date): MyApp starting..." >> $LOG_FILE

while true; do
    echo "$(date): MyApp heartbeat - PID: $$" >> $LOG_FILE
    sleep 10
done
EOF

sudo chmod +x /usr/local/bin/myapp.sh

# 2. Unit 파일 생성
sudo tee /etc/systemd/system/myapp.service << 'EOF'
[Unit]
Description=My Custom Application
After=network.target

[Service]
Type=simple
ExecStart=/usr/local/bin/myapp.sh
ExecStop=/bin/kill -TERM $MAINPID
Restart=on-failure
RestartSec=5
StandardOutput=journal
StandardError=journal
User=nobody
Group=nogroup

# 리소스 제한
MemoryLimit=100M
CPUQuota=20%

[Install]
WantedBy=multi-user.target
EOF

# 3. systemd에 변경 사항 알림 (필수!)
sudo systemctl daemon-reload

# 4. 서비스 시작 및 활성화
sudo systemctl start myapp
sudo systemctl enable myapp

# 5. 상태 확인
sudo systemctl status myapp

# 6. 로그 확인
sudo journalctl -u myapp -f
```

### 실습 6: 자동 복구 테스트 (20분)

```bash
# 현재 상태 확인
sudo systemctl status myapp
# Main PID 확인

# 프로세스 강제 종료
sudo pkill -f myapp.sh

# 자동 재시작 확인 (5초 후)
sleep 6
sudo systemctl status myapp
# 새로운 PID로 재시작됨!

# 재시작 횟수 확인
systemctl show myapp -p NRestarts
```

다양한 Restart 정책:
```ini
# 항상 재시작 (무한)
Restart=always
RestartSec=5

# 실패 시만 재시작
Restart=on-failure
RestartSec=5

# 재시작 제한 (burst 방지)
StartLimitIntervalSec=60
StartLimitBurst=3
# 60초 내 3번 초과 재시작 시도하면 실패로 처리
```

### 실습 7: 환경 변수와 시크릿 관리 (20분)

```bash
# 환경 변수 파일 생성
sudo tee /etc/myapp/myapp.conf << 'EOF'
APP_ENV=production
LOG_LEVEL=info
DB_HOST=localhost
DB_PORT=5432
EOF

# Unit 파일에서 환경 변수 로드
sudo tee /etc/systemd/system/myapp-env.service << 'EOF'
[Unit]
Description=MyApp with Environment Variables

[Service]
Type=simple
EnvironmentFile=/etc/myapp/myapp.conf
Environment="EXTRA_VAR=value"
ExecStart=/usr/local/bin/myapp-env.sh
Restart=on-failure

[Install]
WantedBy=multi-user.target
EOF

# 환경 변수 확인 스크립트
sudo tee /usr/local/bin/myapp-env.sh << 'EOF'
#!/bin/bash
echo "APP_ENV: $APP_ENV"
echo "LOG_LEVEL: $LOG_LEVEL"
echo "DB_HOST: $DB_HOST"
while true; do sleep 60; done
EOF
sudo chmod +x /usr/local/bin/myapp-env.sh

# 적용
sudo systemctl daemon-reload
sudo systemctl start myapp-env
sudo journalctl -u myapp-env -n 10
```

---

## Part 4: 트러블슈팅 (45분)

### 실습 8: 서비스 장애 진단 (30분)

서비스가 시작 안 될 때:
```bash
# 1. 상태 확인 (에러 메시지 확인)
sudo systemctl status myapp

# 2. 상세 로그 확인
sudo journalctl -u myapp -n 100 --no-pager

# 3. Unit 파일 문법 검사
sudo systemd-analyze verify /etc/systemd/system/myapp.service

# 4. 의존성 확인
systemctl list-dependencies myapp

# 5. 실패 원인 분석
systemctl show myapp -p Result,ExecMainStatus,ActiveState
```

일반적인 문제와 해결:
```
문제 1: "Failed to start - exec format error"
원인: 스크립트에 shebang(#!/bin/bash) 없음
해결: 스크립트 첫 줄에 #!/bin/bash 추가

문제 2: "Failed to start - permission denied"
원인: 실행 권한 없음
해결: chmod +x /path/to/script

문제 3: "Start request repeated too quickly"
원인: 재시작 제한 초과
해결: systemctl reset-failed myapp && systemctl start myapp

문제 4: "Unit not found"
원인: daemon-reload 안 함
해결: systemctl daemon-reload
```

### 실습 9: 서비스 목록 및 상태 관리 (15분)

```bash
# 실행 중인 서비스
systemctl list-units --type=service --state=running

# 실패한 서비스
systemctl list-units --type=service --state=failed

# 모든 서비스 (설치된)
systemctl list-unit-files --type=service

# 실패한 서비스 초기화
sudo systemctl reset-failed

# 특정 서비스 실패 초기화
sudo systemctl reset-failed myapp

# 부팅 시간 분석
systemd-analyze
systemd-analyze blame | head -10
```

---

## 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | systemctl status 사용 | 서비스 상태 확인 | |
| 2 | systemctl start/stop/restart | 서비스 제어 | |
| 3 | systemctl enable/disable | 자동 시작 설정 | |
| 4 | journalctl -u 사용 | 서비스 로그 확인 | |
| 5 | journalctl 시간/우선순위 필터 | 로그 필터링 | |
| 6 | Unit 파일 구조 이해 | [Unit], [Service], [Install] | |
| 7 | 사용자 정의 서비스 생성 | myapp.service | |
| 8 | Restart=on-failure 테스트 | 자동 복구 확인 | |

---

## 핵심 명령어

```bash
# 서비스 상태/제어
systemctl status/start/stop/restart <service>
systemctl enable/disable <service>
systemctl is-active/is-enabled <service>

# Unit 파일 변경 후 (필수!)
systemctl daemon-reload

# 로그 확인
journalctl -u <service>           # 서비스 로그
journalctl -u <service> -f        # 실시간
journalctl -u <service> -n 100    # 최근 100줄
journalctl -u <service> -p err    # 에러만
journalctl -u <service> --since "1 hour ago"

# 문제 해결
systemctl reset-failed <service>
systemd-analyze verify /path/to/unit
systemctl list-dependencies <service>
```

---

## 면접 대비 핵심 포인트

### Q1: 서비스가 죽었을 때 자동으로 재시작하려면?

A: Unit 파일의 [Service] 섹션에 Restart=on-failure 또는 Restart=always를 설정합니다. RestartSec으로 재시작 간격을 조절하고, StartLimitBurst/StartLimitIntervalSec으로 무한 재시작을 방지합니다.

### Q2: systemctl과 service 명령어의 차이는?

A: service는 레거시 SysV init 명령어이고, systemctl은 systemd 전용 명령어입니다. 현대 Linux에서는 systemctl을 사용해야 합니다. service 명령어는 내부적으로 systemctl로 리다이렉트됩니다.

### Q3: journalctl로 특정 시간대의 에러 로그만 보려면?

A: journalctl -u 서비스명 -p err --since "시작시간" --until "종료시간" 을 사용합니다. 예: journalctl -u docker -p err --since "2026-01-12 00:00:00" --until "2026-01-12 12:00:00"

---

## 학습 기록

```
학습일: ____년 __월 __일
실제 소요 시간: ____시간

완료한 실습:
- [ ] systemctl status 확인
- [ ] journalctl 로그 확인
- [ ] 사용자 정의 서비스 생성
- [ ] 자동 복구 테스트

이해가 어려웠던 부분:

추가 학습 필요 항목:
```

---

## 정리

```bash
# 실습 리소스 삭제
sudo systemctl stop myapp myapp-env
sudo systemctl disable myapp myapp-env
sudo rm /etc/systemd/system/myapp.service
sudo rm /etc/systemd/system/myapp-env.service
sudo rm /usr/local/bin/myapp.sh /usr/local/bin/myapp-env.sh
sudo rm -rf /etc/myapp
sudo rm /var/log/myapp.log
sudo systemctl daemon-reload
```

---

## 다음 학습: Day 16

주제: Linux 보안 기초 (권한, 방화벽)
- 파일 권한 (chmod, chown)
- 방화벽 (ufw, iptables)
- SELinux/AppArmor 기초