# Day 12: Docker 볼륨과 데이터 관리

## 오늘의 목표

토스플레이스 연결점: "컨테이너 오케스트레이션 서비스 운영"
"인프라에 대한 오너십을 가지고 주도적으로 운영/개선해온 경험"

컨테이너의 데이터 영속성을 관리하는 방법을 학습합니다. Docker 볼륨, 바인드 마운트, tmpfs를 이해하고 실제 데이터베이스 컨테이너에 적용합니다.

---

## 예상 학습 시간: 4시간

| 구분 | 시간 | 내용 |
|------|------|------|
| 핵심 개념 | 45분 | 볼륨 종류, 특징 |
| 볼륨 실습 | 1시간 | Named Volume, Bind Mount |
| DB 실습 | 1.5시간 | MySQL, Redis 데이터 영속화 |
| 백업/복원 | 45분 | 데이터 백업 및 복원 |

---

## Part 1: 볼륨 핵심 개념 (45분)

### 1.1 왜 볼륨이 필요한가?

```
컨테이너 특성:
- 컨테이너는 일시적(ephemeral)
- 컨테이너 삭제 시 내부 데이터도 삭제
- 같은 이미지의 다른 컨테이너와 데이터 공유 불가

볼륨이 해결하는 문제:
- 데이터 영속성 (Persistence)
- 컨테이너 간 데이터 공유
- 호스트와 컨테이너 간 파일 공유
- 데이터 백업 및 마이그레이션
```

### 1.2 볼륨 종류

```
1. Named Volume (도커 관리 볼륨)
   - Docker가 관리하는 볼륨
   - /var/lib/docker/volumes/에 저장
   - 이름으로 참조
   - 권장 방식

2. Bind Mount (바인드 마운트)
   - 호스트의 특정 경로를 컨테이너에 마운트
   - 개발 환경에서 코드 공유에 유용
   - 호스트 경로 의존성

3. tmpfs Mount
   - 메모리에만 저장 (호스트/컨테이너 재시작 시 삭제)
   - 민감한 임시 데이터에 사용
   - Linux에서만 지원
```

### 1.3 볼륨 비교

| 특성 | Named Volume | Bind Mount | tmpfs |
|------|-------------|------------|-------|
| 위치 | Docker 관리 | 호스트 경로 | 메모리 |
| 영속성 | 영구 | 영구 | 휘발성 |
| 이동성 | 높음 | 낮음 | N/A |
| 성능 | 좋음 | 호스트와 동일 | 매우 빠름 |
| 사용 | 프로덕션 | 개발 | 임시 데이터 |

---

## Part 2: 볼륨 실습 (1시간)

### 실습 1: Named Volume

```bash
# 볼륨 생성
docker volume create mydata

# 볼륨 목록
docker volume ls

# 볼륨 상세 정보
docker volume inspect mydata

# 출력 예시:
# [
#     {
#         "Name": "mydata",
#         "Mountpoint": "/var/lib/docker/volumes/mydata/_data",
#         ...
#     }
# ]

# 볼륨 사용 (컨테이너에 마운트)
docker run -d --name vol-test \
  -v mydata:/app/data \
  nginx

# 컨테이너에서 파일 생성
docker exec vol-test sh -c "echo 'Hello Volume' > /app/data/test.txt"

# 확인
docker exec vol-test cat /app/data/test.txt

# 컨테이너 삭제
docker rm -f vol-test

# 새 컨테이너에서 데이터 확인 (데이터 유지됨!)
docker run --rm -v mydata:/app/data alpine cat /app/data/test.txt
# 출력: Hello Volume

# 볼륨 삭제
docker volume rm mydata
```

### 실습 2: Bind Mount

```bash
# 호스트 디렉토리 생성
mkdir -p ~/docker-data/html

# 테스트 파일 생성
echo "<h1>Hello from Host</h1>" > ~/docker-data/html/index.html

# Bind Mount로 nginx 실행
docker run -d --name web \
  -p 8080:80 \
  -v ~/docker-data/html:/usr/share/nginx/html:ro \
  nginx

# 확인
curl http://localhost:8080

# 호스트에서 파일 수정
echo "<h1>Updated from Host!</h1>" > ~/docker-data/html/index.html

# 변경 즉시 반영
curl http://localhost:8080

# 정리
docker rm -f web
rm -rf ~/docker-data/html
```

