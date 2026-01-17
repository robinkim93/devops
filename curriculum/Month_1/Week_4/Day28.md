# 📅 Day 28: GitHub 업로드 및 포트폴리오 정리

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "이력서에 트러블슈팅 경험 작성"

프로젝트를 GitHub에 업로드하고 포트폴리오로 정리하여 면접에서 활용할 수 있도록 합니다.

---

## ⏰ 예상 소요 시간: 3시간

| 구분 | 시간 | 내용 |
|------|------|------|
| Git 설정 | 30분 | .gitignore, 커밋 |
| GitHub 업로드 | 30분 | 원격 저장소 연결 |
| README 작성 | 1시간 | 프로젝트 문서화 |
| 포트폴리오 정리 | 1시간 | GitHub Profile 설정 |

---

## 📚 Part 1: Git 프로젝트 설정 (30분)

### .gitignore 생성

```bash
cd ~/portfolio/month1-troubleshooting

cat << 'EOF' > .gitignore
# Docker
*.log
docker-compose.override.yml

# Python
__pycache__/
*.py[cod]
*$py.class
*.so
.Python
env/
venv/
.env
.env.local

# IDE
.vscode/
.idea/
*.swp
*.swo

# OS
.DS_Store
Thumbs.db

# 테스트/임시 파일
*.tmp
*.bak
coverage/
.pytest_cache/

# 민감 정보
*.pem
*.key
secrets/
EOF
```

### Git 초기화 및 커밋

```bash
# Git 초기화
git init

# 사용자 정보 설정 (최초 1회)
git config user.name "Your Name"
git config user.email "your_email@example.com"

# 파일 추가
git add .

# 커밋 메시지 작성
git commit -m "feat: Linux troubleshooting portfolio project

📦 프로젝트 구성:
- 3-Tier 웹 애플리케이션 (Nginx + Flask + Redis)
- Docker Compose 기반 컨테이너 환경

🔧 장애 시뮬레이션:
- OOMKilled 재현 및 분석
- CPU 병목 분석
- 네트워크 연결 문제 해결

📄 문서:
- 트러블슈팅 가이드
- 장애 분석 블로그 포스트

🎯 학습 목표:
- Linux 시스템 모니터링
- Docker 컨테이너 디버깅
- 성능 분석 도구 활용
"
```

---

## 📚 Part 2: GitHub 저장소 연결 (30분)

### GitHub 저장소 생성

```bash
# GitHub에서 새 저장소 생성
# 이름: linux-troubleshooting-portfolio
# 설명: Docker 환경에서 발생하는 다양한 장애를 분석하고 해결하는 포트폴리오

# 원격 저장소 연결
git remote add origin https://github.com/YOUR_USERNAME/linux-troubleshooting-portfolio.git

# 기본 브랜치 설정
git branch -M main

# Push
git push -u origin main
```

### SSH 키 설정 (권장)

```bash
# SSH 키 확인/생성
ls -la ~/.ssh/
ssh-keygen -t ed25519 -C "your_email@example.com"

# 공개키 복사
cat ~/.ssh/id_ed25519.pub
# GitHub Settings > SSH Keys에 추가

# SSH로 원격 저장소 변경
git remote set-url origin git@github.com:YOUR_USERNAME/linux-troubleshooting-portfolio.git
```

---

## 📚 Part 3: README.md 작성 (1시간)

### 전문적인 README 구조

```markdown
# 🔧 Linux Troubleshooting Portfolio

Docker 컨테이너 환경에서 발생하는 다양한 장애(OOMKilled, CPU 병목, 네트워크 문제)를 
분석하고 해결하는 과정을 문서화한 DevOps 포트폴리오입니다.

## 📋 목차
- [프로젝트 소개](#-프로젝트-소개)
- [기술 스택](#-기술-스택)
- [아키텍처](#-아키텍처)
- [장애 시나리오](#-장애-시나리오)
- [빠른 시작](#-빠른-시작)
- [트러블슈팅 가이드](#-트러블슈팅-가이드)
- [학습 내용](#-학습-내용)

## 🎯 프로젝트 소개

이 프로젝트는 DevOps 엔지니어로서 실제 운영 환경에서 발생할 수 있는 
장애 상황을 시뮬레이션하고, 체계적인 분석 방법론을 통해 문제를 해결하는 
역량을 보여주기 위해 제작되었습니다.

### 핵심 역량
- ✅ 시스템 모니터링 및 성능 분석
- ✅ 컨테이너 환경 디버깅
- ✅ 장애 근본 원인 분석 (RCA)
- ✅ 문서화 및 재발 방지 대책 수립

## 🛠 기술 스택

| 분류 | 기술 |
|------|------|
| **Container** | Docker, Docker Compose |
| **Web Server** | Nginx |
| **Application** | Python, Flask |
| **Cache** | Redis |
| **Monitoring** | top, htop, vmstat, iostat |
| **Debugging** | strace, lsof, ss, tcpdump |

## 🏗 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                    Client Request                       │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                   Nginx (Port 80)                       │
│                   - Reverse Proxy                       │
│                   - Load Balancing                      │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                   Flask App (Port 5000)                 │
│                   - /health - 헬스체크                  │
│                   - /stress/memory - 메모리 부하        │
│                   - /stress/cpu - CPU 부하              │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                   Redis (Port 6379)                     │
│                   - Session Cache                       │
│                   - Data Store                          │
└─────────────────────────────────────────────────────────┘
```

