# 📅 Day 19: HTTP/HTTPS 기초

## 🎯 오늘의 목표

> **토스플레이스 연결점**: API 트러블슈팅, TLS 인증서 관리
> "OS, Network 등 다양한 레이어에서의 모니터링, 트러블슈팅 경험"

HTTP 요청/응답을 분석하고 HTTPS 인증서 문제를 진단할 수 있어야 합니다. 토스플레이스의 오프라인 결제 시스템에서 API 통신 문제를 빠르게 진단하는 능력은 필수입니다.

---

## ⏰ 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| HTTP 이론 | 45분 | 메서드, 상태 코드, 헤더 |
| curl 실습 | 1시간 | API 호출 및 분석 |
| HTTPS/TLS | 1시간 | 인증서, 암호화 |
| 트러블슈팅 | 1.25시간 | 실제 문제 해결 |

---

## 📚 Part 1: HTTP 기초 (45분)

### 1.1 HTTP 프로토콜 개요

```
┌─────────────────────────────────────────────────────────────────────┐
│  HTTP (HyperText Transfer Protocol) 개요                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Client                         Server                               │
│  ┌──────┐                       ┌──────┐                            │
│  │      │  ── HTTP Request ──>  │      │                            │
│  │ 브라우저│                       │ 웹서버 │                            │
│  │ API  │  <── HTTP Response ── │ API  │                            │
│  └──────┘                       └──────┘                            │
│                                                                      │
│  특징:                                                               │
│  • Stateless: 각 요청은 독립적                                      │
│  • Text 기반: 사람이 읽을 수 있는 형식                              │
│  • Request-Response 모델                                            │
│                                                                      │
│  버전:                                                               │
│  • HTTP/1.1: 가장 널리 사용 (Keep-Alive 기본)                       │
│  • HTTP/2: 멀티플렉싱, 헤더 압축                                    │
│  • HTTP/3: QUIC 기반, UDP 사용                                      │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 HTTP 메서드

| 메서드 | 용도 | 특징 | 예시 |
|--------|------|------|------|
| **GET** | 데이터 조회 | 안전, 멱등 | `GET /users/123` |
| **POST** | 데이터 생성 | Body 포함 | `POST /users` |
| **PUT** | 데이터 전체 수정 | 멱등, 전체 교체 | `PUT /users/123` |
| **PATCH** | 데이터 부분 수정 | 일부만 변경 | `PATCH /users/123` |
| **DELETE** | 데이터 삭제 | 멱등 | `DELETE /users/123` |
| **HEAD** | 헤더만 조회 | GET과 동일하나 Body 없음 | `HEAD /users/123` |
| **OPTIONS** | 허용 메서드 조회 | CORS preflight | `OPTIONS /api` |

**멱등성(Idempotency)이란?**
```
동일한 요청을 여러 번 보내도 결과가 같음

멱등한 메서드: GET, PUT, DELETE, HEAD, OPTIONS
비멱등 메서드: POST, PATCH