### 실습 3: tmpfs Mount

```bash
# tmpfs 마운트 (메모리에만 저장)
docker run -d --name tmpfs-test \
  --tmpfs /app/temp:rw,noexec,nosuid,size=100m \
  nginx

# 파일 생성
docker exec tmpfs-test sh -c "echo 'Temp Data' > /app/temp/secret.txt"

# 확인
docker exec tmpfs-test cat /app/temp/secret.txt

# 컨테이너 재시작 후 데이터 확인
docker restart tmpfs-test
docker exec tmpfs-test cat /app/temp/secret.txt 2>/dev/null || echo "File not found (expected)"

# 정리
docker rm -f tmpfs-test
```

---

## Part 3: 데이터베이스 실습 (1.5시간)

### 실습 4: MySQL 데이터 영속화

```bash
# MySQL 볼륨 생성
docker volume create mysql-data

# MySQL 실행
docker run -d --name mysql \
  -e MYSQL_ROOT_PASSWORD=rootpass \
  -e MYSQL_DATABASE=testdb \
  -e MYSQL_USER=testuser \
  -e MYSQL_PASSWORD=testpass \
  -v mysql-data:/var/lib/mysql \
  -p 3306:3306 \
  mysql:8.0

# 초기화 대기 (30초 정도)
sleep 30

# 데이터 생성
docker exec -i mysql mysql -uroot -prootpass testdb <<EOF
CREATE TABLE users (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100));
INSERT INTO users (name) VALUES ('Alice'), ('Bob'), ('Charlie');
SELECT * FROM users;
EOF

# 컨테이너 삭제
docker rm -f mysql

# 새 컨테이너로 데이터 확인
docker run -d --name mysql-new \
  -e MYSQL_ROOT_PASSWORD=rootpass \
  -v mysql-data:/var/lib/mysql \
  mysql:8.0

sleep 20

# 데이터가 유지됨!
docker exec mysql-new mysql -uroot -prootpass testdb -e "SELECT * FROM users;"

# 정리
docker rm -f mysql-new
```

### 실습 5: Redis 데이터 영속화

```bash
# Redis 볼륨 생성
docker volume create redis-data

# Redis 실행 (AOF 영속화 활성화)
docker run -d --name redis \
  -v redis-data:/data \
  -p 6379:6379 \
  redis:alpine redis-server --appendonly yes

# 데이터 저장
docker exec redis redis-cli SET greeting "Hello Redis"
docker exec redis redis-cli SET counter 100

# 데이터 확인
docker exec redis redis-cli GET greeting
docker exec redis redis-cli GET counter

# 컨테이너 삭제
docker rm -f redis

# 새 컨테이너에서 데이터 확인
docker run -d --name redis-new \
  -v redis-data:/data \
  redis:alpine redis-server --appendonly yes

docker exec redis-new redis-cli GET greeting
docker exec redis-new redis-cli GET counter
# 데이터가 유지됨!

# 정리
docker rm -f redis-new
```

### 실습 6: Docker Compose로 데이터 관리

```bash
mkdir -p ~/docker-volume-test && cd ~/docker-volume-test

cat << 'EOF' > docker-compose.yml
services:
  db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpass
      MYSQL_DATABASE: myapp
    volumes:
      - db-data:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    ports:
      - "3306:3306"

  redis:
    image: redis:alpine
    command: redis-server --appendonly yes
    volumes:
      - redis-data:/data
    ports:
      - "6379:6379"

volumes:
  db-data:
  redis-data:
EOF

# 초기화 SQL
cat << 'EOF' > init.sql
CREATE TABLE IF NOT EXISTS products (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100),
    price DECIMAL(10,2)
);
INSERT INTO products (name, price) VALUES ('Product A', 19.99);
INSERT INTO products (name, price) VALUES ('Product B', 29.99);
EOF

# 실행
docker-compose up -d

# 확인
sleep 30
docker-compose exec db mysql -uroot -prootpass myapp -e "SELECT * FROM products;"

# 정리
docker-compose down
# 볼륨은 유지됨
docker volume ls | grep docker-volume-test

# 볼륨까지 삭제
docker-compose down -v
```

---

## Part 4: 백업/복원 (45분)

### 실습 7: 볼륨 백업

