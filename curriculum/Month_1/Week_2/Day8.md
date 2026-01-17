# Day 8: Docker 설치 및 기본 명령어

## 오늘의 목표

토스플레이스 연결점: "컨테이너 오케스트레이션 서비스 운영"
"Podman, Containerd 등 컨테이너 런타임 경험"

Docker를 설치하고 기본 명령어를 학습합니다. 컨테이너의 생명주기를 이해하고 이미지와 컨테이너를 관리하는 방법을 익힙니다.

---

## 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 개념 학습 | 45분 | 컨테이너 vs VM, Docker 아키텍처 |
| 설치 | 30분 | Docker 설치 및 설정 |
| 기본 실습 | 1.5시간 | 이미지, 컨테이너 관리 |
| 심화 실습 | 1시간 15분 | 로그, 포트, 볼륨 |

---

## Part 1: 컨테이너 개념 (45분)

### 1.1 컨테이너 vs 가상 머신

```
가상 머신 (VM):
┌─────────────────────────────────────────┐
│            Application                   │
├─────────────────────────────────────────┤
│         Guest OS (Linux/Windows)         │
├─────────────────────────────────────────┤
│              Hypervisor                  │
├─────────────────────────────────────────┤
│              Host OS                     │
├─────────────────────────────────────────┤
│              Hardware                    │
└─────────────────────────────────────────┘

컨테이너:
┌─────────────────────────────────────────┐
│            Application                   │
├─────────────────────────────────────────┤
│         Container Runtime (Docker)       │
├─────────────────────────────────────────┤
│              Host OS                     │
├─────────────────────────────────────────┤
│              Hardware                    │
└─────────────────────────────────────────┘

차이점:
- VM: 전체 OS 가상화 (무거움, 느림)
- 컨테이너: 프로세스 격리 (가벼움, 빠름)
```

### 1.2 Docker 아키텍처

```
Docker 구성요소:

┌────────────────────────────────────────────────────────┐
│                    Docker Client                        │
│                 (docker CLI)                            │
└────────────────────────┬───────────────────────────────┘
                         │ REST API
                         ▼
┌────────────────────────────────────────────────────────┐
│                    Docker Daemon                        │
│                    (dockerd)                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │   Images     │  │  Containers  │  │   Networks   │ │
│  └──────────────┘  └──────────────┘  └──────────────┘ │
└────────────────────────┬───────────────────────────────┘
                         │
                         ▼
┌────────────────────────────────────────────────────────┐
│              Container Runtime (containerd)             │
└────────────────────────────────────────────────────────┘
```

### 1.3 핵심 개념

| 개념 | 설명 |
|------|------|
| Image | 컨테이너 실행을 위한 읽기 전용 템플릿 |
| Container | 이미지의 실행 인스턴스 |
| Registry | 이미지 저장소 (Docker Hub) |
| Dockerfile | 이미지 빌드 스크립트 |
| Volume | 데이터 영속화 |

---

## Part 2: Docker 설치 (30분)

### 실습 1: Docker 설치 (Ubuntu/Debian)

```bash
# 이전 버전 제거
sudo apt remove docker docker-engine docker.io containerd runc

# 필수 패키지 설치
sudo apt update
sudo apt install -y ca-certificates curl gnupg

# Docker GPG 키 추가
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# Docker 저장소 추가
echo \
  "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Docker 설치
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# sudo 없이 docker 사용
sudo usermod -aG docker $USER
newgrp docker

# 설치 확인
docker --version
docker info
```

### 실습 2: Docker 설치 (macOS)

```bash
# Homebrew로 Docker Desktop 설치
brew install --cask docker

# Docker Desktop 실행 후
docker --version
docker info
```

### 실습 3: 설치 확인

```bash
# Docker 버전
docker --version

# Docker 정보
docker info

# Hello World 테스트
docker run hello-world
```

---

## Part 3: 기본 명령어 (1.5시간)

### 실습 4: 이미지 관리

```bash
# 이미지 검색
docker search nginx

# 이미지 다운로드
docker pull nginx
docker pull nginx:1.24    # 특정 버전
docker pull redis:alpine  # alpine 버전

# 이미지 목록
docker images
docker image ls

# 이미지 상세 정보
docker inspect nginx

# 이미지 히스토리
docker history nginx

# 이미지 삭제
docker rmi nginx:1.24
docker image prune       # 미사용 이미지 삭제
docker image prune -a    # 모든 미사용 이미지 삭제
```

### 실습 5: 컨테이너 실행

```bash
# 기본 실행
docker run nginx
# Ctrl+C로 종료

# 백그라운드 실행
docker run -d nginx

# 이름 지정
docker run -d --name my-nginx nginx

# 포트 매핑
docker run -d -p 8080:80 --name web nginx
curl http://localhost:8080

# 환경변수 설정
docker run -d -e MYSQL_ROOT_PASSWORD=secret mysql

# 자동 삭제 (종료 시)
docker run --rm nginx echo "Hello"
```

### 실습 6: 컨테이너 관리