예시:
• DELETE /users/123 → 첫 번째: 삭제됨, 두 번째: 이미 없음 (결과 동일)
• POST /users → 매번 새 사용자 생성 (결과 다름)
```

### 1.3 HTTP 상태 코드

```
┌─────────────────────────────────────────────────────────────────────┐
│  HTTP Status Codes                                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1xx: Informational (정보)                                          │
│  ├── 100 Continue: 계속 진행                                        │
│  └── 101 Switching Protocols: 프로토콜 변경 (WebSocket)             │
│                                                                      │
│  2xx: Success (성공)                                                │
│  ├── 200 OK: 요청 성공                                              │
│  ├── 201 Created: 리소스 생성됨                                     │
│  ├── 202 Accepted: 요청 수락됨 (비동기 처리)                        │
│  └── 204 No Content: 성공, 응답 본문 없음                           │
│                                                                      │
│  3xx: Redirection (리다이렉트)                                      │
│  ├── 301 Moved Permanently: 영구 이동                               │
│  ├── 302 Found: 임시 이동                                           │
│  ├── 304 Not Modified: 캐시 사용                                    │
│  └── 307 Temporary Redirect: 임시 리다이렉트 (메서드 유지)          │
│                                                                      │
│  4xx: Client Error (클라이언트 오류)                                │
│  ├── 400 Bad Request: 잘못된 요청                                   │
│  ├── 401 Unauthorized: 인증 필요                                    │
│  ├── 403 Forbidden: 권한 없음                                       │
│  ├── 404 Not Found: 리소스 없음                                     │
│  ├── 405 Method Not Allowed: 허용되지 않는 메서드                   │
│  ├── 408 Request Timeout: 요청 시간 초과                            │
│  ├── 429 Too Many Requests: 요청 과다 (Rate Limit)                  │
│  └── 499 Client Closed Request: 클라이언트가 연결 종료 (Nginx)      │
│                                                                      │
│  5xx: Server Error (서버 오류)                                      │
│  ├── 500 Internal Server Error: 서버 내부 오류                      │
│  ├── 502 Bad Gateway: 게이트웨이 오류 (upstream 문제)               │
│  ├── 503 Service Unavailable: 서비스 불가 (과부하/점검)             │
│  └── 504 Gateway Timeout: 게이트웨이 시간 초과                      │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

**토스플레이스 관점에서 중요한 상태 코드:**
```
• 502 Bad Gateway: 백엔드 서비스 다운 → 즉시 확인 필요
• 503 Service Unavailable: 서비스 과부하 → 스케일링 검토
• 504 Gateway Timeout: 응답 지연 → 성능 최적화 필요
• 429 Too Many Requests: Rate Limiting 동작 → 정상적 보호
```

### 1.4 HTTP 헤더

```bash
# 주요 요청 헤더
Host: api.toss.im              # 대상 호스트
Content-Type: application/json # 요청 본문 형식
Authorization: Bearer xxx      # 인증 토큰
Accept: application/json       # 응답 형식 요청
User-Agent: curl/7.68.0        # 클라이언트 정보
X-Request-ID: abc-123          # 요청 추적 ID

# 주요 응답 헤더
Content-Type: application/json # 응답 본문 형식
Content-Length: 1234           # 응답 크기
Cache-Control: no-cache        # 캐시 정책
X-Response-Time: 45ms          # 응답 시간
Set-Cookie: session=xxx        # 쿠키 설정
```

---

## 🛠️ Part 2: curl 실습 (1시간)

### 실습 1: curl 기본 (30분)

```bash
# 기본 GET 요청
curl https://httpbin.org/get

# 상세 출력 (헤더 포함)
# -v: verbose 모드로 요청/응답 헤더 모두 표시
curl -v https://httpbin.org/get

# 출력 해석
# * : curl 내부 정보 (연결, TLS 등)
# > : 요청 헤더 (보내는 것)
# < : 응답 헤더 (받는 것)
# { : 데이터 전송 중
# } : 데이터 수신 중

# 헤더만 보기 (-I: HEAD 요청)
curl -I https://httpbin.org/get

# 응답 헤더 포함하여 출력 (-i)
curl -i https://httpbin.org/get

# POST 요청 (form data)
curl -X POST https://httpbin.org/post -d "name=test&value=123"

# POST 요청 (JSON)
curl -X POST https://httpbin.org/post \
  -H "Content-Type: application/json" \
  -d '{"name": "test", "value": 123}'

# PUT 요청
curl -X PUT https://httpbin.org/put \
  -H "Content-Type: application/json" \
  -d '{"id": 1, "name": "updated"}'

# DELETE 요청
curl -X DELETE https://httpbin.org/delete

# 응답 코드만 확인
curl -s -o /dev/null -w "%{http_code}" https://httpbin.org/get

# 여러 정보 출력
curl -s -o /dev/null -w "
HTTP Code: %{http_code}
Time Total: %{time_total}s
Size: %{size_download} bytes
" https://httpbin.org/get
```

