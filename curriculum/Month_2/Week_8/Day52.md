# Day 52: Podman 기초

## 오늘의 목표

토스플레이스 연결점: "Podman, Containerd 등 컨테이너 런타임 경험"
"Kubernetes 클러스터를 운영/최적화"

Docker 대안인 Podman 사용법을 익힙니다. Podman은 데몬 없이 동작하며 rootless 컨테이너를 지원하여 보안이 강화된 컨테이너 런타임입니다.

---

## 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 핵심 개념 | 45분 | Podman vs Docker, 아키텍처 |
| 기본 실습 | 1시간 | 이미지, 컨테이너 관리 |
| 고급 실습 | 1.5시간 | Rootless, Pod, K8s YAML |
| 빌드 실습 | 45분 | 이미지 빌드, 레지스트리 |

---

## Part 1: Podman이란? (45분)

### 1.1 Docker vs Podman

```
Docker vs Podman 비교:

+----------------+---------------------------+---------------------------+
|     항목       |          Docker           |          Podman           |
+----------------+---------------------------+---------------------------+
| 데몬           | dockerd 필요 (항상 실행)  | 없음 (daemonless)         |
| 권한           | root 필요 (기본)          | rootless 지원             |
| CLI            | docker 명령어             | docker 호환 (alias 가능)  |
| Pod 지원       | 없음 (Compose만)          | 네이티브 Pod 지원         |
| 이미지 포맷    | OCI 표준                  | OCI 표준                  |
| K8s 연동       | 별도 변환 필요            | 직접 YAML 생성 가능       |
| 보안           | 중앙 데몬 = SPOF          | 프로세스별 격리           |
+----------------+---------------------------+---------------------------+
```

### 1.2 왜 Podman을 사용하나?

```
Podman 장점:

1. 보안 강화
   - rootless 컨테이너: 일반 사용자로 실행
   - root 권한 없이도 컨테이너 실행 가능
   - 컨테이너가 호스트 root 권한 획득 불가

2. 데몬 없음 (Daemonless)
   - 단일 장애점(SPOF) 없음
   - 시스템 리소스 절약
   - 각 컨테이너가 독립 프로세스

3. Kubernetes 친화적
   - Pod 개념 네이티브 지원
   - K8s YAML 직접 생성/실행
   - CRI-O와 동일한 기반

4. Docker 호환
   - 대부분의 docker 명령어 그대로 사용
   - alias docker=podman 가능
   - 학습 곡선 최소화
```

### 1.3 Podman 아키텍처

```
Docker 아키텍처:
+----------+     +----------+     +-----------+
|  Client  | --> | dockerd  | --> | Container |
| (docker) |     | (daemon) |     |           |
+----------+     +----------+     +-----------+
                    SPOF!

Podman 아키텍처:
+----------+     +-----------+
|  Client  | --> | Container |  (직접 실행, 데몬 없음)
| (podman) |     |           |
+----------+     +-----------+

Podman은 각 컨테이너를 별도 프로세스로 직접 실행
- podman run -> conmon -> runc -> container
```

### 1.4 Podman 구성 요소

| 컴포넌트 | 역할 |
|---------|------|
| **podman** | CLI 도구 (docker 호환) |
| **conmon** | Container Monitor, 컨테이너 감시 |
| **runc** | OCI 컨테이너 런타임 |
| **crun** | runc 대안 (더 빠름) |
| **buildah** | 이미지 빌드 도구 |
| **skopeo** | 이미지 복사/검사 도구 |

---

## Part 2: 기본 실습 (1시간)

### 실습 1: Podman 설치

**Ubuntu/Debian:**
```bash
# 저장소 추가 및 설치
sudo apt update
sudo apt install -y podman

# 버전 확인
podman --version

# podman info
podman info
```

**macOS:**
```bash
# Homebrew로 설치
brew install podman

# podman machine 초기화 (macOS는 VM 필요)
podman machine init
podman machine start

# 상태 확인
podman machine list
```

**Docker alias 설정 (선택):**
```bash
# .bashrc 또는 .zshrc에 추가
echo "alias docker=podman" >> ~/.bashrc
source ~/.bashrc

# 이제 docker 명령어가 podman으로 실행됨
docker ps  # = podman ps
```

### 실습 2: 기본 명령어 (Docker와 동일)

```bash
# 이미지 다운로드
podman pull nginx:latest
podman pull redis:alpine

# 이미지 목록
podman images

# 컨테이너 실행
podman run -d --name web -p 8080:80 nginx

# 컨테이너 목록
podman ps
podman ps -a

# 로그 확인
podman logs web
podman logs -f web  # follow

# 컨테이너 접속
podman exec -it web /bin/bash

# 컨테이너 상태
podman stats web

# 컨테이너 중지/시작
podman stop web
podman start web

# 컨테이너 삭제
podman rm web
podman rm -f web  # 강제

# 이미지 삭제
podman rmi nginx
podman rmi -f nginx
```

### 실습 3: 볼륨과 네트워크