```bash
# 실행 중인 컨테이너
docker ps

# 모든 컨테이너 (중지 포함)
docker ps -a

# 컨테이너 중지
docker stop my-nginx
docker stop $(docker ps -q)  # 모든 컨테이너 중지

# 컨테이너 시작
docker start my-nginx

# 컨테이너 재시작
docker restart my-nginx

# 컨테이너 삭제
docker rm my-nginx
docker rm -f my-nginx        # 강제 삭제 (실행 중이어도)
docker rm $(docker ps -aq)   # 모든 컨테이너 삭제

# 컨테이너 일시정지/재개
docker pause my-nginx
docker unpause my-nginx
```

### 실습 7: 컨테이너 접속

```bash
# 실행 중인 컨테이너에서 명령 실행
docker run -d --name web nginx
docker exec web ls /etc/nginx

# 컨테이너 쉘 접속
docker exec -it web /bin/bash
# 또는
docker exec -it web sh

# 컨테이너 내부에서
ls -la
cat /etc/nginx/nginx.conf
exit

# 새 터미널로 프로세스 확인
docker exec web ps aux
```

---

## Part 4: 심화 명령어 (1시간 15분)

### 실습 8: 로그 확인

```bash
docker run -d --name web nginx

# 로그 확인
docker logs web

# 실시간 로그
docker logs -f web

# 최근 N줄
docker logs --tail 10 web

# 타임스탬프 포함
docker logs -t web

# 특정 시간 이후
docker logs --since 10m web
```

### 실습 9: 리소스 모니터링

```bash
# 실시간 리소스 사용량
docker stats

# 특정 컨테이너만
docker stats web

# 한 번만 출력
docker stats --no-stream

# 컨테이너 상세 정보
docker inspect web

# 특정 정보 추출
docker inspect web --format '{{.State.Status}}'
docker inspect web --format '{{.NetworkSettings.IPAddress}}'
docker inspect web --format '{{json .Config.Env}}'
```

### 실습 10: 포트와 볼륨

```bash
# 포트 매핑 확인
docker run -d -p 8080:80 --name web nginx
docker port web

# 볼륨 마운트
docker run -d \
  -v /host/path:/container/path \
  --name web nginx

# 현재 디렉토리 마운트
docker run -d \
  -v $(pwd)/html:/usr/share/nginx/html \
  -p 8080:80 \
  --name web nginx

# 볼륨 생성 및 사용
docker volume create mydata
docker run -d -v mydata:/data --name app nginx
docker volume ls
docker volume inspect mydata
```

### 실습 11: 컨테이너 ↔ 호스트 파일 복사

```bash
docker run -d --name web nginx

# 컨테이너 → 호스트
docker cp web:/etc/nginx/nginx.conf ./nginx.conf

# 호스트 → 컨테이너
echo "<h1>Hello Docker</h1>" > index.html
docker cp index.html web:/usr/share/nginx/html/

# 확인
docker exec web cat /usr/share/nginx/html/index.html
```

---

## 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | 컨테이너 개념 | VM vs 컨테이너 차이 | |
| 2 | Docker 설치 | docker --version 확인 | |
| 3 | 이미지 관리 | pull, images, rmi | |
| 4 | 컨테이너 실행 | run -d -p --name | |
| 5 | 컨테이너 관리 | ps, stop, rm | |
| 6 | 컨테이너 접속 | exec -it | |
| 7 | 로그 확인 | logs -f | |
| 8 | 리소스 모니터링 | stats, inspect | |
| 9 | 볼륨 마운트 | -v 옵션 | |
| 10 | 파일 복사 | docker cp | |

---

## 핵심 명령어 정리

```bash
# 이미지
docker pull <image>
docker images
docker rmi <image>

# 컨테이너
docker run -d -p <host>:<container> --name <name> <image>
docker ps [-a]
docker stop/start/restart <container>
docker rm [-f] <container>

# 접속/로그
docker exec -it <container> sh
docker logs -f <container>

# 모니터링
docker stats
docker inspect <container>

# 정리
docker system prune -a
```

---

## 면접 대비

**Q: 컨테이너와 가상 머신의 차이는?**
> "가상 머신은 하이퍼바이저 위에 전체 OS를 가상화하여 무겁고 부팅이 느립니다. 컨테이너는 호스트 OS의 커널을 공유하며 프로세스를 격리하여 가볍고 빠르게 시작됩니다."

**Q: docker run -d -p 8080:80의 의미는?**
> "-d는 백그라운드 실행, -p 8080:80은 호스트의 8080 포트를 컨테이너의 80 포트로 매핑합니다. 호스트의 8080으로 접속하면 컨테이너의 80으로 전달됩니다."

---

## 정리

```bash
# 모든 컨테이너 삭제
docker rm -f $(docker ps -aq)

# 미사용 리소스 정리
docker system prune -a

# 볼륨까지 정리
docker system prune -a --volumes
```

---

## 다음 학습: Day 9

주제: Dockerfile 작성
- Dockerfile 문법
- 이미지 빌드
- 멀티 스테이지 빌드