### 실습 2: 헤더 조작

```bash
# 커스텀 헤더 추가
curl -H "X-Custom-Header: my-value" \
     -H "Authorization: Bearer my-token" \
     https://httpbin.org/headers

# User-Agent 변경
curl -A "MyApp/1.0" https://httpbin.org/user-agent

# 쿠키 전송
curl -b "session=abc123; user=john" https://httpbin.org/cookies

# 쿠키 저장 및 사용
curl -c cookies.txt https://httpbin.org/cookies/set?name=value
curl -b cookies.txt https://httpbin.org/cookies

# Referer 설정
curl -e "https://google.com" https://httpbin.org/headers

# 기본 인증 (Basic Auth)
curl -u username:password https://httpbin.org/basic-auth/username/password

# Bearer 토큰 인증
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." https://api.example.com/
```

### 실습 3: 응답 시간 측정 (20분)

```bash
# 응답 시간 포맷 파일 생성
cat << 'EOF' > /tmp/curl-format.txt
     DNS Lookup:  %{time_namelookup}s
  TCP Connection:  %{time_connect}s
  TLS Handshake:  %{time_appconnect}s
   Time to First Byte:  %{time_starttransfer}s
                       ----------
        Total Time:  %{time_total}s

   Download Size:  %{size_download} bytes
  Download Speed:  %{speed_download} bytes/sec
     HTTP Status:  %{http_code}
EOF

# 시간 측정
curl -w "@/tmp/curl-format.txt" -o /dev/null -s https://google.com

# 간단한 시간 측정
curl -s -o /dev/null -w "Total: %{time_total}s\n" https://google.com
```

**시간 항목 해석:**
```
time_namelookup  : DNS 조회 완료 시간
                   → 높으면 DNS 서버 문제 또는 네트워크 지연

time_connect     : TCP 연결 완료 시간
                   → 높으면 서버 응답 지연 또는 네트워크 문제

time_appconnect  : TLS 핸드셰이크 완료 시간 (HTTPS만)
                   → 높으면 TLS 설정 최적화 필요

time_pretransfer : 전송 준비 완료 시간
                   → 요청 전송 직전까지 걸린 시간

time_starttransfer : 첫 바이트 수신 시간 (TTFB)
                   → 서버 처리 시간 포함, 가장 중요한 지표

time_total       : 전체 완료 시간
                   → 응답 다운로드까지 포함
```

### 실습 4: 고급 curl 옵션

```bash
# 연결 타임아웃 설정
curl --connect-timeout 5 https://example.com

# 전체 타임아웃 설정
curl --max-time 10 https://example.com

# 리다이렉트 따라가기
curl -L https://google.com

# 리다이렉트 따라가며 경로 표시
curl -L -v https://google.com 2>&1 | grep "< location\|< HTTP"

# 최대 리다이렉트 횟수
curl -L --max-redirs 3 https://example.com

# 압축 요청 (gzip, deflate)
curl --compressed https://example.com

# 프록시 사용
curl -x http://proxy:8080 https://example.com

# SOCKS 프록시
curl --socks5 localhost:1080 https://example.com

# 특정 IP로 연결 (DNS 우회)
curl --resolve example.com:443:1.2.3.4 https://example.com

# HTTP 버전 지정
curl --http1.1 https://example.com
curl --http2 https://example.com

# 재시도
curl --retry 3 --retry-delay 2 https://example.com

# 병렬 요청 (여러 URL)
curl --parallel --parallel-max 5 \
  https://example1.com \
  https://example2.com \
  https://example3.com
```

---

## 📚 Part 3: HTTPS/TLS (1시간)

### 3.1 HTTPS 개요

