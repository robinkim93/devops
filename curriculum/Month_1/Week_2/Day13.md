# Day 13: Docker 네트워킹 심화

## 오늘의 목표

토스플레이스 연결점: "Network 등 다양한 레이어에서의 트러블슈팅 경험"
"컨테이너 오케스트레이션 서비스 운영"

Docker 네트워크의 동작 원리와 컨테이너 간 통신 방법을 깊이 이해합니다. 네트워크 드라이버, DNS 서비스 디스커버리, 네트워크 트러블슈팅을 실습합니다.

---

## 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 핵심 개념 | 45분 | 네트워크 드라이버, 구조 |
| 기본 실습 | 1시간 | bridge, host, none |
| 심화 실습 | 1.5시간 | 커스텀 네트워크, DNS |
| 트러블슈팅 | 45분 | 네트워크 문제 분석 |

---

## Part 1: 네트워크 핵심 개념 (45분)

### 1.1 Docker 네트워크 드라이버

```
Docker 네트워크 드라이버:

1. bridge (기본)
   - 기본 네트워크 드라이버
   - 컨테이너 간 통신 가능
   - NAT를 통해 외부 통신

2. host
   - 호스트 네트워크 직접 사용
   - 포트 매핑 불필요
   - 성능 좋지만 격리 없음

3. none
   - 네트워크 없음
   - 완전히 격리된 컨테이너

4. overlay
   - 멀티 호스트 네트워크
   - Docker Swarm, Kubernetes에서 사용

5. macvlan
   - 물리 네트워크에 직접 연결
   - 고유 MAC 주소 할당
```

### 1.2 Bridge 네트워크 구조

```
Bridge 네트워크 동작:

Host
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│    ┌──────────────┐    ┌──────────────┐                     │
│    │ Container A  │    │ Container B  │                     │
│    │   eth0       │    │   eth0       │                     │
│    │ 172.17.0.2   │    │ 172.17.0.3   │                     │
│    └──────┬───────┘    └──────┬───────┘                     │
│           │                   │                              │
│           └───────────┬───────┘                              │
│                       │                                      │
│              ┌────────┴────────┐                             │
│              │   docker0       │  172.17.0.1                │
│              │  (Bridge)       │                             │
│              └────────┬────────┘                             │
│                       │ NAT                                  │
│              ┌────────┴────────┐                             │
│              │   eth0 (Host)   │  192.168.1.100             │
│              └─────────────────┘                             │
│                       │                                      │
└───────────────────────│──────────────────────────────────────┘
                        │
                  [ 외부 네트워크 ]
```

### 1.3 기본 네트워크 확인

```bash
# 네트워크 목록
docker network ls

# 출력 예시:
# NETWORK ID     NAME      DRIVER    SCOPE
# abc123         bridge    bridge    local
# def456         host      host      local
# ghi789         none      null      local

# bridge 네트워크 상세
docker network inspect bridge

# docker0 인터페이스 확인
ip addr show docker0
```

---

## Part 2: 기본 실습 (1시간)

### 실습 1: Bridge 네트워크

```bash
# 기본 bridge 네트워크로 컨테이너 실행
docker run -d --name web1 nginx
docker run -d --name web2 nginx

# IP 주소 확인
docker inspect web1 --format '{{.NetworkSettings.IPAddress}}'
docker inspect web2 --format '{{.NetworkSettings.IPAddress}}'

# web1에서 web2로 통신 (IP로)
docker exec web1 ping -c 3 $(docker inspect web2 --format '{{.NetworkSettings.IPAddress}}')

# 이름으로 통신 시도 (기본 bridge에서는 실패!)
docker exec web1 ping -c 3 web2
# 에러: Name or service not known

# 정리
docker rm -f web1 web2
```

### 실습 2: Host 네트워크

```bash
# host 네트워크로 nginx 실행
docker run -d --name web-host --network host nginx

# 포트 매핑 없이 80번 포트 사용
curl http://localhost:80

# 호스트 네트워크 인터페이스 사용
docker exec web-host ip addr

# 호스트의 네트워크와 동일한지 확인
ip addr

# 정리
docker rm -f web-host
```

### 실습 3: None 네트워크

```bash
# 네트워크 없는 컨테이너
docker run -d --name isolated --network none alpine sleep 3600

# 네트워크 인터페이스 확인 (lo만 있음)
docker exec isolated ip addr

# 출력:
# 1: lo: <LOOPBACK,UP,LOWER_UP>
#     inet 127.0.0.1/8 scope host lo

# 외부 통신 불가
docker exec isolated ping -c 1 8.8.8.8
# 에러: Network is unreachable

# 정리
docker rm -f isolated
```

---

## Part 3: 커스텀 네트워크와 DNS (1.5시간)

### 실습 4: 커스텀 Bridge 네트워크

```bash
# 커스텀 네트워크 생성
docker network create mynet

# 네트워크 확인
docker network ls
docker network inspect mynet

# 컨테이너 실행
docker run -d --name app1 --network mynet nginx
docker run -d --name app2 --network mynet nginx

# DNS로 통신 (커스텀 네트워크에서는 가능!)
docker exec app1 ping -c 3 app2
# 성공!

# DNS 해석 확인
docker exec app1 nslookup app2

# 출력 예시:
# Server:    127.0.0.11
# Address 1: 127.0.0.11
# Name:      app2
# Address 1: 172.18.0.3 app2.mynet

# 정리
docker rm -f app1 app2
docker network rm mynet
```

### 실습 5: 멀티 네트워크 연결