```bash
# 테스트 볼륨 생성 및 데이터 추가
docker volume create backup-test
docker run --rm -v backup-test:/data alpine sh -c "
  echo 'Important Data 1' > /data/file1.txt
  echo 'Important Data 2' > /data/file2.txt
  mkdir /data/subdir
  echo 'Subdir Data' > /data/subdir/file3.txt
"

# 볼륨 백업 (tar 아카이브로)
docker run --rm \
  -v backup-test:/source:ro \
  -v $(pwd):/backup \
  alpine tar czf /backup/backup-test.tar.gz -C /source .

# 백업 파일 확인
ls -lh backup-test.tar.gz
tar tzf backup-test.tar.gz
```

### 실습 8: 볼륨 복원

```bash
# 복원용 새 볼륨 생성
docker volume create backup-restore

# 백업에서 복원
docker run --rm \
  -v backup-restore:/target \
  -v $(pwd):/backup:ro \
  alpine tar xzf /backup/backup-test.tar.gz -C /target

# 복원된 데이터 확인
docker run --rm -v backup-restore:/data alpine ls -la /data
docker run --rm -v backup-restore:/data alpine cat /data/file1.txt
docker run --rm -v backup-restore:/data alpine cat /data/subdir/file3.txt

# 정리
docker volume rm backup-test backup-restore
rm backup-test.tar.gz
```

### 실습 9: MySQL 논리적 백업

```bash
# MySQL 볼륨 생성 및 실행
docker volume create mysql-backup-test
docker run -d --name mysql-backup \
  -e MYSQL_ROOT_PASSWORD=rootpass \
  -e MYSQL_DATABASE=testdb \
  -v mysql-backup-test:/var/lib/mysql \
  mysql:8.0

sleep 30

# 데이터 생성
docker exec -i mysql-backup mysql -uroot -prootpass testdb <<EOF
CREATE TABLE orders (id INT PRIMARY KEY, total DECIMAL(10,2));
INSERT INTO orders VALUES (1, 100.00), (2, 200.00);
EOF

# mysqldump로 백업
docker exec mysql-backup mysqldump -uroot -prootpass testdb > testdb_backup.sql

# 백업 파일 확인
head -20 testdb_backup.sql

# 정리
docker rm -f mysql-backup
docker volume rm mysql-backup-test
rm testdb_backup.sql
```

---

## 오늘의 체크리스트

| # | 항목 | 설명 | 완료 |
|---|------|------|------|
| 1 | 볼륨 종류 이해 | Named, Bind, tmpfs | |
| 2 | Named Volume 실습 | create, 사용, 삭제 | |
| 3 | Bind Mount 실습 | 호스트 경로 마운트 | |
| 4 | MySQL 영속화 | 볼륨으로 데이터 유지 | |
| 5 | Redis 영속화 | AOF 활성화 | |
| 6 | Compose 볼륨 | volumes 섹션 | |
| 7 | 볼륨 백업/복원 | tar로 백업 | |

---

## 핵심 명령어

```bash
# 볼륨 관리
docker volume create <name>
docker volume ls
docker volume inspect <name>
docker volume rm <name>
docker volume prune          # 미사용 볼륨 삭제

# 마운트
-v <volume>:/path            # Named Volume
-v /host/path:/container/path # Bind Mount
--tmpfs /path                # tmpfs

# Compose
volumes:
  my-volume:
services:
  app:
    volumes:
      - my-volume:/data
```

---

## 면접 대비

**Q: Named Volume과 Bind Mount의 차이는?**
> "Named Volume은 Docker가 관리하며 이동성이 좋아 프로덕션에 적합합니다. Bind Mount는 호스트 경로를 직접 마운트하여 개발 시 코드 공유에 유용하지만 호스트 의존성이 있습니다."

**Q: 컨테이너 삭제 시 데이터를 유지하려면?**
> "볼륨을 사용합니다. docker rm은 컨테이너만 삭제하고 볼륨은 유지됩니다. docker-compose down -v는 볼륨도 함께 삭제하므로 주의해야 합니다."

---

## 정리

```bash
cd ~
rm -rf ~/docker-volume-test
docker volume prune -f
```

---

## 다음 학습: Day 13

주제: Docker 네트워킹 심화
- 네트워크 드라이버
- 컨테이너 간 통신
- DNS 서비스 디스커버리