```
┌─────────────────────────────────────────────────────────────────────┐
│  HTTPS = HTTP + TLS (Transport Layer Security)                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  HTTP (평문)              HTTPS (암호화)                            │
│  ┌──────────┐             ┌──────────────────┐                      │
│  │ GET /api │   ────>     │ 암호화된 데이터   │                      │
│  │ Host:... │   ────>     │ (읽을 수 없음)    │                      │
│  └──────────┘             └──────────────────┘                      │
│                                                                      │
│  TLS가 제공하는 기능:                                               │
│  1. 기밀성 (Confidentiality): 데이터 암호화                         │
│  2. 무결성 (Integrity): 데이터 변조 감지                            │
│  3. 인증 (Authentication): 서버/클라이언트 신원 확인                │
│                                                                      │
│  TLS 핸드셰이크:                                                    │
│  ┌────────┐                         ┌────────┐                      │
│  │ Client │  ── ClientHello ──>     │ Server │                      │
│  │        │  <── ServerHello ──     │        │                      │
│  │        │  <── Certificate ──     │        │                      │
│  │        │  ── Key Exchange ──>    │        │                      │
│  │        │  ── Finished ──>        │        │                      │
│  │        │  <── Finished ──        │        │                      │
│  └────────┘     [암호화 통신]        └────────┘                      │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.2 인증서 확인

```bash
# 인증서 정보 확인
openssl s_client -connect google.com:443 -servername google.com </dev/null 2>/dev/null | \
  openssl x509 -text -noout | head -30

# 인증서 주요 정보만 출력
openssl s_client -connect google.com:443 -servername google.com </dev/null 2>/dev/null | \
  openssl x509 -noout -subject -issuer -dates

# 출력 예시:
# subject=CN = *.google.com           # 도메인
# issuer=C = US, O = Google Trust Services # 발급 기관
# notBefore=Dec  4 08:24:29 2023 GMT  # 시작일
# notAfter=Feb 26 08:24:28 2024 GMT   # 만료일

# 만료일만 확인
echo | openssl s_client -connect google.com:443 -servername google.com 2>/dev/null | \
  openssl x509 -noout -dates

# 만료까지 남은 일수
echo | openssl s_client -connect google.com:443 -servername google.com 2>/dev/null | \
  openssl x509 -noout -enddate | \
  awk -F= '{print $2}' | \
  xargs -I {} sh -c 'echo "만료일: {}"; echo "남은 일수: $(( ($(date -d "{}" +%s) - $(date +%s)) / 86400 ))일"'

# 인증서 체인 확인
openssl s_client -connect google.com:443 -servername google.com -showcerts </dev/null 2>/dev/null | \
  grep -E "s:|i:"

# 인증서 체인 전체 출력
openssl s_client -connect google.com:443 -servername google.com -showcerts </dev/null
```

### 3.3 TLS 버전 및 암호화 확인

```bash
# 지원하는 TLS 버전 확인
# TLS 1.2
openssl s_client -connect google.com:443 -tls1_2 </dev/null 2>/dev/null | \
  grep "Protocol"

# TLS 1.3
openssl s_client -connect google.com:443 -tls1_3 </dev/null 2>/dev/null | \
  grep "Protocol"

# 사용된 암호화 스위트 확인
openssl s_client -connect google.com:443 </dev/null 2>/dev/null | \
  grep "Cipher"

# 서버가 지원하는 암호화 스위트 목록
nmap --script ssl-enum-ciphers -p 443 google.com

# curl로 TLS 정보 확인
curl -v --tlsv1.2 https://google.com 2>&1 | grep -E "SSL|TLS|subject|issuer"
```

### 3.4 인증서 문제 진단

```bash
# 자체 서명 인증서 테스트
curl https://self-signed.badssl.com/
# 에러: SSL certificate problem: self-signed certificate

# 인증서 검증 비활성화 (테스트용, 프로덕션 금지!)
curl -k https://self-signed.badssl.com/

# 만료된 인증서 테스트
curl https://expired.badssl.com/
# 에러: SSL certificate problem: certificate has expired

