# 📅 Day 18: DNS 기초와 트러블슈팅

## 🎯 오늘의 목표

> **토스플레이스 연결점**: "OS, Network 등 다양한 레이어에서의 모니터링, 트러블슈팅 경험"
> DNS는 모든 네트워크 통신의 기반이며, Kubernetes Service Discovery의 핵심

DNS(Domain Name System) 동작 원리를 이해하고 문제를 진단/해결할 수 있는 능력을 배양합니다. 토스플레이스에서 Kubernetes 환경의 서비스 디스커버리와 네트워크 문제 해결에 필수적인 지식입니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| DNS 개념 | 45분 | 동작 원리, 레코드 타입 |
| dig/nslookup 실습 | 1시간 | DNS 쿼리 도구 |
| DNS 설정 | 45분 | resolv.conf, hosts |
| 트러블슈팅 | 1.5시간 | 문제 진단 및 해결 |

---

## 📚 Part 1: DNS 핵심 개념 (45분)

### 1.1 DNS란?

```
┌─────────────────────────────────────────────────────────────────────┐
│  DNS (Domain Name System) - 인터넷의 전화번호부                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  문제: IP 주소는 기억하기 어려움                                    │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  142.250.196.110 보다 google.com이 기억하기 쉬움            │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  해결: 도메인 이름 → IP 주소 변환 시스템                            │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  google.com ──→ DNS Server ──→ 142.250.196.110             │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  토스플레이스 관련성:                                               │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • Kubernetes CoreDNS: Service 이름 → ClusterIP 변환        │    │
│  │  • Service Discovery: my-service.namespace.svc.cluster.local│    │
│  │  • 외부 서비스 연결: 외부 API, DB 엔드포인트 해석           │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 DNS 조회 과정

```
┌─────────────────────────────────────────────────────────────────────┐
│  DNS 조회 과정 (www.example.com)                                    │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Client              Local DNS          Root DNS       TLD DNS       │
│    │                 (Resolver)         (.)            (.com)        │
│    │                     │               │               │           │
│    │  1. www.example.com │               │               │           │
│    │ ────────────────────>               │               │           │
│    │                     │               │               │           │
│    │                     │  2. .com NS?  │               │           │
│    │                     │ ──────────────>               │           │
│    │                     │               │               │           │
│    │                     │  3. TLD DNS IP│               │           │
│    │                     │ <──────────────               │           │
│    │                     │               │               │           │
│    │                     │  4. example.com NS?           │           │
│    │                     │ ──────────────────────────────>           │
│    │                     │               │               │           │
│    │                     │  5. Auth DNS IP               │           │
│    │                     │ <──────────────────────────────           │
│    │                     │               │               │           │
│    │                     │  6. www.example.com A record? │           │
│    │                     │ ────────────────────────────────────>     │
│    │                     │                               Auth DNS    │
│    │                     │  7. 93.184.216.34                         │
│    │                     │ <────────────────────────────────────     │
│    │                     │               │               │           │
│    │  8. 93.184.216.34   │               │               │           │
│    │ <────────────────────               │               │           │
│                                                                      │
│  계층: Root(.) → TLD(.com) → Authoritative(example.com) → 최종 응답 │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.3 DNS 레코드 타입

| 타입 | 설명 | 예시 | 용도 |
|------|------|------|------|
| **A** | IPv4 주소 | example.com → 93.184.216.34 | 기본 주소 매핑 |
| **AAAA** | IPv6 주소 | example.com → 2606:2800:... | IPv6 지원 |
| **CNAME** | 별칭 (Alias) | www.example.com → example.com | 도메인 별칭 |
| **MX** | 메일 서버 | example.com → mail.example.com (우선순위 10) | 이메일 라우팅 |
| **NS** | 네임서버 | example.com → ns1.example.com | 도메인 관리 서버 |
| **TXT** | 텍스트 정보 | example.com → "v=spf1 ..." | SPF, DKIM, 인증 |
| **SOA** | Start of Authority | Zone 관리 정보 | DNS Zone 메타데이터 |
| **PTR** | 역방향 조회 | 93.184.216.34 → example.com | IP → 도메인 |
| **SRV** | 서비스 레코드 | _http._tcp.example.com | 서비스 위치 (K8s 사용) |

