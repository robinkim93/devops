# Cloud-Native Payment Simulation Platform

클라우드 네이티브 환경에서 운영되는 결제 시뮬레이션 플랫폼입니다. 이 프로젝트는 Kubernetes 환경에서의 마이크로서비스 운영, 트래픽 시뮬레이션, 모니터링, 장애 대응 등을 실험하기 위한 플랫폼입니다.

## 프로젝트 구조

```
payment-simulation-platform/
├── app/                  # 메인 Spring Boot 애플리케이션 (포트: 8080)
├── auth-module/          # 인증 모듈 (라이브러리)
├── payment-module/       # 결제 모듈 (라이브러리)
├── point-module/         # 포인트 모듈 (라이브러리)
├── coupon-module/        # 쿠폰 모듈 (라이브러리)
├── frontend/             # React 프론트엔드 (포트: 3000)
├── build.gradle          # 루트 Gradle 빌드 파일
├── settings.gradle       # Gradle 모듈 설정
└── docker-compose.yml    # MySQL, Redis 인프라 (로컬 개발용)
```

**구조 설명:**
- `app` 모듈이 메인 Spring Boot 애플리케이션으로, 모든 모듈을 의존성으로 포함합니다.
- 각 모듈(auth, payment, point, coupon)은 라이브러리 모듈로 구성되어 코드를 모듈화합니다.
- 하나의 `gradlew bootRun`으로 모든 기능이 포함된 단일 애플리케이션이 실행됩니다.

## 기술 스택

### Backend
- **Spring Boot 3.2.0** - Java 17
- **Spring Data JPA** - 데이터베이스 접근
- **MySQL 8.0** - 트랜잭션 데이터 저장
- **Redis 7** - 토큰/세션 관리, 캐싱
- **JWT** - 인증 토큰 관리

### Frontend
- **React 18** - UI 프레임워크
- **Vite** - 빌드 도구
- **Axios** - HTTP 클라이언트

## 서비스 설명

### Auth Module (인증 서비스)
- 사용자 회원가입/로그인
- JWT 토큰 발급 및 검증
- Redis를 통한 토큰 관리

**API 엔드포인트:**
- `POST /api/auth/register` - 회원가입
- `POST /api/auth/login` - 로그인
- `POST /api/auth/validate` - 토큰 검증
- `POST /api/auth/logout` - 로그아웃

### Payment Module (결제 서비스)
- 가상 결제 처리
- 결제 완료 후 포인트 적립 (결제 금액의 1%)
- 10,000원 이상 결제 시 쿠폰 자동 발급
- 결제 내역 조회

**API 엔드포인트:**
- `POST /api/payments` - 결제 처리
- `GET /api/payments/{id}` - 결제 상세 조회
- `GET /api/payments/user` - 사용자 결제 내역 조회

### Point Module (포인트 서비스)
- 포인트 적립/차감
- 포인트 잔액 조회
- Redis 분산 락을 통한 동시성 제어
- 포인트 트랜잭션 기록

**API 엔드포인트:**
- `POST /api/points/add` - 포인트 적립
- `POST /api/points/use` - 포인트 사용
- `GET /api/points/balance/{userId}` - 포인트 잔액 조회

### Coupon Module (쿠폰 서비스)
- 쿠폰 발급
- 쿠폰 사용
- 사용자 쿠폰 목록 조회

**API 엔드포인트:**
- `POST /api/coupons/issue` - 쿠폰 발급
- `GET /api/coupons/user/{userId}` - 사용자 쿠폰 목록
- `GET /api/coupons/user/{userId}/active` - 사용 가능한 쿠폰 목록
- `POST /api/coupons/{couponId}/use` - 쿠폰 사용

## 로컬 개발 환경 설정

### 사전 요구사항
- Java 17 이상
- Gradle 7.6 이상
- Node.js 18 이상
- Docker & Docker Compose

### 1. 인프라 실행 (MySQL, Redis)

```bash
docker-compose up -d
```

### 2. Backend 서비스 실행

루트 디렉토리에서 하나의 명령으로 실행:

```bash
./gradlew :app:bootRun
```

또는 빌드 후 실행:

```bash
./gradlew build
cd app
./gradlew bootRun
```

### 3. Frontend 실행

```bash
cd frontend
npm install
npm run dev
```

### 4. 접속

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
  - Auth: http://localhost:8080/api/auth/*
  - Payment: http://localhost:8080/api/payments/*
  - Point: http://localhost:8080/api/points/*
  - Coupon: http://localhost:8080/api/coupons/*

## 데이터베이스 스키마

### MySQL 데이터베이스: `payment_db`

**users** - 사용자 정보
- id, email, password, name, created_at

**payments** - 결제 정보
- id, user_id, amount, status, order_id, payment_method, created_at, completed_at

**point_balances** - 포인트 잔액
- id, user_id, balance, updated_at

**point_transactions** - 포인트 트랜잭션
- id, user_id, amount, type, balance_after, reason, created_at

**coupons** - 쿠폰 정보
- id, user_id, coupon_type, status, discount_amount, discount_percent, expires_at, used_at, created_at

## API 호출 예시

### 1. 회원가입
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "name": "홍길동"
  }'
```

### 2. 로그인
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

### 3. 결제 처리
```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {토큰}" \
  -d '{
    "amount": 15000,
    "orderId": "ORDER-12345",
    "paymentMethod": "CARD"
  }'
```

## 다음 단계 (프로젝트 로드맵)

이 프로젝트는 다음 단계로 진행됩니다:

1. ✅ **2단계: 기본 서비스 개발** (현재 단계)
2. **3단계: 트래픽 시뮬레이션 구성** - k6 시나리오 작성
3. **4단계: Kubernetes 배포** - Deployment, Service, HPA 설정
4. **5단계: Istio Service Mesh 적용** - mTLS, Rate Limit, Circuit Breaker
5. **6단계: CI/CD 및 GitOps 구성** - Argo CD 연동
6. **7단계: 모니터링/로깅 구성** - Prometheus, Grafana, Thanos
7. **8단계: 비용 & 리소스 최적화 실험**
8. **9단계: 장애 대응 & 복구 자동화 테스트**
9. **10단계: AWS 환경 IaC 적용** - Terraform으로 EKS 구성

## 주의사항

- 이 프로젝트는 **시뮬레이션 목적**으로 개발되었습니다. 실제 결제 시스템으로 사용하지 마세요.
- 로컬 개발 환경에서는 단일 Spring Boot 애플리케이션으로 실행됩니다.
- Kubernetes 배포 시 각 모듈을 독립적인 Deployment로 분리 배포할 수 있습니다.
- 프로덕션 환경에서는 반드시 보안 설정을 강화해야 합니다.

## 라이선스

이 프로젝트는 학습 및 포트폴리오 목적으로 제작되었습니다.