## 💥 장애 시나리오

### 시나리오 1: OOMKilled
- **증상**: 컨테이너가 갑자기 재시작됨
- **원인**: 메모리 누수로 인한 OOM
- **해결**: 메모리 제한 설정, 누수 코드 수정

### 시나리오 2: CPU 병목
- **증상**: 응답 시간 급격히 증가
- **원인**: 무한 루프 또는 비효율적 알고리즘
- **해결**: 프로파일링으로 핫스팟 식별

### 시나리오 3: 네트워크 연결 실패
- **증상**: 서비스 간 통신 불가
- **원인**: DNS 오류, 포트 충돌
- **해결**: 네트워크 진단 도구로 원인 파악

## 🚀 빠른 시작

```bash
# 저장소 클론
git clone https://github.com/YOUR_USERNAME/linux-troubleshooting-portfolio.git
cd linux-troubleshooting-portfolio

# 컨테이너 실행
docker-compose up -d

# 헬스체크
curl http://localhost/health

# 장애 시뮬레이션
curl http://localhost/stress/memory?mb=100
curl http://localhost/stress/cpu?duration=30
```

## 📖 트러블슈팅 가이드

상세한 트러블슈팅 가이드는 [docs/troubleshooting-guide.md](docs/troubleshooting-guide.md)에서 
확인할 수 있습니다.

### 주요 분석 명령어

```bash
# 컨테이너 상태 확인
docker stats
docker logs <container_name>

# 시스템 리소스 확인
top -b -n 1 | head -20
free -h
df -h

# 프로세스 분석
ps aux --sort=-%mem | head -10
lsof -p <pid>
```

## 📝 학습 내용

이 프로젝트를 통해 학습한 내용:

1. **Linux 시스템 관리**
   - 프로세스/메모리/디스크 관리
   - 성능 모니터링 도구 활용

2. **Docker 운영**
   - 컨테이너 라이프사이클 관리
   - 리소스 제한 설정
   - 로그 수집 및 분석

3. **트러블슈팅 방법론**
   - 체계적인 문제 분석
   - 근본 원인 분석 (RCA)
   - 재발 방지 대책 수립

## 👤 Author

- GitHub: [@YOUR_USERNAME](https://github.com/YOUR_USERNAME)
- LinkedIn: [Your Name](https://linkedin.com/in/your-profile)

## 📄 License

This project is licensed under the MIT License.
```

---

## 📚 Part 4: GitHub Profile 설정 (1시간)

### 저장소 설명 및 토픽 설정

```
📌 GitHub 저장소 설정

Description:
Docker 컨테이너 환경에서 발생하는 장애(OOMKilled, CPU 병목, 연결 문제)를 
분석하고 해결하는 과정을 문서화한 DevOps 포트폴리오

Topics (태그):
docker, linux, troubleshooting, devops, flask, nginx, 
monitoring, debugging, performance, redis
```

### 프로젝트 구조 최종 확인

```
linux-troubleshooting-portfolio/
├── README.md                      # 프로젝트 소개
├── docker-compose.yml             # 컨테이너 구성
├── .gitignore
│
├── app/                           # Flask 애플리케이션
│   ├── app.py
│   ├── Dockerfile
│   └── requirements.txt
│
├── nginx/                         # 리버스 프록시
│   ├── Dockerfile
│   └── nginx.conf
│
├── scripts/                       # 유틸리티 스크립트
│   ├── health_check.sh
│   └── memory_alert.sh
│
└── docs/                          # 문서
    ├── troubleshooting-guide.md   # 트러블슈팅 가이드
    └── blog-post-oom.md           # OOMKilled 분석기
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | .gitignore 생성 | ☐ |
| 2 | Git 초기화 및 커밋 | ☐ |
| 3 | GitHub 저장소 생성 | ☐ |
| 4 | 원격 저장소 연결 및 Push | ☐ |
| 5 | README.md 작성 | ☐ |
| 6 | 저장소 설명/토픽 설정 | ☐ |
| 7 | 프로젝트 구조 최종 검토 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# Git 기본
git init
git add .
git commit -m "message"
git remote add origin <url>
git push -u origin main

# Git 상태 확인
git status
git log --oneline
git remote -v
```

---

## 📝 포트폴리오 활용 팁

### 면접에서 활용하기

```
Q: 트러블슈팅 경험을 말씀해주세요.

A: "Docker 환경에서 OOMKilled 문제를 해결한 경험이 있습니다. 
   컨테이너가 갑자기 재시작되는 현상이 발생했는데, 
   docker stats와 dmesg를 통해 메모리 초과가 원인임을 확인했습니다.
   
   근본 원인은 API 응답을 메모리에 캐싱하는 로직에서 
   크기 제한이 없어 메모리가 계속 증가한 것이었습니다.
   
   해결책으로 캐시 크기 제한과 TTL을 설정하고,
   Docker 리소스 제한(memory limit)을 추가했습니다.
   
   이 경험을 GitHub에 문서화했으며, 링크를 공유드릴 수 있습니다."
```

### GitHub 프로필에 Pin하기

```
1. GitHub 프로필 페이지 접속
2. "Customize your pins" 클릭
3. linux-troubleshooting-portfolio 선택
4. 가장 앞에 배치
```

---

## ➡️ 다음: Day 29

**주제**: Month 1 총정리 및 복습
- 4주간 학습 내용 복습
- 핵심 명령어 치트시트
- 면접 예상 질문 정리