```bash
# 각 레코드 타입 조회 예시
dig google.com A        # IPv4 주소
dig google.com AAAA     # IPv6 주소
dig google.com MX       # 메일 서버
dig google.com NS       # 네임서버
dig google.com TXT      # 텍스트 레코드
dig google.com SOA      # SOA 레코드
```

### 1.4 DNS 캐싱과 TTL

```
┌─────────────────────────────────────────────────────────────────────┐
│  DNS 캐싱 (TTL - Time To Live)                                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  TTL: DNS 응답을 캐시에 저장하는 시간 (초)                          │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  google.com.    300    IN    A    142.250.196.110          │    │
│  │                  ↑                                          │    │
│  │               TTL 300초 (5분)                               │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  캐싱 계층:                                                          │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  1. 브라우저 캐시 (Chrome: chrome://net-internals/#dns)     │    │
│  │  2. OS 캐시 (systemd-resolved, nscd)                       │    │
│  │  3. 로컬 DNS 서버 캐시 (공유기, ISP)                       │    │
│  │  4. 상위 DNS 서버 캐시                                      │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  TTL 설정 전략:                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  • 짧은 TTL (60-300초): 빈번한 변경, DR 환경               │    │
│  │  • 긴 TTL (3600-86400초): 안정적인 서비스, 부하 감소       │    │
│  │  • 마이그레이션: 사전에 TTL 낮춤 → 전환 → TTL 복구         │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Part 2: dig 명령어 마스터 (1시간)

### 2.1 dig 기본 사용법

```bash
mkdir -p ~/network-practice/day18
cd ~/network-practice/day18

# dig 설치 확인
which dig || sudo apt install dnsutils -y

# 기본 쿼리
echo "=== 기본 DNS 쿼리 ==="
dig google.com

# 출력 설명:
# ; <<>> DiG 9.x.x <<>> google.com
# ;; QUESTION SECTION:      ← 질문
# ;google.com.                    IN      A
# 
# ;; ANSWER SECTION:         ← 응답
# google.com.             300     IN      A       142.250.196.110
#                          ↑       ↑       ↑       ↑
#                         TTL   Class   Type    Value
#
# ;; Query time: 10 msec    ← 쿼리 시간
# ;; SERVER: 192.168.1.1    ← 사용된 DNS 서버
```

### 2.2 dig 고급 옵션

```bash
# 간결한 출력 (+short)
echo "=== 간결한 출력 ==="
dig +short google.com
# 출력: 142.250.196.110

# 상세 출력 (+noall +answer)
echo "=== Answer Section만 ==="
dig +noall +answer google.com

# 특정 레코드 타입 조회
echo "=== A 레코드 ==="
dig google.com A +short

echo "=== MX 레코드 ==="
dig google.com MX +short
# 출력: 10 smtp.google.com.

echo "=== NS 레코드 ==="
dig google.com NS +short

echo "=== TXT 레코드 ==="
dig google.com TXT +short

# 모든 레코드 타입 (ANY) - 일부 서버에서 제한됨
dig google.com ANY +short
```

### 2.3 특정 DNS 서버 지정

```bash
# @ 기호로 DNS 서버 지정
echo "=== 특정 DNS 서버 사용 ==="

# Google Public DNS
dig @8.8.8.8 google.com +short
dig @8.8.4.4 google.com +short

# Cloudflare DNS
dig @1.1.1.1 google.com +short

# 로컬 DNS 서버 (예: 공유기)
dig @192.168.1.1 google.com +short

# 응답 비교
echo "=== DNS 서버별 응답 비교 ==="
echo "Google DNS:    $(dig @8.8.8.8 google.com +short | head -1)"
echo "Cloudflare:    $(dig @1.1.1.1 google.com +short | head -1)"
echo "System DNS:    $(dig google.com +short | head -1)"
```

### 2.4 DNS 경로 추적 (+trace)

```bash
# DNS 조회 경로 전체 추적
echo "=== DNS 경로 추적 ==="
dig +trace google.com

# 출력 예시:
# .                        518400  IN      NS      a.root-servers.net.
# .                        518400  IN      NS      b.root-servers.net.
# ...
# com.                     172800  IN      NS      a.gtld-servers.net.
# com.                     172800  IN      NS      b.gtld-servers.net.
# ...
# google.com.              172800  IN      NS      ns1.google.com.
# google.com.              172800  IN      NS      ns2.google.com.
# ...
# google.com.              300     IN      A       142.250.196.110

