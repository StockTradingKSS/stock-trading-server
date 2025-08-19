# 환경 설정 가이드

## 개요
이 프로젝트는 로컬 개발 환경과 프로덕션 환경을 분리하여 설정되어 있습니다.

## 환경별 설정

### 🏠 로컬 개발 환경 (Profile: local)

#### 필수 준비사항
1. **Docker Desktop 실행**
2. **PostgreSQL 컨테이너 시작**:
   ```bash
   docker-compose up -d
   ```

#### 실행 방법
1. PostgreSQL 시작: `docker-compose up -d`
2. IntelliJ에서 "Local Development" 실행 설정 사용
3. 또는 터미널에서: `./gradlew :stock-service:bootRun --args='--spring.profiles.active=local'`

#### 데이터베이스 설정
- **데이터베이스**: PostgreSQL 16
- **포트**: 5432
- **JPA**: `ddl-auto: update` (자동 테이블 생성)
- **연결**: `jdbc:postgresql://localhost:5432/stocktrading`

#### 유용한 명령어
```bash
# PostgreSQL 상태 확인
docker-compose ps

# 데이터베이스 접속
docker exec -it stock-postgres psql -U postgres -d stocktrading

# 컨테이너 중단
docker-compose down

# 데이터까지 삭제
docker-compose down -v
```

### 🚀 프로덕션 환경 (Profile: prod)

#### 설정 요소
- **데이터베이스**: AWS Aurora DSQL
- **인증**: ECS Task Role (IAM Role 기반)
- **포트**: 8080
- **JPA**: `ddl-auto: validate` (기존 스키마 사용)

#### ECS Task Role 권한
- `AmazonAuroraDSQLFullAccess`
- CloudWatch Logs 쓰기 권한

#### 환경변수 (GitHub Secrets)
```
DSQL_ENDPOINT=your-dsql-endpoint
DSQL_USER=admin
DSQL_DATABASE=postgres
DSQL_REGION=ap-northeast-2
KIWOOM_API_APP_KEY=your-key
KIWOOM_API_APP_SECRET=your-secret
```

## 주요 변경사항

### ✅ 보안 개선
- AWS 자격증명 환경변수 제거
- ECS Task Role 기반 인증 적용
- secretkey.properties에서 AWS 키 제거

### ✅ 로컬 개발 편의성
- Docker Compose로 PostgreSQL 실행
- 프로필별 설정 완전 분리
- 개발용 더미 API 키

### ✅ 인프라 최적화
- Docker 헬스체크 start-period 120초
- 메모리 512MB 표준화
- 프로필별 설정 분리

## 트러블슈팅

### 로컬 환경
```bash
# Docker 상태 확인
docker ps

# PostgreSQL 연결 확인
docker exec -it stock-postgres psql -U postgres -d stocktrading

# 로그 확인
docker logs stock-postgres

# 포트 충돌 시
docker-compose down
netstat -ano | findstr :5432  # Windows
lsof -i :5432                 # macOS/Linux
```

### 프로덕션 환경
```bash
# ECS Task 로그 확인
aws logs describe-log-groups --log-group-name-prefix /ecs/stock-trading

# Task Role 권한 확인
aws iam list-attached-role-policies --role-name ecsTaskRole-stock-trading
```

## 개발 가이드

### 새로운 개발자 온보딩
1. Docker Desktop 설치 및 실행
2. 프로젝트 클론: `git clone <repo>`
3. PostgreSQL 시작: `docker-compose up -d`
4. IntelliJ에서 "Local Development" 실행
5. http://localhost:8080 접속 확인

### 일반적인 개발 워크플로우
```bash
# 1. PostgreSQL 시작
docker-compose up -d

# 2. 애플리케이션 실행 (IntelliJ 또는 CLI)
./gradlew :stock-service:bootRun --args='--spring.profiles.active=local'

# 3. 개발 완료 후 정리
docker-compose down
```

### 프로덕션 배포
1. main 브랜치에 코드 푸시
2. GitHub Actions 자동 실행
3. ECS 서비스 자동 업데이트
4. 헬스체크 통과 후 서비스 활성화

## 파일 구조
```
stock-service/
├── src/main/resources/
│   ├── application.yml          # 공통 + 프로필별 설정
│   ├── application-local.yml    # 로컬 전용 설정 (옵션)
│   └── secretkey.properties     # 프로덕션 시크릿 (AWS 키 제거됨)
├── .env                         # 로컬 환경변수
└── Dockerfile                   # 프로덕션 컨테이너
docker-compose.yml               # 로컬 PostgreSQL
```