```bash
# 볼륨 생성
podman volume create mydata

# 볼륨 목록
podman volume ls

# 볼륨 마운트
podman run -d --name db -v mydata:/var/lib/mysql mysql:8

# 볼륨 삭제
podman volume rm mydata

# 네트워크 생성
podman network create mynet

# 네트워크 목록
podman network ls

# 네트워크에 컨테이너 연결
podman run -d --name web --network mynet nginx
podman run -d --name redis --network mynet redis

# 네트워크 검사
podman network inspect mynet
```

---

## Part 3: 고급 실습 (1.5시간)

### 실습 4: Rootless 컨테이너 (30분)

Rootless 컨테이너는 Podman의 핵심 기능입니다.

```bash
# 일반 사용자로 컨테이너 실행 (sudo 없이!)
podman run -d --name rootless-nginx -p 8081:80 nginx

# 프로세스 확인 - root가 아님!
podman top rootless-nginx

# 출력 예시:
# USER    PID   PPID  ...
# 100     1     0     nginx: master process
# 101     7     1     nginx: worker process

# 호스트에서 프로세스 확인
ps aux | grep nginx
# 일반 사용자 권한으로 실행됨!

# 컨테이너 내부에서 확인
podman exec rootless-nginx id
# uid=0(root) gid=0(root) groups=0(root)
# 컨테이너 내부는 root처럼 보이지만, 호스트에서는 일반 사용자!
```

**Rootless 장점:**
```
1. 보안 강화
   - 컨테이너가 탈출해도 호스트 root 권한 없음
   - User Namespace 격리

2. 다중 사용자 환경
   - 각 사용자가 독립적으로 컨테이너 실행
   - 서로 간섭 없음

3. 권한 없는 환경
   - sudo 권한 없이 컨테이너 실행
   - CI/CD 파이프라인에서 유용
```

### 실습 5: Podman Pod (K8s Pod 개념) (40분)

Podman은 Kubernetes의 Pod 개념을 네이티브로 지원합니다.

```bash
# Pod 생성 (infra 컨테이너 포함)
podman pod create --name mypod -p 8080:80

# Pod 목록
podman pod ps

# 출력 예시:
# POD ID        NAME    STATUS   CREATED   INFRA ID      # OF CONTAINERS
# abc123        mypod   Running  1m ago    def456        1

# Pod에 컨테이너 추가
podman run -d --pod mypod --name web nginx
podman run -d --pod mypod --name redis redis

# Pod 내 컨테이너 확인
podman ps --pod

# 출력 예시:
# CONTAINER ID  IMAGE    COMMAND     PORTS       POD ID   PODNAME
# xxx           nginx    ...         0.0.0.0:8080->80/tcp  abc123  mypod
# yyy           redis    ...                     abc123  mypod
# zzz           pause    ...                     abc123  mypod (infra)

# 같은 Pod 내 컨테이너는 localhost로 통신 가능!
podman exec web curl localhost:6379
# Redis에 접속 가능 (같은 네트워크 네임스페이스)

# Pod 중지/시작
podman pod stop mypod
podman pod start mypod

# Pod 삭제 (내부 컨테이너도 함께 삭제)
podman pod rm mypod
podman pod rm -f mypod  # 강제
```

**Pod 사용 시나리오:**
```
1. Sidecar 패턴
   - Main App + Log Collector
   - Main App + Monitoring Agent

2. Ambassador 패턴
   - Main App + Proxy
   - localhost 통신으로 간단한 연결

3. K8s 마이그레이션 준비
   - 로컬에서 Pod 구조 테스트
   - K8s 배포 전 검증
```

### 실습 6: K8s YAML 생성 (20분)

Podman은 실행 중인 컨테이너/Pod에서 Kubernetes YAML을 생성할 수 있습니다.

```bash
# 컨테이너 실행
podman run -d --name myapp -p 8080:80 nginx

# K8s YAML 생성
podman generate kube myapp > myapp.yaml

# 내용 확인
cat myapp.yaml
```

**생성된 YAML 예시:**
```yaml
# myapp.yaml
apiVersion: v1
kind: Pod
metadata:
  labels:
    app: myapp
  name: myapp
spec:
  containers:
  - image: docker.io/library/nginx:latest
    name: myapp
    ports:
    - containerPort: 80
      hostPort: 8080
```

```bash
# Podman에서 YAML로 실행 (play kube)
podman play kube myapp.yaml

# Pod로 생성됨
podman pod ps
podman ps --pod

# 삭제
podman play kube --down myapp.yaml

# 정리
podman rm -f myapp
```

---

## Part 4: 이미지 빌드 실습 (45분)

### 실습 7: 이미지 빌드

Podman은 Dockerfile(Containerfile)을 사용하여 이미지를 빌드합니다.