# Root → TLD(.com) → Authoritative(google.com) → 최종 응답
```

### 2.5 역방향 DNS 조회

```bash
# IP → 도메인 (PTR 레코드)
echo "=== 역방향 DNS 조회 ==="
dig -x 8.8.8.8 +short
# 출력: dns.google.

dig -x 1.1.1.1 +short
# 출력: one.one.one.one.

# 역방향 조회 상세
dig -x 142.250.196.110
```

### 2.6 응답 시간 측정

```bash
# DNS 쿼리 시간 측정
echo "=== DNS 응답 시간 측정 ==="

# 단일 측정
dig google.com | grep "Query time"

# 여러 DNS 서버 비교
echo "=== DNS 서버별 응답 시간 ==="
for dns in 8.8.8.8 1.1.1.1 168.126.63.1; do
    time=$(dig @$dns google.com | grep "Query time" | awk '{print $4}')
    echo "$dns: ${time}ms"
done

# 여러 번 측정 평균
echo "=== 평균 응답 시간 (5회) ==="
total=0
for i in {1..5}; do
    time=$(dig @8.8.8.8 google.com | grep "Query time" | awk '{print $4}')
    total=$((total + time))
done
echo "평균: $((total / 5))ms"
```

---

## 🛠️ Part 3: nslookup 명령어 (30분)

### 3.1 nslookup 기본 사용법

```bash
# 기본 쿼리
echo "=== nslookup 기본 ==="
nslookup google.com

# 출력:
# Server:         192.168.1.1     ← 사용된 DNS 서버
# Address:        192.168.1.1#53  ← 포트 53
#
# Non-authoritative answer:       ← 캐시된 응답
# Name:   google.com
# Address: 142.250.196.110

# 특정 DNS 서버 지정
echo "=== 특정 DNS 서버 ==="
nslookup google.com 8.8.8.8
```

### 3.2 레코드 타입 지정

```bash
# 특정 레코드 타입 조회
echo "=== 레코드 타입별 조회 ==="

# MX 레코드
nslookup -type=MX google.com

# NS 레코드
nslookup -type=NS google.com

# TXT 레코드
nslookup -type=TXT google.com

# SOA 레코드
nslookup -type=SOA google.com

# CNAME 레코드
nslookup -type=CNAME www.github.com
```

### 3.3 dig vs nslookup 비교

| 기능 | dig | nslookup |
|------|-----|----------|
| 출력 상세도 | 매우 상세 | 간결 |
| 스크립팅 | 적합 (+short) | 부적합 |
| 역방향 조회 | dig -x | nslookup IP |
| 경로 추적 | +trace | 미지원 |
| 플랫폼 | Linux 중심 | Windows/Linux |
| 권장 사용 | 트러블슈팅 | 간단한 확인 |

---

## 🛠️ Part 4: DNS 설정 파일 (45분)

### 4.1 /etc/resolv.conf

```bash
# DNS 서버 설정 확인
echo "=== /etc/resolv.conf ==="
cat /etc/resolv.conf

# 예시 출력:
# nameserver 192.168.1.1      ← 첫 번째 DNS 서버
# nameserver 8.8.8.8          ← 두 번째 DNS 서버 (폴백)
# search example.com dev.example.com  ← 검색 도메인
# options timeout:2 attempts:3        ← 옵션

# 주요 설정 설명:
# nameserver: DNS 서버 IP (최대 3개)
# search: 짧은 호스트명에 자동 추가될 도메인
#         예: "myhost" → "myhost.example.com" 먼저 시도
# domain: 단일 검색 도메인
# options: 타임아웃, 재시도 횟수 등
```

### 4.2 systemd-resolved 관리

```bash
# systemd-resolved 상태 확인 (Ubuntu 18.04+)
echo "=== systemd-resolved 상태 ==="
resolvectl status

# 또는 이전 명령어
systemd-resolve --status

# DNS 통계
resolvectl statistics

# DNS 캐시 초기화
echo "=== DNS 캐시 초기화 ==="
sudo resolvectl flush-caches

# 캐시 상태 확인
resolvectl statistics | grep -i cache
```

### 4.3 /etc/hosts

```bash
# 로컬 DNS 오버라이드
echo "=== /etc/hosts ==="
cat /etc/hosts

# 기본 내용:
# 127.0.0.1       localhost
# ::1             localhost ip6-localhost ip6-loopback