# 인증서 오류 상세 확인
curl -v https://expired.badssl.com/ 2>&1 | grep -E "SSL|error"

# 특정 CA 인증서로 검증
curl --cacert /path/to/ca.crt https://internal.example.com

# 클라이언트 인증서 사용
curl --cert client.crt --key client.key https://mtls.example.com
```

### 3.5 인증서 만료 모니터링 스크립트

```bash
#!/bin/bash
# check-ssl-expiry.sh - 인증서 만료 확인 스크립트

DOMAINS=(
  "google.com"
  "github.com"
  "example.com"
)

WARNING_DAYS=30

echo "=== SSL 인증서 만료 확인 ==="
echo ""

for domain in "${DOMAINS[@]}"; do
  expiry=$(echo | openssl s_client -connect ${domain}:443 -servername ${domain} 2>/dev/null | \
    openssl x509 -noout -enddate 2>/dev/null | cut -d= -f2)
  
  if [ -n "$expiry" ]; then
    expiry_epoch=$(date -d "$expiry" +%s 2>/dev/null || date -j -f "%b %d %T %Y %Z" "$expiry" +%s 2>/dev/null)
    now_epoch=$(date +%s)
    days_left=$(( (expiry_epoch - now_epoch) / 86400 ))
    
    if [ $days_left -lt 0 ]; then
      echo "❌ $domain: 만료됨!"
    elif [ $days_left -lt $WARNING_DAYS ]; then
      echo "⚠️ $domain: ${days_left}일 남음 (만료일: $expiry)"
    else
      echo "✅ $domain: ${days_left}일 남음"
    fi
  else
    echo "❌ $domain: 연결 실패"
  fi
done
```

---

## 🛠️ Part 4: HTTP 트러블슈팅 (1.25시간)

### 4.1 일반적인 HTTP 문제

```
┌─────────────────────────────────────────────────────────────────────┐
│  HTTP 트러블슈팅 체크리스트                                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1. 연결 문제                                                       │
│     □ DNS 해석 실패 → dig, nslookup 확인                            │
│     □ TCP 연결 실패 → telnet, nc 확인                               │
│     □ 방화벽 차단 → 포트 확인, 보안 그룹 확인                       │
│                                                                      │
│  2. 인증서 문제                                                     │
│     □ 만료 → openssl s_client로 만료일 확인                         │
│     □ 도메인 불일치 → CN/SAN 확인                                   │
│     □ 체인 불완전 → 중간 인증서 확인                                │
│                                                                      │
│  3. 타임아웃                                                        │
│     □ DNS 타임아웃 → DNS 서버 확인                                  │
│     □ 연결 타임아웃 → 서버 상태 확인                                │
│     □ 응답 타임아웃 → 서버 부하 확인                                │
│                                                                      │
│  4. 응답 오류                                                       │
│     □ 4xx → 클라이언트 요청 확인 (인증, 권한, 경로)                 │
│     □ 5xx → 서버 로그 확인, 백엔드 상태 확인                        │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.2 연결 테스트

```bash
# DNS 해석 확인
dig example.com
nslookup example.com

# TCP 연결 테스트
# telnet
telnet example.com 443

# nc (netcat)
nc -zv example.com 443

# curl 연결 테스트
curl -v --connect-timeout 5 https://example.com 2>&1 | head -20

# 특정 IP로 직접 연결 테스트
curl --resolve example.com:443:93.184.216.34 https://example.com

# MTR (네트워크 경로 추적)
mtr -rwc 10 example.com
```

### 4.3 다양한 HTTP 문제 시뮬레이션 및 해결