```bash
# 작업 디렉토리
mkdir -p ~/podman-build && cd ~/podman-build

# Containerfile (= Dockerfile)
cat << 'EOF' > Containerfile
FROM nginx:alpine

# 메타데이터
LABEL maintainer="devops@example.com"
LABEL version="1.0"

# 커스텀 HTML
COPY index.html /usr/share/nginx/html/

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -q --spider http://localhost/ || exit 1

EXPOSE 80
EOF

# index.html 생성
cat << 'EOF' > index.html
<!DOCTYPE html>
<html>
<head><title>Podman Build</title></head>
<body>
<h1>Built with Podman!</h1>
<p>Container Runtime: Podman</p>
</body>
</html>
EOF

# 이미지 빌드
podman build -t myapp:v1 .

# 이미지 확인
podman images | grep myapp

# 실행 및 테스트
podman run -d --name test-app -p 8080:80 myapp:v1
curl http://localhost:8080

# 정리
podman rm -f test-app
```

### 실습 8: Buildah 사용 (고급)

Buildah는 Podman의 이미지 빌드 도구입니다.

```bash
# Buildah 설치 (이미 설치되어 있을 수 있음)
sudo apt install -y buildah

# 스크래치 이미지에서 빌드 (Dockerfile 없이)
container=$(buildah from scratch)

# 파일 추가
buildah copy $container index.html /
buildah config --cmd "/bin/cat /index.html" $container

# 이미지 커밋
buildah commit $container myimage:v1

# 확인
podman images | grep myimage
```

### 실습 9: 레지스트리 연동

```bash
# Docker Hub 로그인
podman login docker.io

# 이미지 태그
podman tag myapp:v1 docker.io/username/myapp:v1

# 이미지 푸시
podman push docker.io/username/myapp:v1

# 이미지 풀
podman pull docker.io/username/myapp:v1

# 로그아웃
podman logout docker.io
```

**Skopeo로 이미지 복사:**
```bash
# 레지스트리 간 복사 (다운로드 없이)
skopeo copy docker://nginx:latest docker://myregistry/nginx:latest

# 이미지 정보 확인
skopeo inspect docker://nginx:latest
```

---

## 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | Podman vs Docker 차이 | 데몬, 보안 | |
| 2 | Podman 설치 | apt/brew | |
| 3 | 기본 명령어 | run, ps, logs, exec | |
| 4 | Rootless 컨테이너 | sudo 없이 실행 | |
| 5 | Podman Pod | pod create, run --pod | |
| 6 | K8s YAML 생성 | generate kube | |
| 7 | 이미지 빌드 | build, Containerfile | |
| 8 | 레지스트리 연동 | login, push, pull | |

---

## 핵심: Docker -> Podman 매핑

```bash
# 대부분의 명령어가 동일!
docker run     -> podman run
docker ps      -> podman ps
docker build   -> podman build
docker images  -> podman images
docker logs    -> podman logs
docker exec    -> podman exec

# Podman 전용 기능
podman pod create        # Pod 생성
podman pod ps            # Pod 목록
podman generate kube     # K8s YAML 생성
podman play kube         # YAML로 Pod 실행
podman system migrate    # 업그레이드 후 마이그레이션
```

---

## 면접 대비 핵심 포인트

**Q1: Podman과 Docker의 가장 큰 차이점은?**
> "Podman은 데몬이 없어서 단일 장애점이 없고, rootless 컨테이너를 지원해서 보안이 강화됩니다. 또한 Kubernetes의 Pod 개념을 직접 지원하여 K8s와 유사한 환경을 제공합니다."

**Q2: Rootless 컨테이너란?**
> "일반 사용자 권한으로 컨테이너를 실행하는 것입니다. User Namespace를 사용해서 컨테이너 내부는 root처럼 보이지만 호스트에서는 일반 사용자로 실행됩니다. 컨테이너 탈출 공격에도 호스트 root 권한을 얻을 수 없어 보안이 강화됩니다."

**Q3: Podman Pod는 언제 사용하나요?**
> "Kubernetes 배포 전 로컬에서 테스트하거나, Sidecar 패턴처럼 여러 컨테이너가 네트워크를 공유해야 할 때 사용합니다. podman generate kube로 K8s YAML을 자동 생성할 수 있어 마이그레이션이 쉽습니다."

---

## 학습 기록

```
학습일: ____년 __월 __일
실제 소요 시간: ____시간

완료한 실습:
- [ ] Podman 설치
- [ ] 기본 명령어 (run, ps, exec)
- [ ] Rootless 컨테이너
- [ ] Podman Pod 생성
- [ ] K8s YAML 생성
- [ ] 이미지 빌드

이해가 어려웠던 부분:

추가 학습 필요 항목:
```

---

## 정리

```bash
# 모든 컨테이너 삭제
podman rm -af

# 모든 Pod 삭제
podman pod rm -af

# 사용하지 않는 리소스 정리
podman system prune -a

# 빌드 디렉토리 삭제
rm -rf ~/podman-build
```

---

## 다음 학습: Day 53

주제: Containerd 기초
- Containerd 아키텍처
- ctr, crictl 명령어
- Kubernetes CRI 연동