# 테스트용 엔트리 추가
echo "=== 로컬 호스트 추가 테스트 ==="
echo "127.0.0.1 mytest.local" | sudo tee -a /etc/hosts

# 확인
ping -c 1 mytest.local

# 정리 (제거)
sudo sed -i '/mytest.local/d' /etc/hosts

# 실제 활용 예시:
# 127.0.0.1       api.local        # 로컬 개발 API
# 192.168.1.100   db.local         # 로컬 DB 서버
# 10.0.0.50       staging.app.com  # 스테이징 환경 테스트
```

### 4.4 DNS 조회 순서

```bash
# /etc/nsswitch.conf - DNS 조회 순서 설정
echo "=== nsswitch.conf ==="
grep hosts /etc/nsswitch.conf

# 출력 예시:
# hosts: files dns mymachines
#         ↑     ↑      ↑
#    /etc/hosts → DNS → systemd-machined

# 순서:
# 1. files (/etc/hosts) 먼저 확인
# 2. dns (resolv.conf의 nameserver) 조회
# 3. 기타 (mDNS 등)
```

---

## 🛠️ Part 5: DNS 트러블슈팅 (1.5시간)

### 5.1 DNS 문제 진단 플로우

```
┌─────────────────────────────────────────────────────────────────────┐
│  DNS 트러블슈팅 플로우차트                                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  시작: "도메인 접속이 안 돼요"                                      │
│         │                                                            │
│         ▼                                                            │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Step 1: ping <domain>                                      │    │
│  │  ├─→ "Name or service not known" → DNS 문제                │    │
│  │  └─→ "No route to host" / Timeout → 네트워크 문제          │    │
│  └─────────────────────────────────────────────────────────────┘    │
│         │                                                            │
│         ▼ (DNS 문제 확인)                                           │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Step 2: dig <domain>                                       │    │
│  │  ├─→ 응답 있음 (ANSWER) → Step 4로                         │    │
│  │  └─→ 응답 없음 (NXDOMAIN/SERVFAIL) → Step 3으로           │    │
│  └─────────────────────────────────────────────────────────────┘    │
│         │                                                            │
│         ▼ (로컬 DNS 문제 확인)                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Step 3: dig @8.8.8.8 <domain>                              │    │
│  │  ├─→ 성공 → 로컬 DNS 서버 문제                             │    │
│  │  │       → /etc/resolv.conf 확인                           │    │
│  │  │       → DNS 서버 변경 고려                              │    │
│  │  └─→ 실패 → 도메인 자체 문제 또는 방화벽                   │    │
│  │           → 도메인 등록 상태 확인 (whois)                  │    │
│  │           → 방화벽 UDP 53 포트 확인                        │    │
│  └─────────────────────────────────────────────────────────────┘    │
│         │                                                            │
│         ▼ (추가 진단)                                                │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  Step 4: 추가 확인                                          │    │
│  │  • /etc/hosts에 잘못된 엔트리?                             │    │
│  │  • DNS 캐시 문제? → flush-caches                           │    │
│  │  • TTL 확인 → 오래된 캐시?                                 │    │
│  │  • CNAME 체인 문제? → +trace로 확인                        │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 5.2 실습: DNS 문제 시뮬레이션 및 해결