```bash
# 1. 리다이렉트 문제
curl -I http://google.com
# HTTP/1.1 301 Moved Permanently
# Location: http://www.google.com/

# 해결: 리다이렉트 따라가기
curl -L http://google.com

# 2. 특정 호스트 헤더로 요청 (가상 호스트)
curl -H "Host: example.com" http://192.168.1.1/

# 3. HTTP/2 지원 확인
curl -I --http2 https://google.com
# HTTP/2 200 표시되면 HTTP/2 지원

# 4. 프록시 통과 테스트
curl -x http://proxy:8080 https://example.com

# 5. 느린 응답 시뮬레이션
curl https://httpbin.org/delay/5  # 5초 지연

# 6. 특정 상태 코드 응답
curl https://httpbin.org/status/500  # 500 에러 응답
curl https://httpbin.org/status/404  # 404 에러 응답

# 7. 대용량 응답
curl -o /dev/null -w "Size: %{size_download}\n" https://httpbin.org/bytes/1000000
```

### 4.4 실전 트러블슈팅 시나리오

```bash
# 시나리오 1: 502 Bad Gateway
# 원인 분석
curl -v https://api.example.com/health 2>&1 | grep -E "< HTTP|error"

# 확인 사항:
# - 백엔드 서버 상태
# - 프록시/로드밸런서 로그
# - 네트워크 연결

# 시나리오 2: 503 Service Unavailable
# 서버 부하 또는 유지보수

# 확인:
curl -I https://api.example.com/
# Retry-After 헤더 확인

# 시나리오 3: SSL 핸드셰이크 실패
openssl s_client -connect example.com:443 -servername example.com

# 확인:
# - 인증서 만료
# - TLS 버전 호환성
# - 암호화 스위트 호환성

# 시나리오 4: 느린 응답
curl -w "@/tmp/curl-format.txt" -o /dev/null -s https://api.example.com/

# 분석:
# - DNS 지연: time_namelookup 높음
# - 연결 지연: time_connect 높음
# - TLS 지연: time_appconnect 높음
# - 서버 지연: time_starttransfer 높음
```

### 4.5 API 모니터링 스크립트

```bash
#!/bin/bash
# api-health-check.sh - API 상태 모니터링

ENDPOINTS=(
  "https://httpbin.org/get"
  "https://httpbin.org/status/200"
  "https://google.com"
)

echo "=== API Health Check ==="
echo "시간: $(date)"
echo ""

for endpoint in "${ENDPOINTS[@]}"; do
  echo "테스트: $endpoint"
  
  result=$(curl -s -o /dev/null -w "%{http_code}|%{time_total}" \
    --connect-timeout 5 --max-time 10 "$endpoint")
  
  http_code=$(echo $result | cut -d'|' -f1)
  time_total=$(echo $result | cut -d'|' -f2)
  
  if [ "$http_code" = "200" ]; then
    echo "  상태: ✅ OK ($http_code)"
  elif [ "$http_code" = "000" ]; then
    echo "  상태: ❌ 연결 실패"
  else
    echo "  상태: ⚠️ $http_code"
  fi
  
  echo "  응답시간: ${time_total}s"
  echo ""
done
```

---

## 📊 Part 5: 토스플레이스 관점의 HTTP/HTTPS

### 5.1 API Gateway 트러블슈팅

```bash
# Istio Ingress Gateway 상태 확인
kubectl get pods -n istio-system -l app=istio-ingressgateway

# Gateway 로그 확인
kubectl logs -n istio-system -l app=istio-ingressgateway --tail=100

# Envoy 액세스 로그 확인
kubectl logs -n istio-system -l app=istio-ingressgateway | \
  grep -E "response_code|upstream_response_time"

# VirtualService 라우팅 확인
kubectl get virtualservice -A
kubectl describe virtualservice <name> -n <namespace>
```

### 5.2 서비스 간 통신 문제

```bash
# Pod 내에서 다른 서비스 호출 테스트
kubectl exec -it <pod-name> -- curl -v http://backend-svc:8080/health

# mTLS 적용 확인
kubectl exec -it <pod-name> -c istio-proxy -- \
  curl -v --insecure https://backend-svc:8080/health

# Istio Proxy 로그
kubectl logs <pod-name> -c istio-proxy | tail -50
```

### 5.3 인증서 관리 (cert-manager)