```bash
# 두 개의 네트워크 생성
docker network create frontend
docker network create backend

# 컨테이너 생성
docker run -d --name web --network frontend nginx
docker run -d --name api --network backend nginx
docker run -d --name db --network backend redis

# api를 frontend에도 연결 (다중 네트워크)
docker network connect frontend api

# api의 네트워크 확인
docker inspect api --format '{{json .NetworkSettings.Networks}}' | jq

# 통신 테스트
# web -> api (frontend 통해)
docker exec web ping -c 2 api
# 성공!

# web -> db (직접 연결 없음)
docker exec web ping -c 2 db
# 실패!

# api -> db (backend 통해)
docker exec api ping -c 2 db
# 성공!

# 정리
docker rm -f web api db
docker network rm frontend backend
```

### 실습 6: 네트워크 별칭 (Alias)

```bash
docker network create mynet

# 별칭으로 컨테이너 실행
docker run -d --name redis-primary --network mynet --network-alias redis redis

# 같은 별칭으로 추가 컨테이너
docker run -d --name redis-replica --network mynet --network-alias redis redis

# 별칭으로 조회 (라운드 로빈)
docker run --rm --network mynet alpine nslookup redis

# 여러 IP가 반환됨
# Address 1: 172.18.0.2 redis-primary.mynet
# Address 2: 172.18.0.3 redis-replica.mynet

# 정리
docker rm -f redis-primary redis-replica
docker network rm mynet
```

### 실습 7: Docker Compose 네트워크

```bash
mkdir -p ~/net-demo && cd ~/net-demo

cat << 'EOF' > docker-compose.yml
services:
  web:
    image: nginx
    networks:
      - frontend
    ports:
      - "8080:80"

  api:
    image: nginx
    networks:
      - frontend
      - backend

  db:
    image: redis
    networks:
      - backend

networks:
  frontend:
  backend:
EOF

docker-compose up -d

# 네트워크 확인
docker network ls | grep net-demo

# 통신 테스트
docker-compose exec web ping -c 2 api
docker-compose exec api ping -c 2 db
docker-compose exec web ping -c 2 db  # 실패!

docker-compose down
cd ~
rm -rf ~/net-demo
```

---

## Part 4: 네트워크 트러블슈팅 (45분)

### 실습 8: 네트워크 진단 도구

```bash
docker network create debug-net

# 디버그용 컨테이너
docker run -d --name target --network debug-net nginx

# nicolaka/netshoot: 네트워크 트러블슈팅 도구 모음
docker run -it --rm --network debug-net nicolaka/netshoot

# 컨테이너 내부에서:
# DNS 확인
nslookup target
dig target

# 연결 테스트
nc -zv target 80

# 포트 스캔
nmap -p 80,443 target

# HTTP 요청
curl -v http://target

# 네트워크 경로
traceroute target

# exit
exit

docker rm -f target
docker network rm debug-net
```

### 실습 9: 일반적인 네트워크 문제

```bash
# 문제 1: 컨테이너 간 통신 안됨

# 진단
docker network ls
docker inspect <container> --format '{{.NetworkSettings.Networks}}'
# -> 같은 네트워크인지 확인

# 문제 2: 외부 접속 안됨

# 진단
docker port <container>
# -> 포트 매핑 확인

docker exec <container> curl -I http://google.com
# -> 컨테이너에서 외부 접속 가능한지

# 문제 3: DNS 해석 안됨

# 진단
docker exec <container> cat /etc/resolv.conf
docker exec <container> nslookup <service_name>
# -> 커스텀 네트워크 사용 여부 확인
```

### 실습 10: iptables 확인

```bash
# Docker NAT 규칙 확인
sudo iptables -t nat -L -n | grep docker

# Docker FORWARD 규칙
sudo iptables -L DOCKER -n

# 특정 포트 매핑 규칙
sudo iptables -t nat -L DOCKER -n --line-numbers
```

---

## 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | 네트워크 드라이버 이해 | bridge, host, none | |
| 2 | 기본 bridge 한계 | DNS 미지원 | |
| 3 | 커스텀 네트워크 | DNS 자동 지원 | |
| 4 | 멀티 네트워크 | network connect | |
| 5 | 네트워크 별칭 | --network-alias | |
| 6 | Compose 네트워크 | 자동 생성, 분리 | |
| 7 | 트러블슈팅 | netshoot, 진단 방법 | |

---

## 핵심 명령어

```bash
# 네트워크 관리
docker network create <name>
docker network ls
docker network inspect <name>
docker network rm <name>
docker network connect <network> <container>
docker network disconnect <network> <container>

# 컨테이너 네트워크 지정
docker run --network <name> <image>
docker run --network-alias <alias> <image>

# 진단
docker exec <container> ping <target>
docker exec <container> nslookup <target>
```

---

## 면접 대비

**Q: 기본 bridge와 커스텀 bridge의 차이는?**
> "기본 bridge는 컨테이너 이름으로 DNS 해석이 안 되어 IP로만 통신해야 합니다. 커스텀 bridge는 내장 DNS 서버를 제공하여 컨테이너 이름으로 통신 가능합니다."

**Q: 컨테이너 간 통신이 안 될 때 어떻게 진단하나요?**
> "먼저 docker network inspect로 같은 네트워크에 있는지 확인합니다. 그 다음 netshoot 같은 도구로 ping, nslookup, nc -zv로 DNS와 포트 연결을 테스트합니다."

---

## 다음 학습: Day 14

주제: Docker 트러블슈팅 종합
- 컨테이너 로그 분석
- 리소스 문제 진단
- 일반적인 문제 해결