```bash
#!/bin/bash
# dns-troubleshoot.sh - DNS 문제 진단 스크립트

DOMAIN=${1:-"google.com"}
echo "=== DNS 트러블슈팅: $DOMAIN ==="

# Step 1: 기본 연결 테스트
echo ""
echo "[Step 1] 기본 연결 테스트"
if ping -c 1 -W 2 $DOMAIN > /dev/null 2>&1; then
    echo "✅ Ping 성공 - 네트워크/DNS 정상"
else
    echo "⚠️ Ping 실패 - 추가 진단 필요"
fi

# Step 2: DNS 해석 확인
echo ""
echo "[Step 2] DNS 해석 확인"
RESOLVED=$(dig +short $DOMAIN | head -1)
if [ -n "$RESOLVED" ]; then
    echo "✅ DNS 해석 성공: $RESOLVED"
else
    echo "⚠️ DNS 해석 실패"
fi

# Step 3: 외부 DNS로 테스트
echo ""
echo "[Step 3] 외부 DNS 서버 테스트 (8.8.8.8)"
EXTERNAL=$(dig @8.8.8.8 +short $DOMAIN | head -1)
if [ -n "$EXTERNAL" ]; then
    echo "✅ 외부 DNS 성공: $EXTERNAL"
    if [ "$RESOLVED" != "$EXTERNAL" ]; then
        echo "⚠️ 로컬 DNS와 결과 다름"
    fi
else
    echo "⚠️ 외부 DNS도 실패 - 도메인 문제 또는 네트워크 차단"
fi

# Step 4: 현재 DNS 서버 확인
echo ""
echo "[Step 4] 현재 DNS 설정"
echo "DNS 서버:"
grep nameserver /etc/resolv.conf 2>/dev/null || echo "resolv.conf 없음"

# Step 5: /etc/hosts 확인
echo ""
echo "[Step 5] /etc/hosts 확인"
if grep -q "$DOMAIN" /etc/hosts; then
    echo "⚠️ /etc/hosts에 엔트리 존재:"
    grep "$DOMAIN" /etc/hosts
else
    echo "✅ /etc/hosts에 관련 엔트리 없음"
fi

# Step 6: 응답 시간
echo ""
echo "[Step 6] DNS 응답 시간"
QUERY_TIME=$(dig $DOMAIN | grep "Query time" | awk '{print $4}')
echo "응답 시간: ${QUERY_TIME}ms"
if [ "${QUERY_TIME:-0}" -gt 100 ]; then
    echo "⚠️ 응답 느림 (>100ms)"
fi

echo ""
echo "=== 진단 완료 ==="
```

```bash
# 스크립트 실행
chmod +x dns-troubleshoot.sh
./dns-troubleshoot.sh google.com
./dns-troubleshoot.sh nonexistent-domain-xyz.com
```

### 5.3 일반적인 DNS 문제와 해결책

| 증상 | 원인 | 확인 방법 | 해결 |
|------|------|----------|------|
| "Name not resolved" | DNS 서버 도달 불가 | `dig @8.8.8.8 domain` | DNS 서버 변경 |
| NXDOMAIN | 도메인 존재하지 않음 | `whois domain` | 도메인 확인 |
| SERVFAIL | DNS 서버 오류 | 다른 DNS 서버 시도 | DNS 서버 변경 |
| 느린 응답 | DNS 서버 과부하/원거리 | 응답 시간 측정 | 가까운 DNS 사용 |
| 오래된 IP | 캐시된 구 정보 | TTL 확인 | 캐시 초기화 |
| 불일치 응답 | /etc/hosts 오버라이드 | hosts 파일 확인 | 엔트리 제거 |

### 5.4 DNS 캐시 관리

```bash
# systemd-resolved 캐시 초기화
sudo resolvectl flush-caches
echo "DNS 캐시 초기화 완료"

# 캐시 통계 확인
resolvectl statistics

# nscd (Name Service Cache Daemon) 사용 시
sudo systemctl restart nscd
# 또는
sudo nscd --invalidate=hosts

# dnsmasq 사용 시
sudo systemctl restart dnsmasq
```

### 5.5 Kubernetes DNS 문제 (미리보기)

```bash
# Kubernetes CoreDNS 확인 (나중에 상세 학습)
# 이것이 토스플레이스에서 사용하는 서비스 디스커버리의 핵심

# CoreDNS Pod 상태
kubectl get pods -n kube-system -l k8s-app=kube-dns

# CoreDNS 서비스
kubectl get svc -n kube-system kube-dns

# K8s 내부 DNS 테스트
kubectl run dns-test --image=busybox:1.36 --rm -it --restart=Never -- \
  nslookup kubernetes.default

# Service DNS 형식:
# <service-name>.<namespace>.svc.cluster.local
# 예: nginx-service.default.svc.cluster.local
```

---

## 📊 Part 6: DNS 보안 (20분)

### 6.1 DNS 보안 위협

```
┌─────────────────────────────────────────────────────────────────────┐
│  주요 DNS 보안 위협                                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. DNS Spoofing/Cache Poisoning                                    │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  공격자가 가짜 DNS 응답 주입                                │    │
│  │  → 사용자가 악성 사이트로 리다이렉트                       │    │
│  │  대응: DNSSEC 사용                                          │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  2. DNS Amplification (DDoS)                                        │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  DNS 서버를 이용한 증폭 공격                                │    │
│  │  → 작은 쿼리 → 큰 응답으로 대상 공격                       │    │
│  │  대응: Rate limiting, Response Rate Limiting (RRL)         │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  3. DNS Hijacking                                                   │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │  DNS 설정 변조                                              │    │
│  │  → /etc/resolv.conf, 공유기 DNS 설정 변경                  │    │
│  │  대응: DNS 설정 모니터링, DNS-over-HTTPS/TLS               │    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 6.2 안전한 DNS 설정

```bash
# DNS-over-HTTPS (DoH) / DNS-over-TLS (DoT) 사용
# systemd-resolved에서 DoT 활성화