```bash
# 인증서 상태 확인
kubectl get certificate -A
kubectl describe certificate <name> -n <namespace>

# 인증서 갱신 이벤트
kubectl get events -A | grep -i certificate

# Secret에서 인증서 확인
kubectl get secret <tls-secret> -n <namespace> -o jsonpath='{.data.tls\.crt}' | \
  base64 -d | openssl x509 -text -noout
```

---

## ✅ 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | HTTP 메서드 이해 | GET, POST, PUT, DELETE | ☐ |
| 2 | HTTP 상태 코드 숙지 | 2xx, 4xx, 5xx 의미 | ☐ |
| 3 | curl -v로 요청 분석 | 헤더 해석 능력 | ☐ |
| 4 | curl -w로 응답 시간 측정 | TTFB, total time | ☐ |
| 5 | SSL 인증서 확인 | openssl s_client | ☐ |
| 6 | 인증서 만료일 확인 | 모니터링 스크립트 | ☐ |
| 7 | HTTP 트러블슈팅 | 연결, 인증서, 타임아웃 | ☐ |

---

## 🔑 오늘 배운 핵심 명령어

```bash
# curl 기본
curl -v https://example.com              # 상세 출력
curl -I https://example.com              # 헤더만
curl -X POST -d '{}' https://example.com # POST 요청
curl -H "Authorization: Bearer xxx"      # 커스텀 헤더

# 시간 측정
curl -w "@format.txt" -o /dev/null -s https://example.com
curl -s -o /dev/null -w "Time: %{time_total}s\n" https://example.com

# 인증서 확인
openssl s_client -connect host:443 -servername host
openssl x509 -noout -dates              # 만료일
openssl x509 -noout -subject -issuer    # 주체/발급자

# 트러블슈팅
curl --connect-timeout 5 https://example.com
curl -L https://example.com             # 리다이렉트 따라가기
curl -k https://self-signed.example.com # 인증서 무시 (테스트용)
```

---

## 💡 면접 대비 핵심 포인트

### Q1: 502 Bad Gateway와 504 Gateway Timeout의 차이는?
**A**: 
- **502 Bad Gateway**: 게이트웨이가 upstream 서버로부터 유효하지 않은 응답을 받음. 백엔드 서버가 다운되었거나 응답이 잘못된 경우.
- **504 Gateway Timeout**: 게이트웨이가 upstream 서버로부터 응답을 기다리다 타임아웃. 백엔드 서버가 너무 느리거나 응답하지 않는 경우.

### Q2: HTTPS 연결이 느릴 때 어떻게 분석하나요?
**A**: curl의 `-w` 옵션으로 단계별 시간을 측정합니다.
- time_namelookup: DNS 지연 → DNS 서버 확인
- time_connect: TCP 연결 지연 → 네트워크 확인
- time_appconnect: TLS 핸드셰이크 지연 → TLS 설정 최적화 (세션 재사용)
- time_starttransfer: 서버 처리 지연 → 백엔드 성능 확인

### Q3: 인증서 만료 전에 어떻게 알 수 있나요?
**A**: 
1. `openssl s_client`로 만료일 확인 스크립트 작성
2. Prometheus + blackbox_exporter로 자동 모니터링
3. cert-manager 사용 시 자동 갱신 및 이벤트 알림
4. 만료 30일 전 알림 설정 권장

---

## 📝 학습 기록

```
학습일: ____년 __월 __일
실제 소요 시간: ____시간

실습 완료 항목:
- [ ] curl 기본 요청
- [ ] 응답 시간 측정
- [ ] 인증서 확인
- [ ] 트러블슈팅 시나리오

이해가 어려웠던 부분:


추가 학습 필요 항목:

```

---

## ➡️ 다음 학습: Day 20

**주제**: Load Balancing 개념
- L4 vs L7 로드밸런싱
- 로드밸런싱 알고리즘
- Kubernetes Service와 Ingress