sudo tee /etc/systemd/resolved.conf.d/dns-over-tls.conf << 'EOF'
[Resolve]
DNS=1.1.1.1 8.8.8.8
DNSOverTLS=yes
EOF

sudo systemctl restart systemd-resolved

# 확인
resolvectl status | grep -i "DNS over TLS"
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | DNS 동작 원리 이해 | 조회 과정, 캐싱, TTL | ☐ |
| 2 | dig 명령어 마스터 | +short, +trace, @server | ☐ |
| 3 | nslookup 사용 | 기본 조회, 타입 지정 | ☐ |
| 4 | DNS 설정 파일 이해 | resolv.conf, hosts | ☐ |
| 5 | DNS 캐시 관리 | flush-caches | ☐ |
| 6 | 트러블슈팅 스크립트 | 진단 플로우 실행 | ☐ |
| 7 | DNS 보안 이해 | 위협, DoT/DoH | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# dig 필수 명령어
dig +short <domain>           # 간결한 IP 출력
dig @8.8.8.8 <domain>         # 특정 DNS 서버 사용
dig +trace <domain>           # DNS 경로 추적
dig -x <IP>                   # 역방향 조회
dig <domain> <TYPE>           # 특정 레코드 타입 (A, MX, NS, TXT)

# nslookup
nslookup <domain>
nslookup -type=MX <domain>

# DNS 설정
cat /etc/resolv.conf          # DNS 서버 확인
cat /etc/hosts                # 로컬 오버라이드

# 캐시 관리
sudo resolvectl flush-caches  # 캐시 초기화
resolvectl statistics         # 캐시 통계
```

---

## 💡 면접 대비 핵심 포인트

### Q1: DNS 조회 과정을 설명해주세요

**A**: "클라이언트가 도메인을 조회하면, 먼저 로컬 캐시와 /etc/hosts를 확인합니다. 없으면 /etc/resolv.conf의 DNS 서버에 쿼리합니다. DNS 서버는 캐시가 없으면 Root → TLD → Authoritative 순으로 재귀 쿼리하여 IP를 반환합니다. 결과는 TTL 동안 캐시됩니다."

### Q2: dig와 nslookup의 차이점은?

**A**: "dig는 더 상세한 출력과 +trace 같은 고급 기능을 제공하며, 스크립팅에 적합합니다. nslookup은 간단하고 Windows에서도 기본 제공됩니다. 트러블슈팅에는 dig를 권장합니다."

### Q3: DNS 문제를 어떻게 진단하나요?

**A**: 
1. `dig domain` - 로컬 DNS로 해석 확인
2. `dig @8.8.8.8 domain` - 외부 DNS로 비교
3. `/etc/resolv.conf` 확인 - DNS 서버 설정
4. `/etc/hosts` 확인 - 로컬 오버라이드
5. `resolvectl flush-caches` - 캐시 문제 배제

### Q4: Kubernetes에서 DNS는 어떻게 동작하나요?

**A**: "Kubernetes는 CoreDNS를 사용하여 Service Discovery를 제공합니다. Pod는 CoreDNS를 DNS 서버로 사용하고, `service-name.namespace.svc.cluster.local` 형식으로 다른 서비스를 찾습니다. 이는 마이크로서비스 간 통신의 핵심입니다."

---

## 📝 학습 기록

```
학습일: ____년 __월 __일
실제 소요 시간: ____시간

실습 완료 항목:
- [ ] dig 기본/고급 옵션
- [ ] DNS 서버별 응답 비교
- [ ] +trace 경로 분석
- [ ] /etc/resolv.conf 분석
- [ ] 트러블슈팅 스크립트 실행

이해가 어려웠던 부분:


추가 학습 필요 항목:

```

---

## ➡️ 다음 학습: Day 19

**주제**: HTTP/HTTPS 기초
- HTTP 메서드와 상태 코드
- curl 활용
- HTTPS 인증서 확인
